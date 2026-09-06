package com.izquierdojl.tolocharadio.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.izquierdojl.tolocharadio.core.ui.components.EmptyState
import com.izquierdojl.tolocharadio.core.ui.components.ErrorBanner
import com.izquierdojl.tolocharadio.core.ui.components.StationArtwork
import com.izquierdojl.tolocharadio.core.ui.components.TagChip
import com.izquierdojl.tolocharadio.data.remote.dto.HistoryEntryDto
import com.izquierdojl.tolocharadio.feature.favorites.relativeTime
import com.izquierdojl.tolocharadio.feature.player.PlayerViewModel

/** Nº máximo de etiquetas visibles por fila (paridad con StationCard). */
private const val MAX_ROW_TAGS = 3

/**
 * Historial: lista de emisoras reproducidas con hora relativa,
 * eliminación individual y limpieza total con diálogo de confirmación.
 *
 * @param onStation abre la ficha al pulsar una emisora.
 * @param onExplore atajo del estado vacío al catálogo.
 */
@Composable
fun HistoryScreen(
    onStation: (String) -> Unit,
    onExplore: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
    player: PlayerViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Limpiar historial") },
            text = { Text("¿Limpiar todo el historial? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.onClearAll()
                    },
                ) {
                    Text("Limpiar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            HistoryScreenContent(
                state = ui,
                onStation = onStation,
                onExplore = onExplore,
                onPlay = {
                    player.play(it.station)
                    viewModel.onPlayTriggered()
                },
                onRemove = viewModel::onRemove,
                onRetry = viewModel::retry,
                onClearAll = { showClearDialog = true },
            )
        }
    }
}

/** Contenido sin estado (puerta de test sin Hilt). */
@Composable
fun HistoryScreenContent(
    state: HistoryUiState,
    onStation: (String) -> Unit,
    onExplore: () -> Unit,
    onPlay: (HistoryEntryDto) -> Unit,
    onRemove: (String) -> Unit,
    onRetry: () -> Unit,
    onClearAll: () -> Unit,
) {
    when (state) {
        HistoryUiState.Loading ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        HistoryUiState.Empty ->
            EmptyState(
                title = "Todavía no has escuchado nada",
                actionLabel = "Explorar",
                onAction = onExplore,
            )
        is HistoryUiState.Error -> ErrorBanner(state.message, onRetry = onRetry)
        is HistoryUiState.Content ->
            HistoryList(
                items = state.items,
                offline = state.offline,
                pendingDeletes = state.pendingDeletes,
                onStation = onStation,
                onPlay = onPlay,
                onRemove = onRemove,
                onClearAll = onClearAll,
            )
    }
}

@Composable
private fun HistoryList(
    items: List<HistoryEntryDto>,
    offline: Boolean,
    pendingDeletes: Set<String>,
    onStation: (String) -> Unit,
    onPlay: (HistoryEntryDto) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Tu historial",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    "Lo último que has escuchado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (items.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Limpiar")
                }
            }
        }

        if (offline) {
            Text(
                "Mostrando caché sin conexión.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            items(items, key = { it.station.id }) { entry ->
                HistoryRow(
                    entry = entry,
                    isDeleting = entry.station.id in pendingDeletes,
                    onOpen = { onStation(entry.station.id) },
                    onPlay = { onPlay(entry) },
                    onRemove = { onRemove(entry.station.id) },
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntryDto,
    isDeleting: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationArtwork(entry.station, Modifier.size(48.dp))
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f).clickable(onClick = onOpen).padding(vertical = 4.dp),
        ) {
            Text(
                entry.station.name.ifBlank { "Emisora sin nombre" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                listOfNotNull(entry.station.country, entry.station.language).joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val tags = entry.station.tags.take(MAX_ROW_TAGS)
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tags.forEach { TagChip(it) }
                }
            }
            relativeTime(entry.playedAt)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onPlay) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir")
        }
        IconButton(onClick = onRemove, enabled = !isDeleting) {
            val tint =
                if (isDeleting) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Eliminar del historial",
                tint = tint,
            )
        }
    }
}

