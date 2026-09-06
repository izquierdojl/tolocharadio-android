package com.example.tolocharadio.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tolocharadio.core.ui.components.StationArtwork
import com.example.tolocharadio.data.remote.dto.StationDto
import kotlinx.coroutines.launch

/**
 * Panel inferior persistente sobre la barra de navegación (spec 004).
 * Izquierda: avatar + nombre + línea técnica; derecha: controles.
 * Visible en cualquier estado salvo [PlayerState.Idle].
 */
@Composable
fun MiniPlayer(
    viewModel: PlayerViewModel = hiltViewModel(),
    snackbar: SnackbarHostState,
) {
    val state by viewModel.state.collectAsState()
    val muted by viewModel.isMuted.collectAsState()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }
    val station = playerStation(state) ?: return
    val displayName = station.name.ifBlank { "Emisora" }
    val error = state as? PlayerState.Error
    Surface(
        tonalElevation = 3.dp,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    .semantics { contentDescription = panelAnnouncement(state, displayName) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PanelIdentity(
                station = station,
                title = displayName,
                subtitle = error?.message ?: panelSubtitle(station),
                isError = error != null,
                modifier = Modifier.weight(1f),
                onOpen = { showSheet = true },
            )
            PanelMainAction(
                state = state,
                onToggle = viewModel::toggle,
                onCancelLoad = viewModel::cancelLoad,
                onRetry = viewModel::retry,
            )
            // En error el silencio se oculta (FR-003b); copiar sigue disponible.
            if (error == null) {
                PanelMuteButton(muted = muted, onToggleMute = viewModel::toggleMute)
            }
            Spacer(Modifier.width(4.dp))
            PanelCopyButton(onCopy = {
                val link = resolveCopyLink(station)
                if (link != null) {
                    clipboard.setText(AnnotatedString(link))
                    scope.launch { snackbar.showSnackbar("Enlace copiado") }
                } else {
                    scope.launch { snackbar.showSnackbar("Enlace no disponible") }
                }
            })
        }
    }
    if (showSheet) {
        FullPlayerSheet(stationName = station.name) {
            showSheet = false
        }
    }
}

/** Emisora activa del panel; `null` en `Idle` (el panel no se muestra). */
private fun playerStation(state: PlayerState): StationDto? =
    when (state) {
        is PlayerState.Buffering -> state.station
        is PlayerState.Playing -> state.station
        is PlayerState.Paused -> state.station
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
        is PlayerState.Buffering -> "Cargando: $displayName"
        is PlayerState.Error -> "Error de reproducción: $displayName"
        PlayerState.Idle -> ""
    }

/** Zona izquierda del panel: avatar + nombre + línea técnica; abre el completo (FR-007). */
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
                onClickLabel = "Abrir reproductor",
                role = Role.Button,
            ) { onOpen() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationArtwork(station = station, modifier = Modifier.size(48.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
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

/** Botón principal del panel según el estado (en `Buffering` cancela la carga). */
@Composable
private fun PanelMainAction(
    state: PlayerState,
    onToggle: () -> Unit,
    onCancelLoad: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        is PlayerState.Buffering -> {
            IconButton(
                onClick = onCancelLoad,
                modifier = Modifier.semantics { contentDescription = "Cancelar carga" },
            ) {
                CircularProgressIndicator(Modifier.size(24.dp))
            }
        }
        is PlayerState.Playing -> {
            IconButton(onClick = onToggle) {
                Icon(Icons.Filled.Pause, contentDescription = "Pausar")
            }
        }
        is PlayerState.Paused -> {
            IconButton(onClick = onToggle) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Reanudar")
            }
        }
        is PlayerState.Error -> {
            IconButton(onClick = onRetry) {
                Icon(Icons.Filled.Refresh, contentDescription = "Reintentar")
            }
        }
        PlayerState.Idle -> Unit
    }
}

/** Silencio local sin detener la emisión (FR-005). */
@Composable
private fun PanelMuteButton(
    muted: Boolean,
    onToggleMute: () -> Unit,
) {
    IconButton(onClick = onToggleMute) {
        Icon(
            if (muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
            contentDescription = if (muted) "Activar sonido" else "Silenciar",
        )
    }
}

/** Copia la URL original del stream al portapapeles (FR-006). */
@Composable
private fun PanelCopyButton(onCopy: () -> Unit) {
    IconButton(onClick = onCopy) {
        Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar enlace")
    }
}

/** Reproductor completo en bottom sheet (nombre + controles). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(
    stationName: String,
    viewModel: PlayerViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Text(stationName, style = MaterialTheme.typography.headlineSmall)
            if (state is PlayerState.Buffering) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::toggle) {
                    Icon(
                        if (state is PlayerState.Playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/pausa",
                    )
                }
                IconButton(onClick = {
                    viewModel.stop()
                    onDismiss()
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Detener")
                }
                if (state is PlayerState.Error) {
                    IconButton(onClick = viewModel::retry) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reintentar")
                    }
                }
            }
            (state as? PlayerState.Error)?.let {
                Text(it.message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
