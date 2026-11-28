# Data Model: Visibilidad UI y navegación

**Feature**: `0061-izquierdojl-20261128-ui-visibilidad-navegacion` | **Date**: 2026-11-28

Sin entidades persistentes nuevas, sin migraciones Room, sin cambios en DataStore ni en credenciales. Todo lo que sigue son modelos de presentación en memoria y tokens de tema.

## Entidades reutilizadas (sin cambios)

- **Emisora** (`StationDto`): `StationDto.id`, `StationDto.name`, `StationDto.favicon`, `StationDto.country`, `StationDto.language`, `StationDto.tags`, `StationDto.codec`, `StationDto.bitrate`, `StationDto.url`. La tarjeta usa `StationDto.id`, `StationDto.name`, `StationDto.favicon`, `StationDto.country`, `StationDto.language`, `StationDto.tags` y `StationDto.bitrate`; el estado de reproducción se compara por `StationDto.id`.
- **Episodio** (`PodcastEpisode`): `PodcastEpisode.id`, `PodcastEpisode.title`, `PodcastEpisode.durationSec`, `PodcastEpisode.imageUrl`; el panel añade `PodcastSummary.title`, `EpisodePlayerUi.positionMs`, `EpisodePlayerUi.durationMs` y `EpisodePlayerUi.playing`.
- **Estado de reproducción** (`PlayerState`): `PlayerState.Idle`, `PlayerState.Buffering`, `PlayerState.Playing`, `PlayerState.Paused`, `PlayerState.Reconnecting`, `PlayerState.Error`. Es la fuente de verdad; la UI no lo modifica.
- **Estado Cast** (`CastPlayerState`): `CastPlayerState.Local(playerState)` o `CastPlayerState.Cast(playerState, deviceName, connectionState)`. El estado efectivo es el del Cast cuando está conectado.

## Modelos de presentación (nuevos, en memoria)

### `TolochaColors`

Data class inmutable con los tokens semánticos del tema. Se provee mediante `LocalTolochaColors` desde `TolochaTheme`.

| Campo | Tipo | Uso |
|-------|------|-----|
| `TolochaColors.isDark` | `Boolean` | Elegir elevación 0/2 dp y sombras |
| `TolochaColors.cardContainer` | `Color` | Fondo de tarjeta |
| `TolochaColors.cardBorder` | `Color` | Borde de 1 dp de tarjeta |
| `TolochaColors.cardPlaceholder` | `Color` | Fondo del hueco de carátula sin favicon |
| `TolochaColors.playerContainer` | `Color` | Fondo del panel de reproducción |
| `TolochaColors.playerBorder` | `Color` | Línea superior de 1 dp del panel |
| `TolochaColors.chromeDivider` | `Color` | Divisor de cabecera y barra inferior |
| `TolochaColors.navIndicator` | `Color` | Pill del destino seleccionado |
| `TolochaColors.navSelectedContent` | `Color` | Icono dentro del pill seleccionado |
| `TolochaColors.navUnselectedContent` | `Color` | Icono y etiqueta no seleccionados |

La etiqueta del destino seleccionado usa `MaterialTheme.colorScheme.onSurface` y peso semibold: el pill solo está detrás del icono, no del texto, así que `navSelectedContent` no sirve como color de etiqueta.

Valores:

| Token | Oscuro | Claro |
|-------|--------|-------|
| `TolochaColors.cardContainer` | `#24422E` (`Pine650`) | `#FFFFFF` |
| `TolochaColors.cardBorder` | `#3C6A4D` (`Pine500`) | `#C4D4C9` (`LightLineStrong`) |
| `TolochaColors.cardPlaceholder` | `#17291C` (`Pine800`) | `#D9E4DC` (`LightLine`) |
| `TolochaColors.playerContainer` | `#1B3121` (`Pine725`) | `#F5F9F6` (`LightSurfaceSoft`) |
| `TolochaColors.playerBorder` | `#3C6A4D` (`Pine500`) | `#C4D4C9` (`LightLineStrong`) |
| `TolochaColors.chromeDivider` | `#203A28` (`Pine700`) | `#D9E4DC` (`LightLine`) |
| `TolochaColors.navIndicator` | `#D3A568` (`Ochre400`) | `#8A5F26` (`Ochre700`) |
| `TolochaColors.navSelectedContent` | `#08100B` (`Pine950`) | `#FFFFFF` |
| `TolochaColors.navUnselectedContent` | `#8CB29A` (`Pine300`) | `#4C6B58` (`LightMuted`) |

`onSurfaceVariant` en oscuro pasa de `Pine400` a `Pine300`; en claro se mantiene `LightMuted`. Con ello, el texto secundario de tarjeta y el texto de chip cumplen AA sin tokens extra.

### `StationPlaybackUiState`

```text
Inactive | Playing | Paused | Loading | Error
```

- `StationPlaybackUiState.Inactive`: la emisora no es la activa o no hay reproducción.
- `StationPlaybackUiState.Playing`: `PlayerState.Playing` con el mismo `StationDto.id`.
- `StationPlaybackUiState.Paused`: `PlayerState.Paused` con el mismo `StationDto.id`.
- `StationPlaybackUiState.Loading`: `PlayerState.Buffering` o `PlayerState.Reconnecting` con el mismo `StationDto.id`.
- `StationPlaybackUiState.Error`: `PlayerState.Error` con el mismo `StationDto.id` (o error sin estación, que no pinta badge).

### `NowPlayingUi`

| Campo | Tipo | Regla |
|-------|------|-------|
| `NowPlayingUi.stationId` | `String?` | `null` en `PlayerState.Idle` o error sin estación |
| `NowPlayingUi.state` | `StationPlaybackUiState` | Derivado del estado efectivo |

Método puro `NowPlayingUi.stateFor(stationId): StationPlaybackUiState` que devuelve `StationPlaybackUiState.Inactive` si el id no coincide.

### `PlayerPanelUi` (derivado, no persistido)

| Campo | Tipo | Regla |
|-------|------|-------|
| `PlayerPanelUi.status` | `String` | `En directo`, `Pausado` o mensaje de error/carga/reconexión |
| `PlayerPanelUi.castDeviceName` | `String?` | Solo con Cast conectado; se añade a la línea de estado |

`PlayerPanelUi` se define como tipo privado en `PlayerUi.kt`. Las acciones de radio siguen en `MiniPlayerActions` y las de podcast en los callbacks de `EpisodeMiniPlayer`; no se crea un modelo de acciones duplicado.

No se introduce un `ViewModel` nuevo: el panel sigue leyendo `PlayerViewModel.state`, `PlayerViewModel.castState` y `PlayerViewModel.isMuted`.

## Mapeo de estados

| `PlayerState` efectivo | Tarjeta | Panel radio | Panel podcast |
|------------------------|---------|-------------|---------------|
| `PlayerState.Idle` | `StationPlaybackUiState.Inactive` | No se muestra | No se muestra |
| `PlayerState.Playing(station)` | `StationPlaybackUiState.Playing` si coincide | `En directo` + play/pausa + silenciar | Play/pausa en pausa visual, tiempos y progreso |
| `PlayerState.Paused(station)` | `StationPlaybackUiState.Paused` si coincide | `Pausado` + play/pausa + silenciar | Play/pausa en play visual |
| `PlayerState.Buffering(station)` | `StationPlaybackUiState.Loading` si coincide | Mensaje de carga + cancelar | Mensaje de carga, sin prometer sonido |
| `PlayerState.Reconnecting(station)` | `StationPlaybackUiState.Loading` si coincide | Mensaje de reconexión | Mensaje de reconexión |
| `PlayerState.Error(station, message)` | `StationPlaybackUiState.Error` si coincide | Mensaje + reintentar/editar servidor; sin silenciar | Mensaje + reintentar |
| `CastPlayerState.Cast(playerState, device)` | El del `playerState` | Igual + `· <device>` | Igual + dispositivo |

## Transiciones y reglas de validación

- **Tarjeta**: `StationPlaybackUiState.Inactive → StationPlaybackUiState.Playing` al pulsar play; `StationPlaybackUiState.Playing → StationPlaybackUiState.Paused` y `StationPlaybackUiState.Paused → StationPlaybackUiState.Playing` al pulsar el botón activo; `StationPlaybackUiState.Loading` no es accionable desde la tarjeta (se cancela desde el panel); `StationPlaybackUiState.Error` permite reintentar con `player.play(station)`.
- **Panel radio**: dos acciones en normal (play/pausa + silenciar); error conserva reintentar/editar servidor y oculta silenciar; carga conserva cancelar. Altura 64 dp; play 56 dp; silenciar 48 dp.
- **Panel podcast**: máximo dos líneas de texto; play 56 dp; saltos y Detener 48 dp; tiempos 10 sp; progreso 2-3 dp; altura 112 dp. Sin duración → solo transcurrido y sin barra.
- **Barra inferior**: 4 destinos en radio, 5 en podcast; etiqueta visible a una línea; seleccionada con indicador y peso; a 200 % de fuente no hay solapamiento.
- **Cabecera/barra**: divisor de 1 dp `chromeDivider`; sin altura extra.
- **Contraste**: texto normal ≥4.5:1; texto grande y controles/estados esenciales ≥3:1; borde de tarjeta ≥1.5:1; superficie de tarjeta ≥1.3:1 solo en oscuro.
- **Semántica**: badge con `contentDescription` (`En reproducción`, `Pausado`, `Cargando`, `Error`); icono de play con `contentDescription` (`Pausar`, `Reanudar`, `Reproducir`); sin duplicar el nombre de la emisora.
- **Datos**: ninguna escritura nueva; no se toca `StationDto`, `PlayerState`, `CastPlayerState`, Room ni DataStore.
