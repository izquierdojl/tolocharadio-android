# Tasks: Reproducción continua y reconexión automática

**Input**: Design documents from `/specs/0044-izquierdojl-20261101-live-playback-reconnect/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Tests**: Incluidos porque la constitución III exige Red-Green y el plan define
tests de dominio, datos, ViewModel y UI. Los tests se escriben antes de la
implementación correspondiente y deben fallar primero.

**Organization**: Tareas agrupadas por historia de usuario para que cada una sea
implementable y validable de forma independiente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivos distintos, sin dependencias)
- **[Story]**: US1, US2, US3 o US4
- Todas las tareas incluyen la ruta exacta del archivo o del documento

## Path Conventions

- Aplicación Android de un módulo: `app/src/main/java/...`, `app/src/test/java/...`,
  `app/src/androidTest/java/...`.
- Documentos de la feature: `specs/0044-izquierdojl-20261101-live-playback-reconnect/`.
- No se añaden módulos Gradle, dependencias ni persistencia nueva.

## Task Summary

- **Total**: 56 tareas (`T001`–`T056`).
- **Setup**: 1; **Foundational**: 6; **US1**: 11; **US2**: 14; **US3**: 8;
  **US4**: 4; **Polish**: 12.
- **MVP**: `T001`–`T018` (Setup + Foundational + US1).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: confirmar rama y baseline de gates antes de tocar código.

- [X] T001 Confirmar rama y baseline en `app/build.gradle.kts`: ejecutar `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck` y `lintDebug` antes de tocar código, y registrar el resultado en `specs/0044-izquierdojl-20261101-live-playback-reconnect/plan.md`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: tipos y utilidades compartidos por todas las historias: ruta,
fallos, política de fallback, bandera de compilación y URLs.

**⚠️ CRITICAL**: ninguna historia puede empezar hasta completar esta fase.

- [X] T002 [P] Crear `PlaybackRoute` (`CONTINUOUS`/`LEGACY`) en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/PlaybackRoute.kt`, `PlaybackFeatureFlags(livePlaybackEnabled: Boolean)` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/PlaybackFeatureFlags.kt` y `StationAvailability(stationId, playable, reason, checkedAt)` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/StationAvailability.kt`; cubrir en `app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/PlaybackFeatureFlagsTest.kt` que la bandera no se persiste y que `CONTINUOUS` solo se usa con `livePlaybackEnabled=true`.
- [X] T003 [P] Crear `PlaybackFailure(cause: InterruptionCause, httpStatus: Int?, playerErrorCode: Int?)` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/PlaybackFailure.kt` y ampliar `InterruptionCause` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/InterruptionCause.kt` con `STREAM_ENDED`, `NETWORK_CHANGED`, `LIVE_ROUTE_UNAVAILABLE` y `UNSUPPORTED_FORMAT`; test en `app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/PlaybackFailureTest.kt`.
- [X] T004 [P] Crear `PlaybackRoutePolicy` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/PlaybackRoutePolicy.kt`: `CONTINUOUS + 415 + !fallbackAttempted → LEGACY` con un intento; `CONTINUOUS + 404/405 + !fallbackAttempted → LEGACY` con política normal; `LEGACY` o fallback ya intentado → sin segundo fallback. Test en `app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/PlaybackRoutePolicyTest.kt`.
- [X] T005 [P] Crear `PlaybackMessages` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/PlaybackMessages.kt` con el mensaje «Esta emisora usa un formato que este reproductor no admite» y los textos de error compartidos; mantener `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/PlaybackStatusReason.kt` solo para motivos de `/status`; test en `app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/PlaybackMessagesTest.kt` y conservar `PlaybackStatusReasonTest.kt`.
- [X] T006 [P] Añadir `BuildConfig.LIVE_PLAYBACK_ENABLED` en `app/build.gradle.kts` alimentado por `-PlivePlayback`/`TOLOCHA_LIVE_PLAYBACK` y proveer `PlaybackFeatureFlags` desde `app/src/main/java/com/izquierdojl/tolocharadio/di/PlayerModule.kt`; verificar compilación con `-PlivePlayback=true` y `-PlivePlayback=false`.
- [X] T007 [P] Añadir `liveStreamUrl(baseUrl, stationId, sequence)` y ampliar `streamUrl` con `?r=` y KDoc de contrato/errores en `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/api/PlaybackApi.kt`; test de URIs, `?r=0`, incremento y ausencia de credenciales en `app/src/test/java/com/izquierdojl/tolocharadio/data/remote/api/PlaybackUrlsTest.kt`.

**Checkpoint**: Foundation ready; las historias pueden empezar.

---

## Phase 3: User Story 1 - Escucha continua en emisoras HLS que caducan (Priority: P1) 🎯 MVP

**Goal**: reproducir `/live` como audio progresivo para todas las emisoras y
caer una sola vez a la ruta legacy ante 415 o servidor anterior a 0.29.0, sin
cambiar la política de reintentos de 0042.

**Independent Test**: reproducir Onda Cero 10 minutos sin recarga; comprobar
Litera como audio progresivo; provocar 415 y verificar el fallback HLS único;
probar un servidor que responde 404/405 en `/live`.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation.**

- [X] T008 [P] [US1] Ampliar `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactoryTest.kt` con `/live?r=N` como progresivo sin MIME local (los tres `Content-Type` se validan en T018), `/playback/{id}?r=N` con HLS para `source.hls=true`, progresivo para el resto y Cast con URL pública/MIME sin cambios.
- [X] T009 [P] [US1] Crear `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/LocalPlaybackStarterTest.kt` para ruta por defecto según la bandera, `sequence`/`attempt`, `recordHistory=false` en fallback, misma emisora, volumen intacto y nueva conexión al punto en directo.
- [X] T010 [P] [US1] Crear `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackFallbackControllerTest.kt` para 415 (un intento legacy), 404/405 (legacy y política normal), fallback ya intentado (sin segundo intento) y fallo de formato en legacy (mensaje, sin bucle).
- [X] T011 [P] [US1] Ampliar `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackErrorClassifierTest.kt` con 415 → `UNSUPPORTED_FORMAT`, 404/405 en `CONTINUOUS` → `LIVE_ROUTE_UNAVAILABLE`, 404/405 en `LEGACY` → `STATION_UNAVAILABLE`, causa anidada y `ERROR_CODE_IO_BAD_HTTP_STATUS`.

### Implementation for User Story 1

- [X] T012 [P] [US1] Ampliar `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/StationMediaItemFactory.kt` con `createLive`, `createLegacy` y `createMediaSource(item, source, route)`: `CONTINUOUS` → `ProgressiveMediaSource` sin MIME local; `LEGACY` → HLS si `source.hls`, progresivo en el resto; Cast conserva `createForCast`.
- [X] T013 [US1] Ampliar `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/LocalPlaybackStarter.kt` con `route`, `sequence` y `attempt`, usando `PlaybackFeatureFlags` para la ruta inicial y sin registrar historial en fallback/recovery.
- [X] T014 [P] [US1] Ampliar `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackErrorClassifier.kt` para recorrer la cadena de causas hasta `HttpDataSource.InvalidResponseCodeException` y clasificar 415/404/405 según la ruta.
- [X] T015 [US1] Crear `PlaybackFallbackController` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackFallbackController.kt` y engancharlo en `PlaybackRecoveryManager.onPlayerError` antes de `startRecovery`: 404/405 cambian a legacy y continúan; 415 hace un único intento legacy; no se cambia `RecoveryPolicy`, watchdog ni estados de 0042. Extender `PlaybackRecoveryLogger` con `live_fallback` (httpStatus, playerErrorCode, intento y station hasheada) para este camino.
- [X] T016 [US1] Ajustar `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` para que el arranque local use la ruta por defecto de `LocalPlaybackStarter`, conserve el camino Cast y no duplique historial.
- [X] T017 [US1] Actualizar `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManagerTest.kt` con el doble de `PlaybackFallbackController` y verificar que el fallback único no entra en la recuperación 0042; debe pasar con T015.
- [ ] T018 [US1] Validar en dispositivo/emulador los escenarios 1, 2, 6 y 7 de `specs/0044-izquierdojl-20261101-live-playback-reconnect/quickstart.md` y anotar el resultado.

**Checkpoint**: US1 funcional y validable de forma independiente; es el MVP.

---

## Phase 4: User Story 2 - Reconexión automática ante cortes (Priority: P2)

**Goal**: detectar y recuperar cortes de red, errores 5xx, `STATE_ENDED`,
estancamientos de 15 s y cambios de red, con 6 intentos, ventana de 60 s y
límite de 2 minutos, en primer plano y en segundo plano.

**Independent Test**: reproducir Litera, cortar la red 10 s y restaurarla; debe
volver sola en menos de 20 s. Repetir con 5xx, `STATE_ENDED`, cambio
Wi-Fi↔datos, token caducado y pantalla apagada.

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation.**

- [X] T019 [P] [US2] Actualizar `app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/RecoveryPolicyTest.kt` con `maxAttempts=6`, `totalTimeoutMs=120000`, `attemptTimeoutMs=15000`, `stallThresholdMs=15000`, `initialBufferTimeoutMs=15000`, `stableWindowMs=60000` y backoff `[0,1000,2000,4000,8000,15000]`.
- [X] T020 [P] [US2] Ampliar `app/src/test/java/com/izquierdojl/tolocharadio/core/session/SessionManagerTest.kt` con JWT con `exp` próximo, `exp` lejano, token opaco y token corrupto; `isAccessTokenExpiring(60 s)` debe ser determinista y no registrar el token.
- [X] T021 [P] [US2] Ampliar `app/src/test/java/com/izquierdojl/tolocharadio/core/network/FakeConnectivityMonitor.kt` con `networkChanges` y un método para emitir un cambio de red en tests.
- [X] T022 [US2] Ampliar `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManagerTest.kt` con 6 intentos, presupuesto acumulado, reset a los 60 s, límite de 120 s, `STATE_ENDED`, cambio de red, refresh de token, una sola reconexión en vuelo, conservación del volumen, vuelta al directo, cancelaciones y ausencia de historial duplicado; debe fallar antes de T028.
- [X] T023 [P] [US2] Ampliar `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackErrorClassifierTest.kt` con errores de red, timeout, decoder/parser, 5xx, 401/403 y causas anidadas.

### Implementation for User Story 2

- [X] T024 [P] [US2] Actualizar `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/RecoveryPolicy.kt` con los valores de T019, validación `maxAttempts in 1..6`, `totalTimeoutMs <= 120000` y `stableWindowMs=60000`.
- [X] T025 [P] [US2] Ampliar `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/RecoveryState.kt` con `initial: Boolean` y `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` con `Buffering(station, retrying=false, waitingForNetwork=false)`.
- [X] T026 [P] [US2] Ampliar `app/src/main/java/com/izquierdojl/tolocharadio/core/session/SessionManager.kt` para leer `exp` del JWT sin verificar firma y exponer `isAccessTokenExpiring(skewSeconds=60)`; token opaco → `false`.
- [X] T027 [P] [US2] Ampliar `app/src/main/java/com/izquierdojl/tolocharadio/core/network/ConnectivityMonitor.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/core/network/AndroidConnectivityMonitor.kt` con `networkChanges`, sin emitir en el registro inicial y detectando cambio de red/transporte.
- [X] T028 [US2] Refactorizar `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManager.kt`: `lastFailure`/`PlaybackFailure`, ruta y `fallbackAttempted`, `STATE_ENDED`, `NETWORK_CHANGED`, `initial`, presupuesto acumulado, reset a 60 s, límite de 120 s, refresh de token, `timeProvider` inyectable, una sola reconexión y cancelaciones existentes; no iniciar colectores en el constructor. Extender `PlaybackRecoveryLogger` con `httpStatus`/`playerErrorCode` en `recovery_attempt`/`recovery_success`/`recovery_failed`.
- [X] T029 [US2] Crear `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryLoggerTest.kt` para verificar que `recovery_attempt`/`recovery_success`/`recovery_failed`/`live_fallback` incluyen httpStatus, playerErrorCode, intento y station hasheada, sin PII.
- [X] T030 [US2] Añadir `start()`/`stop()` a `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManager.kt` y llamarlos desde `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt` en `onCreate`/`onDestroy`; los colectores no se inician en el constructor y el `ViewModel` solo observa/delega. La recuperación debe funcionar con pantalla apagada/background.
- [X] T031 [US2] Actualizar `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManagerTest.kt` para el constructor/`timeProvider` final y dejar la batería en verde.
- [ ] T032 [US2] Validar en dispositivo/emulador los escenarios 3, 4, 5, 8 y 9 de `specs/0044-izquierdojl-20261101-live-playback-reconnect/quickstart.md` y anotar el resultado.

**Checkpoint**: US1 y US2 funcionan de forma independiente y combinada.

---

## Phase 5: User Story 3 - Saber qué ocurre y poder reintentar (Priority: P2)

**Goal**: mostrar `CONNECTING`/`Reintentando…` antes del primer audio,
`RECONNECTING` tras un corte, y `FAILED` con «Reintentar» o «Editar servidor»
cuando corresponda, de forma coherente entre mini-player, reproductor completo,
ficha y notificación.

**Independent Test**: provocar un fallo inicial y comprobar «Reintentando…»;
provocar un corte tras sonar y comprobar «Reconectando (n/6)…»; agotar los
intentos y comprobar el mensaje con «Reintentar»; pulsar varias veces y verificar
que no hay conexiones paralelas.

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation.**

- [X] T033 [P] [US3] Ampliar `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` con `initial=true` → `Buffering(retrying=true)`, `initial=false` → `Reconnecting`, `waitingForNetwork` en ambos casos, `FAILED(RETRY)`/`FAILED(EDIT_SERVER)` y que `onPlayerError` no fija un `Error` inmediato.
- [X] T034 [P] [US3] Ampliar `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/player/RecoveryUiTest.kt` con «Reintentando…», «Reconectando (n/6)…», «Sin conexión…», mensaje 415 y botón «Reintentar».
- [X] T035 [P] [US3] Ampliar `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/player/MiniPlayerTest.kt` con los mismos estados, `contentDescription` y acción de reintento.

### Implementation for User Story 3

- [X] T036 [US3] Actualizar `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt`: mapear `RecoveryState.initial` a `Buffering`, retirar el `Error` inmediato de `onPlayerError`, usar `RecoveryPolicy.maxAttempts` y mantener `retry()`/`toggle()` idempotentes.
- [X] T037 [US3] Actualizar `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/StationDetail.kt` con los subtítulos «Reintentando…», «Reconectando (n/6)…», «Sin conexión…» y el mensaje de 415; la ficha usa el mismo `PlayerState`, añade la acción de reintento, extrae `StationDetailContent` sin estado para tests y añade `contentDescription`/anuncios de TalkBack sin repetir ticks.
- [X] T038 [US3] Ajustar `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt` para mostrar «Reintentar» solo en `FAILED(RETRY)` y mantener stop/mute en el resto; la notificación refleja buffering/playing/paused y no duplica el texto detallado. Sin player nuevo.
- [X] T039 [US3] Crear/actualizar `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt`, `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/player/RecoveryUiTest.kt`, `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/player/MiniPlayerTest.kt` y `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/explore/StationDetailPlaybackStateTest.kt` para que pasen con T036–T038, usando `StationDetailContent` sin estado, y verificar la idempotencia de pulsaciones repetidas y la coherencia de la ficha.
- [ ] T040 [US3] Validar los estados, la ficha y la notificación con los escenarios 3, 6 y 11 de `specs/0044-izquierdojl-20261101-live-playback-reconnect/quickstart.md` y anotar el resultado.

**Checkpoint**: US1, US2 y US3 funcionan de forma independiente y combinada.

---

## Phase 6: User Story 4 - Comprobar disponibilidad antes de reproducir (Priority: P3)

**Goal**: consultar `/status` al abrir la ficha o pulsar play, avisar del motivo
si no es reproducible y no retrasar la interfaz más de 2 segundos.

**Independent Test**: abrir una emisora no disponible y comprobar que no arranca
el player; simular un backend lento (>2 s) y comprobar que la app intenta
reproducir; en una emisora disponible, el precheck no añade más de 2 s.

### Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation.**

- [X] T041 [US4] Ampliar `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` con `playable=false` (sin player), timeout de 2 s (intenta reproducir), error de transporte (intenta reproducir), `playable=true` y reutilización del resultado durante 5 s; crear `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/explore/StationDetailAvailabilityTest.kt` para el aviso y el botón deshabilitado al abrir la ficha, usando `StationDetailContent` sin estado.

### Implementation for User Story 4

- [X] T042 [US4] Actualizar `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` con `checkAvailability(station)` y `availability`/TTL de 5 s (sin cambiar el `PlayerState` global; `play` aplica `Error` si `playable=false`), y `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/StationDetail.kt` para invocarlo al abrir la ficha, mostrar el aviso y deshabilitar/sustituir «Reproducir» cuando `playable=false`; `StationDetailContent` recibe `availability`/`PlayerState`/callbacks, se limpia `availability` al cambiar de emisora/stop y se reutilizan `PlaybackStatusReason`/`PlaybackMessages`.
- [X] T043 [US4] Actualizar `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` y `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/explore/StationDetailAvailabilityTest.kt` para que pasen con T042, usando `StationDetailContent`, y verificar que la consulta se reutiliza durante 5 s y se cancela al cambiar de emisora o cerrar la ficha.
- [ ] T044 [US4] Validar el escenario 10 de `specs/0044-izquierdojl-20261101-live-playback-reconnect/quickstart.md`, incluida la apertura de la ficha, y anotar el resultado.

**Checkpoint**: las cuatro historias son independientes y el precheck no bloquea la UI.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: calidad, documentación, activación por defecto y validación final.

- [X] T045 [P] Añadir KDoc a `PlaybackRoute`, `PlaybackRoutePolicy`, `PlaybackFailure`, `PlaybackFeatureFlags`, `PlaybackMessages`, `RecoveryPolicy` y `StationAvailability` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/`, a `liveStreamUrl`/`streamUrl` en `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/api/PlaybackApi.kt`, y a `PlaybackFallbackController` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/`.
- [X] T046 [P] Revisar logs y textos para que no incluyan PII, URLs, títulos ni credenciales en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryLogger.kt`, `PlaybackRecoveryManager.kt` y `PlayerUi.kt`.
- [X] T047 [P] Documentar la bandera `-PlivePlayback`/`TOLOCHA_LIVE_PLAYBACK` y el despliegue por fases en `README.md` y `docs/RELEASE.md`.
- [X] T048 Ejecutar `testDebugUnitTest` sobre `app/src/test/java/com/izquierdojl/tolocharadio/` y corregir cualquier test de dominio, datos, sesión, red, manager o ViewModel que falle.
- [X] T049 Ejecutar `detekt`, `ktlintCheck` y `lintDebug` sobre `app/build.gradle.kts` y corregir los warnings nuevos.
- [X] T050 Ejecutar `assembleDebug` sobre `app/build.gradle.kts` con `-PlivePlayback=true` y `-PlivePlayback=false`.
- [X] T051 Ejecutar `connectedDebugAndroidTest` sobre `app/src/androidTest/java/com/izquierdojl/tolocharadio/` con dispositivo/emulador y corregir las pruebas de UI.
- [ ] T052 Ejecutar los escenarios 1–11 y los protocolos SC-001..SC-008 de `specs/0044-izquierdojl-20261101-live-playback-reconnect/quickstart.md` y registrar los resultados.
- [ ] T053 Verificar no regresiones de foco de audio, auriculares/Bluetooth, temporizador de apagado, Cast, notificación e historial con la batería existente y el escenario 11 de `quickstart.md`.
- [X] T054 Verificar que no se añaden dependencias, módulos Gradle, endpoints, claves de DataStore ni tablas Room; revisar `app/build.gradle.kts` y `app/src/main/java/com/izquierdojl/tolocharadio/di/`.
- [X] T055 Revisar la trazabilidad FR-001..FR-025 / SC-001..SC-010 en `specs/0044-izquierdojl-20261101-live-playback-reconnect/spec.md`, `plan.md`, `tasks.md` y los tests.
- [X] T056 Fase 3: cambiar el valor por defecto de release a `LIVE_PLAYBACK_ENABLED=true` en `app/build.gradle.kts`, verificar que HLS solo se usa como reserva y que el camino de manifiesto deja de ser la ruta principal.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias; solo T001.
- **Foundational (Phase 2)**: depende de Setup; bloquea todas las historias.
- **US1 (Phase 3)**: depende de Foundational; es el MVP.
- **US2 (Phase 4)**: depende de Foundational y de la ruta/factory de US1.
- **US3 (Phase 5)**: depende de Foundational y del estado de US2; su UI puede probarse con `RecoveryState` sintético, pero el flujo real necesita US2.
- **US4 (Phase 6)**: depende de Foundational; puede desarrollarse en paralelo con US2/US3.
- **Polish (Phase 7)**: depende de todas las historias deseadas.

### User Story Dependencies

- **US1**: independiente tras Foundational; entrega el MVP.
- **US2**: requiere US1 para la ruta y el factory, pero sus tests de política pueden empezar antes.
- **US3**: requiere US2 para los estados reales; los tests Compose pueden usar estados sintéticos.
- **US4**: independiente tras Foundational; toca `PlayerViewModel`, `StationDetail` y `PlaybackStatusReason`.

### Within Each User Story

- Los tests MUST escribirse y fallar antes de la implementación.
- Modelos/entidades antes de servicios.
- Servicios antes de UI/integración.
- Cada historia termina con su validación en `quickstart.md` y su checkpoint.

### Parallel Opportunities

- T002–T007 son archivos distintos y pueden ir en paralelo.
- T008–T011 son tests de archivos distintos y pueden ir en paralelo.
- T012 y T014 son archivos distintos; T013 depende de T012; T015 depende de T013/T014.
- T019–T021 y T023 son tests de archivos distintos; T022 depende de T021.
- T024–T027 son archivos distintos y pueden ir en paralelo; T028 depende de todos.
- T033–T035 son tests de archivos distintos y pueden ir en paralelo.
- T045–T047 y T054–T055 son documentos/revisiones independientes.

---

## Parallel Example: Foundational

```bash
# Lanzar en paralelo los tipos y utilidades compartidas:
Task: "T002 PlaybackRoute + PlaybackFeatureFlags"
Task: "T003 PlaybackFailure + InterruptionCause"
Task: "T004 PlaybackRoutePolicy"
Task: "T005 PlaybackMessages 415"
Task: "T006 BuildConfig + PlayerModule"
Task: "T007 PlaybackApi URL builders"
```

## Parallel Example: User Story 1

```bash
# Tests primero, en paralelo:
Task: "T008 StationMediaItemFactoryTest"
Task: "T009 LocalPlaybackStarterTest"
Task: "T010 PlaybackFallbackControllerTest"
Task: "T011 PlaybackErrorClassifierTest"
```

## Parallel Example: User Story 2

```bash
# Tests de política, sesión y red:
Task: "T019 RecoveryPolicyTest"
Task: "T020 SessionManagerTest"
Task: "T021 FakeConnectivityMonitor"
Task: "T023 PlaybackErrorClassifierTest"
```

## Parallel Example: User Story 3

```bash
# Tests de estado y UI:
Task: "T033 PlayerViewModelTest"
Task: "T034 RecoveryUiTest"
Task: "T035 MiniPlayerTest"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1: Setup.
2. Completar Phase 2: Foundational.
3. Completar Phase 3: US1 (`/live` + fallback + flag).
4. **STOP and VALIDATE**: ejecutar los escenarios 1, 2, 6 y 7 de `quickstart.md`.
5. Desplegar en pruebas con `-PlivePlayback=true`; release con `false`.

### Incremental Delivery

1. **Fase 1**: US1 con la bandera; no cambia la política 0042.
2. **Fase 2**: US2 + US3; supervisor nuevo, estados, precheck.
3. **Fase 3**: T056; activación por defecto y HLS solo como reserva.
4. Cada historia se valida de forma independiente antes de pasar a la siguiente.

### Parallel Team Strategy

1. El equipo completa Setup + Foundational.
2. Después:
   - Developer A: US1 (ruta, factory, fallback).
   - Developer B: US4 (precheck) en paralelo.
   - Developer C: tests de política/sesión de US2.
3. US2 y US3 se integran después de US1; US4 no bloquea a las demás.

---

## Notes

- `[P]` significa archivos distintos y sin dependencias incompletas.
- Los tests deben fallar antes de la implementación correspondiente.
- No ejecutar commits automáticos: el hook de git es opcional y requiere
  petición explícita del usuario.
- Detenerse en cada checkpoint para validar la historia de forma independiente.
- Evitar tareas vagas o cambios en el mismo archivo desde tareas paralelas.
- La bandera de compilación no es una preferencia de usuario ni se persiste.

---

## Format Validation

- 56/56 tareas siguen `- [ ] Txxx [P?] [US?] descripción con ruta`.
- IDs secuenciales `T001`–`T056`, sin duplicados.
- Todas las tareas de historia incluyen `[US1]`, `[US2]`, `[US3]` o `[US4]`.
- Todas las tareas incluyen una ruta de archivo o documento verificable.
- `[P]` solo se usa en tareas de archivos distintos y sin dependencias
  incompletas.

---

## Phase 8: Convergence

- [X] T057 [CRITICAL] Cancelar la recuperación en curso y no reabrir el stream cuando se pierde el foco de audio, entra una llamada, se desconectan auriculares/Bluetooth o se activa Cast; revalidar `castPlayerManager.isCastConnected`, `profileController.applying` y el estado de pausa/foco antes de cada intento en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManager.kt`, con tests en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManagerTest.kt` per FR-014/Constitution IV (partial).
- [X] T058 Centralizar la generación de `?r=` y la ruta efectiva para que la primera conexión use `r=0`, cada reconexión use un valor distinto y los reinicios por cambio de perfil o vuelta de Cast conserven la ruta y no repitan el fallback; ajustar `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/LocalPlaybackStarter.kt`, `PlaybackRecoveryManager.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt`, con tests de secuencia inicial, recovery, perfil y Cast en `LocalPlaybackStarterTest.kt` y `PlaybackRecoveryManagerTest.kt` per FR-004/FR-005/FR-022 (partial).
- [X] T059 Clasificar los errores de parser/contenedor/decoder no soportados en la ruta `CONTINUOUS` como `UNSUPPORTED_FORMAT` para que `PlaybackRoutePolicy` intente una sola vez `LEGACY`; mantener el mensaje de formato en `LEGACY` y añadir tests en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackErrorClassifierTest.kt` y `PlaybackRecoveryManagerTest.kt` per FR-003/plan R1-R2 (partial).
- [X] T060 Renovar la sesión antes de consumir un intento cuando el fallo es 401/403 o el token está a punto de caducar, de forma que el refresh no gaste uno de los 6 reintentos; ajustar `PlaybackRecoveryManager.runAttempts` y cubrir el presupuesto con un test en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManagerTest.kt` per FR-008/contracts/playback-live-api.md (contradicts).
- [X] T061 Extender `PlaybackRecoveryLogger.success`/`failure` para recibir `PlaybackFailure` y registrar `httpStatus`/`playerErrorCode` cuando existan, sin PII; actualizar `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManager.kt` y `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryLoggerTest.kt` per FR-019/SC-007/plan T028-T029 (partial).
- [X] T062 Hacer que la notificación multimedia refleje el estado lógico de reconexión/buffering durante los intervalos de backoff, no solo el estado crudo del player; ajustar `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt` o su proveedor de notificación y verificarlo con una prueba o escenario de `quickstart.md` per FR-018/contracts/playback-live-ui.md (partial).
- [X] T063 Completar la coherencia visual y de accesibilidad del reproductor completo: mostrar «Cargando…» en `Buffering` normal y usar `contentDescription` «Pausar»/«Reanudar» según el estado en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt`, con cobertura en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/player/RecoveryUiTest.kt` o un test del reproductor completo per FR-011/contracts/playback-live-ui.md (partial).
