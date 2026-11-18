package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary

/** Datos de pintado de una tarjeta de episodio. */
data class EpisodeCardUi(
    val episode: PodcastEpisode,
    val podcast: PodcastSummary? = null,
    val isPlaying: Boolean = false,
    val isDownloaded: Boolean = false,
)

/**
 * Tarjeta de episodio para las vistas de cuadrícula (inicio y favoritos):
 * tocar abre la ficha, el botón central reproduce/pausa y el corazón
 * marca el favorito. Las acciones secundarias viven en la vista de lista.
 */
@Composable
fun EpisodeCard(
    ui: EpisodeCardUi,
    onPlayPause: () -> Unit,
    onOpenDetail: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val episode = ui.episode
    Card(
        onClick = onOpenDetail,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            ) {
                AsyncImage(
                    model = episode.imageUrl ?: ui.podcast?.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
                FavoriteButton(
                    isFavorite = episode.isFavorite,
                    onToggle = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).size(40.dp),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                ) {
                    Icon(
                        if (ui.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (ui.isPlaying) "Pausar" else "Reproducir",
                    )
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(
                    episode.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        listOfNotNull(
                            ui.podcast?.title,
                            formatEpisodeDuration(episode.durationSec),
                            if (ui.isDownloaded) "Descargado" else null,
                            if (episode.progress?.completed == true) "Completado" else null,
                        ).joinToString(" · "),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
