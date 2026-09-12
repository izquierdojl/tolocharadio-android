# Bug Assessment: Los errores de reproducción en Cast no se muestran

- **Slug**: 0028-jlizquierdo-20260912-cast-playback-error-ui
- **Created**: 2026-09-12
- **Source**: follow-up detectado durante la depuración del Cast (bugs 0023-0027)
- **Verdict**: valid
- **Severity**: low

## Report (verbatim or summarized)

Follow-up apuntado al cerrar el bug 0027: `CastPlayerManager` no implementa `onPlayerError`, por lo que un fallo del receptor (401, stream caído, HLS sin CORS…) deja la UI en spinner/sin mensaje, sin explicar qué pasó.

## Symptom

Si la reproducción remota falla, el usuario no ve ningún error: el panel se queda cargando o sin estado coherente.

## Reproduction

1. Castear una emisora cuyo stream falle en el receptor.
2. La UI no muestra ningún mensaje de error.

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt` — `castPlayerListener` solo implementa `onPlaybackStateChanged`/`onIsPlayingChanged`.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` — `retry()` solo miraba `_state` (local), no el estado efectivo de Cast.

## Root Cause Hypothesis

**Confidence: high**

`CastPlayer` reporta los fallos por `Player.Listener.onPlayerError`, pero el listener de la app no lo sobreescribe; además `updateCastState()` reconstruye el estado desde `playbackState` (que tras un error queda IDLE), sin conservar el error. Resultado: el error se pierde.

## Proposed Remediation

**Preferred**: sobreescribir `onPlayerError` en `castPlayerListener`, guardar el `PlayerState.Error` (con log de `errorCode`, sin PII) y mostrarlo en `_castState` hasta que se recargue una emisora. `retry()` debe usar el estado efectivo para reintentar la emisora del error de Cast.

**Files likely to change**:
- `CastPlayerManager.kt`, `PlayerViewModel.kt`, `PlayerViewModelTest.kt`

**Tests to add or update**:
- `retry()` reintenta la emisora del error de Cast.

## Risks & Considerations

- No pisar el error con el `onPlaybackStateChanged(IDLE)` posterior; se limpia solo al recargar emisora o cerrar la sesión.
- No incluir datos personales en logs (solo tag + `errorCode`).

## Open Questions

- Ninguna.
