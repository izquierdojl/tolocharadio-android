package com.izquierdojl.tolocharadio.support

import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodeProgress
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastQueue

/** Episodio de prueba con valores mínimos y progreso opcional. */
fun testEpisode(
    id: Long = 1L,
    podcastId: Long = 7L,
    title: String = "Episodio $id",
    durationSec: Long? = 1_800L,
    progress: PodcastEpisodeProgress? = null,
    isFavorite: Boolean = false,
): PodcastEpisode =
    PodcastEpisode(
        id = id,
        podcastId = podcastId,
        title = title,
        durationSec = durationSec,
        progress = progress,
        isFavorite = isFavorite,
    )

/** Progreso de prueba. */
fun testProgress(
    positionSec: Long,
    durationSec: Long? = 1_800L,
    completed: Boolean = false,
): PodcastEpisodeProgress =
    PodcastEpisodeProgress(
        positionSec = positionSec,
        durationSec = durationSec,
        completed = completed,
    )

/** Cola de prueba con revisión y límite por defecto. */
fun testQueue(
    vararg episodes: PodcastEpisode,
    revision: Long = 1L,
    limit: Int = 200,
): PodcastQueue =
    PodcastQueue(
        revision = revision,
        limit = limit,
        items = episodes.toList(),
    )
