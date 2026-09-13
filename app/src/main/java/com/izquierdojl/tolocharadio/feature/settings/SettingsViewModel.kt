package com.izquierdojl.tolocharadio.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.izquierdojl.tolocharadio.core.ui.navigation.StartScreen
import com.izquierdojl.tolocharadio.core.ui.theme.ThemeMode
import com.izquierdojl.tolocharadio.core.util.AppBuildInfo
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.domain.servers.GetServersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI de Configuración (sustituye a Perfil, FR-011). */
data class SettingsUi(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val startScreen: StartScreen = StartScreen.EXPLORE,
    val activeServerAlias: String? = null,
    val message: String? = null,
)

/** Información general de la aplicación para el diálogo "Acerca de". */
data class AppInfo(
    val appName: String,
    val version: String,
    val versionCode: Long,
    val applicationId: String,
    val buildType: String,
    val developer: String,
    val license: String,
    val repositoryUrl: String,
    val theme: String,
    val startScreen: String,
    val activeServerAlias: String? = null,
)

/** Estado de la UI del diálogo de información de la aplicación. */
sealed interface AppInfoUiState {
    data object Hidden : AppInfoUiState

    data class Showing(val info: AppInfo) : AppInfoUiState
}

/** Configuración: tema claro/oscuro y pantalla de arranque (todo local, sin sesión). */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val prefs: InstancePrefs,
        private val getServers: GetServersUseCase,
        private val buildInfo: AppBuildInfo,
    ) : ViewModel() {
        private val _ui = MutableStateFlow(SettingsUi())
        val ui: StateFlow<SettingsUi> = _ui.asStateFlow()

        private val _appInfoUiState = MutableStateFlow<AppInfoUiState>(AppInfoUiState.Hidden)
        val appInfoUiState: StateFlow<AppInfoUiState> = _appInfoUiState.asStateFlow()

        private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val messages: SharedFlow<String> = _messages.asSharedFlow()

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
            viewModelScope.launch {
                getServers().collect { servers ->
                    _ui.value =
                        _ui.value.copy(activeServerAlias = servers.firstOrNull { it.isActive }?.alias)
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

        /** Muestra el diálogo con la versión real del build y la configuración vigente (FR-001/FR-002). */
        fun showAppInfoDialog() {
            _appInfoUiState.value = AppInfoUiState.Showing(currentAppInfo())
        }

        /** Cierra el diálogo de información de la aplicación. */
        fun dismissAppInfoDialog() {
            _appInfoUiState.value = AppInfoUiState.Hidden
        }

        /** Resultado del copiado; solo emite un aviso, nunca cierra el diálogo (FR-006). */
        fun onCopyResult(success: Boolean) {
            _messages.tryEmit(
                if (success) "Información copiada" else "No se pudo copiar la información",
            )
        }

        private fun currentAppInfo(): AppInfo =
            AppInfo(
                appName = buildInfo.appName,
                version = buildInfo.versionName,
                versionCode = buildInfo.versionCode,
                applicationId = buildInfo.applicationId,
                buildType = buildInfo.buildType,
                developer = buildInfo.developer,
                license = buildInfo.license,
                repositoryUrl = buildInfo.repositoryUrl,
                theme = _ui.value.themeMode.label(),
                startScreen = _ui.value.startScreen.label(),
                activeServerAlias = _ui.value.activeServerAlias,
            )
    }
