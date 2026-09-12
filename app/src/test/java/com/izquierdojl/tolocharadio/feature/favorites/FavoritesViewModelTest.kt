package com.izquierdojl.tolocharadio.feature.favorites

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.repo.FavoritesRepo
import com.izquierdojl.tolocharadio.domain.ObserveFavoritesUseCase
import com.izquierdojl.tolocharadio.domain.ReorderFavoritesUseCase
import com.izquierdojl.tolocharadio.domain.ToggleFavoriteUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FavoritesViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val observe: ObserveFavoritesUseCase = mockk()
    private val toggle: ToggleFavoriteUseCase = mockk(relaxed = true)
    private val reorder: ReorderFavoritesUseCase = mockk(relaxed = true)
    private val repo: FavoritesRepo = mockk(relaxed = true)

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
}
