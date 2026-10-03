package com.izquierdojl.tolocharadio.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Serialización de los DTOs de estadísticas contra ejemplos del contrato
 * `contracts/stats-api.md` (Principio III: contratos DTO con tests).
 */
class StatsDtosTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val stationJson = """{"id":"s1","name":"Rock FM"}"""

    @Test
    fun `top deserializa station y totalMs`() {
        val dto =
            json.decodeFromString<StatsTopListDto>(
                """{"items":[{"station":$stationJson,"totalMs":120000}]}""",
            )
        val item = dto.items.single()
        assertEquals("s1", item.station.id)
        assertEquals(120000L, item.totalMs)
    }

    @Test
    fun `timeline deserializa granularidad e items`() {
        val dto =
            json.decodeFromString<StatsTimelineDto>(
                """{"granularity":"week","items":[{"bucket":"2026-09-01","totalMs":2000}]}""",
            )
        assertEquals("week", dto.granularity)
        assertEquals("2026-09-01", dto.items.single().bucket)
        assertEquals(2000L, dto.items.single().totalMs)
    }

    @Test
    fun `habits deserializa weekday hour y totalMs`() {
        val dto =
            json.decodeFromString<StatsHabitListDto>(
                """{"items":[{"weekday":3,"hour":21,"totalMs":5000}]}""",
            )
        val item = dto.items.single()
        assertEquals(3, item.weekday)
        assertEquals(21, item.hour)
        assertEquals(5000L, item.totalMs)
    }

    @Test
    fun `genres deserializa genre y totalMs`() {
        val dto =
            json.decodeFromString<StatsGenreListDto>(
                """{"items":[{"genre":"rock","totalMs":9000}]}""",
            )
        assertEquals("rock", dto.items.single().genre)
        assertEquals(9000L, dto.items.single().totalMs)
    }

    @Test
    fun `countries deserializa country con countryCode nulo`() {
        val explicitNull =
            json.decodeFromString<StatsCountryListDto>(
                """{"items":[{"country":"Espana","countryCode":null,"totalMs":1000}]}""",
            )
        val missing =
            json.decodeFromString<StatsCountryListDto>(
                """{"items":[{"country":"Francia","totalMs":1000}]}""",
            )
        assertNull(explicitNull.items.single().countryCode)
        assertNull(missing.items.single().countryCode)
        assertEquals("Espana", explicitNull.items.single().country)
    }

    @Test
    fun `recent deserializa startedAt y durationMs`() {
        val dto =
            json.decodeFromString<StatsRecentListDto>(
                """{"items":[{"station":$stationJson,"startedAt":1757000000000,"durationMs":300000}]}""",
            )
        val item = dto.items.single()
        assertEquals(1757000000000L, item.startedAt)
        assertEquals(300000L, item.durationMs)
    }

    @Test
    fun `campos desconocidos se ignoran`() {
        val dto =
            json.decodeFromString<StatsTopListDto>(
                """{"items":[{"station":$stationJson,"totalMs":1,"futuro":true}],"meta":{}}""",
            )
        assertEquals(1L, dto.items.single().totalMs)
    }
}
