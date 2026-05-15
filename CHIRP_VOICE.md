# Chirp Voice Input (OpenRouter STT)

LeanType Chirp includes **direct speech-to-text** using the OpenRouter audio/transcriptions endpoint. It uses the existing microphone key on the keyboard: tap once to start recording, tap again to stop and transcribe. No auxiliary IME, no keyboard switching, no background listening.

## How it works

- Audio is recorded locally in PCM, encoded to WAV, then base64-encoded.
- The encoded audio is sent to `https://openrouter.ai/api/v1/audio/transcriptions`.
- The transcription text is committed directly into the current input field.
- Audio is sent only after you explicitly tap the mic key again to stop recording.

## Setup

1. Go to **Settings → AI Integration**.
2. Enable **Enable OpenRouter voice input**.
3. Enter your **OpenRouter API key**.
4. (Optional) Change the **STT model**; default is `google/chirp-3`.

## Permissions

The app requests `RECORD_AUDIO` at install time. On Android 6+ you must grant the permission when prompted, or allow it in system Settings → Apps → LeanType Chirp → Permissions.

## Usage

1. Make sure the mic key is visible (it appears when Chirp voice is enabled, or when the system voice input key is enabled).
2. Tap the mic key — a toast says "Listening… tap mic again to stop".
3. Speak.
4. Tap the mic key again — the app shows "Transcribing…", then inserts the text.

## Build notes

- The feature is only compiled into the **standard** flavor (`standardImplementation` is not used; the code is in `main`).
- OkHttp and Kotlinx Coroutines are added as dependencies in `app/build.gradle.kts`.
- No additional manifest entries are needed beyond `RECORD_AUDIO`.

## Privacy

- Audio is **not** stored locally after transcription.
- Only the WAV base64 payload is sent to OpenRouter.
- Review [OpenRouter privacy policy](https://openrouter.ai/privacy) for details.
