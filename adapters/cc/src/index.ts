#!/usr/bin/env node

import * as ws from 'ws';
import { handleClaudeMessage } from './router';

// IPC port for the Gateway
const GATEWAY_URL = 'ws://127.0.0.1:8765';

async function main() {
    console.log(`[Claude Adapter] Connecting to Gateway at ${GATEWAY_URL}...`);
    
    let globalStepIndex = 0;
    
    function connect() {
        const client = new ws.WebSocket(GATEWAY_URL);

        client.on('open', () => {
            console.log('[Claude Adapter] Connected to Gateway IPC.');
            // Register as the cc adapter
            client.send(JSON.stringify({
                type: 'REGISTER_ADAPTER',
                name: 'cc'
            }));
        });

        client.on('message', async (data) => {
            try {
                const messageStr = data.toString();
                const payload = JSON.parse(messageStr);

                if (payload.type === 'SEND_MESSAGE') {
                    console.log(`[Claude Adapter] Received SEND_MESSAGE from Gateway.`);
                    // We pass `client` as the websocket so `handleClaudeMessage` sends responses back to Gateway
                    await handleClaudeMessage(payload, client as any, globalStepIndex);
                    globalStepIndex += 10; 
                }

            } catch (error) {
                console.error('[Claude Adapter] Error parsing IPC message:', error);
            }
        });

        client.on('close', () => {
            console.log('[Claude Adapter] Disconnected from Gateway. Reconnecting in 3s...');
            setTimeout(connect, 3000);
        });

        client.on('error', (err) => {
            console.error('[Claude Adapter] IPC Connection Error:', err.message);
        });
    }

    connect();
}

main().catch(err => {
    console.error("Fatal Error:", err);
    process.exit(1);
});
