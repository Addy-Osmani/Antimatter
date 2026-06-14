import { spawn, ChildProcess } from 'child_process';
import * as fs from 'fs';
import * as path from 'os';
import * as readline from 'readline';

export class CloudflareTunnel {
    private quickTunnelProcess: ChildProcess | null = null;
    private url: string | null = null;
    private isIntentionalClose: boolean = false;

    constructor(private readonly port: number) {}

    public async start(): Promise<string | null> {
        // Mode A: Persistent Zero Trust Configuration
        const configPath = path.homedir() + '/.antimatter_daemon/config.json';
        let cloudflareHostname = process.env.CLOUDFLARE_HOSTNAME;

        if (fs.existsSync(configPath)) {
            try {
                const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
                if (config.cloudflare_hostname) {
                    cloudflareHostname = config.cloudflare_hostname;
                }
            } catch (e) {
                console.warn('[Tunnel] Failed to read config.json:', e);
            }
        }

        this.isIntentionalClose = false;

        if (cloudflareHostname) {
            console.log(`[Tunnel] Using designated Cloudflare Zero Trust hostname: ${cloudflareHostname}`);
            this.url = `wss://${cloudflareHostname}`;
            return this.url;
        }

        // Mode B: Quick Tunnel Fallback
        return new Promise((resolve) => {
            console.log('[Tunnel] Starting Cloudflared Quick Tunnel...');
            this.quickTunnelProcess = spawn('cloudflared', ['tunnel', '--url', `http://localhost:${this.port}`]);

            if (this.quickTunnelProcess.stderr) {
                const rl = readline.createInterface({ input: this.quickTunnelProcess.stderr });
                rl.on('line', (line) => {
                    const output = line.toString();
                    
                    const primaryMatch = output.match(/https:\/\/[a-zA-Z0-9-]+\.trycloudflare\.com/);
                    const fallbackMatch = output.match(/([a-zA-Z0-9-]+\.trycloudflare\.com)/);
                    const httpUrl = primaryMatch?.[0] ?? (fallbackMatch ? `https://${fallbackMatch[1]}` : null);

                    if (httpUrl && !this.url) {
                        this.url = httpUrl.replace('https://', 'wss://');
                        console.log(`[Tunnel] Quick tunnel established: ${this.url}`);
                        resolve(this.url);
                    }
                });
            }

            this.quickTunnelProcess.on('close', (code) => {
                if (!this.isIntentionalClose) {
                    console.log(`[Tunnel] Quick tunnel closed unexpectedly with code ${code}.`);
                    this.url = null;
                }
            });

            this.quickTunnelProcess.on('error', (err) => {
                console.log(`[Tunnel] Quick tunnel spawn error: ${err.message}`);
                resolve(null);
            });
            
            // Timeout resolve after 15 seconds
            setTimeout(() => resolve(null), 15000);
        });
    }

    public stop() {
        this.isIntentionalClose = true;
        if (this.quickTunnelProcess) {
            this.quickTunnelProcess.kill();
            this.quickTunnelProcess = null;
        }
        this.url = null;
    }

    public getUrl(): string | null {
        return this.url;
    }
}
