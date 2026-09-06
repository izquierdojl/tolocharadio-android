package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.remote.api.AuthApi
import com.izquierdojl.tolocharadio.data.remote.api.LoginBody
import com.izquierdojl.tolocharadio.data.remote.dto.AuthResponseDto
import com.izquierdojl.tolocharadio.data.remote.dto.ThemeDto
import com.izquierdojl.tolocharadio.data.remote.dto.UserDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AuthRepoTest {
    private val store: TokenStore = mockk(relaxed = true)
    private val session = SessionManager(store)
    private val api: AuthApi = mockk()
    private val repo = AuthRepo(api, session)
    private val user = UserDto(1, "a@b.c", "Ana", ThemeDto.DARK, 0)

    @Test
    fun `login OK deja sesion lista`() =
        runTest {
            coEvery { api.login(LoginBody("a@b.c", "secreta123")) } returns
                Response.success(AuthResponseDto(user, "a", "r"))
            val r = repo.login("a@b.c", "secreta123")
            assertTrue(r is ApiResult.Ok)
            assertTrue(session.authState.value is com.izquierdojl.tolocharadio.core.session.AuthState.Authenticated)
        }

    @Test
    fun `login 401 devuelve Unauthorized`() =
        runTest {
            coEvery { api.login(any()) } returns
                Response.error(
                    401,
                    "{}".toResponseBody("application/json".toMediaType()),
                )
            val r = repo.login("a@b.c", "mala")
            assertTrue(
                (r as ApiResult.Err).error is com.izquierdojl.tolocharadio.core.network.DomainError.Unauthorized,
            )
        }
}
