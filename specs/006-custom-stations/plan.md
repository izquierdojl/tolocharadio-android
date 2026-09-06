# Implementation Plan: Emisoras personalizadas — Mis emisoras

**Branch**: `006-custom-stations` | **Date**: 2026-09-06 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/006-custom-stations/spec.md`

## Summary

Implementar la sección Mis emisoras con paridad a la web (`/mis-emisoras`): lista de emisoras creadas por el usuario, formulario superior Nombre + URL del stream con validación en cliente, reproducción vía proxy con mini-player persistente, borrado individual con invalidación cruzada de Favoritos y caché offline con Room. La barra inferior y la guardia de sesión ya existen (`Routes.CUSTOM_STATIONS` en `AUTH_REQUIRED`); el destino muestra un placeholder (`HomeScreen`) que se sustituye por la pantalla real. Sigue el patrón de History (spec 005) como referencia.

## Technical Context

**Language/Version**: Kotlin, `compileSdk` declarado en Gradle, `minSdk = 26`

**Primary Dependencies**: Jetpack Compose + Material3, Hilt, Retrofit + OkHttp + kotlinx.serialization, Room, Coroutines + Flow, Coil (solo emblema local para personalizadas), Media3 ExoPlayer (existente)

**Storage**: Room (`TolochaDb`, versión actual 3) — nueva entidad `CachedCustomStation` + DAO para caché offline de lectura. DataStore ya existe para ajustes.

**Testing**: JUnit + kotlinx-coroutines-test + Turbine, MockK, Compose Test (composeTestRule). Tests de serialización DTO contra ejemplos OpenAPI.

**Target Platform**: Android (API 26+), dispositivo/emulador

**Project Type**: Mobile app (Android, single `app` module)

**Performance Goals**: Mis emisoras visible en < 2 s con red normal (SC-001). Reflejo de añadir/eliminar en < 2 s (SC-005).

**Constraints**: HTTPS-only para el backend; URLs de stream `http:`/`https:` (cualquiera, no solo locales — un stream http público es válido aunque `NormalizeBaseUrlUseCase` lo rechazaría como base de instancia). Bearer en memoria, mensajes de error en español, lista completa sin paginación (`{items}`).

**Scale/Scope**: 1 nueva pantalla (CustomStations), 1 ViewModel, 1 API interface, 1 Repo, 1 Room entity + DAO, 1 use case de validación (puro). Modificación de nav graph (sustituir placeholder) y DB (migración 3→4).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Justification |
|-----------|--------|---------------|
| I. MVVM + Clean por capas | ✅ PASS | UI (CustomStationsScreen) → domain (ValidateCustomStationUseCase + ObserveCustomStationsUseCase opcional) → data (CustomStationsRepo + CustomStationsApi + CustomStationsCache). ViewModel sin referencias a View ni Context. |
| II. Kotlin-First, Compose M3, Media3 | ✅ PASS | UI en Compose + M3. Reproducción reutiliza `PlayerViewModel.play()` existente (proxy Bearer vía Media3). Red con Retrofit + kotlinx.serialization. Room para caché. Sin pantallas XML. |
| III. Test-First | ✅ PASS | Tests unitarios para ViewModel, UseCase de validación, Repo, DTO serialization. Compose Test para flujos críticos (añadir con validación, reproducir, eliminar). Red-Green obligatorio. |
| IV. Streaming Robusto | ✅ PASS | Errores del backend mapeados a mensajes en español (incluido 422 con `details` por campo). Actualización optimista con reversión ante fallo. Caché offline como fallback. Estado de error con reintento. |
| V. Simplicidad Modular (YAGNI) | ✅ PASS | Sin multi-módulo nuevo. Reutiliza `StationListDto`, `OkResult`, `PlayerViewModel`, componentes (EmptyState, StationArtwork/emblema). Sin edición de personalizadas (la web no la tiene). Sin abstracción de formulario genérico (un solo consumidor). |

**Violations**: Ninguna.

*Re-check post-Phase 1 (2026-09-06)*: el diseño confirma los 5 principios — sin nuevas dependencias, sin módulos nuevos, reutilización máxima (DTO wrapper, player, caché). Sin violaciones.

## Project Structure

### Documentation (this feature)

```text
specs/006-custom-stations/
├── spec.md              # Feature specification
├── plan.md              # This file (/speckit.plan output)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── CustomStationsApi.kt  # Retrofit interface contract
├── checklists/
│   └── requirements.md  # Quality checklist
└── tasks.md             # Phase 2 output (/speckit.tasks - NOT created here)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── data/
│   ├── local/
│   │   ├── CustomStationsCache.kt     # NEW: CachedCustomStation entity + DAO + MIGRATION_3_4
│   │   └── TolochaDb.kt              # MODIFIED: version 4, add entity + DAO
│   ├── remote/
│   │   ├── api/
│   │   │   └── CustomStationsApi.kt  # NEW: Retrofit interface (ver contracts/)
│   │   └── dto/
│   │       └── StationDtos.kt        # MODIFIED: add CustomStationResultDto + CreateCustomStationBody
│   └── repo/
│       └── CustomStationsRepo.kt     # NEW: repository (Flow + caché + invalidación de favoritos)
├── di/
│   └── NetworkModule.kt              # MODIFIED: bind CustomStationsApi
├── domain/
│   └── ValidateCustomStationUseCase.kt  # NEW: validación pura nombre + URL (ver research D1)
└── feature/
    └── customstations/
        ├── CustomStationsScreen.kt     # NEW: Compose screen (lista + formulario)
        └── CustomStationsViewModel.kt  # NEW: ViewModel (estado lista + estado formulario)

# Modified files:
app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt
# Lines 195-197: replace HomeScreen placeholder with CustomStationsScreen + auth guard
# (patrón idéntico al bloque HISTORY, lines 184-194)
```

**Structure Decision**: Single `app` module, organizado por feature (`feature/customstations/`) espejo de la web. Sigue el patrón establecido por `feature/favorites/` y `feature/history/`. La entrada de la barra inferior y `AUTH_REQUIRED` ya incluyen `CUSTOM_STATIONS` — solo falta el contenido del destino.

