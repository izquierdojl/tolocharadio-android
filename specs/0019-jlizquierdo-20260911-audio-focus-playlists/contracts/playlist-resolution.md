# Contract: Detección, parseo y resolución de listas de emisoras

**Feature**: `0019-jlizquierdo-20260911-audio-focus-playlists` | **Requisitos**: FR-006..FR-013 | **SC**: SC-001, SC-005, SC-006

Contrato del flujo que convierte `station.url` en una `PlaybackSource` reproducible, incluyendo formatos, seguridad, orden y errores.

## 1. Detección de formato

Entrada: `station.url` (puede venir de catálogo o de emisora personalizada; FR-008).

| Extensión del path (sin query, case-insensitive) | Formato | Acción |
|---------------------------------------------------|---------|--------|
| `.m3u8` | HLS | `Single(DirectHls(url original))` — lo gestiona `HlsMediaSource` |
| `.m3u` | M3U | Fetch + parseo (secciones 3-6) |
| `.pls` | PLS | Fetch + parseo (secciones 3-6) |
| otra / sin extensión / URL malformada | — | `Proxied(stationId)` (pipeline actual, sin cambios) |

- Se usa el *path* de la URL con `java.net.URI`, ignorando `?query` y `#fragment`.
- Nunca se lanza excepción por URL malformada: se degrada a `Proxied`.

## 2. Fetch de la lista de texto

| Aspecto | Regla |
|---------|-------|
| Cliente | OkHttp dedicado **sin** interceptores de auth |
| Cabecera | `User-Agent: TolochaRadio-Android`; **nunca** `Authorization` (no filtrar el Bearer a terceros) |
| Redirects | Seguidos; la URL final es la base para resolver relativas |
| Timeout | conexión/lectura 10 s |
| Cuerpo | límite ~1 MB; se lee como texto |
| Fallo de red | `Unavailable(NETWORK)` |

## 3. Gramática M3U

- Se lee línea a línea.
- Se ignoran líneas vacías y las que empiezan por `#` (`#EXTM3U`, `#EXTINF`, directivas…).
- Cada línea útil es una URL (absoluta o relativa); se toma el primer token recortado antes de espacios.
- Si la URL es relativa, se resuelve contra la URL final del fetch (`URI.resolve`).
- Ejemplo válido:
  ```text
  #EXTM3U
  #EXTINF:-1,Radio Ejemplo
  https://stream.example.com/live.mp3
  ```

## 4. Gramática PLS

- Se leen líneas `clave=valor`.
- Se consideran entradas las claves `FileN` (N entero); se ordenan por N ascendente.
- `TitleN`, `LengthN`, `NumberOfEntries` y comentarios (`;`, `#`) se ignoran.
- URLs relativas se resuelven contra la URL final del fetch.
- Ejemplo válido:
  ```ini
  [playlist]
  NumberOfEntries=2
  File1=https://stream.example.com/live.aac
  Title1=Radio Ejemplo
  File2=https://backup.example.com/live.aac
  ```

## 5. Seguridad (FR-010)

1. Se resuelven todas las entradas.
2. Se descartan las que no sean `https` (incluye `http`, `rtsp`, `file`…).
3. El resultado nunca contiene URLs no-HTTPS.
4. Si tras el filtro no queda ninguna y **había** entradas → `Unavailable(INSECURE_ONLY)`.
5. Si el texto no es ni M3U ni PLS reconocible → `Unavailable(MALFORMED)`.
6. Si no hay ninguna entrada (vacío) → `Unavailable(NO_ENTRIES)`.

## 6. Orden, deduplicación y tope

- Orden original (M3U) o por `N` ascendente (PLS), preservado.
- Deduplicación estable por URL normalizada (primer puesto gana).
- Máximo **5** candidatos.
- Reproducción: primer candidato; si falla, siguiente (máx. 2 saltos, 3 entradas por intento), sin bucles (FR-009, caso borde).

## 7. Asignación a Media3

| Fuente | MediaSource | MimeType del `MediaItem` |
|--------|-------------|---------------------------|
| `Proxied` | `ProgressiveMediaSource` + datasource con Bearer | (sin cambios) |
| `DirectProgressive` | `ProgressiveMediaSource` + datasource sin auth | inferido por ExoPlayer |
| `DirectHls` | `HlsMediaSource` + datasource sin auth | `MimeTypes.APPLICATION_M3U8` |

- El `mimeType` se fija en el `MediaItem` para que el receptor Cast reconozca HLS.
- La `MediaMetadata` (título/artista/artwork) se conserva en todos los casos.

## 8. Errores observables

| Situación | Resultado | Mensaje |
|-----------|-----------|---------|
| Sin red al descargar la lista | `Unavailable(NETWORK)` | "No se pudo descargar la lista. Comprueba tu conexión." |
| Lista vacía / sin entradas | `Unavailable(NO_ENTRIES)` | "La lista de la emisora no contiene ninguna emisión reproducible." |
| Solo entradas HTTP | `Unavailable(INSECURE_ONLY)` | "La emisora solo ofrece conexiones no seguras (HTTP), bloqueadas por la app." |
| Texto no reconocible | `Unavailable(MALFORMED)` | "La lista de la emisora no tiene un formato válido." |
| Fallo del stream directo tras agotar candidatos | `PlayerState.Error` | "Se ha interrumpido la reproducción." + reintento manual |

Todos los estados conservan el botón de reintento y nunca cierran la app (SC-006).

## 9. Mapeo de aceptación

| Escenario | Verificación |
|-----------|--------------|
| US2-1 `.m3u8` | `HlsMediaSource` reproduce la emisora de prueba (SC-001) |
| US2-2 `.m3u`/`.pls` | Primera entrada HTTPS suena (SC-001) |
| US2-3 lista irresoluble | Error tipado accionable sin crash (SC-006) |
| US2-4 emisora personalizada | Mismo flujo que catálogo (FR-008) |
| Regresión | Emisoras directas siguen por proxy + Bearer (SC-005) |
| Unitarias | `PlaylistFormatTest`, `PlaylistParserTest`, `ResolvePlaybackSourceUseCaseTest`, `PlaylistPlaybackQueueTest` |

## 10. Excepciones documentadas

- Estas fuentes **no** atraviesan el proxy autenticado; el historial no se registra server-side para ellas (Complexity Tracking del plan).
- HLS sin extensión `.m3u8` en la URL no se detecta en v1.
