# Security & Zero Trust

Operating a remote-control interface for an IDE demands enterprise-grade security. The Antimatter project takes the security of your host development environment **extremely seriously**.

Antimatter securely connects your mobile device to your local machine **without opening any firewall ports** or exposing your local IP address. It achieves this using [**Cloudflare Zero Trust**](https://developers.cloudflare.com/cloudflare-one/) (via `cloudflared`).

!!! danger "Reporting a vulnerability"
    If you discover a security vulnerability, please do **NOT** open a public issue. Instead, use [GitHub's private vulnerability reporting](https://github.com/saifmukhtar/antimatter/security/advisories/new) or email the maintainers directly.

---

## :material-shield-check: Defense-in-Depth Mechanisms

Because Antimatter exposes a local WebSocket server, we implement **multiple overlapping security layers** so that compromising any single layer is not sufficient for an attacker to gain access.

### :material-numeric-1-circle: 256-bit Bearer Token + Ed25519 Handshake

<div class="step-card" markdown>

**Token generation:** On first run, the **Gateway** generates a 256-bit Bearer Token with os-level entropy. It's stored securely in the OS keychain (Keychain on macOS, Credential Manager on Windows, `libsecret` on Linux) and **persists across restarts**.

**Token verification:** Every WebSocket connection must present this token. The Gateway checks it with timing-safe comparison — immune to timing side-channel attacks. Invalid tokens → close code `4001 Unauthorized`.

**Ed25519 handshake:** After the token check, the Android client sends an `AUTH_CHALLENGE` nonce. The Gateway signs it with its persistent Ed25519 private key and returns `AUTH_RESPONSE`. The client verifies the signature against the public key received during QR pairing — this proves the Gateway's identity and prevents Man-in-the-Middle attacks.

*(Note: Adapters NEVER possess these cryptographic keys or tokens. They are isolated from the security boundary).*

</div>

### :material-numeric-2-circle: Origin Header Validation (CSWSH Protection)

<div class="step-card" markdown>

To protect against **Cross-Site WebSocket Hijacking (CSWSH)**, the Gateway enforces strict `Origin` header validation. Only these origins are accepted:

- `vscode-webview://…` (the extension's own webview)
- `https://<team>.cloudflareaccess.com` (Cloudflare Access)

Malicious websites in your browser **cannot** silently connect to the local server.

</div>

### :material-numeric-3-circle: Gateway vs Adapter Sandboxing

<div class="step-card" markdown>

The system strictly divides network ingress from local execution:

- **Gateway Layer:** Connects to the internet via Cloudflare. It holds the secrets, terminates the TLS, and performs cryptographic authentication. It CANNOT execute arbitrary code or read files.
- **Adapter Layer:** Runs locally (e.g., inside VS Code or the Python daemon). It can read files and execute code, but it has ZERO exposure to the internet. It only accepts IPC commands from the locally bound `127.0.0.1:8765` Gateway.

This creates a massive security airgap. If an attacker breaches the Cloudflare tunnel, they must still defeat the 256-bit cryptographic handshake. Even if they defeat that, the Gateway only forwards structured JSON IPC payloads to the adapter, preventing RCE.

</div>

### :material-numeric-4-circle: Payload Size Limits (DoS Mitigation)

<div class="step-card" markdown>

To protect against memory exhaustion attacks and Denial of Service (DoS), the WebSocket router strictly limits the size of incoming payloads. 

- **5MB Limit:** Any payload exceeding 5MB is immediately dropped.
- The server responds with a `4000` close code or a standard error payload if the payload is grossly oversized.

</div>

---

## :material-tunnel: How the Tunnel Works

```text
┌──────────────┐      outbound      ┌──────────────────┐      WSS      ┌──────────────┐
│  Gateway     │ ──────────────────▶│  Cloudflare Edge │◀────────────── │  Android App │
│  :8765       │  cloudflared conn  │  (TLS termination│  Bearer token  │  (Client)    │
└──────┬───────┘                    │   + routing)     │  + Ed25519     │              │
       │                            └──────────────────┘                └──────────────┘
       ▼
┌──────────────┐
│  Adapters    │
│  (ag, cc)    │
└──────────────┘
```

1. The Gateway starts a WebSocket server on `127.0.0.1:8765`.
2. It downloads (if missing) and launches `cloudflared` in the background.
3. `cloudflared` creates an **outbound** connection to Cloudflare's edge — no inbound ports needed.
4. Cloudflare assigns a public URL.
5. The Android app connects to this URL; Cloudflare routes traffic back through the tunnel.

---

## :material-auto-fix: Automatic Quick Tunnel (TryCloudflare)

This is the default — **zero configuration required**.

When the Gateway starts, it spawns a `cloudflared` tunnel, parses the assigned URL, and embeds the URL + pairing token + public key into the QR code. TryCloudflare URLs are ephemeral and change on restart, requiring you to re-scan the QR code.

---

## :material-shield-lock: Manual Cloudflare Zero Trust Setup

For a **persistent, enterprise-grade** setup with your own domain, use Cloudflare Zero Trust.

### 1. Create a Tunnel
1. Install `cloudflared` on your machine.
2. Authenticate: `cloudflared tunnel login`
3. Create: `cloudflared tunnel create antimatter`
4. Route: `cloudflared tunnel route dns antimatter ide.yourdomain.com`

### 2. Configure Ingress (`~/.cloudflared/config.yml`)

```yaml
tunnel: <YOUR_TUNNEL_UUID>
credentials-file: ~/.cloudflared/<YOUR_TUNNEL_UUID>.json
ingress:
  - hostname: ide.yourdomain.com
    service: ws://localhost:8765
  - service: http_status:404
```

Run it: `cloudflared tunnel run antimatter`

### 3. Add Cloudflare Access (Enterprise Security)

1. In the [Cloudflare Zero Trust Dashboard](https://one.dash.cloudflare.com/), go to **Access → Applications → Add an application**.
2. Create a **Self-hosted** app matching `ide.yourdomain.com`.
3. Add an **Access Policy** (e.g. allow your email domain).
4. Generate a **Service Auth Client ID and Client Secret**.

### 4. Configure the Gateway & App

**On your desktop:**
```bash
antimatter-gateway config set cloudflare_url "wss://ide.yourdomain.com"
antimatter-gateway config set cloudflare_client_id "YOUR_ID"
antimatter-gateway config set cloudflare_client_secret "YOUR_SECRET"
```

**On the Android App:**
1. Tap **Advanced Options** on the Connect screen.
2. Enter your custom URL, Client ID, and Client Secret.
3. Tap **Connect**!
