# LeanType Chirp v1.0.11

## What's New

### Upstream Sync

- Synced to upstream LeanType v4.1.2 improvements, including the latest keyboard, settings, and stability changes.

### Offline Voice

- Added offline voice integration for the external `com.leanbitlab.leantype.voice.offline` engine, with language selection and in-toolbar waveform visualization. The Standard APK does not bundle the offline engine or model; offline voice requires separate engine plugin and model installation.

### Chirp Preservation

- Preserved optional OpenRouter/Requesty Chirp speech-to-text integration in the LeanType microphone key.

### Stability

- Fixed a crash that could occur when clipboard or screenshot suggestions render while the active keyboard is unavailable; suggestions now safely omit toolbar icons until a keyboard is present.
- Fixed a startup crash after reboot before the device is unlocked: Chirp voice input now waits until credential-protected preferences are available, then initializes normally after unlock.

## Download

Signed Standard APK:

`1-LeanType-Chirp_1.0.11-standard-release.apk`

Includes Cloud AI, Chirp, and offline voice integration. Requires Internet permission. Offline voice requires separate installation of the `com.leanbitlab.leantype.voice.offline` engine plugin and model.

Install with Android package installer or:

```bash
adb install -r 1-LeanType-Chirp_1.0.11-standard-release.apk
```

Do not install unsigned or incomplete APK files. Android rejects them with `INSTALL_PARSE_FAILED_NO_CERTIFICATES`.
