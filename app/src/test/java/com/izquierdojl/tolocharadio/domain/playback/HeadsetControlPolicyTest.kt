package com.izquierdojl.tolocharadio.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Test

/** Matriz contenido × comando × fuente × ajustes (spec 0060, FR-006/FR-009/FR-012). */
class HeadsetControlPolicyTest {
    private val settings = HeadsetControlSettings(skipBackSeconds = 5, skipForwardSeconds = 60)

    @Test
    fun `episodio reproduciendo salta adelante y atras`() {
        assertEquals(
            HeadsetAction.Skip(60_000L),
            HeadsetControlPolicy.decide(
                PlaybackContent.EpisodePlaying,
                HeadsetCommand.SkipForward,
                SkipSource.HeadsetGesture,
                settings,
            ),
        )
        assertEquals(
            HeadsetAction.Skip(-5_000L),
            HeadsetControlPolicy.decide(
                PlaybackContent.EpisodePlaying,
                HeadsetCommand.SkipBack,
                SkipSource.HeadsetGesture,
                settings,
            ),
        )
    }

    @Test
    fun `episodio pausado tambien salta`() {
        assertEquals(
            HeadsetAction.Skip(-5_000L),
            HeadsetControlPolicy.decide(
                PlaybackContent.EpisodePaused,
                HeadsetCommand.Previous,
                SkipSource.HeadsetGesture,
                settings,
            ),
        )
    }

    @Test
    fun `los gestos desactivados no bloquean controles explicitos`() {
        val disabled = settings.copy(gesturesEnabled = false)
        assertEquals(
            HeadsetAction.Ignore,
            HeadsetControlPolicy.decide(
                PlaybackContent.EpisodePlaying,
                HeadsetCommand.Next,
                SkipSource.HeadsetGesture,
                disabled,
            ),
        )
        assertEquals(
            HeadsetAction.Skip(60_000L),
            HeadsetControlPolicy.decide(
                PlaybackContent.EpisodePlaying,
                HeadsetCommand.SkipForward,
                SkipSource.ExplicitControl,
                disabled,
            ),
        )
    }

    @Test
    fun `radio e idle no permiten saltos`() {
        assertEquals(
            HeadsetAction.Ignore,
            HeadsetControlPolicy.decide(
                PlaybackContent.LiveRadio,
                HeadsetCommand.SkipForward,
                SkipSource.ExplicitControl,
                settings,
            ),
        )
        assertEquals(
            HeadsetAction.Ignore,
            HeadsetControlPolicy.decide(
                PlaybackContent.Idle,
                HeadsetCommand.SkipForward,
                SkipSource.ExplicitControl,
                settings,
            ),
        )
    }

    @Test
    fun `next y previous son alias de los saltos`() {
        assertEquals(
            HeadsetControlPolicy.decide(
                PlaybackContent.EpisodePlaying,
                HeadsetCommand.SkipForward,
                SkipSource.HeadsetGesture,
                settings,
            ),
            HeadsetControlPolicy.decide(
                PlaybackContent.EpisodePlaying,
                HeadsetCommand.Next,
                SkipSource.HeadsetGesture,
                settings,
            ),
        )
        assertEquals(
            HeadsetControlPolicy.decide(
                PlaybackContent.EpisodePlaying,
                HeadsetCommand.SkipBack,
                SkipSource.HeadsetGesture,
                settings,
            ),
            HeadsetControlPolicy.decide(
                PlaybackContent.EpisodePlaying,
                HeadsetCommand.Previous,
                SkipSource.HeadsetGesture,
                settings,
            ),
        )
    }
}
