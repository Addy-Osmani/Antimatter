# REJECTED Security Claims

> These claims are **factually false**. Each one either hallucinates code that does not exist, misidentifies an existing mitigation, or draws a wrong conclusion from the architecture. Every rejection is backed by direct code inspection with file + line citations.

---

## 1. PTY Terminal Lacking E2EE (AM-001, AM-051, AM-073)

**Report Claim (CVSS 10.0):** PTY output is sent to mobile in plaintext with no E2EE.

**Status: REJECTED**

- `core/gateway/src/antimatter_gateway/pty_manager.py` **lines 69–70**:
  ```python
  if self.router.gateway and self.router.gateway.e2ee:
      await self.router.broadcast_to_clients(payload, self.router.gateway.e2ee)
  ```
  PTY output is wrapped in `broadcast_to_clients`, which at `router.py` **lines 143–144** encrypts with AES-GCM before sending:
  ```python
  plaintext = json.dumps(payload)
  envelope = e2ee.encrypt(plaintext, direction="output")
  ```
  PTY traffic IS E2EE encrypted. This is the most repeated claim in the report and it is entirely wrong.

---

## 2. Symmetric Key in QR Code Plaintext (AM-022, AM-030, AM-006)

**Report Claim (CVSS 9.8):** The QR code contains a plaintext 256-bit symmetric AES key.

**Status: REJECTED**

- `core/gateway/src/antimatter_gateway/qr.py` **lines 21–25**:
  ```python
  params = {
      "url": cloudflare_url or "ws://127.0.0.1:8765",
      "token": pairing_token,
      "x25519_pub": gateway_x25519_pub
  }
  ```
  The QR code contains an **X25519 ECDH public key** (`x25519_pub`), not a symmetric key. The `token` is the Ed25519 public key encoded as base58, used for the challenge-response auth.

  When Cloudflare credentials are present, they are encrypted with AES-GCM before being embedded (`qr.py` **lines 34–43**):
  ```python
  key = hashlib.sha256(pairing_token.encode("utf-8")).digest()
  aesgcm = AESGCM(key)
  ...
  params["cfenc"] = f"{...iv...}:{...tag...}:{...ciphertext...}"
  ```
  No raw symmetric session key is ever placed in the QR code.

---

## 3. Plaintext Room DB (AM-042, AM-007)

**Report Claim (CVSS 8.0):** The Android Room database stores conversation data in plaintext.

**Status: REJECTED**

- `android/core/data/src/main/java/dev/saifmukhtar/antimatter/core/data/AppDatabase.kt` **lines 7, 61–63**:
  ```kotlin
  import net.sqlcipher.database.SupportFactory
  ...
  val passphrase = userPrefs.getDatabasePassphrase()
  val factory = SupportFactory(passphrase)
  ```
  SQLCipher is explicitly used. The passphrase itself is a **cryptographically random 256-bit key** generated via `SecureRandom`, stored in `EncryptedSharedPreferences` (AES-256-GCM-backed Android Keystore).

  `android/core/data/src/main/java/dev/saifmukhtar/antimatter/core/data/UserPreferencesRepository.kt` **lines 182–191**:
  ```kotlin
  fun getDatabasePassphrase(): ByteArray {
      var passphrase = securePrefs.getString(DB_PASSPHRASE, null)
      if (passphrase == null) {
          val key = ByteArray(32)
          secureRandom.nextBytes(key)
          passphrase = Base64.encodeToString(key, Base64.NO_WRAP)
          securePrefs.edit().putString(DB_PASSPHRASE, passphrase).apply()
      }
      return Base64.decode(passphrase, Base64.NO_WRAP)
  }
  ```

---

## 4. No Rate Limiting on WebSocket (AM-008, AM-015)

**Report Claim (CVSS 7.5):** There is no rate limiting on the WebSocket server.

**Status: REJECTED**

- `core/gateway/src/antimatter_gateway/server.py` **lines 16–27**:
  ```python
  _ip_failure_counts: dict[str, dict] = {}
  RATE_LIMIT_MAX_FAILURES = 5
  RATE_LIMIT_WINDOW = 60
  ```
  Rate limiting based on per-IP failure counts is implemented and applied at **line 59**:
  ```python
  if rate_data and rate_data["count"] >= RATE_LIMIT_MAX_FAILURES and now < rate_data["reset_at"]:
      await websocket.close(1008, "Rate Limited")
  ```
  **Note:** This is a *failure-based* rate limiter (it limits IPs that fail auth), not a *connection-rate* limiter. A DoS by opening many connections without failing auth is still possible. This nuance is worth recording but the report's absolute claim of "no rate limiting" is false.

---

## 5. Terminal Emulator RCE / Privilege Escalation on Mobile (AM-004, AM-012, AM-078)

**Report Claim (CVSS 9.0):** The Termux/SwiftTerm terminal emulator on mobile allows RCE or privilege escalation.

**Status: REJECTED**

- The Android app uses the Termux terminal emulator library **only as a UI rendering engine** for text received over the network from the Gateway PTY. The Android feature code at `android/feature/terminal/src/main/java/.../TerminalScreen.kt` and `TerminalViewModel.kt` does not spawn any local shell.
- Grep across `android/feature/` for `Runtime.getRuntime`, `ProcessBuilder`, `spawn`, `exec` returns no results.
- The `android/terminal-emulator/` module is the same terminal emulator rendering library that Termux uses, but instantiated without a local shell process. Shell execution happens exclusively on the remote Gateway.
- Same applies to iOS: `ios/Packages/CoreNetwork/Sources/CoreNetwork/AgentProtocol.swift` only decodes and forwards PTY output to the SwiftTerm view.

---

## 6. Plaintext PTY Logs / Command History Written to Disk (AM-041, AM-075)

**Report Claim (CVSS 9.8):** PTY commands are logged to a file in plaintext.

**Status: REJECTED**

- `core/gateway/src/antimatter_gateway/pty_manager.py` does not write to any file. The only log calls are:
  - **Line 18:** `logger.info(f"PTY session {session_id} already exists...")`
  - **Line 33:** `logger.info(f"Started PTY session {session_id} with PID {pty.pid}")`
  - **Line 51:** `logger.warning(f"Backpressure! Dropping output frames...")`
  - **Line 55:** `logger.error(f"PTY read error...")`
  - **Line 79:** `logger.error(f"Failed to start PTY...")`
  - **Line 109:** `logger.info(f"Cleaning up PTY session...")`
  None of these log raw shell output or command input.

---

## 7. PTY Runs as Root (AM-074)

**Report Claim (CVSS 9.5):** The PTY process runs as root.

**Status: REJECTED**

- `core/gateway/src/antimatter_gateway/pty_manager.py` **line 31**:
  ```python
  pty = ptyprocess.PtyProcess.spawn(['/bin/bash', '--noprofile', '--norc'], dimensions=(rows, cols), env=env)
  ```
  `ptyprocess.PtyProcess.spawn` inherits the process owner of the calling Python interpreter. The Gateway does not call `setuid(0)` or `sudo` anywhere in its codebase. It runs as whoever starts `antimatter-gateway` in their terminal.

---

## 8. No PIN Fallback for Biometric (AM-016)

**Report Claim (CVSS 7.2):** If biometrics fail, the user is locked out with no PIN fallback.

**Status: REJECTED**

- `android/app/src/main/java/dev/saifmukhtar/antimatter/MainActivity.kt` **line 94**:
  ```kotlin
  .setAllowedAuthenticators(
      BiometricManager.Authenticators.BIOMETRIC_STRONG or
      BiometricManager.Authenticators.DEVICE_CREDENTIAL
  )
  ```
  `DEVICE_CREDENTIAL` explicitly adds PIN, Pattern, and Password as fallback authenticators at the Android OS level.

---

## 9. iOS Keychain Storing Symmetric Session Keys

**Report Claim:** The iOS Keychain stores symmetric E2EE session keys permanently.

**Status: REJECTED**

- `ios/Packages/CoreData/Sources/CoreData/ConnectionStore.swift` **line 25**:
  ```swift
  try? KeychainManager.shared.save(key: "antimatter_credentials", data: data)
  ```
  What is stored under `antimatter_credentials` are the pairing credentials: the Gateway WebSocket URL, the Ed25519 public key (`pubKey`), and optionally the Cloudflare `clientId`/`clientSecret` — not session keys.

- Session keys (`c2sKey`, `s2cKey`) are declared in `ios/Packages/CoreNetwork/Sources/CoreNetwork/E2EESession.swift` **lines 14–15**:
  ```swift
  private var c2sKey: SymmetricKey?
  private var s2cKey: SymmetricKey?
  ```
  As `private var` they are held in heap memory only, never written to disk or Keychain, and discarded when the `E2EESession` object is deallocated.

---

## 10. Android Keystore Storing E2EE Session Keys

**Report Claim:** The Android Keystore system (`KeyGenerator`) is used to store E2EE symmetric session keys.

**Status: REJECTED**

- `android/core/network/src/main/java/dev/saifmukhtar/antimatter/core/network/E2EESession.kt` **lines 27–28**:
  ```kotlin
  private var c2sKey: ByteArray? = null
  private var s2cKey: ByteArray? = null
  ```
  Session keys are `ByteArray?` class-level private fields stored exclusively in heap memory. The file uses no `KeyGenerator`, no `KeyStore`, and no `AndroidKeyStore` provider.

---

## 11. Config Stored in Plaintext `~/.antimatter/config.yaml` (AM-018)

**Report Claim:** The Gateway config is stored in plaintext YAML at `~/.antimatter/config.yaml`.

**Status: REJECTED**

- `core/shared-config/src/antimatter_shared_config/config.py` **line 6**:
  ```python
  CONFIG_FILE_PATH = Path(os.path.expanduser("~/.antimatter_daemon/config.json"))
  ```
  The actual file is `~/.antimatter_daemon/config.json`, not `~/.antimatter/config.yaml`.

- Sensitive keys are excluded from the file (**line 49–51**):
  ```python
  safe_data = config.model_dump(exclude_none=True, exclude={
      "cloudflare_client_secret", "pairing_token", "private_key_pem", "gateway_priv_x25519"
  })
  ```
  Only non-sensitive fields (URLs, allowed workspaces) go to the JSON file.

- Permissions are enforced (**lines 58–59**):
  ```python
  os.chmod(temp_path, 0o600)
  ```
  File is owner-only readable/writable.

- All sensitive values (`private_key_pem`, `gateway_priv_x25519`, `pairing_token`, `cloudflare_client_secret`) go to `keyring` or the headless AES-GCM vault (**lines 43–46**).

---

## 12. Hallucinated `tunnel.py` with `--no-tls-verify` (AM-053)

**Report Claim:** A file `core/gateway/src/antimatter_gateway/tunnel.py` exists and spawns `cloudflared` with `--no-tls-verify`.

**Status: REJECTED**

- The directory `core/gateway/src/antimatter_gateway/` contains exactly four Python files: `__pycache__/`, `pty_manager.py`, `qr.py`, `router.py`, `server.py`.
- There is no `tunnel.py`. The claim is hallucinated. The Gateway never spawns `cloudflared` via Python `subprocess`. Cloudflare tunnels must be started externally by the user.

---

## 13. No Key Derivation — Raw `os.urandom(32)` Used as Session Key (AM-023)

**Report Claim:** The session key is a raw `os.urandom(32)` value without proper ECDH/HKDF derivation.

**Status: REJECTED**

- `core/shared-crypto/src/antimatter_crypto/e2ee.py` **lines 40–48**:
  ```python
  def derive_session_keys(self, peer_public_key_b64: str) -> None:
      peer_pub = X25519PublicKey.from_public_bytes(...)
      shared_secret = self._private_key.exchange(peer_pub)
      self._c2s_key = self._derive_key(shared_secret, b"antimatter-v1:client-to-server")
      self._s2c_key = self._derive_key(shared_secret, b"antimatter-v1:server-to-client")
  ```
  Full ECDH (X25519) + HKDF-SHA256 with direction-specific info strings. No raw random bytes are used as session keys.

---

## 14. Static Keys in QR Pairing — No Ephemeral Keys (AM-006)

**Report Claim:** The QR code uses static keys, not ephemeral ECDH keys, allowing session hijacking from QR leak.

**Status: PARTIALLY REJECTED / NUANCED**

- The `x25519_pub` in the QR code is the **Gateway's static X25519 public key**, which is correct and intended — it is the *Gateway's long-term identity key* used to initiate the ECDH handshake.
- The **mobile client** generates a **fresh ephemeral X25519 keypair per session** (`E2EESession.kt` line 16), so the mobile side does use ephemeral keys.
- The risk is that if the QR code's `x25519_pub` value leaks along with *a captured ciphertext of a past session*, and the Gateway's static private key is separately compromised, past sessions can be decrypted. This is the same as finding #6 (No Forward Secrecy on Gateway). It's real but the specific framing of "static keys in QR = session hijacking" is imprecise — the QR code is only scanned once and is not a live session token.
