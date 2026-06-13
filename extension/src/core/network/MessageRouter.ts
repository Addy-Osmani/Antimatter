import { WebSocket } from 'ws';
import { InboundMessage } from './types';

export class MessageRouter {
  private handlers = new Map<string, (msg: any, ws: WebSocket) => Promise<void>>();

  register<T extends InboundMessage['type']>(
    type: T,
    handler: (msg: Extract<InboundMessage, { type: T }>, ws: WebSocket) => Promise<void>
  ) {
    this.handlers.set(type, handler as any);
  }

  async route(raw: string, ws: WebSocket) {
    if (Buffer.byteLength(raw, 'utf8') > 5 * 1024 * 1024) {
      console.error(`Payload too large. Dropping message.`);
      ws.send(JSON.stringify({ type: 'ERROR', message: 'Payload exceeds 5MB limit' }));
      return;
    }

    let msg: InboundMessage;
    try {
      msg = JSON.parse(raw);
    } catch {
      console.error(`Bad JSON from client: ${raw.slice(0, 100)}`);
      return;
    }

    const handler = this.handlers.get(msg.type);
    if (handler) {
      if (msg.id) {
        ws.send(JSON.stringify({ type: 'ACK', id: msg.id }));
      }
      try {
        await handler(msg, ws);
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : String(err);
        console.error(`Error handling ${msg.type}: ${message}`);
        ws.send(JSON.stringify({ type: 'ERROR', message }));
      }
    } else {
      console.warn(`No handler for message type: ${msg.type}`);
    }
  }
}
