# Tasks: Mejoras estéticas UI

**Input**: Design documents from `/specs/0039-jlizquierdo-20260920-mejoras-esteticas-ui/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: La spec no pide tests explícitamente, pero la constitución (III) los exige en flujos críticos de UI (favoritas, player) y el plan (R7) define la estrategia; por eso cada historia incluye su tarea de test UI primero (enfoque Red-Green).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Partir de un entorno verificado y en verde antes de tocar UI

- [X] T001 Verificar rama `0039-jlizquierdo-20260920-mejoras-esteticas-ui` y documentos de diseño en `specs/0039-jlizquierdo-20260920-mejoras-esteticas-ui/spec.md`
- [X] T002 [P] Ejecutar baseline `assembleDebug` para confirmar compilación en verde (config en `app/build.gradle.kts`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Auditoría de usos actuales que condiciona las tres historias (qué se puede borrar sin romper nada)

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T003 Auditar usos de `FavoriteRowMenu`, `onMoveUp`/`onMoveDown`, `FavoritesViewModel.moveBy`, `PanelCopyButton` y aserciones del menú en `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` y `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreenTest.kt`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Reordenar favoritas solo por arrastre (Priority: P1) ⭐ MVP

**Goal**: Eliminar por completo el menú "Mover arriba/abajo" de favoritas; el arrastre queda como única vía de reordenación (contrato C1)

**Independent Test**: Con 2+ favoritas, ninguna fila muestra más opciones; arrastrar y soltar cambia el orden y persiste al salir y volver (spec US1, SC-001/SC-002)

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T004 [P] [US1] Actualizar test UI de favoritas sin menú + arrastre que persiste, incluyendo caso sin conexión/lista mínima, en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreenTest.kt`

### Implementation for User Story 1

- [X] T005 [US1] Eliminar `FavoriteRowMenu` y su llamada en `FavoriteRow` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt`
- [X] T006 [US1] Eliminar callbacks `onMoveUp`/`onMoveDown`, campos `canMoveUp`/`canMoveDown` sin uso e imports huérfanos (`MoreVert`, `DropdownMenu*`) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt`, y eliminar `moveBy` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModel.kt` solo si T003 confirma cero consumidores de UI (si sigue en uso, dejarlo y anotar el motivo en el commit)
- [X] T007 [US1] Verificar en verde tests de favoritas (`FavoritesScreenTest` + `FavoritesViewModelTest` en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreenTest.kt` y `app/src/test/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModelTest.kt`)

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Compartir enlace desde la ficha (Priority: P1)

**Goal**: Mini-player con solo play/pausa + silenciar en reproducción normal; ficha con sección "Enlace" (compartir del sistema + copiar) reutilizando `resolveCopyLink` (contratos C2/C3)

**Independent Test**: En reproducción normal el panel muestra 2 acciones y ningún copiar; en la ficha la sección "Enlace" comparte el enlace real, copiar avisa "Enlace copiado" y sin enlace avisa "Enlace no disponible" (spec US2, SC-003/SC-004)

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T008 [P] [US2] Actualizar/añadir tests UI de panel mínimo y ficha con sección "Enlace" en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/player/` (crear el fichero de test si no existe)

### Implementation for User Story 2

- [X] T009 [P] [US2] Retirar `PanelCopyButton` del `MiniPlayer` conservando reintentar en error y cancelar en carga en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt`
- [X] T010 [P] [US2] Añadir sección "Enlace" con texto del enlace recortado (2 líneas máx.) + Compartir (`ACTION_SEND`) + Copiar vía `resolveCopyLink` de `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PanelHelpers.kt` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/StationInfoSheet.kt` (Compartir y Copiar usan el enlace completo)
- [X] T011 [US2] Verificar en verde tests de player (`PlayerViewModelTest` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` + tests UI de T008)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Encabezados explicativos (Priority: P2)

**Goal**: Subtítulos confirmados bajo "Explorar" y "Tus favoritos" con el mismo estilo del historial (contrato C4)

**Independent Test**: Explorar muestra "Descubre emisoras de todo el mundo." y Favoritas "Tus emisoras guardadas, en tu orden.", con la misma apariencia que el historial (spec US3, SC-005)

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T012 [P] [US3] Añadir aserciones de subtítulo en tests UI de Explorar y Favoritas en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreenTest.kt` y `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/explore/` (crear el fichero de test si no existe)

### Implementation for User Story 3

- [X] T013 [US3] Añadir `subtitle` a `SectionHeader` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/ExploreScreen.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt` (componente sin cambios en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/SectionHeader.kt`)
- [X] T014 [US3] Verificar en verde tests UI de encabezados en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreenTest.kt` y `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/explore/`

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Gates obligatorios y validación end-to-end de la feature

- [X] T015 Ejecutar gates `assembleDebug`, `testDebugUnitTest` y `detekt ktlintCheck lintDebug` vía `app/build.gradle.kts` y dejarlos en verde
- [X] T016 Ejecutar validación `quickstart.md` en `specs/0039-jlizquierdo-20260920-mejoras-esteticas-ui/quickstart.md` incluyendo `connectedDebugAndroidTest` (requiere dispositivo/emulador)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - May integrate with US1/US2 but should be independently testable

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- T002 (baseline build) can run in parallel with T001 (verificación de rama)
- T009 (panel en `PlayerUi.kt`) and T010 (ficha en `StationInfoSheet.kt`) touch different files and can run in parallel
- Once Foundational completes, US1/US2/US3 can start in parallel (different files except shared `FavoritesScreen.kt` between US1 and US3 — coordinate if parallelized)
- Caution: T005/T006 (both in `FavoritesScreen.kt`) and T013 (also `FavoritesScreen.kt`) touch the same file — run sequentially if same developer

---

## Parallel Example: User Story 2

```bash
# Launch tests for User Story 2 first (must FAIL before implementation):
Task: "Actualizar/añadir tests UI de panel mínimo y ficha con sección Enlace in app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/player/"

# Launch implementation in parallel (different files, no dependencies):
Task: "Retirar PanelCopyButton del MiniPlayer in app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt"
Task: "Añadir sección Enlace con Compartir + Copiar in app/src/main/java/com/izquierdojl/tolocharadio/feature/player/StationInfoSheet.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently (favoritas sin menú + arrastre persiste)
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Tests incluidos por mandato constitucional (III) para flujos críticos, aunque la spec no los pida explícitamente
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence

---

## Phase 7: Convergence

**Purpose**: Cerrar el único hueco detectado por `/speckit.converge` (2026-09-20): T008 cubrió la ficha (`StationInfoSheetTest`) pero no el mini-player mínimo exigido por el plan R7 / contrato C2 / constitución III

- [X] T017 Añadir test UI Compose del MiniPlayer mínimo (2 acciones en reproducción normal sin copiar enlace; reintentar en error y cancelar en carga) en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/player/` (crear el fichero de test si no existe) per plan:R7 / C2 (partial)
