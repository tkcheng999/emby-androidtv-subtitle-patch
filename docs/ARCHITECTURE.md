# Architecture and source contracts

This document describes the shipped helper code, not a proposed rewrite. All host-class names and private fields below are coupled to the exact APK hash in the README.

## 1. Data flow

```mermaid
sequenceDiagram
    participant UI as Native library/detail UI
    participant Server as Emby-compatible server
    participant Controller as PlaybackController
    participant Player as Native ExoPlayer
    participant Patch as Bridge / Policy / Selector
    UI->>Server: Library and PlaybackInfo requests
    Server-->>Controller: Source and possibly incomplete MediaStreams
    Controller->>Player: Open existing authorized playback stream
    Player-->>Controller: READY; actual mapped text tracks available
    Controller->>Patch: prepare(controller)
    Patch->>Player: Inspect supported renderer/group/track tuples
    Patch->>Patch: Combine server candidates with needed player entries
    Patch-->>Controller: Default choice and native menu entries
    Controller->>Patch: select(controller, index)
    alt Player-only index >= 10000
        Patch->>Player: Exact SelectionOverride
        Patch->>Controller: Keep server-bound subtitle state cleared
    else Official server index or Off
        Patch-->>Controller: Preserve native switch path
    end
```

No new network client or credential store is added. Remote media retrieval still belongs to the original application. The patch cannot make an unreachable private media URL reachable.

## 2. Three distinct index spaces

Never treat these as interchangeable:

1. **Server subtitle index:** used in metadata, PlaybackInfo and playback status.
2. **Player renderer/group/track tuple:** identifies a concrete text track in the active selector.
3. **Local menu index:** `Policy.BASE + ordinal`, starting at `10000`; only the patch understands it.

The original client also reserves `99` and `100` for local closed-caption behavior. `Policy.CLIENT_ONLY = 99` separates ordinary server entries from those pseudo-indices for several decisions. This follows the tested client's assumptions; it is not a universal Emby API rule that all real subtitle indices must be below 99.

`Bridge.select()` consumes **every** index at or above 10000, including stale or invalid ones. It must never allow such an index to fall through to the original method, whose early statements write the index into server-bound options. A valid player selection sets `mCurrentOptions.subtitleStreamIndex = -1` and clears `mCurrentStreamInfo.subtitleStreamIndex`. Static inspection of the tested client's progress reporting also found that indices at or above 99 are omitted. These defenses are not substitutes for rechecking the protocol if porting to another version.

## 3. Hook inventory

| Target | Injected helper / change | Purpose |
|---|---|---|
| `PlaybackController.addCCStreamIfNeeded()` | `Bridge.prepare(this)` | Discover tracks at the existing initial prepared/READY path |
| `PlaybackController.switchSubtitleStream(int)` | `Bridge.select(this, index)` | Route player-only IDs locally; preserve native server/Off path |
| `PlaybackController.getSubtitleStreamIndex()` | `Bridge.current(this)` | Expose active local index to native menu selection |
| `MediaSelectorSpinner.lambda$performClick$2...` | `Bridge.preplayClick(this, rowPosition)` | Capture an explicit popup-row click rather than initialization |
| `VideoManager.enabledSubtitleTrack(List,int,boolean)` | `Bridge.selectExternal(...)` | Resolve server external-file index to actual merged child/group |
| `SubtitleDecoderFactory$1.supportsFormat(Format)` | Recognize `application/pgs` | Advertise a decoder already constructed by `createDecoder` |
| `SubtitleView` (optional) | `Diagnostics.cues(...)` | Count the first 12 non-empty cue events when `EMBY_TRACE_CUES=1` |
| Manifest / provider constants | Independent package, permissions, authorities and app label | Coexistence without permission/provider identity collisions |

Each injection expects specific method signatures and register declarations. Exact previously injected text is skipped. Missing or unexpected structures fail rather than being blindly patched. The build also rejects any original APK whose digest differs from the pinned one.

## 4. Discovery and menu population

`Bridge.prepare()` clears stale per-prepare data, drops a leftover synthetic default, strips previously injected entries, and reads the **current** `VideoManager.mExoTrackSelector`.

It skips non-native playback, active burned-in subtitles, live TV, and unavailable mapped tracks. Text renderers have type `3`. Tracks are accepted when the tested client reports support `4` or its custom supported value `7`. CEA-608/708 continue on the original closed-caption path.

When external subtitles are merged, media child `0` is the video and subsequent children are external subtitle sources. Player discovery skips those external children so they are not exposed again as new embedded tracks.

Player entries are exposed when:

```text
supported player embedded-track count > server embedded-track count
```

For each exposed player track the patch creates:

- a `SubtitleStreamInfo` entry for the native subtitle menu;
- a lightweight `MediaStream` marker used by the overlay's “has subtitles” check;
- a stored tuple and a logical key built from language, label, MIME and duplicate occurrence.

The suffix `[Player]` distinguishes locally discovered entries. PGS entries use the native format label `pgssub` so irrelevant text-position controls are not offered.

**Heuristic limitation:** partial metadata can produce both server entries and a full set of player entries. Equal counts do not prove metadata is accurate. This is count-based exposure, not a complete semantic reconciliation engine.

## 5. Automatic selection policy

`Selector.score()` normalizes case and `_` to `-` in language tags. It recognizes Chinese tags including `zh`, `chi`, `zho`, `cmn`, `yue`; Chinese labels and script markers; English `en`, `eng` and common labels.

| Signal | Score effect |
|---|---:|
| Recognized Chinese | Base 1000 |
| Chinese forced flag or `forced` label | −500 |
| Chinese-dub-specific label with known non-Chinese audio | −200 |
| Simplified language/script/label | +100 |
| Traditional language/script/label | −50 |
| Recognized English | Base 100 |
| English forced flag or `forced` label | −30 |
| Neither recognized | −1, not selected by this policy |

Thus a full traditional subtitle can outrank forced simplified, and even penalized Chinese remains ahead of English. The algorithm does not read subtitle text to determine script or completeness. Equal scores keep the first candidate encountered; there is no explicit SRT/ASS/PGS quality ranking.

`Policy.choose()` first honors a matching explicit manual choice. Otherwise it scores auto-eligible candidates. Server entries requiring `Encode` delivery are not newly auto-selected because that can trigger a burn-in transcode. If no eligible Chinese or English candidate exists, the original default survives (after any stale synthetic default is cleared).

The `requested` parameter remains in the policy interface for context, but a nonnegative value is **not** treated as evidence of a manual choice: ordinary details-page defaults also populate it.

## 6. Manual choices and lifecycle

### During playback

A `WeakHashMap` keyed by controller stores the current subtitle-list identity, plain track descriptions, selected local index, pending automatic index, item ID and manual choice. Values do not retain the player or selector. The live selector is fetched again for each application.

`pendingAuto` distinguishes the expected automatic switch callback from a menu selection. Manual Off follows the original negative-index path. A player choice is remembered by its logical track key rather than a renderer ordinal. A different item ID resets manual state. This is in-memory session behavior, not a persistent per-film preference database.

Before a local override is applied, renderer type, bounds, support, language, label and MIME must still match the live track. The chosen text renderer is enabled; other text renderers are disabled and their overrides cleared.

### Before playback

Native details-page code inherits a default index from the server, and spinner initialization can resemble a selection callback. Merely seeing a requested index or `onItemSelected` is therefore insufficient.

The patch hooks the actual popup-row click. It verifies this is the subtitles selector, resolves the selected server index, and stores a one-use choice with item/source IDs and a monotonic timestamp. `prepare()` consumes it only when item and source match and it is younger than 10 minutes. This also represents **explicit Off**, which the original details intent otherwise omits because it only carries nonnegative indices.

Only one pending preplay choice is stored. A mismatched prepare consumes and discards it. Audio and video-version selector clicks are ignored by this hook. In-play remembered server choices are less strongly scoped than preplay choices: they are per controller/item, not a persistent item/source preference map. Version-switch behavior beyond tested flows must be checked separately.

## 7. External subtitle mapping

The real test case contained two server-listed external Chinese files but also undeclared embedded English tracks. Selecting external server index 1 through the original ordinal logic selected an embedded English PGS track.

`selectExternal()` reconstructs the original external-source construction order using recognized formats, a non-null URL and `isExternalUrl=true`. It requires that reconstructed count match `externalSubSources.size()`. It then finds the merged child corresponding to the chosen file and applies an exact selector override. Ordinal mode, invalid indices, unknown shapes and ambiguous counts return control to native behavior.

This corrects the observed external-file mismatch. It does not rewrite all official embedded-server mappings.

## 8. Reflection, exceptions and diagnostics

Reflection avoids compiling the production helper against fake Emby classes. Field lookup walks superclasses; method lookup checks arity and compatible boxed parameter types. This remains brittle across private-field renames and application updates.

Failures are logged and guarded. Preparation attempts to remove injected entries and stale defaults on failure; selection still consumes invalid synthetic IDs. “Guarded” does not mean all failures restore ideal playback state.

Diagnostics log track labels and exception messages as well as numeric information. Optional cue instrumentation does not log subtitle text, but **raw logs still require privacy review** because metadata labels or exception messages can contain sensitive strings. Never commit device logcat wholesale.
