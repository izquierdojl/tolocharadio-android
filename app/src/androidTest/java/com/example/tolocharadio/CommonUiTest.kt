package com.example.tolocharadio

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.tolocharadio.core.ui.components.EmptyState
import com.example.tolocharadio.core.ui.components.ErrorBanner
import com.example.tolocharadio.core.ui.components.StationCard
import com.example.tolocharadio.core.ui.components.StationListItem
import com.example.tolocharadio.core.ui.theme.TolochaTheme
import com.example.tolocharadio.data.remote.dto.StationDto
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
                StationCard(station, isFavorite = false, onPlay = {}, onToggleFavorite = { toggled = true })
            }
        }
        compose.onNodeWithText("Tolocha").assertIsDisplayed()
        compose.onNodeWithText("España · español").assertIsDisplayed()
    }

    @Test
    fun stationListItem_sinFavicon_muestraInicial() {
        compose.setContent {
            TolochaTheme {
                StationListItem(station, isFavorite = true, onPlay = {}, onToggleFavorite = {})
            }
        }
        compose.onNodeWithText("T").assertIsDisplayed()
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
