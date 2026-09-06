package com.izquierdojl.tolocharadio.feature.explore

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.remote.dto.PaginationDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationPageDto
import com.izquierdojl.tolocharadio.data.repo.FavoritesRepo
import com.izquierdojl.tolocharadio.data.repo.FavoritesResult
import com.izquierdojl.tolocharadio.data.repo.StationsRepo
import com.izquierdojl.tolocharadio.domain.ToggleFavoriteUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExploreViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repo: StationsRepo = mockk()
    private val favorites: FavoritesRepo = mockk(relaxed = true)
    private val toggle: ToggleFavoriteUseCase = mockk()
    private val page =
        StationPageDto(
            listOf(StationDto("u1", "Tolocha"), StationDto("u2", "Sierra")),
            PaginationDto(0, 24, true),
        )

    private suspend fun ExploreViewModel.awaitContent(): ExploreUiState.Content {
        var s: ExploreUiState = ExploreUiState.Loading
        ui.test {
            s = awaitItem()
            if (s is ExploreUiState.Loading) s = awaitItem()
        }
        return s as ExploreUiState.Content
    }

    @Test
    fun `init carga primera pagina con hasMore`() =
        runTest {
            coEvery { repo.search(any()) } returns ApiResult.Ok(page)
            coEvery { favorites.list() } returns ApiResult.Ok(FavoritesResult(emptyList(), false))
            every { favorites.favoriteIds } returns MutableStateFlow(emptySet())
            val content = ExploreViewModel(repo, favorites, toggle).awaitContent()
            assertEquals(2, content.items.size)
            assertTrue(content.hasMore)
        }

    @Test
    fun `error con lista vacia muestra Error`() =
        runTest {
            coEvery { repo.search(any()) } returns ApiResult.Err(DomainError.Unavailable("x"))
            coEvery { favorites.list() } returns ApiResult.Ok(FavoritesResult(emptyList(), false))
            every { favorites.favoriteIds } returns MutableStateFlow(emptySet())
            var s: ExploreUiState = ExploreUiState.Loading
            ExploreViewModel(repo, favorites, toggle).ui.test {
                s = awaitItem()
                if (s is ExploreUiState.Loading) s = awaitItem()
            }
            assertTrue(s is ExploreUiState.Error)
        }

    @Test
    fun `favorito optimista y rollback ante error`() =
        runTest {
            coEvery { repo.search(any()) } returns ApiResult.Ok(page)
            coEvery { favorites.list() } returns ApiResult.Ok(FavoritesResult(emptyList(), false))
            coEvery { toggle("u1", false) } returns ApiResult.Err(DomainError.Unknown("x"))
            every { favorites.favoriteIds } returns MutableStateFlow(emptySet())
            val v = ExploreViewModel(repo, favorites, toggle)
            assertTrue(v.awaitContent().items.isNotEmpty())
            v.toggleFavorite("u1")
            var s: ExploreUiState = ExploreUiState.Loading
            v.ui.test {
                // Tras el toggle hay dos emisiones (optimista + rollback):
                // nos quedamos con la última estable.
                s = awaitItem()
                val cur = s
                if (cur is ExploreUiState.Content && "u1" in cur.favorites) s = awaitItem()
            }
            val content = s as ExploreUiState.Content
            assertTrue("u1" !in content.favorites)
        }

    @Test
    fun `favoritas hidratadas marcan existentes en Explorar`() =
        runTest {
            coEvery { repo.search(any()) } returns ApiResult.Ok(page)
            coEvery { favorites.list() } returns ApiResult.Ok(FavoritesResult(emptyList(), false))
            every { favorites.favoriteIds } returns MutableStateFlow(setOf("u1"))
            val content = ExploreViewModel(repo, favorites, toggle).awaitContent()
            assertEquals(setOf("u1"), content.favorites)
        }
}

class StationDetailViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repo: StationsRepo = mockk()
    private val favorites: FavoritesRepo = mockk(relaxed = true)
    private val toggle: ToggleFavoriteUseCase = mockk()

    @Test
    fun `detalle OK muestra emisora`() =
        runTest {
            coEvery { repo.detail("u1") } returns ApiResult.Ok(StationDto("u1", "Tolocha"))
            coEvery { favorites.list() } returns ApiResult.Ok(FavoritesResult(emptyList(), false))
            every { favorites.favoriteIds } returns MutableStateFlow(emptySet())
            var s: DetailUiState = DetailUiState.Loading
            StationDetailViewModel(repo, favorites, toggle, SavedStateHandle(mapOf("stationId" to "u1"))).ui.test {
                s = awaitItem()
                if (s is DetailUiState.Loading) s = awaitItem()
            }
            assertEquals("Tolocha", (s as DetailUiState.Content).station.name)
        }

    @Test
    fun `ficha marca favorita existente`() =
        runTest {
            coEvery { repo.detail("u1") } returns ApiResult.Ok(StationDto("u1", "Tolocha"))
            coEvery { favorites.list() } returns ApiResult.Ok(FavoritesResult(emptyList(), false))
            every { favorites.favoriteIds } returns MutableStateFlow(setOf("u1"))
            var s: DetailUiState = DetailUiState.Loading
            StationDetailViewModel(repo, favorites, toggle, SavedStateHandle(mapOf("stationId" to "u1"))).ui.test {
                s = awaitItem()
                if (s is DetailUiState.Loading) s = awaitItem()
            }
            assertEquals(true, (s as DetailUiState.Content).isFavorite)
        }
}
