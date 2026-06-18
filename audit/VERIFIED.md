# VERIFIED Security Claims

> These claims are **confirmed true** by direct inspection of the source code. Each entry includes the exact file path and line number(s) that prove the claim.

---

## 1. Plaintext IPC: Gateway → Adapters (AM-002, AM-050)

**Claim:** After E2EE decryption, the Gateway sends commands to local adapters in plaintext over a WebSocket.

**Status: VERIFIED**

- `core/gateway/src/antimatter_gateway/router.py` **line 134**:
  ```python
  await adapter["ws"].send(json.dumps(parsed_cmd))
  ```
  The decrypted `parsed_cmd` dict is serialised to plain JSON and sent directly to the adapter's WebSocket, with no re-encryption.

- `core/gateway/src/antimatter_gateway/router.py` **line 143–145**:
  ```python
  plaintext = json.dumps(payload)
  envelope = e2ee.encrypt(plaintext, direction="output")
  ```
  In `broadcast_to_clients` the Gateway *does* re-encrypt before sending to mobile clients. The gap is the **adapter direction** at line 134 only.

---

## 2. No Adapter Authentication (AM-010, AM-020, AM-052)

**Claim:** Any local process can connect to the Gateway and register as an adapter without mTLS, shared secret, or certificate verification.

**Status: VERIFIED**

- `core/gateway/src/antimatter_gateway/server.py` **lines 81–90**:
  ```python
  if msg_type == "REGISTER_ADAPTER":
      agent_id = data.get("id")
      agent_name = data.get("name")
      ...
      await self.router.register_adapter(agent_id, agent_name, websocket, workspace_root)
  ```
  The only check is that `agent_id` and `agent_name` are non-null strings. There is no token, certificate, or any secret verification on the `REGISTER_ADAPTER` path.

---

## 3. No PTY Sandboxing or Command Restrictions (AM-070, AM-071)

**Claim:** The PTY spawns a raw `/bin/bash` with no sandbox, no resource limits, no command allow/blocklist.

**Status: VERIFIED**

- `core/gateway/src/antimatter_gateway/pty_manager.py` **line 31**:
  ```python
  pty = ptyprocess.PtyProcess.spawn(['/bin/bash', '--noprofile', '--norc'], dimensions=(rows, cols), env=env)
  ```
  There is no `nsjail`, `firejail`, `seccomp`, `cgroups`, or any syscall/command restriction wrapping this call.

---

## 4. No Session Tokens / JWT on Gateway (AM-011, AM-021, AM-072)

**Claim:** Once the E2EE handshake completes, there are no session tokens, expiry, or JWT mechanisms. Any established E2EE connection is indefinitely valid.

**Status: VERIFIED**

- `core/gateway/src/antimatter_gateway/server.py` **lines 68–69 and 128–136**:
  ```python
  authenticated = False
  e2ee_established = False
  ...
  self.e2ee.derive_session_keys(client_pubkey)
  e2ee_established = True
  ```
  After the `HELLO` handshake, there is no token issued, no expiry timer, and no invalidation mechanism. The session stays live until the WebSocket disconnects.

---

## 5. Replay Attack Vulnerability — No msg_id Enforcement (AM-005, AM-013)

**Claim:** The `msg_id` inside the AAD is sent but never validated against a monotonically-increasing counter on the receiver side, so a captured ciphertext can be replayed.

**Status: VERIFIED**

- `core/shared-crypto/src/antimatter_crypto/e2ee.py` **lines 62–74** (encrypt side):
  ```python
  self._msg_counter += 1
  aad = f"{direction}:v1:msg_id:{self._msg_counter}".encode()
  ```
- `core/shared-crypto/src/antimatter_crypto/e2ee.py` **lines 77–90** (decrypt side):
  ```python
  aad = envelope["aad"].encode()
  if not aad.startswith(expected_direction.encode()):
      raise ValueError(...)
  ```
  The receiver only verifies the **direction prefix** of the AAD. It does **not** check whether the `msg_id` in the AAD is strictly greater than the last seen one, meaning a captured valid ciphertext can be replayed verbatim and will decrypt successfully.

  Android confirms the same pattern at `android/core/network/src/main/java/dev/saifmukhtar/antimatter/core/network/E2EESession.kt` **line 95–96**:
  ```kotlin
  if (!aad.startsWith(expectedDirection)) {
      throw IllegalArgumentException(...)
  }
  ```
  Only prefix check, no counter-based validation.

---

## 6. No Forward Secrecy on Gateway (AM-023, AM-043)

**Claim:** The Gateway uses a static, persisted X25519 private key for all ECDH handshakes. If this key leaks, all past intercepted sessions can be decrypted.

**Status: VERIFIED**

- `core/gateway/src/antimatter_gateway/server.py` **lines 37, 46–48**:
  ```python
  self.e2ee = E2EESession(role="gateway", private_key_b64=self.config.gateway_priv_x25519)
  ...
  self.config.gateway_priv_x25519 = self.e2ee.private_key_b64
  needs_save = True
  ```
  The Gateway loads a persisted private key from the config on startup and saves a newly generated one if none exists. This key is **reused across all sessions**.

  Contrast with Android (`E2EESession.kt` **line 16**):
  ```kotlin
  private val keyPair = KeyPairGenerator.getInstance("X25519").generateKeyPair()
  ```
  The mobile client generates a **fresh ephemeral keypair per session**, so the Gateway is the sole SPOF for forward secrecy.

---

## 7. Plaintext Commands Logged to stdout (New Finding)

**Claim:** The Gateway logs the full plaintext decrypted command payload to stdout at INFO level.

**Status: VERIFIED**

- `core/gateway/src/antimatter_gateway/server.py` **line 148**:
  ```python
  logger.info(f"Decrypted command: {parsed_cmd}")
  ```
  After decryption, the full plaintext command dict (which may include shell input, file paths, AI prompts) is logged at INFO level. On any system with log aggregation or forwarding (e.g., `journald`, `systemd` service logs), this data is written to disk in plaintext. The report did not specifically highlight this but it is a real, confirmed finding.

---

## 8. Ed25519 Signature Logged (New Finding)

**Claim:** The Gateway logs the Ed25519 challenge value and its generated signature to stdout.

**Status: VERIFIED**

- `core/gateway/src/antimatter_gateway/server.py` **lines 102–104**:
  ```python
  logger.info(f"Received challenge: {challenge}")
  logger.info(f"Generated signature: {sig}")
  ```
  The base64-encoded challenge and signature are logged at INFO level. While this does not expose the private key directly, logging authentication material is poor practice and may aid forensic replay analysis.

---

## 9. Missing Input Validation on PTY Input (AM-009)

**Claim:** PTY input is accepted and written directly to the spawned shell with no validation, blocklist, or sanitization.

**Status: VERIFIED**

- `core/gateway/src/antimatter_gateway/pty_manager.py` **lines 81–89**:
  ```python
  def write_input(self, session_id: str, data_b64: str):
      ...
      data = base64.b64decode(data_b64)
      self.sessions[session_id].write(data)
  ```
  The only processing is a `base64.b64decode`. There is no command parsing, no blocklist check, no rate limit per session, and no validation of the decoded bytes before they are written directly to the PTY.

---

## 10. Headless Vault Encrypted with Weak Password Fallback (New Finding)

**Claim:** In headless environments, secrets are encrypted with a user-supplied password using PBKDF2-SHA256 (100 000 iterations). If a weak password is chosen, the vault can be brute-forced.

**Status: VERIFIED**

- `core/shared-config/src/antimatter_shared_config/secure_store.py` **lines 43–49**:
  ```python
  kdf = PBKDF2HMAC(
      algorithm=hashes.SHA256(),
      length=32,
      salt=salt,
      iterations=100000,
  )
  ```
- `core/shared-config/src/antimatter_shared_config/secure_store.py` **line 55**:
  ```python
  password = os.environ.get("ANTIMATTER_MASTER_PASSWORD")
  ```
  If the env var is not set, the user is interactively prompted. There is no minimum password length enforcement, no complexity requirement, and no lockout policy. 100 000 PBKDF2-SHA256 iterations is below the 2023 OWASP recommendation of 600 000 for password storage.

---

## 11. Backpressure Drop — PTY Output Silently Discarded (New Finding)

**Claim:** Under load, PTY output frames are silently dropped, causing the mobile terminal to lose data with no error feedback.

**Status: VERIFIED**

- `core/gateway/src/antimatter_gateway/pty_manager.py` **lines 50–51**:
  ```python
  except asyncio.QueueFull:
      logger.warning(f"Backpressure! Dropping output frames for {session_id}")
  ```
  The queue has a hard cap of 1 000 frames (`maxsize=1000`, line 42). When full, frames are dropped with only a `WARNING` log. The mobile client receives no `PTY_OUTPUT_DROPPED` message and has no way to detect data loss.
