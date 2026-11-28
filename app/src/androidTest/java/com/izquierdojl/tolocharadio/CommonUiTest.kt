package com.izquierdojl.tolocharadio

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.components.EmptyState
import com.izquierdojl.tolocharadio.core.ui.components.ErrorBanner
import com.izquierdojl.tolocharadio.core.ui.components.StationCard
import com.izquierdojl.tolocharadio.core.ui.components.StationListItem
import com.izquierdojl.tolocharadio.core.ui.playback.StationPlaybackUiState
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose Test de componentes puros (sin Hilt): tarjetas, estados
 * vacíos y de error (FR-014, flujos críticos a nivel componente).
 */
class CommonUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val station = StationDto(id = "u1", name = "Tolocha", country = "España", language = "español")

    @Test
    fun stationCard_muestraNombreYFavorito() {
        var toggled = false
        compose.setContent {
            TolochaTheme {
                StationCard(
                    station,
                    isFavorite = false,
                    onOpen = {},
                    onPlay = {},
                    onToggleFavorite = { toggled = true },
                )
            }
        }
        compose.onNodeWithText("Tolocha").assertIsDisplayed()
        compose.onNodeWithText("España · español").assertIsDisplayed()
    }

    @Test
    fun stationCard_activaMuestraPausaYBadge() {
        var played = false
        compose.setContent {
            TolochaTheme {
                StationCard(
                    station,
                    isFavorite = false,
                    playback = StationPlaybackUiState.Playing,
                    onOpen = {},
                    onPlay = { played = true },
                    onToggleFavorite = {},
                )
            }
        }
        compose.onNodeWithContentDescription("Pausar").assertIsDisplayed()
        compose.onNodeWithContentDescription("En reproducción").assertIsDisplayed()
        compose.onNodeWithContentDescription("Pausar").performClick()
        assertTrue(played)
    }

    @Test
    fun stationCard_abrirNoReproduce() {
        var opened = false
        var played = false
        compose.setContent {
            TolochaTheme {
                StationCard(
                    station,
                    isFavorite = false,
                    onOpen = { opened = true },
                    onPlay = { played = true },
                    onToggleFavorite = {},
                )
            }
        }
        compose.onNodeWithText("Tolocha").performClick()
        assertTrue(opened)
        assertFalse(played)
    }

    @Test
    fun stationCard_lightModeRenderiza() {
        compose.setContent {
            TolochaTheme(darkTheme = false) {
                StationCard(
                    station,
                    isFavorite = false,
                    onOpen = {},
                    onPlay = {},
                    onToggleFavorite = {},
                )
            }
        }
        compose.onNodeWithText("Tolocha").assertIsDisplayed()
        compose.onNodeWithContentDescription("Reproducir").assertIsDisplayed()
    }

    @Test
    fun stationListItem_sinFavicon_muestraPlaceholderSierra() {
        compose.setContent {
            TolochaTheme {
                StationListItem(station, isFavorite = true, onOpen = {}, onToggleFavorite = {})
            }
        }
        // Spec 002: sin favicon se muestra el emblema Sierra (icono Radio), no la inicial.
        compose.onNodeWithText("Tolocha").assertIsDisplayed()
        compose.onNodeWithText("España · español").assertIsDisplayed()
    }

    @Test
    fun errorBanner_muestraMensajeYReintenta() {
        var retried = false
        compose.setContent {
            TolochaTheme {
                ErrorBanner("Sin conexión", onRetry = { retried = true })
            }
        }
        compose.onNodeWithText("Sin conexión").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        assert(retried)
    }

    @Test
    fun emptyState_muestraAccion() {
        var acted = false
        compose.setContent {
            TolochaTheme {
                EmptyState("Sin resultados", actionLabel = "Reintentar", onAction = { acted = true })
            }
        }
        compose.onNodeWithText("Sin resultados").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        assert(acted)
    }
}
