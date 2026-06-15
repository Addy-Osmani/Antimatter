import WebSocket from 'ws';
import { ConnectionManager } from '../../feature/connect/ConnectionManager';
import { MessageRouter } from './MessageRouter';

export class GatewayClient {
    private client: WebSocket | null = null;
    private readonly gatewayUrl = 'ws://127.0.0.1:8765';

    constructor(
        private connectionManager: ConnectionManager,
        private router: MessageRouter,
        private workspaceRoot: string,
        private log: (msg: string) => void
    ) {}

    public start() {
        this.connect();
    }

    private connect() {
        this.log(`Connecting to Gateway at ${this.gatewayUrl}...`);
        this.client = new WebSocket(this.gatewayUrl);

        this.client.on('open', () => {
            this.log('Connected to Gateway IPC.');
            // Register as the ag adapter
            this.client!.send(JSON.stringify({
                type: 'REGISTER_ADAPTER',
                name: 'ag',
                workspaceRoot: this.workspaceRoot
            }));
        });

        this.client.on('message', async (data: WebSocket.RawData) => {
            try {
                const messageStr = data.toString();
                const payload = JSON.parse(messageStr);
                
                // Route through the existing router
                await this.router.route(payload, this.client!);
            } catch (error) {
                this.log(`Error parsing IPC message: ${error}`);
            }
        });

        this.client.on('close', () => {
            this.log('Disconnected from Gateway. Reconnecting in 3s...');
            setTimeout(() => this.connect(), 3000);
        });

        this.client.on('error', (err) => {
            this.log(`IPC Connection Error: ${err.message}`);
        });

        // Bind connection manager to broadcast back to Gateway
        this.connectionManager.setGatewayClient(this.client as any);
    }

    public stop() {
        if (this.client) {
            this.client.close();
            this.client = null;
        }
    }
}
