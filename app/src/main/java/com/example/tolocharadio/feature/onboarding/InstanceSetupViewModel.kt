package com.example.tolocharadio.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tolocharadio.core.session.SessionManager
import com.example.tolocharadio.data.local.InstancePrefs
import com.example.tolocharadio.data.local.TolochaDb
import com.example.tolocharadio.domain.NormalizeBaseUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI de configuración de instancia. */
sealed interface SetupUiState {
    data class Idle(val url: String = "") : SetupUiState

    data class Error(val url: String, val message: String) : SetupUiState

    data object Connecting : SetupUiState
}

/** Onboarding: valida y guarda la `baseUrl` (FR-002, US-1). */
@HiltViewModel
class InstanceSetupViewModel
    @Inject
    constructor(
        private val validator: InstanceValidator,
        private val prefs: InstancePrefs,
        private val db: TolochaDb,
        private val session: SessionManager,
        private val normalize: NormalizeBaseUrlUseCase,
    ) : ViewModel() {
        private val _ui = MutableStateFlow<SetupUiState>(SetupUiState.Idle())
        val ui: StateFlow<SetupUiState> = _ui.asStateFlow()

        fun onUrlChange(url: String) {
            _ui.value = SetupUiState.Idle(url)
        }

        /** Valida `/health`+`/config`; en éxito guarda y marca setup. */
        fun connect(
            rawUrl: String,
            onConnected: () -> Unit,
        ) {
            val url = normalize(rawUrl)
            if (url == null) {
                _ui.value = SetupUiState.Error(rawUrl, "Esa URL no parece válida. Usa https://…")
                return
            }
            _ui.value = SetupUiState.Connecting
            viewModelScope.launch {
                // Se valida contra la URL introducida (no la base del grafo).
                val valid = validator.validate(url)
                if (!valid) {
                    _ui.value = SetupUiState.Error(rawUrl, "No se puede conectar con esa instancia.")
                    return@launch
                }
                prefs.setBaseUrl(url)
                prefs.setSetupDone(true)
                onConnected()
            }
        }

        /** Cambiar de instancia cierra sesión y limpia la caché anterior. */
        fun switchInstance(
            rawUrl: String,
            onSwitched: () -> Unit,
        ) {
            viewModelScope.launch {
                session.logout()
                db.stationsCache().clear()
                connect(rawUrl, onSwitched)
            }
        }
    }
