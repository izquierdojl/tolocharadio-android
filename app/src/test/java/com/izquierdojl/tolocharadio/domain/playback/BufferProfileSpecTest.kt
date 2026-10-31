package com.izquierdojl.tolocharadio.domain.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class BufferProfileSpecTest {
    @Test
    fun `el perfil por defecto es equilibrado`() {
        assertEquals(BufferProfile.BALANCED, BufferProfile.DEFAULT)
    }

    @Test
    fun `un valor persistido desconocido cae a equilibrado`() {
        assertEquals(BufferProfile.BALANCED, BufferProfile.fromPersisted("XXX"))
        assertEquals(BufferProfile.BALANCED, BufferProfile.fromPersisted(null))
    }

    @Test
    fun `los tres perfiles respetan los valores de producto`() {
        assertEquals(
            BufferProfileSpec(10_000, 30_000, 1_000, 2_000),
            BufferProfileSpec.forProfile(BufferProfile.DATA_SAVER),
        )
        assertEquals(
            BufferProfileSpec(20_000, 50_000, 2_000, 4_000),
            BufferProfileSpec.forProfile(BufferProfile.BALANCED),
        )
        assertEquals(
            BufferProfileSpec(40_000, 60_000, 6_000, 7_000),
            BufferProfileSpec.forProfile(BufferProfile.MAX_STABILITY),
        )
    }

    @Test
    fun `maxima estabilidad anade como maximo cinco segundos de arranque`() {
        val balanced = BufferProfileSpec.forProfile(BufferProfile.BALANCED)
        val max = BufferProfileSpec.forProfile(BufferProfile.MAX_STABILITY)
        assertEquals(4_000, max.bufferForPlaybackMs - balanced.bufferForPlaybackMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza maxBuffer menor que minBuffer`() {
        BufferProfileSpec(10_000, 5_000, 1_000, 2_000)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza minBuffer menor que bufferForPlayback`() {
        BufferProfileSpec(1_000, 10_000, 2_000, 2_000)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rechaza rebuffer superior a siete segundos`() {
        BufferProfileSpec(20_000, 50_000, 2_000, 8_000)
    }
}
