# Antimatter

> **Disclaimer**: Antimatter is an unofficial, community-driven, open-source project. It is **NOT** an official product of Google, nor is it officially affiliated with the Antigravity IDE project.

Antimatter is an open-source bridge ecosystem that connects your mobile device directly to the local **Antigravity IDE** running on your host machine. It consists of a VS Code Extension (the Bridge) and a modern Android App (the Client). 

By connecting your phone to the IDE, you can view your active AI agent's trajectory, monitor its thought process, read logs in real-time, send new prompts, and browse your workspace files—all from your mobile device.

---

## Architecture & How It Works (The Reverse Engineering Bypass)

Antigravity IDE currently lacks a public, official API for external third-party clients to natively hook into its core agent loop. To bypass this restriction, the Antimatter ecosystem relies on cleanly reverse-engineering the IDE's file system footprint.

Here is exactly how the bridge operates under the hood:

### 1. Real-time Log Tailing (`transcript.jsonl`)
When an Antigravity agent operates, it continuously writes its state, internal reasoning steps, tool calls, and final outputs sequentially into a JSONL (JSON Lines) transcript file. 
- **Path**: `~/.gemini/antigravity-ide/brain/<conversation-id>/.system_generated/logs/transcript.jsonl`
- **Mechanism**: The Antimatter VS Code Extension continuously monitors and "tails" this file using Node.js filesystem watchers (similar to the UNIX `tail -f` command). It buffers incoming chunks, parses the raw JSON lines into structured objects, and broadcasts these trajectory steps over a local WebSocket server.

### 2. Prompt Injection (`manual_input.json`)
To allow the mobile app to send instructions *back* to the agent, we exploit the IDE's interruption mechanism.
- **Mechanism**: The Antigravity core engine natively watches a specific configuration file for user interruptions or feedback. The Antimatter Extension takes your Android chat message and writes it directly into a `manual_input.json` file on the host machine. The core agent loop detects this file modification, suspends its current task, reads your prompt, and executes it immediately as if you had typed it into the IDE's native chat box.

### 3. File Edits & Command API Overrides
For actions like accepting diffs, the extension taps directly into the exposed VS Code command palette APIs (e.g. `vscode.commands.executeCommand('antigravity.prioritized.agentAcceptAllInFile')`) triggering the internal Antigravity functions safely without needing direct engine access.

---

## Production Setup: Cloudflare Zero Trust Integration

While Antimatter defaults to using Localtunnel for easy public access, we highly recommend using **Cloudflare Zero Trust (Cloudflared)** for a robust, production-ready environment. This ensures your local IDE WebSocket is protected by Cloudflare Access (OAuth) and prevents unauthorized individuals from accessing your machine.

### Step 1: Install Cloudflared
On the machine running your Antigravity IDE (the Host), install the `cloudflared` CLI.
- **Mac (Homebrew)**: `brew install cloudflare/cloudflare/cloudflared`
- **Linux (Debian/Ubuntu)**: 
  ```bash
  curl -L --output cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
  sudo dpkg -i cloudflared.deb
  ```

### Step 2: Authenticate
Login to your Cloudflare account to obtain a certificate.
```bash
cloudflared tunnel login
```
A browser window will open. Select the domain you want to use (e.g., `yourdomain.com`).

### Step 3: Create the Tunnel
Create a named tunnel for Antimatter.
```bash
cloudflared tunnel create antimatter-bridge
```
You will receive a Tunnel UUID. Take note of it.

### Step 4: Route DNS
Route a designated subdomain to this tunnel. For example, `ide.yourdomain.com`.
```bash
cloudflared tunnel route dns antimatter-bridge ide.yourdomain.com
```

### Step 5: Configure the Ingress Rules
Create a `config.yml` file in your `.cloudflared` directory (`~/.cloudflared/config.yml`):
```yaml
tunnel: <YOUR-TUNNEL-UUID>
credentials-file: /home/youruser/.cloudflared/<YOUR-TUNNEL-UUID>.json

ingress:
  - hostname: ide.yourdomain.com
    service: http://localhost:8765
  - service: http_status:404
```

### Step 6: Run as a Service
When installing the service, `cloudflared` expects the configuration to be in `/etc/cloudflared`. Copy your files over first:
```bash
sudo mkdir -p /etc/cloudflared
sudo cp ~/.cloudflared/config.yml /etc/cloudflared/
sudo cp ~/.cloudflared/*.json /etc/cloudflared/

sudo cloudflared service install
sudo systemctl start cloudflared
sudo systemctl enable cloudflared
```

### Step 7: Configure the Antimatter Extension
1. Open Antigravity IDE (VS Code).
2. Go to **Settings** (`Ctrl+,`) and search for `Antimatter`.
3. Locate **Antimatter: Cloudflare Hostname** (`antimatter.cloudflareHostname`).
4. Enter your designated domain: `ide.yourdomain.com`.

**Result**: The extension will automatically bypass Localtunnel and broadcast your secure `wss://ide.yourdomain.com` URL to the mobile app!

### Step 8: Add OAuth (Cloudflare Access)
To ensure only YOU can connect to the mobile app:
1. Go to your **Cloudflare Zero Trust Dashboard**.
2. Navigate to **Access > Applications** and click **Add an Application**.
3. Select **Self-Hosted**.
4. Enter your Application Name and the Subdomain (`ide.yourdomain.com`).
5. Create an **Access Policy** that explicitly allows only your personal email address (via Google or GitHub OAuth).
6. Now, when the Android app connects, it will require your Google/GitHub authentication!

---

## VS Code Marketplace Publishing Guide

If you wish to fork this project and publish the extension automatically to the Visual Studio Marketplace, follow these steps:

### Prerequisites
1. Install the VSCE CLI tool globally: `npm install -g @vscode/vsce`.
2. Ensure you have a **Microsoft Account**.

### 1. Create a Publisher ID
1. Navigate to the [VS Code Marketplace Publisher Management](https://marketplace.visualstudio.com/manage) portal.
2. Sign in and create a new **Publisher**. Remember your Publisher ID.
3. Open `antimatter_extension/package.json` and change the `"publisher": "dev-saifmukhtar-antimatter"` field to your new Publisher ID.

### 2. Generate a Personal Access Token (PAT)
1. Go to [Azure DevOps](https://dev.azure.com/).
2. Create an organization if you don't have one.
3. From the user settings menu (top right), select **Personal Access Tokens**.
4. Click **New Token**.
5. Name it "VSCE Publish".
6. Set **Organization** to `All accessible organizations`.
7. Expand **Show all scopes**. Find **Marketplace** and check **Acquire** and **Manage**.
8. Create the token and copy it securely.

### 3. Login to VSCE
In your terminal, authenticate the CLI with your publisher:
```bash
vsce login <YOUR_PUBLISHER_ID>
```
When prompted, paste your Personal Access Token.

### 4. Publish
Navigate to the extension directory and run the publish command:
```bash
cd antimatter_extension
npm install
npm run build
vsce publish
```
Within a few minutes, your extension will be live on the Marketplace! Users will now receive **automatic updates** directly within Antigravity IDE whenever you publish a new version.

---

## Repository Structure

- `antimatter_app/`: The Android application written in Kotlin and Jetpack Compose.
- `antimatter_extension/`: The VS Code Extension written in TypeScript.
- `CODE_OF_CONDUCT.md`: Our community interaction guidelines.

## License
MIT License
