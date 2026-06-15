import * as vscode from 'vscode';
import { ConnectionManager } from './feature/connect/ConnectionManager';
import { ChatStateManager } from './core/state/ChatStateManager';
import { MessageRouter } from './core/network/MessageRouter';
import { GatewayClient } from './core/network/GatewayClient';
import { BrainWatcher } from './feature/chat/BrainWatcher';
import { ChatCommandHandler } from './feature/chat/ChatCommandHandler';
import { FileCommandHandler } from './feature/files/FileCommandHandler';
import { HistoryManager } from './feature/chat/HistoryManager';
import { FileSystemHelper } from './core/data/FileSystemHelper';

let outputChannel: vscode.OutputChannel;

function log(msg: string) {
  if (!outputChannel) return;
  const time = new Date().toISOString().split('T')[1].slice(0, -1);
  const line = `[${time}] ${msg}`;
  outputChannel.appendLine(line);
}

export async function activate(context: vscode.ExtensionContext) {
  outputChannel = vscode.window.createOutputChannel('Antimatter Bridge');
  log('Antimatter Bridge activating...');

  // 1. Data & State Managers
  const connectionManager = new ConnectionManager();
  const chatManager = new ChatStateManager();
  const fsHelper = new FileSystemHelper();
  
  // 2. Core Infrastructure (IPC to Gateway)
  const router = new MessageRouter();
  const workspaceRoot = vscode.workspace.workspaceFolders?.[0]?.uri.fsPath || '';
  const gatewayClient = new GatewayClient(connectionManager, router, workspaceRoot, log);
  const brainWatcher = new BrainWatcher(chatManager, connectionManager, log);

  // 3. Feature Handlers
  new ChatCommandHandler(router, chatManager, log);
  new FileCommandHandler(router, fsHelper, log);
  new HistoryManager(router, log);

  // 4. Register Routes
  router.register('SUBSCRIBE_CONVERSATION', async (msg, _ws) => {
    chatManager.setActiveConversation(msg.conversationId);
    brainWatcher.setConversation(msg.conversationId, msg.lastKnownStepCount || 0);
  });

  router.register('PING', async (_msg, _ws) => {
    // Gateway will handle generic PINGs, but we can reply if it routes it here
    connectionManager.broadcast({ type: 'PONG' });
  });

  const startBridge = async () => {
    try {
      gatewayClient.start();
      brainWatcher.start();
      log('Bridge connected to Gateway IPC.');
      vscode.window.showInformationMessage(`Antimatter Bridge started.`);
    } catch (e) {
      log(`Failed to start bridge: ${e}`);
    }
  };

  const stopBridge = () => {
    gatewayClient.stop();
    brainWatcher.stop();
    log('Bridge stopped.');
  };

  // 6. Register VS Code Commands
  context.subscriptions.push(
    vscode.commands.registerCommand('antimatter.startBridge', startBridge),
    vscode.commands.registerCommand('antimatter.stopBridge', stopBridge),
    vscode.commands.registerCommand('antimatter.showPairingQR', () => {
      vscode.window.showInformationMessage('Pairing is now managed by the Gateway terminal. Check your CLI!');
    })
  );

  // Watch active editor changes → stream to Android
  context.subscriptions.push(
    vscode.window.onDidChangeActiveTextEditor((editor) => {
      if (editor) {
        connectionManager.broadcast({
          type: 'ACTIVE_FILE',
          path: editor.document.uri.fsPath,
          language: editor.document.languageId,
        });
      }
    }),
  );

  // Auto-start
  const config = vscode.workspace.getConfiguration('antimatter');
  if (config.get<boolean>('autoStart', true)) {
    startBridge();
  }

  log('Antimatter Bridge activated.');
}

export function deactivate() {
}
