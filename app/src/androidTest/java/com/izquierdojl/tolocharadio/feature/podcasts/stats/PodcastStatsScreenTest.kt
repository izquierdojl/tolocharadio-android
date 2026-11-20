package com.izquierdojl.tolocharadio.feature.podcasts.stats

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastStatsSummaryDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastTimelineDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastTimelineEntryDto
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PodcastStatsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun content(
        state: PodcastStatsUi,
        onRetry: () -> Unit = {},
        onEditServer: () -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                PodcastStatsContent(
                    state = state,
                    onRetry = onRetry,
                    onEditServer = onEditServer,
                )
            }
        }
    }

    private fun summary() =
        PodcastStatsSummaryDto(
            totalMs = 3_600_000L,
            podcastCount = 2,
            startedEpisodes = 3,
            completedEpisodes = 2,
            completionRate = 0.66,
            avgMsPerEpisode = 1_200_000L,
        )

    @Test
    fun modoPodcastMuestraSuEncabezadoYBloques() {
        content(
            PodcastStatsUi.Content(
                summary = summary(),
                timeline =
                    PodcastTimelineDto(
                        granularity = "day",
                        items = listOf(PodcastTimelineEntryDto("2026-11-19", 1_000L)),
                    ),
            ),
        )

        compose.onNodeWithText("Tus estadísticas de podcast").assertIsDisplayed()
        compose.onNodeWithText("Tiempo total").assertIsDisplayed()
        compose.onNodeWithText("Evolución de la escucha").assertIsDisplayed()
    }

    @Test
    fun vacioMuestraMensajePropioDePodcast() {
        content(PodcastStatsUi.Empty)

        compose.onNodeWithText(
            "Todavía no hay escucha de podcast. Reproduce un episodio y vuelve aquí.",
        ).assertIsDisplayed()
    }

    @Test
    fun errorOfreceReintento() {
        var retried = false
        content(PodcastStatsUi.Error("No se pudo cargar"), onRetry = { retried = true })

        compose.onNodeWithText("Reintentar").performClick()
        assertTrue(retried)
    }

    @Test
    fun errorDeCredencialesOfreceEditarServidor() {
        var edited = false
        content(
            PodcastStatsUi.Error("No se pudo autenticar", isAuthError = true),
            onEditServer = { edited = true },
        )

        compose.onNodeWithText("Editar servidor").performClick()
        assertTrue(edited)
    }
}
