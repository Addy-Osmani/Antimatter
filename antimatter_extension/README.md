# Antimatter Bridge

**Antimatter** is an ecosystem that bridges your live **Antigravity IDE** session to a companion mobile app. 

*Note: This is an unofficial, open-source tool and is not affiliated with Google or the official Antigravity IDE project.*

## What this Extension Does
This extension runs a local WebSocket server and file watcher. It tails the `transcript.jsonl` output of your active Antigravity agent and broadcasts the trajectory (thoughts, tool calls, and outputs) to your mobile device in real-time. It also injects prompt commands back into your agent via `manual_input.json`.

## Features
- **Real-Time Agent Mirroring**: See what your AI is doing on your phone.
- **Remote Control**: Send chat messages directly to your agent from your phone.
- **Diff Accept/Reject**: Review file edits directly from your phone and accept/reject them.
- **Secure Networking**: Supports local Wi-Fi, Localtunnel (for quick public access), and Cloudflare Zero Trust (for secure, authenticated routing).

## How to Connect
1. Install the **Antimatter App** on your Android device.
2. Install this extension in your Antigravity VS Code environment.
3. Open the VS Code Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`) and type: `Antimatter: Start Bridge Server`.
4. The extension will automatically open a tunnel and broadcast the secure `wss://` URL to your phone (if on the same Wi-Fi) or you can manually enter the URL in the app's Connect Screen.

## Configuration Settings
You can customize the bridge by going to VS Code Settings and searching for `Antimatter`:
- `antimatter.port`: The local port the WebSocket server runs on (Default: `8765`).
- `antimatter.autoStart`: Whether to automatically start the bridge when VS Code launches (Default: `true`).
- `antimatter.useLocalTunnel`: Enable the free Localtunnel fallback if Cloudflare is not configured (Default: `true`).
- `antimatter.cloudflareHostname`: Your designated Cloudflare Zero Trust domain (e.g. `ide.yourdomain.com`). If provided, this overrides Localtunnel completely.

---
**Repository**: [github.com/saifmukhtar/antimatter](https://github.com/saifmukhtar/antimatter)
