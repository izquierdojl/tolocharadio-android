# Contract: Podcasts API (Android consume el backend existente)

Base: `{baseUrl}/api/v1` (Retrofit del servidor activo, Bearer automático salvo `GET /config`). Errores: `{error:{code,message,status,details?}}` → `DomainError` (401/403 renueva + re-login silencioso; 404 NotFound idempotente en bajas; **409 Conflict → recargar cola**; 422 Validation por campo). Fuente verificada: `apps/web/src/lib/api.ts`, `docs/api.md`.

## Config (extensión)

- `GET /config` → añade `features{podcasts, podcastDiscovery, podcastOpml, podcastQueue, podcastFavorites, podcastStats, podcastDownloads}` y `limits{podcastQueueMax, podcastSubscriptionsMax, podcastFeedMaxBytes, podcastEpisodesPageSize, podcastOpmlMaxFeeds}` (todo opcional; ausente = desactivado). DTO Android con defaults que conservan Radio.

## Descubrir y suscripciones

| Método | Ruta | Cuerpo/Query | Respuesta |
|--------|------|--------------|-----------|
| GET | `/podcasts/search?q&limit` | `q` texto, `limit` | `{items: SearchResult[]}` |
| GET | `/podcasts/episodes/search?q&offset&limit` | búsqueda global de episodios | `{items: Episode(+podcast)[], pagination}` |
| POST | `/podcasts/resolve` | `{feedUrl}` | `{podcast, episodes: EpisodePage}` |
| GET | `/podcasts/subscriptions` | — | `{items: PodcastSummary[]}` |
| POST | `/podcasts/subscriptions` | `{podcastId?\|feedUrl?}` | `{podcast}` |
| DELETE | `/podcasts/subscriptions/:podcastId` | — | `{ok:true}` (404 = ya no estaba) |
| POST | `/podcasts/subscriptions/bulk` | `{feedUrls[]}` | `BulkSubscriptionResult{imported,skipped,failed,items,errors[]}` |
| GET | `/podcasts/subscriptions/opml` | texto OPML 2.0 (no JSON) | `text/plain` |

## Portada, ficha y episodios

| Método | Ruta | Notas |
|--------|------|-------|
| GET | `/podcasts/home` | `{continueListening, latestEpisodes, subscriptions, queue: QueueSummary}` |
| GET | `/podcasts/:podcastId` | `{podcast}` |
| GET | `/podcasts/:podcastId/episodes?sort&filter&q&offset&limit` | `sort` recientes\|duración\|título; `filter` sin empezar\|en progreso\|completados |
| GET | `/podcasts/episodes/:id/progress` | `{progress}` |
| PUT | `/podcasts/episodes/:id/progress` | `{sessionId, sequence, positionSec}` → `{progress}` |
| POST | `/podcasts/episodes/:id/progress` | keepalive (fondo/cierre), mismo cuerpo |
| POST | `/podcasts/episodes/:id/dismiss` | `{dismissed}` — sale de continuar sin completar |

## Cola (sincronizada por `revision`)

| Método | Ruta | Notas |
|--------|------|-------|
| GET | `/podcasts/queue` | `{queue: {revision, updatedAt, limit, items[] (con isFavorite)}}` |
| GET | `/podcasts/queue/count` | `{count, limit}` (insignia) |
| POST | `/podcasts/queue` | `{episodeId, position: end\|next}` → `{queue}` (existente = mover) |
| PUT | `/podcasts/queue/order` | `{baseRevision, episodeIds[]}` → `{queue}`; `409` obsoleta → recargar |
| DELETE | `/podcasts/queue` | vacía; `?keepEpisodeId=` conserva uno en primera posición; no detiene |
| DELETE | `/podcasts/queue/:episodeId` | quita uno → `{queue}` |

## Favoritos de episodios (independientes de emisoras)

| Método | Ruta |
|--------|------|
| GET | `/podcasts/favorites` → `{items: Episode[]}` |
| POST | `/podcasts/favorites` `{episodeId}` → `{episode}` |
| DELETE | `/podcasts/favorites/:episodeId` → `{ok:true}` (404 idempotente) |

## Reproducción y descarga (URLs firmadas HMAC, TTL 6 h; contextos distintos)

| Método | Ruta | Notas |
|--------|------|-------|
| POST | `/podcasts/episodes/:id/playback` | `{url, expiresAt}` → Media3 con DataSource plano (sin Bearer) |
| GET | firmado `/podcasts/episodes/:id/stream` | audio (`Range`, `206/416`); lo usa el player internamente |
| POST | `/podcasts/episodes/:id/download` | `{url, expiresAt}` → DownloadManager del sistema |
| GET | firmado `/podcasts/episodes/:id/download` | archivo original + `Content-Disposition tolocharadio_*` |

## Estadísticas (privadas, cuenta activa)

`GET /podcasts/stats/summary` (tiempo, programas, empezados ≥30 s, completados, tasa, media) · `top-podcasts?limit` · `top-episodes?limit` · `timeline?granularity=day|week|month` · `habits` · `categories?limit` · `recent?limit`.
