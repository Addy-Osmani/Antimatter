# Antimatter IDE Roadmap

This document outlines the planned future features and architectural improvements for the Antimatter IDE (Android App & VS Code Extension).

## 🔒 Security & Privacy

### End-to-End Encryption (E2EE)
**Status:** Planned
Currently, the WebSocket connection is secured by TLS (via Cloudflare) and an auto-generated 256-bit Pairing Token, with an Ed25519 cryptographic handshake to prevent Man-in-the-Middle attacks. 
However, to ensure absolute privacy even from tunnel providers (like Cloudflare), we plan to implement true End-to-End Encryption (E2EE) using a Diffie-Hellman key exchange. 
- Traffic will be encrypted *before* leaving the VS Code extension.
- It will only be decrypted locally on the Android device.
- Ensures Zero-Knowledge routing through any intermediary proxy or tunnel.

## 💻 Core Features

### Advanced Terminal Integration
**Status:** Planned
The current terminal implementation uses a basic Node.js `child_process.spawn` to proxy shell commands over the WebSocket. 
The future goal is to implement a fully featured, isolated terminal environment:
- **Pseudo-Terminal (PTY) Support:** Use `node-pty` to provide a true TTY environment, allowing for interactive commands (like `nano`, `vim`, `htop`, or interactive prompts).
- **Sandboxing & Isolation:** Explore options to restrict the terminal session strictly to the workspace directory to prevent accidental global system modifications.
- **ANSI Escape Code Rendering:** Implement a fully compliant Xterm.js-style renderer in Jetpack Compose to correctly display colored output, cursor movements, and complex terminal UI layouts.

### Remote Workspace Switching
**Status:** Under Consideration / Long-term
Allowing the user to browse and switch the active VS Code workspace directly from the Android App.
- **The Need:** Sometimes you need to jump to a different project/workspace while on the go, without returning to the physical host machine.
- **Security Implications:** This is highly sensitive. Granting the companion app the ability to navigate the host file system and open new directories vastly expands the attack surface. 
- **Planned Approach:** If implemented, this feature will require an extremely strict security model (e.g., pre-approved workspace whitelists, secondary biometric confirmations, or restricted filesystem read access) to ensure a compromised pairing token cannot be used to arbitrarily browse the host OS.
