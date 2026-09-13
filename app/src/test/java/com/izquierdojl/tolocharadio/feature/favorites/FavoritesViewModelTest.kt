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
import io.mockk.coVerify
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
    fun `moveUp sube una posicion y guarda (FR-012)`() =
        runTest {
            val v = viewModelWith(twoFavorites())
            advanceUntilIdle()

            v.moveBy("u2", -1)
            advanceUntilIdle()

            coVerify { reorder(listOf("u1", "u2"), listOf("u2", "u1")) }
        }

    @Test
    fun `moveDown baja una posicion y guarda (FR-012)`() =
        runTest {
            val v = viewModelWith(twoFavorites())
            advanceUntilIdle()

            v.moveBy("u1", 1)
            advanceUntilIdle()

            coVerify { reorder(listOf("u1", "u2"), listOf("u2", "u1")) }
        }

    @Test
    fun `moveUp en el extremo no mueve ni guarda (FR-013)`() =
        runTest {
            val v = viewModelWith(twoFavorites())
            advanceUntilIdle()

            v.moveBy("u1", -1)
            advanceUntilIdle()

            val state = v.ui.value as FavoritesUiState.Content
            assertEquals(listOf("u1", "u2"), state.items.map { it.station.id })
            coVerify(exactly = 0) { reorder(any(), any()) }
        }

    @Test
    fun `moveDown en el extremo no mueve ni guarda (FR-013)`() =
        runTest {
            val v = viewModelWith(twoFavorites())
            advanceUntilIdle()

            v.moveBy("u2", 1)
            advanceUntilIdle()

            coVerify(exactly = 0) { reorder(any(), any()) }
        }

    @Test
    fun `move con id inexistente no hace nada`() =
        runTest {
            val v = viewModelWith(twoFavorites())
            advanceUntilIdle()

            v.moveBy("zzz", -1)
            v.moveBy("zzz", 1)
            advanceUntilIdle()

            coVerify(exactly = 0) { reorder(any(), any()) }
        }

    @Test
    fun `move sin conexion no hace nada (FR-018)`() =
        runTest {
            val v = viewModelWith(twoFavorites(), offline = true)
            advanceUntilIdle()

            v.moveBy("u2", -1)
            v.moveBy("u1", 1)
            advanceUntilIdle()

            coVerify(exactly = 0) { reorder(any(), any()) }
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
                v.moveBy("u2", -1)
                advanceUntilIdle()
                val state = v.ui.value as FavoritesUiState.Content
                assertEquals(listOf("u1", "u2"), state.items.map { it.station.id })
                assertTrue(awaitItem().isNotBlank())
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
                v.moveBy("u1", 1)
                advanceUntilIdle()
                val state = v.ui.value as FavoritesUiState.Content
                assertEquals(listOf("u1", "u2"), state.items.map { it.station.id })
                assertTrue(awaitItem().isNotBlank())
            }
        }
}
