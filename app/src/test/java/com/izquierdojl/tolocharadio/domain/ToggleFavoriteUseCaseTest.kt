package com.izquierdojl.tolocharadio.domain

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.remote.dto.FavoriteDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.data.repo.FavoritesRepo
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleFavoriteUseCaseTest {
    private val repo: FavoritesRepo = mockk()
    private val useCase = ToggleFavoriteUseCase(repo)

    @Test
    fun `anade cuando no es favorito`() =
        runTest {
            coEvery { repo.add("u1") } returns ApiResult.Ok(FavoriteDto(StationDto("u1", "T")))
            val r = useCase("u1", false)
            assertEquals(ApiResult.Ok(true), r)
        }

    @Test
    fun `error mantiene estado y lo propaga`() =
        runTest {
            coEvery { repo.remove("u1") } returns
                ApiResult.Err(com.izquierdojl.tolocharadio.core.network.DomainError.Unknown("x"))
            val r = useCase("u1", true)
            assertTrue(r is ApiResult.Err)
        }
}

