# Contract: Control de volumen del dispositivo Cast

**Feature**: `0035-jlizquierdo-20260916-cast-volume-control` | **Date**: 2026-09-16

Contrato interno de la feature (app Android, sin API externa). Define las fronteras entre UI,
controlador, adaptador Cast y la `MediaSession` de media3, más el contrato de UI. Referencias:
[data-model.md](../data-model.md), [research.md](../research.md).

---

## 1. Contrato `Player` de la sesión (`CastDeviceVolumePlayer`)

El `MediaSession` de `RadioPlaybackService` recibe este wrapper mientras hay sesión Cast
(`session.setPlayer(wrapper)` en lugar de `CastPlayer`). Todo lo no listado se delega en el
`CastPlayer` subyacente (delegación `Player by delegate`).

| Miembro | Contrato |
|---------|----------|
| `getDeviceInfo()` | `PLAYBACK_TYPE_REMOTE` + `maxVolume = 100` mientras `castVolumeSupported = true` (dispara el `VolumeProviderCompat` remoto y la barra del sistema). Si el controlador marca no soportado, pasa a `PLAYBACK_TYPE_LOCAL`: `CastPlayerManager` reinstala el player de la sesión para devolver las teclas al volumen del móvil. |
| `getAvailableCommands()` | Comandos del delegado + `COMMAND_GET_DEVICE_VOLUME`, `COMMAND_SET_DEVICE_VOLUME`, `COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS`, `COMMAND_ADJUST_DEVICE_VOLUME`, `COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS`. Sin ellos el provider queda `VOLUME_CONTROL_FIXED` (síntoma actual). |
| `getDeviceVolume(): Int` | `0..100` desde `PlaybackVolumeController.castVolume`. |
| `setDeviceVolume(volume: Int, flags: Int)` | Delega en el controlador (clamp, ~1 paso). Nunca lanza. |
| `setDeviceVolume(volume: Int)` (deprecado) | Delegado al anterior. |
| `increaseDeviceVolume(flags)` / `decreaseDeviceVolume(flags)` | ±1 paso en el controlador. |
| `isDeviceMuted(): Boolean` | `PlaybackVolumeController.castMuted`. |
| `setDeviceMuted(muted: Boolean, flags)` / `setDeviceMuted(muted)` | Delega en el controlador. |
| `addListener` / `removeListener` | Registra además en la lista propia del wrapper para poder emitir eventos sintéticos; siempre reenvía al delegado. |
| `emitDeviceVolumeChanged(volume: Int, muted: Boolean)` (no es API `Player`) | Emite `Player.Listener.onDeviceVolumeChanged` a los listeners registrados, en hilo principal. Lo invoca `CastPlayerManager` al observar el estado del controlador; permite que la barra del sistema refleje eco y confirmaciones. |

**Errores**: ninguna operación lanza; con `remoteActive = false` son no-op (carrera de
desconexión). Los fallos del receptor se manifiestan como falta de eco (D7), no como excepción.

**Hilos**: todas las invocaciones ocurren en el hilo principal (SystemUI/provider y Cast SDK
entregan callbacks en main); el wrapper no hace trabajo bloqueante.

---

## 2. Contrato `PlaybackVolumeController` (`@Singleton`)

Consumidores: `PlayerViewModel` (UI), `RadioPlaybackService` (comando MUTE),
`CastPlayerManager` (bind/unbind).

| Miembro | Firma | Contrato |
|---------|-------|----------|
| `muted` | `StateFlow<Boolean>` | Estado de silencio único de la app. |
| `castVolume` | `StateFlow<Float>` | `0f..1f`; último valor real conocido del receptor. |
| `castVolumeSupported` | `StateFlow<Boolean>` | `false` tras detectar receptor sin soporte; `true` al conectar. |
| `bind(device: RemoteVolumeDevice)` | — | Enlaza sesión, lee volumen/silencio reales, aplica `muted` al receptor (FR-015), resetea soporte. |
| `unbind()` | — | Desenlaza, aplica `muted` al reproductor local, cancela la ventana de detección. |
| `setCastVolume(volume: Float)` | — | Clamp `[0,1]`; si `muted`, des-silencia primero (FR-008); escribe en el receptor y actualiza `castVolume`. |
| `stepCastVolume(delta: Int)` | — | ±1 paso (`delta` en pasos, `0..100`); usado por las teclas de la sesión. |
| `toggleMute()` | — | Alterna `muted` y lo aplica al dispositivo activo (Cast o ExoPlayer). |
| `resetMute()` | — | `muted = false` aplicado al dispositivo activo (regla spec 004 en play/stop). |
| `setMuted(muted: Boolean)` | — | Fija el silencio de la salida activa (lo usan ADJUST_MUTE/UNMUTE/TOGGLE del sistema vía el `Player` de la sesión). |
| `deviceVolumePercent(): Int` | — | Volumen en pasos `0..100` para `Player.getDeviceVolume()`. |
| `setDeviceVolumePercent(percent: Int)` | — | Aplica el volumen en pasos `0..100` recibido de `Player.setDeviceVolume()`. |
| `isDeviceMuted(): Boolean` | — | Silencio vigente para `Player.isDeviceMuted()`. |

**Precondiciones**: métodos llamados en hilo principal. **Postcondiciones**: el estado
observable refleja el último valor confirmado por el receptor; nunca se bloquea ni se deja el
stream en pausa (FR-012).

**Notificaciones de error (FR-009/FR-014)**: la detección de no soportado se expone como
estado (`castVolumeSupported`) más un aviso único consumible por la UI; los fallos
transitorios se autocorrigen sin aviso.

---

## 3. Contrato de UI

### Full-player (`FullPlayerSheet`)

- El slider de volumen **solo** se muestra con sesión Cast (`castState is Cast`) y
  `castVolumeSupported = true`.
- Valor: `castVolume` observado (nunca un `remember` local); `onValueChange` llama a
  `setCastVolume(it)` de forma continua (FR-004).
- Etiqueta: "Volumen" + nombre del dispositivo Cast (ya disponible en `CastPlayerState.Cast`).
- Si `castVolumeSupported = false`: se oculta el slider y se muestra el aviso
  "Este dispositivo no permite ajustar el volumen desde el móvil" (no bloqueante).
- Accesibilidad: `contentDescription` del slider "Volumen del dispositivo {nombre}" y
  `stateDescription` con el porcentaje; el icono de volumen refleja `> 0`.

### Mini-player y notificación

- El botón de silencio usa `muted` del controlador y `toggleMute()` (mismo estado que el
  full-player).
- El comando `CUSTOM_COMMAND_MUTE` de `RadioPlaybackService` delega en `toggleMute()` del
  controlador (afecta a Cast cuando hay sesión; a ExoPlayer en local).
- La notificación no añade controles de volumen (clarificación Q1).

### Resto de la app

- Sin Cast conectado, el comportamiento local no cambia (slider oculto, silencio local).
- Al conectar/desconectar Cast, el silencio vigente se conserva en la nueva salida (FR-015) y
  el botón lo refleja sin parpadeos.

---

## 4. Fuera de contrato (no implementado)

- Persistencia de volumen por dispositivo (el receptor conserva su propio volumen).
- Slider o botones de volumen en la notificación.
- Control de volumen por altavoz en grupos multi-room (se usa el volumen del grupo/receptor).
- `RemoteMediaClient.setStreamVolume` y volumen por stream.
