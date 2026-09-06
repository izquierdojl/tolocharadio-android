package com.izquierdojl.tolocharadio.data.remote.dto

import com.izquierdojl.tolocharadio.core.network.TolochaJson
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryDtoTest {
    @Test
    fun `HistoryEntryDto se deserializa con station y playedAt`() {
        val json =
            "{\"station\":{\"id\":\"s1\",\"name\":\"Rock FM\"}," +
                "\"playedAt\":1700000000000}"
        val dto = TolochaJson.decodeFromString<HistoryEntryDto>(json)
        assertEquals("s1", dto.station.id)
        assertEquals("Rock FM", dto.station.name)
        assertEquals(1700000000000, dto.playedAt)
    }

    @Test
    fun `HistoryEntryDto con playedAt por defecto es 0`() {
        val dto =
            TolochaJson.decodeFromString<HistoryEntryDto>(
                """{"station":{"id":"s2","name":"Jazz","url":"http://x/stream","isCustom":false}}""",
            )
        assertEquals(0L, dto.playedAt)
    }

    @Test
    fun `HistoryListDto se deserializa con items vacios`() {
        val dto =
            TolochaJson.decodeFromString<HistoryListDto>(
                """{"items":[]}""",
            )
        assertEquals(emptyList<HistoryEntryDto>(), dto.items)
    }

    @Test
    fun `HistoryListDto se deserializa con multiples entradas`() {
        val json =
            "{\"items\":[" +
                "{\"station\":{\"id\":\"s1\",\"name\":\"Rock\"},\"playedAt\":3000}," +
                "{\"station\":{\"id\":\"s2\",\"name\":\"Jazz\"},\"playedAt\":2000}," +
                "{\"station\":{\"id\":\"s1\",\"name\":\"Rock\"},\"playedAt\":1000}]}"
        val dto = TolochaJson.decodeFromString<HistoryListDto>(json)
        assertEquals(3, dto.items.size)
        assertEquals("s1", dto.items[0].station.id)
        assertEquals(3000L, dto.items[0].playedAt)
    }
}

