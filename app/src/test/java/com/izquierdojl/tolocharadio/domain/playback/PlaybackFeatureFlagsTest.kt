package com.izquierdojl.tolocharadio.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackFeatureFlagsTest {
    @Test
    fun `bandera activa usa ruta continua`() {
        assertEquals(PlaybackRoute.CONTINUOUS, PlaybackFeatureFlags(true).defaultRoute)
    }

    @Test
    fun `bandera desactivada usa ruta legacy`() {
        assertEquals(PlaybackRoute.LEGACY, PlaybackFeatureFlags(false).defaultRoute)
    }
}
