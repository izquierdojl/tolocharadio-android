package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import org.junit.Rule
import org.junit.Test

/** Botón de descarga con porcentaje (US6). */
class DownloadProgressButtonTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun muestraElPorcentajeDescargado() {
        compose.setContent {
            TolochaTheme {
                DownloadProgressButton(DownloadUiState.Downloading(0.45f), onDownload = {})
            }
        }

        compose.onNodeWithText("45%").assertIsDisplayed()
        compose.onNodeWithContentDescription("Descargando 45 %").assertExists()
    }

    @Test
    fun muestraAnimacionIndeterminadaSinTotal() {
        compose.setContent {
            TolochaTheme {
                DownloadProgressButton(DownloadUiState.Downloading(null), onDownload = {})
            }
        }

        compose.onNodeWithContentDescription("Descargando…").assertExists()
    }

    @Test
    fun muestraElEstadoDescargado() {
        compose.setContent {
            TolochaTheme {
                DownloadProgressButton(DownloadUiState.Downloaded, onDownload = {})
            }
        }

        compose.onNodeWithContentDescription("Descargado").assertExists()
    }

    @Test
    fun elEstadoInicialOfreceDescargar() {
        compose.setContent {
            TolochaTheme {
                DownloadProgressButton(DownloadUiState.NotDownloaded, onDownload = {})
            }
        }

        compose.onNodeWithContentDescription("Descargar").assertExists()
    }
}
