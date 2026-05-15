# Contributing to LeanType Chirp

Thanks for helping improve LeanType Chirp.

## What This Fork Is

LeanType Chirp is a fork of [LeanType](https://github.com/LeanBitLab/HeliboardL), which is itself a fork of [HeliBoard](https://github.com/Helium314/HeliBoard), OpenBoard, and AOSP LatinIME.

This fork focuses on optional OpenRouter Chirp speech-to-text integrated directly into the LeanType mic key.

## What Belongs Here

- Chirp STT recording/transcription fixes
- OpenRouter model selection and request handling
- Voice-input privacy, permission, and UX improvements
- Documentation for this fork
- Bug fixes introduced by this fork

## What Belongs Upstream

Please consider upstream LeanType or HeliBoard for:

- Core keyboard behavior
- Layouts, dictionaries, and translations
- Theme and gesture typing changes
- Non-Chirp AI proofreading/translation behavior

## Development Setup

Requirements:

- Android SDK 35+
- JDK 17

Build debug APK:

```bash
./gradlew :app:assembleStandardDebug
```

Install locally:

```bash
adb install -r app/build/outputs/apk/standard/debug/*LeanType-Chirp*.apk
adb shell pm grant com.leantypechirp.keyboard android.permission.RECORD_AUDIO
adb shell ime enable com.leantypechirp.keyboard/helium314.keyboard.latin.LatinIME
adb shell ime set com.leantypechirp.keyboard/helium314.keyboard.latin.LatinIME
```

## Code Guidelines

- Keep changes focused and minimal.
- Do not reformat unrelated upstream code.
- Do not commit API keys, local.properties, keystores, logs, or captured audio.
- Keep network features explicit and user-triggered.
- Store secrets only in credential-protected app storage.
- Do not add telemetry or background recording.
- Preserve GPL-3.0 license compatibility.

## Testing Checklist

Before opening a PR:

- Build `:app:assembleStandardDebug`.
- Enable Chirp voice in **Settings → AI Integration**.
- Verify mic tap starts recording.
- Verify second mic tap stops/transcribes/inserts text.
- Verify keyboard does not switch IMEs.
- Verify disabling Chirp hides or disables the custom mic behavior.
- Verify API key is not logged.

## Security

Report vulnerabilities privately. See [SECURITY.md](SECURITY.md).

## License

By contributing, you agree that your contribution is licensed under GPL-3.0.
