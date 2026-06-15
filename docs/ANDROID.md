# Android App Reference

The client half of Antimatter is a native **Android** app built with **Jetpack Compose**,
**Hilt** (dependency injection), **Room** (local persistence), and **Gson** (JSON).

It connects to the `antimatter-core` Gateway over WebSocket, authenticates via an Ed25519 handshake, and then selects an active adapter to communicate with.

- Source: [`android/`](https://github.com/saifmukhtar/antimatter/tree/main/android)
- Package: `dev.saifmukhtar.antimatter`
- Multi‑module Gradle project (`settings.gradle.kts`)

## Module graph

```text
:app                     # MainActivity, Application, navigation host
├── :core:network        # WebSocket client, foreground service, protocol model
├── :core:data           # Room database, DAOs, entities, preferences
├── :core:ui             # Compose theme, Markdown renderer, shared UI utils
├── :feature:connect     # QR scan + pairing + connection state
├── :feature:chat        # Trajectory/chat UI + prompting
└── :feature:files       # Workspace browser + file viewer
```

## :core:network
| File | Responsibility |
|------|----------------|
| `BridgeWebSocket.kt` | The WebSocket client: connects to the Gateway, performs the Ed25519 `AUTH_CHALLENGE`/`AUTH_RESPONSE` handshake, and exposes inbound messages as a flow. |
| `BridgeService.kt` | A foreground `Service` that keeps the socket alive in the background and surfaces system alerts as notifications. |

## Selecting an Adapter

Because Antimatter now uses an **Independent Adapter Model**, the Android app connects to the Gateway first, rather than directly to the agent.

Once the `AUTH_RESPONSE` is verified, the Android app sends a `GET_AVAILABLE_AGENTS` payload. The Gateway responds with a list of currently connected adapters (e.g. `["ag", "ag2", "cc"]`). The user selects an adapter in the UI, and all subsequent `SEND_MESSAGE` payloads are stamped with that target so the Gateway can route them appropriately.

## Feature modules
Each feature follows a **Screen + ViewModel** (MVVM) pattern with Compose:

| Module | Screens | ViewModel |
|--------|---------|-----------|
| `:feature:connect` | `ConnectScreen`, `QRScannerScreen` | `ConnectionViewModel` |
| `:feature:chat` | `ChatScreen`, `ChatBubble`, `ThinkingBubble`, `ToolCallCard`, `MessageInput` | `ChatViewModel` |
| `:feature:files` | `FilesScreen`, `FileViewScreen` | `FilesViewModel` |

## Build

| Task | Command |
|------|---------|
| Lint | `./gradlew lintDebug` |
| Build debug APK | `./gradlew assembleDebug` |
| Install on device/emulator | `./gradlew installDebug` |

Open the `android/` directory in Android Studio and let Gradle sync.
