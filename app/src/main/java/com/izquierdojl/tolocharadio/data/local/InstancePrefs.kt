package com.izquierdojl.tolocharadio.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.izquierdojl.tolocharadio.BuildConfig
import com.izquierdojl.tolocharadio.core.ui.ViewMode
import com.izquierdojl.tolocharadio.core.ui.navigation.ContentMode
import com.izquierdojl.tolocharadio.core.ui.navigation.PodcastStartScreen
import com.izquierdojl.tolocharadio.core.ui.navigation.StartScreen
import com.izquierdojl.tolocharadio.core.ui.theme.ThemeMode
import com.izquierdojl.tolocharadio.domain.playback.BufferProfile
import com.izquierdojl.tolocharadio.domain.podcasts.DownloadRetention
import com.izquierdojl.tolocharadio.domain.podcasts.TopCountry
import com.izquierdojl.tolocharadio.domain.podcasts.TopGenre
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferencias de instancia y tema. La `baseUrl` es editable
 * (onboarding + ajustes); cambiarla cierra la sesión (FR-002).
 */
@Singleton
@Suppress("TooManyFunctions")
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

        /** Pantalla de arranque de podcast (spec 0048, US5). Default HOME. */
        val podcastStartScreen: Flow<PodcastStartScreen> =
            store.data.map {
                it[KEY_PODCAST_START_SCREEN]?.let { name ->
                    runCatching { PodcastStartScreen.valueOf(name) }.getOrDefault(PodcastStartScreen.HOME)
                } ?: PodcastStartScreen.HOME
            }

        /** Modo global Radio/Podcasts (spec 0046, FR-001). Default RADIO. */
        val contentMode: Flow<ContentMode> =
            store.data.map {
                it[KEY_CONTENT_MODE]?.let { name ->
                    runCatching { ContentMode.valueOf(name) }.getOrDefault(ContentMode.RADIO)
                } ?: ContentMode.RADIO
            }

        /** Sección inicial Radio/Podcasts del arranque (spec 0046, FR-003). Default RADIO. */
        val defaultSection: Flow<ContentMode> =
            store.data.map {
                it[KEY_DEFAULT_SECTION]?.let { name ->
                    runCatching { ContentMode.valueOf(name) }.getOrDefault(ContentMode.RADIO)
                } ?: ContentMode.RADIO
            }

        /** Orden manual de suscripciones de podcast (ids separados por coma). */
        val podcastSubscriptionOrder: Flow<List<Long>> =
            store.data.map { prefs ->
                prefs[KEY_PODCAST_SUBSCRIPTION_ORDER]
                    ?.split(',')
                    ?.mapNotNull { it.trim().toLongOrNull() }
                    ?: emptyList()
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

        /** Potenciador de voz de episodios (spec 0047, US4). Default false. */
        val voiceBoost: Flow<Boolean> =
            store.data.map { it[KEY_VOICE_BOOST] ?: false }

        /** Días de retención de descargas; 0 = nunca (extensión spec 0047). */
        val downloadRetentionDays: Flow<Int> =
            store.data.map { it[KEY_DOWNLOAD_RETENTION_DAYS] ?: DownloadRetention.DEFAULT_DAYS }

        /** País del ranking de podcasts; valor desconocido cae a España. */
        val topChartsCountry: Flow<TopCountry> =
            store.data.map { TopCountry.fromId(it[KEY_TOP_CHARTS_COUNTRY]) }

        /** Género del ranking de podcasts; valor desconocido cae a «Todas». */
        val topChartsGenre: Flow<TopGenre> =
            store.data.map { TopGenre.fromId(it[KEY_TOP_CHARTS_GENRE]) }

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

        /** Persiste la pantalla de arranque de podcast (spec 0048, US5). */
        suspend fun setPodcastStartScreen(screen: PodcastStartScreen) {
            store.edit { it[KEY_PODCAST_START_SCREEN] = screen.name }
        }

        /** Persiste el modo global Radio/Podcasts (spec 0046, FR-002). */
        suspend fun setContentMode(mode: ContentMode) {
            store.edit { it[KEY_CONTENT_MODE] = mode.name }
        }

        /** Persiste la sección inicial del arranque (spec 0046, FR-003). */
        suspend fun setDefaultSection(mode: ContentMode) {
            store.edit { it[KEY_DEFAULT_SECTION] = mode.name }
        }

        /** Persiste el orden manual de suscripciones (solo en este dispositivo). */
        suspend fun setPodcastSubscriptionOrder(ids: List<Long>) {
            store.edit { it[KEY_PODCAST_SUBSCRIPTION_ORDER] = ids.joinToString(",") }
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

        /** Persiste el potenciador de voz de episodios (spec 0047, US4). */
        suspend fun setVoiceBoost(enabled: Boolean) {
            store.edit { it[KEY_VOICE_BOOST] = enabled }
        }

        /** Persiste los días de retención de descargas (0 = nunca). */
        suspend fun setDownloadRetentionDays(days: Int) {
            store.edit { it[KEY_DOWNLOAD_RETENTION_DAYS] = days.coerceAtLeast(DownloadRetention.NEVER) }
        }

        /** Persiste el país del ranking de podcasts. */
        suspend fun setTopChartsCountry(country: TopCountry) {
            store.edit { it[KEY_TOP_CHARTS_COUNTRY] = country.id }
        }

        /** Persiste el género del ranking (`ALL` como cadena vacía). */
        suspend fun setTopChartsGenre(genre: TopGenre) {
            store.edit { it[KEY_TOP_CHARTS_GENRE] = genre.id ?: "" }
        }

        private companion object {
            val KEY_BASE_URL = stringPreferencesKey("base_url")
            val KEY_SETUP_DONE = booleanPreferencesKey("setup_done")
            val KEY_DARK = booleanPreferencesKey("dark_theme")
            val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
            val KEY_START_SCREEN = stringPreferencesKey("start_screen")
            val KEY_PODCAST_START_SCREEN = stringPreferencesKey("podcast_start_screen")
            val KEY_CONTENT_MODE = stringPreferencesKey("content_mode")
            val KEY_DEFAULT_SECTION = stringPreferencesKey("default_section")
            val KEY_PODCAST_SUBSCRIPTION_ORDER = stringPreferencesKey("podcast_subscription_order")
            val KEY_VIEW_MODE = stringPreferencesKey("view_mode")
            val KEY_BUFFER_PROFILE = stringPreferencesKey("buffer_profile")
            val KEY_RESUME_ON_CONNECTIVITY = booleanPreferencesKey("resume_on_connectivity")
            val KEY_VOICE_BOOST = booleanPreferencesKey("voice_boost")
            val KEY_DOWNLOAD_RETENTION_DAYS = intPreferencesKey("download_retention_days")
            val KEY_TOP_CHARTS_COUNTRY = stringPreferencesKey("top_charts_country")
            val KEY_TOP_CHARTS_GENRE = stringPreferencesKey("top_charts_genre")
        }
    }
