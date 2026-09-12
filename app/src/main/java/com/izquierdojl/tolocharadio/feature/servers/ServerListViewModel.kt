package com.izquierdojl.tolocharadio.feature.servers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
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
    val isSwitching: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

/** Lista de servidores: activar, editar (vía formulario) y eliminar. */
@HiltViewModel
class ServerListViewModel
    @Inject
    constructor(
        private val getServers: GetServersUseCase,
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

        /**
         * Cambia al servidor activo: asegura su sesión (refresh/re-login),
         * re-apunta la red y renace el proceso.
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

        /** Elimina un servidor; si era el último, avisa para volver al formulario. */
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
