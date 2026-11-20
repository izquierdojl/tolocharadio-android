# Implementation Plan: Refinamientos de experiencia podcast

**Branch**: `0048-izquierdojl-20261119-podcast-ux-refinements` | **Date**: 2026-11-19 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0048-izquierdojl-20261119-podcast-ux-refinements/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command; its definition describes the execution workflow.

## Summary

Seis refinamientos sobre el modo podcast existente (spec 0046) y la experiencia podcast de la spec 0047, sin cambios en el contrato del servidor:

1. **Cola con episodio en curso**: bloque superior con portada, programa, título, tiempo transcurrido, duración y avance, seguido de los pendientes; el episodio en curso no se duplica.
2. **Duración y restante en la cola**: cada fila muestra la duración y, si el episodio está empezado, los minutos restantes en una línea secundaria.
3. **Detalle del episodio**: el texto "Detener" pasa a un icono coherente con el resto de controles y se añade un favorito con estado vivo.
4. **Tarjeta inferior**: tiempo transcurrido y tiempo restante, actualizados con el sondeo de posición existente.
5. **Arranque de podcast**: nueva preferencia de pantalla de arranque de podcast, independiente de la de radio y persistida en DataStore.
6. **Gráficas por modo**: Configuración abre las estadísticas de radio o podcast según el selector Radio | Podcasts; la vista de podcast gana paridad visual con las gráficas de radio.

El enfoque reutiliza la arquitectura existente (MVVM + Clean, Hilt, Compose M3, Media3), el sondeo de posición de `EpisodePlaybackController` (1 s) y los DTO de estadísticas ya existentes. Las reglas nuevas de tiempo y arranque se extraen a funciones puras; las gráficas se refactorizan en primitivas compartidas para que radio y podcast no diverjan. No se añaden dependencias, módulos Gradle, endpoints ni migraciones de Room. Se corrige además el cierre del episodio al detener para que la tarjeta inferior no quede en un estado no reanudable y desaparezca como exige FR-035.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JVM target 17 (`app/build.gradle.kts`).

**Primary Dependencies**: Jetpack Compose + Material 3 (Compose BOM 2026.05.00), Media3 1.11.0 (`exoplayer`, `session`, `ui`, `cast`, `datasource-okhttp`), Hilt 2.60.1, Room 2.7.2, DataStore 1.1.1, Retrofit + OkHttp + kotlinx.serialization, Coil, Navigation Compose. Sin dependencias nuevas.

**Storage**: DataStore Preferences para la nueva preferencia `podcast_start_screen` (enum persistido como nombre, por dispositivo). Room **sin cambios de esquema**: las estadísticas y el progreso se leen del servidor; no se persiste nada nuevo. El estado de reproducción sigue siendo efímero en `EpisodePlayerViewModel`/`EpisodePlaybackController`.

**Testing**: JUnit 4 + `kotlinx-coroutines-test` + Turbine + MockK para `domain`, `data` y ViewModel; Compose Test (`createComposeRule`) para cola, reproductor completo, tarjeta inferior, Configuración y Gráficas. Gates: `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck`, `lintDebug`.

**Target Platform**: Android; `minSdk 26`, `compileSdk`/`targetSdk` 37. Móvil, tema oscuro por defecto, UI en español.

**Project Type**: Aplicación Android de un solo módulo `app`, organizada por feature (MVVM + Clean por capas).

**Performance Goals**:
- Las cifras de tiempo de la cola y de la tarjeta inferior se actualizan con el sondeo existente de 1 s, sin tickers nuevos ni trabajo de red adicional.
- El bloque superior de la cola no realiza peticiones propias: reutiliza `EpisodePlayerUi` y `QueueRepo`.
- El refresco de la cola al volver a la pantalla es silencioso (conserva el contenido visible) y no bloquea la interacción.
- Las estadísticas de podcast siguen cargando sus bloques en paralelo; el renderizado usa `Canvas` y composables existentes, sin librerías de gráficos.
- Sin trabajo nuevo en arranque en frío: la preferencia de podcast se lee del DataStore junto con las demás.

**Constraints**:
- Sin cambios de API, DTO, autenticación ni credenciales (FR-060).
- Sin nuevas librerías, módulos Gradle ni migraciones Room (YAGNI, constitución V).
- Sin XML/Views nuevas; todo Compose M3.
- Sin PII en logs; errores accionables en español.
- El cambio de modo no debe dejar visible contenido obsoleto en Gráficas: `STATS` y `PODCAST_STATS` se intercambian al cambiar de modo.
- La vista de podcast no muestra datos de radio ni cae silenciosamente en ellos.
- El episodio en curso no se duplica en la cola y el restante nunca es negativo ni inventado.
- Detener debe dejar un estado limpio: la tarjeta inferior no puede quedar mostrando un episodio no reanudable.

**Scale/Scope**: 1 módulo, 6 áreas de UI/estado, 62 requisitos funcionales, 6 historias. Cola de hasta 200 episodios; 5 destinos de podcast; 7 consultas de estadísticas de podcast en paralelo. Sin cambios de backend.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio / gate | Cumplimiento | Evidencia en el plan |
|------------------|--------------|----------------------|
| **I. MVVM + Clean** | PASS | Reglas de tiempo (`EpisodeTime`), resolución de arranque (`StartDestination`) y mapeo de estadísticas son funciones puras; ViewModels exponen `UiState` y no usan `Context` de Activity; Hilt para VMs y repos. |
| **II. Kotlin + Compose M3 + Media3** | PASS | UI Compose M3; se reutiliza `EpisodePlayerViewModel`/`EpisodePlaybackController` y Media3 sin cambios de motor; sin XML nuevo, sin dependencias nuevas. |
| **III. Test-First** | PASS | Tests Red-Green para tiempo/restante, favorito vivo, preferencia de arranque, ViewModel de estadísticas y cierre al detener; Compose Test para cola, reproductor, tarjeta, Configuración y Gráficas. |
| **IV. Streaming robusto** | PASS | Tiempos acotados y sin negativos; fallo de favorito conserva estado y avisa; errores de estadísticas con reintento y edición de servidor si son de credenciales; sin PII; la reproducción no se interrumpe. |
| **V. Simplicidad (YAGNI)** | PASS | Primitivas de gráficas compartidas justificadas por dos consumidores (radio y podcast); sin nuevas librerías, módulos ni endpoints; se reutilizan DTO y bloques existentes. |
| **Gates de calidad** | PASS | CI: `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck`, `lintDebug`. Compose tests en emulador como gate local pre-merge. |
| **Gobernanza (v3.1.0)** | PASS | No se altera navegación de primer nivel, autenticación ni contrato; la nueva preferencia es local y la paridad de secciones se mantiene. No se requiere enmienda. |

## Project Structure

### Documentation (this feature)

```text
specs/0048-izquierdojl-20261119-podcast-ux-refinements/
├── plan.md              # Este archivo
├── research.md          # Fase 0
├── data-model.md        # Fase 1
├── quickstart.md        # Fase 1
├── contracts/
│   └── ui-contracts.md  # Fase 1 (contratos internos de UI y semántica)
├── checklists/
│   └── requirements.md  # Ya existe; se revalida al final
└── tasks.md             # Fase 2 (/speckit.tasks, no se crea aquí)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── core/ui/components/
│   ├── EpisodeRow.kt                  # reutiliza el formato de duración existente
│   ├── TolochaTestTags.kt             # + tags de cola en curso, tiempos y arranque podcast
│   └── CommonUi.kt                    # FavoriteButton existente, sin cambios de API
├── core/ui/navigation/
│   ├── PodcastStartScreen.kt          # NUEVO: enum HOME/DISCOVER/SUBSCRIPTIONS/QUEUE/FAVORITES + label/route
│   ├── Routes.kt                      # + statsRoute y routeAfterModeChange puros
│   ├── StartDestination.kt            # NUEVO: resolución pura del destino de arranque
│   ├── TolochaNavGraph.kt             # + podcastStartScreen, onStats por modo, guard de STATS/PODCAST_STATS y callback de cola al reproductor
│   └── BottomDest.kt                  # sin cambios
├── data/local/InstancePrefs.kt        # + podcastStartScreen (DataStore)
├── domain/podcasts/
│   ├── EpisodeTime.kt                 # NUEVO: restante, minutos restantes y acotación
│   └── EpisodePlaybackUseCases.kt     # sin cambios
├── feature/settings/
│   ├── SettingsViewModel.kt           # + podcastStartScreen y persistencia
│   ├── SettingsScreen.kt              # + selector de arranque de podcast; radio etiquetada
│   └── SettingsLabels.kt              # + label de PodcastStartScreen
├── feature/podcasts/
│   ├── EpisodeActionsViewModel.kt     # + toggleFavorite(id, estado) y favoritos cargados
│   ├── EpisodePlayerViewModel.kt      # cierre limpio al detener/terminar sin siguiente
│   ├── sheet/EpisodeSheet.kt          # favorito vivo (sin estado obsoleto)
│   ├── player/
│   │   ├── EpisodeTimeFormat.kt       # NUEVO: m:ss / h:mm:ss y restante
│   │   ├── EpisodeMiniPlayer.kt       # + fila de tiempo (componente interno testeable)
│   │   └── EpisodeFullPlayerSheet.kt  # detener como icono + favorito (acciones extraídas para test)
│   ├── queue/
│   │   ├── QueueViewModel.kt          # + refresco silencioso de progresos
│   │   ├── QueueScreen.kt             # + bloque en curso y metadatos (componentes internos)
│   │   └── QueueNowPlayingCard.kt     # NUEVO: cabecera de reproducción de la cola
│   └── stats/
│       ├── PodcastStatsViewModel.kt   # + Empty/Error(isAuthError), matriz y carga completa
│       ├── PodcastStatsScreen.kt      # + paridad visual; PodcastStatsContent stateless para test
│       └── PodcastStatsMappers.kt     # NUEVO: DTO de podcast -> primitivas de gráficas
├── feature/stats/
│   ├── StatsScreen.kt                 # encabezado "Tus estadísticas de radio"
│   └── blocks/
│       ├── StatsSummaryCards.kt       # extrae SummaryCard reutilizable
│       ├── StatsSummaryItem.kt        # NUEVO: modelo de resumen para podcast
│       ├── TopStationsChart.kt        # delega en RankingBars
│       ├── GenreCountryCharts.kt      # delega en ShareBars
│       ├── RecentList.kt              # delega en RecentActivityList
│       ├── ChartPrimitives.kt         # NUEVO: RankingBars, ShareBars, RecentActivityList
│       └── TimelineChart.kt           # se reutiliza con StatsTimelineDto mapeado
├── MainActivity.kt                    # + collect de podcastStartScreen
└── di/                                # sin cambios

app/src/test/java/com/izquierdojl/tolocharadio/
├── domain/podcasts/EpisodeTimeTest.kt
├── feature/podcasts/player/EpisodeTimeFormatTest.kt
├── core/ui/navigation/StartDestinationTest.kt        # arranque, Gráficas y cambio de modo
├── data/local/InstancePrefsTest.kt                   # + podcastStartScreen
├── data/repo/EpisodeFavoritesRepoTest.kt             # + loaded: false → true → fallo
├── feature/settings/SettingsViewModelTest.kt          # ampliado
├── feature/settings/SettingsLabelsTest.kt             # + labels de PodcastStartScreen
├── feature/podcasts/EpisodeActionsViewModelTest.kt    # ampliado/creado
├── feature/podcasts/EpisodePlayerViewModelTest.kt     # cierre al detener
├── feature/podcasts/queue/QueueViewModelTest.kt       # refresco silencioso y exclusión del episodio en curso
├── feature/podcasts/stats/PodcastStatsMappersTest.kt
└── feature/podcasts/stats/PodcastStatsViewModelTest.kt

app/src/androidTest/java/com/izquierdojl/tolocharadio/
├── core/ui/components/PodcastCardsTest.kt             # favorito lleno/vacío en tarjeta y fila
├── feature/podcasts/queue/QueueScreenTest.kt
├── feature/podcasts/player/EpisodeMiniPlayerTest.kt
├── feature/podcasts/player/EpisodeFullPlayerSheetTest.kt
├── feature/settings/SettingsPodcastStartTest.kt
├── feature/stats/StatsEntryFlowTest.kt                # encabezado y selección de modo
└── feature/podcasts/stats/PodcastStatsScreenTest.kt
```

**Structure Decision**: se mantiene el módulo único `app` y la separación por capas existente. Las reglas nuevas (restante/redondeo, resolución del destino de arranque, mapeo de estadísticas) son funciones puras y testeables en JVM. La UI de la cola se extrae a `QueueNowPlayingCard` para no engordar `QueueScreen` y cumplir Detekt. Las primitivas de gráficas viven en `feature/stats/blocks` y se reutilizan desde radio y podcast; no se duplica el dibujo de barras, rankings ni actividad reciente. Los DTO de podcast no se tocan; solo se mapean en la capa de presentación.

## Phase 0: Research

Resumen de decisiones (detalle en [research.md](./research.md)):

- **R1 Tiempos de la cola y tarjeta**: usar `EpisodePlayerUi.positionMs`/`durationMs`, alimentado por el sondeo de 1 s de `EpisodePlaybackController`; no crear tickers nuevos. El bloque superior y la tarjeta solo renderizan.
- **R2 Formato y restante**: funciones puras en `domain/podcasts/EpisodeTime.kt` (restante acotado, minutos restantes con techo, casos desconocidos) y formato compartido `m:ss`/`h:mm:ss`; la cola reutiliza `formatEpisodeDuration`.
- **R3 Favorito del detalle**: usar el conjunto vivo `EpisodeActionsViewModel.favoriteIds` con indicador de carga y un `toggleFavorite(id, isFavorite)` explícito; actualizar también la ficha para que no quede un estado obsoleto.
- **R4 Arranque de podcast**: enum `PodcastStartScreen`, clave DataStore `podcast_start_screen`, selector en Configuración y resolución pura `resolveStartDestination`; por defecto `HOME`; sin podcasts cae al arranque de radio.
- **R5 Gráficas por modo**: `SettingsScreen.onStats` elige `PODCAST_STATS` si `podcastMode`; `PodcastRouteEffects` intercambia `STATS` ↔ `PODCAST_STATS` al cambiar de modo y cae a `STATS` si no hay podcasts. Sin selector interno. La selección de destino se encapsula en `routeAfterModeChange(...)` y se prueba en JVM.
- **R6 Paridad de gráficas**: extraer `RankingBars`, `ShareBars`, `RecentActivityList` y `SummaryCard` de los bloques de radio; reutilizar `StatsTimelineChart` y `HabitsHeatmap` con mapeos de los DTO de podcast. La vista de podcast usa esos bloques, no listas de texto.
- **R7 Pruebas**: unitarias JVM para tiempo, arranque y mapeos; ViewModel para estadísticas/favoritos/cierre; Compose para cola, reproductor, tarjeta, Configuración y Gráficas. Actualizar `StatsEntryFlowTest` y `SettingsViewModelTest`.
- **R8 Cierre limpio**: al detener el episodio (y al terminar sin siguiente) se captura la duración para el reporte final y se limpia `EpisodePlayerUi`; así la tarjeta desaparece y no queda un play muerto.

## Phase 1: Design & Contracts

Artefactos generados:

- [data-model.md](./data-model.md): tiempo de reproducción, metadatos de cola, favorito vivo, preferencia de arranque de podcast, estados de estadísticas y mapeos a primitivas de gráficas.
- [contracts/ui-contracts.md](./contracts/ui-contracts.md): contrato del bloque en curso, fila de cola, acciones del reproductor completo, tarjeta inferior, selector de arranque de podcast y Gráficas por modo.
- [quickstart.md](./quickstart.md): comandos y escenarios manuales de validación end-to-end.

**Post-Design Constitution Check**: se mantiene el resultado anterior (PASS). No hay nuevas dependencias, módulos, permisos ni cambios de contrato. La refactorización de bloques de gráficas tiene dos consumidores reales (radio y podcast), por lo que no introduce abstracción especulativa. El cierre limpio al detener corrige un estado no reanudable sin cambiar el motor de reproducción.

## Complexity Tracking

Sin violaciones que justificar. La refactorización de primitivas de gráficas se justifica por la paridad exigida entre radio y podcast; no se añade ninguna capa, módulo ni dependencia nueva.

## Estado de implementación (2026-11-20)

- **Gates**: `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck` y `lintDebug` en verde.
- **Compose tests**: `connectedDebugAndroidTest` en `Medium_Phone_API_37.0` en verde.
- **Constitución v3.1.0**: PASS; sin cambios de API, DTO, Room, backup, credenciales ni auth.
- **Corrección reportada**: el X de la tarjeta inferior limpia `EpisodePlayerUi` y oculta la tarjeta; el detener del reproductor completo es un icono. `EpisodePlaybackController.stop()` conserva la posición antes de parar.
- **Validación manual**: T052 (escenarios 3.1–3.7) y T053 (TalkBack/200 %) verificadas por el usuario en emulador (2026-11-20).
