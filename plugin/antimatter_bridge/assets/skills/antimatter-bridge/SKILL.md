---
name: antimatter-bridge
description: "Starts the Antimatter Bridge to connect the Android app to the agent. Use this skill when the user asks to start, manage, or connect their Antimatter Android app."
---
# Antimatter Bridge Skill

This skill teaches the agent how to launch and manage the Antimatter Bridge for the user natively.

## How to use this skill

1. When the user asks to start the bridge, execute the following command in the background (using the `manage_task` or `run_command` tool):

```bash
antimatter start
```

2. Monitor the output of the background task. By default, the daemon will automatically spawn a **Cloudflare Quick Tunnel** (TryCloudflare) to secure the connection across the internet. When the server finishes starting the tunnel, it will print out a **Pairing Token (Manual)** and a **Public Key (Base64)**.

3. You MUST display this information back to the user clearly so they can scan or input it into their Android App! You can format it nicely into a code block.

4. The daemon runs indefinitely and handles all Android communication seamlessly via the Google Antigravity SDK. You do not need to intervene in the process once it has started.

## Configuring Cloudflare Zero Trust

If the user asks you to configure their Cloudflare Custom URL, Client ID, or Client Secret for the Antimatter Bridge, you can do this for them natively!

1. Create or read the JSON configuration file located at `~/.antimatter_daemon/config.json`.
2. Update the `cloudflare_url`, `cloudflare_client_id`, or `cloudflare_client_secret` fields in the JSON file.
3. Save the file.
4. If the bridge daemon is currently running, automatically restart it (stop the task and start a new one) so the new QR code generates.

## Stopping or Restarting the Bridge

If the user asks to "stop", "terminate", or "kill" the Antimatter Bridge:
1. Use the `manage_task` tool to `list` the currently running background tasks.
2. Find the task that corresponds to the `server.py` command.
3. Use the `manage_task` tool with action `kill` to terminate it.
4. Inform the user that the bridge has been successfully taken offline.

If the user asks to "restart" the bridge:
1. Kill the existing task using the instructions above.
2. Start the bridge again using the command provided in section 1.
