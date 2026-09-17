# Bug Fix: Chromecast volume forced to 100% after connection

- **Slug**: 0036-jlizquierdo-20260917-cast-volume-forced-to-max
- **Fixed**: 2026-09-17
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

**v2 (2026-09-17, post-verificación en dispositivo)**: el primer fix (reordenar `bind()` antes de `startVolumeEvents()`) NO resolvió el bug: el receptor salta al 100% nada más conectar, incluso antes de reproducir.

Root cause real: `emitDeviceVolume()` en `CastPlayerManager` llamaba a `player.setDeviceVolume(...)` sobre un `CastPlayer` **raw** de Media3. En Media3, `CastPlayer.setDeviceVolume` **escribe al receptor** (`CastSession.setVolume`). Al conectar, `CastSession.volume` devuelve 1.0 por defecto (antes del primer eco del receptor), por lo que la primera emisión del collector aplicaba físicamente 100% al Chromecast.

Fix: usar el wrapper `CastDeviceVolumePlayer` (que existía sin usarse) como player de la sesión, y que `emitDeviceVolume()` sincronice **solo por notificación** (`emitDeviceVolumeChanged`), sin escribir nunca al receptor. Las escrituras al receptor solo ocurren por interacción del usuario (`setCastVolume`/`stepCastVolume` vía teclas/slider).

## Summary v1 (original, incompleto)

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

**v2**: la hipótesis del assessment (condición de carrera bind/collector) era insuficiente. La evidencia post-fix mostró que el bug es una **escritura al receptor en el ciclo de eco/notificación**: `emitDeviceVolume()` → `CastPlayer.setDeviceVolume()` → `CastSession.setVolume` al receptor con el valor stale 1.0. El fix definitivo elimina toda escritura al receptor en la sincronización de estado (solo notificación a la MediaSession) y activa el wrapper `CastDeviceVolumePlayer` que la spec 0035 contemplaba. Reordenar bind/collector se conserva (inofensivo y mejora la primera sincronización de UI).

## Follow-ups

- Consider adding a `CastPlayerManagerTest` with a fake `PlaybackVolumeController` to verify the full `createCastPlayer()` flow (currently no unit test for this class).
- Consider initializing `_castVolume` with `Float.NaN` as a defensive measure for the edge case where `readVolume()` returns `null` during `bind()`.
