# Implementation Plan: Historial — lista, reproducción y gestión

**Branch**: `005-history-management` | **Date**: 2026-09-06 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/005-history-management/spec.md`

## Summary

Implementar la pantalla de Historial con paridad a la web (`/historial`): lista de emisoras reproducidas ordenadas por más reciente, deduplicación en cliente, reproducción directa con actualización automática, eliminación individual y limpiar todo con diálogo de confirmación. Vista de lista única (sin toggle de tarjetas — pendiente en spec global de UX). Caché offline con Room. Sigue el patrón de Favorites (spec 003) como referencia.

## Technical Context

**Language/Version**: Kotlin, `compileSdk` declarado en Gradle, `minSdk = 26`

**Primary Dependencies**: Jetpack Compose + Material3, Hilt, Retrofit + OkHttp + kotlinx.serialization, Room, Coroutines + Flow, Coil, Media3 ExoPlayer

**Storage**: Room (`TolochaDb`) — entidad `CachedHistoryEntry` + DAO para caché offline de lectura. DataStore ya existe para ajustes.

**Testing**: JUnit + kotlinx-coroutines-test + Turbine, MockK, Compose Test (composeTestRule). Tests de serialización DTO contra ejemplos OpenAPI.

**Target Platform**: Android (API 26+), dispositivo/emulador

**Project Type**: Mobile app (Android, single `app` module)

**Performance Goals**: Historial visible en < 2 s con red normal (SC-001). Reversión de estado en < 2 s ante error (SC-006).

**Constraints**: HTTPS-only, Bearer en memoria, mensajes de error en español, historial completo sin paginación (decenas a cientos de entradas).

**Scale/Scope**: 1 nueva pantalla (History),1 ViewModel, 1 API interface, 1 Repo, 1 Room entity + DAO, 1 use case opcional. Actualización de nav graph y DB (migración 2→3).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Justification |
|-----------|--------|---------------|
| I. MVVM + Clean por capas | ✅ PASS | UI (HistoryScreen) → domain (ObserveHistoryUseCase) → data (HistoryRepo + HistoryApi + HistoryCache). ViewModel sin referencias a View ni Context. |
| II. Kotlin-First, Compose M3, Media3 | ✅ PASS | UI en Compose + M3. Reproducción reutiliza Media3 existente (PlayerViewModel). Red con Retrofit + kotlinx.serialization. Room para caché. |
| III. Test-First | ✅ PASS | Tests unitarios para ViewModel, UseCase, Repo, DTO serialization. Compose Test para flujo crítico (historial + play + eliminar). |
| IV. Streaming Robusto | ✅ PASS | Errores del backend mapeados a mensajes en español. Reversión optimista ante fallo. Caché offline como fallback. Estado de error con reintento. |
| V. Simplicidad Modular (YAGNI) | ✅ PASS | Sin multi-módulo nuevo. Reutiliza componentes existentes (EmptyState, ErrorBanner, StationArtwork, FavoriteButton, RelativeTime). Sin abstracciones nuevas sin dos consumidores. |

**Violations**: Ninguna.

## Project Structure

### Documentation (this feature)

```text
specs/005-history-management/
├── spec.md              # Feature specification
├── plan.md              # This file (/speckit.plan output)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── HistoryApi.kt    # Retrofit interface contract
├── checklists/
│   └── requirements.md  # Quality checklist
└── tasks.md             # Phase 2 output (/speckit.tasks - NOT created here)
```

### Source Code (repository root)

```text
app/src/main/java/com/example/tolocharadio/
├── data/
│   ├── local/
│   │   ├── HistoryCache.kt          # NEW: CachedHistoryEntry entity + HistoryCacheDao
│   │   └── TolochaDb.kt             # MODIFIED: version 3, add entity + DAO
│   ├── remote/
│   │   ├── api/
│   │   │   └── HistoryApi.kt        # NEW: Retrofit interface
│   │   └── dto/
│   │       └── StationDtos.kt       # MODIFIED: add HistoryListDto
│   └── repo/
│       └── HistoryRepo.kt           # NEW: repository
├── di/
│   └── NetworkModule.kt             # MODIFIED: bind HistoryApi
├── domain/
│   └── ObserveHistoryUseCase.kt     # NEW: use case
└── feature/
    └── history/
        ├── HistoryScreen.kt          # NEW: Compose screen
        └── HistoryViewModel.kt       # NEW: ViewModel

# Modified files:
app/src/main/java/com/example/tolocharadio/core/ui/navigation/TolochaNavGraph.kt
# Lines 183-185: replace HomeScreen placeholder with HistoryScreen
```

**Structure Decision**: Single `app` module, organized by feature (`feature/history/`) mirroring the web structure. Follows the existing pattern established by `feature/favorites/`.
