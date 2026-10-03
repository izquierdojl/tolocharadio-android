package com.izquierdojl.tolocharadio.feature.stats.blocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.domain.stats.StatsSummary
import com.izquierdojl.tolocharadio.feature.stats.StatsFormat

/**
 * Resumen del periodo en tres tarjetas (paridad con `StatsSummary.tsx`,
 * R4): tiempo total, emisora destacada y día con más escucha.
 */
@Composable
fun StatsSummaryCards(
    summary: StatsSummary,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryCard(
            label = "Tiempo total",
            value = StatsFormat.formatDurationMs(summary.totalMs),
            hint = "Datos aproximados",
        )
        SummaryCard(
            label = "Tu emisora destacada",
            value = summary.topStation?.name ?: "—",
            hint = summary.topStationMs.takeIf { it > 0 }?.let { StatsFormat.formatDurationMs(it) },
        )
        SummaryCard(
            label = "Día con más escucha",
            value = summary.peakBucket?.let { StatsFormat.formatFullDay(it) } ?: "—",
            hint = summary.peakTotalMs.takeIf { it > 0 }?.let { StatsFormat.formatDurationMs(it) },
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    hint: String? = null,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (hint != null) {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
