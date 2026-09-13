# Bug Fix: Mostrar errores de reproducción en Cast y permitir reintentar

- **Slug**: 0028-jlizquierdo-20260912-cast-playback-error-ui
- **Fixed**: 2026-09-12
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

El listener del `CastPlayer` ahora captura `onPlayerError`, guarda el error (con log del `errorCode`, sin PII) y lo expone en `_castState` hasta recargar una emisora; la UI muestra "No se pudo reproducir en el dispositivo." y el botón de reintentar. `retry()` usa el estado efectivo, así funciona también desde Cast.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt` | modified | `castError` + `onPlayerError`; `updateCastState` prioriza el error; se limpia al cargar/cerrar sesión |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` | modified | `retry()` usa `effectiveState()` |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` | updated test | retry desde error de Cast |

## Diff Highlights

```kotlin
override fun onPlayerError(error: PlaybackException) {
    val station = activeStationHolder.station ?: return
    android.util.Log.w("CastPlayerManager", "Error de reproducción Cast code=${error.errorCode}", error)
    castError = PlayerState.Error(station, "No se pudo reproducir en el dispositivo.")
    updateCastState()
}
```

```kotlin
CastPlayerState.Cast(castError ?: castPlaybackState(current), currentDeviceName(), _connectionState.value)
```

```kotlin
// PlayerViewModel
fun retry() {
    val station = (effectiveState() as? PlayerState.Error)?.station ?: return
    play(station)
}
```

## Tests Added or Updated

- `PlayerViewModelTest`: `retry reintenta la emisora del error de Cast`.

## Local Verification

- `gradlew.bat testDebugUnitTest detekt ktlintCheck lintDebug assembleDebug` → **BUILD SUCCESSFUL**.

## Deviations from Assessment

- Ninguna.

## Follow-ups

- Ninguna pendiente conocida.
