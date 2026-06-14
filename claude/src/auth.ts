import nacl from 'tweetnacl';
import bs58 from 'bs58';

export class AuthHandler {
    public readonly publicKey: Uint8Array;
    private readonly secretKey: Uint8Array;
    public cloudflareUrl: string | null = null;
    public cloudflareClientId: string | null = null;

    constructor() {
        // Generate Ed25519 key pair
        const keyPair = nacl.sign.keyPair();
        this.publicKey = keyPair.publicKey;
        this.secretKey = keyPair.secretKey;
    }

    public get pairingToken(): string {
        return bs58.encode(Buffer.from(this.publicKey));
    }

    public get qrPayload(): string {
        const payload: any = {
            v: 1,
            k: this.pairingToken
        };
        
        if (this.cloudflareUrl) {
            payload.c = this.cloudflareUrl;
            if (this.cloudflareClientId) {
                payload.cid = this.cloudflareClientId;
            }
        }
        
        return `v1:${Buffer.from(JSON.stringify(payload)).toString('base64')}`;
    }

    public signChallenge(challengeBase64: string): string {
        const challengeBytes = Buffer.from(challengeBase64, 'base64');
        const signature = nacl.sign.detached(challengeBytes, this.secretKey);
        return Buffer.from(signature).toString('base64');
    }
}
