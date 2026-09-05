package com.example.tolocharadio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.tolocharadio.core.session.SessionManager
import com.example.tolocharadio.core.ui.navigation.TolochaNavGraph
import com.example.tolocharadio.core.ui.theme.ThemeMode
import com.example.tolocharadio.core.ui.theme.TolochaTheme
import com.example.tolocharadio.core.ui.theme.resolveDarkTheme
import com.example.tolocharadio.data.local.InstancePrefs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Única Activity. Toda la UI es Compose (constitución II). */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var instancePrefs: InstancePrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val hasInstance by instancePrefs.hasInstance.collectAsState(initial = false)
            val mode by instancePrefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            TolochaTheme(darkTheme = resolveDarkTheme(mode, isSystemInDarkTheme())) {
                TolochaNavGraph(sessionManager = sessionManager, hasInstance = hasInstance)
            }
        }
    }
}
