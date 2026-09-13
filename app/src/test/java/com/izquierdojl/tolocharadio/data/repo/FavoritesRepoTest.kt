package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.local.CachedFavorite
import com.izquierdojl.tolocharadio.data.local.FavoritesCacheDao
import com.izquierdojl.tolocharadio.data.local.TolochaDb
import com.izquierdojl.tolocharadio.data.remote.api.FavoritesApi
import com.izquierdojl.tolocharadio.data.remote.api.OkResult
import com.izquierdojl.tolocharadio.data.remote.dto.FavoriteDto
import com.izquierdojl.tolocharadio.data.remote.dto.FavoriteListDto
import com.izquierdojl.tolocharadio.data.remote.dto.FavoriteResultDto
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class FavoritesRepoTest {
    private val api: FavoritesApi = mockk()
    private val dao: FavoritesCacheDao = mockk(relaxed = true)
    private val db: TolochaDb = mockk { every { favoritesCache() } returns dao }
    private val repo = FavoritesRepo(api, db)

    private val fav1 = FavoriteDto(StationDto("u1", "Uno"), 1000)
    private val fav2 = FavoriteDto(StationDto("u2", "Dos"), 2000)

    @Test
    fun `list OK publica ids deduplica y cachea en orden`() =
        runTest {
            coEvery { api.list() } returns
                Response.success(FavoriteListDto(listOf(fav1, fav2, fav1)))
            val r = repo.list()
            assertTrue(r is ApiResult.Ok)
            val ok = r as ApiResult.Ok
            assertEquals(false, ok.value.offline)
            assertEquals(listOf("u1", "u2"), ok.value.items.map { it.station.id })
            assertEquals(setOf("u1", "u2"), repo.favoriteIds.value)
            assertEquals(listOf(fav1, fav2), repo.favorites.value)
            coVerify { dao.replaceAll(match { it.map(CachedFavorite::id) == listOf("u1", "u2") }) }
        }

    @Test
    fun `list 503 con cache devuelve cache offline`() =
        runTest {
            coEvery { api.list() } throws java.io.IOException("down")
            coEvery { dao.loadOrdered() } returns
                listOf(CachedFavorite("u9", "Nueve", null, null, null, "", 5, 0, 0))
            val r = repo.list()
            assertTrue(r is ApiResult.Ok)
            val ok = r as ApiResult.Ok
            assertEquals(true, ok.value.offline)
            assertEquals("u9", ok.value.items.single().station.id)
            assertEquals(setOf("u9"), repo.favoriteIds.value)
            assertEquals("u9", repo.favorites.value.single().station.id)
        }

    @Test
    fun `list 503 sin cache devuelve Unavailable`() =
        runTest {
            coEvery { api.list() } throws java.io.IOException("down")
            coEvery { dao.loadOrdered() } returns emptyList()
            val r = repo.list()
            assertTrue((r as ApiResult.Err).error is DomainError.Unavailable)
        }

    @Test
    fun `list 401 con cache devuelve cache offline`() =
        runTest {
            coEvery { api.list() } returns
                Response.error(401, """{"error":{"code":"UNAUTHORIZED","message":"x","status":401}}""".toResponseBody())
            coEvery { dao.loadOrdered() } returns
                listOf(CachedFavorite("u9", "Nueve", null, null, null, "", 5, 0, 0))
            val r = repo.list()
            assertTrue(r is ApiResult.Ok)
            val ok = r as ApiResult.Ok
            assertEquals(true, ok.value.offline)
            assertEquals("u9", ok.value.items.single().station.id)
        }

    @Test
    fun `list 401 sin cache devuelve Unauthorized`() =
        runTest {
            coEvery { api.list() } returns
                Response.error(401, """{"error":{"code":"UNAUTHORIZED","message":"x","status":401}}""".toResponseBody())
            coEvery { dao.loadOrdered() } returns emptyList()
            val r = repo.list()
            assertTrue((r as ApiResult.Err).error is DomainError.Unauthorized)
        }

    @Test
    fun `add OK suma el id al flujo`() =
        runTest {
            coEvery { api.add(any()) } returns Response.success(FavoriteResultDto(fav1))
            val r = repo.add("u1")
            assertTrue(r is ApiResult.Ok)
            assertEquals(setOf("u1"), repo.favoriteIds.value)
            assertEquals(listOf(fav1), repo.favorites.value)
            coVerify { dao.upsert(match { it.id == "u1" }) }
        }

    @Test
    fun `add OK agrega la favorita al final de la lista observada`() =
        runTest {
            coEvery { api.list() } returns Response.success(FavoriteListDto(listOf(fav1)))
            coEvery { api.add(any()) } returns Response.success(FavoriteResultDto(fav2))
            repo.list()
            repo.add("u2")
            assertEquals(listOf("u1", "u2"), repo.favorites.value.map { it.station.id })
        }

    @Test
    fun `add en blanco no llama a la API`() =
        runTest {
            val r = repo.add("  ")
            assertTrue(r is ApiResult.Err)
            coVerify(exactly = 0) { api.add(any()) }
        }

    @Test
    fun `remove OK resta el id del flujo`() =
        runTest {
            coEvery { api.add(any()) } returns Response.success(FavoriteResultDto(fav1))
            coEvery { api.remove("u1") } returns Response.success(OkResult(true))
            repo.add("u1")
            val r = repo.remove("u1")
            assertTrue(r is ApiResult.Ok)
            assertEquals(emptySet<String>(), repo.favoriteIds.value)
            assertEquals(emptyList<FavoriteDto>(), repo.favorites.value)
            coVerify { dao.deleteById("u1") }
        }

    @Test
    fun `remove 404 reconcilia como no favorita`() =
        runTest {
            coEvery { api.remove("u1") } returns
                Response.error(404, """{"error":{"code":"NOT_FOUND","message":"x","status":404}}""".toResponseBody())
            val r = repo.remove("u1")
            assertTrue(r is ApiResult.Ok)
            assertEquals(emptySet<String>(), repo.favoriteIds.value)
        }

    @Test
    fun `reorder envia los ids exactos`() =
        runTest {
            coEvery { api.reorder(any()) } returns Response.success(OkResult(true))
            val r = repo.reorder(listOf("u2", "u1"))
            assertTrue(r is ApiResult.Ok)
            coVerify { api.reorder(match { it.stationIds == listOf("u2", "u1") }) }
        }
}
