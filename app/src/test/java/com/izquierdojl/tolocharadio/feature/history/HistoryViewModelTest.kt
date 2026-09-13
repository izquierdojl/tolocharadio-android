package com.izquierdojl.tolocharadio.feature.history

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.remote.dto.HistoryEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.HistoryRepo
import com.izquierdojl.tolocharadio.data.repo.HistoryResult
import com.izquierdojl.tolocharadio.domain.ObserveHistoryUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val observe: ObserveHistoryUseCase = mockk()
    private val itemsFlow = MutableStateFlow<List<HistoryEntryDto>>(emptyList())
    private val repo: HistoryRepo =
        mockk(relaxed = true) {
            every { items } returns itemsFlow
        }

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

    @Test
    fun `escucha nueva en el repo aparece al momento`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(HistoryResult(emptyList(), offline = false))
            val v = HistoryViewModel(observe, repo)
            advanceUntilIdle()
            assertTrue(v.ui.value is HistoryUiState.Empty)

            itemsFlow.value = listOf(HistoryEntryDto(StationDto("s1", "Rock FM"), 4000))
            advanceUntilIdle()

            val state = v.ui.value as HistoryUiState.Content
            assertEquals("s1", state.items.single().station.id)
        }
}
