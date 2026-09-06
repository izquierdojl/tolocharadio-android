package com.izquierdojl.tolocharadio.feature.servers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.core.session.TokenStore
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
        private val tokenStore: TokenStore,
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
            email: String = "",
            password: String = "",
        ) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isAdding = true, error = null)
                val result = addServerUseCase(url, alias, email, password)
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
         * Cambia al servidor activo (sin tocar el por defecto, FR-006) y
         * re-apunta la red a su URL:
         * - Con credenciales guardadas (refresh o email+password): renace
         *   el proceso y la sesión se restaura automáticamente (FR-006/006b).
         * - Sin credenciales: navega a Login ([onNeedLogin]) para pedirlas
         *   para ese servidor (ya activo).
         */
        fun switchServer(
            serverId: String,
            onNeedLogin: () -> Unit = {},
        ) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isSwitching = true, error = null)
                val result = switchServerUseCase(serverId)
                when (result) {
                    is ApiResult.Err ->
                        _uiState.value =
                            _uiState.value.copy(
                                isSwitching = false,
                                error = result.error.userMessage(),
                            )
                    is ApiResult.Ok -> {
                        val creds = tokenStore.getServerCredentials(serverId)
                        val hasCredentials =
                            creds?.refresh != null || (creds?.email != null && creds?.password != null)
                        instancePrefs.setBaseUrl(result.value.url)
                        if (hasCredentials) {
                            ProcessPhoenix.triggerRebirth(context)
                        } else {
                            _uiState.value = _uiState.value.copy(isSwitching = false)
                            onNeedLogin()
                        }
                    }
                }
            }
        }

        fun deleteServer(serverId: String) {
            viewModelScope.launch {
                deleteServerUseCase(serverId)
                _uiState.value = _uiState.value.copy(successMessage = "Servidor eliminado")
            }
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(error = null)
        }

        fun clearSuccess() {
            _uiState.value = _uiState.value.copy(successMessage = null)
        }
    }
