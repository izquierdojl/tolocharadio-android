package com.example.tolocharadio.feature.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.core.network.userMessage
import com.example.tolocharadio.core.ui.theme.ThemeMode
import com.example.tolocharadio.data.local.InstancePrefs
import com.example.tolocharadio.data.remote.dto.ThemeDto
import com.example.tolocharadio.data.remote.dto.UserDto
import com.example.tolocharadio.data.repo.AuthRepo
import com.example.tolocharadio.data.repo.UserRepo
import com.jakewharton.processphoenix.ProcessPhoenix
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI del perfil. */
data class ProfileUi(
    val user: UserDto? = null,
    val name: String = "",
    val darkTheme: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val message: String? = null,
    val loading: Boolean = true,
)

/** Perfil mínimo: ver/editar nombre+tema, password, logout y baseUrl (US-6). */
@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val users: UserRepo,
        private val auth: AuthRepo,
        private val prefs: InstancePrefs,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _ui = MutableStateFlow(ProfileUi())
        val ui: StateFlow<ProfileUi> = _ui.asStateFlow()

        init {
            refresh()
            viewModelScope.launch {
                prefs.themeMode.collect { mode ->
                    _ui.value = _ui.value.copy(themeMode = mode, darkTheme = mode != ThemeMode.LIGHT)
                }
            }
        }

        /** Recarga `GET /users/me`. */
        fun refresh() {
            viewModelScope.launch {
                when (val r = users.me()) {
                    is ApiResult.Ok ->
                        _ui.value =
                            ProfileUi(
                                user = r.value,
                                name = r.value.name.orEmpty(),
                                darkTheme = r.value.theme == ThemeDto.DARK,
                                loading = false,
                            )
                    is ApiResult.Err ->
                        _ui.value =
                            ProfileUi(
                                message = r.error.userMessage(),
                                loading = false,
                            )
                }
            }
        }

        fun onNameChange(v: String) {
            _ui.value = _ui.value.copy(name = v)
        }

        fun onThemeChange(dark: Boolean) {
            onThemeModeChange(if (dark) ThemeMode.DARK else ThemeMode.LIGHT)
        }

        /** Selector Sistema/Claro/Oscuro (spec 002, FR-010). SYSTEM es solo local. */
        fun onThemeModeChange(mode: ThemeMode) {
            _ui.value = _ui.value.copy(themeMode = mode, darkTheme = mode != ThemeMode.LIGHT)
            viewModelScope.launch {
                prefs.setThemeMode(mode)
                if (mode != ThemeMode.SYSTEM) {
                    users.patchMe(null, if (mode == ThemeMode.DARK) ThemeDto.DARK else ThemeDto.LIGHT)
                }
            }
        }

        /** Guarda el nombre vía `PATCH /users/me`. */
        fun saveName() {
            viewModelScope.launch {
                when (val r = users.patchMe(_ui.value.name.ifBlank { null }, null)) {
                    is ApiResult.Ok -> _ui.value = _ui.value.copy(user = r.value, message = "Perfil actualizado.")
                    is ApiResult.Err -> _ui.value = _ui.value.copy(message = r.error.userMessage())
                }
            }
        }

        /** Cambia la contraseña y fuerza re-login (revocación). */
        fun changePassword(
            current: String,
            new: String,
            onDone: () -> Unit,
        ) {
            viewModelScope.launch {
                when (val r = users.patchPassword(current, new)) {
                    is ApiResult.Ok -> {
                        auth.logout()
                        onDone()
                    }
                    is ApiResult.Err -> _ui.value = _ui.value.copy(message = r.error.userMessage())
                }
            }
        }

        /** Cierra sesión. */
        fun logout(onDone: () -> Unit) {
            viewModelScope.launch {
                auth.logout()
                onDone()
            }
        }

        /** Cambia de instancia: limpia sesión y renace contra la nueva base. */
        fun switchInstance(url: String) {
            viewModelScope.launch {
                auth.logout()
                prefs.setBaseUrl(url)
                prefs.setSetupDone(true)
                ProcessPhoenix.triggerRebirth(context)
            }
        }
    }
