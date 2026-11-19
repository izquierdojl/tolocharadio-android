package com.izquierdojl.tolocharadio.feature.player

import org.junit.Assert.assertEquals
import org.junit.Test

/** Selección de botones de la notificación (US2, FR-007–FR-013). */
class MediaButtonPolicyTest {
    @Test
    fun `radio sin fallo no publica seek`() {
        assertEquals(MediaButtonLayout.RADIO, MediaButtonPolicy.layout(episodeActive = false, radioRetry = false))
    }

    @Test
    fun `radio con fallo recuperable anade reintentar`() {
        assertEquals(
            MediaButtonLayout.RADIO_RETRY,
            MediaButtonPolicy.layout(episodeActive = false, radioRetry = true),
        )
    }

    @Test
    fun `episodio activo publica seek aunque haya fallo de radio`() {
        assertEquals(
            MediaButtonLayout.EPISODE,
            MediaButtonPolicy.layout(episodeActive = true, radioRetry = true),
        )
    }
}
