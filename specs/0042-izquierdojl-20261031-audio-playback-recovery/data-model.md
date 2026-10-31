# Data Model: Continuidad de reproducción y buffer ajustable

**Feature**: `0042-izquierdojl-20261031-audio-playback-recovery`
**Date**: 2026-10-31

## Entidades

### 1. `BufferProfile`

Preferencia de comportamiento del buffer local.

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | enum | `DATA_SAVER`, `BALANCED`, `MAX_STABILITY` |
| `label` | UI | "Ahorro de datos", "Equilibrado", "Máxima estabilidad" |
| `description` | UI | Explica arranque, memoria/datos iniciales y tolerancia a cortes |
| `isDefault` | booleano | Solo `BALANCED` es el valor por defecto |

**Persistencia**: `buffer_profile` en DataStore, por nombre del enum. Un valor
desconocido o corrupto cae a `BALANCED` sin error.

### 2. `BufferProfileSpec`

Parámetros técnicos que se traducen a `DefaultLoadControl`.

| Campo | Tipo | Regla |
|---|---|---|
| `minBufferMs` | `Long` | > 0 y ≥ `bufferForPlaybackMs` |
| `maxBufferMs` | `Long` | ≥ `minBufferMs` |
| `bufferForPlaybackMs` | `Long` | > 0 |
| `bufferForPlaybackAfterRebufferMs` | `Long` | > 0 y ≤ 7 000 para cumplir SC-001 |

**Valores**:

| Perfil | min | max | playback | after rebuffer |
|---|---:|---:|---:|---:|
| `DATA_SAVER` | 10 000 | 30 000 | 1 000 | 2 000 |
| `BALANCED` | 20 000 | 50 000 | 2 000 | 4 000 |
| `MAX_STABILITY` | 40 000 | 60 000 | 6 000 | 7 000 |

**Invariante de producto**: `MAX_STABILITY.bufferForPlaybackMs -
BALANCED.bufferForPlaybackMs ≤ 5 000` (arranque adicional máximo de 5 s).

### 3. `PlaybackPreferences`

| Campo | Tipo | Default | Persistencia |
|---|---|---|---|
| `bufferProfile` | `BufferProfile` | `BALANCED` | DataStore `buffer_profile` |
| `resumeOnConnectivity` | `Boolean` | `true` | DataStore `resume_on_connectivity` |

**Reglas**:
- Ambos valores son locales al dispositivo; no se sincronizan con el servidor.
- `resumeOnConnectivity=false` no desactiva la recuperación mientras hay red:
  solo impide reanudar automáticamente al volver la conexión.
- El cambio de `bufferProfile` se persiste antes de solicitar la aplicación
  inmediata; si el servicio no está activo, se aplicará al siguiente arranque.

### 4. `InterruptionCause`

Clasificación de la causa de la interrupción.

| Valor | Origen | Respuesta |
|---|---|---|
| `NO_CONNECTIVITY` | `ConnectivityMonitor` | `WaitingForNetwork`, sin reintentos |
| `STALL` | watchdog de posición | reintento con backoff |
| `NETWORK_ERROR` | `PlaybackException` de red/timeout | reintento con backoff |
| `AUTH_ERROR` | HTTP 401/403 en la causa | `AuthenticateServerUseCase` y reintento; si falla, `EDIT_SERVER` |
| `STATION_UNAVAILABLE` | HTTP 404/503 | reintento acotado y error accionable |
| `DECODER_ERROR` | error de decodificación/parser | reintento acotado |
| `UNKNOWN` | cualquier otro error | reintento acotado |

### 5. `RecoveryPolicy`

| Campo | Valor | Regla |
|---|---:|---|
| `maxAttempts` | 5 | 1..5 |
| `totalTimeoutMs` | 60 000 | No superar 60 s desde el primer intento |
| `attemptTimeoutMs` | 7 000 | Ventana para considerar un intento fallido |
| `stallThresholdMs` | 5 000 | Solo tras la primera reproducción |
| `initialBufferTimeoutMs` | 10 000 | Carga inicial sin `READY`/`isPlaying` |
| `backoffMs` | `[0, 1 000, 2 000, 4 000, 8 000]` | Espera antes de cada intento |

**Cálculo del peor caso**:
- Corte tras reproducción: 5 s (detección) + 15 s (backoff) + 35 s (5 × 7 s) =
  **55 s**, dentro de 60 s.
- Carga inicial: 10 s + 15 s + 35 s = **60 s**, en el límite.

**Reglas de cancelación**:
- Pausa manual, stop, cambio de emisora, fin del temporizador de apagado o
  conexión Cast activa cancelan la recuperación.
- Un reintento manual inicia un ciclo nuevo de hasta 5 intentos/60 s.
- Sin conectividad no se consumen intentos ni se ejecuta backoff.

### 6. `RecoveryState`

Estado interno del manager, independiente de la UI.

| Estado | Campos | Significado |
|---|---|---|
| `Idle` | — | Sin recuperación en curso |
| `WaitingForNetwork` | `cause`, `stationId` | Sin red; esperando reconexión |
| `Reconnecting` | `stationId`, `attempt`, `maxAttempts`, `cause` | Intento en curso |
| `Failed` | `stationId`, `cause`, `message`, `action` | Se agotaron los intentos o no es recuperable |
| `PausedAfterLoss` | `stationId` | La red volvió pero el ajuste de reanudación está desactivado |

`RecoveryState` se expone como `StateFlow` y el `PlayerViewModel` lo combina con
el estado del player.

### 7. `PlayerErrorAction`

| Valor | Uso en UI |
|---|---|
| `RETRY` | Botón "Reintentar" en mini-player, reproductor completo y notificación |
| `EDIT_SERVER` | Acción "Editar servidor" para 401/403 no recuperables |

### 8. `PlayerState` (extensión)

Se mantiene el estado sellado existente y se añade:

| Estado | Campos | Notas |
|---|---|---|
| `Reconnecting` | `station`, `attempt`, `maxAttempts`, `waitingForNetwork` | `stationOrNull()` lo incluye |
| `Error` | `station`, `message`, `action` | `action` por defecto `RETRY` para no romper llamadas existentes |

**Mapeo a `ActiveStationHolder.PlayerStateType`**:

| `PlayerState` | Holder |
|---|---|
| `Reconnecting(waitingForNetwork=true/false)` | `BUFFERING` |
| `PausedAfterLoss` | `PAUSED` |
| `Error` | `ERROR` |
| `Playing` | `PLAYING` |
| `Paused` | `PAUSED` |
| `Idle` | `IDLE` |

No se añade un nuevo valor al enum del holder para no romper el mapeo de Cast;
la UI distingue "Reconectando" por el `RecoveryState`/`PlayerState`.

## Claves de DataStore

| Clave | Tipo | Default | Fallback |
|---|---|---|---|
| `buffer_profile` | `String` | `BALANCED` | `BALANCED` |
| `resume_on_connectivity` | `Boolean` | `true` | `true` |

No hay migración Room ni cambios en `tolocha.db`.

## Transiciones de estado

```text
Idle
  └─ play(station) ───────────────► Buffering
Buffering
  ├─ READY + playing ─────────────► Playing
  ├─ error/stall ≥ umbral ────────► Reconnecting
  └─ pausa/stop ──────────────────► Paused/Idle
Playing
  ├─ stall 5 s ───────────────────► Reconnecting
  ├─ onPlayerError ───────────────► Reconnecting / Error
  ├─ pausa manual ────────────────► Paused
  └─ stop/cambio/Cast ────────────► Idle/Paused
Reconnecting
  ├─ éxito ───────────────────────► Playing
  ├─ sin red ─────────────────────► WaitingForNetwork
  ├─ nuevo intento ───────────────► Reconnecting
  ├─ límite agotado ──────────────► Error(RETRY)
  ├─ auth no recuperable ─────────► Error(EDIT_SERVER)
  └─ pausa/stop/cambio/Cast ──────► Paused/Idle
WaitingForNetwork
  ├─ red vuelve + resume=true ────► Reconnecting
  ├─ red vuelve + resume=false ───► PausedAfterLoss
  └─ pausa/stop ──────────────────► Paused/Idle
Error
  ├─ Reintentar ──────────────────► Reconnecting
  ├─ Editar servidor ─────────────► navegación a servidores
  └─ stop/cambio ─────────────────► Idle
```

## Reglas de integridad

1. Una recuperación correcta no crea una entrada de historial nueva.
2. Un cambio de perfil mientras suena reconecta una sola vez y no duplica
   historial.
3. No puede haber dos ciclos de recuperación simultáneos; un `Mutex`/job único
   serializa los intentos.
4. Un reintento manual repetido no abre conexiones paralelas.
5. La recuperación no se ejecuta si `CastPlayerManager.isCastConnected` es true.
6. El estado `Error` siempre tiene un mensaje en español y una acción.
7. Los logs de recuperación no incluyen título, URL, token ni credenciales.
