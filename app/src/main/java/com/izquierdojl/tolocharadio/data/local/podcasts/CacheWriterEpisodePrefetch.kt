package com.izquierdojl.tolocharadio.data.local.podcasts

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import com.izquierdojl.tolocharadio.core.network.ConnectivityMonitor
import com.izquierdojl.tolocharadio.di.ApplicationScope
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodeCache
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePlaybackSource
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePrefetch
import com.izquierdojl.tolocharadio.domain.podcasts.EpisodePrefetchPolicy
import com.izquierdojl.tolocharadio.domain.podcasts.NetworkKind
import com.izquierdojl.tolocharadio.domain.podcasts.PrefetchState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InterruptedIOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Precarga con `CacheWriter` de Media3 (spec 0058, R6).
 *
 * En red no medida descarga el episodio completo; en medida mantiene ~5 min
 * por delante del punto de reproducción. Se cancela al pausar, parar o cambiar
 * de red. No usa WorkManager: solo corre mientras el servicio está vivo.
 */
@Singleton
@OptIn(UnstableApi::class)
class CacheWriterEpisodePrefetch
    @Inject
    constructor(
        @ApplicationScope private val scope: CoroutineScope,
        private val connectivity: ConnectivityMonitor,
        private val cache: EpisodeCache,
        private val policy: EpisodePrefetchPolicy,
    ) : EpisodePrefetch {
        private val _state = MutableStateFlow(PrefetchState.IDLE)
        override val state: StateFlow<PrefetchState> = _state.asStateFlow()

        private var job: Job? = null
        private var writer: CacheWriter? = null
        private var source: EpisodePlaybackSource? = null
        private var episodeId: Long = 0
        private var currentPositionMs: Long = 0
        private var windowStartMs: Long = 0

        init {
            scope.launch {
                connectivity.isMetered.collect {
                    if (_state.value.running) restart()
                }
            }
        }

        override fun start(
            source: EpisodePlaybackSource,
            episodeId: Long,
            positionMs: Long,
        ) {
            this.source = source
            this.episodeId = episodeId
            this.currentPositionMs = positionMs
            this.windowStartMs = positionMs
            if (source.local) {
                stop()
                return
            }
            launchWriter(source)
        }

        override fun update(positionMs: Long) {
            currentPositionMs = positionMs
            if (!_state.value.running) return
            if (kind() == NetworkKind.UNMETERED) return
            if (positionMs - windowStartMs >= WINDOW_STEP_MS) {
                windowStartMs = positionMs
                source?.let(::launchWriter)
            }
        }

        override fun stop() {
            writer?.cancel()
            writer = null
            job?.cancel()
            job = null
            _state.value = PrefetchState.IDLE
        }

        private fun restart() {
            val source = source ?: return
            windowStartMs = currentPositionMs
            launchWriter(source)
        }

        private fun launchWriter(source: EpisodePlaybackSource) {
            writer?.cancel()
            writer = null
            job?.cancel()
            val target = policy.targetBytes(kind())
            val startAt = windowStartMs
            _state.value =
                PrefetchState(
                    episodeId = episodeId,
                    targetBytes = target,
                    running = true,
                )
            job =
                scope.launch(Dispatchers.IO) {
                    val context = coroutineContext
                    val dataSource = cache.cacheDataSource(episodeId)
                    if (dataSource == null) {
                        _state.value = _state.value.copy(running = false)
                        return@launch
                    }
                    val spec =
                        DataSpec
                            .Builder()
                            .setUri(source.uri)
                            .setPosition(startAt)
                            .setLength(target ?: C.LENGTH_UNSET.toLong())
                            .setKey(EpisodeCache.cacheKey(episodeId))
                            .build()
                    val cacheWriter =
                        CacheWriter(dataSource, spec, ByteArray(BUFFER_BYTES)) { _, cached, _ ->
                            if (context[Job]?.isActive != true) throw CancellationException()
                            _state.value = _state.value.copy(cachedBytes = cached)
                        }
                    writer = cacheWriter
                    try {
                        cacheWriter.cache()
                        _state.value = _state.value.copy(running = false)
                    } catch (cancelled: CancellationException) {
                        _state.value = PrefetchState.IDLE
                        throw cancelled
                    } catch (interrupted: InterruptedIOException) {
                        Log.i(TAG, "cancelled cause=${interrupted.javaClass.simpleName}")
                        _state.value = PrefetchState.IDLE
                    } catch (error: IOException) {
                        Log.w(TAG, "failed cause=${error.javaClass.simpleName}")
                        _state.value = _state.value.copy(running = false)
                    }
                }
        }

        private fun kind(): NetworkKind {
            return if (connectivity.isMetered.value) NetworkKind.METERED else NetworkKind.UNMETERED
        }

        private companion object {
            const val TAG = "EpisodePrefetch"

            /** Reinicia el margen en móvil cada minuto de avance. */
            const val WINDOW_STEP_MS = 60_000L

            const val BUFFER_BYTES = 64 * 1024
        }
    }
