package com.izquierdojl.tolocharadio.feature.podcasts.show

import androidx.activity.ComponentActivity
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.izquierdojl.tolocharadio.core.ui.components.EmptyState
import com.izquierdojl.tolocharadio.core.ui.components.EpisodeRow
import com.izquierdojl.tolocharadio.core.ui.components.SubscribeButton
import com.izquierdojl.tolocharadio.core.ui.components.downloadUiState
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.repo.QueuePosition
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodeActionsViewModel
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerViewModel
import com.izquierdojl.tolocharadio.feature.podcasts.sheet.EpisodeSheet

/**
 * Ficha del programa (US6, FR-011/FR-012): portada, descripción y
 * episodios con filtros, órdenes, búsqueda y paginación. Toda la
 * pantalla es un único scroll: al subir, la cabecera desaparece y la
 * lista gana espacio.
 */
@Composable
@Suppress("CyclomaticComplexMethod")
fun PodcastShowScreen(
    podcastId: Long,
    onBack: () -> Unit,
    snackbar: SnackbarHostState,
    viewModel: PodcastShowViewModel = hiltViewModel(),
    player: EpisodePlayerViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
    actions: EpisodeActionsViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val ui by viewModel.ui.collectAsState()
    val playerUi by player.ui.collectAsState()
    val favorites by actions.favoriteIds.collectAsState()
    val downloads by actions.downloadRecords.collectAsState()
    val downloadProgress by actions.downloadProgress.collectAsState()
    val downloadsEnabled by actions.downloadsEnabled.collectAsState()
    var sheet by remember { mutableStateOf<Pair<PodcastEpisode, PodcastSummary?>?>(null) }

    LaunchedEffect(podcastId) {
        viewModel.open(podcastId)
    }
    LaunchedEffect(playerUi.message) {
        playerUi.message?.let {
            snackbar.showSnackbar(it)
            player.clearMessage()
        }
    }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(Unit) {
        actions.progressChanged.collect { viewModel.refresh() }
    }

    val podcast = ui.podcast
    if (ui.loading && podcast == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (podcast == null) {
        Column(Modifier.fillMaxSize()) {
            TextButton(onClick = onBack) { Text("Volver") }
            Text(ui.message ?: "No se pudo cargar el programa.", modifier = Modifier.padding(16.dp))
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text("Volver")
            }
        }
        item {
            ShowHeader(podcast = podcast, onToggleSubscribe = viewModel::toggleSubscribe)
        }
        podcast.description?.let {
            item {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        item {
            OutlinedTextField(
                value = ui.query,
                onValueChange = viewModel::setQuery,
                label = { Text("Buscar en episodios") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                trailingIcon = {
                    Row {
                        if (ui.query.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    viewModel.setQuery("")
                                    viewModel.search()
                                },
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                        IconButton(onClick = viewModel::search) {
                            Icon(Icons.Filled.Search, contentDescription = "Buscar")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
        item {
            ShowChips {
                EpisodeFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = ui.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.label()) },
                    )
                }
            }
        }
        item {
            ShowChips {
                EpisodeSort.entries.forEach { sort ->
                    FilterChip(
                        selected = ui.sort == sort,
                        onClick = { viewModel.setSort(sort) },
                        label = { Text(sort.label()) },
                    )
                }
            }
        }
        if (ui.loading) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }
        if (ui.items.isEmpty() && !ui.loading) {
            item { EmptyState(title = "Sin episodios para este filtro") }
        } else {
            items(ui.items, key = { it.id }) { episode ->
                EpisodeRow(
                    episode = episode.copy(isFavorite = episode.id in favorites),
                    podcast = podcast,
                    isPlaying = playerUi.episode?.id == episode.id && playerUi.playing,
                    isDownloaded = downloads[episode.id]?.localUri != null,
                    downloadState = downloadUiState(downloads[episode.id], downloadProgress[episode.id]),
                    onDownload = { actions.download(episode) }.takeIf { downloadsEnabled },
                    onPlayPause = {
                        if (playerUi.episode?.id == episode.id) {
                            player.toggle()
                        } else {
                            player.play(episode, podcast)
                        }
                    },
                    onOpenDetail = { sheet = episode to podcast },
                    onToggleFavorite = { actions.toggleFavorite(episode) },
                    onTogglePlayed = { actions.togglePlayed(episode) },
                    onQueueNext = { actions.addToQueue(episode.id, QueuePosition.NEXT) },
                    onQueueEnd = { actions.addToQueue(episode.id, QueuePosition.END) },
                )
            }
            if (ui.hasMore) {
                item {
                    TextButton(
                        onClick = viewModel::loadMore,
                        enabled = !ui.loadingMore,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Text(if (ui.loadingMore) "Cargando…" else "Mostrar más")
                    }
                }
            }
        }
    }

    sheet?.let { (episode, podcastItem) ->
        EpisodeSheet(
            episode = episode,
            podcast = podcastItem,
            downloadsEnabled = downloadsEnabled,
            onClose = { sheet = null },
        )
    }
}

/** Cabecera del programa (portada, autor, episodios y suscripción). */
@Composable
private fun ShowHeader(
    podcast: PodcastSummary,
    onToggleSubscribe: () -> Unit,
) {
    Row(Modifier.padding(horizontal = 16.dp)) {
        AsyncImage(
            model = podcast.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .size(96.dp)
                    .clip(MaterialTheme.shapes.medium),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(podcast.title, style = MaterialTheme.typography.headlineSmall)
            podcast.author?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(
                "${podcast.episodeCount} episodios",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(4.dp))
            SubscribeButton(
                subscribed = podcast.isSubscribed,
                onToggle = onToggleSubscribe,
            )
        }
    }
}

/** Fila de chips con desplazamiento horizontal para pantallas estrechas. */
@Composable
private fun ShowChips(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

private fun EpisodeFilter.label(): String =
    when (this) {
        EpisodeFilter.ALL -> "Todos"
        EpisodeFilter.UNPLAYED -> "Sin empezar"
        EpisodeFilter.IN_PROGRESS -> "En progreso"
        EpisodeFilter.COMPLETED -> "Completados"
    }

private fun EpisodeSort.label(): String =
    when (this) {
        EpisodeSort.RECENT -> "Recientes"
        EpisodeSort.TITLE -> "Título"
        EpisodeSort.DURATION -> "Duración"
    }
