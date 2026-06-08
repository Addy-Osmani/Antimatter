# Antimatter Bridge (VS Code Extension)

<p align="center">
  <img src="https://raw.githubusercontent.com/saifmukhtar/antimatter/main/antimatter_extension/icon.png" width="128" alt="Antimatter Logo">
</p>

**Antimatter** is an ecosystem that bridges your live **Google AntiGravity IDE** session to a companion mobile app.

*Note: This is an unofficial, open-source tool and is not affiliated with Google or the official AntiGravity IDE project.*

## What this Extension Does
This extension runs a local WebSocket server and file watcher. It tails the `transcript.jsonl` output of your active AntiGravity agent and broadcasts the trajectory (thoughts, tool calls, and outputs) to your mobile device in real-time. It also injects prompt commands back into your agent via native VS Code API hooks (`vscode.commands.executeCommand`).

## Features
- **Real-Time Agent Mirroring**: See what your AI is doing on your phone.
- **Remote Control**: Send chat messages directly to your agent from your phone.
- **Diff Accept/Reject**: Review file edits directly from your phone and accept/reject them.
- **Enterprise Security**: All connections require a 256-bit Bearer Token. Strict Origin validation prevents Cross-Site WebSocket Hijacking (CSWSH).
- **Cloudflare Zero Trust**: Natively supports authenticated remote routing via `cloudflared`.

## How to Connect
1. Ensure your AntiGravity IDE host is running Node.js 22+.
2. Install this `.vsix` extension in your AntiGravity IDE.
3. Open the VS Code Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`) and type: `Antimatter: Show Pairing QR Code`.
3. Open the **Antimatter App** on your Android device and tap the QR Scanner.
4. Scan the code on your screen. The extension will automatically securely pair your phone using a newly generated Bearer Token.

## Configuration Settings
You can customize the bridge by going to VS Code Settings and searching for `Antimatter`:

| Setting ID | Description | Default |
|---|---|---|
| `antimatter.autoStart` | Automatically start the bridge server when Antigravity opens | `true` |
| `antimatter.port` | WebSocket port for the Antimatter bridge server | `8765` |
| `antimatter.useLocalTunnel` | Enable LocalTunnel for a persistent public URL | `false` |
| `antimatter.localtunnelSubdomain` | Custom subdomain for LocalTunnel (e.g. 'my-bridge' -> my-bridge.loca.lt) | `"saifs-antimatter"` |
| `antimatter.cloudflareHostname` | Designated Cloudflare Zero Trust hostname (e.g. ide.mydomain.com) | `""` |

*(Note: We have officially deprecated the insecure Localtunnel fallback to protect your host environment from MITM attacks).*

## Commands
You can trigger these via the Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`):

| Command ID | Title |
|---|---|
| `antimatter.showPairingQR` | **Antimatter: Show Pairing QR** (Use this to connect the app!) |
| `antimatter.setCloudflareCredentials` | Antimatter: Set Cloudflare Credentials (Securely saves Client ID and Secret in Keychain) |
| `antimatter.startBridge` | Antimatter: Start Bridge Server |
| `antimatter.stopBridge` | Antimatter: Stop Bridge Server |
| `antimatter.showStatus` | Antimatter: Show Connection Status |

---
**Repository**: [github.com/saifmukhtar/antimatter](https://github.com/saifmukhtar/antimatter)
