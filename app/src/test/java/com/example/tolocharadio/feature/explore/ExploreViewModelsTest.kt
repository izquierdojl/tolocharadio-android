package com.example.tolocharadio.feature.explore

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.tolocharadio.MainDispatcherRule
import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.core.network.DomainError
import com.example.tolocharadio.data.remote.dto.PaginationDto
import com.example.tolocharadio.data.remote.dto.StationDto
import com.example.tolocharadio.data.remote.dto.StationPageDto
import com.example.tolocharadio.data.repo.StationsRepo
import com.example.tolocharadio.domain.ToggleFavoriteUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExploreViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repo: StationsRepo = mockk()
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
            val content = ExploreViewModel(repo, toggle).awaitContent()
            assertEquals(2, content.items.size)
            assertTrue(content.hasMore)
        }

    @Test
    fun `error con lista vacia muestra Error`() =
        runTest {
            coEvery { repo.search(any()) } returns ApiResult.Err(DomainError.Unavailable("x"))
            var s: ExploreUiState = ExploreUiState.Loading
            ExploreViewModel(repo, toggle).ui.test {
                s = awaitItem()
                if (s is ExploreUiState.Loading) s = awaitItem()
            }
            assertTrue(s is ExploreUiState.Error)
        }

    @Test
    fun `favorito optimista y rollback ante error`() =
        runTest {
            coEvery { repo.search(any()) } returns ApiResult.Ok(page)
            coEvery { toggle("u1", false) } returns ApiResult.Err(DomainError.Unknown("x"))
            val v = ExploreViewModel(repo, toggle)
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
}

class StationDetailViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repo: StationsRepo = mockk()
    private val toggle: ToggleFavoriteUseCase = mockk()

    @Test
    fun `detalle OK muestra emisora`() =
        runTest {
            coEvery { repo.detail("u1") } returns ApiResult.Ok(StationDto("u1", "Tolocha"))
            var s: DetailUiState = DetailUiState.Loading
            StationDetailViewModel(repo, toggle, SavedStateHandle(mapOf("stationId" to "u1"))).ui.test {
                s = awaitItem()
                if (s is DetailUiState.Loading) s = awaitItem()
            }
            assertEquals("Tolocha", (s as DetailUiState.Content).station.name)
        }
}
