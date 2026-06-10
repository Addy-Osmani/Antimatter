# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-06-10

### Added
- **Biometric Authentication:** Integrated Android Biometric Prompt (Fingerprint/Face Unlock) to restrict sensitive app features like the Remote Terminal.
- **Remote Terminal:** Added a fully functional terminal UI to remotely execute shell commands on your desktop via the VS Code extension's `child_process.spawn`.
- **Partial Text Selection:** Wrapped user and AI chat messages in selection containers, enabling the native Android copy/share toolbar for specific lines of code.
- **Android App Links (HTTPS):** Replaced legacy custom URI deep links (`antimatter://`) with verified HTTPS App Links to prevent intent hijacking by malicious local apps.
- **SQLCipher Data-at-Rest Encryption:** Integrated SQLCipher into the Android Room database, encrypting all chat histories and source code trajectories at rest using a 256-bit key from the Android Keystore.
- **Strict Origin Validation:** Enforced regex-based validation against `cloudflareaccess.com` in the VS Code WebSocket server to prevent Cross-Site WebSocket Hijacking.
- **Strict Localhost Binding:** The VS Code WebSocket bridge now explicitly binds to `127.0.0.1`, completely blocking unauthenticated access from the raw local network.
- **Token Authentication:** Implemented a cryptographically secure 256-bit Bearer Token handshake with `crypto.timingSafeEqual()` for all WebSocket connections, completely mitigating unauthorized local network access and timing attacks.
- **Secure Pairing:** Introduced a QR code-based pairing mechanism for securely transferring WebSocket URLs and Bearer Tokens to the Android client.
- **ZXing Core Migration:** Completely replaced `com.google.mlkit:barcode-scanning` with pure-Java `com.google.zxing:core` to eliminate pre-compiled C++ binaries (`.so` files) that caused F-Droid CI failures.
- **Cloudflare Zero Trust Integration:** Fully deprecated the insecure Localtunnel dependency in favor of authenticated `cloudflared` tunnels.
- **Token Bucket Rate Limiting:** Implemented strict inbound rate limiting to prevent WebSocket connection floods and memory exhaustion/DoS attacks.

### Changed
- **UI/UX Polishing:** Fixed keyboard padding overlaps and double-inset spacing bugs in the Jetpack Compose `Scaffold` hierarchy to ensure smooth rendering underneath the status and navigation bars.
- **Atomic File Operations:** Transitioned the `manual_input.json` override mechanism to use atomic `fs.renameSync` with temporary files, resolving intermittent message delivery failures caused by OS-level file locking contentions.
- **File System Sandboxing:** Enforced strict path normalization and sandbox boundaries within the extension to mitigate Local File Arbitrary Read vulnerabilities.
- **Dynamic Watcher Reattachment:** Overhauled `fs.watch` logic to monitor the parent directory, fixing race conditions where the extension failed to capture logs if the Android app connected before the IDE instantiated a new chat session.

### Removed
- **Plaintext Cloudflare Config:** Removed `antimatter.cloudflareClientSecret` from `package.json` configurations to prevent accidental commits of zero-trust secrets to public repositories.
- **Localtunnel Default Configuration:** Removed the `useLocalTunnel` fallback to prevent unencrypted Man-in-the-Middle (MITM) interceptions of the IDE data stream.
- **Unsanitized Command Injection:** Removed raw text inputs in favor of structured, sanitized API calls via `vscode.commands.executeCommand('antigravity.sendPromptToAgentPanel')`.

## [0.1.0] - 2026-06-01

### Added
- Initial release of the Antimatter ecosystem.
- Bi-directional WebSocket bridge connecting the Android companion app to the local Google AntiGravity IDE.
- Real-time `transcript.jsonl` tailing and TrajectoryStep serialization.
- Basic Android Jetpack Compose UI with Markdown rendering.
