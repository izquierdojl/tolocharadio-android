package com.izquierdojl.tolocharadio.feature.podcasts.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import org.junit.Rule
import org.junit.Test

/** Interruptor del potenciador de voz (US4). */
class VoiceBoostToggleTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun muestraEstadoYNotificaElCambio() {
        var enabled by mutableStateOf(false)
        compose.setContent {
            TolochaTheme {
                VoiceBoostToggle(
                    enabled = enabled,
                    onToggle = { enabled = it },
                )
            }
        }

        compose.onNodeWithText("Potenciador de voz").assertIsDisplayed()
        compose.onNodeWithText("Desactivado").assertIsDisplayed()
        compose.onNodeWithTag(TolochaTestTags.VOICE_BOOST).performClick()
        compose.onNodeWithText("Activado").assertIsDisplayed()
    }
}
