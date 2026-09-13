package com.izquierdojl.tolocharadio.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.remote.dto.HistoryEntryDto
import com.izquierdojl.tolocharadio.data.repo.HistoryRepo
import com.izquierdojl.tolocharadio.domain.ObserveHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Espera (ms) tras reproducir para que el servidor registre la escucha (paridad con web: 500 ms). */
private const val PLAY_REFRESH_DELAY_MS = 500L

/** UI de Historial: lista del servidor, vacío, error. */
sealed interface HistoryUiState {
    data object Loading : HistoryUiState

    data object Empty : HistoryUiState

    data class Content(
        val items: List<HistoryEntryDto>,
        val offline: Boolean = false,
        val pendingDeletes: Set<String> = emptySet(),
    ) : HistoryUiState

    /** Error con marca de credenciales: en ese caso se ofrece editar el servidor (FR-006). */
    data class Error(
        val message: String,
        val isAuthError: Boolean = false,
    ) : HistoryUiState
}

/**
 * Historial: lista, eliminación individual, limpieza y reproducción
 * persistente. La verdad es el servidor; caché Room como fallback offline.
 */
@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        private val observe: ObserveHistoryUseCase,
        private val repo: HistoryRepo,
    ) : ViewModel() {
        private val _ui = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
        val ui: StateFlow<HistoryUiState> = _ui.asStateFlow()

        private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val messages: SharedFlow<String> = _messages.asSharedFlow()

        init {
            refresh()
            // Reconciliación reactiva: escuchas registradas desde el player
            // (u otros orígenes) aparecen al momento, sin recargar de red.
            viewModelScope.launch {
                repo.items.collect(::onHistoryChanged)
            }
        }

        /**
         * Aplica la lista observada del repo conservando el estado transitorio
         * de la UI (borrados en vuelo, banner offline).
         */
        private fun onHistoryChanged(items: List<HistoryEntryDto>) {
            val current = _ui.value
            if (current is HistoryUiState.Loading && items.isEmpty()) return
            if (items.isEmpty() && current is HistoryUiState.Error) return
            val pendingDeletes = (current as? HistoryUiState.Content)?.pendingDeletes
            _ui.value =
                when {
                    items.isEmpty() && pendingDeletes.isNullOrEmpty() -> HistoryUiState.Empty
                    current is HistoryUiState.Content -> current.copy(items = items)
                    else -> HistoryUiState.Content(items)
                }
        }

        /** Carga en curso: evita duplicar peticiones al reanudar. */
        private var loading = false

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
                    if (_ui.value !is HistoryUiState.Content) _ui.value = HistoryUiState.Loading
                    when (val r = observe()) {
                        is ApiResult.Ok -> {
                            _ui.value =
                                if (r.value.items.isEmpty()) {
                                    HistoryUiState.Empty
                                } else {
                                    HistoryUiState.Content(r.value.items, offline = r.value.offline)
                                }
                        }
                        is ApiResult.Err -> {
                            if (_ui.value !is HistoryUiState.Content) {
                                _ui.value =
                                    HistoryUiState.Error(
                                        r.error.userMessage(),
                                        isAuthError = r.error is DomainError.Unauthorized,
                                    )
                            } else {
                                _messages.tryEmit(r.error.userMessage())
                            }
                        }
                    }
                } finally {
                    loading = false
                }
            }
        }

        /** Alias de [refresh] para el botón de reintento. */
        fun retry() = refresh()

        /**
         * Elimina una emisora del historial (optimista).
         * En caso de error revierte el estado y muestra snackbar.
         */
        fun onRemove(stationId: String) {
            val content = _ui.value as? HistoryUiState.Content ?: return
            if (content.items.none { it.station.id == stationId }) return
            _ui.value =
                content.copy(
                    items = content.items.filterNot { it.station.id == stationId },
                    pendingDeletes = content.pendingDeletes + stationId,
                )
            viewModelScope.launch {
                when (val r = repo.remove(stationId)) {
                    is ApiResult.Ok -> {
                        val current = _ui.value as? HistoryUiState.Content
                        _ui.value =
                            current?.copy(
                                pendingDeletes = current.pendingDeletes - stationId,
                            ) ?: _ui.value
                    }
                    is ApiResult.Err -> {
                        _ui.value = content
                        _messages.tryEmit(r.error.userMessage())
                    }
                }
            }
        }

        /**
         * Limpia todo el historial (optimista).
         * En caso de error revierte el estado y muestra snackbar.
         */
        fun onClearAll() {
            val content = _ui.value as? HistoryUiState.Content ?: return
            _ui.value = HistoryUiState.Empty
            viewModelScope.launch {
                when (val r = repo.clear()) {
                    is ApiResult.Ok -> { /* Éxito: lista vacía ya reflejada */ }
                    is ApiResult.Err -> {
                        // Reversión: vuelve a su estado anterior
                        _ui.value = content
                        _messages.tryEmit(r.error.userMessage())
                    }
                }
            }
        }

        /**
         * Refresca el historial tras una reproducción desde la propia pantalla.
         * La lista se actualiza automáticamente situando la emisora en primera posición.
         */
        fun onPlayTriggered() {
            viewModelScope.launch {
                // Breve espera para que el servidor registre la reproducción vía proxy
                kotlinx.coroutines.delay(PLAY_REFRESH_DELAY_MS)
                when (val r = observe()) {
                    is ApiResult.Ok -> {
                        if (r.value.items.isNotEmpty()) {
                            _ui.value = HistoryUiState.Content(r.value.items, offline = r.value.offline)
                        }
                    }
                    is ApiResult.Err -> { /* No interrumpir reproducción por error de refresh */ }
                }
            }
        }
    }
