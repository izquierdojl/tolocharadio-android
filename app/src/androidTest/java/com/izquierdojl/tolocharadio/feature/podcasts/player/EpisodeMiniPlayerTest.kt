package com.izquierdojl.tolocharadio.feature.podcasts.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerUi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EpisodeMiniPlayerTest {
    @get:Rule
    val compose = createComposeRule()

    private val episode =
        PodcastEpisode(
            id = 1L,
            podcastId = 7L,
            title = "Episodio en curso",
            durationSec = 1_800L,
        )

    private fun content(
        ui: EpisodePlayerUi,
        onOpen: () -> Unit = {},
        onStop: () -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                EpisodeMiniPlayerContent(
                    ui = ui,
                    episode = episode,
                    onOpenFullPlayer = onOpen,
                    onToggle = {},
                    onSkipBack = {},
                    onSkipForward = {},
                    onStop = onStop,
                )
            }
        }
    }

    private fun ui(
        playing: Boolean = true,
        positionMs: Long = 60_000L,
        durationMs: Long = 1_800_000L,
    ) = EpisodePlayerUi(
        episode = episode,
        playing = playing,
        positionMs = positionMs,
        durationMs = durationMs,
    )

    @Test
    fun muestraElTiempoTranscurridoYElRestante() {
        content(ui())

        compose.onNodeWithText("1:00").assertIsDisplayed()
        compose.onNodeWithText("-29:00").assertIsDisplayed()
        compose.onNodeWithContentDescription("Transcurrido 1:00. Quedan 29:00").assertIsDisplayed()
    }

    @Test
    fun sinDuracionSoloMuestraElTranscurrido() {
        content(ui(durationMs = 0L))

        compose.onNodeWithText("1:00").assertIsDisplayed()
        compose.onNodeWithText("-", substring = true).assertDoesNotExist()
    }

    @Test
    fun pausadoMuestraReanudar() {
        content(ui(playing = false))

        compose.onNodeWithContentDescription("Reproducir").assertIsDisplayed()
    }

    @Test
    fun elBotonDetenerInvocaElCierreYNoAbreElReproductor() {
        var stopped = false
        var opened = false
        content(ui(), onOpen = { opened = true }, onStop = { stopped = true })

        compose.onNodeWithContentDescription("Detener").performClick()

        assertTrue(stopped)
        assertFalse(opened)
    }
}
