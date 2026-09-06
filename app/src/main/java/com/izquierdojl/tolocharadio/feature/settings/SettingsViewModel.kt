package com.izquierdojl.tolocharadio.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.ui.navigation.StartScreen
import com.izquierdojl.tolocharadio.core.ui.theme.ThemeMode
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.ThemeDto
import com.izquierdojl.tolocharadio.data.repo.UserRepo
import com.izquierdojl.tolocharadio.domain.auth.LogoutUseCase
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

/** Configuración: tema claro/oscuro, pantalla de arranque y logout. */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val prefs: InstancePrefs,
        private val users: UserRepo,
        private val logoutUseCase: LogoutUseCase,
    ) : ViewModel() {
        private val _ui = MutableStateFlow(SettingsUi())
        val ui: StateFlow<SettingsUi> = _ui.asStateFlow()

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
            viewModelScope.launch {
                prefs.setThemeMode(mode)
                if (mode != ThemeMode.SYSTEM) {
                    users.patchMe(null, if (mode == ThemeMode.DARK) ThemeDto.DARK else ThemeDto.LIGHT)
                }
            }
        }

        /** Pantalla de arranque con sesión restaurada (FR-011b). */
        fun onStartScreenChange(screen: StartScreen) {
            _ui.value = _ui.value.copy(startScreen = screen)
            viewModelScope.launch { prefs.setStartScreen(screen) }
        }

        /** Cierra sesión via [LogoutUseCase]. */
        fun logout(onDone: () -> Unit) {
            viewModelScope.launch {
                logoutUseCase()
                onDone()
            }
        }
    }
