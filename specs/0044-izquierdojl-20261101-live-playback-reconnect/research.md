# Research: Reproducción continua y reconexión automática

**Feature**: `0044-izquierdojl-20261101-live-playback-reconnect`
**Date**: 2026-11-01

Este documento resuelve las decisiones técnicas de la fase 0. No quedan
`NEEDS CLARIFICATION`. Las decisiones de producto ya fijadas en `spec.md` son:
flujo continuo para todas las emisoras, HLS solo como ruta de reserva,
activación por bandera de compilación, 6 reintentos con espera
`0/1/2/4/8/15 s`, watchdog de 15 s, reinicio tras 60 s estables y límite de
2 minutos.

## R1. Reproducción progresiva del flujo continuo

**Decision**: para la ruta `CONTINUOUS`, construir siempre una
`ProgressiveMediaSource` con el `PlayerDataSourceFactory` autenticado. No fijar
`MediaItem.mimeType` en la reproducción local: el extractor se elige por el
`Content-Type` de la respuesta (`audio/aac`, `audio/mpeg`, `audio/aacp`) y por
la URI. Para Cast se conserva el `MediaItem` actual con URL pública y
`mimeType`, porque el receptor no usa el proxy.

**Rationale**: `GET /api/v1/playback/{id}/live` devuelve audio progresivo
chunked, sin `Content-Length` y sin manifiesto. `ProgressiveMediaSource` con
`DefaultExtractorsFactory` acepta ADTS/AAC y MP3; Media3 selecciona extractores
por cabeceras HTTP y URI, no por `MediaItem.mimeType`
(`DefaultExtractorsFactory.createExtractors`). Forzar
`application/x-mpegURL` haría que el player esperase un manifiesto y rompería
el caso principal. `DefaultMediaSourceFactory` también detecta por
`Content-Type`, pero la elección explícita evita que una respuesta HLS
inesperada active el camino de manifiesto.

**Alternatives considered**:
- `DefaultMediaSourceFactory` para todo: funciona, pero deja la decisión de
  motor al `Content-Type` y oculta el contrato de la ruta continua.
- `HlsMediaSource` para `/live`: incorrecto, porque la respuesta no es un
  manifiesto.
- `MediaPlayer` crudo: prohibido por la constitución (Media3).

**Note**: si el servidor devolviese un contenedor no soportado, el error se
clasifica como formato no compatible y se aplica R2; no se fuerza un MIME que
pueda ocultar el problema. La validación cubre los tres `Content-Type`
(`audio/aac`, `audio/mpeg`, `audio/aacp`) y respuestas chunked sin
`Content-Length`.

## R2. Detección de 415/404/405 y fallback a la ruta existente

**Decision**: extraer el código HTTP recorriendo la cadena de causas de
`PlaybackException` hasta encontrar `HttpDataSource.InvalidResponseCodeException`
(expone `responseCode` y `dataSpec.uri`). La clasificación es consciente de la
ruta activa:

| Ruta | Respuesta | Decisión |
|---|---|---|
| `CONTINUOUS` | 415 | Cambiar una sola vez a `LEGACY`; intentar la ruta de reserva. Si vuelve a fallar por formato/parser, `FAILED` con el mensaje de formato no admitido. |
| `CONTINUOUS` | 404/405 | Cambiar una sola vez a `LEGACY`; continuar con la política normal de reintentos. |
| `CONTINUOUS` | 401/403 | Renovar sesión; si no es posible, `FAILED(EDIT_SERVER)`. |
| `CONTINUOUS` | 5xx, red, timeout | Política normal de reintentos con la ruta continua. |
| `LEGACY` | 415 o parser/contenedor no soportado | `FAILED` con «Esta emisora usa un formato que este reproductor no admite». |
| `LEGACY` | 404/405/5xx | Errores normales de emisora/servidor; reintentos acotados y error accionable. |
| Cualquiera | Fallback ya intentado | No se vuelve a cambiar de ruta; se aplica el error correspondiente. |

No se hace una petición `HEAD` ni una preflight HTTP: la primera petición real
del player ya devuelve el código y evita una conexión adicional. El estado de
ruta y el flag `fallbackAttempted` viven en memoria durante la sesión de
reproducción; no se persisten. Un 404 de `/live` es ambiguo entre servidor
antiguo y emisora inexistente, pero la ruta `LEGACY` resuelve la ambigüedad:
si la emisora no existe, esa segunda petición devuelve el 404 real y se muestra
el error de emisora no disponible.

**Rationale**: `InvalidResponseCodeException` se construye con
`ERROR_CODE_IO_BAD_HTTP_STATUS` y conserva el `responseCode`; es la fuente fiable
para distinguir 415 de 401/404/503. El fallback único evita bucles deterministas
y cumple FR-003/FR-004. La ruta de reserva reutiliza exactamente el
`HlsMediaSource`/`ProgressiveMediaSource` ya probado en 0021/0042.

**Alternatives considered**:
- Preflight `HEAD`: el endpoint puede no soportar `HEAD`, añade latencia y no
  garantiza que la conexión de reproducción se comporte igual.
- Tratar todo 404 de `/live` como emisora no disponible: rompe la compatibilidad
  con servidores anteriores a 0.29.0.
- Reintentar 415 con la misma ruta: error determinista y bucle inútil.

## R3. Política de reconexión, watchdog y presupuesto de intentos

**Decision**: actualizar `RecoveryPolicy` a:

| Campo | Valor |
|---|---:|
| `maxAttempts` | 6 |
| `totalTimeoutMs` | 120 000 |
| `attemptTimeoutMs` | 15 000 |
| `stallThresholdMs` | 15 000 |
| `initialBufferTimeoutMs` | 15 000 |
| `backoffMs` | `[0, 1 000, 2 000, 4 000, 8 000, 15 000]` |

El presupuesto de intentos es **acumulado dentro de una ventana de estabilidad
de 60 s**: tras una recuperación correcta se marca `stableSince`; si pasan 60 s
reproduciendo sin incidencias, el contador vuelve a 0. Si hay otra interrupción
antes de esos 60 s, se consumen los intentos restantes. Cada secuencia de
reconexión conserva su propio límite de 2 minutos. Un reintento manual del
usuario inicia una secuencia nueva con presupuesto completo, porque es una
acción explícita y evita dejar al usuario sin salida tras un fallo persistente.
Un cambio de emisora reinicia ruta, presupuesto y ventana.

`hasPlayedOnce` se marca solo con `onIsPlayingChanged(true)`, no con
`STATE_READY`. Antes del primer audio, los reintentos se representan como
`CONNECTING` + «Reintentando…»; `RECONNECTING` se reserva para cortes después de
haber sonado. `STATE_ENDED` con emisora activa, sin pausa manual y sin Cast
inicia una reconexión con causa `STREAM_ENDED`. Un cambio de red válido inicia
una reconexión con causa `NETWORK_CHANGED`.

**Rationale**: cumple FR-006/FR-007 y el presupuesto de 2 minutos. La ventana de
60 s implementa el «reset tras 60 s de reproducción estable» sin reiniciar el
contador en cada corte seguido. El peor caso de una secuencia es
`30 s de esperas + 6 × 15 s de intento = 120 s`, justo el límite. El manager
recibe un `timeProvider` inyectable (por defecto `SystemClock.elapsedRealtime`)
para que los tests de la ventana de 60 s y del límite de 120 s usen tiempo
virtual sin depender del reloj real. La reconexión reutiliza el mismo
`ExoPlayer`, por lo que conserva el volumen, y crea un `MediaItem` nuevo para
volver al punto en directo.

**Alternatives considered**:
- Mantener 5 intentos/60 s de 0042: la spec 0044 los sustituye.
- Watchdog de 5 s: demasiado agresivo para un stream que bufferea.
- Reiniciar el contador en cada interrupción: no implementa el reset tras 60 s
  estables y permite bucles de 6 intentos cada pocos segundos.
- Reintentar 415 como un fallo normal: bucle determinista.

## R4. Dónde vive el supervisor

**Decision**: reutilizar `PlaybackRecoveryManager` como singleton con scope Main,
pero con ciclo de vida `start()`/`stop()` controlado por
`RadioPlaybackService`: el servicio inicia los colectores en `onCreate` y los
cancela en `onDestroy`; el `PlayerViewModel` solo observa su `StateFlow` y
delega acciones. Los colectores no arrancan en el constructor del manager. No se
crea un servicio ni un manager nuevos.

**Rationale**: el manager de 0042 ya sobrevive a la Activity y funciona con la
pantalla apagada; el servicio es el dueño del player y de la sesión multimedia.
Atar el arranque/parada al servicio cumple FR-015 sin duplicar el player ni el
ciclo de vida.

**Alternatives considered**:
- Manager en el ViewModel: muere al cerrar la Activity y no recupera en
  background.
- `WorkManager`: pensado para trabajos diferidos, no para mantener un stream
  activo.
- Servicio nuevo: YAGNI y duplicaría la sesión existente.

## R5. Bandera de activación por compilación

**Decision**: añadir `BuildConfig.LIVE_PLAYBACK_ENABLED`, alimentado por
`-PlivePlayback` o `TOLOCHA_LIVE_PLAYBACK`. Un `PlaybackFeatureFlags` inyectable
por Hilt expone el valor al dominio/aplicación. En las fases 1–2 el valor por
defecto de release es `false` y las compilaciones de desarrollo/CI lo activan
con la propiedad; en la fase 3 se cambia el valor por defecto de release a
`true`. No se añade DataStore, ajuste visible ni configuración remota.

**Rationale**: el usuario eligió una bandera de compilación. Es la opción más
simple para desplegar por fases y revertir publicando otra versión, sin estado
persistente ni backend nuevo (YAGNI).

**Alternatives considered**:
- Bandera remota: exige endpoint, caché, manejo de fallos y amplía el alcance.
- Ajuste interno en DataStore: añade estado y una vía de configuración no
  solicitada.
- Sin bandera: impide el despliegue por fases y la retirada gradual del camino
  anterior.

## R6. Renovación de token antes de reconectar

**Decision**: al guardar el access token, `SessionManager` intenta leer el claim
`exp` si el token es un JWT (Base64URL + `kotlinx.serialization`, sin verificar
la firma). Expone `isAccessTokenExpiring(skew = 60 s)`. Antes de cada intento de
reconexión, el manager llama a `AuthenticateServerUseCase` solo si el token está
a punto de caducar; ante un 401/403 el flujo es siempre renovar y, si falla,
`FAILED(EDIT_SERVER)`. Si el token es opaco o no tiene `exp`, no se hace refresh
proactivo y se confía en el 401.

**Rationale**: cumple FR-008 sin rotar el refresh en cada uno de los 6 intentos.
No añade dependencias: `java.util.Base64` y `kotlinx.serialization` ya están
presentes. La firma no se verifica porque el `exp` solo se usa para programar
una renovación local, no para autorizar.

**Alternatives considered**:
- Renovar siempre antes de cada intento: más latencia y rotaciones innecesarias.
- Renovar solo ante 401: no cumple el refresh preventivo.
- Añadir una librería JWT: dependencia nueva innecesaria.

## R7. Precheck de disponibilidad con presupuesto de 2 s

**Decision**: `PlayerViewModel.checkAvailability(station)` centraliza el
precheck de `/status` para la ficha y para `play`, con
`withTimeoutOrNull(2_000)`. Si responde `playable=false`, publica
`availability` con el motivo y no arranca el player; `play` lo convierte en
`Error` solo cuando el usuario intenta reproducir. Si `playable=true`, arranca;
si hay timeout o error de transporte, intenta la reproducción y mantiene
`CONNECTING`. El resultado se reutiliza si tiene menos de 5 s. La ficha
deshabilita o sustituye «Reproducir» cuando `playable=false`. La consulta no
bloquea la interfaz y se cancela al cambiar de emisora o cerrar la ficha.

**Rationale**: cumple FR-016/FR-017 y la asunción de que la comprobación es
informativa salvo un `playable=false` explícito. Reutiliza `PlaybackStatusReason`
para el motivo de `/status` y `PlaybackMessages` para el mensaje 415; evita
duplicar el timeout y la consulta entre ficha y play.

**Alternatives considered**:
- Mantener la consulta bloqueante actual: puede dejar la UI esperando.
- Cachear el estado: riesgo de mostrar información obsoleta.
- Omitir el precheck: peor experiencia ante emisoras caídas.

## R8. Diagnóstico estructurado

**Decision**: introducir `PlaybackFailure(cause, httpStatus, playerErrorCode)` y
extender `PlaybackRecoveryLogger` para registrar `event=recovery_attempt`,
`event=live_fallback`, `event=recovery_success` y `event=recovery_failed` con
causa, código HTTP, código de error del player, número de intento y `stationId`
hasheado. `live_fallback` se añade en la fase de fallback (Phase A); los campos
de intento/éxito/fallo se completan en Phase B. El mensaje 415 vive en
`PlaybackMessages`, no en `PlaybackStatusReason`. No se registran títulos, URLs
completas, tokens ni credenciales. El tag sigue siendo `PlaybackRecovery`.

**Rationale**: el servidor ya registra duración/bytes/cierre; con ambos lados se
localiza una incidencia. Los campos pedidos en 3.7 quedan cubiertos sin PII y
sin un canal de telemetría nuevo.

**Alternatives considered**:
- Volcar la excepción cruda: puede incluir URL o datos técnicos y no es
  estructurado.
- Enviar telemetría al servidor: fuera de alcance y con implicaciones de
  privacidad.

## R9. Estados de UI: conectar, reintentar y reconectar

**Decision**: ampliar `PlayerState.Buffering` con `retrying` y
`waitingForNetwork`. El estado `Buffering` representa `CONNECTING`; si
`retrying=true` muestra «Reintentando…», y si `waitingForNetwork=true` muestra
«Sin conexión. Se reanudará al volver la red.». `PlayerState.Reconnecting` se
usa solo después del primer audio. `PlayerState.Error` mantiene mensaje y acción
(`RETRY`/`EDIT_SERVER`). La notificación refleja el estado de transporte
(buffering/playing/paused), conserva los controles estándar y añade el botón
«Reintentar» en error; no duplica los textos detallados, que viven en
mini-player, reproductor completo y ficha. Los textos nuevos son en español y
los `contentDescription` existentes se amplían para los nuevos subtítulos y la
ficha.

**Rationale**: implementa la distinción acordada en clarify sin introducir un
estado nuevo que rompa el mapeo de Cast/`ActiveStationHolder`. Compose M3 ya
tiene los componentes de spinner, texto y botón.

**Alternatives considered**:
- Nuevo `PlayerState.Connecting`: más churn en UI, tests y mapeos.
- Usar siempre `Reconnecting`: confunde antes de que la emisora haya sonado.

## R10. Cambios de red Wi-Fi ↔ datos móviles

**Decision**: ampliar `ConnectivityMonitor` con `networkChanges: Flow<Unit>`. La
implementación Android emite cuando el callback de red detecta una red distinta
o un cambio de transporte, sin emitir en el registro inicial. El manager, si hay
emisora activa, reproducción solicitada, conectividad y no hay Cast/pausa/
recuperación en curso, inicia una reconexión con causa `NETWORK_CHANGED`. La
reconexión única en vuelo coalesce eventos repetidos.

**Rationale**: un cambio de red puede dejar la conexión anterior colgada sin que
el player emita error; el watchdog tardaría hasta 15 s. El callback existente ya
detecta disponibilidad y transporte sin polling ni permisos nuevos.

**Alternatives considered**:
- Confiar solo en el watchdog: recuperación más lenta.
- Polling periódico: batería y latencia.
- Detectar solo por `isOnline`: no distingue Wi-Fi de datos móviles.

## R11. Estrategia de pruebas

**Decision**: tests JVM de dominio para `RecoveryPolicy`, `PlaybackRoutePolicy`,
`PlaybackErrorClassifier` y `PlaybackFeatureFlags`; tests de
`StationMediaItemFactory` para URIs/MIME/tipo de `MediaSource` y los tres
`Content-Type`; tests del manager con `ExoPlayer`/`PlaybackEngine` falsos y un
`FakeConnectivityMonitor` con `networkChanges`; tests de `PlayerViewModel` para
precheck, reutilización <5 s, estados iniciales, volumen y punto en directo;
tests Compose de `StationDetailAvailabilityTest` y
`StationDetailPlaybackStateTest`; validación con streams reales en
`quickstart.md`.

**Rationale**: evita tests instrumentados frágiles de ExoPlayer y sigue el
patrón de 0042 (`RecoveryScheduler` + `TestScope`). No se añaden dependencias de
test.

**Alternatives considered**:
- Tests instrumentados del player real: lentos y dependientes de red.
- Mockear clases internas de Media3: frágil ante actualizaciones.

## R12. Persistencia

**Decision**: no se añade persistencia. La bandera vive en `BuildConfig`; la
ruta, el número de intento, `fallbackAttempted`, `hasPlayedOnce` y la ventana de
estabilidad viven en memoria durante la sesión de reproducción. El resultado 415
no se guarda por emisora (spec, Assumptions).

**Rationale**: evita estados obsoletos y cumple YAGNI. No hay migración de
DataStore ni de Room.

**Alternatives considered**:
- Persistir 415 por emisora: una actualización del backend quedaría bloqueada.
- Guardar la bandera en DataStore: no es una preferencia de usuario.
