package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.remote.api.PodcastDismissResult
import com.izquierdojl.tolocharadio.data.remote.api.PodcastProgressValue
import com.izquierdojl.tolocharadio.data.remote.api.PodcastProgressWrapper
import com.izquierdojl.tolocharadio.data.remote.api.PodcastsApi
import com.izquierdojl.tolocharadio.data.remote.dto.ReportEpisodeProgressBody
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/** Progreso: reporte, keepalive y descarte (T036, Red→Green). */
class ProgressRepoTest {
    private val api: PodcastsApi = mockk()
    private val repo = ProgressRepo(api)

    private fun wrapper() = PodcastProgressWrapper(PodcastProgressValue(120, 3600, false, null, 5))

    @Test
    fun `load mapea el progreso`() =
        runTest {
            coEvery { api.progress(11L) } returns Response.success(wrapper())
            val r = repo.load(11L)
            assertTrue(r is ApiResult.Ok)
            assertEquals(120L, (r as ApiResult.Ok).value?.positionSec)
        }

    @Test
    fun `report envia sesion secuencia y posicion`() =
        runTest {
            coEvery {
                api.reportProgress(
                    11L,
                    ReportEpisodeProgressBody(
                        sessionId = "s1",
                        sequence = 3,
                        positionSec = 60,
                        listenedMs = 30_000,
                        source = "android",
                    ),
                )
            } returns Response.success(wrapper())
            val r = repo.report(11L, "s1", 3, 60, listenedMs = 30_000)
            assertTrue(r is ApiResult.Ok)
        }

    @Test
    fun `report respeta listenedMs aunque difiera de la posicion`() =
        runTest {
            coEvery {
                api.reportProgress(
                    11L,
                    ReportEpisodeProgressBody(
                        sessionId = "s3",
                        sequence = 4,
                        positionSec = 1_200,
                        listenedMs = 5_000,
                        source = "android",
                    ),
                )
            } returns Response.success(wrapper())
            val r = repo.report(11L, "s3", 4, 1_200, listenedMs = 5_000)
            assertTrue(r is ApiResult.Ok)
        }

    @Test
    fun `report incluye listenedMs y completed para desmarcar`() =
        runTest {
            coEvery {
                api.reportProgress(
                    11L,
                    ReportEpisodeProgressBody(
                        sessionId = "s2",
                        sequence = 1,
                        positionSec = 0,
                        listenedMs = 0,
                        durationSec = 100,
                        completed = false,
                        source = "android",
                    ),
                )
            } returns Response.success(wrapper())
            val r = repo.report(11L, "s2", 1, 0, durationSec = 100, listenedMs = 0, completed = false)
            assertTrue(r is ApiResult.Ok)
        }

    @Test
    fun `dismiss devuelve el flag del servidor`() =
        runTest {
            coEvery { api.dismiss(11L) } returns Response.success(PodcastDismissResult(true))
            val r = repo.dismiss(11L)
            assertTrue(r is ApiResult.Ok)
            assertEquals(true, (r as ApiResult.Ok).value)
        }
}
