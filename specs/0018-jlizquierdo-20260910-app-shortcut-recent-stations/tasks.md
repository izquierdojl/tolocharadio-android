---
description: "Task list for Emisoras recientes en accesos directos del icono"
---

# Tasks: Emisoras recientes en los accesos directos del icono

**Input**: Design documents from `/specs/0018-jlizquierdo-20260910-app-shortcut-recent-stations/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: INCLUIDOS Y OBLIGATORIOS. La constitución (Principio III, NON-NEGOTIABLE) exige tests unitarios y Red-Green: escribir el test que falla antes de la implementación. No se añade Robolectric; la lógica de plataforma se reduce a un wrapper delgado.

**Organization**: tareas agrupadas por user story para implementación y prueba independientes.

**Nota de entorno**: `setup-tasks.ps1` no es ejecutable (bug `python3` stub de Microsoft Store; ver `research.md` R0). Este archivo se generó desde `.specify/templates/tasks-template.md`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: US1–US4 según `spec.md`
- Rutas exactas de archivo en cada tarea

## Path Conventions

- Módulo único `app` (Android/Kotlin). Código: `app/src/main/java/com/izquierdojl/tolocharadio/`
- Tests: `app/src/test/java/com/izquierdojl/tolocharadio/`
- Base package: `com.izquierdojl.tolocharadio`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: preparar paquetes y confirmar que no hacen falta dependencias nuevas

- [X] T001 Verificar rama `0018-jlizquierdo-20260910-app-shortcut-recent-stations` y crear los paquetes/dirs `app/src/main/java/com/izquierdojl/tolocharadio/core/shortcuts/`, `app/src/main/java/com/izquierdojl/tolocharadio/domain/shortcuts/`, `app/src/test/java/com/izquierdojl/tolocharadio/core/shortcuts/` y `app/src/test/java/com/izquierdojl/tolocharadio/domain/shortcuts/`
- [X] T002 [P] Confirmar en `gradle/libs.versions.toml` y `app/build.gradle.kts` que `androidx.core:core-ktx` (ShortcutManagerCompat) y Coil ya están disponibles; NO añadir dependencias

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: modelos y piezas puras compartidas por todas las historias

**⚠️ CRITICAL**: ninguna user story puede empezar hasta completar esta fase

### Tests fundacionales (escribir primero, deben FALLAR)

- [X] T003 [P] Escribir `BuildShortcutStationsUseCaseTest` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/shortcuts/BuildShortcutStationsUseCaseTest.kt` (dedupe por `stationId` con `playedAt` máximo, orden desc, cap N, lista vacía, incluye personalizadas, nombre vacío → fallback)
- [X] T004 [P] Escribir `ShortcutSpecFactoryTest` en `app/src/test/java/com/izquierdojl/tolocharadio/core/shortcuts/ShortcutSpecFactoryTest.kt` (id `hist-<stationId>`, ranks 0..N-1, recorte de etiquetas, `iconUrl` nulo permitido)
- [X] T005 [P] Escribir `ShortcutIntentsTest` en `app/src/test/java/com/izquierdojl/tolocharadio/core/shortcuts/ShortcutIntentsTest.kt` (parseo de action/extras, `station_id` ausente/blank → null, extras desconocidos ignorados)

### Modelos y utilidades fundacionales

- [X] T006 [P] Crear `ShortcutStation` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/shortcuts/ShortcutStation.kt`
- [X] T007 Implementar `BuildShortcutStationsUseCase` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/shortcuts/BuildShortcutStationsUseCase.kt` (hacer pasar T003)
- [X] T008 Implementar `ShortcutSpec` + `ShortcutSpecFactory` en `app/src/main/java/com/izquierdojl/tolocharadio/core/shortcuts/ShortcutSpec.kt` (hacer pasar T004)
- [X] T009 Implementar `ShortcutIntents` (action `com.izquierdojl.tolocharadio.OPEN_STATION`, extras, parser) en `app/src/main/java/com/izquierdojl/tolocharadio/core/shortcuts/ShortcutIntents.kt` (hacer pasar T005)
- [X] T010 [P] Crear `PendingShortcutHolder` (`StateFlow<PendingShortcut?>`) en `app/src/main/java/com/izquierdojl/tolocharadio/core/shortcuts/PendingShortcutHolder.kt`
- [X] T011 [P] Definir la interfaz `ShortcutPublisher` (`publish`, `clear`, `maxSlots`) en `app/src/main/java/com/izquierdojl/tolocharadio/core/shortcuts/ShortcutPublisher.kt`

**Checkpoint**: modelos, parser y publisher abstracto listos; las historias pueden empezar

---

## Phase 3: User Story 1 - Ver mis últimas emisoras al mantener pulsado el icono (Priority: P1) 🎯 MVP

**Goal**: publicar hasta N accesos dinámicos (N = min(maxSlots, 5)) con las emisoras únicas más recientes del historial, ordenadas y sin duplicados; sin sesión o sin historial no se muestra ninguno.

**Independent Test**: con cuenta e historial de ≥5 emisoras, mantener pulsado el icono y ver las 4-5 más recientes sin repetidas; con cuenta vacía o sin sesión, no aparece ningún acceso de emisora. Verificación rápida: `adb shell cmd shortcut get-shortcuts --user 0 com.izquierdojl.tolocharadio`.

### Tests for User Story 1 ⚠️ (escribir primero, deben FALLAR)

- [X] T012 [P] [US1] Escribir `ShortcutSyncCoordinatorTest` en `app/src/test/java/com/izquierdojl/tolocharadio/core/shortcuts/ShortcutSyncCoordinatorTest.kt` con `ShortcutPublisher` fake + `HistoryRepo`/`SessionManager` MockK: publica al emitir `items` autenticado, no publica con `Unauthenticated`, limpia con historial vacío, respeta `maxSlots`

### Implementation for User Story 1

- [X] T013 [US1] Implementar `AndroidShortcutPublisher` en `app/src/main/java/com/izquierdojl/tolocharadio/core/shortcuts/AndroidShortcutPublisher.kt` (`ShortcutManagerCompat.setDynamicShortcuts`/`removeAllDynamicShortcuts`, `getMaxShortcutCountPerActivity` con tope 5, favicon Coil best-effort con timeout ~1,5 s y fallback al icono de la app, errores de plataforma registrados y tolerados)
- [X] T014 [US1] Implementar `ShortcutSyncCoordinator` en `app/src/main/java/com/izquierdojl/tolocharadio/core/shortcuts/ShortcutSyncCoordinator.kt` (observa `HistoryRepo.items` y `SessionManager.authState`, `Mutex` de serialización, `publish`/`clear`, clase `@Singleton` con `start()`)
- [X] T015 [US1] Arrancar el coordinador al inicio: inyectar `ShortcutSyncCoordinator` en `app/src/main/java/com/izquierdojl/tolocharadio/TolochaApp.kt` y llamar a `start()` en `onCreate` (con sesión autenticada dispara `historyRepo.list()` + publicación)
- [X] T016 [US1] Validar manualmente el Escenario A de `quickstart.md` (menú con emisoras recientes, dedupe, cap) y registrar la salida de `adb shell cmd shortcut get-shortcuts` — verificado manualmente en emulador por el usuario (2026-09-10)

**Checkpoint**: el menú del icono muestra el historial — MVP demostrable

---

## Phase 4: User Story 2 - Reproducir directamente desde el acceso del icono (Priority: P1)

**Goal**: al pulsar un acceso, la app arranca/pasa a primer plano, reproduce la emisora y abre el reproductor a pantalla completa; sesión caducada → Login con aviso; emisora no disponible → mensaje accionable.

**Independent Test**: con app cerrada, segundo plano y primer plano, pulsar un acceso del icono reproduce la emisora y muestra el full player; simulable con `adb shell am start -a com.izquierdojl.tolocharadio.OPEN_STATION --es station_id "<ID>"`.

### Tests for User Story 2 ⚠️ (escribir primero, deben FALLAR)

- [X] T017 [P] [US2] Escribir `ResolveShortcutLaunchUseCaseTest` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/shortcuts/ResolveShortcutLaunchUseCaseTest.kt` (Play desde historial en memoria, Play desde caché, fetch remoto `StationsRepo.detail`, 404/error → Unavailable, `Loading` → Wait, `Unauthenticated` → GoLogin con reason)
- [X] T018 [P] [US2] Ampliar `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` con `fullPlayerVisible` (abre/cierra) manteniendo los tests existentes en verde

### Implementation for User Story 2

- [X] T019 [US2] Crear `ShortcutLaunchResolution` (sellado: `Play`/`GoLogin`/`Wait`/`Unavailable`) e implementar `ResolveShortcutLaunchUseCase` en `app/src/main/java/com/izquierdojl/tolocharadio/domain/shortcuts/ResolveShortcutLaunchUseCase.kt` (hacer pasar T017; recibe historial en memoria + loaders de caché/remoto)
- [X] T020 [US2] Añadir a `PlayerViewModel` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt`: `fullPlayerVisible: StateFlow<Boolean>`, `openFullPlayer()`, `closeFullPlayer()` (hacer pasar T018)
- [X] T021 [US2] Actualizar `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt`: `MiniPlayer` renderiza `FullPlayerSheet` desde `fullPlayerVisible` del VM (elimina el `showFullSheet` local nunca activado); conserva `StationInfoSheet` en el gesto actual
- [X] T022 [US2] Parsear el intent de shortcut en `app/src/main/java/com/izquierdojl/tolocharadio/MainActivity.kt` (`onCreate` y `onNewIntent` → `ShortcutIntents.parse` → `PendingShortcutHolder.set`), sin navegar desde la Activity
- [X] T023 [US2] Consumir el pendiente en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`: `LaunchedEffect` sobre el holder y `authState`; `Play` → `playerVm.play` + `openFullPlayer`; `GoLogin` → navegar a `Routes.LOGIN` + snackbar "Tu sesión ha caducado. Inicia sesión de nuevo."; `Unavailable` → snackbar en español; limpiar el pendiente en todos los casos
- [X] T024 [US2] Validar manualmente el Escenario B de `quickstart.md` (cerrada/segundo plano/primer plano + misma emisora sonando) usando `am start` y el lanzador — verificado manualmente en emulador por el usuario (2026-09-10)

**Checkpoint**: US1 y US2 funcionan de forma independiente — funcionalidad completa de valor P1

---

## Phase 5: User Story 3 - Menú siempre al día con mi historial (Priority: P2)

**Goal**: el menú se actualiza con cada cambio de historial y al volver a primer plano; sin conexión muestra la última lista conocida (caché) y al pulsar aplica el error de reproducción existente.

**Independent Test**: reproducir una emisora nueva y ver que pasa a la primera posición en ≤5 s; eliminar/limpiar historial y ver desaparecer accesos; en modo avión, el menú conserva la última lista y el toque da error con reintento.

### Tests for User Story 3 ⚠️ (escribir primero, deben FALLAR)

- [X] T025 [P] [US3] Ampliar `ShortcutSyncCoordinatorTest` en `app/src/test/java/com/izquierdojl/tolocharadio/core/shortcuts/ShortcutSyncCoordinatorTest.kt`: `onForeground()` refresca (`historyRepo.list()`) y republica; con `Unavailable` usa la caché (`loadOrdered`) y no falla; `remove`/`clear` republican; sin sesión `onForeground()` no publica

### Implementation for User Story 3

- [X] T026 [US3] Exponer la última lista cacheada para el fallback offline en `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/HistoryRepo.kt` (p. ej. `suspend fun snapshot(): List<HistoryEntryDto>` leyendo `db.historyCache().loadOrdered()`), sin romper el contrato de `items`
- [X] T027 [US3] Implementar `onForeground()` y el fallback offline en `app/src/main/java/com/izquierdojl/tolocharadio/core/shortcuts/ShortcutSyncCoordinator.kt`; registrar el observador de `ProcessLifecycleOwner` en `app/src/main/java/com/izquierdojl/tolocharadio/TolochaApp.kt` (una sola vez, sin duplicar si se recrea el observer)
- [X] T028 [US3] Validar manualmente los Escenarios C y D de `quickstart.md` (sincronización ≤5 s, cambios externos al volver a primer plano, modo avión) — verificado manualmente en emulador por el usuario (2026-09-10)

**Checkpoint**: el menú refleja el historial en todo momento y es coherente offline

---

## Phase 6: User Story 4 - Nunca mostrar datos de otra cuenta (Priority: P2)

**Goal**: al cerrar sesión o cambiar de cuenta/instancia, los accesos de la cuenta anterior desaparecen de inmediato y no se muestran a otra cuenta.

**Independent Test**: con emisoras publicadas, cerrar sesión y comprobar con `adb shell cmd shortcut get-shortcuts` que no queda ningún `hist-*`; entrar con otra cuenta sin historial y verificar que sigue vacío.

### Tests for User Story 4 ⚠️ (escribir primero, deben FALLAR)

- [X] T029 [P] [US4] Ampliar tests: `ShortcutSyncCoordinatorTest` (limpia en `Unauthenticated` y en `clearNow()`), `app/src/test/java/com/izquierdojl/tolocharadio/auth/domain/usecase/LogoutUseCaseTest.kt` (invoca `clearNow` tras logout) y `app/src/test/java/com/izquierdojl/tolocharadio/servers/data/repo/ServerRepositoryTest.kt` (switch invoca `clearNow`)

### Implementation for User Story 4

- [X] T030 [US4] Añadir `clearNow()` al coordinador (`app/src/main/java/com/izquierdojl/tolocharadio/core/shortcuts/ShortcutSyncCoordinator.kt`) e invocarlo desde `app/src/main/java/com/izquierdojl/tolocharadio/domain/auth/LogoutUseCase.kt` (tras `logout()`) y desde `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/servers/ServerRepository.kt` (`switchTo`, antes de recargar datos)
- [X] T031 [US4] Validar manualmente el Escenario E de `quickstart.md` (logout, cuenta nueva sin historial, cambio de servidor) — verificado manualmente en emulador por el usuario (2026-09-10)

**Checkpoint**: sin fugas de datos entre cuentas (SC-003)

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: gates de calidad, documentación y validación integral

- [X] T032 Ejecutar `./gradlew testDebugUnitTest detekt ktlintCheck lintDebug` y corregir cualquier error o warning nuevo (obligatorio para el merge)
- [X] T033 [P] Añadir KDoc a las APIs públicas nuevas de `domain/shortcuts/` y `core/shortcuts/` (contrato, errores, hilos) según constitución IV/V
- [X] T034 Ejecutar la validación integral de `quickstart.md` (Escenarios A–G, incluidos sesión caducada F y matriz de ≥3 lanzadores + degradación G) con build de debug — verificado manualmente en emulador por el usuario (2026-09-10)
- [X] T035 Documentar en la PR: sin dependencias ni esquema nuevos, política de iconos (fallback al icono de app), límite de slots por lanzador y referencia al bug de tooling de `research.md` R0 — verificado manualmente en emulador por el usuario (2026-09-10)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias
- **Foundational (Phase 2)**: depende de Setup — BLOQUEA todas las historias
- **US1 (Phase 3)**: depende de Foundational; es el MVP
- **US2 (Phase 4)**: depende de Foundational; puede ejecutarse en paralelo con US1 (archivos distintos), aunque el valor completo se demuestra con ambos (los dos son P1)
- **US3 (Phase 5)**: depende de US1 (extiende `ShortcutSyncCoordinator` y `TolochaApp`)
- **US4 (Phase 6)**: depende de US1 (usa el coordinador y `clearNow`); independiente de US2/US3
- **Polish (Phase 7)**: depende de que las historias objetivo estén completas

### User Story Dependencies

- **US1**: sin dependencias de otras historias
- **US2**: sin dependencias de US1 (solo de Foundational); toca player, Activity y NavGraph
- **US3**: requiere US1 (mismo coordinador)
- **US4**: requiere US1 (mismo coordinador)

### Within Each User Story

- Tests PRIMERO y en rojo antes de implementar (constitución III)
- Modelos/casos de uso antes de integraciones; core antes de UI
- Cada historia termina con su validación manual de quickstart

### Parallel Opportunities

- Setup: T002 en paralelo con T001
- Foundational: T003, T004, T005 (tests) en paralelo; T006, T010, T011 en paralelo; T007 tras T003; T008 tras T004; T009 tras T005
- Tras Foundational: US1 (T012–T016) y US2 (T017–T024) pueden ir en paralelo por dos personas
- US3 y US4 pueden ir en paralelo entre sí una vez terminada US1
- Polish: T033 en paralelo con T032

---

## Parallel Example: User Story 1

```bash
# Tests de US1 en paralelo (archivos distintos):
Task: "Escribir ShortcutSyncCoordinatorTest en app/src/test/java/com/izquierdojl/tolocharadio/core/shortcuts/ShortcutSyncCoordinatorTest.kt"

# Implementación con archivos independientes en paralelo:
Task: "Implementar AndroidShortcutPublisher en app/src/main/java/com/izquierdojl/tolocharadio/core/shortcuts/AndroidShortcutPublisher.kt"
Task: "Implementar ShortcutSyncCoordinator en app/src/main/java/com/izquierdojl/tolocharadio/core/shortcuts/ShortcutSyncCoordinator.kt"
```

## Parallel Example: User Story 2

```bash
# Tests de US2 en paralelo:
Task: "Escribir ResolveShortcutLaunchUseCaseTest en app/src/test/java/com/izquierdojl/tolocharadio/domain/shortcuts/ResolveShortcutLaunchUseCaseTest.kt"
Task: "Ampliar PlayerViewModelTest con fullPlayerVisible en app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt"

# Implementación en paralelo (archivos distintos):
Task: "Implementar ResolveShortcutLaunchUseCase en app/src/main/java/com/izquierdojl/tolocharadio/domain/shortcuts/ResolveShortcutLaunchUseCase.kt"
Task: "Añadir fullPlayerVisible a PlayerViewModel en app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1: Setup
2. Completar Phase 2: Foundational (bloquea todo)
3. Completar Phase 3: US1 (menú con emisoras recientes)
4. **PARAR Y VALIDAR**: Escenario A de quickstart + `cmd shortcut get-shortcuts`
5. Demo del menú del icono

### Incremental Delivery

1. Setup + Foundational → base lista
2. US1 → menú visible (MVP)
3. US2 → reproducción directa + full player (segundo P1; experiencia completa)
4. US3 → sincronización y offline (fiabilidad)
5. US4 → privacidad entre cuentas (endurecimiento)
6. Polish → gates de CI y validación integral

### Parallel Team Strategy

Con dos personas: tras Foundational, una toma US1 y otra US2 (sin solapamiento de archivos). Al terminar US1, US3 y US4 pueden repartirse en paralelo (US3 toca coordinador/TolochaApp/HistoryRepo; US4 toca LogoutUseCase/ServerRepository y tests del coordinador — coordinar la edición de `ShortcutSyncCoordinatorTest`).

---

## Notes

- [P] = archivos distintos y sin dependencias pendientes
- La etiqueta [Story] da trazabilidad a `spec.md`
- Verificar que cada test falla antes de implementar (Red-Green)
- No commitear sin que lo pida el usuario; el hook `speckit.git.commit` es opcional
- Evitar: tareas vagas, conflictos de archivo y dependencias cruzadas que rompan la independencia de las historias
