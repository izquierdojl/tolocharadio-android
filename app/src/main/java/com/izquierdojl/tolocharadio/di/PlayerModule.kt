package com.izquierdojl.tolocharadio.di

import android.content.Context
import com.izquierdojl.tolocharadio.BuildConfig
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.domain.playback.PlaybackFeatureFlags
import com.izquierdojl.tolocharadio.feature.player.ActiveStationHolder
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

/** ExoPlayer compartido + datasource del proxy autenticado del servidor. */
@Module
@InstallIn(SingletonComponent::class)
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

    @Provides
    @Singleton
    fun activeStationHolder(): ActiveStationHolder = ActiveStationHolder()

    @Provides
    @Singleton
    fun playbackVolumeController(engine: PlaybackEngine): PlaybackVolumeController = PlaybackVolumeController(engine)

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
