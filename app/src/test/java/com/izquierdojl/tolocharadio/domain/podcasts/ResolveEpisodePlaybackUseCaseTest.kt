package com.izquierdojl.tolocharadio.domain.podcasts

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.api.PodcastsApi
import com.izquierdojl.tolocharadio.data.remote.dto.IssuedToken
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.repo.DownloadsRepo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/** Resolución local vs firmada y reemisión (T037, Red→Green). */
class ResolveEpisodePlaybackUseCaseTest {
    private val api: PodcastsApi = mockk()
    private val downloads: DownloadsRepo = mockk()
    private val prefs: InstancePrefs =
        mockk {
            every { baseUrl } returns kotlinx.coroutines.flow.MutableStateFlow("https://srv.example")
        }
    private val useCase = ResolveEpisodePlaybackUseCase(api, downloads, prefs)
    private val episode = PodcastEpisode(11L, 7L, "E1")

    @Test
    fun `archivo local completo gana al modo online`() =
        runTest {
            coEvery { downloads.localUriFor(11L) } returns "content://descargas/11"
            val r = useCase(episode)
            assertTrue(r is ApiResult.Ok)
            val source = (r as ApiResult.Ok).value
            assertTrue(source.local)
            assertEquals("content://descargas/11", source.uri)
        }

    @Test
    fun `sin descarga emite url firmada absolutizada`() =
        runTest {
            coEvery { downloads.localUriFor(11L) } returns null
            coEvery { api.issuePlayback(11L) } returns
                Response.success(IssuedToken("/api/v1/podcasts/episodes/11/stream?sig=x", 9_999_999_999L))
            val r = useCase(episode)
            assertTrue(r is ApiResult.Ok)
            val source = (r as ApiResult.Ok).value
            assertFalse(source.local)
            assertEquals("https://srv.example/api/v1/podcasts/episodes/11/stream?sig=x", source.uri)
        }

    @Test
    fun `firmada proxima a vencer necesita reemision`() {
        val source = EpisodePlaybackSource("https://cdn.example/e", false, 1_000L)
        assertTrue(useCase.needsRefresh(source, nowSec = 900L, marginSec = 300L))
        assertFalse(useCase.needsRefresh(source, nowSec = 100L, marginSec = 300L))
        val local = EpisodePlaybackSource("content://x", true, Long.MAX_VALUE)
        assertFalse(useCase.needsRefresh(local, nowSec = 900L, marginSec = 300L))
    }
}
