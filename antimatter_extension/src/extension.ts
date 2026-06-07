import * as vscode from 'vscode';
import { WebSocketServer, WebSocket } from 'ws';
import * as os from 'os';
import * as net from 'net';
import * as fs from 'fs';
import * as path from 'path';
import * as child_process from 'child_process';
import localtunnel from 'localtunnel';

// ─────────────────────────────────────────────────────────────────────────────
//  TYPES
// ─────────────────────────────────────────────────────────────────────────────

/** Typed messages sent FROM Android app TO bridge */
type InboundMessage =
  | { type: 'SEND_MESSAGE'; text: string }
  | { type: 'NEW_CONVERSATION' }
  | { type: 'CANCEL_RESPONSE' }
  | { type: 'ACCEPT_EDITS' }
  | { type: 'REJECT_EDITS' }
  | { type: 'CHANGE_MODEL' }
  | { type: 'NEXT_HUNK' }
  | { type: 'PREV_HUNK' }
  | { type: 'ACCEPT_HUNK' }
  | { type: 'REJECT_HUNK' }
  | { type: 'GET_FILES'; path?: string }
  | { type: 'READ_FILE'; path: string }
  | { type: 'WRITE_FILE'; path: string; content: string }
  | { type: 'SUBSCRIBE_CONVERSATION'; conversationId: string }
  | { type: 'GET_HISTORY' }
  | { type: 'PING' };

/** Typed messages sent FROM bridge TO Android app */
type OutboundMessage =
  | { type: 'PONG' }
  | { type: 'SESSION_STATE'; conversationId: string | null; model: string; stepCount: number; cloudflareUrl: string | null }
  | { type: 'STEP'; step: TrajectoryStep; index: number }
  | { type: 'GENERATING'; conversationId: string }
  | { type: 'RESPONSE_COMPLETE'; conversationId: string }
  | { type: 'ACTIVE_FILE'; path: string; language: string }
  | { type: 'FILE_CONTENT'; path: string; content: string; language: string }
  | { type: 'FILE_TREE'; tree: FileNode[] }
  | { type: 'CLOUDFLARE_URL'; url: string }
  | { type: 'HISTORY_LIST'; conversations: { id: string; timestamp: number; title: string }[] }
  | { type: 'ERROR'; message: string }
  | { type: 'SUCCESS'; message: string };

interface TrajectoryStep {
  case: string;
  value?: string;
  tool?: string;
  command?: string;
  [key: string]: unknown;
}

interface FileNode {
  name: string;
  path: string;
  isDir: boolean;
  children?: FileNode[];
}

// ─────────────────────────────────────────────────────────────────────────────
//  STATE
// ─────────────────────────────────────────────────────────────────────────────

let wss: WebSocketServer | null = null;
let localTunnelInstance: localtunnel.Tunnel | null = null;
let cloudflareUrl: string | null = null;
let statusBarItem: vscode.StatusBarItem;
let outputChannel: vscode.OutputChannel;

// Conversation tracking
let currentConversationId: string | null = null;
let currentStepCount = 0;
let lastReadBytes = 0;
let currentTranscriptHistory: TrajectoryStep[] = [];
let manualOverrideConversationId: string | null = null;
let manualOverrideTime: number = 0;
let isIntentionalClose = false;
let pollingTimer: NodeJS.Timeout | null = null;
let tailBuffer = '';

// Connected clients
const clients = new Set<WebSocket>();

// Dump VS Code commands
vscode.commands.getCommands(true).then(cmds => {
  fs.writeFileSync('/home/saif/antigravity-commands.txt', cmds.join('\n'));
});

// ─────────────────────────────────────────────────────────────────────────────
//  ENTRY POINT
// ─────────────────────────────────────────────────────────────────────────────

export function activate(context: vscode.ExtensionContext) {
  outputChannel = vscode.window.createOutputChannel('Antimatter Bridge');
  log('Antimatter Bridge activating...');

  // Status bar item showing connection count
  statusBarItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 100);
  statusBarItem.command = 'antimatter.showStatus';
  statusBarItem.text = '$(broadcast) Antimatter';
  statusBarItem.tooltip = 'Antimatter Bridge — click for status';
  statusBarItem.show();
  context.subscriptions.push(statusBarItem);

  // Register commands
  context.subscriptions.push(
    vscode.commands.registerCommand('antimatter.startBridge', () => startBridge(context)),
    vscode.commands.registerCommand('antimatter.stopBridge', stopBridge),
    vscode.commands.registerCommand('antimatter.showStatus', showStatus),
  );

  // Auto-start if configured
  const config = vscode.workspace.getConfiguration('antimatter');
  if (config.get<boolean>('autoStart', true)) {
    startBridge(context);
  }

  // Watch active editor changes → stream to Android
  context.subscriptions.push(
    vscode.window.onDidChangeActiveTextEditor((editor) => {
      if (editor && clients.size > 0) {
        broadcast({
          type: 'ACTIVE_FILE',
          path: editor.document.uri.fsPath,
          language: editor.document.languageId,
        });
      }
    }),
  );

  log('Antimatter Bridge activated.');
}

export function deactivate() {
  stopBridge();
}

// ─────────────────────────────────────────────────────────────────────────────
//  BRIDGE SERVER LIFECYCLE
// ─────────────────────────────────────────────────────────────────────────────

async function startBridge(context: vscode.ExtensionContext) {
  const config = vscode.workspace.getConfiguration('antimatter');
  const port = config.get<number>('port', 8765);
  isIntentionalClose = false;

  if (wss) {
    log('Bridge already running.');
    return;
  }

  try {
    wss = new WebSocketServer({ port, host: '0.0.0.0' });
    log(`WebSocket server started on port ${port}`);

    wss.on('connection', (ws, req) => {
      const remoteAddr = req.socket.remoteAddress ?? 'unknown';
      log(`Client connected: ${remoteAddr}`);
      clients.add(ws);
      updateStatusBar();

      // Send current session state immediately on connect
      sendToClient(ws, {
        type: 'SESSION_STATE',
        conversationId: currentConversationId,
        model: 'gemini-2.5-pro',
        stepCount: currentStepCount,
        cloudflareUrl,
      });

      // Send all existing history
      currentTranscriptHistory.forEach((step, index) => {
        sendToClient(ws, { type: 'STEP', step, index });
      });

      // Also send currently active file
      const activeEditor = vscode.window.activeTextEditor;
      if (activeEditor) {
        sendToClient(ws, {
          type: 'ACTIVE_FILE',
          path: activeEditor.document.uri.fsPath,
          language: activeEditor.document.languageId,
        });
      }

      ws.on('message', (raw) => handleInboundMessage(ws, raw.toString()));

      ws.on('close', () => {
        log(`Client disconnected: ${remoteAddr}`);
        clients.delete(ws);
        updateStatusBar();
      });

      ws.on('error', (err) => {
        log(`Client error (${remoteAddr}): ${err.message}`);
        clients.delete(ws);
        updateStatusBar();
      });
    });

    wss.on('error', (err) => {
      log(`WebSocket server error: ${err.message}`);
      vscode.window.showErrorMessage(`Antimatter Bridge error: ${err.message}`);
    });

    // Start mDNS discovery broadcast removed per user request

    // Tunnel Handling (Cloudflared or LocalTunnel)
    const cloudflareHostname = config.get<string>('cloudflareHostname', '').trim();
    const useLocalTunnel = config.get<boolean>('useLocalTunnel', true);

    if (cloudflareHostname) {
      log(`Using designated Cloudflare Zero Trust hostname: ${cloudflareHostname}`);
      cloudflareUrl = `wss://${cloudflareHostname}`;
      // Send URL slightly delayed to ensure clients that might connect receive it,
      // though typically clients connect AFTER the URL is known.
      setTimeout(() => broadcast({ type: 'CLOUDFLARE_URL', url: cloudflareUrl as string }), 500);
    } else if (useLocalTunnel) {
      await startLocalTunnel(port);
    }

    // Start polling for conversation state changes
    startConversationPolling();

    updateStatusBar();
    vscode.window.showInformationMessage(
      `Antimatter Bridge running on port ${port}. Your phone can now connect!`,
    );
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : String(err);
    log(`Failed to start bridge: ${message}`);
    vscode.window.showErrorMessage(`Failed to start Antimatter Bridge: ${message}`);
  }
}

function stopBridge() {
  isIntentionalClose = true;
  if (pollingTimer) {
    clearInterval(pollingTimer);
    pollingTimer = null;
  }
  
  if (localTunnelInstance) {
    localTunnelInstance.close();
    localTunnelInstance = null;
  }

  cloudflareUrl = null;

  for (const client of clients) {
    client.close();
  }
  clients.clear();

  if (wss) {
    wss.close();
    wss = null;
    log('Bridge stopped.');
  }

  updateStatusBar();
}

// ─────────────────────────────────────────────────────────────────────────────
//  INBOUND MESSAGE HANDLER (Android → PC)
// ─────────────────────────────────────────────────────────────────────────────

async function handleInboundMessage(ws: WebSocket, raw: string) {
  let msg: InboundMessage;
  try {
    msg = JSON.parse(raw) as InboundMessage;
  } catch {
    log(`Bad JSON from client: ${raw.slice(0, 100)}`);
    return;
  }

  log(`← ${msg.type}`);

  try {
    switch (msg.type) {
      // ── AI Chat Commands ──────────────────────────────────────────────────

      case 'SEND_MESSAGE':
        // Focus the Antigravity Agent panel
        await vscode.commands.executeCommand('antigravity.openAgent');
        // Send the text directly to the agent panel
        await vscode.commands.executeCommand('antigravity.sendPromptToAgentPanel', msg.text);
        break;

      case 'NEW_CONVERSATION':
        await vscode.commands.executeCommand('antigravity.startNewConversation');
        currentConversationId = null;
        currentStepCount = 0;
        break;

      case 'CANCEL_RESPONSE':
        await vscode.commands.executeCommand('workbench.action.chat.cancel');
        break;

      case 'CHANGE_MODEL':
        await vscode.commands.executeCommand('workbench.action.chat.openModelPicker');
        break;

      // ── Diff / Edit Commands ──────────────────────────────────────────────

      case 'ACCEPT_EDITS':
        await vscode.commands.executeCommand(
          'antigravity.prioritized.agentAcceptAllInFile',
        );
        break;

      case 'REJECT_EDITS':
        await vscode.commands.executeCommand(
          'antigravity.prioritized.agentRejectAllInFile',
        );
        break;

      case 'ACCEPT_HUNK':
        await vscode.commands.executeCommand(
          'antigravity.prioritized.agentAcceptFocusedHunk',
        );
        break;

      case 'REJECT_HUNK':
        await vscode.commands.executeCommand(
          'antigravity.prioritized.agentRejectFocusedHunk',
        );
        break;

      case 'NEXT_HUNK':
        await vscode.commands.executeCommand(
          'antigravity.prioritized.agentFocusNextHunk',
        );
        break;

      case 'PREV_HUNK':
        await vscode.commands.executeCommand(
          'antigravity.prioritized.agentFocusPreviousHunk',
        );
        break;

      // ── File System Commands ──────────────────────────────────────────────

      case 'GET_FILES': {
        const rootPath = msg.path
          ? vscode.Uri.file(msg.path)
          : vscode.workspace.workspaceFolders?.[0]?.uri;
        if (!rootPath) {
          sendToClient(ws, { type: 'ERROR', message: 'No workspace folder open' });
          break;
        }
        const tree = await buildFileTree(rootPath.fsPath, 2);
        sendToClient(ws, { type: 'FILE_TREE', tree });
        break;
      }

      case 'READ_FILE': {
        try {
          const doc = await vscode.workspace.openTextDocument(
            vscode.Uri.file(msg.path),
          );
          sendToClient(ws, {
            type: 'FILE_CONTENT',
            path: msg.path,
            content: doc.getText(),
            language: doc.languageId,
          });
        } catch (err) {
          sendToClient(ws, {
            type: 'ERROR',
            message: `Cannot read file: ${msg.path}`,
          });
        }
        break;
      }

      case 'WRITE_FILE': {
        try {
          const uri = vscode.Uri.file(msg.path);
          await vscode.workspace.fs.writeFile(uri, Buffer.from(msg.content, 'utf8'));
          sendToClient(ws, { type: 'SUCCESS', message: `File saved: ${msg.path}` });
        } catch (err) {
          sendToClient(ws, {
            type: 'ERROR',
            message: `Cannot write file: ${msg.path}`,
          });
        }
        break;
      }

      // ── Session Management ────────────────────────────────────────────────

      case 'SUBSCRIBE_CONVERSATION':
        manualOverrideConversationId = msg.conversationId;
        manualOverrideTime = Date.now();
        currentConversationId = msg.conversationId;
        currentStepCount = 0;
        lastReadBytes = 0;
        tailBuffer = '';
        currentTranscriptHistory = [];
        log(`Subscribed to conversation: ${msg.conversationId}`);
        await pollFromBrain();
        break;

      case 'PING':
        sendToClient(ws, { type: 'PONG' });
        break;

      case 'GET_HISTORY':
        const history = await getConversationHistory();
        sendToClient(ws, { type: 'HISTORY_LIST', conversations: history });
        break;

      default:
        log(`Unknown message type: ${(msg as { type: string }).type}`);
    }
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : String(err);
    log(`Error handling ${msg.type}: ${message}`);
    sendToClient(ws, { type: 'ERROR', message });
  }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CONVERSATION STATE POLLING (Antigravity → Android streaming)
//
//  Strategy: We monitor the ~/.gemini/antigravity-ide/brain/ directory for the 
//  most recently modified conversation folder. Inside it, we tail the 
//  transcript.jsonl file to stream TrajectoryStep events to the Android app.
// ─────────────────────────────────────────────────────────────────────────────



function startConversationPolling() {
  if (pollingTimer) return;

  pollingTimer = setInterval(async () => {
    if (clients.size === 0) return; // No one listening, skip

    try {
      await pollFromBrain();
    } catch (e) {
      log(`Polling error: ${e}`);
    }
  }, 1000);
}

async function pollFromBrain() {
  const brainDir = path.join(os.homedir(), '.gemini', 'antigravity-ide', 'brain');
  if (!fs.existsSync(brainDir)) return;

  const dirs = fs.readdirSync(brainDir, { withFileTypes: true });
  let latestDir = '';
  let latestTime = 0;

  // Find the most recently modified conversation folder
  for (const dir of dirs) {
    if (!dir.isDirectory()) continue;
    
    // Ignore tempmediaStorage and other non-UUID folders if possible
    if (dir.name === 'tempmediaStorage' || dir.name.startsWith('.')) continue;

    const dirPath = path.join(brainDir, dir.name);
    const transcriptPath = path.join(dirPath, '.system_generated', 'logs', 'transcript.jsonl');
    
    if (fs.existsSync(transcriptPath)) {
      const stat = fs.statSync(transcriptPath);
      if (stat.mtimeMs > latestTime) {
        latestTime = stat.mtimeMs;
        latestDir = dir.name;
      }
    }
  }

  if (manualOverrideConversationId) {
    if (latestTime > manualOverrideTime) {
      manualOverrideConversationId = null;
    } else {
      latestDir = manualOverrideConversationId;
    }
  }

  if (!latestDir) return;

  const activeTranscriptPath = path.join(brainDir, latestDir, '.system_generated', 'logs', 'transcript.jsonl');

  // If conversation changed, reset
  if (latestDir !== currentConversationId) {
    currentConversationId = latestDir;
    currentStepCount = 0;
    lastReadBytes = 0;
    tailBuffer = '';
    currentTranscriptHistory = [];
    log(`New active conversation detected: ${latestDir}`);
    broadcast({ type: 'GENERATING', conversationId: latestDir });
  }

  const stat = fs.statSync(activeTranscriptPath);
  
  // File shrunk or was reset? Reset bytes
  if (stat.size < lastReadBytes) {
    lastReadBytes = 0;
    currentStepCount = 0;
  }

  if (stat.size > lastReadBytes) {
    // Read the newly added bytes
    const buffer = Buffer.alloc(stat.size - lastReadBytes);
    const fd = fs.openSync(activeTranscriptPath, 'r');
    fs.readSync(fd, buffer, 0, buffer.length, lastReadBytes);
    fs.closeSync(fd);
    
    lastReadBytes = stat.size;
    
    // Parse JSONL lines
    const content = buffer.toString('utf-8');
    const lines = content.split('\n').filter(line => line.trim().length > 0);
    
    for (const line of lines) {
      try {
        const entry = JSON.parse(line);
        // The trajectory step we send to the app should match what it expects
        // 'entry' has source, type, content, tool_calls, etc.
        let stepValue = entry.content || '';
        
        // Strip XML tags from user input
        if (entry.type === 'USER_INPUT' && stepValue.includes('<USER_REQUEST>')) {
          const match = stepValue.match(/<USER_REQUEST>([\s\S]*?)<\/USER_REQUEST>/);
          if (match && match[1]) {
            stepValue = match[1].trim();
          }
        }

        const step: TrajectoryStep = {
          case: entry.type,
          value: stepValue,
          ...entry
        };
        
        currentTranscriptHistory.push(step);
        broadcast({ type: 'STEP', step, index: currentStepCount });
        currentStepCount++;
      } catch (e) {
        // Skip malformed JSON
      }
    }
  }
}





// ─────────────────────────────────────────────────────────────────────────────
//  FILE SYSTEM UTILITIES
// ─────────────────────────────────────────────────────────────────────────────

async function buildFileTree(dirPath: string, depth: number): Promise<FileNode[]> {
  if (depth === 0) return [];

  const IGNORED = new Set([
    'node_modules', '.git', 'dist', 'build', 'out', '.gradle',
    '__pycache__', '.venv', 'venv', '.idea', '.DS_Store',
  ]);

  try {
    const entries = fs.readdirSync(dirPath, { withFileTypes: true });
    const nodes: FileNode[] = [];

    for (const entry of entries) {
      if (IGNORED.has(entry.name)) continue;

      const fullPath = path.join(dirPath, entry.name);
      const node: FileNode = {
        name: entry.name,
        path: fullPath,
        isDir: entry.isDirectory(),
      };

      if (entry.isDirectory() && depth > 1) {
        node.children = await buildFileTree(fullPath, depth - 1);
      }

      nodes.push(node);
    }

    // Directories first, then files, alphabetical
    return nodes.sort((a, b) => {
      if (a.isDir !== b.isDir) return a.isDir ? -1 : 1;
      return a.name.localeCompare(b.name);
    });
  } catch {
    return [];
  }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TUNNEL UTILITIES (LocalTunnel)
// ─────────────────────────────────────────────────────────────────────────────

async function startLocalTunnel(port: number): Promise<void> {
  const config = vscode.workspace.getConfiguration('antimatter');
  const subdomain = config.get<string>('localtunnelSubdomain', '');

  log(`Starting LocalTunnel on port ${port}...`);
  try {
    const opts: any = { port };
    if (subdomain) {
      opts.subdomain = subdomain;
    }
    
    localTunnelInstance = await localtunnel(opts);
    
    localTunnelInstance.on('error', err => {
      log(`LocalTunnel error: ${err.message}`);
    });

    localTunnelInstance.on('close', () => {
      log('LocalTunnel closed.');
      cloudflareUrl = null;
      if (!isIntentionalClose && wss) {
        log('Restarting LocalTunnel in 5s...');
        setTimeout(() => startLocalTunnel(port), 5000);
      }
    });

    cloudflareUrl = localTunnelInstance.url.replace('https://', 'wss://');
    log(`LocalTunnel URL: ${cloudflareUrl}`);
    broadcast({ type: 'CLOUDFLARE_URL', url: cloudflareUrl });
  } catch (err: any) {
    log(`Failed to start LocalTunnel: ${err.message}`);
  }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HISTORY UTILITIES
// ─────────────────────────────────────────────────────────────────────────────

async function getConversationHistory(): Promise<{ id: string; timestamp: number; title: string }[]> {
  const brainDir = path.join(os.homedir(), '.gemini', 'antigravity-ide', 'brain');
  if (!fs.existsSync(brainDir)) return [];

  const history: { id: string; timestamp: number; title: string }[] = [];
  const dirs = fs.readdirSync(brainDir, { withFileTypes: true });

  for (const dir of dirs) {
    if (!dir.isDirectory() || dir.name.startsWith('.') || dir.name === 'tempmediaStorage') continue;

    const transcriptPath = path.join(brainDir, dir.name, '.system_generated', 'logs', 'transcript.jsonl');
    if (!fs.existsSync(transcriptPath)) continue;

    const stat = fs.statSync(transcriptPath);
    let title = 'New Conversation';

    try {
      // Read just enough to get the first user message for a title
      const fd = fs.openSync(transcriptPath, 'r');
      const buffer = Buffer.alloc(4096);
      const bytesRead = fs.readSync(fd, buffer, 0, 4096, 0);
      fs.closeSync(fd);

      const content = buffer.toString('utf-8', 0, bytesRead);
      const lines = content.split('\n');
      for (const line of lines) {
        if (!line.trim()) continue;
        try {
          const entry = JSON.parse(line);
          if (entry.type === 'USER_INPUT' && entry.content) {
            let text = entry.content;
            if (text.includes('<USER_REQUEST>')) {
               const match = text.match(/<USER_REQUEST>([\s\S]*?)<\/USER_REQUEST>/);
               if (match && match[1]) text = match[1].trim();
            }
            title = text.length > 50 ? text.substring(0, 50) + '...' : text;
            break;
          }
        } catch { }
      }
    } catch { }

    history.push({
      id: dir.name,
      timestamp: stat.mtimeMs,
      title
    });
  }

  // Sort newest first
  return history.sort((a, b) => b.timestamp - a.timestamp);
}

// ─────────────────────────────────────────────────────────────────────────────
//  HELPERS
// ─────────────────────────────────────────────────────────────────────────────

function broadcast(msg: OutboundMessage) {
  const json = JSON.stringify(msg);
  for (const client of clients) {
    if (client.readyState === WebSocket.OPEN) {
      client.send(json);
    }
  }
}

function sendToClient(ws: WebSocket, msg: OutboundMessage) {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(msg));
  }
}

function log(message: string) {
  const t = new Date().toISOString();
  fs.appendFileSync('/home/saif/antimatter-bridge.log', `[${t}] ${message}\n`);
  if (outputChannel) {
    const timestamp = new Date().toLocaleTimeString();
    outputChannel.appendLine(`[${timestamp}] ${message}`);
  }
}

function updateStatusBar() {
  if (!statusBarItem) return;
  const count = clients.size;
  if (count === 0) {
    statusBarItem.text = '$(broadcast) Antimatter';
    statusBarItem.tooltip = 'Antimatter Bridge — no devices connected';
    statusBarItem.color = undefined;
  } else {
    statusBarItem.text = `$(broadcast) Antimatter (${count})`;
    statusBarItem.tooltip = `Antimatter Bridge — ${count} device(s) connected`;
    statusBarItem.color = new vscode.ThemeColor('statusBarItem.prominentForeground');
  }
}

function showStatus() {
  const port = vscode.workspace.getConfiguration('antimatter').get<number>('port', 8765);
  const localIp = getLocalIp();
  const lines = [
    `🟢 Antimatter Bridge running on port ${port}`,
    ``,
    `📡 Local Wi-Fi: ws://${localIp}:${port}`,
    cloudflareUrl ? `☁️  Remote URL: ${cloudflareUrl.replace('https://', 'wss://')}` : `☁️  Cloudflare Tunnel: disabled`,
    ``,
    `📱 Connected devices: ${clients.size}`,
    `💬 Active conversation: ${currentConversationId ?? 'none'}`,
    `📝 Steps captured: ${currentStepCount}`,
  ];

  vscode.window.showInformationMessage(lines.join('\n'), { modal: true }, 'OK');
}

function getLocalIp(): string {
  const interfaces = os.networkInterfaces();
  for (const iface of Object.values(interfaces)) {
    if (!iface) continue;
    for (const addr of iface) {
      if (addr.family === 'IPv4' && !addr.internal) {
        return addr.address;
      }
    }
  }
  return 'localhost';
}
