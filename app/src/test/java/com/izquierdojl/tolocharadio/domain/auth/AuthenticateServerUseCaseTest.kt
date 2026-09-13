package com.izquierdojl.tolocharadio.domain.auth

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity
import com.izquierdojl.tolocharadio.data.repo.AuthRepo
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticateServerUseCaseTest {
    private val repository: ServerRepository = mockk(relaxed = true)
    private val authRepo: AuthRepo = mockk(relaxed = true)
    private val tokens: TokenStore = mockk(relaxed = true)
    private val useCase = AuthenticateServerUseCase(repository, authRepo, tokens)

    private val server = SavedServerEntity(id = "srv1", url = "https://radio.test", alias = "Test")

    @Test
    fun `fija el servidor activo y asegura su sesion`() =
        runTest {
            coEvery { repository.getStartupServer() } returns server
            coEvery { authRepo.ensureSession("srv1", "https://radio.test") } returns ApiResult.Ok(Unit)

            val r = useCase()

            assertTrue(r is ApiResult.Ok)
            verify { tokens.setActiveServerId("srv1") }
        }

    @Test
    fun `sin servidor de arranque no toca credenciales`() =
        runTest {
            coEvery { repository.getStartupServer() } returns null

            val r = useCase()

            assertTrue((r as ApiResult.Err).error is DomainError.NotFound)
            verify(exactly = 0) { tokens.setActiveServerId(any()) }
        }
}
