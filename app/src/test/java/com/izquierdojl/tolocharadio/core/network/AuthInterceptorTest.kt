package com.izquierdojl.tolocharadio.core.network

import com.izquierdojl.tolocharadio.core.session.SessionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import okhttp3.Interceptor
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthInterceptorTest {
    private val session = SessionManager()
    private val interceptor = AuthInterceptor(session)

    private fun capturedRequest(authHeader: String?): Request? {
        if (authHeader != null) session.setAccess(authHeader.removePrefix("Bearer "))
        val request = Request.Builder().url("https://radio.test/api/v1/favorites").build()
        val slot = slot<Request>()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(capture(slot)) } returns mockk(relaxed = true)
        interceptor.intercept(chain)
        return slot.captured
    }

    @Test
    fun `con sesion anade Authorization Bearer`() {
        val captured = capturedRequest("Bearer access-1")
        assertEquals("Bearer access-1", captured?.header("Authorization"))
    }

    @Test
    fun `sin sesion no anade Authorization`() {
        val captured = capturedRequest(null)
        assertNull(captured?.header("Authorization"))
    }
}
