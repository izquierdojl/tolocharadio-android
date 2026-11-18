package com.izquierdojl.tolocharadio.feature.podcasts.sheet

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.izquierdojl.tolocharadio.core.ui.components.FavoriteButton
import com.izquierdojl.tolocharadio.core.ui.components.formatEpisodeDuration
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.repo.QueuePosition
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodeActionsViewModel
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            Row(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = {
                    if (playingThis) {
                        player.toggle()
                    } else {
                        player.play(episode, podcast)
                    }
                }) {
                    Text(if (playingThis) "Pausar" else "Reproducir")
                }
                FavoriteButton(
                    isFavorite = episode.isFavorite,
                    onToggle = { actions.toggleFavorite(episode) },
                )
            }
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = { actions.addToQueue(episode.id, QueuePosition.NEXT) }) {
                    Text("A continuación")
                }
                TextButton(onClick = { actions.addToQueue(episode.id, QueuePosition.END) }) {
                    Text("Al final")
                }
            }
            val completed = episode.progress?.completed == true
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = {
                    actions.togglePlayed(episode)
                    onClose()
                }) {
                    Icon(
                        if (completed) {
                            Icons.Filled.RadioButtonUnchecked
                        } else {
                            Icons.Filled.CheckCircle
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (completed) {
                            "Marcar como pendiente"
                        } else {
                            "Marcar como reproducido"
                        },
                    )
                }
            }
            Row(Modifier.fillMaxWidth()) {
                if (downloadsEnabled) {
                    TextButton(onClick = { actions.download(episode) }) {
                        Text("Descargar")
                    }
                }
                if (showDismiss) {
                    TextButton(onClick = {
                        actions.dismiss(episode.id)
                        onClose()
                    }) {
                        Text("Descartar")
                    }
                }
            }
        }
    }
}
