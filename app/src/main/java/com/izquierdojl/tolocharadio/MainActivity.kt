package com.izquierdojl.tolocharadio

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.core.session.SessionState
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.core.shortcuts.PendingShortcutHolder
import com.izquierdojl.tolocharadio.core.shortcuts.ShortcutIntents
import com.izquierdojl.tolocharadio.core.ui.navigation.ContentMode
import com.izquierdojl.tolocharadio.core.ui.navigation.PodcastStartScreen
import com.izquierdojl.tolocharadio.core.ui.navigation.StartScreen
import com.izquierdojl.tolocharadio.core.ui.navigation.TolochaNavGraph
import com.izquierdojl.tolocharadio.core.ui.theme.ThemeMode
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.core.ui.theme.resolveDarkTheme
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.local.servers.MigrationHelper
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import com.izquierdojl.tolocharadio.domain.auth.AuthenticateServerUseCase
import com.izquierdojl.tolocharadio.domain.servers.StartupGate
import com.izquierdojl.tolocharadio.feature.player.HeadsetVolumeGestureHandler
import com.tolocharadio.domain.notification.AppState
import com.tolocharadio.ui.notification.NotificationNavigation
import com.tolocharadio.ui.notification.NotificationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Única Activity. Toda la UI es Compose (constitución II). */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var instancePrefs: InstancePrefs

    @Inject
    lateinit var migrationHelper: MigrationHelper

    @Inject
    lateinit var serverRepository: ServerRepository

    @Inject
    lateinit var tokenStore: TokenStore

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var authenticateServer: AuthenticateServerUseCase

    @Inject
    lateinit var pendingShortcutHolder: PendingShortcutHolder

    @Inject
    lateinit var headsetVolumeGestureHandler: HeadsetVolumeGestureHandler

    private val notificationNavigation = NotificationNavigation()

    /** VM de notificaciones a ámbito de Activity (spec 0016, T048). */
    private val notificationVm: NotificationViewModel by lazy {
        ViewModelProvider(this)[NotificationViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // Migra baseUrl existente a SavedServer (sin credenciales).
            migrationHelper.migrateIfNeeded()
        }

        // Handle notification tap intent
        handleNotificationIntent(intent)
        // Handle app shortcut tap intent (spec 0018)
        handleShortcutIntent(intent)

        enableEdgeToEdge()
        setContent {
            val mode by instancePrefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            // `null` hasta la primera emisión: el arranque no debe resolverse
            // con los valores por defecto de las preferencias.
            val startScreen: StartScreen? by instancePrefs.startScreen.collectAsState(initial = null)
            val podcastStartScreen: PodcastStartScreen? by
                instancePrefs.podcastStartScreen.collectAsState(initial = null)
            val defaultSection: ContentMode? by instancePrefs.defaultSection.collectAsState(initial = null)
            // `null` mientras Room emite la primera vez: evita mostrar el
            // formulario de servidor antes de saber si ya hay uno guardado.
            val servers by serverRepository.servers.collectAsState(initial = null)

            TolochaTheme(darkTheme = resolveDarkTheme(mode, isSystemInDarkTheme())) {
                val loadedServers = servers
                val loadedStartScreen = startScreen
                val loadedPodcastStartScreen = podcastStartScreen
                val loadedDefaultSection = defaultSection
                if (loadedServers == null) {
                    Surface(Modifier.fillMaxSize()) {}
                } else if (loadedStartScreen == null ||
                    loadedPodcastStartScreen == null ||
                    loadedDefaultSection == null
                ) {
                    Surface(Modifier.fillMaxSize()) {}
                } else {
                    val startupServer =
                        loadedServers.firstOrNull { it.isActive }
                            ?: loadedServers.firstOrNull { it.isDefault }
                            ?: loadedServers.firstOrNull()
                    val startupHasCredentials = startupServer?.let { tokenStore.hasCredentials(it.id) } == true
                    val gate =
                        remember(loadedServers, startupHasCredentials) {
                            when {
                                loadedServers.isEmpty() -> StartupGate.NoServers
                                !startupHasCredentials -> StartupGate.NeedsCredentials
                                else -> StartupGate.Ready
                            }
                        }

                    // Auto-login silencioso con el servidor de arranque (FR-004).
                    LaunchedEffect(gate) {
                        if (gate == StartupGate.Ready) authenticateServer()
                    }

                    TolochaNavGraph(
                        startupGate = gate,
                        startupServerId = startupServer?.id,
                        startScreen = loadedStartScreen,
                        podcastStartScreen = loadedPodcastStartScreen,
                        defaultSection = loadedDefaultSection,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // El access token vive solo en memoria: al volver del reposo,
        // re-autentica con las credenciales guardadas si no hay sesión.
        if (sessionManager.state.value == SessionState.Idle) {
            lifecycleScope.launch { authenticateServer() }
        }
    }

    /**
     * Intercepta las teclas de volumen para distinguir pulsación corta/larga
     * (spec 0060). Si el handler no captura, el sistema gestiona el volumen.
     */
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        headsetVolumeGestureHandler.handle(
            keyCode = event.keyCode,
            action = event.action,
            repeatCount = event.repeatCount,
            hostResumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
        ) || super.dispatchKeyEvent(event)

    override fun onPause() {
        headsetVolumeGestureHandler.cancel()
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        notificationVm.updateAppState(AppState.FOREGROUND)
    }

    override fun onStop() {
        super.onStop()
        notificationVm.updateAppState(AppState.BACKGROUND)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
        handleShortcutIntent(intent)
    }

    /**
     * Ruta de producción del tap de notificación (spec 0016, FR-001–FR-004).
     * La notificación multimedia de Media3 llega con la acción pero sin extras
     * (US2/AC1): se trata como PLAYBACK para enfocar el reproductor. Con extras
     * completos se delega en el caso de uso (validación, estado de app, router).
     */
    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.action != NotificationNavigation.NOTIFICATION_TAP_ACTION) return
        val data = notificationNavigation.extractNotificationData(intent)
        if (data != null) {
            notificationVm.handleNotificationTap(data)
        } else {
            notificationVm.handlePlaybackNotificationTap()
        }
    }

    /**
     * Publica el acceso directo pendiente para que el grafo de navegación
     * lo consuma (spec 0018, FR-005). No navega desde la Activity.
     */
    private fun handleShortcutIntent(intent: Intent?) {
        val parsed =
            ShortcutIntents.parse(
                action = intent?.action,
                stationId = intent?.getStringExtra(ShortcutIntents.EXTRA_STATION_ID),
                stationName = intent?.getStringExtra(ShortcutIntents.EXTRA_STATION_NAME),
            ) ?: return
        pendingShortcutHolder.set(parsed)
    }
}
