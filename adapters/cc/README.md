# Antimatter: Claude Code Adapter (CC)

This directory contains the Antimatter IPC adapter for the Claude Desktop / Claude Code environment.

## Architecture

This adapter follows the **Independent Adapter Model**. It does NOT contain any complex networking, Cloudflare tunnels, or cryptographic pairing logic. Instead, it acts purely as a "dumb" IPC client that connects to the central Antimatter Gateway.

1. **Connection**: The Node.js application connects locally to the Gateway via WebSocket at `ws://127.0.0.1:8765`.
2. **Registration**: Upon connection, it sends `{"type": "REGISTER_ADAPTER", "name": "cc"}`.
3. **Execution**: When you send a message from the Antimatter Android app targeting Claude, the Gateway securely routes that payload to this adapter. This adapter interfaces with the Anthropic Claude SDK to stream events locally.

## Building

```bash
npm install
npm run build
```

For full system documentation, please see the `docs/` folder in the repository root.
