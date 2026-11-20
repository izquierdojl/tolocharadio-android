package com.izquierdojl.tolocharadio.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * DTOs de podcast, espejo de `apps/web/src/lib/types.ts`.
 * Campos opcionales con defaults: el servidor puede ampliar el contrato.
 *
 * Programa con estado de suscripción de la cuenta activa.
 */
@Serializable
data class PodcastSummary(
    val id: Long,
    val feedUrl: String,
    val title: String,
    val author: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val language: String? = null,
    val link: String? = null,
    val categories: List<String> = emptyList(),
    val lastFetchedAt: Long? = null,
    val episodeCount: Int = 0,
    val isSubscribed: Boolean = false,
    val subscribedAt: Long? = null,
    val latestPendingEpisodeAt: Long? = null,
)

/** Progreso de escucha de un episodio. */
@Serializable
data class PodcastEpisodeProgress(
    val positionSec: Long = 0,
    val durationSec: Long? = null,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val updatedAt: Long = 0,
)

/** Episodio con progreso y favorito (las listas ya lo incluyen). */
@Serializable
data class PodcastEpisode(
    val id: Long,
    val podcastId: Long,
    val title: String,
    val description: String? = null,
    val publishedAt: Long? = null,
    val durationSec: Long? = null,
    val imageUrl: String? = null,
    val episodeNumber: Int? = null,
    val seasonNumber: Int? = null,
    val explicit: Boolean = false,
    val progress: PodcastEpisodeProgress? = null,
    val isFavorite: Boolean = false,
)

/** Resultado de directorio (sin id de servidor). */
@Serializable
data class PodcastSearchResult(
    val title: String,
    val author: String? = null,
    val feedUrl: String,
    val imageUrl: String? = null,
    val categories: List<String> = emptyList(),
    val description: String? = null,
)

/** Página `{items,pagination}` de episodios. */
@Serializable
data class PodcastEpisodePage(
    val items: List<PodcastEpisode> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
)

/** Episodio de búsqueda global con su programa embebido. */
@Serializable
data class PodcastEpisodeSearchItem(
    val id: Long,
    val podcastId: Long,
    val title: String,
    val description: String? = null,
    val publishedAt: Long? = null,
    val durationSec: Long? = null,
    val imageUrl: String? = null,
    val episodeNumber: Int? = null,
    val seasonNumber: Int? = null,
    val explicit: Boolean = false,
    val progress: PodcastEpisodeProgress? = null,
    val isFavorite: Boolean = false,
    val podcast: PodcastSummary,
)

/** Página de búsqueda global de episodios. */
@Serializable
data class PodcastEpisodeSearchPage(
    val items: List<PodcastEpisodeSearchItem> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
)

/** Cola sincronizada por revisión optimista del servidor. */
@Serializable
data class PodcastQueue(
    val revision: Long = 0,
    val updatedAt: Long? = null,
    val limit: Int = 200,
    val items: List<PodcastEpisode> = emptyList(),
)

/** Resumen de cola para la portada. */
@Serializable
data class PodcastQueueSummary(
    val revision: Long = 0,
    val count: Int = 0,
    val limit: Int = 200,
    val next: PodcastEpisode? = null,
)

/** Portada agregada (`/podcasts/home`). */
@Serializable
data class PodcastHome(
    val continueListening: List<PodcastEpisode> = emptyList(),
    val latestEpisodes: List<PodcastEpisode> = emptyList(),
    val subscriptions: List<PodcastSummary> = emptyList(),
    val queue: PodcastQueueSummary = PodcastQueueSummary(),
)

/** Errores por feed de una importación en lote. */
@Serializable
data class BulkSubscriptionError(
    val feedUrl: String,
    val message: String,
)

/** Resultado de `POST /podcasts/subscriptions/bulk`. */
@Serializable
data class BulkSubscriptionResult(
    val imported: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val items: List<PodcastSummary> = emptyList(),
    val errors: List<BulkSubscriptionError> = emptyList(),
)

/** URL firmada de reproducción o descarga (TTL 6 h, contextos HMAC distintos). */
@Serializable
data class IssuedToken(
    val url: String,
    val expiresAt: Long,
)

/** Resumen privado de escucha de podcast (`GET /podcasts/stats/summary`). */
@Serializable
data class PodcastStatsSummaryDto(
    val totalMs: Long = 0,
    val episodesStarted: Int = 0,
    val episodesCompleted: Int = 0,
    val completionRate: Double = 0.0,
    val podcastsCount: Int = 0,
    val averageEpisodeMs: Long = 0,
)

/** Programa con tiempo escuchado (ranking plano de `top-podcasts`). */
@Serializable
data class PodcastTopPodcastDto(
    val podcastId: Long,
    val title: String,
    val author: String? = null,
    val imageUrl: String? = null,
    val totalMs: Long = 0,
    val episodesStarted: Int = 0,
    val episodesCompleted: Int = 0,
)

/** Episodio con tiempo escuchado (ranking plano de `top-episodes`). */
@Serializable
data class PodcastTopEpisodeDto(
    val episodeId: Long,
    val podcastId: Long,
    val title: String,
    val podcastTitle: String,
    val imageUrl: String? = null,
    val totalMs: Long = 0,
    val completed: Boolean = false,
)

/** Cubo temporal `{bucket,totalMs}` (día, semana o mes). */
@Serializable
data class PodcastTimelineEntryDto(
    val bucket: String,
    val totalMs: Long = 0,
)

/** Serie temporal de escucha de podcast. */
@Serializable
data class PodcastTimelineDto(
    val granularity: String = "day",
    val items: List<PodcastTimelineEntryDto> = emptyList(),
)

/** Escucha por día de semana (0..6 = lun..dom) y hora (0..23). */
@Serializable
data class PodcastHabitEntryDto(
    val weekday: Int = 0,
    val hour: Int = 0,
    val totalMs: Long = 0,
)

/** Tiempo por categoría. */
@Serializable
data class PodcastCategoryEntryDto(
    val category: String,
    val totalMs: Long = 0,
)

/** Sesión de escucha reciente (`GET /podcasts/stats/recent`). */
@Serializable
data class PodcastRecentEntryDto(
    val episodeId: Long,
    val podcastId: Long,
    val episodeTitle: String,
    val podcastTitle: String,
    val imageUrl: String? = null,
    val startedAt: Long = 0,
    val endedAt: Long = 0,
    val listenedMs: Long = 0,
    val positionSec: Long? = null,
    val durationSec: Long? = null,
    val completed: Boolean = false,
)

/** Cuerpo de `PUT /podcasts/queue/order` (revisión optimista, FR-022). */
@Serializable
data class ReorderQueueBody(
    val baseRevision: Long,
    val episodeIds: List<Long>,
)

/** Cuerpo de `POST /podcasts/subscriptions` (una de las dos). */
@Serializable
data class SubscribePodcastBody(
    val podcastId: Long? = null,
    val feedUrl: String? = null,
)

/** Cuerpo de `POST /podcasts/queue` (`position`: end|next). */
@Serializable
data class AddToQueueBody(
    val episodeId: Long,
    val position: String = "end",
)

/** Cuerpo de `PUT/POST …/progress` (idempotente por `sequence`). */
@Serializable
data class ReportEpisodeProgressBody(
    val sessionId: String,
    val sequence: Long,
    val positionSec: Long,
    val listenedMs: Long,
    val durationSec: Long? = null,
    val completed: Boolean? = null,
    val occurredAt: Long? = null,
    val source: String? = null,
)
