package com.izquierdojl.tolocharadio.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Regresión del bug 0043: con poco alto disponible, el contenido de
 * Configuración debe poder desplazarse hasta "Gráficas" y "Acerca de".
 */
class SettingsContentTest {
    @get:Rule
    val compose = createComposeRule()

    private fun content(
        onStats: () -> Unit = {},
        onAbout: () -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                Box(Modifier.fillMaxWidth().height(320.dp)) {
                    SettingsContent(
                        ui = SettingsUi(),
                        actions =
                            SettingsActions(
                                onStats = onStats,
                                onAbout = onAbout,
                            ),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    @Test
    fun conPocoAltoSePuedeLlegarAGraficasYAcercaDe() {
        var stats = false
        var about = false
        content(onStats = { stats = true }, onAbout = { about = true })

        compose.onNodeWithText("Gráficas").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(stats)

        compose.onNodeWithText("Acerca de").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(about)
    }
}
