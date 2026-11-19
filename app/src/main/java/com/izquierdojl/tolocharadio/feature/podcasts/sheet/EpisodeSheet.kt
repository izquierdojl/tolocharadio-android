package com.izquierdojl.tolocharadio.feature.podcasts.sheet

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.izquierdojl.tolocharadio.core.ui.components.DownloadUiState
import com.izquierdojl.tolocharadio.core.ui.components.FavoriteButton
import com.izquierdojl.tolocharadio.core.ui.components.downloadUiState
import com.izquierdojl.tolocharadio.core.ui.components.formatEpisodeDuration
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.repo.QueuePosition
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodeActionsViewModel
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Ficha del episodio en hoja inferior modal (US5, FR-013/FR-017):
 * portada, programa, fecha, duración, progreso y descripción sin
 * detener la reproducción ni salir de la página. Al cerrarse devuelve
 * el foco al elemento que la abrió (el llamante conserva su estado).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("CyclomaticComplexMethod")
fun EpisodeSheet(
    episode: PodcastEpisode,
    podcast: PodcastSummary?,
    downloadsEnabled: Boolean,
    showDismiss: Boolean = false,
    onClose: () -> Unit,
    player: EpisodePlayerViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
    actions: EpisodeActionsViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val playerUi by player.ui.collectAsState()
    val playingThis = playerUi.episode?.id == episode.id && playerUi.playing
    val downloadRecords by actions.downloadRecords.collectAsState()
    val downloadProgress by actions.downloadProgress.collectAsState()
    val downloadState = downloadUiState(downloadRecords[episode.id], downloadProgress[episode.id])
    val dateText =
        remember(episode.publishedAt) {
            episode.publishedAt?.let {
                SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(it))
            }
        }
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            AsyncImage(
                model = episode.imageUrl ?: podcast?.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(160.dp)
                        .clip(MaterialTheme.shapes.medium),
            )
            Spacer(Modifier.height(12.dp))
            Text(episode.title, style = MaterialTheme.typography.headlineSmall)
            podcast?.let { Text(it.title, style = MaterialTheme.typography.bodyMedium) }
            Text(
                listOfNotNull(
                    dateText,
                    formatEpisodeDuration(episode.durationSec),
                    episode.episodeNumber?.let { "E$it" },
                    episode.seasonNumber?.let { "T$it" },
                    if (episode.explicit) "Explícito" else null,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
            )
            episode.description?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            val completed = episode.progress?.completed == true
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledIconButton(
                    onClick = {
                        if (playingThis) {
                            player.toggle()
                        } else {
                            player.play(episode, podcast)
                        }
                    },
                    modifier = Modifier.size(44.dp),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                ) {
                    Icon(
                        if (playingThis) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playingThis) "Pausar" else "Reproducir",
                    )
                }
                FavoriteButton(
                    isFavorite = episode.isFavorite,
                    onToggle = { actions.toggleFavorite(episode) },
                    modifier = Modifier.size(38.dp),
                )
                IconButton(
                    onClick = { actions.addToQueue(episode.id, QueuePosition.NEXT) },
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(Icons.Filled.PlaylistPlay, contentDescription = "A continuación")
                }
                IconButton(
                    onClick = { actions.addToQueue(episode.id, QueuePosition.END) },
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(Icons.Filled.PlaylistAdd, contentDescription = "Al final")
                }
                IconButton(
                    onClick = {
                        actions.togglePlayed(episode)
                        onClose()
                    },
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        if (completed) Icons.Filled.RadioButtonUnchecked else Icons.Filled.CheckCircle,
                        contentDescription =
                            if (completed) {
                                "Marcar como pendiente"
                            } else {
                                "Marcar como reproducido"
                            },
                        tint =
                            if (completed) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (downloadsEnabled) {
                    CompactDownloadAction(downloadState) { actions.download(episode) }
                }
                if (showDismiss) {
                    IconButton(
                        onClick = {
                            actions.dismiss(episode.id)
                            onClose()
                        },
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Descartar",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Acción de descarga compacta de la ficha: icono, progreso o descargado. */
@Composable
private fun CompactDownloadAction(
    state: DownloadUiState,
    onDownload: () -> Unit,
) {
    when (state) {
        DownloadUiState.NotDownloaded ->
            IconButton(onClick = onDownload, modifier = Modifier.size(38.dp)) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = "Descargar",
                    modifier = Modifier.size(20.dp),
                )
            }
        is DownloadUiState.Downloading -> {
            val fraction = state.fraction
            val label =
                if (fraction == null) {
                    "Descargando"
                } else {
                    "Descargando ${(fraction * 100).roundToInt()} %"
                }
            Box(
                modifier =
                    Modifier
                        .size(38.dp)
                        .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                if (fraction == null) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    CircularProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
        DownloadUiState.Downloaded ->
            Icon(
                Icons.Filled.OfflinePin,
                contentDescription = "Descargado",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
            )
        is DownloadUiState.Failure ->
            IconButton(onClick = onDownload, modifier = Modifier.size(38.dp)) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = "Reintentar descarga",
                    modifier = Modifier.size(20.dp),
                )
            }
    }
}
