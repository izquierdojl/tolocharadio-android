package com.izquierdojl.tolocharadio.feature.stats.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTopListDto
import com.izquierdojl.tolocharadio.feature.stats.StatsFormat

private const val BAR_HEIGHT_DP = 6
private const val RANK_WIDTH_DP = 24

/** Mínimo de barra visible para categorías con poco tiempo (legibilidad). */
private const val MIN_BAR_FRACTION = 0.02f

/**
 * Ranking de emisoras del periodo con barra proporcional dibujada en
 * composables (R1): orden descendente por `totalMs` (paridad con
 * `TopStations.tsx`).
 */
@Composable
fun TopStationsChart(
    top: StatsTopListDto,
    modifier: Modifier = Modifier,
) {
    val items = top.items
    val maxMs = items.maxOfOrNull { it.totalMs }?.coerceAtLeast(1L) ?: 1L
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier.fillMaxWidth()) {
        Text("Emisoras más escuchadas", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            Text(
                "Sin datos en este periodo",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }
        items.forEachIndexed { index, entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Text(
                    "${index + 1}.",
                    style = MaterialTheme.typography.labelMedium,
                    color = muted,
                    modifier = Modifier.width(RANK_WIDTH_DP.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.station.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(BAR_HEIGHT_DP.dp)
                            .clip(RoundedCornerShape(BAR_HEIGHT_DP.dp))
                            .background(trackColor),
                    ) {
                        val fraction =
                            (entry.totalMs.toFloat() / maxMs).coerceIn(MIN_BAR_FRACTION, 1f)
                        Box(
                            Modifier
                                .fillMaxWidth(fraction)
                                .height(BAR_HEIGHT_DP.dp)
                                .background(barColor),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    StatsFormat.formatDurationMs(entry.totalMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = muted,
                )
            }
        }
    }
}
