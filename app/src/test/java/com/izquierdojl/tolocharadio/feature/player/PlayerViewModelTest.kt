package com.izquierdojl.tolocharadio.feature.player

import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.cast.CastConnectionState
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.cast.CastPlayerState
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.PlaybackStatusDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.PlaybackRepo
import com.izquierdojl.tolocharadio.domain.playback.PlaybackStatusReason
import com.izquierdojl.tolocharadio.domain.playback.ResolvePlaybackSourceUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
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
    private val activeStationHolder: ActiveStationHolder = mockk(relaxed = true)
    private val exoPlayer: ExoPlayer = mockk(relaxed = true)
    private val castExoPlayer: ExoPlayer = mockk(relaxed = true)
    private val castPlayerManager: CastPlayerManager =
        mockk(relaxed = true) {
            every { exoPlayer } returns castExoPlayer
            every { castState } returns mockk(relaxed = true)
        }
    private val resolveSource = ResolvePlaybackSourceUseCase()
    private val mediaItemFactory: StationMediaItemFactory = mockk(relaxed = true)
    private val station = StationDto(id = "u1", name = "Tolocha")

    private fun vm() =
        PlayerViewModel(
            context,
            playback,
            prefs,
            activeStationHolder,
            exoPlayer,
            castPlayerManager,
            resolveSource,
            mediaItemFactory,
        )

    @Before
    fun setup() {
        coEvery { playback.status(any()) } coAnswers { awaitCancellation() }
    }

    @Test
    fun `estado inicial es Idle`() {
        assertTrue(vm().state.value is PlayerState.Idle)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `play con status no playable mapea el motivo a mensaje`() =
        runTest {
            coEvery { playback.status("u1") } returns
                ApiResult.Ok(PlaybackStatusDto("u1", false, PlaybackStatusReason.PLAYLIST_EMPTY))
            val viewModel = vm()
            viewModel.play(station)
            advanceUntilIdle()
            val state = viewModel.state.value
            assertTrue(state is PlayerState.Error)
            assertEquals(station, (state as PlayerState.Error).station)
            assertEquals(PlaybackStatusReason.reasonToMessage(PlaybackStatusReason.PLAYLIST_EMPTY), state.message)
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `play con emisora playable arranca la reproduccion`() =
        runTest {
            coEvery { playback.status("u1") } returns
                ApiResult.Ok(PlaybackStatusDto("u1", true, null))
            val viewModel = vm()
            viewModel.play(station)
            advanceUntilIdle()
            coVerify(exactly = 1) { playback.status("u1") }
            assertTrue(viewModel.state.value is PlayerState.Buffering)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `emisora hls tambien pasa por el precheck`() =
        runTest {
            val hls = StationDto(id = "h1", name = "HLS", url = "https://host/live.m3u8")
            coEvery { playback.status("h1") } returns
                ApiResult.Ok(PlaybackStatusDto("h1", true, null))
            val viewModel = vm()
            viewModel.play(hls)
            advanceUntilIdle()
            coVerify(exactly = 1) { playback.status("h1") }
            assertTrue(viewModel.state.value is PlayerState.Buffering)
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
                ApiResult.Ok(PlaybackStatusDto("u1", false, PlaybackStatusReason.STREAM_UNAVAILABLE))
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

    private fun captureListener(): Player.Listener {
        val listenerSlot = slot<Player.Listener>()
        verify { exoPlayer.addListener(capture(listenerSlot)) }
        return listenerSlot.captured
    }

    @Test
    fun `onPlayerError durante Buffering preserva la emisora`() {
        val viewModel = vm()
        val listener = captureListener()
        viewModel.play(station)
        assertTrue(viewModel.state.value is PlayerState.Buffering)
        listener.onPlayerError(mockk<PlaybackException>(relaxed = true))
        val state = viewModel.state.value
        assertTrue(state is PlayerState.Error)
        assertEquals(station, (state as PlayerState.Error).station)
    }

    @Test
    fun `onPlayerError durante Playing preserva la emisora`() {
        val viewModel = vm()
        val listener = captureListener()
        viewModel.play(station)
        listener.onPlaybackStateChanged(Player.STATE_READY)
        every { castPlayerManager.activePlayer } returns mockk { every { playWhenReady } returns true }
        listener.onIsPlayingChanged(true)
        assertTrue(viewModel.state.value is PlayerState.Playing)
        listener.onPlayerError(mockk<PlaybackException>(relaxed = true))
        val state = viewModel.state.value
        assertTrue(state is PlayerState.Error)
        assertEquals(station, (state as PlayerState.Error).station)
    }

    @Test
    fun `onPlayerError en Idle no cambia estado`() {
        val viewModel = vm()
        val listener = captureListener()
        assertTrue(viewModel.state.value is PlayerState.Idle)
        listener.onPlayerError(mockk<PlaybackException>(relaxed = true))
        assertTrue(viewModel.state.value is PlayerState.Idle)
    }

    @Test
    fun `full player visible se abre y cierra`() {
        val viewModel = vm()
        assertTrue(!viewModel.fullPlayerVisible.value)
        viewModel.openFullPlayer()
        assertTrue(viewModel.fullPlayerVisible.value)
        viewModel.closeFullPlayer()
        assertTrue(!viewModel.fullPlayerVisible.value)
    }

    @Test
    fun `stop oculta el reproductor completo`() {
        val viewModel = vm()
        viewModel.openFullPlayer()
        viewModel.stop()
        assertTrue(!viewModel.fullPlayerVisible.value)
    }

    @Test
    fun `toggle pausa el CastPlayer cuando el estado efectivo es Playing`() {
        every { castPlayerManager.castState } returns
            MutableStateFlow<CastPlayerState>(
                CastPlayerState.Cast(PlayerState.Playing(station), "TV", CastConnectionState.CONNECTED),
            )
        every { castPlayerManager.isCastConnected } returns true
        val castPlayer = mockk<Player>(relaxed = true)
        every { castPlayerManager.activePlayer } returns castPlayer

        vm().toggle()

        verify { castPlayer.playWhenReady = false }
    }

    @Test
    fun `toggle reanuda el CastPlayer cuando el estado efectivo es Paused`() {
        every { castPlayerManager.castState } returns
            MutableStateFlow<CastPlayerState>(
                CastPlayerState.Cast(PlayerState.Paused(station), "TV", CastConnectionState.CONNECTED),
            )
        every { castPlayerManager.isCastConnected } returns true
        val castPlayer = mockk<Player>(relaxed = true)
        every { castPlayerManager.activePlayer } returns castPlayer

        vm().toggle()

        verify { castPlayer.playWhenReady = true }
    }
}
