### 💖 Support Our Work
As an open-source, community-funded project, we operate on a very limited budget and have little time for marketing. If LeanType Chirp helps you daily, please consider becoming a [sponsor](https://github.com/sponsors/LeanBitLab). Even if you can't contribute financially, sharing LeanType Chirp with your friends, family, or on social media makes a world of difference. Thank you for your support!

## 🚀 What's New in v1.0.9

### 🔄 Upstream Sync
- **Synced to LeanType 4.1.0**: Merged upstream/main through all LeanType 4.1.0 improvements, including the inline clipboard history item editing, searchable preference & language dialogs, foldable mode toggle restoration, physical keyboard toolbar exemption, and handwriting engine crash fixes.
- **Toolbar & Clipboard Auto-Spanning**: Unified key auto-spanning across suggestion and clipboard toolbars with graceful square-key fallback and horizontal scrolling when keys exceed container width.

### 🤖 Chirp Preservation
- **Chirp Voice Integration Kept**: Custom microphone transcription via the Chirp STT client, its settings, and fork branding are fully preserved through the upstream merge.

### 🧹 AI Request Cleanup
- **Removed Temperature & Output Limits**: Dropped `temperature` and `maxOutputTokens` from AI proofreading `generationConfig` so providers choose their own defaults instead of forcing tuning fields on every request.

### 🐛 Stability
- **Clipboard Crash Synchronization**: Clipboard history crash synchronization already carried in the fork is retained and verified against the synced upstream clipboard refactor (`ClipboardHistoryView`, `ClipboardHistoryManager`, `ClipboardDao`).

## 📦 Download

Download one signed Standard APK:

`1-LeanType-Chirp_1.0.9-standard-release.apk`

Includes Cloud AI, Handwrite, and Chirp. Requires Internet permission.

Android rejects unsigned APKs with `INSTALL_PARSE_FAILED_NO_CERTIFICATES`; release APKs must be signed before installation.