# Agent handoff

Read README.md, docs/PROJECT_REPORT.zh-CN.md, docs/ARCHITECTURE.md, docs/TESTING.md and docs/BUILD.md first.

This is a version-pinned patch, not the full Emby application source. The application logic in src/ and patch.py is the tested baseline. Do not update hooks for a different APK without new static and runtime evidence.

- Never add credentials, account details, private keys, real server URLs, device addresses, raw logs or original APKs to Git.
- No changes to login, payment/license checks, server configuration, or media library scanning.
- Never forward synthetic subtitle IDs (>=10000) to server-bound playback state.
- Preserve explicit manual Off and subtitle choices; Chinese first, simplified preferred, English fallback.
- Host fakes and minimal smali fixtures are tests only; never ship them in an APK.
- Host tests: sh tests/run_host_tests.sh (JDK 17 and Python 3).
- Native build needs the exact original APK hash and a local signing key; see docs/BUILD.md.
- Do not claim host tests prove on-device behavior, all formats, full dialogue coverage or seeking.
- Device validation must be separately authorized for the target device. The historical test owner required post-2000 Western films and excluded Harry Potter.
- Do not delegate to Opus; the project owner has prohibited further usage due to quota.
- Keep the privacy boundary when adding screenshots; a launcher screenshot can contain Wi-Fi SSIDs even if it has no passwords.
- This repository has no selected open-source license. Do not invent rights over upstream code or Emby trademarks.
