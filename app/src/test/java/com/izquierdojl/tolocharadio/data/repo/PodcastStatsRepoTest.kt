package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.remote.api.PodcastsApi
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastStatsSummaryDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/** Stats de podcast: mapeo del resumen (T053, Red→Green). */
class PodcastStatsRepoTest {
    private val api: PodcastsApi = mockk()
    private val repo = PodcastStatsRepo(api)

    @Test
    fun `summary devuelve agregados`() =
        runTest {
            coEvery { api.statsSummary() } returns
                Response.success(
                    PodcastStatsSummaryDto(
                        totalMs = 3_600_000,
                        podcastCount = 4,
                        startedEpisodes = 10,
                        completedEpisodes = 7,
                        completionRate = 0.7,
                        avgMsPerEpisode = 360_000,
                    ),
                )
            val r = repo.summary()
            assertTrue(r is ApiResult.Ok)
            val s = (r as ApiResult.Ok).value
            assertEquals(3_600_000L, s.totalMs)
            assertEquals(0.7, s.completionRate, 0.001)
        }
}
