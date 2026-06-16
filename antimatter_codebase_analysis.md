# Antimatter Ecosystem — Codebase Deep Dive

## What Is Antimatter?

Antimatter is an open-source **bridge ecosystem** that lets you remotely control and monitor your local AI development agents (Google Antigravity IDE, Antigravity 2.0, Claude Code) from a mobile device (Android/iOS). It achieves this without opening firewall ports by tunneling through Cloudflare.

The project is available on F-Droid, MIT licensed, and community-maintained.

---

## Architecture: The Independent Adapter Model

The signature architectural decision of this project is the **separation of security/networking from agent-specific code**.

```
Mobile App (Android/iOS)
        │
        │  E2EE WebSocket (Cloudflare Tunnel — WSS)
        ▼
┌────────────────────────────────────────────┐
│        antimatter-core  (Python daemon)    │
│  ┌─────────────────────────────────────┐   │
│  │       GatewayServer  :8765          │   │
│  │  ├─ Rate limiting (IP-based)        │   │
│  │  ├─ Ed25519 Handshake Auth          │   │
│  │  ├─ E2EE (X25519 ECDH + AES-GCM)   │   │
│  │  ├─ PTY Manager (bash sessions)     │   │
│  │  └─ MessageRouter (IPC dispatch)   │   │
│  └─────────────────────────────────────┘   │
│             ▲               ▲              │
│      Cloudflare         cloudflared        │
└────────────────────────────────────────────┘
        │  Plaintext IPC WebSocket (127.0.0.1:8765)
        ├─────────────────┬──────────────────┐
        ▼                 ▼                  ▼
   adapters/ag/      adapters/ag2/      adapters/cc/
  (TypeScript VS    (Python daemon)   (Node.js daemon)
   Code extension)
```

**Why this matters:** Adapters never touch cryptographic keys, never see the internet — they only speak to the locally-bound IPC port. The Gateway is the sole security boundary. This means writing a new adapter for a new AI tool is trivially easy.

---

## Component Breakdown

### 1. `core/` — The Gateway (`antimatter-core` on PyPI)

**Language:** Python (asyncio + websockets)

**Key files:**
- [server.py](file:///home/saif/antimatter/core/gateway/src/antimatter_gateway/server.py) — `GatewayServer` class; the main event loop
- [router.py](file:///home/saif/antimatter/core/gateway/src/antimatter_gateway/router.py) — `MessageRouter`; IPC dispatch + file tree
- [pty_manager.py](file:///home/saif/antimatter/core/gateway/src/antimatter_gateway/pty_manager.py) — `PtyManager`; spawns `/bin/bash` PTY sessions
- [tunnel.py](file:///home/saif/antimatter/core/gateway/src/antimatter_gateway/tunnel.py) — `CloudflaredManager`
- [qr.py](file:///home/saif/antimatter/core/gateway/src/antimatter_gateway/qr.py) — QR pairing code generation

**Shared packages (installed from `core/`):**
- `antimatter_shared_config` — config model, OS keyring integration
- `antimatter_crypto` — Ed25519Auth, E2EESession (X25519 ECDH + HKDF + AES-GCM)
- `antimatter_fs` — file tree builder
- `antimatter_shared_protocol` — protocol constants

**Connection lifecycle (server side):**
1. Rate limit check on IP
2. Client sends `REGISTER_ADAPTER` → enters the adapter IPC loop (bypasses auth)
3. Client sends `AUTH_CHALLENGE` → Gateway signs nonce with Ed25519 private key → returns `AUTH_RESPONSE` + X25519 pubkey
4. Client sends `HELLO` with its X25519 pubkey → ECDH shared secret derived → HKDF produces two directional AES-256 keys (`c2s` and `s2c`)
5. All subsequent messages are E2EE encrypted AES-GCM envelopes with AAD direction tagging

**PTY support:** The `PtyManager` spawns a `/bin/bash` process using `ptyprocess`, reads its FD non-blockingly in the asyncio event loop, base64-encodes chunks, and broadcasts them as `PTY_OUTPUT` messages over the E2EE channel to mobile clients.

**CLI commands:** `antimatter start`, `antimatter qr`, `antimatter setup` (Cloudflare Zero Trust), `antimatter config set <key> <value>`

---

### 2. `adapters/` — The Lightweight IPC Clients

#### `adapters/ag/` — Antigravity IDE Adapter (TypeScript VS Code Extension)

The most feature-rich adapter. On activation, it:
- Connects to the Gateway at `ws://127.0.0.1:8765` and registers as `ag`
- Starts a `BrainWatcher` that tails `transcript.jsonl` files from the Antigravity IDE's brain directory, parsing trajectory steps in real-time and forwarding them to the Gateway
- Handles `SUBSCRIBE_CONVERSATION`, `GET_HISTORY`, `READ_FILE`, `WRITE_FILE`, `SEND_MESSAGE`, `PING`
- Streams `ACTIVE_FILE` events when the VS Code editor focus changes

Key classes: `GatewayClient`, `BrainWatcher`, `ChatCommandHandler`, `FileCommandHandler`, `HistoryManager`, `MessageRouter`, `ChatStateManager`

#### `adapters/cc/` — Claude Code Adapter (Node.js)

Minimal implementation. Registers as `cc`, listens for `SEND_MESSAGE` commands, and uses `@anthropic-ai/claude-agent-sdk` to stream Claude Code events back to the Gateway.

#### `adapters/ag2/` — Antigravity 2.0 Adapter (Python daemon)

Monitors the standalone Antigravity 2.0 application separately from the VS Code extension.

---

### 3. `android/` — The Android App (Kotlin + Jetpack Compose)

**Architecture:** Multi-module MVVM, Hilt DI, Room + DataStore

**Module structure:**
```
android/
├── app/              ← App entry, Hilt setup, navigation
├── core/
│   ├── network/      ← BridgeWebSocket, BridgeMessage, E2EESession
│   ├── data/         ← Room DB (AppDatabase, AppDao, entities)
│   └── ui/           ← shared UI components
└── feature/
    ├── connect/      ← QR scanner, ConnectScreen, ConnectionViewModel
    ├── chat/         ← ChatScreen, ChatViewModel, ChatBubble, ToolCallCard
    ├── files/        ← FilesScreen, FilesViewModel, FileViewScreen
    └── terminal/     ← TerminalScreen, TerminalViewModel
```

**Key files:**

[BridgeWebSocket.kt](file:///home/saif/antimatter/android/core/network/src/main/java/dev/saifmukhtar/antimatter/core/network/BridgeWebSocket.kt) — The core networking class. Uses OkHttp for the WebSocket. Implements:
- Auth flow: generates 32-byte nonce → sends `AUTH_CHALLENGE` → verifies Ed25519 signature → ECDH with returned X25519 pubkey → derives E2EE keys → sends `HELLO`
- Exponential backoff reconnection (up to 20 retries, capped at 30s)
- ACK-based retry queue for reliable delivery (3 retries, 5s timeout)
- All post-handshake messages encrypted with `E2EESession`

[E2EESession.kt](file:///home/saif/antimatter/android/core/network/src/main/java/dev/saifmukhtar/antimatter/core/network/E2EESession.kt) — Pure JCA implementation of X25519 ECDH + HKDF-SHA256 + AES-256-GCM. Handles the X.509 encoding dance needed for Android's `KeyFactory` to accept the Python library's raw 32-byte keys.

[BridgeMessage.kt](file:///home/saif/antimatter/android/core/network/src/main/java/dev/saifmukhtar/antimatter/core/network/BridgeMessage.kt) — Complete message model with sealed classes for `InboundMessage` and `OutboundMessage`, plus `TrajectoryStep` and `StepCase` enum.

[ChatViewModel.kt](file:///home/saif/antimatter/android/feature/chat/src/main/java/dev/saifmukhtar/antimatter/feature/chat/ChatViewModel.kt) — The richest ViewModel. Manages:
- Offline-first: loads cached steps from Room instantly, then delta-syncs with the server via `lastKnownStepCount`
- FTS (Room Full Text Search) for conversation history search
- Multimodal: encodes images to JPEG base64 data URIs (max 1024×1024, 70% quality)
- Artifact caching with GZIP compression in Room
- Agent switching (resets conversation on switch)
- Scroll state persistence per conversation

**Room DB entities:** `ConversationEntity`, step entities, `ArtifactEntity`, `AgentEntity`

**Background service:** `BridgeService` (foreground service) keeps the WebSocket alive when the app is backgrounded, with a persistent notification.

---

### 4. `ios/` — The iOS App (Swift/SwiftUI)

**Structure:** `Packages/` directory with local Swift Packages:
- `CoreData`, `CoreNetwork`, `CoreUI`
- `FeatureChat`, `FeatureConnect`, `FeatureFiles`, `FeatureTerminal`

[AntimatterApp.swift](file:///home/saif/antimatter/ios/AntimatterApp.swift) — SwiftUI app entry point

[AppCoordinator.swift](file:///home/saif/antimatter/ios/AppCoordinator.swift) — Navigation coordinator

The iOS app mirrors the Android feature set with SwiftUI, using `SwiftTerm` for the PTY terminal display (vs Android using Termux-style rendering) and `CryptoKit` for the E2EE implementation.

---

## Security Model (Defense in Depth)

| Layer | Mechanism | Purpose |
|-------|-----------|---------|
| Transport | Cloudflare TLS (WSS) | In-transit encryption |
| Identity | 256-bit Bearer Token (timing-safe compare) | Prevents unauthorized connections |
| Identity | Ed25519 MITM Guard | Proves Gateway identity; prevents tunnel impostor |
| Privacy | X25519 ECDH + HKDF + AES-256-GCM | Zero-knowledge E2EE (even from Cloudflare) |
| CSRF | Origin header validation | Blocks cross-site WebSocket hijacking |
| DoS | 5MB payload cap + IP rate limiting | Memory exhaustion protection |
| Sandboxing | Adapter/Gateway separation | Adapters never touch keys or internet |

**Directional E2EE keys:**
- `c2s_key = HKDF(shared_secret, info="antimatter-v1:client-to-server")`
- `s2c_key = HKDF(shared_secret, info="antimatter-v1:server-to-client")`

Each message has an AAD tag like `cmd:v1:msg_id:N` or `output:v1:msg_id:N` that prevents cross-direction replay attacks.

---

## WebSocket Protocol Summary

All frames are UTF-8 JSON. Key message types:

**Auth sequence:** `AUTH_CHALLENGE` → `AUTH_RESPONSE` → `HELLO` (E2EE established)

**Agent discovery:** `AVAILABLE_AGENTS` (server-pushed on adapter connect/disconnect)

**Conversation flow:** `SUBSCRIBE_CONVERSATION` → `SESSION_STATE` + `STEP_BATCH` → `GENERATING` → `STEP` → `RESPONSE_COMPLETE`

**File ops:** `GET_FILES` → `FILE_TREE` | `READ_FILE` → `FILE_CONTENT` | `WRITE_FILE`

**PTY:** `PTY_START` → `PTY_OUTPUT` stream (base64 ANSI sequences) ← `PTY_INPUT` (base64 keystrokes) | `PTY_RESIZE`

**Controls:** `SEND_MESSAGE`, `NEW_CONVERSATION`, `CANCEL_RESPONSE`, `ACCEPT_EDITS`/`REJECT_EDITS`, `NEXT_HUNK`/`PREV_HUNK`/`ACCEPT_HUNK`/`REJECT_HUNK`, `CHANGE_WORKSPACE`

---

## Roadmap Status

Everything listed in the roadmap is marked **Shipped** as of the current codebase:
- Independent Adapter Model ✅
- End-to-End Encryption ✅
- Biometric Authentication Gate ✅
- Native Remote PTY Terminal ✅
- Remote Workspace Switching ✅
- Push Notifications ✅
- Conversation Search (FTS) ✅
- Message Retry with ACK ✅
- Multimodal Media Support ✅

There are **no open planned items** in `ROADMAP.md` — suggesting the project is either complete or the roadmap is not yet updated with next targets.

---

## Key Design Decisions & Motives

**Why Cloudflare Quick Tunnels?** Zero firewall configuration, works on any network, free, ephemeral (good for security). Persistent Zero Trust tunnels are optional for enterprise setups.

**Why the adapter model?** Each AI tool needs bespoke "hacks" (reading internal transcript files, injecting prompts via undocumented APIs). Bundling that code with cryptography would mean security bugs in any agent-specific code could compromise the whole system. The airgap is intentional.

**Why Room + offline caching?** Mobile connections are unreliable. The delta-sync pattern (`lastKnownStepCount`) means reconnecting only fetches new steps, not the full history.

**Why directional AES keys (not one shared key)?** Prevents a compromised client from decrypting its own encrypted uploads and prevents replaying server→client traffic as client→server commands.

---

## Open Questions I Have

After this deep read, here are things I'd want to clarify before doing any development work:

1. The `adapters/ag/` extension registers with a fixed name `"ag"` but no `id` field — while the Gateway's `REGISTER_ADAPTER` handler expects an `id`. Is there a mismatch between the old VS Code extension and the new Gateway API?

2. The iOS app exists as Swift Packages under `ios/Packages/` but there's no `.xcodeproj` or `Package.swift` at the root — it looks like the Xcode project may have been deleted or moved. Is iOS development active?

3. The roadmap shows everything as "Shipped" — what are the actual next priorities for the project?
