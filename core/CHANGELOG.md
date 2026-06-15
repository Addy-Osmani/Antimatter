# Core Infrastructure Changelog

## [0.1.4] - Unreleased
### Added
- Extracted Gateway functionality from monolithic IDE extension into a standalone Python daemon.
- Independent Cloudflare Zero Trust tunnel management.
- OS Keyring support for secure Ed25519 secret storage.
- Local `127.0.0.1:8765` WebSocket IPC router for handling multi-adapter messages.

## [1.0.0] - 2026-06-10
### Added
- Strict Origin Validation against `cloudflareaccess.com` to prevent Cross-Site WebSocket Hijacking.
- Strict Localhost Binding, blocking unauthenticated LAN access.
- Token Authentication (256-bit Bearer Token) with `crypto.timingSafeEqual()`.
- Ed25519 Handshake logic for persistent keypair verification.
- Rate Limiting (per-IP token-bucket) to prevent DoS.
