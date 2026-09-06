package com.izquierdojl.tolocharadio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidateCustomStationUseCaseTest {
    private val validate = ValidateCustomStationUseCase()

    @Test
    fun `name vacio o solo espacios es invalido`() {
        assertEquals("Escribe un nombre para la emisora.", validate.name(""))
        assertEquals("Escribe un nombre para la emisora.", validate.name("   "))
    }

    @Test
    fun `name con texto es valido`() {
        assertNull(validate.name("Radio Sierra"))
    }

    @Test
    fun `streamUrl malformada es invalida`() {
        assertEquals("La URL del stream no es válida.", validate.streamUrl("no-es-una-url"))
        assertEquals("La URL del stream no es válida.", validate.streamUrl(""))
        assertEquals("La URL del stream no es válida.", validate.streamUrl("https://"))
    }

    @Test
    fun `streamUrl con esquema no http es invalida`() {
        assertEquals(
            "La URL debe empezar por http:// o https://.",
            validate.streamUrl("ftp://emisora/live"),
        )
    }

    @Test
    fun `streamUrl http y https en cualquier host son validas`() {
        assertNull(validate.streamUrl("https://stream.ejemplo.org/live.mp3"))
        // http público también vale (difiere de NormalizeBaseUrlUseCase)
        assertNull(validate.streamUrl("http://stream.ejemplo.org/live"))
    }
}
