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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.feature.favorites.relativeTime
import com.izquierdojl.tolocharadio.feature.stats.StatsFormat

private const val BAR_HEIGHT_DP = 6
private const val RANK_WIDTH_DP = 24
private const val MIN_BAR_FRACTION = 0.02f

/** Elemento de un ranking por tiempo escuchado. */
data class RankingItem(
    val label: String,
    val totalMs: Long,
)

/** Elemento de un reparto por categoría. */
data class ShareItem(
    val label: String,
    val totalMs: Long,
)

/** Elemento de actividad reciente. */
data class RecentItem(
    val label: String,
    val startedAt: Long,
    val durationMs: Long,
)

/** Ranking genérico con barra proporcional, usado por radio y podcast. */
@Composable
fun RankingBars(
    title: String,
    items: List<RankingItem>,
    modifier: Modifier = Modifier,
) {
    val maxMs = items.maxOfOrNull { it.totalMs }?.coerceAtLeast(1L) ?: 1L
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall)
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
                        entry.label,
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

/** Reparto genérico con barras de color, usado por radio y podcast. */
@Composable
fun ShareBars(
    title: String,
    entries: List<ShareItem>,
    modifier: Modifier = Modifier,
) {
    val maxMs = entries.maxOfOrNull { it.totalMs }?.coerceAtLeast(1L) ?: 1L
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        if (entries.isEmpty()) {
            Text(
                "Sin datos en este periodo",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }
        entries.forEachIndexed { index, entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.label,
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
                                .background(SHARE_COLORS[index % SHARE_COLORS.size]),
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

/** Actividad reciente genérica, usada por radio y podcast. */
@Composable
fun RecentActivityList(
    title: String,
    items: List<RecentItem>,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            Text(
                "Sin datos en este periodo",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }
        items.forEach { entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.label,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        relativeTime(entry.startedAt) ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = muted,
                    )
                }
                Text(
                    StatsFormat.formatDurationMs(entry.durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = muted,
                )
            }
        }
    }
}

/** Paleta de repartos, espejo de la web y del tema Tolocha. */
private val SHARE_COLORS =
    listOf(
        Color(0xFFC0883E),
        Color(0xFF5F8F73),
        Color(0xFFB4C47E),
        Color(0xFFD3A568),
        Color(0xFF8CB29A),
        Color(0xFFA67430),
        Color(0xFF3C6A4D),
        Color(0xFFE2C091),
        Color(0xFFCFDAA2),
        Color(0xFF6B4A1D),
    )
