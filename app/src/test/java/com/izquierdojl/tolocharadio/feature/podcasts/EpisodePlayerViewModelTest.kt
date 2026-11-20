package com.izquierdojl.tolocharadio.feature.podcasts

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.repo.ProgressRepo
import com.izquierdojl.tolocharadio.data.repo.QueueRepo
import com.izquierdojl.tolocharadio.data.repo.QueueState
import com.izquierdojl.tolocharadio.domain.podcasts.DismissEpisodeUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePlaybackController
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePlaybackSource
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePlayerEvent
import com.izquierdojl.tolocharadio.domain.podcasts.LoadEpisodeProgressUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.NextEpisodeUseCase
import com.izquierdojl.tolocharadio.domain.podcasts.ResolveEpisodePlaybackUseCase
import com.izquierdojl.tolocharadio.feature.player.VoiceBoostController
import com.izquierdojl.tolocharadio.support.testEpisode
import com.izquierdojl.tolocharadio.support.testProgress
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Reanudación desde la cola y auto-avance (US1, FR-001–FR-006). */
class EpisodePlayerViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val state = MutableStateFlow<EpisodePlaybackController.State>(EpisodePlaybackController.State.Idle)
    private val events = MutableSharedFlow<EpisodePlayerEvent>(extraBufferCapacity = 1)
    private val controller = mockk<EpisodePlaybackController>(relaxed = true)
    private val progress = mockk<ProgressRepo>(relaxed = true)
    private val queue = mockk<QueueRepo>(relaxed = true)
    private val resolve = mockk<ResolveEpisodePlaybackUseCase>()
    private val next = mockk<NextEpisodeUseCase>(relaxed = true)
    private val dismiss = mockk<DismissEpisodeUseCase>(relaxed = true)
    private val loadProgress = mockk<LoadEpisodeProgressUseCase>(relaxed = true)
    private val voiceBoost = mockk<VoiceBoostController>(relaxed = true)

    init {
        every { controller.state } returns state
        every { controller.events } returns events
        every { controller.positionMs() } returns 0L
        every { voiceBoost.enabled } returns MutableStateFlow(false)
    }

    private val viewModel by lazy {
        EpisodePlayerViewModel(
            controller = controller,
            progress = progress,
            queue = queue,
            resolve = resolve,
            next = next,
            dismiss = dismiss,
            loadProgress = loadProgress,
            voiceBoost = voiceBoost,
        )
    }

    private fun source() = EpisodePlaybackSource(uri = "https://cdn/e.mp3", local = false, expiresAt = 0L)

    private fun prepareResolve() {
        coEvery { resolve(any()) } returns ApiResult.Ok(source())
        coEvery { loadProgress(any()) } returns ApiResult.Ok(null)
    }

    private fun prepareQueue(vararg episodes: com.izquierdojl.tolocharadio.data.remote.dto.PodcastEpisode) {
        every { queue.queue } returns MutableStateFlow(QueueState(revision = 1, items = episodes.toList(), limit = 200))
    }

    @Test
    fun `play usa el progreso embebido`() =
        runTest {
            prepareResolve()
            val episode = testEpisode(progress = testProgress(120L, 1_800L))

            viewModel.play(episode, null)
            advanceUntilIdle()

            verify(exactly = 1) { controller.play(any(), episode, null, 120_000L, any()) }
        }

    @Test
    fun `play prefiere el progreso remoto mas reciente`() =
        runTest {
            prepareResolve()
            val episode = testEpisode(progress = testProgress(120L, 1_800L))
            coEvery { loadProgress(1L) } returns ApiResult.Ok(testProgress(200L, 1_800L))

            viewModel.play(episode, null)
            advanceUntilIdle()

            verify(exactly = 1) { controller.play(any(), episode, null, 200_000L, any()) }
        }

    @Test
    fun `play consulta el progreso remoto cuando falta`() =
        runTest {
            prepareResolve()
            val episode = testEpisode(progress = null)
            coEvery { loadProgress(1L) } returns ApiResult.Ok(testProgress(90L, 1_800L))

            viewModel.play(episode, null)
            advanceUntilIdle()

            verify(exactly = 1) { controller.play(any(), episode, null, 90_000L, any()) }
        }

    @Test
    fun `play empieza en cero si el episodio esta completado`() =
        runTest {
            prepareResolve()
            val episode = testEpisode(progress = testProgress(1_800L, 1_800L, completed = true))

            viewModel.play(episode, null)
            advanceUntilIdle()

            verify(exactly = 1) { controller.play(any(), episode, null, 0L, any()) }
        }

    @Test
    fun `play empieza en cero si el progreso remoto falla`() =
        runTest {
            prepareResolve()
            val episode = testEpisode(progress = null)
            coEvery { loadProgress(1L) } returns
                ApiResult.Err(com.izquierdojl.tolocharadio.core.network.DomainError.Unavailable("offline"))

            viewModel.play(episode, null)
            advanceUntilIdle()

            verify(exactly = 1) { controller.play(any(), episode, null, 0L, any()) }
        }

    @Test
    fun `el auto-avance reanuda el siguiente episodio`() =
        runTest {
            prepareResolve()
            val first = testEpisode(id = 1L, progress = null)
            val second = testEpisode(id = 2L, progress = testProgress(300L, 1_800L))
            prepareQueue(first, second)
            coEvery { next(1L) } returns 2L

            viewModel.play(first, null)
            advanceUntilIdle()
            events.tryEmit(EpisodePlayerEvent.Ended(episodeId = 1L, durationSec = 1_800L))
            advanceUntilIdle()

            verify(exactly = 1) { controller.play(any(), second, null, 300_000L, any()) }
        }

    @Test
    fun `retroceder en el inicio se queda en cero`() =
        runTest {
            prepareResolve()
            val episode = testEpisode(progress = null)
            viewModel.play(episode, null)
            advanceUntilIdle()
            state.value =
                EpisodePlaybackController.State.Active(
                    episode = episode,
                    podcast = null,
                    playing = true,
                    positionMs = 5_000L,
                    durationMs = 1_800_000L,
                    rate = 1f,
                    local = false,
                    casting = false,
                )
            advanceUntilIdle()

            viewModel.skipBack()
            advanceUntilIdle()

            verify { controller.seekTo(0L) }
        }

    @Test
    fun `stop limpia el episodio y reporta la duracion capturada`() =
        runTest {
            prepareResolve()
            val episode = testEpisode(progress = null)
            viewModel.play(episode, null)
            advanceUntilIdle()
            state.value =
                EpisodePlaybackController.State.Active(
                    episode = episode,
                    podcast = null,
                    playing = true,
                    positionMs = 10_000L,
                    durationMs = 1_800_000L,
                    rate = 1f,
                    local = false,
                    casting = false,
                )
            advanceUntilIdle()
            every { controller.stop() } returns 10_000L

            viewModel.stop()
            advanceUntilIdle()

            assertNull(viewModel.ui.value.episode)
            coVerify {
                progress.report(
                    episodeId = 1L,
                    sessionId = any(),
                    sequence = any(),
                    positionSec = 10L,
                    durationSec = 1_800L,
                    listenedMs = 10_000L,
                    completed = null,
                )
            }
        }
}
