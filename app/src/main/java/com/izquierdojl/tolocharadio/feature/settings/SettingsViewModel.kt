package com.izquierdojl.tolocharadio.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.ui.navigation.StartScreen
import com.izquierdojl.tolocharadio.core.ui.theme.ThemeMode
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI de Configuración (sustituye a Perfil, FR-011). */
data class SettingsUi(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val startScreen: StartScreen = StartScreen.EXPLORE,
    val message: String? = null,
)

/** Información general de la aplicación para el diálogo "Acerca de". */
data class AppInfo(
    val appName: String = "Tolocha Radio",
    val version: String = "1.0",
    val repositoryUrl: String = "https://github.com/izquierdojl/tolocharadio-android",
    val developer: String = "izquierdojl",
    val license: String = "MIT",
)

/** Estado de la UI del diálogo de información de la aplicación. */
sealed interface AppInfoUiState {
    data object Hidden : AppInfoUiState

    data class Showing(val info: AppInfo = AppInfo()) : AppInfoUiState
}

/** Configuración: tema claro/oscuro y pantalla de arranque (todo local, sin sesión). */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val prefs: InstancePrefs,
    ) : ViewModel() {
        private val _ui = MutableStateFlow(SettingsUi())
        val ui: StateFlow<SettingsUi> = _ui.asStateFlow()

        private val _appInfoUiState = MutableStateFlow<AppInfoUiState>(AppInfoUiState.Hidden)
        val appInfoUiState: StateFlow<AppInfoUiState> = _appInfoUiState.asStateFlow()

        init {
            viewModelScope.launch {
                prefs.themeMode.collect { mode ->
                    _ui.value = _ui.value.copy(themeMode = mode)
                }
            }
            viewModelScope.launch {
                prefs.startScreen.collect { screen ->
                    _ui.value = _ui.value.copy(startScreen = screen)
                }
            }
        }

        /** Selector Sistema/Claro/Oscuro (spec 002, FR-010). SYSTEM es solo local. */
        fun onThemeModeChange(mode: ThemeMode) {
            _ui.value = _ui.value.copy(themeMode = mode)
            viewModelScope.launch { prefs.setThemeMode(mode) }
        }

        /** Pantalla de arranque con servidor configurado (FR-005). */
        fun onStartScreenChange(screen: StartScreen) {
            _ui.value = _ui.value.copy(startScreen = screen)
            viewModelScope.launch { prefs.setStartScreen(screen) }
        }

        /** Muestra el diálogo de información de la aplicación. */
        fun showAppInfoDialog() {
            _appInfoUiState.value = AppInfoUiState.Showing()
        }

        /** Cierra el diálogo de información de la aplicación. */
        fun dismissAppInfoDialog() {
            _appInfoUiState.value = AppInfoUiState.Hidden
        }
    }
