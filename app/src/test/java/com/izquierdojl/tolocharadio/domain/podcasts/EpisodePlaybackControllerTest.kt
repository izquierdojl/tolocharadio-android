package com.izquierdojl.tolocharadio.domain.podcasts

import androidx.media3.common.Player
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.cast.CastConnectionState
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.feature.player.PlaybackEngine
import com.izquierdojl.tolocharadio.feature.player.StationMediaItemFactory
import com.izquierdojl.tolocharadio.feature.player.VoiceBoostController
import com.izquierdojl.tolocharadio.support.testEpisode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Propagación de la posición de reanudación al receptor Cast (bug 0056). */
class EpisodePlaybackControllerTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val cast = mockk<CastPlayerManager>(relaxed = true)
    private val player = mockk<Player>(relaxed = true)
    private val engine = mockk<PlaybackEngine>(relaxed = true)
    private val mediaItems = mockk<StationMediaItemFactory>(relaxed = true)
    private val voiceBoost = mockk<VoiceBoostController>(relaxed = true)

    @Test
    fun `play en Cast propaga la posicion de reanudacion`() =
        runTest {
            every { cast.isCastConnected } returns true
            every { cast.activePlayer } returns player
            every { cast.connectionState } returns MutableStateFlow(CastConnectionState.CONNECTED)
            val controller =
                EpisodePlaybackController(
                    engine = engine,
                    cast = cast,
                    mediaItems = mediaItems,
                    voiceBoost = voiceBoost,
                    scope = backgroundScope,
                )
            val episode = testEpisode(id = 3L)
            val podcast = PodcastSummary(id = 7L, feedUrl = "https://feeds/7", title = "Programa")
            val source = EpisodePlaybackSource("https://cdn/e.mp3", local = false, expiresAt = 0L)

            controller.play(source, episode, podcast, startMs = 120_000L, rate = 1f)

            verify(exactly = 1) {
                cast.connectToEpisode(source.uri, episode.title, podcast.title, 120_000L)
            }
            val state = controller.state.value as EpisodePlaybackController.State.Active
            assertEquals(120_000L, state.positionMs)
            assertEquals(true, state.casting)
        }
}
