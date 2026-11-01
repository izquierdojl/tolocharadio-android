package com.izquierdojl.tolocharadio.domain.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationAvailabilityTest {
    private val availability = StationAvailability("s1", true, null, checkedAt = 1_000)

    @Test
    fun `el resultado es fresco durante cinco segundos`() {
        assertTrue(availability.isFresh(6_000))
        assertFalse(availability.isFresh(6_001))
        assertFalse(availability.isFresh(999))
    }
}
