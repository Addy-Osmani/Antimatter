# Antimatter Architecture

Antimatter is an open-source bridge ecosystem that connects your mobile device directly to the local **Google AntiGravity IDE** running on your host machine.

> **Disclaimer**: Antimatter is an unofficial, community-driven, open-source project. It is **NOT** an official product of Google.

## System Overview
Because the AntiGravity IDE does not expose a public, formal API (REST or WebSocket) for external control, the Antimatter project relies entirely on **undocumented file-system heuristics and native VS Code event hooks**.

The system is composed of two primary components:
1.  **Antimatter Bridge (VS Code Extension)**: A Node.js daemon running inside the IDE that acts as a message broker.
2.  **Antimatter Client (Android App)**: A Jetpack Compose mobile application that connects to the Bridge via WebSockets.

---

## 1. The Outbound Telemetry Stream (IDE -> Mobile)
AntiGravity maintains state and history within a hidden system directory (`~/.gemini/antigravity-ide/brain/`). Within each unique conversation session, the IDE records every action taken by the AI agent—including thought processes, tool invocations, shell command executions, and raw Markdown generation—into a `transcript.jsonl` file.

### Dynamic File Tailing
The VS Code extension operates a highly optimized Node.js file system watcher on this JSON Lines file. 
-   **Byte-Range Tailing:** Rather than reading the entire file into memory (which can cause severe memory leaks on long trajectories), the extension maintains a byte-offset cursor and only parses newly appended bytes.
-   **Dynamic Reattachment:** Because new sessions generate new folders, the watcher dynamically monitors the parent `brain/` directory to seamlessly attach to the active session's transcript file.

Once parsed, the extension serializes these raw strings into structured `TrajectoryStep` JSON objects and broadcasts them via the WebSocket protocol to the Android client.

---

## 2. The Inbound Command Stream (Mobile -> IDE)
To inject commands from the mobile application back into the IDE without formal APIs, Antimatter relies on native VS Code Command Palette hooks.

### Prompt Injection
When a user sends a chat message from the Android client, the extension receives the payload and executes the native command `vscode.commands.executeCommand('antigravity.sendPromptToAgentPanel', msg.text)`. This allows the extension to safely inject sanitized text directly into the agent's context window.

### Diff Review and Remote Orchestration
The Android app provides native UI components to review file modifications proposed by the agent. By transmitting specific payloads, the mobile client can trigger commands like `antigravity.prioritized.agentAcceptAllInFile` remotely.

---

## 3. Android Client Architecture
The Android client (`antimatter_app`) is built using modern Kotlin paradigms.

### UI & Presentation Layer
-   **Jetpack Compose:** The entire UI is built declaratively. Complex Markdown and code syntax highlighting are rendered using Markwon and Prism4j.
-   **MVVM Architecture:** Uses `ChatViewModel` and `FilesViewModel` with `StateFlow` to ensure UI state survives configuration changes (like screen rotation) without dropping WebSocket frames.

### Networking & Data Layer
-   **OkHttp WebSockets:** Maintains the persistent bidirectional connection.
-   **Foreground Services:** The `BridgeService.kt` operates as an Android Foreground Service, ensuring the WebSocket connection remains alive and continues fetching the agent's thought process even when the app is minimized.
-   **EncryptedDataStore:** Secures connection strings, WebSocket URLs, and Bearer Tokens using the Android Keystore system.

### Distribution Flavors
To comply with strict FOSS requirements (like F-Droid), the Android app uses Product Flavors:
-   **`foss`:** Completely strips proprietary trackers and relies on `com.google.zxing:core` (pure Java) for barcode scanning.
-   **`standard`:** Contains Firebase Crashlytics for telemetry and debugging.
