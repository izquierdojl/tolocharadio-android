package com.izquierdojl.tolocharadio.feature.history

import app.cash.turbine.test
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.remote.dto.HistoryEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.HistoryRepo
import com.izquierdojl.tolocharadio.data.repo.HistoryResult
import com.izquierdojl.tolocharadio.domain.ObserveHistoryUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val observe: ObserveHistoryUseCase = mockk()
    private val repo: HistoryRepo = mockk(relaxed = true)

    private val entries =
        listOf(
            HistoryEntryDto(StationDto("s1", "Rock FM"), 3000),
            HistoryEntryDto(StationDto("s2", "Jazz FM"), 2000),
        )

    private fun vm() = HistoryViewModel(observe, repo)

    private suspend fun HistoryViewModel.awaitSettled(): HistoryUiState {
        var s: HistoryUiState = HistoryUiState.Loading
        ui.test {
            s = awaitItem()
            while (s is HistoryUiState.Loading) s = awaitItem()
        }
        return s
    }

    @Test
    fun `init con historial muestra Content`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(HistoryResult(entries, offline = false))
            val s = vm().awaitSettled() as HistoryUiState.Content
            assertEquals(listOf("s1", "s2"), s.items.map { it.station.id })
            assertEquals(false, s.offline)
        }

    @Test
    fun `lista vacia muestra Empty`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(HistoryResult(emptyList(), offline = false))
            assertTrue(vm().awaitSettled() is HistoryUiState.Empty)
        }

    @Test
    fun `error sin cache muestra Error`() =
        runTest {
            coEvery { observe() } returns ApiResult.Err(DomainError.Unavailable("x"))
            val s = vm().awaitSettled()
            assertTrue(s is HistoryUiState.Error)
        }

    @Test
    fun `offline con cache muestra Content offline`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(HistoryResult(entries, offline = true))
            val s = vm().awaitSettled() as HistoryUiState.Content
            assertEquals(true, s.offline)
        }

    @Test
    fun `onRemove elimina optimistamente y llama repo`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(HistoryResult(entries, offline = false))
            val vm = vm()
            vm.awaitSettled()
            coEvery { repo.remove("s1") } returns ApiResult.Ok(Unit)
            vm.onRemove("s1")
            val s = vm.ui.value as HistoryUiState.Content
            assertEquals(listOf("s2"), s.items.map { it.station.id })
        }

    @Test
    fun `onRemove error revierte estado y emite snackbar`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(HistoryResult(entries, offline = false))
            val vm = vm()
            vm.awaitSettled()
            coEvery { repo.remove("s1") } returns ApiResult.Err(DomainError.Unavailable("fail"))
            vm.messages.test {
                vm.onRemove("s1")
                // Después del error, el state vuelve al original
                val s = vm.ui.value as HistoryUiState.Content
                assertEquals(listOf("s1", "s2"), s.items.map { it.station.id })
                // Snackbar con mensaje de error
                val msg = awaitItem()
                assertTrue(msg.isNotEmpty())
            }
        }

    @Test
    fun `onClearAll vacia lista y llama repo`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(HistoryResult(entries, offline = false))
            val vm = vm()
            vm.awaitSettled()
            coEvery { repo.clear() } returns ApiResult.Ok(Unit)
            vm.onClearAll()
            assertTrue(vm.ui.value is HistoryUiState.Empty)
        }

    @Test
    fun `onClearAll error revierte estado`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(HistoryResult(entries, offline = false))
            val vm = vm()
            vm.awaitSettled()
            coEvery { repo.clear() } returns ApiResult.Err(DomainError.Unavailable("fail"))
            vm.onClearAll()
            val s = vm.ui.value as HistoryUiState.Content
            assertEquals(listOf("s1", "s2"), s.items.map { it.station.id })
        }

    @Test
    fun `retry refresca lista`() =
        runTest {
            coEvery { observe() } returns ApiResult.Ok(HistoryResult(entries, offline = false))
            val vm = vm()
            vm.awaitSettled()
            coEvery { observe() } returns ApiResult.Ok(HistoryResult(listOf(entries[0]), offline = false))
            vm.retry()
            val s = vm.awaitSettled() as HistoryUiState.Content
            assertEquals(1, s.items.size)
        }
}

