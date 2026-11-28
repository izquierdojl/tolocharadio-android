package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.izquierdojl.tolocharadio.R
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary

/**
 * Acciones secundarias de un episodio (spec 0062), abiertas con pulsación
 * larga en [EpisodeRow]. Mantiene toda la funcionalidad que antes ocupaba
 * una fila de iconos.
 */
@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeActionsSheet(
    episode: PodcastEpisode,
    podcast: PodcastSummary?,
    isDownloaded: Boolean,
    downloadState: DownloadUiState,
    onDownload: (() -> Unit)?,
    onQueueNext: (() -> Unit)?,
    onQueueEnd: (() -> Unit)?,
    onToggleFavorite: () -> Unit,
    onTogglePlayed: (() -> Unit)?,
    onOpenDetail: () -> Unit,
    onDismiss: () -> Unit,
) {
    val downloaded = isDownloaded || downloadState is DownloadUiState.Downloaded
    val completed = episode.progress?.completed == true
    val run: (() -> Unit) -> Unit = { action ->
        onDismiss()
        action()
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = episode.imageUrl ?: podcast?.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small),
                    error = painterResource(R.drawable.sierra_emblem),
                    fallback = painterResource(R.drawable.sierra_emblem),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        episode.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    podcast?.title?.let {
                        Text(
                            it,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when {
                downloadState is DownloadUiState.Downloading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(16.dp))
                        Text(downloadLabel(downloadState), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                downloaded -> {
                    ActionRow(Icons.Filled.OfflinePin, "Descargado")
                }
                onDownload != null -> {
                    ActionRow(Icons.Filled.Download, downloadLabel(downloadState)) {
                        run(onDownload)
                    }
                }
            }

            onQueueNext?.let {
                ActionRow(Icons.AutoMirrored.Filled.PlaylistPlay, "Añadir a continuación") { run(it) }
            }
            onQueueEnd?.let {
                ActionRow(Icons.AutoMirrored.Filled.PlaylistAdd, "Añadir al final") { run(it) }
            }
            ActionRow(
                if (episode.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                if (episode.isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
            ) {
                run(onToggleFavorite)
            }
            onTogglePlayed?.let {
                ActionRow(
                    if (completed) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    if (completed) "Marcar como pendiente" else "Marcar como reproducido",
                ) {
                    run(it)
                }
            }
            ActionRow(Icons.Filled.Info, "Abrir ficha") { run(onOpenDetail) }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: (() -> Unit)? = null,
) {
    val contentColor =
        if (onClick == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                ).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = contentColor)
    }
}
