# Implementation Plan: Alternador de Vista Lista/Tarjetas en la Barra Superior

**Branch**: `008-view-mode-toggle` | **Date**: 2026-09-06 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/008-view-mode-toggle/spec.md`

## Summary

Añadir un alternador global de vista (lista ↔ tarjetas) en la barra superior compartida (`TolochaNavGraph` → `TopAppBar.actions`), visible solo en las secciones con listas de emisoras (Explorar, Favoritos, Historial, Mis emisoras). El modo es una preferencia local persistente (DataStore vía `InstancePrefs`, clave `view_mode`, default `LIST`, parseo tolerante con `runCatching`), expuesta por un único `ViewModeViewModel` a ámbito de Activity. Explore ya tiene toggle local en memoria (`ExploreViewModel.gridMode`, icono en el `trailingIcon` del buscador): se elimina y se unifica con la preferencia global.

## Technical Context

**Language/Version**: Kotlin (JVM target por AGP del proyecto), Jetpack Compose + Material 3

**Primary Dependencies**: Compose M3, Hilt, DataStore Preferences, kotlinx.coroutines Flow (ya aprobadas; sin dependencias nuevas)

**Storage**: DataStore Preferences (clave nueva `view_mode` en `InstancePrefs`); sin cambios en Room ni en el backend

**Testing**: JUnit + kotlinx-coroutines-test + Turbine (ViewModel), Compose UI Test (flujos críticos), MockK

**Target Platform**: Android, `minSdk 26`, `targetSdk` estable del proyecto

**Project Type**: mobile-app (módulo único `app`, features espejo de la web)

**Performance Goals**: re-presentación de la vista en < 1 s en gama media (SC-001); el toggle no dispara red ni recarga (`FR-003`)

**Constraints**: sin recarga de datos al alternar (estado del ViewModel intacto), preferencia global simultánea en las 4 secciones (`FR-004`), icono = modo destino (`FR-002`), accesibilidad con `contentDescription` dinámico

**Scale/Scope**: 1 control compartido + 4 pantallas afectadas + 1 preferencia nueva; ~6-8 tareas de implementación

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Veredicto | Notas |
|---|---|---|
| I. MVVM + Clean por capas | PASS | `ViewMode` (dominio/core) → `InstancePrefs` (data, Flow) → `ViewModeViewModel` (UI, `StateFlow`) → Composables sin lógica. Sin `Context` de Activity en VM. |
| II. Kotlin-First, Compose M3, Media3 | PASS | UI 100% Compose M3 (`TopAppBar` action + `IconButton`). Sin XML. Sin dependencias nuevas (Principio V). |
| III. Calidad Test-First | PASS | Tests obligatorios: `InstancePrefs` (default/corrupto/persistencia), `ViewModeViewModel` (toggle/global), tests Compose del alternador en Explorar (flujo crítico). |
| IV. Streaming Robusto y Errores | PASS | No afecta al player ni a red; estado de error/vacío se conserva al alternar (FR-007, edge cases de la spec). |
| V. Simplicidad Modular (YAGNI) | PASS | Se elimina el toggle local duplicado de Explore (código muerto). Una sola fuente de verdad para el modo. Sin multi-módulo. |

Gates CI: Gradle build OK, tests unitarios OK, Detekt/ktlint/Android Lint sin errores.

## Project Structure

### Documentation (this feature)

```text
specs/008-view-mode-toggle/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── ui-contract.md
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── core/ui/navigation/
│   └── TolochaNavGraph.kt        # TopAppBar compartido: action del alternador condicionado a rutas con lista
├── core/ui/components/
│   └── ViewModeToggle.kt         # (nuevo) IconButton del alternador (icono modo destino + contentDescription dinámico)
├── data/local/
│   └── InstancePrefs.kt          # + Flow<ViewMode> viewMode + setViewMode() (clave "view_mode", parseo runCatching)
└── feature/
    ├── explore/ExploreScreen.kt   # quita trailingIcon toggle; render según modo global
    ├── explore/ExploreViewModel.kt# elimina gridMode/toggleGrid (código muerto tras unificar)
    ├── favorites/FavoritesScreen.kt# + rama grid (StationCard)
    ├── history/HistoryScreen.kt   # + rama grid (StationCard)
    └── customstations/CustomStationsScreen.kt # + rama grid (StationCard)
app/src/main/java/com/izquierdojl/tolocharadio/feature/ (ViewModeViewModel.kt — único, ámbito Activity)
app/src/test/java/com/izquierdojl/tolocharadio/
├── data/local/InstancePrefsTest.kt
└── feature/ViewModeViewModelTest.kt
app/src/androidTest/... (Compose test del alternador en Explorar)
```

**Structure Decision**: Módulo único `app` (constitución I: modularización pragmática). El estado del modo vive en `InstancePrefs` (data) y se expone por un `ViewModeViewModel` único a ámbito de Activity (mismo patrón que `PlayerViewModel`, spec 004), para que las 4 secciones y la TopAppBar compartida observen la misma `StateFlow` sin duplicar colección.

## Complexity Tracking

Sin violaciones constitucionales que justificar.
