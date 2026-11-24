package com.izquierdojl.tolocharadio.feature.podcasts.discover

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
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
        height: Dp? = null,
    ) {
        compose.setContent {
            TolochaTheme {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .then(if (height != null) Modifier.height(height) else Modifier.fillMaxSize()),
                ) {
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
                        modifier = Modifier.fillMaxSize(),
                    )
                }
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

    /** Regresión del bug 0059: con poco alto, búsqueda y selectores comparten scroll. */
    @Test
    fun conPocoAlto_seDesplazaTodaLaPantallaHastaElFinalDelRanking() {
        val items =
            (1..8).map { index ->
                PodcastSearchResult("Podcast $index", "Autor", "https://ranking.example/$index.xml")
            }
        content(top = TopChartsUi(visible = true, items = items), height = 320.dp)
        compose.onNodeWithText("Buscar podcasts").assertIsDisplayed()

        compose
            .onNodeWithTag(TolochaTestTags.TOP_CHARTS_LIST)
            .performScrollToNode(hasTestTag(TolochaTestTags.TOP_CHARTS_COUNTRY))
        compose.onNodeWithTag(TolochaTestTags.TOP_CHARTS_COUNTRY).assertIsDisplayed()

        compose
            .onNodeWithTag(TolochaTestTags.TOP_CHARTS_LIST)
            .performScrollToNode(hasText("Podcast 8"))
        compose.onNodeWithText("Podcast 8").assertIsDisplayed()
        compose.onAllNodesWithText("Buscar podcasts").assertCountEquals(0)
    }
}
