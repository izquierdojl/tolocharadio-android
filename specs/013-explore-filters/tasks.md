# Tasks: Filtros avanzados de Explorar

**Input**: Design documents from `/specs/013-explore-filters/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are included following existing project patterns (JUnit5, Mockk, Turbine).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: No setup needed — data layer already supports filters. Skip to Phase 2.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T001 [P] Create CatalogList sealed interface in `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/ExploreViewModel.kt`
- [x] T002 [P] Add catalog list StateFlows (_countries, _languages, _tags) to ExploreViewModel in `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/ExploreViewModel.kt`
- [x] T003 Implement parallel catalog list loading in ExploreViewModel.init() in `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/ExploreViewModel.kt`

**Checkpoint**: Foundation ready — ExploreViewModel can load catalog lists from API

---

## Phase 3: User Story 1 — Filtrar emisoras por país, idioma y género (Priority: P1) 🎯 MVP

**Goal**: User can filter stations by country, language, and genre using dropdown controls combined with existing name search

**Independent Test**: Open Explorar, select a country/language/genre, tap "Buscar", verify results match filters. Tap "Limpiar filtros" to reset.

### Tests for User Story 1 ⚠️

- [x] T004 [P] [US1] Unit test for catalog list loading states in `app/src/test/java/com/izquierdojl/tolocharadio/feature/explore/ExploreViewModelsTest.kt`
- [x] T005 [P] [US1] Unit test for filter application with country/language/tag in `app/src/test/java/com/izquierdojl/tolocharadio/feature/explore/ExploreViewModelsTest.kt`
- [x] T006 [P] [US1] Unit test for clear filters functionality in `app/src/test/java/com/izquierdojl/tolocharadio/feature/explore/ExploreViewModelsTest.kt`

### Implementation for User Story 1

- [x] T007 [P] [US1] Create FilterComboBox composable component in `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/FilterComboBox.kt`
- [x] T008 [US1] Add country, language, and tag FilterComboBox fields to ExploreScreen in `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/ExploreScreen.kt`
- [x] T009 [US1] Add "Limpiar filtros" button to ExploreScreen in `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/ExploreScreen.kt`
- [x] T010 [US1] Wire filter field changes to ExploreViewModel.setFilters() in `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/ExploreScreen.kt`

**Checkpoint**: User can filter by country, language, and genre. Filters combine with name search. "Limpiar filtros" resets all.

---

## Phase 4: User Story 2 — Autocompletado con listas de sugerencias (Priority: P1)

**Goal**: Filter dropdowns show autocomplete suggestions from server lists. If list fails to load, field allows manual input with warning.

**Independent Test**: Type partial value in filter field, verify suggestions appear. Disconnect network, verify degraded mode with manual input.

### Tests for User Story 2 ⚠️

- [x] T011 [P] [US2] Unit test for degraded mode (catalog list error) in `app/src/test/java/com/izquierdojl/tolocharadio/feature/explore/ExploreViewModelsTest.kt`

### Implementation for User Story 2

- [x] T012 [US2] Implement autocomplete filtering logic in FilterComboBox in `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/FilterComboBox.kt`
- [x] T013 [US2] Add degraded mode (manual input with warning) to FilterComboBox in `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/FilterComboBox.kt`
- [x] T014 [US2] Add loading state (disabled fields with spinner) to FilterComboBox in `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/FilterComboBox.kt`

**Checkpoint**: Filter fields show autocomplete suggestions. Degraded mode works when API fails. Loading state shows spinner.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup

- [x] T015 Run quickstart.md validation scenarios from `specs/013-explore-filters/quickstart.md`
- [x] T016 Verify existing "Cargar más" pagination still works with filters applied
- [x] T017 Verify view mode toggle (list/grid) still works with filter results

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: Skipped — no setup needed
- **Phase 2 (Foundational)**: No dependencies — can start immediately
- **Phase 3 (US1)**: Depends on Phase 2 completion
- **Phase 4 (US2)**: Depends on Phase 2 completion; T012-T014 extend T007's FilterComboBox
- **Phase 5 (Polish)**: Depends on Phase 3 and Phase 4 completion

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Phase 2. No dependency on other stories.
- **User Story 2 (P1)**: Depends on Phase 2. Extends US1's FilterComboBox (T007) with autocomplete and degraded mode.

### Within Each User Story

- Tests (T004-T006, T011) can run in parallel with each other
- FilterComboBox (T007) must complete before ExploreScreen modifications (T008-T010)
- T012-T014 extend T007's component

### Parallel Opportunities

- T001, T002 can run in parallel (different concerns in same file)
- T004, T005, T006 can run in parallel (different test methods)
- T007 can run in parallel with T004-T006
- T011 can run in parallel with T007

---

## Parallel Example: Phase 2 + Phase 3 Tests

```bash
# Launch foundational tasks in parallel:
Task: T001 - Create CatalogList sealed interface
Task: T002 - Add catalog list StateFlows

# Once T001+T002 complete, launch tests in parallel with FilterComboBox:
Task: T004 - Unit test catalog list loading states
Task: T005 - Unit test filter application
Task: T006 - Unit test clear filters
Task: T007 - Create FilterComboBox component
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational (T001-T003)
2. Complete Phase 3: User Story 1 (T004-T010)
3. **STOP and VALIDATE**: Test filter by country, language, genre independently
4. Deploy/demo if ready

### Incremental Delivery

1. Phase 2 → ViewModel can load catalog lists
2. Phase 3 → User can filter by country/language/genre (MVP!)
3. Phase 4 → Autocomplete + degraded mode complete
4. Phase 5 → Validation and cleanup

---

## Notes

- Data layer is ready: `StationsApi` and `StationsRepo` already support all filter endpoints
- `ExploreFilters` data class already has `country`, `language`, `tag` fields
- `StationQuery` already passes these filters to the API
- Main work is UI: FilterComboBox component + ExploreScreen layout
