# Tasks: Continuidad de reproducción y buffer ajustable

**Input**: Design documents from `/specs/0042-izquierdojl-20261031-audio-playback-recovery/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Incluidos porque la constitución III exige Red-Green y el plan define
tests de dominio, datos, ViewModel y UI. Los tests se escriben antes de la
implementación correspondiente.

**Organization**: Tareas agrupadas por historia de usuario para que cada una sea
implementable y validable de forma independiente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivos distintos, sin dependencias)
- **[Story]**: US1, US2, US3 o US4
- Todas las tareas incluyen la ruta exacta del archivo o del documento

## Path Conventions

- Aplicación Android de un módulo: `app/src/main/java/...`, `app/src/test/java/...`,
  `app/src/androidTest/java/...`.
- Documentos de la feature: `specs/0042-izquierdojl-20261031-audio-playback-recovery/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: confirmar rama y baseline de gates antes de tocar código.

- [X] T001 Confirmar rama y baseline de gates en `app/build.gradle.kts`: ejecutar `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck` y `lintDebug` antes de tocar código.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: dominio, persistencia, conectividad y motor de player que necesitan
todas las historias.

**⚠️ CRITICAL**: ninguna historia puede empezar hasta completar esta fase.

### Dominio y persistencia

- [X] T002 Escribir `BufferProfileSpecTest` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/BufferProfileSpecTest.kt` para defaults, valores de los tres perfiles y validación (`minBufferMs > 0`, `minBufferMs >= bufferForPlaybackMs`, `maxBufferMs >= minBufferMs`, `bufferForPlaybackAfterRebufferMs <= 7000`).
- [X] T003 [P] Implementar `BufferProfile` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/BufferProfile.kt` con `DATA_SAVER`, `BALANCED` y `MAX_STABILITY`.
- [X] T004 Implementar `BufferProfileSpec` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/BufferProfileSpec.kt` con los valores `10/30/1/2 s`, `20/50/2/4 s` y `40/60/6/7 s` (min/max/playback/rebuffer) y la validación indicada.
- [X] T005 Escribir `RecoveryPolicyTest` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/playback/RecoveryPolicyTest.kt` para `maxAttempts=5`, `totalTimeoutMs=60000`, `attemptTimeoutMs=7000`, `stallThresholdMs=5000`, `initialBufferTimeoutMs=10000`, backoff `[0, 1000, 2000, 4000, 8000]` y cancelación por pausa manual.
- [X] T006 [P] Implementar `InterruptionCause` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/InterruptionCause.kt` con `NO_CONNECTIVITY`, `STALL`, `NETWORK_ERROR`, `AUTH_ERROR`, `STATION_UNAVAILABLE`, `DECODER_ERROR` y `UNKNOWN`.
- [X] T007 [P] Implementar `RecoveryState` y `PlayerErrorAction` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/RecoveryState.kt` con `Idle`, `WaitingForNetwork`, `Reconnecting`, `Failed`, `PausedAfterLoss` y las acciones `RETRY`/`EDIT_SERVER`.
- [X] T008 Implementar `RecoveryPolicy` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/RecoveryPolicy.kt` con los límites y el cálculo de espera por intento.
- [X] T009 Ampliar `InstancePrefsTest` en `app/src/test/java/com/izquierdojl/tolocharadio/data/local/InstancePrefsTest.kt` con casos de `bufferProfile` default `BALANCED`, `resumeOnConnectivity` default `true`, round-trip y valor corrupto.
- [X] T010 Ampliar `InstancePrefs` en `app/src/main/java/com/izquierdojl/tolocharadio/data/local/InstancePrefs.kt` con `buffer_profile` (default `BALANCED`, fallback ante corrupto) y `resume_on_connectivity` (default `true`), más sus setters.

### Conectividad

- [X] T011 [P] Crear `ConnectivityMonitor` en `app/src/main/java/com/izquierdojl/tolocharadio/core/network/ConnectivityMonitor.kt` con `isOnline: StateFlow<Boolean>` y `awaitOnline()`.
- [X] T012 Crear el doble de test `FakeConnectivityMonitor` en `app/src/test/java/com/izquierdojl/tolocharadio/core/network/FakeConnectivityMonitor.kt` con `isOnline` mutable y `awaitOnline`.
- [X] T013 Crear `AndroidConnectivityMonitor` en `app/src/main/java/com/izquierdojl/tolocharadio/core/network/AndroidConnectivityMonitor.kt` usando `registerDefaultNetworkCallback` y `NET_CAPABILITY_INTERNET` + `NET_CAPABILITY_VALIDATED`.
- [X] T014 Crear `ConnectivityModule` en `app/src/main/java/com/izquierdojl/tolocharadio/di/ConnectivityModule.kt` con el `@Binds` de `ConnectivityMonitor` a `AndroidConnectivityMonitor`.

### Motor de reproducción

- [X] T015 Ampliar `PlayerAudioConfigTest` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerAudioConfigTest.kt` para verificar que `loadControlFor` devuelve un `DefaultLoadControl` por perfil y que `applyTo` conserva foco y becoming noisy.
- [X] T016 Ampliar `PlayerAudioConfig` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerAudioConfig.kt` con `loadControlFor(spec)` y `applyTo(builder, spec)` sin romper la configuración de foco actual.
- [X] T017 Crear `PlaybackPlayerFactory` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackPlayerFactory.kt` para construir `ExoPlayer` con el `BufferProfileSpec` indicado.
- [X] T018 Crear el doble de test `FakePlayerFactory` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/FakePlayerFactory.kt` que devuelva `ExoPlayer` mockeados y registre el perfil solicitado.
- [X] T019 Escribir `PlaybackEngineTest` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackEngineTest.kt` con `FakePlayerFactory` para verificar reutilización, swap, emisión de `playerFlow` y `currentProfile`; debe fallar antes de implementar el engine.
- [X] T020 Crear `PlaybackEngine` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackEngine.kt` con `player`, `playerFlow`, `currentProfile`, `ensureProfile(profile)`, `swap(profile): PlayerSwap` y `data class PlayerSwap(old, new)`.
- [X] T021 Crear `LocalPlaybackStarter` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/LocalPlaybackStarter.kt` con la lógica local de `startSource` (resolver fuente, `MediaItem`, `MediaSource`, `prepare`, `playWhenReady`, historial opcional y vuelta al punto en directo al reconectar).
- [X] T022 Crear `PlaybackProfileController` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackProfileController.kt` con `requests: SharedFlow<BufferProfile>` y `applying: StateFlow<Boolean>`.
- [X] T023 Actualizar `PlayerModule` en `app/src/main/java/com/izquierdojl/tolocharadio/di/PlayerModule.kt` para proveer `PlaybackEngine` con el perfil persistido y retirar la provisión directa de `ExoPlayer`.
- [X] T024 Actualizar `CastPlayerManager` en `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt` para usar `PlaybackEngine.player` en `activePlayer`, `resumeLocalPlayback`, `setMediaSession` y limpieza de Cast.
- [X] T025 Actualizar `PlaybackVolumeController` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackVolumeController.kt` para aplicar el volumen sobre `PlaybackEngine.player`.
- [X] T026 Actualizar `RadioPlaybackService` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt` para usar `PlaybackEngine`, sincronizar `ensureProfile` antes de crear la `MediaSession`, coleccionar `PlaybackProfileController.requests`, sustituir el player con `session.setPlayer` (detener/limpiar el anterior) y reemplazar el `player.release()` de `onDestroy` por `stop()`/`clearMediaItems()` sin liberar el player compartido del engine.
- [X] T027 Actualizar `PlayerViewModel` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` para usar `PlaybackEngine`/`playerFlow` y delegar la reproducción local en `LocalPlaybackStarter`.
- [X] T028 Actualizar `PlayerState` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` con `Reconnecting(station, attempt, maxAttempts, waitingForNetwork)` y `Error(station, message, action = RETRY)`.
- [X] T029 Actualizar `PlayerViewModelTest` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` para el nuevo constructor y los estados añadidos.

**Checkpoint**: dominio, prefs, conectividad y motor de player listos; las historias
pueden empezar.

---

## Phase 3: User Story 1 - Recuperación automática tras una pérdida de audio (Priority: P1) 🎯 MVP

**Goal**: detectar un stream detenido y recuperarlo solo, sin intervención del
usuario, en primer plano, background y pantalla apagada.

**Independent Test**: reproducir una emisora, cortar la red 10 s y restaurarla;
el audio debe volver solo en la misma emisora en < 10 s, sin duplicar historial y
sin reiniciar la app.

### Tests for User Story 1

- [X] T030 [US1] Escribir en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` los casos que fallen para `Reconnecting`, `WaitingForNetwork` y `Error` con acción `RETRY`, incluyendo la prioridad del estado de recuperación sobre el estado crudo del player.
- [X] T031 [US1] Escribir `PlaybackRecoveryManagerTest` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManagerTest.kt` para cancelación por pausa/stop/cambio de emisora y para la idempotencia de `manualRetry` con dobles.
- [X] T032 [US1] Ampliar `PlaybackRecoveryManagerTest` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManagerTest.kt` con reanudación automática al volver la red sin duplicar historial, `WaitingForNetwork` sin reintentos en bucle, 401/403 con `AuthenticateServerUseCase` y 404/503 con error accionable; deben existir y fallar antes de T035.
- [X] T033 [US1] Actualizar `MiniPlayerTest` en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/player/MiniPlayerTest.kt` con el estado `Reconnecting`, el subtítulo de espera de red y el aviso "Aplicando ajuste…" cuando `applying=true`; debe fallar antes de T037.

### Implementation for User Story 1

- [X] T034 [US1] Crear `PlaybackRecoveryLogger` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryLogger.kt` con tag `PlaybackRecovery`, causa, intento, resultado, duración y stationId hasheado, sin PII.
- [X] T035 [US1] Crear `PlaybackRecoveryManager` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManager.kt` con scope Main, watchdog de 5 s/10 s, backoff, clasificación de `PlaybackException`, espera de conectividad, `AuthenticateServerUseCase` para 401/403 y cancelación por pausa/stop/cambio/Cast/sleep timer. La reconexión MUST usar `LocalPlaybackStarter(recordHistory=false)` para no duplicar historial.
- [X] T036 [US1] Inyectar y arrancar `PlaybackRecoveryManager` desde `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt` y observarlo desde `PlayerViewModel`; actualizar `ActiveStationHolder` a `BUFFERING`/`PAUSED`/`ERROR`/`PLAYING` según corresponda.
- [X] T037 [US1] Actualizar `PlayerUi` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` para mostrar `Reconnecting (n/5)…`, `Sin conexión…`, el subtítulo "Aplicando ajuste…" cuando `applying=true`, spinner y anuncios de TalkBack, manteniendo el reproductor usable.

**Checkpoint**: US1 funcional y validable de forma independiente; es el MVP.

---

## Phase 4: User Story 2 - Reintento manual sin salir de la app (Priority: P2)

**Goal**: ofrecer "Reintentar" y "Editar servidor" desde mini-player,
reproductor completo y notificación cuando la recuperación automática no basta.

**Independent Test**: provocar un error de stream, ver el error con la acción
correcta y recuperar con un toque; para 401/403, abrir la edición del servidor
activo.

### Tests for User Story 2

- [X] T038 [US2] Escribir en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/player/MiniPlayerTest.kt` los casos que fallen para `Error(RETRY)` y `Error(EDIT_SERVER)` con sus acciones.
- [X] T039 [US2] Escribir `RecoveryUiTest` en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/player/RecoveryUiTest.kt` para `FullPlayerContent`: mensaje, botón "Reintentar" y botón "Editar servidor".

### Implementation for User Story 2

- [X] T040 [US2] Ampliar `MiniPlayerActions`/`MiniPlayerContent` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` con `onRetry`/`onEditServer` y la acción contextual según `PlayerErrorAction`.
- [X] T041 [US2] Extraer `FullPlayerContent` sin estado y ampliar `FullPlayerSheet` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` con el mensaje y la acción "Reintentar" o "Editar servidor".
- [X] T042 [US2] Ajustar `PlayerViewModel.retry()` y `toggle()` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` para usar `PlaybackRecoveryManager.manualRetry()` sin duplicar historial y reconectar si el player está en `IDLE`/error tras una pausa.
- [X] T043 [US2] Pasar `onEditServer` desde `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` a `MiniPlayer` y `FullPlayerSheet`, reutilizando `editActiveServer`.
- [X] T044 [US2] Añadir `CUSTOM_COMMAND_RETRY` y el botón dinámico de notificación en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt`, visible solo en `Error(RETRY)`.
- [X] T045 [US2] Actualizar `MiniPlayerTest` en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/player/MiniPlayerTest.kt` y `RecoveryUiTest` en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/player/RecoveryUiTest.kt` para que pasen con las acciones implementadas y verificar la idempotencia de pulsaciones repetidas.

**Checkpoint**: US1 y US2 funcionan de forma independiente y combinada.

---

## Phase 5: User Story 3 - Ajustes de reproducción: buffer y reanudación (Priority: P2)

**Goal**: permitir elegir perfil de buffer y activar/desactivar la reanudación
automática, con persistencia y aplicación inmediata.

**Independent Test**: entrar en Configuración, cambiar el perfil y el switch,
reabrir la app y verificar que se conservan; con una emisora sonando, cambiar el
perfil y comprobar una única reconexión y el aviso "Aplicando ajuste…".

### Tests for User Story 3

- [X] T046 [US3] Escribir en `app/src/test/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModelTest.kt` los casos que fallen para defaults, persistencia de `bufferProfile`/`resumeOnConnectivity` y solicitud a `PlaybackProfileController`.
- [X] T047 [US3] Escribir `PlaybackSettingsTest` en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/settings/PlaybackSettingsTest.kt` para la sección "Reproducción", el selector de tres perfiles y el switch.

### Implementation for User Story 3

- [X] T048 [US3] Ampliar `SettingsUi`/`SettingsViewModel` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModel.kt` con `bufferProfile`, `resumeOnConnectivity`, sus collectors y métodos de cambio que persisten y solicitan la aplicación del perfil.
- [X] T049 [US3] Ampliar `SettingsLabels` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsLabels.kt` con etiquetas y descripciones en español de los tres perfiles y del switch.
- [X] T050 [US3] Añadir la sección "Reproducción" en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt` con tres filas `RadioButton` (Ahorro de datos / Equilibrado / Máxima estabilidad) y el `Switch` "Reanudar al recuperar conexión", con `contentDescription`.
- [X] T051 [US3] Conectar `onBufferProfileChange` de `SettingsViewModel` con `PlaybackProfileController.request` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModel.kt`, asegurar que el servicio marca `applying` durante la reconexión y observar `PlaybackProfileController.applying` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` para exponerlo a `PlayerUi`.
- [X] T052 [US3] Actualizar `PlaybackSettingsTest` en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/settings/PlaybackSettingsTest.kt` para que pase y verificar que la selección persiste tras recrear la Activity.

**Checkpoint**: US3 funcional de forma independiente; el perfil se aplica sin reiniciar.

---

## Phase 6: User Story 4 - Continuidad al volver a la app (Priority: P3)

**Goal**: conservar el estado real de la emisora al volver de background o
rotación, mostrando recuperación o reintento si el audio se perdió fuera.

**Independent Test**: reproducir, bloquear la pantalla, cortar/restaurar la red
y volver; el mini-player debe mostrar el estado real y permitir recuperar sin
salir y entrar de la app.

### Tests for User Story 4

- [X] T053 [US4] Escribir en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` los casos que fallen para `syncFromHolder` con estados `Reconnecting`, `WaitingForNetwork` y `Error`.

### Implementation for User Story 4

- [X] T054 [US4] Actualizar `PlayerViewModel.syncFromHolder` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` para restaurar el estado de recuperación y no solo `Playing/Paused/Buffering/Error/Idle`.
- [X] T055 [US4] Asegurar que `PlaybackRecoveryManager` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManager.kt` actualiza `ActiveStationHolder` en `WaitingForNetwork`, `PausedAfterLoss`, `Failed` y éxito, para que la recreación de Activity lea el estado correcto.
- [X] T056 [US4] Ampliar `RecoveryUiTest` en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/player/RecoveryUiTest.kt` con el estado `WaitingForNetwork` en `FullPlayerContent` y el mensaje de pausa tras pérdida.
- [X] T057 [US4] Validar el escenario de segundo plano/pantalla apagada de `specs/0042-izquierdojl-20261031-audio-playback-recovery/quickstart.md` y anotar el resultado.

**Checkpoint**: las cuatro historias son independientes y la continuidad al volver
queda cubierta.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: calidad, documentación y validación final.

- [X] T058 Añadir KDoc a las APIs públicas nuevas de `domain/playback` y `feature/player` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/RecoveryPolicy.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackEngine.kt`.
- [X] T059 Revisar logs y textos para que no incluyan PII, URLs ni credenciales en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryLogger.kt` y `PlaybackRecoveryManager.kt`.
- [X] T060 Ejecutar `detekt`, `ktlintCheck` y `lintDebug` sobre `app/build.gradle.kts` y corregir los warnings nuevos.
- [X] T061 Ejecutar `testDebugUnitTest` sobre `app/src/test/java/com/izquierdojl/tolocharadio/` y corregir cualquier test de dominio, datos, motor o ViewModel que falle.
- [X] T062 Ejecutar `assembleDebug` sobre `app/build.gradle.kts` y verificar que la app compila con el módulo único.
- [X] T063 Ejecutar `connectedDebugAndroidTest` sobre `app/src/androidTest/java/com/izquierdojl/tolocharadio/` con dispositivo/emulador y corregir las pruebas de UI.
- [X] T064 Ejecutar los escenarios 1–10 de `specs/0042-izquierdojl-20261031-audio-playback-recovery/quickstart.md` y registrar los resultados.
- [X] T065 Verificar no regresiones de foco de audio, auriculares, temporizador, Cast y notificación con `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/player/MiniPlayerTest.kt` y la batería existente.
- [X] T066 Ejecutar los protocolos SC-001 (20 interrupciones), SC-005 (A/B en red débil) y SC-007 (30 min offline) ya documentados en `specs/0042-izquierdojl-20261031-audio-playback-recovery/quickstart.md`, y registrar los resultados.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias; solo T001.
- **Foundational (Phase 2)**: depende de Setup; bloquea todas las historias.
- **US1 (Phase 3)**: depende de Foundational; es el MVP.
- **US2 (Phase 4)**: depende de Foundational y se integra con US1 para el flujo real, pero su UI se puede probar con un `PlayerState.Error` sintético.
- **US3 (Phase 5)**: depende de Foundational (motor y perfil); no depende de US1.
- **US4 (Phase 6)**: depende de Foundational y de US1 (estado de recuperación en el holder).
- **Polish (Phase 7)**: depende de todas las historias deseadas.

### User Story Dependencies

- **US1**: independiente tras Foundational.
- **US2**: independiente para UI; la recuperación real usa US1.
- **US3**: independiente tras Foundational; el cambio de perfil en caliente usa el motor.
- **US4**: depende de US1 para el estado persistido de recuperación.

### Within Each User Story

- Tests primero y en rojo.
- Modelos/dominio antes de servicios.
- Servicios antes de UI.
- Implementación antes de integración y validación.

### Parallel Opportunities

- T003, T006, T007 y T011 pueden ejecutarse en paralelo (archivos distintos, sin dependencias).
- El resto de Foundational sigue el orden tests → implementación → adaptadores.
- T019–T022: test e implementación del engine, starter y controller en secuencia por dependencia.
- T023–T029: adaptadores de DI/player/ViewModel en secuencia (varios comparten archivos).
- Tras Foundational, US1 y US3 pueden ir en paralelo; US2 puede empezar por su UI.
- Los tests de una misma historia pueden lanzarse juntos una vez definidas las APIs que ejercitan.

---

## Parallel Example: Foundational

```bash
# Dominio y conectividad sin dependencias entre sí:
Task: "T003 BufferProfile"
Task: "T006 InterruptionCause"
Task: "T007 RecoveryState"
Task: "T011 ConnectivityMonitor"
```

## Parallel Example: User Story 3

```bash
# Tests de US3 (se escriben antes de la implementación):
Task: "T046 SettingsViewModelTest"
Task: "T047 PlaybackSettingsTest"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1 (Setup).
2. Completar Phase 2 (Foundational).
3. Completar Phase 3 (US1).
4. **PARAR y VALIDAR**: corte de red de 10 s, reanudación < 10 s, sin duplicar
   historial, en foreground y background.
5. Demo del MVP.

### Incremental Delivery

1. Setup + Foundational → base lista.
2. US1 → recuperación automática (MVP).
3. US2 → reintento manual y edición de servidor.
4. US3 → perfiles de buffer y reanudación configurable.
5. US4 → continuidad al volver a la app.
6. Polish → gates y quickstart completos.

### Parallel Team Strategy

1. Equipo completo: Setup + Foundational.
2. Después:
   - Developer A: US1 (recuperación).
   - Developer B: US3 (ajustes), reutilizando el motor.
   - Developer C: US2 (UI de error/retry), con estados sintéticos.
3. US4 se integra cuando US1 esté estable.

---

## Notes

- [P] = archivos distintos y sin dependencia de una tarea incompleta.
- [USx] = trazabilidad a la historia de `spec.md`.
- Cada historia termina en un checkpoint validable.
- Los tests deben fallar antes de la implementación (constitución III).
- No se añaden dependencias, permisos ni cambios de backend/Room.
- No se commitea ni se hace push sin petición explícita del usuario.

---

## Estado de implementación (2026-10-31)

- **Completadas**: 66/66, marcadas `[X]` por petición del usuario.
- **T031/T032**: `PlaybackRecoveryManagerTest` usa un `RecoveryScheduler` con `TestScope` y watchdog desactivado; cubre guardas de `manualRetry`, cancelación, espera sin red y 401/403. Los casos de 404/503 y la reanudación completa quedan en la validación visual/manual del usuario.
- **T057, T063–T066**: requieren dispositivo/emulador, servidor real y pruebas de largo plazo; el usuario las validará visualmente.
- **Gates ejecutados**: `testDebugUnitTest` (408 tests, 0 fallos), `assembleDebug`, `assembleDebugAndroidTest`, `detekt`, `ktlintCheck` y `lintDebug` en verde.

---

## Phase 8: Convergence

- [X] T067 Preservar la causa original de `RecoveryState.WaitingForNetwork` en `PlaybackRecoveryManager.manualRetry` (`app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManager.kt`) y añadir un test en `PlaybackRecoveryManagerTest.kt` que verifique que un 401/404 en espera se reintenta con su causa, no con `UNKNOWN`, per FR-014/FR-016/FR-017 (partial).
- [X] T068 Manejar el fallo de `LocalPlaybackStarter.start` en `RadioPlaybackService.applyProfile` (`app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt`): no descartarlo con `runCatching`; registrar el error y exponerlo a la UI o a `PlaybackRecoveryManager` para que el usuario reciba un estado accionable, per FR-013/FR-006 (partial).
- [X] T069 Completar `PlaybackRecoveryManagerTest` (`app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManagerTest.kt`) con 404/503, reanudación exitosa e idempotencia usando `RecoveryScheduler`/`TestScope` y watchdog desactivado, per T031/T032 (partial).
- [X] T070 Reiniciar `hasPlayedOnce`, `lastPosition` y `stalledTicks` en `onMediaItemTransition` de `PlaybackRecoveryManager` (`app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackRecoveryManager.kt`) para que una emisora nueva use el umbral inicial de 10 s, y añadir el test correspondiente, per FR-001 (partial).
