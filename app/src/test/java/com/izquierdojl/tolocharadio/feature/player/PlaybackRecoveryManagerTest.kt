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
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.domain.auth.AuthenticateServerUseCase
import com.izquierdojl.tolocharadio.domain.playback.InterruptionCause
import com.izquierdojl.tolocharadio.domain.playback.PlaybackFailure
import com.izquierdojl.tolocharadio.domain.playback.PlaybackFeatureFlags
import com.izquierdojl.tolocharadio.domain.playback.PlaybackRoute
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
    private val fallbackController: PlaybackFallbackController = mockk(relaxed = true)
    private val authenticate: AuthenticateServerUseCase = mockk(relaxed = true)
    private val session = SessionManager()
    private val sessionState = PlaybackSessionState(PlaybackFeatureFlags(livePlaybackEnabled = true))
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

    private fun manager(
        scope: CoroutineScope? = null,
        clock: PlaybackClock = PlaybackClock { 0L },
    ): PlaybackRecoveryManager {
        val scheduler =
            if (scope == null) {
                RecoveryScheduler()
            } else {
                RecoveryScheduler(scope, watchdogEnabled = false)
            }
        schedulerForTest = scheduler
        coEvery { fallbackController.start(any(), any(), any(), any()) } returns Result.success(Unit)
        return PlaybackRecoveryManager(
            engine = engine,
            connectivity = connectivity,
            prefs = prefs,
            activeStationHolder = holder,
            fallbackController = fallbackController,
            authenticateServer = authenticate,
            castPlayerManager = cast,
            profileController = profileController,
            logger = logger,
            scheduler = scheduler,
            flags = PlaybackFeatureFlags(livePlaybackEnabled = true),
            sessionState = sessionState,
            session = session,
            clock = clock,
        )
    }

    private fun managerForTest(scope: TestScope): PlaybackRecoveryManager {
        val testScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(scope.testScheduler))
        return manager(testScope, PlaybackClock { scope.testScheduler.currentTime })
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
            manager.start()
            advanceUntilIdle()
            assertTrue(manager.manualRetry())
            assertTrue(manager.manualRetry())
            advanceUntilIdle()
            coVerify(exactly = 1) {
                fallbackController.start(station, PlaybackRoute.CONTINUOUS, any(), recordHistory = false)
            }
            assertTrue(manager.status.value is RecoveryState.Idle)
        }

    @Test
    fun `onMediaItemTransition reinicia el watchdog`() =
        runTest {
            val manager = managerForTest(this)
            manager.start()
            advanceUntilIdle()
            val listener = captureListener()
            listener.onIsPlayingChanged(true)
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
            manager.start()
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

    @Test
    fun `415 en continua hace un unico fallback legacy`() =
        runTest {
            every { exoPlayer.isPlaying } returns true
            every { exoPlayer.playbackState } returns Player.STATE_READY
            val manager = managerForTest(this)
            manager.start()
            advanceUntilIdle()
            val listener = captureListener()
            listener.onPlayerError(httpError(415))
            advanceUntilIdle()
            coVerify(exactly = 1) {
                fallbackController.start(station, PlaybackRoute.LEGACY, any(), recordHistory = false)
            }
            assertTrue(manager.status.value is RecoveryState.Idle)
        }

    @Test
    fun `404 en continua cambia a legacy y recupera`() =
        runTest {
            every { exoPlayer.isPlaying } returns true
            every { exoPlayer.playbackState } returns Player.STATE_READY
            val manager = managerForTest(this)
            manager.start()
            advanceUntilIdle()
            val listener = captureListener()
            listener.onPlayerError(httpError(404))
            advanceUntilIdle()
            coVerify(exactly = 1) {
                fallbackController.start(station, PlaybackRoute.LEGACY, any(), recordHistory = false)
            }
            assertTrue(manager.status.value is RecoveryState.Idle)
        }

    @Test
    fun `STATE_ENDED inicia recuperacion`() =
        runTest {
            every { exoPlayer.isPlaying } returns true
            every { exoPlayer.playbackState } returns Player.STATE_READY
            val manager = managerForTest(this)
            manager.start()
            advanceUntilIdle()
            val listener = captureListener()
            listener.onIsPlayingChanged(true)
            listener.onPlaybackStateChanged(Player.STATE_ENDED)
            advanceUntilIdle()
            coVerify(exactly = 1) {
                fallbackController.start(station, PlaybackRoute.CONTINUOUS, any(), recordHistory = false)
            }
        }

    @Test
    fun `agota los seis intentos y falla`() =
        runTest {
            every { exoPlayer.isPlaying } returns false
            every { exoPlayer.playbackState } returns Player.STATE_BUFFERING
            every { exoPlayer.playWhenReady } returns true
            val manager = managerForTest(this)
            manager.start()
            advanceUntilIdle()
            val listener = captureListener()
            listener.onPlayerError(httpError(500))
            advanceUntilIdle()
            coVerify(exactly = 6) {
                fallbackController.start(station, PlaybackRoute.CONTINUOUS, any(), recordHistory = false)
            }
            assertEquals(120_000, testScheduler.currentTime)
            assertTrue(manager.status.value is RecoveryState.Failed)
        }

    @Test
    fun `el presupuesto se reinicia tras sesenta segundos estables`() =
        runTest {
            every { exoPlayer.isPlaying } returns true
            every { exoPlayer.playbackState } returns Player.STATE_READY
            val attempts = mutableListOf<Int>()
            every { logger.attempt(any<PlaybackFailure>(), any(), any()) } answers {
                attempts += invocation.args[1] as Int
            }
            val manager = managerForTest(this)
            manager.start()
            advanceUntilIdle()
            val listener = captureListener()
            listener.onIsPlayingChanged(true)

            listener.onPlayerError(httpError(500))
            runCurrent()
            listener.onPlayerError(httpError(500))
            runCurrent()
            assertEquals(listOf(1, 2), attempts)

            // El segundo intento espera 1 s de backoff antes de reconectar.
            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(2, manager.budgetSnapshot().usedAttempts)
            assertTrue(manager.budgetSnapshot().stableSince != null)

            advanceTimeBy(60_000)
            runCurrent()
            assertEquals(0, manager.budgetSnapshot().usedAttempts)

            listener.onPlayerError(httpError(500))
            runCurrent()
            assertEquals(listOf(1, 2, 1), attempts)
        }

    @Test
    fun `un segundo error durante la reconexion no lanza otra`() =
        runTest {
            every { exoPlayer.isPlaying } returns false
            every { exoPlayer.playbackState } returns Player.STATE_BUFFERING
            every { exoPlayer.playWhenReady } returns true
            val manager = managerForTest(this)
            manager.start()
            advanceUntilIdle()
            val listener = captureListener()
            listener.onPlayerError(httpError(500))
            runCurrent()
            listener.onPlayerError(httpError(500))
            runCurrent()
            advanceTimeBy(14_000)
            runCurrent()
            coVerify(exactly = 1) {
                fallbackController.start(station, PlaybackRoute.CONTINUOUS, any(), recordHistory = false)
            }
            verify(exactly = 0) { exoPlayer.volume = any() }
            manager.cancel()
        }

    @Test
    fun `cancel durante el backoff detiene la reconexion`() =
        runTest {
            every { exoPlayer.isPlaying } returns true
            every { exoPlayer.playbackState } returns Player.STATE_READY
            val manager = managerForTest(this)
            manager.start()
            advanceUntilIdle()
            val listener = captureListener()
            listener.onPlayerError(httpError(500))
            runCurrent()
            listener.onPlayerError(httpError(500))
            runCurrent()
            manager.cancel()
            advanceUntilIdle()
            coVerify(exactly = 1) {
                fallbackController.start(station, PlaybackRoute.CONTINUOUS, any(), recordHistory = false)
            }
            assertTrue(manager.status.value is RecoveryState.Idle)
        }

    @Test
    fun `un parser no soportado en continua hace un unico fallback legacy`() =
        runTest {
            every { exoPlayer.isPlaying } returns true
            every { exoPlayer.playbackState } returns Player.STATE_READY
            val manager = managerForTest(this)
            manager.start()
            advanceUntilIdle()
            val listener = captureListener()
            listener.onPlayerError(parserError())
            advanceUntilIdle()
            coVerify(exactly = 1) {
                fallbackController.start(station, PlaybackRoute.LEGACY, any(), recordHistory = false)
            }
        }

    @Test
    fun `la recuperacion usa el marcador posterior al arranque inicial`() =
        runTest {
            every { exoPlayer.isPlaying } returns true
            every { exoPlayer.playbackState } returns Player.STATE_READY
            val manager = managerForTest(this)
            manager.start()
            advanceUntilIdle()
            assertEquals(0, sessionState.nextSequence())
            val listener = captureListener()
            listener.onPlayerError(httpError(500))
            advanceUntilIdle()
            coVerify(exactly = 1) {
                fallbackController.start(station, PlaybackRoute.CONTINUOUS, 1, recordHistory = false)
            }
        }

    @Test
    fun `la perdida de foco cancela la recuperacion en vuelo`() =
        runTest {
            every { exoPlayer.isPlaying } returns false
            every { exoPlayer.playbackState } returns Player.STATE_BUFFERING
            every { exoPlayer.playWhenReady } returns true
            val manager = managerForTest(this)
            manager.start()
            advanceUntilIdle()
            val listener = captureListener()
            listener.onPlayerError(httpError(500))
            runCurrent()
            listener.onPlayWhenReadyChanged(
                false,
                Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS,
            )
            advanceTimeBy(16_000)
            runCurrent()
            coVerify(exactly = 1) {
                fallbackController.start(station, PlaybackRoute.CONTINUOUS, any(), recordHistory = false)
            }
            assertTrue(manager.status.value is RecoveryState.Idle)
        }

    @Test
    fun `Cast activo durante la recuperacion detiene los reintentos`() =
        runTest {
            every { exoPlayer.isPlaying } returns false
            every { exoPlayer.playbackState } returns Player.STATE_BUFFERING
            every { exoPlayer.playWhenReady } returns true
            val manager = managerForTest(this)
            manager.start()
            advanceUntilIdle()
            val listener = captureListener()
            listener.onPlayerError(httpError(500))
            runCurrent()
            every { cast.isCastConnected } returns true
            advanceTimeBy(16_000)
            runCurrent()
            coVerify(exactly = 1) {
                fallbackController.start(station, PlaybackRoute.CONTINUOUS, any(), recordHistory = false)
            }
            assertTrue(manager.status.value is RecoveryState.Idle)
        }

    @Test
    fun `un 401 que no puede renovar no consume presupuesto`() =
        runTest {
            coEvery { authenticate() } returns
                ApiResult.Err(DomainError.Unauthorized("expired"))
            val manager = managerForTest(this)
            manager.start()
            advanceUntilIdle()
            val listener = captureListener()
            listener.onPlayerError(httpError(401))
            advanceUntilIdle()
            verify {
                logger.failure(any<PlaybackFailure>(), 0, station.id, any())
            }
            val status = manager.status.value
            assertTrue(status is RecoveryState.Failed)
            assertEquals(
                PlayerErrorAction.EDIT_SERVER,
                (status as RecoveryState.Failed).action,
            )
        }

    @Test
    fun `un cambio de red inicia una reconexion`() =
        runTest {
            every { exoPlayer.isPlaying } returns true
            every { exoPlayer.playWhenReady } returns true
            every { exoPlayer.playbackState } returns Player.STATE_READY
            val manager = managerForTest(this)
            manager.start()
            advanceUntilIdle()
            connectivity.emitNetworkChange()
            advanceUntilIdle()
            coVerify(exactly = 1) {
                fallbackController.start(station, PlaybackRoute.CONTINUOUS, any(), recordHistory = false)
            }
        }

    @Test
    fun `renueva el token antes de reconectar si caduca`() =
        runTest {
            session.setAccess(
                "eyJhbGciOiJub25lIn0.eyJleHAiOjF9.firma",
            )
            coEvery { authenticate() } returns ApiResult.Ok(Unit)
            every { exoPlayer.isPlaying } returns true
            every { exoPlayer.playbackState } returns Player.STATE_READY
            val manager = managerForTest(this)
            manager.start()
            advanceUntilIdle()
            val listener = captureListener()
            listener.onPlayerError(httpError(500))
            advanceUntilIdle()
            coVerify(exactly = 1) { authenticate() }
        }

    private fun captureListener(): Player.Listener {
        val listenerSlot = slot<Player.Listener>()
        verify { exoPlayer.addListener(capture(listenerSlot)) }
        return listenerSlot.captured
    }

    private fun parserError(): PlaybackException =
        PlaybackException(
            "parser",
            null,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        )

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
