package com.izquierdojl.tolocharadio.feature.history

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.repo.HistoryRepo
import com.izquierdojl.tolocharadio.domain.ObserveHistoryUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val observe: ObserveHistoryUseCase = mockk()
    private val repo: HistoryRepo = mockk(relaxed = true)

    @Test
    fun `error de credenciales marca la edicion de servidor (FR-006)`() =
        runTest {
            coEvery { observe() } returns ApiResult.Err(DomainError.Unauthorized("bad_credentials"))
            val v = HistoryViewModel(observe, repo)
            advanceUntilIdle()
            val state = v.ui.value as HistoryUiState.Error
            assertTrue(state.isAuthError)
        }

    @Test
    fun `error de red no marca autenticacion`() =
        runTest {
            coEvery { observe() } returns ApiResult.Err(DomainError.Unavailable("down"))
            val v = HistoryViewModel(observe, repo)
            advanceUntilIdle()
            val state = v.ui.value as HistoryUiState.Error
            assertFalse(state.isAuthError)
        }
}
