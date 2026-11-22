# Contract: Caché de episodios y cambio de tipo de contenido

**Feature**: `0058-izquierdojl-20261122-podcast-buffer-cache`
**Date**: 2026-11-22

Contratos internos entre capas. No hay API pública nueva ni cambios de servidor.

## `PlaybackEngine` (feature/player)

```kotlin
val activeKind: ContentKind

/**
 * Garantiza que el player activo usa el LoadControl del kind.
 * Si cambia, crea el nuevo player y deja el anterior para que el
 * servicio lo libere tras actualizar la MediaSession.
 */
fun ensureKind(kind: ContentKind): ExoPlayer

/** Player anterior pendiente de liberar por el servicio (o null). */
fun takePendingRelease(): ExoPlayer?
```

Reglas:
- `ensureKind` MUST ser idempotente para el mismo kind.
- MUST NOT liberar el player anterior antes de que la sesión apunte al nuevo.
- `LIVE` MUST conservar el `BufferProfile` actual del usuario.
- `VOD` MUST usar `EpisodeBufferSpec` y no el perfil de radio.

## `ConnectivityMonitor` (core/network)

```kotlin
val isMetered: StateFlow<Boolean>
```

Reglas:
- `isMetered = false` solo si la red tiene `NET_CAPABILITY_NOT_METERED` y no
  está en roaming y el Ahorro de datos no está activo.
- MUST emitir al cambiar de red o de capacidades.

## `EpisodeCache` (interfaz en `domain/podcasts`, implementación en `data/local/podcasts`)

```kotlin
val stats: Flow<EpisodeCacheStats>

/** Factory de lectura/escritura para un episodio en streaming. */
fun factoryFor(episodeId: Long): DataSource.Factory

/** Factory sin caché (descargas locales o Cast). */
fun passthrough(): DataSource.Factory

suspend fun clear()
```

Reglas:
- La clave MUST ser `episode-<episodeId>`.
- MUST aplicar `CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR`.
- MUST NOT escribir en caché para URIs `content://` o `file://`.
- `stats.sizeBytes` MUST NOT superar el límite de la red activa.

## `EpisodePrefetch` (interfaz en `domain/podcasts`, implementación en `data/local/podcasts`)

```kotlin
val state: StateFlow<PrefetchState>

fun start(episode: PodcastEpisode, source: EpisodePlaybackSource, positionMs: Long)
fun update(positionMs: Long)
fun stop()
```

Reglas:
- MUST cancelarse al pausar, parar, cambiar de episodio o perder la red.
- En `NetworkKind.UNMETERED`, el objetivo es el episodio completo.
- En `NetworkKind.METERED`, el objetivo es `positionMs + 300_000` y el límite de
  50 MB.
- MUST NOT ejecutarse si `source.local == true`.

## Ajustes (feature/settings)

```kotlin
data class CacheSettingsUi(
    val sizeBytes: Long,
    val limitUnmeteredBytes: Long,
    val limitMeteredBytes: Long,
    val metered: Boolean,
)

fun onClearCache()
```
