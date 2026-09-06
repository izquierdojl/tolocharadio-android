package com.izquierdojl.tolocharadio.auth.data.local

import com.izquierdojl.tolocharadio.core.session.AuthState
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.remote.dto.ThemeDto
import com.izquierdojl.tolocharadio.data.remote.dto.UserDto
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionManagerTest {
    private lateinit var sessionManager: SessionManager
    private val mockTokens = mockk<TokenStore>(relaxed = true)

    @Before
    fun setup() {
        sessionManager = SessionManager(mockTokens)
    }

    @Test
    fun `initial state is Loading`() {
        assertTrue(sessionManager.authState.value is AuthState.Loading)
    }

    @Test
    fun `setAuthenticated updates state to Authenticated`() =
        runTest {
            val user = UserDto(id = 1, email = "test@test.com", name = "Test", theme = ThemeDto.DARK, createdAt = 0L)
            sessionManager.setAuthenticated(user, "access-token", "refresh-token")
            assertTrue(sessionManager.authState.value is AuthState.Authenticated)
            assertEquals(user, (sessionManager.authState.value as AuthState.Authenticated).user)
        }

    @Test
    fun `accessTokenNow returns current access token`() =
        runTest {
            val user = UserDto(id = 1, email = "test@test.com", name = "Test", theme = ThemeDto.DARK, createdAt = 0L)
            sessionManager.setAuthenticated(user, "access-token", "refresh-token")
            assertEquals("access-token", sessionManager.accessTokenNow())
        }

    @Test
    fun `accessTokenNow returns null when not authenticated`() {
        assertNull(sessionManager.accessTokenNow())
    }

    @Test
    fun `logout clears state to Unauthenticated`() =
        runTest {
            val user = UserDto(id = 1, email = "test@test.com", name = "Test", theme = ThemeDto.DARK, createdAt = 0L)
            sessionManager.setAuthenticated(user, "access-token", "refresh-token")
            sessionManager.logout()
            assertTrue(sessionManager.authState.value is AuthState.Unauthenticated)
            assertNull(sessionManager.accessTokenNow())
        }

    @Test
    fun `logout with reason sets reason`() =
        runTest {
            sessionManager.logout("expired")
            val state = sessionManager.authState.value as AuthState.Unauthenticated
            assertEquals("expired", state.reason)
        }

    @Test
    fun `setUser updates user when authenticated`() =
        runTest {
            val user1 = UserDto(id = 1, email = "test@test.com", name = "Test", theme = ThemeDto.DARK, createdAt = 0L)
            val user2 = UserDto(id = 1, email = "test@test.com", name = "Updated", theme = ThemeDto.DARK, createdAt = 0L)
            sessionManager.setAuthenticated(user1, "access-token", "refresh-token")
            sessionManager.setUser(user2)
            assertEquals("Updated", (sessionManager.authState.value as AuthState.Authenticated).user.name)
        }

    @Test
    fun `setAccess updates access token`() =
        runTest {
            val user = UserDto(id = 1, email = "test@test.com", name = "Test", theme = ThemeDto.DARK, createdAt = 0L)
            sessionManager.setAuthenticated(user, "old-token", "refresh-token")
            sessionManager.setAccess("new-token")
            assertEquals("new-token", sessionManager.accessTokenNow())
        }

    @Test
    fun `markRestoring sets Loading state`() =
        runTest {
            val user = UserDto(id = 1, email = "test@test.com", name = "Test", theme = ThemeDto.DARK, createdAt = 0L)
            sessionManager.setAuthenticated(user, "access-token", "refresh-token")
            sessionManager.markRestoring()
            assertTrue(sessionManager.authState.value is AuthState.Loading)
        }
}
