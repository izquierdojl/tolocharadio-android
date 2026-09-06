package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.local.CachedCustomStation
import com.izquierdojl.tolocharadio.data.local.CustomStationsCacheDao
import com.izquierdojl.tolocharadio.data.local.TolochaDb
import com.izquierdojl.tolocharadio.data.remote.api.CustomStationsApi
import com.izquierdojl.tolocharadio.data.remote.api.OkResult
import com.izquierdojl.tolocharadio.data.remote.dto.CustomStationResultDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationListDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class CustomStationsRepoTest {
    private val api: CustomStationsApi = mockk()
    private val dao: CustomStationsCacheDao = mockk(relaxed = true)
    private val db: TolochaDb = mockk { every { customStationsCache() } returns dao }
    private val repo = CustomStationsRepo(api, db)

    private val s1 = StationDto("c1", "Radio Sierra", url = "https://x/live", isCustom = true)
    private val s2 = StationDto("c2", "Radio Mar", url = "https://y/live", isCustom = true)

    @Test
    fun `list OK filtra ids vacios y refresca cache`() =
        runTest {
            coEvery { api.list() } returns
                Response.success(StationListDto(listOf(s1, StationDto("", "Sin id"), s2)))
            val r = repo.list()
            assertTrue(r is ApiResult.Ok)
            val ok = r as ApiResult.Ok
            assertEquals(false, ok.value.offline)
            assertEquals(listOf("c1", "c2"), ok.value.items.map { it.id })
            coVerify { dao.replaceAll(match { it.map(CachedCustomStation::id) == listOf("c1", "c2") }) }
        }

    @Test
    fun `list error de red con cache devuelve offline`() =
        runTest {
            coEvery { api.list() } throws java.io.IOException("down")
            coEvery { dao.loadAll() } returns
                listOf(CachedCustomStation("c9", "Nueve", "https://z/live", 5000))
            val r = repo.list()
            assertTrue(r is ApiResult.Ok)
            val ok = r as ApiResult.Ok
            assertEquals(true, ok.value.offline)
            assertEquals("c9", ok.value.items.single().id)
            assertEquals(true, ok.value.items.single().isCustom)
        }

    @Test
    fun `list error de red sin cache devuelve Unavailable`() =
        runTest {
            coEvery { api.list() } throws java.io.IOException("down")
            coEvery { dao.loadAll() } returns emptyList()
            val r = repo.list()
            assertTrue((r as ApiResult.Err).error is DomainError.Unavailable)
        }

    @Test
    fun `create OK añade al state y cache`() =
        runTest {
            coEvery { api.create(any()) } returns Response.success(CustomStationResultDto(s1))
            val r = repo.create("Radio Sierra", "https://x/live")
            assertTrue(r is ApiResult.Ok)
            assertEquals("c1", (r as ApiResult.Ok).value.id)
            assertEquals(listOf("c1"), repo.items.value.map { it.id })
            coVerify { dao.upsertAll(match { it.single().id == "c1" }) }
        }

    @Test
    fun `create con nombre en blanco retorna Validation sin llamar red`() =
        runTest {
            val r = repo.create("  ", "https://x/live")
            assertTrue(r is ApiResult.Err)
            assertTrue((r as ApiResult.Err).error is DomainError.Validation)
            coVerify(exactly = 0) { api.create(any()) }
        }

    @Test
    fun `delete OK elimina del state y cache`() =
        runTest {
            coEvery { api.list() } returns Response.success(StationListDto(listOf(s1, s2)))
            repo.list()
            coEvery { api.delete("c1") } returns Response.success(OkResult())
            val r = repo.delete("c1")
            assertTrue(r is ApiResult.Ok)
            assertEquals(listOf("c2"), repo.items.value.map { it.id })
            coVerify { dao.deleteById("c1") }
        }

    @Test
    fun `delete 404 se trata como exito`() =
        runTest {
            coEvery { api.list() } returns Response.success(StationListDto(listOf(s1)))
            repo.list()
            coEvery { api.delete("c1") } returns
                Response.error(404, """{"error":{"code":"NOT_FOUND","message":"x","status":404}}""".toResponseBody())
            val r = repo.delete("c1")
            assertTrue(r is ApiResult.Ok)
            assertTrue(repo.items.value.isEmpty())
        }

    @Test
    fun `delete error de red no modifica state`() =
        runTest {
            coEvery { api.list() } returns Response.success(StationListDto(listOf(s1)))
            repo.list()
            coEvery { api.delete("c1") } throws java.io.IOException("timeout")
            val r = repo.delete("c1")
            assertTrue(r is ApiResult.Err)
            assertEquals(listOf("c1"), repo.items.value.map { it.id })
        }
}

