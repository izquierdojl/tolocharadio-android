package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.remote.api.StatsApi
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTimelineDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTimelineEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTopEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StatsTopListDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * `StatsRepo`: respuestas del contrato y mapeo de errores con `safeCall`
 * (401 → Unauthorized, 400 → Unknown del contrato, red → Unavailable).
 */
class StatsRepoTest {
    private val api: StatsApi = mockk()
    private val repo = StatsRepo(api)

    @Test
    fun `top OK devuelve items`() =
        runTest {
            coEvery { api.top(any(), any(), any()) } returns
                Response.success(
                    StatsTopListDto(listOf(StatsTopEntryDto(StationDto("s1", "Rock FM"), 1000))),
                )
            val ok = repo.top(null, null, null) as ApiResult.Ok
            assertEquals("Rock FM", ok.value.items.single().station.name)
        }

    @Test
    fun `timeline OK devuelve granularidad`() =
        runTest {
            coEvery { api.timeline(any(), any(), any()) } returns
                Response.success(
                    StatsTimelineDto(
                        granularity = "week",
                        items = listOf(StatsTimelineEntryDto("2026-09-01", 2000)),
                    ),
                )
            val ok = repo.timeline("2026-08-01", "2026-09-01", "week") as ApiResult.Ok
            assertEquals("week", ok.value.granularity)
            assertEquals(2000L, ok.value.items.single().totalMs)
        }

    @Test
    fun `error 401 mapea Unauthorized`() =
        runTest {
            coEvery { api.top(any(), any(), any()) } returns
                Response.error(
                    401,
                    """{"error":{"code":"UNAUTHORIZED","message":"x","status":401}}""".toResponseBody(),
                )
            val err = repo.top(null, null, null) as ApiResult.Err
            assertTrue(err.error is DomainError.Unauthorized)
        }

    @Test
    fun `error 400 INVALID_PARAMS mapea Unknown del contrato`() =
        runTest {
            coEvery { api.timeline(any(), any(), any()) } returns
                Response.error(
                    400,
                    """{"error":{"code":"INVALID_PARAMS","message":"from invalido","status":400}}""".toResponseBody(),
                )
            val err = repo.timeline(null, null, "day") as ApiResult.Err
            assertTrue(err.error is DomainError.Unknown)
            assertEquals("from invalido", (err.error as DomainError.Unknown).message)
        }

    @Test
    fun `error de red mapea Unavailable`() =
        runTest {
            coEvery { api.recent(any()) } throws IOException("down")
            val err = repo.recent(null) as ApiResult.Err
            assertTrue(err.error is DomainError.Unavailable)
        }
}
