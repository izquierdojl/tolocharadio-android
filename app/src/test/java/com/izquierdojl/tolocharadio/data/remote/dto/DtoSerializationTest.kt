package com.izquierdojl.tolocharadio.data.remote.dto

import com.izquierdojl.tolocharadio.core.network.TolochaJson
import com.izquierdojl.tolocharadio.data.remote.api.ReorderBody
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Test

class DtoSerializationTest {
    @Test
    fun `Station del OpenAPI se deserializa`() {
        val dto =
            TolochaJson.decodeFromString<StationDto>(
                """{"id":"uuid-1","name":"Tolocha","url":"http://x/stream",
               "country":"España","language":"español","tags":["rock"],
               "bitrate":128,"isCustom":false}""",
            )
        assertEquals("uuid-1", dto.id)
        assertEquals(listOf("rock"), dto.tags)
    }

    @Test
    fun `StationPage con paginacion hasMore`() {
        val dto =
            TolochaJson.decodeFromString<StationPageDto>(
                """{"items":[],"pagination":{"offset":24,"limit":24,"hasMore":true}}""",
            )
        assertEquals(true, dto.pagination.hasMore)
    }

    @Test
    fun `PlaybackStatus no disponible con motivo`() {
        val dto =
            TolochaJson.decodeFromString<PlaybackStatusDto>(
                """{"id":"uuid-1","playable":false,"reason":"offline"}""",
            )
        assertEquals(false, dto.playable)
    }

    @Test
    fun `FavoriteList del OpenAPI se deserializa en orden`() {
        val dto =
            TolochaJson.decodeFromString<FavoriteListDto>(
                """{"items":[
                {"station":{"id":"u1","name":"Uno"},"addedAt":1700000000000},
                {"station":{"id":"u2","name":"Dos"},"addedAt":1700000001000}]}""",
            )
        assertEquals(listOf("u1", "u2"), dto.items.map { it.station.id })
        assertEquals(1700000000000, dto.items.first().addedAt)
    }

    @Test
    fun `ReorderBody se serializa como stationIds`() {
        val json = TolochaJson.encodeToString(ReorderBody(listOf("u1", "u2")))
        assertEquals("""{"stationIds":["u1","u2"]}""", json)
    }
}
