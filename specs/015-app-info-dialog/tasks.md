# Tasks: Diálogo de información de la aplicación

**Input**: Design documents from `/specs/015-app-info-dialog/`

**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Incluidos — constitución principio III (Test-First, NON-NEGOTIABLE).

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Path Conventions

- **Main source**: `app/src/main/java/com/izquierdojl/tolocharadio/`
- **Test source**: `app/src/test/java/com/izquierdojl/tolocharadio/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: No setup needed — feature uses existing project structure.

> Este feature no requiere inicialización de proyecto. Los paquetes `feature/settings/` y `core/ui/components/` ya existen o se crean en las tareas correspondientes.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Modelo de datos que TODAS las historias necesitan

- [x] T001 Create `AppInfo` data class in `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModel.kt` with fields: `appName: String`, `version: String`, `repositoryUrl: String`, `developer: String`, `license: String`
- [x] T002 Create `AppInfoUiState` sealed interface in `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModel.kt` with `Hidden` and `Showing(info: AppInfo)` variants

**Checkpoint**: Data model complete — AppInfo and AppInfoUiState ready for use

---

## Phase 3: User Story 1 — Ver información general de la app (Priority: P1) MVP

**Goal**: El usuario pulsa "Acerca de" en Configuración y ve un diálogo con nombre, versión, desarrollador y licencia.

**Independent Test**: Navegar a Configuración → pulsar "Acerca de" → verificar que el diálogo muestra los datos esperados.

### Tests for User Story 1

- [x] T003 [P] [US1] Create `SettingsViewModelTest` additions in `app/src/test/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModelTest.kt` testing: showAppInfoDialog transitions to Showing, dismissAppInfoDialog transitions to Hidden, AppInfo contains correct static data

### Implementation for User Story 1

- [x] T004 [US1] Add `appInfoUiState: StateFlow<AppInfoUiState>` to `SettingsViewModel` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModel.kt` with initial state Hidden, and methods `showAppInfoDialog()` and `dismissAppInfoDialog()` to toggle state
- [x] T005 [US1] Create `AppInfoDialog` composable in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/AppInfoDialog.kt` with AlertDialog showing appName as title, version/developer/license as body text, and "Cerrar" button; accept `AppInfo` and `onDismiss` callback
- [x] T006 [US1] Integrate `AppInfoDialog` in `SettingsScreen` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt` — add "Acerca de" clickable row after logout button, observe `appInfoUiState` from ViewModel, show dialog when state is Showing

**Checkpoint**: User Story 1 complete — user can open info dialog and see app data

---

## Phase 4: User Story 2 — Acceder al repositorio desde el diálogo (Priority: P2)

**Goal**: El usuario pulsa el enlace del repositorio en el diálogo y se abre el navegador.

**Independent Test**: Abrir diálogo → pulsar "Ver repositorio" → verificar que se abre el navegador con la URL correcta.

### Implementation for User Story 2

- [x] T007 [US2] Add clickable "Ver repositorio" link in `AppInfoDialog` in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/AppInfoDialog.kt` — styled with primary color and underline, launches `Intent.ACTION_VIEW` with `repositoryUrl` from AppInfo
- [x] T008 [US2] Handle edge case: no browser installed — wrap `Intent.ACTION_VIEW` in try/catch in `AppInfoDialog`, fail silently without crash

**Checkpoint**: User Story 2 complete — user can open repo link from dialog

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Edge cases, accessibility, and validation

- [x] T009 [P] Add accessibility: `contentDescription` for "Acerca de" row — "Acerca de la aplicación" in `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt`; ensure repo link is announced as "Enlace" and close button as "Cerrar diálogo de información" in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/AppInfoDialog.kt`
- [x] T010 Run Detekt + ktlint on modified files: `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModel.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/AppInfoDialog.kt`
- [x] T011 Run quickstart.md validation scenarios from `specs/015-app-info-dialog/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: N/A — no setup needed
- **Phase 2 (Foundational)**: No dependencies — can start immediately
- **Phase 3 (US1)**: Depends on Phase 2 completion (T001-T002)
- **Phase 4 (US2)**: Depends on Phase 3 completion (T005-T006)
- **Phase 5 (Polish)**: Depends on all user stories complete

### User Story Dependencies

- **US1 (P1)**: Depends on Phase 2 only — core MVP
- **US2 (P2)**: Depends on US1 (needs AppInfoDialog to exist)

### Within Each User Story

- Tests MUST be written and FAIL before implementation (constitution III)
- Models before ViewModels
- ViewModels before UI
- UI before integration

### Parallel Opportunities

- T001, T002 can be written together (same file, sequential but fast)
- T003 can run in parallel with T004 (test file independent of implementation)
- T009 can run in parallel with T010 (accessibility vs lint)

---

## Parallel Example: Phase 3 (User Story 1)

```bash
# T003: Write ViewModel tests (can start while T004 is in progress)
# T004: Implement ViewModel state and methods
# T005: Create AppInfoDialog composable (depends on T001, T002)
# T006: Integrate in SettingsScreen (depends on T004, T005)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: T001-T002 (data model)
2. Complete Phase 3: T003-T006 (ViewModel + UI + integration)
3. **STOP and VALIDATE**: Navegar a Configuración → pulsar "Acerca de" → verificar diálogo
4. Deploy/demo if ready

### Incremental Delivery

1. Phase 2 → Foundation ready (data model complete)
2. Phase 3 (US1) → Test independently → **MVP!**
3. Phase 4 (US2) → Test independently → Deploy/Demo
4. Phase 5 → Polish and validate

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- Constitution III requires tests — included in Phase 3
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
