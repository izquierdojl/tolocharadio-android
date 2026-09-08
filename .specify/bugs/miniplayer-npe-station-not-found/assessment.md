# Bug Assessment: NullPointerException in MiniPlayer when station is unavailable

- **Slug**: miniplayer-npe-station-not-found
- **Created**: 2026-09-08
- **Source**: pasted text (logcat)
- **Verdict**: valid
- **Severity**: critical

## Report (verbatim or summarized)

User reports: "si la emisora no se encuentra, se cierra app" (if the station is not found, the app closes).

Logcat shows a `FATAL EXCEPTION` with `java.lang.NullPointerException` at:
```
com.izquierdojl.tolocharadio.feature.player.PlayerUiKt.MiniPlayer$lambda$11$0(PlayerUi.kt:95)
```

A second crash also appears in the log (`IllegalStateException: The activity must be a subclass of FragmentActivity` at `MediaRouteButton.showDialogForType`), but the primary crash that causes the app to close is the NPE.

## Symptom

When a user attempts to play a station that is unavailable or unreachable, the app crashes with a `NullPointerException` in the `MiniPlayer` composable instead of showing an error state in the mini-player panel.

## Reproduction

1. Open the app
2. Navigate to a station that is currently unavailable (server down, stream URL broken, network issue)
3. Tap play on that station
4. App crashes with NPE

## Suspected Code Paths

- `PlayerViewModel.kt:116-118` — `onPlayerError` only extracts station from `PlayerState.Playing`, missing `Buffering` and `Paused` states. When ExoPlayer errors during buffering, `current` is `null`, producing `PlayerState.Error(null, ...)`.
- `PlayerUi.kt:114` — `station!!` non-null assertion. If `station` is `null` (from `playerStation` returning `null` for `Error(null, ...)`), this line crashes. While `AnimatedVisibility(visible = isVisible)` guards this block, `station!!` is a latent crash vector.
- `PlayerUi.kt:81-82` — `playerStation(state)` returns `null` when `PlayerState.Error.station` is `null`, making `isVisible = false`. The `AnimatedVisibility` should prevent the content lambda from executing, but `station!!` remains dangerous.
- `PlayerUi.kt:133` — `resolveCopyLink(station)` passes a potentially null `station` (the local val from line 81) to a function expecting non-null `StationDto`. This path is inside `AnimatedVisibility` so normally guarded, but is another unsafe access pattern.

## Root Cause Hypothesis

**Confidence**: high

The `onPlayerError` callback in `PlayerViewModel` (line 116-118) only looks for the current station in `PlayerState.Playing`:

```kotlin
override fun onPlayerError(error: PlaybackException) {
    val current = (_state.value as? PlayerState.Playing)?.station
    _state.value = PlayerState.Error(current, "Se ha interrumpido la reproducción.")
```

When ExoPlayer reports a playback error while the player is in `Buffering` state (which is the normal state when a stream is loading), the cast returns `null` because the state is not `Playing`. This creates `PlayerState.Error(null, message)`.

The `MiniPlayer` composable has a guard (`isVisible = station != null`) that should hide the panel, but the `station!!` non-null assertion at line 114 is a latent crash. The R8/D8 compiler may inline the lambda such that the NPE manifests during recomposition of the `AnimatedVisibility` content, even when `isVisible` is `false` — the Compose runtime can recompose the content lambda before the exit animation completes, and the state may have changed in the interim.

Additionally, when `scheduleRetry` (line 318-328) is called with `null` station, it returns early (`if (station == null || attempt > MAX_RETRY) return`), so no automatic retry occurs, leaving the user stuck in the error state.

## Proposed Remediation

**Preferred**: Fix `onPlayerError` to extract the station from all non-Idle states, matching the pattern already used in `onPlaybackStateChanged` and `onIsPlayingChanged`:

```kotlin
override fun onPlayerError(error: PlaybackException) {
    val current =
        (_state.value as? PlayerState.Playing)?.station
            ?: (_state.value as? PlayerState.Buffering)?.station
            ?: (_state.value as? PlayerState.Paused)?.station
            ?: return
    _state.value = PlayerState.Error(current, "Se ha interrumpido la reproducción.")
    syncHolderToState()
    syncCastState()
    scheduleRetry(current, attempt = 1)
}
```

Additionally, replace the `station!!` non-null assertion at `PlayerUi.kt:114` with a safe call or early return to eliminate the latent crash vector:

```kotlin
// Instead of: station!!,
// Use the non-null station captured by the isVisible guard
val safeStation = station ?: return@AnimatedVisibility
```

**Alternatives**:
- **Guard only at UI level**: Add a `station ?: return@AnimatedVisibility` inside the `AnimatedVisibility` content. This prevents the crash but doesn't fix the root cause — the error state would have `null` station, causing `retry()` to silently fail and `scheduleRetry` to skip.
- **Make Error.station non-nullable**: Change `PlayerState.Error(val station: StationDto?, ...)` to `PlayerState.Error(val station: StationDto, ...)`. This forces all error-creation sites to provide a station, but requires handling the `onPlayerError` case where no prior station is available (e.g., fall back to `Idle` instead of `Error`).

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` — fix `onPlayerError` station extraction
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` — remove `station!!` non-null assertion, add safe access

**Tests to add or update**:
- Unit test: `onPlayerError` when state is `Buffering` should produce `Error(station, ...)` with non-null station
- Unit test: `onPlayerError` when state is `Paused` should produce `Error(station, ...)` with non-null station
- Unit test: `onPlayerError` when state is `Idle` should not crash (early return)
- UI test: MiniPlayer should not crash when `PlayerState.Error` has null station (defensive)

## Risks & Considerations

- The `station!!` pattern appears in multiple places inside `AnimatedVisibility` blocks. Removing it is low-risk since the `isVisible` guard already handles visibility.
- The `resolveCopyLink(station)` call at line 133 also receives `station` (which could be null) — this should be guarded with `station?.let { ... }` or moved inside the `isVisible` check (which it already is via AnimatedVisibility, but the defensive pattern should be consistent).
- The second crash (`FragmentActivity` / `MediaRouteButton`) appears to be a separate issue. `MainActivity` already extends `FragmentActivity`, so this may be caused by context issues in the `AndroidView` factory or a race condition during Activity recreation. It needs separate investigation.

## Open Questions

- [NEEDS CLARIFICATION: Does the second crash (MediaRouteButton / FragmentActivity) reproduce independently? It may be a separate bug triggered by Activity recreation or process death scenarios.]
- [NEEDS CLARIFICATION: Is the user testing on a device with Chromecast support? The MediaRouteButton crash may only manifest on devices with media routing capabilities.]
