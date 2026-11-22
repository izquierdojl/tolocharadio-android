# Data Model: Buffer profundo y caché en disco para episodios

**Feature**: `0058-izquierdojl-20261122-podcast-buffer-cache`
**Date**: 2026-11-22

Este documento define las entidades y sus restricciones canónicas. Cualquier
tarea que implemente o valide un campo debe citar `Entidad.campo`.

## ContentKind

Tipo de contenido que posee el reproductor.

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `ContentKind` | enum | Valores exactos: `LIVE`, `VOD`. Un solo kind activo a la vez. |

**Ubicación canónica**: `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/ContentKind.kt`

## EpisodeBufferSpec

Parámetros de `DefaultLoadControl` para el reproductor VOD.

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `minBufferMs` | `Long` | `>= 120_000`; `>= bufferForPlaybackMs`; `>= bufferForPlaybackAfterRebufferMs` |
| `maxBufferMs` | `Long` | `>= minBufferMs`; valor de producto `240_000` |
| `bufferForPlaybackMs` | `Long` | `> 0`; valor de producto `2_000` |
| `bufferForPlaybackAfterRebufferMs` | `Long` | `> 0`; `<= 2_500`; valor de producto `2_500` |

**Invariante de memoria**: con bitrate 192 kbps, `maxBufferMs = 240_000` ocupa
≈ 5,6 MB, por debajo del objetivo de audio de Media3 (12,8 MB).

**Ubicación canónica**: `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/EpisodeBufferSpec.kt`

## NetworkKind

Clasificación de la red activa para la política de caché.

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `NetworkKind` | enum | Valores exactos: `UNMETERED`, `METERED`. |

Se deriva de `ConnectivityMonitor.isMetered`. Roaming y Ahorro de datos implican
`METERED`.

**Ubicación canónica**: `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodeCachePolicy.kt`

## EpisodeCachePolicy

Límites y margen de precarga.

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `limitUnmeteredBytes` | `Long` | `> 0`; `>= limitMeteredBytes`; valor de producto `524_288_000` (500 MB) |
| `limitMeteredBytes` | `Long` | `> 0`; `<= limitUnmeteredBytes`; valor de producto `52_428_800` (50 MB) |
| `prefetchAheadMeteredMs` | `Long` | `> 0`; `<= 300_000`; valor de producto `300_000` (5 min) |

**Derivaciones**:
- `limitFor(NetworkKind.UNMETERED) = limitUnmeteredBytes`
- `limitFor(NetworkKind.METERED) = limitMeteredBytes`

**Ubicación canónica**: `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodeCachePolicy.kt`

## EpisodeCacheEntry

Entrada de audio en la caché local.

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `episodeId` | `Long` | `> 0`; identidad única; nunca la URL firmada |
| `bytes` | `Long` | `>= 0`; suma de lo cacheado para ese episodio |
| `lastUsedAt` | `Long` | epoch ms; `>= 0`; se actualiza al reproducir desde caché |

**Ciclo de vida**: `ausente → escribiendo → cacheado → usado → (desalojado | borrado)`.
Al desalojar se aplica LRU por `lastUsedAt`.

**Ubicación canónica**: `app/src/main/java/com/izquierdojl/tolocharadio/data/local/podcasts/EpisodeCacheEntry.kt`

## EpisodeCacheStats

Resumen para la pantalla de ajustes.

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `sizeBytes` | `Long` | `>= 0`; `<=` límite de la red activa |
| `entryCount` | `Int` | `>= 0` |

**Ubicación canónica**: `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodeCachePolicy.kt`

## PrefetchState

Estado observable de la precarga del episodio en curso.

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `episodeId` | `Long?` | `null` si no hay precarga; `> 0` si la hay |
| `cachedBytes` | `Long` | `>= 0`; no decrece mientras la precarga está activa |
| `targetBytes` | `Long?` | `>= 0`; `null` para "episodio completo" en red no medida |
| `running` | `Boolean` | `true` solo mientras hay trabajo activo |

**Transiciones**: `idle → running → completed | cancelled | failed`.
`cancelled` al pausar, parar, cambiar de episodio o perder la red.

**Ubicación canónica**: `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePrefetchPolicy.kt`
