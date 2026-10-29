# Static review and disposition

The project used REA/JADX evidence from the pinned shipped APK. Opus 5.5 performed static review and some patch/test edits. It was expressly prohibited from running tests, operating ADB, installing APKs, changing the server or using another model. The implementer performed runtime verification and subsequent fixes independently.

A follow-up static investigation reached its time limit; a report-only continuation summarized the existing evidence. This is not described as a completed second runtime test. The owner later prohibited further Opus delegation due to quota. No new Opus work is part of publication.

Raw review transcripts and decompilation exports are private. The following is a curated final disposition, not a verbatim reproduction of an intermediate report whose assumptions were later corrected.

| Finding | Final disposition |
|---|---|
| Stale local IDs could reach server-bound options | Every ID >=10000 is consumed locally; invalid/stale selection cannot fall through |
| Player restart recreates track selector | Retrieve the live selector; revalidate exact tuple and format before applying |
| Synthetic default survives `playInternal` | Clear it before discovery and in failure cleanup |
| Duplicate server and player entries | Count-based exposure and external-child exclusion; partial-metadata duplicates remain a documented limitation |
| Manual Off or another track overwritten on READY | Store manual choice per controller/item and distinguish the expected automatic callback |
| Multiple text renderers remain enabled | Select one text renderer, disable and clear overrides on the others |
| Player selection leaves a stale server subtitle index | Clear server-bound options/stream information after local selection |
| Weak-map value might retain player graph | Store plain track/state information, not player/selector objects |
| Re-running patch injects duplicate labels/hooks | Exact already-injected checks and failure on unexpected signatures |
| Provider identities collide in independent package | Rename manifest authorities/permissions and related provider literals |
| Language labels and normalized codes incomplete | Extend Chinese variants; add English fallback per updated requirement |
| A nonnegative request is assumed to be manual | Rejected after tracing details-page defaults. Only explicit UI choice is privileged |
| Spinner initialization mistaken for user input | Hook actual popup row click, rather than the review's initial-position heuristic |
| Explicit preplay Off lost in native intent | One-use item/source-scoped memory carries Off to prepare |
| PGS exists but not considered supported | Enable the existing decoder's MIME in supportsFormat; no decoder rewrite |
| External server index targets embedded English ordinal | Reconstruct merged external source order and select its exact child/group |
| Transcoded seek may bypass rediscovery | Unresolved; no universal seek-completion claim |

## Why the final preplay design differs from the recommendation

The intermediate review suggested comparing the current selection with its initial spinner position. That could not fully represent a user explicitly confirming the same item. Additional inspection found the popup row-click handler; it provides direct evidence of a click, including confirmation of the initial row. The final implementation uses that hook and is covered by host and device checks.

## Static evidence versus inference

The original call paths, fields, method signatures, external merge ordering and PGS factory structure were observed in decompiled code. Whether a particular subtitle rendered, a menu was usable on a remote, or an image was visible required runtime tests. Potential weak-reference cycles and untested seek paths were treated as risks rather than claimed measured failures.

Useful classes to inspect when reproducing the analysis on a privately supplied original APK:

- `PlaybackController`, its prepared/event callbacks and `startItem`/`playInternal` paths;
- `VideoManager.prepareExoPlayer`, `enabledSubtitleTrack` and selector setup;
- `FullDetailsActivity` details-row construction and play-response callback;
- `MediaSelectorBar`, `MediaSelectorSpinner` and the subtitle-list listener;
- `SubtitleDecoderFactory$1`, `SubtitleView`, `MergingMediaPeriod`;
- original progress reporting and menu item dispatch.

Review results are version-specific. The repository's synthetic fixtures capture only patch structure and cannot replace this inspection for a new APK version.
