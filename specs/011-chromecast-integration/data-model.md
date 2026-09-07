# Data Model: Chromecast Integration

**Feature**: 011-chromecast-integration
**Date**: 2026-09-07

## Entities

### CastConnectionState (Enum)

Representa el estado de la conexión con un dispositivo Chromecast.

| Value | Description |
|-------|-------------|
| `DISCONNECTED` | No hay conexión activa con ningún Chromecast |
| `CONNECTING` | Conexión en proceso (dispositivo seleccionado, sesión iniciando) |
| `CONNECTED` | Conexión activa, audio reproduciéndose en Chromecast |
| `RECONNECTING` | Conexión perdida, intentando reconectar automáticamente |

**State transitions**:
```
DISCONNECTED → CONNECTING (usuario selecciona dispositivo)
CONNECTING → CONNECTED (sesión Cast establecida)
CONNECTING → DISCONNECTED (error de conexión)
CONNECTED → DISCONNECTED (usuario desconecta o pérdida de red)
CONNECTED → RECONNECTING (pérdida temporal de red)
RECONNECTING → CONNECTED (reconexión exitosa)
RECONNECTING → DISCONNECTED (reconexión fallida o timeout)
```

---

### CastDeviceInfo (Data class)

Información del dispositivo Chromecast descubierto.

| Field | Type | Description |
|-------|------|-------------|
| `deviceId` | `String` | ID único del dispositivo (del SDK Cast) |
| `name` | `String` | Nombre del dispositivo (ej: "Living Room TV") |
| `deviceType` | `CastDeviceType` | Tipo de dispositivo |
| `isConnected` | `Boolean` | Si está actualmente conectado |

---

### CastDeviceType (Enum)

Tipo de dispositivo Chromecast.

| Value | Description |
|-------|-------------|
| `CHROMECAST` | Chromecast dongle |
| `AUDIO` | Chromecast Audio / Google Home / Nest |
| `TV` | Smart TV con Chromecast integrado |
| `UNKNOWN` | Tipo no reconocido |

---

### CastPlayerState (Sealed Interface)

Estado de reproducción cuando el audio está en Chromecast. Extiende el concepto de `PlayerState` existente.

| State | Fields | Description |
|-------|--------|-------------|
| `Local` | `playerState: PlayerState` | Reproduciendo localmente (estado actual de la app) |
| `Cast` | `playerState: PlayerState, deviceName: String, connectionState: CastConnectionState` | Reproduciendo en Chromecast |

**Nota**: El `PlayerState` existente (Idle, Buffering, Playing, Paused, Error) se mantiene igual. `CastPlayerState` es un wrapper que indica si la reproducción es local o remota, y añade metadata del dispositivo.

---

## Relationships

```
PlayerViewModel
    ├── observes: CastPlayerState
    └── uses: CastPlayerManager

CastPlayerManager (@Singleton)
    ├── owns: ExoPlayer (local)
    ├── owns: CastPlayer? (remoto, nullable)
    ├── observes: CastConnectionState
    └── manages: MediaSession.setPlayer(activePlayer)

RadioPlaybackService
    ├── uses: MediaSession
    └── player: Player (dynamic — local or Cast)
```

---

## Validation Rules

| Rule | Description |
|------|-------------|
| VR-001 | Solo puede haber una conexión Cast activa a la vez |
| VR-002 | El precheck `playable` debe pasar antes de transferir a Cast |
| VR-003 | Al desconectar, la emisora actual debe ser la misma que se estaba reproduciendo en Cast |
| VR-004 | El volumen del Cast se controla independientemente del volumen local |
| VR-005 | La MediaSession debe reflejar el player activo (local o Cast) en todo momento |

---

## Data Flow: Transfer Audio to Chromecast

```
1. User taps Cast button
2. Cast SDK shows device picker
3. User selects device
4. CastPlayerManager.connect(device)
   → CastConnectionState.CONNECTING
5. Cast SDK establishes session
6. CastPlayerManager creates CastPlayer
7. Transfer state: ExoPlayer → CastPlayer
   - Current media item (station URL)
   - Playback state (playing/buffering)
   - Metadata (station name, artwork)
8. MediaSession.setPlayer(CastPlayer)
9. ExoPlayer.stop()
   → CastConnectionState.CONNECTED
10. PlayerViewModel updates UI with Cast state
```

## Data Flow: Disconnect from Chromecast

```
1. User taps Cast button → "Disconnect" OR connection lost
2. CastPlayerManager.disconnect()
   → CastConnectionState.DISCONNECTED
3. Transfer state: CastPlayer → ExoPlayer
   - Current station (from ActiveStationHolder)
   - Resume playback
4. MediaSession.setPlayer(ExoPlayer)
5. CastPlayer released
6. PlayerViewModel updates UI (local state)
```
