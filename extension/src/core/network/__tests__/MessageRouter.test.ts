import { MessageRouter } from '../MessageRouter';
import { WebSocket } from 'ws';

describe('MessageRouter', () => {
    let router: MessageRouter;
    let mockWs: jest.Mocked<WebSocket>;

    beforeEach(() => {
        router = new MessageRouter();
        mockWs = {
            send: jest.fn(),
        } as unknown as jest.Mocked<WebSocket>;
        
        jest.spyOn(console, 'error').mockImplementation(() => {});
        jest.spyOn(console, 'warn').mockImplementation(() => {});
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    test('should route message to registered handler', async () => {
        const handler = jest.fn().mockResolvedValue(undefined);
        router.register('PING', handler);

        const payload = JSON.stringify({ type: 'PING' });
        await router.route(payload, mockWs);

        expect(handler).toHaveBeenCalledWith({ type: 'PING' }, mockWs);
    });

    test('should silently drop unregistered message types', async () => {
        const payload = JSON.stringify({ type: 'UNKNOWN_TYPE' });
        await router.route(payload, mockWs);

        expect(console.warn).toHaveBeenCalledWith(expect.stringContaining('No handler for message type: UNKNOWN_TYPE'));
        expect(mockWs.send).not.toHaveBeenCalled();
    });

    test('should catch handler exceptions and send ERROR message', async () => {
        const handler = jest.fn().mockRejectedValue(new Error('Simulated failure'));
        router.register('PING', handler);

        const payload = JSON.stringify({ type: 'PING' });
        await router.route(payload, mockWs);

        expect(mockWs.send).toHaveBeenCalledWith(JSON.stringify({ type: 'ERROR', message: 'Simulated failure' }));
        expect(console.error).toHaveBeenCalledWith(expect.stringContaining('Error handling PING: Simulated failure'));
    });

    test('should reject malformed JSON silently to avoid crashing', async () => {
        await router.route('INVALID JSON {', mockWs);

        expect(console.error).toHaveBeenCalledWith(expect.stringContaining('Bad JSON from client'));
        expect(mockWs.send).not.toHaveBeenCalled();
    });

    test('should reject payloads exceeding 5MB', async () => {
        // Create a 6MB payload string
        const largeString = 'a'.repeat(6 * 1024 * 1024);
        
        await router.route(largeString, mockWs);

        expect(console.error).toHaveBeenCalledWith(expect.stringContaining('Payload too large'));
        expect(mockWs.send).toHaveBeenCalledWith(JSON.stringify({ type: 'ERROR', message: 'Payload exceeds 5MB limit' }));
    });

    test('should automatically send ACK if message has an id', async () => {
        const handler = jest.fn().mockResolvedValue(undefined);
        router.register('PING', handler);

        const payload = JSON.stringify({ type: 'PING', id: '12345-abcde' });
        await router.route(payload, mockWs);

        expect(mockWs.send).toHaveBeenCalledWith(JSON.stringify({ type: 'ACK', id: '12345-abcde' }));
        expect(handler).toHaveBeenCalledWith({ type: 'PING', id: '12345-abcde' }, mockWs);
    });
});
