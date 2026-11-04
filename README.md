# AetherAgent

Android AI agent built with Kotlin, Jetpack Compose, Hilt, Room, DataStore, and Ktor.

## Features in this scaffold

- OpenAI-compatible `/chat/completions` API connection.
- Chat history persisted locally with Room.
- LLM endpoint, model, API key, and optional GitHub token stored with Preferences DataStore.
- GitHub repository search and DuckDuckGo HTML search tools registered through Hilt multibindings.
- Compose chat and settings screens.
- GitHub Actions workflow for debug APK and optional signed release APK.

## Build

Requirements: JDK 17, Android SDK 35, and Gradle 8.9.

The repository is bootstrapped with a Gradle wrapper fallback script. On the first GitHub Actions run, CI installs Gradle 8.9, generates the official wrapper JAR/scripts, and commits those generated wrapper files to `main`. After that, use:

```bash
./gradlew assembleDebug
```

## Configure the app

Open **Pengaturan** in the app and set:

- **Base URL**: default `https://api.openai.com/v1`; must be an OpenAI-compatible API base URL.
- **API key LLM**: provider API key.
- **Model**: model identifier accepted by the selected provider.
- **GitHub token**: optional token for higher GitHub API rate limits.

The app calls `{Base URL}/chat/completions`. API keys are stored in Android Preferences DataStore on-device; do not commit keys to this repository.

## Release signing

Add these GitHub Actions repository secrets:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Without the keystore secrets, CI builds and uploads the debug APK only. Release builds use the configured release keystore when the secrets are present.

## Notes

The initial scaffold keeps `domain/agent_tools` as requested. Tool classes are registered in the dependency graph; tool execution is exposed through `ToolRegistry` and is not yet automatically orchestrated by the chat completion loop.
