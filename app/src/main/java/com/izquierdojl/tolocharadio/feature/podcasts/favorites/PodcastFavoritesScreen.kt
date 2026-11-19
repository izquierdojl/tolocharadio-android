package com.izquierdojl.tolocharadio.feature.podcasts.favorites

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.izquierdojl.tolocharadio.core.ui.ViewMode
import com.izquierdojl.tolocharadio.core.ui.components.EmptyState
import com.izquierdojl.tolocharadio.core.ui.components.EpisodeCard
import com.izquierdojl.tolocharadio.core.ui.components.EpisodeCardUi
import com.izquierdojl.tolocharadio.core.ui.components.EpisodeRow
import com.izquierdojl.tolocharadio.core.ui.components.SectionHeader
import com.izquierdojl.tolocharadio.core.ui.components.ViewModeToggle
import com.izquierdojl.tolocharadio.core.ui.components.downloadUiState
import com.izquierdojl.tolocharadio.data.local.podcasts.DownloadRecord
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.repo.QueuePosition
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodeActionsViewModel
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerUi
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerViewModel
import com.izquierdojl.tolocharadio.feature.podcasts.sheet.EpisodeSheet

/**
 * Episodios favoritos (US5, FR-023): lista o tarjetas, con ficha y
 * reproducción, independiente de emisoras favoritas.
 */
@Composable
fun PodcastFavoritesScreen(
    snackbar: SnackbarHostState,
    viewModel: EpisodeFavoritesViewModel = hiltViewModel(),
    player: EpisodePlayerViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
    actions: EpisodeActionsViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val ui by viewModel.ui.collectAsState()
    val playerUi by player.ui.collectAsState()
    val actionMessage by actions.message.collectAsState()
    val downloads by actions.downloadRecords.collectAsState()
    val downloadProgress by actions.downloadProgress.collectAsState()
    val downloadsEnabled by actions.downloadsEnabled.collectAsState()
    var sheet by remember { mutableStateOf<Pair<PodcastEpisode, PodcastSummary?>?>(null) }
    var viewMode by rememberSaveable { mutableStateOf(ViewMode.LIST) }

    LaunchedEffect(playerUi.message) {
        playerUi.message?.let {
            snackbar.showSnackbar(it)
            player.clearMessage()
        }
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbar.showSnackbar(it)
            actions.clearMessage()
        }
    }

    LaunchedEffect(Unit) {
        actions.progressChanged.collect { viewModel.refreshSilently() }
    }

    Column(Modifier.fillMaxSize()) {
        SectionHeader(
            title = "Favoritos",
            subtitle = "Tus episodios marcados.",
            actions = {
                ViewModeToggle(
                    mode = viewMode,
                    onToggle = {
                        viewMode = if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                    },
                )
            },
        )
        when (val state = ui) {
            is EpisodeFavoritesUi.Loading ->
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            is EpisodeFavoritesUi.Empty ->
                EmptyState(title = "Sin episodios favoritos")
            is EpisodeFavoritesUi.Error ->
                Column {
                    Text(state.message, modifier = Modifier.padding(16.dp))
                    TextButton(onClick = viewModel::refresh) {
                        Text("Reintentar")
                    }
                }
            is EpisodeFavoritesUi.Content ->
                if (viewMode == ViewMode.GRID) {
                    FavoritesGrid(
                        items = state.items,
                        playerUi = playerUi,
                        downloads = downloads,
                        onPlay = { episode ->
                            if (playerUi.episode?.id == episode.id) {
                                player.toggle()
                            } else {
                                player.play(episode, null)
                            }
                        },
                        onDetail = { sheet = it to null },
                        onFavorite = actions::toggleFavorite,
                    )
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.items, key = { it.id }) { episode ->
                            EpisodeRow(
                                episode = episode,
                                podcast = null,
                                isPlaying = playerUi.episode?.id == episode.id && playerUi.playing,
                                isDownloaded = downloads[episode.id]?.localUri != null,
                                downloadState = downloadUiState(downloads[episode.id], downloadProgress[episode.id]),
                                onDownload = { actions.download(episode) }.takeIf { downloadsEnabled },
                                onPlayPause = {
                                    if (playerUi.episode?.id == episode.id) {
                                        player.toggle()
                                    } else {
                                        player.play(episode, null)
                                    }
                                },
                                onOpenDetail = { sheet = episode to null },
                                onToggleFavorite = { actions.toggleFavorite(episode) },
                                onTogglePlayed = { actions.togglePlayed(episode) },
                                onQueueNext = { actions.addToQueue(episode.id, QueuePosition.NEXT) },
                                onQueueEnd = { actions.addToQueue(episode.id, QueuePosition.END) },
                            )
                        }
                    }
                }
        }
    }
    sheet?.let { (episode, podcast) ->
        EpisodeSheet(
            episode = episode,
            podcast = podcast,
            downloadsEnabled = downloadsEnabled,
            onClose = { sheet = null },
        )
    }
}

/** Cuadrícula de favoritos (2 columnas de tarjetas de episodio). */
@Composable
private fun FavoritesGrid(
    items: List<PodcastEpisode>,
    playerUi: EpisodePlayerUi,
    downloads: Map<Long, DownloadRecord?>,
    onPlay: (PodcastEpisode) -> Unit,
    onDetail: (PodcastEpisode) -> Unit,
    onFavorite: (PodcastEpisode) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { episode ->
            EpisodeCard(
                ui =
                    EpisodeCardUi(
                        episode = episode,
                        isPlaying = playerUi.episode?.id == episode.id && playerUi.playing,
                        isDownloaded = downloads[episode.id]?.localUri != null,
                    ),
                onPlayPause = { onPlay(episode) },
                onOpenDetail = { onDetail(episode) },
                onToggleFavorite = { onFavorite(episode) },
            )
        }
    }
}
