package com.izquierdojl.tolocharadio.feature.podcasts

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.remote.dto.PaginationDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodePage
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastHome
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.repo.EpisodesRepo
import com.izquierdojl.tolocharadio.feature.podcasts.home.PodcastHomeUi
import com.izquierdojl.tolocharadio.feature.podcasts.home.PodcastHomeViewModel
import com.izquierdojl.tolocharadio.feature.podcasts.show.EpisodeFilter
import com.izquierdojl.tolocharadio.feature.podcasts.show.PodcastShowViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Portada y ficha: estados, filtros y paginación (T054, Red→Green). */
class HomeShowViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repo: EpisodesRepo = mockk()
    private val show = PodcastSummary(7L, "https://a.example/f.xml", "Tolocha")

    private fun ep(id: Long) = PodcastEpisode(id, 7L, "E$id")

    @Test
    fun `home vacia muestra Empty`() =
        runTest {
            coEvery { repo.home() } returns ApiResult.Ok(PodcastHome())
            val v = PodcastHomeViewModel(repo)
            assertTrue(v.ui.first { it !is PodcastHomeUi.Loading } is PodcastHomeUi.Empty)
        }

    @Test
    fun `home con datos muestra Content`() =
        runTest {
            coEvery { repo.home() } returns
                ApiResult.Ok(PodcastHome(continueListening = listOf(ep(1))))
            val v = PodcastHomeViewModel(repo)
            val ui = v.ui.first { it !is PodcastHomeUi.Loading }
            assertTrue(ui is PodcastHomeUi.Content)
            assertEquals(1, (ui as PodcastHomeUi.Content).home.continueListening.size)
        }

    @Test
    fun `cambiar filtro recarga con el nuevo parametro`() =
        runTest {
            coEvery { repo.detail(7L) } returns ApiResult.Ok(show)
            coEvery {
                repo.episodes(7L, any(), any(), any(), any(), any())
            } returns ApiResult.Ok(PodcastEpisodePage(listOf(ep(1)), PaginationDto(0, 20, false)))
            val v = PodcastShowViewModel(repo, mockk(), mockk())
            v.open(7L)
            v.setFilter(EpisodeFilter.IN_PROGRESS)
            coVerify {
                repo.episodes(7L, "recent", "in_progress", null, 0, 20)
            }
            assertEquals(EpisodeFilter.IN_PROGRESS, v.ui.first().filter)
        }
}
