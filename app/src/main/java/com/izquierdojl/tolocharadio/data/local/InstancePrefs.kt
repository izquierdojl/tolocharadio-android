package com.izquierdojl.tolocharadio.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.izquierdojl.tolocharadio.BuildConfig
import com.izquierdojl.tolocharadio.core.ui.ViewMode
import com.izquierdojl.tolocharadio.core.ui.navigation.StartScreen
import com.izquierdojl.tolocharadio.core.ui.theme.ThemeMode
import com.izquierdojl.tolocharadio.domain.playback.BufferProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferencias de instancia y tema. La `baseUrl` es editable
 * (onboarding + ajustes); cambiarla cierra la sesión (FR-002).
 */
@Singleton
class InstancePrefs
    @Inject
    constructor(private val store: DataStore<Preferences>) {
        val baseUrl: Flow<String> = store.data.map { it[KEY_BASE_URL] ?: BuildConfig.TOLOCHA_BASE_URL }
        val hasInstance: Flow<Boolean> = store.data.map { it[KEY_SETUP_DONE] ?: false }
        val darkTheme: Flow<Boolean> = store.data.map { it[KEY_DARK] ?: true }

        /** Selector Sistema/Claro/Oscuro (spec 002, FR-010). Default SYSTEM. */
        val themeMode: Flow<ThemeMode> =
            store.data.map {
                it[KEY_THEME_MODE]?.let { name ->
                    runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
                } ?: ThemeMode.SYSTEM
            }

        /** Pantalla de arranque con sesión (FR-011b). Default EXPLORE. */
        val startScreen: Flow<StartScreen> =
            store.data.map {
                it[KEY_START_SCREEN]?.let { name ->
                    runCatching { StartScreen.valueOf(name) }.getOrDefault(StartScreen.EXPLORE)
                } ?: StartScreen.EXPLORE
            }

        /**
         * Modo de vista global lista/tarjetas (spec 008). Default LIST;
         * un valor corrupto o ilegible cae a LIST sin error (FR-010).
         */
        val viewMode: Flow<ViewMode> =
            store.data.map {
                it[KEY_VIEW_MODE]?.let { name ->
                    runCatching { ViewMode.valueOf(name) }.getOrDefault(ViewMode.LIST)
                } ?: ViewMode.LIST
            }

        /** Perfil de buffer local (spec 0042, FR-010/FR-012). Default BALANCED. */
        val bufferProfile: Flow<BufferProfile> =
            store.data.map { BufferProfile.fromPersisted(it[KEY_BUFFER_PROFILE]) }

        /** Reanudar al recuperar conexión (spec 0042, FR-023). Default true. */
        val resumeOnConnectivity: Flow<Boolean> =
            store.data.map { it[KEY_RESUME_ON_CONNECTIVITY] ?: true }

        suspend fun setBaseUrl(url: String) {
            store.edit { it[KEY_BASE_URL] = url }
        }

        suspend fun setSetupDone(done: Boolean) {
            store.edit { it[KEY_SETUP_DONE] = done }
        }

        suspend fun setDarkTheme(dark: Boolean) {
            store.edit { it[KEY_DARK] = dark }
        }

        suspend fun setThemeMode(mode: ThemeMode) {
            store.edit {
                it[KEY_THEME_MODE] = mode.name
                it[KEY_DARK] = mode != ThemeMode.LIGHT
            }
        }

        suspend fun setStartScreen(screen: StartScreen) {
            store.edit { it[KEY_START_SCREEN] = screen.name }
        }

        /** Persiste el modo de vista; cada toggle sobrescribe el valor previo (FR-006). */
        suspend fun setViewMode(mode: ViewMode) {
            store.edit { it[KEY_VIEW_MODE] = mode.name }
        }

        /** Persiste el perfil de buffer local (spec 0042, FR-012). */
        suspend fun setBufferProfile(profile: BufferProfile) {
            store.edit { it[KEY_BUFFER_PROFILE] = profile.name }
        }

        /** Persiste la preferencia de reanudación al recuperar conexión (FR-023). */
        suspend fun setResumeOnConnectivity(enabled: Boolean) {
            store.edit { it[KEY_RESUME_ON_CONNECTIVITY] = enabled }
        }

        private companion object {
            val KEY_BASE_URL = stringPreferencesKey("base_url")
            val KEY_SETUP_DONE = booleanPreferencesKey("setup_done")
            val KEY_DARK = booleanPreferencesKey("dark_theme")
            val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
            val KEY_START_SCREEN = stringPreferencesKey("start_screen")
            val KEY_VIEW_MODE = stringPreferencesKey("view_mode")
            val KEY_BUFFER_PROFILE = stringPreferencesKey("buffer_profile")
            val KEY_RESUME_ON_CONNECTIVITY = booleanPreferencesKey("resume_on_connectivity")
        }
    }
