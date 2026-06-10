# Security Policy

Operating a remote-control interface for an IDE with terminal access demands enterprise-grade security. The Antimatter project takes the security of your host development environment extremely seriously.

## Supported Versions
Currently, only the `main` branch and the latest published GitHub Release are supported with security updates.

## Reporting a Vulnerability
If you discover a security vulnerability within Antimatter, please do **NOT** open a public issue. Instead, send an email directly to the maintainers or use GitHub's private vulnerability reporting feature.

---

## Implemented Security Mechanisms

Because Antimatter exposes a local WebSocket server, we have implemented several critical security safeguards to prevent unauthorized access and remote code execution.

### 1. Bearer Token & Ed25519 Authentication
By default, the WebSocket server does **not** allow unauthenticated connections.
- Upon initialization, the VS Code extension generates a cryptographically secure, high-entropy 256-bit Bearer Token using Node's `crypto.randomBytes()`. This provides mathematical equivalence to AES-256 encryption.
- The extension rejects any incoming HTTP upgrade request that does not present this exact token, making brute-force attacks impossible.
- Furthermore, the server proves its identity to the client using an **Ed25519 Cryptographic Handshake**, ensuring absolute protection against Man-in-the-Middle (MITM) attacks and server spoofing.
- The token is securely transmitted to the Android client via a local QR code pairing mechanism.

### 2. Local Biometric Security
While network traffic is heavily protected, physical access to your mobile device is protected locally. Sensitive features—such as the **Remote Terminal**—are locked behind Android's `androidx.biometric` API. The remote terminal proxy will *only* establish a session if you successfully authenticate with a Fingerprint or Face Unlock, ensuring that if your phone is left unlocked on a desk, an unauthorized person cannot execute host commands.

### 2. Origin Header Validation
To protect against **Cross-Site WebSocket Hijacking (CSWSH)**, the extension enforces strict `Origin` header validation. Malicious websites running in your browser cannot silently initiate a connection to `ws://localhost:8765` to hijack your IDE.

### 3. Path Normalization & Sandboxing
The Android client has the ability to request file tree data and file contents. To prevent **Local File Arbitrary Read** vulnerabilities (e.g., requesting `../../../../etc/passwd`), the extension strictly sanitizes and normalizes all incoming file paths. Reads are sandboxed to the active workspace or the `.gemini/antigravity-ide` directory.

### 4. Denial of Service (DoS) Mitigation
The extension employs a strict **Token Bucket Rate Limiting** algorithm. This prevents an attacker (or a runaway bug in the Android client) from flooding the WebSocket with thousands of payloads per second, which would exhaust the host machine's memory buffer and crash the IDE.

### 5. Secure Tunnels (Cloudflare Zero Trust)
We actively discourage the use of unencrypted, public third-party tunnels (like Localtunnel) due to Man-in-the-Middle (MITM) interception risks. Antimatter natively supports **Cloudflare Zero Trust (`cloudflared`)**, allowing you to route traffic over an encrypted tunnel protected by OAuth identity verification.
