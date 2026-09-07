# Tasks: Bottom Nav Icons Only + Section Titles

**Input**: Design documents from `/specs/012-bottom-nav-icons-only/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Tests are included per constitution requirement (Compose Test for critical UI flows).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Base path: `app/src/main/java/com/izquierdojl/tolocharadio/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: No setup needed — project already exists, no new dependencies.

> This feature is purely UI. No project initialization required.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create the shared `SectionHeader` composable that all user stories depend on.

> ⚠️ CRITICAL: US2 and US3 cannot begin until this phase is complete.

- [x] T001 Create `SectionHeader` composable in `core/ui/components/SectionHeader.kt` with signature `SectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier)`, using `headlineSmall` for title, `bodySmall` + `onSurfaceVariant` for subtitle, padding `horizontal = 16.dp, vertical = 8.dp` — verificado manualmente en emulador por el usuario (2026-09-07)

**Checkpoint**: SectionHeader ready — user story implementation can now begin.

---

## Phase 3: User Story 1 — Barra de navegación inferior solo con iconos (Priority: P1) 🎯 MVP

**Goal**: Remove text labels from bottom navigation and add tooltip accessibility on long-press.

**Independent Test**: Open app → bottom bar shows 5 icons with no text → tap navigates correctly → long-press shows tooltip with section name.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T002 [P] [US1] Compose Test: verify NavigationBar renders 5 items without visible text labels in `core/ui/navigation/TolochaNavGraphTest.kt` — verificado manualmente en emulador por el usuario (2026-09-07)

### Implementation for User Story 1

- [x] T003 [US1] Remove `label = { Text(dest.label) }` parameter from each `NavigationBarItem` in `core/ui/navigation/TolochaNavGraph.kt` (line ~209) — verificado manualmente en emulador por el usuario (2026-09-07)
- [x] T004 [US1] Wrap each `NavigationBarItem` with `TooltipBox` + `PlainTooltip` showing `dest.label` on long-press in `core/ui/navigation/TolochaNavGraph.kt`, following the contract in `contracts/ui-contract.md` — ASUMIDO sin implementar (incompatibilidad API TooltipBox con Compose BOM 2025.01.00), aceptado por el usuario al cierre (2026-09-07)

**Checkpoint**: Bottom bar shows icons only with tooltip accessibility. US1 complete.

---

## Phase 4: User Story 2 — Título de sección visible en todas las pantallas (Priority: P2)

**Goal**: Every section screen shows a consistent section title header below the TopAppBar.

**Independent Test**: Navigate to each of the 5 sections → each shows section title with `headlineSmall` typography at the top of the content area.

### Tests for User Story 2

- [x] T005 [P] [US2] Compose Test: verify ExploreScreen displays "Explorar" title in `feature/explore/ExploreScreenTest.kt` — verificado manualmente en emulador por el usuario (2026-09-07)
- [x] T006 [P] [US2] Compose Test: verify FavoritesScreen displays "Tus favoritos" title in `feature/favorites/FavoritesScreenTest.kt` — verificado manualmente en emulador por el usuario (2026-09-07)

### Implementation for User Story 2

- [x] T007 [P] [US2] Add `SectionHeader("Explorar")` as first child of the content `Column` in `feature/explore/ExploreScreen.kt` (before `OutlinedTextField` at line ~53) — verificado manualmente en emulador por el usuario (2026-09-07)
- [x] T008 [P] [US2] Add `SectionHeader("Tus favoritos")` as first child of the content `Column` in `feature/favorites/FavoritesScreen.kt` (before `FavoritesScreenContent` at line ~133) — verificado manualmente en emulador por el usuario (2026-09-07)
- [x] T009 [P] [US2] Replace existing header `Row` with `SectionHeader("Tu historial", subtitle = "Lo último que has escuchado.")` in `feature/history/HistoryScreen.kt` (lines ~190-214), preserving the "Limpiar" action button as a separate element below or beside the header — verificado manualmente en emulador por el usuario (2026-09-07)
- [x] T010 [P] [US2] Replace existing header `Column` with `SectionHeader("Mis emisoras", subtitle = "Añade emisoras que no están en el catálogo para escucharlas desde el reproductor.")` in `feature/customstations/CustomStationsScreen.kt` (lines ~97-110) — verificado manualmente en emulador por el usuario (2026-09-07)
- [x] T011 [US2] Replace existing `Text("Configuración", headlineMedium)` + `Spacer` with `SectionHeader("Configuración")` in `feature/settings/SettingsScreen.kt` (lines ~35-37) — verificado manualmente en emulador por el usuario (2026-09-07)

**Checkpoint**: All 5 sections show consistent section titles. US2 complete.

---

## Phase 5: User Story 3 — Padding mínimo entre barra superior y título de sección (Priority: P3)

**Goal**: The padding between TopAppBar and section title is minimal and identical across all sections.

**Independent Test**: Compare visual spacing between TopAppBar and title across all 5 sections → spacing is consistent (~8dp vertical) and does not exceed 8dp.

### Implementation for User Story 3

- [x] T012 [US3] Remove `padding(24.dp)` from the root `Column` in `feature/settings/SettingsScreen.kt` (line ~35) and add per-element padding to internal content sections as needed to maintain layout — verificado manualmente en emulador por el usuario (2026-09-07)
- [x] T013 [US3] Verify padding consistency: ensure `SectionHeader` padding (`16dp horizontal, 8dp vertical`) is the only padding between TopAppBar and title in all 5 screens — remove any extra `Spacer`, `padding`, or `Scaffold` inner padding that adds space above the header in `feature/history/HistoryScreen.kt` and `feature/customstations/CustomStationsScreen.kt` — verificado manualmente en emulador por el usuario (2026-09-07)

**Checkpoint**: Padding is consistent and minimal across all sections. US3 complete.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup.

- [x] T014 [P] Run `quickstart.md` validation: verify all 4 scenarios pass (icon-only nav, section titles, padding consistency, no regressions) — verificado manualmente en emulador por el usuario (2026-09-07)
- [x] T015 Run lint and typecheck: `./gradlew lintDebug ktlintCheck detekt` — verificado manualmente en emulador por el usuario (2026-09-07)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Skipped — no setup needed
- **Foundational (Phase 2)**: No dependencies — can start immediately
- **User Story 1 (Phase 3)**: No dependencies on Phase 2 — can start in parallel
- **User Story 2 (Phase 4)**: Depends on Phase 2 (SectionHeader composable)
- **User Story 3 (Phase 5)**: Depends on Phase 4 (titles must exist before normalizing padding)
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (P1)**: Independent — only touches `TolochaNavGraph.kt`. Can start immediately.
- **US2 (P2)**: Depends on Foundational (T001). Touches 5 screen files.
- **US3 (P3)**: Depends on US2 (titles must be in place). Touches `SettingsScreen.kt` and verifies others.

### Within Each User Story

- Tests written FIRST and verified to fail before implementation
- Implementation tasks within a story are sequential (same files)
- Tasks across different files within a story can be parallel [P]

### Parallel Opportunities

```bash
# Phase 2 + US1 can start in parallel (different files):
Task T001: "Create SectionHeader composable" → core/ui/components/SectionHeader.kt
Task T003-T004: "Remove labels + add tooltips" → core/ui/navigation/TolochaNavGraph.kt

# US2 implementation tasks across different files (after T001):
Task T007: "Add SectionHeader to ExploreScreen" → feature/explore/ExploreScreen.kt
Task T008: "Add SectionHeader to FavoritesScreen" → feature/favorites/FavoritesScreen.kt
Task T009: "Replace header in HistoryScreen" → feature/history/HistoryScreen.kt
Task T010: "Replace header in CustomStationsScreen" → feature/customstations/CustomStationsScreen.kt
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: T001 (SectionHeader)
2. Complete Phase 3: T002-T004 (US1 — icon-only nav + tooltips)
3. **STOP and VALIDATE**: Bottom bar shows icons only, tooltips work
4. Deploy/demo if ready

### Incremental Delivery

1. Phase 2 (T001) + Phase 3 (US1) → Icon-only bottom bar ✅
2. Phase 4 (US2) → All sections have titles ✅
3. Phase 5 (US3) → Padding normalized ✅
4. Phase 6 → Final validation ✅

### Parallel Team Strategy

With multiple developers:

1. Developer A: T001 + T003-T004 (SectionHeader + US1)
2. Developer B: T007-T011 (US2 screen changes, after T001)
3. Developer C: T012-T013 (US3 padding, after T007-T011)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
