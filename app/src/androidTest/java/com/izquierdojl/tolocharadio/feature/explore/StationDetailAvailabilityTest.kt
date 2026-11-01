package com.izquierdojl.tolocharadio.feature.explore

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.domain.playback.PlaybackStatusReason
import com.izquierdojl.tolocharadio.domain.playback.StationAvailability
import com.izquierdojl.tolocharadio.feature.player.PlayerState
import org.junit.Rule
import org.junit.Test

class StationDetailAvailabilityTest {
    @get:Rule
    val compose = createComposeRule()

    private val station = StationDto("s1", "Test", url = "https://ejemplo.com/stream")

    private fun content(
        availability: StationAvailability?,
        onPlay: () -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                StationDetailContent(
                    station = station,
                    isFavorite = false,
                    availability = availability,
                    playerState = PlayerState.Idle,
                    onPlay = onPlay,
                    onRetry = {},
                    onToggleFavorite = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun noDisponible_muestraMotivoYDeshabilitaReproducir() {
        var played = false
        content(
            StationAvailability(
                stationId = "s1",
                playable = false,
                reason = PlaybackStatusReason.STREAM_UNAVAILABLE,
                checkedAt = 0,
            ),
            onPlay = { played = true },
        )
        compose.onNodeWithText(PlaybackStatusReason.reasonToMessage(PlaybackStatusReason.STREAM_UNAVAILABLE))
            .assertIsDisplayed()
        compose.onNodeWithText("No disponible").assertIsDisplayed()
        compose.onAllNodesWithText("Reproducir").assertCountEquals(0)
        assert(!played)
    }

    @Test
    fun disponible_permiteReproducir() {
        var played = false
        content(
            StationAvailability("s1", true, null, 0),
            onPlay = { played = true },
        )
        compose.onNodeWithText("Reproducir").assertIsDisplayed()
        compose.onNodeWithText("Reproducir").performClick()
        assert(played)
    }
}
