package com.izquierdojl.tolocharadio.feature.servers

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.domain.servers.SavedServer
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Flujo crítico (constitución III): selección de servidor sin credenciales
 * y estado del servidor activo.
 */
@RunWith(AndroidJUnit4::class)
class ServerSwitchUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun server(active: Boolean) =
        SavedServer(
            id = "b",
            url = "https://srv-b.example.com",
            alias = "srv-b",
            appName = null,
            isActive = active,
            isDefault = false,
            createdAt = 0,
        )

    @Test
    fun pulsar_un_servidor_solicita_el_cambio() {
        var switched = false
        compose.setContent {
            TolochaTheme {
                ServerCard(server = server(false), onSwitch = { switched = true }, onEdit = {}, onDelete = {})
            }
        }

        compose.onNodeWithText("srv-b").performClick()
        assertTrue(switched)
    }

    @Test
    fun el_servidor_activo_muestra_su_etiqueta() {
        compose.setContent {
            TolochaTheme {
                ServerCard(server = server(true), onSwitch = {}, onEdit = {}, onDelete = {})
            }
        }

        compose.onNodeWithText("Activo").assertIsDisplayed()
    }
}
