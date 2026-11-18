package com.izquierdojl.tolocharadio.core.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.components.ModeSwitchButton
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Navegación crítica modo Radio↔Podcasts (spec 0046, T029): el
 * alternador de la barra muestra el modo destino y propaga el cambio.
 */
class ModeNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun enRadioOfreceCambiarAPodcasts() {
        compose.setContent {
            TolochaTheme {
                ModeSwitchButton(mode = ContentMode.RADIO, onSelect = {})
            }
        }

        compose.onNodeWithContentDescription("Cambiar a Podcasts").assertIsDisplayed()
    }

    @Test
    fun enPodcastsOfreceCambiarARadio() {
        compose.setContent {
            TolochaTheme {
                ModeSwitchButton(mode = ContentMode.PODCAST, onSelect = {})
            }
        }

        compose.onNodeWithContentDescription("Cambiar a Radio").assertIsDisplayed()
    }

    @Test
    fun pulsarElAlternadorPropagaElCambio() {
        var selected = ContentMode.RADIO
        compose.setContent {
            TolochaTheme {
                ModeSwitchButton(mode = selected, onSelect = { selected = it })
            }
        }

        compose.onNodeWithContentDescription("Cambiar a Podcasts").performClick()
        assertEquals(ContentMode.PODCAST, selected)
    }
}
