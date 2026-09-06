package com.example.tolocharadio.domain

import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.core.network.DomainError
import com.example.tolocharadio.data.remote.dto.HistoryEntryDto
import com.example.tolocharadio.data.remote.dto.StationDto
import com.example.tolocharadio.data.repo.HistoryRepo
import com.example.tolocharadio.data.repo.HistoryResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveHistoryUseCaseTest {
    private val repo: HistoryRepo = mockk()
    private val useCase = ObserveHistoryUseCase(repo)

    private val entry1 = HistoryEntryDto(StationDto("s1", "Rock FM"), 3000)
    private val entry2 = HistoryEntryDto(StationDto("s2", "Jazz FM"), 2000)

    @Test
    fun `invoke delega en repo list`() =
        runTest {
            coEvery { repo.list() } returns
                ApiResult.Ok(HistoryResult(listOf(entry1, entry2), offline = false))
            val r = useCase()
            assertTrue(r is ApiResult.Ok)
            val ok = r as ApiResult.Ok
            assertEquals(2, ok.value.items.size)
            assertEquals(false, ok.value.offline)
        }

    @Test
    fun `invoke propagates error`() =
        runTest {
            coEvery { repo.list() } returns ApiResult.Err(DomainError.Unavailable("x"))
            val r = useCase()
            assertTrue(r is ApiResult.Err)
        }

    @Test
    fun `invoke returns offline flag`() =
        runTest {
            coEvery { repo.list() } returns
                ApiResult.Ok(HistoryResult(listOf(entry1), offline = true))
            val r = useCase()
            assertTrue((r as ApiResult.Ok).value.offline)
        }
}
