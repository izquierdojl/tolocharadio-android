package com.izquierdojl.tolocharadio.domain

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.remote.dto.FavoriteDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.FavoritesRepo
import com.izquierdojl.tolocharadio.data.repo.FavoritesResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveFavoritesUseCaseTest {
    private val repo: FavoritesRepo = mockk()
    private val useCase = ObserveFavoritesUseCase(repo)

    @Test
    fun `lista OK deduplica conservando el primer orden`() =
        runTest {
            val dup =
                FavoritesResult(
                    listOf(
                        FavoriteDto(StationDto("u1", "Uno"), 1),
                        FavoriteDto(StationDto("u2", "Dos"), 2),
                        FavoriteDto(StationDto("u1", "Uno"), 3),
                    ),
                    offline = false,
                )
            coEvery { repo.list() } returns ApiResult.Ok(dup)
            val r = useCase() as ApiResult.Ok
            assertEquals(listOf("u1", "u2"), r.value.items.map { it.station.id })
        }

    @Test
    fun `error se propaga sin transformar`() =
        runTest {
            coEvery { repo.list() } returns ApiResult.Err(DomainError.Unavailable("x"))
            val r = useCase()
            assertTrue((r as ApiResult.Err).error is DomainError.Unavailable)
        }
}

class ReorderFavoritesUseCaseTest {
    private val repo: FavoritesRepo = mockk(relaxed = true)
    private val useCase = ReorderFavoritesUseCase(repo)

    @Test
    fun `permutacion exacta delega el PUT`() =
        runTest {
            coEvery { repo.reorder(any()) } returns ApiResult.Ok(Unit)
            val r = useCase(current = listOf("u1", "u2"), next = listOf("u2", "u1"))
            assertTrue(r is ApiResult.Ok)
            coVerify { repo.reorder(listOf("u2", "u1")) }
        }

    @Test
    fun `conjunto distinto aborta sin llamar a la API`() =
        runTest {
            val r = useCase(current = listOf("u1", "u2"), next = listOf("u2", "u3"))
            assertTrue(r is ApiResult.Err)
            coVerify(exactly = 0) { repo.reorder(any()) }
        }

    @Test
    fun `duplicados abortan sin llamar a la API`() =
        runTest {
            val r = useCase(current = listOf("u1", "u2"), next = listOf("u1", "u1"))
            assertTrue(r is ApiResult.Err)
            coVerify(exactly = 0) { repo.reorder(any()) }
        }

    @Test
    fun `id en blanco aborta sin llamar a la API`() =
        runTest {
            val r = useCase(current = listOf("u1", ""), next = listOf("", "u1"))
            assertTrue(r is ApiResult.Err)
            coVerify(exactly = 0) { repo.reorder(any()) }
        }
}

