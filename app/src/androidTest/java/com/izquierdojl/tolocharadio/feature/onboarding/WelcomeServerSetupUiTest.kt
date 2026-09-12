package com.izquierdojl.tolocharadio.feature.onboarding

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Flujo crítico (constitución III): bienvenida bloqueante y alta del
 * primer servidor sin credenciales.
 */
@RunWith(AndroidJUnit4::class)
class WelcomeServerSetupUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun bienvenida_muestra_mensaje_y_no_pide_credenciales() {
        compose.setContent {
            TolochaTheme {
                WelcomeContent(
                    url = "",
                    alias = "",
                    state = SetupUiState.Idle(),
                    onUrlChange = {},
                    onAliasChange = {},
                    onConnect = {},
                )
            }
        }

        compose.onNodeWithText("Configura un servidor para acceder a tu contenido.").assertIsDisplayed()
        compose.onAllNodesWithText("Contraseña").assertCountEquals(0)
        compose.onAllNodesWithText("Usuario").assertCountEquals(0)
    }

    @Test
    fun con_url_la_accion_añadir_servidor_se_puede_pulsar() {
        var connected = false
        compose.setContent {
            TolochaTheme {
                WelcomeContent(
                    url = "https://radio.test",
                    alias = "Mi servidor",
                    state = SetupUiState.Idle("https://radio.test", "Mi servidor"),
                    onUrlChange = {},
                    onAliasChange = {},
                    onConnect = { connected = true },
                )
            }
        }

        compose.onNodeWithText("Añadir servidor").performClick()
        assertTrue(connected)
    }
}
