# Implementation Plan: Filtros avanzados de Explorar

**Branch**: `013-explore-filters` | **Date**: 2026-09-07 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/013-explore-filters/spec.md`

## Summary

Añadir filtros de búsqueda por país, idioma y género/etiqueta a la pantalla de Explorar de la app Android, alcanzando paridad con la app web. Los controles serán combobox con autocompletado que cargan listas del servidor. Se mantiene el botón "Cargar más" existente para paginación.

## Technical Context

**Language/Version**: Kotlin 1.9+, Android SDK 35 (minSdk 26)

**Primary Dependencies**: Jetpack Compose (Material3), Hilt, Retrofit2, Kotlin Serialization, Coroutines/Flow

**Storage**: Room (local cache for stations), DataStore (instance prefs)

**Testing**: JUnit5, Mockk, Turbine, Compose Test

**Target Platform**: Android 10+ (API 26+)

**Project Type**: Mobile app (Android)

**Performance Goals**: Filter lists load <2s, search results appear <1s

**Constraints**: Offline-capable (cache fallback), no auth required for catalog lists

**Scale/Scope**: 2 screens modified (ExploreScreen, ExploreViewModel), 1 new component (FilterComboBox)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

No constitution file found. Proceeding without governance constraints.

## Project Structure

### Documentation (this feature)

```text
specs/013-explore-filters/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── contracts/           # Phase 1 output (API contracts)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── feature/explore/
│   ├── ExploreScreen.kt        # Modified: add filter controls
│   ├── ExploreViewModel.kt     # Modified: add catalog list loading
│   └── FilterComboBox.kt       # New: reusable combobox component
├── data/repo/
│   └── StationsRepo.kt         # Existing: countries(), languages(), tags() already available
├── data/remote/api/
│   └── StationsApi.kt          # Existing: endpoints already defined
└── data/remote/dto/
    └── StationDtos.kt          # Existing: StringListDto already defined

app/src/test/java/com/izquierdojl/tolocharadio/
├── feature/explore/
│   └── ExploreViewModelsTest.kt  # Modified: add filter tests
└── data/repo/
    └── StationsRepoTest.kt       # Existing: may need filter tests
```

**Structure Decision**: Modificar componentes existentes en `feature/explore/` y crear un nuevo componente reutilizable `FilterComboBox.kt`. No se necesitan cambios en capa de datos (APIs y repos ya soportan filtros).
