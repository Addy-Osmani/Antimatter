import * as vscode from 'vscode';
import { MessageRouter } from '../../core/network/MessageRouter';
import { ChatStateManager } from '../../core/state/ChatStateManager';

export class ChatCommandHandler {
  constructor(
    private router: MessageRouter,
    private chatState: ChatStateManager,
    _log: (msg: string) => void  // retained for API consistency; not used in this handler
  ) {
    this.registerHandlers();
  }

  private registerHandlers() {
    this.router.register('SEND_MESSAGE', async (msg, _ws) => {
      await vscode.commands.executeCommand('antigravity.openAgent');
      await vscode.commands.executeCommand('antigravity.sendPromptToAgentPanel', msg.text);
    });

    this.router.register('NEW_CONVERSATION', async (_msg, _ws) => {
      await vscode.commands.executeCommand('antigravity.startNewConversation');
      this.chatState.clearActiveConversation();
    });

    this.router.register('CANCEL_RESPONSE', async (_msg, _ws) => {
      await vscode.commands.executeCommand('workbench.action.chat.cancel');
    });

    this.router.register('CHANGE_MODEL', async (_msg, _ws) => {
      await vscode.commands.executeCommand('workbench.action.chat.openModelPicker');
    });

    this.router.register('ACCEPT_EDITS', async (_msg, _ws) => {
      await vscode.commands.executeCommand('antigravity.prioritized.agentAcceptAllInFile');
    });

    this.router.register('REJECT_EDITS', async (_msg, _ws) => {
      await vscode.commands.executeCommand('antigravity.prioritized.agentRejectAllInFile');
    });

    this.router.register('ACCEPT_HUNK', async (_msg, _ws) => {
      await vscode.commands.executeCommand('antigravity.prioritized.agentAcceptFocusedHunk');
    });

    this.router.register('REJECT_HUNK', async (_msg, _ws) => {
      await vscode.commands.executeCommand('antigravity.prioritized.agentRejectFocusedHunk');
    });

    this.router.register('NEXT_HUNK', async (_msg, _ws) => {
      await vscode.commands.executeCommand('antigravity.prioritized.agentFocusNextHunk');
    });

    this.router.register('PREV_HUNK', async (_msg, _ws) => {
      await vscode.commands.executeCommand('antigravity.prioritized.agentFocusPreviousHunk');
    });
  }
}
