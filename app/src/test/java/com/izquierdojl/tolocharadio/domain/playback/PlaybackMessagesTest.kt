package com.izquierdojl.tolocharadio.domain.playback

import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackMessagesTest {
    @Test
    fun `los mensajes visibles estan en espanol y no vacios`() {
        listOf(
            PlaybackMessages.UNSUPPORTED_FORMAT,
            PlaybackMessages.STATION_UNAVAILABLE,
            PlaybackMessages.RECONNECTION_FAILED,
            PlaybackMessages.AUTH_EXPIRED,
            PlaybackMessages.PLAYBACK_INTERRUPTED,
            PlaybackMessages.OFFLINE,
        ).forEach { assertTrue(it.isNotBlank()) }
    }
}
