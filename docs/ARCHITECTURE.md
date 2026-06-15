# Architecture Deep Dive

Antimatter bridges the gap between your local AI development environment and your mobile device using the **Independent Adapter Model**.

Instead of packing all authentication, cryptography, and networking logic into every individual agent integration, Antimatter cleanly separates the infrastructure from the agent-specific "hacks".

!!! abstract "TL;DR"
    A central Gateway (`antimatter-core`) runs in the background, managing the secure Cloudflare tunnel and Ed25519 pairing. Lightweight "adapters" (`ag`, `ag2`, `cc`) connect to this local Gateway over a WebSocket IPC channel (`127.0.0.1:8765`). When your Android app sends a message through the tunnel, the Gateway routes it to the correct adapter, which interacts with its respective AI agent.

---

## :material-sitemap: The Independent Adapter Model

```text
┌────────────────────────────────────────────────────────┐
│                   Independent Adapters                 │
│                                                        │
│  ┌──────────────┐   ┌───────────────┐   ┌───────────┐  │
│  │  Antigravity │   │  Antigravity  │   │  Claude   │  │
│  │   IDE (AG)   │   │   2.0 (AG2)   │   │ Code (CC) │  │
│  │ (TypeScript) │   │   (Python)    │   │ (Node.js) │  │
│  └──────┬───────┘   └───────┬───────┘   └─────┬─────┘  │
│         │                   │                 │        │
└─────────┼───────────────────┼─────────────────┼────────┘
          │                   │                 │
          ▼                   ▼                 ▼
 ┌───────────────────────────────────────────────────────┐
 │               Antimatter Gateway (`core/`)            │
 │                                                       │
 │  ┌─────────────────────────────────────────────────┐  │
 │  │                 IPC Router (ws)                 │  │
 │  └────────────────────────┬────────────────────────┘  │
 │                           │                           │
 │  ┌────────────────────────┴────────────────────────┐  │
 │  │        Security & Infrastructure Layer          │  │
 │  │  ├─ Ed25519 Cryptography & Handshakes           │  │
 │  │  ├─ OS Keyring Secret Storage                   │  │
 │  │  └─ Cloudflare Zero Trust Tunnel Manager        │  │
 │  └────────────────────────┬────────────────────────┘  │
 │                           │                           │
 └───────────────────────────┼───────────────────────────┘
                             │ Cloudflare Tunnel
                             ▼
 ┌───────────────────────────────────────────────────────┐
 │                  Android App (Client)                 │
 │                                                       │
 │  BridgeWebSocket (OkHttp)                             │
 │  ├─ Token auth + Ed25519 verify                       │
 │  └─ emits Flow<InboundMessage>                        │
 │                                                       │
 │  Feature Screens (Compose)                            │
 │  ├─ ChatScreen + ChatViewModel                        │
 │  └─ FilesScreen + FilesViewModel                      │
 └───────────────────────────────────────────────────────┘
```

---

## :material-numeric-1-circle: The Gateway (`core/`)

The Gateway is the brain of the operation. It runs as a background process on your machine and handles everything complex so the adapters don't have to:

1. **Cloudflare Tunnels**: It spawns `cloudflared` to expose a secure `wss://` endpoint.
2. **Ed25519 Handshake**: It generates the private keys, stores them securely in the OS keyring, and handles the cryptographic challenge-response with the Android app.
3. **IPC Routing**: It hosts a local WebSocket server at `ws://127.0.0.1:8765`. 

When the Android app connects via Cloudflare, the Gateway establishes the secure session. When the Android app asks "What agents are available?", the Gateway broadcasts `AVAILABLE_AGENTS` based on which local adapters are currently connected to its IPC server.

---

## :material-numeric-2-circle: The Adapters (`adapters/`)

Adapters are extremely lightweight, "dumb" clients. Because they don't have to handle security or tunnels, they can be written in any language and use whatever messy, reverse-engineering hacks are necessary to talk to a specific AI agent.

1. **`adapters/ag/`**: A TypeScript VS Code extension that watches local `transcript.jsonl` files and reads the workspace file tree.
2. **`adapters/ag2/`**: A Python background daemon that monitors the standalone Antigravity 2.0 application.
3. **`adapters/cc/`**: A Node.js daemon that uses the `@anthropic-ai/claude-agent-sdk` to stream Claude Code events.

When an adapter boots, it connects to `ws://127.0.0.1:8765` and sends:
`{"type": "REGISTER_ADAPTER", "name": "ag"}`

The Gateway notes this registration. If the Android app sends a message targeting `ag`, the Gateway forwards it to that specific WebSocket connection.

---

## :material-numeric-3-circle: Message Routing

The **MessageRouter** inside the Gateway dispatches each inbound JSON message.

| Message type | Target | Action |
|-------------|---------------|--------|
| `AUTH_CHALLENGE` | Gateway | Sign nonce, reply `AUTH_RESPONSE` |
| `GET_FILES`, `READ_FILE` | Target Adapter | Request workspace files |
| `SEND_MESSAGE` | Target Adapter | Inject prompt into the specific agent |
| `PING` | Gateway | Reply `PONG` to keep Cloudflare tunnel alive |

---

## :material-numeric-4-circle: The Android Client

The app is built natively with **Kotlin** and **Jetpack Compose**, following a multi-module MVVM architecture:

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Networking** | OkHttp + `BridgeWebSocket` | WebSocket client, token + Ed25519 auth |
| **Background** | `BridgeService` (Foreground Service) | Keeps socket alive when app is backgrounded |
| **Persistence** | Room + DataStore | Offline conversation/step/artifact history |
| **UI** | Jetpack Compose | Declarative UI with Material 3 theming |
| **Rendering** | Custom `MarkdownText` | Renders AI Markdown responses with syntax highlighting |

When connecting, the Android app establishes the websocket to the Gateway. It presents a UI to let you pick which active agent adapter you want to interact with.

---

## :material-arrow-right-bold: Next Steps

- [**WebSocket Protocol Reference**](PROTOCOL.md) — the full message contract
- [**Adapters Overview**](ADAPTERS.md)
