package com.izquierdojl.tolocharadio.servers.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.local.CacheManager
import com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity
import com.izquierdojl.tolocharadio.data.local.servers.ServerDao
import com.izquierdojl.tolocharadio.data.remote.api.SystemApi
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
        repo = ServerRepository(dao, validator, systemApi, cacheManager, shortcutClearer)
    }

    @Test
    fun `switchTo marca activo sin tocar isDefault (FR-006) y limpia cache (FR-007)`() =
        runTest {
            val target = server("b")
            coEvery { dao.getById("b") } returns target

            val result = repo.switchTo("b")

            assertTrue(result is ApiResult.Ok)
            coVerify(exactly = 0) { dao.clearDefault() }
            coVerify { dao.clearActive() }
            coVerify { dao.setActive("b") }
            coVerify { cacheManager.clearAll() }
            coVerify { shortcutClearer.clear() }
        }

    @Test
    fun `primer servidor añadido es activo y por defecto (FR-003)`() =
        runTest {
            coEvery { dao.count() } returns 0
            coEvery { validator.validate(any()) } returns true

            val result = repo.add("https://nuevo.example.com", "srv")

            assertTrue(result is ApiResult.Ok)
            coVerify { dao.clearActive() }
            coVerify { dao.clearDefault() }
            coVerify {
                dao.insert(
                    match { it.isActive && it.isDefault && it.url == "https://nuevo.example.com" },
                )
            }
        }

    @Test
    fun `añadir segundo servidor no cambia activo ni por defecto`() =
        runTest {
            coEvery { dao.count() } returns 1
            coEvery { validator.validate(any()) } returns true

            val result = repo.add("https://nuevo.example.com", "srv2")

            assertTrue(result is ApiResult.Ok)
            coVerify(exactly = 0) { dao.clearActive() }
            coVerify(exactly = 0) { dao.clearDefault() }
            coVerify {
                dao.insert(match { !it.isActive && !it.isDefault })
            }
        }

    @Test
    fun `delete del por defecto y activo promociona otro servidor (FR-013)`() =
        runTest {
            val def = server("a", isActive = true, isDefault = true)
            val other = server("b")
            coEvery { dao.getById("a") } returns def
            coEvery { dao.getAll() } returnsMany
                listOf(flowOf(listOf(other)), flowOf(listOf(other)))

            repo.delete("a")

            coVerify { dao.deleteById("a") }
            coVerify { dao.setDefault("b") }
            coVerify { dao.setActive("b") }
        }

    @Test
    fun `delete del ultimo servidor no promociona ninguno`() =
        runTest {
            val only = server("a", isActive = true, isDefault = true)
            coEvery { dao.getById("a") } returns only
            coEvery { dao.getAll() } returns flowOf(emptyList())

            repo.delete("a")

            coVerify { dao.deleteById("a") }
            coVerify(exactly = 0) { dao.setDefault(any()) }
            coVerify(exactly = 0) { dao.setActive(any()) }
        }

    @Test
    fun `URL inaccesible no guarda servidor (FR-004)`() =
        runTest {
            coEvery { validator.validate(any()) } returns false
            val result = repo.add("https://roto.example.com", "srv")
            assertTrue(result is ApiResult.Err)
            coVerify(exactly = 0) { dao.insert(any()) }
        }
}
