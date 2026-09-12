package com.izquierdojl.tolocharadio.domain.shortcuts

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.remote.dto.HistoryEntryDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.HistoryRepo
import com.izquierdojl.tolocharadio.data.repo.StationsRepo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResolveShortcutLaunchUseCaseTest {
    private val items = MutableStateFlow<List<HistoryEntryDto>>(emptyList())
    private val historyRepo = mockk<HistoryRepo>(relaxed = true)
    private val stationsRepo = mockk<StationsRepo>(relaxed = true)
    private val useCase = ResolveShortcutLaunchUseCase(historyRepo, stationsRepo)

    @Before
    fun setup() {
        every { historyRepo.items } returns items
    }

    @Test
    fun `station id en blanco devuelve Unavailable`() =
        runTest {
            assertTrue(useCase("  ") is ShortcutLaunchResolution.Unavailable)
        }

    @Test
    fun `reproduce desde el historial en memoria`() =
        runTest {
            items.value = listOf(HistoryEntryDto(StationDto("s1", "Rock"), 1000))
            val result = useCase("s1")
            assertTrue(result is ShortcutLaunchResolution.Play)
            assertEquals("s1", (result as ShortcutLaunchResolution.Play).station.id)
        }

    @Test
    fun `usa la cache si no esta en memoria`() =
        runTest {
            coEvery { historyRepo.snapshot() } returns
                listOf(HistoryEntryDto(StationDto("s2", "Jazz"), 500))
            val result = useCase("s2")
            assertEquals("s2", (result as ShortcutLaunchResolution.Play).station.id)
        }

    @Test
    fun `consulta al servidor si no esta en cache`() =
        runTest {
            coEvery { historyRepo.snapshot() } returns emptyList()
            coEvery { stationsRepo.detail("s3") } returns ApiResult.Ok(StationDto("s3", "Pop"))
            val result = useCase("s3")
            assertEquals("s3", (result as ShortcutLaunchResolution.Play).station.id)
        }

    @Test
    fun `error remoto devuelve Unavailable`() =
        runTest {
            coEvery { historyRepo.snapshot() } returns emptyList()
            coEvery { stationsRepo.detail("s4") } returns
                ApiResult.Err(DomainError.NotFound("STATION_NOT_FOUND"))
            assertTrue(useCase("s4") is ShortcutLaunchResolution.Unavailable)
        }
}
