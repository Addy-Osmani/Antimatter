# UNVERIFIED Security Claims

> These claims describe **legitimate architectural concerns** — real risks or missing features — but cannot be tied to a specific line of code in this repository. They are not hallucinations; they are structural or deployment-level issues that sit outside the codebase itself.

---

## 1. Cloudflare as Trusted Third-Party / TLS Termination (AM-003, AM-053, AM-064)

**Claim:** Cloudflare terminates TLS and acts as a trusted intermediary. If Cloudflare is compromised, the cleartext E2EE ciphertext is exposed at the network layer (though the E2EE payload itself remains encrypted).

**Status: UNVERIFIED (Architectural)**

- The Gateway does not manage the Cloudflare tunnel directly from within the codebase. There is no `tunnel.py` file. The Cloudflare tunnel is an **externally run binary** (`cloudflared`).
- The risk is real: Cloudflare terminates TLS on its edge, so it can see the raw WebSocket framing (though the content inside is still AES-GCM encrypted). However, there is no code line in this repo that introduces or mitigates this risk.
- The report's claim that `tunnel.py` exists and has hardcoded `--no-tls-verify` is **REJECTED** (see REJECTED.md). The architectural metadata risk (Cloudflare sees IPs/User-Agents) is real but external.

---

## 2. Gateway is a Single Point of Failure (AM-080)

**Claim:** If the Gateway process crashes or the host machine is unavailable, all remote access is lost.

**Status: UNVERIFIED (Architectural)**

- This is an architectural observation, not a code defect. The entire system is intentionally built around a single Gateway daemon. There is no clustering, failover, or redundancy mechanism.
- Cannot be pinpointed to a specific line of code. The risk is real, but fixing it requires architectural design work, not a code patch.

---

## 3. No Integration / End-to-End Tests (AM-083)

**Claim:** There are no integration tests covering the full Mobile ↔ Gateway ↔ Adapter ↔ AI chain.

**Status: UNVERIFIED (Structural Absence)**

- Unit tests for the Android connect flow exist at:
  `android/feature/connect/src/test/java/.../ConnectionViewModelTest.kt`
- However, no gateway-level integration tests that exercise the full encrypted round-trip were found in the repository.
- This is a genuine gap but it is a *missing* thing, not a *broken* thing — there is no single line to point to.

---

## 4. No PTY Session Isolation / Resource Limits (AM-076, AM-077)

**Claim:** Multiple PTY sessions share the same process namespace and have no CPU/memory cgroup limits.

**Status: UNVERIFIED (Structural Absence)**

- The `pty_manager.py` runs all PTY sessions inside the same Python process, sharing the same user namespace. There are no `cgroups`, `namespaces`, `rlimit` calls, or `seccomp` filters.
- This is a true architectural risk but it cannot be attributed to a single line — it is the *absence* of sandboxing infrastructure.

---

## 5. No Log Rotation (AM-017, AM-044)

**Claim:** The Gateway does not implement log rotation. Logs grow unbounded over time.

**Status: UNVERIFIED (Deployment-Level)**

- `server.py` line 208 sets up `logging.basicConfig(level=logging.INFO)`, directing logs to `stdout`.
- Log rotation in a daemon is typically handled by the process supervisor (`systemd`, `supervisord`, `Docker`). Since the codebase does not include any of these deployment configs, the risk is real but external to the code.

---

## 6. Dependency Pinning (AM-084)

**Claim:** Python dependencies are not pinned to exact versions, creating supply-chain risk.

**Status: UNVERIFIED — PARTIALLY TRUE**

- `core/gateway/pyproject.toml` uses range specifiers (`>=`), not exact pins (`==`).
- However, `core/uv.lock` exists, which means `uv` lock file does produce reproducible installs.
- Whether this fully mitigates the supply-chain risk depends on CI/CD practices not captured in the codebase itself.

---

## 7. No Jailbreak / Root Detection on iOS (AM-019)

**Claim:** The iOS app does not check for jailbreak, so on a jailbroken device the Keychain can be dumped.

**Status: UNVERIFIED (Structural Absence)**

- Grep for `jailbreak` across the `ios/` directory returned zero results.
- `KeychainManager.swift` does not use `kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly` or any jailbreak-detection heuristic.
- The risk is real on jailbroken devices, but there is no single line of code to point to — it is the absence of a mitigation.

---

## 8. No User Pairing Confirmation (AM-014)

**Claim:** Once a QR code is scanned, pairing completes automatically without a user PIN or out-of-band confirmation step.

**Status: UNVERIFIED (UX/Flow Absence)**

- The `ConnectViewModel.swift` and `ConnectionViewModel.kt` process the QR deep link and call `saveCredentials` immediately after the Ed25519 handshake succeeds, with no additional user confirmation prompt.
- This is not technically wrong (the QR scan itself is the physical proof of presence), but there is no second-factor PIN binding to the device — the risk is real in proximity-attack or screen-share scenarios.
