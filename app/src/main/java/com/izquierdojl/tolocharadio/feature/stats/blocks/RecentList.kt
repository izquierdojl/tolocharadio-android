package com.izquierdojl.tolocharadio.feature.stats.blocks

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.izquierdojl.tolocharadio.data.remote.dto.StatsRecentListDto

/** Actividad reciente de radio con la primitiva compartida. */
@Composable
fun RecentList(
    recent: StatsRecentListDto,
    modifier: Modifier = Modifier,
) {
    RecentActivityList(
        title = "Actividad reciente",
        items = recent.items.map { RecentItem(it.station.name, it.startedAt, it.durationMs) },
        modifier = modifier,
    )
}
