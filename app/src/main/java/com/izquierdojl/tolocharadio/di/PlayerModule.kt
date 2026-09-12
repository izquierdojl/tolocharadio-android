package com.izquierdojl.tolocharadio.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.izquierdojl.tolocharadio.cast.CastPlayerManager
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.remote.playlist.OkHttpPlaylistFetcher
import com.izquierdojl.tolocharadio.domain.playback.PlaylistFetcher
import com.izquierdojl.tolocharadio.feature.player.ActiveStationHolder
import com.izquierdojl.tolocharadio.feature.player.AuthDataSourceFactory
import com.izquierdojl.tolocharadio.feature.player.DirectDataSourceFactory
import com.izquierdojl.tolocharadio.feature.player.PlayerAudioConfig
import com.izquierdojl.tolocharadio.feature.player.StationMediaItemFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/** ExoPlayer compartido + datasources (proxy con Bearer y directo sin auth). */
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
    fun authDataSource(session: SessionManager): AuthDataSourceFactory = AuthDataSourceFactory(session, OkHttpClient())

    @Provides
    @Singleton
    fun directDataSource(): DirectDataSourceFactory = DirectDataSourceFactory()

    @Provides
    @Singleton
    fun playlistFetcher(fetcher: OkHttpPlaylistFetcher): PlaylistFetcher = fetcher

    @Provides
    @Singleton
    fun activeStationHolder(): ActiveStationHolder = ActiveStationHolder()

    @Provides
    @Singleton
    fun castPlayerManager(
        @ApplicationContext context: Context,
        activeStationHolder: ActiveStationHolder,
        prefs: InstancePrefs,
        exoPlayer: ExoPlayer,
        mediaItemFactory: StationMediaItemFactory,
    ): CastPlayerManager = CastPlayerManager(context, activeStationHolder, prefs, exoPlayer, mediaItemFactory)
}
