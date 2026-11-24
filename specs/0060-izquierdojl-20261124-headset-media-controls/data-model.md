# Data Model: Controles de auriculares para pausar y saltar

**Feature**: `0060-izquierdojl-20261124-headset-media-controls`
**Date**: 2026-11-24

Este documento define las entidades, valores canónicos y restricciones. Cualquier
tarea que implemente o valide un campo debe citar `Entidad.campo`.

## HeadsetControlSettings

Preferencias globales de los controles de auricular. Es la única entidad
persistida.

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `skipBackSeconds` | `Int` | Valores permitidos: `5, 10, 15, 20, 30, 45, 60`. Default `15`. Valor ausente o inválido → `15`. |
| `skipForwardSeconds` | `Int` | Valores permitidos: `5, 10, 15, 20, 30, 45, 60`. Default `30`. Valor ausente o inválido → `30`. |
| `gesturesEnabled` | `Boolean` | Default `true`. Si es `false`, se ignoran los saltos de `SkipSource.HeadsetGesture`; los controles explícitos (reproductor, notificación, bloqueo) siguen saltando. |

**Derivaciones**:
- `skipBackMs = skipBackSeconds * 1_000L`
- `skipForwardMs = skipForwardSeconds * 1_000L`
- `shortLabel` de cada valor: `"5 s"`, `"10 s"`, …

**Invariantes**:
- Los dos segundos son independientes; no hay relación de orden.
- La preferencia es global para la app, no por servidor, emisora ni podcast.
- Cambiar un valor se aplica al siguiente salto, sin reiniciar la reproducción.

**Ubicación canónica**:
`app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/HeadsetControlSettings.kt`

**Persistencia**:
- `headset_skip_back_sec` (`Int`)
- `headset_skip_forward_sec` (`Int`)
- `headset_skip_enabled` (`Boolean`)

## PlaybackContent

Estado de contenido que determina si un salto es posible. Es efímero y se
deriva en cada gesto.

| Valor | Procedencia | Permite salto |
|-------|-------------|---------------|
| `Idle` | Sin episodio ni emisora activos | No |
| `LiveRadio` | Radio en directo activa | No |
| `EpisodePlaying` | `EpisodePlaybackController.State.Active(playing = true)` | Sí |
| `EpisodePaused` | `EpisodePlaybackController.State.Active(playing = false)` | Sí |

**Reglas**:
- `Loading`, `Reconnecting`, `WaitingForNetwork` y `Error` no se consideran
  `EpisodePlaying`/`EpisodePaused` para saltos: se comportan como contenido sin
  salto y no interrumpen el audio.
- La radio en directo nunca permite salto, aunque el player esté pausado.

**Ubicación canónica**:
`app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/HeadsetControlPolicy.kt`

## HeadsetCommand

Comando lógico recibido por el controlador, independiente de la tecla física.

| Valor | Origen |
|-------|--------|
| `SkipBack` | `KEYCODE_MEDIA_PREVIOUS`, botón de retroceso de la notificación, `COMMAND_SEEK_BACK`/`COMMAND_SEEK_TO_PREVIOUS` |
| `SkipForward` | `KEYCODE_MEDIA_NEXT`, botón de avance de la notificación, `COMMAND_SEEK_FORWARD`/`COMMAND_SEEK_TO_NEXT` |
| `Next` | `KEYCODE_MEDIA_NEXT` físico (alias de `SkipForward`) |
| `Previous` | `KEYCODE_MEDIA_PREVIOUS` físico (alias de `SkipBack`) |

**Nota**: play/pausa no forma parte de esta entidad; lo gestiona Media3 de forma
nativa para no duplicar comportamiento.

**Ubicación canónica**:
`app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/HeadsetControlPolicy.kt`

## SkipSource

Origen del salto; decide si el interruptor `gesturesEnabled` aplica.

| Valor | Origen | `gesturesEnabled = false` |
|-------|--------|---------------------------|
| `HeadsetGesture` | Pulsación larga de volumen y `KEYCODE_MEDIA_NEXT`/`PREVIOUS` físicos | `Ignore` |
| `ExplicitControl` | Botones de salto del reproductor, notificación, bloqueo y comandos de la sesión | Sigue saltando |

**Ubicación canónica**:
`app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/HeadsetControlPolicy.kt`

## HeadsetAction

Resultado de la política.

| Valor | Significado |
|-------|-------------|
| `Skip(deltaMs)` | Mover la posición del episodio `deltaMs` (positivo adelante, negativo atrás). |
| `Ignore` | No ejecutar ninguna acción; el evento se consume si venía de un gesto de salto. |

**Invariante**: `Skip` solo se produce con `EpisodePlaying`/`EpisodePaused` y
con `SkipSource.ExplicitControl` o `gesturesEnabled = true`.

**Ubicación canónica**:
`app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/HeadsetControlPolicy.kt`

## HeadsetControlSettingsCache

Caché en memoria del flujo de DataStore para consumidores no suspendidos.

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `settings` | `StateFlow<HeadsetControlSettings>` | Valor inicial `DEFAULT`; se actualiza con cada emisión de DataStore. |

**Ciclo de vida**: se crea al arrancar la app (`@ApplicationScope`), vive
mientras el proceso está vivo y no se persiste.

**Ubicación canónica**:
`app/src/main/java/com/izquierdojl/tolocharadio/data/local/HeadsetControlSettingsCache.kt`

## VolumeGestureState

Estado interno de una pulsación de volumen. No se persiste.

| Estado | Campos | Transición |
|--------|--------|------------|
| `Idle` | — | `ACTION_DOWN` elegible → `Tracking` |
| `Tracking` | `keyCode`, `job`, `longTriggered = false` | `ACTION_UP` antes de 500 ms → `ShortPress`; 500 ms → `LongPress`; cancelación → `Idle` |
| `ShortPress` | `keyCode` | Ajusta volumen una vez y vuelve a `Idle` |
| `LongPress` | `keyCode` | Ejecuta un salto, ignora repeticiones y `ACTION_UP`, vuelve a `Idle` |

**Elegibilidad**: `hostResumed = true` y
`HeadsetControlsController.canSkip(SkipSource.HeadsetGesture) = true` (episodio
activo, gestos activos, modo de audio normal y sin supresión por foco).

**Ubicación canónica**:
`app/src/main/java/com/izquierdojl/tolocharadio/feature/player/HeadsetVolumeGestureHandler.kt`

## MediaButtonLayout

Decisión de qué botones publica la notificación. Ya existe y se amplía.

| Valor | Condición | Botones de salto |
|-------|-----------|------------------|
| `RADIO` | Radio sin fallo | No |
| `RADIO_RETRY` | Radio con fallo recuperable | No |
| `EPISODE` | Episodio activo | Sí, con segundos configurados |

**Invariante**: `RADIO` y `RADIO_RETRY` nunca muestran botones de salto
(FR-009); `EPISODE` los muestra con los segundos vigentes, incluso con
`gesturesEnabled = false` (los botones explícitos siguen activos, SC-014).

**Ubicación canónica**:
`app/src/main/java/com/izquierdojl/tolocharadio/feature/player/MediaButtonPolicy.kt`

## Matriz de decisión

| Contenido | Comando | Fuente | `gesturesEnabled` | Acción |
|-----------|---------|--------|-------------------|--------|
| `EpisodePlaying` | `SkipForward`/`Next` | `HeadsetGesture` | `true` | `Skip(+forwardMs)` |
| `EpisodePlaying` | `SkipBack`/`Previous` | `HeadsetGesture` | `true` | `Skip(-backMs)` |
| `EpisodePaused` | `SkipForward`/`Next` | `HeadsetGesture` | `true` | `Skip(+forwardMs)` |
| `EpisodePaused` | `SkipBack`/`Previous` | `HeadsetGesture` | `true` | `Skip(-backMs)` |
| `EpisodePlaying`/`EpisodePaused` | cualquier salto | `ExplicitControl` | cualquiera | `Skip(±segundos)` |
| Cualquiera | cualquier salto | `HeadsetGesture` | `false` | `Ignore` |
| `LiveRadio` | cualquier salto | cualquiera | cualquiera | `Ignore` |
| `Idle` | cualquier salto | cualquiera | cualquiera | `Ignore` |

**Gating transversal**: si el modo de audio no es `MODE_NORMAL` (llamada,
ringtone o comunicación) o el player está suprimido por pérdida de foco,
`HeadsetControlsController.canSkip(source)` es `false` y todos los saltos se
ignoran, con independencia de la matriz (FR-020).

**Límites de posición**:
- `targetMs = (episodeController.positionMs() + deltaMs).coerceIn(0, durationMs)`.
- Si `durationMs` no es válido (`C.TIME_UNSET` o `<= 0`), se usa
  `coerceAtLeast(0)` y se delega el límite superior al player.
- Un salto hacia adelante cerca del final termina el episodio de forma normal
  (FR-010).

## Relaciones

- `HeadsetControlSettings` (1) → `HeadsetControlSettingsCache` (1) → consumidores
  (servicio, controlador, handler, ViewModels).
- `PlaybackContent` + `HeadsetCommand` + `SkipSource` +
  `HeadsetControlSettings` → `HeadsetAction` (decisión pura).
- `HeadsetAction.Skip` → `EpisodePlaybackController.seekTo` → player local o
  Cast.
- `MediaButtonLayout` → `CommandButton` de la notificación → comandos de sesión.

## Fuera del modelo

- No hay entidad de usuario, servidor, emisora ni episodio nueva.
- No se persiste el historial de gestos ni los saltos realizados.
- No se guardan posiciones de salto por podcast; el estado de posición sigue
  siendo del `EpisodePlaybackController` y del servidor.
