package com.izquierdojl.tolocharadio.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.izquierdojl.tolocharadio.core.ui.components.AppInfoDialog
import com.izquierdojl.tolocharadio.core.ui.components.SectionHeader
import com.izquierdojl.tolocharadio.core.ui.navigation.StartScreen
import com.izquierdojl.tolocharadio.core.ui.theme.ThemeMode
import com.izquierdojl.tolocharadio.domain.playback.BufferProfile

/**
 * Configuración (sustituye a Perfil, FR-005): tema claro/oscuro y
 * pantalla de arranque. Los servidores son una sección propia de
 * primer nivel (FR-006); no hay cierre de sesión (FR-008).
 */
@Composable
fun SettingsScreen(
    onStats: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val appInfoUiState by viewModel.appInfoUiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Column(Modifier.fillMaxSize()) {
        SectionHeader(title = "Configuración")
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("Tema", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = ui.themeMode == mode,
                        onClick = { viewModel.onThemeModeChange(mode) },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ThemeMode.entries.size,
                            ),
                        label = {
                            Text(
                                when (mode) {
                                    ThemeMode.SYSTEM -> "Sistema"
                                    ThemeMode.DARK -> "Oscuro"
                                    ThemeMode.LIGHT -> "Claro"
                                },
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Pantalla de arranque", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                StartScreen.entries.forEachIndexed { index, screen ->
                    SegmentedButton(
                        selected = ui.startScreen == screen,
                        onClick = { viewModel.onStartScreenChange(screen) },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = StartScreen.entries.size,
                            ),
                        label = {
                            Text(
                                when (screen) {
                                    StartScreen.FAVORITES -> "Favoritos"
                                    StartScreen.HISTORY -> "Historial"
                                    StartScreen.EXPLORE -> "Explorar"
                                },
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Se abrirá automáticamente al arrancar la app con un servidor configurado.",
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(16.dp))
            PlaybackSettingsSection(
                bufferProfile = ui.bufferProfile,
                resumeOnConnectivity = ui.resumeOnConnectivity,
                onBufferProfileChange = viewModel::onBufferProfileChange,
                onResumeOnConnectivityChange = viewModel::onResumeOnConnectivityChange,
            )

            ui.message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it)
            }

            Spacer(Modifier.height(24.dp))
            SettingsActionRow(
                label = "Gráficas",
                description = "Ver gráficas de escucha",
                onClick = onStats,
            )
            SettingsActionRow(
                label = "Acerca de",
                description = "Acerca de la aplicación",
                onClick = viewModel::showAppInfoDialog,
            )
        }
    }

    (appInfoUiState as? AppInfoUiState.Showing)?.let { showing ->
        AppInfoDialog(
            info = showing.info,
            onDismiss = { viewModel.dismissAppInfoDialog() },
            onCopyResult = viewModel::onCopyResult,
            snackbar = snackbar,
        )
    }
}

/**
 * Sección "Reproducción" sin estado (spec 0042, FR-010/FR-011/FR-023).
 * Separada para poder probarla sin Hilt.
 */
@Composable
internal fun PlaybackSettingsSection(
    bufferProfile: BufferProfile,
    resumeOnConnectivity: Boolean,
    onBufferProfileChange: (BufferProfile) -> Unit,
    onResumeOnConnectivityChange: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text("Reproducción", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text("Buffer de audio", style = MaterialTheme.typography.bodyMedium)
        BufferProfile.entries.forEach { profile ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "${profile.label()}. ${profile.description()}" }
                        .clickable { onBufferProfileChange(profile) }
                        .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = bufferProfile == profile,
                    onClick = { onBufferProfileChange(profile) },
                )
                Column(Modifier.padding(start = 8.dp)) {
                    Text(profile.label(), style = MaterialTheme.typography.bodyLarge)
                    Text(profile.description(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Reanudar al recuperar conexión" }
                    .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Reanudar al recuperar conexión", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Si la emisora estaba sonando, vuelve sola cuando regresa la red.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = resumeOnConnectivity,
                onCheckedChange = onResumeOnConnectivityChange,
                modifier = Modifier.semantics { contentDescription = "Cambiar reanudación automática" },
            )
        }
    }
}

/**
 * Fila de acción de Configuración (patrón de "Acerca de"): etiqueta
 * clicable con `contentDescription` para accesibilidad (FR-001).
 */
@Composable
internal fun SettingsActionRow(
    label: String,
    description: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = description }
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
    )
}
