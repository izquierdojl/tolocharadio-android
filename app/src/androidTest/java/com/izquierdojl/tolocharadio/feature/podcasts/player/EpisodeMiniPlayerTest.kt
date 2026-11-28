package com.izquierdojl.tolocharadio.feature.podcasts.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerUi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

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
        overrideEpisode: PodcastEpisode = episode,
        onOpen: () -> Unit = {},
        onStop: () -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                EpisodeMiniPlayerContent(
                    ui = ui,
                    episode = overrideEpisode,
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
        uiEpisode: PodcastEpisode = episode,
    ) = EpisodePlayerUi(
        episode = uiEpisode,
        playing = playing,
        positionMs = positionMs,
        durationMs = durationMs,
    )

    @Test
    fun panelSinImagenMideComoMaximo112Dp() {
        content(ui())
        val heightDp = compose.onRoot().fetchSemanticsNode().boundsInRoot.height / compose.density.density
        assertTrue("Panel de $heightDp dp", heightDp <= 112.5f)
        compose.onNodeWithText(episode.title).assertIsDisplayed()
    }

    @Test
    fun panelConImagenMideComoMaximo72DpYOcultaElTexto() {
        val withImage = episode.copy(imageUrl = "https://example.org/episode.png")
        content(ui(uiEpisode = withImage), overrideEpisode = withImage)
        val heightDp = compose.onRoot().fetchSemanticsNode().boundsInRoot.height / compose.density.density
        assertTrue("Panel de $heightDp dp", heightDp <= 72.5f)
        compose.onNodeWithText(withImage.title).assertDoesNotExist()
        compose.onNodeWithContentDescription("${withImage.title}, Podcast").assertExists()
    }

    @Test
    fun losControlesCentralesQuedanCentradosEnLaBarra() {
        val withImage = episode.copy(imageUrl = "https://example.org/episode.png")
        content(ui(uiEpisode = withImage), overrideEpisode = withImage)
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val play = compose.onNodeWithContentDescription("Pausar").fetchSemanticsNode().boundsInRoot
        val density = compose.density.density
        val topGap = (play.top - root.top) / density
        val bottomGap = (root.bottom - play.bottom) / density
        val horizontalOffset = ((play.left + play.right) / 2f - (root.left + root.right) / 2f) / density
        assertTrue("Huecos $topGap/$bottomGap dp", abs(topGap - bottomGap) <= 1.5f)
        assertTrue("Descentrado $horizontalOffset dp", abs(horizontalOffset) <= 1.5f)
    }

    @Test
    fun muestraElTiempoTranscurridoYElRestante() {
        content(ui())

        compose.onNodeWithText("1:00").assertIsDisplayed()
        compose.onNodeWithText("-29:00").assertIsDisplayed()
        compose.onNodeWithContentDescription("Transcurrido 1:00 de 30:00. Quedan 29:00").assertIsDisplayed()
    }

    @Test
    fun sinDuracionSoloMuestraElTranscurrido() {
        content(ui(durationMs = 0L))

        compose.onNodeWithText("1:00").assertIsDisplayed()
        compose.onNodeWithText("-", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Transcurrido 1:00").assertIsDisplayed()
    }

    @Test
    fun pausadoMuestraReanudar() {
        content(ui(playing = false))

        compose.onNodeWithContentDescription("Reproducir").assertIsDisplayed()
    }

    @Test
    fun lasEtiquetasDeSaltoReflejanLosSegundosConfigurados() {
        content(ui().copy(skipBackSeconds = 5, skipForwardSeconds = 60))

        compose.onNodeWithContentDescription("Retroceder 5 segundos").assertIsDisplayed()
        compose.onNodeWithContentDescription("Avanzar 60 segundos").assertIsDisplayed()
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
