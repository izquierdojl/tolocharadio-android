package com.izquierdojl.tolocharadio.core.network

import okhttp3.Request
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

/**
 * Regresión FR-011: el cliente HTTP de la app no añade ninguna credencial
 * (ni `Authorization` ni tokens en la URL).
 */
class NoAuthRequestsTest {
    private class Captured(val request: Request) : IOException("captured")

    @Test
    fun `el cliente no añade Authorization`() {
        val client = RetrofitFactory.createHttpClient(debug = false)
        val probe =
            client
                .newBuilder()
                .addInterceptor { chain -> throw Captured(chain.request()) }
                .build()
        val request =
            Request
                .Builder()
                .url("https://instancia.invalid/api/v1/stations")
                .build()

        var captured: Request? = null
        try {
            probe.newCall(request).execute()
        } catch (e: Captured) {
            captured = e.request
        } catch (_: IOException) {
            // Sin red en el entorno de test.
        }

        assertNull(captured?.header("Authorization"))
        assertNull(captured?.url?.queryParameter("token"))
    }
}
