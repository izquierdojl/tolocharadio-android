package com.izquierdojl.tolocharadio.cast

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.cast.CastPlayer
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.api.streamUrl
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.feature.player.ActiveStationHolder
import com.izquierdojl.tolocharadio.feature.player.AuthDataSourceFactory
import com.izquierdojl.tolocharadio.feature.player.PlayerState
import com.izquierdojl.tolocharadio.feature.player.PlayerStateType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CastPlayerManager
    @Inject
    constructor(
        private val context: Context,
        private val activeStationHolder: ActiveStationHolder,
        private val prefs: InstancePrefs,
        val exoPlayer: ExoPlayer,
    ) {
        private val _connectionState = MutableStateFlow(CastConnectionState.DISCONNECTED)
        val connectionState: StateFlow<CastConnectionState> = _connectionState.asStateFlow()

        private val _castState = MutableStateFlow<CastPlayerState>(CastPlayerState.Local(PlayerState.Idle))
        val castState: StateFlow<CastPlayerState> = _castState.asStateFlow()

        private var castPlayer: CastPlayer? = null
        private var castContext: CastContext? = null
        private var sessionManagerListener: SessionManagerListener<CastSession>? = null
        private var mediaSession: androidx.media3.session.MediaSession? = null

        // FR-009: Audio focus management
        private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        private var audioFocusRequest: AudioFocusRequest? = null
        private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    castPlayer?.playWhenReady = false
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    castPlayer?.playWhenReady = false
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    castPlayer?.playWhenReady = true
                }
            }
        }

        val isCastConnected: Boolean
            get() = _connectionState.value == CastConnectionState.CONNECTED

        val activePlayer: Player
            get() = if (isCastConnected) castPlayer ?: exoPlayer else exoPlayer

        init {
            initCastContext()
        }

        fun setMediaSession(session: androidx.media3.session.MediaSession) {
            this.mediaSession = session
        }

        private fun initCastContext() {
            runCatching {
                castContext = CastContext.getSharedInstance(context)
                sessionManagerListener = createSessionManagerListener()
                castContext?.sessionManager?.addSessionManagerListener(
                    sessionManagerListener!!,
                    CastSession::class.java,
                )
            }.onFailure { e ->
                android.util.Log.e("CastPlayerManager", "Failed to initialize CastContext", e)
            }
        }

        private fun createSessionManagerListener() =
            object : SessionManagerListener<CastSession> {
                override fun onSessionStarting(session: CastSession) {
                    _connectionState.value = CastConnectionState.CONNECTING
                }

                override fun onSessionStarted(session: CastSession, sessionId: String) {
                    _connectionState.value = CastConnectionState.CONNECTED
                    createCastPlayer(session)
                }

                override fun onSessionStartFailed(session: CastSession, error: Int) {
                    _connectionState.value = CastConnectionState.DISCONNECTED
                    castPlayer = null
                }

                override fun onSessionEnding(session: CastSession) {
                    // Will be handled in onSessionEnded
                }

                override fun onSessionEnded(session: CastSession, error: Int) {
                    _connectionState.value = CastConnectionState.DISCONNECTED
                    releaseCastPlayer()
                    // FR-008: Auto-resume local playback on unexpected disconnect
                    resumeLocalPlayback()
                }

                override fun onSessionResuming(session: CastSession, sessionId: String) {
                    _connectionState.value = CastConnectionState.RECONNECTING
                }

                override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                    _connectionState.value = CastConnectionState.CONNECTED
                    createCastPlayer(session)
                }

                override fun onSessionResumeFailed(session: CastSession, error: Int) {
                    _connectionState.value = CastConnectionState.DISCONNECTED
                    releaseCastPlayer()
                    // FR-008: Auto-resume local playback on resume failure
                    resumeLocalPlayback()
                }

                override fun onSessionSuspended(session: CastSession, reason: Int) {
                    _connectionState.value = CastConnectionState.RECONNECTING
                }
            }

        @OptIn(UnstableApi::class)
        private fun createCastPlayer(session: CastSession) {
            castPlayer?.release()
            castContext?.let { ctx ->
                castPlayer = CastPlayer(ctx).apply {
                    addListener(castPlayerListener)
                }
            }
            // FR-009: Request audio focus when connecting to Cast
            requestAudioFocus()
            // Update MediaSession to use CastPlayer
            castPlayer?.let { player ->
                mediaSession?.setPlayer(player)
            }
            updateCastState()
        }

        private fun releaseCastPlayer() {
            castPlayer?.removeListener(castPlayerListener)
            castPlayer?.release()
            castPlayer = null
            // FR-009: Abandon audio focus when disconnecting from Cast
            abandonAudioFocus()
            // Restore MediaSession to ExoPlayer
            mediaSession?.setPlayer(exoPlayer)
            updateCastState()
        }

        // FR-009: Request audio focus for Cast playback
        private fun requestAudioFocus() {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .setAcceptsDelayedFocusGain(true)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        }

        // FR-009: Abandon audio focus
        private fun abandonAudioFocus() {
            audioFocusRequest?.let { request ->
                audioManager.abandonAudioFocusRequest(request)
            }
            audioFocusRequest = null
        }

        // FR-008: Resume local playback after Cast disconnect
        private fun resumeLocalPlayback() {
            val station = activeStationHolder.station ?: return
            val holderState = activeStationHolder.playerState
            if (holderState == PlayerStateType.PLAYING || holderState == PlayerStateType.BUFFERING) {
                val baseUrl = runBlocking { prefs.baseUrl.first() }
                val streamUrl = streamUrl(baseUrl, station.id)
                val metadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist("Tolocha Radio")
                    .setArtworkUri(station.favicon?.let { android.net.Uri.parse(it) })
                    .setAlbumTitle("Tolocha Radio")
                    .build()
                val item = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaMetadata(metadata)
                    .build()
                val dataSource = AuthDataSourceFactory(
                    com.izquierdojl.tolocharadio.core.session.SessionManager(
                        com.izquierdojl.tolocharadio.core.session.TokenStore(context),
                    ),
                    OkHttpClient(),
                )
                exoPlayer.setMediaSource(
                    androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSource)
                        .createMediaSource(item),
                )
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        }

        private val castPlayerListener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    updateCastState()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updateCastState()
                }
            }

        private fun updateCastState() {
            val current = activeStationHolder.station ?: return
            val holderState = activeStationHolder.playerState
            val playerState =
                when (holderState) {
                    PlayerStateType.PLAYING -> PlayerState.Playing(current)
                    PlayerStateType.PAUSED -> PlayerState.Paused(current)
                    PlayerStateType.BUFFERING -> PlayerState.Buffering(current)
                    PlayerStateType.ERROR -> PlayerState.Error(current, "Se ha interrumpido la reproducción.")
                    PlayerStateType.IDLE -> PlayerState.Idle
                }
            _castState.value =
                if (isCastConnected) {
                    val deviceName = castContext?.sessionManager?.currentCastSession?.castDevice?.friendlyName ?: "Chromecast"
                    CastPlayerState.Cast(playerState, deviceName, _connectionState.value)
                } else {
                    CastPlayerState.Local(playerState)
                }
        }

        @OptIn(UnstableApi::class)
        fun connectToStation(station: StationDto) {
            if (!isCastConnected) return
            val player = castPlayer ?: return

            val baseUrl = runBlocking { prefs.baseUrl.first() }
            val streamUrl = streamUrl(baseUrl, station.id)
            val mediaItem =
                MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(station.name)
                            .setArtist("Tolocha Radio")
                            .setArtworkUri(station.favicon?.let { android.net.Uri.parse(it) })
                            .setAlbumTitle("Tolocha Radio")
                            .build(),
                    )
                    .build()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        }

        fun disconnect() {
            castPlayer?.stop()
            castPlayer?.clearMediaItems()
            releaseCastPlayer()
            _connectionState.value = CastConnectionState.DISCONNECTED
            _castState.value = CastPlayerState.Local(PlayerState.Idle)
            // FR-008: Resume local playback on manual disconnect
            resumeLocalPlayback()
        }

        fun updateLocalState(state: PlayerState) {
            _castState.value = CastPlayerState.Local(state)
        }

        fun updateCastPlayerState(state: PlayerState) {
            if (isCastConnected) {
                val deviceName = castContext?.sessionManager?.currentCastSession?.castDevice?.friendlyName ?: "Chromecast"
                _castState.value = CastPlayerState.Cast(state, deviceName, _connectionState.value)
            }
        }

        fun release() {
            sessionManagerListener?.let { listener ->
                castContext?.sessionManager?.removeSessionManagerListener(listener, CastSession::class.java)
            }
            releaseCastPlayer()
            castContext = null
        }
    }
