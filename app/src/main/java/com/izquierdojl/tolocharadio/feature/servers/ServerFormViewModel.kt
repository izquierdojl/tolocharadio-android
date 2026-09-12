package com.izquierdojl.tolocharadio.feature.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import com.izquierdojl.tolocharadio.domain.servers.AddServerUseCase
import com.izquierdojl.tolocharadio.domain.servers.UpdateServerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Modo del formulario unificado. */
enum class ServerFormMode { ADD, EDIT }

/** Estado UI del formulario unificado de servidor. */
data class ServerFormUi(
    val mode: ServerFormMode = ServerFormMode.ADD,
    val serverId: String? = null,
    val url: String = "",
    val alias: String = "",
    val email: String = "",
    val password: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

/**
 * Formulario unificado de servidor (alta y edición): URL, alias,
 * email y contraseña. En edición precarga la contraseña enmascarada
 * (FR-002/FR-007).
 */
@HiltViewModel
class ServerFormViewModel
    @Inject
    constructor(
        private val addServer: AddServerUseCase,
        private val updateServer: UpdateServerUseCase,
        private val repository: ServerRepository,
        private val tokens: TokenStore,
        private val prefs: InstancePrefs,
    ) : ViewModel() {
        private val _ui = MutableStateFlow(ServerFormUi())
        val ui: StateFlow<ServerFormUi> = _ui.asStateFlow()

        /** Carga el servidor para editar (incluye credenciales enmascaradas). */
        fun loadForEdit(serverId: String) {
            viewModelScope.launch {
                val server = repository.getById(serverId) ?: return@launch
                val creds = tokens.getCredentials(serverId)
                _ui.value =
                    ServerFormUi(
                        mode = ServerFormMode.EDIT,
                        serverId = serverId,
                        url = server.url,
                        alias = server.alias,
                        email = creds?.email.orEmpty(),
                        password = creds?.password.orEmpty(),
                    )
            }
        }

        fun onUrlChange(url: String) {
            _ui.value = _ui.value.copy(url = url, error = null)
        }

        fun onAliasChange(alias: String) {
            _ui.value = _ui.value.copy(alias = alias, error = null)
        }

        fun onEmailChange(email: String) {
            _ui.value = _ui.value.copy(email = email, error = null)
        }

        fun onPasswordChange(password: String) {
            _ui.value = _ui.value.copy(password = password, error = null)
        }

        /** Guarda el servidor; valida en cliente y revalida con login. */
        fun save() {
            val state = _ui.value
            val clientError = clientValidation(state)
            if (clientError != null) {
                _ui.value = state.copy(error = clientError)
                return
            }
            _ui.value = state.copy(isSaving = true, error = null)
            viewModelScope.launch {
                val result =
                    if (state.mode == ServerFormMode.ADD) {
                        addServer(state.url, state.alias, state.email, state.password)
                    } else {
                        updateServer(state.serverId.orEmpty(), state.alias, state.email, state.password)
                    }
                when (result) {
                    is ApiResult.Ok -> {
                        if (state.mode == ServerFormMode.ADD) {
                            prefs.setBaseUrl(result.value.url)
                            prefs.setSetupDone(true)
                        }
                        _ui.value = _ui.value.copy(isSaving = false, saved = true)
                    }
                    is ApiResult.Err -> {
                        _ui.value = _ui.value.copy(isSaving = false, error = result.error.userMessage())
                    }
                }
            }
        }

        private fun clientValidation(state: ServerFormUi): String? =
            when {
                state.url.isBlank() -> "Escribe la URL del servidor."
                state.alias.isBlank() -> "Escribe un alias para el servidor."
                state.email.isBlank() -> "Escribe el email de la cuenta."
                state.password.isEmpty() -> "Escribe la contraseña."
                else -> null
            }
    }
