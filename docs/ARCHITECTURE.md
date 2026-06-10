# Architecture Deep Dive

Antimatter is built on the philosophy of bridging the gap between your local development environment and your mobile device, without requiring any official APIs from the host IDE. This document explains the core architecture of how Antimatter reverse-engineers and integrates with the Google AntiGravity IDE.

## High-Level Flow

1. **AntiGravity IDE**: The host IDE runs local agent trajectories, saving logs in a hidden `.system_generated` folder inside the workspace.
2. **VS Code Extension (The Bridge)**: Antimatter runs as a VS Code extension inside AntiGravity. It constantly monitors the filesystem, parses the JSONL agent logs, and spins up a local WebSocket server.
3. **Cloudflare Zero Trust**: The local WebSocket server is securely tunneled to the public internet via `cloudflared`, providing a public WSS endpoint (e.g., `wss://antimatter-extension...`).
4. **Android App (The Client)**: The mobile app connects to the WSS endpoint using a Bearer Token (scanned via QR code), receives the JSON payloads, and renders them using Jetpack Compose.

## 1. The File System Monitor (Reverse-Engineering)

Since AntiGravity does not provide an official API to stream agent thoughts, Antimatter uses pure file-system monitoring to intercept the agent's brain activity.

When an agent runs, it creates a unique `conversation-id` folder inside:
`<appDataDir>/brain/<conversation-id>/.system_generated/logs/transcript.jsonl`

The VS Code extension uses `fs.watch` to monitor the `brain` directory for the most recently modified conversation. It streams the `transcript.jsonl` file, parsing each JSON line into a `TrajectoryStep` object.

## 2. The WebSocket Bridge

The VS Code extension runs a `ws` WebSocket server. To bypass local network restrictions (such as firewalls or NATs), the extension programmatically spawns a `cloudflared` process. 
This process creates a secure Quick Tunnel, exposing the local WebSocket server to a public Cloudflare edge node.

## 3. Remote Terminal Execution (TerminalCommandHandler)

Antimatter allows users to proxy raw terminal commands from their phone to their host PC. This is handled by the `TerminalCommandHandler.ts` inside the VS Code extension:
- It listens for `EXECUTE_COMMAND` messages over the WebSocket.
- Uses Node.js `child_process.spawn` to launch `/bin/sh` or `cmd.exe`.
- Streams `stdout`, `stderr`, and `exit codes` back as `COMMAND_OUTPUT` messages, creating a live shell proxy.

## 4. Payload Serialization and Chunking

The parsed `TrajectoryStep` objects are serialized into JSON and broadcasted to connected Android clients. Because full conversation histories can easily exceed 10+ Megabytes, the extension chunks the history payloads into smaller arrays (e.g., 10 steps per message). This prevents Cloudflare Tunnel from aborting the connection due to WebSocket frame size limits.

## 5. The Android Client

The Android app is built natively using Kotlin and Jetpack Compose. 

- **Connection**: It uses `OkHttp` to establish the WebSocket connection.
- **Biometric Security**: Sensitive features (like the Remote Terminal) are locked behind Android's `androidx.biometric` API, ensuring that only the physical owner of the phone can execute host commands.
- **State Management**: It maintains a `ChatUiState` containing the conversation history. As new steps arrive, it patches the existing trajectory state.
- **Rendering**: It uses `reverseLayout` LazyColumns to natively anchor the chat to the bottom of the screen, ensuring that as AI "Thinking" bubbles expand, the UI smoothly pushes older messages upward without jitter. Markdown is rendered using `Markwon`.

## 6. Extensibility

Because the core data structure is unified around the `TrajectoryStep` class, any future tools or plugins added to the AntiGravity IDE will automatically be parsed by the file system monitor and sent to the mobile app, as long as they adhere to the JSONL logging format.
