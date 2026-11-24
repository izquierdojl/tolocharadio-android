package com.izquierdojl.tolocharadio.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.izquierdojl.tolocharadio.R
import com.izquierdojl.tolocharadio.feature.podcasts.EPISODE_RATES

/**
 * Controles VOD del episodio (US3, FR-014/FR-017): reproducir/pausar, saltos
 * configurables y velocidad 1x–2x. Solo visible con episodio activo.
 */
@Composable
@Suppress("LongParameterList")
fun EpisodePlayerControls(
    playing: Boolean,
    rate: Float,
    casting: Boolean,
    skipBackSeconds: Int,
    skipForwardSeconds: Int,
    onToggle: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onCycleRate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSkipBack) {
            Icon(
                Icons.AutoMirrored.Filled.Undo,
                contentDescription = stringResource(R.string.media_seek_back_seconds, skipBackSeconds),
            )
        }
        FilledIconButton(onClick = onToggle) {
            Icon(
                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Pausar" else "Reproducir",
            )
        }
        IconButton(onClick = onSkipForward) {
            Icon(
                Icons.AutoMirrored.Filled.Redo,
                contentDescription = stringResource(R.string.media_seek_forward_seconds, skipForwardSeconds),
            )
        }
        TextButton(onClick = onCycleRate, enabled = !casting) {
            Text(
                "${rateString(rate)}x",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun rateString(rate: Float): String {
    val current = EPISODE_RATES.minByOrNull { kotlin.math.abs(it - rate) } ?: 1f
    return if (current == current.toInt().toFloat()) {
        current.toInt().toString()
    } else {
        current.toString()
    }
}
