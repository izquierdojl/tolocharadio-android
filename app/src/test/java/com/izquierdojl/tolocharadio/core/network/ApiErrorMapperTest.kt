package com.izquierdojl.tolocharadio.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiErrorMapperTest {
    @Test
    fun `401 mapea a Unauthorized`() {
        val e = mapHttpError(401, null)
        assertTrue(e is DomainError.Unauthorized)
    }

    @Test
    fun `404 mapea a NotFound con codigo del servidor`() {
        val e = mapHttpError(404, """{"error":{"code":"STATION_GONE","message":"x","status":404}}""")
        assertEquals(DomainError.NotFound("STATION_GONE"), e)
    }

    @Test
    fun `409 mapea a Conflict`() {
        val e = mapHttpError(409, """{"error":{"code":"EMAIL_TAKEN","message":"En uso","status":409}}""")
        assertTrue(e is DomainError.Conflict)
    }

    @Test
    fun `422 mapea a Validation con detalles por campo`() {
        val e =
            mapHttpError(
                422,
                """{"error":{"code":"INVALID","message":"m","status":422,
                "details":[{"field":"password","message":"Mínimo 8"}]}}""",
            )
        assertEquals(DomainError.Validation(listOf(FieldError("password", "Mínimo 8"))), e)
    }

    @Test
    fun `503 mapea a Unavailable`() {
        assertTrue(mapHttpError(503, null) is DomainError.Unavailable)
    }

    @Test
    fun `mensajes de usuario en espanol sin texto tecnico`() {
        assertTrue(DomainError.Unauthorized("X").userMessage().contains("sesión"))
        assertTrue(DomainError.Unavailable("x").userMessage().contains("conexión"))
    }
}

