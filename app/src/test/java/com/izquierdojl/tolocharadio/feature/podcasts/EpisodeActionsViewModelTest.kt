package com.izquierdojl.tolocharadio.feature.podcasts

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.local.podcasts.DownloadRecord
import com.izquierdojl.tolocharadio.data.repo.DownloadProgress
import com.izquierdojl.tolocharadio.data.repo.DownloadsRepo
import com.izquierdojl.tolocharadio.data.repo.EpisodeFavoritesRepo
import com.izquierdojl.tolocharadio.data.repo.PodcastCapabilities
import com.izquierdojl.tolocharadio.data.repo.ProgressRepo
import com.izquierdojl.tolocharadio.data.repo.QueueRepo
import com.izquierdojl.tolocharadio.data.repo.SystemRepo
import com.izquierdojl.tolocharadio.domain.podcasts.CleanupDownloadsUseCase
import com.izquierdojl.tolocharadio.support.testEpisode
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EpisodeActionsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val favorites = mockk<EpisodeFavoritesRepo>(relaxed = true)
    private val queue = mockk<QueueRepo>(relaxed = true)
    private val downloads = mockk<DownloadsRepo>(relaxed = true)
    private val progress = mockk<ProgressRepo>(relaxed = true)
    private val system = mockk<SystemRepo>(relaxed = true)
    private val cleanup = mockk<CleanupDownloadsUseCase>(relaxed = true)

    private fun vm(
        loaded: Boolean = true,
        ids: Set<Long> = emptySet(),
    ): EpisodeActionsViewModel {
        every { favorites.favoriteIds } returns MutableStateFlow(ids)
        every { favorites.loaded } returns MutableStateFlow(loaded)
        coEvery { favorites.list() } returns ApiResult.Ok(emptyList())
        coEvery { favorites.toggle(any(), any()) } returns ApiResult.Ok(Unit)
        every { downloads.records } returns MutableStateFlow(emptyMap<Long, DownloadRecord>())
        every { downloads.progress } returns MutableStateFlow(emptyMap<Long, DownloadProgress>())
        coEvery { downloads.refresh() } just Runs
        coEvery { cleanup(any()) } just Runs
        coEvery { system.podcastCapabilities() } returns ApiResult.Ok(PodcastCapabilities())
        return EpisodeActionsViewModel(favorites, queue, downloads, progress, system, cleanup)
    }

    @Test
    fun `expone favoritos cargados`() {
        val viewModel = vm(loaded = true)

        assertTrue(viewModel.favoritesLoaded.value)
    }

    @Test
    fun `toggle con estado explicito usa el booleano mostrado`() =
        runTest {
            val viewModel = vm(ids = setOf(1L))

            viewModel.toggleFavorite(episodeId = 1L, isFavorite = true)
            advanceUntilIdle()

            coVerify { favorites.toggle(1L, true) }
        }

    @Test
    fun `toggleFavorite de episodio usa el conjunto vivo`() =
        runTest {
            val viewModel = vm(ids = setOf(1L))
            val episode = testEpisode(id = 1L, isFavorite = false)

            viewModel.toggleFavorite(episode)
            advanceUntilIdle()

            coVerify { favorites.toggle(1L, true) }
        }

    @Test
    fun `un fallo de favorito informa sin romper`() =
        runTest {
            val viewModel = vm()
            coEvery { favorites.toggle(any(), any()) } returns
                ApiResult.Err(DomainError.Unavailable("offline"))

            viewModel.toggleFavorite(episodeId = 1L, isFavorite = false)
            advanceUntilIdle()

            assertNotNull(viewModel.message.value)
        }
}
