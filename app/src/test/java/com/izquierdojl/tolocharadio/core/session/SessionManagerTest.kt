package com.izquierdojl.tolocharadio.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class SessionManagerTest {
    private val session = SessionManager()

    @Test
    fun `setAccess guarda el token y emite Ready`() {
        session.setAccess("access")
        assertEquals("access", session.accessTokenNow())
        assertEquals(SessionState.Ready, session.state.value)
    }

    @Test
    fun `setAuthenticating emite Authenticating`() {
        session.setAuthenticating()
        assertEquals(SessionState.Authenticating, session.state.value)
    }

    @Test
    fun `clear borra el token y emite Idle`() {
        session.setAccess("access")
        session.clear()
        assertNull(session.accessTokenNow())
        assertEquals(SessionState.Idle, session.state.value)
    }

    @Test
    fun `detecta JWT que caduca dentro del margen`() {
        session.setAccess(jwtWithExp(1_000))
        assertTrue(session.isAccessTokenExpiring(nowEpochSeconds = 950, skewSeconds = 60))
        assertFalse(session.isAccessTokenExpiring(nowEpochSeconds = 800, skewSeconds = 60))
    }

    @Test
    fun `token opaco no tiene caducidad conocida`() {
        session.setAccess("opaque-token")
        assertFalse(session.isAccessTokenExpiring(nowEpochSeconds = Long.MAX_VALUE / 2))
    }

    private fun jwtWithExp(exp: Long): String {
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"alg":"none"}""".toByteArray())
        val payload =
            Base64.getUrlEncoder().withoutPadding()
                .encodeToString("""{"exp":$exp}""".toByteArray())
        return "$header.$payload.signature"
    }
}
