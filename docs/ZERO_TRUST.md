# Zero Trust Security Guide

Antimatter securely connects your mobile device to your local machine without opening any firewall ports or exposing your local IP address. It achieves this using **Cloudflare Zero Trust** (via `cloudflared` Quick Tunnels).

## How the Tunnel Works

1. When the VS Code extension starts, it spawns a local WebSocket server on a random open port (e.g., `8765`).
2. It then automatically downloads (if missing) and launches the `cloudflared` binary in the background.
3. `cloudflared` creates an outbound connection to Cloudflare's edge network, assigning you a temporary, secure URL (e.g., `wss://antimatter-extension.saifmukhtar.dev`).
4. Your Android app connects to this Cloudflare URL, and Cloudflare securely routes the traffic back down the tunnel to your local machine.

## Double-Layered Protection (Bearer Token + Ed25519)

To ensure that no one else can connect to your tunnel (even if they guess or find your TryCloudflare URL), Antimatter implements strict cryptographic authentication.

1. The extension generates a cryptographically secure, random 32-character token on startup.
2. The Android app MUST provide this token in the initial HTTP Upgrade request (`?token=YOUR_TOKEN`).
3. If the token is missing or incorrect, the extension immediately drops the connection and returns an HTTP 401 Unauthorized status.

## Manual Tunnel Configuration

While the extension handles tunneling automatically, you can also manually configure a persistent Cloudflare Zero Trust tunnel if you prefer a static domain name (rather than a randomized Quick Tunnel URL).

### 1. Install Cloudflared
Download the `cloudflared` daemon from the official [Cloudflare repository](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/downloads/).

### 2. Login
Authenticate with your Cloudflare account:
```bash
cloudflared tunnel login
```

### 3. Create a Tunnel
```bash
cloudflared tunnel create antimatter
```

### 4. Route Traffic
Route your custom domain (e.g., `antimatter.yourdomain.com`) to the tunnel:
```bash
cloudflared tunnel route dns antimatter antimatter.yourdomain.com
```

### 5. Add Cloudflare Access (Enterprise Security)
For the ultimate security setup, protect your tunnel route using a **Cloudflare Access Application**. 
1. In your Cloudflare Zero Trust dashboard, create an Access App for `antimatter.yourdomain.com`.
2. Generate a **Service Auth Client ID and Client Secret**.
3. In the Antimatter Android app, tap **Advanced Options** on the Connect screen and input these credentials.

This creates **Double-Layered Protection**:
- **Layer 1 (The Edge):** Attackers and bots are blocked at the Cloudflare Edge network because they lack the Service Auth headers.
- **Layer 2 (Local):** Even if an attacker bypasses the Edge, the local VS Code extension will reject them because they lack the 256-bit Antimatter Pairing Token and cannot complete the Ed25519 handshake.

### 6. Configure the Extension
Open the VS Code settings (Command Palette -> `Preferences: Open User Settings`) and search for `Antimatter`. Enter your custom Cloudflare URL in the **Cloudflare Hostname** setting. If you set up Cloudflare Access, also enter your Client ID here so the extension can automatically generate a QR code with the credentials embedded.

The extension will now bypass the automatic TryCloudflare tunnel and use your static, persistent enterprise tunnel instead.
