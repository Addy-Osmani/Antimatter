# Unverified Security Claims

These claims highlight legitimate architectural risks, missing features, or third-party dependency risks, but they cannot be tied to a specific line of code in the repository.

## Architectural Risks
1. **Cloudflare Metadata Leakage & TLS Termination [AM-053, AM-003, AM-064]**: Cloudflare acts as a reverse proxy, meaning it terminates TLS and inherently has access to client metadata (IP address, User-Agent). However, the `cloudflared` tunnel is run externally, and there is no `tunnel.py` script in the repo managing it.
2. **Single Point of Failures [AM-080, AM-081]**: The Gateway daemon and the Cloudflare Tunnel serve as single points of failure (SPOF) for remote access capabilities. This is an architectural observation rather than a code defect.
3. **No Session Isolation / Resource Limits for PTY [AM-076, AM-077]**: The PTY manager does not use namespaces or cgroups to enforce memory, CPU limits, or session isolation.

## Missing Features / Testing
4. **No Integration Tests [AM-083]**: There are no full end-to-end integration tests for the Mobile ↔ Gateway ↔ Adapters workflow.
5. **No Log Rotation [AM-017, AM-044]**: The gateway application logs directly to `stdout`. Therefore, log file rotation is an external deployment and ops concern rather than an application code defect.
6. **No Dependency Version Pinning [AM-084]**: Claim states dependencies are not pinned, which requires verification in packaging files but refers to a configuration/maintenance risk rather than active runtime code.
