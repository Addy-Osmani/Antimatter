# Adapters

The Antimatter ecosystem is built on an **Independent Adapter Architecture**. The central Gateway (`antimatter-core`) handles all security, routing, cryptography, and Cloudflare tunneling.

Adapters are lightweight client scripts that connect locally to the Gateway (`ws://127.0.0.1:8765`) and translate specific AI Agent events into the standard Antimatter IPC protocol.

There are currently three officially supported adapters:

---

## :material-microsoft-visual-studio-code: Antigravity IDE (`ag`)

The `ag` adapter bridges the Gateway and the official Google Antigravity IDE (VS Code).

- **Source:** [`adapters/ag/`](https://github.com/saifmukhtar/antimatter/tree/main/adapters/ag)
- **Technology:** TypeScript, VS Code Extension (`.vsix`)

When the extension is activated, it instantiates a `GatewayClient` that connects to the IPC server, registering itself as `"name": "ag"`. It listens for `SEND_MESSAGE` payloads and translates them into native `vscode.commands.executeCommand` calls. It also parses `transcript.jsonl` to stream agent trajectory data back to the app.

---

## :material-language-python: Antigravity 2.0 (`ag2`)

The `ag2` adapter integrates the Gateway with the standalone Google Antigravity 2.0 application.

- **Source:** [`adapters/ag2/`](https://github.com/saifmukhtar/antimatter/tree/main/adapters/ag2)
- **Technology:** Python daemon

This lightweight Python daemon monitors the local `.system_generated/logs/transcript.jsonl` to detect AI agent activity. It acts as an IPC bridge, passing prompts from the Android app directly into the local SDK subprocess. Because it operates alongside the Antigravity 2.0 SDK, it provides an agent Skill allowing you to control the daemon via natural language (e.g., *"Start my Antimatter bridge"*).

---

## :material-robot-outline: Claude Code (`cc`)

The `cc` adapter is the integration layer between the Gateway and Anthropics' Claude Code CLI.

- **Source:** [`adapters/cc/`](https://github.com/saifmukhtar/antimatter/tree/main/adapters/cc)
- **Technology:** Node.js, `@anthropic-ai/claude-agent-sdk`

This adapter uses the official Claude Agent SDK to stream events and logs. When new tokens or function calls occur in Claude, it proxies those updates to the Gateway as `STEP` messages. When you use the Android app to send a prompt, the adapter injects the message directly into the running Claude session.
