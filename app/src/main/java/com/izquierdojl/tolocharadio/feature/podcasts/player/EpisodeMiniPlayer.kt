package com.izquierdojl.tolocharadio.feature.podcasts.player

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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.izquierdojl.tolocharadio.R
import com.izquierdojl.tolocharadio.core.ui.components.NowPlayingContainer
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.domain.podcasts.remainingMs
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerUi
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerViewModel

/**
 * Mini-player de episodio (US3/spec 0047): tres filas —podcast, episodio
 * y acciones—. Tocar la barra abre el "now playing" completo; las acciones
 * no propagan el toque.
 */
@Composable
fun EpisodeMiniPlayer(
    viewModel: EpisodePlayerViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
    onOpenFullPlayer: () -> Unit,
) {
    val ui by viewModel.ui.collectAsState()
    val episode = ui.episode ?: return
    EpisodeMiniPlayerContent(
        ui = ui,
        episode = episode,
        onOpenFullPlayer = onOpenFullPlayer,
        onToggle = viewModel::toggle,
        onSkipBack = viewModel::skipBack,
        onSkipForward = viewModel::skipForward,
        onStop = viewModel::stop,
    )
}

/** Contenido sin Hilt de la tarjeta inferior, testeable y con tiempos. */
@Composable
internal fun EpisodeMiniPlayerContent(
    ui: EpisodePlayerUi,
    episode: PodcastEpisode,
    onOpenFullPlayer: () -> Unit,
    onToggle: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onStop: () -> Unit,
) {
    val remaining = remainingMs(ui.positionMs, ui.durationMs)
    val timeDescription =
        remaining?.let {
            "Transcurrido ${formatPlaybackTime(ui.positionMs)} de " +
                "${formatPlaybackTime(ui.durationMs)}. Quedan ${formatPlaybackTime(it)}"
        } ?: "Transcurrido ${formatPlaybackTime(ui.positionMs)}"
    val timeStyle = MaterialTheme.typography.labelSmall.copy(lineHeight = 12.sp)
    val imageUrl = episode.imageUrl ?: ui.podcast?.imageUrl
    val hasImage = !imageUrl.isNullOrBlank()
    val imageDescription = "${episode.title}, ${ui.podcast?.title ?: "Podcast"}"
    NowPlayingContainer(onClick = onOpenFullPlayer) {
        Column {
            if (!hasImage) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            episode.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            ui.podcast?.title ?: "Podcast",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasImage) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = imageDescription,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(56.dp)
                                .clip(MaterialTheme.shapes.small),
                        error = painterResource(R.drawable.sierra_emblem),
                        fallback = painterResource(R.drawable.sierra_emblem),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                MiniControlsRow(
                    ui = ui,
                    onToggle = onToggle,
                    onSkipBack = onSkipBack,
                    onSkipForward = onSkipForward,
                    onStop = onStop,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .padding(horizontal = 8.dp)
                        .testTag(TolochaTestTags.EPISODE_TIME_ROW)
                        .semantics {
                            contentDescription = timeDescription
                        },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    formatPlaybackTime(ui.positionMs),
                    style = timeStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                remaining?.let {
                    Text(
                        formatRemainingTime(it),
                        style = timeStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (ui.durationMs > 0) {
                LinearProgressIndicator(
                    progress = { (ui.positionMs.toFloat() / ui.durationMs.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                )
            }
        }
    }
}

@Composable
private fun MiniControlsRow(
    ui: EpisodePlayerUi,
    onToggle: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.height(56.dp)) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onSkipBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription =
                        stringResource(R.string.media_seek_back_seconds, ui.skipBackSeconds),
                    modifier = Modifier.size(24.dp),
                )
            }
            FilledIconButton(
                onClick = onToggle,
                modifier = Modifier.size(56.dp),
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Icon(
                    if (ui.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (ui.playing) "Pausar" else "Reproducir",
                    modifier = Modifier.size(30.dp),
                )
            }
            IconButton(onClick = onSkipForward, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription =
                        stringResource(R.string.media_seek_forward_seconds, ui.skipForwardSeconds),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        IconButton(
            onClick = onStop,
            modifier = Modifier.align(Alignment.CenterEnd).size(48.dp),
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Detener",
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Suppress("MagicNumber")
@Preview(name = "EpisodeMiniPlayer dark", showBackground = true, backgroundColor = 0xFF08100B)
@Composable
internal fun EpisodeMiniPlayerDarkPreview() {
    val episode =
        PodcastEpisode(
            id = 1L,
            podcastId = 1L,
            title = "Episodio de prueba",
            durationSec = 1_800L,
            imageUrl = "https://picsum.photos/seed/tolocha-mini/400/400",
        )
    TolochaTheme(darkTheme = true) {
        EpisodeMiniPlayerContent(
            ui =
                EpisodePlayerUi(
                    episode = episode,
                    playing = true,
                    positionMs = 60_000L,
                    durationMs = 1_800_000L,
                ),
            episode = episode,
            onOpenFullPlayer = {},
            onToggle = {},
            onSkipBack = {},
            onSkipForward = {},
            onStop = {},
        )
    }
}

@Suppress("MagicNumber")
@Preview(name = "EpisodeMiniPlayer light", showBackground = true, backgroundColor = 0xFFEEF3EF)
@Composable
internal fun EpisodeMiniPlayerLightPreview() {
    val episode = PodcastEpisode(id = 2L, podcastId = 1L, title = "Otro episodio", durationSec = 1_200L)
    TolochaTheme(darkTheme = false) {
        EpisodeMiniPlayerContent(
            ui =
                EpisodePlayerUi(
                    episode = episode,
                    playing = false,
                    positionMs = 30_000L,
                    durationMs = 1_200_000L,
                ),
            episode = episode,
            onOpenFullPlayer = {},
            onToggle = {},
            onSkipBack = {},
            onSkipForward = {},
            onStop = {},
        )
    }
}
