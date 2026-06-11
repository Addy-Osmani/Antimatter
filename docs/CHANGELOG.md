# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## :material-tag: [1.1.0] — 2026-06-11

### :material-plus-circle: Added

- **Antigravity 2.0 Integration** — full support for the new Antigravity 2.0 standalone application via a native Python (`asyncio`) daemon plugin.
- **Dual-Bridge Architecture** — the ecosystem now maintains two separate bridges (`extension/` for IDE and `plugin/` for 2.0) that share the same WebSocket protocol.
- **Python Ed25519 Handshake** — ported the cryptographic identity verification to Python for the new daemon.

### :material-sync: Changed

- **Thought Process Streaming** — fixed bugs in `agent_bridge.py` step parsing to ensure the Android UI correctly indexes `plannerResponse` and `text` streams without creating blank bubbles.
- **Documentation Overhaul** — completely updated the repository `README.md`, `ARCHITECTURE.md`, and MkDocs navigation to reflect the dual-bridge architecture.

## :material-tag: [1.0.0] — 2026-06-10

### :material-plus-circle: Added

- **Biometric Authentication** — integrated Android `BiometricPrompt` (fingerprint/face) to gate sensitive features like the Remote Terminal.
- **Remote Terminal** — full terminal UI to execute shell commands on the host via the extension's `child_process.spawn`.
- **Partial Text Selection** — long-press user/AI chat messages to select specific lines and trigger the native Android copy/share toolbar.
- **Android App Links (HTTPS)** — replaced legacy `antimatter://` deep links with verified HTTPS App Links to prevent intent hijacking.
- **SQLCipher Data-at-Rest Encryption** — all chat histories and trajectory data encrypted at rest with a 256-bit key from the Android Keystore.
- **Strict Origin Validation** — regex-based validation against `cloudflareaccess.com` in the WebSocket server prevents Cross-Site WebSocket Hijacking.
- **Strict Localhost Binding** — WebSocket bridge binds to `127.0.0.1`, blocking unauthenticated LAN access.
- **Token Authentication** — 256-bit Bearer Token with `crypto.timingSafeEqual()` for all connections.
- **Ed25519 Handshake** — persistent keypair for bridge identity verification.
- **Secure QR Pairing** — one-scan transfer of WebSocket URL, Bearer Token, and Ed25519 public key.
- **ZXing Core Migration** — replaced `com.google.mlkit:barcode-scanning` with pure-Java `com.google.zxing:core` for F-Droid FOSS compliance.
- **Cloudflare Zero Trust Integration** — deprecated insecure Localtunnel in favor of `cloudflared` tunnels.
- **Rate Limiting** — per-IP token-bucket rate limiting prevents connection floods and DoS.

### :material-sync: Changed

- **UI/UX Polish** — fixed keyboard padding overlaps and double-inset spacing in the Compose `Scaffold` hierarchy.
- **Atomic File Operations** — `manual_input.json` writes now use `fs.renameSync` with temp files, fixing intermittent message delivery failures.
- **File System Sandboxing** — enforced strict path normalization and workspace-scoped reads.
- **Dynamic Watcher Reattachment** — fixed race conditions where `fs.watch` missed logs if the app connected before the agent started.

### :material-minus-circle: Removed

- **Plaintext Cloudflare Config** — removed `antimatter.cloudflareClientSecret` from `package.json` to prevent accidental secret commits.
- **Localtunnel Fallback** — removed the `useLocalTunnel` option to prevent unencrypted MITM interceptions.
- **Unsanitized Command Injection** — replaced raw text inputs with structured `vscode.commands.executeCommand` calls.

---

## :material-tag-outline: [0.1.0] — 2026-06-01

### :material-plus-circle: Added

- Initial release of the Antimatter ecosystem.
- Bi-directional WebSocket bridge connecting the Android companion app to the local AntiGravity IDE.
- Real-time `transcript.jsonl` tailing and `TrajectoryStep` serialization.
- Basic Android Jetpack Compose UI with Markdown rendering.
