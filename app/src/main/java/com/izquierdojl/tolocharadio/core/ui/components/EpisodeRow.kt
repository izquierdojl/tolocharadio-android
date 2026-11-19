package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Segundos por hora/minuto para el formato de duración. */
private const val SECONDS_PER_HOUR = 3600L
private const val SECONDS_PER_MINUTE = 60L

/** Formato corto de duración en segundos (1 h 05 min / 12:34). */
fun formatEpisodeDuration(totalSec: Long?): String {
    if (totalSec == null || totalSec <= 0) return "--:--"
    val hours = totalSec / SECONDS_PER_HOUR
    val minutes = (totalSec % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = totalSec % SECONDS_PER_MINUTE
    return if (hours > 0) {
        "$hours h %02d min".format(minutes)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

/**
 * Fila compacta de episodio (paridad con `EpisodeRow` web): título a una
 * línea, apoyo a una línea, reproducir/pausar, encolar (toque = al final;
 * pulsación larga = elegir posición), descarga, favorito, estado
 * reproducido/pendiente y ficha al tocarla.
 */
@Composable
@Suppress("LongParameterList", "CyclomaticComplexMethod")
fun EpisodeRow(
    episode: PodcastEpisode,
    podcast: PodcastSummary?,
    isPlaying: Boolean,
    isDownloaded: Boolean = false,
    downloadState: DownloadUiState = if (isDownloaded) DownloadUiState.Downloaded else DownloadUiState.NotDownloaded,
    onPlayPause: () -> Unit,
    onOpenDetail: () -> Unit,
    onToggleFavorite: () -> Unit,
    onQueueNext: (() -> Unit)? = null,
    onQueueEnd: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onTogglePlayed: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val dateText =
        remember(episode.publishedAt) {
            episode.publishedAt?.let {
                SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(it))
            }
        }
    val completed = episode.progress?.completed == true
    val downloaded = isDownloaded || downloadState is DownloadUiState.Downloaded
    ListItem(
        modifier = modifier.clickable(onClick = onOpenDetail),
        headlineContent = {
            Text(episode.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Column {
                podcast?.title?.let {
                    Text(
                        it,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    listOfNotNull(
                        dateText,
                        formatEpisodeDuration(episode.durationSec),
                        if (downloaded) "Descargado" else null,
                        if (completed) "Completado" else null,
                    ).joinToString(" · "),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                val progress = episode.progress
                if (progress != null && !progress.completed && (progress.durationSec ?: 0) > 0) {
                    LinearProgressIndicator(
                        progress = { progress.positionSec.toFloat() / progress.durationSec!!.toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        )
                    }
                    when {
                        downloadState is DownloadUiState.Downloading ->
                            DownloadProgressButton(downloadState, onDownload ?: {})
                        downloadState is DownloadUiState.Failure && onDownload != null ->
                            DownloadProgressButton(downloadState, onDownload)
                        onDownload != null && !downloaded ->
                            DownloadProgressButton(DownloadUiState.NotDownloaded, onDownload)
                        downloaded ->
                            Icon(
                                Icons.Filled.OfflinePin,
                                contentDescription = "Descargado",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp),
                            )
                    }
                    if (onQueueNext != null || onQueueEnd != null) {
                        QueueAction(onNext = onQueueNext, onEnd = onQueueEnd)
                    }
                    FavoriteButton(isFavorite = episode.isFavorite, onToggle = onToggleFavorite)
                    if (onTogglePlayed != null) {
                        IconButton(onClick = onTogglePlayed) {
                            Icon(
                                if (completed) {
                                    Icons.Filled.CheckCircle
                                } else {
                                    Icons.Filled.RadioButtonUnchecked
                                },
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
                    }
                }
            }
        },
        leadingContent = {
            AsyncImage(
                model = episode.imageUrl ?: podcast?.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small),
            )
        },
    )
}

/**
 * Acción de cola: un toque añade al final; una pulsación larga abre el
 * menú para elegir entre "a continuación" y "al final".
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QueueAction(
    onNext: (() -> Unit)?,
    onEnd: (() -> Unit)?,
) {
    val default = onEnd ?: onNext ?: return
    val canChoose = onNext != null && onEnd != null
    var expanded by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    Box {
        Icon(
            Icons.Filled.PlaylistAdd,
            contentDescription = "Añadir a la cola. Mantén pulsado para elegir posición.",
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = default,
                        onLongClick = {
                            if (canChoose) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                expanded = true
                            } else {
                                default()
                            }
                        },
                    ).padding(12.dp),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            onNext?.let {
                DropdownMenuItem(
                    text = { Text("A continuación") },
                    onClick = {
                        expanded = false
                        it()
                    },
                )
            }
            onEnd?.let {
                DropdownMenuItem(
                    text = { Text("Al final") },
                    onClick = {
                        expanded = false
                        it()
                    },
                )
            }
        }
    }
}
