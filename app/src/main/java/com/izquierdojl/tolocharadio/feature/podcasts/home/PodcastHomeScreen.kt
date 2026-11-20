package com.izquierdojl.tolocharadio.feature.podcasts.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.izquierdojl.tolocharadio.core.ui.components.PodcastCard
import com.izquierdojl.tolocharadio.core.ui.components.PodcastRow
import com.izquierdojl.tolocharadio.core.ui.components.SectionHeader
import com.izquierdojl.tolocharadio.core.ui.components.ViewModeToggle
import com.izquierdojl.tolocharadio.core.ui.components.downloadUiState
import com.izquierdojl.tolocharadio.data.local.podcasts.DownloadRecord
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.repo.DownloadProgress
import com.izquierdojl.tolocharadio.data.repo.QueuePosition
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodeActionsViewModel
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerViewModel
import com.izquierdojl.tolocharadio.feature.podcasts.FavoriteFailureEffect
import com.izquierdojl.tolocharadio.feature.podcasts.sheet.EpisodeSheet
import androidx.compose.foundation.lazy.grid.items as gridItems

/** Nº de programas en vista previa de la portada. */
private const val HOME_SUBSCRIPTIONS_PREVIEW = 8

/**
 * Portada de podcasts (US6): continuar escuchando (con descarte),
 * novedades, programas y resumen de la cola, en lista o tarjetas.
 */
@Composable
@Suppress("LongParameterList")
fun PodcastHomeScreen(
    onDiscover: () -> Unit,
    onQueue: () -> Unit,
    onSubscriptions: () -> Unit,
    onOpenShow: (Long) -> Unit,
    snackbar: SnackbarHostState,
    viewModel: PodcastHomeViewModel = hiltViewModel(),
    player: EpisodePlayerViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
    actions: EpisodeActionsViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val ui by viewModel.ui.collectAsState()
    val playerUi by player.ui.collectAsState()
    val actionMessage by actions.message.collectAsState()
    val favoriteFailure by actions.favoriteFailure.collectAsState()
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

    FavoriteFailureEffect(
        failure = favoriteFailure,
        snackbar = snackbar,
        onRetry = actions::retryFavorite,
        onDismiss = actions::clearFavoriteFailure,
    )

    LaunchedEffect(Unit) {
        actions.progressChanged.collect { viewModel.refreshSilently() }
    }

    val favorites by actions.favoriteIds.collectAsState()
    val downloads by actions.downloadRecords.collectAsState()
    val downloadProgress by actions.downloadProgress.collectAsState()
    val downloadsEnabled by actions.downloadsEnabled.collectAsState()

    val homeActions =
        HomeActions(
            onPlay = { episode, podcast ->
                if (playerUi.episode?.id == episode.id) {
                    player.toggle()
                } else {
                    player.play(episode, podcast)
                }
            },
            onDetail = { episode, podcast -> sheet = episode to podcast },
            onFavorite = actions::toggleFavorite,
            onPlayed = actions::togglePlayed,
            onQueue = { actions.addToQueue(it.id, QueuePosition.END) },
            onQueueNext = { actions.addToQueue(it.id, QueuePosition.NEXT) },
            onDownload =
                (
                    { episode: PodcastEpisode -> actions.download(episode) }
                        .takeIf { downloadsEnabled }
                ),
            onOpenShow = onOpenShow,
            onSubscriptions = onSubscriptions,
            onQueueScreen = onQueue,
        )

    Column(Modifier.fillMaxSize()) {
        SectionHeader(
            title = "Podcasts",
            subtitle = "Novedades y lo que dejaste a medias.",
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
            is PodcastHomeUi.Loading ->
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            is PodcastHomeUi.Empty ->
                EmptyState(
                    title = "Aún no sigues ningún podcast",
                    actionLabel = "Descubrir podcasts",
                    onAction = onDiscover,
                )
            is PodcastHomeUi.Error ->
                Column {
                    Text(state.message, modifier = Modifier.padding(16.dp))
                    TextButton(onClick = viewModel::refresh) {
                        Text("Reintentar")
                    }
                }
            is PodcastHomeUi.Content -> {
                val home = state.home
                val sections =
                    HomeSectionsState(
                        subscriptions = home.subscriptions,
                        continueListening = home.continueListening,
                        latestEpisodes = home.latestEpisodes,
                        favorites = favorites,
                        downloads = downloads,
                        downloadProgress = downloadProgress,
                        playingId = playerUi.episode?.id,
                        playing = playerUi.playing,
                        queueCount = home.queue.count,
                        queueLimit = home.queue.limit,
                    )
                if (viewMode == ViewMode.GRID) {
                    HomeGrid(sections = sections, actions = homeActions)
                } else {
                    HomeList(sections = sections, actions = homeActions)
                }
            }
        }
    }
    sheet?.let { (episode, podcast) ->
        EpisodeSheet(
            episode = episode,
            podcast = podcast,
            downloadsEnabled = downloadsEnabled,
            showDismiss = true,
            onClose = { sheet = null },
        )
    }
}

/** Estado de pintado compartido por las dos vistas de la portada. */
private data class HomeSectionsState(
    val subscriptions: List<PodcastSummary>,
    val continueListening: List<PodcastEpisode>,
    val latestEpisodes: List<PodcastEpisode>,
    val favorites: Set<Long>,
    val downloads: Map<Long, DownloadRecord?>,
    val downloadProgress: Map<Long, DownloadProgress>,
    val playingId: Long?,
    val playing: Boolean,
    val queueCount: Int,
    val queueLimit: Int,
)

/** Acciones compartidas por las dos vistas de la portada. */
private data class HomeActions(
    val onPlay: (PodcastEpisode, PodcastSummary?) -> Unit,
    val onDetail: (PodcastEpisode, PodcastSummary?) -> Unit,
    val onFavorite: (PodcastEpisode) -> Unit,
    val onPlayed: (PodcastEpisode) -> Unit,
    val onQueue: (PodcastEpisode) -> Unit,
    val onQueueNext: (PodcastEpisode) -> Unit,
    val onDownload: ((PodcastEpisode) -> Unit)?,
    val onOpenShow: (Long) -> Unit,
    val onSubscriptions: () -> Unit,
    val onQueueScreen: () -> Unit,
)

/** Portada en lista compacta. */
@Composable
private fun HomeList(
    sections: HomeSectionsState,
    actions: HomeActions,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        if (sections.continueListening.isNotEmpty()) {
            homeEpisodeSection(
                keyPrefix = "continue",
                title = "Continuar escuchando",
                episodes = sections.continueListening,
                sections = sections,
                actions = actions,
            )
        }
        if (sections.latestEpisodes.isNotEmpty()) {
            homeEpisodeSection(
                keyPrefix = "latest",
                title = "Novedades",
                episodes = sections.latestEpisodes,
                sections = sections,
                actions = actions,
            )
        }
        if (sections.subscriptions.isNotEmpty()) {
            item {
                HomeSectionTitle(
                    title = "Tus podcasts",
                    actionLabel = "Ver todos",
                    onAction = actions.onSubscriptions,
                )
            }
            items(sections.subscriptions.take(HOME_SUBSCRIPTIONS_PREVIEW), key = { "podcast-${it.id}" }) { summary ->
                PodcastRow(
                    summary = summary,
                    onOpen = { actions.onOpenShow(summary.id) },
                )
            }
        }
        item {
            TextButton(
                onClick = actions.onQueueScreen,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("Cola (${sections.queueCount}/${sections.queueLimit})")
            }
        }
    }
}

/** Portada en cuadrícula de tarjetas. */
@Composable
private fun HomeGrid(
    sections: HomeSectionsState,
    actions: HomeActions,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        homeEpisodeGrid(
            keyPrefix = "continue",
            title = "Continuar escuchando",
            episodes = sections.continueListening,
            sections = sections,
            actions = actions,
        )
        homeEpisodeGrid(
            keyPrefix = "latest",
            title = "Novedades",
            episodes = sections.latestEpisodes,
            sections = sections,
            actions = actions,
        )
        if (sections.subscriptions.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HomeSectionTitle(
                    title = "Tus podcasts",
                    actionLabel = "Ver todos",
                    onAction = actions.onSubscriptions,
                )
            }
            gridItems(
                sections.subscriptions.take(HOME_SUBSCRIPTIONS_PREVIEW),
                key = { "podcast-${it.id}" },
            ) { summary ->
                PodcastCard(
                    summary = summary,
                    onOpen = { actions.onOpenShow(summary.id) },
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            TextButton(
                onClick = actions.onQueueScreen,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Text("Cola (${sections.queueCount}/${sections.queueLimit})")
            }
        }
    }
}

/** Cabecera de sección con acción "ver todos" opcional. */
@Composable
private fun HomeSectionTitle(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

/** Sección de episodios en lista (paridad con `EpisodeRow` web). */
private fun LazyListScope.homeEpisodeSection(
    keyPrefix: String,
    title: String,
    episodes: List<PodcastEpisode>,
    sections: HomeSectionsState,
    actions: HomeActions,
) {
    item {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    items(episodes, key = { "$keyPrefix-${it.id}" }) { episode ->
        val podcast = sections.subscriptions.firstOrNull { it.id == episode.podcastId }
        EpisodeRow(
            episode = episode.copy(isFavorite = episode.id in sections.favorites),
            podcast = podcast,
            isPlaying = sections.playingId == episode.id && sections.playing,
            isDownloaded = sections.downloads[episode.id]?.localUri != null,
            downloadState =
                downloadUiState(
                    sections.downloads[episode.id],
                    sections.downloadProgress[episode.id],
                ),
            onDownload = actions.onDownload?.let { download -> { download(episode) } },
            onPlayPause = { actions.onPlay(episode, podcast) },
            onOpenDetail = { actions.onDetail(episode, podcast) },
            onToggleFavorite = { actions.onFavorite(episode) },
            onTogglePlayed = { actions.onPlayed(episode) },
            onQueueNext = { actions.onQueueNext(episode) },
            onQueueEnd = { actions.onQueue(episode) },
        )
    }
}

/** Sección de episodios en cuadrícula de tarjetas. */
private fun LazyGridScope.homeEpisodeGrid(
    keyPrefix: String,
    title: String,
    episodes: List<PodcastEpisode>,
    sections: HomeSectionsState,
    actions: HomeActions,
) {
    if (episodes.isEmpty()) return
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        )
    }
    gridItems(episodes, key = { "$keyPrefix-${it.id}" }) { episode ->
        val podcast = sections.subscriptions.firstOrNull { it.id == episode.podcastId }
        EpisodeCard(
            ui =
                EpisodeCardUi(
                    episode = episode.copy(isFavorite = episode.id in sections.favorites),
                    podcast = podcast,
                    isPlaying = sections.playingId == episode.id && sections.playing,
                    isDownloaded = sections.downloads[episode.id]?.localUri != null,
                ),
            onPlayPause = { actions.onPlay(episode, podcast) },
            onOpenDetail = { actions.onDetail(episode, podcast) },
            onToggleFavorite = { actions.onFavorite(episode) },
        )
    }
}
