package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica la configuración de foco de audio (spec 0019, FR-001..FR-005). */
class PlayerAudioConfigTest {
    @Test
    fun `usa USAGE_MEDIA para solicitar AUDIOFOCUS_GAIN`() {
        assertEquals(C.USAGE_MEDIA, PlayerAudioConfig.audioAttributes.usage)
    }

    @Test
    fun `usa CONTENT_TYPE_SPEECH para pausar en lugar de duck`() {
        assertEquals(C.AUDIO_CONTENT_TYPE_SPEECH, PlayerAudioConfig.audioAttributes.contentType)
    }

    @Test
    fun `gestiona foco y becoming noisy`() {
        assertTrue(PlayerAudioConfig.HANDLE_AUDIO_FOCUS)
        assertTrue(PlayerAudioConfig.HANDLE_AUDIO_BECOMING_NOISY)
    }
}
