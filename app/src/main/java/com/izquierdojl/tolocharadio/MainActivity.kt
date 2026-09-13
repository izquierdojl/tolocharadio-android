package com.izquierdojl.tolocharadio

import android.content.Intent
import android.os.Bundle
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
import androidx.lifecycle.lifecycleScope
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.core.session.SessionState
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.core.shortcuts.PendingShortcutHolder
import com.izquierdojl.tolocharadio.core.shortcuts.ShortcutIntents
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
import com.tolocharadio.ui.notification.NotificationNavigation
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

    private val notificationNavigation = NotificationNavigation()

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
            val startScreen by instancePrefs.startScreen.collectAsState(initial = StartScreen.EXPLORE)
            // `null` mientras Room emite la primera vez: evita mostrar el
            // formulario de servidor antes de saber si ya hay uno guardado.
            val servers by serverRepository.servers.collectAsState(initial = null)

            TolochaTheme(darkTheme = resolveDarkTheme(mode, isSystemInDarkTheme())) {
                val loadedServers = servers
                if (loadedServers == null) {
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
                        startScreen = startScreen,
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        // Handle custom notification tap action
        if (intent?.action == NotificationNavigation.NOTIFICATION_TAP_ACTION) {
            val notificationData = notificationNavigation.extractNotificationData(intent)
            if (notificationData != null) {
                // Log the notification tap
                android.util.Log.d(
                    "MainActivity",
                    "Notification tapped: ${notificationData.type} - ${notificationData.title}",
                )
                // The notification system will handle the navigation
            }
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
