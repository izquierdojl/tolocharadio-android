package com.izquierdojl.tolocharadio.feature.podcasts.discover

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSearchResult
import com.izquierdojl.tolocharadio.domain.podcasts.TopCountry
import com.izquierdojl.tolocharadio.domain.podcasts.TopGenre
import com.izquierdojl.tolocharadio.feature.podcasts.TopChartsUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Contrato visual de la sección «Populares» (spec 0053, US1–US3). */
class TopChartsSectionTest {
    @get:Rule
    val compose = createComposeRule()

    private val item =
        PodcastSearchResult(
            title = "Podcast A",
            author = "Autor",
            feedUrl = "https://a.example/f.xml",
            categories = listOf("Rock"),
        )

    private fun content(
        ui: TopChartsUi,
        subscribed: Set<String> = emptySet(),
        onToggle: (PodcastSearchResult) -> Unit = {},
        onRetry: () -> Unit = {},
        onEditServer: () -> Unit = {},
        onCountry: (TopCountry) -> Unit = {},
        onGenre: (TopGenre) -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                TopChartsSection(
                    ui = ui,
                    subscribedFeedUrls = subscribed,
                    onCountryChange = onCountry,
                    onGenreChange = onGenre,
                    onToggleSubscribe = onToggle,
                    onRetry = onRetry,
                    onEditServer = onEditServer,
                )
            }
        }
    }

    @Test
    fun cargando_muestraIndicadorYTexto() {
        content(TopChartsUi(visible = true, loading = true))

        compose.onNodeWithText("Cargando ranking…").assertIsDisplayed()
    }

    @Test
    fun contenido_muestraPosicionYTituloYPermiteSuscribirse() {
        var subscribed = false
        content(
            TopChartsUi(visible = true, items = listOf(item)),
            onToggle = { subscribed = true },
        )

        compose.onNodeWithText("Populares").assertIsDisplayed()
        compose.onNodeWithText("Podcast A").assertIsDisplayed()
        compose.onNodeWithText("1").assertIsDisplayed()
        compose.onNodeWithText("Suscribirse").performClick()
        assertTrue(subscribed)
    }

    @Test
    fun vacio_muestraMensajeEspecifico() {
        content(TopChartsUi(visible = true, items = emptyList()))

        compose.onNodeWithText("No hay ranking disponible para esta selección.").assertIsDisplayed()
    }

    @Test
    fun errorDeRed_muestraReintentar() {
        var retried = false
        content(
            TopChartsUi(visible = true, error = "No se pudo cargar"),
            onRetry = { retried = true },
        )

        compose.onNodeWithText("No se pudo cargar").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        assertTrue(retried)
    }

    @Test
    fun errorDeCredenciales_muestraEditarServidorSinReintentar() {
        var edited = false
        content(
            TopChartsUi(visible = true, error = "Revisa credenciales", isAuthError = true),
            onEditServer = { edited = true },
        )

        compose.onNodeWithText("Editar servidor").performClick()
        compose.onAllNodesWithText("Reintentar").assertCountEquals(0)
        assertTrue(edited)
    }

    @Test
    fun avisoDeCategoria_muestraReintentar() {
        var retried = false
        content(
            TopChartsUi(visible = true, items = listOf(item), genreUnavailable = true),
            onRetry = { retried = true },
        )

        compose
            .onNodeWithText("El ranking por categoría no está disponible ahora. Mostrando el ranking general.")
            .assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        assertTrue(retried)
    }

    @Test
    fun selectorDePais_cambiaYPersistePorCallback() {
        var selected: TopCountry? = null
        content(TopChartsUi(visible = true), onCountry = { selected = it })

        compose.onNodeWithTag(TolochaTestTags.TOP_CHARTS_COUNTRY).performClick()
        compose.onNodeWithText("México").performClick()

        assertEquals(TopCountry.MEXICO, selected)
    }
}
