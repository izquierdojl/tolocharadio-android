package com.izquierdojl.tolocharadio.feature.explore

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.izquierdojl.tolocharadio.core.ui.ViewMode
import com.izquierdojl.tolocharadio.core.ui.components.ViewModeToggle
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import org.junit.Rule
import org.junit.Test

/**
 * Compose Test del alternador lista/tarjetas (spec 008, US-1 y US-3):
 * icono = modo destino (FR-002), cambio sin recargar (FR-003) e
 * invariantes de contexto (FR-007).
 */
class ViewModeToggleTest {
    @get:Rule
    val compose = createComposeRule()

    private val stations =
        listOf(
            StationDto("u1", "Uno", country = "España", language = "español"),
            StationDto("u2", "Dos"),
        )

    @Test
    fun toggle_enListaAnunciaElModoDestinoYAlterna() {
        var toggles = 0
        compose.setContent {
            TolochaTheme {
                ViewModeToggle(mode = ViewMode.LIST, onToggle = { toggles++ })
            }
        }
        compose.onNodeWithContentDescription("Cambiar a vista de tarjetas").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cambiar a vista de tarjetas").performClick()
        assert(toggles == 1)
    }

    @Test
    fun toggle_enTarjetasAnunciaElModoDestino() {
        compose.setContent {
            TolochaTheme {
                ViewModeToggle(mode = ViewMode.GRID, onToggle = {})
            }
        }
        compose.onNodeWithContentDescription("Cambiar a vista de lista").assertIsDisplayed()
    }

    @Test
    fun contenido_cambiaDeListaATarjetasSinRecargar() {
        var mode by mutableStateOf(ViewMode.LIST)
        var loads = 0
        compose.setContent {
            TolochaTheme {
                ExploreContent(
                    ui =
                        ExploreUiState.Content(
                            items = stations,
                            favorites = emptySet(),
                            hasMore = false,
                        ),
                    mode = mode,
                    onStation = {},
                    onToggleFavorite = {},
                    onRetry = {},
                    onLoadMore = { loads++ },
                )
            }
        }
        // Lista: el play es el click de la fila, sin botón "Reproducir".
        compose.onAllNodesWithContentDescription("Reproducir").assertCountEquals(0)
        compose.onNodeWithText("Uno").assertIsDisplayed()
        compose.runOnIdle { mode = ViewMode.GRID }
        // Tarjetas: cada card trae su botón "Reproducir" (sin recarga de datos).
        compose.onAllNodesWithContentDescription("Reproducir").assertCountEquals(2)
        compose.onNodeWithText("Uno").assertIsDisplayed()
        assert(loads == 0)
    }

    @Test
    fun contenido_tarjetasConservaBusquedaYPaginados() {
        var mode by mutableStateOf(ViewMode.GRID)
        compose.setContent {
            TolochaTheme {
                ExploreContent(
                    ui =
                        ExploreUiState.Content(
                            items = stations,
                            favorites = emptySet(),
                            hasMore = true,
                            offlineCache = true,
                        ),
                    mode = mode,
                    onStation = {},
                    onToggleFavorite = {},
                    onRetry = {},
                    onLoadMore = {},
                )
            }
        }
        compose.onNodeWithText("Mostrando caché sin conexión.").assertIsDisplayed()
        compose.onNodeWithText("Cargar más").assertIsDisplayed()
        compose.runOnIdle { mode = ViewMode.LIST }
        compose.onNodeWithText("Mostrando caché sin conexión.").assertIsDisplayed()
        compose.onNodeWithText("Cargar más").assertIsDisplayed()
    }

    @Test
    fun contenido_vacioMantieneEmptyStateEnAmbosModos() {
        var mode by mutableStateOf(ViewMode.LIST)
        compose.setContent {
            TolochaTheme {
                ExploreContent(
                    ui = ExploreUiState.Content(items = emptyList(), favorites = emptySet(), hasMore = false),
                    mode = mode,
                    onStation = {},
                    onToggleFavorite = {},
                    onRetry = {},
                    onLoadMore = {},
                )
            }
        }
        compose.onNodeWithText("Sin resultados. Prueba con otra búsqueda.").assertIsDisplayed()
        compose.runOnIdle { mode = ViewMode.GRID }
        compose.onNodeWithText("Sin resultados. Prueba con otra búsqueda.").assertIsDisplayed()
    }

    @Test
    fun contenido_errorMantieneReintentoEnAmbosModos() {
        var mode by mutableStateOf(ViewMode.LIST)
        var retries = 0
        compose.setContent {
            TolochaTheme {
                ExploreContent(
                    ui = ExploreUiState.Error("Sin conexión"),
                    mode = mode,
                    onStation = {},
                    onToggleFavorite = {},
                    onRetry = { retries++ },
                    onLoadMore = {},
                )
            }
        }
        compose.onNodeWithText("Sin conexión").assertIsDisplayed()
        compose.runOnIdle { mode = ViewMode.GRID }
        compose.onNodeWithText("Reintentar").performClick()
        assert(retries == 1)
    }
}
