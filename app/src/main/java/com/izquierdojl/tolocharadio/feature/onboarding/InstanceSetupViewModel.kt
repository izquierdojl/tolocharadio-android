package com.izquierdojl.tolocharadio.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.userMessage
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.domain.NormalizeBaseUrlUseCase
import com.izquierdojl.tolocharadio.domain.servers.AddServerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI de configuración del primer servidor. */
sealed interface SetupUiState {
    data class Idle(val url: String = "", val alias: String = "") : SetupUiState

    data class Error(val url: String, val alias: String, val message: String) : SetupUiState

    data object Connecting : SetupUiState
}

/**
 * Bienvenida/onboarding: valida la URL y crea el primer servidor
 * (activo y por defecto). No pide credenciales (FR-002/FR-003).
 */
@HiltViewModel
class InstanceSetupViewModel
    @Inject
    constructor(
        private val addServer: AddServerUseCase,
        private val prefs: InstancePrefs,
        private val normalize: NormalizeBaseUrlUseCase,
    ) : ViewModel() {
        private val _ui = MutableStateFlow<SetupUiState>(SetupUiState.Idle())
        val ui: StateFlow<SetupUiState> = _ui.asStateFlow()

        private var currentUrl: String = ""
        private var currentAlias: String = ""

        fun onUrlChange(url: String) {
            currentUrl = url
            _ui.value = SetupUiState.Idle(currentUrl, currentAlias)
        }

        fun onAliasChange(alias: String) {
            currentAlias = alias
            _ui.value = SetupUiState.Idle(currentUrl, currentAlias)
        }

        /** Valida `/health`+`/config`, crea el servidor y marca setup. */
        fun connect(
            rawUrl: String,
            alias: String,
            onConnected: () -> Unit,
        ) {
            val url = normalize(rawUrl)
            if (url == null) {
                _ui.value = SetupUiState.Error(rawUrl, alias, "Esa URL no parece válida. Usa https://…")
                return
            }
            _ui.value = SetupUiState.Connecting
            viewModelScope.launch {
                val effectiveAlias = alias.trim().ifBlank { "Mi servidor" }
                when (val result = addServer(url, effectiveAlias)) {
                    is ApiResult.Ok -> {
                        prefs.setBaseUrl(result.value.url)
                        prefs.setSetupDone(true)
                        onConnected()
                    }
                    is ApiResult.Err -> {
                        _ui.value = SetupUiState.Error(rawUrl, alias, result.error.userMessage())
                    }
                }
            }
        }
    }
