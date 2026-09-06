package com.izquierdojl.tolocharadio.servers.domain.usecase

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import com.izquierdojl.tolocharadio.domain.servers.SwitchServerUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SwitchServerUseCaseTest {
    private lateinit var useCase: SwitchServerUseCase
    private val repository = mockk<ServerRepository>(relaxed = true)

    @Before
    fun setup() {
        useCase = SwitchServerUseCase(repository)
    }

    @Test
    fun `invoke returns error when server not found`() =
        runTest {
            coEvery { repository.switchTo("nonexistent") } returns
                ApiResult.Err(
                    com.izquierdojl.tolocharadio.core.network.DomainError.NotFound("not_found"),
                )
            val result = useCase("nonexistent")
            assertTrue(result is ApiResult.Err)
        }

    @Test
    fun `invoke returns server on success`() =
        runTest {
            val server =
                SavedServerEntity(
                    id = "1",
                    url = "https://radio.example.com",
                    alias = "My Server",
                    appName = "TolochaRadio",
                    isDefault = true,
                )
            coEvery { repository.switchTo("1") } returns ApiResult.Ok(server)
            val result = useCase("1")
            assertTrue(result is ApiResult.Ok)
            assertEquals("My Server", (result as ApiResult.Ok).value.alias)
            assertEquals(true, result.value.isDefault)
        }
}
