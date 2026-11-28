package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Fila de episodio (spec 0062): imagen 56 dp, título a dos líneas, podcast a
 * una línea, meta/progreso y play de 48 dp a la derecha. El resto de acciones
 * se abre con pulsación larga para no ocupar una fila entera.
 */
@OptIn(ExperimentalFoundationApi::class)
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
    val downloadStatus =
        when (downloadState) {
            is DownloadUiState.Downloading -> "Descargando…"
            is DownloadUiState.Failure -> "Error de descarga"
            DownloadUiState.Downloaded -> "Descargado"
            DownloadUiState.NotDownloaded -> null
        }
    val meta =
        listOfNotNull(
            dateText,
            formatEpisodeDuration(episode.durationSec),
            if (downloaded) "Descargado" else downloadStatus,
            if (completed) "Completado" else null,
        ).joinToString(" · ")
    var showActions by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    ListItem(
        modifier =
            modifier.combinedClickable(
                onClick = onOpenDetail,
                onLongClickLabel = "Más acciones",
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showActions = true
                },
            ),
        headlineContent = {
            Text(episode.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Column {
                podcast?.title?.let {
                    Text(
                        it,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    meta,
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
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp).height(3.dp),
                    )
                }
                if (downloadState is DownloadUiState.Downloading) {
                    val fraction = downloadState.fraction
                    if (fraction != null) {
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp).height(3.dp),
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp).height(3.dp),
                        )
                    }
                }
            }
        },
        leadingContent = {
            AsyncImage(
                model = episode.imageUrl ?: podcast?.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small),
            )
        },
        trailingContent = {
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(48.dp),
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    modifier = Modifier.size(24.dp),
                )
            }
        },
    )
    if (showActions) {
        EpisodeActionsSheet(
            episode = episode,
            podcast = podcast,
            isDownloaded = downloaded,
            downloadState = downloadState,
            onDownload = onDownload,
            onQueueNext = onQueueNext,
            onQueueEnd = onQueueEnd,
            onToggleFavorite = onToggleFavorite,
            onTogglePlayed = onTogglePlayed,
            onOpenDetail = onOpenDetail,
            onDismiss = { showActions = false },
        )
    }
}
