# Bug Fix: Alinear los DTO de estadísticas de podcast con el contrato real

- **Slug**: 0049-izquierdojl-20261120-podcast-stats-dto-mismatch
- **Fixed**: 2026-11-20
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Se alinearon los DTO de `/podcasts/stats/*` con las respuestas planas que
devuelve el backend y se actualizaron los mapeos a las gráficas. Con ello
`top-podcasts` y `top-episodes` dejan de lanzar `MissingFieldException` (causa
del banner rojo), y `summary`/`recent` muestran las cifras y etiquetas correctas.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/dto/PodcastDtos.kt` | modified | `PodcastStatsSummaryDto`, `PodcastTopPodcastDto`, `PodcastTopEpisodeDto` y `PodcastRecentEntryDto` con los nombres y formas reales del backend. |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsMappers.kt` | modified | Rankings leen `title`; recientes usan `episodeTitle` y `listenedMs`; resumen usa `episodesStarted`, `episodesCompleted`, `podcastsCount` y `averageEpisodeMs`. |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsViewModel.kt` | modified | El estado vacío comprueba `episodesStarted` (nombre nuevo). |
| `app/src/test/java/com/izquierdojl/tolocharadio/support/PodcastFixtures.kt` | modified | Fixtures adaptadas a los DTO planos y a los nombres nuevos. |
| `app/src/test/java/com/izquierdojl/tolocharadio/data/remote/dto/PodcastDtosTest.kt` | modified | Nuevos tests de deserialización con payloads reales del backend. |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsMappersTest.kt` | modified | Nombres de resumen actualizados. |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsViewModelTest.kt` | modified | Fixture de estado vacío con nombres nuevos. |
| `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/PodcastStatsRepoTest.kt` | modified | Constructor del resumen actualizado. |
| `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsScreenTest.kt` | modified | Constructor del resumen actualizado; compila con `compileDebugAndroidTestKotlin`. |

## Diff Highlights

```kotlin
data class PodcastStatsSummaryDto(
    val totalMs: Long = 0,
    val episodesStarted: Int = 0,
    val episodesCompleted: Int = 0,
    val completionRate: Double = 0.0,
    val podcastsCount: Int = 0,
    val averageEpisodeMs: Long = 0,
)

data class PodcastTopPodcastDto(
    val podcastId: Long,
    val title: String,
    val author: String? = null,
    val imageUrl: String? = null,
    val totalMs: Long = 0,
    val episodesStarted: Int = 0,
    val episodesCompleted: Int = 0,
)

data class PodcastTopEpisodeDto(
    val episodeId: Long,
    val podcastId: Long,
    val title: String,
    val podcastTitle: String,
    val imageUrl: String? = null,
    val totalMs: Long = 0,
    val completed: Boolean = false,
)
```

```kotlin
fun PodcastRecents.toRecentItems(): List<RecentItem> =
    items.map {
        RecentItem(
            label = it.episodeTitle,
            startedAt = it.startedAt,
            durationMs = it.listenedMs,
        )
    }
```

## Tests Added or Updated

- `PodcastDtosTest::PodcastStatsSummary lee los agregados reales del backend` — fija `episodesStarted`, `episodesCompleted`, `podcastsCount` y `averageEpisodeMs`.
- `PodcastDtosTest::PodcastTopPodcasts lee el ranking plano del backend` — evita que vuelva a exigirse el objeto anidado `podcast`.
- `PodcastDtosTest::PodcastTopEpisodes lee el ranking plano del backend` — evita que vuelva a exigirse el objeto anidado `episode`.
- `PodcastDtosTest::PodcastRecents lee episodeTitle y listenedMs` — fija la forma de actividad reciente.
- `PodcastDtosTest::timeline habits y categories del backend se deserializan` — cubre el resto de bloques.
- Tests existentes de mappers, ViewModel y repo actualizados a los nombres nuevos; se conserva el test de que un bloque fallido no se sustituye silenciosamente.

## Local Verification

- `./gradlew testDebugUnitTest --tests "com.izquierdojl.tolocharadio.data.remote.dto.PodcastDtosTest" --tests "com.izquierdojl.tolocharadio.feature.podcasts.stats.PodcastStatsMappersTest" --tests "com.izquierdojl.tolocharadio.feature.podcasts.stats.PodcastStatsViewModelTest" --tests "com.izquierdojl.tolocharadio.data.repo.PodcastStatsRepoTest"` → BUILD SUCCESSFUL.
- `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL.
- `./gradlew compileDebugAndroidTestKotlin detekt ktlintCheck` → BUILD SUCCESSFUL.
- No se ejecutó `lintDebug` ni tests en dispositivo/emulador; no se modificó código nativo ni de UI más allá de constructores de test.

## Deviations from Assessment

- El assessment no listaba `PodcastStatsViewModel.kt`, pero fue necesario actualizar la comprobación de estado vacío (`startedEpisodes` → `episodesStarted`) como consecuencia directa del renombrado del DTO.
- `PodcastStatsScreenTest.kt` se actualizó porque el assessment lo contemplaba condicionalmente ("si cambian los constructores usados en el test").
- No se añadieron serializadores tolerantes a ambas formas: se aplicó la alternativa preferida del assessment (contrato plano actual).

## Follow-ups

- Verificar en emulador/dispositivo contra un servidor ≥ 0.40.x: Configuración → Gráficas en modo Podcasts debe mostrar resumen, evolución, rankings, hábitos, categorías y recientes sin banner rojo.
- Valorar añadir el filtro de periodo 7/30/90/todo a las gráficas de podcast (hoy se consulta todo el histórico); es una mejora aparte.
- Actualizar el ejemplo "orientativo" de `docs/analisis-podcasts.md` en el repo del backend para que no vuelva a inducir DTO anidados en futuros clientes.
