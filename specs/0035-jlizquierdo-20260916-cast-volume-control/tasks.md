---
description: "Task list for feature 0035 — Control de volumen del dispositivo Chromecast"
---

# Tasks: Control de volumen del dispositivo Chromecast

**Input**: Design documents from `/specs/0035-jlizquierdo-20260916-cast-volume-control/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/cast-volume.md,
quickstart.md

**Tests**: SÍ. La constitución (Principio III, Test-First NON-NEGOTIABLE) exige tests JUnit +
MockK/Turbine para `ViewModel` y lógica de player. Regla Red-Green: el test se escribe antes y
debe fallar antes de la implementación.

**Organization**: Tareas agrupadas por historia de usuario. Cada historia es
independientemente implementable y testeable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivo distinto, sin dependencias pendientes)
- **[Story]**: US1/US2/US3/US4
- Todas las tareas incluyen rutas de archivo exactas

## Path Conventions

Módulo único Android: `app/src/main/java/com/izquierdojl/tolocharadio/`,
`app/src/test/java/com/izquierdojl/tolocharadio/`. No hay `androidTest` nuevo (los flujos
críticos de la constitución no cambian; la validación de volumen es manual en dispositivo
real porque el emulador no descubre receptores Cast).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar un punto de partida verde y que no hacen falta dependencias nuevas.

- [x] T001 Ejecutar los gates obligatorios en la rama para confirmar base verde: `.\gradlew.bat testDebugUnitTest detekt ktlintCheck lintDebug` (raíz del repo)
- [x] T002 [P] Confirmar que no se añaden dependencias: media3 `1.4.1` y `play-services-cast-framework 21.5.0` ya están en `gradle/libs.versions.toml`; no editarlo (raíz del repo)
      *(Nota 2026-09-18: contradicho por la implementación — `7496a53` editó el toml subiendo media3 a 1.11.0 "para soporte nativo de volumen en CastPlayer", justificado en el commit. Los artefactos no se actualizaron entonces.)*

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Contrato del dispositivo remoto y controlador único de volumen/silencio; los usan
las 4 historias.

**⚠️ CRITICAL**: Ninguna historia puede empezar hasta completar esta fase.

### Tests for Foundational (Red-Green)

- [x] T003 Escribir el test (Red) de `PlaybackVolumeController` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackVolumeControllerTest.kt`: clamp 0..1 y rechazo de NaN; `setCastVolume`/`stepCastVolume` escriben en el dispositivo y actualizan estado; ajustar volumen estando silenciado des-silencia (FR-008); `toggleMute` aplica a remoto o a ExoPlayer según `remoteActive`; `bind` lee volumen/silencio reales y aplica el silencio vigente al receptor (FR-015); `unbind` aplica el silencio al reproductor local; `resetMute` (regla spec 004); setters remotos son no-op sin sesión (FR-004, FR-007, FR-015, data-model)

### Implementation for Foundational

- [x] T004 [P] Crear la interfaz `RemoteVolumeDevice` con KDoc (lectura/escritura de volumen y silencio + `observe`/`stopObserving`) en `app/src/main/java/com/izquierdojl/tolocharadio/cast/RemoteVolumeDevice.kt` (contracts/cast-volume.md §2, data-model)
- [x] T005 [P] Añadir el qualifier `@VolumeScope` y los providers de `CoroutineScope` (`SupervisorJob() + Dispatchers.Main.immediate`) y `PlaybackVolumeController` en `app/src/main/java/com/izquierdojl/tolocharadio/di/PlayerModule.kt` (plan.md Structure)
- [x] T006 Implementar `PlaybackVolumeController` con KDoc en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackVolumeController.kt` hasta poner T003 en verde (núcleo: estado, mapeos 0–100/0.0–1.0, mute, bind/unbind, resetMute; constante `VOLUME_CONFIRMATION_TIMEOUT_MS = 3_000`)

**Checkpoint**: Controlador y contrato listos — US1, US2, US3 y US4 pueden empezar.

---

## Phase 3: User Story 1 - Las teclas de volumen del teléfono controlan el volumen del dispositivo Cast (Priority: P1) 🎯 MVP

**Goal**: Con sesión Cast activa, las teclas físicas ajustan el volumen del receptor y
aparece la barra de volumen del sistema identificando la sesión remota.

**Independent Test**: Reproducir en Cast y pulsar volumen (o `adb shell input keyevent 24/25`):
el receptor cambia de volumen, el móvil no, y la barra del sistema aparece. Manual, quickstart
§3.1.

### Tests for User Story 1 (Red-Green)

- [x] T007 [US1] Escribir el test (Red) de `CastDeviceVolumePlayer` en `app/src/test/java/com/izquierdojl/tolocharadio/cast/CastDeviceVolumePlayerTest.kt`: `getAvailableCommands()` incluye los 5 comandos de volumen de dispositivo; `getDeviceInfo()` es remoto con `maxVolume = 100` y pasa a LOCAL cuando `castVolumeSupported = false`; la delegación de reproducción llega al `Player` delegado; los comandos de volumen siguen disponibles con el player en `Idle`/`Paused` (FR-013); `setDeviceVolume`/`increase`/`decrease` mapean a pasos 0–100 en el controlador; `emitDeviceVolumeChanged` notifica a los listeners registrados (contracts/cast-volume.md §1, research D2/D7)

### Implementation for User Story 1

- [x] T008 [US1] Implementar `CastDeviceVolumePlayer` con KDoc (delegación `Player by delegate` + comandos de volumen + `DeviceInfo` remoto/local según soporte + emisión de `onDeviceVolumeChanged`) en `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastDeviceVolumePlayer.kt` hasta poner T007 en verde (FR-001, FR-009, FR-011, FR-013, research D2/D7)
- [x] T009 [P] [US1] Implementar `CastSessionVolumeDevice` con KDoc (adaptador de `CastSession.setVolume/getVolume/setMute/isMute` + `Cast.Listener.onVolumeChanged` en `observe`/`stopObserving`) en `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastSessionVolumeDevice.kt` (FR-005, research D1/D4)
- [x] T010 [US1] Modificar `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt`: crear el wrapper en `createCastPlayer()`, pasar el wrapper a `mediaSession.setPlayer(...)`, `bind`/`unbind` del controlador con la `CastSession`, colectar `castVolume`+`muted` (scope `@VolumeScope`) para llamar a `emitDeviceVolumeChanged`, y cuando `castVolumeSupported` pase a `false` reinstalar el player de la sesión con una instancia nueva del wrapper (media3 vuelve a reproducción local y las teclas controlan el móvil) (FR-001, FR-009, FR-010, FR-011)
- [x] T011 [US1] Validación manual en dispositivo real según quickstart §3.1: teclas físicas cambian el volumen del receptor, el volumen del móvil no cambia, barra del sistema visible y, al desconectar, las teclas vuelven a controlar el móvil (SC-001, SC-002, SC-009)

**Checkpoint**: US1 funcional y validable de forma independiente (MVP).

---

## Phase 4: User Story 2 - Control deslizante de volumen coherente con el dispositivo (Priority: P1)

**Goal**: El slider del reproductor completo muestra y cambia el volumen real del receptor,
también al reabrir el panel o volver de segundo plano.

**Independent Test**: Con Cast conectado, abrir el full-player: el slider muestra el nivel
real; arrastrarlo cambia el volumen audible; cerrar y reabrir mantiene el valor real. Manual,
quickstart §3.2.

### Tests for User Story 2 (Red-Green)

- [x] T012 [US2] Actualizar (Red) `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt`: nuevo parámetro `PlaybackVolumeController` en `vm()` y casos de exposición de `castVolume` y de `setCastVolume` delegando en el controlador (FR-003, FR-004, FR-006)

### Implementation for User Story 2

- [x] T013 [US2] Exponer en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` `castVolume` (StateFlow) y `setCastVolume(Float)`, inyectando `PlaybackVolumeController`, hasta poner T012 en verde (FR-003, FR-006)
- [x] T014 [US2] Enlazar el slider de `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` al estado del ViewModel (eliminar `remember` local y la escritura de `activePlayer.volume`), ajuste continuo en `onValueChange`, etiqueta "Volumen" + nombre del dispositivo y `contentDescription`/`stateDescription` accesibles (FR-003, FR-004, FR-006)
- [x] T015 [US2] Validación manual según quickstart §3.2: valor real al abrir, arrastre continuo < 1 s, persistencia del valor al cerrar/reabrir y sin reinicio del stream (SC-003, SC-005, SC-007)

**Checkpoint**: US1 y US2 funcionales y validables de forma independiente.

---

## Phase 5: User Story 3 - Los cambios hechos desde el propio dispositivo se reflejan en la app (Priority: P2)

**Goal**: Los cambios de volumen originados en el receptor (mando del TV, Google Home, otro
teléfono) se reflejan en el slider en ≤ 2 s; si el receptor no admite volumen, se degrada con
aviso.

**Independent Test**: Cambiar volumen desde el mando del TV con la app abierta: el slider se
actualiza en ≤ 2 s. Con un receptor sin soporte: tras ~3 s el slider se oculta con aviso y la
reproducción continúa. Manual, quickstart §3.3 y §3.5.

### Tests for User Story 3 (Red-Green)

- [x] T016 [US3] Ampliar (Red) `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackVolumeControllerTest.kt`: eco equivalente (±1 paso) confirma y corrige el valor; eco divergente corrige al valor real; ventana de `VOLUME_CONFIRMATION_TIMEOUT_MS` (3 s) sin eco con `TestScope` avanza el tiempo y marca `castVolumeSupported = false` con aviso emitido una sola vez; pulsaciones consecutivas rápidas no disparan falsos "no soportado" (la ventana se cancela/reinicia con cada ajuste); `bind()` de nueva sesión resetea el soporte; sin avisos en fallos transitorios ya confirmados (FR-005, FR-009, FR-014, research D4/D7)

### Implementation for User Story 3

- [x] T017 [US3] Implementar en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackVolumeController.kt` la confirmación por eco, la ventana de detección `VOLUME_CONFIRMATION_TIMEOUT_MS` con cancelación por ajuste nuevo, `castVolumeSupported` y el aviso único, hasta poner T016 en verde (FR-009, FR-014)
- [x] T018 [US3] Exponer en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` `castVolumeSupported` y el flujo de aviso único (`SharedFlow`) para la UI (FR-009)
- [x] T019 [US3] En `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt`: ocultar el slider cuando `castVolumeSupported = false`, mostrar el aviso inline "Este dispositivo no permite ajustar el volumen desde el móvil" y snackbar único desde `MiniPlayer` (FR-009, contracts/cast-volume.md §3)
- [x] T020 [US3] Validación manual según quickstart §3.3 y §3.5: reflejo de cambio externo ≤ 2 s (repetir 5 veces), degradación correcta en receptor sin soporte (aviso + slider oculto + las teclas vuelven a controlar el volumen del móvil) y "Cast conectado en pausa" (FR-013) (SC-004, FR-009, FR-013, FR-014)

**Checkpoint**: US1, US2 y US3 funcionales.

---

## Phase 6: User Story 4 - Silenciar y restablecer el volumen del dispositivo Cast (Priority: P2)

**Goal**: El botón de silencio (mini-player y notificación) silencia el receptor y restaura
exactamente el nivel previo; el estado sobrevive al cambio local ↔ Cast.

**Independent Test**: Con Cast conectado, pulsar Silenciar: el receptor deja de sonar; volver a
pulsar restaura el nivel previo; silenciar en local y conectar al Cast arranca silenciado.
Manual, quickstart §3.4.

### Tests for User Story 4 (Red-Green)

- [x] T021 [US4] Actualizar (Red) `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt`: `isMuted` refleja el controlador en ambos sentidos, `toggleMute()` delega en el controlador (sin escribir `exoPlayer.volume` directamente), y `play`/`stop` llaman a `resetMute()` (FR-007, FR-008, FR-015)

### Implementation for User Story 4

- [x] T022 [US4] Modificar `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt`: `isMuted` desde el controlador, `toggleMute()` delegado y sustituir los escritos directos de `exoPlayer.volume` por `resetMute()` en `resetPlaybackSession()`/`stop()`, hasta poner T021 en verde (FR-007, FR-015)
- [x] T023 [US4] Modificar `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt`: el comando `CUSTOM_COMMAND_MUTE` delega en `PlaybackVolumeController.toggleMute()` (afecta al dispositivo activo; sin controles nuevos en la notificación) (FR-007, clarificación Q1)
- [x] T024 [US4] Validación manual según quickstart §3.4: silencio/restauración exacta en Cast, des-silencio al subir volumen, persistencia local ↔ Cast y MUTE desde la notificación (SC-006, FR-015)

**Checkpoint**: Las 4 historias funcionales de forma independiente.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Gates, auditoría de logs y validación end-to-end.

- [x] T025 [P] Auditar que los códigos nuevos/modificados (`CastPlayerManager.kt`, `CastDeviceVolumePlayer.kt`, `CastSessionVolumeDevice.kt`, `PlaybackVolumeController.kt`, `RadioPlaybackService.kt`) registran solo tag + causa + `code`/`status`, sin PII ni credenciales (constitución IV/V)
- [x] T026 Ejecutar los gates completos y dejarlos verdes: `.\gradlew.bat testDebugUnitTest detekt ktlintCheck lintDebug assembleDebug` (raíz del repo)
- [x] T027 Ejecutar quickstart completo en 2 receptores compatibles (p. ej. altavoz Nest y Google TV) y, si hay un Chromecast dongle sin CEC disponible, validar además la degradación de §3.5; regresión local (spec 004/011): §3.1–§3.5 y tabla SC-001…SC-009 (SC-008)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Setup; BLOQUEA US1–US4.
- **US1 (Phase 3)**: depende de Foundational. MVP.
- **US2 (Phase 4)**: depende de Foundational; no depende de US1 (el slider usa el controlador,
  no el wrapper), aunque comparten la misma sesión en pruebas manuales.
- **US3 (Phase 5)**: depende de Foundational y de la UI de US2 para el slider/aviso.
- **US4 (Phase 6)**: depende de Foundational; independiente de US1–US3 (solo toca VM,
  servicio y controlador).
- **Polish (Phase 7)**: depende de las historias deseadas.

### Within Each User Story

- Tests (Red) antes de la implementación (constitución III).
- Adaptadores antes que el manager; controlador antes que VM/UI.
- Validación manual al cerrar cada historia.

### Parallel Opportunities

- T004/T005 (interfaz y DI) en paralelo.
- T009 (adaptador Cast) en paralelo con T008 (wrapper).
- US2, US3 y US4 comparten `PlayerViewModel.kt`/`PlayerUi.kt`: ejecutarlas en secuencia para
  evitar conflictos de archivo. US4 podría adelantarse tras US2 si hay más de una persona.

---

## Parallel Example: User Story 1

```text
T007  Test Red-Green de CastDeviceVolumePlayer  (archivo de test nuevo)
T009  CastSessionVolumeDevice                  (archivo de producción nuevo)
```

---

## Implementation Strategy

### MVP First (User Story 1 solo)

1. Completar Phase 1 (Setup) y Phase 2 (Foundational).
2. Completar Phase 3 (US1): teclas físicas + barra del sistema.
3. **PARAR Y VALIDAR**: quickstart §3.1 en un receptor real.
4. Demo: ya se cumple la petición original (experiencia Pocket Casts).

### Incremental Delivery

1. US1 → MVP demostrable.
2. US2 → el slider deja de ser decorativo.
3. US3 → coherencia con cambios externos + degradación.
4. US4 → silencio fiable en Cast y notificación.
5. Polish → gates y validación multi-receptor.

---

## Notes

- [P] = distinto archivo, sin dependencias pendientes.
- Los tests de framework Android no usan Robolectric (no está en el proyecto): la lógica se
  aísla tras `RemoteVolumeDevice` y el wrapper solo depende de interfaces de media3
  (`research.md` D10).
- No editar `gradle/libs.versions.toml` (sin dependencias nuevas); no subir media3
  (`research.md` D3).
- Verificar que cada test Red falla antes de implementar y que los gates quedan verdes antes
  de cada checkpoint.

---

## Phase 8: Convergence

**Origen**: `/speckit.converge` 2026-09-20 — 15 FR, 9 SC, 4 historias, decisiones de `plan.md` y 5 principios de constitución revisados contra el código. Sin hallazgos funcionales: el silencio remoto (`CastSessionVolumeDevice`), el eco nativo, la vuelta a salida local y la notificación sin controles de volumen cumplen lo vigente; lo que la 0037 supersedó ya estaba fuera del código.

- [ ] T028 Completar la anotación de supersede iniciada en T018: marcar en `specs/0035-jlizquierdo-20260916-cast-volume-control/spec.md` los restos que siguen describiendo el deslizador in-app eliminado — US3 (historia completa: pruebas, AC1/AC2), FR-005, FR-006, FR-008, SC-003, SC-004, SC-005, Key Entities "Estado de volumen expuesto a la UI" y los edge cases del slider — como SUPERSEDED por 0037 FR-004/FR-008 (la representación es ahora la barra del sistema), y sustituir la sección "On-Device Testing Findings (2026-09-16)" y su "Decisión pendiente" por una nota de resolución (media3 1.11.0 desde `7496a53`: volumen de dispositivo nativo, teclas y barra validados en 0037 quickstart §3.2) per plan: supersede donde corresponda (partial) — MEDIUM
