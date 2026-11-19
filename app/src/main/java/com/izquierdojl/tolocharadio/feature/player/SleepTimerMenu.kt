package com.izquierdojl.tolocharadio.feature.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.izquierdojl.tolocharadio.R
import com.izquierdojl.tolocharadio.domain.SleepTimerDuration

/**
 * Contenido del menú de duraciones/cancelación del temporizador
 * (spec 014), reutilizado por el cajón de la cabecera (spec 0047).
 */
@Composable
fun SleepTimerMenuContent(
    uiState: SleepTimerUiState,
    onStart: (SleepTimerDuration) -> Unit,
    onCancel: () -> Unit,
) {
    if (uiState is SleepTimerUiState.Active) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.sleep_timer_remaining_minutes, uiState.remainingMinutes),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            onClick = { },
            enabled = false,
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.sleep_timer_cancel),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            },
            onClick = onCancel,
        )
    } else {
        SleepTimerDuration.entries.forEach { duration ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.sleep_timer_minutes, duration.minutes),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                onClick = { onStart(duration) },
            )
        }
    }
}

/**
 * Fila del temporizador dentro del cajón: muestra el estado activo y
 * abre las duraciones o la cancelación.
 */
@Composable
fun SleepTimerDrawerItem(
    uiState: SleepTimerUiState,
    onStart: (SleepTimerDuration) -> Unit,
    onCancel: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.menu_timer)) },
            icon = {
                Icon(
                    Icons.Filled.Timer,
                    contentDescription = stringResource(R.string.sleep_timer_inactive_description),
                )
            },
            badge = {
                if (uiState is SleepTimerUiState.Active) {
                    Text(stringResource(R.string.sleep_timer_badge_minutes, uiState.remainingMinutes))
                }
            },
            selected = false,
            onClick = { expanded = true },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SleepTimerMenuContent(
                uiState = uiState,
                onStart = {
                    expanded = false
                    onStart(it)
                },
                onCancel = {
                    expanded = false
                    onCancel()
                },
            )
        }
    }
}
