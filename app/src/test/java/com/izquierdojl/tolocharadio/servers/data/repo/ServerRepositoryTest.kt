package com.izquierdojl.tolocharadio.servers.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.local.CacheManager
import com.izquierdojl.tolocharadio.data.local.servers.ServerDao
import com.izquierdojl.tolocharadio.data.remote.api.SystemApi
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import com.izquierdojl.tolocharadio.feature.onboarding.InstanceValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ServerRepositoryTest {
    private lateinit var repo: ServerRepository
    private val dao = mockk<ServerDao>(relaxed = true)
    private val validator = mockk<InstanceValidator>(relaxed = true)
    private val systemApi = mockk<SystemApi>(relaxed = true)
    private val cacheManager = mockk<CacheManager>(relaxed = true)
    private val tokens = mockk<TokenStore>(relaxed = true)

    private fun server(
        id: String,
        isActive: Boolean = false,
        isDefault: Boolean = false,
    ) = com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity(
        id = id,
        url = "https://srv-$id.example.com",
        alias = id,
        isActive = isActive,
        isDefault = isDefault,
    )

    @Before
    fun setup() {
        repo = ServerRepository(dao, validator, systemApi, cacheManager, tokens)
    }

    @Test
    fun `switchTo marca activo sin tocar isDefault (FR-006 C2)`() = runTest {
        val prev = server("a", isActive = true, isDefault = true)
        val target = server("b")
        coEvery { dao.getById("b") } returns target
        coEvery { dao.getActive() } returnsMany listOf(prev, null)
        coEvery { dao.getAll() } returns flowOf(listOf(prev, target))
        every { tokens.getActiveServerId() } returns "a"
        every { tokens.getRefresh() } returns "refresh-a"
        every { tokens.getServerCredentials("a") } returns
            TokenStore.ServerCredentials("refresh-a", "a@b.c", "secreta123")

        val result = repo.switchTo("b")

        assertTrue(result is ApiResult.Ok)
        coVerify(exactly = 0) { dao.clearDefault() }
        coVerify { dao.clearActive() }
        coVerify { dao.setActive("b") }
        coVerify { cacheManager.clearAll() }
        // El refresh del servidor destino pasa a ser la sesión global
        coVerify { tokens.setServerCredentials("a", "refresh-a", "a@b.c", "secreta123") }
    }

    @Test
    fun `primer servidor añadido es activo y por defecto (FR-005)`() = runTest {
        coEvery { dao.count() } returns 0
        coEvery { validator.validate(any()) } returns true

        val result = repo.add("https://nuevo.example.com", "srv", "a@b.c", "secreta123")

        assertTrue(result is ApiResult.Ok)
        coVerify { dao.clearActive() }
        coVerify { dao.clearDefault() }
        coVerify {
            dao.insert(
                match {
                    it.isActive && it.isDefault && it.userEmail == "a@b.c"
                },
            )
        }
        coVerify { tokens.saveServerAuth(match { it.isNotBlank() }, "a@b.c", "secreta123") }
    }

    @Test
    fun `añadir segundo servidor no cambia activo por defecto`() = runTest {
        coEvery { dao.count() } returns 1
        coEvery { validator.validate(any()) } returns true

        val result = repo.add("https://nuevo.example.com", "srv2", "", "")

        assertTrue(result is ApiResult.Ok)
        coVerify(exactly = 0) { dao.clearActive() }
        coVerify(exactly = 0) { dao.clearDefault() }
        coVerify {
            dao.insert(
                match { !it.isActive && !it.isDefault },
            )
        }
    }

    @Test
    fun `delete borra credenciales cifradas (FR-008)`() = runTest {
        val only = server("a", isActive = true, isDefault = true)
        coEvery { dao.getById("a") } returns only
        coEvery { dao.getAll() } returns flowOf(emptyList())

        repo.delete("a")

        coVerify { tokens.deleteServerCredentials("a") }
        coVerify { dao.deleteById("a") }
    }

    @Test
    fun `delete del por defecto promociona otro servidor`() = runTest {
        val def = server("a", isActive = true, isDefault = true)
        val other = server("b")
        coEvery { dao.getById("a") } returns def
        coEvery { dao.getAll() } returnsMany listOf(flowOf(listOf(other)), flowOf(listOf(other)))

        repo.delete("a")

        coVerify { dao.setDefault("b") }
        coVerify { dao.setActive("b") }
    }

    @Test
    fun `URL inaccesible no guarda servidor (FR-012)`() = runTest {
        coEvery { validator.validate(any()) } returns false
        val result = repo.add("https://roto.example.com", "srv")
        assertTrue(result is ApiResult.Err)
        coVerify(exactly = 0) { dao.insert(any()) }
        assertFalse(result is ApiResult.Ok)
        assertEquals(true, result is ApiResult.Err)
    }
}
