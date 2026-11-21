package com.izquierdojl.tolocharadio.feature.podcasts.subscriptions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.feature.podcasts.SubscriptionSearchScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SubscriptionSearchBarTest {
    @get:Rule
    val compose = createComposeRule()

    private fun setBar(
        term: String = "",
        scope: SubscriptionSearchScope = SubscriptionSearchScope.PODCASTS,
        searching: Boolean = false,
        onScopeChange: (SubscriptionSearchScope) -> Unit = {},
        onClear: () -> Unit = {},
        onSubmit: () -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                SubscriptionSearchBar(
                    term = term,
                    scope = scope,
                    searching = searching,
                    onTermChange = {},
                    onScopeChange = onScopeChange,
                    onSubmit = onSubmit,
                    onClear = onClear,
                )
            }
        }
    }

    @Test
    fun muestraLaEtiquetaDelAmbitoPodcastsPorDefecto() {
        setBar()

        compose.onNodeWithText("Buscar en tus podcasts").assertExists()
        compose.onNodeWithText("Podcasts").assertIsSelected()
    }

    @Test
    fun cambiarAEpisodiosActualizaEtiquetaYMuestraBuscar() {
        var selected: SubscriptionSearchScope? = null
        compose.setContent {
            var scope by remember { mutableStateOf(SubscriptionSearchScope.PODCASTS) }
            TolochaTheme {
                SubscriptionSearchBar(
                    term = "hola",
                    scope = scope,
                    searching = false,
                    onTermChange = {},
                    onScopeChange = {
                        scope = it
                        selected = it
                    },
                    onSubmit = {},
                    onClear = {},
                )
            }
        }

        compose.onNodeWithText("Episodios").performClick()

        assertEquals(SubscriptionSearchScope.EPISODES, selected)
        compose.onNodeWithText("Buscar episodios en tus suscripciones").assertExists()
        compose.onNodeWithContentDescription("Buscar episodios").assertIsEnabled()
    }

    @Test
    fun buscarEstaDeshabilitadoConTerminoVacio() {
        setBar(term = "", scope = SubscriptionSearchScope.EPISODES)

        compose.onNodeWithContentDescription("Buscar episodios").assertIsNotEnabled()
    }

    @Test
    fun limpiarInvocaElCallback() {
        var cleared = false
        setBar(term = "hola", onClear = { cleared = true })

        compose.onNodeWithContentDescription("Limpiar búsqueda").performClick()

        assertTrue(cleared)
    }

    @Test
    fun elAmbitoMantieneUnAreaTactilDeAlMenos48Dp() {
        setBar()

        compose.onNodeWithTag(TolochaTestTags.SUBSCRIPTIONS_SEARCH_SCOPE)
            .assertHeightIsAtLeast(48.dp)
    }
}
