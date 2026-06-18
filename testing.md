# Antimatter Testing & Bug Tracking

## Known Leftover Bugs & Tasks

### 1. Legacy Conversations Visibility
- **Issue**: The AG2 adapter only reads conversations that use the new `transcript.jsonl` format. 
- **Impact**: There are ~14 older conversations in the `.system_generated/messages/` legacy format that do not appear in the sidebar and cannot be accessed.
- **Action Required**: Add a fallback or migration logic in `AgentBridge` to parse and serve the older legacy messages if `transcript.jsonl` does not exist.

### 2. Publishing to Open VSX
- **Issue**: Need to finalize stability and readiness for publishing the VSCode extension.
- **Impact**: The extension is not yet published under `antimatter-saifmukhtar-dev`.
- **Action Required**: Run final integration tests, package the VSIX, and push to the Open VSX registry.

### 3. User-Reported Unspecified Bugs
- **Issue**: User mentioned finding 2-3 additional bugs during the last testing session.
- **Impact**: Pending details from the user.
- **Action Required**: User to list the specific UI/UX or adapter bugs here for the next session.

---

*(Add new bugs below this line)*
