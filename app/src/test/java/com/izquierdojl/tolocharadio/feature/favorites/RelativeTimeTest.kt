package com.izquierdojl.tolocharadio.feature.favorites

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RelativeTimeTest {
    private val now = 1_700_000_000_000L

    @Test
    fun `cero y futuro se ocultan`() {
        assertNull(relativeTime(0, now))
        assertNull(relativeTime(now + 1, now))
    }

    @Test
    fun `minutos horas y dias`() {
        assertEquals("ahora mismo", relativeTime(now - 10_000, now))
        assertEquals("hace 5 min", relativeTime(now - 5 * 60_000, now))
        assertEquals("hace 3 h", relativeTime(now - 3 * 3_600_000, now))
        assertEquals("hace 1 día", relativeTime(now - 25 * 3_600_000, now))
        assertEquals("hace 2 días", relativeTime(now - 2 * 86_400_000, now))
    }
}

