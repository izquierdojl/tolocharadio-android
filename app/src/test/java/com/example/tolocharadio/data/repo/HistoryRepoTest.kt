package com.example.tolocharadio.data.repo

import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.core.network.DomainError
import com.example.tolocharadio.data.local.CachedHistoryEntry
import com.example.tolocharadio.data.local.HistoryCacheDao
import com.example.tolocharadio.data.local.TolochaDb
import com.example.tolocharadio.data.remote.api.HistoryApi
import com.example.tolocharadio.data.remote.api.OkResult
import com.example.tolocharadio.data.remote.dto.HistoryEntryDto
import com.example.tolocharadio.data.remote.dto.HistoryListDto
import com.example.tolocharadio.data.remote.dto.StationDto
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

class HistoryRepoTest {
    private val api: HistoryApi = mockk()
    private val dao: HistoryCacheDao = mockk(relaxed = true)
    private val db: TolochaDb = mockk { every { historyCache() } returns dao }
    private val repo = HistoryRepo(api, db)

    private val entry1 = HistoryEntryDto(StationDto("s1", "Rock FM"), 3000)
    private val entry2 = HistoryEntryDto(StationDto("s2", "Jazz FM"), 2000)
    private val entry1Old = HistoryEntryDto(StationDto("s1", "Rock FM"), 1000)

    @Test
    fun `list OK deduplica y ordena por playedAt descendente`() =
        runTest {
            coEvery { api.list() } returns
                Response.success(HistoryListDto(listOf(entry2, entry1Old, entry1)))
            val r = repo.list()
            assertTrue(r is ApiResult.Ok)
            val ok = r as ApiResult.Ok
            assertEquals(false, ok.value.offline)
            // Deduplicados: s1 se queda con playedAt=3000 (el más reciente), s2 con 2000
            assertEquals(listOf("s1", "s2"), ok.value.items.map { it.station.id })
            assertEquals(listOf(3000L, 2000L), ok.value.items.map { it.playedAt })
            coVerify { dao.replaceAll(match { it.map(CachedHistoryEntry::id) == listOf("s1", "s2") }) }
        }

    @Test
    fun `list error de red con cache devuelve offline`() =
        runTest {
            coEvery { api.list() } throws java.io.IOException("down")
            coEvery { dao.loadOrdered() } returns
                listOf(CachedHistoryEntry("s9", "Nueve", null, null, null, "", 5000, 0))
            val r = repo.list()
            assertTrue(r is ApiResult.Ok)
            val ok = r as ApiResult.Ok
            assertEquals(true, ok.value.offline)
            assertEquals("s9", ok.value.items.single().station.id)
        }

    @Test
    fun `list error de red sin cache devuelve Unavailable`() =
        runTest {
            coEvery { api.list() } throws java.io.IOException("down")
            coEvery { dao.loadOrdered() } returns emptyList()
            val r = repo.list()
            assertTrue((r as ApiResult.Err).error is DomainError.Unavailable)
        }

    @Test
    fun `remove OK elimina del state y cache`() =
        runTest {
            coEvery { api.list() } returns Response.success(HistoryListDto(listOf(entry1, entry2)))
            repo.list() // Carga inicial
            coEvery { api.remove("s1") } returns Response.success(OkResult())
            val r = repo.remove("s1")
            assertTrue(r is ApiResult.Ok)
            assertEquals(listOf("s2"), repo.items.value.map { it.station.id })
            coVerify { dao.deleteById("s1") }
        }

    @Test
    fun `remove 404 se trata como exito`() =
        runTest {
            coEvery { api.list() } returns Response.success(HistoryListDto(listOf(entry1)))
            repo.list()
            coEvery { api.remove("s1") } returns
                Response.error(404, """{"error":{"code":"NOT_FOUND","message":"x","status":404}}""".toResponseBody())
            val r = repo.remove("s1")
            assertTrue(r is ApiResult.Ok)
            assertEquals(emptyList<HistoryEntryDto>(), repo.items.value)
        }

    @Test
    fun `remove error de red propagates error`() =
        runTest {
            coEvery { api.list() } returns Response.success(HistoryListDto(listOf(entry1)))
            repo.list()
            coEvery { api.remove("s1") } throws java.io.IOException("timeout")
            val r = repo.remove("s1")
            assertTrue(r is ApiResult.Err)
            // State no se modifica en caso de error de red (no NotFound)
            assertEquals(listOf("s1"), repo.items.value.map { it.station.id })
        }

    @Test
    fun `clear OK vacia state y cache`() =
        runTest {
            coEvery { api.list() } returns Response.success(HistoryListDto(listOf(entry1)))
            repo.list()
            coEvery { api.clear() } returns Response.success(OkResult())
            val r = repo.clear()
            assertTrue(r is ApiResult.Ok)
            assertEquals(emptyList<HistoryEntryDto>(), repo.items.value)
            coVerify { dao.clear() }
        }

    @Test
    fun `clear error de red propagates error`() =
        runTest {
            coEvery { api.list() } returns Response.success(HistoryListDto(listOf(entry1)))
            repo.list()
            coEvery { api.clear() } throws java.io.IOException("timeout")
            val r = repo.clear()
            assertTrue(r is ApiResult.Err)
            // State no se modifica en caso de error
            assertEquals(listOf("s1"), repo.items.value.map { it.station.id })
        }

    @Test
    fun `remove blank id retorna Validation`() =
        runTest {
            val r = repo.remove("")
            assertTrue(r is ApiResult.Err)
            assertTrue((r as ApiResult.Err).error is DomainError.Validation)
        }
}
