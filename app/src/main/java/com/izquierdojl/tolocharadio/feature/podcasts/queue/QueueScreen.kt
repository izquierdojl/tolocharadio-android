package com.izquierdojl.tolocharadio.feature.podcasts.queue

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.AsyncImage
import com.izquierdojl.tolocharadio.core.ui.components.EmptyState
import com.izquierdojl.tolocharadio.core.ui.components.SectionHeader
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.core.ui.components.formatEpisodeDuration
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.domain.podcasts.remainingLabel
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerUi
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Distancia al borde (px) que dispara el auto-scroll durante el arrastre. */
private const val QUEUE_DRAG_EDGE_PX = 96f

/** Desplazamiento (px) máximo por tick de auto-scroll. */
private const val QUEUE_DRAG_SCROLL_PX = 32f

/** Fracción mínima del auto-scroll justo en el borde del umbral. */
private const val QUEUE_DRAG_SCROLL_MIN_RATIO = 0.25f

/** Escala de la tarjeta activa durante el arrastre. */
private const val QUEUE_DRAG_LIFT_SCALE = 1.02f

/** Elevación (dp) de la tarjeta activa durante el arrastre. */
private const val QUEUE_DRAG_LIFT_ELEVATION = 8

/**
 * Cola de reproducción (US4, FR-019–FR-022): tarjetas que se
 * intercambian con el dedo (paridad web), quitar y vaciado sin
 * detener el episodio en curso.
 */
@Composable
fun QueueScreen(
    snackbar: SnackbarHostState,
    onOpenFullPlayer: () -> Unit,
    viewModel: QueueViewModel = hiltViewModel(),
    player: EpisodePlayerViewModel = hiltViewModel(LocalContext.current as ComponentActivity),
) {
    val ui by viewModel.ui.collectAsState()
    val message by viewModel.message.collectAsState()
    val playerUi by player.ui.collectAsState()
    val playingId = playerUi.episode?.id

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(playingId) {
        viewModel.setPlayingId(playingId)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshSilently()
    }

    QueueScreenContent(
        state = ui,
        playerUi = playerUi,
        onPlay = { player.play(it, viewModel.podcastFor(it)) },
        onRemove = viewModel::remove,
        onMove = viewModel::moveItem,
        onCommit = viewModel::commitOrder,
        onClear = { viewModel.clear(playingId) },
        onRetry = viewModel::refresh,
        onOpenFullPlayer = onOpenFullPlayer,
    )
}

/**
 * Contenido sin Hilt de la cola: cabecera fija con el episodio en
 * curso y, debajo, los pendientes. Separado para los tests Compose.
 */
@Composable
@Suppress("LongParameterList")
internal fun QueueScreenContent(
    state: QueueUiState,
    playerUi: EpisodePlayerUi,
    onPlay: (PodcastEpisode) -> Unit,
    onRemove: (Long) -> Unit,
    onMove: (Int, Int) -> Unit,
    onCommit: () -> Unit,
    onClear: () -> Unit,
    onRetry: () -> Unit,
    onOpenFullPlayer: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        SectionHeader(title = "Cola", subtitle = "Arrastra las tarjetas para reordenar.")
        playerUi.episode?.let { episode ->
            QueueNowPlayingCard(
                episode = episode,
                podcast = playerUi.podcast,
                playing = playerUi.playing,
                positionMs = playerUi.positionMs,
                durationMs = playerUi.durationMs,
                onOpen = onOpenFullPlayer,
            )
        }
        when (state) {
            is QueueUiState.Loading ->
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            is QueueUiState.Empty ->
                EmptyState(
                    title = if (playerUi.episode == null) "Cola vacía" else "No hay más episodios en cola",
                    actionLabel = null,
                    onAction = null,
                )
            is QueueUiState.Error ->
                Column {
                    Text(state.message, modifier = Modifier.padding(16.dp))
                    TextButton(onClick = onRetry) {
                        Text("Reintentar")
                    }
                }
            is QueueUiState.Content -> {
                TextButton(onClick = onClear) {
                    Text("Vaciar cola")
                }
                QueueList(
                    state = state,
                    onPlay = onPlay,
                    onRemove = onRemove,
                    onMove = onMove,
                    onCommit = onCommit,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun QueueList(
    state: QueueUiState.Content,
    onPlay: (PodcastEpisode) -> Unit,
    onRemove: (Long) -> Unit,
    onMove: (Int, Int) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var columnTop by remember { mutableFloatStateOf(Float.NaN) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    LazyColumn(
        state = listState,
        modifier =
            modifier
                .testTag(TolochaTestTags.QUEUE_LIST)
                .onGloballyPositioned { columnTop = it.positionInWindow().y },
    ) {
        itemsIndexed(state.items, key = { _, item -> item.id }) { _, item ->
            val dragging = draggingId == item.id
            QueueCard(
                episode = item,
                dragging = dragging,
                onPlay = { onPlay(item) },
                onRemove = { onRemove(item.id) },
                dragModifier =
                    Modifier.queueDragHandle(
                        id = item.id,
                        listState = listState,
                        columnTop = { columnTop },
                        indexOf = { state.items.indexOfFirst { it.id == item.id } },
                        callbacks =
                            QueueDragCallbacks(
                                onMove = onMove,
                                onCommit = onCommit,
                                onAutoScroll = { dy ->
                                    if (scrollJob?.isActive != true) {
                                        scrollJob = scope.launch { listState.scrollBy(dy) }
                                    }
                                },
                                onDragStateChange = { active ->
                                    draggingId = if (active) item.id else null
                                },
                            ),
                    ),
            )
        }
    }
}

@Composable
private fun QueueCard(
    episode: PodcastEpisode,
    dragging: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    dragModifier: Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    ListItem(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .zIndex(if (dragging) 1f else 0f)
                .graphicsLayer {
                    val scale = if (dragging) QUEUE_DRAG_LIFT_SCALE else 1f
                    scaleX = scale
                    scaleY = scale
                }.shadow(if (dragging) QUEUE_DRAG_LIFT_ELEVATION.dp else 1.dp, shape)
                .background(
                    if (dragging) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
                    shape,
                ).clickable(onClick = onPlay),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text(episode.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            val duration =
                episode.durationSec?.takeIf { it > 0L }
                    ?: episode.progress?.durationSec?.takeIf { it > 0L }
            val remaining =
                if (episode.progress?.completed == true) {
                    null
                } else {
                    remainingLabel(episode.progress?.positionSec ?: 0L, duration)
                }
            val line = listOfNotNull(duration?.let { formatEpisodeDuration(it) }, remaining).joinToString(" · ")
            if (line.isNotBlank()) {
                Text(
                    line,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(TolochaTestTags.QUEUE_EPISODE_META),
                )
            }
        },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.DragHandle,
                    contentDescription = "Arrastrar para reordenar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = dragModifier.padding(end = 4.dp),
                )
                AsyncImage(
                    model = episode.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.small),
                )
            }
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Quitar de la cola")
            }
        },
    )
}

/**
 * Asa de arrastre: mueve la tarjeta en vivo y al soltar guarda el
 * orden (PUT con revisión; 409 recarga).
 *
 * Callbacks del asa de arrastre de la cola.
 */
private data class QueueDragCallbacks(
    val onMove: (Int, Int) -> Unit,
    val onCommit: () -> Unit,
    val onAutoScroll: (Float) -> Unit,
    val onDragStateChange: (Boolean) -> Unit,
)

private fun Modifier.queueDragHandle(
    id: Long,
    listState: LazyListState,
    columnTop: () -> Float,
    indexOf: () -> Int,
    callbacks: QueueDragCallbacks,
): Modifier =
    composed {
        val haptics = LocalHapticFeedback.current
        val indexOfState by rememberUpdatedState(indexOf)
        val callbacksState by rememberUpdatedState(callbacks)
        val topState by rememberUpdatedState(columnTop)
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
                            val top = topState()
                            if (!top.isNaN()) {
                                val viewportY = handleY + change.position.y - top
                                val target =
                                    listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                        item.key != id &&
                                            viewportY >= item.offset &&
                                            viewportY <= item.offset + item.size
                                    }?.index
                                if (target != null) {
                                    val from = indexOfState()
                                    if (from != -1 && from != target) {
                                        callbacksState.onMove(from, target)
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                                val viewportH = listState.layoutInfo.viewportSize.height.toFloat()
                                when {
                                    viewportY < QUEUE_DRAG_EDGE_PX ->
                                        callbacksState.onAutoScroll(-queueDragDelta(QUEUE_DRAG_EDGE_PX - viewportY))
                                    viewportY > viewportH - QUEUE_DRAG_EDGE_PX ->
                                        callbacksState.onAutoScroll(
                                            queueDragDelta(viewportY - (viewportH - QUEUE_DRAG_EDGE_PX)),
                                        )
                                }
                            }
                        },
                    )
                },
        )
    }

private fun queueDragDelta(over: Float): Float {
    val clamped = over.coerceIn(0f, QUEUE_DRAG_EDGE_PX)
    val ratio = QUEUE_DRAG_SCROLL_MIN_RATIO + (1 - QUEUE_DRAG_SCROLL_MIN_RATIO) * (clamped / QUEUE_DRAG_EDGE_PX)
    return QUEUE_DRAG_SCROLL_PX * ratio
}
