package com.izquierdojl.tolocharadio.core.network

import com.izquierdojl.tolocharadio.core.session.AuthState
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.remote.api.AuthApi
import com.izquierdojl.tolocharadio.data.remote.api.RefreshBody
import com.izquierdojl.tolocharadio.data.remote.dto.AuthResponseDto
import com.izquierdojl.tolocharadio.data.remote.dto.ThemeDto
import com.izquierdojl.tolocharadio.data.remote.dto.UserDto
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class TokenAuthenticatorTest {
    private val store: TokenStore =
        mockk(relaxed = true) {
            every { getRefresh() } returns "refresh-viejo"
        }
    private val session = SessionManager(store)
    private val api: AuthApi = mockk()
    private val lazyApi =
        object : dagger.Lazy<AuthApi> {
            override fun get(): AuthApi = api
        }
    private val authenticator = TokenAuthenticator(session, store, lazyApi)
    private val user = UserDto(1, "a@b.c", null, ThemeDto.DARK, 0)

    private fun responseWith(authHeader: String?): okhttp3.Response {
        val request =
            Request.Builder()
                .url("https://radio.test/api/v1/stations")
                .apply { if (authHeader != null) header("Authorization", authHeader) }
                .build()
        return mockk {
            every { this@mockk.request } returns request
            every { priorResponse } returns null
        }
    }

    @Test
    fun `401 renueva y reintenta con el token nuevo`() =
        runTest {
            session.setAuthenticated(user, "access-viejo", "refresh-viejo")
            coEvery { api.refresh(RefreshBody("refresh-viejo")) } returns
                Response.success(AuthResponseDto(user, "access-nuevo", "refresh-nuevo"))
            val next = authenticator.authenticate(null, responseWith("Bearer access-viejo"))
            assertEquals("Bearer access-nuevo", next?.header("Authorization"))
            assertTrue(session.authState.value is AuthState.Authenticated)
        }

    @Test
    fun `refresh fallido cierra sesion sin reintento`() =
        runTest {
            session.setAuthenticated(user, "access-viejo", "refresh-viejo")
            coEvery { api.refresh(any()) } returns
                Response.error(
                    401,
                    "{}".toResponseBody("application/json".toMediaType()),
                )
            val next = authenticator.authenticate(null, responseWith("Bearer access-viejo"))
            assertNull(next)
            assertTrue(session.authState.value is AuthState.Unauthenticated)
        }
}

