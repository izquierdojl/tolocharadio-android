package com.izquierdojl.tolocharadio.feature.explore

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.feature.player.PlayerState
import org.junit.Rule
import org.junit.Test

class StationDetailPlaybackStateTest {
    @get:Rule
    val compose = createComposeRule()

    private val station = StationDto("s1", "Test", url = "https://ejemplo.com/stream")

    private fun content(
        state: PlayerState,
        onRetry: () -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                StationDetailContent(
                    station = station,
                    isFavorite = false,
                    availability = null,
                    playerState = state,
                    onPlay = {},
                    onRetry = onRetry,
                    onToggleFavorite = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun reconectando_muestraIntento() {
        content(PlayerState.Reconnecting(station, attempt = 2, maxAttempts = 6))
        compose.onNodeWithText("Reconectando (2/6)…").assertIsDisplayed()
    }

    @Test
    fun error_permiteReintentar() {
        var retried = false
        content(PlayerState.Error(station, "Fallo de red"), onRetry = { retried = true })
        compose.onNodeWithText("Fallo de red").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        assert(retried)
    }
}
