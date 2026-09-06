package com.izquierdojl.tolocharadio.data.repo

import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.remote.api.PlaybackApi
import com.izquierdojl.tolocharadio.data.remote.api.streamUrl
import com.izquierdojl.tolocharadio.data.remote.dto.PlaybackStatusDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class PlaybackRepoTest {
    private val api: PlaybackApi = mockk()
    private val repo = PlaybackRepo(api)

    @Test
    fun `status no playable con motivo`() =
        runTest {
            coEvery { api.status("u1") } returns
                Response.success(PlaybackStatusDto("u1", false, "offline"))
            val r = repo.status("u1")
            assertTrue(r is ApiResult.Ok && !(r as ApiResult.Ok).value.playable)
        }

    @Test
    fun `streamUrl no lleva token`() {
        val url = streamUrl("https://radio.test/", "u1")
        assertEquals("https://radio.test/api/v1/playback/u1", url)
        assertTrue(!url.contains("token", ignoreCase = true))
    }
}
