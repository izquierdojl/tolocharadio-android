package com.izquierdojl.tolocharadio

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.core.session.SessionRestorer
import com.izquierdojl.tolocharadio.core.ui.navigation.StartScreen
import com.izquierdojl.tolocharadio.core.ui.navigation.TolochaNavGraph
import com.izquierdojl.tolocharadio.core.ui.theme.ThemeMode
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.core.ui.theme.resolveDarkTheme
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.local.servers.MigrationHelper
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import com.tolocharadio.ui.notification.NotificationNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Única Activity. Toda la UI es Compose (constitución II). */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var instancePrefs: InstancePrefs

    @Inject
    lateinit var sessionRestorer: SessionRestorer

    @Inject
    lateinit var migrationHelper: MigrationHelper

    @Inject
    lateinit var serverRepository: ServerRepository

    private val notificationNavigation = NotificationNavigation()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // Migrar baseUrl existente a SavedServer si es necesario
            migrationHelper.migrateIfNeeded()
            // Restaurar sesión antes de setContent para que la UI
            // muestre el estado correcto desde el primer frame.
            sessionRestorer.restore()
        }

        // Handle notification tap intent
        handleNotificationIntent(intent)

        enableEdgeToEdge()
        setContent {
            val hasInstance by instancePrefs.hasInstance.collectAsState(initial = false)
            val mode by instancePrefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val startScreen by instancePrefs.startScreen.collectAsState(initial = StartScreen.EXPLORE)
            val hasServers by serverRepository.servers
                .map { it.isNotEmpty() }
                .collectAsState(initial = false)
            TolochaTheme(darkTheme = resolveDarkTheme(mode, isSystemInDarkTheme())) {
                TolochaNavGraph(
                    sessionManager = sessionManager,
                    hasInstance = hasInstance,
                    hasServers = hasServers,
                    startScreen = startScreen,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        // Handle custom notification tap action
        if (intent?.action == NotificationNavigation.NOTIFICATION_TAP_ACTION) {
            val notificationData = notificationNavigation.extractNotificationData(intent)
            if (notificationData != null) {
                // Log the notification tap
                android.util.Log.d("MainActivity", "Notification tapped: ${notificationData.type} - ${notificationData.title}")
                // The notification system will handle the navigation
            }
        }
    }
}
