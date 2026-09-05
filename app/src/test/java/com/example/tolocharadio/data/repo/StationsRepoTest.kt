package com.example.tolocharadio.data.repo

import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.core.network.DomainError
import com.example.tolocharadio.data.local.CachedStation
import com.example.tolocharadio.data.local.StationsCacheDao
import com.example.tolocharadio.data.local.TolochaDb
import com.example.tolocharadio.data.remote.api.StationsApi
import com.example.tolocharadio.data.remote.dto.PaginationDto
import com.example.tolocharadio.data.remote.dto.StationDto
import com.example.tolocharadio.data.remote.dto.StationPageDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class StationsRepoTest {
    private val api: StationsApi = mockk()
    private val dao: StationsCacheDao = mockk(relaxed = true)
    private val db: TolochaDb = mockk { every { stationsCache() } returns dao }
    private val repo = StationsRepo(api, db)
    private val station = StationDto(id = "u1", name = "Tolocha")

    @Test
    fun `search OK pagina y cachea`() =
        runTest {
            coEvery { api.search(any(), any(), any(), any(), any(), any(), any()) } returns
                Response.success(StationPageDto(listOf(station), PaginationDto(0, 24, false)))
            val r = repo.search(StationQuery(name = "tol"))
            assertTrue(r is ApiResult.Ok)
            coVerify { dao.upsertAll(any()) }
        }

    @Test
    fun `search limita limit a 100`() =
        runTest {
            coEvery { api.search(any(), any(), any(), any(), 100, any(), any()) } returns
                Response.success(StationPageDto(emptyList(), PaginationDto(0, 100, false)))
            repo.search(StationQuery(limit = 500))
            coVerify { api.search(any(), any(), any(), any(), 100, any(), any()) }
        }

    @Test
    fun `503 con cache devuelve cache`() =
        runTest {
            coEvery { api.search(any(), any(), any(), any(), any(), any(), any()) } throws
                java.io.IOException("down")
            coEvery { dao.search(any(), any()) } returns
                listOf(CachedStation("u1", "Tolocha", null, null, null, "", 0))
            val r = repo.search(StationQuery(name = "tol"))
            assertTrue(r is ApiResult.Ok)
            assertEquals("Tolocha", (r as ApiResult.Ok).value.items.first().name)
        }

    @Test
    fun `503 sin cache devuelve Unavailable`() =
        runTest {
            coEvery { api.search(any(), any(), any(), any(), any(), any(), any()) } throws
                java.io.IOException("down")
            coEvery { dao.search(any(), any()) } returns emptyList()
            val r = repo.search(StationQuery(name = "tol"))
            assertTrue((r as ApiResult.Err).error is DomainError.Unavailable)
        }
}
