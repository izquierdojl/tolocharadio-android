package com.izquierdojl.tolocharadio.data.local.podcasts

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.CacheSpan
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodeCache

/**
 * Evictor LRU con límite dinámico (spec 0058, R8).
 *
 * El límite se consulta en cada desalojo porque cambia al pasar de Wi-Fi a
 * datos móviles (500 MB → 50 MB). Solo se gestionan claves `episode-<id>`.
 */
@OptIn(UnstableApi::class)
class QuotaCacheEvictor(
    private val limitProvider: () -> Long,
    private val clock: () -> Long = System::currentTimeMillis,
) : CacheEvictor {
    private val entries = LinkedHashMap<String, EpisodeCacheEntry>(INITIAL_CAPACITY, LOAD_FACTOR, true)

    override fun requiresCacheSpanTouches(): Boolean = true

    override fun onCacheInitialized() = Unit

    override fun onStartFile(
        cache: Cache,
        key: String,
        position: Long,
        length: Long,
    ) {
        touch(key)
        evict(cache)
    }

    override fun onSpanAdded(
        cache: Cache,
        span: CacheSpan,
    ) {
        val key = span.key
        val id = episodeIdOf(key) ?: return
        val previous = entries[key]?.bytes ?: 0L
        entries[key] = EpisodeCacheEntry(id, previous + span.length.coerceAtLeast(0), clock())
        evict(cache)
    }

    override fun onSpanRemoved(
        cache: Cache,
        span: CacheSpan,
    ) {
        val current = entries[span.key] ?: return
        val remaining = (current.bytes - span.length.coerceAtLeast(0)).coerceAtLeast(0)
        if (remaining > 0) {
            entries[span.key] = current.copy(bytes = remaining)
        } else {
            entries.remove(span.key)
        }
    }

    override fun onSpanTouched(
        cache: Cache,
        oldSpan: CacheSpan,
        newSpan: CacheSpan,
    ) {
        touch(newSpan.key)
    }

    /** Recorta por LRU hasta que el espacio no supere el límite vigente. */
    fun evict(cache: Cache) {
        val limit = limitProvider()
        var space = cache.cacheSpace
        if (space <= limit) return
        for (key in entries.keys.toList()) {
            if (space <= limit) break
            cache.removeResource(key)
            entries.remove(key)
            space = cache.cacheSpace
        }
    }

    /** Entradas conocidas, de menos a más reciente. */
    fun entries(): List<EpisodeCacheEntry> = entries.values.toList()

    private fun touch(key: String) {
        val id = episodeIdOf(key) ?: return
        val previous = entries[key]
        entries[key] =
            if (previous == null) {
                EpisodeCacheEntry(id, bytes = 0, lastUsedAt = clock())
            } else {
                previous.copy(lastUsedAt = clock())
            }
    }

    private fun episodeIdOf(key: String): Long? {
        return key.removePrefix(EpisodeCache.KEY_PREFIX).toLongOrNull()?.takeIf { it > 0 }
    }

    private companion object {
        const val INITIAL_CAPACITY = 16
        const val LOAD_FACTOR = 0.75f
    }
}
