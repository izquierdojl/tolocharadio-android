package com.izquierdojl.tolocharadio.feature.player

import android.media.AudioManager
import android.view.KeyEvent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.data.local.HeadsetControlSettingsCache
import com.izquierdojl.tolocharadio.domain.playback.HeadsetControlSettings
import com.izquierdojl.tolocharadio.domain.playback.SkipSource
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePlaybackController
import com.izquierdojl.tolocharadio.support.testEpisode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Ejecución y gating de los saltos (spec 0060, US2). */
class HeadsetControlsControllerTest {
    private lateinit var state: MutableStateFlow<EpisodePlaybackController.State>
    private lateinit var settings: MutableStateFlow<HeadsetControlSettings>
    private lateinit var episodeController: EpisodePlaybackController
    private lateinit var settingsCache: HeadsetControlSettingsCache
    private lateinit var player: ExoPlayer
    private lateinit var engine: PlaybackEngine
    private lateinit var audioManager: AudioManager
    private lateinit var controller: HeadsetControlsController

    private val episode = testEpisode(id = 1L, progress = null)

    @Before
    fun setUp() {
        state = MutableStateFlow(EpisodePlaybackController.State.Idle)
        settings = MutableStateFlow(HeadsetControlSettings.DEFAULT)
        episodeController = mockk(relaxed = true)
        every { episodeController.state } returns state
        every { episodeController.positionMs() } returns 30_000L
        settingsCache = mockk()
        every { settingsCache.settings } returns settings
        every { settingsCache.current() } answers { settings.value }
        player = mockk(relaxed = true)
        every { player.playbackSuppressionReason } returns Player.PLAYBACK_SUPPRESSION_REASON_NONE
        every { player.playbackState } returns Player.STATE_READY
        every { player.mediaItemCount } returns 0
        engine = mockk()
        every { engine.player } returns player
        audioManager = mockk()
        every { audioManager.mode } returns AudioManager.MODE_NORMAL
        controller =
            HeadsetControlsController(
                episodeController = episodeController,
                settingsCache = settingsCache,
                engine = engine,
                audioManager = audioManager,
            )
    }

    private fun active(
        playing: Boolean = true,
        positionMs: Long = 30_000L,
        durationMs: Long = 1_800_000L,
    ) {
        every { episodeController.positionMs() } returns positionMs
        state.value =
            EpisodePlaybackController.State.Active(
                episode = episode,
                podcast = null,
                playing = playing,
                positionMs = positionMs,
                durationMs = durationMs,
                rate = 1f,
                local = false,
                casting = false,
            )
    }

    @Test
    fun `salta adelante y atras con el episodio activo`() {
        active()

        controller.skipForward(SkipSource.HeadsetGesture)
        controller.skipBack(SkipSource.HeadsetGesture)

        verify { episodeController.seekTo(60_000L) }
        verify { episodeController.seekTo(15_000L) }
    }

    @Test
    fun `el salto funciona con el episodio pausado`() {
        active(playing = false, positionMs = 100_000L)

        controller.skipForward(SkipSource.HeadsetGesture)

        verify { episodeController.seekTo(130_000L) }
    }

    @Test
    fun `la radio no permite saltos`() {
        state.value = EpisodePlaybackController.State.Idle
        every { player.mediaItemCount } returns 1

        controller.skipForward(SkipSource.ExplicitControl)

        verify(exactly = 0) { episodeController.seekTo(any()) }
    }

    @Test
    fun `los gestos desactivados no bloquean los controles explicitos`() {
        active()
        settings.value = HeadsetControlSettings(gesturesEnabled = false)

        controller.skipForward(SkipSource.HeadsetGesture)
        verify(exactly = 0) { episodeController.seekTo(any()) }

        controller.skipForward(SkipSource.ExplicitControl)
        verify { episodeController.seekTo(60_000L) }
    }

    @Test
    fun `una llamada bloquea los saltos`() {
        active()
        every { audioManager.mode } returns AudioManager.MODE_IN_CALL

        controller.skipForward(SkipSource.ExplicitControl)

        verify(exactly = 0) { episodeController.seekTo(any()) }
    }

    @Test
    fun `la perdida de foco bloquea los saltos`() {
        active()
        every { player.playbackSuppressionReason } returns
            Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS

        controller.skipForward(SkipSource.ExplicitControl)

        verify(exactly = 0) { episodeController.seekTo(any()) }
    }

    @Test
    fun `el buffering no permite saltos`() {
        active()
        every { player.playbackState } returns Player.STATE_BUFFERING

        controller.skipForward(SkipSource.ExplicitControl)

        verify(exactly = 0) { episodeController.seekTo(any()) }
    }

    @Test
    fun `procesa next solo en el primer down y consume repeticiones y up`() {
        active()

        assertTrue(controller.onMediaButton(KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.ACTION_DOWN, 0))
        assertTrue(controller.onMediaButton(KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.ACTION_DOWN, 1))
        assertTrue(controller.onMediaButton(KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.ACTION_UP, 0))

        verify(exactly = 1) { episodeController.seekTo(60_000L) }
    }

    @Test
    fun `play pausa se delega en Media3`() {
        active()

        assertFalse(
            controller.onMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.ACTION_DOWN, 0),
        )
        verify(exactly = 0) { episodeController.seekTo(any()) }
    }

    @Test
    fun `tras reconectar el auricular los saltos siguen funcionando`() {
        active()
        controller.skipForward(SkipSource.HeadsetGesture)
        state.value = EpisodePlaybackController.State.Idle
        active()
        controller.skipBack(SkipSource.HeadsetGesture)

        verify { episodeController.seekTo(60_000L) }
        verify { episodeController.seekTo(15_000L) }
    }
}
