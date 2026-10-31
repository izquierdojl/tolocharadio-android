package com.izquierdojl.tolocharadio.feature.player

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.core.network.FakeConnectivityMonitor
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.domain.auth.AuthenticateServerUseCase
import com.izquierdojl.tolocharadio.domain.playback.InterruptionCause
import com.izquierdojl.tolocharadio.domain.playback.PlayerErrorAction
import com.izquierdojl.tolocharadio.domain.playback.RecoveryState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackRecoveryManagerTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val exoPlayer: ExoPlayer = mockk(relaxed = true)
    private val engine: PlaybackEngine =
        mockk {
            every { player } returns exoPlayer
            every { playerFlow } returns MutableStateFlow(exoPlayer)
        }
    private val connectivity = FakeConnectivityMonitor(initialOnline = true)
    private val prefs: InstancePrefs =
        mockk(relaxed = true) {
            every { resumeOnConnectivity } returns flowOf(true)
        }
    private val holder = ActiveStationHolder()
    private val starter: LocalPlaybackStarter = mockk(relaxed = true)
    private val authenticate: AuthenticateServerUseCase = mockk(relaxed = true)
    private val cast: CastPlayerManager =
        mockk(relaxed = true) {
            every { isCastConnected } returns false
        }
    private val profileController: PlaybackProfileController =
        mockk(relaxed = true) {
            every { applying } returns MutableStateFlow(false)
        }
    private val logger: PlaybackRecoveryLogger = mockk(relaxed = true)
    private val station = StationDto(id = "u1", name = "Tolocha")
    private lateinit var schedulerForTest: RecoveryScheduler

    private fun manager(scope: CoroutineScope? = null): PlaybackRecoveryManager {
        val scheduler =
            if (scope == null) {
                RecoveryScheduler()
            } else {
                RecoveryScheduler(scope, watchdogEnabled = false)
            }
        schedulerForTest = scheduler
        return PlaybackRecoveryManager(
            engine = engine,
            connectivity = connectivity,
            prefs = prefs,
            activeStationHolder = holder,
            localPlaybackStarter = starter,
            authenticateServer = authenticate,
            castPlayerManager = cast,
            profileController = profileController,
            logger = logger,
            scheduler = scheduler,
        )
    }

    private fun managerForTest(scope: TestScope): PlaybackRecoveryManager {
        val testScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(scope.testScheduler))
        return manager(testScope)
    }

    @Before
    fun setup() {
        holder.update(station, PlayerStateType.BUFFERING)
    }

    @After
    fun tearDown() {
        if (::schedulerForTest.isInitialized) {
            schedulerForTest.cancel()
        }
    }

    @Test
    fun `manualRetry sin emisora devuelve false`() {
        holder.clear()
        assertFalse(manager().manualRetry())
    }

    @Test
    fun `manualRetry con Cast devuelve false`() {
        every { cast.isCastConnected } returns true
        assertFalse(manager().manualRetry())
    }

    @Test
    fun `causeForManualRetry conserva la causa de WaitingForNetwork`() {
        val manager = manager()
        assertEquals(
            InterruptionCause.AUTH_ERROR,
            manager.causeForManualRetry(
                RecoveryState.WaitingForNetwork("u1", InterruptionCause.AUTH_ERROR),
            ),
        )
        assertEquals(
            InterruptionCause.STATION_UNAVAILABLE,
            manager.causeForManualRetry(
                RecoveryState.WaitingForNetwork("u1", InterruptionCause.STATION_UNAVAILABLE),
            ),
        )
    }

    @Test
    fun `manualRetry con red reconecta una sola vez`() =
        runTest {
            every { exoPlayer.isPlaying } returns true
            every { exoPlayer.playbackState } returns Player.STATE_READY
            val manager = managerForTest(this)
            advanceUntilIdle()
            assertTrue(manager.manualRetry())
            assertTrue(manager.manualRetry())
            advanceUntilIdle()
            coVerify(exactly = 1) { starter.start(station, false, true) }
            assertTrue(manager.status.value is RecoveryState.Idle)
        }

    @Test
    fun `onMediaItemTransition reinicia el watchdog`() =
        runTest {
            val manager = managerForTest(this)
            advanceUntilIdle()
            val listener = captureListener()
            listener.onPlaybackStateChanged(Player.STATE_READY)
            assertTrue(manager.watchdogSnapshot().hasPlayedOnce)
            listener.onMediaItemTransition(null, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)
            assertFalse(manager.watchdogSnapshot().hasPlayedOnce)
        }

    @Test
    fun `cancel deja el manager inactivo`() {
        val manager = manager()
        manager.cancel()
        assertTrue(manager.status.value is RecoveryState.Idle)
    }

    @Test
    fun `manualRetry sin red deja WaitingForNetwork sin reintentar`() =
        runTest {
            connectivity.setOnline(false)
            val manager = managerForTest(this)
            assertTrue(manager.manualRetry())
            val status = manager.status.value
            assertTrue(status is RecoveryState.WaitingForNetwork)
            assertEquals(
                InterruptionCause.NO_CONNECTIVITY,
                (status as RecoveryState.WaitingForNetwork).cause,
            )
        }

    @Test
    fun `un 401 no recuperable expone EDIT_SERVER`() =
        runTest {
            coEvery { authenticate() } returns
                ApiResult.Err(DomainError.Unauthorized("expired"))
            val manager = managerForTest(this)
            advanceUntilIdle()
            val listener = captureListener()
            listener.onPlayerError(httpError(401))
            advanceUntilIdle()
            val status = manager.status.value
            assertTrue(status is RecoveryState.Failed)
            assertEquals(
                PlayerErrorAction.EDIT_SERVER,
                (status as RecoveryState.Failed).action,
            )
        }

    private fun captureListener(): Player.Listener {
        val listenerSlot = slot<Player.Listener>()
        verify { exoPlayer.addListener(capture(listenerSlot)) }
        return listenerSlot.captured
    }

    private fun httpError(code: Int): PlaybackException =
        PlaybackException(
            "http",
            HttpDataSource.InvalidResponseCodeException(
                code,
                "error",
                null,
                emptyMap(),
                mockk<DataSpec>(relaxed = true),
                ByteArray(0),
            ),
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        )
}
