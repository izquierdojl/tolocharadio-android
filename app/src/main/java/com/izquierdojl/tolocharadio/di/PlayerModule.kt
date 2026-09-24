package com.izquierdojl.tolocharadio.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.feature.player.ActiveStationHolder
import com.izquierdojl.tolocharadio.feature.player.PlaybackVolumeController
import com.izquierdojl.tolocharadio.feature.player.PlayerAudioConfig
import com.izquierdojl.tolocharadio.feature.player.PlayerDataSourceFactory
import com.izquierdojl.tolocharadio.feature.player.StationMediaItemFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/** ExoPlayer compartido + datasource del proxy autenticado del servidor. */
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    @Provides
    @Singleton
    fun exoPlayer(
        @ApplicationContext context: Context,
    ): ExoPlayer = PlayerAudioConfig.applyTo(ExoPlayer.Builder(context)).build()

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
    fun playbackVolumeController(exoPlayer: ExoPlayer): PlaybackVolumeController = PlaybackVolumeController(exoPlayer)

    @Provides
    @Singleton
    fun castPlayerManager(
        @ApplicationContext context: Context,
        activeStationHolder: ActiveStationHolder,
        prefs: InstancePrefs,
        exoPlayer: ExoPlayer,
        mediaItemFactory: StationMediaItemFactory,
        volume: PlaybackVolumeController,
    ): CastPlayerManager {
        return CastPlayerManager(context, activeStationHolder, prefs, exoPlayer, mediaItemFactory, volume)
    }
}
