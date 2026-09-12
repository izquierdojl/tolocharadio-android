# Data Model: Reproducción exclusiva y soporte de listas m3u/m3u8/pls

**Feature**: `0019-jlizquierdo-20260911-audio-focus-playlists` | **Date**: 2026-09-11

**Sin persistencia**: esta feature no añade tablas Room, DataStore ni migraciones. Todos los modelos son de dominio/memoria. El único estado en memoria ampliado es `ActiveStationHolder` (singleton existente, sobrevive a recreaciones de Activity/ViewModel, no al proceso).

## Modelos nuevos

### `PlaylistFormat` (enum, `domain/playback`)

Formato deducido de la URL de la emisora.

| Valor | Extensión de `station.url` (sin query) | Tratamiento |
|-------|-----------------------------------------|-------------|
| `HLS` | `.m3u8` (case-insensitive) | `DirectHls` (Media3 HLS) |
| `M3U` | `.m3u` | Fetch + parseo de lista de texto |
| `PLS` | `.pls` | Fetch + parseo de lista de texto |

- **Detección**: `PlaylistFormat.detect(url): PlaylistFormat?` con `java.net.URI` (path sin query). `null` = stream directo (pipeline proxy actual).
- **Invariantes**: URL vacía, sin path o malformada → `null`; nunca lanza.

### `PlaybackSource` (sellado, `domain/playback`)

Fuente de reproducción ya decidida, agnóstica de plataforma.

| Variante | Campos | Uso |
|----------|--------|-----|
| `Proxied` | `stationId: String` | Emisora normal: URI `/api/v1/playback/:id` + Bearer (sin cambios) |
| `DirectProgressive` | `url: String` (https) | Entrada de lista que no es HLS |
| `DirectHls` | `url: String` (https) | `.m3u8` o entrada de lista HLS |

- **Invariantes**: `Direct*` solo con esquema `https`; `Proxied` siempre construye la URI con `streamUrl(baseUrl, stationId)` en la capa de presentación (el modelo no conoce `baseUrl`).

### `PlaybackError` (sellado, `domain/playback`)

Motivo tipado de indisponibilidad de una emisora de lista (FR-009/FR-013).

| Valor | Significado | Mensaje de UI (español) |
|-------|-------------|--------------------------|
| `NETWORK` | No se pudo descargar la lista | "No se pudo descargar la lista. Comprueba tu conexión." |
| `NO_ENTRIES` | Lista vacía o sin entradas utilizables | "La lista de la emisora no contiene ninguna emisión reproducible." |
| `INSECURE_ONLY` | Solo entradas no-HTTPS | "La emisora solo ofrece conexiones no seguras (HTTP), bloqueadas por la app." |
| `MALFORMED` | Texto no reconocible como m3u/pls | "La lista de la emisora no tiene un formato válido." |

### `ResolutionResult` (sellado, `domain/playback`)

Resultado de `ResolvePlaybackSourceUseCase`.

| Variante | Campos | Semántica |
|----------|--------|-----------|
| `Proxied` | `source: PlaybackSource.Proxied` | Emisora normal (precheck bloqueante) |
| `Single` | `source: PlaybackSource` | Un único destino directo (HLS o progresivo) |
| `Candidates` | `sources: List<PlaybackSource>` | Varias entradas de lista, en orden; máx. 5 |
| `Unavailable` | `error: PlaybackError` | No reproducible por la vía directa |

- **Invariantes**: `Candidates` no vacía, sin duplicados, todas `https`, tamaño ≤ 5.

### `PlaylistContent` (data, `data/remote/playlist` / interfaz en `domain`)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `text` | `String` | Cuerpo de la lista |
| `finalUrl` | `String` | URL final tras redirects (base para resolver relativas) |

- **Invariantes**: `finalUrl` es la URL efectiva de la respuesta; el parser nunca usa la URL original si hubo redirects.

### `PlaylistPlaybackQueue` (`feature/player`, puro)

Estado de reintento acotado dentro de una emisora de lista.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `sources` | `List<PlaybackSource>` | Candidatos en orden (1..5) |
| `index` | `Int` | Candidato actual (0-based) |

- **Operaciones**: `current(): PlaybackSource`, `advance(): Boolean` (true si hay siguiente), `exhausted: Boolean` (index al final).
- **Invariantes**: `advance()` nunca deja `index` fuera de rango; tope de saltos aplicado por el llamante (máx. 2).
- **Transiciones**: `Inicio(index=0)` → `advance()` → `Intento 2(index=1)` → `advance()` → `Intento 3(index=2)` → `advance()=false` → `Error`.

### `PlayerAudioConfig` (`feature/player`, puro)

| Elemento | Valor |
|----------|-------|
| `usage` | `C.USAGE_MEDIA` |
| `contentType` | `C.AUDIO_CONTENT_TYPE_SPEECH` (fuerza pausa en `CAN_DUCK`) |
| `handleAudioFocus` | `true` |
| `handleAudioBecomingNoisy` | `true` |

- **Invariantes**: la configuración se aplica una sola vez, al construir el `ExoPlayer` singleton (`di/PlayerModule.kt`).

## Entidades existentes reutilizadas

| Entidad | Ubicación | Cambio en esta feature |
|---------|-----------|------------------------|
| `StationDto` | `data/remote/dto/StationDtos.kt` | Sin cambios; su `url` pasa a usarse para clasificar formato (no para streams directos, que siguen por proxy) |
| `PlayerState` | `feature/player/PlayerViewModel.kt` | Sin cambios de variantes; `Error` recibe los mensajes de `PlaybackError` |
| `ActiveStationHolder` | `feature/player/ActiveStationHolder.kt` | **+** campo `resolvedSource: PlaybackSource?` (memoria) para reanudar post-Cast sin nueva red |
| `PlaybackRepo` / `PlaybackStatusDto` | `data/repo`, `dto` | Reutilizados; el precheck solo es bloqueante para `Proxied` |
| `AuthDataSourceFactory` | `feature/player` | Reutilizado para proxy; se añade datasource **sin** auth para URLs directas |
| `CastPlayerManager` | `cast` | `connectToStation` recibe la fuente resuelta; mantiene su foco manual |

## Reglas derivadas de requisitos

| Regla | Origen |
|-------|--------|
| Un único `ExoPlayer` singleton gestiona foco; nunca se crean players locales adicionales | FR-001..FR-005, constitución IV |
| Solo `https` llega al player desde fuentes de lista; `http` se descarta y produce `INSECURE_ONLY` si no queda ninguna | FR-010 |
| Emisoras sin formato de lista conservan proxy + Bearer sin cambios | FR-011 |
| El fallback entre entradas es acotado (máx. 3 intentos por reproducción) y sin duplicados | FR-009, caso borde del spec |
| El token Bearer no se envía nunca en fetch de listas ni en reproducción directa | FR-010, constitución II |
