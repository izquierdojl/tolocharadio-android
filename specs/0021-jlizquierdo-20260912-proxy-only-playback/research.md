# Research: Reproducción unificada por el proxy autenticado

**Feature**: `0021-jlizquierdo-20260912-proxy-only-playback` | **Date**: 2026-09-12

## R1. Contrato del servicio para reproducción (nuevo)

- **Decision**: apoyarse en el contrato ya desplegado en el repo backend (`resolve-playlist-proxy`):
  - `GET /api/v1/playback/:id` (Bearer): stream directo sin cambios; para emisoras `.m3u`/`.pls` resuelve la primera entrada HTTPS reproducible server-side y la sirve como stream continuo; para `.m3u8` entrega el manifiesto **reescrito** con `content-type` HLS. Registra historial en el consumo del stream principal.
  - `GET /api/v1/playback/:id/hls?u=&d=&s=` (Bearer): sirve variantes/segmentos firmados por el servidor (protección SSRF), propaga `Range` y **no** registra historial.
  - `GET /api/v1/playback/:id/status` (Bearer): `{id, playable, reason?}`; resuelve listas y HLS para decidir disponibilidad.
  - Errores tipados `{error:{code,message,status,details?}}` con códigos `PLAYLIST_EMPTY`, `PLAYLIST_INSECURE_ONLY`, `PLAYLIST_MALFORMED`, `PLAYLIST_UNREACHABLE`, `STREAM_UNAVAILABLE`.
- **Rationale**: es la fuente de verdad ya implementada y probada; el cliente no debe duplicar resolución ni reescribir manifiestos.
- **Alternatives**: seguir resolviendo en cliente (excepción 0019) — descartado: pierde historial server-side y preestado de disponibilidad (precheck) bloqueante.
- **Limits**: una lista de texto cuya primera entrada es un HLS se sirve sin reescribir (ver R7).

## R2. Selección de motor HLS con URI de proxy sin extensión

- **Decision**: la app determina si una emisora es HLS por la extensión de su enlace (`station.url` termina en `.m3u8`, sin query, case-insensitive) y, en tal caso, construye un `MediaItem` con `mimeType = MimeTypes.APPLICATION_M3U8` y una `HlsMediaSource`; en el resto, `ProgressiveMediaSource`. La URI es **siempre** la del proxy (`streamUrl(baseUrl, stationId)`).
- **Rationale**: la URI del proxy no tiene extensión, así que Media3 no puede inferir HLS por ella; el `mimeType` explícito y la `HlsMediaSource` son la vía soportada para forzar el extractor HLS sobre el manifiesto reescrito.
- **Alternatives**: `DefaultMediaSourceFactory` (no infiere sin extensión ni mime); sniffing del `Content-Type` de la primera respuesta (petición extra y más complejidad); que el backend exponga el tipo (cambio de backend innecesario).
- **Limits**: un HLS servido sin `.m3u8` en la URL seguiría tratándose como progresivo (mismo límite ya documentado en 0019).

## R3. Autenticación de subrecursos HLS (Bearer)

- **Decision**: `AuthDataSourceFactory` (OkHttp) se usa para **todas** las fuentes, incluidas las HLS. Como fija `Authorization: Bearer` en el `DataSource` que crea, cada petición de manifiesto, variante o segmento (`/playback/:id/hls?u=&d=&s=`) viaja autenticada.
- **Rationale**: el servicio exige `requireAuth` en el endpoint de subrecursos; el token no debe ir en la URL (la firma `u/d/s` es anti-SSRF, no autenticación).
- **Alternatives**: URL prefirmada con token (prohibido: token en URL).
- **Limits**: el `DataSource` se recrea por petición; el token se lee en el momento de crear el datasource (`accessTokenNow()`), por lo que un refresh de sesión se refleja en nuevas peticiones.

## R4. Traducción de motivos de error del servicio

- **Decision**: mapear los `reason`/códigos del servicio a mensajes accionables en español con una función pura (`PlaybackStatusReason`): `PLAYLIST_EMPTY` → "La lista de la emisora no contiene ninguna emisión reproducible.", `PLAYLIST_INSECURE_ONLY` → "La emisora solo ofrece conexiones no seguras (HTTP), bloqueadas por la app.", `PLAYLIST_MALFORMED` → "La lista de la emisora no tiene un formato válido.", `PLAYLIST_UNREACHABLE`/`STREAM_UNAVAILABLE` → "La emisora no está disponible en este momento.", desconocido → mensaje genérico con reintento.
- **Rationale**: hoy el preestado de disponibilidad (precheck) pasa el `reason` crudo a la UI (`r.value.reason`), lo que muestra códigos técnicos; FR-006 exige mensajes comprensibles y sin filtrar detalles técnicos (constitución IV).
- **Alternatives**: mostrar el `message` del servidor (puede ser técnico); dejar el código crudo (mala UX).
- **Limits**: los fallos que ocurren durante el streaming (no en el preestado de disponibilidad (precheck)) llegan como `PlaybackException` de Media3 sin el JSON del servicio; se usa el mensaje genérico accionable.

## R5. Inventario de retirada (código muerto)

- **Decision**: eliminar `PlaylistParser`, `PlaylistFetcher` (`PlaylistContent`/`PlaylistFetchException`), `OkHttpPlaylistFetcher`, `PlaylistPlaybackQueue`, `DirectDataSourceFactory`, `PlaybackError` y `PlaylistFormat` (sustituida por el helper `HlsStation.isHls`); retirar los bindings Hilt de `PlaylistFetcher` y `DirectDataSourceFactory`, los campos `resolvedSource`/`updateResolvedSource` de `ActiveStationHolder`, la cola/fallback de `PlayerViewModel` y sus tests (`PlaylistParserTest`, `PlaylistPlaybackQueueTest`, `PlaylistFormatTest`).
- **Rationale**: constitución V (YAGNI) y "el código muerto MUST eliminarse"; además elimina el uso de un datasource sin credenciales que ya no debe existir (FR-011: nada de URLs de proveedores).
- **Alternatives**: dejar el código sin usar (deuda y superficie de ataque).
- **Limits**: `HlsStation.isHls` conserva la única detección local necesaria para elegir el motor (R2).

## R6. Compatibilidad con instancias no actualizadas

- **Decision**: asumir una instancia actualizada (FR-013). Sin feature-detect ni fallback a reproducción directa. Documentar el requisito en las notas de la versión.
- **Rationale**: mantiene el cliente simple y sin resolución de listas; el fallback reintroduciría la excepción retirada.
- **Alternatives**: fallback a reproducción directa (reintroduce código y la vulnerabilidad de hablar con terceros); exigir versión mínima (más maquinaria para poco valor).
- **Limits**: contra un backend antiguo, las emisoras de lista fallarán con error accionable.

## R7. Límite conocido: lista de texto que resuelve a HLS

- **Decision**: documentar el límite y abrir un issue en el backend (FR-014). No se mitiga en el cliente.
- **Rationale**: el backend actual sirve la primera entrada de una lista `.m3u`/`.pls` sin reescribir si es un HLS; la app la trataría como progresiva. Mitigarlo en cliente exigiría volver a resolver listas (contradice FR-002).
- **Alternatives**: bloquear la 0021 (retrasa el cierre del issue #4); heurística de reintento en cliente (poco fiable con segmentos relativos).
- **Limits**: cobertura de estas emisoras queda pendiente del cambio backend de seguimiento.
