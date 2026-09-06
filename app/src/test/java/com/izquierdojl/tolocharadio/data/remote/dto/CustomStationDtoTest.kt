package com.izquierdojl.tolocharadio.data.remote.dto

import com.izquierdojl.tolocharadio.core.network.TolochaJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomStationDtoTest {
    @Test
    fun `CreateCustomStationBody se serializa con name y url`() {
        val body = CreateCustomStationBody("Radio Sierra", "https://stream.ejemplo.org/live.mp3")
        val json = TolochaJson.encodeToString(body)
        assertTrue(json.contains(""""name":"Radio Sierra""""))
        assertTrue(json.contains(""""url":"https://stream.ejemplo.org/live.mp3""""))
    }

    @Test
    fun `CustomStationResultDto se deserializa con station isCustom`() {
        val json =
            "{\"station\":{\"id\":\"c1\",\"name\":\"Radio Sierra\"," +
                "\"url\":\"https://stream.ejemplo.org/live.mp3\",\"isCustom\":true}}"
        val dto = TolochaJson.decodeFromString<CustomStationResultDto>(json)
        assertEquals("c1", dto.station.id)
        assertEquals("Radio Sierra", dto.station.name)
        assertEquals(true, dto.station.isCustom)
    }

    @Test
    fun `StationListDto sirve de envoltorio para GET custom-stations`() {
        val json = """{"items":[{"id":"c1","name":"Radio Sierra","isCustom":true}]}"""
        val dto = TolochaJson.decodeFromString<StationListDto>(json)
        assertEquals("c1", dto.items.single().id)
        assertEquals(true, dto.items.single().isCustom)
    }
}
