# Bug Assessment: Emitir a Cast un episodio a medias empieza desde el principio

- **Slug**: 0056-izquierdojl-20261122-podcast-cast-resume-position
- **Created**: 2026-11-22
- **Source**: hallazgo durante el cierre del bug 0054
- **Verdict**: valid
- **Severity**: medium

## Report (verbatim or summarized)

> Detectado al revisar el fix 0054: la reanudación de un episodio con progreso
> parcial funciona en local, pero al emitirlo a Cast empieza en 0:00. El
> controlador pasa la posición a `EpisodePlaybackController.play()`, pero la
> rama de Cast la ignora.

No hay URL; el hallazgo viene de la revisión de código del fix 0054.

## Symptom

Con una sesión Cast conectada, seleccionar desde la cola un episodio que ya se
escuchó parcialmente lo reproduce desde el principio en el dispositivo remoto.
Lo esperado es que el receptor empiece en la posición guardada, igual que la
reproducción local (FR-001/FR-006 de spec 0047).

## Reproduction

1. Conectar un dispositivo Cast e iniciar sesión en el servidor de podcasts.
2. Tener un episodio de la cola con progreso parcial (p. ej. 27:43 de 40:38).
3. Con Cast conectado, seleccionar ese episodio desde la cola.
4. **Observado**: el receptor empieza en 0:00. **Esperado**: continúa en la
   posición parcial.

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePlaybackController.kt:147` — la rama `casting` llama a `cast.connectToEpisode(source.uri, episode.title, podcast?.title)` sin pasar `startMs`.
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt:422` — `connectToEpisode()` construye el `MediaItem` y llama a `player.setMediaItem(item)` (posición 0) antes de `prepare()`.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactory.kt:166` — `createEpisodeCastItem()` no lleva posición; es el `Player` quien debe recibirla.

## Root Cause Hypothesis

Confianza alta. `EpisodePlaybackController.play()` recibe `startMs` y lo aplica
en la rama local (`engine.player.seekTo(startMs)`), pero la rama Cast no lo
propaga: `connectToEpisode()` no tiene parámetro de posición y siempre llama a
`setMediaItem(item)`, que equivale a empezar en 0. `CastPlayer` hereda
`setMediaItem(MediaItem, long)` de `BasePlayer`, que lo reenvía a
`RemoteCastPlayer.setMediaItems(items, 0, startPositionMs)`, así que el
receptor puede recibir la posición sin cambios de contrato.

## Proposed Remediation

**Preferred**:
1. Añadir `startMs: Long` a `CastPlayerManager.connectToEpisode(...)`.
2. Usar `player.setMediaItem(mediaItem, startMs)` en lugar de `setMediaItem(mediaItem)`, manteniendo `prepare()` y `playWhenReady = true`.
3. Pasar `startMs` desde la rama Cast de `EpisodePlaybackController.play()`.

**Alternativas**:
- `prepare()` + `seekTo(startMs)` después: funciona en algunos receptores, pero el seek puede perderse si la sesión aún no está lista; `setMediaItem(item, startMs)` es la vía soportada por Media3.
- Reutilizar el `MediaItem` local con `setStartPositionMs`: no existe en `MediaItem`; la posición va en el `Player`.

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePlaybackController.kt`
- `app/src/test/java/com/izquierdojl/tolocharadio/...` (ver tests)

**Tests to add or update**:
- Test de que la rama Cast de `EpisodePlaybackController.play()` propaga `startMs` (con un fake/mock de `CastPlayerManager`).
- Si se extrae la construcción de la llamada a un helper puro, test unitario `startMs > 0` → `setMediaItem(item, startMs)`.
- Verificación manual con un receptor Cast real: episodio a medias → empieza en la posición guardada; pausa, desconexión y vuelta a local → conserva la posición.

## Risks & Considerations

- Algunos receptores pueden ignorar la posición inicial si el medio aún no es seekable; hay que probarlo con el Chromecast real.
- No hay cambios de API ni de servidor: la URL firmada y el `MediaItem` son los mismos.
- La reproducción local y el resto de la cola no se ven afectados.
- Es un fallo independiente del fix 0054; no bloquea su cierre.

## Open Questions

- [NEEDS CLARIFICATION: confirmar en un receptor Cast físico si `setMediaItem(item, startMs)` reanuda correctamente; el emulador no permite validar el receptor real.]
