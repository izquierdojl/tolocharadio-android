package com.izquierdojl.tolocharadio.feature.favorites

import app.cash.turbine.test
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.remote.dto.FavoriteDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.FavoritesRepo
import com.izquierdojl.tolocharadio.data.repo.FavoritesResult
import com.izquierdojl.tolocharadio.domain.ObserveFavoritesUseCase
import com.izquierdojl.tolocharadio.domain.ReorderFavoritesUseCase
import com.izquierdojl.tolocharadio.domain.ToggleFavoriteUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FavoritesViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val observe: ObserveFavoritesUseCase = mockk()
    private val toggle: ToggleFavoriteUseCase = mockk()
    private val reorder: ReorderFavoritesUseCase = mockk()
    private val repo: FavoritesRepo = mockk(relaxed = true)

    private val favs =
        listOf(
            FavoriteDto(StationDto("u1", "Uno"), 1000),
            FavoriteDto(StationDto("u2", "Dos"), 2000),
        )

    private fun vm() = FavoritesViewModel(observe, toggle, reorder, repo)

    private suspend fun FavoritesViewModel.awaitSettled(): FavoritesUiState {
        var s: FavoritesUiState = FavoritesUiState.Loading
        ui.test {
            s = awaitItem()
            while (s is FavoritesUiState.Loading) s = awaitItem()
        }
        return s
    }

    @Test
    fun `init con favoritas muestra Content en orden`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(FavoritesResult(favs, offline = false))
            val s = vm().awaitSettled() as FavoritesUiState.Content
            assertEquals(listOf("u1", "u2"), s.items.map { it.station.id })
            assertEquals(false, s.offline)
        }

    @Test
    fun `lista vacia muestra Empty`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(FavoritesResult(emptyList(), offline = false))
            assertTrue(vm().awaitSettled() is FavoritesUiState.Empty)
        }

    @Test
    fun `error sin cache muestra Error`() =
        runTest {
            coEvery { observe() } returns ApiResult.Err(DomainError.Unavailable("x"))
            val s = vm().awaitSettled()
            assertTrue(s is FavoritesUiState.Error)
        }

    @Test
    fun `offline con cache muestra Content offline`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(FavoritesResult(favs, offline = true))
            val s = vm().awaitSettled() as FavoritesUiState.Content
            assertEquals(true, s.offline)
        }

    @Test
    fun `quitar con deshacer y undo restaura en su sitio`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(FavoritesResult(favs, offline = false))
            coEvery { repo.remove("u1") } returns ApiResult.Ok(Unit)
            coEvery { repo.add("u1") } returns ApiResult.Ok(FavoriteDto(StationDto("u1", "Uno"), 1))
            val v = vm()
            assertTrue(v.awaitSettled() is FavoritesUiState.Content)
            v.removeWithUndo("u1")
            var s = v.awaitSettled() as FavoritesUiState.Content
            assertEquals(listOf("u2"), s.items.map { it.station.id })
            assertTrue(s.pendingUndo != null)
            v.undoRemove()
            s = v.awaitSettled() as FavoritesUiState.Content
            assertEquals(listOf("u1", "u2"), s.items.map { it.station.id })
            assertTrue(s.pendingUndo == null)
        }

    @Test
    fun `quitar con error revierte y avisa`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(FavoritesResult(favs, offline = false))
            coEvery { repo.remove("u1") } returns ApiResult.Err(DomainError.Unavailable("x"))
            val v = vm()
            assertTrue(v.awaitSettled() is FavoritesUiState.Content)
            v.removeWithUndo("u1")
            val s = v.awaitSettled() as FavoritesUiState.Content
            assertEquals(listOf("u1", "u2"), s.items.map { it.station.id })
            assertTrue(s.pendingUndo == null)
        }

    @Test
    fun `undo expira a los 10 segundos`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                coEvery { observe() } returns ApiResult.Ok(FavoritesResult(favs, offline = false))
                coEvery { repo.remove("u1") } returns ApiResult.Ok(Unit)
                val v = vm()
                testScheduler.runCurrent()
                v.removeWithUndo("u1")
                testScheduler.runCurrent()
                var s = v.awaitSettled() as FavoritesUiState.Content
                assertTrue(s.pendingUndo != null)
                advanceTimeBy(10_001)
                testScheduler.runCurrent()
                s = v.awaitSettled() as FavoritesUiState.Content
                assertTrue(s.pendingUndo == null)
                assertEquals(listOf("u2"), s.items.map { it.station.id })
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `reorder OK guarda y refresca`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(FavoritesResult(favs, offline = false))
            coEvery { reorder(any(), any()) } returns ApiResult.Ok(Unit)
            val v = vm()
            assertTrue(v.awaitSettled() is FavoritesUiState.Content)
            v.moveItem(1, 0)
            var s = v.awaitSettled() as FavoritesUiState.Content
            assertEquals(listOf("u2", "u1"), s.items.map { it.station.id })
            v.commitOrder()
            s = v.awaitSettled() as FavoritesUiState.Content
            assertEquals(false, s.savingOrder)
        }

    @Test
    fun `reorder con divergencia revierte al ultimo confirmado`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(FavoritesResult(favs, offline = false))
            coEvery { reorder(any(), any()) } returns ApiResult.Err(DomainError.Conflict("x", ""))
            val v = vm()
            assertTrue(v.awaitSettled() is FavoritesUiState.Content)
            v.moveItem(1, 0)
            v.commitOrder()
            val s = v.awaitSettled() as FavoritesUiState.Content
            assertEquals(listOf("u1", "u2"), s.items.map { it.station.id })
            assertEquals(false, s.savingOrder)
        }

    @Test
    fun `toggle con error revierte`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(FavoritesResult(favs, offline = false))
            coEvery { toggle("u3", false) } returns ApiResult.Err(DomainError.Unavailable("x"))
            val v = vm()
            assertTrue(v.awaitSettled() is FavoritesUiState.Content)
            v.toggleFavorite("u3")
            val s = v.awaitSettled() as FavoritesUiState.Content
            assertEquals(listOf("u1", "u2"), s.items.map { it.station.id })
        }
}
