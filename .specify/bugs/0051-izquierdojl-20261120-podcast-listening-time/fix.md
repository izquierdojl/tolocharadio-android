# Bug Fix: Contabilizar el tiempo real de escucha de podcast, no la posición

- **Slug**: 0051-izquierdojl-20261120-podcast-listening-time
- **Fixed**: 2026-11-20
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

El fallo era de la **app Android**: cada reporte de progreso enviaba
`listenedMs = posición`, así que un salto en la barra de progreso declaraba como
escuchados todos los minutos recorridos. Se añadió un acumulador de tiempo real
que sólo suma avances continuos de reproducción (ignorando saltos y pausas) y se
usa en reportes periódicos, pausa, búsqueda, parada, fin y keepalive. El API no
necesita cambios: ya acumula correctamente el `listenedMs` que recibe, igual que
hace la web.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/ListeningTimeTracker.kt` | added | Acumulador puro: suma deltas continuos, descarta saltos hacia delante/atrás y pausas; se reinicia por sesión. |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt` | modified | Alimenta el acumulador con `State.Active`, reinicia por sesión y envía `listenedMs` acumulado en todos los reportes. El keepalive de segundo plano ahora incrementa `sequence`. |
| `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/ProgressRepo.kt` | modified | `listenedMs` pasa a ser parámetro obligatorio en `report()` y `keepalive()`; se elimina el valor por defecto basado en la posición. |
| `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/ListeningTimeTrackerTest.kt` | added | Cubre avance continuo, saltos, retrocesos, pausa, reinicio y sincronización final. |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModelTest.kt` | modified | Reportes de parada, seek, keepalive y fin usan el tiempo acumulado; nuevos tests anti-inflado. |
| `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/ProgressRepoTest.kt` | modified | Envía `listenedMs` explícito y fija que no se derive de la posición. |

## Diff Highlights

```kotlin
class ListeningTimeTracker(private val maxContinuousDeltaMs: Long = MAX_CONTINUOUS_LISTEN_DELTA_MS) {
    private var lastPositionMs: Long? = null
    private var listenedMs: Long = 0L

    val totalMs: Long get() = listenedMs

    fun onProgress(positionMs: Long, playing: Boolean): Long {
        val previous = lastPositionMs
        if (previous != null && playing) {
            val delta = positionMs - previous
            if (delta in 1..maxContinuousDeltaMs) listenedMs += delta
        }
        lastPositionMs = positionMs
        return listenedMs
    }

    fun synchronize(positionMs: Long): Long = onProgress(positionMs, playing = true)
}
```

```kotlin
is EpisodePlaybackController.State.Active -> {
    listening.onProgress(state.positionMs, state.playing)
    current.copy(
        playing = state.playing,
        // …
    )
}
```

```kotlin
val session = sessionId ?: return null
listening.synchronize(positionMs)
sequence += 1
// …
progress.report(
    // …
    listenedMs = listening.totalMs,
    // …
)
```

```kotlin
suspend fun report(
    episodeId: Long,
    sessionId: String,
    sequence: Long,
    positionSec: Long,
    listenedMs: Long,
    durationSec: Long? = null,
    completed: Boolean? = null,
): ApiResult<PodcastEpisodeProgress>
```

## Tests Added or Updated

- `ListeningTimeTrackerTest::suma solo el avance continuo mientras se reproduce` — acumula 1 s por muestra.
- `ListeningTimeTrackerTest::los saltos hacia delante no inflan el tiempo escuchado` — un salto de 20 min no suma.
- `ListeningTimeTrackerTest::el retroceso no resta y la reescucha vuelve a contar` — comportamiento de reescucha real.
- `ListeningTimeTrackerTest::las muestras en pausa no suman tiempo` — pausa sin acumulación.
- `ListeningTimeTrackerTest::reset empieza una sesion nueva` — aísla sesiones.
- `ListeningTimeTrackerTest::synchronize recupera el ultimo tramo sin contar saltos` — cierre de reporte seguro.
- `EpisodePlayerViewModelTest::un seek no infla el tiempo escuchado` — reporta 3 s escuchados tras saltar al minuto 10.
- `EpisodePlayerViewModelTest::stop limpia el episodio y reporta el tiempo escuchado` — parada envía el acumulado.
- `EpisodePlayerViewModelTest::parar cerca del final retira el episodio si el servidor lo completa` — el flag del servidor sigue mandando para la cola, pero el tiempo no se infla.
- `EpisodePlayerViewModelTest::el keepalive de segundo plano envia el tiempo acumulado y avanza la secuencia` — `sequence` 1 y 2, sin duplicados.
- `EpisodePlayerViewModelTest::al terminar informa el tiempo escuchado y no la duracion completa` — `Ended` ya no declara 2 h.
- `ProgressRepoTest::report respeta listenedMs aunque difiera de la posicion` — el repositorio no vuelve a derivar el tiempo de la posición.

## Local Verification

- Comando: `./gradlew testDebugUnitTest --tests "com.izquierdojl.tolocharadio.domain.podcasts.ListeningTimeTrackerTest" --tests "com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerViewModelTest" --tests "com.izquierdojl.tolocharadio.data.repo.ProgressRepoTest"` → `BUILD SUCCESSFUL`; 6 + 16 + 5 tests, 0 fallos.
- Comando: `./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` → `BUILD SUCCESSFUL`; 122 suites, 641 tests, 0 fallos, 0 errores; `detekt`, `ktlintCheck` y `lintDebug` en verde.
- Comprobación manual: revisión del contrato en el checkout del backend
  (`~/soft/tolocharadio/apps/api/src/services/podcasts/progress.ts`) y de la
  implementación web (`apps/web/src/lib/playbackEngine.ts`); el backend acumula
  deltas de `listenedMs` y la web los calcula con avance continuo, confirmando
  que el desajuste estaba sólo en Android.
- No se ejecutaron tests en dispositivo/emulador; no se modificó UI ni
  reproducción, sólo el cálculo y el envío de estadísticas.

## Deviations from Assessment

- **No se cambió `EpisodeActionsViewModel.togglePlayed()`** para enviar
  `listenedMs = 0`. El assessment lo proponía como punto 5 y lo dejaba abierto:
  el backend calcula `episodesStarted` sumando `listenedMs`, así que marcar un
  episodio como reproducido con 0 ms lo contaría como completado pero no como
  empezado, distorsionando la tasa de finalización. Es una decisión de producto
  aparte; se deja documentada como follow-up. El flujo automático (el que
  describe el usuario) sí queda corregido.
- La ventana anti-salto se fijó en **10 s** (el assessment admitía 5–10 s) para
  absorber el muestreo de 1 s y la reproducción a 2× sin perder tramos reales.
- No se modificó el API ni la base de datos: la investigación confirmó que el
  servidor ya recibe y acumula correctamente el tiempo escuchado.

## Follow-ups

- Decidir si "marcar como reproducido" debe sumar tiempo de escucha. Si se
  quiere que no sume, habría que ajustar también el cálculo de `episodesStarted`
  del backend para que no dependa de `listenedMs`.
- Verificar en dispositivo con un episodio de 2 h: escuchar 5 min, saltar al
  minuto 90, parar y comprobar que las gráficas suben ~5 min, no ~95 min.
- Las estadísticas ya registradas con el cálculo antiguo no se corrigen
  retroactivamente; el fix aplica a los reportes nuevos.
- Valorar un límite de cordura en el backend (tiempo escuchado vs. tiempo
  transcurrido entre reportes) para proteger a clientes antiguos o defectuosos.
