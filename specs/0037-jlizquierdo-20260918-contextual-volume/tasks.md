---
description: "Task list for feature 0037 — Volumen contextual único estilo Pocket Casts"
---

# Tasks: Volumen contextual único estilo Pocket Casts

**Input**: Design documents from `/specs/0037-jlizquierdo-20260918-contextual-volume/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md,
contracts/contextual-volume.md, quickstart.md

**Tests**: SÍ. La constitución (Principio III, Test-First NON-NEGOTIABLE) exige Red-Green
para la lógica del controlador y del ViewModel (JUnit + MockK + Turbine). `CastPlayerManager`
no tiene arnés JVM (acoplado a Android/Cast): se valida en dispositivo real según
quickstart.md, como en la 0035.

**Organization**: Tareas agrupadas por historia de usuario; cada historia es
independientemente implementable y validable. La feature es sustractiva: elimina el
pipeline de escritura de volumen (bug 0036), el deslizador in-app, el wrapper muerto y la
maquinaria de degradación.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivo distinto, sin dependencias pendientes)
- **[Story]**: US1/US2/US3/US4
- Todas las tareas incluyen rutas de archivo exactas

## Path Conventions

Módulo único Android: `app/src/main/java/com/izquierdojl/tolocharadio/`,
`app/src/test/java/com/izquierdojl/tolocharadio/`. Sin `androidTest` nuevo (validación de
volumen manual en dispositivo real: el emulador no descubre receptores Cast).

---

## Phase 1: Setup

**Purpose**: Confirmar base verde y que no hacen falta dependencias nuevas.

- [x] T001 Ejecutar los gates obligatorios en la rama para confirmar base verde: `.\gradlew.bat testDebugUnitTest detekt ktlintCheck lintDebug` (raíz del repo)
- [x] T002 [P] Confirmar media3 queda en `1.11.0` y NO editar `gradle/libs.versions.toml`: el soporte nativo de volumen del `CastPlayer` ya está en el binario; esta feature no añade ni sube dependencias (plan.md Technical Context, research D1)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Eliminar el código muerto compartido sobre el que actúan todas las historias.

**⚠️ CRITICAL**: Ninguna historia puede empezar hasta completar esta fase.

- [x] T003 [P] Eliminar el wrapper muerto y su test: borrar `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastDeviceVolumePlayer.kt` y `app/src/test/java/com/izquierdojl/tolocharadio/cast/CastDeviceVolumePlayerTest.kt` (nunca instanciado desde 0035; research D4; regla dura del contrato §1: sin wrappers en el player de sesión). Gates siguen verdes

**Checkpoint**: Sin referencias al wrapper; base limpia para US1–US4.

---

## Phase 3: User Story 1 - Al conectar al Cast, el volumen del receptor no cambia (Priority: P1) 🎯 MVP

**Goal**: Nadie escribe volumen en el receptor como efecto de conectar/reconectar: muere
el bug 0036 (salto al 100%) y la app deja de gestionar la degradación por receptor sin
volumen (FR-011, Q1: modelo Pocket Casts).

**Independent Test**: Receptor al 30% → conectar → suena al 30% sin salto; desconectar y
reconectar → el volumen del receptor no cambia (quickstart §3.1, SC-001). Manual en
dispositivo real.

### Implementation for User Story 1

- [x] T004 [US1] En `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt`: eliminar el pipeline de escritura de volumen — `startVolumeEvents()`, `emitDeviceVolume()`, el campo `volumeEventsJob` (con su cancelación en `releaseCastPlayer()`) y la llamada a `startVolumeEvents()` en `createCastPlayer()` (FR-001, research D3: el collector emite el 1.0 por defecto y `setDeviceVolume` lo escribe al receptor)
- [x] T005 [US1] En `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt`: eliminar la maquinaria de degradación — `observeVolumeSupport()`, `reinstallSessionPlayerAsLocal()`, `volumeSupportJob` y su llamada en `init` (FR-011: sin gestión especial) — y corregir el KDoc de `createCastPlayer()` para que describa la realidad: la sesión recibe el `CastPlayer` crudo con volumen de dispositivo nativo de media3 1.11.0 (contracts §1)
- [x] T006 [US1] Validación manual según quickstart §3.1: receptor al 30%, conectar (sin salto al 100% ni en sonido ni en barra), desconectar/reconectar y pérdida de red: el volumen del receptor permanece inalterado (SC-001, FR-001)

**Checkpoint**: Bug 0036 muerto de raíz; US1 validable de forma independiente (MVP).

---

## Phase 4: User Story 2 - Una sola franja de volumen contextual (Priority: P1)

**Goal**: Con Cast activo, el único control de volumen visible es el del sistema; el
full-player pierde su deslizador y el ViewModel sus exposiciones de volumen.

**Independent Test**: Con Cast activo, teclas → barra del sistema identifica el
dispositivo y solo cambia el receptor; full-player sin deslizador; local → teclas del
teléfono (quickstart §3.2, SC-002/003/004).

### Tests for User Story 2 (Red-Green)

- [x] T007 [US2] Actualizar `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` (eliminación de casos obsoletos; la Red real de la feature está en T011): eliminar los casos de `castVolume`/`setCastVolume`/`castVolumeSupported`/avisos de volumen; conservar `isMuted`/`toggleMute()` delegando en el controlador y `resetMute()` en play/stop (FR-004, FR-006)

### Implementation for User Story 2

- [x] T008 [US2] Eliminar las exposiciones de volumen de `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` (`castVolume`, `setCastVolume`, `castVolumeSupported`, flujo de avisos) hasta poner T007 en verde (FR-004)
- [x] T009 [US2] En `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt`: eliminar el componible `CastVolumeControl`, su invocación en `FullPlayerSheet`, los `collectAsState` de volumen/soporte y el snackbar de avisos de volumen; limpiar imports (FR-004, contracts §3)
- [x] T010 [US2] Validación manual según quickstart §3.2: teclas en local (volumen del teléfono), teclas con Cast (solo receptor, barra con nombre del dispositivo), full-player sin control de volumen, app en segundo plano, desconexión → teclas al teléfono al primer intento (SC-002, SC-003, SC-004, FR-002/003/010)

**Checkpoint**: Una sola franja; US1 y US2 funcionales de forma independiente.

---

## Phase 5: User Story 3 - Silencio coherente y respetuoso con el receptor (Priority: P2)

**Goal**: El controlador se reduce al silencio con la semántica nueva de `bind()`: solo
escribe `writeMuted(true)` si la app estaba silenciada; nunca des-silencia un receptor
silenciado externamente (lo lee y representa).

**Independent Test**: App silenciada en local → conectar → receptor silenciado; receptor
silenciado con el mando + app sin silencio → conectar → permanece silenciado y el botón lo
refleja (quickstart §3.3, SC-005).

### Tests for User Story 3 (Red-Green)

- [x] T011 [US3] Reescribir (Red) `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlaybackVolumeControllerTest.kt` a semántica solo-silencio: `toggleMute`/`setMuted` aplican a remoto o `ExoPlayer` según `isRemoteActive` y marcan origen usuario; `bind()` con app silenciada → `writeMuted(true)` (carry-over, FR-006); `bind()` con app sin silencio → NO escribe nada y representa el silencio real del receptor con origen eco (FR-005); eco de silencio del receptor sincroniza `muted` sin avisos marcando origen eco; `unbind()` aplica el silencio al local SOLO si su origen es usuario, y con origen eco lo descarta (`muted → false`, el local conserva su volumen) (FR-006); `resetMute()`; sin estado de volumen (contracts §2, data-model §Estado e invariante 4)

### Implementation for User Story 3

- [x] T012 [US3] Reducir `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackVolumeController.kt` a solo-silencio hasta poner T011 en verde: eliminar `castVolume`, `castVolumeSupported`, `notices`, `pendingVolume`, `VOLUME_CONFIRMATION_TIMEOUT_MS` y las funciones de volumen (`setCastVolume`, `stepCastVolume`, `deviceVolumePercent`, `setDeviceVolumePercent`); implementar `bind()` según contracts §2 con campo interno de origen del silencio (usuario/eco) que gobierna `unbind()` (data-model §Estado e invariante 4)
- [x] T013 [US3] Reducir el contrato y el adaptador a silencio: eliminar `readVolume`/`writeVolume` de `app/src/main/java/com/izquierdojl/tolocharadio/cast/RemoteVolumeDevice.kt` y de `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastSessionVolumeDevice.kt` (conservar `readMuted`/`writeMuted`/`observe`/`stopObserving` sobre `CastSession.setMute/isMute` + `Cast.Listener.onVolumeChanged`) (data-model §RemoteVolumeDevice; depende de T012)
- [x] T014 [US3] Verificar en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt` que `CUSTOM_COMMAND_MUTE` y el botón de silencio del mini-player siguen delegando en `toggleMute()` sin cambios (FR-006; ajustar solo si la compilación lo exige — plan: SIN CAMBIOS)
- [x] T015 [US3] Validación manual según quickstart §3.3: carry-over local↔Cast (origen usuario), receptor silenciado externamente permanece silenciado tras conectar (SC-005), des-silencio restaura el nivel exacto, subir volumen des-silencia en salida Cast (nativo, FR-007), desconectar con silencio externo adoptado → el local retoma con sonido y el botón queda desactivado (FR-006), MUTE desde notificación en background (FR-005/006/007)

**Checkpoint**: US1, US2 y US3 funcionales; el silencio sobrevive al cambio de salida.

---

## Phase 6: User Story 4 - El volumen visible es el real del dispositivo (Priority: P3)

**Goal**: Verificar el comportamiento nativo de media3 1.11.0: eco de cambios externos
≤ 2 s y teclas en background. Sin cambios de código esperados.

**Independent Test**: Cambiar volumen desde el mando del TV con Cast activo → la barra del
sistema refleja el nivel en ≤ 2 s (quickstart §3.4, SC-006).

- [x] T016 [US4] Validación manual según quickstart §3.4: eco de cambios externos ≤ 2 s (5 repeticiones), teclas muy rápidas sin desincronización, ajuste con app en background (SC-006, FR-008/010 — nativo del `CastPlayer` 1.11.0)
- [x] T017 [US4] Edge cases de quickstart §3.5: auriculares BT + Cast (teclas → Cast), Cast en pausa (teclas disponibles), regresión local (play/stop des-silencia, volumen del teléfono intacto); **regla de parada**: cualquier síntoma de audio roto tras conectar → parar y diagnosticar (research D6), sin tocar el wiring del player de sesión

**Checkpoint**: Las 4 historias completas y validadas.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Documentación de supersede, gates completos y validación end-to-end.

- [x] T018 [P] Anotar supersede en `specs/0035-jlizquierdo-20260916-cast-volume-control/spec.md`: marcar FR-003/FR-004 (deslizador in-app) y FR-009 (degradación con aviso) como SUPERSEDED por 0037 (FR-004/FR-011), con nota en User Story 2 (FR-004, FR-011)
- [x] T019 [P] Anotar en `specs/0035-jlizquierdo-20260916-cast-volume-control/plan.md` la nota de obsolescencia (media3 real 1.11.0 desde 7496a53; wrapper/pipeline/D11 obsoletos, modelo vigente en 0037) y en `specs/0035-jlizquierdo-20260916-cast-volume-control/tasks.md` la corrección de T002 (editó el toml justificadamente) (research D1, plan.md Structure Decision)
- [x] T020 Ejecutar los gates completos y dejarlos verdes: `.\gradlew.bat testDebugUnitTest detekt ktlintCheck lintDebug assembleDebug` (raíz del repo)
- [x] T021 Ejecutar el quickstart completo §3.1–§3.5 y la tabla SC-001…SC-007 en dispositivo real; regresión de volumen local y de la notificación multimedia (SC-001…SC-007)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Setup; BLOQUEA US1–US4.
- **US1 (Phase 3)**: depende de Foundational. MVP. Debe completarse ANTES que US3
  (el manager deja de referenciar `castVolumeSupported` antes de que US3 lo elimine del
  controlador).
- **US2 (Phase 4)**: depende de Foundational; independiente de US1/US3 (toca VM/UI).
- **US3 (Phase 5)**: depende de Foundational y de US1 (por el orden manager→controlador
  descrito arriba); toca controlador/`cast/`, no VM/UI.
- **US4 (Phase 6)**: depende de US1–US3 (validación end-to-end del comportamiento nativo).
- **Polish (Phase 7)**: depende de las historias deseadas.

### Within Each User Story

- Tests (Red) antes de la implementación (constitución III) en US2 y US3.
- En US1, ambas tareas tocan `CastPlayerManager.kt`: ejecutarlas en secuencia.
- Validación manual al cerrar cada historia.

### Parallel Opportunities

- T002 (confirmación de dependencias) en paralelo con T001.
- T003 (borrado del wrapper) es el único cambio de Foundational.
- US2 y US3 pueden ir en paralelo tras US1 si hay más de una persona (archivos disjuntos:
  VM/UI vs controlador/`cast/`).
- T018/T019 (anotaciones de docs 0035) en paralelo entre sí.

---

## Parallel Example: User Story 3

```text
T011  Test Red-Green del controlador solo-silencio  (archivo de test reescrito)
      → T012  Reducción del controlador              (PlaybackVolumeController.kt)
        → T013  Contrato + adaptador solo-silencio    (RemoteVolumeDevice.kt, CastSessionVolumeDevice.kt)
T014  Verificación del servicio (archivo distinto, sin dependencias de T012/T013)
```

---

## Implementation Strategy

### MVP First (User Story 1 solo)

1. Completar Phase 1 (Setup) y Phase 2 (Foundational).
2. Completar Phase 3 (US1): eliminar el pipeline de escritura → bug 0036 muerto.
3. **PARAR Y VALIDAR**: quickstart §3.1 en receptor real (receptor al 30% → conectar →
   sin salto).
4. Demo: ya se cumple la queja más dolorosa (el salto al 100% al conectar).

### Incremental Delivery

1. US1 → MVP demostrable (volumen del receptor intacto al conectar).
2. US2 → una sola franja (el slider in-app desaparece).
3. US3 → silencio coherente + controlador reducido.
4. US4 → verificación del eco nativo y edge cases.
5. Polish → supersede documentado, gates y validación completa.

---

## Notes

- [P] = distinto archivo, sin dependencias pendientes.
- Regla dura (contracts §1, research D6): **nunca envolver el player de la sesión** — el
  `CastPlayer` crudo permanece como player de la sesión; ante cualquier síntoma de audio
  roto tras conectar, parar y diagnosticar (quickstart §3.5.4).
- No editar `gradle/libs.versions.toml` (media3 1.11.0 ya soporta volumen de dispositivo
  nativo; research D1).
- `CastPlayerManager` y la UI de volumen se validan manualmente (sin arnés JVM ni
  Robolectric en el proyecto), como en la 0035; el controlador y el ViewModel sí llevan
  Red-Green (constitución III).
- Verificar que cada test Red falla antes de implementar y que los gates quedan verdes en
  cada checkpoint.
