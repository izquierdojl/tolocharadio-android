# Bug Fix: Retirar de la cola los episodios completados

- **Slug**: 0050-izquierdojl-20261120-podcast-queue-completed-returns
- **Fixed**: 2026-11-20
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Al terminar un episodio, el reproductor ahora espera el reporte de completado y
lo retira de la cola antes de auto-avanzar, igual que la web. Si el usuario para
cerca del final y el servidor auto-completa el progreso, también se retira. Los
reportes periódicos no retiran nada para no afectar a un episodio completado que
se está reescuchando.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt` | modified | `onEnded()` espera el reporte y llama a `queue.remove()` antes de buscar el siguiente; `stop()` usa el `completed` devuelto por el servidor para retirar el episodio; `reportProgress()` centraliza el reporte y devuelve el progreso. |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModelTest.kt` | modified | Nuevos tests de retirada al terminar, parada al 98 %, parada a medias, fallo de red y reporte periódico. |

## Diff Highlights

```kotlin
fun stop() {
    val id = _ui.value.episode?.id
    val durationMs = _ui.value.durationMs
    val position = controller.stop()
    ticker?.cancel()
    _ui.value = EpisodePlayerUi(rate = _ui.value.rate)
    if (id == null) return
    viewModelScope.launch {
        val result = reportProgress(id, position, completed = false, durationMs = durationMs)
        // El servidor auto-completa cerca del final; si lo hizo, sale de la cola.
        if (result?.completed == true) queue.remove(id)
    }
}
```

```kotlin
private fun onEnded(
    episodeId: Long,
    durationSec: Long,
) {
    viewModelScope.launch {
        ticker?.cancel()
        complete(episodeId, durationSec)
        // Paridad con la web: el episodio terminado sale de la cola antes de avanzar.
        queue.remove(episodeId)
        val nextId = next(episodeId)
        if (nextId == null) {
            _ui.value = EpisodePlayerUi(rate = _ui.value.rate)
            return@launch
        }
        val item = queue.queue.value.items.firstOrNull { it.id == nextId } ?: return@launch
        val sameShow = _ui.value.podcast?.takeIf { it.id == item.podcastId }
        play(item, sameShow)
    }
}
```

## Tests Added or Updated

- `EpisodePlayerViewModelTest::el auto-avance reanuda el siguiente episodio` — ahora verifica el orden `queue.remove(1)` → `next(1)`.
- `EpisodePlayerViewModelTest::al terminar sin siguiente retira el episodio y limpia la tarjeta` — cubre el último episodio de la cola.
- `EpisodePlayerViewModelTest::un fallo al retirar de la cola no impide el auto-avance` — la reproducción continúa si falla la red.
- `EpisodePlayerViewModelTest::parar cerca del final retira el episodio si el servidor lo completa` — simula la parada al 98 % con `completed = true`.
- `EpisodePlayerViewModelTest::parar a mitad no retira el episodio de la cola` — evita retiradas prematuras.
- `EpisodePlayerViewModelTest::un reporte periodico de un completado no lo retira de la cola` — protege la reescucha de episodios ya completados.

## Local Verification

- `./gradlew testDebugUnitTest --tests "com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerViewModelTest"` → BUILD SUCCESSFUL, 13/13 tests.
- `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL, 629 tests, 0 fallos.
- `./gradlew detekt ktlintCheck compileDebugAndroidTestKotlin` → BUILD SUCCESSFUL.
- No se hizo prueba manual en dispositivo para no alterar la cola y el progreso reales de la cuenta; la verificación end-to-end queda para `/speckit.bug.test`.

## Deviations from Assessment

- No fue necesario modificar `ProgressRepo`: `report()` ya devolvía `ApiResult<PodcastEpisodeProgress>`, así que el ViewModel ahora consume ese resultado en lugar de descartarlo.
- El keepalive de segundo plano sigue sin procesar el `completed` de la respuesta; se deja como mejora posterior, tal y como contemplaba el assessment.

## Follow-ups

- Verificar en emulador/dispositivo: terminar un episodio de la cola y comprobar que desaparece de pendientes; parar al ~98 % y comprobar el mismo resultado.
- Valorar procesar también la respuesta del keepalive si el servidor completa en segundo plano.
- Revisar si conviene que el backend retire el elemento de la cola al completarse, para que cualquier cliente quede consistente sin lógica duplicada.
