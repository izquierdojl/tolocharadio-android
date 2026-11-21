package com.izquierdojl.tolocharadio.feature.podcasts.subscriptions

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.izquierdojl.tolocharadio.R
import com.izquierdojl.tolocharadio.core.ui.components.EpisodeRow
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.core.ui.components.downloadUiState
import com.izquierdojl.tolocharadio.data.local.podcasts.DownloadRecord
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.repo.DownloadProgress
import com.izquierdojl.tolocharadio.data.repo.QueuePosition
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodeActionsViewModel
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerUi
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerViewModel
import com.izquierdojl.tolocharadio.feature.podcasts.FavoriteFailureEffect
import com.izquierdojl.tolocharadio.feature.podcasts.SubscriptionSearchUi
import com.izquierdojl.tolocharadio.feature.podcasts.sheet.EpisodeSheet

/**
 * Resultados de la búsqueda de episodios en Suscripciones (US8).
 * Es stateless: recibe el estado y las acciones ya resueltas por el host.
 */
@Composable
@Suppress("LongParameterList")
fun SubscriptionSearchResults(
    state: SubscriptionSearchUi,
    favoriteIds: Set<Long>,
    playerUi: EpisodePlayerUi,
    downloads: Map<Long, DownloadRecord>,
    downloadProgress: Map<Long, DownloadProgress>,
    downloadsEnabled: Boolean,
    onPlayPause: (PodcastEpisode, PodcastSummary) -> Unit,
    onOpenDetail: (PodcastEpisode, PodcastSummary) -> Unit,
    onToggleFavorite: (PodcastEpisode) -> Unit,
    onQueueNext: (Long) -> Unit,
    onQueueEnd: (Long) -> Unit,
    onDownload: (PodcastEpisode) -> Unit,
    onTogglePlayed: (PodcastEpisode) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag(TolochaTestTags.SUBSCRIPTIONS_SEARCH_RESULTS),
    ) {
        when {
            state.loading && state.results.isEmpty() -> SearchLoading()
            state.error != null && state.results.isEmpty() -> SearchError(state.error, onRetry)
            state.showResults && state.results.isEmpty() -> SearchEmpty(state.submittedTerm)
            state.results.isNotEmpty() ->
                SearchResultList(
                    state = state,
                    favoriteIds = favoriteIds,
                    playerUi = playerUi,
                    downloads = downloads,
                    downloadProgress = downloadProgress,
                    downloadsEnabled = downloadsEnabled,
                    onPlayPause = onPlayPause,
                    onOpenDetail = onOpenDetail,
                    onToggleFavorite = onToggleFavorite,
                    onQueueNext = onQueueNext,
                    onQueueEnd = onQueueEnd,
                    onDownload = onDownload,
                    onTogglePlayed = onTogglePlayed,
                    onLoadMore = onLoadMore,
                    onRetry = onRetry,
                )
        }
    }
}

@Composable
private fun SearchLoading() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            stringResource(R.string.subscriptions_search_loading),
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SearchError(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag(TolochaTestTags.SUBSCRIPTIONS_SEARCH_ERROR),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.subscriptions_search_retry))
        }
    }
}

@Composable
private fun SearchEmpty(term: String) {
    Text(
        stringResource(R.string.subscriptions_search_empty, term),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .testTag(TolochaTestTags.SUBSCRIPTIONS_SEARCH_EMPTY),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
@Suppress("LongParameterList")
private fun SearchResultList(
    state: SubscriptionSearchUi,
    favoriteIds: Set<Long>,
    playerUi: EpisodePlayerUi,
    downloads: Map<Long, DownloadRecord>,
    downloadProgress: Map<Long, DownloadProgress>,
    downloadsEnabled: Boolean,
    onPlayPause: (PodcastEpisode, PodcastSummary) -> Unit,
    onOpenDetail: (PodcastEpisode, PodcastSummary) -> Unit,
    onToggleFavorite: (PodcastEpisode) -> Unit,
    onQueueNext: (Long) -> Unit,
    onQueueEnd: (Long) -> Unit,
    onDownload: (PodcastEpisode) -> Unit,
    onTogglePlayed: (PodcastEpisode) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        item {
            SearchResultsHeader(
                term = state.submittedTerm,
                count = state.results.size,
            )
        }
        items(state.results, key = { it.episode.id }) { result ->
            val episode =
                result.episode.copy(isFavorite = result.episode.id in favoriteIds)
            EpisodeRow(
                episode = episode,
                podcast = result.podcast,
                isPlaying = playerUi.episode?.id == episode.id && playerUi.playing,
                isDownloaded = downloads[episode.id]?.localUri != null,
                downloadState =
                    downloadUiState(
                        downloads[episode.id],
                        downloadProgress[episode.id],
                    ),
                onPlayPause = { onPlayPause(episode, result.podcast) },
                onOpenDetail = { onOpenDetail(episode, result.podcast) },
                onToggleFavorite = { onToggleFavorite(episode) },
                onQueueNext = { onQueueNext(episode.id) },
                onQueueEnd = { onQueueEnd(episode.id) },
                onDownload = { onDownload(episode) }.takeIf { downloadsEnabled },
                onTogglePlayed = { onTogglePlayed(episode) },
            )
        }
        item {
            SearchResultsFooter(
                loadingMore = state.loadingMore,
                error = state.error,
                hasMore = state.hasMore,
                onLoadMore = onLoadMore,
                onRetry = onRetry,
            )
        }
    }
}

@Composable
private fun SearchResultsHeader(
    term: String,
    count: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.subscriptions_search_results, term),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(
                if (count == 1) {
                    R.string.subscriptions_search_results_count_one
                } else {
                    R.string.subscriptions_search_results_count
                },
                count,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TolochaTestTags.SUBSCRIPTIONS_SEARCH_STATUS),
        )
    }
}

@Composable
private fun SearchResultsFooter(
    loadingMore: Boolean,
    error: String?,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        loadingMore ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        error != null ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.subscriptions_search_retry))
                }
            }
        hasMore ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(
                    onClick = onLoadMore,
                    modifier = Modifier.testTag(TolochaTestTags.SUBSCRIPTIONS_SEARCH_LOAD_MORE),
                ) {
                    Text(stringResource(R.string.subscriptions_search_load_more))
                }
            }
    }
}

/**
 * Host de los resultados: conecta el estado con el player y las acciones
 * compartidas de la actividad, igual que Inicio y Favoritos.
 */
@Composable
@Suppress("LongParameterList")
fun SubscriptionSearchHost(
    state: SubscriptionSearchUi,
    snackbar: SnackbarHostState,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onProgressChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
    player: EpisodePlayerViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
    actions: EpisodeActionsViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val playerUi by player.ui.collectAsState()
    val favoriteIds by actions.favoriteIds.collectAsState()
    val downloads by actions.downloadRecords.collectAsState()
    val downloadProgress by actions.downloadProgress.collectAsState()
    val downloadsEnabled by actions.downloadsEnabled.collectAsState()
    val message by actions.message.collectAsState()
    val favoriteFailure by actions.favoriteFailure.collectAsState()
    var sheet by remember { mutableStateOf<Pair<PodcastEpisode, PodcastSummary?>?>(null) }

    LaunchedEffect(message) {
        message?.let {
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
        actions.progressChanged.collect { onProgressChanged(it) }
    }

    SubscriptionSearchResults(
        state = state,
        favoriteIds = favoriteIds,
        playerUi = playerUi,
        downloads = downloads,
        downloadProgress = downloadProgress,
        downloadsEnabled = downloadsEnabled,
        onPlayPause = { episode, podcast ->
            if (playerUi.episode?.id == episode.id) {
                player.toggle()
            } else {
                player.play(episode, podcast)
            }
        },
        onOpenDetail = { episode, podcast -> sheet = episode to podcast },
        onToggleFavorite = actions::toggleFavorite,
        onQueueNext = { actions.addToQueue(it, QueuePosition.NEXT) },
        onQueueEnd = { actions.addToQueue(it, QueuePosition.END) },
        onDownload = actions::download,
        onTogglePlayed = actions::togglePlayed,
        onLoadMore = onLoadMore,
        onRetry = onRetry,
        modifier = modifier,
    )

    sheet?.let { (episode, podcast) ->
        EpisodeSheet(
            episode = episode,
            podcast = podcast,
            downloadsEnabled = downloadsEnabled,
            onClose = { sheet = null },
        )
    }
}
