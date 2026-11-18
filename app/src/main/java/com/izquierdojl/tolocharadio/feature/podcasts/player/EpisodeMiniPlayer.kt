package com.izquierdojl.tolocharadio.feature.podcasts.player

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerViewModel

/**
 * Mini-player de episodio (US3): visible sobre la barra inferior cuando
 * hay un episodio cargado. Tocar la barra abre el "now playing" completo;
 * el play central va flanqueado por atrás/adelante y la X detiene.
 */
@Composable
fun EpisodeMiniPlayer(
    viewModel: EpisodePlayerViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
    onOpenFullPlayer: () -> Unit,
) {
    val ui by viewModel.ui.collectAsState()
    val episode = ui.episode ?: return
    Surface(
        tonalElevation = 3.dp,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenFullPlayer),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = episode.imageUrl ?: ui.podcast?.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.small),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        episode.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (ui.downloaded) {
                            Icon(
                                Icons.Filled.OfflinePin,
                                contentDescription = "Episodio descargado",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            (ui.podcast?.title ?: "") +
                                if (ui.downloaded) " · Descargado" else "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(
                    onClick = viewModel::skipBack,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Atrás 15 segundos",
                        modifier = Modifier.size(20.dp),
                    )
                }
                FilledIconButton(
                    onClick = viewModel::toggle,
                    modifier = Modifier.size(40.dp),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                ) {
                    Icon(
                        if (ui.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (ui.playing) "Pausar" else "Reproducir",
                    )
                }
                IconButton(
                    onClick = viewModel::skipForward,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Adelante 30 segundos",
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = viewModel::stop,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Detener",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            if (ui.durationMs > 0) {
                LinearProgressIndicator(
                    progress = { (ui.positionMs.toFloat() / ui.durationMs.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
