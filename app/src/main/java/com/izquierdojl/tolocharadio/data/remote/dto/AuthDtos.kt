package com.izquierdojl.tolocharadio.data.remote.dto

import kotlinx.serialization.Serializable

/** Capacidades opcionales publicadas en `GET /config` (ausente = desactivado). */
@Serializable
data class PodcastFeaturesDto(
    val radio: Boolean = true,
    val podcasts: Boolean = false,
    val podcastDiscovery: String? = null,
    val podcastOpml: Boolean = false,
    val podcastQueue: Boolean = false,
    val podcastFavorites: Boolean = false,
    val podcastStats: Boolean = false,
    val podcastDownloads: Boolean = false,
)

/** Límites opcionales publicados en `GET /config`. */
@Serializable
data class PodcastLimitsDto(
    val podcastQueueMax: Int = 200,
    val podcastSubscriptionsMax: Int = 0,
    val podcastFeedMaxBytes: Int = 0,
    val podcastEpisodesPageSize: Int = 0,
    val podcastOpmlMaxFeeds: Int = 0,
)

/** Configuración pública de la instancia. */
@Serializable
data class AppConfigDto(
    val appName: String,
    val features: PodcastFeaturesDto? = null,
    val limits: PodcastLimitsDto? = null,
)

/** `true` si el servidor activo ofrece el modo Podcast. */
fun AppConfigDto.podcastsEnabled(): Boolean = features?.podcasts == true

/** `true` si el servidor activo permite descargar episodios. */
fun AppConfigDto.podcastDownloadsEnabled(): Boolean = features?.podcastDownloads == true

/**
 * `true` si el servidor ofrece el ranking de podcasts de Apple.
 * Requiere podcasts activos y el proveedor `itunes`; cualquier otro
 * proveedor o un campo ausente se considera no soportado.
 */
fun AppConfigDto.podcastTopChartsEnabled(): Boolean {
    val features = features ?: return false
    return features.podcasts && features.podcastDiscovery == "itunes"
}

/** Máximo de cola cuando el servidor no publica límite. */
const val DEFAULT_PODCAST_QUEUE_MAX = 200

/** Máximo de cola publicado por el servidor (defecto 200). */
fun AppConfigDto.podcastQueueMax(): Int {
    val max = limits?.podcastQueueMax ?: DEFAULT_PODCAST_QUEUE_MAX
    return if (max > 0) max else DEFAULT_PODCAST_QUEUE_MAX
}

/** Entrada de `{id,playable,reason}` de `playback/:id/status`. */
@Serializable
data class PlaybackStatusDto(
    val id: String,
    val playable: Boolean,
    val reason: String? = null,
)

/**
 * Respuesta de `POST /auth/login` y `POST /auth/refresh`.
 * El campo `user` del backend se ignora (`ignoreUnknownKeys`); la app
 * no modela perfil.
 */
@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
)
