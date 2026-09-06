package com.izquierdojl.tolocharadio.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.remote.dto.FavoriteDto
import com.izquierdojl.tolocharadio.data.repo.FavoritesRepo
import com.izquierdojl.tolocharadio.domain.ObserveFavoritesUseCase
import com.izquierdojl.tolocharadio.domain.ReorderFavoritesUseCase
import com.izquierdojl.tolocharadio.domain.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Tiempo visible del Deshacer tras quitar (aclaración 2026-09-05). */
const val UNDO_TIMEOUT_MS = 10_000L

/** Favorita eliminada pendiente de deshacer, con su posición original. */
data class PendingUndo(val favorite: FavoriteDto, val index: Int)

/** UI de Favoritos: lista del servidor, vacío, error y progreso de orden. */
sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState

    data object Empty : FavoritesUiState

    data class Content(
        val items: List<FavoriteDto>,
        val offline: Boolean = false,
        val savingOrder: Boolean = false,
        val pendingUndo: PendingUndo? = null,
    ) : FavoritesUiState

    data class Error(val message: String) : FavoritesUiState
}

/**
 * Favoritos: lista, quitar con deshacer (10 s) y reorden con
 * autoguardado. La verdad es el servidor; ante divergencia de orden
 * gana el servidor (aclaración 2026-09-05).
 */
@HiltViewModel
class FavoritesViewModel
    @Inject
    constructor(
        private val observe: ObserveFavoritesUseCase,
        private val toggle: ToggleFavoriteUseCase,
        private val reorder: ReorderFavoritesUseCase,
        private val repo: FavoritesRepo,
    ) : ViewModel() {
        private val _ui = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
        val ui: StateFlow<FavoritesUiState> = _ui.asStateFlow()

        private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val messages: SharedFlow<String> = _messages.asSharedFlow()

        /** Ventana de Deshacer (ms). Mutable para tests; en producción [UNDO_TIMEOUT_MS]. */
        var undoTimeoutMs: Long = UNDO_TIMEOUT_MS

        /** Último orden confirmado por el servidor (reversión y permutación). */
        private var confirmed: List<FavoriteDto> = emptyList()
        private var undoJob: Job? = null

        init {
            refresh()
        }

        /** Recarga la lista del servidor (o caché offline). */
        fun refresh() {
            viewModelScope.launch {
                if (_ui.value !is FavoritesUiState.Content) _ui.value = FavoritesUiState.Loading
                when (val r = observe()) {
                    is ApiResult.Ok -> {
                        confirmed = r.value.items
                        cancelUndo()
                        _ui.value =
                            if (r.value.items.isEmpty()) {
                                FavoritesUiState.Empty
                            } else {
                                FavoritesUiState.Content(r.value.items, offline = r.value.offline)
                            }
                    }
                    is ApiResult.Err -> {
                        if (_ui.value !is FavoritesUiState.Content) {
                            _ui.value = FavoritesUiState.Error(r.error.userMessage())
                        } else {
                            _messages.tryEmit(r.error.userMessage())
                        }
                    }
                }
            }
        }

        /** Alias de [refresh] para el botón de reintento. */
        fun retry() = refresh()

        /**
         * Toggle genérico (p. ej. corazón en ficha): si está en la lista
         * equivale a quitar con deshacer; si no, añade y refresca.
         */
        fun toggleFavorite(stationId: String) {
            val items = (_ui.value as? FavoritesUiState.Content)?.items.orEmpty()
            if (items.any { it.station.id == stationId }) {
                removeWithUndo(stationId)
                return
            }
            viewModelScope.launch {
                when (val r = toggle(stationId, false)) {
                    is ApiResult.Ok -> refresh()
                    is ApiResult.Err -> _messages.tryEmit(r.error.userMessage())
                }
            }
        }

        /** Quita optimista con opción de deshacer durante [undoTimeoutMs]. */
        fun removeWithUndo(stationId: String) {
            val content = _ui.value as? FavoritesUiState.Content ?: return
            val index = content.items.indexOfFirst { it.station.id == stationId }
            if (index == -1) return
            val removed = content.items[index]
            _ui.value = content.copy(items = content.items - removed, pendingUndo = PendingUndo(removed, index))
            scheduleUndoExpiry()
            viewModelScope.launch {
                when (val r = repo.remove(stationId)) {
                    is ApiResult.Ok -> {
                        confirmed = confirmed.filterNot { it.station.id == stationId }
                    }
                    is ApiResult.Err -> {
                        // Reversión: vuelve a su sitio y avisa.
                        cancelUndo()
                        _ui.value = content
                        _messages.tryEmit(r.error.userMessage())
                    }
                }
            }
        }

        /** Deshace el quitado re-guardando en su posición original. */
        fun undoRemove() {
            val content = _ui.value as? FavoritesUiState.Content ?: return
            val pending = content.pendingUndo ?: return
            cancelUndo()
            _ui.value = content.copy(pendingUndo = null)
            viewModelScope.launch {
                when (val r = repo.add(pending.favorite.station.id)) {
                    is ApiResult.Ok -> {
                        val at = pending.index.coerceIn(0, content.items.size)
                        val items = content.items.toMutableList().apply { add(at, pending.favorite) }
                        confirmed = items
                        _ui.value = content.copy(items = items, pendingUndo = null)
                    }
                    is ApiResult.Err -> {
                        _messages.tryEmit(r.error.userMessage())
                        refresh()
                    }
                }
            }
        }

        /** Mueve localmente (arrastrar); el guardado llega con [commitOrder]. */
        fun moveItem(
            from: Int,
            to: Int,
        ) {
            val content = _ui.value as? FavoritesUiState.Content ?: return
            if (from == to || from !in content.items.indices || to !in content.items.indices) return
            val items = content.items.toMutableList().apply { add(to, removeAt(from)) }
            _ui.value = content.copy(items = items)
        }

        /** Guarda el orden actual en el servidor (autoguardado al soltar). */
        fun commitOrder() {
            val content = _ui.value as? FavoritesUiState.Content ?: return
            val next = content.items.map { it.station.id }
            if (next == confirmed.map { it.station.id }) return
            _ui.value = content.copy(savingOrder = true)
            viewModelScope.launch {
                when (val r = reorder(confirmed.map { it.station.id }, next)) {
                    is ApiResult.Ok -> {
                        confirmed = content.items
                        _ui.value = content.copy(savingOrder = false)
                        refresh()
                    }
                    is ApiResult.Err -> {
                        // Gana el servidor: se muestra su orden con aviso.
                        _ui.value = content.copy(items = confirmed, savingOrder = false)
                        _messages.tryEmit(
                            "El orden cambió en otro dispositivo. Mostrando el guardado.",
                        )
                        refresh()
                    }
                }
            }
        }

        private fun scheduleUndoExpiry() {
            undoJob?.cancel()
            undoJob =
                viewModelScope.launch {
                    delay(undoTimeoutMs)
                    val content = _ui.value as? FavoritesUiState.Content ?: return@launch
                    _ui.value = content.copy(pendingUndo = null)
                }
        }

        private fun cancelUndo() {
            undoJob?.cancel()
            undoJob = null
        }
    }

