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

    private fun stubCatalogLists() {
        coEvery { repo.countries() } returns ApiResult.Ok(listOf("Spain"))
        coEvery { repo.languages() } returns ApiResult.Ok(listOf("Spanish"))
        coEvery { repo.tags() } returns ApiResult.Ok(listOf("rock"))
    }

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
            stubCatalogLists()
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
            stubCatalogLists()
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
            stubCatalogLists()
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
            stubCatalogLists()
            coEvery { repo.search(any()) } returns ApiResult.Ok(page)
            coEvery { favorites.list() } returns ApiResult.Ok(FavoritesResult(emptyList(), false))
            every { favorites.favoriteIds } returns MutableStateFlow(setOf("u1"))
            val content = ExploreViewModel(repo, favorites, toggle).awaitContent()
            assertEquals(setOf("u1"), content.favorites)
        }

    @Test
    fun `catalog lists loaded on init`() =
        runTest {
            coEvery { repo.search(any()) } returns ApiResult.Ok(page)
            coEvery { repo.countries() } returns ApiResult.Ok(listOf("Spain", "France"))
            coEvery { repo.languages() } returns ApiResult.Ok(listOf("Spanish", "English"))
            coEvery { repo.tags() } returns ApiResult.Ok(listOf("rock", "jazz"))
            coEvery { favorites.list() } returns ApiResult.Ok(FavoritesResult(emptyList(), false))
            every { favorites.favoriteIds } returns MutableStateFlow(emptySet())
            val vm = ExploreViewModel(repo, favorites, toggle)
            vm.awaitContent()
            val countries = vm.countries.value as CatalogList.Loaded
            assertEquals(listOf("France", "Spain"), countries.items)
            val languages = vm.languages.value as CatalogList.Loaded
            assertEquals(listOf("English", "Spanish"), languages.items)
            val tags = vm.tags.value as CatalogList.Loaded
            assertEquals(listOf("jazz", "rock"), tags.items)
        }

    @Test
    fun `catalog list error sets degraded mode`() =
        runTest {
            coEvery { repo.search(any()) } returns ApiResult.Ok(page)
            coEvery { repo.countries() } returns ApiResult.Err(DomainError.Unavailable("fail"))
            coEvery { repo.languages() } returns ApiResult.Ok(listOf("Spanish"))
            coEvery { repo.tags() } returns ApiResult.Ok(listOf("rock"))
            coEvery { favorites.list() } returns ApiResult.Ok(FavoritesResult(emptyList(), false))
            every { favorites.favoriteIds } returns MutableStateFlow(emptySet())
            val vm = ExploreViewModel(repo, favorites, toggle)
            vm.awaitContent()
            assertTrue(vm.countries.value is CatalogList.Error)
            assertTrue(vm.languages.value is CatalogList.Loaded)
        }

    @Test
    fun `setFilters with country passes to search`() =
        runTest {
            coEvery { repo.search(any()) } returns ApiResult.Ok(page)
            coEvery { repo.countries() } returns ApiResult.Ok(listOf("Spain"))
            coEvery { repo.languages() } returns ApiResult.Ok(listOf("Spanish"))
            coEvery { repo.tags() } returns ApiResult.Ok(listOf("rock"))
            coEvery { favorites.list() } returns ApiResult.Ok(FavoritesResult(emptyList(), false))
            every { favorites.favoriteIds } returns MutableStateFlow(emptySet())
            val vm = ExploreViewModel(repo, favorites, toggle)
            vm.awaitContent()
            vm.setFilters(ExploreFilters(country = "Spain"))
            vm.awaitContent()
            coEvery { repo.search(match { it.country == "Spain" }) } returns ApiResult.Ok(page)
        }

    @Test
    fun `clear filters resets to no filters`() =
        runTest {
            coEvery { repo.search(any()) } returns ApiResult.Ok(page)
            coEvery { repo.countries() } returns ApiResult.Ok(listOf("Spain"))
            coEvery { repo.languages() } returns ApiResult.Ok(listOf("Spanish"))
            coEvery { repo.tags() } returns ApiResult.Ok(listOf("rock"))
            coEvery { favorites.list() } returns ApiResult.Ok(FavoritesResult(emptyList(), false))
            every { favorites.favoriteIds } returns MutableStateFlow(emptySet())
            val vm = ExploreViewModel(repo, favorites, toggle)
            vm.awaitContent()
            vm.setFilters(ExploreFilters(country = "Spain", language = "Spanish"))
            vm.awaitContent()
            vm.setFilters(ExploreFilters())
            vm.awaitContent()
            assertEquals(null, vm.filters.country)
            assertEquals(null, vm.filters.language)
            assertEquals(null, vm.filters.tag)
        }

    @Test
    fun `all catalog lists error still allows search`() =
        runTest {
            coEvery { repo.search(any()) } returns ApiResult.Ok(page)
            coEvery { repo.countries() } returns ApiResult.Err(DomainError.Unavailable("fail"))
            coEvery { repo.languages() } returns ApiResult.Err(DomainError.Unavailable("fail"))
            coEvery { repo.tags() } returns ApiResult.Err(DomainError.Unavailable("fail"))
            coEvery { favorites.list() } returns ApiResult.Ok(FavoritesResult(emptyList(), false))
            every { favorites.favoriteIds } returns MutableStateFlow(emptySet())
            val vm = ExploreViewModel(repo, favorites, toggle)
            val content = vm.awaitContent()
            assertEquals(2, content.items.size)
            assertTrue(vm.countries.value is CatalogList.Error)
            assertTrue(vm.languages.value is CatalogList.Error)
            assertTrue(vm.tags.value is CatalogList.Error)
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
