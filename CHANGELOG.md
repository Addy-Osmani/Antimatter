# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Token Authentication:** Implemented a cryptographically secure 256-bit Bearer Token handshake for all WebSocket connections, completely mitigating unauthorized local network access.
- **Secure Pairing:** Introduced a QR code-based pairing mechanism for securely transferring WebSocket URLs and Bearer Tokens to the Android client.
- **Product Flavors (Android):** Introduced `foss` and `standard` product flavors. The `foss` build is 100% free and open-source, stripping all proprietary Google APIs to ensure F-Droid compliance.
- **ZXing Core Migration:** Completely replaced `com.google.mlkit:barcode-scanning` with pure-Java `com.google.zxing:core` to eliminate pre-compiled C++ binaries (`.so` files) that caused F-Droid CI failures.
- **Cloudflare Zero Trust Integration:** Fully deprecated the insecure Localtunnel dependency in favor of authenticated `cloudflared` tunnels.
- **Token Bucket Rate Limiting:** Implemented strict inbound rate limiting to prevent WebSocket connection floods and memory exhaustion/DoS attacks.

### Changed
- **Atomic File Operations:** Transitioned the `manual_input.json` override mechanism to use atomic `fs.renameSync` with temporary files, resolving intermittent message delivery failures caused by OS-level file locking contentions.
- **File System Sandboxing:** Enforced strict path normalization and sandbox boundaries within the extension to mitigate Local File Arbitrary Read vulnerabilities.
- **Dynamic Watcher Reattachment:** Overhauled `fs.watch` logic to monitor the parent directory, fixing race conditions where the extension failed to capture logs if the Android app connected before the IDE instantiated a new chat session.

### Removed
- **Localtunnel Default Configuration:** Removed the `useLocalTunnel` fallback to prevent unencrypted Man-in-the-Middle (MITM) interceptions of the IDE data stream.
- **Unsanitized Command Injection:** Removed raw text inputs in favor of structured, sanitized API calls via `vscode.commands.executeCommand('antigravity.sendPromptToAgentPanel')`.

## [0.1.0] - 2026-06-01

### Added
- Initial release of the Antimatter ecosystem.
- Bi-directional WebSocket bridge connecting the Android companion app to the local Google AntiGravity IDE.
- Real-time `transcript.jsonl` tailing and TrajectoryStep serialization.
- Basic Android Jetpack Compose UI with Markdown rendering.
