package com.izquierdojl.tolocharadio.servers.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.local.CacheManager
import com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity
import com.izquierdojl.tolocharadio.data.local.servers.ServerDao
import com.izquierdojl.tolocharadio.data.remote.api.SystemApi
import com.izquierdojl.tolocharadio.data.repo.AuthRepo
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import com.izquierdojl.tolocharadio.domain.shortcuts.ShortcutClearer
import com.izquierdojl.tolocharadio.feature.onboarding.InstanceValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ServerRepositoryTest {
    private lateinit var repo: ServerRepository
    private val dao = mockk<ServerDao>(relaxed = true)
    private val validator = mockk<InstanceValidator>(relaxed = true)
    private val systemApi = mockk<SystemApi>(relaxed = true)
    private val cacheManager = mockk<CacheManager>(relaxed = true)
    private val shortcutClearer = mockk<ShortcutClearer>(relaxed = true)
    private val tokens = mockk<TokenStore>(relaxed = true)
    private val authRepo = mockk<AuthRepo>(relaxed = true)

    private fun server(
        id: String,
        isActive: Boolean = false,
        isDefault: Boolean = false,
    ) = SavedServerEntity(
        id = id,
        url = "https://srv-$id.example.com",
        alias = id,
        isActive = isActive,
        isDefault = isDefault,
    )

    @Before
    fun setup() {
        repo = ServerRepository(dao, validator, systemApi, cacheManager, shortcutClearer, tokens, authRepo)
    }

    @Test
    fun `add con credenciales validas hace login y guarda activo+defecto (FR-001)`() =
        runTest {
            coEvery { dao.count() } returns 0
            coEvery { validator.validate(any()) } returns true
            coEvery { authRepo.login(any(), any(), any(), any()) } returns ApiResult.Ok(Unit)

            val result = repo.add("https://nuevo.example.com", "srv", "a@b.c", "secreta123")

            assertTrue(result is ApiResult.Ok)
            coVerify { dao.insert(match { it.isActive && it.isDefault }) }
            coVerify { tokens.setActiveServerId(any()) }
        }

    @Test
    fun `add con login fallido no persiste nada (FR-003)`() =
        runTest {
            coEvery { dao.count() } returns 0
            coEvery { validator.validate(any()) } returns true
            coEvery { authRepo.login(any(), any(), any(), any()) } returns
                ApiResult.Err(DomainError.Unauthorized("bad_credentials"))

            val result = repo.add("https://nuevo.example.com", "srv", "a@b.c", "mala")

            assertTrue(result is ApiResult.Err)
            coVerify(exactly = 0) { dao.insert(any()) }
        }

    @Test
    fun `URL inaccesible no guarda servidor (FR-003)`() =
        runTest {
            coEvery { validator.validate(any()) } returns false
            val result = repo.add("https://roto.example.com", "srv", "a@b.c", "secreta123")
            assertTrue(result is ApiResult.Err)
            coVerify(exactly = 0) { dao.insert(any()) }
        }

    @Test
    fun `update revalida credenciales y actualiza alias (FR-007)`() =
        runTest {
            coEvery { dao.getById("a") } returns server("a", isActive = true)
            coEvery { authRepo.login("a", any(), "a@b.c", "nueva123") } returns ApiResult.Ok(Unit)

            val result = repo.update("a", "Nuevo alias", "a@b.c", "nueva123")

            assertTrue(result is ApiResult.Ok)
            coVerify { dao.insert(match { it.alias == "Nuevo alias" }) }
        }

    @Test
    fun `switchTo asegura sesion y limpia cache (FR-008)`() =
        runTest {
            coEvery { dao.getById("b") } returns server("b")
            coEvery { authRepo.ensureSession("b", any()) } returns ApiResult.Ok(Unit)

            val result = repo.switchTo("b")

            assertTrue(result is ApiResult.Ok)
            coVerify { dao.clearActive() }
            coVerify { dao.setActive("b") }
            coVerify { tokens.setActiveServerId("b") }
            coVerify { cacheManager.clearAll() }
            coVerify { shortcutClearer.clear() }
        }

    @Test
    fun `switchTo con credenciales invalidas no cambia el activo`() =
        runTest {
            coEvery { dao.getById("b") } returns server("b")
            coEvery { authRepo.ensureSession("b", any()) } returns ApiResult.Err(DomainError.Unauthorized("bad"))

            val result = repo.switchTo("b")

            assertTrue(result is ApiResult.Err)
            coVerify(exactly = 0) { dao.setActive(any()) }
        }

    @Test
    fun `delete borra credenciales cifradas`() =
        runTest {
            coEvery { dao.getById("a") } returns server("a", isActive = true, isDefault = true)
            coEvery { dao.getAll() } returns flowOf(emptyList())

            repo.delete("a")

            coVerify { tokens.deleteCredentials("a") }
            coVerify { dao.deleteById("a") }
        }
}
