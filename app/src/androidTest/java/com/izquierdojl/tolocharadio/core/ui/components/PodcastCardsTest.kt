package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Tarjetas de programa y episodio (US7). */
class PodcastCardsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun summary(
        id: Long,
        title: String,
    ) = PodcastSummary(
        id = id,
        feedUrl = "https://feed.example/$id",
        title = title,
        author = "Autor",
        episodeCount = 12,
    )

    @Test
    fun laTarjetaDeProgramaMuestraElTituloYLaAccionEnSuFila() {
        compose.setContent {
            TolochaTheme {
                PodcastCard(
                    summary = summary(1L, "Un título de programa bastante largo para dos líneas"),
                    onOpen = {},
                    onUnsubscribe = {},
                )
            }
        }

        compose.onNodeWithText("Un título de programa bastante largo para dos líneas").assertExists()
        compose.onNodeWithContentDescription("Cancelar suscripción").assertExists()
    }

    @Test
    fun laTarjetaDeEpisodioMuestraPodcastYAcciones() {
        compose.setContent {
            TolochaTheme {
                EpisodeCard(
                    ui =
                        EpisodeCardUi(
                            episode =
                                PodcastEpisode(
                                    id = 1L,
                                    podcastId = 7L,
                                    title = "Episodio con un título largo",
                                    durationSec = 1_800L,
                                ),
                            podcast = summary(7L, "Podcast de prueba"),
                        ),
                    onPlayPause = {},
                    onOpenDetail = {},
                    onToggleFavorite = {},
                )
            }
        }

        compose.onNodeWithText("Episodio con un título largo").assertExists()
        compose.onNodeWithText("Podcast de prueba", substring = true).assertExists()
        compose.onNodeWithContentDescription("Reproducir").assertExists()
        compose.onNodeWithContentDescription("Añadir a favoritos").assertExists()
    }

    @Test
    fun laTarjetaDeEpisodioMuestraCorazonLlenoSiEsFavorito() {
        compose.setContent {
            TolochaTheme {
                EpisodeCard(
                    ui =
                        EpisodeCardUi(
                            episode =
                                PodcastEpisode(
                                    id = 1L,
                                    podcastId = 7L,
                                    title = "Episodio favorito",
                                    durationSec = 1_800L,
                                    isFavorite = true,
                                ),
                            podcast = summary(7L, "Podcast de prueba"),
                        ),
                    onPlayPause = {},
                    onOpenDetail = {},
                    onToggleFavorite = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Quitar de favoritos").assertExists()
    }

    @Test
    fun laFilaDeEpisodioMuestraPlayPrincipalYAccionesAlMantenerPulsado() {
        compose.setContent {
            TolochaTheme {
                EpisodeRow(
                    episode =
                        PodcastEpisode(
                            id = 1L,
                            podcastId = 7L,
                            title = "Episodio favorito",
                            durationSec = 1_800L,
                            isFavorite = true,
                        ),
                    podcast = summary(7L, "Podcast de prueba"),
                    isPlaying = false,
                    onPlayPause = {},
                    onOpenDetail = {},
                    onToggleFavorite = {},
                    onDownload = {},
                    onQueueNext = {},
                    onQueueEnd = {},
                    onTogglePlayed = {},
                )
            }
        }

        compose.onNodeWithContentDescription("Reproducir").assertExists()
        compose.onAllNodesWithContentDescription("Descargar").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Quitar de favoritos").assertCountEquals(0)

        compose.onNodeWithText("Episodio favorito").performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithText("Quitar de favoritos").assertExists()
        compose.onNodeWithText("Descargar").assertExists()
        compose.onNodeWithText("Añadir a continuación").assertExists()
        compose.onNodeWithText("Añadir al final").assertExists()
        compose.onNodeWithText("Marcar como reproducido").assertExists()
        compose.onNodeWithText("Abrir ficha").assertExists()
    }

    @Test
    fun lasTarjetasDeUnaFilaTienenLaMismaAltura() {
        compose.setContent {
            TolochaTheme {
                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f)) {
                        PodcastCard(
                            summary = summary(1L, "Corto"),
                            onOpen = {},
                            onUnsubscribe = {},
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        PodcastCard(
                            summary = summary(2L, "Un título mucho más largo que ocupa dos líneas"),
                            onOpen = {},
                            onUnsubscribe = {},
                        )
                    }
                }
            }
        }

        val cards = compose.onAllNodesWithTag(TolochaTestTags.PODCAST_CARD)
        val first = cards[0].fetchSemanticsNode().boundsInRoot.height
        val second = cards[1].fetchSemanticsNode().boundsInRoot.height
        assertEquals(first, second, 0.5f)
    }
}
