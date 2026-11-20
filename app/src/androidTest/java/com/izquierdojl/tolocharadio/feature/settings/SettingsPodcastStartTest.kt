package com.izquierdojl.tolocharadio.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.core.ui.navigation.PodcastStartScreen
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsPodcastStartTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun elSelectorDePodcastMuestraLasCincoOpciones() {
        var selected: PodcastStartScreen? = null
        compose.setContent {
            TolochaTheme {
                SettingsContent(
                    ui = SettingsUi(podcastStartScreen = PodcastStartScreen.HOME),
                    actions =
                        SettingsActions(
                            onPodcastStartScreenChange = { selected = it },
                        ),
                )
            }
        }

        compose.onNodeWithTag(TolochaTestTags.PODCAST_START_SCREEN).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(TolochaTestTags.PODCAST_START_SCREEN).performClick()
        compose.onNodeWithText("Descubrir").assertIsDisplayed()
        compose.onNodeWithText("Suscripciones").assertIsDisplayed()
        compose.onNodeWithText("Cola").performClick()

        assertEquals(PodcastStartScreen.QUEUE, selected)
    }
}
