# Antigravity 2.0 Integration

Antimatter now supports direct, native integration with the standalone **Google Antigravity 2.0** application (which operates entirely via the Google Antigravity SDK). 

Unlike the classic IDE integration (which uses a VS Code extension), the Antigravity 2.0 integration is a native **Antigravity Plugin**. It connects directly to the core SDK agent process and streams messages locally via WebSockets.

## Key Differences from IDE

- **No VS Code Needed:** You don't need VS Code or the IDE extension. The bridge runs as a background process natively managed by the Agent.
- **Terminal Isolation:** Terminal remote execution is gracefully degraded (disabled) in Antigravity 2.0 to ensure strict boundary safety, as there is no standard IDE shell to interact with.
- **Background Execution:** The bridge runs completely headlessly. 
- **Cloudflare Quick Tunnels:** Just like the VS Code extension, the native daemon will automatically run the `cloudflared` CLI in the background to spawn a secure public `wss://*.trycloudflare.com` tunnel. Local network fallbacks are never used unless explicitly forced.

## Installation & Setup

1. **Install the Package:** Open your terminal and run:
   ```bash
   pip install antimatter-bridge
   ```
2. **Initialize the Plugin:** Run the CLI setup script. This will automatically inject the required `plugin.json` and `SKILL.md` files into your Antigravity plugins directory:
   ```bash
   antimatter init
   ```
3. **Start the Bridge:** 
   Open your Antigravity 2.0 application chat and type:
   > *"Start my Antimatter bridge"*
4. **Pair Device:**
   The agent will use its new skill to automatically spin up the background daemon and output your **Pairing Token** and **Public Key**. Copy these into your Antimatter Android App to connect instantly!

## Python Dependencies

If you are running the daemon manually or developing, ensure you have Python 3.11+ installed. The dependencies are managed automatically via the pip package. You can ensure all required libraries are installed by running:

```bash
pip install antimatter-bridge[all]
```

## Interacting with the Agent

Because Antimatter is a native plugin, you can control the entire bridge simply by talking to your AI Agent! Here are some example prompts you can use:

**Starting & Stopping**
- *"Start my Antimatter bridge"*
- *"Stop the Antimatter bridge"*
- *"Terminate the background bridge process"*
- *"Restart the Android bridge"*

**Configuration via Agent**
- *"Configure my Antimatter bridge to use cloudflare URL wss://my-tunnel.com"*
- *"Set my Antimatter Client ID to abc and my Client Secret to xyz"*
- *"I changed my Cloudflare settings. Please update my antimatter configuration and restart the bridge."*

**Manual Configuration**
Alternatively, you can manually edit the configuration file located at `~/.antimatter_daemon/config.json`.
An example of the structure with dummy secrets is automatically generated for you at `~/.antimatter_daemon/config.json`:
```json
{
    "gemini_api_key": "AIzaSyDummyKey_YOUR_GEMINI_API_KEY_HERE",
    "cloudflare_url": "wss://my-tunnel.yourdomain.com",
    "cloudflare_client_id": "dummy_client_id.access",
    "cloudflare_client_secret": "dummy_client_secret_xyz123",
    "pairing_token": "YOUR_32_BYTE_URLSAFE_BASE64_TOKEN_OR_LEAVE_BLANK_TO_AUTOGENERATE",
    "private_key_pem": "-----BEGIN PRIVATE KEY-----\nYOUR_PEM_KEY_OR_LEAVE_BLANK_TO_AUTOGENERATE\n-----END PRIVATE KEY-----\n"
}
```
*Note: If `pairing_token` or `private_key_pem` are omitted, the daemon will automatically generate secure cryptographic credentials on its next startup.*

## Technical Architecture

The plugin leverages the official `google-antigravity` Python SDK and connects directly to the agent's pre/post turn lifecycle hooks.

The bridge consists of:
1. `plugin.json` — Registration metadata.
2. `SKILL.md` — Natural language instructions teaching the Agent how to start the background service.
3. `server.py` & `agent_bridge.py` — A standalone `asyncio` WebSocket server running the exact Ed25519 cryptographic handshake protocol matching the Android app's original expectations.
