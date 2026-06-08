# Cloudflare Zero Trust Configuration Guide

Antimatter explicitly deprecates insecure local network exposure (like LocalTunnel or raw IP port-forwarding). Instead, it relies on **Cloudflare Zero Trust** to provide enterprise-grade security, TLS 1.3 encryption, and DDoS protection for your live WebSocket connection.

This guide covers two methods to set up your tunnel. If you don't own a domain name, simply leave your VS Code settings blank, and the extension will automatically generate a free `trycloudflare.com` tunnel for you. If you own a domain and want a persistent, branded URL, follow the instructions below.

---

## 1. The Beginner Guide (UI Dashboard)

If you are unfamiliar with command-line networking, Cloudflare provides a simple web dashboard to create your tunnel.

### Prerequisites
- A free Cloudflare account.
- A domain name (e.g., `saifmukhtar.dev`) connected to Cloudflare nameservers.

### Step-by-Step Setup
1. **Navigate to Zero Trust**: Log in to your Cloudflare dashboard and click on **Zero Trust** in the left sidebar.
2. **Access Tunnels**: Go to **Networks > Tunnels**.
3. **Create a Tunnel**: Click the **Create a tunnel** button. Select **Cloudflared** as your connector type. Name it something memorable, like `antimatter-bridge`.
4. **Install the Connector**: You will be presented with a command to install the `cloudflared` daemon on your host machine. Copy the command for your specific Operating System (Windows, macOS, or Linux) and run it in your terminal.
5. **Configure Public Hostname**: Once the tunnel connects, you must route traffic to it. Click on the **Public Hostname** tab.
    - **Subdomain**: Choose a subdomain (e.g., `ide`).
    - **Domain**: Select your domain from the dropdown.
    - **Service**: Set the Type to `HTTP` and the URL to `localhost:8765` (this is the default port Antimatter runs on).
6. **Save and Apply**: Click save. The tunnel is now active!
7. **VS Code Setup**: Open your VS Code settings, search for `Antimatter`, and enter your new hostname (e.g., `ide.saifmukhtar.dev`) into the `Cloudflare Hostname` field.

---

## 2. The Advanced Guide (CLI & Systemd)

For power users who prefer Infrastructure as Code, you can manage your tunnel entirely via the command line.

### Prerequisites
- Install the `cloudflared` CLI tool via your package manager (e.g., `brew install cloudflared`, `apt install cloudflared`).

### Step-by-Step Setup
1. **Login**: Authenticate your CLI with your Cloudflare account.
   ```bash
   cloudflared tunnel login
   ```
   This will open a browser window and download a `cert.pem` file to `~/.cloudflared/`.

2. **Create the Tunnel**:
   ```bash
   cloudflared tunnel create antimatter-bridge
   ```
   This generates a UUID for your tunnel and saves a `credentials.json` file.

3. **Configure Routing**: Create a `config.yml` file in `~/.cloudflared/`:
   ```yaml
   tunnel: <your-tunnel-uuid>
   credentials-file: /home/user/.cloudflared/<your-tunnel-uuid>.json

   ingress:
     - hostname: antimatter.yourdomain.com
       service: http://localhost:8765
     - service: http_status:404
   ```

4. **Route DNS**: Point your domain to the tunnel UUID.
   ```bash
   cloudflared tunnel route dns antimatter-bridge antimatter.yourdomain.com
   ```

5. **Run the Tunnel**:
   ```bash
   cloudflared tunnel run antimatter-bridge
   ```

### Troubleshooting: Error 1033 (QUIC UDP Timeouts)
By default, `cloudflared` attempts to connect to the Cloudflare Edge network using the QUIC protocol over UDP. Many corporate firewalls and ISPs aggressively block UDP traffic, resulting in continuous `1033 Argo Tunnel Errors` and `timeout: no recent network activity` logs.

**The Fix:** Force `cloudflared` to use HTTP2 (TCP) instead of QUIC.
Update your run command:
```bash
cloudflared tunnel --protocol http2 run antimatter-bridge
```
If you installed the tunnel as a system service, you must edit the systemd unit file (usually located at `/etc/systemd/system/cloudflared.service`) and append `--protocol http2` to the `ExecStart` directive.
