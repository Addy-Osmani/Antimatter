import { spawn } from 'child_process';
import * as vscode from 'vscode';
import { MessageRouter } from '../../core/network/MessageRouter';
import { ConnectionManager } from '../connect/ConnectionManager';

export class TerminalCommandHandler {
  constructor(
    private router: MessageRouter,
    private connectionManager: ConnectionManager,
    private log: (msg: string) => void
  ) {
    this.registerHandlers();
  }

  private registerHandlers() {
    this.router.register('EXECUTE_COMMAND', async (msg, ws) => {
      this.log(`Executing command: ${msg.command}`);
      
      const cmdTrimmed = msg.command.trim();
      const allowedPatterns = [
        /^npm\s+/,
        /^yarn\s+/,
        /^git\s+/,
        /^ls\s*/,
        /^cat\s+/,
        /^pwd\s*/,
        /^echo\s+/,
        /^mkdir\s+/
      ];

      const isAllowed = allowedPatterns.some(pattern => pattern.test(cmdTrimmed));
      if (!isAllowed && !cmdTrimmed.startsWith('rm ')) {
        this.log(`Command rejected by allowlist: ${msg.command}`);
        this.connectionManager.broadcast({
          type: 'COMMAND_OUTPUT',
          text: `Error: Command not in allowlist.\n`,
          isError: true
        });
        return;
      }

      if (cmdTrimmed.startsWith('rm ')) {
        const answer = await vscode.window.showWarningMessage(
          `Antimatter: Execute destructive command "${msg.command}"?`,
          { modal: true },
          'Execute', 'Cancel'
        );
        if (answer !== 'Execute') {
          this.connectionManager.broadcast({
            type: 'COMMAND_OUTPUT',
            text: `Command execution cancelled by user.\n`,
            isError: true
          });
          return;
        }
      }

      try {
        const shell = process.platform === 'win32' ? 'cmd.exe' : '/bin/sh';
        const args = process.platform === 'win32' ? ['/c', msg.command] : ['-c', msg.command];
        
        const cwd = vscode.workspace.workspaceFolders?.[0]?.uri.fsPath || process.cwd();
        const child = spawn(shell, args, { cwd });
        
        const TIMEOUT_MS = 5 * 60 * 1000;
        const timeoutTimer = setTimeout(() => {
          child.kill('SIGTERM');
          this.connectionManager.broadcast({
            type: 'COMMAND_OUTPUT',
            text: `\n[Process terminated due to 5-minute timeout]\n`,
            isError: true
          });
        }, TIMEOUT_MS);
        
        child.stdout.on('data', (data) => {
          this.connectionManager.broadcast({
            type: 'COMMAND_OUTPUT',
            text: data.toString(),
            isError: false
          });
        });
        
        child.stderr.on('data', (data) => {
          this.connectionManager.broadcast({
            type: 'COMMAND_OUTPUT',
            text: data.toString(),
            isError: true
          });
        });
        
        child.on('close', (code) => {
          clearTimeout(timeoutTimer);
          this.connectionManager.broadcast({
            type: 'COMMAND_OUTPUT',
            text: `\n[Process exited with code ${code}]\n`,
            isError: code !== 0
          });
        });
        
        child.on('error', (err) => {
          clearTimeout(timeoutTimer);
          this.log(`Failed to start command: ${err.message}`);
          this.connectionManager.broadcast({
            type: 'COMMAND_OUTPUT',
            text: `Failed to start process: ${err.message}\n`,
            isError: true
          });
        });
      } catch (err: any) {
        this.log(`Error executing command: ${err.message}`);
        this.connectionManager.broadcast({
          type: 'COMMAND_OUTPUT',
          text: `Execution error: ${err.message}\n`,
          isError: true
        });
      }
    });
  }
}
