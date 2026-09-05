package com.example.tolocharadio.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.example.tolocharadio.core.session.SessionManager
import com.example.tolocharadio.feature.player.AuthDataSourceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/** ExoPlayer compartido + datasource con Bearer (FR-007). */
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    @Provides
    @Singleton
    fun exoPlayer(
        @ApplicationContext context: Context,
    ): ExoPlayer = ExoPlayer.Builder(context).build()

    @Provides
    @Singleton
    fun authDataSource(session: SessionManager): AuthDataSourceFactory = AuthDataSourceFactory(session, OkHttpClient())
}
