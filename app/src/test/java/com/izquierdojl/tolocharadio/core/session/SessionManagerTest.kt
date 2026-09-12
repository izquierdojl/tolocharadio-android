package com.izquierdojl.tolocharadio.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
}
