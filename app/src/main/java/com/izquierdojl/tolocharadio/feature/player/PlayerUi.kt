package com.izquierdojl.tolocharadio.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.izquierdojl.tolocharadio.cast.CastConnectionState
import com.izquierdojl.tolocharadio.cast.CastPlayerState
import com.izquierdojl.tolocharadio.core.ui.components.NowPlayingContainer
import com.izquierdojl.tolocharadio.core.ui.components.StationArtwork
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.domain.playback.PlayerErrorAction
import kotlinx.coroutines.launch

/**
 * Panel inferior persistente sobre la barra de navegación (spec 004).
 * Izquierda: avatar + nombre + línea técnica; derecha: controles.
 * Visible en cualquier estado salvo [PlayerState.Idle].
 * Transiciones suaves con AnimatedVisibility (spec 010, US4, SC-005).
 */
@Composable
@Suppress("CyclomaticComplexMethod")
fun MiniPlayer(
    viewModel: PlayerViewModel = hiltViewModel(),
    snackbar: SnackbarHostState,
    onEditServer: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val muted by viewModel.isMuted.collectAsState()
    val castState by viewModel.castState.collectAsState()
    val castConnectionState by viewModel.castPlayerManager.connectionState.collectAsState()
    val fullPlayerVisible by viewModel.fullPlayerVisible.collectAsState()
    val scope = rememberCoroutineScope()
    var showStationInfo by remember { mutableStateOf(false) }
    var wasConnecting by remember { mutableStateOf(false) }
    // Con Cast conectado el estado real lo gobierna el CastPlayer.
    val effectiveState = (castState as? CastPlayerState.Cast)?.playerState ?: state
    val station = playerStation(effectiveState)
    val isVisible = station != null
    val error = effectiveState as? PlayerState.Error
    // FR-012: Mostrar Snackbar cuando la conexión a Chromecast falla
    LaunchedEffect(castConnectionState) {
        if (castConnectionState == CastConnectionState.CONNECTING) {
            wasConnecting = true
        } else if (wasConnecting && castConnectionState == CastConnectionState.DISCONNECTED) {
            wasConnecting = false
            scope.launch {
                snackbar.showSnackbar("No se pudo conectar al dispositivo")
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        val safeStation = station ?: return@AnimatedVisibility
        val panelStatus =
            when (val s = effectiveState) {
                is PlayerState.Playing -> "En directo"
                is PlayerState.Paused -> "Pausado"
                is PlayerState.Reconnecting ->
                    when {
                        s.applying -> "Aplicando ajuste…"
                        s.waitingForNetwork -> "Sin conexión. Se reanudará al volver la red."
                        else -> "Reconectando (${s.attempt}/${s.maxAttempts})…"
                    }
                is PlayerState.Buffering ->
                    when {
                        s.waitingForNetwork -> "Sin conexión. Se reanudará al volver la red."
                        s.retrying -> "Reintentando…"
                        else -> "Cargando…"
                    }
                is PlayerState.Error -> s.message
                PlayerState.Idle -> ""
            }
        MiniPlayerContent(
            state = effectiveState,
            station = safeStation,
            muted = muted,
            panel = PlayerPanelUi(panelStatus, (castState as? CastPlayerState.Cast)?.deviceName),
            actions =
                MiniPlayerActions(
                    onOpen = { showStationInfo = true },
                    onToggle = viewModel::toggle,
                    onCancelLoad = viewModel::cancelLoad,
                    onRetry = viewModel::retry,
                    onEditServer = onEditServer,
                    onToggleMute = viewModel::toggleMute,
                ),
        )
    }
    if (showStationInfo && station != null) {
        StationInfoSheet(station = station) {
            showStationInfo = false
        }
    }
    if (fullPlayerVisible && station != null) {
        FullPlayerSheet(
            stationName = station.name,
            station = station,
            viewModel = viewModel,
            onDismiss = { viewModel.closeFullPlayer() },
            onEditServer = onEditServer,
        )
    }
}

/**
 * Acciones del mini-player (evita firmas largas).
 */
data class MiniPlayerActions(
    val onOpen: () -> Unit,
    val onToggle: () -> Unit,
    val onCancelLoad: () -> Unit,
    val onRetry: () -> Unit,
    val onEditServer: () -> Unit,
    val onToggleMute: () -> Unit,
)

/** Estado de presentación del panel de radio (spec 0061). */
internal data class PlayerPanelUi(
    val status: String,
    val castDeviceName: String? = null,
) {
    fun displayStatus(): String = if (castDeviceName.isNullOrBlank()) status else "$status · $castDeviceName"
}

/**
 * Contenido sin estado del mini-player (puerta de test sin Hilt, spec 0039, C2):
 * en reproducción normal exactamente 2 acciones (principal + silenciar), sin
 * copiar enlace; en error solo reintentar; en carga indicador con cancelar.
 */
@Composable
internal fun MiniPlayerContent(
    state: PlayerState,
    station: StationDto,
    muted: Boolean,
    panel: PlayerPanelUi,
    actions: MiniPlayerActions,
) {
    val displayName = station.name.ifBlank { "Emisora" }
    val error = state as? PlayerState.Error
    NowPlayingContainer {
        Row(
            modifier =
                Modifier.fillMaxWidth().heightIn(min = 63.dp).padding(horizontal = 8.dp)
                    .semantics { contentDescription = panelAnnouncement(state, displayName) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PanelIdentity(
                station = station,
                title = displayName,
                subtitle = panel.displayStatus(),
                isError = error != null,
                modifier = Modifier.weight(1f),
                onOpen = actions.onOpen,
            )
            PanelMainAction(
                state = state,
                onToggle = actions.onToggle,
                onCancelLoad = actions.onCancelLoad,
                onRetry = actions.onRetry,
                onEditServer = actions.onEditServer,
            )
            // En error el silencio se oculta (FR-003b); el enlace se comparte desde la ficha (spec 0039).
            if (error == null) {
                PanelMuteButton(muted = muted, onToggleMute = actions.onToggleMute)
            }
        }
    }
}

/** Emisora activa del panel; `null` en `Idle` (el panel no se muestra). */
private fun playerStation(state: PlayerState): StationDto? =
    when (state) {
        is PlayerState.Buffering -> state.station
        is PlayerState.Playing -> state.station
        is PlayerState.Paused -> state.station
        is PlayerState.Reconnecting -> state.station
        is PlayerState.Error -> state.station
        PlayerState.Idle -> null
    }

/** Anuncio de TalkBack del panel según el estado. */
private fun panelAnnouncement(
    state: PlayerState,
    displayName: String,
): String =
    when (state) {
        is PlayerState.Playing -> "Sonando: $displayName"
        is PlayerState.Paused -> "Pausado: $displayName"
        is PlayerState.Buffering ->
            when {
                state.waitingForNetwork -> "Sin conexión: $displayName"
                state.retrying -> "Reintentando: $displayName"
                else -> "Cargando: $displayName"
            }
        is PlayerState.Reconnecting ->
            when {
                state.applying -> "Aplicando ajuste: $displayName"
                state.waitingForNetwork -> "Sin conexión: $displayName"
                else -> "Reconectando: $displayName"
            }
        is PlayerState.Error -> "Error de reproducción: $displayName"
        PlayerState.Idle -> ""
    }

/** Zona izquierda del panel: avatar + nombre + estado; abre la ficha de la emisora. */
@Composable
private fun PanelIdentity(
    station: StationDto,
    title: String,
    subtitle: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
) {
    Row(
        modifier =
            modifier.clickable(
                onClickLabel = "Abrir ficha de la emisora",
                role = Role.Button,
            ) { onOpen() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationArtwork(station = station, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}

/** Botón principal del panel según el estado (en `Buffering` cancela la carga).
 *  Crossfade para transiciones suaves entre estados (spec 010, US4, SC-005).
 */
@Composable
private fun PanelMainAction(
    state: PlayerState,
    onToggle: () -> Unit,
    onCancelLoad: () -> Unit,
    onRetry: () -> Unit,
    onEditServer: () -> Unit,
) {
    Crossfade(
        targetState = state,
        label = "panel-action-crossfade",
    ) { current ->
        when (current) {
            is PlayerState.Buffering -> {
                IconButton(
                    onClick = onCancelLoad,
                    modifier = Modifier.size(48.dp).semantics { contentDescription = "Cancelar carga" },
                ) {
                    CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp)
                }
            }
            is PlayerState.Reconnecting -> {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp)
                }
            }
            is PlayerState.Playing -> {
                FilledIconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(56.dp),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = "Pausar", modifier = Modifier.size(30.dp))
                }
            }
            is PlayerState.Paused -> {
                FilledIconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(56.dp),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Reanudar", modifier = Modifier.size(30.dp))
                }
            }
            is PlayerState.Error -> {
                IconButton(
                    onClick =
                        if (current.action == PlayerErrorAction.EDIT_SERVER) {
                            onEditServer
                        } else {
                            onRetry
                        },
                    modifier = Modifier.size(48.dp),
                ) {
                    if (current.action == PlayerErrorAction.EDIT_SERVER) {
                        Icon(Icons.Filled.Settings, contentDescription = "Editar servidor")
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reintentar")
                    }
                }
            }
            PlayerState.Idle -> Unit
        }
    }
}

/** Silencio local sin detener la emisión (FR-005). */
@Composable
private fun PanelMuteButton(
    muted: Boolean,
    onToggleMute: () -> Unit,
) {
    IconButton(onClick = onToggleMute, modifier = Modifier.size(48.dp)) {
        Icon(
            if (muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
            contentDescription = if (muted) "Activar sonido" else "Silenciar",
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Reproductor completo en bottom sheet (nombre + controles). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(
    stationName: String,
    station: StationDto? = null,
    viewModel: PlayerViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onEditServer: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val castState by viewModel.castState.collectAsState()
    val isCastConnected = castState is CastPlayerState.Cast
    val effectiveState = (castState as? CastPlayerState.Cast)?.playerState ?: state
    var showStationInfo by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        FullPlayerContent(
            state = effectiveState,
            stationName = stationName,
            station = station,
            isCastConnected = isCastConnected,
            castDeviceName = (castState as? CastPlayerState.Cast)?.deviceName ?: "Chromecast",
            onToggle = viewModel::toggle,
            onStop = {
                viewModel.stop()
                onDismiss()
            },
            onRetry = viewModel::retry,
            onEditServer = onEditServer,
            onOpenInfo = { showStationInfo = true },
        )
    }
    if (showStationInfo && station != null) {
        StationInfoSheet(station = station) {
            showStationInfo = false
        }
    }
}

/** Contenido sin estado del reproductor completo (testeable sin Hilt). */
@Composable
@Suppress("LongParameterList", "CyclomaticComplexMethod")
internal fun FullPlayerContent(
    state: PlayerState,
    stationName: String,
    station: StationDto?,
    isCastConnected: Boolean,
    castDeviceName: String,
    onToggle: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onEditServer: () -> Unit,
    onOpenInfo: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = station != null) { onOpenInfo() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (station != null) {
                StationArtwork(station = station, modifier = Modifier.size(48.dp))
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(stationName, style = MaterialTheme.typography.headlineSmall)
                if (isCastConnected) {
                    Text(
                        castDeviceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        when (state) {
            is PlayerState.Buffering -> {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
                Text(
                    when {
                        state.waitingForNetwork -> "Sin conexión. Se reanudará al volver la red."
                        state.retrying -> "Reintentando…"
                        else -> "Cargando…"
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            is PlayerState.Reconnecting -> {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
                Text(
                    when {
                        state.applying -> "Aplicando ajuste…"
                        state.waitingForNetwork -> "Sin conexión. Se reanudará al volver la red."
                        else -> "Reconectando (${state.attempt}/${state.maxAttempts})…"
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            is PlayerState.Error -> {
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            else -> Unit
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggle) {
                Icon(
                    if (state is PlayerState.Playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state is PlayerState.Playing) "Pausar" else "Reanudar",
                )
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Filled.Close, contentDescription = "Detener")
            }
            if (state is PlayerState.Error) {
                if (state.action == PlayerErrorAction.EDIT_SERVER) {
                    TextButton(onClick = onEditServer) {
                        Text("Editar servidor")
                    }
                } else {
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reintentar")
                    }
                }
            }
        }
    }
}
