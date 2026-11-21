package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PullToRefreshContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun elGestoAlPrincipioInvocaOnRefreshUnaVez() {
        var refreshes = 0
        compose.setContent {
            PullToRefreshContent(
                refreshing = false,
                onRefresh = { refreshes++ },
                modifier = Modifier.testTag("pull_refresh"),
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(20) { index ->
                        Text("Elemento $index", Modifier.height(56.dp))
                    }
                }
            }
        }

        compose.onNodeWithTag("pull_refresh").performTouchInput {
            swipeDown(startY = centerY, endY = bottom, durationMillis = 500)
        }
        compose.waitForIdle()

        assertEquals(1, refreshes)
    }

    @Test
    fun laAccionAccesibleActualizarInvocaOnRefresh() {
        var refreshes = 0
        compose.setContent {
            PullToRefreshContent(
                refreshing = false,
                onRefresh = { refreshes++ },
                modifier = Modifier.testTag("pull_refresh"),
            ) {
                Box(Modifier.fillMaxSize()) {
                    Text("Contenido")
                }
            }
        }

        val node = compose.onNodeWithTag("pull_refresh").fetchSemanticsNode()
        val actions = node.config[SemanticsActions.CustomActions]
        assertEquals("Actualizar", actions.single().label)
        assertTrue(actions.single().action())
        assertEquals(1, refreshes)
    }

    @Test
    fun mientrasActualizaMantieneElContenidoYMuestraElIndicador() {
        compose.setContent {
            PullToRefreshContent(
                refreshing = true,
                onRefresh = {},
                modifier = Modifier.testTag("pull_refresh"),
            ) {
                Box(Modifier.fillMaxSize()) {
                    Text("Contenido")
                }
            }
        }

        compose.waitForIdle()
        compose.onNodeWithText("Contenido").assertIsDisplayed()
        compose
            .onNodeWithTag(TolochaTestTags.PULL_REFRESH_INDICATOR, useUnmergedTree = true)
            .assertIsDisplayed()

        val node = compose.onNodeWithTag("pull_refresh").fetchSemanticsNode()
        assertFalse(node.config.contains(SemanticsActions.CustomActions))
        assertEquals("Actualizando…", node.config[SemanticsProperties.StateDescription])
    }
}
