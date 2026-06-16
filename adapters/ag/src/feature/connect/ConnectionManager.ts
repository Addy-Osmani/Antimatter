import * as WebSocket from 'ws';

export class ConnectionManager {
    private gatewayClient: WebSocket | null = null;

    public setGatewayClient(client: WebSocket) {
        this.gatewayClient = client;
    }

    public broadcast(payload: any) {
        if (!this.gatewayClient || this.gatewayClient.readyState !== 1) {
            return;
        }

        // Send the payload directly — the Gateway's _adapter_loop picks up whatever
        // the adapter sends and forwards it verbatim (E2EE encrypted) to mobile clients.
        this.gatewayClient.send(JSON.stringify(payload));
    }
}
