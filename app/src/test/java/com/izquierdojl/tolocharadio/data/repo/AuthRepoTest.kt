package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.core.session.SessionState
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.remote.InstanceApiFactory
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
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AuthRepoTest {
    private val store: TokenStore = mockk(relaxed = true)
    private val session = SessionManager()
    private val api: AuthApi = mockk()
    private val apiFactory: InstanceApiFactory = mockk()

    private val repo =
        AuthRepo(session, store, apiFactory).also {
            every { apiFactory.authApi(any()) } returns api
        }

    @Test
    fun `login OK guarda credenciales y deja sesion lista (FR-001)`() =
        runTest {
            coEvery { api.login(LoginBody("a@b.c", "secreta123")) } returns
                Response.success(AuthResponseDto("a", "r"))

            val r = repo.login("srv1", "https://radio.test", "a@b.c", "secreta123")

            assertTrue(r is ApiResult.Ok)
            assertEquals(SessionState.Ready, session.state.value)
            coVerify { store.setCredentials("srv1", "a@b.c", "secreta123", "r") }
            coVerify { apiFactory.authApi("https://radio.test") }
        }

    @Test
    fun `login 401 devuelve Unauthorized`() =
        runTest {
            coEvery { api.login(any()) } returns
                Response.error(401, "{}".toResponseBody("application/json".toMediaType()))

            val r = repo.login("srv1", "https://radio.test", "a@b.c", "mala")

            assertTrue((r as ApiResult.Err).error is DomainError.Unauthorized)
        }

    @Test
    fun `ensureSession sin credenciales devuelve Unauthorized`() =
        runTest {
            every { store.getCredentials("srv1") } returns null
            val r = repo.ensureSession("srv1", "https://radio.test")
            assertTrue((r as ApiResult.Err).error is DomainError.Unauthorized)
        }

    @Test
    fun `ensureSession con refresh valido no re-loguea`() =
        runTest {
            every { store.getCredentials("srv1") } returns
                TokenStore.ServerCredentials("a@b.c", "secreta123", "refresh-1")
            coEvery { api.refresh(RefreshBody("refresh-1")) } returns
                Response.success(AuthResponseDto("access-1", "refresh-2"))

            val r = repo.ensureSession("srv1", "https://radio.test")

            assertTrue(r is ApiResult.Ok)
            coVerify(exactly = 0) { api.login(any()) }
            assertEquals(SessionState.Ready, session.state.value)
        }

    @Test
    fun `ensureSession con refresh fallido re-loguea`() =
        runTest {
            every { store.getCredentials("srv1") } returns
                TokenStore.ServerCredentials("a@b.c", "secreta123", "refresh-viejo")
            coEvery { api.refresh(any()) } returns
                Response.error(401, "{}".toResponseBody("application/json".toMediaType()))
            coEvery { api.login(LoginBody("a@b.c", "secreta123")) } returns
                Response.success(AuthResponseDto("access-2", "refresh-3"))

            val r = repo.ensureSession("srv1", "https://radio.test")

            assertTrue(r is ApiResult.Ok)
            assertEquals(SessionState.Ready, session.state.value)
        }
}
