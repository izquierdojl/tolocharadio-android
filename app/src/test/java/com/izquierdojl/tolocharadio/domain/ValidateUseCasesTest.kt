package com.izquierdojl.tolocharadio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NormalizeBaseUrlUseCaseTest {
    private val useCase = NormalizeBaseUrlUseCase()

    @Test
    fun `anade https y quita barra final`() {
        assertEquals("https://radio.mi-dominio.com", useCase("radio.mi-dominio.com/"))
    }

    @Test
    fun `rechaza http no local`() {
        assertNull(useCase("http://radio.mi-dominio.com"))
    }

    @Test
    fun `acepta http local`() {
        assertEquals("http://192.168.1.10:3000", useCase("http://192.168.1.10:3000/"))
    }

    @Test
    fun `rechaza vacia y sin host`() {
        assertNull(useCase("   "))
        assertNull(useCase("https://"))
    }
}

class ValidateAuthUseCaseTest {
    private val validate = ValidateAuthUseCase()

    @Test
    fun `email invalido`() {
        assertEquals(true, validate.email("no-es-email") != null)
        assertEquals(null, validate.email("ana@radio.com"))
    }

    @Test
    fun `password 8 a 72`() {
        assertEquals(true, validate.password("corta") != null)
        assertEquals(null, validate.password("suficiente1"))
    }
}
