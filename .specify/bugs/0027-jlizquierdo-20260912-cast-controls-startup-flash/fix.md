# Bug Fix: Controles de Cast (pausa/play) y flash del formulario al arrancar

- **Slug**: 0027-jlizquierdo-20260912-cast-controls-startup-flash
- **Fixed**: 2026-09-12
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Durante Cast el estado ahora se lee del `CastPlayer` real: aparece el botón Pause y pausa/reanuda desde el móvil (el toggle usa el "estado efectivo"). Al arrancar, la app muestra un fondo neutro hasta que Room emite los servidores, evitando el parpadeo del formulario.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt` | modified | `updateCastState` deriva del `CastPlayer`; helpers `castPlaybackState`/`localPlaybackState`/`currentDeviceName`; se elimina `updateCastPlayerState` |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` | modified | `effectiveState()`; `toggle`/`cancelLoad` usan estado efectivo; listener de ExoPlayer ignora eventos durante Cast; `syncCastState` no pisa el estado de Cast |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` | modified | mini-player y full player usan el estado efectivo; volumen controla el player activo |
| `app/src/main/java/com/izquierdojl/tolocharadio/MainActivity.kt` | modified | `collectAsState(initial = null)` + fondo neutro hasta cargar servidores |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` | updated test | toggle pausa/reanuda en Cast |

## Diff Highlights

```kotlin
// CastPlayerManager
private fun castPlaybackState(station: StationDto): PlayerState =
    when (castPlayer?.playbackState) {
        Player.STATE_READY ->
            if (castPlayer?.isPlaying == true) PlayerState.Playing(station) else PlayerState.Paused(station)
        Player.STATE_BUFFERING -> PlayerState.Buffering(station)
        Player.STATE_ENDED, Player.STATE_IDLE -> PlayerState.Idle
        else -> PlayerState.Buffering(station)
    }
```

```kotlin
// PlayerViewModel
private fun effectiveState(): PlayerState =
    (castPlayerManager.castState.value as? CastPlayerState.Cast)?.playerState ?: _state.value

fun toggle() {
    val player = castPlayerManager.activePlayer
    when (effectiveState()) {
        is PlayerState.Playing -> player.playWhenReady = false
        is PlayerState.Paused -> player.playWhenReady = true
        else -> Unit
    }
    ...
}
```

```kotlin
// MainActivity
val servers by serverRepository.servers.collectAsState(initial = null)
...
if (loadedServers == null) Surface(Modifier.fillMaxSize()) {} else { ... }
```

## Tests Added or Updated

- `PlayerViewModelTest`: `toggle pausa el CastPlayer cuando el estado efectivo es Playing`, `toggle reanuda el CastPlayer cuando el estado efectivo es Paused`.

## Local Verification

- `gradlew.bat testDebugUnitTest detekt ktlintCheck lintDebug assembleDebug` → **BUILD SUCCESSFUL**.

## Deviations from Assessment

- Ninguna.

## Follow-ups

- Valorar mostrar errores de Cast en el panel (`CastPlayerManager` no implementa `onPlayerError`).
