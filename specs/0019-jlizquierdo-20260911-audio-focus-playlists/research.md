# Research: Reproducción exclusiva y soporte de listas m3u/m3u8/pls

**Feature**: `0019-jlizquierdo-20260911-audio-focus-playlists` | **Date**: 2026-09-11

Este documento resuelve las incógnitas técnicas del plan. No quedan `NEEDS CLARIFICATION`.

## R0. Tooling: `setup-plan.ps1` con shim de `python3` (mejora sobre 0018)

- **Decision**: Anteponer al PATH de la invocación un shim temporal `python3.cmd` (contenido `@echo off` + `python %*`) en `%TEMP%\opencode\pyshim`, y ejecutar `setup-plan.ps1 -Json` normalmente. `setup-plan.ps1` copió el template y devolvió el JSON de rutas.
- **Rationale**: `Get-Python3Command` (`.specify/scripts/powershell/common.ps1:322`) devuelve `python3` sin validarlo; en este equipo `python3` es el alias de Microsoft Store (falla con exit 9009) y el resolver lanza `Invalid preset manifest` (`common.ps1:668`) aunque `preset.yml` es válido. El shim hace que `Get-Command python3` resuelva a un ejecutable real (`python` 3.14.7 con PyYAML) sin tocar scripts generados ni la configuración de Windows.
- **Alternatives**: copiar/rellenar artefactos a mano desde `.specify/templates/` (plan 0018; funciona pero pierde la resolución de presets); parchear `common.ps1` (se pierde en cada `specify upgrade`); deshabilitar el alias de Windows (cambio de máquina, fuera del repo).
- **Limits**: el shim es por invocación (el PATH no persiste entre shells). Un upgrade de Spec Kit que arregle `Get-Python3Command` lo hace innecesario.

## R1. Foco de audio local con Media3 (FR-001..FR-005)

- **Decision**: configurar el `ExoPlayer` compartido en `di/PlayerModule.kt` con:
  - `androidx.media3.common.AudioAttributes`: `usage = C.USAGE_MEDIA`, `contentType = C.AUDIO_CONTENT_TYPE_SPEECH`.
  - `ExoPlayer.Builder(context).setAudioAttributes(attrs, /* handleAudioFocus = */ true).setHandleAudioBecomingNoisy(true).build()`.
- **Rationale** (verificado contra `media3-exoplayer-1.4.1-sources.jar`):
  - `USAGE_MEDIA` hace que Media3 solicite `AUDIOFOCUS_GAIN` (`convertAudioAttributesToFocusGain`), de modo que al reproducir TolochaRadio las demás apps que respetan el foco se detienen (FR-002).
  - Ante `AUDIOFOCUS_LOSS` el `AudioFocusManager` ejecuta `PLAYER_COMMAND_DO_NOT_PLAY` (pausa sin auto-reanudar; FR-004).
  - Ante `AUDIOFOCUS_LOSS_TRANSIENT` ejecuta `WAIT_FOR_CALLBACK`: el player queda *suprimido* manteniendo `playWhenReady = true`; en `AUDIOFOCUS_GAIN` se ejecuta `PLAY_WHEN_READY` y reanuda. Si el usuario pausó durante la interrupción, `playWhenReady` ya es `false` y por tanto no se reanuda por sorpresa (FR-003).
  - `handleAudioBecomingNoisy = true` pausa al desconectar auriculares/Bluetooth (caso borde del spec).
  - `contentType = SPEECH` es la condición que usa Media3 para `willPauseWhenDucked()` (`AudioFocusManager.willPauseWhenDucked()` = `contentType == C.AUDIO_CONTENT_TYPE_SPEECH`). Con ello `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK` **pausa** en lugar de bajar el volumen, tal como decidió el usuario en clarify (opción A, "no duck").
  - El foco vive en el player (no en la Activity), por lo que aplica igual en background, notificación y controles Bluetooth (FR-005).
- **Alternatives**: `AudioFocusRequest` manual en el servicio (como hace `CastPlayerManager`): más código, obliga a reimplementar la distinción "reanudar salvo pausa manual" y duplica responsabilidad; `CONTENT_TYPE_MUSIC` (duck en `CAN_DUCK`, incumple el clarify); `CONTENT_TYPE_SPEECH` sin `handleAudioFocus` (no pediría foco).
- **Limits**: el camino Cast conserva su foco manual (`CastPlayerManager.kt:52-67, 172-193`) porque el dispositivo remoto es quien reproduce; cuando Cast está conectado el `ExoPlayer` local no está en `READY`/`playWhenReady`, así que no solicita foco y no hay doble petición.

## R2. HLS (`.m3u8`): dependencia y MediaSource

- **Decision**: añadir `androidx.media3:media3-exoplayer-hls:1.4.1` (misma versión que `media3` del catálogo) y construir `HlsMediaSource.Factory(directDataSource).createMediaSource(item)` para emisoras HLS.
- **Rationale**: Media3 no incluye el extractor HLS en `media3-exoplayer`; el módulo oficial es la vía soportada. Según el clarify, la lista HLS se reproduce como un único stream continuo (variantes/segmentos gestionados por ExoPlayer, sin intervención del usuario). Reproducir la URL original (no el proxy) es lo que permite resolver segmentos relativos correctamente.
- **Alternatives**: `DefaultMediaSourceFactory` (elegiría HLS por reflexión pero obtendría el mismo problema de proxy si no se fija el mime; se prefiere selección explícita); `ffmpeg`/VLC u otra librería (dependencia pesada, fuera del stack Media3 y de la constitución II).
- **Limits**: solo se detecta HLS por extensión de `station.url` (`.m3u8`, ignorando query). Un HLS servido sin extensión en la URL no se detectará en v1 (documentado; el campo `codec` del backend no es fiable).

## R3. Resolución de listas de texto (`.m3u` / `.pls`)

- **Decision**:
  - `PlaylistFormat.detect(station.url)`: con `java.net.URI` sobre la URL sin query; extensiones case-insensitive `m3u8` → HLS, `m3u` → M3U, `pls` → PLS, resto → `null` (stream directo).
  - Fetch: `PlaylistFetcher.fetch(url)` con OkHttp **sin** `Authorization` (no se filtra el Bearer a terceros), `followRedirects = true`, timeout de conexión/lectura 10 s, máximo ~1 MB de cuerpo; devuelve `PlaylistContent(text, finalUrl)` usando la URL final tras redirects.
  - Parseo puro (`PlaylistParser`): **m3u** = líneas no vacías que no empiezan por `#` (se ignoran `#EXTM3U`, `#EXTINF` y demás directivas); **pls** = líneas `FileN=URL` ordenadas por `N`. En ambos casos se recorta, se resuelve contra `finalUrl` las URLs relativas con `URI.resolve`, se descartan esquemas distintos de `https` (FR-010: HTTP en claro bloqueado) y se deduplica preservando orden.
  - Candidatos: lista ordenada, máximo 5 (FR-009 sin bucles).
- **Rationale**: `.m3u`/`.pls` no son formatos de audio; Media3 no los reproduce. El parseo es texto plano y no requiere librería externa. Fetch sin credenciales + filtro HTTPS cumple el principio de seguridad (nunca token en URL ni a terceros).
- **Alternatives**: librería de playlists (p. ej. `jplaylist`/`m3u-parser`) — innecesaria para dos gramáticas triviales y añade dependencia sin justificar; resolución server-side (ver R5); aceptar `http` (viola `usesCleartextTraffic=false` y FR-010).
- **Limits**: `file://`, `rtsp://` y otros esquemas se descartan. Un `.m3u8` que sea una lista de pistas sueltas (no HLS) se delega a ExoPlayer HLS (comportamiento estándar). No hay fallback automático entre variantes HLS (lo gestiona ExoPlayer).

## R4. Fuente de reproducción y política de proxy

- **Decision**: `ResolvePlaybackSourceUseCase(station, baseUrl)` devuelve:
  - `Proxied(stationId)` → emisora normal: se mantiene el pipeline actual (proxy `/playback/:id` + `ProgressiveMediaSource`, Bearer).
  - `Single(DirectHls(station.url))` → `.m3u8`.
  - `Candidates(List<PlaybackSource>)` → `.m3u`/`.pls` resueltos (cada candidato `DirectHls` si acaba en `.m3u8`, si no `DirectProgressive`).
  - `Unavailable(PlaybackError)` → red, sin entradas, solo entradas inseguras o URL malformada.
- **Rationale**: la excepción de proxy está justificada en Complexity Tracking; centralizar la decisión permite que local, Cast y reanudación compartan el mismo resultado.
- **Alternatives**: reproducir HLS a través del proxy (segmentos relativos roto); mantener proxy y "esperar" a un cambio de backend (bloquea la feature).
- **Limits**: no se envían cabeceras de sesión a hosts de terceros; si un stream directo exige autenticación propia no funcionará (fuera de alcance).

## R5. Interacción con el precheck `playback/:id/status`

- **Decision**: para `Proxied` se mantiene el precheck bloqueante actual. Para formatos de lista, el precheck deja de ser bloqueante (`playable = false` no impide reproducir) porque el servidor no puede validar listas/HLS; un error de red/autenticación del precheck se registra pero la reproducción directa puede intentarse igualmente. La resolución de la lista es la que decide el error final.
- **Rationale**: FR-012 pide mantener el "preestado", pero bloquear con un falso negativo haría inservible la feature. Se conserva la llamada para no cambiar el flujo ni la semántica de errores de red.
- **Alternatives**: omitir el precheck para listas (pierde señales de red/auth); respetar `playable=false` (puede impedir emisoras válidas).
- **Limits**: si el backend mejora la validación de listas, este bypass podrá retirarse.

## R6. Fallback acotado entre entradas de una lista

- **Decision**: `PlaylistPlaybackQueue` (puro) mantiene la lista de candidatos y un índice; expone `current`, `advance(): Boolean` y `exhausted`. En `PlayerViewModel.onPlayerError`, si la fuente actual es una lista con candidatos restantes, se reproduce el siguiente candidato automáticamente (máximo 2 saltos, es decir hasta 3 entradas por intento); al agotarse se emite `PlayerState.Error` con mensaje accionable. El historial de candidatos no se mezcla con `retry()` manual, que reinicia desde el primer candidato.
- **Rationale**: cubre el caso borde del spec ("si la primera falla, se intenta la siguiente") sin bucles: dedupe + tope. Aislar la cola en una clase pura la hace testeable sin Android.
- **Alternatives**: sin fallback (frustra al usuario si la primera entrada está muerta); fallback ilimitado (riesgo de bucle); fallback con backoff de red (sobredimensionado y cubierto parcialmente por el reintento manual).
- **Limits**: el fallback es inmediato ante cualquier `PlaybackException` del candidato; errores transitorios de red también consumen un salto (tope 3, aceptable en v1).

## R7. Centralización de `MediaItem`/`MediaSource` (local, Cast y reanudación)

- **Decision**: crear `StationMediaItemFactory` con `create(station, source)` → `MediaItem` (URI proxy o directa, `MediaMetadata` con título/artista/artwork, `mimeType = MimeTypes.APPLICATION_M3U8` cuando es HLS) y `createMediaSource(item, source)` → `HlsMediaSource` o `ProgressiveMediaSource`. La usan `PlayerViewModel.startStream`, `CastPlayerManager.connectToStation(station, source)` y `CastPlayerManager.resumeLocalPlayback` (leyendo la fuente guardada en `ActiveStationHolder.resolvedSource`, sin repetir red).
- **Rationale**: hoy hay 3 copias divergentes de la construcción de `MediaItem` (`PlayerViewModel.kt:225-256`, `CastPlayerManager.kt:196-225` y `258-280`); con HLS/mime la divergencia produciría bugs. `mimeType` en el item es además lo que permite a Cast anunciar HLS al receptor.
- **Alternatives**: parchear los 3 sitios por separado (duplicación y riesgo); inyectar un `MediaSource.Factory` global (no cubre Cast, que usa `setMediaItem`).
- **Limits**: `resumeLocalPlayback` usa la última fuente resuelta; si el proceso murió, cae al proxy para emisoras normales (comportamiento actual) y a la URL directa si es HLS, sin fetch de lista (mejor esfuerzo).

## R8. Taxonomía de error y mensajes (FR-009/FR-013)

- **Decision**: `PlaybackError` sellado/enum con mapeo a mensajes en español:
  - `NETWORK` → "No se pudo descargar la lista. Comprueba tu conexión."
  - `NO_ENTRIES` → "La lista de la emisora no contiene ninguna emisión reproducible."
  - `INSECURE_ONLY` → "La emisora solo ofrece conexiones no seguras (HTTP), bloqueadas por la app."
  - `MALFORMED` → "La lista de la emisora no tiene un formato válido."
  - Un fallo del player en fuente directa se mapea al mensaje genérico actual de interrupción, conservando el botón de reintento.
- **Rationale**: FR-013 pide distinguir fallo de resolución vs red vs emisora caída; mensajes accionables sin filtrar detalles técnicos (constitución IV).
- **Alternatives**: mensaje único genérico (incumple FR-013); exponer el error crudo del servidor (prohibido por constitución IV).
- **Limits**: el código `PlaybackException.errorCode` no se muestra; solo se distingue la clase de fallo de resolución.

## R9. Estrategia de tests (constitución III)

- **Decision** (tests antes de la implementación, sin Robolectric):
  - `PlaylistFormatTest`: detección `.m3u8`/`.m3u`/`.pls`/desconocido, query string, mayúsculas, URL inválida.
  - `PlaylistParserTest`: m3u con comentarios/`#EXTINF`, relativas y absolutas, http descartadas, dedupe, vacío; pls `FileN` desordenado, `TitleN` ignorado, malformado.
  - `ResolvePlaybackSourceUseCaseTest`: clasificación, fetch OK → candidatos, fetch KO → `NETWORK`, sin entradas → `NO_ENTRIES`, solo http → `INSECURE_ONLY`, emisora custom.
  - `PlaylistPlaybackQueueTest`: `current`, `advance`, tope, `exhausted`.
  - `PlayerAudioConfigTest`: atributos `USAGE_MEDIA` + `CONTENT_TYPE_SPEECH` en JVM (Media3 `common` no depende de Android).
  - `PlayerViewModelTest` (ampliado): reproduce fuente directa/HLS sin precheck bloqueante, fallback en `onPlayerError`, agotamiento → `Error`, no-regresión del camino proxy.
  - `CastPlayerManager` y el builder de ExoPlayer quedan como integración delgada; verificación instrumentada/manual en quickstart.
- **Rationale**: cubre las reglas críticas con tests JVM rápidos y deterministas en `testDebugUnitTest` (gate de CI), sin dependencias de test nuevas.
- **Limits**: el comportamiento real de foco (llamadas, VLC, auriculares) y la reproducción HLS real requieren dispositivo/emulador; se validan en quickstart.

## R10. Flujo de `PlayerViewModel.play()` con fuentes de lista

- **Decision**: `play(station)` pasa a: cancelar carga previa → reset de mute/volumen → estado `Buffering` → `viewModelScope`: `ResolvePlaybackSourceUseCase(station, base)`:
  - `Proxied` → precheck actual (bloqueante) → `startStream`.
  - `Single`/`Candidates` → `startStream` directo (creando `PlaylistPlaybackQueue` si hay varios) sin esperar al precheck bloqueante.
  - `Unavailable` → `PlayerState.Error(station, mensaje)`.
  `startStream` guarda la fuente en `ActiveStationHolder.resolvedSource` y crea la `MediaSource` con la fábrica; si Cast está conectado delega en `connectToStation(station, source)`.
- **Rationale**: un solo punto de decisión, con la semántica de errores existente y compatibilidad con shortcuts/Cast/sleep timer (FR-012). El trabajo de red sigue en `viewModelScope`/IO.
- **Alternatives**: resolver dentro de `startStream` (oculta la lógica y complica el fallback); resolver en el repositorio de playback (mezcla red/proxy con clasificación de formato).
- **Limits**: el precheck se omite para listas (R5); el historial server-side no se registra en fuentes directas (Complexity Tracking).

## Resolved unknowns

| Incógnita | Resolución |
|-----------|------------|
| Cómo gestionar foco sin romper "reanudar salvo pausa manual" | Media3 `handleAudioFocus` + `CONTENT_TYPE_SPEECH` (R1) |
| Cómo pausar en vez de duck | `CONTENT_TYPE_SPEECH` activa `willPauseWhenDucked()` en Media3 (R1) |
| Librería para HLS | `media3-exoplayer-hls:1.4.1` (R2) |
| Cómo reproducir `.m3u`/`.pls` | Fetch sin auth + parser puro + resolución de relativas + filtro HTTPS (R3) |
| Proxy vs directo | Directo solo para formatos de lista, justificado (R4, Complexity Tracking) |
| Precheck en emisoras de lista | No bloqueante (R5) |
| Fallback entre entradas | Cola pura con tope de 3 entradas (R6) |
| Evitar duplicar MediaItem en 3 sitios | `StationMediaItemFactory` + `resolvedSource` en el holder (R7) |
| Mensajes de error diferenciados | `PlaybackError` tipado (R8) |
| Cómo testear sin Robolectric | Lógica pura + fakes/MockK; dispositivo para foco/HLS (R9) |
| Flujo de play con listas | Resolver antes de precheck; ramas `Proxied`/`Single`/`Candidates`/`Unavailable` (R10) |
