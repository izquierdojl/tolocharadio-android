package com.izquierdojl.tolocharadio.data.local.podcasts

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import com.izquierdojl.tolocharadio.core.network.ConnectivityMonitor
import com.izquierdojl.tolocharadio.di.ApplicationScope
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodeCache
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodeCachePolicy
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodeCacheStats
import com.izquierdojl.tolocharadio.domain.podcasts.NetworkKind
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caché de episodios sobre `SimpleCache` de Media3 (spec 0058, R4/R8).
 *
 * La creación del `SimpleCache` es E/S y se hace en segundo plano; hasta que
 * está lista, [factoryFor] degrada al datasource plano. Así nunca se bloquea el
 * hilo del player (FR-011).
 */
@Singleton
@OptIn(UnstableApi::class)
class SimpleCacheEpisodeCache
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val plainDataSource: DataSource.Factory,
        private val connectivity: ConnectivityMonitor,
        private val policy: EpisodeCachePolicy,
        @ApplicationScope private val scope: CoroutineScope,
    ) : EpisodeCache {
        private val cacheRef = AtomicReference<SimpleCache?>(null)
        private val limitBytes = AtomicLong(policy.limitFor(NetworkKind.UNMETERED))
        private val evictor = QuotaCacheEvictor(limitProvider = { limitBytes.get() })
        private val _stats = MutableStateFlow(EpisodeCacheStats.EMPTY)
        override val stats: Flow<EpisodeCacheStats> = _stats.asStateFlow()

        init {
            scope.launch(Dispatchers.IO) {
                val cache =
                    runCatching {
                        SimpleCache(
                            File(context.cacheDir, CACHE_DIR),
                            evictor,
                            StandaloneDatabaseProvider(context),
                        )
                    }.getOrNull()
                cacheRef.set(cache)
                refreshStats()
            }
            scope.launch {
                connectivity.isMetered.collect { metered ->
                    setNetworkKind(if (metered) NetworkKind.METERED else NetworkKind.UNMETERED)
                }
            }
        }

        override fun factoryFor(episodeId: Long): DataSource.Factory =
            object : DataSource.Factory {
                override fun createDataSource(): DataSource {
                    val cache = cacheRef.get() ?: return plainDataSource.createDataSource()
                    return cacheFactory(cache).createDataSource()
                }
            }

        override fun passthrough(): DataSource.Factory = plainDataSource

        override fun cacheDataSource(episodeId: Long): CacheDataSource? {
            val cache = cacheRef.get() ?: return null
            return cacheFactory(cache).createDataSource() as? CacheDataSource
        }

        private fun cacheFactory(cache: SimpleCache): CacheDataSource.Factory =
            CacheDataSource
                .Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(plainDataSource)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        override fun setNetworkKind(kind: NetworkKind) {
            limitBytes.set(policy.limitFor(kind))
            scope.launch(Dispatchers.IO) {
                cacheRef.get()?.let { evictor.evict(it) }
                refreshStats()
            }
        }

        override suspend fun clear() =
            withContext(Dispatchers.IO) {
                cacheRef.get()?.let { cache ->
                    cache.keys.toList().forEach { cache.removeResource(it) }
                }
                refreshStats()
            }

        private fun refreshStats() {
            val cache = cacheRef.get() ?: return
            _stats.value =
                EpisodeCacheStats(
                    sizeBytes = cache.cacheSpace.coerceAtLeast(0),
                    entryCount = cache.keys.size,
                )
        }

        private companion object {
            const val CACHE_DIR = "episode-cache"
        }
    }
