package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.remote.api.EpisodeIdBody
import com.izquierdojl.tolocharadio.data.remote.api.PodcastEpisodeWrapper
import com.izquierdojl.tolocharadio.data.remote.api.PodcastFavoriteList
import com.izquierdojl.tolocharadio.data.remote.api.PodcastsApi
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/** Favoritos de episodios (T046, Red→Green). */
class EpisodeFavoritesRepoTest {
    private val api: PodcastsApi = mockk()
    private val repo = EpisodeFavoritesRepo(api)
    private val ep = PodcastEpisode(11L, 7L, "E1", isFavorite = true)

    @Test
    fun `list publica favoritos e ids`() =
        runTest {
            coEvery { api.favorites() } returns Response.success(PodcastFavoriteList(listOf(ep)))
            val r = repo.list()
            assertTrue(r is ApiResult.Ok)
            assertEquals(setOf(11L), repo.favoriteIds.value)
        }

    @Test
    fun `add suma el id al flujo`() =
        runTest {
            coEvery { api.addFavorite(EpisodeIdBody(11L)) } returns
                Response.success(PodcastEpisodeWrapper(ep))
            val r = repo.add(11L)
            assertTrue(r is ApiResult.Ok)
            assertEquals(setOf(11L), repo.favoriteIds.value)
        }

    @Test
    fun `remove 404 se reconcilia como exito`() =
        runTest {
            coEvery { api.removeFavorite(11L) } returns
                Response.error(404, """{"error":{"code":"NOT_FOUND","message":"x","status":404}}""".toResponseBody())
            val r = repo.remove(11L)
            assertTrue(r is ApiResult.Ok)
        }
}
