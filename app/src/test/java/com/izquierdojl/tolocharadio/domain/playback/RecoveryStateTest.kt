package com.izquierdojl.tolocharadio.domain.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryStateTest {
    @Test
    fun `solo reconectar o esperar red publica transporte buffering`() {
        assertTrue(RecoveryState.Reconnecting("s1", 1, 6, InterruptionCause.STALL).isTransportBuffering())
        assertTrue(RecoveryState.WaitingForNetwork("s1", InterruptionCause.NO_CONNECTIVITY).isTransportBuffering())
        assertFalse(RecoveryState.Idle.isTransportBuffering())
        assertFalse(
            RecoveryState.Failed(
                "s1",
                InterruptionCause.NETWORK_ERROR,
                "error",
                PlayerErrorAction.RETRY,
            ).isTransportBuffering(),
        )
    }
}
