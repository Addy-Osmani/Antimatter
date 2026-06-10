# Welcome to Antimatter

Antimatter is an open-source bridge ecosystem that connects your mobile device directly to the local **Google AntiGravity IDE** running on your host machine.

By connecting your phone to the IDE, you can view your active AI agent's trajectory, monitor its thought process, read logs in real-time, send new prompts, and browse your workspace files—all from your mobile device.

---

## 🚀 Quick Start

You do not need to compile the code yourself to use Antimatter.

1. **Download the Extension**: Go to our [GitHub Releases](https://github.com/saifmukhtar/antimatter/releases) page and download the latest `.vsix` file.
2. **Install in AntiGravity**: Open your AntiGravity IDE, go to the Extensions panel, click the `...` menu, and select **"Install from VSIX..."**.
3. **Download the Android App**: Also on the [GitHub Releases](https://github.com/saifmukhtar/antimatter/releases) page, download the latest `.apk` file and install it on your Android device. 
4. **Setup the Tunnel**: Decide between TryCloudflare (quick) or Cloudflare Zero Trust (highly secure). See our [Zero Trust Guide](ZERO_TRUST.md) for detailed steps.
5. **Connect**: Open the VS Code Command Palette (`Ctrl+Shift+P`) and type `Antimatter: Show Pairing QR Code`. Scan this code with the Android app to securely transfer the WebSocket URL and Pairing Token.

!!! warning "Security Disclaimer"
    Antimatter requires exposing your local development environment to the internet via Cloudflare Tunnels. While we use 256-bit Pairing Tokens and Ed25519 cryptographic handshakes, you should always treat this with caution. Never share your Pairing Token or QR Code.

!!! note "Community Project"
    Antimatter is an unofficial, community-driven, open-source project. It is **NOT** an official product of Google, nor is it officially affiliated with the Google AntiGravity IDE project.

## Why Antimatter?

Developing with autonomous agents often requires leaving them running for long periods. Antimatter ensures you aren't chained to your desk. With features like **Remote Terminal Execution** and **Real-Time Streaming**, you can manage your IDE from anywhere in the world securely.

Check out the [Features](FEATURES.md) page for a full breakdown of what Antimatter can do!
