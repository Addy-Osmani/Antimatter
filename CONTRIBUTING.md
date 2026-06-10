# Contributing to Antimatter

Welcome to the Antimatter ecosystem! Antimatter is an open-source companion app and VSCode extension for Google's AntiGravity IDE.

**Important Note:** Antimatter is a community-driven project and is **NOT** affiliated with, endorsed by, or supported by Google.

## Getting Started

To contribute to Antimatter, you will need to set up both the VSCode Extension and the Android application.

### Prerequisites
* Node.js (v22+)
* Android Studio (Koala or newer)
* Google AntiGravity IDE

### Setting Up the VSCode Extension
1. Navigate to the `extension` directory.
2. Run `npm install` to grab the dependencies.
3. Run `npm run watch` to start the TypeScript compiler in watch mode.
4. Press `F5` in VSCode to launch a new Extension Development Host.

### Setting Up the Android App
1. Open the `android` directory in Android Studio.
2. Let Gradle sync and download the dependencies.
3. Build and run the `app` configuration on your emulator or physical device.
4. *(Optional for Crashlytics)* To enable crash reporting locally in debug builds, ensure you have a valid `google-services.json` file in `android/app/`.

## Submitting Pull Requests
1. Fork the repository and create your branch from `main`.
2. Ensure you have run formatting and linting:
   * Extension: `npm run lint`
   * Android: `./gradlew lintDebug`
3. If you've added code that should be tested, add tests.
4. Issue that pull request!

## Code Style
We enforce standard TypeScript formatting (ESLint/Prettier) and Kotlin formatting (Ktlint). Please ensure your IDE is configured to respect these rules.
