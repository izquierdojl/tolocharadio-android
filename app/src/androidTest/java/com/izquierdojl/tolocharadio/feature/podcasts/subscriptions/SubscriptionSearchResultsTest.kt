package com.izquierdojl.tolocharadio.feature.podcasts.subscriptions

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.feature.podcasts.EpisodePlayerUi
import com.izquierdojl.tolocharadio.feature.podcasts.SubscriptionSearchScope
import com.izquierdojl.tolocharadio.feature.podcasts.SubscriptionSearchUi
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SubscriptionSearchResultsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun podcast(id: Long = 1): PodcastSummary {
        return PodcastSummary(id = id, feedUrl = "https://feed.example/$id", title = "Podcast $id")
    }

    private fun result(id: Long = 10) =
        SubscriptionEpisodeResult(
            episode = PodcastEpisode(id = id, podcastId = 1, title = "Episodio $id"),
            podcast = podcast(),
        )

    private fun setResults(state: SubscriptionSearchUi) {
        compose.setContent {
            TolochaTheme {
                SubscriptionSearchResults(
                    state = state,
                    favoriteIds = emptySet(),
                    playerUi = EpisodePlayerUi(),
                    downloads = emptyMap(),
                    downloadProgress = emptyMap(),
                    downloadsEnabled = false,
                    onPlayPause = { _, _ -> },
                    onOpenDetail = { _, _ -> },
                    onToggleFavorite = {},
                    onQueueNext = {},
                    onQueueEnd = {},
                    onDownload = {},
                    onTogglePlayed = {},
                    onLoadMore = {},
                    onRetry = {},
                )
            }
        }
    }

    @Test
    fun muestraElEstadoDeCarga() {
        setResults(
            SubscriptionSearchUi(
                submittedTerm = "hola",
                scope = SubscriptionSearchScope.EPISODES,
                loading = true,
                showResults = true,
            ),
        )

        compose.onNodeWithText("Buscando episodios…").assertExists()
    }

    @Test
    fun muestraElVacioConElTermino() {
        setResults(
            SubscriptionSearchUi(
                submittedTerm = "hola",
                scope = SubscriptionSearchScope.EPISODES,
                showResults = true,
            ),
        )

        compose.onNodeWithText("Sin resultados para «hola»", substring = true).assertExists()
    }

    @Test
    fun muestraElErrorYElReintento() {
        setResults(
            SubscriptionSearchUi(
                submittedTerm = "hola",
                scope = SubscriptionSearchScope.EPISODES,
                error = "No se pudieron buscar episodios.",
                showResults = true,
            ),
        )

        compose.onNodeWithTag(TolochaTestTags.SUBSCRIPTIONS_SEARCH_ERROR).assertExists()
        compose.onNodeWithText("Reintentar").assertExists()
    }

    @Test
    fun muestraResultadosYCargaMas() {
        var loaded = false
        compose.setContent {
            TolochaTheme {
                SubscriptionSearchResults(
                    state =
                        SubscriptionSearchUi(
                            submittedTerm = "hola",
                            scope = SubscriptionSearchScope.EPISODES,
                            results = listOf(result()),
                            showResults = true,
                            hasMore = true,
                        ),
                    favoriteIds = emptySet(),
                    playerUi = EpisodePlayerUi(),
                    downloads = emptyMap(),
                    downloadProgress = emptyMap(),
                    downloadsEnabled = false,
                    onPlayPause = { _, _ -> },
                    onOpenDetail = { _, _ -> },
                    onToggleFavorite = {},
                    onQueueNext = {},
                    onQueueEnd = {},
                    onDownload = {},
                    onTogglePlayed = {},
                    onLoadMore = { loaded = true },
                    onRetry = {},
                )
            }
        }

        compose.onNodeWithText("Episodio 10").assertExists()
        compose.onNodeWithText("Episodios para «hola»").assertExists()
        compose.onNodeWithText("Cargar más").performClick()

        assertTrue(loaded)
    }
}
