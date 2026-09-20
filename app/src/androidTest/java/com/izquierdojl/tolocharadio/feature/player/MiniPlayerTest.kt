package com.izquierdojl.tolocharadio.feature.player

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import org.junit.Rule
import org.junit.Test

/**
 * Compose Test del mini-player mínimo (sin Hilt, spec 0039, US2, contrato C2):
 * en reproducción normal exactamente 2 acciones (principal + silenciar), sin
 * copiar enlace; en error solo reintentar; en carga indicador con cancelar.
 */
class MiniPlayerTest {
    @get:Rule
    val compose = createComposeRule()

    private val station = StationDto("s1", "Test", url = "https://ejemplo.com/stream")

    private fun panel(
        state: PlayerState,
        muted: Boolean = false,
        subtitle: String = "España · español",
        onToggle: () -> Unit = {},
        onCancelLoad: () -> Unit = {},
        onRetry: () -> Unit = {},
        onToggleMute: () -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                MiniPlayerContent(
                    state = state,
                    station = station,
                    muted = muted,
                    subtitle = subtitle,
                    actions =
                        MiniPlayerActions(
                            onOpen = {},
                            onToggle = onToggle,
                            onCancelLoad = onCancelLoad,
                            onRetry = onRetry,
                            onToggleMute = onToggleMute,
                        ),
                )
            }
        }
    }

    @Test
    fun sonando_muestraPausarYSilenciarSinCopiar() {
        panel(PlayerState.Playing(station))
        compose.onNodeWithContentDescription("Pausar").assertIsDisplayed()
        compose.onNodeWithContentDescription("Silenciar").assertIsDisplayed()
        compose.onAllNodesWithText("Compartir").assertCountEquals(0)
        compose.onAllNodesWithText("Copiar").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Reintentar").assertCountEquals(0)
    }

    @Test
    fun pausado_muestraReanudarYSilenciar() {
        panel(PlayerState.Paused(station))
        compose.onNodeWithContentDescription("Reanudar").assertIsDisplayed()
        compose.onNodeWithContentDescription("Silenciar").assertIsDisplayed()
    }

    @Test
    fun silenciado_muestraActivarSonido() {
        panel(PlayerState.Playing(station), muted = true)
        compose.onNodeWithContentDescription("Activar sonido").assertIsDisplayed()
    }

    @Test
    fun pausar_invocaOnToggle() {
        var toggled = false
        panel(PlayerState.Playing(station), onToggle = { toggled = true })
        compose.onNodeWithContentDescription("Pausar").performClick()
        assert(toggled)
    }

    @Test
    fun error_muestraReintentarSinSilenciar() {
        panel(PlayerState.Error(station, "Fallo de red"), subtitle = "Fallo de red")
        compose.onNodeWithContentDescription("Reintentar").assertIsDisplayed()
        compose.onNodeWithText("Fallo de red").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Silenciar").assertCountEquals(0)
        compose.onAllNodesWithContentDescription("Activar sonido").assertCountEquals(0)
    }

    @Test
    fun carga_muestraCancelarCarga() {
        panel(PlayerState.Buffering(station))
        compose.onNodeWithContentDescription("Cancelar carga").assertIsDisplayed()
    }
}
