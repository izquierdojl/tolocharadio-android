# Tasks: Favoritos — lista, marcado y navegación

**Input**: Design documents from `/specs/003-favorites-management/` (plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md)

**Prerequisites**: plan.md (required) ✅, spec.md (required, 4 user stories) ✅, research.md (8 decisiones) ✅, data-model.md ✅, contracts/ ✅

**Tests**: INCLUIDOS — exigidos por la constitución III (Test-First NON-NEGOTIABLE: todo UseCase/Repo/VM nuevo con JUnit + Turbine; Compose Test para flujos críticos de favoritas) y por los criterios SC de la spec. Regla Red-Green: cada test se escribe primero y MUST fallar antes de implementar.

**Organization**: tareas agrupadas por historia de usuario; cada historia es un incremento independientemente testeable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: paralelizable (archivos distintos, sin dependencias pendientes)
- **[Story]**: historia propietaria ([US1]–[US4] según spec.md)
- Rutas base: `app/src/main/java/com/izquierdojl/tolocharadio/…`, tests en `app/src/test/…`, UI en `app/src/androidTest/…`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar el único punto abierto contra el servidor y dejar la línea base en verde antes de tocar código.

- [x] T001 Confirmar el nombre exacto del campo de `PUT /favorites/order` en `GET /api/v1/openapi.json` de la instancia y anotarlo en `specs/003-favorites-management/contracts/favorites-api.md`
- [x] T002 Ejecutar la línea base en verde (`:app:testDebugUnitTest`, `lintDebug`, `detekt`) según `specs/003-favorites-management/quickstart.md` antes de empezar

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Capa data + dominio de favoritos lista (API, caché, repo con `favoriteIds` compartido, casos de uso). BLOQUEA todas las historias.

**⚠️ CRITICAL**: ninguna tarea de US1–US4 puede empezar hasta completar esta fase.

### Tests primero (Red-Green: escribir, ver fallar, luego implementar)

- [x] T003 [P] Test de serialización de `ReorderBody` y `FavoriteListDto` (orden) ampliando `app/src/test/java/com/izquierdojl/tolocharadio/data/remote/dto/DtoSerializationTest.kt`
- [x] T004 [P] Test de `FavoritesRepo.list/reorder` (éxito, 401, 404, 422, 503, idempotencia de `DELETE`) en `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/FavoritesRepoTest.kt`
- [x] T005 [P] Test de `FavoritesCacheDao` (replaceAll/loadOrdered) y de la migración Room v1→v2 en `app/src/test/java/com/izquierdojl/tolocharadio/data/local/FavoritesCacheTest.kt`
- [x] T006 [P] Tests de `ObserveFavoritesUseCase` (red + fallback a caché, dedup VR-01) y `ReorderFavoritesUseCase` (permutación exacta VR-02, rechazo de `stationId` en blanco VR-03) en `app/src/test/java/com/izquierdojl/tolocharadio/domain/ObserveReorderUseCasesTest.kt`

### Implementación

- [x] T007 Añadir `PUT favorites/order` (`ReorderBody`) a `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/api/FavoritesApi.kt` (usa el nombre de campo confirmado en T001)
- [x] T008 [P] Crear entidad `CachedFavorite` + `FavoritesCacheDao` en `app/src/main/java/com/izquierdojl/tolocharadio/data/local/FavoritesCache.kt`
- [x] T009 Subir `TolochaDb` a versión 2 con migración v1→v2 y limpieza al hacer logout/cambiar `baseUrl` en `app/src/main/java/com/izquierdojl/tolocharadio/data/local/TolochaDb.kt`
- [x] T010 Ampliar `FavoritesRepo` con `observe()/list()/reorder()` y `Flow<Set<String>> favoriteIds` compartido en `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/FavoritesRepo.kt`
- [x] T011 [P] Crear `ObserveFavoritesUseCase` (red + caché offline marcada, verdad = servidor) en `app/src/main/java/com/izquierdojl/tolocharadio/domain/ObserveFavoritesUseCase.kt`
- [x] T012 [P] Crear `ReorderFavoritesUseCase` (valida permutación exacta antes del `PUT`) en `app/src/main/java/com/izquierdojl/tolocharadio/domain/ReorderFavoritesUseCase.kt`

**Checkpoint**: `T003–T006` en verde tras implementar `T007–T012`; repo expone lista, ids compartidos y reorder. Las historias pueden empezar.

---

## Phase 3: User Story 1 - Ver mis emisoras favoritas (Priority: P1) ⭐ MVP

**Goal**: sección Favoritos con lista completa en orden del servidor, fecha relativa, estado vacío con CTA a Explorar y error con reintento (FR-001/FR-002/FR-007-parcial/FR-009/FR-010).

**Independent Test**: con cuenta con ≥3 favoritas se ve la lista en < 2 s (SC-001); con cuenta nueva se ve el vacío con botón a Explorar; sin red con caché se ve lista offline; sin caché se ve error + reintento.

### Tests primero

- [x] T013 [P] [US1] Tests del VM (carga, vacío, error+reintento, offline con/sin caché) con Turbine en `app/src/test/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModelTest.kt`
- [x] T014 [P] [US1] Compose Test base (lista visible, vacío con CTA, error con reintento) en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreenTest.kt` — escrito y compila; PENDIENTE ejecución en dispositivo (emulador local API 37 incompatible con compose-ui-test, 2026-09-05)

### Implementación

- [x] T015 [P] [US1] Helper de fecha relativa ("hace X", `addedAt=0`/futuro → ocultar) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/RelativeTime.kt`
- [x] T016 [US1] `FavoritesViewModel` con `FavoritesUiState` sellado (`Loading/Empty/Content/Error`) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModel.kt`
- [x] T017 [US1] `FavoritesScreen` (tarjetas con Coil + placeholder, `EmptyState`, offline badge, botón reintento) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt`
- [x] T018 [US1] Sustituir el placeholder de `Routes.FAVORITES` por `FavoritesScreen` real en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`

**Checkpoint**: US-1 funciona sola de extremo a extremo (SC-001). Se puede validar y demostrar sin US-2–US-4.

---

## Phase 4: User Story 2 - Marcar y desmarcar desde cualquier sitio (Priority: P1)

**Goal**: corazón coherente en Explorar (lista/grid), ficha y Favoritos; toggle optimista con reversión; deshacer 10 s al quitar en la lista (FR-003/FR-004/FR-005).

**Independent Test**: marcar en Explorar → relleno en ficha y Favoritos; quitar en Favoritos → `Snackbar` 10 s → deshacer restaura en su sitio; fallo de red → reversión < 2 s (SC-002/SC-003/SC-006).

### Tests primero

- [x] T019 [P] [US2] Ampliar `app/src/test/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModelTest.kt` (toggle+rollback, quitar+deshacer 10 s, expiración confirma)
- [x] T020 [P] [US2] Ampliar `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreenTest.kt` (toggle, `Snackbar` deshacer, reversión ante error) — escrito y compila; PENDIENTE ejecución en dispositivo (ver T014, 2026-09-05)
- [x] T021 [P] [US2] Ampliar `app/src/test/java/com/izquierdojl/tolocharadio/feature/explore/ExploreViewModelsTest.kt` (favoritas hidratadas desde el flujo compartido, toggle coherente)

### Implementación

- [x] T022 [US2] `removeWithUndo()`/`undoRemove()` (reinserción en índice previo, expiración 10 s) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModel.kt`
- [x] T023 [US2] `FavoriteButton` + `Snackbar` con Deshacer (10 s, `contentDescription` ES) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt`
- [x] T024 [US2] Hidratar `favoriteIds` desde el `Flow` compartido del repo (eliminar set local divergente) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/ExploreViewModel.kt`

**Checkpoint**: US-1 y US-2 funcionan y el marcado es coherente en todas las pantallas (SC-003).

---

## Phase 5: User Story 3 - Ordenar mis favoritas (Priority: P2)

**Goal**: reorden drag & drop con asa + autoguardado al soltar, indicador no bloqueante, reversión al último confirmado y conflicto "gana servidor" (FR-006).

**Independent Test**: arrastrar B a primera posición → persiste tras reinicio (SC-004); corte de red al guardar → reversión + reintento; orden divergente en servidor → se muestra el del servidor + aviso.

### Tests primero

- [x] T025 [P] [US3] Ampliar `app/src/test/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModelTest.kt` (`moveItem`+`commitOrder`, error→reversión, divergencia→gana-servidor)
- [x] T026 [P] [US3] Ampliar `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreenTest.kt` (arrastrar por el asa cambia el orden visible) — escrito y compila; PENDIENTE ejecución en dispositivo (ver T014, 2026-09-05)

### Implementación

- [x] T027 [US3] Lógica `moveItem()`/`commitOrder()` (`savingOrder`, reversión, gana-servidor + aviso) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModel.kt`
- [x] T028 [US3] Fila arrastrable con asa (`contentDescription` "Reordenar") e indicador de guardado no bloqueante en `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt`

**Checkpoint**: US-3 funciona sobre la lista de US-1 sin romper US-2 (SC-004).

---

## Phase 6: User Story 4 - Navegar y escuchar desde Favoritos (Priority: P2)

**Goal**: Favoritos en la bottom bar con paridad web, ficha por tarjeta, play con mini-player persistente y guard de sesión (FR-007/FR-008).

**Independent Test**: bottom bar → Favoritos (< 2 s); sin sesión → Login; tarjeta → ficha; play → audio continuo por 3 secciones con mini-player visible (SC-005).

### Tests primero

- [x] T029 [P] [US4] Ampliar `app/src/test/java/com/izquierdojl/tolocharadio/core/ui/navigation/RoutesTest.kt` (`FAVORITES` en `AUTH_REQUIRED`, redirección sin sesión)

### Implementación

- [x] T030 [US4] Navegación tarjeta→ficha (`Routes.stationDetail`), play persistente y estado seleccionado de "Favoritos" en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` (+ callbacks en `FavoritesScreen.kt` si faltan)

**Checkpoint**: las 4 historias funcionan de extremo a extremo; SC-005 verificado manual + test.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: gates de merge y validación final (constitución III/IV/V, spec 002 para a11y visual).

- [x] T031 [P] Dejar Android Lint + ktlint + Detekt sin errores ni warnings nuevos y limpiar código muerto en los archivos tocados (`app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/`, `data/repo/FavoritesRepo.kt`, `data/local/`, `domain/`)
- [x] T032 [P] Recorrido completo de `specs/003-favorites-management/quickstart.md` (automatizado + manual con las dos cuentas de prueba) y anotar resultado en la PR — automatizado OK; PENDIENTE recorrido manual contra instancia real (2026-09-05)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias — empezar aquí (T001 confirma el campo del `PUT`; T002 congela la base).
- **Foundational (Phase 2)**: depende de Setup — **BLOQUEA** US1–US4.
- **User Stories**: todas dependen de Foundational; entre ellas solo integración UI, no bloqueo de datos:
  - US1 (Phase 3) → US2 (Phase 4) → US3 (Phase 5) → US4 (Phase 6) en orden de entrega recomendado (P1 → P2).
  - Con equipo: tras Foundational, US2 puede empezarse en paralelo a US1 (archivos distintos salvo `FavoritesScreen.kt`/`FavoritesViewModel.kt`, que se integran al final); US3/US4 requieren la pantalla de US1.
- **Polish (Phase 7)**: tras las historias que se entreguen.

### Within Each Story

- Tests (T013/T014, T019–T021, T025/T026, T029) escritos PRIMERO y en rojo antes de implementar; modelos/VM antes que UI; UI antes que integración de navegación.

### Parallel Opportunities

- T003/T004/T005/T006 (4 archivos de test distintos) en paralelo; T008 + T011 + T012 en paralelo; T013/T014/T015 en paralelo; T019/T020/T021 en paralelo; T025/T026 en paralelo; T031/T032 en paralelo.
- Historias en paralelo por persona una vez cerrada Phase 2 (con integración final en `FavoritesScreen.kt`/`FavoritesViewModel.kt`).

---

## Parallel Example: Foundational tests (Red-Green)

```bash
Task: "Ampliar DtoSerializationTest con ReorderBody en app/src/test/.../data/remote/dto/DtoSerializationTest.kt"   # T003
Task: "Crear FavoritesRepoTest en app/src/test/.../data/repo/FavoritesRepoTest.kt"                                   # T004
Task: "Crear FavoritesCacheTest en app/src/test/.../data/local/FavoritesCacheTest.kt"                               # T005
Task: "Crear ObserveReorderUseCasesTest en app/src/test/.../domain/ObserveReorderUseCasesTest.kt"                    # T006
```

## Parallel Example: User Story 1

```bash
Task: "Tests VM con Turbine en .../feature/favorites/FavoritesViewModelTest.kt"          # T013
Task: "Compose Test base en .../feature/favorites/FavoritesScreenTest.kt (androidTest)"  # T014
Task: "Helper RelativeTime en .../feature/favorites/RelativeTime.kt"                     # T015
```

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Phase 1 Setup + Phase 2 Foundational (bloqueante).
2. Phase 3 US1 (ver lista) → **STOP y VALIDAR** (SC-001, vacío, offline, error+reintento).
3. Phase 4 US2 (marcar/deshacer) → **STOP y VALIDAR** (SC-002/SC-003/SC-006) → MVP demoable (el corazón del pedido: ver + marcar favoritas).
4. US3 → US4 como incrementos (orden personalizado, navegación/play).

### Incremental Delivery

Setup + Foundational → +US1 (demo) → +US2 (MVP) → +US3 (demo) → +US4 (cierre) → Polish. Cada fase deja los gates verdes.

---

## Notes

- [P] = archivos distintos sin dependencias; [USn] traza cada tarea a su historia.
- `ToggleFavoriteUseCase` se reutiliza sin cambios (ya optimista con rollback); no crear servicio paralelo.
- `PUT /favorites/order` envía la lista COMPLETA (permutación exacta); validar en dominio (VR-02), no solo en UI.
- Caché Room solo-lectura; verdad = servidor; limpiar al logout/cambiar `baseUrl`.
- Commit tras cada tarea o grupo lógico; parar en cada checkpoint y validar la historia.


---

## Phase 8: Convergence

**Origen**: `/speckit.converge` 2026-09-20 — 11 FR, 4 historias y 5 principios revisados contra el código. Repo/cache/reorden con deshacer, estados y offline conformes; el único hueco es la referencia al flujo de Login retirado.

- [X] T033 Anotar el supersede de la 0024/Constitución II en `specs/003-favorites-management/spec.md`: marcar FR-007 ("sin sesión redirige a Login") y cualquier AC/assumption equivalente como SUPERSEDED por la spec 0024 (sin pantallas de login: arranque bloqueante `StartupGate.NeedsCredentials` y error de credenciales → Editar servidor) per FR-007 + Constitución II (contradicts) — MEDIUM
