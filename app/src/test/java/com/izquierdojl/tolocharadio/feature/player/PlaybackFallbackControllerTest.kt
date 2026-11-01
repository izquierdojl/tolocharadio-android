package com.izquierdojl.tolocharadio.feature.player

import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.domain.playback.PlaybackRoute
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PlaybackFallbackControllerTest {
    private val starter: LocalPlaybackStarter = mockk(relaxed = true)
    private val controller = PlaybackFallbackController(starter)
    private val station = StationDto(id = "s1", name = "Radio")

    @Test
    fun `inicia la ruta legacy sin historial`() =
        runTest {
            coEvery { starter.start(station, PlaybackRoute.LEGACY, 7, recordHistory = false) } returns Unit
            val result = controller.start(station, PlaybackRoute.LEGACY, 7)
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                starter.start(station, PlaybackRoute.LEGACY, 7, recordHistory = false)
            }
        }

    @Test
    fun `propaga el fallo del starter`() =
        runTest {
            coEvery { starter.start(any(), any(), any(), any()) } throws IOException("boom")
            assertFalse(controller.start(station, PlaybackRoute.LEGACY, 8).isSuccess)
        }
}
