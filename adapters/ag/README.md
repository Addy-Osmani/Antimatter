# Antimatter: Antigravity IDE Adapter (AG)

This directory contains the Antimatter IPC adapter for the Antigravity IDE (VS Code).

## Architecture

This adapter follows the **Independent Adapter Model**. It does NOT contain any complex networking, Cloudflare tunnels, or cryptographic pairing logic. Instead, it acts purely as a "dumb" IPC client that connects to the central Antimatter Gateway.

1. **Connection**: The extension connects locally to the Gateway via WebSocket at `ws://127.0.0.1:8765`.
2. **Registration**: Upon connection, it sends `{"type": "REGISTER_ADAPTER", "name": "ag"}`.
3. **Execution**: When you send a message from the Antimatter Android app targeting the IDE, the Gateway securely routes that payload to this extension. The extension executes the command inside the IDE workspace and returns the result to the Gateway.

## Building

```bash
npm install
npm run build
```

For full system documentation, please see the `docs/` folder in the repository root.
