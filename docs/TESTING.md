# Verification ledger and limitations

Date of recorded device acceptance: **2026-10-30**, Asia/Shanghai. These are historical observations, not a continuous certification of every server or media item.

## Evidence levels

- **Static:** reverse-engineered code relationships in the pinned APK.
- **Host:** executable Java tests against fakes, plus Python text-injection tests.
- **Device:** playback on the original Android 11 television, supported by private screenshots/logs.
- **Owner:** the user's own confirmation that the working installation played correctly.

Raw device artifacts are not published because they can reveal account, library, network or device information. This repository publishes the methods, sanitized results and artifact hashes so another operator can reproduce checks on their own material.

## Automated suite

| Suite | Checks | Main coverage |
|---|---:|---|
| `SelectorTest` | 19 assertions | Chinese/script/region/label variants, simplified preference, forced/dub penalties, English fallback, unknown languages |
| `PolicyTest` | 15 assertions | Manual choices, automatic policy, stale defaults, original fallback, burn-in exclusion, count-based exposure |
| `BridgeTest` | 37 assertions | Local entries and markers, exact overrides, server-state isolation, stale IDs, recreated selectors, manual state, merged external tracks, PGS support 7, preplay choices |
| `test_patch.py` | 6 tests | Idempotence, package/provider rewriting, PGS declaration, explicit click hook, external mapping hook, unexpected shape rejection |

These counts are assertions/tests, **not 77 end-to-end playback scenarios**. Run `sh tests/run_host_tests.sh` to reproduce. The public package was tested after replacing private decompiled reference fixtures with minimal hand-written structural fixtures. This reduces the public suite's reliance on upstream code and does not turn it into a real-APK integration test. The production helper and injection script retain the tested implementation.

During development, the preplay, PGS-codec and external-mapping regressions were observed failing before their fixes and passing afterward. The public repository is the final snapshot, not a reconstructed commit-by-commit history of those experiments.

## Device matrix

The owner requested random candidates from the Western-film category with production year later than 2000 and explicitly excluded Harry Potter. The local helper used a saved allowlist to avoid accidental cross-title playback.

| Sample | Conditions | Observation | What this does not prove |
|---|---|---|---|
| *Annihilation* (2018, UK/US) | Server subtitle metadata empty; actual MKV includes ASS/PGS Chinese tracks | Earlier build: automatic simplified ASS, manual traditional, Off. Final build after cold restart: 7 text tracks, automatic simplified PGS, Chinese title translation visible | Full-film subtitle timing, dialogue coverage, all ASS effects, all renderers across devices |
| *Hotel Artemis* (2018, UK/US) | English/German PGS, no Chinese in probed file | Three tracks discovered; English chosen automatically. Bitmap cue reached visible SubtitleView; English location/date translation captured on screen | That the selected English track contains complete dialogue. Observed cues were sparse |
| *Proxima* (2019, France/Germany) | Server lists two external Chinese SRT files; stream also has embedded English SRT/PGS | Preplay Traditional selected; mismatch reproduced, then exact external merged-child mapping fixed and traditional characters observed. Preplay Off remained Off | Every external format, subtitle timing alignment, all combinations of source-version switching |
| *The Hating Game* (2021) | Candidate in random sample | Probe failed | No successful playback or subtitle claim is made |

An earlier accidental return to a stale excluded-film detail page was discarded from acceptance. The task stack was reset and the allowlist was introduced. No excluded-film result is used to substantiate this matrix.

After the above tests, the owner independently tested the TV and stated that it worked. The superseded applications were removed only afterward and only at the owner's request.

## Build and branding verification

- Fixed original APK digest checked.
- Helper compiled against Android API classes, not test stubs.
- APK rebuilt, aligned, signed and signature-verified.
- Installed APK checksum matched the intended local artifact.
- Branding update changed only `res/drawable/app_icon.png` among non-signature entries. All DEX were byte-identical to the accepted build.
- The new **free subtitle** banner was observed in the television launcher.
- The curated publication source was rebuilt from the pinned original APK with cue tracing enabled. Extracted DEX matched the installed accepted build byte-for-byte; banner pixels matched (aapt repacked the PNG bytes). This rebuild was not installed again.
- Installation used an update, not uninstall/reinstall or app-data clearing.

Historical hashes and unchanged DEX digests are in [ARTIFACTS.json](ARTIFACTS.json). A freshly signed copy with a different key will have a different APK hash.

## Not completed or not established

1. Arbitrary seeking, especially the transcoded MKV/TS path that replaces stream information without the same READY discovery path.
2. An extra final-build pause/resume loop: control was handed back to the user rather than claiming this check passed.
3. Full-film synchronization, every ASS style/font, subtitle availability throughout dialogue, all PGS variants.
4. Whole-library, all-device, all-client-version or all-server compatibility.
5. Full metadata reconciliation when the server lists only some tracks or mislabels them.
6. Persistence of manual choices across process death. Current state is in memory.
7. All source-version switches while keeping the same item and controller, especially remembered server indices.
8. Burn-in or live-TV enhancement. Those paths are intentionally not handled by discovery.
9. A strict packet capture proving absence of every possible server-side index leak. The protection is established by code analysis, targeted state tests and the verified original progress path, not a universal network trace.

## Reproducible acceptance checklist for a new device

Use a separately authorized device and content you can access. Keep account information out of issue reports.

- Record target APK digest, Android version, build-tool versions and whether cue tracing is enabled.
- Run host tests and verify the APK signature.
- Confirm the native media library loads without a bulk rescan.
- Choose a new title with embedded Chinese and incomplete server metadata; wait until actual subtitle cues should appear.
- Confirm `[Player]` entries become available, Chinese is selected automatically and visible characters appear.
- Select a different subtitle; then Off. Confirm automatic policy does not undo the explicit action.
- Use a no-Chinese title with a genuine English track; distinguish full-dialogue from forced/sign translations.
- Use a server-listed external subtitle title; choose before playback, including selecting the current default and Off.
- Test new-title transition, restart, source-version change, pause/resume and seeking separately. Record unsupported seek capability instead of forcing a pass.
- Record both success and failure. Strip URLs, tokens, IDs, SSIDs and account names from any shared evidence.

## Sanitized examples of useful diagnostics

```text
Discovered 7 player subtitle tracks; server embedded=0 exposed=true default -1 -> 10000 requested=-1 manual=false
Explicit preplay subtitle index=-1
Mapped external subtitle index=1 child=2 group=3
Rendered cues=1 bitmaps=1 ...
```

The ellipsis is explanatory, not an exact log suffix. Log selection plus a real visible cue is stronger than either alone. A positive language label or a selected menu row is insufficient proof of rendered subtitles.
