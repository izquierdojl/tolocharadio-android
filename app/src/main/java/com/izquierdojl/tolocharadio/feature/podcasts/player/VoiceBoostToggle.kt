package com.izquierdojl.tolocharadio.feature.podcasts.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.R
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags

/**
 * Interruptor del potenciador de voz (spec 0047, US4). Solo se
 * muestra en el reproductor de episodios, nunca en la radio.
 */
@Composable
fun VoiceBoostToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stateText = stringResource(if (enabled) R.string.voice_boost_on else R.string.voice_boost_off)
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.VolumeUp, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.voice_boost), style = MaterialTheme.typography.titleSmall)
            Text(
                stateText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            modifier =
                Modifier
                    .testTag(TolochaTestTags.VOICE_BOOST)
                    .semantics { stateDescription = stateText },
        )
    }
}
