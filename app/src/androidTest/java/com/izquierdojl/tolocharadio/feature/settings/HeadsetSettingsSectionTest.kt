package com.izquierdojl.tolocharadio.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Sección de controles de auriculares en Configuración (spec 0060, US3). */
class HeadsetSettingsSectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun muestraValoresYSwitchYCambiaElRetroceso() {
        var back = 15
        compose.setContent {
            TolochaTheme {
                Box(Modifier.fillMaxWidth().height(480.dp)) {
                    SettingsContent(
                        ui =
                            SettingsUi(
                                headsetSkipBackSeconds = 15,
                                headsetSkipForwardSeconds = 30,
                                headsetSkipGesturesEnabled = false,
                            ),
                        actions =
                            SettingsActions(
                                onHeadsetSkipBackSecondsChange = { back = it },
                            ),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        compose.onNodeWithTag(TolochaTestTags.HEADSET_CONTROLS).performScrollTo().assertExists()
        compose.onNodeWithTag(TolochaTestTags.HEADSET_SKIP_BACK).performScrollTo().performClick()
        compose.onNodeWithText("5 s").performClick()
        assertEquals(5, back)

        compose.onNodeWithTag(TolochaTestTags.HEADSET_GESTURES_SWITCH).performScrollTo().assertIsOff()
    }
}
