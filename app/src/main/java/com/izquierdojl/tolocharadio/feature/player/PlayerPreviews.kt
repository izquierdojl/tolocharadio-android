package com.izquierdojl.tolocharadio.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto

@Preview(name = "MiniPlayer dark", showBackground = true, backgroundColor = 0xFF08100B)
@Composable
internal fun MiniPlayerDarkPreview() {
    val station = StationDto("preview", "Radio Sierra", url = "https://stream.test/radio.mp3")
    TolochaTheme(darkTheme = true) {
        MiniPlayerContent(
            state = PlayerState.Playing(station),
            station = station,
            muted = false,
            panel = PlayerPanelUi("En directo", "Sala"),
            actions = emptyActions(),
        )
    }
}

@Preview(name = "MiniPlayer light", showBackground = true, backgroundColor = 0xFFEEF3EF)
@Composable
internal fun MiniPlayerLightPreview() {
    val station = StationDto("preview", "Radio Sierra", url = "https://stream.test/radio.mp3")
    TolochaTheme(darkTheme = false) {
        MiniPlayerContent(
            state = PlayerState.Paused(station),
            station = station,
            muted = true,
            panel = PlayerPanelUi("Pausado"),
            actions = emptyActions(),
        )
    }
}

private fun emptyActions() =
    MiniPlayerActions(
        onOpen = {},
        onToggle = {},
        onCancelLoad = {},
        onRetry = {},
        onEditServer = {},
        onToggleMute = {},
    )
