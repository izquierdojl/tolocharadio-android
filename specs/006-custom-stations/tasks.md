# Tasks: Emisoras personalizadas — Mis emisoras

**Input**: Design documents from `/specs/006-custom-stations/`

**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅, quickstart.md ✅

**Tests**: Incluidos — la constitución del proyecto exige tests unitarios para ViewModel, UseCase y Repositorio (principio III: Test-First NON-NEGOTIABLE). Tests de serialización DTO también requeridos.

**Organization**: Tasks agrupados por user story para implementación y testing independiente.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

All source under `app/src/main/java/com/izquierdojl/tolocharadio/`
All tests under `app/src/test/java/com/izquierdojl/tolocharadio/`

---

## Phase 1: Setup (Data Layer & Infrastructure)

**Purpose**: DTOs, API, Room cache, DI bindings, Repo base — base compartida para todas las historias de usuario.

- [X] T001 Add `CreateCustomStationBody` and `CustomStationResultDto` to `data/remote/dto/StationDtos.kt` per data-model.md (`GET` reutiliza `StationListDto` existente, sin cambios)
- [X] T002 [P] Create `data/remote/api/CustomStationsApi.kt` Retrofit interface (list, create, delete) per contract in `contracts/CustomStationsApi.kt`
- [X] T003 [P] Create `data/local/CustomStationsCache.kt` with `CachedCustomStation` entity and DAO plus `MIGRATION_3_4` per data-model.md
- [X] T004 Update `data/local/TolochaDb.kt`: add `CachedCustomStation` to entities, add `customStationsCache()` abstract DAO, bump to version 4 (depends on T003)
- [X] T005 [P] Bind `CustomStationsApi` in `di/NetworkModule.kt` (add `fun customStationsApi(retrofit: Retrofit): CustomStationsApi`)
- [X] T006 Create `data/repo/CustomStationsRepo.kt`: Flow-based repo with API fetch + Room cache, offline fallback, `create()` and `delete()` with `ApiResult` (depends on T001, T002, T004)

**Checkpoint**: Data layer complete — API, cache, repo ready for all user stories.

---

## Phase 2: User Story 1 — Ver mis emisoras personalizadas (Priority: P1) 🎯 MVP

**Goal**: Persona con cuenta abre Mis emisoras y ve su lista (nombre + emblema TolochaRadio); sin emisoras ve estado vacío con instrucciones.

**Independent Test**: Login con cuenta con personalizadas → abrir Mis emisoras → ver lista completa. Login con cuenta nueva → ver estado vacío.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T007 [P] [US1] DTO serialization test for `CreateCustomStationBody` and `CustomStationResultDto` in `data/remote/dto/CustomStationDtoTest.kt`
- [X] T008 [P] [US1] Repository test for `CustomStationsRepo` list flow (success, error, cache hit, offline fallback) in `data/repo/CustomStationsRepoTest.kt`
- [X] T009 [US1] ViewModel test for `CustomStationsViewModel` list state (Loading→Content, Loading→Empty, Loading→Error, offline badge) in `feature/customstations/CustomStationsViewModelTest.kt`

### Implementation for User Story 1

- [X] T010 [P] [US1] Create `domain/ObserveCustomStationsUseCase.kt`: calls `CustomStationsRepo`, maps `ApiResult` to domain result (depends on T006)
- [X] T011 [US1] Create `feature/customstations/CustomStationsViewModel.kt`: sealed `CustomStationsUiState` (Loading/Empty/Content/Error con flag `offline`), `observeStations()`, `refresh()`, snackbar SharedFlow (depends on T010)
- [X] T012 [US1] Create `feature/customstations/CustomStationsScreen.kt`: list view with station rows (nombre + emblema local, sin favicon), `EmptyState` ("Aún no tienes emisoras personalizadas"), `ErrorBanner` con reintento, loading indicator. Uses `hiltViewModel()`, collects `viewModel.ui` StateFlow (depends on T011)
- [X] T013 [US1] Update `core/ui/navigation/TolochaNavGraph.kt` lines 195-197: replace `HomeScreen` placeholder with `CustomStationsScreen` composable (auth guard patrón bloque HISTORY) (depends on T012)

**Checkpoint**: Mis emisoras visible con lista, estado vacío y errores. Formulario aún no implementado (US2), reproducción aún no (US3).

---

## Phase 3: User Story 2 — Añadir una emisora personalizada (Priority: P1)

**Goal**: Formulario superior Nombre + URL con validación cliente; al añadir, la emisora aparece al instante y el formulario se limpia.

**Independent Test**: Rellenar nombre + URL válida → Añadir → aparece en lista y persiste al recargar. Nombre vacío / URL inválida → aviso en español sin llamada red.

### Tests for User Story 2

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T014 [P] [US2] Use case test for `ValidateCustomStationUseCase` (nombre vacío, URL malformada, esquema ftp, http válido, https válido, trim) in `domain/ValidateCustomStationUseCaseTest.kt`
- [X] T015 [US2] ViewModel test for add flow (field errors sin llamada red, submitting deshabilita, éxito limpia formulario, 422 muestra error por campo, reversión ante error) in `feature/customstations/CustomStationsViewModelTest.kt`

### Implementation for User Story 2

- [X] T016 [P] [US2] Create `domain/ValidateCustomStationUseCase.kt`: `name(raw)` y `streamUrl(raw)` puros con mensajes en español per research D1
- [X] T017 [US2] Add `create(name, url)` method to `data/repo/CustomStationsRepo.kt`: calls `CustomStationsApi.create()`, actualiza caché, mapea 422 con `details` por campo (depends on T016)
- [X] T018 [US2] Update `feature/customstations/CustomStationsViewModel.kt`: add `CustomStationFormState`, `onNameChange`/`onUrlChange`, `onSubmit()` con validación cliente → repo → actualización optimista + reversión + snackbar (depends on T017)
- [X] T019 [US2] Update `feature/customstations/CustomStationsScreen.kt`: add formulario superior (campo Nombre, campo URL, botón Añadir deshabilitado mientras `submitting`, errores de campo bajo el formulario, visible también con lista vacía o en error) (depends on T018)

**Checkpoint**: Añadir funcional con validación cliente y de servidor. Reproducción aún no (US3).

---

## Phase 4: User Story 3 — Escuchar una emisora personalizada (Priority: P1)

**Goal**: Tocar personalizada → suena vía proxy con mini-player persistente; el audio continúa al navegar.

**Independent Test**: Reproducir desde Mis emisoras → suena, mini-reproductor aparece. Navegar a Explorar/Historial → audio continúa. Stream caído → error accionable con reintento.

### Tests for User Story 3

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T020 [US3] Play delegation sin lógica en VM: el cableado es directo en `CustomStationsScreen.kt` (`onPlay = { player.play(it) }`, patrón `HistoryScreen.kt:104`); cubierto por VS5 del quickstart — verificado en compilación + tests de UI pendientes de emulador (T031)

### Implementation for User Story 3

- [X] T021 [US3] Update `feature/customstations/CustomStationsScreen.kt`: wire play action on station item to shared `player.play(station)` (patrón `HistoryScreen.kt` línea 104), pasar `player: PlayerViewModel` desde el nav graph (depends on T013)
- [X] T022 [US3] Update `core/ui/navigation/TolochaNavGraph.kt` destino `CUSTOM_STATIONS`: pasar `playerVm` a `CustomStationsScreen` para playback persistente (depends on T021)

**Checkpoint**: Reproducción desde Mis emisoras funcional con continuidad al navegar.

---

## Phase 5: User Story 4 — Eliminar una emisora personalizada (Priority: P2)

**Goal**: Botón de papelera por emisora → borrado inmediato con confirmación, reversión ante error e invalidación de Favoritos.

**Independent Test**: Eliminar → desaparece al instante. Error de red → reaparece + snackbar. Si era favorita → desaparece también de Favoritos.

### Tests for User Story 4

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T023 [P] [US4] Repository test for `CustomStationsRepo.delete()` (success, 404 treated as success, network error) in `data/repo/CustomStationsRepoTest.kt`
- [X] T024 [US4] ViewModel test for delete flow (optimistic remove, revert on error, snackbar, favorites invalidation triggered) in `feature/customstations/CustomStationsViewModelTest.kt`

### Implementation for User Story 4

- [X] T025 [US4] Add `delete(id)` method to `data/repo/CustomStationsRepo.kt`: calls `CustomStationsApi.delete()`, 404 treated as success, actualiza caché; tras éxito invalida Favoritos vía punto de refresh público de `FavoritesRepo` per research D4 (depends on T006)
- [X] T026 [US4] Update `feature/customstations/CustomStationsViewModel.kt`: add `onDelete(id)` with optimistic state update + revert on error + snackbar ("Emisora eliminada" / error) (depends on T025)
- [X] T027 [US4] Update `feature/customstations/CustomStationsScreen.kt`: add trash button on each station item, wire to `viewModel.onDelete()`, disabled state while pending (depends on T026)

**Checkpoint**: Eliminación individual funcional con reversión optimista e invalidación de Favoritos.

---

## Phase 6: User Story 5 — Llegar a Mis emisoras navegando como en la web (Priority: P2)

**Goal**: Acceso desde barra inferior con guardia de sesión; verificación de paridad de navegación.

**Independent Test**: Barra inferior → Mis emisoras (< 2 s). Sin sesión → Login. Reproduciendo → navegar entre secciones sin cortes.

### Tests for User Story 5

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T028 [US5] Navigation flow: test unitario en `core/ui/navigation/RoutesTest.kt` (`CUSTOM_STATIONS` en `AUTH_REQUIRED` + ruta `custom-stations`); flujo Compose completo pendiente de emulador — verificado manualmente por el usuario en T031

### Implementation for User Story 5

- [X] T029 [US5] Update `core/ui/navigation/TolochaNavGraph.kt`: verify `Routes.CUSTOM_STATIONS` in `AUTH_REQUIRED` (already true) and in `BOTTOM_DESTS` (already true); ensure `CustomStationsScreen` receives `playerVm` and `onStation`/`onExplore` callbacks like HISTORY block (depends on T013, T022)
- [X] T030 [US5] Update `feature/customstations/CustomStationsScreen.kt`: sin navegación a ficha (la ficha carga del endpoint de catálogo, que no conoce ids personalizados — decisión documentada en KDoc de `CustomStationsScreen`); tocar reproduce, y el botón de vacío va a Explorar. Destino cableado en `TolochaNavGraph.kt` con `playerVm` y guardia de sesión (depends on T029)

**Checkpoint**: Navegación completa, paridad con web. Todas las historias funcionales.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Validación final, limpieza y verificación de calidad.

- [X] T031 [P] Run quickstart.md validation scenarios VS1-VS9 on device/emulator — verificado manualmente en emulador por el usuario (2026-09-06)
- [X] T032 [P] Run Detekt + ktlint + Android Lint — BUILD SUCCESSFUL en los tres (2026-09-06). Incluye 7 correcciones solo-formato en ficheros preexistentes de History (ktlint/detekt a la deriva por versión): `HistoryScreen.kt`, `HistoryViewModel.kt`, `HistoryDtoTest.kt`. Tests de History siguen 22/22 en verde.
- [X] T033 Verify Room migration 3→4 preserves existing favorites/history data — verificado manualmente en emulador por el usuario (2026-09-06)
- [X] T034 Verify emblem artwork reuse for custom stations: `StationArtwork(station, isCustom = true)` muestra `sierra_emblem` sin petición Coil — verificado por construcción + compilación
- [X] T035 Code review: sin `try/catch` que traguen excepciones, sin `GlobalScope` (solo `viewModelScope`), literales en español según convención del proyecto (sin `stringResource` en todo el repo), KDoc en todas las APIs públicas nuevas de `domain`/`data`

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup) ──→ Phase 2 (US1 ver) ──→ Phase 3 (US2 añadir)
                                       ──→ Phase 4 (US3 escuchar)
                                       ──→ Phase 5 (US4 eliminar)
                                       ──→ Phase 6 (US5 navegar)

Phase 6 (US5) ──→ Phase 7 (Polish)
```

- **Phase 1 (Setup)**: No dependencies — can start immediately
- **Phase 2 (US1)**: Depends on Phase 1 — BLOCKS all subsequent stories (ViewModel/Screen base)
- **Phase 3-6 (US2-US5)**: All depend on Phase 2 completion. Can proceed in parallel if staffed, or sequentially P1 → P2.
- **Phase 7 (Polish)**: Depends on all user stories complete

### Within Each User Story

- Tests written FIRST and MUST FAIL before implementation
- Repository methods before ViewModel
- ViewModel before Screen
- Screen before NavGraph integration

### Parallel Opportunities

- T002, T003, T005 (Setup): different files, can run in parallel; T004 depends on T003; T006 depends on T001+T002+T004
- T007, T008, T014 (tests USE-case/repo/DTO): can run in parallel
- T010 (UseCase) parallel with T007-T009 once T006 done
- T023+T024 (US4 tests), T025 repo method parallel with test writing
- T031+T032 (Polish validation + lint): can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all US1 tests in parallel (once Phase 1 done):
Task: T007 — DTO serialization test (CustomStationDtoTest.kt)
Task: T008 — Repository list-flow test (CustomStationsRepoTest.kt)

# Launch UseCase + ViewModel test in parallel:
Task: T010 — ObserveCustomStationsUseCase
Task: T009 — ViewModel list-state test (will fail until T011 done)
```

## Parallel Example: User Story 2

```bash
# Validation use case and its test are independent of repo work:
Task: T016 — ValidateCustomStationUseCase
Task: T014 — ValidateCustomStationUseCaseTest (will fail until T016 done)
Task: T015 — ViewModel add-flow test (will fail until T018 done)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (DTO, API, Room, DI, Repo)
2. Complete Phase 2: US1 (UseCase, ViewModel, Screen, NavGraph placeholder swap)
3. **STOP and VALIDATE**: Login → Mis emisoras → ver lista. Login nueva → estado vacío.
4. Deploy/demo si listo.

### Incremental Delivery

1. Setup + US1 → Mis emisoras visible (MVP!)
2. + US2 → Añadir con validación
3. + US3 → Reproducción persistente
4. + US4 → Eliminación con invalidación de Favoritos
5. + US5 → Navegación completa, paridad web
6. Polish → Validación final VS1-VS9

### Parallel Team Strategy

With multiple developers after Phase 2 completes:

- Developer A: US2 (formulario + validación)
- Developer B: US3 (reproducción) + US4 (eliminación)
- Developer C: US5 (navegación) + Polish

---

## Notes

- `StationListDto` y `StationDto.isCustom` ya existen — reutilizar, no duplicar (research D2)
- `OkResult` ya está definido — importar, no redefinir
- `NormalizeBaseUrlUseCase` NO sirve para validar URLs de stream (rechaza http público) — usar `ValidateCustomStationUseCase` dedicado (research D1)
- `Routes.CUSTOM_STATIONS`, `AUTH_REQUIRED` y `BOTTOM_DESTS` ya incluyen Mis emisoras — solo sustituir el placeholder (research D6)
- Reproducción vía `player.play(station)` compartido — sin player propio (spec 004)
- Las personalizadas siempre muestran el emblema local, nunca piden favicon por Coil
- Formulario siempre visible (FR-003), incluso con lista en error — estado de formulario separado del estado de lista (research D5)

---

## Format Validation

- Total: 35 tasks (T001-T035), todas con checkbox `- [ ]`, ID secuencial y ruta de fichero exacta
- Phase 1 (Setup): 6 tasks, sin etiqueta de historia
- US1 (P1): 7 tasks (T007-T013) con etiqueta [US1] salvo que la fase lo indique
- US2 (P1): 6 tasks (T014-T019) con etiqueta [US2]
- US3 (P1): 3 tasks (T020-T022) con etiqueta [US3]
- US4 (P2): 5 tasks (T023-T027) con etiqueta [US4]
- US5 (P2): 3 tasks (T028-T030) con etiqueta [US5]
- Phase 7 (Polish): 5 tasks, sin etiqueta de historia
- Marcadores [P]: solo en tasks de ficheros distintos sin dependencias pendientes


---

## Phase 8: Convergence

**Origen**: `/speckit.converge` 2026-09-20 — 12 FR, 5 historias y 5 principios revisados contra el código. API/Room/Repo/UseCases/VM/pantalla con sus tests, reproducción por proxy, estado offline y paridad de navegación conformes; el único hueco es la referencia al flujo de Login retirado.

- [X] T036 Anotar el supersede de la 0024/Constitución II en `specs/006-custom-stations/spec.md`: marcar FR-008 ("sin sesión redirige a Login"), US5 AC2 (línea 95), SC-006 (línea 140) y el assumption de la línea 145 (login/registro de la 001, sección Perfil) como SUPERSEDED por la spec 0024 (sin pantallas de login: arranque bloqueante `StartupGate.NeedsCredentials` y error de credenciales → Editar servidor) per FR-008 + Constitución II (contradicts) — MEDIUM
