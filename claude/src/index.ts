#!/usr/bin/env node

import * as ws from 'ws';
import * as qrcode from 'qrcode-terminal';
import { handleClaudeMessage } from './router';
import { AuthHandler } from './auth';
import { CloudflareTunnel } from './tunnel';

const PORT = process.env.ANTIMATTER_PORT ? parseInt(process.env.ANTIMATTER_PORT) : 8081;

async function main() {
    console.log(`[Antimatter Bridge] Starting daemon on ws://localhost:${PORT}...`);
    const server = new ws.WebSocketServer({ port: PORT });
    
    const authHandler = new AuthHandler();
    const tunnel = new CloudflareTunnel(PORT);

    // Initialize Cloudflare Tunnel
    const url = await tunnel.start();
    if (url) {
        authHandler.cloudflareUrl = url;
    }

    // Print Pairing UI
    console.log("\n" + "=".repeat(50));
    console.log("ANTIMATTER BRIDGE SECURE PAIRING");
    console.log("=".repeat(50));
    
    if (url) {
        console.log(`\nCloudflare Tunnel: ${url}`);
        if (authHandler.cloudflareClientId) {
            console.log("Zero Trust Security: ENABLED");
        } else {
            console.log("Zero Trust Security: NOT CONFIGURED (Public Quick Tunnel)");
        }
    } else {
        console.log("\nConnection: Local Network (Cloudflare Quick Tunnel failed or not used)");
    }

    console.log("\nScan this QR Code with the Antimatter Android App:\n");
    qrcode.generate(authHandler.qrPayload, { small: true });

    console.log("\n" + "=".repeat(50));
    console.log("MANUAL PAIRING TOKENS");
    console.log("=".repeat(50));
    console.log(`Pairing Token: ${authHandler.pairingToken}`);
    console.log(`Public Key (Base64): ${Buffer.from(authHandler.publicKey).toString('base64')}`);

    let globalStepIndex = 0;

    server.on('connection', (websocket, request) => {
        let authenticated = false;
        
        console.log('[Antimatter Bridge] Mobile client connected via Zero Trust Tunnel.');

        websocket.on('message', async (data) => {
            try {
                const messageStr = data.toString();
                const payload = JSON.parse(messageStr);

                if (payload.type === 'AUTH_CHALLENGE') {
                    if (!payload.challenge) {
                        websocket.send(JSON.stringify({ type: 'ERROR', message: 'Missing challenge payload' }));
                        return;
                    }
                    const signature = authHandler.signChallenge(payload.challenge);
                    websocket.send(JSON.stringify({
                        type: 'AUTH_RESPONSE',
                        signature: signature
                    }));
                    authenticated = true;
                    console.log("[Antimatter Bridge] Client authenticated successfully.");
                    
                    // Send initial state
                    websocket.send(JSON.stringify({
                        type: 'SESSION_STATE',
                        conversationId: "default",
                        model: "claude-3-5-sonnet",
                        stepCount: globalStepIndex,
                        cloudflareUrl: authHandler.cloudflareUrl,
                        environment: "2.0"
                    }));
                    return;
                }

                if (!authenticated) {
                    console.warn("[Antimatter Bridge] Unauthorized message received before authentication.");
                    websocket.send(JSON.stringify({ type: 'ERROR', message: 'Unauthorized' }));
                    return;
                }

                // Handle valid authenticated payloads
                if (payload.type === 'SEND_MESSAGE' || payload.type === 'EXECUTE_COMMAND') {
                    console.log(`[Antimatter Bridge] Received valid payload type: ${payload.type}`);
                    await handleClaudeMessage(payload, websocket, globalStepIndex);
                    globalStepIndex += 10; 
                } else if (payload.type === 'PING') {
                    websocket.send(JSON.stringify({ type: 'PONG' }));
                }

            } catch (error) {
                console.error('[Antimatter Bridge] Error parsing message:', error);
            }
        });

        websocket.on('close', () => {
            console.log('[Antimatter Bridge] Mobile client disconnected.');
        });
    });
}

main().catch(err => {
    console.error("Fatal Error:", err);
    process.exit(1);
});
