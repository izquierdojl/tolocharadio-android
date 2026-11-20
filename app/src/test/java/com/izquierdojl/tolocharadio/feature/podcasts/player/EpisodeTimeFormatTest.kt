package com.izquierdojl.tolocharadio.feature.podcasts.player

import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeTimeFormatTest {
    @Test
    fun `formatPlaybackTime usa minutos y segundos bajo una hora`() {
        assertEquals("0:00", formatPlaybackTime(0L))
        assertEquals("1:05", formatPlaybackTime(65_000L))
        assertEquals("59:59", formatPlaybackTime(3_599_000L))
    }

    @Test
    fun `formatPlaybackTime usa horas a partir de una hora`() {
        assertEquals("1:00:00", formatPlaybackTime(3_600_000L))
        assertEquals("1:01:01", formatPlaybackTime(3_661_000L))
    }

    @Test
    fun `formatPlaybackTime acota valores negativos`() {
        assertEquals("0:00", formatPlaybackTime(-1_000L))
    }

    @Test
    fun `formatRemainingTime anade el signo menos`() {
        assertEquals("-1:05", formatRemainingTime(65_000L))
        assertEquals("-0:00", formatRemainingTime(-1L))
    }
}
