# Implementation Plan: Reproducción continua y reconexión automática

**Branch**: `0044-izquierdojl-20261101-live-playback-reconnect` | **Date**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0044-izquierdojl-20261101-live-playback-reconnect/spec.md`

## Summary

Adoptar el flujo continuo `GET /api/v1/playback/{stationId}/live` para todas las
emisoras, reproducirlo como audio progresivo con Media3 y mantener el camino
HLS únicamente como reserva para emisoras 415 o servidores anteriores a 0.29.0.
Sobre el supervisor existente de la spec 0042 se ajusta la política a 6
intentos (`0/1/2/4/8/15 s`), watchdog de 15 s, presupuesto compartido durante
60 s de estabilidad y límite de 2 minutos; se añaden reintentos desde el primer
fallo, detección de `STATE_ENDED`, cambios de red, precheck de disponibilidad de
2 s, refresh de token antes de reconectar, estados de UI diferenciados y
diagnóstico estructurado. La activación se controla con una bandera de
compilación, sin configuración remota ni ajuste visible.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JVM target 17.

**Primary Dependencies**: AndroidX Media3 1.11.0 (`media3-exoplayer`,
`media3-exoplayer-hls`, `media3-session`, `media3-datasource-okhttp`), Hilt
2.60.1, Compose BOM 2026.05.00 + Material 3, DataStore Preferences 1.1.1,
Coroutines/Flow, Retrofit 2.11.0, OkHttp 4.12.0, JUnit 4, Turbine 1.1.0, MockK
1.13.12 y Compose UI Test.

**Storage**: sin persistencia nueva. La bandera vive en
`BuildConfig.LIVE_PLAYBACK_ENABLED`; la ruta, el número de intento, el flag de
fallback y la ventana de estabilidad viven en memoria. No hay cambios en
DataStore ni en Room.

**Testing**: JUnit 4 + `kotlinx-coroutines-test` + Turbine + MockK para dominio,
datos y ViewModels; Compose UI Test para los estados del reproductor; gates
`assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck`, `lintDebug` y
`connectedDebugAndroidTest` cuando haya dispositivo.

**Target Platform**: Android `minSdk` 26, `compileSdk`/`targetSdk` 37.

**Project Type**: aplicación Android de un solo módulo Gradle (`app`).

**Performance Goals**: precheck de disponibilidad ≤2 s; watchdog de 15 s;
máximo 6 intentos y 120 s por secuencia; recuperación de un corte de red de
10 s en menos de 20 s; 95% de sesiones HLS de 10 minutos sin corte atribuible a
la caducidad del origen; 90% de sesiones de 30 minutos sin interrupciones
visibles.

**Constraints**: reproducción en segundo plano vía `MediaSessionService`;
HTTPS-only; sin credenciales ni PII en logs; sin dependencias nuevas; sin
cambios en el contrato de autenticación ni en el backend; Chromecast fuera del
alcance de `/live`; HLS conservado solo como reserva; activación por bandera de
compilación; textos visibles en español.

**Scale/Scope**: un stream local activo por dispositivo, dos rutas
(`CONTINUOUS`/`LEGACY`), cuatro estados visibles, seis intentos y un precheck de
2 s. Sin módulos Gradle nuevos.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Cumplimiento | Evidencia en el plan |
|---|---|---|
| I. MVVM + Clean | PASS | `PlaybackRoute`, `PlaybackRoutePolicy`, `PlaybackFailure`, `RecoveryPolicy` y `PlaybackFeatureFlags` son dominio puro; la integración con Media3 vive en `feature/player`; `SessionManager`/`ConnectivityMonitor` en `core`; el ViewModel no usa `Context` de Activity y Hilt provee las dependencias. |
| II. Kotlin + Compose M3 + Media3 | PASS | Se usa `ProgressiveMediaSource`/`HlsMediaSource` de Media3, Compose M3 para los estados y no se añaden XML/Views ni dependencias nuevas. |
| III. Calidad Test-First | PASS | Tests Red-Green de política, ruta, clasificador, URLs, factory, manager, sesión, ViewModel y Compose; gates completos en CI. |
| IV. Streaming robusto | PASS | Estados sellados, backoff acotado, `STATE_ENDED`, cambio de red, 401/403, precheck, logs estructurados sin PII, KDoc en APIs públicas de `domain`/`data` y funcionamiento en background. |
| V. Simplicidad modular (YAGNI) | PASS | Se reutilizan el manager, el servicio, el player, DataStore y el datasource existentes. La bandera de compilación es la opción más simple para el despliegue por fases. Sin módulos ni librerías nuevas. |
| Quality Gates | PASS | `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck`, `lintDebug` y pruebas conectadas de UI. |

**Post-design check (Phase 1)**: PASS. No hay violaciones que requieran
excepciones constitucionales. La máquina de estados de ruta y el presupuesto de
60 s son complejidad de producto exigida por FR-003/FR-007/FR-021, no una
abstracción prematura; se documentan en `Complexity Tracking`.

## Project Structure

### Documentation (this feature)

```text
specs/0044-izquierdojl-20261101-live-playback-reconnect/
├── plan.md              # Este archivo
├── research.md          # Fase 0
├── data-model.md        # Fase 1
├── quickstart.md        # Fase 1
├── contracts/
│   ├── playback-live-api.md
│   └── playback-live-ui.md
├── checklists/
│   └── requirements.md
└── tasks.md             # Fase 2 generada (/speckit.tasks)
```

### Source Code (repository root)

```text
app/build.gradle.kts                         # BuildConfig.LIVE_PLAYBACK_ENABLED

app/src/main/java/com/izquierdojl/tolocharadio/
├── core/network/
│   ├── ConnectivityMonitor.kt               # + networkChanges
│   └── AndroidConnectivityMonitor.kt        # emite cambios de red/transporte
├── core/session/
│   └── SessionManager.kt                    # exp del JWT + isAccessTokenExpiring
├── data/remote/api/
│   └── PlaybackApi.kt                       # liveStreamUrl()/streamUrl() con ?r=
├── di/
│   └── PlayerModule.kt                      # provee PlaybackFeatureFlags
├── domain/playback/
│   ├── InterruptionCause.kt                 # + STREAM_ENDED, NETWORK_CHANGED, ...
│   ├── PlaybackFailure.kt                   # causa + HTTP + errorCode
│   ├── PlaybackFeatureFlags.kt              # bandera de compilación inyectable
│   ├── PlaybackMessages.kt                  # textos de error 415 y recuperación
│   ├── PlaybackRoute.kt                     # CONTINUOUS | LEGACY
│   ├── PlaybackRoutePolicy.kt               # decisión pura de fallback
│   ├── PlaybackStatusReason.kt              # motivos de /status; sin 415
│   ├── RecoveryPolicy.kt                    # 6 intentos, 15 s, 120 s, 60 s
│   ├── RecoveryState.kt                     # + initial
│   └── StationAvailability.kt               # playable/reason/checkedAt
├── feature/explore/
│   └── StationDetail.kt                     # StationDetailScreen + StationDetailContent
└── feature/player/
    ├── ActiveStationHolder.kt               # mapeo de estados sin cambios de enum
    ├── LocalPlaybackStarter.kt              # route/sequence/attempt
    ├── PlaybackErrorClassifier.kt           # causa + httpStatus + playerErrorCode
    ├── PlaybackClock.kt                    # reloj inyectable del supervisor
    ├── PlaybackFallbackController.kt        # fallback único 415/404/405
    ├── PlaybackRecoveryLogger.kt            # attempt/fallback/success/failure
    ├── PlaybackRecoveryManager.kt           # ruta, presupuesto, watchdog, estado
    ├── PlayerUi.kt                          # CONNECTING/RECONNECTING/FAILED
    ├── PlayerViewModel.kt                   # precheck 2 s, mapeo de estados
    ├── RadioPlaybackService.kt              # dueño del supervisor y retry
    └── StationMediaItemFactory.kt           # live/legacy + MediaSource

app/src/test/java/com/izquierdojl/tolocharadio/
├── core/network/FakeConnectivityMonitor.kt  # + networkChanges
├── core/session/SessionManagerTest.kt       # expiración JWT
├── domain/playback/
│   ├── PlaybackFeatureFlagsTest.kt
│   ├── PlaybackMessagesTest.kt
│   ├── PlaybackRoutePolicyTest.kt
│   └── RecoveryPolicyTest.kt                # valores 0044
└── feature/player/
    ├── PlaybackErrorClassifierTest.kt       # 415, 404/405, causa anidada
    ├── PlaybackFallbackControllerTest.kt    # fallback único y ruta legacy
    ├── PlaybackRecoveryLoggerTest.kt        # logs estructurados sin PII
    ├── StationMediaItemFactoryTest.kt       # URIs, MIME, MediaSource
    ├── PlaybackRecoveryManagerTest.kt       # fallback, 60 s, STATE_ENDED
    └── PlayerViewModelTest.kt               # precheck, estados iniciales

app/src/androidTest/java/com/izquierdojl/tolocharadio/
├── feature/explore/StationDetailAvailabilityTest.kt
├── feature/explore/StationDetailPlaybackStateTest.kt
└── feature/player/RecoveryUiTest.kt         # textos y acciones de estado
```

**Structure Decision**: se mantiene el módulo único `app` y la separación por
features existente. Las reglas puras (ruta, política, clasificación de causa) se
extraen a `domain/playback`; la integración con Media3, el servicio y la UI vive
en `feature/player`; la conectividad y la sesión en `core`; las URLs en
`data/remote/api`. No se crean módulos Gradle.

## Phase 0: Outline & Research

Las decisiones completas están en [research.md](./research.md). Resumen:

- **R1**: `ProgressiveMediaSource` explícita para `/live`; sin `mimeType` local;
  el extractor se elige por `Content-Type`/URI. Cast conserva su ruta pública.
- **R2**: detección de 415/404/405 por la cadena de causas de
  `PlaybackException`; fallback único y consciente de la ruta, sin preflight.
- **R3**: `RecoveryPolicy` de 6 intentos, 15 s de watchdog, 120 s por secuencia,
  presupuesto acumulado y reset tras 60 s estables; `STATE_ENDED` y cambios de
  red; reintentos iniciales en `CONNECTING`.
- **R4**: supervisor singleton con `start()`/`stop()` controlados por
  `RadioPlaybackService`; el ViewModel solo observa y delega.
- **R5**: bandera `BuildConfig.LIVE_PLAYBACK_ENABLED` + `PlaybackFeatureFlags`;
  sin configuración remota ni DataStore.
- **R6**: `exp` del JWT en `SessionManager`; refresh proactivo solo si caduca en
  ≤60 s; ante 401/403, refresh/relogin.
- **R7**: precheck de `/status` con `withTimeoutOrNull(2 s)` en play y al abrir
  la ficha; solo un `playable=false` explícito bloquea.
- **R8**: `PlaybackFailure` + logs con causa, HTTP, errorCode e intento; sin PII;
  el mensaje 415 vive en `PlaybackMessages`.
- **R9**: `PlayerState.Buffering` con `retrying`/`waitingForNetwork`;
  `Reconnecting` solo después del primer audio; la notificación refleja el
  estado de transporte y no duplica textos.
- **R10**: `ConnectivityMonitor.networkChanges`; reconexión única al cambiar de
  red.
- **R11**: tests JVM con player/engine falsos, tests de `PlayerViewModel` para
  precheck/cache y Compose de `StationDetailContent` mediante
  `StationDetailAvailabilityTest` y `StationDetailPlaybackStateTest`; quickstart
  para streams reales.
- **R12**: sin persistencia nueva; 415 no se guarda por emisora.

## Phase 1: Design & Contracts

- [data-model.md](./data-model.md): rutas, sesión, intentos, fallos, política,
  estados, precheck, eventos y transiciones.
- [contracts/playback-live-api.md](./contracts/playback-live-api.md): endpoints,
  autenticación, `Content-Type`, errores 401/404/405/415/503, marcador `?r=` y
  compatibilidad con servidores antiguos.
- [contracts/playback-live-ui.md](./contracts/playback-live-ui.md): estados
  visibles, textos, acciones, notificación, accesibilidad y consistencia.
- [quickstart.md](./quickstart.md): escenarios manuales, comandos `adb` y
  protocolos medibles para SC-001..SC-008.

## Implementation Phases

Las fases técnicas se alinean con el despliegue por fases de la spec: la fase 1
no cambia los reintentos, el watchdog ni los estados de 0042; el único punto que
toca del manager es el hook de fallback, que se ejecuta antes de su recuperación.
La fase 2 aplica el supervisor nuevo y la UI.

### Phase A - Ruta continua y fallback (fase 1)

1. Crear `PlaybackRoute`, `PlaybackFeatureFlags` y `PlaybackFailure`; ampliar
   `InterruptionCause` con `LIVE_ROUTE_UNAVAILABLE` y `UNSUPPORTED_FORMAT`
   (las causas `STREAM_ENDED`/`NETWORK_CHANGED` pueden declararse ya, pero no
   cambian el comportamiento de 0042 en esta fase).
2. Crear `PlaybackRoutePolicy` con la decisión pura de fallback:
   - `CONTINUOUS + 415 + !fallbackAttempted → LEGACY` con un intento;
   - `CONTINUOUS + 404/405 + !fallbackAttempted → LEGACY` con política normal;
   - `LEGACY` o fallback ya intentado → sin segundo fallback.
3. Añadir `BuildConfig.LIVE_PLAYBACK_ENABLED` en `app/build.gradle.kts`
   alimentado por `-PlivePlayback`/`TOLOCHA_LIVE_PLAYBACK`; proveer
   `PlaybackFeatureFlags` desde `PlayerModule`.
4. Añadir `liveStreamUrl(baseUrl, stationId, sequence)` y ampliar `streamUrl`
   con `?r=` y KDoc de contrato/errores; tests de codificación y unicidad.
5. Ampliar `StationMediaItemFactory` con `createLive`, `createLegacy` y
   `createMediaSource(item, source, route)`:
   - `CONTINUOUS` → `ProgressiveMediaSource`, sin MIME local;
   - `LEGACY` → HLS si `source.hls`, progresivo en el resto;
   - Cast conserva `createForCast` y su MIME.
6. Ampliar `LocalPlaybackStarter.start` con `route`, `sequence` y `attempt`;
   `recordHistory=false` en fallback.
7. Crear `PlaybackFallbackController` con el cambio de ruta único y la
   ejecución de la ruta legacy. Se engancha en
   `PlaybackRecoveryManager.onPlayerError` antes de `startRecovery`: para
   415/404/405 no se reintenta la ruta continua; 404/405 cambian a legacy y
   continúan con la política existente, y 415 hace un único intento legacy (si
   falla por formato, mensaje y fin). El resto de `RecoveryPolicy` y umbrales no
   cambia en esta fase. Extender `PlaybackRecoveryLogger` con
   `live_fallback` para este camino.
8. Crear `PlaybackMessages` con el mensaje de formato no admitido y los textos
   de error compartidos; no añadir el 415 a `PlaybackStatusReason`, que sigue
   reservado a los motivos de `/status`.
9. Tests Red-Green de ruta, flags, clasificación 415/404/405, URLs, factory,
   logger de fallback y fallback único.

### Phase B - Supervisor, presupuesto y diagnóstico (fase 2)

1. Actualizar `RecoveryPolicy` a 6 intentos, backoff `[0, 1, 2, 4, 8, 15] s`,
   `attemptTimeoutMs=15 s`, `stallThresholdMs=15 s`,
   `initialBufferTimeoutMs=15 s`, `totalTimeoutMs=120 s` y
   `stableWindowMs=60 s`.
2. Ampliar `RecoveryState` con `initial` y `PlayerState.Buffering` con
   `retrying`/`waitingForNetwork`.
3. `SessionManager.setAccess` lee `exp` del JWT sin verificar firma y expone
   `isAccessTokenExpiring(skew=60 s)`; token opaco → `false`.
4. Ampliar `ConnectivityMonitor` con `networkChanges`; implementarlo en
   `AndroidConnectivityMonitor` sin emitir en el registro inicial y actualizar
   `FakeConnectivityMonitor`.
5. Refactorizar `PlaybackRecoveryManager`:
   - `PlaybackFailure` con HTTP/errorCode y `lastFailure` por intento;
   - `STATE_ENDED` como corte recuperable;
   - cambio de red como corte recuperable;
   - reintentos iniciales con `initial=true`;
   - presupuesto acumulado y reset tras 60 s estables;
   - refresh de token antes de cada intento si expira;
   - una sola reconexión en vuelo;
   - cancelación por pausa/stop/cambio/sleep/Cast/foco;
   - `start()`/`stop()` del manager los añade T030 y los invoca
     `RadioPlaybackService.onCreate`/`onDestroy`; el `ViewModel` solo observa;
   - reloj inyectable (`timeProvider`, por defecto
     `SystemClock.elapsedRealtime`) para probar la ventana de 60 s y el límite
     de 120 s con tiempo virtual.
6. Tests de logs estructurados (`PlaybackRecoveryLoggerTest`) y de fallback
   único, 415 sin bucle, 404/405, `STATE_ENDED`, cambio de red, presupuesto de
   60 s, límite de 120 s, refresh y cancelaciones.

### Phase C - Disponibilidad y UI (fase 2)

1. `PlayerViewModel.checkAvailability(station)` comparte el precheck de
   `/status` entre la ficha y `play`, con `withTimeoutOrNull(2_000)`:
   - `playable=false` → `availability` con motivo; `play` lo convierte en
     `Error` y no arranca el player;
   - `playable=true` → arrancar;
   - timeout/error → arrancar y mantener `CONNECTING`.
   `checkAvailability` no cambia el `PlayerState` global; la ficha muestra el
   aviso local. El resultado se reutiliza si tiene menos de 5 s; la ficha
   deshabilita o sustituye «Reproducir» cuando `playable=false`.
2. `PlayerViewModel` mapea `RecoveryState`:
   - `initial=true` → `Buffering(retrying=true)` o
     `Buffering(waitingForNetwork=true)`;
   - `initial=false` → `Reconnecting`;
   - `Idle` tras recuperación → `Playing`/`Buffering`.
3. Retirar el `Error` inmediato de `PlayerViewModel.onPlayerError`; el manager es
   la única fuente de verdad de errores de reproducción. Mantener el manejo de
   Cast y perfil.
4. `PlayerUi` y `StationDetail.kt` muestran «Reintentando…»,
   «Reconectando (n/6)…», «Sin conexión…» y el mensaje de 415; la ficha usa el
   mismo `PlayerState` y añade un `StationDetailContent` sin estado para tests,
   con `contentDescription` coherentes.
5. `RadioPlaybackService` conserva el botón «Reintentar» en `FAILED(RETRY)` y
   el supervisor en el servicio; la notificación refleja buffering/playing/
   paused y no duplica el texto detallado. No se crea otro player.
6. Tests de precheck (timeout, error, no disponible, reutilización <5 s) en
   `PlayerViewModelTest` y `StationDetailAvailabilityTest`; mapeo de estados,
   Compose de `StationDetailContent` (estados inicial/reconexión/415/ficha) y
   acción de reintento.

### Phase D - Regresión, gates y activación (fase 3)

1. Ejecutar tests unitarios, Compose y de regresión de foco, auriculares,
   temporizador, Cast, notificación e historial.
2. Verificar que la bandera desactivada conserva el comportamiento anterior.
3. Ejecutar `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck`,
   `lintDebug` y `connectedDebugAndroidTest` si hay dispositivo.
4. Validar [quickstart.md](./quickstart.md), incluidos SC-001..SC-008.
5. Fase 3: cambiar el valor por defecto de release a `true`, verificar que HLS
   solo se usa como reserva y retirar el camino de manifiesto como ruta
   principal cuando ya no se use.

## Product Rollout

| Fase | Qué se publica | Bandera |
|---|---|---|
| Fase 1 | Phase A: `/live`, reproducción progresiva, fallback 415/404/405, sin cambiar reintentos/watchdog/estados de 0042 | `-PlivePlayback=true` en builds de prueba; release `false` |
| Fase 2 | Phase B + C: supervisor nuevo, estados, precheck, token, red y diagnóstico | Release aún `false`; pruebas con `true` |
| Fase 3 | Phase D.5: activación por defecto y limpieza del camino principal HLS | Release `true`; HLS solo fallback |

## Dependencies & Ordering

- Phase A es bloqueante para B (ruta, URLs y factory).
- Phase B es bloqueante para C (estados y supervisor).
- Phase C es bloqueante para D (validación end-to-end).
- Los tests de cada fase se escriben antes de la implementación correspondiente.
- La fase 1 puede publicarse sola con la bandera activa en pruebas; la fase 2
  no se publica hasta que B y C estén verdes.

## Risks & Mitigations

| Riesgo | Impacto | Mitigación |
|---|---|---|
| El 415 no está en la causa directa de `PlaybackException` | Alto | Recorrer la cadena de causas hasta `InvalidResponseCodeException`; test con causa anidada y con `ERROR_CODE_IO_BAD_HTTP_STATUS`. |
| Un 404 de `/live` se confunde con emisora inexistente | Medio | Fallback único a `LEGACY`; si la emisora no existe, la segunda petición devuelve el 404 real. |
| Carrera entre el listener del ViewModel y el manager al fijar `Error` | Alto | El ViewModel deja de fijar errores de player; el manager es la única fuente y publica `RecoveryState`. |
| El presupuesto de 60 s se reinicia por error en cada corte | Medio | `stableSince` + job de 60 s; tests de cortes repetidos y de reset. |
| El refresh proactivo rota tokens en exceso | Medio | Leer `exp` y refrescar solo si caduca en ≤60 s; ante 401, flujo normal. |
| La bandera de compilación queda mal configurada en release | Medio | Test de `PlaybackFeatureFlags`, build de verificación con `-PlivePlayback=false/true` y nota en release. |
| Cambio de red genera reconexiones duplicadas | Medio | `networkChanges` sin emisión inicial + una sola reconexión en vuelo. |
| El fallback HLS no soporta el contenedor | Medio | Un solo intento; si falla por formato, mensaje y fin; sin bucle. |
| Red lenta agota el límite de 2 minutos | Medio | Backoff acotado, `attemptTimeoutMs=15 s` y estado `FAILED` accionable; no spinner indefinido. |
| El servicio se recrea y pierde el estado de ruta | Bajo | `start()`/`stop()` ata los colectores al servicio; la ruta se reinicia en la siguiente selección de emisora. |
| ExoPlayer es difícil de testear en JVM | Medio | Política pura en dominio; player/engine falsos; Compose y quickstart para lo visual. |

## Complexity Tracking

No hay violaciones constitucionales. La complejidad añadida está justificada por
requisitos explícitos:

| Decisión | Por qué | Alternativa descartada |
|---|---|---|
| `PlaybackRoutePolicy` + presupuesto de 60 s | FR-003/FR-007/FR-021 exigen fallback único y reset tras estabilidad | Reiniciar 6 intentos en cada corte (más simple, pero incumple el reset) |
| Lectura de `exp` del JWT | FR-008 pide refresh antes de reconectar sin rotar el refresh en cada intento | Refrescar siempre (latencia/rotación) o solo ante 401 (no cumple) |
| `Buffering(retrying/waitingForNetwork)` | La clarificación distingue `CONNECTING` de `RECONNECTING` antes del primer audio | Nuevo estado `Connecting` (más churn en UI, tests y mapeos) |

No se añaden módulos Gradle, dependencias, endpoints ni cambios de esquema.

## Next Step

`/speckit.analyze` para revalidar la coherencia tras las correcciones y,
después, `/speckit.implement` empezando por el MVP (`T001`–`T018`).
