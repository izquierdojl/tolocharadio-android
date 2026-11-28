package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.theme.TolochaTheme
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Menú de acciones secundarias de un episodio (spec 0062). */
class EpisodeActionsSheetTest {
    @get:Rule
    val compose = createComposeRule()

    private val episode =
        PodcastEpisode(
            id = 1L,
            podcastId = 7L,
            title = "Episodio de prueba",
            durationSec = 1_800L,
        )

    private fun sheet(
        downloadState: DownloadUiState = DownloadUiState.NotDownloaded,
        onDownload: (() -> Unit)? = {},
        onQueueNext: (() -> Unit)? = {},
        onQueueEnd: (() -> Unit)? = {},
        onToggleFavorite: () -> Unit = {},
        onTogglePlayed: (() -> Unit)? = {},
        onOpenDetail: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        compose.setContent {
            TolochaTheme {
                EpisodeActionsSheet(
                    episode = episode,
                    podcast = null,
                    isDownloaded = false,
                    downloadState = downloadState,
                    onDownload = onDownload,
                    onQueueNext = onQueueNext,
                    onQueueEnd = onQueueEnd,
                    onToggleFavorite = onToggleFavorite,
                    onTogglePlayed = onTogglePlayed,
                    onOpenDetail = onOpenDetail,
                    onDismiss = onDismiss,
                )
            }
        }
    }

    @Test
    fun muestraTodasLasAccionesDisponibles() {
        sheet()
        compose.onNodeWithText("Descargar").assertExists()
        compose.onNodeWithText("Añadir a continuación").assertExists()
        compose.onNodeWithText("Añadir al final").assertExists()
        compose.onNodeWithText("Añadir a favoritos").assertExists()
        compose.onNodeWithText("Marcar como reproducido").assertExists()
        compose.onNodeWithText("Abrir ficha").assertExists()
    }

    @Test
    fun ocultaDescargaSiNoEstaDisponible() {
        sheet(onDownload = null)
        compose.onNodeWithText("Descargar").assertDoesNotExist()
        compose.onNodeWithText("Añadir a favoritos").assertExists()
    }

    @Test
    fun accionCierraEInvocaElCallback() {
        var dismissed = false
        var favorite = false
        sheet(
            onToggleFavorite = { favorite = true },
            onDismiss = { dismissed = true },
        )
        compose.onNodeWithText("Añadir a favoritos").performClick()
        assertTrue(favorite)
        assertTrue(dismissed)
    }
}
