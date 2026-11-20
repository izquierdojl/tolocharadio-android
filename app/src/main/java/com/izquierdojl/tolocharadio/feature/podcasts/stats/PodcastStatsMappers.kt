package com.izquierdojl.tolocharadio.feature.podcasts.stats

import com.izquierdojl.tolocharadio.data.remote.api.PodcastCategories
import com.izquierdojl.tolocharadio.data.remote.api.PodcastHabits
import com.izquierdojl.tolocharadio.data.remote.api.PodcastRecents
import com.izquierdojl.tolocharadio.data.remote.api.PodcastTopEpisodes
import com.izquierdojl.tolocharadio.data.remote.api.PodcastTopPodcasts
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastStatsSummaryDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastTimelineDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsHabitEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsHabitListDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTimelineDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTimelineEntryDto
import com.izquierdojl.tolocharadio.feature.stats.StatsFormat
import com.izquierdojl.tolocharadio.feature.stats.blocks.RankingItem
import com.izquierdojl.tolocharadio.feature.stats.blocks.RecentItem
import com.izquierdojl.tolocharadio.feature.stats.blocks.ShareItem
import com.izquierdojl.tolocharadio.feature.stats.blocks.StatsSummaryItem
import kotlin.math.roundToInt

/** Mapeos de los DTO de estadísticas de podcast a las primitivas de gráficas. */
fun PodcastStatsSummaryDto.toSummaryItems(): List<StatsSummaryItem> =
    listOf(
        StatsSummaryItem(
            label = "Tiempo total",
            value = StatsFormat.formatDurationMs(totalMs),
            hint = "Datos aproximados",
        ),
        StatsSummaryItem(
            label = "Episodios",
            value = "$episodesStarted empezados · $episodesCompleted completados",
            hint = "Tasa de finalización: ${(completionRate * 100).roundToInt()} %",
        ),
        StatsSummaryItem(
            label = "Programas",
            value = podcastsCount.toString(),
            hint = "Media por episodio: ${StatsFormat.formatDurationMs(averageEpisodeMs)}",
        ),
    )

fun PodcastTimelineDto.toStatsTimeline(): StatsTimelineDto =
    StatsTimelineDto(
        granularity = granularity,
        items = items.map { StatsTimelineEntryDto(bucket = it.bucket, totalMs = it.totalMs) },
    )

fun PodcastHabits.toStatsHabits(): StatsHabitListDto =
    StatsHabitListDto(
        items = items.map { StatsHabitEntryDto(weekday = it.weekday, hour = it.hour, totalMs = it.totalMs) },
    )

fun PodcastTopPodcasts.toRankingItems(): List<RankingItem> {
    return items.map { RankingItem(label = it.title, totalMs = it.totalMs) }
}

fun PodcastTopEpisodes.toRankingItems(): List<RankingItem> {
    return items.map { RankingItem(label = it.title, totalMs = it.totalMs) }
}

fun PodcastCategories.toShareItems(): List<ShareItem> {
    return items.map { ShareItem(label = it.category, totalMs = it.totalMs) }
}

fun PodcastRecents.toRecentItems(): List<RecentItem> =
    items.map {
        RecentItem(
            label = it.episodeTitle,
            startedAt = it.startedAt,
            durationMs = it.listenedMs,
        )
    }
