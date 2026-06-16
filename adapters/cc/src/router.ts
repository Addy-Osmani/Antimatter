import * as ws from 'ws';
// Ensure the hypothetical SDK exists in package.json or is a placeholder
import { query } from '@anthropic-ai/claude-agent-sdk';

export async function handleClaudeMessage(payload: any, websocket: ws.WebSocket, initialStepIndex: number) {
    let currentStepIndex = initialStepIndex;
    // Use the conversationId from the payload if present, otherwise generate a fallback
    const conversationId: string = payload.conversationId ?? `cc-${Date.now()}`;

    try {
        if (payload.type === 'SEND_MESSAGE') {
            // Notify the mobile client that generation has started
            websocket.send(JSON.stringify({
                type: 'GENERATING',
                conversationId,
            }));

            // Query the local Claude SDK instance
            const responseStream = query({ prompt: payload.text });

            for await (const rawEvent of responseStream) {
                const event = rawEvent as any;
                if (event.type === 'text' || event.type === 'assistant') {
                    websocket.send(JSON.stringify({
                        type: 'STEP',
                        index: currentStepIndex++,
                        step: {
                            case: 'text',
                            value: event.text
                        }
                    }));
                } else if (event.type === 'tool_use') {
                    websocket.send(JSON.stringify({
                        type: 'STEP',
                        index: currentStepIndex++,
                        step: {
                            case: 'toolCall',
                            value: {
                                name: event.name,
                                arguments: event.input
                            }
                        }
                    }));
                } else if (event.type === 'thought' || event.type === 'planner') {
                    websocket.send(JSON.stringify({
                        type: 'STEP',
                        index: currentStepIndex++,
                        step: {
                            case: 'plannerResponse',
                            value: event.content
                        }
                    }));
                }
            }

            // Notify the mobile client that generation is complete
            websocket.send(JSON.stringify({
                type: 'RESPONSE_COMPLETE',
                conversationId,
            }));
        }
    } catch (error) {
        console.error("[Claude SDK Error]:", error);
        // Use 'message' (not 'error') to match Android's InboundMessage.Error data class
        websocket.send(JSON.stringify({
            type: 'ERROR',
            message: String(error)
        }));
    }
}
