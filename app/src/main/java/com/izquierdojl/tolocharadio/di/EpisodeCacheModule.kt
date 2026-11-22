package com.izquierdojl.tolocharadio.di

import com.izquierdojl.tolocharadio.data.local.podcasts.CacheWriterEpisodePrefetch
import com.izquierdojl.tolocharadio.data.local.podcasts.SimpleCacheEpisodeCache
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodeCache
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodeCachePolicy
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePrefetch
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePrefetchPolicy
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Caché y precarga de episodios (spec 0058). */
@Module
@InstallIn(SingletonComponent::class)
abstract class EpisodeCacheModule {
    @Binds
    @Singleton
    abstract fun bindEpisodeCache(impl: SimpleCacheEpisodeCache): EpisodeCache

    @Binds
    @Singleton
    abstract fun bindEpisodePrefetch(impl: CacheWriterEpisodePrefetch): EpisodePrefetch
}

/** Políticas por defecto: 500/50 MB y 5 min (spec 0058, FR-008/FR-015). */
@Module
@InstallIn(SingletonComponent::class)
object EpisodeCacheProviders {
    @Provides
    @Singleton
    fun episodeCachePolicy(): EpisodeCachePolicy = EpisodeCachePolicy.DEFAULT

    @Provides
    @Singleton
    fun episodePrefetchPolicy(): EpisodePrefetchPolicy = EpisodePrefetchPolicy.DEFAULT
}
