import { MessageRouter } from '../MessageRouter';

describe('MessageRouter', () => {
    let mockSocket: any;
    let router: MessageRouter;

    beforeEach(() => {
        mockSocket = {
            send: jest.fn(),
            close: jest.fn(),
            readyState: 1 // OPEN
        };
        router = new MessageRouter();
    });

    test('should reject payloads exceeding 5MB', async () => {
        const fakeLargeData = 'a'.repeat(6 * 1024 * 1024);

        await router.route(fakeLargeData, mockSocket as any);

        expect(mockSocket.send).toHaveBeenCalledWith(expect.stringContaining('Payload exceeds 5MB limit'));
    });

    test('should parse and route valid small payloads', async () => {
        const validPayload = JSON.stringify({ type: 'PING' });
        
        let handlerCalled = false;
        router.register('PING', async (_msg, _ws) => {
            handlerCalled = true;
        });

        await router.route(validPayload, mockSocket as any);
        
        expect(handlerCalled).toBe(true);
        expect(mockSocket.send).not.toHaveBeenCalledWith(expect.stringContaining('Payload exceeds 5MB limit'));
    });
});
