# Bug Fix: NullPointerException in MiniPlayer when station is unavailable

- **Slug**: miniplayer-npe-station-not-found
- **Fixed**: 2026-09-08
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Fixed `onPlayerError` in `PlayerViewModel` to extract the current station from all non-Idle states (Buffering, Playing, Paused) instead of only Playing, replaced the `station!!` non-null assertion in `MiniPlayer` with a safe early-return, and removed automatic retry on playback error (caused infinite loop). Manual retry via button still works.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` | modified | `onPlayerError` now checks Buffering and Paused states. Removed `scheduleRetry` call, method, `retryJob` field, `MAX_RETRY`/`RETRY_BASE_MS` constants, and unused `delay` import. |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` | modified | Replaced `station!!` with `val safeStation = station ?: return@AnimatedVisibility`. |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` | added tests | 3 new tests: `onPlayerError durante Buffering preserva la emisora`, `onPlayerError durante Playing preserva la emisora`, `onPlayerError en Idle no cambia estado`. |

## Diff Highlights

**PlayerViewModel.kt — onPlayerError (final):**
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
    // No automatic retry — user retries manually via retry() button
}
```

**Removed code:** `scheduleRetry()` method, `retryJob` field, `MAX_RETRY`, `RETRY_BASE_MS`, `delay` import.

## Tests Added or Updated

- `PlayerViewModelTest::onPlayerError durante Buffering preserva la emisora` — pins down that ExoPlayer errors during buffering produce Error(station, ...) with non-null station
- `PlayerViewModelTest::onPlayerError durante Playing preserva la emisora` — same for Playing state
- `PlayerViewModelTest::onPlayerError en Idle no cambia estado` — confirms early return when no station is available (no crash)

## Local Verification

- Commands run: `testDebugUnitTest --tests PlayerViewModelTest` → 11/11 passed
- Compilation: clean (no errors)

## Deviations from Assessment

- Assessment proposed keeping `scheduleRetry` with reduced retries. User reported infinite loop, so automatic retry was removed entirely. Manual retry button (`retry()`) is preserved.

## Follow-ups

- The second crash (`IllegalStateException: The activity must be a subclass of FragmentActivity` at `MediaRouteButton.showDialogForType`) is a separate issue and should be assessed independently.
- Consider making `PlayerState.Error.station` non-nullable in a future refactor.
