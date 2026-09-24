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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val observe: ObserveFavoritesUseCase = mockk()
    private val toggle: ToggleFavoriteUseCase = mockk(relaxed = true)
    private val reorder: ReorderFavoritesUseCase = mockk(relaxed = true)
    private val favoritesFlow = MutableStateFlow<List<FavoriteDto>>(emptyList())
    private val repo: FavoritesRepo =
        mockk(relaxed = true) {
            every { favorites } returns favoritesFlow
        }

    @Test
    fun `error de credenciales marca la edicion de servidor (FR-006)`() =
        runTest {
            coEvery { observe() } returns ApiResult.Err(DomainError.Unauthorized("bad_credentials"))
            val v = FavoritesViewModel(observe, toggle, reorder, repo)
            advanceUntilIdle()
            val state = v.ui.value as FavoritesUiState.Error
            assertTrue(state.isAuthError)
        }

    @Test
    fun `error de red no marca autenticacion`() =
        runTest {
            coEvery { observe() } returns ApiResult.Err(DomainError.Unavailable("down"))
            val v = FavoritesViewModel(observe, toggle, reorder, repo)
            advanceUntilIdle()
            val state = v.ui.value as FavoritesUiState.Error
            assertFalse(state.isAuthError)
        }

    @Test
    fun `alta externa aparece al momento sin recrear el ViewModel`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(FavoritesResult(emptyList(), offline = false))
            val v = FavoritesViewModel(observe, toggle, reorder, repo)
            advanceUntilIdle()
            assertTrue(v.ui.value is FavoritesUiState.Empty)

            favoritesFlow.value = listOf(FavoriteDto(StationDto("u1", "Uno"), 1000))
            advanceUntilIdle()

            val state = v.ui.value as FavoritesUiState.Content
            assertEquals("u1", state.items.single().station.id)
        }

    @Test
    fun `baja externa vacia la lista`() =
        runTest {
            val favorite = FavoriteDto(StationDto("u1", "Uno"), 1000)
            favoritesFlow.value = listOf(favorite)
            coEvery { observe() } returns ApiResult.Ok(FavoritesResult(listOf(favorite), offline = false))
            val v = FavoritesViewModel(observe, toggle, reorder, repo)
            advanceUntilIdle()
            assertTrue(v.ui.value is FavoritesUiState.Content)

            favoritesFlow.value = emptyList()
            advanceUntilIdle()

            assertTrue(v.ui.value is FavoritesUiState.Empty)
        }

    @Test
    fun `quitar con deshacer no se pisa al confirmar el repo`() =
        runTest {
            val favorite = FavoriteDto(StationDto("u1", "Uno"), 1000)
            favoritesFlow.value = listOf(favorite)
            coEvery { observe() } returns ApiResult.Ok(FavoritesResult(listOf(favorite), offline = false))
            coEvery { repo.remove("u1") } returns ApiResult.Ok(Unit)
            val v = FavoritesViewModel(observe, toggle, reorder, repo)
            advanceUntilIdle()

            v.removeWithUndo("u1")
            favoritesFlow.value = emptyList()
            runCurrent()

            val state = v.ui.value as FavoritesUiState.Content
            assertEquals(emptyList<FavoriteDto>(), state.items)
            assertNotNull(state.pendingUndo)
        }

    private fun twoFavorites(): List<FavoriteDto> =
        listOf(
            FavoriteDto(StationDto("u1", "Uno"), 1000),
            FavoriteDto(StationDto("u2", "Dos"), 2000),
        )

    private fun viewModelWith(
        items: List<FavoriteDto>,
        offline: Boolean = false,
        reorderResult: ApiResult<Unit> = ApiResult.Ok(Unit),
    ): FavoritesViewModel {
        favoritesFlow.value = items
        coEvery { observe() } returns ApiResult.Ok(FavoritesResult(items, offline = offline))
        coEvery { reorder(any(), any()) } returns reorderResult
        return FavoritesViewModel(observe, toggle, reorder, repo)
    }

    @Test
    fun `commitOrder error restaura el orden confirmado y avisa (FR-010)`() =
        runTest {
            val v =
                viewModelWith(
                    twoFavorites(),
                    reorderResult = ApiResult.Err(DomainError.Unavailable("down")),
                )
            advanceUntilIdle()

            v.messages.test {
                v.moveItem(1, 0)
                v.commitOrder()
                advanceUntilIdle()
                val state = v.ui.value as FavoritesUiState.Content
                assertEquals(listOf("u1", "u2"), state.items.map { it.station.id })
                val msg = awaitItem()
                assertEquals("No se pudo guardar el orden.", msg.text)
                assertEquals("Reintentar", msg.actionLabel)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `reintento tras fallo recuperable vuelve a intentar el guardado (FR-010)`() =
        runTest {
            favoritesFlow.value = twoFavorites()
            coEvery { observe() } returns ApiResult.Ok(FavoritesResult(twoFavorites(), offline = false))
            var calls = 0
            coEvery { reorder(any(), any()) } answers {
                calls++
                if (calls == 1) ApiResult.Err(DomainError.Unavailable("down")) else ApiResult.Ok(Unit)
            }
            val v = FavoritesViewModel(observe, toggle, reorder, repo)
            advanceUntilIdle()

            v.messages.test {
                v.moveItem(1, 0)
                v.commitOrder()
                advanceUntilIdle()
                val msg = awaitItem()
                assertEquals("Reintentar", msg.actionLabel)
                assertNotNull(msg.onAction)
                msg.onAction?.invoke()
                advanceUntilIdle()
                assertEquals(2, calls)
                val state = v.ui.value as FavoritesUiState.Content
                assertFalse(state.savingOrder)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `commitOrder conflicto muestra el orden del servidor y avisa (FR-011)`() =
        runTest {
            val v =
                viewModelWith(
                    twoFavorites(),
                    reorderResult = ApiResult.Err(DomainError.Conflict("ORDER_CHANGED", "")),
                )
            advanceUntilIdle()

            v.messages.test {
                v.moveItem(0, 1)
                v.commitOrder()
                advanceUntilIdle()
                val state = v.ui.value as FavoritesUiState.Content
                assertEquals(listOf("u1", "u2"), state.items.map { it.station.id })
                val msg = awaitItem()
                assertEquals("El orden cambió en otro dispositivo. Mostrando el guardado.", msg.text)
                assertEquals(null, msg.actionLabel)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
