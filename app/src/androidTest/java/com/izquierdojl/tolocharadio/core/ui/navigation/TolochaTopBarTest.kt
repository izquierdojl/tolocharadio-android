package com.izquierdojl.tolocharadio.core.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Cabecera con hamburguesa, logotipo adaptable, modo y Cast (US3). */
class TolochaTopBarTest {
    @get:Rule
    val compose = createComposeRule()

    private fun setTopBar(
        width: Int? = null,
        onOpenMenu: () -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                val content: @Composable () -> Unit = {
                    TolochaTopBar(
                        mode = ContentMode.RADIO,
                        podcastsEnabled = true,
                        onSelectMode = { },
                        onOpenMenu = onOpenMenu,
                        castAction = { Box(Modifier.size(48.dp)) },
                    )
                }
                if (width == null) {
                    content()
                } else {
                    Box(Modifier.width(width.dp)) { content() }
                }
            }
        }
    }

    @Test
    fun muestraHamburguesaModoYCast() {
        setTopBar()
        compose.onNodeWithContentDescription("Abrir menú").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cambiar a Podcasts").assertIsDisplayed()
        compose.onNodeWithText("TolochaRadio").assertExists()
    }

    @Test
    fun enAnchoEstrechoReduceElLogotipoAlEmblema() {
        setTopBar(width = 300)
        compose.onNodeWithText("TolochaRadio").assertDoesNotExist()
        compose.onNodeWithContentDescription("Emblema de TolochaRadio").assertExists()
    }

    @Test
    fun pulsarLaHamburguesaAbreElMenu() {
        var opened = false
        setTopBar(onOpenMenu = { opened = true })
        compose.onNodeWithContentDescription("Abrir menú").performClick()
        assertTrue(opened)
    }
}
