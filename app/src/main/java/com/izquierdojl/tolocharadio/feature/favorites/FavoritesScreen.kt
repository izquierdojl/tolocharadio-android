package com.izquierdojl.tolocharadio.feature.favorites

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.izquierdojl.tolocharadio.core.ui.ViewMode
import com.izquierdojl.tolocharadio.core.ui.components.EmptyState
import com.izquierdojl.tolocharadio.core.ui.components.ErrorBanner
import com.izquierdojl.tolocharadio.core.ui.components.FavoriteButton
import com.izquierdojl.tolocharadio.core.ui.components.SectionHeader
import com.izquierdojl.tolocharadio.core.ui.components.StationArtwork
import com.izquierdojl.tolocharadio.core.ui.components.StationCard
import com.izquierdojl.tolocharadio.core.ui.components.TagChip
import com.izquierdojl.tolocharadio.data.remote.dto.FavoriteDto
import com.izquierdojl.tolocharadio.feature.ViewModeViewModel
import com.izquierdojl.tolocharadio.feature.player.PlayerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Distancia al borde (px) que dispara el auto-scroll durante el arrastre. */
private const val DRAG_EDGE_PX = 96f

/** Desplazamiento (px) por tick de auto-scroll durante el arrastre. */
private const val DRAG_SCROLL_PX = 32f

/** Nº máximo de etiquetas visibles por fila (paridad con `StationCard`). */
private const val MAX_ROW_TAGS = 3

/** Acciones de la lista de favoritas (evita firmas largas). */
data class FavoriteListActions(
    val onStation: (String) -> Unit,
    val onExplore: () -> Unit,
    val onPlay: (FavoriteDto) -> Unit,
    val onRemove: (String) -> Unit,
    val onMove: (Int, Int) -> Unit,
    val onCommit: () -> Unit,
    val onRetry: () -> Unit,
    val onEditServer: () -> Unit = {},
)

/**
 * Favoritos: lista en orden personalizado con asa de arrastre,
 * deshacer de 10 s y reproducción persistente.
 *
 * @param onStation abre la ficha al pulsar una favorita.
 * @param onExplore atajo del estado vacío al catálogo.
 */
@Composable
fun FavoritesScreen(
    onStation: (String) -> Unit,
    onExplore: () -> Unit,
    onEditServer: () -> Unit = {},
    viewModel: FavoritesViewModel = hiltViewModel(),
    player: PlayerViewModel = hiltViewModel(),
    viewModeVm: ViewModeViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val ui by viewModel.ui.collectAsState()
    val mode by viewModeVm.mode.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    val pendingUndo = (ui as? FavoritesUiState.Content)?.pendingUndo
    if (pendingUndo != null) {
        LaunchedEffect(pendingUndo) {
            try {
                val result =
                    snackbar.showSnackbar(
                        message = "Favorita eliminada",
                        actionLabel = "Deshacer",
                        duration = SnackbarDuration.Indefinite,
                    )
                if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove()
            } finally {
                snackbar.currentSnackbarData?.dismiss()
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }
    // Al volver del reposo, refresca con la sesión/red restauradas.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onForeground()
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) {
        Column(Modifier.fillMaxSize()) {
            SectionHeader(title = "Tus favoritos")
            FavoritesScreenContent(
                state = ui,
                mode = mode,
                actions =
                    FavoriteListActions(
                        onStation = onStation,
                        onExplore = onExplore,
                        onPlay = { player.play(it.station) },
                        onRemove = viewModel::removeWithUndo,
                        onMove = viewModel::moveItem,
                        onCommit = viewModel::commitOrder,
                        onRetry = viewModel::refresh,
                        onEditServer = onEditServer,
                    ),
            )
        }
    }
}

/** Contenido sin estado (puerta de test sin Hilt). El reorden solo existe en modo lista (research D4). */
@Composable
fun FavoritesScreenContent(
    state: FavoritesUiState,
    actions: FavoriteListActions,
    mode: ViewMode = ViewMode.LIST,
) {
    when (state) {
        FavoritesUiState.Loading -> CircularProgressIndicator(Modifier.padding(32.dp))
        FavoritesUiState.Empty ->
            EmptyState(
                title = "Aún no tienes favoritas",
                actionLabel = "Explorar",
                onAction = actions.onExplore,
            )
        is FavoritesUiState.Error ->
            ErrorBanner(
                message = state.message,
                onRetry = if (state.isAuthError) actions.onEditServer else actions.onRetry,
                retryLabel = if (state.isAuthError) "Editar servidor" else "Reintentar",
            )
        is FavoritesUiState.Content ->
            if (mode == ViewMode.GRID) {
                FavoritesGrid(
                    s = state,
                    onStation = actions.onStation,
                    onRemove = actions.onRemove,
                )
            } else {
                FavoritesList(
                    s = state,
                    onStation = actions.onStation,
                    onPlay = actions.onPlay,
                    onRemove = actions.onRemove,
                    onMove = actions.onMove,
                    onCommit = actions.onCommit,
                )
            }
    }
}

/** Cuadrícula de favoritas: la card abre la ficha y el corazón quita (FR-008). */
@Composable
private fun FavoritesGrid(
    s: FavoritesUiState.Content,
    onStation: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    if (s.offline) {
        Text(
            "Mostrando caché sin conexión.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(s.items, key = { it.station.id }) { fav ->
            StationCard(
                station = fav.station,
                isFavorite = true,
                onPlay = { onStation(fav.station.id) },
                onToggleFavorite = { onRemove(fav.station.id) },
            )
        }
    }
}

@Composable
private fun FavoritesList(
    s: FavoritesUiState.Content,
    onStation: (String) -> Unit,
    onPlay: (FavoriteDto) -> Unit,
    onRemove: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
    onCommit: () -> Unit,
) {
    if (s.offline) {
        Text(
            "Mostrando caché sin conexión.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    if (s.savingOrder) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var columnTop by remember { mutableFloatStateOf(Float.NaN) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    LazyColumn(
        state = listState,
        modifier = Modifier.onGloballyPositioned { columnTop = it.positionInWindow().y },
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(s.items, key = { it.station.id }) { fav ->
            FavoriteRow(
                fav = fav,
                dragModifier =
                    Modifier.favoriteDragHandle(
                        id = fav.station.id,
                        listState = listState,
                        columnTop = { columnTop },
                        indexOf = { s.items.indexOfFirst { it.station.id == fav.station.id } },
                        onMove = onMove,
                        onCommit = onCommit,
                        onAutoScroll = { dy ->
                            if (scrollJob?.isActive != true) {
                                scrollJob = scope.launch { listState.scrollBy(dy) }
                            }
                        },
                    ),
                onOpen = { onStation(fav.station.id) },
                onPlay = { onPlay(fav) },
                onRemove = { onRemove(fav.station.id) },
            )
        }
    }
    Text(
        "Arrastra el asa para reordenar. El corazón quita la favorita (puedes deshacer).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
    )
}

@Composable
private fun FavoriteRow(
    fav: FavoriteDto,
    dragModifier: Modifier,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.DragHandle,
            contentDescription = "Reordenar",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = dragModifier.padding(8.dp),
        )
        StationArtwork(fav.station, Modifier.size(48.dp))
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f).clickable(onClick = onOpen).padding(vertical = 4.dp),
        ) {
            Text(
                fav.station.name.ifBlank { "Emisora sin nombre" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                listOfNotNull(fav.station.country, fav.station.language).joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val tags = fav.station.tags.take(MAX_ROW_TAGS)
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tags.forEach { TagChip(it) }
                }
            }
            relativeTime(fav.addedAt)?.let {
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
        FavoriteButton(isFavorite = true, onToggle = onRemove)
    }
}

/**
 * Asa de arrastre: pulsación larga + vertical mueve la fila en vivo
 * ([onMove]) y al soltar guarda el orden ([onCommit]).
 */
private fun Modifier.favoriteDragHandle(
    id: String,
    listState: LazyListState,
    columnTop: () -> Float,
    indexOf: () -> Int,
    onMove: (Int, Int) -> Unit,
    onCommit: () -> Unit,
    onAutoScroll: (Float) -> Unit,
): Modifier =
    composed {
        val indexOfState by rememberUpdatedState(indexOf)
        val onMoveState by rememberUpdatedState(onMove)
        val onCommitState by rememberUpdatedState(onCommit)
        val autoScrollState by rememberUpdatedState(onAutoScroll)
        val columnTopState by rememberUpdatedState(columnTop)
        var handleY by remember { mutableFloatStateOf(0f) }
        this.then(
            Modifier
                .onGloballyPositioned { handleY = it.positionInWindow().y }
                .pointerInput(id, listState) {
                    detectDragGesturesAfterLongPress(
                        onDragEnd = { onCommitState() },
                        onDragCancel = { onCommitState() },
                        onDrag = { change, _ ->
                            change.consume()
                            val top = columnTopState()
                            if (!top.isNaN()) {
                                onDragMove(
                                    viewportY = handleY + change.position.y - top,
                                    id = id,
                                    listState = listState,
                                    indexOfState = indexOfState,
                                    onMoveState = onMoveState,
                                    autoScrollState = autoScrollState,
                                )
                            }
                        },
                    )
                },
        )
    }

/** Calcula la fila bajo el dedo durante el arrastre y mueve/auto-desplaza. */
private fun onDragMove(
    viewportY: Float,
    id: String,
    listState: LazyListState,
    indexOfState: () -> Int,
    onMoveState: (Int, Int) -> Unit,
    autoScrollState: (Float) -> Unit,
) {
    val info = listState.layoutInfo
    val target =
        info.visibleItemsInfo
            .firstOrNull { item ->
                item.key != id &&
                    viewportY >= item.offset &&
                    viewportY <= item.offset + item.size
            }?.index
    if (target != null) {
        val from = indexOfState()
        if (from != -1 && from != target) onMoveState(from, target)
    }
    val viewportH = info.viewportSize.height.toFloat()
    when {
        viewportY < DRAG_EDGE_PX -> autoScrollState(-DRAG_SCROLL_PX)
        viewportY > viewportH - DRAG_EDGE_PX -> autoScrollState(DRAG_SCROLL_PX)
    }
}
