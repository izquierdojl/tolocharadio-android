package com.izquierdojl.tolocharadio.feature.player

import com.izquierdojl.tolocharadio.domain.playback.PlaybackFeatureFlags
import com.izquierdojl.tolocharadio.domain.playback.PlaybackRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSessionStateTest {
    @Test
    fun `la ruta inicial sigue la bandera`() {
        assertEquals(
            PlaybackRoute.CONTINUOUS,
            PlaybackSessionState(PlaybackFeatureFlags(true)).route,
        )
        assertEquals(
            PlaybackRoute.LEGACY,
            PlaybackSessionState(PlaybackFeatureFlags(false)).route,
        )
    }

    @Test
    fun `el marcador empieza en cero y es monotono`() {
        val state = PlaybackSessionState(PlaybackFeatureFlags(true))
        assertEquals(0, state.nextSequence())
        assertEquals(1, state.nextSequence())
        assertEquals(2, state.nextSequence())
    }

    @Test
    fun `el fallback conserva la ruta legacy`() {
        val state = PlaybackSessionState(PlaybackFeatureFlags(true))
        state.useLegacyRoute()
        assertEquals(PlaybackRoute.LEGACY, state.route)
        state.resetRoute(PlaybackRoute.CONTINUOUS)
        assertEquals(PlaybackRoute.CONTINUOUS, state.route)
    }
}
