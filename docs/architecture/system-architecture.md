# Antimatter System Architecture

This document provides a high-level, comprehensive overview of the entire Antimatter ecosystem. The architecture follows an **Independent Adapter Model**, where a centralized **Gateway** handles zero-knowledge End-to-End Encryption (E2EE), secure WebSocket routing, and Cloudflare access, while lightweight **Adapters** run independently to connect various AI tools and IDEs.

## High-Level Component Map

```mermaid
flowchart TB
    %% Styling Classes
    classDef mobile fill:#005c99,stroke:#00a3cc,stroke-width:2px,color:#fff
    classDef cloudflare fill:#f38020,stroke:#d9660a,stroke-width:2px,color:#fff
    classDef gateway fill:#2a0a4a,stroke:#7b2cbf,stroke-width:2px,color:#fff
    classDef security fill:#9d0208,stroke:#dc2f02,stroke-width:2px,color:#fff
    classDef adapter fill:#0a4a2a,stroke:#2dc653,stroke-width:2px,color:#fff
    classDef storage fill:#3a3a3a,stroke:#666,stroke-width:2px,color:#fff

    %% Mobile Clients
    subgraph Clients["Mobile Clients (Decryption & Rendering)"]
        direction LR
        Android["Android App (Kotlin)"]:::mobile
        iOS["iOS App (Swift)"]:::mobile
        
        AppE2EE["E2EE Module (CryptoKit / JCA)"]:::security
        AppStorage["Keychain / EncryptedPrefs"]:::storage
        
        Android --> AppE2EE
        iOS --> AppE2EE
        AppE2EE --> AppStorage
    end

    %% Network Boundary
    CF["Cloudflare Quick Tunnel (WSS)"]:::cloudflare

    %% Local Gateway Node
    subgraph GatewayNode["Antimatter Gateway (127.0.0.1:8765)"]
        direction TB
        Server["WebSocket Server"]:::gateway
        Router["Message Router"]:::gateway
        FS["Shared FS Interceptor"]:::gateway
        
        subgraph Security["Core Crypto Layer"]
            direction LR
            Auth["Ed25519 Auth Manager"]:::security
            E2EE["X25519 ECDH + AES-GCM Engine"]:::security
        end
        
        Server --> Security
        Security --> Router
        Router --> FS
    end

    %% Independent Adapters
    subgraph Adapters["Independent Adapters (Plaintext IPC)"]
        direction LR
        AG["adapter-ag (Antigravity IDE)"]:::adapter
        AG2["adapter-ag2 (Antigravity 2.0)"]:::adapter
        CC["adapter-cc (Claude Code)"]:::adapter
        
        subgraph Watchers["Background Services"]
            BrainWatcher["Brain Watcher (Auto-detect conversations)"]:::storage
        end
        
        AG2 -.-> BrainWatcher
    end

    %% Data Flows
    Clients == "AES-GCM Ciphertext over TLS" ==> CF
    CF == "TLS Termination (Ciphertext remains opaque)" ==> Server
    
    Router == "Local Plaintext JSON IPC" ==> AG
    Router == "Local Plaintext JSON IPC" ==> AG2
    Router == "Local Plaintext JSON IPC" ==> CC
```

## Boundary Descriptions

### 1. Mobile Clients
The iOS and Android apps act as the **sole decrypters** of the AI's output. Because the system uses E2EE, the clients do not trust the network. They use native cryptographic libraries (`CryptoKit` for iOS, `JCA` for Android) to perform Ed25519 authentication and X25519 ECDH key exchanges.

### 2. Cloudflare Network
Cloudflare acts as a Zero Trust tunnel, exposing the local Gateway to the internet. While Cloudflare terminates the TLS connection, the actual payloads passing through the WebSocket are opaque AES-256-GCM ciphertexts. **Cloudflare cannot read the AI's thoughts, code, or commands.**

### 3. Antimatter Gateway
The Gateway acts as the central hub of the user's local machine. It:
1. Validates the incoming Ed25519 signatures.
2. Performs the ECDH handshake to establish session keys.
3. Decrypts incoming `cmd:` packets and encrypts outgoing `output:` packets.
4. Routes local IPC traffic to whichever adapter the user is actively controlling.
5. Intercepts `GET_FILES` calls via `antimatter_fs` to prevent blocking the adapters' event loops.

### 4. Independent Adapters
Adapters are lightweight integration plugins (`ag`, `ag2`, `cc`). They know nothing about encryption or the internet. They simply connect to the Gateway over `ws://127.0.0.1:8765`, register themselves, and communicate using plaintext JSON IPC. This decoupling allows new integrations to be written rapidly without recreating complex networking logic.
