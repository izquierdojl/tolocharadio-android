package com.izquierdojl.tolocharadio.servers.domain.usecase

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import com.izquierdojl.tolocharadio.domain.servers.UpdateServerUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateServerUseCaseTest {
    private lateinit var useCase: UpdateServerUseCase
    private val repository = mockk<ServerRepository>(relaxed = true)

    @Before
    fun setup() {
        useCase = UpdateServerUseCase(repository)
    }

    @Test
    fun `invoke valida alias email y password`() =
        runTest {
            assertTrue(useCase("1", "", "a@b.c", "secreta123") is ApiResult.Err)
            assertTrue(useCase("1", "srv", "mal", "secreta123") is ApiResult.Err)
            assertTrue(useCase("1", "srv", "a@b.c", "") is ApiResult.Err)
        }

    @Test
    fun `invoke revalida con login y actualiza el alias (FR-007)`() =
        runTest {
            val server =
                SavedServerEntity(
                    id = "1",
                    url = "https://radio.example.com",
                    alias = "Nuevo",
                    appName = "TolochaRadio",
                )
            coEvery { repository.update("1", "Nuevo", "a@b.c", "nueva123") } returns ApiResult.Ok(server)
            val result = useCase("1", "Nuevo", "a@b.c", "nueva123")
            assertTrue(result is ApiResult.Ok)
            assertEquals("Nuevo", (result as ApiResult.Ok).value.alias)
            coVerify { repository.update("1", "Nuevo", "a@b.c", "nueva123") }
        }

    @Test
    fun `invoke propaga error de credenciales`() =
        runTest {
            coEvery { repository.update(any(), any(), any(), any()) } returns
                ApiResult.Err(DomainError.Unauthorized("bad_credentials"))
            val result = useCase("1", "srv", "a@b.c", "mala")
            assertTrue(result is ApiResult.Err)
        }
}
