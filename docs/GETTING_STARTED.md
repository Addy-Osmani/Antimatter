# Getting Started

Welcome to the Antimatter ecosystem! This guide covers everything you need to set up the system, explore its features, and troubleshoot any issues.

The system is split into two halves — the **Gateway / Adapter** on your desktop, and the **Android App** in your pocket — connected by a secure WebSocket channel.

---

## :material-clipboard-check: Prerequisites

| Requirement | Purpose |
|-------------|---------|
| **Python 3.11+** | Required by the `antimatter-core` Gateway |
| **Node.js** | 22+ (Required if using `ag` or `cc` adapters) |
| **Android device** | Android 8.0+ |

---

## :material-numeric-1-circle: Install the Gateway (`antimatter-core`)

The Antimatter Gateway is the secure router that runs in the background. It manages Cloudflare tunnels and Ed25519 pairing.

1. Install the core gateway from PyPI using `uv` or `pip`:
   ```bash
   uv tool install antimatter-core
   ```
2. Start the Gateway in the background:
   ```bash
   antimatter-gateway start
   ```

*The Gateway will automatically spawn a local IPC server on `ws://127.0.0.1:8765`.*

---

## :material-numeric-2-circle: Install an Adapter

The Gateway alone doesn't do anything without an adapter. You must install the specific adapter for the AI Agent you are using.

### Antigravity IDE (`ag`)
A VS Code / Antigravity Extension.
1. Download the `.vsix` from GitHub releases.
2. Install via VS Code Extensions panel (Install from VSIX).
3. The extension will automatically connect to your running Gateway.

### Antigravity 2.0 (`ag2`)
A standalone Python daemon.
```bash
uv tool install antimatter-ag2
antimatter-ag2 start
```

### Claude Code (`cc`)
A Node.js daemon for the Anthropic CLI.
```bash
npm i -g antimatter-cc
antimatter-cc start
```

---

## :material-numeric-3-circle: Install the Android App

The companion app connects to the Gateway and gives you full control of the agent.

=== ":material-google-play: GitHub Releases"
    1. Go to [**GitHub Releases**](https://github.com/saifmukhtar/antimatter/releases).
    2. Download the latest **`.apk`** file.
    3. On your Android device, open the APK to install.

=== ":material-store: F-Droid"
    Antimatter is available on [**F-Droid**](https://f-droid.org/packages/dev.saifmukhtar.antimatter/) for 100% FOSS compliance:
    1. Search for **"Antimatter"** and install.

---

## :material-numeric-4-circle: Pair Your Phone

1. With the Gateway running, execute the pairing command:
   ```bash
   antimatter-gateway pair
   ```
2. A QR code will be generated in your terminal.
3. Open the **Antimatter app** on your phone.
4. Tap the **QR Scanner** button and scan the code.

The app will securely perform an Ed25519 handshake with the Gateway. Once connected, you can select which active adapter you want to interact with!

---

## :material-star-shooting: Features Overview

### :material-message-text: AI Chat Interface
- **Seamless Prompting** — type a message and it's injected directly into the active agent.
- **Rich Markdown Rendering** — AI responses render with full Markdown support.
- **Edit Decisions** — when the agent proposes file edits, accept or reject them directly from the chat UI.
- **Thinking Indicator** — a live typing/thinking animation shows when the agent is generating.

### :material-folder-multiple: Workspace Explorer
- **Live File Tree** — browse the files in your workspace in real-time.
- **File Viewing & Writing** — tap any file to read its contents or write quick edits on the go.

### :material-shield-lock: Network Security
- **Cloudflare Zero Trust** — automatic quick tunnels or persistent enterprise tunnels.
- **Ed25519 Verification** — the app verifies the bridge's identity via a cryptographic handshake after connecting.

---

## :material-lifebuoy: Troubleshooting & FAQ

Most connection failures map to a specific [WebSocket close code](PROTOCOL.md#websocket-close-codes).

### :material-close-circle: Phone can't connect

??? failure "Invalid token (close `4001`)"
    The pairing token on the phone doesn't match the bridge. **Fix:** re-pair by running `antimatter-gateway pair` and scanning again.

??? failure "Rate limited (close `4000`)"
    After 5 failed token attempts, your IP is banned for 60 seconds. **Fix:** wait a minute, then re-scan the QR code.

??? failure "Forbidden origin (HTTP 403)"
    **Fix:** make sure you're connecting through the Cloudflare tunnel URL, not directly to a raw IP/port.

### :material-server-off: "Gateway not running"

1. Run `antimatter-gateway start`.
2. Check if port `8765` is already in use (`lsof -i :8765`).

### :material-eye-off: No agent activity shows up

Make sure:
- The adapter for your agent (e.g. `antimatter-ag2`) is running.
- An agent conversation is **actually running** in the IDE/CLI.

### :material-frequently-asked-questions: FAQ

??? question "Is Antimatter affiliated with Google?"
    No. Antimatter is an unofficial, community-driven, open-source project and is **not** affiliated with Google or the official AntiGravity IDE project.

??? question "Do I need a domain?"
    No. TryCloudflare works with no domain and zero configuration. A domain (Cloudflare Zero Trust) is recommended for the strongest, double-layered security. See the [Security Policy](SECURITY.md).

??? question "Where are my pairing token and keys stored?"
    They are stored securely in your OS keychain. The 32-byte pairing token and the persistent Ed25519 keypair survive restarts and are **never** written to plain settings files.

??? question "What data does Antimatter collect?"
    None. Antimatter is fully local + tunnel. No telemetry, no analytics, no cloud services beyond the Cloudflare tunnel. All data stays between your machine and your phone.
