package com.izquierdojl.tolocharadio.core.session

import com.izquierdojl.tolocharadio.data.remote.dto.ThemeDto
import com.izquierdojl.tolocharadio.data.remote.dto.UserDto
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {
    private val store: TokenStore =
        mockk {
            every { setRefresh(any()) } just runs
        }
    private val session = SessionManager(store)
    private val user = UserDto(1, "a@b.c", "Ana", ThemeDto.DARK, 0)

    @Test
    fun `setAuthenticated guarda access y emite Authenticated`() {
        session.setAuthenticated(user, "access", "refresh")
        assertEquals("access", session.accessTokenNow())
        assertTrue(session.authState.value is AuthState.Authenticated)
        verify { store.setRefresh("refresh") }
    }

    @Test
    fun `logout borra secretos y emite Unauthenticated`() {
        session.setAuthenticated(user, "access", "refresh")
        session.logout()
        assertNull(session.accessTokenNow())
        assertTrue(session.authState.value is AuthState.Unauthenticated)
        verify { store.setRefresh(null) }
    }
}
