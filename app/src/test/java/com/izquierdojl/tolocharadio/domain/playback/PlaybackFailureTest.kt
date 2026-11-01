package com.izquierdojl.tolocharadio.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackFailureTest {
    @Test
    fun `conserva causa y detalles opcionales`() {
        val failure = PlaybackFailure(InterruptionCause.UNSUPPORTED_FORMAT, 415, 2001)
        assertEquals(InterruptionCause.UNSUPPORTED_FORMAT, failure.cause)
        assertEquals(415, failure.httpStatus)
        assertEquals(2001, failure.playerErrorCode)
    }

    @Test
    fun `los detalles son opcionales`() {
        val failure = PlaybackFailure(InterruptionCause.NETWORK_ERROR)
        assertNull(failure.httpStatus)
        assertNull(failure.playerErrorCode)
    }
}
