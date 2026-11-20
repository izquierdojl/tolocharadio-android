# Data Model: Refinamientos de experiencia podcast

**Feature**: `0048-izquierdojl-20261119-podcast-ux-refinements` | **Date**: 2026-11-19

No se añaden entidades persistidas ni tablas nuevas. La única preferencia nueva se guarda en DataStore (`podcast_start_screen`). El resto son estados derivados de reproducción, cola, favoritos y estadísticas ya existentes.

---

## 1. Tiempo de reproducción y cola

### `EpisodePlayerUi` (existente, sin cambios de contrato)

| Campo | Tipo | Regla |
|-------|------|-------|
| `episode` | `PodcastEpisode?` | Episodio en curso; `null` si no hay ninguno |
| `podcast` | `PodcastSummary?` | Programa para portada/título |
| `playing` | `Boolean` | Reproduciendo o pausado |
| `positionMs` | `Long` | Posición viva del reproductor; ≥ 0 |
| `durationMs` | `Long` | Duración conocida; 0 si aún no se conoce |
| `loading` | `Boolean` | Carga de fuente en curso |

### `EpisodeTimeInfo` (derivado, no persistido)

| Campo | Tipo | Regla |
|-------|------|-------|
| `positionMs` | `Long` | Acotado a `0..durationMs` cuando la duración es válida |
| `durationMs` | `Long?` | `null` si `durationMs <= 0` |
| `remainingMs` | `Long?` | `durationMs - positionMs`; `null` si no hay duración; nunca negativo |
| `elapsedLabel` | `String` | `m:ss` o `h:mm:ss` |
| `remainingLabel` | `String?` | `-m:ss` o `-h:mm:ss`; `null` si no hay duración |
| `progressFraction` | `Float?` | `positionMs / durationMs` en `0f..1f`; `null` si no hay duración |

### `QueueEpisodeMeta` (derivado por fila de cola)

| Campo | Tipo | Regla |
|-------|------|-------|
| `durationLabel` | `String?` | `formatEpisodeDuration(duration)` si `duration = episode.durationSec ?: progress.durationSec` es `> 0`; si no, `null` |
| `remainingLabel` | `String?` | `"Quedan N min"` o `"Queda menos de 1 min"`; `null` si no aplica |
| `isStarted` | `Boolean` | `positionSec > 0 && !completed` |
| `isCompleted` | `Boolean` | `progress.completed == true` |

**Reglas de `remainingWholeMinutes(positionSec, durationSec)`**

1. Duración efectiva = `episode.durationSec ?: progress.durationSec`; si es `null` o `<= 0` → `null`.
2. `positionSec <= 0` → `null` (no ha empezado).
3. `completed == true` → `null`.
4. `positionSec >= durationSec` → `null` (no queda tiempo).
5. En otro caso → `ceil((durationSec - positionSec) / 60.0)`, mínimo 1.
6. Si el resultado es 0 pero la diferencia es positiva → `"Queda menos de 1 min"`.

**Transiciones**

| Evento | Efecto |
|--------|--------|
| Reproduce / pausa | `positionMs` se actualiza cada 1 s; al pausar queda fijo |
| Seek o salto ±15/30 s | `positionMs` cambia y se refleja en ≤ 1 s |
| Cambio de episodio | Se reinician `positionMs`, `durationMs` y etiquetas |
| Detener | Se captura el reporte final y se limpia `EpisodePlayerUi`; desaparece la tarjeta |
| Fin sin siguiente | Mismo cierre limpio que detener |
| Duración desconocida | Solo se muestra el transcurrido; sin total, restante ni porcentaje inventado |

**Relaciones**: `EpisodeTimeInfo` pertenece a un episodio en curso; `QueueEpisodeMeta` pertenece a una fila de cola. Ambos comparten la regla de restante acotado.

---

## 2. Favorito de episodio

### `EpisodeFavoriteState` (derivado, no persistido)

| Campo | Tipo | Regla |
|-------|------|-------|
| `episodeId` | `Long` | Episodio |
| `isFavorite` | `Boolean` | `favoriteIds.contains(id)` si los favoritos ya cargaron; si no, `episode.isFavorite` |
| `loaded` | `Boolean` | `true` tras un `list()` correcto o una mutación satisfactoria |

**Transiciones**

| Estado de entrada | Acción | Estado de salida |
|-------------------|--------|------------------|
| No favorito | Pulsar | Favorito; se actualiza `favoriteIds` y la lista |
| Favorito | Pulsar | No favorito; se actualiza `favoriteIds` y la lista |
| Cualquiera | Fallo de red/sesión | Se conserva el estado anterior; mensaje accionable en español |
| Cambio en otra vista | Volver al detalle/ficha | El estado vivo se refleja sin recargar |

**Invariantes**

- El estado mostrado y la operación de toggle usan el mismo booleano; no se puede enviar "quitar" cuando la UI muestra "añadir".
- La reproducción no se pausa, reinicia ni cierra al alternar el favorito.
- No se registran identificadores de cuenta ni datos personales en logs.

---

## 3. Preferencia de arranque de podcast

### `PodcastStartScreen` (persistida en DataStore)

| Valor | Etiqueta | Ruta |
|-------|----------|------|
| `HOME` | Inicio | `Routes.PODCAST_HOME` |
| `DISCOVER` | Descubrir | `Routes.PODCAST_DISCOVER` |
| `SUBSCRIPTIONS` | Suscripciones | `Routes.PODCAST_SUBSCRIPTIONS` |
| `QUEUE` | Cola | `Routes.PODCAST_QUEUE` |
| `FAVORITES` | Favoritos | `Routes.PODCAST_FAVORITES` |

- Clave: `podcast_start_screen`.
- Valor por defecto: `HOME`.
- Valor corrupto o desconocido: `HOME` sin error.
- Se guarda como el nombre del enum, igual que `start_screen`.

### `StartDestination` (derivado, no persistido)

| Campo | Tipo | Regla |
|-------|------|-------|
| `radioStart` | `StartScreen` | Preferencia existente (EXPLORE/FAVORITES/HISTORY) |
| `podcastStart` | `PodcastStartScreen` | Preferencia nueva |
| `defaultSection` | `ContentMode` | RADIO o PODCAST |
| `podcastsEnabled` | `Boolean` | Capacidad del servidor |
| `route` | `String` | Ruta resuelta |

**Regla `resolveStartDestination(...)`**

1. Si `defaultSection == PODCAST && podcastsEnabled` → `podcastStart.route()`.
2. En cualquier otro caso → ruta de `radioStart` (`EXPLORE`, `FAVORITES`, `HISTORY`).
3. Si el servidor no ofrece podcasts, no se usa `podcastStart` y no se muestra error.

**Transiciones**

| Evento | Efecto |
|--------|--------|
| Cambiar la preferencia de podcast | Se persiste; se aplica en el siguiente arranque |
| Cambiar la preferencia de radio | No altera la de podcast |
| Cambiar de servidor | Se conserva la preferencia; si el nuevo servidor no tiene podcasts, se usa radio |
| Arranque con sección Podcasts y podcasts disponibles | Se abre la ruta elegida |
| Arranque con sección Podcasts y sin podcasts | Se abre la ruta de radio sin error |

---

## 4. Estadísticas por modo

### `ContentMode` (existente)

`RADIO` o `PODCAST`. En el grafo se deriva `podcastMode = podcastsEnabled && mode == PODCAST`.

### `PodcastStatsUi` (estado de la vista)

| Estado | Campos | Regla |
|--------|--------|-------|
| `Loading` | — | Carga inicial o reintento |
| `Empty` | — | Sin tiempo, episodios empezados ni recientes |
| `Error` | `message`, `isAuthError` | Error accionable; credenciales → editar servidor |
| `Content` | `summary`, `topPodcasts`, `topEpisodes`, `timeline`, `habits`, `categories`, `recent`, `habitsMatrix` | Todos los bloques cargados correctamente |

### `StatsSummaryItem` (derivado para la UI)

| Campo | Tipo | Origen |
|-------|------|--------|
| `label` | `String` | Texto en español |
| `value` | `String` | Valor formateado |
| `hint` | `String?` | Detalle opcional |

**Resumen de podcast propuesto**

| Tarjeta | Valor | Pista |
|---------|-------|-------|
| Tiempo total | `formatDurationMs(totalMs)` | "Datos aproximados" |
| Episodios | `"$startedEpisodes empezados · $completedEpisodes completados"` | `"Tasa de finalización: X %"` |
| Programas | `podcastCount` | `"Media por episodio: formatDurationMs(avgMsPerEpisode)"` |

### `RankingItem`, `ShareItem`, `RecentItem` (derivados para gráficas)

| Modelo | Campos | Uso |
|--------|--------|-----|
| `RankingItem` | `label`, `totalMs` | Programas y episodios más escuchados |
| `ShareItem` | `label`, `totalMs` | Categorías |
| `RecentItem` | `label`, `startedAt`, `durationMs` | Actividad reciente |

**Mapeos**

| Origen podcast | Destino |
|----------------|---------|
| `PodcastTimelineDto` | `StatsTimelineDto` (`bucket`, `totalMs`) para `StatsTimelineChart` |
| `PodcastHabits` | `List<StatsHabitEntryDto>` para `buildHabitsMatrix` y `HabitsHeatmap` |
| `PodcastTopPodcasts` | `List<RankingItem>` |
| `PodcastTopEpisodes` | `List<RankingItem>` |
| `PodcastCategories` | `List<ShareItem>` |
| `PodcastRecents` | `List<RecentItem>` (episodio nulo → "Episodio") |
| `PodcastStatsSummaryDto` | `List<StatsSummaryItem>` |

**Transiciones**

| Evento | Efecto |
|--------|--------|
| Abrir Gráficas en modo Radio | Se muestra `StatsScreen` (radio) |
| Abrir Gráficas en modo Podcasts | Se muestra `PodcastStatsScreen` |
| Cambiar de modo con Gráficas abierta | Se intercambia `STATS` ↔ `PODCAST_STATS`; no queda contenido obsoleto |
| Sin podcasts | Se muestra la vista de radio |
| Error de cualquier bloque | `Error` con reintento; no se cae a datos de radio |
| Error de credenciales | `Error(isAuthError = true)` con "Editar servidor" |
| Datos vacíos | `Empty` con mensaje propio de podcast |

**Invariantes**

- Una vista no mezcla datos de radio y podcast.
- La vista de podcast no sustituye sus gráficas por las de radio.
- Las cifras se formatean con `StatsFormat`; no se inventan series ni porcentajes.
- La privacidad se mantiene: solo la cuenta activa y sin PII en logs.

---

## 5. Persistencia y migraciones

- **DataStore**: nueva clave `podcast_start_screen` (String). No hay migración de datos; si la clave no existe, `HOME`.
- **Room**: sin cambios de esquema ni migraciones. El progreso de escucha y las estadísticas siguen en el servidor.
- **Descargas**: sin cambios.
- **Credenciales**: sin cambios; no se almacena nada nuevo sensible.
