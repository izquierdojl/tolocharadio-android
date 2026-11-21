package com.izquierdojl.tolocharadio.domain.podcasts

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.remote.dto.PaginationDto
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodeSearchPage
import com.izquierdojl.tolocharadio.data.repo.EpisodesRepo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionSearchUseCaseTest {
    private val repo: EpisodesRepo = mockk()

    @Test
    fun `termino vacio no llama al repositorio`() =
        runTest {
            val result = SearchSubscriptionEpisodesUseCase(repo)("   ")

            assertTrue(result is ApiResult.Err)
            coVerify(exactly = 0) { repo.searchEpisodes(any(), any(), any()) }
        }

    @Test
    fun `delega el termino recortado y la paginacion`() =
        runTest {
            val page = PodcastEpisodeSearchPage(pagination = PaginationDto(hasMore = true))
            coEvery { repo.searchEpisodes("hola", 5, 10) } returns ApiResult.Ok(page)

            val result = SearchSubscriptionEpisodesUseCase(repo)("  hola  ", offset = 5, limit = 10)

            assertEquals(ApiResult.Ok(page), result)
            coVerify(exactly = 1) { repo.searchEpisodes("hola", 5, 10) }
        }

    @Test
    fun `propaga el error del repositorio`() =
        runTest {
            val error = DomainError.Unavailable("Sin conexión")
            coEvery { repo.searchEpisodes("hola", 0, 20) } returns ApiResult.Err(error)

            assertEquals(
                ApiResult.Err(error),
                SearchSubscriptionEpisodesUseCase(repo)("hola"),
            )
        }
}
