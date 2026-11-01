# Data Model: Reproducción continua y reconexión automática

**Feature**: `0044-izquierdojl-20261101-live-playback-reconnect`
**Date**: 2026-11-01

## Entidades

### 1. `PlaybackRoute`

Ruta efectiva de reproducción de una emisora.

| Valor | URI | Motor | Uso |
|---|---|---|---|
| `CONTINUOUS` | `/api/v1/playback/{id}/live?r=N` | `ProgressiveMediaSource` | Ruta principal cuando la bandera está activa |
| `LEGACY` | `/api/v1/playback/{id}?r=N` | HLS si la emisora es `.m3u8`; progresivo en el resto | Fallback por 415, servidor antiguo o bandera desactivada |

**Reglas**:
- La ruta se decide por sesión de reproducción; no se persiste.
- `CONTINUOUS` solo se usa si `PlaybackFeatureFlags.livePlaybackEnabled` es
  `true`.
- `LEGACY` es la ruta de reserva y también la ruta principal cuando la bandera
  está desactivada.
- Cast no usa ninguna de estas rutas: conserva la URL pública de la emisora.

### 2. `PlaybackFeatureFlags`

Configuración de activación de la compilación.

| Campo | Tipo | Origen | Regla |
|---|---|---|---|
| `livePlaybackEnabled` | `Boolean` | `BuildConfig.LIVE_PLAYBACK_ENABLED` | `false` en release durante fases 1–2; `true` en debug/CI y en fase 3 |

**Persistencia**: ninguna. Es una constante de compilación inyectada por Hilt.

### 3. `PlaybackSession`

Estado en memoria de la reproducción activa.

| Campo | Tipo | Regla |
|---|---|---|
| `stationId` | `String` | UUID de radio-browser o `custom:<hex>` |
| `route` | `PlaybackRoute` | Ruta actual |
| `connectionSequence` | `Long` | Monótono; se usa como `?r=N` para evitar caché |
| `fallbackAttempted` | `Boolean` | Como máximo un cambio de `CONTINUOUS` a `LEGACY` |
| `hasPlayedOnce` | `Boolean` | `true` solo tras `onIsPlayingChanged(true)` |
| `usedAttempts` | `Int` | 0..6; presupuesto acumulado en la ventana de estabilidad |
| `stableSince` | `Long?` | `elapsedRealtime` del inicio de la reproducción estable |
| `currentAttempt` | `Int` | 1..6 dentro de la secuencia actual |
| `lastFailure` | `PlaybackFailure?` | Último error observado, para clasificar el intento |

**Ciclo de vida**: se crea al seleccionar una emisora, se reinicia al cambiar de
emisora o al pulsar "Reintentar" manualmente, y se descarta al detener.
`connectionSequence` sigue incrementándose aunque se reinicie el presupuesto;
así dos conexiones nunca comparten el mismo `?r`.

### 4. `PlaybackAttempt`

Intento de conexión concreto.

| Campo | Tipo | Regla |
|---|---|---|
| `sequence` | `Long` | Identificador anticaché; se envía como `?r=` |
| `number` | `Int` | 1..6; número acumulado que se muestra en la UI |
| `route` | `PlaybackRoute` | Ruta usada en este intento |
| `startedAt` | `Long` | `elapsedRealtime` |
| `failure` | `PlaybackFailure?` | Error del intento, si lo hubo |
| `result` | `SUCCESS` / `FAILURE` / `CANCELLED` | Resultado |

### 5. `PlaybackFailure`

Error normalizado de reproducción.

| Campo | Tipo | Origen |
|---|---|---|
| `cause` | `InterruptionCause` | Clasificación de dominio |
| `httpStatus` | `Int?` | `InvalidResponseCodeException.responseCode` |
| `playerErrorCode` | `Int?` | `PlaybackException.errorCode` |

**Causas** (`InterruptionCause`):

| Valor | Significado | Respuesta |
|---|---|---|
| `NO_CONNECTIVITY` | Sin red validada | `WaitingForNetwork`, sin reintentos |
| `STALL` | 15 s sin progreso | Reintento con backoff |
| `NETWORK_ERROR` | Error de red/timeout del player | Reintento con backoff |
| `NETWORK_CHANGED` | Cambio Wi-Fi ↔ datos móviles | Reintento inmediato único |
| `STREAM_ENDED` | El origen cerró la conexión | Reintento con backoff |
| `AUTH_ERROR` | 401/403 | Renovar sesión; si falla, `EDIT_SERVER` |
| `STATION_UNAVAILABLE` | 404/503 en ruta legacy | Reintento acotado y error accionable |
| `LIVE_ROUTE_UNAVAILABLE` | 404/405 en ruta continua | Fallback único a `LEGACY` |
| `UNSUPPORTED_FORMAT` | 415 o contenedor no soportado | Fallback único; si falla, mensaje de formato |
| `DECODER_ERROR` | Error de decodificación/parser | Reintento acotado |
| `UNKNOWN` | Cualquier otro error | Reintento acotado |

### 6. `RecoveryPolicy`

Política de reintentos y watchdog.

| Campo | Valor | Regla |
|---|---:|---|
| `maxAttempts` | 6 | 1..6 |
| `totalTimeoutMs` | 120 000 | Máximo 2 minutos por secuencia |
| `attemptTimeoutMs` | 15 000 | Ventana para considerar fallido un intento |
| `stallThresholdMs` | 15 000 | Sin avance de posición tras haber sonado |
| `initialBufferTimeoutMs` | 15 000 | Sin primer audio/READY en carga inicial |
| `backoffMs` | `[0, 1 000, 2 000, 4 000, 8 000, 15 000]` | Espera antes de cada intento |
| `stableWindowMs` | 60 000 | Reinicio del presupuesto tras reproducción estable |

**Cálculo del peor caso**: `0+1+2+4+8+15 = 30 s` de esperas +
`6 × 15 s = 90 s` de intentos = **120 s**.

**Reglas**:
- Cada secuencia de reconexión tiene su propio límite de 2 minutos.
- El presupuesto de 6 intentos se comparte dentro de la ventana de 60 s.
- Un reintento manual o un cambio de emisora reinicia el presupuesto.
- Sin conectividad no se consumen intentos ni se ejecuta backoff.
- Tras 60 s de reproducción estable, `usedAttempts` vuelve a 0.

### 7. `RecoveryState`

Estado interno del supervisor.

| Estado | Campos | Significado |
|---|---|---|
| `Idle` | — | Sin recuperación en curso |
| `WaitingForNetwork` | `stationId`, `cause`, `initial` | Sin red; esperando reconexión |
| `Reconnecting` | `stationId`, `attempt`, `maxAttempts`, `cause`, `initial` | Intento en curso |
| `Failed` | `stationId`, `cause`, `message`, `action` | Se agotaron los intentos o el error no es recuperable |
| `PausedAfterLoss` | `stationId` | La red volvió pero la reanudación está desactivada |

`initial = true` significa que la emisora aún no ha sonado. El ViewModel lo usa
para mostrar `CONNECTING` + «Reintentando…» en lugar de `RECONNECTING`.

### 8. `PlayerState` (extensión)

Se mantiene el estado sellado existente de 0042 y se amplía:

| Estado | Campos nuevos | Notas |
|---|---|---|
| `Buffering` | `station`, `retrying: Boolean = false`, `waitingForNetwork: Boolean = false` | Representa `CONNECTING`; los flags eligen el subtítulo |
| `Reconnecting` | `station`, `attempt`, `maxAttempts`, `waitingForNetwork`, `applying` | Solo después del primer audio o durante un cambio de perfil |
| `Error` | `station`, `message`, `action` | Acción `RETRY` o `EDIT_SERVER` |

**Mapeo a `ActiveStationHolder.PlayerStateType`**:

| `PlayerState` | Holder |
|---|---|
| `Buffering(retrying/waiting)` | `BUFFERING` |
| `Reconnecting` | `BUFFERING` |
| `Playing` | `PLAYING` |
| `Paused` | `PAUSED` |
| `Error` | `ERROR` |
| `Idle` | `IDLE` |

### 9. `StationAvailability`

Resultado del precheck de disponibilidad.

| Campo | Tipo | Regla |
|---|---|---|
| `stationId` | `String` | Emisora consultada |
| `playable` | `Boolean` | `false` bloquea el arranque con el motivo |
| `reason` | `String?` | Código del backend, traducido por `PlaybackStatusReason` |
| `checkedAt` | `Long` | `elapsedRealtime` de la respuesta; se usa para reutilizar el resultado durante 5 s |

**Resultado de la consulta**:
- `Playable`: arrancar.
- `NotPlayable(reason)`: mostrar el motivo y no arrancar.
- `Unknown` (timeout > 2 s o error de transporte): intentar la reproducción y
  mantener `CONNECTING`.

Se materializa en `PlayerViewModel.availability` y se comparte entre la ficha y
`play`; si el resultado tiene menos de 5 s se reutiliza y no se repite la
consulta. La ficha deshabilita o sustituye «Reproducir» cuando
`playable=false`. No se persiste.

### 10. `PlaybackEvent`

Evento de diagnóstico estructurado.

| Campo | Tipo | Regla |
|---|---|---|
| `type` | `ATTEMPT` / `FALLBACK` / `SUCCESS` / `FAILURE` | Tipo de evento |
| `stationHash` | `String` | `stationId.hashCode()` en hexadecimal; sin PII |
| `route` | `PlaybackRoute` | Ruta del intento |
| `attempt` | `Int` | Número de intento |
| `cause` | `InterruptionCause?` | Causa normalizada |
| `httpStatus` | `Int?` | Código HTTP, si existe |
| `playerErrorCode` | `Int?` | Código de `PlaybackException`, si existe |
| `durationMs` | `Long?` | Duración del intento o de la recuperación |

## Modelo de implementación

`PlaybackSession`, `PlaybackAttempt` y `PlaybackEvent` son modelos
conceptuales, no DTO públicos:

- `PlaybackSession`/`PlaybackAttempt`: campos internos del
  `PlaybackRecoveryManager` (ruta, secuencia, presupuesto, último fallo).
- `StationAvailability`: valor de dominio concreto que `PlayerViewModel`
  publica como `availability`, compartido entre ficha y play con TTL de 5 s.
- `PlaybackEvent`: campos que `PlaybackRecoveryLogger` escribe en el log.

No se persisten. `StationAvailability` sí es un valor público de `domain` y se
documenta con KDoc; `PlaybackSession`/`PlaybackAttempt`/`PlaybackEvent` no
requieren clases públicas adicionales.

## Persistencia

No se añade persistencia nueva:

| Dato | Dónde vive | Motivo |
|---|---|---|
| `livePlaybackEnabled` | `BuildConfig` | Bandera de compilación, no preferencia |
| Ruta, intentos, `fallbackAttempted`, `hasPlayedOnce` | Memoria del manager | Solo tienen sentido en la sesión activa |
| Resultado 415 | No se guarda | Evita bloquear una emisora tras una actualización |
| Token/credenciales | `EncryptedSharedPreferences` (sin cambios) | Contrato de autenticación existente |
| Buffer/reanudación | DataStore existente (sin cambios) | Spec 0042 |

No hay migración de Room ni de DataStore.

## Transiciones de estado

```text
Idle
  └─ play(station) ─────────────────────────────► Buffering (CONNECTING)
Buffering (CONTINUOUS)
  ├─ primer audio ──────────────────────────────► Playing
  ├─ error recuperable ─────────────────────────► Buffering(retrying)
  ├─ 415/404/405 ───────────────────────────────► Buffering(retrying, LEGACY)
  ├─ sin red ───────────────────────────────────► Buffering(waitingForNetwork)
  └─ límite agotado ────────────────────────────► Error(RETRY)
Playing
  ├─ stall 15 s / STATE_ENDED / error ──────────► Reconnecting
  ├─ cambio de red ─────────────────────────────► Reconnecting
  ├─ pausa manual ──────────────────────────────► Paused
  └─ stop / cambio / Cast ──────────────────────► Idle/Paused
Reconnecting
  ├─ éxito ─────────────────────────────────────► Playing
  ├─ nuevo intento ─────────────────────────────► Reconnecting
  ├─ sin red ───────────────────────────────────► WaitingForNetwork
  ├─ límite agotado ────────────────────────────► Error(RETRY)
  └─ auth no recuperable ───────────────────────► Error(EDIT_SERVER)
WaitingForNetwork
  ├─ red vuelve + resume=true ──────────────────► Reconnecting
  ├─ red vuelve + resume=false ─────────────────► PausedAfterLoss
  └─ pausa/stop ────────────────────────────────► Paused/Idle
Error
  ├─ Reintentar ────────────────────────────────► Reconnecting
  ├─ Editar servidor ───────────────────────────► navegación a servidores
  └─ stop/cambio ───────────────────────────────► Idle
```

## Reglas de integridad

1. Solo puede haber una reconexión en vuelo; los eventos repetidos se agrupan.
2. Solo se permite un cambio de ruta `CONTINUOUS → LEGACY` por sesión.
3. Un 415 nunca se reintenta en bucle; si el fallback legacy falla por formato,
   se muestra el mensaje y se detiene.
4. Una recuperación correcta no crea una entrada nueva de historial.
5. No se reconecta si el usuario pausó/detuvo, cambió de emisora, venció el
   temporizador de apagado, hay Cast activo o la interrupción es foco/llamada/
   auriculares.
6. `hasPlayedOnce` solo se activa cuando el player está realmente reproduciendo.
7. El presupuesto se reinicia tras 60 s estables, un reintento manual o un
   cambio de emisora.
8. `?r=` es único por conexión y no contiene credenciales ni PII.
9. Los logs usan `stationHash`; nunca título, URL, token ni credenciales.
10. El estado `Error` siempre tiene mensaje en español y acción.

## Trazabilidad

Cubre FR-001..FR-025 y SC-001..SC-010. Los estados y la política se alinean con
las clarificaciones de `spec.md` (HLS solo como reserva, bandera de compilación
y reintentos desde el primer fallo).
