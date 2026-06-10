# Antimatter IDE Features

Welcome to the detailed breakdown of the features included in the Antimatter IDE. The ecosystem is split into two halves: the VS Code Extension (which acts as the bridge server) and the Android App (which acts as the mobile client). 

Together, they allow you to remotely interact with your code, chat with the AI, and run terminal commands—all secured by military-grade cryptography.

---

## 📱 Android App Features

### 💬 AI Chat Interface
- **Seamless Prompting:** Talk directly to the Antigravity agent running on your computer.
- **Partial Text Selection:** Long-press on any AI response or user message to select specific lines of text, bringing up the native Android copy/share toolbar.
- **Markdown Rendering:** AI responses are fully rendered with Markdown, including bolding, italics, links, and code blocks.
- **Message Edit Workflow:** Approve or reject code edits proposed by the AI directly from the chat UI.
- **Continuous Scrolling:** Scroll history is maintained effortlessly as long-running queries process.

### 📂 Workspace Explorer
- **Live File Tree:** Browse the files currently open or present in your VS Code workspace.
- **File Viewing:** Open specific files to read their contents remotely.
- **Real-Time Sync:** File trees sync via the WebSocket connection so you always see what's on your PC.

### 💻 Remote Terminal
- **Secure Access:** Tapping the terminal icon prompts a Biometric (Fingerprint/Face) lock. The terminal will *only* open if you are the physical owner of the device.
- **Live Command Proxy:** Execute commands directly onto your desktop's shell. 
- **Real-Time Output:** The terminal streams `stdout` and `stderr` in real-time, functioning exactly as if you were typing on your desktop.
- **Unrestricted Power:** By design, the terminal runs with the same permissions as your VS Code instance, giving you full control over git, npm, and system configs.

### 🛡️ Network Security
- **Cloudflare Zero Trust Integration:** Easily input your Cloudflare Client ID and Client Secret for enterprise-grade Zero Trust access.
- **QR Code Pairing:** Connect instantly by scanning the QR code from VS Code—no typing required.

---

## 🧩 VS Code Extension Features

### 🔌 Background Bridge Server
- **Silent Operation:** Runs an invisible WebSocket server binding to a local port (default `8080`) to stream telemetry, files, and chat data to the mobile client.
- **Auto-Start:** Automatically initializes when you open the Antigravity IDE workspace.

### 🔐 Cryptographic Authentication
- **Pairing Token Validation:** On the first run, the extension generates a 256-bit cryptographic "Pairing Token". The server rejects *any* connection that does not present this token, making local network snooping mathematically impossible.
- **Ed25519 Handshake:** Uses public-key cryptography to sign nonces, proving the server's identity to the mobile app and preventing Man-in-the-Middle (MITM) attacks.

### 📡 Cloudflare Tunnel Management
- **One-Click Tunnel Restart:** Easily restart your Cloudflare tunnel bindings directly from the VS Code command palette.
- **QR Code Generation:** Embeds the Pairing Token and connection URLs into a highly scannable QR code right inside your IDE.

---

## 🚀 Setup Guide

To get the most out of Antimatter, you need to expose your local VS Code extension to your phone over the internet. We recommend two approaches depending on your domain ownership:

### Method 1: TryCloudflare (No Domain Required)
If you don't own a domain, you can use Cloudflare's free temporary tunnels.
1. Run `cloudflared tunnel --url localhost:8080` on your desktop.
2. Cloudflare will give you a temporary URL (e.g., `wss://funny-words.trycloudflare.com`).
3. Enter this URL in the Antimatter Android app, or paste it into the VS Code extension settings to generate a QR code.
> **Security Note on TryCloudflare:** Because the URL is public, anyone who guesses the URL can attempt to connect. However, because Antimatter uses a 256-bit **Pairing Token**, unauthorized users will still be instantly rejected.

### Method 2: Cloudflare Zero Trust (Domain Required - Recommended)
If you own a domain, this is the most secure and robust setup.
1. Setup a Cloudflare Zero Trust tunnel pointing `ide.yourdomain.com` to `localhost:8080`.
2. Protect the route using a Cloudflare Access Application, and generate a **Service Auth Client ID and Secret**.
3. In the Android App, go to **Advanced Options** on the Connect screen.
4. Enter your custom `wss://ide.yourdomain.com` URL along with your Client ID and Client Secret.
> **Security Note:** This setup provides double-layered protection. Attackers are blocked at the Cloudflare edge (Zero Trust), and even if they bypass that, they are blocked by the Antimatter Pairing Token locally.
