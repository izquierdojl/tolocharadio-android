package com.izquierdojl.tolocharadio.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
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

/**
 * Mensaje transitorio para el snackbar, con acción opcional cuando el
 * error es recuperable (p. ej. reintento del guardado de orden, FR-010).
 */
data class FavoritesMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

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

    /** Error con marca de credenciales: en ese caso se ofrece editar el servidor (FR-006). */
    data class Error(
        val message: String,
        val isAuthError: Boolean = false,
    ) : FavoritesUiState
}

/**
 * Favoritos: lista, quitar con deshacer (10 s) y reorden con
 * autoguardado. La verdad es el servidor; ante divergencia de orden
 * gana el servidor (aclaración 2026-09-05).
 */
@HiltViewModel
@Suppress("TooManyFunctions")
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

        private val _messages = MutableSharedFlow<FavoritesMessage>(extraBufferCapacity = 1)
        val messages: SharedFlow<FavoritesMessage> = _messages.asSharedFlow()

        /** Ventana de Deshacer (ms). Mutable para tests; en producción [UNDO_TIMEOUT_MS]. */
        var undoTimeoutMs: Long = UNDO_TIMEOUT_MS

        /** Último orden confirmado por el servidor (reversión y permutación). */
        private var confirmed: List<FavoriteDto> = emptyList()

        /** Orden intentado que falló de forma recuperable (reintento, FR-010). */
        private var pendingOrder: List<String>? = null

        private var undoJob: Job? = null

        /** Carga en curso: evita duplicar peticiones al reanudar. */
        private var loading = false

        init {
            refresh()
            // Reconciliación reactiva: altas/bajas hechas en otras pantallas
            // (Explorar, ficha) se reflejan al momento, sin recargar de red.
            viewModelScope.launch {
                repo.favorites.collect(::onFavoritesChanged)
            }
        }

        /**
         * Aplica la lista observada del repo conservando el estado transitorio
         * de la UI (deshacer en curso, guardado de orden, banner offline).
         */
        private fun onFavoritesChanged(items: List<FavoriteDto>) {
            val current = _ui.value
            if (current is FavoritesUiState.Loading && items.isEmpty()) return
            if (items.isEmpty() && current is FavoritesUiState.Error) return
            confirmed = items
            val pendingUndo = (current as? FavoritesUiState.Content)?.pendingUndo
            _ui.value =
                when {
                    items.isEmpty() && pendingUndo == null -> FavoritesUiState.Empty
                    current is FavoritesUiState.Content -> current.copy(items = items)
                    else -> FavoritesUiState.Content(items)
                }
        }

        /**
         * Recarga al volver a primer plano (la sesión o la red pueden
         * haberse perdido durante el reposo). Se omite si ya hay una
         * carga en curso.
         */
        fun onForeground() {
            if (loading) return
            refresh()
        }

        /** Recarga la lista del servidor (o caché offline). */
        fun refresh() {
            viewModelScope.launch {
                loading = true
                try {
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
                                _ui.value =
                                    FavoritesUiState.Error(
                                        r.error.userMessage(),
                                        isAuthError = r.error is DomainError.Unauthorized,
                                    )
                            } else {
                                _messages.tryEmit(FavoritesMessage(r.error.userMessage()))
                            }
                        }
                    }
                } finally {
                    loading = false
                }
            }
        }

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
                    is ApiResult.Err -> _messages.tryEmit(FavoritesMessage(r.error.userMessage()))
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
            undoJob?.cancel()
            undoJob =
                viewModelScope.launch {
                    delay(undoTimeoutMs)
                    val current = _ui.value as? FavoritesUiState.Content ?: return@launch
                    _ui.value = current.copy(pendingUndo = null)
                }
            viewModelScope.launch {
                when (val r = repo.remove(stationId)) {
                    is ApiResult.Ok -> {
                        confirmed = confirmed.filterNot { it.station.id == stationId }
                    }
                    is ApiResult.Err -> {
                        // Reversión: vuelve a su sitio y avisa.
                        cancelUndo()
                        _ui.value = content
                        _messages.tryEmit(FavoritesMessage(r.error.userMessage()))
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
                        _messages.tryEmit(FavoritesMessage(r.error.userMessage()))
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
            val intended = content.items
            val next = intended.map { it.station.id }
            if (next == confirmed.map { it.station.id }) return
            _ui.value = content.copy(savingOrder = true)
            viewModelScope.launch { persistOrder(next, intended) }
        }

        /**
         * Envía la permutación al servidor. Tras un fallo recuperable se
         * conserva [pendingOrder] para el reintento del snackbar (FR-010);
         * en conflicto entre dispositivos gana el servidor (FR-011).
         */
        private suspend fun persistOrder(
            next: List<String>,
            intended: List<FavoriteDto>,
        ) {
            when (val r = reorder(confirmed.map { it.station.id }, next)) {
                is ApiResult.Ok -> {
                    confirmed = intended
                    pendingOrder = null
                    (_ui.value as? FavoritesUiState.Content)?.let { _ui.value = it.copy(savingOrder = false) }
                    refresh()
                }
                is ApiResult.Err -> {
                    val conflict = r.error is DomainError.Conflict
                    pendingOrder = if (conflict) null else next
                    (_ui.value as? FavoritesUiState.Content)?.let {
                        _ui.value = it.copy(items = confirmed, savingOrder = false)
                    }
                    _messages.tryEmit(
                        if (conflict) {
                            FavoritesMessage("El orden cambió en otro dispositivo. Mostrando el guardado.")
                        } else {
                            FavoritesMessage(
                                text = "No se pudo guardar el orden.",
                                actionLabel = "Reintentar",
                                onAction = ::retryCommitOrder,
                            )
                        },
                    )
                    refresh()
                }
            }
        }

        /** Reintenta guardar el orden tras un fallo recuperable (FR-010). */
        private fun retryCommitOrder() {
            val target = pendingOrder ?: return
            val content = _ui.value as? FavoritesUiState.Content ?: return
            val byId = content.items.associateBy { it.station.id }
            if (byId.keys != target.toSet()) {
                // La lista cambió desde el fallo: se descarta la reintención.
                pendingOrder = null
                return
            }
            _ui.value = content.copy(items = target.mapNotNull { byId[it] })
            commitOrder()
        }

        private fun cancelUndo() {
            undoJob?.cancel()
            undoJob = null
        }
    }
