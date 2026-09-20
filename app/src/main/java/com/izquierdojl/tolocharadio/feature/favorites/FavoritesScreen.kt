package com.izquierdojl.tolocharadio.feature.favorites

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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

/** Desplazamiento (px) máximo por tick de auto-scroll durante el arrastre. */
private const val DRAG_SCROLL_PX = 32f

/** Fracción mínima del auto-scroll justo en el borde del umbral. */
private const val DRAG_SCROLL_MIN_RATIO = 0.25f

/** Escala de la fila activa durante el arrastre (feedback de "levantado"). */
private const val DRAG_LIFT_SCALE = 1.02f

/** Elevación (dp) de la fila activa durante el arrastre. */
private const val DRAG_LIFT_ELEVATION = 8

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

/** Datos de pintado de una fila de favorita. */
private data class FavoriteRowState(
    val fav: FavoriteDto,
    val showDragHandle: Boolean,
    val isDragging: Boolean,
)

/** Callbacks de una fila de favorita. */
private data class FavoriteRowActions(
    val onOpen: () -> Unit,
    val onPlay: () -> Unit,
    val onRemove: () -> Unit,
)

/** Callbacks del asa de arrastre. */
private data class DragCallbacks(
    val onMove: (Int, Int) -> Unit,
    val onCommit: () -> Unit,
    val onAutoScroll: (Float) -> Unit,
    val onDragStateChange: (Boolean) -> Unit,
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

    // El Scaffold externo (TolochaNavGraph) ya aplica los insets; el interno
    // solo aloja el Snackbar y no debe reañadir el inset superior (bug 0038).
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            SectionHeader(title = "Tus favoritos", subtitle = "Tus emisoras guardadas, en tu orden.")
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
                FavoritesList(s = state, actions = actions)
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
    actions: FavoriteListActions,
) {
    val reorderEnabled = !s.offline && s.items.size > 1
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
    var draggingId by remember { mutableStateOf<String?>(null) }
    LazyColumn(
        state = listState,
        modifier = Modifier.onGloballyPositioned { columnTop = it.positionInWindow().y },
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        itemsIndexed(s.items, key = { _, fav -> fav.station.id }) { _, fav ->
            FavoriteRow(
                state =
                    FavoriteRowState(
                        fav = fav,
                        showDragHandle = reorderEnabled,
                        isDragging = draggingId == fav.station.id,
                    ),
                actions =
                    FavoriteRowActions(
                        onOpen = { actions.onStation(fav.station.id) },
                        onPlay = { actions.onPlay(fav) },
                        onRemove = { actions.onRemove(fav.station.id) },
                    ),
                modifier = Modifier.animateItem(),
                dragModifier =
                    if (reorderEnabled) {
                        Modifier.favoriteDragHandle(
                            id = fav.station.id,
                            listState = listState,
                            columnTop = { columnTop },
                            indexOf = { s.items.indexOfFirst { it.station.id == fav.station.id } },
                            callbacks =
                                DragCallbacks(
                                    onMove = actions.onMove,
                                    onCommit = actions.onCommit,
                                    onAutoScroll = { dy ->
                                        if (scrollJob?.isActive != true) {
                                            scrollJob = scope.launch { listState.scrollBy(dy) }
                                        }
                                    },
                                    onDragStateChange = { active ->
                                        draggingId = if (active) fav.station.id else null
                                    },
                                ),
                        )
                    } else {
                        Modifier
                    },
            )
        }
    }
    if (reorderEnabled) {
        Text(
            "Arrastra el asa para reordenar. El corazón quita la favorita (puedes deshacer).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun FavoriteRow(
    state: FavoriteRowState,
    actions: FavoriteRowActions,
    modifier: Modifier = Modifier,
    dragModifier: Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val lift = if (state.isDragging) DRAG_LIFT_ELEVATION.dp else 0.dp
    val rowModifier =
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .zIndex(if (state.isDragging) 1f else 0f)
            .graphicsLayer {
                val scale = if (state.isDragging) DRAG_LIFT_SCALE else 1f
                scaleX = scale
                scaleY = scale
            }.shadow(lift, shape)
            .background(if (state.isDragging) MaterialTheme.colorScheme.surface else Color.Transparent, shape)
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.showDragHandle) {
            Icon(
                Icons.Filled.DragHandle,
                contentDescription = "Reordenar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = dragModifier.padding(8.dp),
            )
        }
        StationArtwork(state.fav.station, Modifier.size(48.dp))
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f).clickable(onClick = actions.onOpen).padding(vertical = 4.dp),
        ) {
            Text(
                state.fav.station.name.ifBlank { "Emisora sin nombre" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                listOfNotNull(state.fav.station.country, state.fav.station.language).joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val tags = state.fav.station.tags.take(MAX_ROW_TAGS)
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tags.forEach { TagChip(it) }
                }
            }
            relativeTime(state.fav.addedAt)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = actions.onPlay) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Reproducir")
        }
        FavoriteButton(isFavorite = true, onToggle = actions.onRemove)
    }
}

/**
 * Asa de arrastre: arrastre inmediato + vertical mueve la fila en vivo
 * ([DragCallbacks.onMove]), resalta la fila activa y al soltar guarda el
 * orden ([DragCallbacks.onCommit]).
 */
private fun Modifier.favoriteDragHandle(
    id: String,
    listState: LazyListState,
    columnTop: () -> Float,
    indexOf: () -> Int,
    callbacks: DragCallbacks,
): Modifier =
    composed {
        val haptics = LocalHapticFeedback.current
        val indexOfState by rememberUpdatedState(indexOf)
        val callbacksState by rememberUpdatedState(callbacks)
        val columnTopState by rememberUpdatedState(columnTop)
        var handleY by remember { mutableFloatStateOf(0f) }
        this.then(
            Modifier
                .onGloballyPositioned { handleY = it.positionInWindow().y }
                .pointerInput(id, listState) {
                    detectDragGestures(
                        onDragStart = {
                            callbacksState.onDragStateChange(true)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = {
                            callbacksState.onDragStateChange(false)
                            callbacksState.onCommit()
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragCancel = {
                            callbacksState.onDragStateChange(false)
                            callbacksState.onCommit()
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val top = columnTopState()
                            if (!top.isNaN()) {
                                val moved =
                                    onDragMove(
                                        viewportY = handleY + change.position.y - top,
                                        id = id,
                                        listState = listState,
                                        indexOfState = indexOfState,
                                        onMoveState = callbacksState.onMove,
                                        autoScrollState = callbacksState.onAutoScroll,
                                    )
                                if (moved) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        },
                    )
                },
        )
    }

/**
 * Calcula la fila bajo el dedo durante el arrastre, mueve y auto-desplaza.
 * Devuelve `true` si la fila cambió de posición (para la háptica).
 */
private fun onDragMove(
    viewportY: Float,
    id: String,
    listState: LazyListState,
    indexOfState: () -> Int,
    onMoveState: (Int, Int) -> Unit,
    autoScrollState: (Float) -> Unit,
): Boolean {
    val info = listState.layoutInfo
    val target =
        info.visibleItemsInfo
            .firstOrNull { item ->
                item.key != id &&
                    viewportY >= item.offset &&
                    viewportY <= item.offset + item.size
            }?.index
    var moved = false
    if (target != null) {
        val from = indexOfState()
        if (from != -1 && from != target) {
            onMoveState(from, target)
            moved = true
        }
    }
    val viewportH = info.viewportSize.height.toFloat()
    when {
        viewportY < DRAG_EDGE_PX -> autoScrollState(-dragScrollDelta(DRAG_EDGE_PX - viewportY))
        viewportY > viewportH - DRAG_EDGE_PX ->
            autoScrollState(dragScrollDelta(viewportY - (viewportH - DRAG_EDGE_PX)))
    }
    return moved
}

/** Velocidad de auto-scroll (px/tick) proporcional a la cercanía al borde. */
private fun dragScrollDelta(distanceToEdge: Float): Float {
    val ratio = (distanceToEdge / DRAG_EDGE_PX).coerceIn(0f, 1f)
    return DRAG_SCROLL_PX * (DRAG_SCROLL_MIN_RATIO + (1f - DRAG_SCROLL_MIN_RATIO) * ratio)
}
