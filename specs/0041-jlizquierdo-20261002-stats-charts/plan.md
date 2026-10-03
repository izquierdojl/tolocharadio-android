# Implementation Plan: Gráficas de escucha (estadísticas) en la app Android

**Branch**: `0041-jlizquierdo-20261002-stats-charts` | **Date**: 2026-10-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0041-jlizquierdo-20261002-stats-charts/spec.md`

## Summary

Nueva pantalla "Gráficas" accesible por un botón en Configuración (junto a "Acerca de") que muestra los agregados de escucha del usuario del servidor activo. Técnica: `StatsApi` (6 endpoints `/api/v1/stats/me/**`, Bearer automático) + `StatsRepo` con `safeCall`/`ApiResult`, caso de uso de carga del conjunto de bloques y caso de uso puro para el resumen derivado; UI Compose M3 con bloques de gráfica dibujados con `Canvas` (sin dependencias nuevas, decisión R1 de `research.md`), filtro de periodo 7/30/90/todo con recarga global, estados `Loading/Empty/Content/Error` con reintento. Sin persistencia local ni cambios de backend.

## Technical Context

**Language/Version**: Kotlin 2.x, JVM target 17, Compose (BOM del proyecto)

**Primary Dependencies**: Jetpack Compose + Material 3, Hilt, Retrofit + OkHttp + kotlinx.serialization, Coroutines + Flow (todas ya en el proyecto; **ninguna dependencia nueva** — gráficas con `Canvas`)

**Storage**: N/A (solo lectura de red; sin Room ni DataStore para esta feature — ver R2 de `research.md`)

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test + Turbine (unit), compose-ui-test-junit4 (1 flujo crítico de UI)

**Target Platform**: Android (minSdk 26, targetSdk/compileSdk 37), módulo único `app`

**Project Type**: mobile-app (MVVM + Clean: `ui → domain → data`)

**Performance Goals**: primeros indicadores visibles < 2 s (SC-002); scroll fluido con > 1 año de historial (SC-005); cambio de periodo < 2 s (US1-2)

**Constraints**: HTTPS-only; `Authorization: Bearer` solo por `AuthInterceptor`/`TokenAuthenticator` existentes; sin PII ni credenciales en logs; mensajes de error en español vía `DomainError.userMessage()`; textos de UI en español

**Scale/Scope**: 1 pantalla con 7 bloques, 1 API interface + 1 DTO file + 1 repo + 2 casos de uso + 1 ViewModel; 6 endpoints de solo lectura; ~4–6 tests nuevos

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio / Gate | Veredicto | Evidencia |
|---|---|---|
| I. MVVM + Clean por capas | PASS | `feature/stats` (UI+VM) → `domain/stats` (casos de uso) → `data` (`StatsApi`/`StatsRepo`); VM sin `Context`; Hilt para VM/repo/API |
| II. Kotlin + Compose M3 + Media3 | PASS | Pantalla nueva en Compose M3 (prohibido XML); sin reproductor (no toca Media3); Retrofit + kotlinx.serialization contra `/api/v1`; auth por servidor ya implementada (Bearer) |
| III. Calidad Test-First | PASS | Tests obligatorios de `data`/`domain`/`ViewModel` (Red-Green); 1 Compose test del flujo crítico entrada-Configuración → vista/estado de error; CI ejecuta `testDebugUnitTest`, `detekt`, `ktlintCheck`, `lintDebug` |
| IV. Streaming Robusto y Errores | PASS | `UiState` sellado con `Loading/Empty/Content/Error` + reintento; errores `{error:{code,...}}` mapeados por `safeCall`/`DomainError`; 401 → `isAuthError` → edición del servidor activo; logs sin PII |
| V. Simplicidad Modular (YAGNI) | PASS | Sin módulos Gradle nuevos, sin dependencias nuevas (Canvas propio), sin caché offline (R2), sin captura local de escuchas (FR-006) |
| Gates de merge | PASS | `assembleDebug`, `testDebugUnitTest`, `detekt ktlintCheck lintDebug` en CI; PR pequeña enlazada a la spec |

**Re-check post-design (Phase 1)**: PASS — el diseño no introduce dependencias, módulos ni persistencia nuevas; mantiene los mismos gates.

## Project Structure

### Documentation (this feature)

```text
specs/0041-jlizquierdo-20261002-stats-charts/
├── plan.md              # Este archivo (/speckit.plan)
├── research.md          # Phase 0 (/speckit.plan)
├── data-model.md        # Phase 1 (/speckit.plan)
├── quickstart.md        # Phase 1 (/speckit.plan)
├── contracts/           # Phase 1 (/speckit.plan)
│   └── stats-api.md
├── checklists/
│   └── requirements.md
├── spec.md
└── tasks.md             # Phase 2 (/speckit.tasks) — aún no creado
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── core/ui/navigation/
│   ├── Routes.kt                    # + const STATS = "stats"
│   └── TolochaNavGraph.kt           # + composable(Routes.STATS) { StatsScreen(...) }
├── data/
│   ├── remote/api/StatsApi.kt       # NUEVO: 6 endpoints /stats/me/**
│   ├── remote/dto/StatsDtos.kt      # NUEVO: DTOs de respuesta
│   └── repo/StatsRepo.kt            # NUEVO: safeCall + ApiResult
├── di/NetworkModule.kt              # + provider de StatsApi
├── domain/stats/
│   ├── LoadStatsUseCase.kt          # NUEVO: carga el conjunto de bloques
│   └── ComputeStatsSummaryUseCase.kt# NUEVO: puro (resumen derivado)
└── feature/
    ├── settings/SettingsScreen.kt   # + fila "Gráficas" junto a "Acerca de"
    └── stats/
        ├── StatsScreen.kt           # NUEVO: pantalla + previews
        ├── StatsViewModel.kt        # NUEVO: UiState sellado
        ├── StatsFormat.kt           # NUEVO: formatos de tiempo/etiquetas (puro)
        └── blocks/                  # NUEVO: composables de bloques con Canvas
            ├── StatsSummaryCards.kt
            ├── TimelineChart.kt
            ├── TopStationsChart.kt
            ├── HabitsHeatmap.kt
            ├── GenreCountryCharts.kt
            └── RecentList.kt

app/src/test/java/com/izquierdojl/tolocharadio/
├── data/repo/StatsRepoTest.kt
├── domain/stats/ComputeStatsSummaryUseCaseTest.kt
├── domain/stats/StatsFormatTest.kt
└── feature/stats/StatsViewModelTest.kt

app/src/androidTest/java/com/izquierdojl/tolocharadio/
└── feature/stats/StatsEntryFlowTest.kt   # flujo crítico: Configuración → Gráficas → error/reintento
```

**Structure Decision**: módulo único `app` organizado por feature espejo de la web (principio I), con `feature/stats/` nueva y la capa `data`/`domain` siguiendo exactamente los patrones de `HistoryApi`/`HistoryRepo`/`ObserveHistoryUseCase`. Sin módulos Gradle nuevos ni reorganizaciones.

## Complexity Tracking

Sin violaciones que justificar: el Constitution Check está en verde antes y después del diseño.
