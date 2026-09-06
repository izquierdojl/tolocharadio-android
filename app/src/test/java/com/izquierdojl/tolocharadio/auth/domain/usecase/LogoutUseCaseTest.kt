package com.izquierdojl.tolocharadio.auth.domain.usecase

import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.repo.AuthRepo
import com.izquierdojl.tolocharadio.domain.auth.LogoutUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class LogoutUseCaseTest {
    private lateinit var useCase: LogoutUseCase
    private val authRepo = mockk<AuthRepo>(relaxed = true)
    private val tokens = mockk<TokenStore>(relaxed = true)

    @Before
    fun setup() {
        useCase = LogoutUseCase(authRepo, tokens)
    }

    @Test
    fun `logout limpia el snapshot de refresh del servidor activo (FR-007)`() = runTest {
        every { tokens.getActiveServerId() } returns "srv1"
        every { tokens.getServerCredentials("srv1") } returns
            TokenStore.ServerCredentials("refresh-viejo", "a@b.c", "secreta123")

        useCase()

        coVerify { authRepo.logout() }
        coVerify {
            tokens.setServerCredentials("srv1", refresh = null, email = "a@b.c", password = "secreta123")
        }
    }

    @Test
    fun `logout conserva email y password para reconexión (FR-014)`() = runTest {
        every { tokens.getActiveServerId() } returns "srv1"
        every { tokens.getServerCredentials("srv1") } returns
            TokenStore.ServerCredentials("refresh-viejo", "a@b.c", "secreta123")

        useCase()

        coVerify {
            tokens.setServerCredentials("srv1", refresh = null, email = "a@b.c", password = "secreta123")
        }
    }

    @Test
    fun `logout sin servidor activo solo cierra sesión`() = runTest {
        every { tokens.getActiveServerId() } returns null

        useCase()

        coVerify { authRepo.logout() }
        coVerify(exactly = 0) { tokens.setServerCredentials(any(), any(), any(), any()) }
    }
}
