package com.izquierdojl.tolocharadio.feature.podcasts.queue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.domain.podcasts.progressFraction
import com.izquierdojl.tolocharadio.feature.podcasts.player.formatPlaybackTime

/**
 * Cabecera fija de la cola con el episodio en curso (US1, FR-001–FR-010).
 * Muestra portada, programa, título, tiempo transcurrido/duración y avance.
 */
@Composable
fun QueueNowPlayingCard(
    episode: PodcastEpisode,
    podcast: PodcastSummary?,
    playing: Boolean,
    positionMs: Long,
    durationMs: Long,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = progressFraction(positionMs, durationMs)
    val stateText = if (playing) "En reproducción" else "Pausado"
    val timeText =
        if (durationMs > 0L) {
            "${formatPlaybackTime(positionMs)} de ${formatPlaybackTime(durationMs)}"
        } else {
            formatPlaybackTime(positionMs)
        }
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag(TolochaTestTags.QUEUE_NOW_PLAYING)
                .clickable(onClick = onOpen)
                .semantics {
                    contentDescription = "$stateText: ${episode.title}. $timeText"
                },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = episode.imageUrl ?: podcast?.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.small),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                podcast?.title?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        formatPlaybackTime(positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    if (durationMs > 0L) {
                        Text(
                            formatPlaybackTime(durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                fraction?.let {
                    LinearProgressIndicator(
                        progress = { it },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .semantics { contentDescription = "Avance del episodio" },
                    )
                }
            }
        }
    }
}
