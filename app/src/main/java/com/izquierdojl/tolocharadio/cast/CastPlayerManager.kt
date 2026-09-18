package com.izquierdojl.tolocharadio.cast

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastReasonCodes
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.izquierdojl.tolocharadio.core.util.CastPermissions
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.dto.StationDto
import com.izquierdojl.tolocharadio.domain.playback.HlsStation
import com.izquierdojl.tolocharadio.domain.playback.PlaybackSource
import com.izquierdojl.tolocharadio.feature.player.ActiveStationHolder
import com.izquierdojl.tolocharadio.feature.player.PlaybackVolumeController
import com.izquierdojl.tolocharadio.feature.player.PlayerState
import com.izquierdojl.tolocharadio.feature.player.PlayerStateType
import com.izquierdojl.tolocharadio.feature.player.StationMediaItemFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/** Avisos de sesión Cast que la UI debe reflejar (bug 0031). */
sealed interface CastNotice {
    /** La sesión se perdió con el receptor posiblemente aún reproduciendo. */
    data class SessionLost(val deviceName: String) : CastNotice
}

@Singleton
@OptIn(UnstableApi::class)
@Suppress("LongParameterList")
class CastPlayerManager
    @Inject
    constructor(
        private val context: Context,
        private val activeStationHolder: ActiveStationHolder,
        private val prefs: InstancePrefs,
        val exoPlayer: ExoPlayer,
        private val mediaItemFactory: StationMediaItemFactory,
        private val volume: PlaybackVolumeController,
    ) {
        private val _connectionState = MutableStateFlow(CastConnectionState.DISCONNECTED)
        val connectionState: StateFlow<CastConnectionState> = _connectionState.asStateFlow()

        private val _castState = MutableStateFlow<CastPlayerState>(CastPlayerState.Local(PlayerState.Idle))
        val castState: StateFlow<CastPlayerState> = _castState.asStateFlow()

        private val _notices = MutableSharedFlow<CastNotice>(extraBufferCapacity = 1)
        val notices: SharedFlow<CastNotice> = _notices.asSharedFlow()

        /** La app pidió desconectar: implica receptor detenido de forma segura. */
        private var userDisconnectRequested = false

        /** Último nombre conocido del receptor (la sesión lo pierde al desconectar). */
        private var lastDeviceName = "Chromecast"

        private var castPlayer: CastPlayer? = null
        private var castContext: CastContext? = null
        private var sessionManagerListener: SessionManagerListener<CastSession>? = null
        private var mediaSession: androidx.media3.session.MediaSession? = null

        /** Error de reproducción remoto pendiente de mostrar (se limpia al recargar). */
        private var castError: PlayerState.Error? = null

        // FR-009: Audio focus management
        private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        private var audioFocusRequest: AudioFocusRequest? = null
        private val audioFocusChangeListener =
            AudioManager.OnAudioFocusChangeListener { focusChange ->
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
            castPlayer?.let { session.setPlayer(it) }
        }

        private fun initCastContext() {
            if (!CastPermissions.areGranted(context)) {
                android.util.Log.w(
                    "CastPlayerManager",
                    "Faltan permisos de descubrimiento Cast: el selector no mostrará dispositivos",
                )
            }
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

                override fun onSessionStarted(
                    session: CastSession,
                    sessionId: String,
                ) {
                    userDisconnectRequested = false
                    _connectionState.value = CastConnectionState.CONNECTED
                    createCastPlayer(session)
                }

                override fun onSessionStartFailed(
                    session: CastSession,
                    error: Int,
                ) {
                    userDisconnectRequested = false
                    android.util.Log.w("CastPlayerManager", "Fallo al iniciar la sesión Cast code=$error")
                    _connectionState.value = CastConnectionState.DISCONNECTED
                    releaseCastPlayer()
                }

                override fun onSessionEnding(session: CastSession) {
                    // Will be handled in onSessionEnded
                }

                override fun onSessionEnded(
                    session: CastSession,
                    error: Int,
                ) {
                    val reasonCode = castContext?.getCastReasonCodeForCastStatusCode(error)
                    val userInitiated =
                        userDisconnectRequested || reasonCode == CastReasonCodes.CASTING_STOPPED
                    val remoteWasPlaying = castPlayer?.isPlaying == true
                    userDisconnectRequested = false
                    android.util.Log.i(
                        "CastPlayerManager",
                        "Sesión Cast terminada: userInitiated=$userInitiated code=$error reason=$reasonCode",
                    )
                    handleSessionEnd(
                        userInitiated = userInitiated,
                        remoteWasPlaying = remoteWasPlaying,
                        deviceName = session.castDevice?.friendlyName ?: lastDeviceName,
                    )
                }

                override fun onSessionResuming(
                    session: CastSession,
                    sessionId: String,
                ) {
                    _connectionState.value = CastConnectionState.RECONNECTING
                }

                override fun onSessionResumed(
                    session: CastSession,
                    wasSuspended: Boolean,
                ) {
                    _connectionState.value = CastConnectionState.CONNECTED
                    createCastPlayer(session)
                }

                override fun onSessionResumeFailed(
                    session: CastSession,
                    error: Int,
                ) {
                    val reasonCode = castContext?.getCastReasonCodeForCastStatusCode(error)
                    val userInitiated =
                        userDisconnectRequested || reasonCode == CastReasonCodes.CASTING_STOPPED
                    userDisconnectRequested = false
                    android.util.Log.w(
                        "CastPlayerManager",
                        "Fallo al reanudar la sesión Cast code=$error reason=$reasonCode",
                    )
                    handleSessionEnd(
                        userInitiated = userInitiated,
                        remoteWasPlaying = castPlayer?.isPlaying == true,
                        deviceName = session.castDevice?.friendlyName ?: lastDeviceName,
                    )
                }

                override fun onSessionSuspended(
                    session: CastSession,
                    reason: Int,
                ) {
                    _connectionState.value = CastConnectionState.RECONNECTING
                }
            }

        /**
         * Crea el `CastPlayer` solo si no existe. En `onSessionResumed` NO se
         * recrea: `CastPlayer.release()` termina la sesión (`endCurrentSession`)
         * y mataría la sesión recién reanudada (bug 0031). Media3 soporta la
         * suspensión/reanudación con el mismo player.
         *
         * La sesión recibe el `CastPlayer` crudo, cuyo volumen de dispositivo es nativo
         * desde media3 1.11.0: las teclas y la barra del sistema se alimentan del
         * `MediaSession` sin wrappers ni escrituras de la app (spec 0037).
         */
        @OptIn(UnstableApi::class)
        private fun createCastPlayer(session: CastSession) {
            castError = null
            if (castPlayer == null) {
                castContext?.let { ctx ->
                    castPlayer =
                        CastPlayer(ctx).apply {
                            addListener(castPlayerListener)
                        }
                }
                castPlayer?.let { player ->
                    mediaSession?.setPlayer(player)
                }
            }
            if (!volume.isRemoteActive) {
                volume.bind(CastSessionVolumeDevice(session))
            }
            // FR-009: Request audio focus when connecting to Cast
            requestAudioFocus()
            lastDeviceName = currentDeviceName()
            updateCastState()
        }

        /**
         * Resuelve el final de una sesión Cast sin duplicar audio (bug 0031) y
         * recupera la escucha tras una parada manual (bug 0032): solo reanuda
         * local si la app/usuario detuvieron el receptor; ante una pérdida no
         * intencionada avisa, porque el receptor puede seguir sonando.
         */
        private fun handleSessionEnd(
            userInitiated: Boolean,
            remoteWasPlaying: Boolean,
            deviceName: String,
        ) {
            _connectionState.value = CastConnectionState.DISCONNECTED
            releaseCastPlayer()
            when (CastFallbackPolicy.decide(userInitiated, remoteWasPlaying)) {
                CastFallbackDecision.RESUME_LOCAL -> resumeLocalPlayback(play = true)
                CastFallbackDecision.RESUME_LOCAL_PAUSED -> resumeLocalPlayback(play = false)
                CastFallbackDecision.DO_NOT_RESUME_LOCAL ->
                    _notices.tryEmit(CastNotice.SessionLost(deviceName))
            }
        }

        private fun releaseCastPlayer() {
            volume.unbind()
            castPlayer?.removeListener(castPlayerListener)
            castPlayer?.release()
            castPlayer = null
            castError = null
            // FR-009: Abandon audio focus when disconnecting from Cast
            abandonAudioFocus()
            // Restore MediaSession to ExoPlayer
            mediaSession?.setPlayer(exoPlayer)
            updateCastState()
        }

        // FR-009: Request audio focus for Cast playback
        private fun requestAudioFocus() {
            val attrs =
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            val request =
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
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

        // FR-008/bug 0031: reanuda en local solo con el receptor ya detenido
        private fun resumeLocalPlayback(play: Boolean = true) {
            val station = activeStationHolder.station ?: return
            val holderState = activeStationHolder.playerState
            if (holderState == PlayerStateType.PLAYING || holderState == PlayerStateType.BUFFERING) {
                val baseUrl = runBlocking { prefs.baseUrl.first() }
                val source = PlaybackSource(station.id, HlsStation.isHls(station.url))
                val item = mediaItemFactory.create(station, source, baseUrl)
                exoPlayer.setMediaSource(mediaItemFactory.createMediaSource(item, source))
                exoPlayer.prepare()
                exoPlayer.playWhenReady = play
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

                override fun onPlayerError(error: PlaybackException) {
                    val station = activeStationHolder.station ?: return
                    android.util.Log.w("CastPlayerManager", "Error de reproducción Cast code=${error.errorCode}", error)
                    castError = PlayerState.Error(station, "No se pudo reproducir en el dispositivo.")
                    updateCastState()
                }
            }

        private fun updateCastState() {
            val current = activeStationHolder.station ?: return
            _castState.value =
                if (isCastConnected) {
                    CastPlayerState.Cast(
                        castError ?: castPlaybackState(current),
                        currentDeviceName(),
                        _connectionState.value,
                    )
                } else {
                    CastPlayerState.Local(localPlaybackState(current))
                }
        }

        /** Estado real del reproductor remoto (`CastPlayer`), no del holder local. */
        private fun castPlaybackState(station: StationDto): PlayerState =
            when (castPlayer?.playbackState) {
                Player.STATE_READY ->
                    if (castPlayer?.isPlaying == true) PlayerState.Playing(station) else PlayerState.Paused(station)
                Player.STATE_BUFFERING -> PlayerState.Buffering(station)
                Player.STATE_ENDED, Player.STATE_IDLE -> PlayerState.Idle
                else -> PlayerState.Buffering(station)
            }

        /** Estado del reproductor local (se refleja en el holder). */
        private fun localPlaybackState(station: StationDto): PlayerState =
            when (activeStationHolder.playerState) {
                PlayerStateType.PLAYING -> PlayerState.Playing(station)
                PlayerStateType.PAUSED -> PlayerState.Paused(station)
                PlayerStateType.BUFFERING -> PlayerState.Buffering(station)
                PlayerStateType.ERROR -> PlayerState.Error(station, "Se ha interrumpido la reproducción.")
                PlayerStateType.IDLE -> PlayerState.Idle
            }

        private fun currentDeviceName(): String {
            val name = castContext?.sessionManager?.currentCastSession?.castDevice?.friendlyName
            return name ?: "Chromecast"
        }

        @OptIn(UnstableApi::class)
        fun connectToStation(
            station: StationDto,
            source: PlaybackSource,
        ) {
            if (!isCastConnected) return
            val player = castPlayer ?: return

            castError = null
            val baseUrl = runBlocking { prefs.baseUrl.first() }
            val item = mediaItemFactory.createForCast(station, source, baseUrl)
            player.setMediaItem(item)
            player.prepare()
            player.playWhenReady = true
        }

        /**
         * Desconexión manual pedida por la app: cierra la sesión con
         * `stopCasting=true` para detener el receptor. `onSessionEnded` resuelve
         * la reanudación local de forma segura (bug 0031).
         */
        fun disconnect() {
            userDisconnectRequested = true
            castContext?.sessionManager?.endCurrentSession(true)
        }

        fun updateLocalState(state: PlayerState) {
            _castState.value = CastPlayerState.Local(state)
        }

        fun release() {
            sessionManagerListener?.let { listener ->
                castContext?.sessionManager?.removeSessionManagerListener(listener, CastSession::class.java)
            }
            releaseCastPlayer()
            castContext = null
        }
    }
