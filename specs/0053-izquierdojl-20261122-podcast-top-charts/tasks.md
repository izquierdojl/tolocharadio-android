---
description: "Task list for feature 0053 — Tops de podcasts por país y género en Descubrir"
---

# Tasks: Tops de podcasts por país y género en Descubrir

**Input**: Design documents from `/specs/0053-izquierdojl-20261122-podcast-top-charts/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: SÍ. La constitución (Principio III, Test-First NON-NEGOTIABLE) exige tests para `domain`, `data` y `ViewModel`, y Compose UI para flujos críticos. Regla Red-Green: el test se escribe antes y debe fallar antes de la implementación.

**Organization**: Tareas agrupadas por historia de usuario. US1 es el MVP; US2 y US3 se construyen sobre la misma pantalla y los mismos archivos, por lo que se secuencian.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (archivo distinto, sin dependencias pendientes)
- **[Story]**: US1/US2/US3; Setup, Foundational y Polish no llevan etiqueta
- Todas las tareas incluyen rutas de archivo exactas
- Los campos restringidos citan `data-model.md` con la forma `Entity.field`; no se duplican sus reglas

## Path Conventions

Módulo único Android:

- Código: `app/src/main/java/com/izquierdojl/tolocharadio/`
- Tests JVM: `app/src/test/java/com/izquierdojl/tolocharadio/`
- Tests instrumentados: `app/src/androidTest/java/com/izquierdojl/tolocharadio/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar base verde y preparar utilidades compartidas de test. El proyecto y el módulo ya existen; no hay inicialización de proyecto.

- [X] T001 Ejecutar los gates base en la rama para confirmar punto de partida verde: `./gradlew testDebugUnitTest detekt ktlintCheck lintDebug` (raíz del repo; en Windows `.\gradlew.bat`). Si falla algo preexistente, documentarlo antes de tocar código.
- [X] T002 [P] Añadir a `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/TolochaTestTags.kt` los tags `TOP_CHARTS_SECTION`, `TOP_CHARTS_COUNTRY`, `TOP_CHARTS_GENRE`, `TOP_CHARTS_LIST`, `TOP_CHARTS_STATUS`, `TOP_CHARTS_RETRY`, `TOP_CHARTS_AUTH_ACTION`, `TOP_CHARTS_NOTICE` y `TOP_CHARTS_EMPTY` según `contracts/top-charts-ui.md` → «Test tags».
- [X] T003 [P] Añadir en `app/src/test/java/com/izquierdojl/tolocharadio/support/PodcastFixtures.kt` un helper `testPodcastSearchResult(...)` que construya `PodcastSearchResult.title`, `PodcastSearchResult.author`, `PodcastSearchResult.feedUrl`, `PodcastSearchResult.imageUrl`, `PodcastSearchResult.categories` y `PodcastSearchResult.description` según `data-model.md`, para reutilizarlo en los tests de ranking.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Capacidad del servidor, contrato de red, catálogo de dominio, fallback y preferencias. Es la base que necesitan las tres historias.

**⚠️ CRITICAL**: Ninguna historia puede empezar hasta que esta fase esté completa y T014 en verde.

### Tests (Red)

- [X] T004 [P] Escribir en `app/src/test/java/com/izquierdojl/tolocharadio/data/remote/dto/PodcastCapabilitiesDtoTest.kt` y `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/SystemRepoTest.kt` los tests (Red) de `PodcastFeaturesDto.podcastDiscovery` ausente/null, `none`, `podcastindex` e `itunes`, de `AppConfigDto.podcastTopChartsEnabled` y de `PodcastCapabilities.topCharts` según `data-model.md` → `PodcastFeaturesDto.podcastDiscovery`, `AppConfigDto.podcastTopChartsEnabled`, `PodcastCapabilities.topCharts`; incluir caché de capacidad (segunda llamada sin red), `forceRefresh` y que un error no se cachea.
- [X] T005 [P] Escribir en `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/TopChartsTest.kt` los tests (Red) de `TopCountry.id`, `TopCountry.label`, `TopGenre.id`, `TopGenre.label`, `TopChartsLoad.items` y `TopChartsLoad.genreUnavailable`: valores únicos, defaults, `fromId` tolerante y fallback a `SPAIN`/`ALL`, según `data-model.md`.
- [X] T006 [P] Escribir en `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/LoadTopChartsUseCaseTest.kt` los tests (Red) de ranking general, categoría correcta, fallback solo para errores recuperables (`Unavailable`/`Unknown`), ausencia de fallback para `Unauthorized`/`NotFound`, aviso con reintento de categoría y error cuando ambos fallan, conservando orden y citando `TopChartsLoad.items` y `TopChartsLoad.genreUnavailable` de `data-model.md`.
- [X] T007 [P] Escribir en `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/PodcastSubscriptionsRepoTest.kt` los tests (Red) de `top(country, genre, limit)`: parámetros enviados, `limit` 50, mapeo de `PodcastSearchPage.items` y de `PodcastSearchResult.title/author/feedUrl/imageUrl/categories/description`, y propagación de errores, según `data-model.md`.
- [X] T008 [P] Escribir en `app/src/test/java/com/izquierdojl/tolocharadio/data/local/InstancePrefsTest.kt` los tests (Red) de `InstancePrefs.topChartsCountry`, `InstancePrefs.topChartsGenre`, las claves `top_charts_country`/`top_charts_genre`, defaults, round-trip y valor corrupto, según `data-model.md`.

### Implementation (Green)

- [X] T009 Implementar en `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/dto/AuthDtos.kt` el campo `PodcastFeaturesDto.podcastDiscovery`, la derivación `AppConfigDto.podcastTopChartsEnabled` y en `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/SystemRepo.kt` el campo `PodcastCapabilities.topCharts` con su mapeo y caché de sesión (`podcastCapabilities(forceRefresh: Boolean = false)`); en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/ModeViewModel.kt` hacer que `refresh()` llame con `forceRefresh = true`, según `data-model.md` (T004).
- [X] T010 Crear `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/TopCharts.kt` con `TopCountry`, `TopGenre`, `TopChartsLoad` y la constante `TOP_CHARTS_LIMIT = 50`, citando `TopCountry.id`, `TopCountry.label`, `TopGenre.id`, `TopGenre.label`, `TopChartsLoad.items` y `TopChartsLoad.genreUnavailable` de `data-model.md` (T005).
- [X] T011 Crear `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/LoadTopChartsUseCase.kt` con la regla general → filtrada → fallback general solo para `Unavailable`/`Unknown` → error, sin fallback para `Unauthorized`/`NotFound`, citando `TopChartsLoad.items` y `TopChartsLoad.genreUnavailable` de `data-model.md` (T006).
- [X] T012 Añadir `top()` en `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/api/PodcastsApi.kt` y `top()` en `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/PodcastSubscriptionsRepo.kt`, enviando `country`, `genre` solo si no es `ALL` y `TOP_CHARTS_LIMIT`, reutilizando `PodcastSearchPage.items` y mapeando `PodcastSearchResult.title/author/feedUrl/imageUrl/categories/description` según `data-model.md` (T007).
- [X] T013 Ampliar `app/src/main/java/com/izquierdojl/tolocharadio/data/local/InstancePrefs.kt` con `InstancePrefs.topChartsCountry`, `InstancePrefs.topChartsGenre`, las claves `top_charts_country`/`top_charts_genre` y los setters correspondientes, según `data-model.md` (T008).
- [X] T014 Ejecutar los tests de la fase y confirmar verde: `./gradlew testDebugUnitTest --tests "*TopChartsTest" --tests "*LoadTopChartsUseCaseTest" --tests "*PodcastCapabilitiesDtoTest" --tests "*SystemRepoTest" --tests "*PodcastSubscriptionsRepoTest" --tests "*InstancePrefsTest"` (raíz del repo).

**Checkpoint**: Fundación lista: capacidad, endpoint, catálogo, fallback y persistencia probados. US1/US2/US3 pueden empezar.

---

## Phase 3: User Story 1 - Descubrir los podcasts más populares sin escribir nada (Priority: P1) 🎯 MVP

**Goal**: Abrir Descubrir con la búsqueda vacía y ver el ranking de España con hasta 50 podcasts numerados, pudiendo suscribirse con el flujo existente.

**Independent Test**: Con una sesión y un servidor con `podcastDiscovery=itunes`, abrir Descubrir sin buscar y comprobar la sección «Populares» con España/«Todas las categorías», elementos numerados y suscripción operativa.

### Tests for User Story 1 (Red)

- [X] T015 [P] [US1] Escribir en `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/TopChartsViewModelTest.kt` los tests (Red) de estado inicial visible/loading inmediato, carga con capacidad Apple y Argentina/España, defaults España/«Todas las categorías», `items` y `loading`. Citar `data-model.md` → `TopChartsUi.visible`, `TopChartsUi.country`, `TopChartsUi.genre`, `TopChartsUi.items`, `TopChartsUi.loading`, `TopChartsUi.error`, `TopChartsUi.isAuthError`, `TopChartsUi.genreUnavailable`. Usar `MainDispatcherRule` y un `DataStore<Preferences>` fake como en `ModeViewModelTest`.
- [X] T016 [P] [US1] Añadir en `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/DiscoverViewModelTest.kt` los tests (Red) de `DiscoverUi.submittedQuery`: se fija al buscar, se limpia al vaciar el campo, no cambia al teclear sin buscar; y de `toggleSubscribe` para un `PodcastSearchResult.feedUrl` del ranking (alta y baja sin duplicar). Citar `data-model.md` → `DiscoverUi.submittedQuery`.
- [X] T017 [P] [US1] Añadir en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/TopChartsSectionTest.kt` el test Compose (Red) de carga, contenido con posición 1/2 y callback de suscripción, citando `TopChartsUi.loading` y `TopChartsUi.items` de `data-model.md` y `contracts/top-charts-ui.md`.

### Implementation for User Story 1 (Green)

- [X] T018 [US1] Crear `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/TopChartsViewModel.kt` inyectando `SystemRepo`, `InstancePrefs` y `LoadTopChartsUseCase`, con `TopChartsUi` (`visible=true`/`loading` inmediatos), comprobación de capacidad cacheada (ocultar solo si es definitivamente no soportada; error con reintento si `ApiResult.Err`), lectura inicial de preferencias, carga inicial, `isAuthError` para credenciales y mapeo de error a español. Citar `data-model.md` → `TopChartsUi.visible`, `TopChartsUi.country`, `TopChartsUi.genre`, `TopChartsUi.items`, `TopChartsUi.loading`, `TopChartsUi.error`, `TopChartsUi.isAuthError`, `TopChartsUi.genreUnavailable` (T015).
- [X] T019 [US1] Añadir `DiscoverUi.submittedQuery` y actualizar `onQueryChange`/`search()` en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/DiscoverViewModel.kt`, citando `data-model.md` → `DiscoverUi.submittedQuery` (T016).
- [X] T020 [P] [US1] Extender `PodcastSearchRow` en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/PodcastRows.kt` con `position: Int? = null` y el número de posición, manteniendo portada, título, autor, categorías y suscripción; citar `data-model.md` → `TopChartsUi.items`, `PodcastSearchResult.title`, `PodcastSearchResult.author`, `PodcastSearchResult.feedUrl`, `PodcastSearchResult.imageUrl`, `PodcastSearchResult.categories`, `PodcastSearchResult.description`.
- [X] T021 [US1] Crear `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/TopChartsSection.kt` sin estado: título, carga, contenido y vacío, reutilizando `PodcastSearchRow`/`SubscribeButton`/`EmptyState` y los textos/tags de `contracts/top-charts-ui.md`; recibir `TopChartsUi`, `subscribedFeedUrls`, `onToggleSubscribe`, `onRetry` y `onEditServer` (con default para US1) y citar `data-model.md` → `TopChartsUi.loading`, `TopChartsUi.items`, `TopChartsUi.error`, `TopChartsUi.isAuthError`, `TopChartsUi.genreUnavailable` (T017).
- [X] T022 [US1] Extraer `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/DiscoverContent.kt` sin estado y hacer que `DiscoverScreen.kt` delegue en él con los estados y callbacks; con `DiscoverUi.submittedQuery` vacío y `TopChartsUi.visible` true mostrar la sección; con búsqueda confirmada mostrar carga/resultados/vacío de búsqueda; sin ranking ni búsqueda conservar el comportamiento actual. Mantener búsqueda, RSS, OPML y snackbar, pasar `subscribedFeedUrls`, `onToggleSubscribe` y `onEditServer` (desde `TolochaNavGraph`) y citar `data-model.md` → `DiscoverUi.submittedQuery`, `TopChartsUi.visible`.

**Checkpoint**: US1 funcional y validable de forma independiente (ranking por defecto + suscripción).

---

## Phase 4: User Story 2 - Elegir país y género y conservar la preferencia (Priority: P2)

**Goal**: Cambiar país y género del ranking y recordar la elección entre sesiones.

**Independent Test**: Cambiar a México y Comedia, verificar la recarga; cerrar y reabrir la app y comprobar que se mantienen ambos selectores.

### Tests for User Story 2 (Red)

- [X] T023 [P] [US2] Ampliar `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/TopChartsViewModelTest.kt` con los tests (Red) de `selectCountry`/`selectGenre`: actualización inmediata, persistencia, recarga con la nueva combinación, lectura de preferencias al crear el VM y última selección gana. Citar `data-model.md` → `TopChartsUi.country`, `TopChartsUi.genre`, `InstancePrefs.topChartsCountry`, `InstancePrefs.topChartsGenre`.
- [X] T024 [P] [US2] Ampliar `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/TopChartsSectionTest.kt` con los tests (Red) de los selectores: valor por defecto, opciones, etiquetas accesibles y callbacks de cambio. Citar `data-model.md` → `TopCountry.id`, `TopCountry.label`, `TopGenre.id`, `TopGenre.label`, `TopChartsUi.country`, `TopChartsUi.genre` y `contracts/top-charts-ui.md`.

### Implementation for User Story 2 (Green)

- [X] T025 [US2] Ampliar `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/TopChartsViewModel.kt` con `selectCountry`/`selectGenre`, persistencia en `InstancePrefs`, recarga y cancelación/generación para la última selección. Citar `data-model.md` → `TopChartsUi.country`, `TopChartsUi.genre`, `InstancePrefs.topChartsCountry`, `InstancePrefs.topChartsGenre` (T023).
- [X] T026 [US2] Añadir a `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/TopChartsSection.kt` los selectores M3 de país y género con etiquetas/estado accesible, usando `TopCountry.id/label` y `TopGenre.id/label` de `data-model.md`; citar también `TopChartsUi.country` y `TopChartsUi.genre` (T024).
- [X] T027 [US2] Cablear en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/DiscoverScreen.kt` los callbacks de los selectores a `TopChartsViewModel.selectCountry`/`selectGenre`, conservando la búsqueda y el resto de controles; citar `data-model.md` → `TopChartsUi.country`, `TopChartsUi.genre`.

**Checkpoint**: US1 y US2 funcionan juntas: ranking por defecto + filtros persistentes.

---

## Phase 5: User Story 3 - Degradación elegante y recuperación (Priority: P3)

**Goal**: Un fallo recuperable de categoría mantiene el ranking general con aviso y acción de reintento; un fallo total muestra error con reintento; las credenciales inválidas muestran «Editar servidor»; un fallo transitorio de capacidad no oculta la sección; sin proveedor Apple la sección se oculta; búsqueda/RSS/OPML no se bloquean.

**Independent Test**: Simular un fallo recuperable de categoría y comprobar general + aviso + reintento; simular fallo total y comprobar error + reintento; simular credenciales inválidas y comprobar «Editar servidor»; simular fallo transitorio de capacidad y comprobar error + reintento; usar un servidor `none`/`podcastindex` y comprobar que no aparece la sección ni se pide el ranking.

### Tests for User Story 3 (Red)

- [X] T028 [P] [US3] Ampliar `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/TopChartsViewModelTest.kt` con los tests (Red) de fallback de categoría recuperable, error total, reintento, fallo transitorio de capacidad (error + reintento, no ocultar), capacidad definitivamente no soportada/404 (visible=false), `isAuthError` con `Unauthorized`, ausencia de fallback en 401/403/404 y matriz `Unavailable`/`Unknown`/`Validation`. Citar `data-model.md` → `TopChartsUi.error`, `TopChartsUi.isAuthError`, `TopChartsUi.genreUnavailable`, `TopChartsUi.visible`, `TopChartsUi.loading`.
- [X] T029 [P] [US3] Ampliar `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/TopChartsSectionTest.kt` con los tests (Red) de aviso de categoría con «Reintentar», error con reintento, error de credenciales con «Editar servidor» (sin reintento), vacío y `liveRegion` de estado. Citar `data-model.md` → `TopChartsUi.error`, `TopChartsUi.isAuthError`, `TopChartsUi.genreUnavailable`, `TopChartsUi.loading`, `TopChartsUi.items` y `contracts/top-charts-ui.md`.

### Implementation for User Story 3 (Green)

- [X] T030 [US3] Ampliar `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/TopChartsViewModel.kt` con `genreUnavailable` y reintento de categoría, error accionable, `isAuthError`, `retry()` con `forceRefresh` solo si el error era de capacidad, ocultación por 404/capacidad definitiva, fallo transitorio de capacidad como error y generación de última selección. Citar `data-model.md` → `TopChartsUi.error`, `TopChartsUi.isAuthError`, `TopChartsUi.genreUnavailable`, `TopChartsUi.visible`, `TopChartsUi.loading` (T028).
- [X] T031 [US3] Completar en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/TopChartsSection.kt` la renderización del aviso de categoría con acción «Reintentar», el error con «Reintentar», el error de credenciales con «Editar servidor» (sin «Reintentar»), el vacío específico, el orden de foco y las semánticas de accesibilidad; citar `data-model.md` → `TopChartsUi.error`, `TopChartsUi.isAuthError`, `TopChartsUi.genreUnavailable`, `TopChartsUi.loading`, `TopChartsUi.items` y `contracts/top-charts-ui.md` (T029).
- [X] T032 [US3] Cablear en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/DiscoverScreen.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` `onRetry`, `onEditServer` (servidor activo), la visibilidad condicional (no componer la sección si `visible=false`) y el aviso de error, asegurando que búsqueda, RSS y OPML siguen operativos durante error/carga; citar `data-model.md` → `TopChartsUi.error`, `TopChartsUi.isAuthError`, `TopChartsUi.visible`.

**Checkpoint**: Las tres historias son independientes y la degradación está cubierta.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Gates, validación end-to-end, documentación y revisión de regresiones.

- [X] T033 Ejecutar los gates obligatorios completos: `./gradlew testDebugUnitTest detekt ktlintCheck lintDebug assembleDebug` (raíz del repo; en Windows `.\gradlew.bat`). Corregir cualquier issue nuevo sin tocar el baseline.
- [X] T034 [P] Ejecutar el test instrumentado de la sección en dispositivo/emulador: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.izquierdojl.tolocharadio.feature.podcasts.discover.TopChartsSectionTest` (raíz del repo).
- [X] T035 Validar `quickstart.md` en dispositivo/emulador: escenarios 1–19 (el 8 puede validarse con el test unitario o con proxy/mock), incluidos persistencia, búsqueda que oculta el ranking, suscripción, fallback con reintento, error total, credenciales/«Editar servidor», fallo transitorio de capacidad, proveedor deshabilitado, 404, red lenta y medición de tiempos; registrar el resultado (raíz del repo, `specs/0053-izquierdojl-20261122-podcast-top-charts/quickstart.md`). — verificado manualmente en emulador por el usuario (2026-11-22)
- [X] T036 [P] Actualizar `docs/uso.md` y la lista de características de `README.md` con la sección «Populares» de podcasts (país/género, suscripción y dependencia de `PODCAST_DISCOVERY_PROVIDER=itunes`), en español.
- [X] T037 [P] Revisar que no se añaden dependencias nuevas ni cambios de esquema, que no hay credenciales/PII en logs, que la UI está en español, que «Editar servidor» abre el servidor activo, que el orden de foco es el del contrato y que búsqueda/RSS/OPML/suscripciones no cambian de comportamiento (raíz del repo).

---

## Phase 7: Convergence (correcciones post-análisis)

**Purpose**: Cubrir los hallazgos del análisis (auth, capacidad transitoria, reintento de categoría, test de pantalla y matriz de errores) y dejar los gates verdes.

- [X] T038 [P] Añadir `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/DiscoverContentTest.kt` con tests Compose de: ranking visible con `submittedQuery` vacío, resultados con búsqueda confirmada, controles de búsqueda/RSS/OPML operativos durante carga/error, y «Editar servidor» invocando `onEditServer`. Citar `contracts/top-charts-ui.md` → «Composición de pantalla» y `data-model.md` → `DiscoverUi.submittedQuery`, `TopChartsUi.visible`, `TopChartsUi.error`, `TopChartsUi.isAuthError`.
- [X] T039 Re-ejecutar los gates completos y los tests instrumentados tras T038: `./gradlew testDebugUnitTest detekt ktlintCheck lintDebug assembleDebug`, `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.izquierdojl.tolocharadio.feature.podcasts.discover.TopChartsSectionTest` y `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.izquierdojl.tolocharadio.feature.podcasts.discover.DiscoverContentTest` (raíz del repo).

---

## Dependencies & Execution Order

### Phase Dependencies

```text
Setup (T001-T003)
   │
   ▼
Foundational (T004-T014)
   │
   ▼
US1 (T015-T022) ──► US2 (T023-T027) ──► US3 (T028-T032) ──► Polish (T033-T037)
```

- **Setup (Phase 1)**: sin dependencias. T001 primero; T002 y T003 en paralelo.
- **Foundational (Phase 2)**: depende de Setup; BLOQUEA US1/US2/US3. Tests T004/T005/T006/T007/T008 pueden escribirse en paralelo (archivos distintos); cada implementación depende de su test. T014 depende de T009–T013.
- **US1 (Phase 3)**: depende de Foundational. T015/T016/T017 (tests) pueden prepararse en paralelo; T018 depende de T015; T019 depende de T016; T020 es independiente; T021 depende de T017, T018 y T020; T022 depende de T019 y T021.
- **US2 (Phase 4)**: depende de US1 (mismos archivos de VM, sección y pantalla). T023/T024 (tests) en paralelo; T025 depende de T023; T026 depende de T024; T027 depende de T025, T026 y T022.
- **US3 (Phase 5)**: depende de US1/US2 (mismos archivos). T028/T029 (tests) en paralelo; T030 depende de T028; T031 depende de T029; T032 depende de T030, T031 y T027.
- **Polish (Phase 6)**: depende de todas las historias deseadas. T033 primero; T034–T037 después.
- **Convergence (Phase 7)**: depende de T022/T032 (host y cableado) y de las correcciones de T028–T031. T038 primero; T039 después. T039 repite los gates para incluir T038.

### Within Each User Story

- Tests (Red) antes de la implementación (Green).
- Modelos/estado antes de servicios; servicios antes de integración.
- Historia completa y validada antes de la siguiente prioridad.

### Parallel Opportunities

- T002 y T003 (archivos distintos).
- Tests fundacionales T004, T005, T006, T007 y T008 (archivos distintos).
- Tests de US1 T015, T016 y T017 (archivos distintos).
- T020 (PodcastRows) puede ir en paralelo con T015–T019 (otros archivos).
- Tests de US2 T023 y T024 (archivos distintos).
- Tests de US3 T028 y T029 (archivos distintos).
- T034, T036 y T037 en la fase final, en paralelo con la validación manual si no comparten archivos.
- T038 (test de pantalla) puede prepararse en paralelo con T036/T037 tras T022/T032; T039 va al final.

**Conflictos de archivo**: `TopChartsViewModel.kt` lo tocan T018, T025 y T030; `TopChartsSection.kt` lo tocan T021, T026 y T031; `DiscoverScreen.kt`/`DiscoverContent.kt` los tocan T022, T027, T032 y T038; `TolochaNavGraph.kt` T032; `TopChartsViewModelTest.kt` lo tocan T015, T023 y T028; `TopChartsSectionTest.kt` lo tocan T017, T024 y T029; `DiscoverContentTest.kt` T038; `DiscoverViewModel.kt` T019 y `DiscoverViewModelTest.kt` T016; `PodcastRows.kt` T020; `InstancePrefs.kt` T013; `PodcastsApi.kt`/`PodcastSubscriptionsRepo.kt` T012; `AuthDtos.kt`/`SystemRepo.kt` T009; `ModeViewModel.kt` T009. No marcar como [P] tareas que compartan archivo o que dependan de una tarea aún incompleta.

---

## Parallel Example: User Story 1

```text
# Preparar tests (Red) en paralelo:
Task: "T015 [P] [US1] TopChartsViewModelTest.kt — carga inicial y visibilidad"
Task: "T016 [P] [US1] DiscoverViewModelTest.kt — submittedQuery y suscripción"
Task: "T017 [P] [US1] TopChartsSectionTest.kt — carga, contenido y suscripción"

# Implementación (Green) tras los tests:
Task: "T018 [US1] TopChartsViewModel.kt"
Task: "T019 [US1] DiscoverViewModel.kt"
Task: "T020 [P] [US1] PodcastRows.kt — posición"
Task: "T021 [US1] TopChartsSection.kt"
Task: "T022 [US1] DiscoverScreen.kt — integración"
```

## Parallel Example: User Story 2

```text
# Preparar tests (Red) en paralelo:
Task: "T023 [P] [US2] TopChartsViewModelTest.kt — filtros, persistencia y última selección"
Task: "T024 [P] [US2] TopChartsSectionTest.kt — selectores y callbacks"

# Implementación (Green) tras los tests:
Task: "T025 [US2] TopChartsViewModel.kt — selectCountry/selectGenre"
Task: "T026 [US2] TopChartsSection.kt — selectores M3"
Task: "T027 [US2] DiscoverScreen.kt — cableado"
```

## Parallel Example: User Story 3

```text
# Preparar tests (Red) en paralelo:
Task: "T028 [P] [US3] TopChartsViewModelTest.kt — fallback, error, retry y 404"
Task: "T029 [P] [US3] TopChartsSectionTest.kt — aviso, error, vacío y liveRegion"

# Implementación (Green) tras los tests:
Task: "T030 [US3] TopChartsViewModel.kt — degradación"
Task: "T031 [US3] TopChartsSection.kt — estados finales"
Task: "T032 [US3] DiscoverScreen.kt — retry y visibilidad"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Phase 1 (Setup) y Phase 2 (Foundational).
2. Completar Phase 3 (US1): ranking por defecto, posición, suscripción e integración.
3. **STOP and VALIDATE**: tests de T015–T017, Compose test de la sección y quickstart 1/7.
4. Desplegar/demostrar si procede.

### Incremental Delivery

1. Setup + Foundational → base probada.
2. US1 → ranking de España + suscripción (MVP).
3. US2 → país/género + persistencia.
4. US3 → fallback con reintento, auth con «Editar servidor», error y ocultación.
5. Polish → gates, dispositivo, docs y revisión.
6. Convergence → test de pantalla y re-ejecución de gates.

---

## Notes

- [P] = archivos distintos y sin dependencias pendientes.
- [Story] mapea cada tarea a su historia para trazabilidad.
- No hay cambios de backend, esquema Room ni dependencias nuevas; el único DTO ampliado es `PodcastFeaturesDto.podcastDiscovery`.
- Verificar que cada test falla antes de implementar (Red-Green).
- No commitear sin petición explícita del usuario (AGENTS.md).
- No se sirven datos caducados ni se inventan `feedUrl`; el servidor omite elementos sin feed válido.
- Phase 7 recoge las correcciones del análisis: auth con «Editar servidor», capacidad transitoria, reintento de categoría y test de pantalla.

### Cobertura de campos restringidos de `data-model.md`

| Campo | Tareas que lo citan |
| --- | --- |
| `TopCountry.id`, `TopCountry.label` | T005, T010, T024, T026 |
| `TopGenre.id`, `TopGenre.label` | T005, T010, T024, T026 |
| `TopChartsLoad.items`, `TopChartsLoad.genreUnavailable` | T005, T006, T010, T011 |
| `TopChartsUi.visible` | T015, T018, T022, T028, T030, T032, T038 |
| `TopChartsUi.country` | T015, T018, T023, T025, T027 |
| `TopChartsUi.genre` | T015, T018, T023, T025, T027 |
| `TopChartsUi.items` | T015, T017, T018, T020, T021, T029, T031 |
| `TopChartsUi.loading` | T015, T018, T021, T028, T029, T030, T031 |
| `TopChartsUi.error` | T015, T018, T021, T028, T029, T030, T031, T032, T038 |
| `TopChartsUi.isAuthError` | T015, T018, T028, T029, T030, T031, T032, T038 |
| `TopChartsUi.genreUnavailable` | T015, T018, T021, T028, T029, T030, T031 |
| `DiscoverUi.submittedQuery` | T016, T019, T022, T038 |
| `PodcastFeaturesDto.podcastDiscovery` | T004, T009 |
| `AppConfigDto.podcastTopChartsEnabled` | T004, T009 |
| `PodcastCapabilities.topCharts` | T004, T009 |
| `PodcastSearchPage.items` | T007, T012 |
| `PodcastSearchResult.title` | T003, T007, T012, T020 |
| `PodcastSearchResult.author` | T003, T007, T012, T020 |
| `PodcastSearchResult.feedUrl` | T003, T007, T012, T016, T020 |
| `PodcastSearchResult.imageUrl` | T003, T007, T012, T020 |
| `PodcastSearchResult.categories` | T003, T007, T012, T020 |
| `PodcastSearchResult.description` | T003, T007, T012, T020 |
| `InstancePrefs.topChartsCountry` | T008, T013, T023, T025 |
| `InstancePrefs.topChartsGenre` | T008, T013, T023, T025 |
| `top_charts_country`, `top_charts_genre` | T008, T013 |
