# AGENTS.md — LeanType Chirp

Instructions for AI agents (OpenCode, Claude Code, etc.) working on this repo.

## Project Identity

- **LeanType Chirp** is a fork of [LeanType](https://github.com/LeanBitLab/HeliboardL).
- LeanType is a fork of [HeliBoard](https://github.com/Helium314/HeliBoard) → [OpenBoard](https://github.com/openboard-team/openboard) → AOSP LatinIME.
- Package: `com.leantypechirp.keyboard`
- License: GPL-3.0-only

## Core Rules

- **Never push to upstream.** Origin is `AutomatorAlex/leantype-chirp`. Upstream remote (`LeanBitLab/LeanType`) is for reference only.
- **Never commit secrets.** API keys, keystores, `local.properties`, and captured audio must never be committed. `.gitignore` covers `local.properties`, `*.keystore`, `keystore.properties`.
- **Never add telemetry or background recording.** All network use must be explicit user action.
- **Never add an auxiliary IME.** The whole point of this fork is direct LeanType mic integration — no keyboard switching.

## Chirp STT Architecture

The mic key in LeanType fires `KeyCode.VOICE_INPUT` which is intercepted in `LatinIME.onEvent()`:
- If `ChirpVoiceController.isEnabled()`, route to `toggleRecording()` and **return**.
- Otherwise, fall back to stock `switchToShortcutIme()`.

Key files:
| File | Role |
|------|------|
| `app/…/LatinIME.java` | Hooks mic key, creates/destroys controller |
| `app/…/chirp/ChirpVoiceController.kt` | State machine: IDLE → RECORDING → TRANSCRIBING |
| `app/…/chirp/audio/AudioRecorder.kt` | PCM capture, silence detection, WAV output |
| `app/…/chirp/audio/WavEncoder.kt` | PCM → 44-byte WAV header |
| `app/…/chirp/network/OpenRouterSttClient.kt` | POST to OpenRouter `/audio/transcriptions` |
| `app/…/chirp/settings/ChirpPreferences.kt` | API key / model / enabled flag (credential-protected prefs) |
| `app/…/suggestions/SuggestionStripView.kt` | Mic visibility (`updateVoiceKey`) + red tint (`setVoiceKeyRecording`) |

## Settings

Chirp settings live under **Settings → AI Integration** (NOT Advanced). The screen is `AIIntegrationScreen.kt`. Settings keys in `SettingsContainer.kt`: `CHIRP_VOICE_ENABLED`, `CHIRP_API_KEY`, `CHIRP_MODEL`.

## Build & Test

```bash
# Build debug APK
./gradlew :app:assembleStandardDebug

# Output
app/build/outputs/apk/standard/debug/1-LeanType-Chirp_<version>-standard-debug.apk
```

Install and test on device:
```bash
adb install -r app/build/outputs/apk/standard/debug/1-LeanType-Chirp_*-standard-debug.apk
adb shell pm grant com.leantypechirp.keyboard android.permission.RECORD_AUDIO
adb shell ime enable com.leantypechirp.keyboard/helium314.keyboard.latin.LatinIME
adb shell ime set com.leantypechirp.keyboard/helium314.keyboard.latin.LatinIME
adb shell am start -n com.leantypechirp.keyboard/helium314.keyboard.settings.SettingsActivity
```

**Test checklist:**
1. Enable Chirp voice in Settings → AI Integration, set OpenRouter key + model.
2. Open normal text field, tap mic once → recording starts, mic turns red.
3. Tap mic again → transcribing, text inserts. Or wait 3s silence → auto-transcribes.
4. Verify no keyboard switch, no auxiliary IME.

## Secrets Audit

Run before any release:
```bash
grep -rI "sk-or-\|sk-ant-\|gsk_\|AIza\|ghp_\|gho_" --include="*.{kt,java,xml,kts,properties,md}" . | grep -v build/ | grep -v .gradle/
grep -rIE "[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}" --include="*.{kt,java,xml,kts,md,yaml,yml,gradle,properties}" . | grep -v build/
```

## Release

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. `./gradlew :app:assembleStandardDebug`
3. Commit, tag, push.
4. Use `gh api` to create release + upload APK (or `gh release create` if token has workflow scope).
5. APK filename must match the pattern in `com.leantypechirp.keyboard.yml` for F-Droid auto-updates.
