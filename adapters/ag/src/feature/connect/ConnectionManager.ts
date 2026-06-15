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

        // We wrap the payload in an IPC envelope so the Gateway knows this is an 
        // outbound message that needs to be E2EE encrypted and sent to the mobile client
        const ipcMessage = {
            type: 'ADAPTER_OUTBOUND',
            payload: payload
        };
        this.gatewayClient.send(JSON.stringify(ipcMessage));
    }
}
