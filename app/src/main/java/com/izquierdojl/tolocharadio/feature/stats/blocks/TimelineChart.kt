package com.izquierdojl.tolocharadio.feature.stats.blocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTimelineDto
import com.izquierdojl.tolocharadio.feature.stats.StatsFormat

/** Proporción del ancho de barra respecto al hueco (R1). */
private const val BAR_WIDTH_RATIO = 0.65f

private const val CHART_HEIGHT_DP = 160

/**
 * Evolución de la escucha del periodo con barras en `Canvas` (R1, sin
 * dependencias): ejes legibles y sin desplazamiento horizontal (FR-009).
 */
@Composable
fun StatsTimelineChart(
    timeline: StatsTimelineDto,
    granularity: String,
    modifier: Modifier = Modifier,
) {
    val items = timeline.items
    val barColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier.fillMaxWidth()) {
        Text("Evolución de la escucha", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            Text(
                "Sin datos en este periodo",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        } else {
            val maxMs = items.maxOf { it.totalMs }.coerceAtLeast(1L)
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(CHART_HEIGHT_DP.dp)
                    .semantics {
                        contentDescription =
                            "Gráfica de evolución de la escucha del periodo con barras por intervalo"
                    },
            ) {
                val slot = size.width / items.size
                val barWidth = slot * BAR_WIDTH_RATIO
                items.forEachIndexed { index, entry ->
                    val barHeight = size.height * (entry.totalMs.toFloat() / maxMs.toFloat())
                    drawRect(
                        color = barColor,
                        topLeft =
                            Offset(
                                index * slot + (slot - barWidth) / 2f,
                                size.height - barHeight,
                            ),
                        size = Size(barWidth, barHeight),
                    )
                }
                drawLine(
                    color = axisColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    StatsFormat.formatBucketLabel(items.first().bucket, granularity),
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                )
                Text(
                    StatsFormat.formatBucketLabel(items.last().bucket, granularity),
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Máx. ${StatsFormat.formatDurationMs(maxMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
        }
    }
}
