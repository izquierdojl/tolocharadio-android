package com.izquierdojl.tolocharadio.feature.settings

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.ui.navigation.ContentMode
import com.izquierdojl.tolocharadio.core.ui.navigation.PodcastStartScreen
import com.izquierdojl.tolocharadio.core.ui.navigation.StartScreen
import com.izquierdojl.tolocharadio.core.ui.theme.ThemeMode
import com.izquierdojl.tolocharadio.core.util.AppBuildInfo
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.domain.playback.BufferProfile
import com.izquierdojl.tolocharadio.domain.podcasts.CleanupDownloadsUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodeCache
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodeCacheStats
import com.izquierdojl.tolocharadio.domain.servers.GetServersUseCase
import com.izquierdojl.tolocharadio.feature.player.PlaybackProfileController
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Caché de episodios en Ajustes (spec 0058, FR-012/SC-005). */
class SettingsCacheTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val prefs: InstancePrefs =
        mockk(relaxed = true) {
            every { themeMode } returns flowOf(ThemeMode.SYSTEM)
            every { startScreen } returns flowOf(StartScreen.EXPLORE)
            every { podcastStartScreen } returns flowOf(PodcastStartScreen.HOME)
            every { defaultSection } returns flowOf(ContentMode.RADIO)
            every { bufferProfile } returns flowOf(BufferProfile.BALANCED)
            every { resumeOnConnectivity } returns flowOf(true)
            every { downloadRetentionDays } returns flowOf(7)
        }
    private val getServers: GetServersUseCase = mockk(relaxed = true)
    private val buildInfo: AppBuildInfo = mockk(relaxed = true)
    private val profileController: PlaybackProfileController = mockk(relaxed = true)
    private val cleanupDownloads: CleanupDownloadsUseCase = mockk(relaxed = true)
    private val episodeCache: EpisodeCache =
        mockk(relaxed = true) {
            every { stats } returns flowOf(EpisodeCacheStats(sizeBytes = 2_048L, entryCount = 1))
        }

    private fun viewModel(): SettingsViewModel =
        SettingsViewModel(
            prefs = prefs,
            getServers = getServers,
            buildInfo = buildInfo,
            profileController = profileController,
            cleanupDownloads = cleanupDownloads,
            episodeCache = episodeCache,
        )

    @Test
    fun `muestra el tamano de la cache`() =
        runTest {
            val viewModel = viewModel()

            advanceUntilIdle()

            assertEquals(2_048L, viewModel.ui.value.cacheSizeBytes)
        }

    @Test
    fun `liberar espacio vacia la cache`() =
        runTest {
            coEvery { episodeCache.clear() } returns Unit
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onClearCache()
            advanceUntilIdle()

            coVerify(exactly = 1) { episodeCache.clear() }
        }
}
