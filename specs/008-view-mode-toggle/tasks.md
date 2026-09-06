---

description: "Task list for feature 008-view-mode-toggle"
---

# Tasks: Alternador de Vista Lista/Tarjetas en la Barra Superior

**Input**: Design documents from `/specs/008-view-mode-toggle/`

**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅, quickstart.md ✅

**Tests**: Incluidos por mandato de la constitución (Principio III, Test-First NON-NEGOTIABLE). Cada test se escribe y FALLA antes de su implementación (Red-Green).

**Organization**: Tareas agrupadas por user story (spec.md: US1 P1, US2 P2, US3 P3).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1 / US2 / US3 según spec.md
- Paths reales del repo (módulo único `app`)

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: No requiere inicialización: el proyecto, Hilt, DataStore y CI ya existen. Solo se fija el contrato de tipos compartido.

- [X] T001 [P] Crear enum `ViewMode` (LIST, GRID) con KDoc en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/ViewMode.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Preferencia persistente y fuente única de estado del modo. BLOQUEA todas las user stories.

**✅ CRITICAL**: No user story work can begin until this phase is complete

### Tests primero (Red)

- [X] T002 [P] Test de preferencia en `app/src/test/java/com/izquierdojl/tolocharadio/data/local/InstancePrefsTest.kt`: default LIST sin clave, round-trip setViewMode(GRID)→lectura, valor corrupto "XXX"→LIST sin error (FR-005, FR-006, FR-010)
- [X] T003 [P] Test de ViewModel en `app/src/test/java/com/izquierdojl/tolocharadio/feature/ViewModeViewModelTest.kt`: estado inicial = valor persistido, toggle() alterna LIST→GRID→LIST y persiste cada cambio, todas las suscripciones observan el mismo valor (FR-003, FR-004; Turbine)

### Implementación (Green)

- [X] T004 Añadir a `app/src/main/java/com/izquierdojl/tolocharadio/data/local/InstancePrefs.kt`: clave `view_mode` (stringPreferencesKey), `val viewMode: Flow<ViewMode>` con `runCatching { ViewMode.valueOf(name) }.getOrDefault(ViewMode.LIST)` y `suspend fun setViewMode(mode: ViewMode)` (patrón themeMode/startScreen, research D1)
- [X] T005 [P] Crear `ViewModeViewModel` (`@HiltViewModel`) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/ViewModeViewModel.kt`: expone `StateFlow<ViewMode>` (colección de InstancePrefs) y `toggle()` optimista que emite y persiste en `viewModelScope` (research D2, D5)

**Checkpoint**: ✅ Preferencia y estado global listos y probados (8 tests OK). Las user stories pueden empezar.

---

## Phase 3: User Story 1 - Alternar entre vista de lista y tarjetas (Priority: P1) — MVP

**Goal**: Control en la barra superior compartida, visible en las 4 secciones con emisoras, que alterna la presentación y respeta el default lista.

**Independent Test**: Abrir Explorar (instalación limpia → lista), pulsar el toggle de la TopAppBar → tarjetas sin recarga; navegar a Favoritos/Historial/Mis emisoras → todas en el modo elegido.

### Tests for User Story 1 (Red antes de implementar)

- [X] T006 [US1] Compose test en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/explore/ViewModeToggleTest.kt`: en Explorar el toggle muestra icono de modo destino y cambia el contenido lista→tarjetas→lista sin recargar (FR-001, FR-002, FR-003)

### Implementation for User Story 1

- [X] T007 Crear componente `ViewModeToggle` (IconButton M3, icono GridView/ViewList según modo destino, contentDescription dinámico "Cambiar a vista de tarjetas/lista") en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/ViewModeToggle.kt` (contract §2)
- [X] T008 Añadir el toggle como action de la TopAppBar compartida en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`, visible solo en rutas EXPLORE/FAVORITES/HISTORY/CUSTOM_STATIONS usando un `ViewModeViewModel` a ámbito de Activity (patrón PlayerViewModel, research D3, contract §3)
- [X] T009 Migrar `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/ExploreScreen.kt` al modo global: eliminar el toggle del `trailingIcon` del buscador y renderizar LazyVerticalGrid(2 col)+StationCard o LazyColumn+StationListItem según el modo compartido
- [X] T010 Eliminar `gridMode`/`toggleGrid` de `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/ExploreViewModel.kt` (código muerto tras unificar; Principio V) y actualizar su test si lo referencia
- [X] T011 [P] [US1] Añadir rama GRID (LazyVerticalGrid + StationCard) a Favoritos en `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt`; reorden drag solo en modo LIST (research D4)
- [X] T012 [P] [US1] Añadir rama GRID (LazyVerticalGrid + StationCard) a Historial en `app/src/main/java/com/izquierdojl/tolocharadio/feature/history/HistoryScreen.kt` (research D4)
- [X] T013 [P] [US1] Añadir rama GRID (LazyVerticalGrid + StationCard) a Mis emisoras en `app/src/main/java/com/izquierdojl/tolocharadio/feature/customstations/CustomStationsScreen.kt` (research D4)

**Checkpoint**: US1 completa y testeable de forma independiente (MVP). Default lista, toggle global, 4 secciones.

---

## Phase 4: User Story 2 - Persistencia del modo entre sesiones (Priority: P2)

**Goal**: La última elección del usuario sobrevive al reinicio de la app; sin preferencia previa, siempre lista.

**Independent Test**: Cambiar a tarjetas, matar la app, reabrirla → todas las secciones en tarjetas; volver a lista y repetir → lista.

### Tests for User Story 2 (Red antes de implementar)

- [X] T014 [US2] Test de persistencia entre instancias en `app/src/test/java/com/izquierdojl/tolocharadio/feature/ViewModeViewModelTest.kt`: dos instancias sucesivas del VM sobre el mismo DataStore reciben el último valor guardado; VM nuevo sin escrituras previas recibe LIST (FR-006, US-2 escenarios 1-3; Turbine)

### Implementation for User Story 2

- [X] T015 [US2] Verificar wiring completo (no se requiere código nuevo): el `ViewModeViewModel` de la TopAppBar y de cada pantalla usa la misma StateFlow de Activity y el mismo DataStore; confirmar en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` que no se crean instancias de VM por destino (research D2)

**Checkpoint**: US1 + US2 operativas: modo elegido persiste al reiniciar.

---

## Phase 5: User Story 3 - Transición consistente sin pérdida de contexto (Priority: P3)

**Goal**: Alternar no recarga datos ni pierde búsqueda, filtros, paginación ni estados vacío/error.

**Independent Test**: En Explorar buscar "rock", cargar 2 páginas, alternar → mismos resultados en el nuevo formato sin spinner completo.

### Tests for User Story 3 (Red antes de implementar)

- [X] T016 [US3] Compose test de invariantes en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/explore/ViewModeToggleTest.kt`: con búsqueda y paginados cargados, alternar conserva búsqueda, items y hasMore; con EmptyState, alternar no rompe; con ErrorBanner, alternar mantiene reintento (FR-007, edge cases spec)

### Implementation for User Story 3

- [X] T017 [US3] Auditar las ramas de render (Explorar, Favoritos, Historial, Mis emisoras) para garantizar que el modo solo cambia el contenedor de presentación y nunca resetea UiState/offset/filtros; corregir desviaciones en `ExploreScreen.kt`, `FavoritesScreen.kt`, `HistoryScreen.kt`, `CustomStationsScreen.kt` (FR-007)

**Checkpoint**: Las 3 user stories son independientes y completas.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Gates de calidad y validación end-to-end

- [X] T018 [P] KDoc en APIs nuevas (`ViewMode`, `InstancePrefs.viewMode/setViewMode`, `ViewModeViewModel`, `ViewModeToggle`) conforme a Principio I/IV
- [X] T019 Ejecutar gates CI: `./gradlew :app:testDebugUnitTest :app:lintDebug detekt ktlintCheck` sin errores (Principio III, Quality Gates)
- [X] T020 Ejecutar validación end-to-end de `specs/008-view-mode-toggle/quickstart.md` (escenarios E1-E6) en emulador y registrar resultado — verificado manualmente en emulador por el usuario (2026-09-06)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (T001)**: sin dependencias.
- **Foundational (T002-T005)**: depende de T001. BLOQUEA todas las stories (tests T002/T003 fallan hasta T004/T005).
- **US1 (T006-T013)**: depende de Phase 2. Dentro de US1: T007 → T008; T009/T010 tras T008 (usan el modo global); T011-T013 paralelizables entre sí tras T004/T005 (no dependen de T008/T009).
- **US2 (T014-T015)**: depende de Phase 2 (y de T008 para validar el wiring).
- **US3 (T016-T017)**: depende de US1 completo (T009-T013).
- **Polish (T018-T020)**: depende de todas las stories.

### User Story Dependencies

- **US1 (P1)**: solo Foundational. MVP.
- **US2 (P2)**: Foundational; no depende de US1 salvo T015 (verificación de wiring compartido).
- **US3 (P3)**: hereda de US1 (misma mecánica); sus invariantes se validan sobre las ramas de render.

### Parallel Opportunities

- T002 y T003 en paralelo (archivos de test distintos).
- T005 en paralelo con T004 (archivos distintos; T003 valida contra T005).
- T011, T012, T013 en paralelo (3 pantallas distintas).
- T018 en paralelo con T019/T020 solo como documentación (no bloquea).

---

## Parallel Example: User Story 1

```text
# Tras Phase 2, lanzar en paralelo:
Task: "Rama GRID en FavoritesScreen.kt" (T011)
Task: "Rama GRID en HistoryScreen.kt" (T012)
Task: "Rama GRID en CustomStationsScreen.kt" (T013)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 + Phase 2 (preferencia + VM probados)
2. Phase 3: US1 → toggle funcional en las 4 secciones
3. **STOP and VALIDATE**: quickstart E1, E2, E6
4. Demo del MVP

### Incremental Delivery

1. MVP (US1) → validar
2. US2 → persistencia al reinicio → validar E3
3. US3 → invariantes de contexto → validar E4, E5
4. Polish → gates CI + quickstart completo

### Notes

- Red-Green: T002→T004, T003→T005, T006→T007/T008/T009, T014→T015, T016→T017.
- Commit por tarea o grupo lógico tras validar su checkpoint.
- Evitar: reordenar drag en GRID (fuera de alcance, research D4), preferencia por sección (FR-004 prohíbe).
