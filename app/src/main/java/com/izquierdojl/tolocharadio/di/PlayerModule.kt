package com.izquierdojl.tolocharadio.di

import android.content.Context
import android.media.AudioManager
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.izquierdojl.tolocharadio.BuildConfig
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.domain.playback.PlaybackFeatureFlags
import com.izquierdojl.tolocharadio.feature.player.ActiveStationHolder
import com.izquierdojl.tolocharadio.feature.player.HeadsetControlsController
import com.izquierdojl.tolocharadio.feature.player.HeadsetVolumeGestureHandler
import com.izquierdojl.tolocharadio.feature.player.PlaybackClock
import com.izquierdojl.tolocharadio.feature.player.PlaybackEngine
import com.izquierdojl.tolocharadio.feature.player.PlaybackHttpClient
import com.izquierdojl.tolocharadio.feature.player.PlaybackPlayerFactory
import com.izquierdojl.tolocharadio.feature.player.PlaybackSessionState
import com.izquierdojl.tolocharadio.feature.player.PlaybackVolumeController
import com.izquierdojl.tolocharadio.feature.player.PlayerDataSourceFactory
import com.izquierdojl.tolocharadio.feature.player.RecoveryAwarePlayer
import com.izquierdojl.tolocharadio.feature.player.StationMediaItemFactory
import com.izquierdojl.tolocharadio.feature.player.SystemPlaybackClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

/** Timeouts del datasource plano de episodios firmados (segundos). */
private const val PLAIN_CONNECT_TIMEOUT_SEC = 15L
private const val PLAIN_READ_TIMEOUT_SEC = 20L

/** ExoPlayer compartido + datasource del proxy autenticado del servidor. */
@Module
@InstallIn(SingletonComponent::class)
@Suppress("TooManyFunctions")
object PlayerModule {
    @Provides
    @Singleton
    fun playbackEngine(
        factory: PlaybackPlayerFactory,
        prefs: InstancePrefs,
    ): PlaybackEngine =
        PlaybackEngine(
            factory = factory,
            initialProfile = runBlocking { prefs.bufferProfile.first() },
        )

    @Provides
    @Singleton
    fun playbackFeatureFlags(): PlaybackFeatureFlags = PlaybackFeatureFlags(BuildConfig.LIVE_PLAYBACK_ENABLED)

    @Provides
    @Singleton
    fun playbackClock(): PlaybackClock = SystemPlaybackClock()

    @Provides
    @Singleton
    fun recoveryAwarePlayer(engine: PlaybackEngine): RecoveryAwarePlayer = RecoveryAwarePlayer(engine.player)

    @Provides
    @Singleton
    fun playerDataSource(session: SessionManager): PlayerDataSourceFactory {
        return PlayerDataSourceFactory(session, PlaybackHttpClient.create())
    }

    /**
     * Datasource plano para URLs firmadas de episodios (spec 0046):
     * la firma HMAC va en la URL y no lleva `Authorization`.
     * Enviar el Bearer del usuario a un host externo filtraría la sesión.
     *
     * `DefaultDataSource` conserva el soporte http(s) y añade `content://`
     * (descargas del sistema) y `file://` para la reproducción local.
     */
    @Provides
    @Singleton
    fun plainDataSource(
        @ApplicationContext context: Context,
    ): DataSource.Factory {
        val client =
            okhttp3.OkHttpClient
                .Builder()
                .connectTimeout(PLAIN_CONNECT_TIMEOUT_SEC, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(PLAIN_READ_TIMEOUT_SEC, java.util.concurrent.TimeUnit.SECONDS)
                .build()
        return DefaultDataSource.Factory(context, OkHttpDataSource.Factory(client))
    }

    @Provides
    @Singleton
    fun activeStationHolder(): ActiveStationHolder = ActiveStationHolder()

    @Provides
    @Singleton
    fun playbackVolumeController(engine: PlaybackEngine): PlaybackVolumeController = PlaybackVolumeController(engine)

    @Provides
    @Singleton
    fun audioManager(
        @ApplicationContext context: Context,
    ): AudioManager = context.getSystemService(AudioManager::class.java)

    @Provides
    @Singleton
    fun headsetVolumeGestureHandler(
        controller: HeadsetControlsController,
        audioManager: AudioManager,
    ): HeadsetVolumeGestureHandler =
        HeadsetVolumeGestureHandler(
            controller = controller,
            audioManager = audioManager,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        )

    @Provides
    @Singleton
    @Suppress("LongParameterList")
    fun castPlayerManager(
        @ApplicationContext context: Context,
        activeStationHolder: ActiveStationHolder,
        prefs: InstancePrefs,
        engine: PlaybackEngine,
        mediaItemFactory: StationMediaItemFactory,
        volume: PlaybackVolumeController,
        sessionState: PlaybackSessionState,
        recoveryAwarePlayer: RecoveryAwarePlayer,
    ): CastPlayerManager =
        CastPlayerManager(
            context = context,
            activeStationHolder = activeStationHolder,
            prefs = prefs,
            engine = engine,
            mediaItemFactory = mediaItemFactory,
            volume = volume,
            sessionState = sessionState,
            localSessionPlayer = recoveryAwarePlayer,
        )
}
