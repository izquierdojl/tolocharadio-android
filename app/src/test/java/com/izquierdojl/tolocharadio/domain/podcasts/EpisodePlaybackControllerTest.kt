package com.izquierdojl.tolocharadio.domain.podcasts

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.cast.CastConnectionState
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.core.network.FakeConnectivityMonitor
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.feature.player.EpisodeMediaSourceFactory
import com.izquierdojl.tolocharadio.feature.player.PlaybackClock
import com.izquierdojl.tolocharadio.feature.player.PlaybackEngine
import com.izquierdojl.tolocharadio.feature.player.VoiceBoostController
import com.izquierdojl.tolocharadio.support.testEpisode
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Propagación de la posición al Cast (bug 0056) y recuperación de red (bug 0057). */
class EpisodePlaybackControllerTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val cast = mockk<CastPlayerManager>(relaxed = true)
    private val localPlayer = mockk<ExoPlayer>(relaxed = true)
    private val engine = mockk<PlaybackEngine>(relaxed = true)
    private val episodeSources = mockk<EpisodeMediaSourceFactory>(relaxed = true)
    private val episodePrefetch = mockk<EpisodePrefetch>(relaxed = true)
    private val voiceBoost = mockk<VoiceBoostController>(relaxed = true)
    private val connectivity = FakeConnectivityMonitor(initialOnline = true)

    private val episode = testEpisode(id = 3L)
    private val podcast = PodcastSummary(id = 7L, feedUrl = "https://feeds/7", title = "Programa")
    private val source = EpisodePlaybackSource("https://cdn/e.mp3", local = false, expiresAt = 0L)

    private fun TestScope.controller(): EpisodePlaybackController {
        every { engine.player } returns localPlayer
        every { cast.isCastConnected } returns false
        every { cast.activePlayer } returns localPlayer
        every { cast.connectionState } returns MutableStateFlow(CastConnectionState.DISCONNECTED)
        every { localPlayer.playbackState } returns Player.STATE_READY
        return EpisodePlaybackController(
            engine = engine,
            cast = cast,
            episodeSources = episodeSources,
            episodePrefetch = episodePrefetch,
            voiceBoost = voiceBoost,
            connectivity = connectivity,
            clock = PlaybackClock { testScheduler.currentTime },
            scope = backgroundScope,
        )
    }

    private fun listenerOf(): Player.Listener {
        val listener = slot<Player.Listener>()
        verify { localPlayer.addListener(capture(listener)) }
        return listener.captured
    }

    private fun networkError(): PlaybackException {
        val code = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        return PlaybackException("net", null, code)
    }

    private fun parserError(): PlaybackException {
        val code = PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED
        return PlaybackException("parser", null, code)
    }

    @Test
    fun `play en Cast propaga la posicion de reanudacion`() =
        runTest {
            every { cast.isCastConnected } returns true
            every { cast.activePlayer } returns localPlayer
            every { cast.connectionState } returns MutableStateFlow(CastConnectionState.CONNECTED)
            val controller =
                EpisodePlaybackController(
                    engine = engine,
                    cast = cast,
                    episodeSources = episodeSources,
                    episodePrefetch = episodePrefetch,
                    voiceBoost = voiceBoost,
                    connectivity = connectivity,
                    clock = PlaybackClock { testScheduler.currentTime },
                    scope = backgroundScope,
                )

            controller.play(source, episode, podcast, startMs = 120_000L, rate = 1f)

            verify(exactly = 1) {
                cast.connectToEpisode(source.uri, episode.title, podcast.title, 120_000L)
            }
            val state = controller.state.value as EpisodePlaybackController.State.Active
            assertEquals(120_000L, state.positionMs)
            assertEquals(true, state.casting)
        }

    @Test
    fun `sin red espera y reanuda en la ultima posicion`() =
        runTest {
            val controller = controller()
            controller.play(source, episode, podcast, startMs = 0L, rate = 1f)
            controller.seekTo(90_000L)
            connectivity.setOnline(false)

            listenerOf().onPlayerError(networkError())
            advanceUntilIdle()

            val waiting = controller.state.value as EpisodePlaybackController.State.WaitingForNetwork
            assertEquals(90_000L, waiting.positionMs)

            connectivity.setOnline(true)
            advanceUntilIdle()

            val active = controller.state.value as EpisodePlaybackController.State.Active
            assertTrue(active.playing)
            assertEquals(90_000L, active.positionMs)
            verify(exactly = 2) { episodeSources.create(any(), any(), any()) }
        }

    @Test
    fun `la pausa cancela la espera de red y no recarga`() =
        runTest {
            val controller = controller()
            controller.play(source, episode, podcast, startMs = 0L, rate = 1f)
            controller.seekTo(60_000L)
            connectivity.setOnline(false)
            listenerOf().onPlayerError(networkError())
            advanceUntilIdle()

            controller.pause()
            connectivity.setOnline(true)
            advanceUntilIdle()

            val paused = controller.state.value as EpisodePlaybackController.State.Active
            assertFalse(paused.playing)
            assertEquals(60_000L, paused.positionMs)
            verify(exactly = 1) { episodeSources.create(any(), any(), any()) }
        }

    @Test
    fun `un error de formato no se reintenta`() =
        runTest {
            val controller = controller()
            controller.play(source, episode, podcast, startMs = 0L, rate = 1f)

            listenerOf().onPlayerError(parserError())
            advanceUntilIdle()

            assertTrue(controller.state.value is EpisodePlaybackController.State.Error)
            verify(exactly = 1) { episodeSources.create(any(), any(), any()) }
        }

    @Test
    fun `resume desde error reintenta en la posicion guardada`() =
        runTest {
            val controller = controller()
            controller.play(source, episode, podcast, startMs = 0L, rate = 1f)
            controller.seekTo(30_000L)
            listenerOf().onPlayerError(parserError())
            advanceUntilIdle()

            controller.resume()
            advanceUntilIdle()

            val active = controller.state.value as EpisodePlaybackController.State.Active
            assertTrue(active.playing)
            assertEquals(30_000L, active.positionMs)
            verify(exactly = 2) { episodeSources.create(any(), any(), any()) }
        }
}
