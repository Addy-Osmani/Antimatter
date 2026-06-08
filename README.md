# Antimatter Ecosystem

[![F-Droid](https://img.shields.io/badge/F--Droid-Get_it_on-blue.svg)](https://f-droid.org/packages/dev.saifmukhtar.antimatter/)
[![GitHub Sponsor](https://img.shields.io/badge/Sponsor-❤️-pink.svg)](https://github.com/sponsors/saifmukhtar)
> [!WARNING]
> **Community Project Disclaimer**
> Antimatter is an unofficial, community-driven, open-source project. It is **NOT** an official product of Google, nor is it officially affiliated with the Google AntiGravity IDE project.

Antimatter is an open-source bridge ecosystem that connects your mobile device directly to the local **Google AntiGravity IDE** running on your host machine.

By connecting your phone to the IDE, you can view your active AI agent's trajectory, monitor its thought process, read logs in real-time, send new prompts, and browse your workspace files—all from your mobile device.

<p align="center">
  <img src="docs/images/first_chat.png" width="200" alt="First Chat Screen">
  <img src="docs/images/workspace.png" width="200" alt="Workspace Screen">
  <img src="docs/images/code_viewer.png" width="200" alt="Code Viewer">
</p>

---

## 📚 Documentation Index (Link Tree)

Because this repository contains multiple sub-projects, we have split the documentation for clarity:

- [**VS Code Extension README**](antimatter_extension/README.md) - Learn how the bridge works.
- [**Android App README**](antimatter_app/README.md) - Learn how the mobile client works.
- [**Architecture Deep Dive**](ARCHITECTURE.md) - Understand how we reverse-engineered the IDE hooks without official APIs.
- [**Zero Trust Guide**](ZERO_TRUST.md) - Learn how to set up Cloudflare Zero Trust (UI & CLI guides).
- [**Security Policy**](SECURITY.md) - Read about our Bearer Tokens, CSWSH protections, and Rate Limiting.
- [**Changelog**](CHANGELOG.md) - Detailed technical tracking of all project updates.
- [**Contributing Guidelines**](CONTRIBUTING.md) - How to run this project locally and submit PRs.
- [**Code of Conduct**](CODE_OF_CONDUCT.md) - Our community interaction guidelines.

---

## 🚀 Quick Start & Download

You do not need to compile the code yourself to use Antimatter. 

1. **Download the Extension**: Go to our [GitHub Releases](https://github.com/saifmukhtar/antimatter/releases) page and download the latest `.vsix` file.
2. **Install in AntiGravity**: Open your AntiGravity IDE, go to the Extensions panel, click the `...` menu, and select **"Install from VSIX..."**.
3. **Download the Android App**: Also on the [GitHub Releases](https://github.com/saifmukhtar/antimatter/releases) page, download the latest `.apk` file and install it on your Android device. We provide two flavors:
    - `foss` (Free and Open Source Software, F-Droid compliant)
    - `standard` (Includes Crashlytics for bug reporting)
4. **Connect**: Open the VS Code Command Palette (`Ctrl+Shift+P`) and type `Antimatter: Show Pairing QR Code`. Scan this code with the Android app to securely transfer the WebSocket URL and Bearer Token.

---

## 🛠️ Tech Stack & Credits

This project leverages several modern open-source technologies:
- **Android App**: Kotlin, Jetpack Compose, OkHttp (WebSockets), Markwon (Markdown rendering).
- **Barcode Scanning**: Pure-Java `com.google.zxing:core` ensuring 100% FOSS compliance for F-Droid.
- **VS Code Extension**: TypeScript, Node.js, `ws` (WebSocket server).
- **Secure Networking**: Cloudflare Zero Trust (`cloudflared`) and free automatic Quick Tunnels.

---

## 👥 Contributors

This project is built and maintained by the open-source community. See the [CONTRIBUTORS.md](CONTRIBUTORS.md) file for a list of all the amazing people who have helped build Antimatter!

## License
MIT License
