# Data Model: Control de volumen del dispositivo Chromecast

**Feature**: `0035-jlizquierdo-20260916-cast-volume-control` | **Date**: 2026-09-16

Todo el modelo es **estado de sesión en memoria** (no se persiste nada: el volumen real vive
en el receptor). Las entidades son Kotlin puro o adaptadores; no hay cambios de Room/DataStore
ni de contrato del backend.

## Entidades

### `PlaybackVolumeState` (estado agregado del controlador)

Representación observable única del volumen y silencio de la app. Expuesta por
`PlaybackVolumeController` como `StateFlow`s.

| Atributo | Tipo | Rango/Valores | Reglas |
|----------|------|---------------|--------|
| `muted` | `Boolean` | — | Fuente única de silencio (FR-007, FR-015). Se aplica al dispositivo activo y refleja el silencio del receptor leído en el eco. |
| `castVolume` | `Float` | `0f..1f` | Volumen real del receptor mientras hay Cast; valor por defecto `1f` sin sesión. |
| `castVolumeSupported` | `Boolean` | — | `false` solo tras detectar receptor sin soporte (D7); se resetea a `true` al conectar. |
| `remoteActive` | `Boolean` | — | `true` entre `bind()` y `unbind()` (sesión Cast conectada). |

**Invariantes**:

- `castVolume` siempre clampa a `[0f, 1f]`; los `NaN` se rechazan (se ignora el ajuste).
- Con `remoteActive = false`, los setters remotos son no-op (evita escribir en una sesión
  cerrada en carrera con la desconexión).
- `muted = true` no altera `castVolume`; al des-silenciar se restaura el nivel previo del
  dispositivo (delegado en `CastSession.setMute`).
- `castVolumeSupported = false` no cambia el estado de reproducción (FR-009, FR-012).

### `VolumeLevel` (valor normalizado)

Tipo de valor sin identidad propia; convención de mapeo entre capas:

| Capa | Unidad | Rango | Conversión |
|------|--------|-------|------------|
| UI (`Slider`) | `Float` | `0f..1f` | — |
| `Player` (dispositivo) | `Int` pasos | `0..100` | `round(volume * 100)` / `steps / 100f` |
| Receptor (`CastSession`) | `Double` | `0.0..1.0` | `steps / 100.0`; eco tolerante a ±1 paso (±0.01) |

### `RemoteVolumeDevice` (contrato del receptor)

Interfaz que abstrae el dispositivo remoto para poder testar el controlador sin Cast SDK.

| Operación | Firma | Reglas |
|-----------|-------|--------|
| Leer volumen | `readVolume(): Double?` | `null` si la sesión ya no está disponible. |
| Escribir volumen | `writeVolume(volume: Double)` | Clamp `[0,1]`; no lanza. |
| Leer silencio | `readMuted(): Boolean` | — |
| Escribir silencio | `writeMuted(muted: Boolean)` | No lanza. |
| Observar | `observe(onChanged: () -> Unit)` | `Cast.Listener.onVolumeChanged` → callback en hilo principal. |
| Dejar de observar | `stopObserving()` | Idempotente; se llama en `unbind()`. |

Implementación real: `CastSessionVolumeDevice` (envuelve `CastSession`); implementación de
test: doble MockK/fake.

### `CastDeviceVolumePlayer` (wrapper de `Player`)

No tiene estado propio: es una vista de `PlaybackVolumeState` + `RemoteVolumeDevice` hacia
media3. Contrato en `contracts/cast-volume.md`.

## Máquina de estados

### Salida activa (local ↔ Cast)

```text
        bind(remote)                         unbind() / pérdida de sesión
LOCAL ─────────────────► CAST ──────────────────────────────► LOCAL
  ▲                       │                                      │
  │  mute → exoPlayer     │  mute → session.setMute              │  mute → exoPlayer
  │  volumen: solo slider │  volumen: keys + slider + eco        │  castVolume se conserva
  └───────────────────────┴──────────────────────────────────────┘
```

- **`bind(remote)`**: `remoteActive = true`, `castVolume = remote.readVolume()`,
  `castMuted = remote.readMuted()`, `castVolumeSupported = true`, y aplica `muted` vigente al
  receptor (FR-015).
- **`unbind()`**: `remote.stopObserving()`, `remoteActive = false`, aplica `muted` al
  reproductor local (`exoPlayer.volume`), `castVolumeSupported = true`.
- La pérdida de sesión ya está resuelta por `CastFallbackPolicy` (spec 0031/0032): el volumen
  no interviene en la política de reanudación.

### Detección de soporte (D7)

```text
CONECTADO/soportado ──ajuste no confirmado + `VOLUME_CONFIRMATION_TIMEOUT_MS` (3 s) sin eco──► CONECTADO/no soportado
        ▲                                                          │
        └──────────────── bind() de nueva sesión ──────────────────┘
```

- Un eco equivalente (±1 paso) o una relectura equivalente confirma y mantiene el estado.
- Al pasar a `no soportado`: se emite **un único** aviso a la UI y se oculta el slider;
  el resto de la reproducción no se altera.

### Silencio

```text
SONANDO ──toggleMute()/MUTE──► SILENCIADO (recuerda nivel) ──toggleMute()/MUTE──► SONANDO
   ▲                                     │
   └── resetMute() (play/stop, spec 004) ◄┘    ajuste de volumen ⇒ des-silencia (FR-008)
```

## Relaciones con el código existente

- `PlayerState` (sellado, spec 004/011) **no cambia**: el volumen es ortogonal a
  `Idle/Buffering/Playing/Paused/Error` (FR-012).
- `CastPlayerState.Local/Cast` (spec 011) sigue gobernando qué reproductor controla la sesión;
  el controlador se enlaza en `CastPlayerManager.createCastPlayer()` y se desenlaza en
  `releaseCastPlayer()`.
- Los avisos de volumen no soportado viven en `PlaybackVolumeController.notices` (uno por
  sesión); el `PlayerViewModel` los reexpone y la UI los muestra sin bloquear.
