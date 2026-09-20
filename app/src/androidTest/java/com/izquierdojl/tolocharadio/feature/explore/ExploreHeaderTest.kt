package com.izquierdojl.tolocharadio.feature.explore

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.izquierdojl.tolocharadio.core.ui.components.SectionHeader
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import org.junit.Rule
import org.junit.Test

/**
 * Contrato del encabezado de Explorar (spec 0039, US3, FR-007): la pantalla
 * completa requiere Hilt y no se puede componer aquí; este test fija los
 * textos exactos que `ExploreScreen` debe pasar a `SectionHeader`.
 */
class ExploreHeaderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun encabezado_muestraTituloYSubtitulo() {
        compose.setContent {
            TolochaTheme {
                SectionHeader(
                    title = "Explorar",
                    subtitle = "Descubre emisoras de todo el mundo.",
                )
            }
        }
        compose.onNodeWithText("Explorar").assertIsDisplayed()
        compose.onNodeWithText("Descubre emisoras de todo el mundo.").assertIsDisplayed()
    }
}
