package com.izquierdojl.tolocharadio.feature.stats.blocks

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTopListDto

/** Ranking de emisoras del periodo con la primitiva compartida. */
@Composable
fun TopStationsChart(
    top: StatsTopListDto,
    modifier: Modifier = Modifier,
) {
    RankingBars(
        title = "Emisoras más escuchadas",
        items = top.items.map { RankingItem(it.station.name, it.totalMs) },
        modifier = modifier,
    )
}
