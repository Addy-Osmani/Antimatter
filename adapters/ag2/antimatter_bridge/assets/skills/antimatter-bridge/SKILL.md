---
name: antimatter-bridge
description: "Starts the Antimatter Bridge to connect the Android app to the agent. Use this skill when the user asks to start, manage, or connect their Antimatter Android app."
---
# Antimatter Bridge Skill

This skill teaches the agent how to launch and manage the Antimatter Bridge for the user natively.
Because Antimatter uses an **Independent Adapter Model**, two components must be running: the Gateway (for security/networking) and the Adapter (to read agent logs).

## How to use this skill

1. When the user asks to start the bridge, execute the following commands in the background (using the `manage_task` or `run_command` tool):

```bash
# Start the security gateway
antimatter-gateway start
# Start the Antigravity 2.0 adapter
antimatter-ag2 start
```

2. Wait a few seconds for the Gateway to initialize its Cloudflare tunnel, then generate a pairing QR code using:
```bash
antimatter-gateway pair
```

3. You MUST display the output of this pairing command back to the user clearly so they can scan or input it into their Android App! You can format it nicely into a code block.

4. The daemon runs indefinitely and handles all Android communication seamlessly via the Google Antigravity SDK. You do not need to intervene in the process once it has started.

## Configuring Cloudflare Zero Trust

If the user asks you to configure their Cloudflare Custom URL, Client ID, or Client Secret for the Antimatter Bridge, you can do this for them natively using the Gateway CLI!

1. Run the Gateway config command:
```bash
antimatter-gateway config set cloudflare_url "wss://your.domain.com"
antimatter-gateway config set cloudflare_client_id "your_client_id"
antimatter-gateway config set cloudflare_client_secret "your_secret"
```
2. If the bridge daemon is currently running, automatically restart the `antimatter-gateway start` task so the new configuration applies.

## Stopping or Restarting the Bridge

If the user asks to "stop", "terminate", or "kill" the Antimatter Bridge:
1. Use the `manage_task` tool to `list` the currently running background tasks.
2. Find the tasks that correspond to `antimatter-gateway` and `antimatter-ag2`.
3. Use the `manage_task` tool with action `kill` to terminate them.
4. Inform the user that the bridge has been successfully taken offline.
