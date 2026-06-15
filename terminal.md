# Native PTY Terminal Architecture Sketch

## 1. Core Philosophy: Decoupled & Direct

The new terminal feature will be a **Native PTY (Pseudo-Terminal)** implementation. Unlike the old system which sent basic strings to an AI agent, this new terminal connects **directly to the low-level Antimatter Gateway (Core)**.

By bypassing the agent adapters entirely, we achieve:

* **Absolute Stability**: If the AI agent adapter crashes, loops, or restarts, your terminal session remains completely uninterrupted.
* **True Interactivity**: Because it allocates a real PTY, you can run interactive TUI applications like `htop`, `vim`, `nano`, or `ssh` directly from your phone.
* **Zero Overhead**: The data stream (ANSI escape codes) routes straight from the host OS to the mobile device with no middleware processing.

---

## 2. Architecture Diagram

```mermaid
graph TD
    subgraph Mobile Device (iOS/Android)
        UI[Native Terminal UI Emulator]
        E2EE_Client[E2EE Crypto Engine]
        Biometrics[Biometric Lock]
    end

    subgraph Host Machine (Gateway Core)
        WSS[WebSocket Server]
        E2EE_Server[E2EE Crypto Engine]
        PTY_Manager[PTY Process Manager]
        Shell[Bash / Zsh Shell]
    end

    subgraph Agent Adapters
        AG[ag Adapter]
        AG2[ag2 Adapter]
    end

    %% Terminal Flow
    Biometrics -.->|Gate / Unlocks| UI
    UI <-->|PTY_INPUT / PTY_OUTPUT| E2EE_Client
    E2EE_Client <-->|Cloudflare WSS| WSS
    WSS <--> E2EE_Server
    E2EE_Server <-->|ANSI Bytes| PTY_Manager
    PTY_Manager <-->|Read/Write fd| Shell

    %% Agent Flow is separate
    E2EE_Server -.->|AGENT_STEP| AG
    E2EE_Server -.->|AGENT_STEP| AG2

    classDef terminal fill:#2d3436,stroke:#0984e3,stroke-width:2px,color:#fff;
    class UI,PTY_Manager,Shell terminal;
```

---

## 3. Protocol Additions

We will introduce a new, dedicated communication channel in our E2EE protocol exclusively for the terminal.

| Payload Type | Direction | Purpose |
|--------------|-----------|---------|
| `PTY_START` | Mobile ➔ Gateway | Requests a new shell instance. Sends initial screen dimensions (Rows/Cols). |
| `PTY_INPUT` | Mobile ➔ Gateway | Raw keystrokes (including Ctrl+C, arrows) sent to the host shell stdin. |
| `PTY_OUTPUT` | Gateway ➔ Mobile | Raw ANSI escape sequences (colors, cursor movements) streamed to the mobile UI. |
| `PTY_RESIZE` | Mobile ➔ Gateway | Triggered when the user rotates their phone, dynamically resizing the host PTY rows/cols. |
| `PTY_PING` | Mobile ➔ Gateway | Keepalive ping (e.g., every 30s) to detect dropped mobile networks and prevent UI freezing. |

---

## 4. Implementation Details

### Gateway (Python Core)

* **Dependency**: Utilize the `ptyprocess` library (via pip/poetry in `pyproject.toml`) as the robust pseudo-terminal backend to spawn and manage the user's default shell (e.g., `/bin/bash` or `/bin/zsh`). It cleanly handles `SIGWINCH` and complex PTY lifecycles.
* Map the master file descriptor (fd) to an asynchronous read loop.
* Whenever bytes are read from the fd, wrap them in `PTY_OUTPUT`, encrypt them via AES-GCM, and push them down the socket.

### Mobile Clients (iOS & Android)

* **Terminal Rendering (iOS)**: We will integrate **SwiftTerm** ([github.com/migueldeicaza/SwiftTerm](https://github.com/migueldeicaza/SwiftTerm)). We will add this as a direct **Swift Package Manager (SPM)** dependency in `Package.swift`, avoiding repository bloat while getting automatic updates for the pure-Swift VT100/Xterm emulator.
* **Terminal Rendering (Android)**: We will integrate the **Termux TerminalView** core components from `termux-app` ([github.com/termux/termux-app](https://github.com/termux/termux-app)). Since Termux no longer publishes standalone Maven packages, we will incorporate it as a **Git Submodule** in an `android/vendor/` directory, directly linking to their `:terminal-view` and `:terminal-emulator` Gradle modules.
* **Keyboard Handling**: Implement a custom accessory keyboard row above the standard iOS/Android keyboard to provide quick access to `Ctrl`, `Alt`, `Esc`, `Tab`, and Arrow Keys.
* **Security Lock**: The terminal screen will be fully inaccessible until a successful `FaceID` or `Fingerprint` scan is completed.

---

## 5. Resilience & Edge Cases

To make this architecture truly bulletproof against real-world mobile networking conditions, we will implement three critical features:

### 1. Backpressure Handling
Interactive apps like `htop` or fast scrolling logs can flood the WebSocket with `PTY_OUTPUT`. If the mobile client's rendering engine falls behind, the app could OOM (Out of Memory). 
**Solution**: Implement a queue limit on the Gateway.
```python
if output_queue.qsize() > 1000:
    output_queue.clear()  # Drop old frames, send fresh screen
```

### 2. Session Persistence (Mosh-style resume)
If the mobile OS kills the Antimatter app in the background, the user shouldn't lose their active `vim` session. 
**Solution**: The Gateway will store a `session_id` mapped to the PTY PID. When the mobile client reconnects, it reattaches to the existing PTY rather than spawning a new one.

### 3. Resize Debouncing
When a user rotates their phone, the OS may fire multiple resize events rapidly. 
**Solution**: Debounce the `PTY_RESIZE` events by 200ms on the client before transmitting to the Gateway. This prevents spamming `ioctl(TIOCSWINSZ)` on the host CPU.

---

## 6. Summary & Philosophy

By hooking directly into the Core Gateway, we transform the Antimatter app from just an "AI observer" into a full-fledged, secure **Remote Systems Administration** tool. The new philosophy is simple: **"The Terminal is first-class, AI can watch if it wants."**

Agents can safely *subscribe* to `PTY_OUTPUT` for context, but they never control the PTY directly. The separation of concerns ensures that your terminal is lightning fast, highly secure, and immune to AI hallucination bugs.
