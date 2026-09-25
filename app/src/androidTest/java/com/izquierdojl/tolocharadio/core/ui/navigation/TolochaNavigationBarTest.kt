package com.izquierdojl.tolocharadio.core.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Regresión del bug 0040: al envolver `NavigationBarItem` en `TooltipBox` se
 * perdía el `Modifier.weight(1f)` del item, el primer destino ocupaba todo el
 * ancho y los otros cuatro quedaban a ancho 0 (invisibles).
 *
 * El test compone la barra real [TolochaNavigationBar] y exige que los cinco
 * destinos sean visibles, tengan ancho > 0 y se repartan horizontalmente.
 */
class TolochaNavigationBarTest {
    @get:Rule
    val compose = createComposeRule()

    private val labels = listOf("Explorar", "Favoritos", "Historial", "Mis emisoras", "Configuración")

    @Test
    fun todosLosDestinosSonVisiblesYRepartenElAncho() {
        compose.setContent {
            TolochaTheme {
                TolochaNavigationBar(currentRoute = Routes.EXPLORE, onNavigate = {})
            }
        }

        labels.forEach { label -> compose.onNodeWithContentDescription(label).assertIsDisplayed() }

        val rootWidth = compose.onRoot().fetchSemanticsNode().boundsInRoot.width
        val centers =
            labels.map { label ->
                val bounds = compose.onNodeWithContentDescription(label).fetchSemanticsNode().boundsInRoot
                assertTrue("$label no tiene ancho", bounds.width > 0f)
                bounds.center.x
            }

        for (i in 1 until centers.size) {
            assertTrue(
                "Los destinos no están repartidos: ${labels[i - 1]} (${centers[i - 1]}) >= ${labels[i]} (${centers[i]})",
                centers[i] > centers[i - 1],
            )
        }
        assertTrue(
            "Los destinos no ocupan el ancho de la barra (${centers.last() - centers.first()} de $rootWidth)",
            centers.last() - centers.first() > rootWidth * 0.5f,
        )
    }
}
