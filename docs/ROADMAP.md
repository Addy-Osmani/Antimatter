# Roadmap

Planned features and architectural improvements for the Antimatter ecosystem. Items are prioritized by security first, then core functionality, then quality-of-life.

!!! info "Want to help?"
    Many of these items are great first contributions. Check the [Contributing Guide](CONTRIBUTING.md) to get started.

---

## :material-shield-lock: Security & Architecture

### :material-check-circle: Independent Adapter Model (v0.1.4)

**Status:** :material-check-circle: Shipped

We have successfully decoupled the core security gateway (`antimatter-core`) from the individual agent adapters (`ag`, `ag2`, `cc`). The Gateway now exclusively handles Ed25519 pairing and Cloudflare zero trust, while adapters act as lightweight local IPC clients.

### :material-lock: End-to-End Encryption (E2EE)

**Status:** :material-check-circle: Shipped

Currently, the WebSocket connection is secured by TLS (via Cloudflare) and a 256-bit pairing token with Ed25519 handshake. Furthermore, to ensure **absolute privacy even from tunnel providers**, we have implemented true E2EE using a Diffie-Hellman key exchange:

- Traffic encrypted *before* leaving the Gateway
- Decrypted only on the Android and iOS device (via CryptoKit / JCA)
- **Zero-knowledge routing** through any intermediary (Cloudflare, proxies, etc.)

### :material-fingerprint: Biometric Authentication Gate

**Status:** :material-check-circle: Shipped

Before establishing the WebSocket connection or waking the app, require local biometric authentication (fingerprint or Face Unlock) on the Android device to ensure only the authorized owner can issue commands to the agent.

---

## :material-rocket-launch: Core Features

### :material-swap-horizontal: Remote Workspace Switching

**Status:** :material-check-circle: Shipped

Allow users to browse and switch the active VS Code workspace from the Android app.

!!! warning "Security implications"
    Granting the companion app filesystem navigation vastly expands the attack surface. If implemented, this will require:

    - Pre-approved workspace whitelists
    - Secondary biometric confirmations
    - Restricted filesystem read access

### :material-bell-ring: Push Notifications

**Status:** :material-check-circle: Shipped

Keep users informed when they are away from the app:
- Send local Android notifications when the AI agent completes a long-running task.
- Notify the user when the agent requires manual approval or encounters an error.

### :material-magnify: Conversation Search

**Status:** :material-check-circle: Shipped

Implement full-text search across the conversation history using Room's FTS (Full Text Search) extension, allowing users to quickly find past commands, code snippets, or agent responses directly from their phone.

### :material-check-all: Message Retry with Acknowledgment

**Status:** :material-check-circle: Shipped

Enhance the WebSocket reliability for unstable cellular connections:
- Added ACK validation for critical operations.
- Implemented an automatic retry queue that holds outbound messages and re-sends them (up to 3 times) upon timeout.

### :material-image-multiple: Multimodal Media Support

**Status:** :material-check-circle: Shipped

Enable rich multimodal interactions between the mobile app and the agent:
- Support sending images and audio clips directly from the mobile app to the agent for processing.
- Render inline images, audio playback, and other rich file types directly in the chat UI.

---

## :material-timeline-check: Status Legend

| Icon | Meaning |
|------|---------|
| :material-check-circle: | Shipped |
| :material-progress-wrench: | In progress |
| :material-clock-outline: | Planned |
| :material-thought-bubble: | Under consideration |
