package com.izquierdojl.tolocharadio.feature.podcasts.queue

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodeProgress
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerUi
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QueueScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val current =
        PodcastEpisode(
            id = 1L,
            podcastId = 7L,
            title = "Episodio en curso",
            durationSec = 1_800L,
        )
    private val pending =
        PodcastEpisode(
            id = 2L,
            podcastId = 7L,
            title = "Pendiente",
            durationSec = 1_200L,
        )

    private fun playerUi(
        playing: Boolean = true,
        positionMs: Long = 60_000L,
        durationMs: Long = 1_800_000L,
    ) = EpisodePlayerUi(
        episode = current,
        playing = playing,
        positionMs = positionMs,
        durationMs = durationMs,
    )

    private fun content(
        state: QueueUiState,
        playerUi: EpisodePlayerUi = playerUi(),
        onOpen: () -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                QueueScreenContent(
                    state = state,
                    playerUi = playerUi,
                    onPlay = {},
                    onRemove = {},
                    onMove = { _, _ -> },
                    onCommit = {},
                    onClear = {},
                    onRetry = {},
                    onOpenFullPlayer = onOpen,
                )
            }
        }
    }

    @Test
    fun muestraElBloqueEnCursoYLosPendientesSinDuplicar() {
        content(QueueUiState.Content(items = listOf(pending), revision = 1L, limit = 200))

        compose.onNodeWithTag(TolochaTestTags.QUEUE_NOW_PLAYING).assertIsDisplayed()
        compose.onAllNodesWithText("Episodio en curso").assertCountEquals(1)
        compose.onNodeWithText("Pendiente").assertIsDisplayed()
    }

    @Test
    fun colaSinPendientesConEpisodioEnCursoMuestraMensaje() {
        content(QueueUiState.Empty)

        compose.onNodeWithTag(TolochaTestTags.QUEUE_NOW_PLAYING).assertIsDisplayed()
        compose.onNodeWithText("No hay más episodios en cola").assertIsDisplayed()
    }

    @Test
    fun pausadoMuestraEstadoYUltimaPosicion() {
        content(
            QueueUiState.Content(items = emptyList(), revision = 1L, limit = 200),
            playerUi = playerUi(playing = false, positionMs = 120_000L),
        )

        compose.onNodeWithText("Pausado").assertIsDisplayed()
        compose.onNodeWithText("2:00").assertIsDisplayed()
    }

    @Test
    fun duracionDesconocidaNoInventaTotalNiBarra() {
        content(
            QueueUiState.Content(items = emptyList(), revision = 1L, limit = 200),
            playerUi = playerUi(durationMs = 0L),
        )

        compose.onNodeWithContentDescription("Avance del episodio").assertDoesNotExist()
        compose.onAllNodesWithText("30:00").assertCountEquals(0)
    }

    @Test
    fun tocarElBloqueAbreElReproductorCompleto() {
        var opened = false
        content(
            QueueUiState.Content(items = emptyList(), revision = 1L, limit = 200),
            onOpen = { opened = true },
        )

        compose.onNodeWithTag(TolochaTestTags.QUEUE_NOW_PLAYING).performClick()
        assertTrue(opened)
    }

    @Test
    fun elBloqueSigueVisibleAlDesplazarLosPendientes() {
        val many = List(20) { index -> pending.copy(id = 100L + index, title = "Pendiente $index") }
        content(QueueUiState.Content(items = many, revision = 1L, limit = 200))

        compose.onNodeWithTag(TolochaTestTags.QUEUE_LIST).performScrollToIndex(19)
        compose.onNodeWithTag(TolochaTestTags.QUEUE_NOW_PLAYING).assertIsDisplayed()
    }

    @Test
    fun muestraDuracionYRestanteEnLaFila() {
        val inProgress =
            pending.copy(
                durationSec = 300L,
                progress = PodcastEpisodeProgress(positionSec = 120L, durationSec = 300L),
            )
        content(QueueUiState.Content(items = listOf(inProgress), revision = 1L, limit = 200))

        compose.onNodeWithText("05:00", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Quedan 3 min", substring = true).assertIsDisplayed()
    }

    @Test
    fun noMuestraRestanteSiElEpisodioEstaCompletado() {
        val completed =
            pending.copy(
                durationSec = 300L,
                progress = PodcastEpisodeProgress(positionSec = 120L, durationSec = 300L, completed = true),
            )
        content(QueueUiState.Content(items = listOf(completed), revision = 1L, limit = 200))

        compose.onNodeWithText("Quedan", substring = true).assertDoesNotExist()
    }

    @Test
    fun noMuestraMetadatosSiLaDuracionEsDesconocida() {
        val unknown = pending.copy(durationSec = null, progress = null)
        content(QueueUiState.Content(items = listOf(unknown), revision = 1L, limit = 200))

        compose.onNodeWithTag(TolochaTestTags.QUEUE_EPISODE_META).assertDoesNotExist()
    }

    @Test
    fun indicaMenosDeUnMinutoCuandoQuedaPoco() {
        val almostDone =
            pending.copy(
                durationSec = 301L,
                progress = PodcastEpisodeProgress(positionSec = 242L, durationSec = 301L),
            )
        content(QueueUiState.Content(items = listOf(almostDone), revision = 1L, limit = 200))

        compose.onNodeWithText("Queda menos de 1 min", substring = true).assertIsDisplayed()
    }
}
