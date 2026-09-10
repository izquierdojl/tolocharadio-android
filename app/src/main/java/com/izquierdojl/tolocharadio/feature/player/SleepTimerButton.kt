package com.izquierdojl.tolocharadio.feature.player

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
 * Botón de temporizador de apagado para la barra superior.
 *
 * Muestra un icono de reloj que al pulsar despliega un menú con opciones de duración.
 * Cuando el temporizador está activo, muestra un badge con el tiempo restante.
 *
 * @param uiState Estado actual del temporizador
 * @param onStart Callback al seleccionar una duración
 * @param onCancel Callback al cancelar el temporizador activo
 */
@Composable
fun SleepTimerButton(
    uiState: SleepTimerUiState,
    onStart: (SleepTimerDuration) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        BadgedBox(
            badge = {
                if (uiState is SleepTimerUiState.Active) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                    ) {
                        Text(
                            text = stringResource(R.string.sleep_timer_badge_minutes, uiState.remainingMinutes),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            },
        ) {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector =
                        if (uiState is SleepTimerUiState.Active) {
                            Icons.Filled.Timer
                        } else {
                            Icons.Outlined.Timer
                        },
                    contentDescription =
                        if (uiState is SleepTimerUiState.Active) {
                            stringResource(
                                R.string.sleep_timer_active_description,
                                uiState.remainingMinutes,
                            )
                        } else {
                            stringResource(R.string.sleep_timer_inactive_description)
                        },
                    tint =
                        if (uiState is SleepTimerUiState.Active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            if (uiState is SleepTimerUiState.Active) {
                // Estado activo: mostrar tiempo restante + cancelar
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
                    onClick = {
                        menuExpanded = false
                        onCancel()
                    },
                )
            } else {
                // Estado inactivo: mostrar opciones de duración
                SleepTimerDuration.entries.forEach { duration ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.sleep_timer_minutes, duration.minutes),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onStart(duration)
                        },
                    )
                }
            }
        }
    }
}
