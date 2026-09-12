package com.izquierdojl.tolocharadio.feature.servers

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Flujo crítico (constitución III): pantalla unificada de servidor con
 * email y contraseña, en alta y edición.
 */
@RunWith(AndroidJUnit4::class)
class ServerFormScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun render(ui: ServerFormUi) {
        compose.setContent {
            TolochaTheme {
                ServerFormContent(
                    ui = ui,
                    showCancel = false,
                    onUrlChange = {},
                    onAliasChange = {},
                    onEmailChange = {},
                    onPasswordChange = {},
                    onSave = {},
                    onCancel = {},
                )
            }
        }
    }

    @Test
    fun alta_pide_url_alias_email_y_contrasena() {
        render(ServerFormUi())
        compose.onAllNodesWithText("Añadir servidor").assertCountEquals(2)
        compose.onNodeWithText("URL del servidor").assertIsDisplayed()
        compose.onNodeWithText("Alias").assertIsDisplayed()
        compose.onNodeWithText("Email de la cuenta").assertIsDisplayed()
        compose.onNodeWithText("Contraseña").assertIsDisplayed()
    }

    @Test
    fun edicion_precarga_credenciales_y_titulo() {
        render(
            ServerFormUi(
                mode = ServerFormMode.EDIT,
                serverId = "1",
                url = "https://radio.test",
                alias = "srv",
                email = "a@b.c",
                password = "secreta123",
            ),
        )
        compose.onNodeWithText("Editar servidor").assertIsDisplayed()
        compose.onNodeWithText("Guardar").assertIsDisplayed()
    }
}
