package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.remote.api.SystemApi
import com.izquierdojl.tolocharadio.data.remote.dto.AppConfigDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastFeaturesDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/** Capacidad de ranking y caché de sesión (spec 0053, G5). */
class SystemRepoTest {
    private val api: SystemApi = mockk()
    private val repo = SystemRepo(api)

    private val enabled =
        AppConfigDto(
            appName = "Tolocha",
            features = PodcastFeaturesDto(podcasts = true, podcastDiscovery = "itunes"),
        )

    @Test
    fun `la capacidad se cachea durante la sesion`() =
        runTest {
            coEvery { api.config() } returns Response.success(enabled)

            val first = repo.podcastCapabilities()
            val second = repo.podcastCapabilities()

            assertTrue((first as ApiResult.Ok).value.topCharts)
            assertTrue((second as ApiResult.Ok).value.topCharts)
            coVerify(exactly = 1) { api.config() }
        }

    @Test
    fun `forceRefresh vuelve a pedir config`() =
        runTest {
            coEvery { api.config() } returns Response.success(enabled)

            repo.podcastCapabilities()
            repo.podcastCapabilities(forceRefresh = true)

            coVerify(exactly = 2) { api.config() }
        }

    @Test
    fun `un error no se cachea`() =
        runTest {
            coEvery { api.config() } returns
                Response.error(
                    503,
                    """{"error":{"code":"UNAVAILABLE","message":"x","status":503}}""".toResponseBody(),
                )

            val failed = repo.podcastCapabilities()
            assertTrue(failed is ApiResult.Err)

            coEvery { api.config() } returns Response.success(enabled)
            val recovered = repo.podcastCapabilities()

            assertTrue((recovered as ApiResult.Ok).value.topCharts)
            coVerify(exactly = 2) { api.config() }
        }

    @Test
    fun `podcastindex y none no habilitan el ranking`() =
        runTest {
            coEvery { api.config() } returns
                Response.success(
                    AppConfigDto(
                        appName = "Tolocha",
                        features = PodcastFeaturesDto(podcasts = true, podcastDiscovery = "podcastindex"),
                    ),
                )

            val result = repo.podcastCapabilities()

            assertFalse((result as ApiResult.Ok).value.topCharts)
        }
}
