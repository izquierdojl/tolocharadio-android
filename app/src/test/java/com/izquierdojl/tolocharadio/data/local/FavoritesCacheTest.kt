package com.izquierdojl.tolocharadio.data.local

import com.izquierdojl.tolocharadio.data.remote.dto.FavoriteDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesCacheTest {
    private val favs =
        listOf(
            FavoriteDto(StationDto("u1", "Uno", country = "España", tags = listOf("rock", "pop")), 1000),
            FavoriteDto(StationDto("u2", "Dos"), 2000),
        )

    @Test
    fun `toCached conserva orden en sortIndex continuo`() {
        val cached = favs.toCached(now = 999)
        assertEquals(listOf("u1", "u2"), cached.map { it.id })
        assertEquals(listOf(0, 1), cached.map { it.sortIndex })
        assertEquals("rock,pop", cached.first().tagsCsv)
        assertEquals(999, cached.first().cachedAt)
    }

    @Test
    fun `toFavorites reconstruye items redondeando el mapeo`() {
        val back = favs.toCached(now = 1).toFavorites()
        assertEquals(listOf("u1", "u2"), back.map { it.station.id })
        assertEquals(listOf("rock", "pop"), back.first().station.tags)
        assertEquals(1000, back.first().addedAt)
    }
}
