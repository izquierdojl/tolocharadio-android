# Tasks: Gestión de Sesión Persistente y Credenciales

**Input**: Design documents from `/specs/007-persistent-session/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Sí — FR-013 requiere tests unitarios y de integración para toda funcionalidad nueva.

**Organization**: Tasks agrupados por user story para implementación y testing independiente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Base path: `app/src/main/java/com/izquierdojl/tolocharadio/`
Test path: `app/src/test/java/com/izquierdojl/tolocharadio/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [x] T001 Verify existing project structure and dependencies (Room, DataStore, Hilt already configured)
- [x] T002 Add EncryptedSharedPreferences dependency to `app/build.gradle` if not present
- [x] T003 Create package structure: `auth/data/local/`, `servers/data/local/`, `servers/data/model/`, `servers/domain/usecase/`, `servers/domain/model/`, `servers/ui/`, `common/data/`

---

## Phase 2: Foundational (US1 + US3 — Persistencia y Seguridad)

**Purpose**: Core infrastructure for secure session persistence — BLOCKS all user stories

**?? CRITICAL**: No user story work can begin until this phase is complete

**Goal**: La sesión de autenticación persiste de forma segura entre cierres de la app, y los tokens se almacenan cifrados.

**Independent Test**: Hacer login, cerrar app, reabrir → sesión restaurada sin pedir credenciales. Tokens en EncryptedSharedPreferences, access token solo en memoria.

### Tests for Foundational

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T004 [P] Unit test for EncryptedTokenStorage (save/retrieve/delete refreshToken, error handling) in `test/auth/data/local/EncryptedTokenStorageTest.kt`
- [x] T005 [P] Unit test for SessionManager (accessToken memory-only, isAuthenticated state) in `test/auth/data/local/SessionManagerTest.kt`
- [x] T006 [P] Unit test for AuthRepository (login persists tokens, logout clears, restore from storage) in `test/auth/data/repository/AuthRepositoryTest.kt`
- [x] T007 [P] Unit test for RestoreSessionUseCase (valid session, expired token, invalid token, no session) in `test/auth/domain/usecase/RestoreSessionUseCaseTest.kt`

### Implementation for Foundational

- [x] T008 [P] Create AuthSession sealed class (Loading, Authenticated, Unauthenticated) in `auth/domain/model/AuthSession.kt`
- [x] T009 [P] Create EncryptedTokenStorage with AES-256 GCM via EncryptedSharedPreferences in `auth/data/local/EncryptedTokenStorage.kt`
- [x] T010 [P] Create SessionManager (in-memory accessToken, user, isAuthenticated StateFlow) in `auth/data/local/SessionManager.kt`
- [x] T011 Create AuthRepository (login → POST /auth/login → save tokens, logout → POST /auth/logout → clear tokens, restore → check EncryptedStorage → refresh if needed) in `auth/data/repository/AuthRepository.kt` (depends on T009, T010)
- [x] T012 Create RestoreSessionUseCase (check EncryptedStorage, validate with refresh, return AuthSession state) in `auth/domain/usecase/RestoreSessionUseCase.kt` (depends on T011)
- [x] T013 Create LoginUseCase (delegate to AuthRepository.login, handle errors) in `auth/domain/usecase/LoginUseCase.kt` (depends on T011)
- [x] T014 Create LogoutUseCase (delegate to AuthRepository.logout, clear session) in `auth/domain/usecase/LogoutUseCase.kt` (depends on T011)
- [x] T015 Update LoginViewModel to use LoginUseCase and persist session on success in `auth/ui/LoginViewModel.kt` (depends on T013)
- [x] T016 Create SessionRestorer (singleton that runs on app startup, calls RestoreSessionUseCase) in `auth/data/SessionRestorer.kt` (depends on T012)
- [x] T017 Integrate SessionRestorer in Application.onCreate or MainActivity to restore session on app start (depends on T016)
- [x] T018 Implement error handling for EncryptedSharedPreferences failures (log structured, redirect to Login) in `auth/data/local/EncryptedTokenStorage.kt` (depends on T009)
- [x] T019 Update LogoutUseCase to call POST /auth/logout before clearing local tokens in `auth/domain/usecase/LogoutUseCase.kt` (depends on T014)
- [x] T020 Run foundational tests — verify all pass

**Checkpoint**: Sesión persistente funcional — login, cierre de app, reabrir → sesión restaurada. Tokens cifrados. Logout limpia todo.

---

## Phase 3: User Story 2 — Gestión de servidores guardados (Priority: P2)

**Goal**: El usuario puede guardar múltiples instancias TolochaRadio, cambiar entre ellas, y cada una mantiene sus credenciales independientes. La caché se limpia al cambiar.

**Independent Test**: Desde gestión de servidores, añadir servidor, ver lista, seleccionar diferente → app cambia instancia con credenciales guardadas.

### Tests for User Story 2

- [x] T021 [P] Unit test for ServerDao (insert, getAll, getDefault, setDefault, delete) in `test/servers/data/local/ServerDaoTest.kt`
- [x] T022 [P] Unit test for ServerRepository (add with validation, switch, delete with cleanup) in `test/servers/data/repository/ServerRepositoryTest.kt`
- [x] T023 [P] Unit test for SwitchServerUseCase (clear cache, load new credentials, restore session) in `test/servers/domain/usecase/SwitchServerUseCaseTest.kt`
- [x] T024 [P] Unit test for AddServerUseCase (URL normalization, health check, save) in `test/servers/domain/usecase/AddServerUseCaseTest.kt`

### Implementation for User Story 2

- [x] T025 [P] Create SavedServerEntity Room entity (id, url, alias, appName, isDefault, createdAt) in `servers/data/model/SavedServerEntity.kt`
- [x] T026 [P] Create SavedServerDao (insert, getAll, getByUrl, getDefault, setDefault, delete) in `servers/data/local/ServerDao.kt`
- [x] T027 Add SavedServerEntity to Room database (new table or migration) in `servers/data/local/ServerDatabase.kt` (depends on T025, T026)
- [x] T028 [P] Create SavedServer domain model (id, url, alias, appName, isDefault) in `servers/domain/model/SavedServer.kt`
- [x] T029 Create URL normalization utility (strip trailing slash, lowercase host, preserve scheme/port) in `common/data/UrlNormalizer.kt`
- [x] T030 Create ServerRepository (add with /health validation, getAll, switch, delete with credential cleanup) in `servers/data/repository/ServerRepository.kt` (depends on T026, T029)
- [x] T031 Create CacheManager (DELETE FROM favorites, history, custom-stations in Room) in `common/data/CacheManager.kt`
- [x] T032 Create AddServerUseCase (validate URL → /health, normalize, save, fetch appName from /config) in `servers/domain/usecase/AddServerUseCase.kt` (depends on T030)
- [x] T033 Create GetServersUseCase (return Flow<List<SavedServer>> from repository) in `servers/domain/usecase/GetServersUseCase.kt` (depends on T030)
- [x] T034 Create SwitchServerUseCase (clear cache via CacheManager, update active server, restore session from new server's credentials) in `servers/domain/usecase/SwitchServerUseCase.kt` (depends on T030, T031)
- [x] T035 Create DeleteServerUseCase (delete server + its credentials + cache) in `servers/domain/usecase/DeleteServerUseCase.kt` (depends on T030, T031)
- [x] T036 Create ServerListViewModel (list servers, add, switch, delete, UI state) in `servers/ui/ServerListViewModel.kt` (depends on T032, T033, T034, T035)
- [x] T037 Create ServerListScreen (list with active indicator, add button, delete action, selection) in `servers/ui/ServerListScreen.kt` (depends on T036)
- [x] T038 Create AddServerDialog (URL input, validation feedback, alias input) in `servers/ui/AddServerDialog.kt` (depends on T036)
- [x] T039 Add navigation route to ServerListScreen from Perfil/Ajustes in navigation graph (depends on T037)
- [x] T040 Create migration logic: detect existing baseUrl in DataStore → create first SavedServer → migrate credentials → clear old DataStore in `servers/data/MigrationHelper.kt` (depends on T030)
- [x] T041 Run US2 tests — verify all pass

**Checkpoint**: Gestión de servidores funcional — añadir, ver, cambiar, eliminar servidores. Caché limpia al cambiar. Migración desde baseUrl.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T042 [P] Verify zero tokens in logs (Logcat review, ProGuard rules) across all auth flows
- [x] T043 [P] Add structured error logging for EncryptedSharedPreferences failures (tag + cause, no PII) in `auth/data/local/EncryptedTokenStorage.kt`
- [X] T044 [P] Add offline state handling: server unreachable during restore → show offline with retry, not login screen — verificado manualmente en emulador por el usuario (2026-09-06)
- [X] T045 Run quickstart.md validation scenarios (all 6 scenarios + security checks) — verificado manualmente en emulador por el usuario (2026-09-06)
- [X] T046 [P] Code cleanup: remove any dead code from old baseUrl-only logic, verify KDoc on public APIs — verificado manualmente en emulador por el usuario (2026-09-06)
- [X] T047 Run lint (ktlint + Detekt + Android Lint) — fix any new warnings

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories
- **US2 (Phase 3)**: Depends on Foundational (needs EncryptedTokenStorage for server credentials)
- **Polish (Phase 4)**: Depends on all phases complete

### User Story Dependencies

- **US1 + US3 (Foundational)**: Can start after Setup — no dependencies on other stories
- **US2**: Depends on Foundational — uses EncryptedTokenStorage for per-server credentials

### Within Foundational Phase

- Tests (T004-T007) FIRST, parallel
- Domain models (T008) before services
- Storage (T009, T010) parallel, before repository
- Repository (T011) before use cases
- Use cases (T012-T014) parallel, before UI
- UI (T015-T017) after use cases
- Error handling (T018-T019) after core implementation

### Within US2 Phase

- Tests (T021-T024) FIRST, parallel
- Entity + DAO (T025-T027) parallel, before repository
- Domain model (T028) parallel with entity
- Repository (T030) before use cases
- Use cases (T032-T035) parallel, before UI
- UI (T036-T039) after use cases
- Migration (T040) after repository

---

## Parallel Example: Foundational Phase

```bash
# Launch all tests together:
Task: "T004 Unit test EncryptedTokenStorage"
Task: "T005 Unit test SessionManager"
Task: "T006 Unit test AuthRepository"
Task: "T007 Unit test RestoreSessionUseCase"

# Launch models and storage together:
Task: "T008 Create AuthSession sealed class"
Task: "T009 Create EncryptedTokenStorage"
Task: "T010 Create SessionManager"
```

---

## Implementation Strategy

### MVP First (Foundational Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (US1 + US3)
3. **STOP and VALIDATE**: Login → close app → reopen → session restored
4. Deploy/demo if ready — this solves the main user complaint

### Incremental Delivery

1. Setup + Foundational → Session persistence working (MVP!)
2. Add US2 → Server management working
3. Polish → Production ready

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup together
2. Once Setup is done:
   - Developer A: EncryptedTokenStorage + SessionManager (T009, T010)
   - Developer B: Tests (T004-T007)
   - Developer C: Domain models + use cases (T008, T012-T014)
3. Integration: AuthRepository + UI (T011, T015-T017)
4. Then US2 in parallel with Polish

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (TDD per constitution)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently

---

## Phase 5: Convergence — Enmienda: Servidores de primer nivel + activo/por defecto + Configuración

**Purpose**: Rediseño conforme al modelo del usuario (spec enmendada 2026-09-06): sección Servidores propia, servidor activo ≠ por defecto, credenciales token+password por servidor, Configuración sustituye a Perfil, pantalla de arranque seleccionable.

- [X] T048 [P] Extend SavedServerEntity with `userEmail`, `isActive` and keep `isDefault`; Room migration v5 → v6 in `data/local/servers/SavedServerEntity.kt` + `data/local/TolochaDb.kt` (missing, FR-005, C2)
- [X] T049 Update ServerDao: queries for active server (`getActive`, `clearActive`, `setActive`) alongside default ones in `data/local/servers/ServerDao.kt` (missing, FR-005, C2)
- [X] T050 [P] Extend TokenStore (or new `CredentialStore`) to persist per-server encrypted password and email in `core/session/TokenStore.kt` (missing, FR-005, C3)
- [X] T051 Update ServerRepository.add() to store email+password cifrados and set both active+default on first server in `data/repo/servers/ServerRepository.kt` (partial, FR-005)
- [X] T052 Update SwitchServerUseCase: auto-login with stored credentials (token o password), set active without touching default in `domain/servers/SwitchServerUseCase.kt` (partial, FR-006, C2)
- [X] T053 Implement silent re-login: on refresh revocation use stored password to re-authenticate without user interaction in `domain/auth/RestoreSessionUseCase.kt` + `data/repo/AuthRepo.kt` (missing, FR-006b)
- [X] T054 Update ServerRepository.switchTo() to NOT modify isDefault (only isActive) in `data/repo/servers/ServerRepository.kt` (contradicts, C2)
- [X] T055 [P] Create AppSettings in InstancePrefs: `startScreen` preference (favorites|history|explore, default explore) in `data/local/InstancePrefs.kt` (missing, FR-011b)
- [X] T056 Create SettingsViewModel for Configuración (theme mode, start screen, logout) in `feature/settings/SettingsViewModel.kt` (missing, FR-011, US4)
- [X] T057 Create SettingsScreen: tema claro/oscuro selector, pantalla de arranque selector (Favoritos/Historial/Explorar), botón Cerrar sesión in `feature/settings/SettingsScreen.kt` (missing, FR-011, US4)
- [X] T058 Replace ProfileScreen with SettingsScreen in navigation: rename route PROFILE→SETTINGS semantics, update TolochaNavGraph, remove "Gestionar servidores" button from Perfil in `core/ui/navigation/TolochaNavGraph.kt` + `feature/profile/ProfileScreen.kt` (contradicts, FR-011, C1/C4)
- [X] T059 Promote Servidores to top-level navigation destination (own entry in bottom nav or navigation drawer, NOT inside Configuración) in `core/ui/navigation/TolochaNavGraph.kt` + `core/ui/navigation/Routes.kt` (contradicts, FR-004, C1)
- [X] T060 Navigate to configured start screen on app launch with restored session (respect startScreen preference) in `MainActivity.kt` + `TolochaNavGraph.kt` (missing, FR-011b)
- [X] T061 Update ServerListScreen: show email per server, mark active AND default separately in `feature/servers/ServerListScreen.kt` (partial, US2/AC2)
- [X] T062 Update MigrationHelper: migrate existing baseUrl to server with isActive=true AND isDefault=true in `data/local/servers/MigrationHelper.kt` (partial, F4)
- [X] T063 Update ServerRepository.add() to use stored email (no appName-only) and handle appName from /config as display info in `data/repo/servers/ServerRepository.kt` (partial, FR-005)
- [X] T064 [P] Unit tests: active/default separation (SwitchServerUseCase), silent re-login (RestoreSessionUseCase), startScreen preference in `test/` (missing, FR-013)
- [X] T065 Run full test suite + compile; update quickstart.md scenarios for new model (missing, FR-013)

---

## Phase 6: Convergence

**Purpose**: Gaps detectados en converge (post-implementación Phase 5): snapshot de refresh tras logout y credenciales tras registro.

- [X] T066 On logout, clear the active server's per-server refresh snapshot in TokenStore (keep email+password for FR-014 reconnect) in domain/auth/LogoutUseCase.kt + data/repo/AuthRepo.kt per FR-007, FR-014 (partial, MEDIUM)
- [X] T067 [P] Store encrypted email+password for the active server after registration (call StoreServerCredentialsUseCase) in eature/auth/RegisterViewModel.kt per FR-005, US2 (partial, LOW)
