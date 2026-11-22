# Implementation Plan: Tops de podcasts por país y género en Descubrir

**Branch**: `0053-izquierdojl-20261122-podcast-top-charts` | **Date**: 2026-11-22 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0053-izquierdojl-20261122-podcast-top-charts/spec.md`

## Summary

Añadir la sección «Populares» a la pantalla Descubrir podcasts con el ranking de Apple por país y género, en paridad con la web. La app mostrará 50 programas ordenados por posición para España y «Todas las categorías» por defecto, permitirá cambiar entre 13 países y 18 categorías, recordará la selección en DataStore y reutilizará las filas y la suscripción existentes. El ranking se obtiene de `GET /api/v1/podcasts/top`, solo se muestra si la instancia anuncia descubrimiento de Apple, y degrada al ranking general si falla una categoría con un error recuperable. Los errores de credenciales ofrecen «Editar servidor»; los fallos transitorios de capacidad/ranking ofrecen reintento.

Enfoque técnico: nuevo view model dedicado (`TopChartsViewModel`) con estado `TopChartsUi`; catálogo y reglas de selección/fallback en `domain/podcasts`; un caso de uso que encapsula la llamada filtrada y el fallback, sin fallback para 401/403/404; método `top()` en el repositorio y en la API; capacidad `topCharts` derivada de `features.podcastDiscovery` y cacheada en `SystemRepo`; preferencias en `InstancePrefs`; sección Compose sin estado (`TopChartsSection`) y host `DiscoverContent` sin estado para tests de pantalla, integrados en Descubrir. Sin dependencias nuevas ni cambios de esquema.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JVM target 17, Android Gradle Plugin 9.4.1

**Primary Dependencies**: Jetpack Compose + Material 3 (BOM 2026.05.00), Hilt 2.60.1, Retrofit 2.11 + OkHttp 4.12 + kotlinx.serialization 1.7.3, DataStore Preferences 1.1.1, Coil 2.6, Coroutines/Flow 1.8.1

**Storage**: DataStore Preferences para la preferencia local país/género. Sin cambios en Room ni en `EncryptedSharedPreferences`.

**Testing**: JUnit 4 + `kotlinx-coroutines-test` + Turbine + MockK para dominio, repositorio/preferencias y ViewModel; Compose UI tests (`createComposeRule`) para los estados de `TopChartsSection` y para `DiscoverContent` (exclusión ranking/búsqueda, controles no bloqueados y «Editar servidor»). Gates: `testDebugUnitTest`, `detekt`, `ktlintCheck`, `lintDebug`.

**Target Platform**: Android, `minSdk` 26, `compileSdk`/`targetSdk` 37, un único módulo `app`

**Project Type**: Aplicación móvil Android (MVVM + Clean por capas, `ui → domain → data`)

**Performance Goals**: «Populares» visible con datos o estado de carga en ≤ 2 s en red típica; cambio de país/género con ranking actualizado en ≤ 3 s en el 95 % de las cargas; una sola petición de hasta 50 elementos por combinación. El estado de carga se muestra antes de comprobar la capacidad y `SystemRepo` cachea la capacidad para evitar un `/config` duplicado.

**Constraints**: HTTPS-only; sin credenciales ni PII en logs; UI en español; sin dependencias nuevas; sin paginación ni scroll infinito; sin cambios de contrato de suscripción; paridad con la web en defaults, listas fijas y flujo general, con degradación de auth/404 alineada con la constitución y el tratamiento existente de la app.

**Scale/Scope**: Una pantalla (Descubrir podcasts), 13 países, 18 categorías, 50 elementos por ranking, una preferencia local por dispositivo.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio / gate | Pre-diseño | Post-diseño | Evidencia |
| --- | --- | --- | --- |
| I. MVVM + Clean (`ui → domain → data`) | PASS | PASS | Catálogo y fallback en `domain/podcasts`; `TopChartsViewModel` sin `Context`; repositorio oculta Retrofit; Hilt para inyección. |
| II. Stack Kotlin + Compose M3 + Media3 | PASS | PASS | Solo Compose M3, Retrofit/OkHttp/kotlinx.serialization, DataStore y Coil ya aprobados. No se añaden librerías. |
| III. Calidad test-first | PASS | PASS | Tests Red-Green de dominio (catálogo, fallback, exclusiones 401/403/404), DTO de capacidad y caché, repositorio, preferencias y ViewModel; Compose UI tests de `TopChartsSection` y `DiscoverContent`. Gates estáticos obligatorios. |
| IV. Streaming robusto y errores tipados | PASS | PASS | `ApiResult`/`DomainError` existentes; fallback por categoría, error con reintento, `isAuthError` con «Editar servidor», ocultación solo sin proveedor Apple/404, sin logs de PII. |
| V. Simplicidad modular (YAGNI) | PASS | PASS | Un módulo, sin dependencias ni tablas nuevas; se reutilizan `PodcastSearchRow`, `SubscribeButton`, `EmptyState`, `SectionHeader` y el flujo de suscripción. |

**Resultado**: sin violaciones. No se requiere tabla de complejidad ni excepción constitucional.

## Project Structure

### Documentation (this feature)

```text
specs/0053-izquierdojl-20261122-podcast-top-charts/
├── plan.md              # Este archivo
├── research.md          # Fase 0: decisiones y alternativas
├── data-model.md        # Fase 1: entidades, DTOs y estados
├── quickstart.md        # Fase 1: validación end-to-end
├── contracts/
│   ├── podcasts-top-api.md      # Contrato del endpoint consumido
│   └── top-charts-ui.md         # Estados y eventos de la sección
├── checklists/
│   └── requirements.md  # Calidad de la spec
└── tasks.md             # Fase 2 (/speckit.tasks), no creado aquí
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── core/ui/components/
│   ├── PodcastRows.kt                 # PodcastSearchRow gana posición opcional
│   └── TolochaTestTags.kt             # Tags de la sección y sus estados
├── core/ui/navigation/
│   └── TolochaNavGraph.kt             # Pasa editActiveServer a DiscoverScreen
├── data/local/
│   └── InstancePrefs.kt               # País/género persistidos y validados
├── data/remote/api/
│   └── PodcastsApi.kt                 # GET podcasts/top
├── data/remote/dto/
│   └── AuthDtos.kt                    # features.podcastDiscovery + capacidad
├── data/repo/
│   ├── PodcastSubscriptionsRepo.kt    # top(country, genre, limit)
│   └── SystemRepo.kt                  # PodcastCapabilities.topCharts + caché
├── domain/podcasts/
│   ├── TopCharts.kt                   # TopCountry, TopGenre, TopChartsLoad
│   └── LoadTopChartsUseCase.kt        # ranking + fallback (sin 401/403/404)
├── feature/podcasts/
│   ├── DiscoverViewModel.kt           # submittedQuery (controla visibilidad)
│   ├── ModeViewModel.kt               # refresh de capacidad con forceRefresh
│   └── TopChartsViewModel.kt          # estado, filtros, carga, auth y reintento
└── feature/podcasts/discover/
    ├── DiscoverContent.kt             # host sin estado para tests de pantalla
    ├── DiscoverScreen.kt              # integra VMs y editActiveServer
    └── TopChartsSection.kt            # UI sin estado (selectores + lista)

app/src/test/java/com/izquierdojl/tolocharadio/
├── data/local/InstancePrefsTest.kt
├── data/remote/dto/PodcastCapabilitiesDtoTest.kt
├── data/repo/PodcastSubscriptionsRepoTest.kt
├── data/repo/SystemRepoTest.kt
├── domain/podcasts/TopChartsTest.kt
├── domain/podcasts/LoadTopChartsUseCaseTest.kt
├── feature/podcasts/DiscoverViewModelTest.kt
└── feature/podcasts/TopChartsViewModelTest.kt

app/src/androidTest/java/com/izquierdojl/tolocharadio/
└── feature/podcasts/discover/
    ├── DiscoverContentTest.kt         # ranking/búsqueda y controles no bloqueados
    └── TopChartsSectionTest.kt        # estados, selectores, auth y reintento
```

**Structure Decision**: se mantiene el módulo único `app` y la organización por feature. La lógica de selección, el catálogo fijo y el fallback viven en `domain`; la red en `data`; el estado de pantalla en un ViewModel dedicado dentro de `feature/podcasts`; la composición visual en `feature/podcasts/discover`. La suscripción sigue siendo responsabilidad de `DiscoverViewModel`, que ya expone `toggleSubscribe` y el conjunto de feeds suscritos, evitando duplicar esa lógica.

## Design Decisions

1. **Contrato del endpoint**: `GET /api/v1/podcasts/top?country=&genre=&limit=` autenticado; `country` ISO de dos letras (`es` por defecto), `genre` numérico opcional, `limit` 1–100 (50 por defecto); respuesta `{items:[PodcastSearchResult]}` en orden de posición. Se reutiliza `PodcastSearchPage` como envoltorio y `PodcastSearchResult` como elemento, sin DTO nuevo.
2. **Capacidad**: se añade `podcastDiscovery: String?` a `PodcastFeaturesDto` y `topCharts: Boolean` a `PodcastCapabilities`. Se considera disponible solo si `podcasts == true` y `podcastDiscovery == "itunes"`. Si la capacidad es definitivamente no soportada, la sección se oculta y no se consulta. `SystemRepo` cachea la capacidad de la sesión (`forceRefresh` para reintentos/refresco de modo) y `TopChartsUi` muestra `visible=true`/`loading` de inmediato mientras la comprueba. Un fallo transitorio de capacidad muestra error con reintento; no se confunde con «no soportado». Como el `/config` de `main` ya publica `podcastDiscovery` pero el endpoint solo existe en la PR #54, un `404` al pedir el ranking se trata como «no soportado»: se oculta la sección sin mostrar error.
3. **Catálogo de filtros**: `TopCountry` y `TopGenre` son enums de dominio con `id` y etiqueta en español; `TopGenre.ALL` representa «Todas las categorías» con `id = null`. Los valores desconocidos o corruptos caen a `SPAIN` y `ALL`.
4. **Estado y ownership**: `TopChartsViewModel` gestiona ranking, filtros, capacidad, carga, error, `isAuthError` y aviso de categoría; `DiscoverViewModel` añade `submittedQuery` para decidir si se muestran resultados de búsqueda o el ranking. La suscripción se delega en `DiscoverViewModel.toggleSubscribe`, que ya mantiene `subscribedFeedUrls`. `DiscoverScreen` delega en `DiscoverContent` sin estado y recibe `onEditServer` desde `TolochaNavGraph`.
5. **Fallback**: sin género se pide el ranking general. Con género, si la petición filtrada falla con un error recuperable (`Unavailable`/`Unknown`), se pide el general del país y se marca `genreUnavailable`; si el general también falla, se expone error con reintento. No hay fallback para `Unauthorized` (401/403) ni `NotFound` (404): se muestra «Editar servidor» o se oculta la sección. El aviso de categoría incluye una acción «Reintentar» que repite la categoría.
6. **Última selección gana**: cada carga cancela el job anterior y comprueba una generación antes de publicar, de modo que respuestas tardías no pisen la selección vigente.
7. **Persistencia**: claves `top_charts_country` y `top_charts_genre` en `InstancePrefs`; se guardan IDs (`es`, `1303`) y se leen con `TopCountry.fromId` / `TopGenre.fromId`. Sin sincronización entre dispositivos.
8. **UI**: `TopChartsSection` recibe estado y callbacks, usa `SectionHeader`/título «Populares», dos selectores M3 con etiqueta accesible, aviso de categoría con «Reintentar», estado de carga, error con reintento o «Editar servidor» si `isAuthError`, y lista `LazyColumn` con posición. `PodcastSearchRow` gana un `position: Int? = null` para mostrar el número sin duplicar la fila. `DiscoverScreen` delega en `DiscoverContent` sin estado para poder testear la exclusión ranking/búsqueda y los controles no bloqueados. La sección ocupa el espacio restante con `weight(1f)` y no anida scrolls.
9. **Sin paginación**: una petición de 50 elementos, sin `loadMore`; cambiar filtro o reintentar vuelve a pedir la combinación. La caché de horas es del servidor; el ViewModel conserva el último resultado en memoria mientras la pantalla vive.
10. **Tests**: no se añade MockWebServer (dependencia nueva); se mockean `PodcastsApi`/repositorios con MockK, como en el resto del proyecto. La UI se prueba con `TopChartsSection` y `DiscoverContent` sin Hilt: estados, exclusión ranking/búsqueda, controles no bloqueados, reintento y «Editar servidor». Se añade una matriz de errores 400/401/403/503 y una comprobación de red lenta en quickstart.

## Phase 0 — Research

Ver [research.md](./research.md). Todas las incógnitas del contexto técnico quedan resueltas: contrato exacto, capacidad y su caché, fallo transitorio de capacidad, ownership del estado, persistencia, fallback con exclusiones 401/403/404, acción «Editar servidor», concurrencia, catálogo fijo y estrategia de tests. No quedan incógnitas pendientes.

## Phase 1 — Design & Contracts

- [data-model.md](./data-model.md): enums, estado de UI, DTOs, preferencias, validaciones y transiciones.
- [contracts/podcasts-top-api.md](./contracts/podcasts-top-api.md): petición, respuesta, errores y semántica del endpoint.
- [contracts/top-charts-ui.md](./contracts/top-charts-ui.md): estados visibles, eventos y reglas de accesibilidad.
- [quickstart.md](./quickstart.md): prerrequisitos, comandos y escenarios de validación manual/automática.

## Implementation Workstreams

1. **Contrato y capacidad**: extender `PodcastFeaturesDto`/`AppConfigDto`, `PodcastCapabilities` y sus tests; añadir caché de capacidad/`forceRefresh` en `SystemRepo` y refresco forzado en `ModeViewModel`; añadir `top()` a `PodcastsApi` y `PodcastSubscriptionsRepo` con tests de mapeo y errores.
2. **Dominio**: crear `TopCharts.kt` (enums, defaults, `TopChartsLoad`) y `LoadTopChartsUseCase` con tests de catálogo, orden, fallback recuperable y exclusiones 401/403/404.
3. **Persistencia**: añadir los dos flows y setters a `InstancePrefs`; tests de default, round-trip y valor corrupto.
4. **Estado de pantalla**: crear `TopChartsViewModel` con carga inicial inmediata, capacidad cacheada, cambio de filtros, última-selección-gana, error, `isAuthError`, retry y fallback; añadir `submittedQuery` a `DiscoverViewModel` y sus tests.
5. **UI**: crear `TopChartsSection` sin estado, `DiscoverContent` sin estado, extender `PodcastSearchRow` con posición, añadir tags y conectar `DiscoverScreen`/`TolochaNavGraph` con ambos ViewModels y `onEditServer`.
6. **Verificación**: tests unitarios, Compose UI tests de sección y pantalla, matriz de errores, gates `testDebugUnitTest`, `detekt`, `ktlintCheck`, `lintDebug` y prueba manual en dispositivo/emulador con tiempos.

## Post-Design Constitution Check

Tras el diseño se mantienen los cinco principios sin cambios: no aparece ninguna dependencia nueva, no se introduce lógica de red en la UI, el fallback y la validación quedan en dominio, la suscripción se reutiliza y todos los estados de error son accionables (reintento para red/ranking, «Editar servidor» para credenciales). **Sin violaciones.**

## Complexity Tracking

No hay violaciones constitucionales que justificar.
