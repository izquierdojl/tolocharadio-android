# Bug Fix: Chromecast volume forced to 100% after connection

- **Slug**: 0036-jlizquierdo-20260917-cast-volume-forced-to-max
- **Fixed**: 2026-09-17
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Reordenar `createCastPlayer()` en `CastPlayerManager` para que `volume.bind()` se ejecute ANTES de `startVolumeEvents()`. Esto garantiza que el volumen real del receptor se lea antes de que el collector emita el valor por defecto (100%) al CastPlayer.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt` | modified | `bind()` antes de `startVolumeEvents()`; `startVolumeEvents()` fuera del bloque `if (castPlayer == null)` |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackVolumeControllerTest.kt` | added test | Verifica que `bind()` nunca deja el volumen en el default 100% |

## Diff Highlights

```kotlin
// CastPlayerManager.kt — createCastPlayer()
private fun createCastPlayer(session: CastSession) {
    castError = null
    if (castPlayer == null) {
        castContext?.let { ctx ->
            castPlayer = CastPlayer(ctx).apply {
                addListener(castPlayerListener)
            }
        }
        castPlayer?.let { player ->
            mediaSession?.setPlayer(player)
        }
    }
    // bug 0036: bind FIRST so the receiver's real volume is read before
    // startVolumeEvents() pushes the initial value to the CastPlayer.
    if (!volume.isRemoteActive) {
        volume.bind(CastSessionVolumeDevice(session))
    }
    startVolumeEvents()
    requestAudioFocus()
    lastDeviceName = currentDeviceName()
    updateCastState()
}
```

## Tests Added or Updated

- `PlaybackVolumeControllerTest::bind nunca deja el volumen en el default 100 por ciento` — pinning that `bind()` overrides the default `_castVolume=1.0` with the receiver's real volume immediately

## Local Verification

- `.\gradlew.bat testDebugUnitTest --tests "com.izquierdojl.tolocharadio.feature.player.PlaybackVolumeControllerTest"` → BUILD SUCCESSFUL (1m 3s)
- All existing tests pass, no regressions

## Deviations from Assessment

None. The fix follows the proposed remediation exactly.

## Follow-ups

- Consider adding a `CastPlayerManagerTest` with a fake `PlaybackVolumeController` to verify the full `createCastPlayer()` flow (currently no unit test for this class).
- Consider initializing `_castVolume` with `Float.NaN` as a defensive measure for the edge case where `readVolume()` returns `null` during `bind()`.
