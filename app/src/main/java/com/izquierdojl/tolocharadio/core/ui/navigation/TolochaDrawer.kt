package com.izquierdojl.tolocharadio.core.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.R
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.domain.SleepTimerDuration
import com.izquierdojl.tolocharadio.feature.player.SleepTimerDrawerItem
import com.izquierdojl.tolocharadio.feature.player.SleepTimerUiState

/**
 * Cajón de acciones secundarias de la cabecera (spec 0047, US3):
 * Temporizador, Servidores y Configuración, sin ocupar la barra inferior.
 */
@Composable
internal fun TolochaDrawerContent(
    sleepTimerUiState: SleepTimerUiState,
    onStartTimer: (SleepTimerDuration) -> Unit,
    onCancelTimer: () -> Unit,
    onServers: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(modifier = modifier.testTag(TolochaTestTags.DRAWER)) {
        Spacer(Modifier.height(12.dp))
        SleepTimerDrawerItem(
            uiState = sleepTimerUiState,
            onStart = onStartTimer,
            onCancel = onCancelTimer,
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.menu_servers)) },
            icon = { Icon(Icons.Filled.Dns, contentDescription = null) },
            selected = false,
            onClick = onServers,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.menu_settings)) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            selected = false,
            onClick = onSettings,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
    }
}
