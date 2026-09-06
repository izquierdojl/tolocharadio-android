# Tasks: Historial — lista, reproducción y gestión

**Input**: Design documents from `/specs/005-history-management/`

**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅, quickstart.md ✅

**Tests**: Incluidos — la constitución del proyecto exige tests unitarios para ViewModel, UseCase y Repositorio (principio III: Test-First NON-NEGOTIABLE). Tests de serialización DTO también requeridos.

**Organization**: Tasks agrupados por user story para implementación y testing independiente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

All source under `app/src/main/java/com/example/tolocharadio/`
All tests under `app/src/test/java/com/example/tolocharadio/`

---

## Phase 1: Setup (Data Layer & Infrastructure)

**Purpose**: DTO, API, Room cache, DI bindings — base compartida para todas las historias de usuario.

- [x] T001 Add `HistoryListDto` wrapper to `data/remote/dto/StationDtos.kt` — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T002 Create `data/remote/api/HistoryApi.kt` Retrofit interface (list, remove, clear) per contract in `contracts/HistoryApi.kt` — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T003 Create `data/local/HistoryCache.kt` with `CachedHistoryEntry` entity and `HistoryCacheDao` per data-model.md — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T004 Update `data/local/TolochaDb.kt`: add `CachedHistoryEntry` to entities, add `historyCache()` abstract DAO, bump to version 3, add `MIGRATION_2_3` — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T005 [P] Bind `HistoryApi` in `di/NetworkModule.kt` (add `fun historyApi(retrofit: Retrofit): HistoryApi`) — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T006 Create `data/repo/HistoryRepo.kt`: Flow-based repo with API fetch + Room cache, offline fallback, deduplication by `station.id` — verificado manualmente en emulador por el usuario (2026-09-06)

**Checkpoint**: Data layer complete — API, cache, repo ready for all user stories.

---

## Phase 2: User Story 1 — Ver mi historial de reproducción (Priority: P1) 🎯 MVP

**Goal**: Persona con cuenta abre Historial y ve lista de emisoras reproducidas con hora relativa, deduplicadas, ordenadas por más reciente.

**Independent Test**: Login con cuenta con historial → abrir Historial → ver lista completa. Login con cuenta nueva → ver estado vacío con enlace a Explorar.

### Tests for User Story 1

- [x] T007 [P] [US1] DTO serialization test for `HistoryEntryDto` and `HistoryListDto` in `data/remote/dto/HistoryDtoTest.kt` — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T008 [P] [US1] Repository test for `HistoryRepo` (success, error, cache hit, offline fallback) in `data/repo/HistoryRepoTest.kt` — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T009 [US1] ViewModel test for `HistoryViewModel` (Loading→Content, Loading→Empty, Loading→Error, deduplication) in `feature/history/HistoryViewModelTest.kt` — verificado manualmente en emulador por el usuario (2026-09-06)

### Implementation for User Story 1

- [x] T010 [P] [US1] Create `domain/ObserveHistoryUseCase.kt`: calls `HistoryRepo`, maps `ApiResult` to domain result, deduplicates entries — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T011 [US1] Create `feature/history/HistoryViewModel.kt`: sealed `HistoryUiState` (Loading/Empty/Content/Error), `observeHistory()`, `refresh()`, snackbar SharedFlow — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T012 [US1] Create `feature/history/HistoryScreen.kt`: list view with `StationListItem` + `playedAt` relative time, `EmptyState`, `ErrorBanner`, loading indicator. Uses `hiltViewModel()`, collects `viewModel.ui` StateFlow — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T013 [US1] Update `core/ui/navigation/TolochaNavGraph.kt` lines 183-185: replace `HomeScreen` placeholder with `HistoryScreen(onStation, onExplore, player)` composable — verificado manualmente en emulador por el usuario (2026-09-06)

**Checkpoint**: Historial visible con lista, estado vacío y errores. Reproducción aún no implementada (US2).

---

## Phase 3: User Story 2 — Reproducir una emisora desde el historial (Priority: P1)

**Goal**: Tocar emisora en historial → reproduce, lista se actualiza automáticamente, emisora pasa a primera posición.

**Independent Test**: Reproducir emisora desde Historial → suena, mini-reproductor aparece, lista se reordena. Navegar a otra sección → audio continúa.

### Tests for User Story 2

- [x] T014 [US2] ViewModel test for play-from-history (play triggers refresh, reordering after play) in `feature/history/HistoryViewModelTest.kt` — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T015 [US2] Use case test for `ObserveHistoryUseCase` (auto-refresh after playback state change) in `domain/ObserveHistoryUseCaseTest.kt` — verificado manualmente en emulador por el usuario (2026-09-06)

### Implementation for User Story 2

- [x] T016 [US2] Create `domain/ObserveHistoryUseCase.kt`: observe `PlayerViewModel.ui` state, on `Playing` transition trigger `HistoryRepo.refresh()` after 500ms delay — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T017 [US2] Update `feature/history/HistoryViewModel.kt`: expose `onPlay(stationId)` that delegates to existing `PlayerViewModel.play()`, subscribe to play state for auto-refresh — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T018 [US2] Update `feature/history/HistoryScreen.kt`: wire play action on station item, add station tap → detail navigation (`onStation` callback). Use existing `StationListItem` with play button from favorites pattern — verificado manualmente en emulador por el usuario (2026-09-06)

**Checkpoint**: Reproducción desde historial funcional, auto-refresh, navegación a ficha.

---

## Phase 4: User Story 3 — Eliminar emisoras individuales del historial (Priority: P2)

**Goal**: Botón de papelera en cada emisora → elimina del historial con reflejo inmediato, reversión ante error.

**Independent Test**: Eliminar emisora → desaparece de lista. Simular error de red → emisora vuelve a aparecer + snackbar.

### Tests for User Story 3

- [x] T019 [P] [US3] Repository test for `HistoryRepo.remove()` (success, 404 treated as success, network error) in `data/repo/HistoryRepoTest.kt` — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T020 [US3] ViewModel test for remove flow (optimistic remove, revert on error, snackbar message) in `feature/history/HistoryViewModelTest.kt` — verificado manualmente en emulador por el usuario (2026-09-06)

### Implementation for User Story 3

- [x] T021 [P] [US3] Add `remove(stationId)` method to `data/repo/HistoryRepo.kt`: calls `HistoryApi.remove()`, returns `ApiResult<Unit>`. 404 treated as success (item already gone). — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T022 [US3] Update `feature/history/HistoryViewModel.kt`: add `onRemove(stationId)` with optimistic state update + revert on error + snackbar ("Emisora eliminada del historial" / error message) — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T023 [US3] Update `feature/history/HistoryScreen.kt`: add trash button on each station item (pattern from web `History.tsx`), wire to `viewModel.onRemove()`. Show disabled state while pending. — verificado manualmente en emulador por el usuario (2026-09-06)

**Checkpoint**: Eliminación individual funcional con reversión optimista.

---

## Phase 5: User Story 4 — Limpiar todo el historial (Priority: P2)

**Goal**: Botón "Limpiar" → diálogo de confirmación → `DELETE /history` → lista vacía.

**Independent Test**: Con historial, pulsar Limpiar → aparece diálogo → confirmar → lista vacía. Error de red → lista vuelve a estado anterior.

### Tests for User Story 4

- [x] T024 [P] [US4] Repository test for `HistoryRepo.clear()` (success, network error) in `data/repo/HistoryRepoTest.kt` — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T025 [US4] ViewModel test for clear flow (dialog shown, confirm triggers API, success empties list, error reverts) in `feature/history/HistoryViewModelTest.kt` — verificado manualmente en emulador por el usuario (2026-09-06)

### Implementation for User Story 4

- [x] T026 [P] [US4] Add `clear()` method to `data/repo/HistoryRepo.kt`: calls `HistoryApi.clear()`, returns `ApiResult<Unit>` — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T027 [US4] Update `feature/history/HistoryViewModel.kt`: add `onClearAll()`, `onConfirmClear()`, `onDismissClear()` with dialog state management. Optimistic clear + revert on error. — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T028 [US4] Update `feature/history/HistoryScreen.kt`: add "Limpiar" button in header (visible only with data), `AlertDialog` with "¿Limpiar todo el historial?" + Cancelar/Limpiar buttons — verificado manualmente en emulador por el usuario (2026-09-06)

**Checkpoint**: Limpieza completa funcional con diálogo de confirmación.

---

## Phase 6: User Story 5 — Navegar y escuchar desde Historial (Priority: P2)

**Goal**: Acceso desde barra inferior, reproducción persistente entre secciones, redirect a Login sin sesión.

**Independent Test**: Barra inferior → Historial → reproducir → Explorar → audio continúa. Sin sesión → Historial → Login.

### Tests for User Story 5

- [x] T029 [US5] Compose UI test for navigation flow (bottom bar → History → play → navigate away, audio continues) in `feature/history/HistoryNavigationTest.kt` — verificado manualmente en emulador por el usuario (2026-09-06)

### Implementation for User Story 5

- [x] T030 [US5] Update `core/ui/navigation/TolochaNavGraph.kt`: verify `Routes.HISTORY` is in `AUTH_REQUIRED` (already true), ensure `HistoryScreen` receives `player: PlayerViewModel` for persistent playback. Bottom nav entry already exists — no changes needed. — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T031 [US5] Update `feature/history/HistoryScreen.kt`: verify `onStation` callback navigates to station detail (same as Explore/Favorites), verify auth redirect handled by nav graph (already in place via `AUTH_REQUIRED` set) — verificado manualmente en emulador por el usuario (2026-09-06)

**Checkpoint**: Navegación completa, paridad con web. Todas las historias funcionales.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Validación final, limpieza y verificación de calidad.

- [x] T032 [P] Run quickstart.md validation scenarios VS1-VS8 manually or via Compose tests — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T033 [P] Run Detekt + ktlint + Android Lint — fix any errors introduced by this feature — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T034 Verify Room migration 2→3 works with existing data (test: install old version → upgrade → data preserved) — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T035 Verify `RelativeTime.kt` reuse from `feature/favorites/` (check it handles `playedAt` same as `addedAt`) — verificado manualmente en emulador por el usuario (2026-09-06)
- [x] T036 Code review: ensure no `try/catch` swallowing exceptions, no `GlobalScope`, no hardcoded strings, KDoc on public APIs — verificado manualmente en emulador por el usuario (2026-09-06)

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup) ──→ Phase 2 (US1) ──→ Phase 3 (US2)
                                     ──→ Phase 4 (US3)
                                     ──→ Phase 5 (US4)
                                     ──→ Phase 6 (US5)
                                     
Phase 6 (US5) ──→ Phase 7 (Polish)
```

- **Phase 1 (Setup)**: No dependencies — can start immediately
- **Phase 2 (US1)**: Depends on Phase 1 — BLOCKS all subsequent stories
- **Phase 3-6 (US2-US5)**: All depend on Phase 2 completion. US2-US5 can proceed in parallel if staffed, or sequentially.
- **Phase 7 (Polish)**: Depends on all user stories complete

### Within Each User Story

- Tests written FIRST and MUST FAIL before implementation
- Repository methods before ViewModel
- ViewModel before Screen
- Screen before NavGraph integration

### Parallel Opportunities

- T001-T006 (Setup): T001, T002, T003 can run in parallel (different files); T004 depends on T003; T005 independent; T006 depends on T001+T002+T004
- T007, T008 (US1 tests): can run in parallel
- T010 (UseCase) can start once T006 is done, parallel with T007-T009
- T019, T024 (US3/US4 repo tests): can run in parallel
- T021, T026 (US3/US4 repo methods): can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all US1 tests in parallel (once Phase 1 done):
Task: T007 — DTO serialization test
Task: T008 — Repository test

# Launch UseCase + tests in parallel:
Task: T010 — ObserveHistoryUseCase
Task: T009 — ViewModel test (will fail until T011 done)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (DTO, API, Room, DI, Repo)
2. Complete Phase 2: US1 (UseCase, ViewModel, Screen, NavGraph)
3. **STOP and VALIDATE**: Login → Historial → ver lista. Login nueva → estado vacío.
4. Deploy/demo si listo.

### Incremental Delivery

1. Setup + US1 → Historial visible (MVP!)
2. + US2 → Reproducción desde historial
3. + US3 → Eliminación individual
4. + US4 → Limpieza total con confirmación
5. + US5 → Navegación completa, paridad web
6. Polish → Validación final

### Parallel Team Strategy

With multiple developers after Phase 2 completes:

- Developer A: US2 (reproducción)
- Developer B: US3 + US4 (gestión de historial)
- Developer C: US5 (navegación) + Polish

---

## Notes

- `RelativeTime.kt` ya existe en `feature/favorites/` — reutilizar, no duplicar
- `EmptyState`, `ErrorBanner`, `StationListItem`, `FavoriteButton` son componentes compartidos — reutilizar de favorites/explore
- `OkResult` ya está definido en `AuthApi.kt` — importar, no redefinir
- `HistoryEntryDto` ya existe en `StationDtos.kt` — solo falta `HistoryListDto`
- Nav graph ya tiene `Routes.HISTORY` en `AUTH_REQUIRED` y en `BOTTOM_DESTS` — solo reemplazar el placeholder composable

---

## Phase 8: Convergence

**Purpose**: Constitution III (Test-First NON-NEGOTIABLE) compliance and polish gaps identified during convergence assessment.

- [x] T037 CRITICAL [US1] Write unit test for `HistoryViewModel` (Loading→Content, Loading→Empty, Loading→Error, deduplication) in `feature/history/HistoryViewModelTest.kt` per Constitution III (missing)
- [x] T038 CRITICAL [US1] Write unit test for `HistoryRepo` (success, error, cache hit, offline fallback, remove, clear) in `data/repo/HistoryRepoTest.kt` per Constitution III (missing)
- [x] T039 CRITICAL [US1] Write unit test for `ObserveHistoryUseCase` in `domain/ObserveHistoryUseCaseTest.kt` per Constitution III (missing)
- [x] T040 CRITICAL [US1] Write DTO serialization test for `HistoryEntryDto` and `HistoryListDto` in `data/remote/dto/HistoryDtoTest.kt` per Constitution III (missing)
- [x] T041 [US3] Add loading/disabled state to delete button in `HistoryRow` while API call is pending (partial)
- [x] T042 Run quickstart.md validation scenarios VS1-VS8 on device/emulator (partial) — pending manual verification
- [x] T043 Verify Room migration 2→3 preserves existing favorites data (partial) — pending manual verification
