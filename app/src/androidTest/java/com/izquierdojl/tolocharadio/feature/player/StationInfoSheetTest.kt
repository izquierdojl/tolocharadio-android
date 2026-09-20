package com.izquierdojl.tolocharadio.feature.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import org.junit.Rule
import org.junit.Test

/**
 * Compose Test de la ficha de emisora (sin Hilt): sección "Enlace"
 * con Compartir + Copiar (spec 0039, US2, contratos C3).
 */
class StationInfoSheetTest {
    @get:Rule
    val compose = createComposeRule()

    private fun sheet(url: String) {
        compose.setContent {
            TolochaTheme {
                StationInfoSheet(
                    station = StationDto("s1", "Test", url = url),
                    onDismiss = {},
                )
            }
        }
    }

    @Test
    fun enlace_muestraCompartirYCopiar() {
        sheet("https://ejemplo.com/stream")
        compose.onNodeWithText("Enlace").assertIsDisplayed()
        compose.onNodeWithText("Compartir").assertIsDisplayed()
        compose.onNodeWithText("Copiar").assertIsDisplayed()
    }

    @Test
    fun sinEnlace_muestraNoDisponible() {
        sheet("")
        compose.onNodeWithText("Enlace").assertIsDisplayed()
        compose.onNodeWithText("Enlace no disponible").assertIsDisplayed()
    }

    @Test
    fun copiar_noBloqueaLaFicha() {
        sheet("https://ejemplo.com/stream")
        compose.onNodeWithText("Copiar").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Cerrar").assertIsDisplayed()
    }
}
