# Bug Fix: Un corte de red en un episodio deja la reproducción en error terminal

- **Slug**: 0057-izquierdojl-20261122-podcast-error-sin-recuperacion
- **Fixed**: 2026-11-22
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

El VOD deja de tratar un fallo de red como terminal: espera a que vuelva la
conectividad y reintenta con backoff en la última posición conocida. Además, la
recuperación de radio deja de reaccionar mientras hay un episodio cargado, por
lo que ya no puede arrancar la emisora ni mostrar sus estados.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePlaybackController.kt` | modified | Estados `WaitingForNetwork`/`Reconnecting`, espera de red, reintento con `RecoveryPolicy`, reanudación en la posición y reintento manual desde `Error`. |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManager.kt` | modified | Gate `episodes.isEpisodeActive()` en error, fin de stream, watchdog, cambios de red, retry manual y reanudación por sistema. |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt` | modified | Nuevo `recovering` en `EpisodePlayerUi`; mapea los estados de espera/reconexión; reinicia el ticker al volver de una recuperación. |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeFullPlayerSheet.kt` | modified | Aviso neutro «Sin conexión. Reintentando…» mientras `recovering`. |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt` | modified | `episodeActive` se deriva de `isEpisodeActive()`, incluyendo espera y reconexión. |
| `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePlaybackControllerTest.kt` | updated | 4 tests nuevos de recuperación. |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManagerTest.kt` | updated | 3 tests nuevos de aislamiento radio/VOD. |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModelTest.kt` | updated | Test del estado `recovering`. |

## Diff Highlights

```kotlin
// PlaybackRecoveryManager
if (episodeOwnsPlayer()) return

/** El VOD es dueño del player mientras haya un episodio cargado. */
private fun episodeOwnsPlayer(): Boolean = episodes.isEpisodeActive()
```

```kotlin
// EpisodePlaybackController: espera sin consumir intentos
if (!connectivity.isOnline.value) {
    _state.value = State.WaitingForNetwork(episode, podcast, position)
    connectivity.awaitOnline()
    startedAt = clock.nowMs()
}
```

## Tests Added or Updated

- `EpisodePlaybackControllerTest::sin red espera y reanuda en la ultima posicion` — el error de red no es terminal y se reanuda en la posición.
- `EpisodePlaybackControllerTest::la pausa cancela la espera de red y no recarga` — pausa del usuario cancela la recuperación.
- `EpisodePlaybackControllerTest::un error de formato no se reintenta` — los errores no recuperables siguen siendo terminales.
- `EpisodePlaybackControllerTest::resume desde error reintenta en la posicion guardada` — el botón play reintenta desde la posición.
- `PlaybackRecoveryManagerTest::un error de episodio no arranca la radio`
- `PlaybackRecoveryManagerTest::el fin de un episodio no reanuda la radio`
- `PlaybackRecoveryManagerTest::un cambio de red no reconecta la radio con episodio activo`
- `PlaybackRecoveryManagerTest::manualRetry con episodio activo devuelve false`
- `EpisodePlayerViewModelTest::el estado de espera de red marca recovering`

## Local Verification

- `./gradlew testDebugUnitTest` → 730 tests, 0 fallos, 0 errores.
- `./gradlew detekt ktlintCheck` → BUILD SUCCESSFUL.
- `./gradlew lintDebug` → BUILD SUCCESSFUL.
- Verificación manual en dispositivo: **no realizada** (requiere salir de cobertura).

## Deviations from Assessment

1. **Espera offline indefinida**: el presupuesto de `RecoveryPolicy` (6 intentos /
   120 s) solo se consume con red disponible. Esperar conectividad es una
   corrutina suspendida, sin coste de batería, y el caso de uso es un túnel o
   una zona sin cobertura. Se documenta en `fix.md` y queda como decisión
   revisable.
2. **Sin `LoadErrorHandlingPolicy`**: los reintentos se concentran en el
   controlador para no duplicar políticas. El assessment lo dejaba como opcional.
3. **Sin reporte de progreso al entrar en error**: se conserva la posición para
   reanudar, pero no se envía al servidor para no interferir con el bug 0054.
4. **Aviso en UI no previsto**: `EpisodePlayerUi.recovering` y el texto
   «Sin conexión. Reintentando…» se añadieron para no dejar la tarjeta muda.
5. **Play como reintento**: cuando el estado es `Error` o la recuperación se
   canceló, `resume()` recarga la fuente en la posición en vez de solo poner
   `playWhenReady` (el player está en error y no reanudaría).

## Follow-ups

- Validar en dispositivo real: episodio en streaming, modo avión 3 min, volver
  a conectar y comprobar la reanudación en la misma posición.
- Si la espera supera la caducidad de la URL firmada, reemitirla antes de
  reanudar (`ResolveEpisodePlaybackUseCase.needsRefresh` ya existe).
- Evaluar `LoadErrorHandlingPolicy` para fallos transitorios internos del player.
- Si el acoplamiento crece, extraer una interfaz `EpisodePlaybackGate` en lugar
  de inyectar el controlador completo en `PlaybackRecoveryManager`.
