package com.izquierdojl.tolocharadio.feature.servers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.domain.servers.AddServerUseCase
import com.izquierdojl.tolocharadio.domain.servers.DeleteServerUseCase
import com.izquierdojl.tolocharadio.domain.servers.GetServersUseCase
import com.izquierdojl.tolocharadio.domain.servers.SavedServer
import com.izquierdojl.tolocharadio.domain.servers.SwitchServerUseCase
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado UI de la pantalla de servidores. */
data class ServerListUiState(
    val servers: List<SavedServer> = emptyList(),
    val isAdding: Boolean = false,
    val isSwitching: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class ServerListViewModel
    @Inject
    constructor(
        private val getServers: GetServersUseCase,
        private val addServerUseCase: AddServerUseCase,
        private val switchServerUseCase: SwitchServerUseCase,
        private val deleteServerUseCase: DeleteServerUseCase,
        private val instancePrefs: InstancePrefs,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ServerListUiState())
        val uiState: StateFlow<ServerListUiState> = _uiState.asStateFlow()

        val servers: StateFlow<List<SavedServer>> =
            getServers()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun addServer(
            url: String,
            alias: String,
        ) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isAdding = true, error = null)
                val result = addServerUseCase(url, alias)
                if (result is ApiResult.Err) {
                    _uiState.value =
                        _uiState.value.copy(
                            isAdding = false,
                            error = result.error.userMessage(),
                        )
                } else {
                    _uiState.value =
                        _uiState.value.copy(
                            isAdding = false,
                            successMessage = "Servidor añadido",
                        )
                }
            }
        }

        /**
         * Cambia al servidor activo (sin tocar el por defecto, FR-006),
         * re-apunta la red a su URL y renace el proceso para recrear
         * Retrofit (sin credenciales que restaurar).
         */
        fun switchServer(serverId: String) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isSwitching = true, error = null)
                when (val result = switchServerUseCase(serverId)) {
                    is ApiResult.Err ->
                        _uiState.value =
                            _uiState.value.copy(
                                isSwitching = false,
                                error = result.error.userMessage(),
                            )
                    is ApiResult.Ok -> {
                        instancePrefs.setBaseUrl(result.value.url)
                        ProcessPhoenix.triggerRebirth(context)
                    }
                }
            }
        }

        /**
         * Elimina un servidor. Si era el último, avisa para volver a la
         * bienvenida (FR-013).
         */
        fun deleteServer(
            serverId: String,
            onNoServers: () -> Unit = {},
        ) {
            viewModelScope.launch {
                deleteServerUseCase(serverId)
                if (getServers().first().isEmpty()) {
                    onNoServers()
                } else {
                    _uiState.value = _uiState.value.copy(successMessage = "Servidor eliminado")
                }
            }
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(error = null)
        }

        fun clearSuccess() {
            _uiState.value = _uiState.value.copy(successMessage = null)
        }
    }
