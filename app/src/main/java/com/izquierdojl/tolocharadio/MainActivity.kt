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
import com.tolocharadio.ui.notification.NotificationNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
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
    lateinit var pendingShortcutHolder: PendingShortcutHolder

    private val notificationNavigation = NotificationNavigation()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // Migra baseUrl existente a SavedServer y limpia el almacén legacy.
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
            val hasServers by serverRepository.servers
                .map { it.isNotEmpty() }
                .collectAsState(initial = false)
            TolochaTheme(darkTheme = resolveDarkTheme(mode, isSystemInDarkTheme())) {
                TolochaNavGraph(
                    hasServers = hasServers,
                    startScreen = startScreen,
                )
            }
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
