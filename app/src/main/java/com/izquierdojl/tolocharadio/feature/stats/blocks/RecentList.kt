package com.izquierdojl.tolocharadio.feature.stats.blocks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.data.remote.dto.StatsRecentListDto
import com.izquierdojl.tolocharadio.feature.favorites.relativeTime
import com.izquierdojl.tolocharadio.feature.stats.StatsFormat

/**
 * Reproducciones recientes (paridad con `RecentList.tsx`): emisora,
 * fecha relativa y duración. El orden descendente por `startedAt` lo
 * garantiza el ViewModel (US3).
 */
@Composable
fun RecentList(
    recent: StatsRecentListDto,
    modifier: Modifier = Modifier,
) {
    val items = recent.items
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier.fillMaxWidth()) {
        Text("Actividad reciente", style = MaterialTheme.typography.titleSmall)
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
                        entry.station.name,
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
