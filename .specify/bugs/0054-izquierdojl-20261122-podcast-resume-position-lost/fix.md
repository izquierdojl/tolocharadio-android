# Bug Fix: No borrar la posición del episodio al perder la reproducción

- **Slug**: 0054-izquierdojl-20261122-podcast-resume-position-lost
- **Fixed**: 2026-11-22
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Cuando la radio (o una parada externa) retiraba el episodio del reproductor
compartido, el controlador pasaba a `Idle` pero el ViewModel mantenía la tarjeta
del episodio. Cualquier keepalive o parada posterior leía la posición 0 y
sobrescribía el progreso del servidor, así que la siguiente selección empezaba
desde el principio. Ahora `Idle` cierra la tarjeta y ninguna ruta puede enviar
una posición 0 de un episodio que ya no está cargado.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt` | modified | `State.Idle` limpia el episodio (conserva la velocidad); `onEnded()` captura el `PodcastSummary` antes de suspender para no perderlo al limpiar. |
| `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` | modified | El temporizador de sueño para primero el episodio y después la radio, para que el reporte final use la posición real. |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModelTest.kt` | tests added | Parada externa, parada posterior y auto-avance con `Idle`. |

## Diff Highlights

```kotlin
is EpisodePlaybackController.State.Idle ->
    if (current.episode == null) {
        current
    } else {
        // Idle = el controlador ya no tiene el episodio
        // (radio, temporizador o parada externa). Se cierra
        // la tarjeta para no reportar la posición 0.
        EpisodePlayerUi(rate = current.rate)
    }
```

```kotlin
private fun onEnded(
    episodeId: Long,
    durationSec: Long,
) {
    // Se captura antes de suspender: el estado Idle limpia la tarjeta.
    val podcast = _ui.value.podcast
    viewModelScope.launch {
        ...
        val sameShow = podcast?.takeIf { it.id == item.podcastId }
```

```kotlin
sleepTimerVm.setStopPlayerCallback {
    // El episodio reporta su posición antes de que la radio pare el player compartido.
    episodeVm.stop()
    playerVm.stop()
}
```

## Tests Added or Updated

- `EpisodePlayerViewModelTest::una parada externa cierra la tarjeta y no reporta posicion cero` — simula la radio tomando el reproductor y comprueba que `onAppBackground()` no envía un keepalive con `positionSec = 0`.
- `EpisodePlayerViewModelTest::parar tras una parada externa no reporta cero` — el botón de parada sobre una tarjeta ya detenida no escribe 0 en el progreso.
- `EpisodePlayerViewModelTest::el auto-avance conserva el programa aunque el controlador pase a idle` — con un reporte de fin lento, `Idle` limpia la tarjeta y el siguiente episodio conserva el `PodcastSummary`.

## Local Verification

- Commands run: `./gradlew testDebugUnitTest --tests "com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerViewModelTest"` → `BUILD SUCCESSFUL`, 19/19 tests (2 fallaban antes del fix).
- Commands run: `./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` → `BUILD SUCCESSFUL`, 132 suites / 720 tests, 0 fallos.
- Manual checks: no se reprodujo en dispositivo para no alterar el progreso real de la cuenta; la reproducción del fallo queda cubierta por los tests unitarios (antes en rojo). La comprobación auditiva/visual end-to-end queda para `/speckit.bug.test`.

## Deviations from Assessment

- **Ampliación de alcance**: se modificó `TolochaNavGraph.kt` para invertir el orden de parada del temporizador de sueño. Sin ello, `playerVm.stop()` limpiaba el episodio antes de que `episodeVm.stop()` pudiera reportar su posición y se perdían hasta 30 s de progreso (no la posición completa, pero sí precisión). El assessment solo listaba el ViewModel y su test.
- El assessment proponía limpiar en `Idle` y capturar el podcast; se implementó tal cual. No fue necesario tocar el servidor ni añadir almacenamiento local.

## Follow-ups

- Valorar reportar la última posición conocida desde `EpisodePlaybackController.stop()` cuando la radio toma el reproductor: hoy se conserva la del último reporte periódico (hasta 30 s de desviación).
- `CastPlayerManager.connectToEpisode()` ignora la posición de reanudación; emitir un episodio a medias sigue empezando en 0. Es un fallo independiente, candidato a otro assessment.
- Verificar en dispositivo el flujo completo con `/speckit.bug.test`.
