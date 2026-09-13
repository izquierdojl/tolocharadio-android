package com.izquierdojl.tolocharadio.feature.favorites

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.izquierdojl.tolocharadio.core.ui.ViewMode
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.FavoriteDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import org.junit.Rule
import org.junit.Test

/**
 * Compose Test de Favoritos sobre el contenido sin estado (sin Hilt):
 * lista, vacío, error, deshacer, reorden por arrastre y menú accesible
 * (US-1–US-3).
 */
class FavoritesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val favs =
        listOf(
            FavoriteDto(StationDto("u1", "Uno", country = "España", language = "español"), 1000),
            FavoriteDto(StationDto("u2", "Dos"), 2000),
        )

    private val manyFavs =
        listOf(
            FavoriteDto(StationDto("u1", "Uno"), 1000),
            FavoriteDto(StationDto("u2", "Dos"), 2000),
            FavoriteDto(StationDto("u3", "Tres"), 3000),
            FavoriteDto(StationDto("u4", "Cuatro"), 4000),
            FavoriteDto(StationDto("u5", "Cinco"), 5000),
        )

    private fun screen(
        state: FavoritesUiState,
        mode: ViewMode = ViewMode.LIST,
        onStation: (String) -> Unit = {},
        onExplore: () -> Unit = {},
        onPlay: (FavoriteDto) -> Unit = {},
        onRemove: (String) -> Unit = {},
        onMove: (Int, Int) -> Unit = { _, _ -> },
        onCommit: () -> Unit = {},
        onMoveUp: (String) -> Unit = {},
        onMoveDown: (String) -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                FavoritesScreenContent(
                    state = state,
                    mode = mode,
                    actions =
                        FavoriteListActions(
                            onStation = onStation,
                            onExplore = onExplore,
                            onPlay = onPlay,
                            onRemove = onRemove,
                            onMove = onMove,
                            onCommit = onCommit,
                            onMoveUp = onMoveUp,
                            onMoveDown = onMoveDown,
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
    fun offline_muestraAvisoDeCacheYSinReorden() {
        screen(FavoritesUiState.Content(favs, offline = true))
        compose.onNodeWithText("Mostrando caché sin conexión.").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Reordenar").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Más opciones").assertCountEquals(0)
    }

    @Test
    fun unaSolaFavorita_noMuestraControlesDeReorden() {
        screen(FavoritesUiState.Content(listOf(favs.first())))
        compose.onAllNodesWithContentDescription("Reordenar").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Más opciones").assertCountEquals(0)
    }

    @Test
    fun grid_noMuestraAsa() {
        screen(FavoritesUiState.Content(favs), mode = ViewMode.GRID)
        compose.onAllNodesWithContentDescription("Reordenar").assertCountEquals(0)
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

    @Test
    fun arrastreEnAsa_mueveYConfirma() {
        var moved = false
        var committed = false
        screen(
            FavoritesUiState.Content(manyFavs),
            onMove = { _, _ -> moved = true },
            onCommit = { committed = true },
        )
        compose.onAllNodesWithContentDescription("Reordenar")[0].performTouchInput {
            down(center)
            advanceEventTime(16)
            moveBy(Offset(0f, 60f))
            advanceEventTime(16)
            moveBy(Offset(0f, 160f))
            advanceEventTime(16)
            moveBy(Offset(0f, 160f))
            advanceEventTime(16)
            up()
        }
        compose.waitForIdle()
        assert(committed)
        assert(moved)
    }

    @Test
    fun arrastreInmediatoSinPulsacionLarga_mueve() {
        var moved = false
        screen(FavoritesUiState.Content(manyFavs), onMove = { _, _ -> moved = true })
        compose.onAllNodesWithContentDescription("Reordenar")[0].performTouchInput {
            down(center)
            moveBy(Offset(0f, 50f))
            moveBy(Offset(0f, 200f))
            up()
        }
        compose.waitForIdle()
        assert(moved)
    }

    @Test
    fun toqueEnLaFila_noIniciaReorden() {
        var moved = false
        var committed = false
        screen(
            FavoritesUiState.Content(favs),
            onMove = { _, _ -> moved = true },
            onCommit = { committed = true },
        )
        compose.onNodeWithText("Uno").performClick()
        compose.waitForIdle()
        assert(!moved)
        assert(!committed)
    }

    @Test
    fun menu_mueveYDeshabilitaExtremos() {
        var up: String? = null
        var down: String? = null
        screen(FavoritesUiState.Content(favs), onMoveUp = { up = it }, onMoveDown = { down = it })

        compose.onAllNodesWithContentDescription("Más opciones")[0].performClick()
        compose.onNodeWithText("Mover arriba").assertIsNotEnabled()
        compose.onNodeWithText("Mover abajo").performClick()
        assert(down == "u1")

        compose.onAllNodesWithContentDescription("Más opciones")[1].performClick()
        compose.onNodeWithText("Mover abajo").assertIsNotEnabled()
        compose.onNodeWithText("Mover arriba").performClick()
        assert(up == "u2")
    }
}
