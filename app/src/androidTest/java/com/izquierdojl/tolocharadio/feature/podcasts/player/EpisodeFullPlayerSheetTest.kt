package com.izquierdojl.tolocharadio.feature.podcasts.player

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EpisodeFullPlayerSheetTest {
    @get:Rule
    val compose = createComposeRule()

    private fun actions(
        isFavorite: Boolean,
        onToggleFavorite: () -> Unit = {},
        onStop: () -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                EpisodeFullPlayerActions(
                    isFavorite = isFavorite,
                    onToggleFavorite = onToggleFavorite,
                    onStop = onStop,
                )
            }
        }
    }

    @Test
    fun detener_esUnIconoSinTextoVisible() {
        actions(isFavorite = false)

        compose.onNodeWithContentDescription("Detener").assertIsDisplayed()
        compose.onAllNodesWithText("Detener").assertCountEquals(0)
    }

    @Test
    fun favorito_muestraEstadoYAlterna() {
        var toggled = false
        actions(isFavorite = false, onToggleFavorite = { toggled = true })

        compose.onNodeWithContentDescription("Añadir a favoritos").assertIsDisplayed().performClick()
        assertTrue(toggled)
    }

    @Test
    fun favorito_marcadoMuestraQuitar() {
        actions(isFavorite = true)

        compose.onNodeWithContentDescription("Quitar de favoritos").assertIsDisplayed()
    }

    @Test
    fun detener_invocaElCallback() {
        var stopped = false
        actions(isFavorite = false, onStop = { stopped = true })

        compose.onNodeWithContentDescription("Detener").performClick()
        assertTrue(stopped)
    }
}
