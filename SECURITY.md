# Security Policy

Operating a remote-control interface for an IDE with terminal access demands enterprise-grade security. The Antimatter project takes the security of your host development environment extremely seriously.

## Supported Versions
Currently, only the `main` branch and the latest published GitHub Release are supported with security updates.

## Reporting a Vulnerability
If you discover a security vulnerability within Antimatter, please do **NOT** open a public issue. Instead, send an email directly to the maintainers or use GitHub's private vulnerability reporting feature.

---

## Implemented Security Mechanisms

Because Antimatter exposes a local WebSocket server, we have implemented several critical security safeguards to prevent unauthorized access and remote code execution.

### 1. Bearer Token Authentication
By default, the WebSocket server does **not** allow unauthenticated connections.
-   Upon initialization, the VS Code extension generates a cryptographically secure, high-entropy 256-bit Bearer Token using Node's `crypto.randomBytes()`.
-   The extension rejects any incoming HTTP upgrade request that does not present this exact token.
-   This token is securely transmitted to the Android client via a local QR code pairing mechanism.

### 2. Origin Header Validation
To protect against **Cross-Site WebSocket Hijacking (CSWSH)**, the extension enforces strict `Origin` header validation. Malicious websites running in your browser cannot silently initiate a connection to `ws://localhost:8765` to hijack your IDE.

### 3. Path Normalization & Sandboxing
The Android client has the ability to request file tree data and file contents. To prevent **Local File Arbitrary Read** vulnerabilities (e.g., requesting `../../../../etc/passwd`), the extension strictly sanitizes and normalizes all incoming file paths. Reads are sandboxed to the active workspace or the `.gemini/antigravity-ide` directory.

### 4. Denial of Service (DoS) Mitigation
The extension employs a strict **Token Bucket Rate Limiting** algorithm. This prevents an attacker (or a runaway bug in the Android client) from flooding the WebSocket with thousands of payloads per second, which would exhaust the host machine's memory buffer and crash the IDE.

### 5. Secure Tunnels (Cloudflare Zero Trust)
We actively discourage the use of unencrypted, public third-party tunnels (like Localtunnel) due to Man-in-the-Middle (MITM) interception risks. Antimatter natively supports **Cloudflare Zero Trust (`cloudflared`)**, allowing you to route traffic over an encrypted tunnel protected by OAuth identity verification.
