package com.izquierdojl.tolocharadio.feature.player

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.PlaybackStatusDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.PlaybackRepo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests base del reproductor (spec 004, T003). La conexión al
 * `MediaController` es best-effort: el estado lo gobierna el listener
 * del `ExoPlayer` compartido, así que el VM debe nacer en `Idle`
 * incluso sin controlador de sesión disponible (tests JVM).
 */
class PlayerViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val context: Context = mockk(relaxed = true)
    private val playback: PlaybackRepo = mockk()
    private val prefs: InstancePrefs =
        mockk {
            every { baseUrl } returns flowOf("https://radio.test/")
        }
    private val dataSource: AuthDataSourceFactory = mockk()
    private val activeStationHolder: ActiveStationHolder = mockk(relaxed = true)
    private val exoPlayer: ExoPlayer = mockk(relaxed = true)
    private val castExoPlayer: ExoPlayer = mockk(relaxed = true)
    private val castPlayerManager: CastPlayerManager =
        mockk(relaxed = true) {
            every { exoPlayer } returns castExoPlayer
            every { castState } returns mockk(relaxed = true)
        }
    private val station = StationDto(id = "u1", name = "Tolocha")

    private fun vm() = PlayerViewModel(context, playback, prefs, dataSource, activeStationHolder, exoPlayer, castPlayerManager)

    @Test
    fun `estado inicial es Idle`() {
        assertTrue(vm().state.value is PlayerState.Idle)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `play con status no playable lleva a Error con motivo`() =
        runTest {
            coEvery { playback.status("u1") } returns
                ApiResult.Ok(PlaybackStatusDto("u1", false, "offline"))
            val viewModel = vm()
            viewModel.play(station)
            advanceUntilIdle()
            val state = viewModel.state.value
            assertTrue(state is PlayerState.Error)
            assertEquals(station, (state as PlayerState.Error).station)
            assertEquals("offline", state.message)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `play con error de red lleva a Error con mensaje en espanol`() =
        runTest {
            coEvery { playback.status("u1") } returns
                ApiResult.Err(DomainError.Unavailable("Servicio no disponible"))
            val viewModel = vm()
            viewModel.play(station)
            advanceUntilIdle()
            val state = viewModel.state.value
            assertTrue(state is PlayerState.Error)
            assertEquals(station, (state as PlayerState.Error).station)
        }

    @Test
    fun `toggleMute silencia y restaura el volumen`() {
        val viewModel = vm()
        viewModel.toggleMute()
        assertTrue(viewModel.isMuted.value)
        verify { castExoPlayer.volume = 0f }
        viewModel.toggleMute()
        assertTrue(!viewModel.isMuted.value)
        verify { castExoPlayer.volume = 1f }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `mute se resetea al reproducir otra emisora`() =
        runTest {
            coEvery { playback.status("u1") } returns
                ApiResult.Ok(PlaybackStatusDto("u1", false, "offline"))
            val viewModel = vm()
            viewModel.toggleMute()
            viewModel.play(station)
            advanceUntilIdle()
            assertTrue(!viewModel.isMuted.value)
        }

    @Test
    fun `mute se resetea al detener`() {
        val viewModel = vm()
        viewModel.toggleMute()
        viewModel.stop()
        assertTrue(!viewModel.isMuted.value)
        assertTrue(viewModel.state.value is PlayerState.Idle)
    }

    @Test
    fun `cancelLoad sin carga queda en Idle`() {
        val viewModel = vm()
        viewModel.cancelLoad()
        assertTrue(viewModel.state.value is PlayerState.Idle)
    }

    @Test
    fun `cancelLoad durante la carga cancela y queda en Idle`() {
        coEvery { playback.status("u1") } coAnswers {
            delay(500)
            ApiResult.Ok(PlaybackStatusDto("u1", false, "tarde"))
        }
        val viewModel = vm()
        viewModel.play(station)
        assertTrue(viewModel.state.value is PlayerState.Buffering)
        viewModel.cancelLoad()
        assertTrue(viewModel.state.value is PlayerState.Idle)
        Thread.sleep(700)
        assertTrue(viewModel.state.value is PlayerState.Idle)
    }
}
