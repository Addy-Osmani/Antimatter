# Architecture Deep Dive

Antimatter bridges the gap between your local development environment and your mobile device **without requiring any official APIs** from the host IDE. Instead, it reverse-engineers the AntiGravity agent's file-system output and exposes it over a secure WebSocket channel.

!!! abstract "TL;DR"
    AntiGravity writes agent logs to disk → the extension watches them with `fs.watch` → parses into `TrajectoryStep` objects → streams over a WebSocket → through a Cloudflare tunnel → to the Android app, which renders them in real-time with Jetpack Compose.

---

## :material-sitemap: High-Level Architecture

Antimatter operates through two distinct bridges depending on your environment, which both route into the exact same secure Cloudflare tunnel and Android client:

```text
┌──────────────────────┐        ┌──────────────────────────────────┐
│   AntiGravity IDE    │        │   VS Code Extension (Bridge)     │
│                      │        │  ├─ BrainWatcher (fs.watch)      │
│  Agent writes logs   │ ─────▶ │  ├─ BridgeWebSocketServer (ws)   │
│  to transcript.jsonl │        │  └─ Terminal proxy (node-pty)    │
└──────────────────────┘        │                                  │
                                │                                  │
┌──────────────────────┐        │   Antigravity 2.0 (Daemon)       │
│   Antigravity 2.0    │        │  ├─ agent_bridge.py              │
│                      │        │  ├─ asyncio WebSocket Server     │
│  Core Agent Process  │ ─────▶ │  └─ Native Plugin Integration    │
└──────────────────────┘        └───────────────┬──────────────────┘
                                                │ Cloudflare Tunnel
                                                ▼
                                ┌──────────────────────────────────┐
                                │   Android App (Client)           │
                                │                                  │
                                │  BridgeWebSocket (OkHttp)        │
                                │  ├─ Token auth + Ed25519 verify  │
                                │  └─ emits Flow<InboundMessage>   │
                                │                                  │
                                │  Feature Screens (Compose)       │
                                │  ├─ ChatScreen + ChatViewModel   │
                                │  ├─ FilesScreen + FilesViewModel │
                                │  ├─ TerminalScreen + TerminalVM  │
                                │  └─ ConnectScreen + ConnectionVM │
                                │                                  │
                                │  Room DB (offline history)       │
                                └──────────────────────────────────┘
```

---

## :material-numeric-1-circle: The File System Monitor (Reverse-Engineering)

Since AntiGravity IDE does **not** provide an official API to stream agent thoughts, Antimatter's IDE Extension uses pure file-system monitoring to intercept the agent's brain activity. (Note: Antigravity 2.0 uses a native SDK plugin and bypasses this reverse-engineering).

When an IDE agent runs, it creates a unique conversation folder:

```text
<appDataDir>/brain/<conversation-id>/.system_generated/logs/transcript.jsonl
```

For the IDE, the extension's **`BrainWatcher`** uses `fs.watch` to monitor the `brain/` directory. For Antigravity 2.0, the **`agent_bridge.py`** daemon tails the same file directly using asyncio. In both cases, the bridge:

1. Detects the most recently modified conversation directory.
2. Tails the `transcript.jsonl` file line-by-line.
3. Parses each JSON line into a [`TrajectoryStep`](PROTOCOL.md#trajectorystep) object.
4. Broadcasts new steps to all connected clients via `STEP` frames.

!!! info "Step types"
    The agent's trajectory includes many step types: `userInput`, `plannerResponse`, `text`, `toolCall`, `runCommand`, `approvalInteraction`, `elicitation`, and more. See the full [`StepCase` enum](PROTOCOL.md#trajectorystep) for the complete mapping.

---

## :material-numeric-2-circle: The WebSocket Bridge

The bridge runs a WebSocket server bound to `127.0.0.1` (never exposed directly). The IDE extension uses [`ws`](https://github.com/websockets/ws), while the Antigravity 2.0 daemon uses Python's `websockets` library. To bypass firewalls and NATs, the bridge spawns a `cloudflared` process that creates a secure Cloudflare tunnel.

**Connection flow:**

1. Client connects to the public `wss://` URL.
2. **Origin validation** — only `vscode-webview://` and `*.cloudflareaccess.com` origins are accepted (prevents CSWSH).
3. **Token verification** — the 256-bit pairing token is checked with `crypto.timingSafeEqual`.
4. **Ed25519 handshake** — the client sends `AUTH_CHALLENGE` with a nonce; the bridge signs it and returns `AUTH_RESPONSE`.
5. Client is marked authenticated and receives `SESSION_STATE`.

!!! tip "Full protocol"
    See the [**WebSocket Protocol Reference**](PROTOCOL.md) for every message type, field, and close code.

---

## :material-numeric-3-circle: Message Routing

The **`MessageRouter`** dispatches each inbound JSON message to the appropriate handler based on its `type` field:

| Message type | Handler module | Action |
|-------------|---------------|--------|
| `AUTH_CHALLENGE` | `AuthHandler` | Sign nonce, reply `AUTH_RESPONSE` |
| `SEND_MESSAGE`, `NEW_CONVERSATION`, `CANCEL_RESPONSE` | `ChatCommandHandler` | Inject into agent via `vscode.commands` |
| `ACCEPT_EDITS`, `REJECT_EDITS`, `*_HUNK` | `ChatCommandHandler` | Proxy diff decisions |
| `GET_FILES`, `READ_FILE`, `WRITE_FILE` | `FileCommandHandler` | Serve workspace tree/files |
| `EXECUTE_COMMAND` | `TerminalCommandHandler` | Spawn shell, stream output |
| `SUBSCRIBE_CONVERSATION`, `GET_HISTORY`, `GET_ARTIFACTS` | `extension.ts` | Replay trajectory / list conversations |
| `PING` | `extension.ts` | Reply `PONG` |

If a handler throws, the bridge sends an `ERROR` frame. Unknown types are logged and ignored.

---

## :material-numeric-4-circle: Remote Terminal Execution

The `TerminalCommandHandler` proxies raw shell commands from the phone to the host:

1. Listens for `EXECUTE_COMMAND` messages over the WebSocket.
2. Uses Node.js `child_process.spawn` to launch the host shell.
3. Streams `stdout` and `stderr` back as `COMMAND_OUTPUT` messages in real-time.

!!! danger "Security"
    The terminal runs with the same permissions as the VS Code process. On the client side, this is gated behind a **biometric lock** (Android `androidx.biometric` API) so only the physical device owner can execute commands.

---

## :material-numeric-5-circle: Payload Serialization & Compression

- Parsed `TrajectoryStep` objects are serialized to JSON and broadcast to connected clients.
- Full conversation histories can exceed **10+ MB**, so the extension batches them into `STEP_BATCH` arrays when a client subscribes.
- **`permessage-deflate`** compression is enabled on the WebSocket server to reduce bandwidth (configurable chunk size and memory level).
- Maximum WebSocket payload: **10 MiB**.

---

## :material-numeric-6-circle: The Android Client

The app is built natively with **Kotlin** and **Jetpack Compose**, following a multi-module MVVM architecture:

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Networking** | OkHttp + `BridgeWebSocket` | WebSocket client, token + Ed25519 auth |
| **Background** | `BridgeService` (Foreground Service) | Keeps socket alive when app is backgrounded |
| **Persistence** | Room + DataStore | Offline conversation/step/artifact history |
| **DI** | Hilt | Dependency injection |
| **UI** | Jetpack Compose | Declarative UI with Material 3 theming |
| **Rendering** | Custom `MarkdownText` | Renders AI Markdown responses with syntax highlighting |

**Key rendering decisions:**

- Chat uses `reverseLayout` `LazyColumn` to anchor at the bottom — as AI "thinking" bubbles expand, older messages scroll up smoothly without jitter.
- Each `TrajectoryStep` maps to a `StepCase` enum that drives rendering: plain text, tool call cards, run-command blocks, approval prompts, etc.

---

## :material-numeric-7-circle: Extensibility

Because the core data structure is unified around `TrajectoryStep`, **any future tools or plugins** added to AntiGravity will automatically be parsed by the file system monitor and streamed to the mobile app — as long as they write to the JSONL log format.

The modular architecture also makes it straightforward to add new feature modules on both sides:

- **Extension**: add a new `feature/` handler and register it on the `MessageRouter`.
- **Android**: add a new `:feature:*` module with a Screen + ViewModel.

---

## :material-arrow-right-bold: Next Steps

- [**WebSocket Protocol Reference**](PROTOCOL.md) — the full message contract
- [**VS Code Extension Reference**](EXTENSION.md) — module map and source layout
- [**Android App Reference**](ANDROID.md) — Gradle module graph and screen/ViewModel map
