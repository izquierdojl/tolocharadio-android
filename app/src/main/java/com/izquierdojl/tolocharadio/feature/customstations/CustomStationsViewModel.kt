package com.izquierdojl.tolocharadio.feature.customstations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.fieldMessage
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.CustomStationsRepo
import com.izquierdojl.tolocharadio.data.repo.FavoritesRepo
import com.izquierdojl.tolocharadio.domain.ObserveCustomStationsUseCase
import com.izquierdojl.tolocharadio.domain.ValidateCustomStationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI de Mis emisoras: lista del servidor, vacío, error. */
sealed interface CustomStationsUiState {
    data object Loading : CustomStationsUiState

    data object Empty : CustomStationsUiState

    data class Content(
        val items: List<StationDto>,
        val offline: Boolean = false,
        val pendingDeletes: Set<String> = emptySet(),
    ) : CustomStationsUiState

    data class Error(val message: String) : CustomStationsUiState
}

/** Estado del formulario de alta (siempre visible, incluso con lista en error). */
data class CustomStationFormState(
    val name: String = "",
    val url: String = "",
    val nameError: String? = null,
    val urlError: String? = null,
    val submitting: Boolean = false,
)

/**
 * Mis emisoras: lista, alta con validación, reproducción persistente y
 * borrado con invalidación de Favoritos. La verdad es el servidor;
 * caché Room como fallback offline.
 */
@HiltViewModel
class CustomStationsViewModel
    @Inject
    constructor(
        private val observe: ObserveCustomStationsUseCase,
        private val repo: CustomStationsRepo,
        private val favorites: FavoritesRepo,
        private val validate: ValidateCustomStationUseCase,
    ) : ViewModel() {
        private val _ui = MutableStateFlow<CustomStationsUiState>(CustomStationsUiState.Loading)
        val ui: StateFlow<CustomStationsUiState> = _ui.asStateFlow()

        private val _form = MutableStateFlow(CustomStationFormState())
        val form: StateFlow<CustomStationFormState> = _form.asStateFlow()

        private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val messages: SharedFlow<String> = _messages.asSharedFlow()

        init {
            refresh()
        }

        /** Recarga la lista del servidor (o caché offline). */
        fun refresh() {
            viewModelScope.launch {
                if (_ui.value !is CustomStationsUiState.Content) _ui.value = CustomStationsUiState.Loading
                when (val r = observe()) {
                    is ApiResult.Ok -> {
                        _ui.value =
                            if (r.value.items.isEmpty()) {
                                CustomStationsUiState.Empty
                            } else {
                                CustomStationsUiState.Content(r.value.items, offline = r.value.offline)
                            }
                    }
                    is ApiResult.Err -> {
                        if (_ui.value !is CustomStationsUiState.Content) {
                            _ui.value = CustomStationsUiState.Error(r.error.userMessage())
                        } else {
                            _messages.tryEmit(r.error.userMessage())
                        }
                    }
                }
            }
        }

        /** Alias de [refresh] para el botón de reintento. */
        fun retry() = refresh()

        /** Actualiza el campo nombre y limpia su error al escribir. */
        fun onNameChange(value: String) {
            _form.value = _form.value.copy(name = value, nameError = null)
        }

        /** Actualiza el campo URL y limpia su error al escribir. */
        fun onUrlChange(value: String) {
            _form.value = _form.value.copy(url = value, urlError = null)
        }

        /**
         * Envía el formulario: valida en cliente (sin red si hay errores),
         * crea en servidor con actualización optimista y limpia el
         * formulario ante éxito. Un 422 muestra el detalle por campo.
         */
        fun onSubmit() {
            val current = _form.value
            if (current.submitting) return
            val name = current.name.trim()
            val url = current.url.trim()
            val nameError = validate.name(name)
            val urlError = validate.streamUrl(url)
            if (nameError != null || urlError != null) {
                _form.value = current.copy(nameError = nameError, urlError = urlError)
                return
            }
            _form.value = current.copy(nameError = null, urlError = null, submitting = true)
            viewModelScope.launch {
                val result = repo.create(name, url)
                when (result) {
                    is ApiResult.Ok -> {
                        _form.value = CustomStationFormState()
                        _messages.tryEmit("Emisora personalizada añadida")
                        refresh()
                    }
                    is ApiResult.Err -> {
                        val error = result.error
                        _form.value =
                            _form.value.copy(
                                submitting = false,
                                nameError = error.fieldMessage("name"),
                                urlError = error.fieldMessage("url"),
                            )
                        if (error.fieldMessage("name") == null && error.fieldMessage("url") == null) {
                            _messages.tryEmit(error.userMessage())
                        }
                    }
                }
            }
        }

        /**
         * Elimina una emisora (optimista). En caso de error revierte el
         * estado y muestra snackbar. Tras el éxito refresca Favoritos
         * (la personalizada podía ser favorita, research D4).
         */
        fun onDelete(id: String) {
            val content = _ui.value as? CustomStationsUiState.Content ?: return
            if (content.items.none { it.id == id }) return
            _ui.value =
                content.copy(
                    items = content.items.filterNot { it.id == id },
                    pendingDeletes = content.pendingDeletes + id,
                )
            viewModelScope.launch {
                val result = repo.delete(id)
                when (result) {
                    is ApiResult.Ok -> {
                        val current = _ui.value
                        _ui.value =
                            if (current is CustomStationsUiState.Content) {
                                val remaining = current.pendingDeletes - id
                                if (current.items.isEmpty()) {
                                    CustomStationsUiState.Empty
                                } else {
                                    current.copy(pendingDeletes = remaining)
                                }
                            } else {
                                current
                            }
                        _messages.tryEmit("Emisora eliminada")
                        favorites.list()
                    }
                    is ApiResult.Err -> {
                        _ui.value = content
                        _messages.tryEmit(result.error.userMessage())
                    }
                }
            }
        }
    }

