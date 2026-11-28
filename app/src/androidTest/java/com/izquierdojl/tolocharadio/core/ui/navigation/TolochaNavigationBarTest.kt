package com.izquierdojl.tolocharadio.core.ui.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.domain.podcasts.QueueBadge
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Regresión del bug 0040 y spec 0061: etiquetas visibles, selección clara,
 * un único anuncio accesible y reparto de ancho de los destinos.
 */
class TolochaNavigationBarTest {
    @get:Rule
    val compose = createComposeRule()

    private val labels = listOf("Explorar", "Favoritos", "Historial", "Mis emisoras")
    private val routes =
        listOf(Routes.EXPLORE, Routes.FAVORITES, Routes.HISTORY, Routes.CUSTOM_STATIONS)

    @Test
    fun todosLosDestinosSonVisiblesYRepartenElAncho() {
        compose.setContent {
            TolochaTheme {
                TolochaNavigationBar(currentRoute = Routes.EXPLORE, onNavigate = {})
            }
        }

        labels.forEach { label ->
            compose.onNodeWithText(label).assertIsDisplayed()
            compose.onAllNodesWithContentDescription(label).assertCountEquals(0)
        }

        val rootWidth = compose.onRoot().fetchSemanticsNode().boundsInRoot.width
        val centers =
            routes.map { route ->
                val bounds = compose.onNodeWithTag("bottom-dest-$route").fetchSemanticsNode().boundsInRoot
                assertTrue("$route no tiene ancho", bounds.width > 0f)
                bounds.center.x
            }

        for (i in 1 until centers.size) {
            assertTrue(
                "Los destinos no están repartidos: ${routes[i - 1]} (${centers[i - 1]}) >= ${routes[i]} (${centers[i]})",
                centers[i] > centers[i - 1],
            )
        }
        assertTrue(
            "Los destinos no ocupan el ancho de la barra (${centers.last() - centers.first()} de $rootWidth)",
            centers.last() - centers.first() > rootWidth * 0.5f,
        )
    }

    @Test
    fun podcastMuestraLasCincoEtiquetas() {
        val podcastLabels = listOf("Inicio", "Descubrir", "Suscripciones", "Cola", "Favoritos")
        compose.setContent {
            TolochaTheme {
                TolochaNavigationBar(
                    currentRoute = Routes.PODCAST_HOME,
                    onNavigate = {},
                    dests = PODCAST_DESTS,
                )
            }
        }
        podcastLabels.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun etiquetasConFuente200SiguenPresentes() {
        compose.setContent {
            TolochaTheme {
                CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                    TolochaNavigationBar(currentRoute = Routes.EXPLORE, onNavigate = {})
                }
            }
        }
        labels.forEach { compose.onNodeWithText(it).assertExists() }
    }

    @Test
    fun laInsigniaDeColaMuestraLosPendientes() {
        compose.setContent {
            TolochaTheme {
                TolochaNavigationBar(
                    currentRoute = Routes.PODCAST_QUEUE,
                    onNavigate = {},
                    dests = PODCAST_DESTS,
                    queueBadge = QueueBadge(3),
                )
            }
        }

        compose
            .onNode(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "3 episodios en cola",
                ),
            ).assertExists()
    }
}
