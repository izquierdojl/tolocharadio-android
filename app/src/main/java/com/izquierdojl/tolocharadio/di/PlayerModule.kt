package com.izquierdojl.tolocharadio.di

import android.content.Context
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.feature.player.ActiveStationHolder
import com.izquierdojl.tolocharadio.feature.player.PlaybackEngine
import com.izquierdojl.tolocharadio.feature.player.PlaybackPlayerFactory
import com.izquierdojl.tolocharadio.feature.player.PlaybackVolumeController
import com.izquierdojl.tolocharadio.feature.player.PlayerDataSourceFactory
import com.izquierdojl.tolocharadio.feature.player.StationMediaItemFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
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
    fun playerDataSource(session: SessionManager): PlayerDataSourceFactory {
        return PlayerDataSourceFactory(session, OkHttpClient())
    }

    @Provides
    @Singleton
    fun activeStationHolder(): ActiveStationHolder = ActiveStationHolder()

    @Provides
    @Singleton
    fun playbackVolumeController(engine: PlaybackEngine): PlaybackVolumeController = PlaybackVolumeController(engine)

    @Provides
    @Singleton
    fun castPlayerManager(
        @ApplicationContext context: Context,
        activeStationHolder: ActiveStationHolder,
        prefs: InstancePrefs,
        engine: PlaybackEngine,
        mediaItemFactory: StationMediaItemFactory,
        volume: PlaybackVolumeController,
    ): CastPlayerManager = CastPlayerManager(context, activeStationHolder, prefs, engine, mediaItemFactory, volume)
}
