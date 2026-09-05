package com.example.tolocharadio.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.tolocharadio.BuildConfig
import com.example.tolocharadio.core.ui.theme.ThemeMode
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

        private companion object {
            val KEY_BASE_URL = stringPreferencesKey("base_url")
            val KEY_SETUP_DONE = booleanPreferencesKey("setup_done")
            val KEY_DARK = booleanPreferencesKey("dark_theme")
            val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        }
    }
