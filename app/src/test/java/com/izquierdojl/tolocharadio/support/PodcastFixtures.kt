package com.izquierdojl.tolocharadio.support

import com.izquierdojl.tolocharadio.data.remote.api.PodcastCategories
import com.izquierdojl.tolocharadio.data.remote.api.PodcastHabits
import com.izquierdojl.tolocharadio.data.remote.api.PodcastRecents
import com.izquierdojl.tolocharadio.data.remote.api.PodcastTopEpisodes
import com.izquierdojl.tolocharadio.data.remote.api.PodcastTopPodcasts
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastCategoryEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodeProgress
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastHabitEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastQueue
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastRecentEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSearchResult
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastStatsSummaryDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastTimelineDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastTimelineEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastTopEpisodeDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastTopPodcastDto

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

/** Resumen de estadísticas de podcast de prueba. */
fun testPodcastStatsSummary(
    totalMs: Long = 3_600_000L,
    episodesStarted: Int = 3,
    episodesCompleted: Int = 2,
    completionRate: Double = 0.66,
    podcastsCount: Int = 2,
    averageEpisodeMs: Long = 1_200_000L,
): PodcastStatsSummaryDto =
    PodcastStatsSummaryDto(
        totalMs = totalMs,
        episodesStarted = episodesStarted,
        episodesCompleted = episodesCompleted,
        completionRate = completionRate,
        podcastsCount = podcastsCount,
        averageEpisodeMs = averageEpisodeMs,
    )

/** Serie temporal de podcast de prueba. */
fun testPodcastTimeline(
    granularity: String = "day",
    vararg entries: Pair<String, Long>,
): PodcastTimelineDto =
    PodcastTimelineDto(
        granularity = granularity,
        items = entries.map { PodcastTimelineEntryDto(it.first, it.second) },
    )

/** Hábitos de podcast de prueba. */
fun testPodcastHabits(vararg entries: Triple<Int, Int, Long>): PodcastHabits =
    PodcastHabits(entries.map { PodcastHabitEntryDto(it.first, it.second, it.third) })

/** Categorías de podcast de prueba. */
fun testPodcastCategories(vararg entries: Pair<String, Long>): PodcastCategories =
    PodcastCategories(entries.map { PodcastCategoryEntryDto(it.first, it.second) })

/** Actividad reciente de podcast de prueba. */
fun testPodcastRecents(vararg entries: Pair<PodcastEpisode?, Long>): PodcastRecents =
    PodcastRecents(
        entries.mapIndexed { index, (episode, listenedMs) ->
            PodcastRecentEntryDto(
                episodeId = episode?.id ?: (index + 1L),
                podcastId = episode?.podcastId ?: 1L,
                episodeTitle = episode?.title ?: "Episodio",
                podcastTitle = "Programa",
                startedAt = index.toLong(),
                listenedMs = listenedMs,
            )
        },
    )

/** Ranking de programas de prueba. */
fun testPodcastTopPodcasts(vararg entries: Pair<PodcastSummary, Long>): PodcastTopPodcasts =
    PodcastTopPodcasts(
        entries.map {
            PodcastTopPodcastDto(
                podcastId = it.first.id,
                title = it.first.title,
                totalMs = it.second,
            )
        },
    )

/** Ranking de episodios de prueba. */
fun testPodcastTopEpisodes(vararg entries: Pair<PodcastEpisode, Long>): PodcastTopEpisodes =
    PodcastTopEpisodes(
        entries.map {
            PodcastTopEpisodeDto(
                episodeId = it.first.id,
                podcastId = it.first.podcastId,
                title = it.first.title,
                podcastTitle = "Programa",
                totalMs = it.second,
            )
        },
    )

/** Resultado de ranking de prueba con todos los campos del contrato. */
fun testPodcastSearchResult(
    title: String = "Podcast",
    author: String? = "Autor",
    feedUrl: String = "https://example.com/feed.xml",
    imageUrl: String? = null,
    categories: List<String> = emptyList(),
    description: String? = null,
): PodcastSearchResult =
    PodcastSearchResult(
        title = title,
        author = author,
        feedUrl = feedUrl,
        imageUrl = imageUrl,
        categories = categories,
        description = description,
    )
