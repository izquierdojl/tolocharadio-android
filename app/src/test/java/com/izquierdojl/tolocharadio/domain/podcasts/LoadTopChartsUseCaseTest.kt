package com.izquierdojl.tolocharadio.domain.podcasts

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSearchResult
import com.izquierdojl.tolocharadio.data.repo.PodcastSubscriptionsRepo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regla de ranking y degradación por categoría (spec 0053, FR-010/FR-013). */
class LoadTopChartsUseCaseTest {
    private val repo: PodcastSubscriptionsRepo = mockk()
    private val useCase = LoadTopChartsUseCase(repo)

    private val first = PodcastSearchResult("A", "Autor", "https://a.example/f.xml")
    private val second = PodcastSearchResult("B", "Autor", "https://b.example/f.xml")

    @Test
    fun `todas las categorias pide el ranking general`() =
        runTest {
            coEvery { repo.top("es", null, TOP_CHARTS_LIMIT) } returns ApiResult.Ok(listOf(first, second))

            val result = useCase(TopCountry.SPAIN, TopGenre.ALL)

            val value = (result as ApiResult.Ok).value
            assertEquals(listOf(first, second), value.items)
            assertFalse(value.genreUnavailable)
            coVerify(exactly = 0) { repo.top("es", "1303", TOP_CHARTS_LIMIT) }
        }

    @Test
    fun `una categoria correcta no pide el general`() =
        runTest {
            coEvery { repo.top("es", "1303", TOP_CHARTS_LIMIT) } returns ApiResult.Ok(listOf(first))

            val result = useCase(TopCountry.SPAIN, TopGenre.COMEDY)

            val value = (result as ApiResult.Ok).value
            assertEquals(listOf(first), value.items)
            assertFalse(value.genreUnavailable)
            coVerify(exactly = 0) { repo.top("es", null, TOP_CHARTS_LIMIT) }
        }

    @Test
    fun `un error recuperable cae al general y marca el aviso`() =
        runTest {
            coEvery { repo.top("es", "1303", TOP_CHARTS_LIMIT) } returns
                ApiResult.Err(DomainError.Unavailable("sin red"))
            coEvery { repo.top("es", null, TOP_CHARTS_LIMIT) } returns ApiResult.Ok(listOf(first))

            val result = useCase(TopCountry.SPAIN, TopGenre.COMEDY)

            val value = (result as ApiResult.Ok).value
            assertEquals(listOf(first), value.items)
            assertTrue(value.genreUnavailable)
        }

    @Test
    fun `un Unknown tambien es recuperable`() =
        runTest {
            coEvery { repo.top("es", "1303", TOP_CHARTS_LIMIT) } returns
                ApiResult.Err(DomainError.Unknown("error"))
            coEvery { repo.top("es", null, TOP_CHARTS_LIMIT) } returns ApiResult.Ok(listOf(first))

            val result = useCase(TopCountry.SPAIN, TopGenre.COMEDY)

            assertTrue((result as ApiResult.Ok).value.genreUnavailable)
        }

    @Test
    fun `no hay fallback para credenciales`() =
        runTest {
            coEvery { repo.top("es", "1303", TOP_CHARTS_LIMIT) } returns
                ApiResult.Err(DomainError.Unauthorized("UNAUTHORIZED"))

            val result = useCase(TopCountry.SPAIN, TopGenre.COMEDY)

            assertTrue(result is ApiResult.Err)
            assertTrue((result as ApiResult.Err).error is DomainError.Unauthorized)
            coVerify(exactly = 0) { repo.top("es", null, TOP_CHARTS_LIMIT) }
        }

    @Test
    fun `no hay fallback para 404`() =
        runTest {
            coEvery { repo.top("es", "1303", TOP_CHARTS_LIMIT) } returns
                ApiResult.Err(DomainError.NotFound("NOT_FOUND"))

            val result = useCase(TopCountry.SPAIN, TopGenre.COMEDY)

            assertTrue((result as ApiResult.Err).error is DomainError.NotFound)
            coVerify(exactly = 0) { repo.top("es", null, TOP_CHARTS_LIMIT) }
        }

    @Test
    fun `si el general tambien falla se propaga el error`() =
        runTest {
            coEvery { repo.top("es", "1303", TOP_CHARTS_LIMIT) } returns
                ApiResult.Err(DomainError.Unavailable("sin red"))
            coEvery { repo.top("es", null, TOP_CHARTS_LIMIT) } returns
                ApiResult.Err(DomainError.Unavailable("sin red"))

            val result = useCase(TopCountry.SPAIN, TopGenre.COMEDY)

            assertTrue(result is ApiResult.Err)
        }
}
