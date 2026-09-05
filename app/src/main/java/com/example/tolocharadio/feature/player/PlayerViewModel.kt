package com.example.tolocharadio.feature.player

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.tolocharadio.core.network.ApiResult
import com.example.tolocharadio.core.network.userMessage
import com.example.tolocharadio.data.local.InstancePrefs
import com.example.tolocharadio.data.remote.api.streamUrl
import com.example.tolocharadio.data.remote.dto.StationDto
import com.example.tolocharadio.data.repo.PlaybackRepo
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado sellado del player (constitución IV). */
sealed interface PlayerState {
    data object Idle : PlayerState

    data class Buffering(val station: StationDto) : PlayerState

    data class Playing(val station: StationDto) : PlayerState

    data class Paused(val station: StationDto) : PlayerState

    data class Error(val station: StationDto?, val message: String) : PlayerState
}

/**
 * Mini-player persistente: precheck `status`, proxy con Bearer,
 * reintento con backoff y supervivencia a navegación/rotación (US-5).
 */
@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val playback: PlaybackRepo,
        private val prefs: InstancePrefs,
        private val dataSource: AuthDataSourceFactory,
        val exoPlayer: ExoPlayer,
    ) : ViewModel() {
        private val _state = MutableStateFlow<PlayerState>(PlayerState.Idle)
        val state: StateFlow<PlayerState> = _state.asStateFlow()

        private var retryJob: Job? = null
        private var controller: MediaController? = null

        private val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val current =
                        (_state.value as? PlayerState.Playing)?.station
                            ?: (_state.value as? PlayerState.Buffering)?.station
                            ?: (_state.value as? PlayerState.Paused)?.station
                            ?: return
                    _state.value =
                        when (playbackState) {
                            Player.STATE_READY ->
                                if (exoPlayer.playWhenReady) {
                                    PlayerState.Playing(current)
                                } else {
                                    PlayerState.Paused(current)
                                }
                            Player.STATE_BUFFERING -> PlayerState.Buffering(current)
                            Player.STATE_ENDED, Player.STATE_IDLE -> PlayerState.Idle
                            else -> _state.value
                        }
                }

                override fun onPlayerError(error: PlaybackException) {
                    val current = (_state.value as? PlayerState.Playing)?.station
                    _state.value = PlayerState.Error(current, "Se ha interrumpido la reproducción.")
                    scheduleRetry(current, attempt = 1)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    val current =
                        (_state.value as? PlayerState.Playing)?.station
                            ?: (_state.value as? PlayerState.Paused)?.station
                            ?: (_state.value as? PlayerState.Buffering)?.station
                            ?: return
                    _state.value = if (isPlaying) PlayerState.Playing(current) else PlayerState.Paused(current)
                }
            }

        init {
            exoPlayer.addListener(listener)
            connectController()
        }

        @OptIn(UnstableApi::class)
        private fun connectController() {
            val token = SessionToken(context, ComponentName(context, RadioPlaybackService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            future.addListener(
                { controller = runCatching { future.get() }.getOrNull() },
                MoreExecutors.directExecutor(),
            )
        }

        /** Reproduce tras el precheck `playable` (FR-007). */
        fun play(station: StationDto) {
            retryJob?.cancel()
            _state.value = PlayerState.Buffering(station)
            viewModelScope.launch {
                when (val r = playback.status(station.id)) {
                    is ApiResult.Ok -> {
                        if (!r.value.playable) {
                            _state.value =
                                PlayerState.Error(
                                    station,
                                    r.value.reason?.ifBlank { null } ?: "Emisora no disponible.",
                                )
                            return@launch
                        }
                        startStream(station)
                    }
                    is ApiResult.Err -> _state.value = PlayerState.Error(station, r.error.userMessage())
                }
            }
        }

        @OptIn(UnstableApi::class)
        private suspend fun startStream(station: StationDto) {
            val base = prefs.baseUrl.first()
            val item = MediaItem.fromUri(streamUrl(base, station.id))
            exoPlayer.setMediaSource(
                androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSource)
                    .createMediaSource(item),
            )
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            _state.value = PlayerState.Buffering(station)
        }

        /** Pausa o reanuda según el estado actual. */
        fun toggle() {
            when (_state.value) {
                is PlayerState.Playing -> exoPlayer.playWhenReady = false
                is PlayerState.Paused -> exoPlayer.playWhenReady = true
                else -> Unit
            }
        }

        /** Detiene y vuelve a Idle. */
        fun stop() {
            retryJob?.cancel()
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            _state.value = PlayerState.Idle
        }

        /** Reintento manual desde el estado Error. */
        fun retry() {
            val station = (_state.value as? PlayerState.Error)?.station ?: return
            play(station)
        }

        private fun scheduleRetry(
            station: StationDto?,
            attempt: Int,
        ) {
            if (station == null || attempt > MAX_RETRY) return
            retryJob?.cancel()
            retryJob =
                viewModelScope.launch {
                    delay(RETRY_BASE_MS * attempt)
                    if (_state.value is PlayerState.Error) play(station)
                }
        }

        override fun onCleared() {
            retryJob?.cancel()
            exoPlayer.removeListener(listener)
            controller?.release()
            super.onCleared()
        }

        private companion object {
            const val MAX_RETRY = 3
            const val RETRY_BASE_MS = 2_000L
        }
    }
