# Implementation Plan: Favoritos — lista, marcado y navegación

**Branch**: `003-favorites-management` | **Date**: 2026-09-05 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/003-favorites-management/spec.md` (con 5 clarificaciones 2026-09-05: drag & drop con asa + autoguardado, deshacer 10 s, caché offline obligatoria, fecha relativa, conflicto de orden gana-servidor).

## Summary

Completar el pendiente FR-009 de la spec 001: pantalla Favoritos real (hoy placeholder a `HomeScreen` en `TolochaNavGraph.kt:155`), lista completa del servidor en orden personalizado, marcado coherente de favoritas en Explorar/ficha/Favoritos, toggle optimista con reversión + deshacer 10 s, reorden drag & drop con `PUT /favorites/order` (permutación exacta) y navegación con paridad web (`Routes.FAVORITES`, exige sesión). Enfoque: extender `FavoritesApi`/`FavoritesRepo` existentes (añadir `list` consumida + `PUT order`), nuevo `ObserveFavoritesUseCase`/`ReorderFavoritesUseCase`, caché Room de solo lectura (`CachedFavorite`), `FavoritesViewModel` + `FavoritesScreen`, y reutilizar player/mini-player sin cambios.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JVM 17, AGP 9.3.2 (ver `gradle/libs.versions.toml`, `app/build.gradle.kts`)

**Primary Dependencies**: Compose BOM 2025.01.00 + Material3, Navigation Compose 2.7.7, Hilt 2.60.1, Retrofit 2.11.0 + OkHttp 4.12.0 + kotlinx.serialization 1.7.3, Media3 1.4.1 (reutilizado, sin cambios), Coil 2.6.0, Coroutines 1.8.1 + Flow

**Storage**: Room 2.7.2 (nueva tabla `favorites_cache`, caché solo-lectura) + DataStore/Security-Crypto existentes (tokens; sin cambios)

**Testing**: JUnit4 + kotlinx-coroutines-test + Turbine 1.1.0, MockK 1.13.12, Compose `ui-test-junit4`, Android Lint + ktlint + Detekt (gates CI)

**Target Platform**: Android `minSdk=26`, `targetSdk=37`, `compileSdk=37`

**Project Type**: mobile-app, un solo módulo `app` por feature (`feature/favorites/`, espejo web)

**Performance Goals**: abrir Favoritos < 2 s con red normal (SC-001); reversión tras error < 2 s (SC-006); guardado de orden no bloquea navegación (progreso en segundo plano)

**Constraints**: HTTPS-only; Bearer en memoria, nunca en URL/logs; refresh 401 con reintento único (infra existente `TokenAuthenticator`); `PUT /favorites/order` exige permutación exacta de ids; caché offline solo-lectura, verdad = servidor; deshacer 10 s; fecha relativa; YAGNI: sin módulos nuevos ni dependencias nuevas

**Scale/Scope**: 1 pantalla nueva + marcado compartido; listas de decenas (carga completa, sin paginación); 4 historias (US-1/US-2 P1, US-3/US-4 P2)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **I. MVVM + Clean por capas**: UI (`feature/favorites/`: `FavoritesScreen`, `FavoritesViewModel` con `StateFlow/UiState`, sin `Context`) → domain (`ObserveFavoritesUseCase`, `ToggleFavoriteUseCase` existente, `ReorderFavoritesUseCase` puros Kotlin) → data (`FavoritesRepo` + `FavoritesApi` Retrofit + `FavoritesCacheDao` Room). Hilt para VM/repos. Sin módulo Gradle nuevo (un feature, API aún no estable).
- [x] **II. Stack Kotlin-First/Compose M3/Media3**: solo Kotlin; UI nueva 100 % Compose M3 (cero XML); player Media3 reutilizado tal cual (mini-player persistente ya existe); red contra `/api/v1` con DTOs existentes (`FavoriteDto`, `FavoriteListDto`); `minSdk=26` intacto; Coil para favicon; corrutinas + Flow (prohibidos `LiveData`/`GlobalScope`).
- [x] **III. Test-First**: nuevos UseCases/Repo/VM con tests JUnit + Turbine (incl. serialización de `ReorderBody`); Compose Test para ver-lista, toggle + deshacer, drag & drop, error con reintento; Lint/ktlint/Detekt sin errores antes de merge.
- [x] **IV. Streaming robusto/errores**: `FavoritesUiState` sellado (`Loading/Content/Empty/Error`); mapeo `{error:{code,message,status}}` → mensajes ES (401→renovar/login, 404→no disponible, 503→reintento/caché); sin PII/tokens en logs; `Station.url` nunca directa (se reusa ficha/player existentes con precheck `playback/:id/status`).
- [x] **V. Simplicidad (YAGNI)**: sin dependencias nuevas (drag & drop con Compose Foundation incluido en BOM, `Snackbar` M3, `DateUtils`/java.time para fecha relativa); sin caché de audio ni DRM; sin multi-módulo.

Post-diseño (Phase 1): sin violaciones nuevas — la tabla Room añadida es caché de lectura ya prevista por la constitución; la migración Room llevará test (gobernanza de versionado). Sin entrada en Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/003-favorites-management/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── favorites-api.md # Endpoints consumidos (contrato servidor, solo lectura para el plan)
│   └── favorites-ui.md  # Contrato UI: estados, acciones, navegación
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/java/com/example/tolocharadio/
├── core/ui/navigation/
│   ├── Routes.kt                  # sin cambios (FAVORITES ya existe)
│   └── TolochaNavGraph.kt         # CAMBIO: destino FAVORITES → FavoritesScreen real
├── core/network/                  # sin cambios (ApiResult/safeCall/userMessage/refresh)
├── data/remote/api/
│   └── FavoritesApi.kt            # CAMBIO: + PUT favorites/order (ReorderBody)
├── data/remote/dto/
│   └── StationDtos.kt             # CAMBIO menor: + ReorderBody(client-only) si se ubica aquí
├── data/local/
│   ├── TolochaDb.kt               # CAMBIO: + CachedFavorite (version 2 + migración)
│   └── FavoritesCache.kt          # NUEVO: entidad CachedFavorite + FavoritesCacheDao
├── data/repo/
│   └── FavoritesRepo.kt           # CAMBIO: + observe()/list()/reorder(), comparte favoriteIds
├── domain/
│   ├── ToggleFavoriteUseCase.kt   # sin cambios (ya optimista; reuso)
│   ├── ObserveFavoritesUseCase.kt # NUEVO: Flow<List<Favorite>> (red + caché fallback)
│   └── ReorderFavoritesUseCase.kt # NUEVO: valida permutación exacta, delega PUT
└── feature/favorites/
    ├── FavoritesScreen.kt         # NUEVO: lista, EmptyState, Snackbar 10 s, drag & drop
    ├── FavoritesViewModel.kt      # NUEVO: UiState sellado, undo, reorder, reintento
    └── RelativeTime.kt            # NUEVO: formato "hace X" (o helper en core; decidir en tasks)

app/src/test/java/com/example/tolocharadio/
├── data/repo/FavoritesRepoTest.kt       # NUEVO (list/reorder, mapeo errores)
├── data/remote/dto/DtoSerializationTest.kt # AMPLIAR (ReorderBody + FavoriteListDto order)
├── data/local/FavoritesCacheTest.kt     # NUEVO (dao + migración v1→v2)
├── domain/ObserveReorderUseCasesTest.kt # NUEVO
└── feature/favorites/FavoritesViewModelTest.kt # NUEVO (Turbine: carga, toggle+rollback, undo 10 s, reorder+gana-servidor)

app/src/androidTest/java/com/example/tolocharadio/
└── feature/favorites/FavoritesScreenTest.kt # NUEVO (composeTestRule: lista, toggle, deshacer, error+reintento, drag)
```

**Structure Decision**: monomódulo `app` por features espejo web (constitución I/V). Sin módulos nuevos. Tests unitarios junto a su capa (`src/test`), UI críticos en `androidTest`.

## Complexity Tracking

> Sin violaciones constitucionales que justificar — tabla vacía intencionadamente.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |
