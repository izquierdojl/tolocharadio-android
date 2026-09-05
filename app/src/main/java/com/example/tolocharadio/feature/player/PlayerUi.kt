package com.example.tolocharadio.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Mini-player persistente sobre la bottom bar (US-5). Visible en
 * cualquier estado salvo [PlayerState.Idle].
 */
@Composable
fun MiniPlayer(viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    if (state is PlayerState.Idle) return
    Row(
        modifier =
            Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                .clickable { showSheet = true },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (val s = state) {
            is PlayerState.Buffering -> {
                CircularProgressIndicator(Modifier.padding(8.dp))
                Text(s.station.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
            is PlayerState.Playing -> {
                IconButton(onClick = viewModel::toggle) {
                    Icon(Icons.Filled.Pause, contentDescription = "Pausar")
                }
                Text(
                    s.station.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            is PlayerState.Paused -> {
                IconButton(onClick = viewModel::toggle) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Reanudar")
                }
                Text(
                    s.station.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            is PlayerState.Error -> {
                IconButton(onClick = viewModel::retry) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reintentar")
                }
                Text(
                    s.message,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            PlayerState.Idle -> Unit
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = viewModel::stop) {
            Icon(Icons.Filled.Close, contentDescription = "Detener")
        }
    }
    if (showSheet) {
        FullPlayerSheet(stationName = (state as? PlayerState.Playing)?.station?.name.orEmpty()) {
            showSheet = false
        }
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
