package com.izquierdojl.tolocharadio.feature.podcasts.player

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.izquierdojl.tolocharadio.core.ui.components.formatEpisodeDuration
import com.izquierdojl.tolocharadio.feature.player.EpisodePlayerControls
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerViewModel

/** Milisegundos por segundo. */
private const val MS_PER_SEC = 1_000L

/** Segundos por minuto. */
private const val SECONDS_PER_MINUTE = 60L

/** Segundos por hora. */
private const val SECONDS_PER_HOUR = 3_600L

/**
 * Reproductor completo del episodio (US3, FR-014/FR-017): se abre como
 * "now playing" a pantalla completa, con carátula grande, pista con
 * tiempos, saltos ±15/30 s, velocidad y origen (local/online).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeFullPlayerSheet(
    viewModel: EpisodePlayerViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
    onDismiss: () -> Unit,
) {
    val ui by viewModel.ui.collectAsState()
    val voiceBoost by viewModel.voiceBoostEnabled.collectAsState()
    val episode = ui.episode
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        if (episode == null) return@ModalBottomSheet
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AsyncImage(
                model = episode.imageUrl ?: ui.podcast?.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(240.dp)
                        .clip(MaterialTheme.shapes.medium),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                episode.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            ui.podcast?.let {
                Text(it.title, style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ui.downloaded) {
                    Icon(
                        Icons.Filled.OfflinePin,
                        contentDescription = "Episodio descargado",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    (if (ui.downloaded) "Reproduciendo descarga · " else "Online · ") +
                        formatEpisodeDuration(episode.durationSec),
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (ui.downloaded) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = ui.positionMs.toFloat(),
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..(ui.durationMs.coerceAtLeast(1L).toFloat()),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth()) {
                Text(
                    formatMillis(ui.positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    formatMillis(ui.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            EpisodePlayerControls(
                playing = ui.playing,
                rate = ui.rate,
                casting = false,
                onToggle = viewModel::toggle,
                onSkipBack = viewModel::skipBack,
                onSkipForward = viewModel::skipForward,
                onCycleRate = viewModel::cycleRate,
            )
            VoiceBoostToggle(
                enabled = voiceBoost,
                onToggle = viewModel::setVoiceBoost,
            )
            ui.message?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.clearMessage() }) {
                    Text("Cerrar aviso")
                }
            }
            TextButton(onClick = {
                viewModel.stop()
                onDismiss()
            }) {
                Text("Detener")
            }
        }
    }
}

/** Formatea una posición en ms como `m:ss` o `h:mm:ss`. */
private fun formatMillis(positionMs: Long): String {
    val totalSec = (positionMs / MS_PER_SEC).coerceAtLeast(0)
    val hours = totalSec / SECONDS_PER_HOUR
    val minutes = (totalSec % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = totalSec % SECONDS_PER_MINUTE
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
