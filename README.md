# Emby Android TV — player-discovered subtitles

![Emby free subtitle banner](branding/app_icon.png)

A version-pinned, client-side patch for **Emby for Android TV 2.5.03g**. It exposes embedded subtitles discovered by the player when the server's media metadata is incomplete, selects Chinese with a preference for simplified Chinese, and falls back to English. It retains the native library and playback UI.

**This is the patch source, not the complete Emby application source.** You supply the matching original APK and your own signing key. No APK, media library, account, token, server address or signing private key is included.

**[完整中文项目报告 →](docs/PROJECT_REPORT.zh-CN.md)**

## What works, and where the boundary is

| Capability | Result |
|---|---|
| Discover subtitles inside an already playable stream | Implemented; tested on metadata-empty remote MKV playback |
| Chinese first, simplified preferred, English fallback | Implemented and tested; full subtitles are preferred to forced tracks within each language policy |
| Choose or turn off subtitles during playback | Implemented through the native subtitle menu; local entries have `[Player]` suffixes |
| Choose before playback | Supported when the server already lists subtitle tracks; explicit choices override automatic selection |
| Metadata-empty subtitle selection before playback | Not implemented; tracks are available after the player reads the stream |
| Server-listed external SRT | Existing path retained, with a fix for merged-track index mismatch |
| PGS bitmap subtitles | Existing decoder enabled in the tested APK's format-support check |
| Server configuration / library rescan | Not required; not changed by this patch |
| Arbitrary seek / transcoded MKV or TS restarts | Not fully validated; documented risk remains |

Chinese preference is a weighted policy, not a guarantee of perfect script identification or subtitle completeness. If neither Chinese nor English can be recognized, the original default is retained. No subtitle downloading, extraction to files, transcription or translation is performed.

## Start here

- **Understand the work:** [detailed Chinese report](docs/PROJECT_REPORT.zh-CN.md)
- **Understand the source:** [architecture and hook contracts](docs/ARCHITECTURE.md)
- **Build and install your copy:** [build instructions](docs/BUILD.md)
- **Check the evidence:** [testing and limitations](docs/TESTING.md), [artifact hashes](docs/ARTIFACTS.json)
- **Understand review decisions:** [review disposition](docs/REVIEW.md)
- **Continue as an agent:** [AGENTS.md](AGENTS.md)
- **Protect private information:** [publication and security notes](SECURITY.md)

### Run tests without an APK, account or television

Requirements: JDK 17, Python 3, a POSIX shell.

```sh
sh tests/run_host_tests.sh
```

Expected: **19 selector checks + 15 policy checks + 37 bridge checks**, then **6 Python tests**. Host tests use hand-written fakes and minimal smali fixtures. They are not an Android emulator and cannot prove that a particular APK or television works.

### Source map

```text
src/local/emby/subtitles/
  Selector.java       language and subtitle-label scoring
  Policy.java         default selection and explicit manual choices
  Bridge.java         native-player reflection bridge and index isolation
  Diagnostics.java   optional cue-count instrumentation
patch.py             version-specific smali/manifest injection
build.sh             clean decode → patch → helper DEX → align → sign
branding/app_icon.png approved TV banner
tests/              host tests, fakes and synthetic structural fixtures
docs/               report, architecture, build, evidence and review notes
```

## Tested baseline

- Android TV client: **2.5.03g**, original package `tv.emby.embyatv`.
- Original APK SHA-256: `0246821852b9a7b6e1448f751601c0040c1f3a9de80c4ea778fb2ba67a16d147`.
- Independent patched package: `local.emby.subtitletest`; display name **Emby 字幕测试版**.
- Original device: Hisense/VIDAA, Android 11. This is the tested device, not a claim of cross-device compatibility.
- Device checks used post-2000 Western films: *Annihilation* (2018), *Hotel Artemis* (2018), *Proxima* (2019). A failed probe is also recorded in the report.
- The owner subsequently tested on the television and reported that it worked. This is user acceptance, not an automated all-library test.

The two superseded Emby clients and Kodi were removed only after user acceptance and explicit requests. Uninstalling them is **not** a prerequisite for using this independent package.

## Scope and attribution

This is an independent experimental patch, not an official Emby release. Emby names and marks identify the target product; the custom banner does not imply endorsement. The phrase **“free subtitle” is a visual label**, not a bypass of paid functionality or a subtitle service.

The repository contains project-authored helper code, patch instructions, test fakes, and documentation. Full decompiled classes, upstream APKs, device screenshots, raw logs, private review-session exports and credentials are deliberately excluded. No open-source license has been selected for this repository; public visibility alone does not grant a blanket license to this work or to upstream Emby code.
