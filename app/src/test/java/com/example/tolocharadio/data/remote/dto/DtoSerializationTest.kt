package com.example.tolocharadio.data.remote.dto

import com.example.tolocharadio.core.network.TolochaJson
import kotlinx.serialization.decodeFromString
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
    fun `AuthResponse con usuario y tokens`() {
        val dto =
            TolochaJson.decodeFromString<AuthResponseDto>(
                """{"user":{"id":1,"email":"a@b.c","name":"Ana","theme":"dark",
               "createdAt":1700000000000},"accessToken":"a","refreshToken":"r"}""",
            )
        assertEquals(ThemeDto.DARK, dto.user.theme)
        assertEquals("r", dto.refreshToken)
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
}
