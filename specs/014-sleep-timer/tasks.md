# Tasks: Temporizador de apagado (Sleep Timer)

**Input**: Design documents from `/specs/014-sleep-timer/`

**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Incluidos — constitución principio III (Test-First, NON-NEGOTIABLE).

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Main source**: `app/src/main/java/com/izquierdojl/tolocharadio/`
- **Test source**: `app/src/test/java/com/izquierdojl/tolocharadio/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: No setup needed — feature uses existing project structure.

> Este feature no requiere inicialización de proyecto. Los paquetes `domain/`, `feature/player/` y `core/ui/navigation/` ya existen.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Modelos de dominio y casos de uso que TODAS las historias necesitan

- [x] T001 Create `SleepTimerDuration` enum in `app/src/main/java/com/izquierdojl/tolocharadio/domain/SleepTimerUseCase.kt` with values MINUTES_15, MINUTES_30, MINUTES_45, MINUTES_60, MINUTES_90 and `minutes` property
- [x] T002 Create `SleepTimerState` sealed interface in `app/src/main/java/com/izquierdojl/tolocharadio/domain/SleepTimerUseCase.kt` with `Inactive` and `Active(durationMinutes, remainingSeconds, expiresAtEpochMs)` variants
- [x] T003 Implement `SleepTimerUseCase` class in `app/src/main/java/com/izquierdojl/tolocharadio/domain/SleepTimerUseCase.kt` with `state: Flow<SleepTimerState>`, `start(duration)`, and `cancel()` methods using coroutines delay for countdown
- [x] T004 [P] Create `SleepTimerUseCaseTest` in `app/src/test/java/com/izquierdojl/tolocharadio/domain/SleepTimerUseCaseTest.kt` testing: start activates timer, cancel returns to Inactive, expiration emits Inactive, restart replaces previous timer

**Checkpoint**: Domain layer complete — UseCase testable independently

---

## Phase 3: User Story 1 — Activar temporizador con duración predefinida (Priority: P1) MVP

**Goal**: El usuario selecciona una duración, el timer se activa, la reproducción se detiene al expirar.

**Independent Test**: Reproducir emisora → activar timer 15s (debug) → verificar que la reproducción se detiene.

### Tests for User Story 1

- [x] T005 [P] [US1] Create `SleepTimerViewModelTest` in `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/SleepTimerViewModelTest.kt` testing: start delegates to UseCase, expiration calls PlayerViewModel.stop(), state flows correctly

### Implementation for User Story 1

- [x] T006 [US1] Create `SleepTimerViewModel` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/SleepTimerViewModel.kt` with `@HiltViewModel`, `state: StateFlow<SleepTimerState>`, `start(duration)`, `cancel()` methods, and expiration callback to `PlayerViewModel.stop()`
- [x] T007 [US1] Create `SleepTimerButton` composable in `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/SleepTimerButton.kt` with `IconButton` (Timer icon), `DropdownMenu` with 5 duration options, and `BadgedBox` for active state indicator
- [x] T008 [US1] Integrate `SleepTimerButton` in `TopAppBar.actions` of `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` — add button between ViewModeToggle and Servers button, inject SleepTimerViewModel via hiltViewModel

**Checkpoint**: User Story 1 complete — user can activate timer and playback stops on expiry

---

## Phase 4: User Story 2 — Ver tiempo restante del temporizador (Priority: P2)

**Goal**: El usuario ve el tiempo restante en formato MM:SS al abrir el menú del timer activo.

**Independent Test**: Activar timer → esperar → abrir menú → verificar que muestra tiempo restante decreciente.

### Implementation for User Story 2

- [x] T009 [US2] Update `SleepTimerViewModel` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/SleepTimerViewModel.kt` to expose `remainingFormatted: StateFlow<String>` that formats remainingSeconds as "MM:SS"
- [x] T010 [US2] Update `SleepTimerButton` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/SleepTimerButton.kt` — when state is Active: show filled Timer icon, add badge with `remainingFormatted`, update DropdownMenu to show "Quedan MM:SS" header

**Checkpoint**: User Story 2 complete — user sees countdown in badge and menu

---

## Phase 5: User Story 3 — Cancelar temporizador activo (Priority: P2)

**Goal**: El usuario cancela el timer activo desde el menú y la reproducción continúa.

**Independent Test**: Activar timer → abrir menú → cancelar → verificar que botón vuelve a estado normal y reproducción continúa.

### Implementation for User Story 3

- [x] T011 [US3] Update `SleepTimerButton` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/SleepTimerButton.kt` — in Active state DropdownMenu, add "Cancelar temporizador" option below "Quedan MM:SS" with divider, call `onCancel` callback

**Checkpoint**: User Story 3 complete — user can cancel active timer

---

## Phase 6: User Story 4 — El temporizador persiste al navegar (Priority: P3)

**Goal**: El timer sigue activo al navegar entre secciones y se muestra el indicador en todas las pantallas.

**Independent Test**: Activar timer → navegar a Favoritos → verificar badge visible → navegar a Historial → verificar badge visible.

### Implementation for User Story 4

- [x] T012 [US4] Verify `SleepTimerViewModel` scope in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` — ensure it uses `hiltViewModel(LocalContext.current as ComponentActivity)` for Activity-scoped persistence (same pattern as PlayerViewModel)
- [x] T013 [US4] Verify `SleepTimerButton` visibility in all authenticated routes — button must appear in TopAppBar regardless of currentRoute (not gated by VIEW_MODE_ROUTES)

**Checkpoint**: User Story 4 complete — timer persists across navigation

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Edge cases, accessibility, and validation

- [x] T014 [P] Handle edge case: timer expiration when playback is paused — in `SleepTimerUseCase`, skip `stop()` call if player state is Paused, just transition to Inactive
- [x] T015 [P] Handle edge case: new timer replaces previous — in `SleepTimerUseCase.start()`, cancel existing coroutine before starting new countdown
- [x] T016 [P] Add accessibility: `contentDescription` for SleepTimerButton icon — "Temporizador de apagado" (inactive) / "Temporizador activo, quedan X minutos" (active)
- [x] T017 Run Detekt + ktlint on modified files: `app/src/main/java/com/izquierdojl/tolocharadio/domain/SleepTimerUseCase.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/SleepTimerViewModel.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/SleepTimerButton.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`
- [x] T018 Run quickstart.md validation scenarios from `specs/014-sleep-timer/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: N/A — no setup needed
- **Phase 2 (Foundational)**: No dependencies — can start immediately
- **Phase 3 (US1)**: Depends on Phase 2 completion (T001-T004)
- **Phase 4 (US2)**: Depends on Phase 3 completion (T006-T008)
- **Phase 5 (US3)**: Depends on Phase 3 completion (T006-T008)
- **Phase 6 (US4)**: Depends on Phase 3 completion (T006-T008)
- **Phase 7 (Polish)**: Depends on all user stories complete

### User Story Dependencies

- **US1 (P1)**: Depends on Phase 2 only — core MVP
- **US2 (P2)**: Depends on US1 (needs SleepTimerViewModel and SleepTimerButton to exist)
- **US3 (P2)**: Depends on US1 (needs SleepTimerButton to exist)
- **US4 (P3)**: Depends on US1 (needs SleepTimerViewModel scope verification)

### Within Each User Story

- Tests MUST be written and FAIL before implementation (constitution III)
- Models before ViewModels
- ViewModels before UI
- UI before integration

### Parallel Opportunities

- T001, T002 can be written together (same file, sequential but fast)
- T004 can run in parallel with T003 (test file independent of implementation)
- T005 can run in parallel with T006 (test file independent of ViewModel)
- T009, T010 can run in parallel with T011 (US2 and US3 modify same files but different sections)
- T014, T015, T016 can all run in parallel (different concerns)

---

## Parallel Example: Phase 2 (Foundational)

```bash
# T001 + T002: Create models in SleepTimerUseCase.kt (sequential, same file)
# T003: Implement UseCase (depends on T001, T002)
# T004: Write UseCase tests (can start while T003 is in progress)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: T001-T004 (models + UseCase + tests)
2. Complete Phase 3: T005-T008 (ViewModel + UI + integration)
3. **STOP and VALIDATE**: Reproducir emisora → activar timer → verificar detención
4. Deploy/demo if ready

### Incremental Delivery

1. Phase 2 → Foundation ready (UseCase tested)
2. Phase 3 (US1) → Test independently → **MVP!**
3. Phase 4 (US2) → Test independently → Deploy/Demo
4. Phase 5 (US3) → Test independently → Deploy/Demo
5. Phase 6 (US4) → Test independently → Deploy/Demo
6. Phase 7 → Polish and validate

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- Constitution III requires tests — included in Phase 2 and Phase 3
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
