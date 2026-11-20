# Bug Assessment: Las gráficas de podcast fallan por DTOs que no coinciden con la API real

- **Slug**: 0049-izquierdojl-20261120-podcast-stats-dto-mismatch
- **Created**: 2026-11-20
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

> Revisa un error en la consulta de gráficas cuando son podcast, da error, salen
> unas letras rojas como que no se han podido obtener los datos. En radio
> funciona correctamente.

No se aportó URL. El reporte es texto pegado; no aplica la política de fetch de
URLs.

## Symptom

En modo Podcasts, al abrir Configuración → Gráficas aparece el `ErrorBanner` en
rojo ("Algo ha fallado. Inténtalo de nuevo.") en lugar del resumen y las
gráficas, aunque la cuenta tenga historial de escucha. La misma pantalla en modo
Radio carga correctamente. No es un fallo de red ni de credenciales: es
determinista con cualquier servidor que devuelva el contrato actual de
`/podcasts/stats/*`.

## Reproduction

1. Tener un servidor TolochaRadio con podcasts habilitados y al menos una
   sesión de escucha de podcast registrada.
2. En la app, activar el selector global **Podcasts**.
3. Ir a **Configuración → Gráficas**.
4. Observar el banner rojo de error en vez de las gráficas de podcast.
5. (Opcional, para confirmar) Filtrar Logcat por la etiqueta `StatsLoad`: el
   error es `unknown` con causa `kotlinx.serialization.MissingFieldException`
   sobre el campo `podcast` (o `episode`).

## Evidence

El contrato implementado por el backend (checkout hermano
`~/soft/tolocharadio`, v0.40.1) y consumido por la web es **plano**:

`apps/api/src/services/podcasts/stats.ts:29-75`

```ts
export interface PodcastStatsSummary {
  totalMs: number;
  episodesStarted: number;
  episodesCompleted: number;
  completionRate: number;
  podcastsCount: number;
  averageEpisodeMs: number;
}
export interface TopPodcastEntry {
  podcastId: number;
  title: string;
  author: string | null;
  imageUrl: string | null;
  totalMs: number;
  episodesStarted: number;
  episodesCompleted: number;
}
export interface TopEpisodeEntry {
  episodeId: number;
  podcastId: number;
  title: string;
  podcastTitle: string;
  imageUrl: string | null;
  totalMs: number;
  completed: boolean;
}
export interface RecentPodcastEntry {
  episodeId: number;
  podcastId: number;
  episodeTitle: string;
  podcastTitle: string;
  imageUrl: string | null;
  startedAt: number;
  endedAt: number;
  listenedMs: number;
  /* … */
}
```

Las rutas (`apps/api/src/routes/podcasts.ts:322-403`) devuelven esas estructuras
tal cual y los tests del backend lo fijan
(`apps/api/test/podcasts-stats.test.ts:169-308`). La web usa los mismos tipos
planos (`apps/web/src/lib/stats.ts:36-82`).

En cambio, los DTO de Android asumen un contrato anidado que solo aparece como
ejemplo "orientativo" en `docs/analisis-podcasts.md:904-931`, anterior a la
implementación:

`app/src/main/java/com/izquierdojl/tolocharadio/data/remote/dto/PodcastDtos.kt:152-173`

```kotlin
data class PodcastStatsSummaryDto(
    val totalMs: Long = 0,
    val podcastCount: Int = 0,          // el servidor envía podcastsCount
    val startedEpisodes: Int = 0,       // el servidor envía episodesStarted
    val completedEpisodes: Int = 0,     // el servidor envía episodesCompleted
    val completionRate: Double = 0.0,
    val avgMsPerEpisode: Long = 0,      // el servidor envía averageEpisodeMs
)
data class PodcastTopPodcastDto(
    val podcast: PodcastSummary,        // el servidor NO anida podcast
    val totalMs: Long = 0,
)
data class PodcastTopEpisodeDto(
    val episode: PodcastEpisode,        // el servidor NO anida episode
    val totalMs: Long = 0,
)
```

`PodcastRecentEntryDto` (`PodcastDtos.kt:206-210`) también espera
`episode: PodcastEpisode?` y `durationMs`, mientras el servidor envía
`episodeTitle`, `podcastTitle` y `listenedMs`.

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/dto/PodcastDtos.kt:152-173`
  — `PodcastStatsSummaryDto`, `PodcastTopPodcastDto` y `PodcastTopEpisodeDto`
  no reflejan el JSON real.
- `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/dto/PodcastDtos.kt:206-210`
  — `PodcastRecentEntryDto` espera un episodio anidado que no llega.
- `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/PodcastStatsRepo.kt:24-38`
  — `safeCall` deserializa con Retrofit/kotlinx; cualquier campo requerido
  ausente lanza `MissingFieldException`.
- `app/src/main/java/com/izquierdojl/tolocharadio/core/network/ApiResult.kt:60-74`
  — la excepción de serialización cae en `catch (e: Exception)` y se convierte
  en `DomainError.Unknown`, cuyo `userMessage()` es el texto rojo genérico.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsViewModel.kt:83-118`
  — se lanzan siete llamadas y **cualquier** `ApiResult.Err` (incluido el de
  `top-podcasts`) sustituye toda la pantalla por `PodcastStatsUi.Error`.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsMappers.kt:22-70`
  — los mapeos usan `it.podcast.title`, `it.episode.title`,
  `startedEpisodes`, `podcastCount`, `avgMsPerEpisode`, `it.episode?.title` y
  `durationMs`, todos inválidos con el contrato real.
- `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/api/PodcastsApi.kt:222-249`
  — las rutas son correctas; el fallo está en las formas de respuesta, no en
  los endpoints.

## Root Cause Hypothesis

Confianza: **high**.

Los DTO de estadísticas de podcast se modelaron siguiendo el ejemplo
"orientativo" de `docs/analisis-podcasts.md`, no la respuesta que el backend
implementó finalmente. `GET /podcasts/stats/top-podcasts` devuelve objetos
planos (`podcastId`, `title`, `totalMs`, …) y `top-episodes` también
(`episodeId`, `title`, …); los DTO de Android exigen los campos anidados
`podcast` y `episode` sin valor por defecto. kotlinx.serialization lanza
`MissingFieldException`, `safeCall` la convierte en `DomainError.Unknown` y el
ViewModel reemplaza el contenido por el banner de error. Por eso radio funciona
(sus DTO sí coinciden con `/stats/me/*`) y podcast no.

El fallo de `top-podcasts`/`top-episodes` enmascara además un segundo desajuste:
aunque se corrigieran solo esos dos, `summary` parsea pero deja
`podcastCount`, `startedEpisodes`, `completedEpisodes` y `avgMsPerEpisode` a 0
porque los nombres no coinciden, y `recent` mostraría "Episodio" con duración 0.
La corrección debe cubrir las cuatro formas.

## Proposed Remediation

**Preferred**: alinear los DTO con el contrato implementado y actualizar los
mapeos:

1. `PodcastStatsSummaryDto` → `episodesStarted`, `episodesCompleted`,
   `podcastsCount`, `averageEpisodeMs` (mantener `totalMs` y `completionRate`).
2. `PodcastTopPodcastDto` → plano: `podcastId`, `title`, `author`,
   `imageUrl`, `totalMs`, `episodesStarted`, `episodesCompleted`.
3. `PodcastTopEpisodeDto` → plano: `episodeId`, `podcastId`, `title`,
   `podcastTitle`, `imageUrl`, `totalMs`, `completed`.
4. `PodcastRecentEntryDto` → `episodeId`, `podcastId`, `episodeTitle`,
   `podcastTitle`, `imageUrl`, `startedAt`, `endedAt`, `listenedMs`,
   `positionSec`, `durationSec`, `completed`.
5. Actualizar `PodcastStatsMappers.kt` para leer los campos planos
   (`it.title` en rankings, `it.episodeTitle` y `it.listenedMs` en recientes) y
   las tarjetas de resumen con los nombres nuevos.

**Alternatives**:
- Cambiar el backend para que devuelva el contrato anidado del documento de
  análisis. Se descarta: la web y los tests del backend ya dependen del contrato
  plano y el cambio rompería a los clientes actuales.
- Hacer los DTO tolerantes a ambas formas con serializadores personalizados.
  Añade complejidad y deuda sin un caso real que lo justifique (YAGNI).

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/dto/PodcastDtos.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsMappers.kt`
- `app/src/test/java/com/izquierdojl/tolocharadio/support/PodcastFixtures.kt`
- `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsMappersTest.kt`
- `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/PodcastStatsRepoTest.kt`
- `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsScreenTest.kt`
  (si cambian los constructores usados en el test)

**Tests to add or update**:
- Test de serialización con `TolochaJson.decodeFromString` usando payloads
  reales del backend para `summary`, `top-podcasts`, `top-episodes`, `recent`,
  `timeline`, `habits` y `categories`. Es el test que habría detectado este bug:
  los tests actuales construyen los DTO directamente y nunca ejercitan el JSON.
- Actualizar `PodcastStatsMappersTest` a los campos planos y a
  `episodeTitle`/`listenedMs` en recientes.
- Mantener el test de "un bloque fallido no se sustituye silenciosamente" tal
  cual; ya protege el comportamiento de error del ViewModel.

## Risks & Considerations

- El cambio asume servidores con el contrato actual (v0.40.x). Si existiera una
  instancia antigua con la forma anidada, volvería a fallar; conviene confirmar
  que la app solo soporta servidores con `podcastStats` de esa versión.
- `PodcastSummary` y `PodcastEpisode` siguen usándose en otras pantallas; solo
  deben dejar de usarse en los DTO de estadísticas, no eliminarse.
- El significado de "duración" en recientes cambia de `durationMs` a
  `listenedMs` (tiempo escuchado en la sesión); es lo que muestra la web y lo
  correcto para el bloque.
- `recent` no filtra por rango (el servidor no lo acepta), así que el bloque
  sigue mostrando las últimas sesiones globales; es el comportamiento actual y
  no forma parte de este bug.
- No hay migración de datos ni cambio de API; el riesgo de regresión se limita
  a la pantalla de estadísticas de podcast.

## Open Questions

- [NEEDS CLARIFICATION: confirmar la versión del servidor contra el que se
  reprodujo (se asume ≥ 0.40.1, el checkout local). Si tras el fix el error
  persiste, capturar la línea `StatsLoad` de Logcat para identificar el campo
  exacto.]
- [NEEDS CLARIFICATION: ¿se desea añadir el filtro de periodo 7/30/90/todo a
  las gráficas de podcast? Hoy no se envían `from`/`to` y el servidor devuelve
  todo el histórico. Es una mejora aparte, no la causa del error.]
