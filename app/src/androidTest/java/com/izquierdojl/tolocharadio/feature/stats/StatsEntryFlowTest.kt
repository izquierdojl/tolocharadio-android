package com.izquierdojl.tolocharadio.feature.stats

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.domain.stats.StatsPeriod
import com.izquierdojl.tolocharadio.feature.settings.SettingsActionRow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Flujo crítico de UI (FR-001/FR-007): la fila "Gráficas" de
 * Configuración abre la vista y el estado de error ofrece reintento.
 * Componentes puros sin Hilt (patrón de `CommonUiTest`).
 */
class StatsEntryFlowTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun filaGraficas_enConfiguracion_abreLaVista() {
        var opened = false
        compose.setContent {
            TolochaTheme {
                SettingsActionRow(
                    label = "Gráficas",
                    description = "Ver gráficas de escucha",
                    onClick = { opened = true },
                )
            }
        }

        compose.onNodeWithText("Gráficas").assertIsDisplayed().performClick()
        assertTrue(opened)
    }

    @Test
    fun estadoDeError_ofreceReintento() {
        var retried = false
        compose.setContent {
            TolochaTheme {
                StatsScreenContent(
                    state =
                        StatsUiState.Error(
                            "Servicio no disponible. Comprueba tu conexión e inténtalo de nuevo.",
                        ),
                    period = StatsPeriod.THIRTY,
                    onPeriodChange = {},
                    onRetry = { retried = true },
                )
            }
        }

        compose.onNodeWithText("Tus estadísticas").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        assertTrue(retried)
    }
}
