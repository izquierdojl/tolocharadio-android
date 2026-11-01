package com.izquierdojl.tolocharadio.feature.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.domain.playback.PlayerErrorAction
import org.junit.Rule
import org.junit.Test

/** Compose test del reproductor completo (spec 0042, US2/US4). */
class RecoveryUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val station = StationDto("s1", "Test", url = "https://ejemplo.com/stream")

    private fun content(
        state: PlayerState,
        onRetry: () -> Unit = {},
        onEditServer: () -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                FullPlayerContent(
                    state = state,
                    stationName = "Test",
                    station = station,
                    isCastConnected = false,
                    castDeviceName = "Chromecast",
                    onToggle = {},
                    onStop = {},
                    onRetry = onRetry,
                    onEditServer = onEditServer,
                    onOpenInfo = {},
                )
            }
        }
    }

    @Test
    fun errorRetry_muestraMensajeYReintentar() {
        var retried = false
        content(PlayerState.Error(station, "Fallo de red"), onRetry = { retried = true })
        compose.onNodeWithText("Fallo de red").assertIsDisplayed()
        compose.onNodeWithContentDescription("Reintentar").assertIsDisplayed()
        compose.onNodeWithContentDescription("Reintentar").performClick()
        assert(retried)
    }

    @Test
    fun errorEditServer_muestraEditarServidor() {
        var edited = false
        content(
            PlayerState.Error(station, "Sesión caducada", PlayerErrorAction.EDIT_SERVER),
            onEditServer = { edited = true },
        )
        compose.onNodeWithText("Editar servidor").assertIsDisplayed()
        compose.onNodeWithText("Editar servidor").performClick()
        assert(edited)
    }

    @Test
    fun bufferingNormal_muestraCargando() {
        content(PlayerState.Buffering(station))
        compose.onNodeWithText("Cargando…").assertIsDisplayed()
    }

    @Test
    fun sonando_muestraPausarYReproductorCompleto() {
        content(PlayerState.Playing(station))
        compose.onNodeWithContentDescription("Pausar").assertIsDisplayed()
    }

    @Test
    fun bufferingRetry_muestraReintentando() {
        content(PlayerState.Buffering(station, retrying = true))
        compose.onNodeWithText("Reintentando…").assertIsDisplayed()
    }

    @Test
    fun waitingForNetwork_muestraSinConexion() {
        content(PlayerState.Reconnecting(station, 0, 5, waitingForNetwork = true))
        compose.onNodeWithText("Sin conexión. Se reanudará al volver la red.").assertIsDisplayed()
    }

    @Test
    fun applying_muestraAplicandoAjuste() {
        content(PlayerState.Reconnecting(station, 0, 5, applying = true))
        compose.onNodeWithText("Aplicando ajuste…").assertIsDisplayed()
    }
}
