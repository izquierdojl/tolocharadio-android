# Data Model: Modo Podcast

**Feature**: `specs/0046-izquierdojl-20261118-modo-podcast-completo/spec.md` — Fuente de verdad del contrato: backend (`apps/web/src/lib/types.ts`, `api.ts`).

## Entidades

### ContentMode
- Modo global de la app: `RADIO` | `PODCAST`.
- Persistencia: DataStore `content_mode` (defecto `RADIO`); preferencia de sección inicial Radio|Podcasts decide el modo de arranque con sesión válida.

### PodcastCapabilities (de `GET /config`)
- `podcasts`, `podcastDiscovery` (`itunes`|`podcastindex`|`none`), `podcastOpml`, `podcastQueue`, `podcastFavorites`, `podcastStats`, `podcastDownloads`: booleanos opcionales (ausente = desactivado).
- `limits`: `podcastQueueMax` (defecto 200), `podcastSubscriptionsMax`, `podcastEpisodesPageSize`, `podcastOpmlMaxFeeds` (opcionales; gobiernan validación local previa).
- Regla: sin `podcasts=true` todo el modo se oculta (FR-001/FR-005).

### PodcastSummary (programa)
- `id: Long`, `feedUrl: String` (http/https), `title: String`, `author: String?`, `description: String?`, `imageUrl: String?` (Coil), `language: String?`, `link: String?`, `categories: List<String>`, `lastFetchedAt: Long?`, `episodeCount: Int`, `isSubscribed: Boolean`, `subscribedAt: Long?`, `latestPendingEpisodeAt: Long?` (solo suscripciones).
- Identidad: `id` del servidor; unicidad en listas por `id` (suscripciones) y por `feedUrl` normalizada en importaciones (evita duplicados OPML/bulk).
- Relaciones: 1 programa → N episodios; 1 cuenta → N suscripciones.

### PodcastEpisode (episodio)
- `id: Long`, `podcastId: Long`, `title: String`, `description: String?`, `publishedAt: Long?`, `durationSec: Long?`, `imageUrl: String?`, `episodeNumber: Int?`, `seasonNumber: Int?`, `explicit: Boolean`, `progress: EpisodeProgress?`, `isFavorite: Boolean` (las listas de cola/portada ya lo incluyen).
- Estado derivado: SinEmpezar (`progress==null` o `positionSec==0`), EnProgreso (`positionSec>0` y no completado), Completado (`progress.completed`).
- Identidad: `id`; en cola y favoritos unicidad por `id` (añadir existente = mover, FR-019).

### EpisodeProgress
- `positionSec: Long`, `durationSec: Long?`, `completed: Boolean`, `completedAt: Long?`, `updatedAt: Long`.
- Escritura: `{sessionId: UUID v4 por inicio de escucha, sequence: Long monótona, positionSec}`; idempotente en servidor.

### EpisodePage / EpisodeSearchPage
- `{items: List<PodcastEpisode>, pagination: {offset, limit, hasMore}}`; búsqueda global devuelve episodios con `podcast` embebido.
- Filtros de ficha: `sort` (recientes|duración|título), `filter` (sin empezar|en progreso|completados).

### PodcastQueue
- `revision: Long` (optimista, solo servidor la incrementa), `updatedAt: Long?`, `limit: Int` (de capabilities), `items: List<PodcastEpisode>`.
- `QueueSummary`: `{revision, count, limit, next: PodcastEpisode?}` (portada).
- Invariantes: `items.size <= limit`; sin duplicados por `id`; el episodio en reproducción se oculta de pendientes en UI pero conserva su hueco en el orden total (`mergeQueueOrder`).

### PodcastHome (portada)
- `continueListening: List<PodcastEpisode>` (con progreso, descartables), `latestEpisodes: List<PodcastEpisode>` (novedades), `subscriptions: List<PodcastSummary>`, `queue: QueueSummary`.

### BulkSubscriptionResult
- `imported, skipped, failed: Int`, `items: List<PodcastSummary>`, `errors: List<{feedUrl, message}>` (informe de importación).

### IssuedToken (reproducción/descarga)
- `url: String` (firmada HMAC, contextos distintos stream vs descarga), `expiresAt: Long` (TTL 6 h).
- Regla: reemisión anticipada si vence durante la escucha; nunca persistir credenciales en la URL (las credenciales de usuario jamás viajan en URL por constitución).

### DownloadRecord (local, Room)
- `episodeId: Long` (PK), `downloadManagerId: Long`, `status` (Descargando|Completo|Fallido), `localUri: String?`, `updatedAt: Long`.
- Transiciones: Ausente → Descargando → Completo; Completo → Ausente (archivo perdido/eliminado); Fallido → Descargando (reintento). Reproducción prefiere `localUri` cuando `status==Completo` y el fichero existe.

### PodcastStats
- Resumen: tiempo total, programas distintos, empezados (≥30 s), completados, tasa, media por episodio; series: evolución temporal (day|week|month), hábitos (weekday 0..6 lun–dom, hour 0..23), categorías, rankings (programas/episodios por tiempo), sesiones recientes. Solo cuenta activa (privado).

## Validación (cliente, previa a red; el servidor valida de verdad)
- `feedUrl`: http/https válida y no vacía (alta manual e importadas).
- Búsqueda: texto no vacío; límite 1..50 aprox.
- Cola: añadir con cola en `limit` → aviso sin romper (FR-019); reorder exige lista completa exacta de ids (igual que favoritos).
- Reorder: `baseRevision` del último GET; `409` → recargar + reintentar (FR-022).
- Descarga: exige `podcastDownloads=true`; nombre `tolocharadio_*` saneado y único en Descargas.
- OPML: fichero ≤ límites (`podcastOpmlMaxFeeds`); `xmlUrl` duplicadas se colapsan en cliente.

## Transiciones clave
- Episodio: SinEmpezar → EnProgreso (primera posición guardada) → Completado (fin natural) ; EnProgreso → descartado (sale de continuar, conserva progreso, no es completado).
- Cola: Pendiente ⇄ EnReproducción (oculto de pendientes, hueco conservado) → Completado/retirado (fin natural lo saca y avanza al siguiente).
- Descarga: según `DownloadRecord` arriba.
