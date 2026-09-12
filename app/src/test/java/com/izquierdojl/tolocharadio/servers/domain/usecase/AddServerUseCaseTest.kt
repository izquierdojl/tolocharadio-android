package com.izquierdojl.tolocharadio.servers.domain.usecase

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import com.izquierdojl.tolocharadio.domain.servers.AddServerUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddServerUseCaseTest {
    private lateinit var useCase: AddServerUseCase
    private val repository = mockk<ServerRepository>(relaxed = true)

    @Before
    fun setup() {
        useCase = AddServerUseCase(repository)
    }

    @Test
    fun `invoke returns error when alias is blank`() =
        runTest {
            val result = useCase("https://radio.example.com", "", "a@b.c", "secreta123")
            assertTrue(result is ApiResult.Err)
        }

    @Test
    fun `invoke returns error when email is invalid`() =
        runTest {
            val result = useCase("https://radio.example.com", "srv", "no-es-email", "secreta123")
            assertTrue(result is ApiResult.Err)
        }

    @Test
    fun `invoke returns error when password is empty`() =
        runTest {
            val result = useCase("https://radio.example.com", "srv", "a@b.c", "")
            assertTrue(result is ApiResult.Err)
        }

    @Test
    fun `invoke returns error when repository add fails`() =
        runTest {
            coEvery { repository.add(any(), any(), any(), any()) } returns
                ApiResult.Err(DomainError.Unavailable("Connection failed"))
            val result = useCase("https://radio.example.com", "My Server", "a@b.c", "secreta123")
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
                    isDefault = false,
                )
            coEvery { repository.add(any(), any(), any(), any()) } returns ApiResult.Ok(server)
            val result = useCase("https://radio.example.com", "My Server", "a@b.c", "secreta123")
            assertTrue(result is ApiResult.Ok)
            assertEquals("My Server", (result as ApiResult.Ok).value.alias)
        }

    @Test
    fun `invoke propaga credenciales al repositorio (FR-001)`() =
        runTest {
            coEvery { repository.add(any(), any(), any(), any()) } returns
                ApiResult.Err(DomainError.Unavailable("x"))
            useCase("https://radio.example.com", "srv", "a@b.c", "secreta123")
            coVerify { repository.add("https://radio.example.com", "srv", "a@b.c", "secreta123") }
        }
}
