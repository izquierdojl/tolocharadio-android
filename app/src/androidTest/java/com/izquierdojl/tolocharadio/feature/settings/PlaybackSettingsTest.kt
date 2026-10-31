package com.izquierdojl.tolocharadio.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.domain.playback.BufferProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/** Compose test de la sección "Reproducción" (spec 0042, US3). */
class PlaybackSettingsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun section(
        bufferProfile: BufferProfile = BufferProfile.BALANCED,
        resume: Boolean = true,
        onBuffer: (BufferProfile) -> Unit = {},
        onResume: (Boolean) -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                PlaybackSettingsSection(
                    bufferProfile = bufferProfile,
                    resumeOnConnectivity = resume,
                    onBufferProfileChange = onBuffer,
                    onResumeOnConnectivityChange = onResume,
                )
            }
        }
    }

    @Test
    fun muestraLosTresPerfilesYElSwitch() {
        section()
        compose.onNodeWithText("Ahorro de datos").assertIsDisplayed()
        compose.onNodeWithText("Equilibrado").assertIsDisplayed()
        compose.onNodeWithText("Máxima estabilidad").assertIsDisplayed()
        compose.onNodeWithText("Reanudar al recuperar conexión").assertIsDisplayed()
    }

    @Test
    fun seleccionarMaximaEstabilidadInvocaCallback() {
        var selected: BufferProfile? = null
        section(onBuffer = { selected = it })
        val max = BufferProfile.MAX_STABILITY
        compose.onNodeWithContentDescription("${max.label()}. ${max.description()}").performClick()
        assertEquals(BufferProfile.MAX_STABILITY, selected)
    }

    @Test
    fun desactivarReanudacionInvocaCallback() {
        var enabled = true
        section(resume = true, onResume = { enabled = it })
        compose.onNodeWithContentDescription("Cambiar reanudación automática").performClick()
        assertFalse(enabled)
    }
}
