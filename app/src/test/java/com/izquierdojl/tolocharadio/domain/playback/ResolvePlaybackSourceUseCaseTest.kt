package com.izquierdojl.tolocharadio.domain.playback

import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolvePlaybackSourceUseCaseTest {
    @Test
    fun `stream directo usa proxy`() =
        runTest {
            val result = ResolvePlaybackSourceUseCase(fetcher(""))(station("https://host/live.mp3"))
            assertEquals(ResolutionResult.Proxied("s1"), result)
        }

    @Test
    fun `m3u8 produce HLS directo`() =
        runTest {
            val result = ResolvePlaybackSourceUseCase(fetcher(""))(station("https://host/live.m3u8"))
            assertEquals(ResolutionResult.Single(PlaybackSource.DirectHls("https://host/live.m3u8")), result)
        }

    @Test
    fun `m3u con varias entradas produce candidatos`() =
        runTest {
            val text = "https://a/1.mp3\nhttps://b/2.m3u8\n"
            val result = ResolvePlaybackSourceUseCase(fetcher(text))(station("https://host/list.m3u"))
            val candidates = (result as ResolutionResult.Candidates).sources
            assertEquals(
                listOf(
                    PlaybackSource.DirectProgressive("https://a/1.mp3"),
                    PlaybackSource.DirectHls("https://b/2.m3u8"),
                ),
                candidates,
            )
        }

    @Test
    fun `m3u con una entrada produce single`() =
        runTest {
            val result = ResolvePlaybackSourceUseCase(fetcher("https://a/1.mp3\n"))(station("https://host/list.m3u"))
            assertEquals(ResolutionResult.Single(PlaybackSource.DirectProgressive("https://a/1.mp3")), result)
        }

    @Test
    fun `fallo de red produce NETWORK`() =
        runTest {
            val useCase = ResolvePlaybackSourceUseCase(PlaylistFetcher { throw PlaylistFetchException("boom") })
            assertEquals(
                ResolutionResult.Unavailable(PlaybackError.NETWORK),
                useCase(station("https://host/list.m3u")),
            )
        }

    @Test
    fun `solo http produce INSECURE_ONLY`() =
        runTest {
            val result = ResolvePlaybackSourceUseCase(fetcher("http://a/1.mp3\n"))(station("https://host/list.m3u"))
            assertEquals(ResolutionResult.Unavailable(PlaybackError.INSECURE_ONLY), result)
        }

    @Test
    fun `sin entradas produce NO_ENTRIES`() =
        runTest {
            val result = ResolvePlaybackSourceUseCase(fetcher("#EXTM3U\n"))(station("https://host/list.m3u"))
            assertEquals(ResolutionResult.Unavailable(PlaybackError.NO_ENTRIES), result)
        }

    @Test
    fun `emisora personalizada usa el mismo pipeline`() =
        runTest {
            val result =
                ResolvePlaybackSourceUseCase(fetcher("[playlist]\nFile1=https://a/1.mp3\n"))(
                    station("https://host/list.pls", custom = true),
                )
            assertTrue(result is ResolutionResult.Single)
        }

    private fun station(
        url: String,
        custom: Boolean = false,
    ) = StationDto(id = "s1", name = "Emisora", url = url, isCustom = custom)

    private fun fetcher(
        text: String,
        finalUrl: String = "https://host/dir/list.m3u",
    ): PlaylistFetcher = PlaylistFetcher { PlaylistContent(text, finalUrl) }
}
