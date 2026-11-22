package com.izquierdojl.tolocharadio.feature.podcasts.discover

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSearchResult
import com.izquierdojl.tolocharadio.feature.podcasts.DiscoverUi
import com.izquierdojl.tolocharadio.feature.podcasts.TopChartsUi
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Pantalla Descubrir sin Hilt: ranking vs búsqueda y controles (spec 0053). */
class DiscoverContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val rankingItem =
        PodcastSearchResult("Podcast ranking", "Autor", "https://ranking.example/f.xml")
    private val searchItem =
        PodcastSearchResult("Resultado búsqueda", "Autor", "https://search.example/f.xml")

    private fun content(
        ui: DiscoverUi = DiscoverUi(),
        top: TopChartsUi = TopChartsUi(visible = true),
        onEditServer: () -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                DiscoverContent(
                    ui = ui,
                    topChartsUi = top,
                    manualUrl = "",
                    onQueryChange = {},
                    onSearch = {},
                    onManualUrlChange = {},
                    onAddManual = {},
                    onImportOpml = {},
                    onExportOpml = {},
                    onBrowseSubscriptions = {},
                    onToggleSubscribe = {},
                    onCountryChange = {},
                    onGenreChange = {},
                    onRetryTopCharts = {},
                    onEditServer = onEditServer,
                )
            }
        }
    }

    @Test
    fun sinBusqueda_muestraPopulares() {
        content(
            ui = DiscoverUi(query = "", submittedQuery = ""),
            top = TopChartsUi(visible = true, items = listOf(rankingItem)),
        )

        compose.onNodeWithText("Populares").assertIsDisplayed()
        compose.onNodeWithText("Podcast ranking").assertIsDisplayed()
    }

    @Test
    fun conBusqueda_muestraResultadosYOcultaPopulares() {
        content(
            ui = DiscoverUi(query = "jazz", submittedQuery = "jazz", results = listOf(searchItem)),
            top = TopChartsUi(visible = true, items = listOf(rankingItem)),
        )

        compose.onNodeWithText("Resultado búsqueda").assertIsDisplayed()
        compose.onAllNodesWithText("Populares").assertCountEquals(0)
    }

    @Test
    fun duranteLaCargaDelRanking_losControlesSiguenOperativos() {
        content(top = TopChartsUi(visible = true, loading = true))

        compose.onNodeWithText("Buscar").assertIsEnabled()
        compose.onNodeWithText("Importar OPML").assertIsEnabled()
        compose.onNodeWithText("Exportar OPML").assertIsEnabled()
    }

    @Test
    fun errorDeCredenciales_abreEditarServidor() {
        var edited = false
        content(
            top = TopChartsUi(visible = true, error = "Revisa credenciales", isAuthError = true),
            onEditServer = { edited = true },
        )

        compose.onNodeWithText("Editar servidor").performClick()
        assertTrue(edited)
    }
}
