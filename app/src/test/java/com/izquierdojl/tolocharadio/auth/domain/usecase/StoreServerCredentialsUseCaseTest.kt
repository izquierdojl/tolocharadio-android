package com.izquierdojl.tolocharadio.auth.domain.usecase

import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import com.izquierdojl.tolocharadio.domain.auth.StoreServerCredentialsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class StoreServerCredentialsUseCaseTest {
    private lateinit var useCase: StoreServerCredentialsUseCase
    private val repository = mockk<ServerRepository>(relaxed = true)
    private val tokens = mockk<TokenStore>(relaxed = true)

    @Before
    fun setup() {
        useCase = StoreServerCredentialsUseCase(repository, tokens)
    }

    @Test
    fun `guarda email y password cifrados del servidor activo (FR-005)`() =
        runTest {
            coEvery { repository.getActive() } returns
                SavedServerEntity(
                    id = "srv1",
                    url = "https://srv.example.com",
                    alias = "srv",
                    isActive = true,
                    isDefault = true,
                )

            useCase("  a@b.c  ", "secreta123")

            coVerify { tokens.setActiveServerId("srv1") }
            coVerify { tokens.saveServerAuth("srv1", "a@b.c", "secreta123") }
        }

    @Test
    fun `sin servidor activo no hace nada`() =
        runTest {
            coEvery { repository.getActive() } returns null
            useCase("a@b.c", "secreta123")
            coVerify(exactly = 0) { tokens.saveServerAuth(any(), any(), any()) }
        }
}
