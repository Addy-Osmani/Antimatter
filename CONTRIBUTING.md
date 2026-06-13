# Contributing to Antimatter

Welcome! Antimatter is an open-source Android companion app and VS Code extension for Google's AntiGravity IDE. We're excited to have you here.

> **Note:** Antimatter is a community-driven project and is **NOT** affiliated with, endorsed by, or supported by Google.

## Table of Contents

- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Setting Up Each Component](#setting-up-each-component)
  - [VS Code Extension](#vs-code-extension)
  - [Python Daemon (Antigravity 2.0)](#python-daemon-antigravity-20)
  - [Android App](#android-app)
- [Development Workflow](#development-workflow)
- [Code Style](#code-style)
- [Running Tests](#running-tests)
- [Submitting a Pull Request](#submitting-a-pull-request)
- [Good First Issues](#good-first-issues)
- [Community](#community)

---

## Quick Start

```bash
git clone https://github.com/saifmukhtar/antimatter.git
cd antimatter
```

The repository is split into three independently buildable components:

| Directory   | Component                      | Language          |
|-------------|-------------------------------|-------------------|
| `extension/`| VS Code Extension              | TypeScript        |
| `plugin/`   | Python Daemon (AG 2.0)         | Python 3.9+       |
| `android/`  | Android Companion App          | Kotlin / Compose  |

---

## Project Structure

```
antimatter/
├── extension/          # VS Code Extension (TypeScript)
│   ├── src/
│   │   ├── core/       # Network, state, data helpers
│   │   └── feature/    # Terminal, chat, files, connect handlers
│   └── package.json
├── plugin/             # Python bridge daemon (AntiGravity 2.0)
│   ├── antimatter_bridge/
│   │   ├── server.py   # WebSocket server & auth
│   │   ├── agent_bridge.py  # Transcript polling & message parsing
│   │   └── auth.py     # Ed25519 key management
│   └── pyproject.toml
├── android/            # Android app (Kotlin / Jetpack Compose)
│   ├── app/            # Application module
│   ├── core/           # Shared: network, data, UI
│   └── feature/        # Screen-level feature modules
└── docs/               # Documentation site (MkDocs)
```

---

## Setting Up Each Component

### VS Code Extension

**Prerequisites:** Node.js 22+, VS Code 1.85+

```bash
cd extension
npm install
npm run watch          # TypeScript compiler in watch mode
```

Press **F5** in VS Code to launch an Extension Development Host with your changes live.

**Available scripts:**

| Script          | Description                          |
|-----------------|--------------------------------------|
| `npm run watch` | Compile in watch mode                |
| `npm run lint`  | TypeScript type-check (0 errors required) |
| `npm test`      | Run Jest unit tests                  |
| `npm run package` | Build `.vsix` for local install    |

---

### Python Daemon (Antigravity 2.0)

**Prerequisites:** Python 3.9+

```bash
cd plugin
pip install -e ".[test]"    # Install with test dependencies
antimatter                   # Start the daemon
```

**Running tests:**

```bash
cd plugin
pytest
```

The daemon starts a WebSocket server on `ws://localhost:8765` and optionally creates a Cloudflare Quick Tunnel for remote access.

---

### Android App

**Prerequisites:** Android Studio Koala (2024.1.1) or newer, JDK 17

1. Open the `android/` directory in Android Studio.
2. Let Gradle sync and download dependencies.
3. Build and run the `app` configuration on your emulator or a physical device (API 33+).

> **Optional:** To enable Crashlytics in debug builds, place a valid `google-services.json` file in `android/app/`.

**Build from CLI:**

```bash
cd android
./gradlew assembleDebug        # Debug APK
./gradlew lintDebug            # Lint
./gradlew test                 # Unit tests
```

---

## Development Workflow

1. **Fork** the repository and create your branch from `main`:
   ```bash
   git checkout -b feat/your-feature-name
   ```
2. **Make your changes.** Keep commits small and focused.
3. **Run lint and tests** for the component(s) you changed:
   - Extension: `npm run lint && npm test`
   - Python: `pytest`
   - Android: `./gradlew lintDebug test`
4. **Open a Pull Request** against `main`. Fill in the PR template.

---

## Code Style

### TypeScript (Extension)

- Formatting: **Prettier** (config in `.prettierrc` if present, otherwise defaults)
- Linting: **TypeScript strict mode** (`tsconfig.json` — `strict: true`, `noUnusedLocals`, `noUnusedParameters`, `noImplicitReturns`)
- Unused callback params must be prefixed with `_` (e.g. `(_msg, ws) =>`)

### Python (Plugin)

- Formatting: [Black](https://black.readthedocs.io/) (88-char line limit)
- Type hints are encouraged on all public functions

### Kotlin (Android)

- Formatting: [Ktlint](https://pinterest.github.io/ktlint/) — run `./gradlew ktlintCheck`
- Follow standard Android/Kotlin conventions

---

## Running Tests

```bash
# Extension
cd extension && npm test

# Python
cd plugin && pytest

# Android (unit tests)
cd android && ./gradlew test
```

---

## Submitting a Pull Request

1. Ensure all lint checks pass (0 TypeScript errors, clean Kotlin lint).
2. Ensure all tests pass.
3. Fill in the PR description — link to the issue it closes.
4. Request a review from `@saifmukhtar`.
5. Maintainer will rebase and merge.

---

## Good First Issues

New to the project? Look for issues labelled **`good first issue`** on GitHub. Great starting points:

- Adding a new command to the terminal allowlist (`extension/src/feature/terminal/TerminalCommandHandler.ts`)
- Adding a unit test for `AuthHandler` or `FileSystemHelper`
- Filtering a new boilerplate pattern from `BrainWatcher.ts`
- Improving error messages in the Android connection screen
- Adding a translation (`android/app/src/main/res/values-*/strings.xml`)

---

## Community

- **Discussions:** [GitHub Discussions](https://github.com/saifmukhtar/antimatter/discussions)
- **Bug Reports:** [GitHub Issues](https://github.com/saifmukhtar/antimatter/issues)
- **Author:** [saifmukhtar.dev](https://saifmukhtar.dev)

If Antimatter helps your workflow, consider [sponsoring via GitHub Sponsors](https://github.com/sponsors/saifmukhtar) to support continued development.
