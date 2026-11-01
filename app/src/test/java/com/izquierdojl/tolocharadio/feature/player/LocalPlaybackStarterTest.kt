package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.HistoryRepo
import com.izquierdojl.tolocharadio.domain.playback.PlaybackFeatureFlags
import com.izquierdojl.tolocharadio.domain.playback.PlaybackRoute
import com.izquierdojl.tolocharadio.domain.playback.ResolvePlaybackSourceUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LocalPlaybackStarterTest {
    private val exoPlayer: ExoPlayer = mockk(relaxed = true)
    private val engine: PlaybackEngine =
        mockk {
            every { player } returns exoPlayer
        }
    private val factory: StationMediaItemFactory = mockk(relaxed = true)
    private val prefs: InstancePrefs =
        mockk {
            every { baseUrl } returns flowOf("https://radio.test/")
        }
    private val history: HistoryRepo = mockk(relaxed = true)
    private val sessionState = PlaybackSessionState(PlaybackFeatureFlags(livePlaybackEnabled = true))
    private val starter =
        LocalPlaybackStarter(
            engine = engine,
            mediaItemFactory = factory,
            resolveSource = ResolvePlaybackSourceUseCase(),
            prefs = prefs,
            history = history,
            sessionState = sessionState,
        )

    @Test
    fun `arranque continuo usa live y no registra historial en fallback`() =
        runTest {
            val station = StationDto(id = "s1", name = "Radio")
            val item = mockk<MediaItem>()
            val source = mockk<MediaSource>()
            every { factory.createLive(station, any(), 4) } returns item
            every { factory.createMediaSource(item, any(), PlaybackRoute.CONTINUOUS) } returns source

            starter.start(station, PlaybackRoute.CONTINUOUS, sequence = 4, recordHistory = false)

            verify { factory.createLive(station, "https://radio.test/", 4) }
            verify { exoPlayer.setMediaSource(source) }
            verify { exoPlayer.prepare() }
            coVerify(exactly = 0) { history.recordLocalPlay(any()) }
        }

    @Test
    fun `arranque legacy usa el manifiesto y registra historial normal`() =
        runTest {
            val station = StationDto(id = "s1", name = "Radio", url = "https://host/live.m3u8")
            val item = mockk<MediaItem>()
            val source = mockk<MediaSource>()
            every { factory.createLegacy(station, any(), any(), 0) } returns item
            every { factory.createMediaSource(item, any(), PlaybackRoute.LEGACY) } returns source

            starter.start(station, PlaybackRoute.LEGACY, recordHistory = true)

            verify { factory.createLegacy(station, any(), "https://radio.test/", 0) }
            verify { exoPlayer.setMediaSource(source) }
            coVerify(exactly = 1) { history.recordLocalPlay(station, any()) }
        }

    @Test
    fun `el arranque inicial usa la ruta de la sesion y r=0`() =
        runTest {
            val station = StationDto(id = "s1", name = "Radio")
            val item = mockk<MediaItem>()
            val source = mockk<MediaSource>()
            every { factory.createLive(station, "https://radio.test/", 0) } returns item
            every { factory.createMediaSource(item, any(), PlaybackRoute.CONTINUOUS) } returns source

            starter.start(station)

            verify { factory.createLive(station, "https://radio.test/", 0) }
        }

    @Test
    fun `cada arranque incrementa el marcador`() =
        runTest {
            val station = StationDto(id = "s1", name = "Radio")
            val item = mockk<MediaItem>()
            every { factory.createLive(station, any(), any()) } returns item

            starter.start(station, recordHistory = false)
            starter.start(station, recordHistory = false)

            verify { factory.createLive(station, "https://radio.test/", 0) }
            verify { factory.createLive(station, "https://radio.test/", 1) }
        }

    @Test
    fun `un fallback legacy se conserva en el siguiente arranque`() =
        runTest {
            val station = StationDto(id = "s1", name = "Radio", url = "https://host/live.m3u8")
            val item = mockk<MediaItem>()
            every { factory.createLegacy(station, any(), any(), any()) } returns item
            sessionState.useLegacyRoute()

            starter.start(station, recordHistory = false)

            verify { factory.createLegacy(station, any(), "https://radio.test/", 0) }
        }
}
