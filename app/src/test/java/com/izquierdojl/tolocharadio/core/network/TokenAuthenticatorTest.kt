package com.izquierdojl.tolocharadio.core.network

import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.core.session.SessionState
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.remote.api.AuthApi
import com.izquierdojl.tolocharadio.data.remote.api.LoginBody
import com.izquierdojl.tolocharadio.data.remote.api.RefreshBody
import com.izquierdojl.tolocharadio.data.remote.dto.AuthResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response

class TokenAuthenticatorTest {
    private val creds = TokenStore.ServerCredentials("a@b.c", "secreta123", "refresh-viejo")
    private val store: TokenStore =
        mockk(relaxed = true) {
            every { getActiveServerId() } returns "srv1"
            every { getCredentials("srv1") } returns creds
        }
    private val session = SessionManager()
    private val api: AuthApi = mockk()
    private val lazyApi =
        object : dagger.Lazy<AuthApi> {
            override fun get(): AuthApi = api
        }
    private val authenticator = TokenAuthenticator(session, store, lazyApi)

    private fun responseWith(authHeader: String?): okhttp3.Response {
        val request =
            Request.Builder()
                .url("https://radio.test/api/v1/favorites")
                .apply { if (authHeader != null) header("Authorization", authHeader) }
                .build()
        val response = mockk<okhttp3.Response>()
        every { response.request } returns request
        every { response.priorResponse } returns null
        return response
    }

    @Test
    fun `401 renueva con el refresh y reintenta una vez (FR-005)`() =
        runTest {
            session.setAccess("access-viejo")
            coEvery { api.refresh(RefreshBody("refresh-viejo")) } returns
                Response.success(AuthResponseDto("access-nuevo", "refresh-nuevo"))

            val next = authenticator.authenticate(null, responseWith("Bearer access-viejo"))

            assertEquals("Bearer access-nuevo", next?.header("Authorization"))
            assertEquals(SessionState.Ready, session.state.value)
            coVerify { store.updateRefresh("srv1", "refresh-nuevo") }
        }

    @Test
    fun `refresh fallido re-loguea con las credenciales guardadas`() =
        runTest {
            session.setAccess("access-viejo")
            coEvery { api.refresh(any()) } returns
                Response.error(401, "{}".toResponseBody("application/json".toMediaType()))
            coEvery { api.login(LoginBody("a@b.c", "secreta123")) } returns
                Response.success(AuthResponseDto("access-2", "refresh-2"))

            val next = authenticator.authenticate(null, responseWith("Bearer access-viejo"))

            assertEquals("Bearer access-2", next?.header("Authorization"))
        }

    @Test
    fun `refresh y re-login fallidos limpian la sesion`() =
        runTest {
            session.setAccess("access-viejo")
            coEvery { api.refresh(any()) } returns
                Response.error(401, "{}".toResponseBody("application/json".toMediaType()))
            coEvery { api.login(any()) } returns
                Response.error(401, "{}".toResponseBody("application/json".toMediaType()))

            val next = authenticator.authenticate(null, responseWith("Bearer access-viejo"))

            assertNull(next)
            assertEquals(SessionState.Idle, session.state.value)
        }
}
