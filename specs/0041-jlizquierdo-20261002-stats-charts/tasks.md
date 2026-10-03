---
description: "Task list for feature 0041 - Gráficas de escucha (estadísticas) en la app Android"
---

# Tasks: Gráficas de escucha (estadísticas) en la app Android

**Input**: Design documents from `/specs/0041-jlizquierdo-20261002-stats-charts/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/stats-api.md,
quickstart.md

**Tests**: Sí. La constitución (Principio III, Test-First NON-NEGOTIABLE) exige tests para
`data`, `domain` y `ViewModel`; los contratos DTO exigen tests de serialización; y hay 1 test
Compose de flujo crítico (entrada en Configuración → vista/estado de error). Regla Red-Green:
el test se escribe antes y debe fallar antes de la implementación.

**Organization**: Tareas agrupadas por historia de usuario. Cada historia es
independientemente implementable y testeable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivo distinto, sin dependencias pendientes)
- **[Story]**: US1/US2/US3
- Todas las tareas incluyen rutas de archivo exactas

## Path Conventions

Módulo único Android: `app/src/main/java/com/izquierdojl/tolocharadio/`,
`app/src/test/java/com/izquierdojl/tolocharadio/`,
`app/src/androidTest/java/com/izquierdojl/tolocharadio/`. Sin dependencias ni módulos nuevos
(R1/R2 de research.md).

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar un punto de partida verde y que no hacen falta dependencias nuevas.

- [X] T001 Ejecutar los gates obligatorios en la rama `0041-jlizquierdo-20261002-stats-charts` para confirmar base verde: `.\gradlew.bat assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` (raíz del repo)
- [X] T002 [P] Confirmar que no se añaden dependencias ni módulos: revisar `gradle/libs.versions.toml` y `app/build.gradle.kts` sin editarlos (R1/R2 de `research.md`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Capa de datos del contrato de estadísticas y formato compartido; las usan las 3
historias.

**🔴 CRITICAL**: Ninguna historia puede empezar hasta completar esta fase.

### Tests for Foundational (Red-Green)

- [X] T003 Escribir el test (Red) de serialización de DTOs en `app/src/test/java/com/izquierdojl/tolocharadio/data/remote/dto/StatsDtosTest.kt` contra ejemplos de `contracts/stats-api.md`: envoltorios `{items}`, `{granularity, items}` de timeline, `countryCode` nullable (`StatsDtos.kt`, data-model.md)
- [X] T004 Escribir el test (Red) de `StatsRepo` en `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/StatsRepoTest.kt`: éxito por cada endpoint, 401 → `DomainError.Unauthorized`, 400 `INVALID_PARAMS` → error del contrato, fallo de red → `DomainError.Unavailable` vía `safeCall`

### Implementation for Foundational

- [X] T005 Crear los DTOs `@Serializable` en `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/dto/StatsDtos.kt` hasta poner T003 en verde: `StatsTopEntry{station, totalMs}`, `StatsTimelineEntry{bucket, totalMs}`, `StatsHabitEntry{weekday 0..6, hour 0..23, totalMs}`, `StatsGenreEntry{genre, totalMs}`, `StatsCountryEntry{country, countryCode?, totalMs}`, `StatsRecentEntry{station, startedAt, durationMs}` + envoltorios (data-model.md)
- [X] T006 [P] Crear `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/api/StatsApi.kt` con los 6 `GET` (`stats/me/top|timeline|habits|genres|countries|recent`), `Response<T>` estilo `HistoryApi`, `@Query from/to` (YYYY-MM-DD, `from <= to`), `limit` 1..50 (def. 10) en top/genres y 1..200 (def. 50) en recent, `granularity` day|week|month (def. day) — `contracts/stats-api.md`
- [X] T007 Implementar `StatsRepo` con KDoc en `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/StatsRepo.kt` (`@Singleton`, `safeCall`/`ApiResult`, un método suspend por endpoint) hasta poner T004 en verde
- [X] T008 [P] Añadir el provider `statsApi` en `app/src/main/java/com/izquierdojl/tolocharadio/di/NetworkModule.kt` (mismo estilo que los providers existentes de `*Api`)
- [X] T009 Escribir el test (Red) de `StatsFormat` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/stats/StatsFormatTest.kt`: `formatDurationMs` (0 min, 1 min, N min, 1 h, H h M min) y `formatBucketLabel`/`formatFullDay` en español por granularidad (R8)
- [X] T010 Crear `StatsFormat` puro con KDoc en `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/StatsFormat.kt` hasta poner T009 en verde (espejo de `apps/web/src/lib/stats.ts`)

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Ver el resumen y la evolución de mi escucha (Priority: P1) 🎯 MVP

**Goal**: Botón "Gráficas" en Configuración (junto a "Acerca de") que abre la vista con filtro
de periodo (7/30/90/todo, defecto 30 días), resumen (tiempo total, emisora destacada, día con
más escucha) y evolución temporal con granularidad automática; estados
`Loading/Empty/Content/Error` con reintento.

**Independent Test**: Con servidor activo e historial, abrir Configuración → "Gráficas" muestra
resumen y evolución del periodo 30 días; cambiar a 7 días recalcula todo (SC-004); sin datos →
estado vacío (US1-3); sin red → error con "Reintentar" (FR-007).

### Tests for User Story 1 (Red-Green)

- [X] T011 [P] [US1] Escribir el test (Red) de `ComputeStatsSummaryUseCase` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/stats/ComputeStatsSummaryUseCaseTest.kt`: `totalMs` = suma del timeline; emisora destacada = `items[0]` del top; `peakBucket` = bucket de mayor `totalMs`; total 0 → señal de estado vacío (R4, data-model.md)
- [X] T012 [P] [US1] Escribir el test (Red) de `StatsViewModel` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/stats/StatsViewModelTest.kt`: `Loading` inicial → `Content` (resumen+timeline); `Empty` con total 0; `Error` con `userMessage()` y `isAuthError` en 401; cambio de periodo recarga todo (SC-004) y calcula granularidad automática (≤62d día, ≤370d semana, resto mes — R5)
- [X] T013 [US1] Escribir el test (Red) de flujo de UI en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/stats/StatsEntryFlowTest.kt`: la fila "Gráficas" de Configuración abre la vista; el estado de error ofrece "Reintentar" y reintenta (FR-001/FR-007)

### Implementation for User Story 1

- [X] T014 [P] [US1] Crear `ComputeStatsSummaryUseCase` puro con KDoc en `app/src/main/java/com/izquierdojl/tolocharadio/domain/stats/ComputeStatsSummaryUseCase.kt` hasta poner T011 en verde
- [X] T015 [US1] Crear `LoadStatsUseCase` con KDoc en `app/src/main/java/com/izquierdojl/tolocharadio/domain/stats/LoadStatsUseCase.kt`: carga top+timeline del rango en paralelo (`coroutineScope` + `async`) y devuelve el bundle inicial (R3); se amplía en US2/US3
- [X] T016 [US1] Crear `StatsViewModel` con `StatsUiState` sellado `Loading/Empty/Content/Error` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/StatsViewModel.kt` (`@HiltViewModel`, patrón `HistoryViewModel`) hasta poner T012 en verde
- [X] T017 [US1] Crear la pantalla `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/StatsScreen.kt` con filtro de periodo (segmented buttons 7/30/90/todo), estados de carga/vacío/error con "Reintentar" y scroll vertical (FR-009)
- [X] T018 [P] [US1] Crear el bloque de resumen `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/blocks/StatsSummaryCards.kt` (tiempo total, emisora destacada, día con más escucha; formatos de `StatsFormat`)
- [X] T019 [P] [US1] Crear el bloque de evolución temporal `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/blocks/TimelineChart.kt` con `Canvas` (serie por bucket, ejes legibles, sin scroll horizontal — R1)
- [X] T020 [US1] Registrar `Routes.STATS = "stats"` y su `composable` en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/Routes.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`
- [X] T021 [US1] Añadir la fila "Gráficas" (junto a "Acerca de", con `contentDescription`) y su callback `onStats` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt` hasta poner T013 en verde

**Checkpoint**: User Story 1 fully functional and testable independently (MVP)

---

## Phase 4: User Story 2 - Explorar qué y de dónde escucho (Priority: P2)

**Goal**: Ranking de emisoras más escuchadas del periodo y reparto de escucha por género y por
país, legibles también con una única categoría.

**Independent Test**: Con historial de varias emisoras de distintos géneros y países, la vista
muestra el ranking ordenado por tiempo de escucha y los repartos por género/país; con una sola
categoría no se rompe el diseño (US2-3).

### Tests for User Story 2 (Red-Green)

- [X] T022 [P] [US2] Ampliar el test (Red) en `app/src/test/java/com/izquierdojl/tolocharadio/feature/stats/StatsViewModelTest.kt` para US2: el bundle incluye top/géneros/países; el cambio de periodo actualiza también estos bloques (SC-004); caso de categoría única

### Implementation for User Story 2

- [X] T023 [US2] Ampliar `app/src/main/java/com/izquierdojl/tolocharadio/domain/stats/LoadStatsUseCase.kt` con genres+countries (top ya cargado en US1) en paralelo (R3)
- [X] T024 [P] [US2] Crear `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/blocks/TopStationsChart.kt` (ranking por `totalMs` descendente, nombre y tiempo por emisora) con `Canvas` (R1)
- [X] T025 [P] [US2] Crear `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/blocks/GenreCountryCharts.kt` (reparto por género y por país, `countryCode` opcional, caso de categoría única) con `Canvas` (R1)
- [X] T026 [US2] Integrar los bloques de US2 en `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/StatsScreen.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/StatsViewModel.kt` hasta poner T022 en verde

**Checkpoint**: User Stories 1 AND 2 both work independently

---

## Phase 5: User Story 3 - Consultar hábitos y actividad reciente (Priority: P3)

**Goal**: Matriz de hábitos por día de la semana y hora con intensidad proporcional al tiempo
escuchado, y lista de reproducciones recientes (emisora, fecha, duración) de más reciente a más
antigua.

**Independent Test**: Con historial repartido en distintos días y franjas horarias, la vista
muestra la matriz 7×24 con intensidad proporcional y las escuchas recientes ordenadas por
`startedAt` descendente (US3-1/US3-2).

### Tests for User Story 3 (Red-Green)

- [X] T027 [P] [US3] Ampliar el test (Red) en `app/src/test/java/com/izquierdojl/tolocharadio/feature/stats/StatsViewModelTest.kt` para US3: el bundle incluye habits+recent; matriz 7×24 (`weekday` 0..6, `hour` 0..23) y orden de recientes descendente

### Implementation for User Story 3

- [X] T028 [US3] Ampliar `app/src/main/java/com/izquierdojl/tolocharadio/domain/stats/LoadStatsUseCase.kt` con habits+recent (bundle completo de R3)
- [X] T029 [P] [US3] Crear `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/blocks/HabitsHeatmap.kt` (matriz día×hora con intensidad proporcional, etiquetas de lunes a domingo en español) con `Canvas` (R1)
- [X] T030 [P] [US3] Crear `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/blocks/RecentList.kt` (emisora, fecha, duración; `startedAt` descendente) con `StatsFormat` (R8)
- [X] T031 [US3] Integrar los bloques de US3 en `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/StatsScreen.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/StatsViewModel.kt` hasta poner T027 en verde

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Mejoras transversales y verificación final.

- [X] T032 [P] Documentar con KDoc las APIs públicas de `app/src/main/java/com/izquierdojl/tolocharadio/domain/stats/` y `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/StatsRepo.kt` (constitución IV) y verificar que los logs no filtran PII (FR-007)
- [X] T033 Accesibilidad y legibilidad en `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/`: `semantics`/`contentDescription` de bloques y filtro, sin desplazamiento horizontal (FR-008/FR-009)
- [X] T034 Ejecutar los gates completos desde la raíz del repo: `.\gradlew.bat assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` y corregir los hallazgos (warnings nuevos justificados en la PR)
- [X] T035 Ejecutar la validación manual de `specs/0041-jlizquierdo-20261002-stats-charts/quickstart.md` (8 escenarios) y `.\gradlew.bat connectedDebugAndroidTest` con dispositivo/emulador — verificado manualmente en emulador por el usuario (2026-10-03)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias - puede empezar ya
- **Foundational (Phase 2)**: depende de Setup - BLOQUEA todas las historias
- **User Stories (Phase 3+)**: todas dependen de Foundational; después pueden avanzar en paralelo o en orden P1 → P2 → P3
- **Polish (Phase 6)**: depende de las historias deseadas completas

### User Story Dependencies

- **US1 (P1)**: empieza tras Foundational; sin dependencias de otras historias (incluye la entrada en Configuración)
- **US2 (P2)**: empieza tras Foundational; añade bloques a la vista creada en US1 (integración en T026), pero es testeable de forma independiente
- **US3 (P3)**: empieza tras Foundational; añade bloques a la vista (integración en T031), testeable de forma independiente

### Within Each User Story

- Los tests se escriben y DEBEN fallar antes de la implementación (Red-Green)
- Casos de uso antes que ViewModel; ViewModel antes que pantalla; bloques en paralelo
- Integración al final de cada historia

### Parallel Opportunities

- T002 (Setup) en paralelo con T001
- Foundational: T006 y T008 en paralelo con T005/T007
- US1: tests T011/T012 en paralelo; bloques T018/T019 en paralelo; T014 en paralelo con T015-T017
- US2: T022 en paralelo con T023; bloques T024/T025 en paralelo
- US3: T027 en paralelo con T028; bloques T029/T030 en paralelo
- T032 en paralelo con T033

---

## Parallel Example: User Story 1

```text
# Tests de US1 en paralelo (archivos distintos):
Task: "T011 [P] [US1] Test (Red) de ComputeStatsSummaryUseCaseTest.kt"
Task: "T012 [P] [US1] Test (Red) de StatsViewModelTest.kt"

# Bloques de US1 en paralelo (archivos distintos):
Task: "T018 [P] [US1] Bloque StatsSummaryCards.kt"
Task: "T019 [P] [US1] Bloque TimelineChart.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1: Setup (T001-T002)
2. Completar Phase 2: Foundational (T003-T010, CRÍTICA - bloquea las historias)
3. Completar Phase 3: US1 (T011-T021)
4. **STOP and VALIDATE**: probar US1 de forma independiente (quickstart escenarios 1, 2, 4, 5)
5. Entregar/demo si está listo

### Incremental Delivery

1. Setup + Foundational → base lista
2. US1 → validar (MVP: entrada + resumen + evolución)
3. US2 → validar (top, géneros, países)
4. US3 → validar (hábitos, recientes)
5. Polish → gates y validación completa

### Parallel Team Strategy

1. El equipo completa Setup + Foundational
2. Con Foundational listo: desarrollador A → US1, B → US2, C → US3
3. Las historias se integran y validan de forma independiente

---

## Notes

- [P] = archivos distintos, sin dependencias
- [Story] etiqueta la tarea con su historia para trazabilidad
- Verificar que los tests fallan antes de implementar (Red-Green)
- Commits solo si el usuario lo pide explícitamente
- Detenerse en cualquier checkpoint para validar la historia de forma independiente
- Evitar: tareas vagas, conflictos de archivo compartido y dependencias entre historias que rompan la independencia
