package com.izquierdojl.tolocharadio.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.izquierdojl.tolocharadio.core.ui.components.AppInfoDialog
import com.izquierdojl.tolocharadio.core.ui.components.SectionHeader
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.core.ui.navigation.ContentMode
import com.izquierdojl.tolocharadio.core.ui.navigation.PodcastStartScreen
import com.izquierdojl.tolocharadio.core.ui.navigation.StartScreen
import com.izquierdojl.tolocharadio.core.ui.theme.ThemeMode
import com.izquierdojl.tolocharadio.domain.playback.BufferProfile
import com.izquierdojl.tolocharadio.domain.playback.HeadsetControlSettings
import com.izquierdojl.tolocharadio.domain.podcasts.DownloadRetention
import java.util.Locale

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
        SettingsContent(
            ui = ui,
            actions =
                SettingsActions(
                    onThemeModeChange = viewModel::onThemeModeChange,
                    onStartScreenChange = viewModel::onStartScreenChange,
                    onPodcastStartScreenChange = viewModel::onPodcastStartScreenChange,
                    onDefaultSectionChange = viewModel::onDefaultSectionChange,
                    onBufferProfileChange = viewModel::onBufferProfileChange,
                    onResumeOnConnectivityChange = viewModel::onResumeOnConnectivityChange,
                    onHeadsetSkipBackSecondsChange = viewModel::onHeadsetSkipBackSecondsChange,
                    onHeadsetSkipForwardSecondsChange = viewModel::onHeadsetSkipForwardSecondsChange,
                    onHeadsetSkipGesturesEnabledChange = viewModel::onHeadsetSkipGesturesEnabledChange,
                    onDownloadRetentionChange = viewModel::onDownloadRetentionChange,
                    onClearEpisodeCache = viewModel::onClearCache,
                    onStats = onStats,
                    onAbout = viewModel::showAppInfoDialog,
                ),
            modifier = Modifier.weight(1f),
        )
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

/** Callbacks de Configuración, agrupados para poder testear el contenido sin Hilt. */
internal data class SettingsActions(
    val onThemeModeChange: (ThemeMode) -> Unit = {},
    val onStartScreenChange: (StartScreen) -> Unit = {},
    val onPodcastStartScreenChange: (PodcastStartScreen) -> Unit = {},
    val onDefaultSectionChange: (ContentMode) -> Unit = {},
    val onBufferProfileChange: (BufferProfile) -> Unit = {},
    val onResumeOnConnectivityChange: (Boolean) -> Unit = {},
    val onHeadsetSkipBackSecondsChange: (Int) -> Unit = {},
    val onHeadsetSkipForwardSecondsChange: (Int) -> Unit = {},
    val onHeadsetSkipGesturesEnabledChange: (Boolean) -> Unit = {},
    val onDownloadRetentionChange: (Int) -> Unit = {},
    val onClearEpisodeCache: () -> Unit = {},
    val onStats: () -> Unit = {},
    val onAbout: () -> Unit = {},
)

/**
 * Contenido desplazable de Configuración, sin Hilt (patrón de test sin
 * ViewModel). El [SectionHeader] queda fijo fuera; el resto se desplaza para
 * que "Gráficas" y "Acerca de" sigan siendo accesibles aunque la sección de
 * reproducción ocupe más alto del disponible.
 */
@Composable
internal fun SettingsContent(
    ui: SettingsUi,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
    ) {
        Text("Tema", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = ui.themeMode == mode,
                    onClick = { actions.onThemeModeChange(mode) },
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
        Text("Pantalla de arranque en radio", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            StartScreen.entries.forEachIndexed { index, screen ->
                SegmentedButton(
                    selected = ui.startScreen == screen,
                    onClick = { actions.onStartScreenChange(screen) },
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
        Text("Pantalla de arranque en podcast", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        PodcastStartSettings(
            current = ui.podcastStartScreen,
            onChange = actions.onPodcastStartScreenChange,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Se aplica al arrancar o al cambiar a Podcasts con un servidor que ofrece podcasts.",
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(16.dp))
        Text("Sección inicial", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ContentMode.entries.forEachIndexed { index, section ->
                SegmentedButton(
                    selected = ui.defaultSection == section,
                    onClick = { actions.onDefaultSectionChange(section) },
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ContentMode.entries.size,
                        ),
                    label = { Text(section.label()) },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Modo con el que se abre la app: Radio o Podcasts.",
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(16.dp))
        PlaybackSettingsSection(
            bufferProfile = ui.bufferProfile,
            resumeOnConnectivity = ui.resumeOnConnectivity,
            onBufferProfileChange = actions.onBufferProfileChange,
            onResumeOnConnectivityChange = actions.onResumeOnConnectivityChange,
        )

        Spacer(Modifier.height(16.dp))
        HeadsetControlsSection(
            skipBackSeconds = ui.headsetSkipBackSeconds,
            skipForwardSeconds = ui.headsetSkipForwardSeconds,
            gesturesEnabled = ui.headsetSkipGesturesEnabled,
            onSkipBackSecondsChange = actions.onHeadsetSkipBackSecondsChange,
            onSkipForwardSecondsChange = actions.onHeadsetSkipForwardSecondsChange,
            onGesturesEnabledChange = actions.onHeadsetSkipGesturesEnabledChange,
        )

        Spacer(Modifier.height(16.dp))
        DownloadSettingsSection(
            retentionDays = ui.downloadRetentionDays,
            onRetentionChange = actions.onDownloadRetentionChange,
        )

        Spacer(Modifier.height(16.dp))
        EpisodeCacheSettingsSection(
            sizeBytes = ui.cacheSizeBytes,
            limitUnmeteredBytes = ui.cacheLimitUnmeteredBytes,
            limitMeteredBytes = ui.cacheLimitMeteredBytes,
            onClear = actions.onClearEpisodeCache,
        )

        ui.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it)
        }

        Spacer(Modifier.height(24.dp))
        SettingsActionRow(
            label = "Gráficas",
            description = "Ver gráficas de escucha",
            onClick = actions.onStats,
        )
        SettingsActionRow(
            label = "Acerca de",
            description = "Acerca de la aplicación",
            onClick = actions.onAbout,
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** Opciones de podcast en la primera fila del selector (3 + 2). */
private const val PODCAST_START_FIRST_ROW = 3

/**
 * Selector de pantalla de arranque de podcast (spec 0048, US5).
 * Mismo lenguaje visual que la pantalla de arranque de radio; las cinco
 * opciones se reparten en dos filas para que quepan sin truncar etiquetas.
 */
@Composable
internal fun PodcastStartSettings(
    current: PodcastStartScreen,
    onChange: (PodcastStartScreen) -> Unit,
) {
    val rows =
        listOf(
            PodcastStartScreen.entries.take(PODCAST_START_FIRST_ROW),
            PodcastStartScreen.entries.drop(PODCAST_START_FIRST_ROW),
        )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(TolochaTestTags.PODCAST_START_SCREEN),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        rows.forEach { row ->
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                row.forEachIndexed { index, screen ->
                    SegmentedButton(
                        selected = current == screen,
                        onClick = { onChange(screen) },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = row.size,
                            ),
                        label = { Text(screen.label()) },
                    )
                }
            }
        }
    }
}

/**
 * Sección de retención de descargas (extensión spec 0047, 2026-11-19).
 * Separa el texto de la fila para que el nombre de la opción siempre se lea.
 */
@Composable
internal fun DownloadSettingsSection(
    retentionDays: Int,
    onRetentionChange: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text("Descargas", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Box {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true }
                        .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Retención de descargas", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Se borran automáticamente los episodios descargados más antiguos.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    DownloadRetention.label(retentionDays),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DownloadRetention.OPTIONS.forEach { days ->
                    DropdownMenuItem(
                        text = { Text(DownloadRetention.label(days)) },
                        onClick = {
                            expanded = false
                            onRetentionChange(days)
                        },
                    )
                }
            }
        }
        Text(
            "Con «Nunca» no se borrarán automáticamente.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/**
 * Sección de caché de episodios (spec 0058, FR-012/SC-005).
 * Muestra el espacio ocupado, los límites por tipo de red y permite liberarla.
 */
@Composable
internal fun EpisodeCacheSettingsSection(
    sizeBytes: Long,
    limitUnmeteredBytes: Long,
    limitMeteredBytes: Long,
    onClear: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text("Caché de episodios", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "Ocupa ${formatCacheBytes(sizeBytes)}",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "Límite: ${formatCacheBytes(limitUnmeteredBytes)} en Wi-Fi y " +
                "${formatCacheBytes(limitMeteredBytes)} en datos móviles.",
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onClear) {
            Text("Liberar espacio")
        }
    }
}

/** Tamaño legible para la sección de caché. */
internal fun formatCacheBytes(bytes: Long): String {
    val mebibytes = bytes.toDouble() / BYTES_PER_MEBIBYTE
    return if (mebibytes >= 1.0) {
        String.format(Locale.getDefault(), "%.1f MB", mebibytes)
    } else {
        String.format(Locale.getDefault(), "%.0f KB", bytes.toDouble() / BYTES_PER_KIBIBYTE)
    }
}

private const val BYTES_PER_KIBIBYTE = 1_024.0
private const val BYTES_PER_MEBIBYTE = 1_048_576.0

/**
 * Sección "Reproducción" sin estado (spec 0042, FR-010/FR-011/FR-023).
 * Las opciones de buffer van plegadas por defecto para no empujar las acciones
 * finales fuera de pantalla; la cabecera resume el perfil activo.
 * Separada para poder probarla sin Hilt.
 */
@Composable
internal fun PlaybackSettingsSection(
    bufferProfile: BufferProfile,
    resumeOnConnectivity: Boolean,
    onBufferProfileChange: (BufferProfile) -> Unit,
    onResumeOnConnectivityChange: (Boolean) -> Unit,
) {
    var bufferExpanded by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Text("Reproducción", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel =
                            if (bufferExpanded) {
                                "Contraer opciones de buffer"
                            } else {
                                "Expandir opciones de buffer"
                            },
                    ) { bufferExpanded = !bufferExpanded }
                    .semantics {
                        stateDescription = if (bufferExpanded) "Expandido" else "Plegado"
                    }
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Buffer de audio", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Perfil actual: ${bufferProfile.label()}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(
                imageVector =
                    if (bufferExpanded) {
                        Icons.Default.ExpandLess
                    } else {
                        Icons.Default.ExpandMore
                    },
                contentDescription = null,
            )
        }
        if (bufferExpanded) {
            BufferProfile.entries.forEach { profile ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "${profile.label()}. ${profile.description()}"
                            }
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
 * Sección «Controles de auriculares» (spec 0060, FR-008/FR-012/FR-019).
 * Los segundos se eligen entre las opciones canónicas; el interruptor solo
 * afecta a los gestos del auricular, no a los botones en pantalla.
 */
@Composable
internal fun HeadsetControlsSection(
    skipBackSeconds: Int,
    skipForwardSeconds: Int,
    gesturesEnabled: Boolean,
    onSkipBackSecondsChange: (Int) -> Unit,
    onSkipForwardSecondsChange: (Int) -> Unit,
    onGesturesEnabledChange: (Boolean) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(TolochaTestTags.HEADSET_CONTROLS),
    ) {
        Text("Controles de auriculares", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        HeadsetSkipSelector(
            label = "Saltar atrás",
            seconds = skipBackSeconds,
            testTag = TolochaTestTags.HEADSET_SKIP_BACK,
            onSecondsChange = onSkipBackSecondsChange,
        )
        Spacer(Modifier.height(8.dp))
        HeadsetSkipSelector(
            label = "Saltar adelante",
            seconds = skipForwardSeconds,
            testTag = TolochaTestTags.HEADSET_SKIP_FORWARD,
            onSecondsChange = onSkipForwardSecondsChange,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Usar gestos de salto en el auricular" }
                    .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Usar gestos de salto en el auricular", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Pulsación larga de volumen y botones anterior/siguiente.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = gesturesEnabled,
                onCheckedChange = onGesturesEnabledChange,
                modifier =
                    Modifier
                        .testTag(TolochaTestTags.HEADSET_GESTURES_SWITCH)
                        .semantics { contentDescription = "Cambiar gestos de salto del auricular" },
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "La pulsación larga de volumen solo funciona con la app abierta y en " +
                "auriculares compatibles. En Bluetooth, usa anterior/siguiente. La radio " +
                "en directo no se puede saltar.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun HeadsetSkipSelector(
    label: String,
    seconds: Int,
    testTag: String,
    onSecondsChange: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(testTag),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .semantics { contentDescription = "$label: $seconds segundos" }
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                skipSecondsLabel(seconds),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            HeadsetControlSettings.OPTIONS_SECONDS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(skipSecondsLabel(option)) },
                    onClick = {
                        expanded = false
                        onSecondsChange(option)
                    },
                )
            }
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
