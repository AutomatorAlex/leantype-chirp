# Security Policy

## Reporting a Vulnerability

Please do **not** open a public issue for vulnerabilities.

Use GitHub's **Private vulnerability reporting / Security Advisory** flow for this repository when available. If you need another reporting path, contact the maintainer listed on the GitHub profile for this fork.

Expected response target: 7 days. Public disclosure should wait until a fix is available in a tagged release.

## Security Model

### API Key Storage

OpenRouter and other provider API keys are stored in Android credential-protected app storage. They are not stored in device-protected storage, so they are unavailable before the device is unlocked after boot.

Keys must never be logged, committed, or exported in backups.

### Network Access

Network-backed features are explicit user actions:

- **OpenRouter Chirp STT** records audio locally, encodes it as WAV/base64, and sends it to `https://openrouter.ai/api/v1/audio/transcriptions` only after you tap the mic key to stop recording.
- **Proofreading and translation** send selected text only when you press the relevant AI action.

LeanType Chirp does not add telemetry, background recording, background transcription, or anonymous usage analytics.

### Third-Party Services

Using Chirp STT sends audio to OpenRouter. Review OpenRouter's privacy policy: <https://openrouter.ai/privacy>.

Using Gemini, Groq, OpenAI-compatible, or other configured AI providers sends text to that provider. This project does not control those services.

## Supported Builds

Security fixes target the latest `main` branch and future tagged releases. Debug builds are for testing only.

## Dependency Security

Notable network dependencies:

- OkHttp 4.12.x
- Kotlin coroutines

Report dependency CVEs through the vulnerability process above.

## Disclaimer

This software is provided as-is, without warranty of any kind. See [LICENSE](LICENSE).
