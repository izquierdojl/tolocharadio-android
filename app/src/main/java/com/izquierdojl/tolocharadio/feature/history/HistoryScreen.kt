package com.izquierdojl.tolocharadio.feature.history

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.izquierdojl.tolocharadio.core.ui.ViewMode
import com.izquierdojl.tolocharadio.core.ui.components.EmptyState
import com.izquierdojl.tolocharadio.core.ui.components.ErrorBanner
import com.izquierdojl.tolocharadio.core.ui.components.StationArtwork
import com.izquierdojl.tolocharadio.core.ui.components.TagChip
import com.izquierdojl.tolocharadio.data.remote.dto.HistoryEntryDto
import com.izquierdojl.tolocharadio.feature.ViewModeViewModel
import com.izquierdojl.tolocharadio.feature.favorites.relativeTime
import com.izquierdojl.tolocharadio.feature.player.PlayerViewModel

/** Nº máximo de etiquetas visibles por fila (paridad con StationCard). */
private const val MAX_ROW_TAGS = 3

/** Relación de aspecto 16:9 del artwork de la tarjeta de cuadrícula. */
private const val GRID_ARTWORK_ASPECT_RATIO = 16f / 9f

/** Color del degradado inferior del artwork en cuadrícula (~70% opacidad). */
private const val GRID_SCRIM_COLOR = 0xB3000000.toInt()

/** Acciones de la sección Historial (evita firmas largas). */
data class HistoryCallbacks(
    val onStation: (String) -> Unit,
    val onExplore: () -> Unit,
    val onPlay: (HistoryEntryDto) -> Unit,
    val onRemove: (String) -> Unit,
    val onRetry: () -> Unit,
    val onClearAll: () -> Unit,
)

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
    viewModeVm: ViewModeViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val ui by viewModel.ui.collectAsState()
    val mode by viewModeVm.mode.collectAsState()
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
                mode = mode,
                callbacks =
                    HistoryCallbacks(
                        onStation = onStation,
                        onExplore = onExplore,
                        onPlay = {
                            player.play(it.station)
                            viewModel.onPlayTriggered()
                        },
                        onRemove = viewModel::onRemove,
                        onRetry = viewModel::retry,
                        onClearAll = { showClearDialog = true },
                    ),
            )
        }
    }
}

/** Contenido sin estado (puerta de test sin Hilt). */
@Composable
fun HistoryScreenContent(
    state: HistoryUiState,
    callbacks: HistoryCallbacks,
    mode: ViewMode = ViewMode.LIST,
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
                onAction = callbacks.onExplore,
            )
        is HistoryUiState.Error -> ErrorBanner(state.message, onRetry = callbacks.onRetry)
        is HistoryUiState.Content ->
            HistoryList(
                state = state,
                mode = mode,
                callbacks = callbacks,
            )
    }
}

@Composable
private fun HistoryList(
    state: HistoryUiState.Content,
    mode: ViewMode,
    callbacks: HistoryCallbacks,
) {
    val items = state.items
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
                TextButton(onClick = callbacks.onClearAll) {
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

        if (state.offline) {
            Text(
                "Mostrando caché sin conexión.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        if (mode == ViewMode.GRID) {
            // Tarjeta de cuadrícula propia: el corazón de StationCard significa
            // "favoritos" y aquí la acción de lista es quitar del historial.
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.station.id }) { entry ->
                    HistoryGridCard(
                        entry = entry,
                        isDeleting = entry.station.id in state.pendingDeletes,
                        onOpen = { callbacks.onStation(entry.station.id) },
                        onPlay = { callbacks.onPlay(entry) },
                        onRemove = { callbacks.onRemove(entry.station.id) },
                    )
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(items, key = { it.station.id }) { entry ->
                    HistoryRow(
                        entry = entry,
                        isDeleting = entry.station.id in state.pendingDeletes,
                        onOpen = { callbacks.onStation(entry.station.id) },
                        onPlay = { callbacks.onPlay(entry) },
                        onRemove = { callbacks.onRemove(entry.station.id) },
                    )
                }
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

/**
 * Tarjeta de cuadrícula del historial (spec 008): artwork 16:9, nombre,
 * país/idioma y hora relativa; play directo y quitar del historial con
 * las mismas descripciones de accesibilidad que la lista (FR-008).
 */
@Composable
private fun HistoryGridCard(
    entry: HistoryEntryDto,
    isDeleting: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        onClick = onOpen,
        shape = MaterialTheme.shapes.large,
        colors =
            androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(GRID_ARTWORK_ASPECT_RATIO)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(GRID_SCRIM_COLOR)),
                        ),
                    ),
        ) {
            StationArtwork(entry.station, Modifier.matchParentSize().clip(MaterialTheme.shapes.large))
            IconButton(onClick = onPlay, modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).size(36.dp)) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir")
            }
            IconButton(
                onClick = onRemove,
                enabled = !isDeleting,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(36.dp),
            ) {
                val tint =
                    if (isDeleting) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar del historial", tint = tint)
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text(
                entry.station.name.ifBlank { "Emisora sin nombre" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                listOfNotNull(entry.station.country, entry.station.language).joinToString(" · ").ifBlank { "Emisora" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            relativeTime(entry.playedAt)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
