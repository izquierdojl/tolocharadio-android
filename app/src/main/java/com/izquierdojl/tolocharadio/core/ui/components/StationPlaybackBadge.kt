package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.core.ui.playback.StationPlaybackUiState

/**
 * Indicador de estado de una emisora en tarjetas y filas.
 *
 * No depende solo del color: combina icono, texto (en tarjeta) y nombre
 * accesible. En filas se usa [compact] para no romper la densidad.
 */
@Composable
fun StationPlaybackBadge(
    state: StationPlaybackUiState,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (state == StationPlaybackUiState.Inactive) return

    val description = badgeDescription(state)
    val text = badgeText(state)
    val icon = badgeIcon(state)
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.semantics { contentDescription = description },
        shape = CircleShape,
        color = colors.surface.copy(alpha = 0.9f),
        contentColor = colors.onSurface,
    ) {
        if (compact) {
            Box(Modifier.padding(4.dp), contentAlignment = Alignment.Center) {
                if (state == StationPlaybackUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = colors.onSurface,
                    )
                } else {
                    Icon(icon!!, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state == StationPlaybackUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = colors.onSurface,
                    )
                } else {
                    Icon(icon!!, contentDescription = null, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text(text, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

private fun badgeDescription(state: StationPlaybackUiState): String =
    when (state) {
        StationPlaybackUiState.Playing -> "En reproducción"
        StationPlaybackUiState.Paused -> "Pausado"
        StationPlaybackUiState.Loading -> "Cargando emisora"
        StationPlaybackUiState.Error -> "Error de reproducción"
        StationPlaybackUiState.Inactive -> ""
    }

private fun badgeText(state: StationPlaybackUiState): String =
    when (state) {
        StationPlaybackUiState.Playing -> "En directo"
        StationPlaybackUiState.Paused -> "Pausado"
        StationPlaybackUiState.Loading -> "Cargando"
        StationPlaybackUiState.Error -> "Error"
        StationPlaybackUiState.Inactive -> ""
    }

private fun badgeIcon(state: StationPlaybackUiState): ImageVector? =
    when (state) {
        StationPlaybackUiState.Playing -> Icons.Filled.GraphicEq
        StationPlaybackUiState.Paused -> Icons.Filled.Pause
        StationPlaybackUiState.Error -> Icons.Filled.Error
        StationPlaybackUiState.Loading, StationPlaybackUiState.Inactive -> null
    }
