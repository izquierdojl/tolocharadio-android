---
description: "Task list for feature 0034 — Reordenación intuitiva de favoritos"
---

# Tasks: Reordenación intuitiva de favoritos

**Input**: Design documents from `/specs/0034-jlizquierdo-20260913-favorites-reorder-animation/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/favorites-reorder-ui.md

**Tests**: SÍ. La constitución (Principio III, Test-First NON-NEGOTIABLE) exige tests para
`ViewModel` y Compose UI para flujos críticos. Regla Red-Green: el test se escribe antes y
debe fallar antes de la implementación.

**Organization**: Tareas agrupadas por historia de usuario. Cada historia es
independientemente implementable y testeable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivo distinto, sin dependencias pendientes)
- **[Story]**: US1/US2/US3
- Todas las tareas incluyen rutas de archivo exactas

## Path Conventions

Módulo único Android: `app/src/main/java/com/izquierdojl/tolocharadio/`,
`app/src/test/java/...`, `app/src/androidTest/java/...`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar un punto de partida verde y que no hacen falta dependencias nuevas.

- [x] T001 Ejecutar los gates obligatorios en la rama para confirmar base verde: `.\gradlew.bat testDebugUnitTest detekt ktlintCheck lintDebug` (raíz del repo)
- [x] T002 [P] Confirmar que no se añaden dependencias: el Compose BOM `2025.01.00` ya aporta `Modifier.animateItem()` y la API de hápticos; no editar `gradle/libs.versions.toml` (raíz del repo)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Regla compartida de cuándo se puede reordenar (la usan US1 y US3).

**⚠️ CRITICAL**: US1 y US3 dependen de esta fase.

- [x] T003 Implementar en `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt` el gate `reorderEnabled = mode == LIST && !state.offline && items.size > 1`, propagándolo a `FavoritesList`/`FavoriteRow` para mostrar u ocultar el asa y el menú de movimiento (FR-001, FR-015, FR-018)
- [x] T004 Añadir en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreenTest.kt` el test (Red) de que con `offline = true` o con una sola favorita no aparece el asa de arrastre (FR-018)

**Checkpoint**: Foundation lista — US1, US2 y US3 pueden empezar.

---

## Phase 3: User Story 1 - Reordenar favoritas viendo el movimiento en vivo (Priority: P1) 🎯 MVP

**Goal**: Al arrastrar una favorita, la fila activa se eleva y el resto se reacomoda con
animación antes de soltar.

**Independent Test**: Con ≥2 favoritas en vista lista, arrastrar el asa y comprobar (manual,
quickstart §2-3) que la fila activa se resalta y las demás se desplazan con animación antes
de soltar; además, un test Compose verifica que el gesto sobre el asa dispara `onMove` y
`onCommit`.

### Tests for User Story 1 (Red-Green)

- [x] T005 [P] [US1] Añadir en `app/src/androidTest/java/.../feature/favorites/FavoritesScreenTest.kt` el test (Red) que inyecta un gesto de arrastre sobre el asa (`down` → avanzar reloj → `moveBy` vertical → `up`) y verifica que se invocan `onMove` y `onCommit` (FR-004, FR-005, FR-009)

### Implementation for User Story 1

- [x] T006 [US1] Aplicar `Modifier.animateItem()` (con `placementSpec` spring) a los `items` del `LazyColumn` en `FavoritesList` — `app/src/main/java/.../feature/favorites/FavoritesScreen.kt` (FR-003)
- [x] T007 [US1] Añadir estado `draggingId: String?` (`remember`) en `FavoritesList` y aplicar a la fila activa `graphicsLayer` (escala), `Modifier.shadow` y `zIndex(1f)`; limpiarlo al terminar — `FavoritesScreen.kt` (FR-002)
- [x] T008 [US1] Emitir háptica con `LocalHapticFeedback` al agarrar (`LongPress`) y al cruzar una fila (`TextHandleMove`), tolerante a dispositivos sin soporte — `FavoritesScreen.kt` (FR-008)
- [x] T009 [US1] Al soltar (`onDragEnd`) y al cancelar (`onDragCancel`), limpiar `draggingId` y llamar a `onCommit` (conservando el orden mostrado; aclaración Q1); respetar `LocalMotionDurationScale` para omitir efectos propios si está desactivado — `FavoritesScreen.kt` (FR-005, FR-014)

**Checkpoint**: US1 funcional y validable de forma independiente.

---

## Phase 4: User Story 2 - Iniciar el arrastre de forma natural y cómoda (Priority: P2)

**Goal**: El arrastre empieza de inmediato sobre el asa (sin pulsación larga) y la lista se
auto-desplaza de forma proporcional al acercarse a los bordes.

**Independent Test**: Un test Compose inyecta un arrastre corto (sin espera) sobre el asa y
verifica que dispara `onMove`; en dispositivo, arrastrar hasta el borde desplaza la lista.

### Tests for User Story 2 (Red-Green)

- [x] T010 [US2] Añadir en `app/src/androidTest/java/.../feature/favorites/FavoritesScreenTest.kt` el test (Red) de que un arrastre inmediato (sin pulsación larga) sobre el asa dispara `onMove` (FR-006)

### Implementation for User Story 2

- [x] T011 [US2] Sustituir `detectDragGesturesAfterLongPress` por `detectDragGestures` en `Modifier.favoriteDragHandle` — `app/src/main/java/.../feature/favorites/FavoritesScreen.kt` (FR-006)
- [x] T012 [US2] Hacer proporcional la velocidad de auto-scroll a la cercanía al borde en `onDragMove` (factor sobre `DRAG_SCROLL_PX` según distancia a `DRAG_EDGE_PX`), manteniendo el umbral — `FavoritesScreen.kt` (FR-007)

**Checkpoint**: US1 y US2 funcionan juntas (arrastre inmediato + animado).

---

## Phase 5: User Story 3 - Reordenar sin gestos de arrastre (Priority: P3)

**Goal**: Un menú de desbordamiento por fila permite mover una posición arriba/abajo,
deshabilitado en los extremos.

**Independent Test**: Con ≥2 favoritas, abrir el menú de una fila intermedia y elegir
"Mover arriba"/"Mover abajo": la fila cambia una posición y se guarda; en los extremos la
opción correspondiente está deshabilitada.

### Tests for User Story 3 (Red-Green)

- [x] T013 [P] [US3] Añadir en `app/src/test/java/.../feature/favorites/FavoritesViewModelTest.kt` los tests (Red) de `moveBy(±1)`: intercambio con la vecina + `commitOrder`, no-op en el extremo, no-op con `offline = true`, y no-op si el id no existe (FR-012, FR-013, FR-018)
- [x] T014 [US3] Añadir en `app/src/androidTest/java/.../feature/favorites/FavoritesScreenTest.kt` el test (Red) de que el menú ofrece "Mover arriba"/"Mover abajo", ejecuta el movimiento, deshabilita el ítem en el extremo (FR-012, FR-013) y no aparece con `offline = true` o con una sola favorita (FR-018)

### Implementation for User Story 3

- [x] T015 [US3] Implementar `moveBy(stationId, delta)` en `app/src/main/java/.../feature/favorites/FavoritesViewModel.kt`: validar `offline`, existencia e índices; delegar en `moveItem` y llamar a `commitOrder()` (FR-012, FR-013, FR-018)
- [x] T016 [US3] Ampliar `FavoriteListActions` con `onMoveUp: (String) -> Unit` y `onMoveDown: (String) -> Unit`, y añadir en `FavoriteRow` un `IconButton` con `DropdownMenu` ("Mover arriba"/"Mover abajo", deshabilitados en extremos) — `FavoritesScreen.kt` (FR-012, FR-013)
- [x] T017 [US3] Cablear las acciones en el host `FavoritesScreen` (`onMoveUp = { viewModel.moveBy(it, -1) }`, `onMoveDown = { viewModel.moveBy(it, 1) }`) — `FavoritesScreen.kt` (FR-012)

**Checkpoint**: Las tres historias funcionan de forma independiente.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verificación final y regresiones.

- [x] T018 Ejecutar los gates obligatorios: `.\gradlew.bat testDebugUnitTest detekt ktlintCheck lintDebug assembleDebug` (raíz del repo)
- [x] T019 [P] Ejecutar los tests instrumentados de Favoritos: `.\gradlew.bat connectedDebugAndroidTest --tests "*FavoritesScreenTest"` (requiere dispositivo/emulador)
- [x] T020 Validar `quickstart.md` en dispositivo: animación en vivo, soltar/guardar, menú accesible, lista larga con auto-scroll, offline (sin reorden), reproducción sin interrupción y "reducir movimiento"; registrar el criterio de SC-002 (reacomodo percibido como inmediato por debajo de 100 ms por cruce) (FR-014, FR-016, SC-001…SC-006)
- [x] T021 [P] Revisar que no se introducen logs con PII ni cambios en el player, y que las descripciones/etiquetas de UI están en español — `FavoritesScreen.kt` (FR-016)
- [x] T022 [P] Añadir tests unitarios en `app/src/test/java/.../feature/favorites/FavoritesViewModelTest.kt` de `commitOrder`: error de red restaura el último orden confirmado y emite mensaje (FR-010); rechazo por conflicto entre dispositivos muestra el orden del servidor y avisa (FR-011)
- [x] T023 [P] Ampliar `app/src/androidTest/java/.../feature/favorites/FavoritesScreenTest.kt` con la regresión de que un toque en la fila fuera del asa no dispara `onMove` ni `onCommit` (FR-017)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Setup; BLOQUEA US1 y US3.
- **US1 (Phase 3)**: depende de Foundational; no depende de US2/US3.
- **US2 (Phase 4)**: depende de Foundational; reutiliza el asa de US1 (recomendado tras US1).
- **US3 (Phase 5)**: depende de Foundational; independiente de US1/US2 en lógica, pero
  comparte `FavoritesScreen.kt` (secuenciar la edición del archivo).
- **Polish (Phase 6)**: depende de las historias deseadas completas.

### Within Each User Story

- Tests (Red) antes de la implementación (Green).
- Constantes/estado local antes de efectos; luego cableado.
- Historia completa y validada antes de la siguiente prioridad.

### Parallel Opportunities

- T002 es independiente (verificación).
- T005 (test en `FavoritesScreenTest.kt`) puede prepararse en paralelo con T006–T009
  (`FavoritesScreen.kt`), pero debe fallar antes de implementar.
- T013 (tests en `FavoritesViewModelTest.kt`) puede prepararse en paralelo con T015–T017.
- T019, T021, T022 y T023 en paralelo al final.

**Nota de conflicto**: `FavoritesScreen.kt` lo tocan T003, T006–T009, T011–T012, T016–T017.
No marcarlas [P] entre sí; editarlas en orden. `FavoritesScreenTest.kt` lo tocan T004, T005,
T010, T014 y T023 (tampoco [P] entre sí).

---

## Parallel Example: User Story 1

```text
# Preparar el test (Red) en paralelo con la implementación de otra historia:
Task: "T005 [P] [US1] Test Compose de arrastre en FavoritesScreenTest.kt"
Task: "T013 [P] [US3] Tests unitarios de moveUp/moveDown en FavoritesViewModelTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1 (Setup) y Phase 2 (Foundational).
2. Completar Phase 3 (US1): animación + realce + cancelar/soltar.
3. **PARAR y VALIDAR** con el test Compose de T005 y manualmente (quickstart §2-3).
4. Desplegar/demostrar si procede.

### Incremental Delivery

1. Setup + Foundational → base lista.
2. US1 → animación en vivo (MVP).
3. US2 → arrastre inmediato + auto-scroll.
4. US3 → menú accesible + offline.
5. Cada historia añade valor sin romper las anteriores.

---

## Notes

- [P] = archivos distintos y sin dependencias pendientes.
- [Story] mapea la tarea a su historia para trazabilidad.
- No hay cambios de backend, DTO ni esquema Room; no se añaden dependencias.
- Verificar que cada test falla antes de implementar (Red-Green).
- No commitear sin petición explícita del usuario (AGENTS.md).

---

## Phase 7: Convergence

**Origen**: `/speckit.converge` 2026-09-20 — 18 FR, 6 SC, 3 historias, 4 clarificaciones, decisiones D1–D8 de `research.md` y 5 principios de constitución revisados contra el código. US1/US2 conformes (animación en vivo, lift, drag inmediato, auto-scroll proporcional, háptica, gate offline/última favorita/cuadrícula) y sin regresiones del menú eliminado por la 0039.

- [ ] T024 Completar el manejo de fallo de guardado de FR-010 en `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModel.kt` (`commitOrder`, líneas 246–253): diferenciar el mensaje según la causa (conflicto entre dispositivos → texto actual; error recuperable de red → aviso de fallo de guardado con acción de reintento que reintente la permutación pendiente) y añadir el caso de test equivalente en `app/src/test/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModelTest.kt` per FR-010 (partial) — MEDIUM
- [ ] T025 Anotar el supersede de la 0039 en `specs/0034-jlizquierdo-20260913-favorites-reorder-animation/spec.md`: marcar US3 (historia completa), FR-012, FR-013, SC-005, los edge cases que mencionan el menú (lista de una sola favorita, sin conexión) y la clarificación Q3 como SUPERSEDED por la spec 0039 FR-001 (eliminación completa del menú de mover; el arrastre es la única vía) per plan: supersede 0039 (contradicts) — MEDIUM
