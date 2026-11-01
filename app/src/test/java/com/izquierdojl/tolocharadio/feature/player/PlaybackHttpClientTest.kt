package com.izquierdojl.tolocharadio.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fija los timeouts del flujo de audio en directo (bug 0045). */
class PlaybackHttpClientTest {
    private val client = PlaybackHttpClient.create()

    @Test
    fun `no aplica timeout de lectura al flujo en directo`() {
        assertEquals(0, client.readTimeoutMillis)
    }

    @Test
    fun `acota el tiempo de conexion`() {
        assertEquals(15_000, client.connectTimeoutMillis)
    }

    @Test
    fun `reintenta conexiones fallidas`() {
        assertTrue(client.retryOnConnectionFailure)
    }
}
