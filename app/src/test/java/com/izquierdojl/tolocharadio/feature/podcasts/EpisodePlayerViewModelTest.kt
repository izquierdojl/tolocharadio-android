package com.izquierdojl.tolocharadio.feature.podcasts

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisodeProgress
import com.izquierdojl.tolocharadio.data.remote.dto.PodcastSummary
import com.izquierdojl.tolocharadio.data.repo.ProgressRepo
import com.izquierdojl.tolocharadio.data.repo.QueueRepo
import com.izquierdojl.tolocharadio.domain.podcasts.DismissEpisodeUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePlaybackController
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePlaybackSource
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePlayerEvent
import com.izquierdojl.tolocharadio.domain.podcasts.NextEpisodeUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.ResolveEpisodePlaybackUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Orquestación VOD: reproducir, pausar, completar y auto-avance (T038, Red→Green). */
class EpisodePlayerViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val controller: EpisodePlaybackController = mockk(relaxed = true)
    private val progress: ProgressRepo = mockk()
    private val queue: QueueRepo = mockk()
    private val resolve: ResolveEpisodePlaybackUseCase = mockk()
    private val next: NextEpisodeUseCase = mockk()
    private val dismiss: DismissEpisodeUseCase = mockk()

    private val episode = PodcastEpisode(11L, 7L, "E1", durationSec = 600)
    private val episodeNext = PodcastEpisode(12L, 7L, "E2", durationSec = 300)
    private val show = PodcastSummary(7L, "https://a.example/f.xml", "Tolocha")
    private val events = MutableSharedFlow<EpisodePlayerEvent>(extraBufferCapacity = 1)
    private val controllerState =
        MutableStateFlow<EpisodePlaybackController.State>(EpisodePlaybackController.State.Idle)

    private fun vm(): EpisodePlayerViewModel {
        every { controller.state } returns controllerState
        every { controller.events } returns events
        every { queue.queue } returns
            MutableStateFlow(com.izquierdojl.tolocharadio.data.repo.QueueState(items = listOf(episodeNext)))
        return EpisodePlayerViewModel(controller, progress, queue, resolve, next, dismiss)
    }

    @Test
    fun `play resuelve fuente y arranca el controlador`() =
        runTest {
            val v = vm()
            coEvery { resolve(episode) } returns
                ApiResult.Ok(EpisodePlaybackSource("https://cdn.example/e", false, 9_999_999_999L))
            coEvery { progress.report(any(), any(), any(), any(), any(), any(), any()) } returns
                ApiResult.Ok(PodcastEpisodeProgress())
            v.play(episode, show)
            coVerify { controller.play(any(), episode, show, 0L, 1f) }
            assertEquals(11L, v.ui.first().episode?.id)
        }

    @Test
    fun `toggle en reproduccion pausa e informa`() =
        runTest {
            val v = vm()
            coEvery { resolve(episode) } returns
                ApiResult.Ok(EpisodePlaybackSource("https://cdn.example/e", false, 9_999_999_999L))
            coEvery { progress.report(any(), any(), any(), any(), any(), any(), any()) } returns
                ApiResult.Ok(PodcastEpisodeProgress())
            v.play(episode, show)
            controllerState.value =
                EpisodePlaybackController.State.Active(episode, show, true, 60_000L, 600_000L, 1f, false, false)
            v.toggle()
            coVerify { controller.pause() }
            coVerify { progress.report(11L, any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `fin natural completa y avanza al siguiente`() =
        runTest {
            val v = vm()
            coEvery { resolve(episode) } returns
                ApiResult.Ok(EpisodePlaybackSource("https://cdn.example/e", false, 9_999_999_999L))
            coEvery { progress.report(any(), any(), any(), any(), any(), any(), any()) } returns
                ApiResult.Ok(PodcastEpisodeProgress())
            coEvery { next(11L) } returns 12L
            coEvery { resolve(match { it.id == 12L }) } returns
                ApiResult.Ok(EpisodePlaybackSource("https://cdn.example/e2", false, 9_999_999_999L))
            v.play(episode, show)
            events.emit(EpisodePlayerEvent.Ended(11L, 600L))
            coVerify { progress.report(11L, any(), any(), 600L, any(), any(), any()) }
            coVerify { controller.play(any(), match { it.id == 12L }, any(), 0L, 1f) }
            assertTrue(v.ui.value.message == null)
        }
}
