package com.izquierdojl.tolocharadio.feature.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppInfoFormatTest {
    private val base =
        AppInfo(
            appName = "Tolocha Radio",
            version = "2.3.1",
            versionCode = 42,
            applicationId = "com.izquierdojl.tolocharadio",
            buildType = "Publicación",
            developer = "izquierdojl",
            license = "MIT",
            repositoryUrl = "https://github.com/izquierdojl/tolocharadio-android",
            theme = "Oscuro",
            startScreen = "Favoritos",
            activeServerAlias = "Mi servidor",
        )

    @Test
    fun `incluye todas las etiquetas`() {
        val text = base.toClipboardText()
        assertTrue(text.contains("Versión: 2.3.1"))
        assertTrue(text.contains("Número de compilación: 42"))
        assertTrue(text.contains("Identificador: com.izquierdojl.tolocharadio"))
        assertTrue(text.contains("Tipo de build: Publicación"))
        assertTrue(text.contains("Tema: Oscuro"))
        assertTrue(text.contains("Pantalla de arranque: Favoritos"))
        assertTrue(text.contains("Servidor activo: Mi servidor"))
        assertTrue(text.contains("Desarrollador: izquierdojl"))
        assertTrue(text.contains("Licencia: MIT"))
        assertTrue(text.contains("Repositorio: https://github.com/izquierdojl/tolocharadio-android"))
    }

    @Test
    fun `omite servidor activo cuando es null`() {
        val text = base.copy(activeServerAlias = null).toClipboardText()
        assertFalse(text.contains("Servidor activo"))
    }

    @Test
    fun `omite servidor activo cuando esta en blanco`() {
        val text = base.copy(activeServerAlias = "   ").toClipboardText()
        assertFalse(text.contains("Servidor activo"))
    }

    @Test
    fun `no contiene credenciales tokens ni PII`() {
        val text = base.toClipboardText().lowercase()
        listOf("password", "contraseña", "token", "authorization", "bearer", "email").forEach {
            assertFalse("El texto no debe contener '$it'", text.contains(it))
        }
    }
}
