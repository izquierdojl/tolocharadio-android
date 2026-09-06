package com.izquierdojl.tolocharadio.feature.favorites

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.FavoriteDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import org.junit.Rule
import org.junit.Test

/**
 * Compose Test de Favoritos sobre el contenido sin estado (sin Hilt):
 * lista, vacío, error, deshacer y asa de reorden (US-1–US-3).
 */
class FavoritesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val favs =
        listOf(
            FavoriteDto(StationDto("u1", "Uno", country = "España", language = "español"), 1000),
            FavoriteDto(StationDto("u2", "Dos"), 2000),
        )

    private fun screen(
        state: FavoritesUiState,
        onStation: (String) -> Unit = {},
        onExplore: () -> Unit = {},
        onPlay: (FavoriteDto) -> Unit = {},
        onRemove: (String) -> Unit = {},
        onMove: (Int, Int) -> Unit = { _, _ -> },
        onCommit: () -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                FavoritesScreenContent(
                    state = state,
                    actions =
                        FavoriteListActions(
                            onStation = onStation,
                            onExplore = onExplore,
                            onPlay = onPlay,
                            onRemove = onRemove,
                            onMove = onMove,
                            onCommit = onCommit,
                            onRetry = onRetry,
                        ),
                )
            }
        }
    }

    @Test
    fun lista_muestraFavoritasConAsaYAcciones() {
        screen(FavoritesUiState.Content(favs))
        compose.onNodeWithText("Uno").assertIsDisplayed()
        compose.onNodeWithText("España · español").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Reordenar").assertCountEquals(2)
        compose.onAllNodesWithContentDescription("Reproducir").assertCountEquals(2)
        compose.onAllNodesWithContentDescription("Quitar de favoritos").assertCountEquals(2)
    }

    @Test
    fun vacio_muestraMensajeYAccesoAExplorar() {
        var explored = false
        screen(FavoritesUiState.Empty, onExplore = { explored = true })
        compose.onNodeWithText("Aún no tienes favoritas").assertIsDisplayed()
        compose.onNodeWithText("Explorar").performClick()
        assert(explored)
    }

    @Test
    fun error_muestraMensajeYReintenta() {
        var retried = false
        screen(FavoritesUiState.Error("Sin conexión"), onRetry = { retried = true })
        compose.onNodeWithText("Sin conexión").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        assert(retried)
    }

    @Test
    fun offline_muestraAvisoDeCache() {
        screen(FavoritesUiState.Content(favs, offline = true))
        compose.onNodeWithText("Mostrando caché sin conexión.").assertIsDisplayed()
    }

    @Test
    fun tarjeta_abreFichaAlPulsar() {
        var opened: String? = null
        screen(FavoritesUiState.Content(favs), onStation = { opened = it })
        compose.onNodeWithText("Uno").performClick()
        assert(opened == "u1")
    }

    @Test
    fun play_reproduceFavorita() {
        var played: String? = null
        screen(FavoritesUiState.Content(favs), onPlay = { played = it.station.id })
        compose.onAllNodesWithContentDescription("Reproducir")[0].performClick()
        assert(played == "u1")
    }

    @Test
    fun corazon_quitaFavorita() {
        var removed: String? = null
        screen(FavoritesUiState.Content(favs), onRemove = { removed = it })
        compose.onAllNodesWithContentDescription("Quitar de favoritos")[0].performClick()
        assert(removed == "u1")
    }
}
