---
title: Testing Status
description: Live tracking of tested features and connectivity.
---

# Antimatter Testing Status & Known Issues

This document tracks the current testing status of the Antimatter ecosystem, outlining known bugs, platform limitations, and areas requiring further optimization.

## Testing Environment

- **Platform Tested**: Android App **ONLY**
- **Note**: The iOS app remains completely untested at this time due to the non-availability of an iOS testing device.

---

## Feature Status

### 1. Gateway & Connection

- **Status**: Tested
- **Functionality**: The persistent WebSocket connection between the local gateway and the Android app establishes successfully.
- **🚨 Known Issues**:
  - The connection drops abruptly when the Android app is sent to the background. Background service persistence needs to be implemented.

### 2. Workspace Browser

- **Status**: Tested
- **Functionality**: Basic directory parsing and file viewing work as expected.
- **⚠️ Needed Improvements**:
  - **Multi-Workspace Selection**: Currently lacking support for seamlessly switching between multiple active workspaces.
  - **Comprehensive File Support**: Needs proper UI support for viewing all file types robustly within the Android app.

### 3. Native PTY Terminal

- **Status**: Tested
- **Functionality**: Core terminal functioning (shell execution, basic I/O) is stable.
- **⚠️ Needed Improvements**:
  - Requires significant performance optimization.
  - Proper UI/UX support and polishing are needed to bring it up to standard.

### 4. Biometric Auth Gates

- **Status**: Tested
- **Functionality**: Fingerprint unlock integration is perfectly stable and working as expected.
- **Known Issues**: None.

### 5. Cloudflare Tunnels

- **Status**: Tested
- **Functionality**: Permanent Cloudflare domain tunnels (`cloudflared tunnel run <name>`) work perfectly.
- **🚨 Known Issues**:
  - **Trycloudflare (Quick Tunnels)**: Currently broken. The temporary `trycloudflare.com` tunnels fail because the URL changes unexpectedly during the authentication challenge and response sequence.

### 6. All Other Features

- **Status**: Untested
- **Note**: Any features not explicitly listed above (e.g., remote prompting) have not been formally tested yet. We cannot guarantee their stability or working order at this time.

---

## Reporting Issues

Community testing is vital! If you test these features and find any unexpected behavior, or if you manage to test the untried features (especially on iOS), it is highly encouraged to report the issues. Your feedback helps stabilize Antimatter!
