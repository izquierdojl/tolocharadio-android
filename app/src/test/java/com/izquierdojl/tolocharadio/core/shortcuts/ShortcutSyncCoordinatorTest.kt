package com.izquierdojl.tolocharadio.core.shortcuts

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.remote.dto.HistoryEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.HistoryRepo
import com.izquierdojl.tolocharadio.data.repo.HistoryResult
import com.izquierdojl.tolocharadio.domain.shortcuts.BuildShortcutStationsUseCase
import com.izquierdojl.tolocharadio.domain.shortcuts.ShortcutStation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShortcutSyncCoordinatorTest {
    private val publisher = mockk<ShortcutPublisher>(relaxed = true)
    private val items = MutableStateFlow<List<HistoryEntryDto>>(emptyList())
    private val historyRepo = mockk<HistoryRepo>(relaxed = true)
    private val build = BuildShortcutStationsUseCase()

    @Before
    fun setup() {
        every { historyRepo.items } returns items
        every { publisher.maxSlots() } returns 5
    }

    private fun TestScope.newCoordinator() =
        ShortcutSyncCoordinator(
            publisher = publisher,
            historyRepo = historyRepo,
            buildStations = build,
            scope =
                CoroutineScope(
                    backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler),
                ),
        )

    private fun entry(
        id: String,
        playedAt: Long,
    ) = HistoryEntryDto(StationDto(id = id, name = "Emisora $id"), playedAt)

    @Test
    fun `publica al cambiar el historial`() =
        runTest {
            coEvery { historyRepo.list() } returns ApiResult.Ok(HistoryResult(emptyList(), false))
            val coordinator = newCoordinator()
            coordinator.start()
            advanceUntilIdle()
            items.value = listOf(entry("s1", 1000), entry("s2", 2000))
            advanceUntilIdle()
            coVerify {
                publisher.publish(match { it.map(ShortcutStation::stationId) == listOf("s2", "s1") })
            }
        }

    @Test
    fun `onForeground refresca el historial`() =
        runTest {
            coEvery { historyRepo.list() } returns ApiResult.Ok(HistoryResult(emptyList(), false))
            val coordinator = newCoordinator()
            coordinator.onForeground()
            advanceUntilIdle()
            coVerify { historyRepo.list() }
        }

    @Test
    fun `offline usa la cache cuando el refresco falla`() =
        runTest {
            coEvery { historyRepo.list() } returns ApiResult.Err(DomainError.Unavailable("down"))
            coEvery { historyRepo.snapshot() } returns listOf(entry("s9", 5000))
            val coordinator = newCoordinator()
            coordinator.onForeground()
            advanceUntilIdle()
            coVerify { publisher.publish(match { it.single().stationId == "s9" }) }
        }

    @Test
    fun `clearNow limpia los accesos`() =
        runTest {
            val coordinator = newCoordinator()
            coordinator.clearNow()
            coVerify { publisher.clear() }
        }
}
