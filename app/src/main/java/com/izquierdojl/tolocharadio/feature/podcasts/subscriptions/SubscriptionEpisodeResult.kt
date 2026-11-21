package com.izquierdojl.tolocharadio.feature.podcasts.subscriptions

import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodeSearchItem
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary

/** Episodio encontrado en las suscripciones, con su programa embebido. */
data class SubscriptionEpisodeResult(
    val episode: PodcastEpisode,
    val podcast: PodcastSummary,
)

/** Adapta el resultado de búsqueda al modelo que consume `EpisodeRow`. */
fun PodcastEpisodeSearchItem.toSubscriptionResult(): SubscriptionEpisodeResult =
    SubscriptionEpisodeResult(
        episode =
            PodcastEpisode(
                id = id,
                podcastId = podcastId,
                title = title,
                description = description,
                publishedAt = publishedAt,
                durationSec = durationSec,
                imageUrl = imageUrl,
                episodeNumber = episodeNumber,
                seasonNumber = seasonNumber,
                explicit = explicit,
                progress = progress,
                isFavorite = isFavorite,
            ),
        podcast = podcast,
    )
