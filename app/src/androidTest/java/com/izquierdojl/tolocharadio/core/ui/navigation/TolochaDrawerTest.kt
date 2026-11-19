package com.izquierdojl.tolocharadio.core.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.feature.player.SleepTimerUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Cajón con Temporizador, Servidores y Configuración (US3). */
class TolochaDrawerTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun muestraLasTresEntradasYNavega() {
        var servers = false
        var settings = false
        compose.setContent {
            TolochaTheme {
                TolochaDrawerContent(
                    sleepTimerUiState = SleepTimerUiState.Inactive,
                    onStartTimer = { },
                    onCancelTimer = { },
                    onServers = { servers = true },
                    onSettings = { settings = true },
                )
            }
        }

        compose.onNodeWithText("Temporizador").assertIsDisplayed()
        compose.onNodeWithText("Servidores").assertIsDisplayed()
        compose.onNodeWithText("Configuración").assertIsDisplayed()

        compose.onNodeWithText("Servidores").performClick()
        assertTrue(servers)
        compose.onNodeWithText("Configuración").performClick()
        assertTrue(settings)
    }

    @Test
    fun elTemporizadorActivoMuestraMinutosYCancelacion() {
        var cancelled = false
        compose.setContent {
            TolochaTheme {
                var state by remember { mutableStateOf<SleepTimerUiState>(SleepTimerUiState.Active(15)) }
                TolochaDrawerContent(
                    sleepTimerUiState = state,
                    onStartTimer = { },
                    onCancelTimer = {
                        cancelled = true
                        state = SleepTimerUiState.Inactive
                    },
                    onServers = { },
                    onSettings = { },
                )
            }
        }

        compose.onNodeWithText("Temporizador").performClick()
        compose.onNodeWithText("Quedan 15 minutos").assertIsDisplayed()
        compose.onNodeWithText("Cancelar temporizador").performClick()
        assertTrue(cancelled)
    }
}
