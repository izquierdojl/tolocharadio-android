# Data Model: Reproducción unificada por el proxy autenticado

**Feature**: `0021-jlizquierdo-20260912-proxy-only-playback` | **Date**: 2026-09-12

## Resumen

Este cambio no introduce persistencia. El modelo se **simplifica**: la fuente de reproducción deja de tener variantes directas y pasa a ser siempre proxy; desaparecen las entidades de resolución de listas en cliente (formato M3U/PLS, candidatos, cola de fallback, errores de red de lista).

## Entidades de dominio (Kotlin puro)

### `PlaybackSource` (modificada)

| Campo | Tipo | Reglas |
|-------|------|--------|
| `stationId` | `String` | Identificador de la emisora; la URI se construye con `streamUrl(baseUrl, stationId)` en presentación |
| `hls` | `Boolean` | `true` cuando el enlace de la emisora termina en `.m3u8`; selecciona `HlsMediaSource` + `mimeType` HLS |

- Invariante: **no existe reproducción fuera del proxy**. Se retiran `DirectProgressive` y `DirectHls`.
- Estado: único tipo `Proxied(stationId, hls)`.

### `HlsStation` (nueva, sustituye a `PlaylistFormat`)

- `fun isHls(url: String): Boolean` — `true` si el path de `url` (sin query ni fragment, case-insensitive) termina en `.m3u8`; URL en blanco o inválida → `false`.
- Pureza: JVM, sin Android.
- Reglas heredadas de 0019 §1 para HLS: se usa `java.net.URI` sobre la URL sin query.

### `ResolvePlaybackSourceUseCase` (simplificada)

- Entrada: `StationDto`.
- Salida: `PlaybackSource` (nunca falla; sin `ResolutionResult` ni `Unavailable`).
- Regla: `Proxied(stationId = station.id, hls = HlsStation.isHls(station.url))`.
- Sin dependencias (no `PlaylistFetcher`); determinista y O(1).

### `PlaybackStatusReason` (nueva)

- Entrada: `reason: String?` (código del servicio) y/o código de error tipado.
- Salida: `String` (mensaje accionable en español).
- Tabla de mapeo:

| Código | Mensaje |
|--------|---------|
| `PLAYLIST_EMPTY` | La lista de la emisora no contiene ninguna emisión reproducible. |
| `PLAYLIST_INSECURE_ONLY` | La emisora solo ofrece conexiones no seguras (HTTP), bloqueadas por la app. |
| `PLAYLIST_MALFORMED` | La lista de la emisora no tiene un formato válido. |
| `PLAYLIST_UNREACHABLE` | No se pudo descargar la lista de la emisora. Comprueba tu conexión. |
| `STREAM_UNAVAILABLE` | La emisora no está disponible en este momento. |
| otro / `null` | Emisora no disponible. |

- Todos los mensajes conservan la opción de reintento en la UI.

## Entidades de presentación (modificadas)

### `PlayerState` (sin cambios estructurales)

- `Idle`, `Buffering(station)`, `Playing(station)`, `Paused(station)`, `Error(station?, message)`.
- El preestado de disponibilidad (precheck) es bloqueante para **todos** los tipos: si `playable == false`, se emite `Error` con `PlaybackStatusReason` y no se arranca el reproductor.
- Se elimina el estado de fallback por candidatos (la cola lo gestiona el servicio).

### `ActiveStationHolder` (simplificada)

- Se mantienen `station` y `playerState`.
- Se **eliminan** `resolvedSource` y `updateResolvedSource` (la fuente ya no se resuelve por red; se recalcula desde `station` cuando hace falta).

### `StationMediaItemFactory` (modificada)

- `uriFor(source, baseUrl)` → siempre `streamUrl(baseUrl, source.stationId)`.
- `mimeTypeFor(source)` → `MimeTypes.APPLICATION_M3U8` si `source.hls`, si no `null`.
- `createMediaSource(item, source)` → `HlsMediaSource.Factory(authDataSource)` si `source.hls`, si no `ProgressiveMediaSource.Factory(authDataSource)`.
- `DirectDataSourceFactory` se elimina.

## Entidades retiradas

| Entidad | Motivo |
|---------|--------|
| `PlaylistFormat` (HLS/M3U/PLS) | Solo se conserva la detección HLS (`HlsStation.isHls`) |
| `PlaylistParser`, `PlaylistParseResult` | La resolución de listas es server-side |
| `PlaylistFetcher`, `PlaylistContent`, `PlaylistFetchException`, `OkHttpPlaylistFetcher` | El cliente ya no descarga listas |
| `PlaybackError` | Los errores llegan tipados del servicio |
| `PlaylistPlaybackQueue` | El fallback entre entradas lo hace el servicio |
| `DirectDataSourceFactory` | No hay fuentes directas |

## Transiciones de estado (reproducción)

1. `play(station)` → `Buffering(station)`.
2. Preestado de disponibilidad (precheck) `playback/:id/status`:
   - `playable == true` → `startSource(station, Proxied(id, hls))` (proxy, con Bearer).
   - `playable == false` → `Error(station, PlaybackStatusReason(reason))`.
   - error de red/API → `Error(station, userMessage)`.
3. Media3 reproduce: HLS sigue el manifiesto reescrito y los subrecursos `/hls` (todos con Bearer); el servicio registra historial en el stream principal.
4. `onPlayerError` → `Error(station, mensaje genérico accionable)` (sin fallback de candidatos).
