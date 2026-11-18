package com.izquierdojl.tolocharadio.feature.podcasts.subscriptions

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback

/** Distancia al borde (px) que dispara el auto-scroll durante el arrastre. */
private const val DRAG_EDGE_PX = 96f

/** Desplazamiento (px) máximo por tick de auto-scroll. */
private const val DRAG_SCROLL_PX = 32f

/** Fracción mínima del auto-scroll justo en el borde del umbral. */
private const val DRAG_SCROLL_MIN_RATIO = 0.25f

/** Callbacks del asa de arrastre de suscripciones. */
data class ReorderDragCallbacks(
    val onMove: (Int, Int) -> Unit,
    val onCommit: () -> Unit,
    val onAutoScroll: (Float) -> Unit,
    val onDragStateChange: (Boolean) -> Unit,
)

/**
 * Asa de reordenación manual: arrastra la fila en vivo, autodesplaza
 * cerca de los bordes y guarda el orden al soltar.
 */
fun Modifier.reorderDragHandle(
    key: Long,
    listState: LazyListState,
    columnTop: () -> Float,
    indexOf: () -> Int,
    callbacks: ReorderDragCallbacks,
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
                .pointerInput(key, listState) {
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
                                val moved =
                                    dragMove(
                                        viewportY = handleY + change.position.y - top,
                                        key = key,
                                        listState = listState,
                                        indexOfState = indexOfState,
                                        callbacks = callbacksState,
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

/** Mueve la fila bajo el dedo y autodesplaza cerca de los bordes. */
@Suppress("LongParameterList")
private fun dragMove(
    viewportY: Float,
    key: Long,
    listState: LazyListState,
    indexOfState: () -> Int,
    callbacks: ReorderDragCallbacks,
): Boolean {
    val info = listState.layoutInfo
    val target =
        info.visibleItemsInfo
            .firstOrNull { item ->
                item.key != key &&
                    viewportY >= item.offset &&
                    viewportY <= item.offset + item.size
            }?.index
    var moved = false
    if (target != null) {
        val from = indexOfState()
        if (from != -1 && from != target) {
            callbacks.onMove(from, target)
            moved = true
        }
    }
    val viewportH = info.viewportSize.height.toFloat()
    when {
        viewportY < DRAG_EDGE_PX ->
            callbacks.onAutoScroll(-dragScrollDelta(DRAG_EDGE_PX - viewportY))
        viewportY > viewportH - DRAG_EDGE_PX ->
            callbacks.onAutoScroll(dragScrollDelta(viewportY - (viewportH - DRAG_EDGE_PX)))
    }
    return moved
}

/** Velocidad de auto-scroll (px/tick) proporcional a la cercanía al borde. */
private fun dragScrollDelta(distanceToEdge: Float): Float {
    val ratio = (distanceToEdge / DRAG_EDGE_PX).coerceIn(0f, 1f)
    return DRAG_SCROLL_PX * (DRAG_SCROLL_MIN_RATIO + (1f - DRAG_SCROLL_MIN_RATIO) * ratio)
}
