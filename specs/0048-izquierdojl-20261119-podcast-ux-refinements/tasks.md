---
description: "Task list for Refinamientos de experiencia podcast"
---

# Tasks: Refinamientos de experiencia podcast

**Input**: Design documents from `/specs/0048-izquierdojl-20261119-podcast-ux-refinements/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: REQUIRED by constitution III (Red-Green). Write each test first, confirm it fails, then implement.

**Organization**: Tasks are grouped by user story so each one can be implemented, tested and delivered independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no pending dependencies).
- **[Story]**: User story label (`US1`…`US6`).
- Every task includes an exact file path.

## Path Conventions

- **Main**: `app/src/main/java/com/izquierdojl/tolocharadio/`
- **Unit tests**: `app/src/test/java/com/izquierdojl/tolocharadio/`
- **Compose tests**: `app/src/androidTest/java/com/izquierdojl/tolocharadio/`
- **Specs**: `specs/0048-izquierdojl-20261119-podcast-ux-refinements/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar base verde y preparar etiquetas/fixtures compartidos.

- [X] T001 Verify baseline gates before changes: run `./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` from the repo root and record any pre-existing failure in the `app/build.gradle.kts` context.
- [X] T002 [P] Add test tags for the queue now-playing card, queue episode metadata, mini-player time row and podcast start selector in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/TolochaTestTags.kt`.
- [X] T003 [P] Extend test builders with `testPlayerUi`, `testPodcastStatsSummary`, `testPodcastTimeline`, `testPodcastHabits`, `testPodcastCategories`, `testPodcastRecents`, `testPodcastTopPodcasts` and `testPodcastTopEpisodes` in `app/src/test/java/com/izquierdojl/tolocharadio/support/PodcastFixtures.kt`.

**Checkpoint**: Baseline known, tags and fixtures ready.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Reglas puras de tiempo y formato compartidas por las historias de cola y reproductor.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T004 [P] Write failing JVM tests for `remainingMs`, `remainingWholeMinutes` and boundary cases (unknown duration, position 0, completed, position ≥ duration, less than one minute) in `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodeTimeTest.kt`.
- [X] T005 Implement `remainingMs` and `remainingWholeMinutes` in `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodeTime.kt`, resolving `data-model.md` `EpisodeTimeInfo.positionMs`, `EpisodeTimeInfo.durationMs`, `EpisodeTimeInfo.remainingMs` and `EpisodeTimeInfo.progressFraction`, plus `QueueEpisodeMeta.remainingLabel`.
- [X] T006 [P] Write failing JVM tests for `m:ss`, `h:mm:ss`, negative/clamped values and remaining formatting in `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeTimeFormatTest.kt`.
- [X] T007 Implement `formatPlaybackTime` and `formatRemainingTime` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeTimeFormat.kt`, resolving `data-model.md` `EpisodeTimeInfo.elapsedLabel` and `EpisodeTimeInfo.remainingLabel`.

**Checkpoint**: Pure time rules and shared clock format ready.

---

## Phase 3: User Story 1 - La cola muestra arriba el episodio en curso y su tiempo (Priority: P1) 🎯 MVP

**Goal**: Bloque superior con portada, programa, título, transcurrido, duración y avance; el episodio en curso no se duplica entre los pendientes.

**Independent Test**: Reproducir un episodio con una cola de tres pendientes, abrir la cola y comprobar el bloque con tiempos; pausar, avanzar, vaciar pendientes y detener.

### Tests for User Story 1

- [X] T008 [P] [US1] Write failing Compose tests for the now-playing card, no duplication, empty queue with current episode, paused state, unknown duration, remains visible after scrolling the pending list and tap callback in `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/queue/QueueScreenTest.kt`.

### Implementation for User Story 1

- [X] T009 [US1] Create `QueueNowPlayingCard` with cover, program, title, elapsed/total and progress bar in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/queue/QueueNowPlayingCard.kt`, resolving `data-model.md` `EpisodePlayerUi.episode`, `EpisodePlayerUi.podcast`, `EpisodePlayerUi.playing`, `EpisodePlayerUi.positionMs`, `EpisodePlayerUi.durationMs`, `EpisodePlayerUi.loading` and `EpisodeTimeInfo.progressFraction`.
- [X] T010 [US1] Update `QueueScreen` to render the card fixed above list/loading/empty/error, remove the "Suena: …" text, preserve `setPlayingId` and add `onOpenFullPlayer` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/queue/QueueScreen.kt`.
- [X] T011 [US1] Wire `QueueScreen(onOpenFullPlayer = { episodeFullPlayer = true })` in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`.

**Checkpoint**: User Story 1 fully functional and independently testable.

---

## Phase 4: User Story 2 - Detalle del episodio con detener gráfico y favorito (Priority: P1)

**Goal**: Detener como icono coherente y favorito vivo que no interrumpe la reproducción; la ficha también refleja el estado.

**Independent Test**: Abrir el reproductor completo, comprobar icono "Detener", marcar/desmarcar favorito, verlo en la ficha y en Favoritos, y comprobar que detener cierra y limpia la tarjeta inferior.

### Tests for User Story 2

- [X] T012 [P] [US2] Write failing repository and ViewModel tests for `loaded` transitions, live favorites, explicit toggle and failure preserving state in `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/EpisodeFavoritesRepoTest.kt` and `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodeActionsViewModelTest.kt`.
- [X] T013 [P] [US2] Write failing Compose tests for icon-only "Detener" (no text node), favorite toggle, no pause/close and accessible labels in `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeFullPlayerSheetTest.kt`, and add filled-heart/empty-heart assertions for `EpisodeCard` and `EpisodeRow` in `app/src/androidTest/java/com/izquierdojl/tolocharadio/core/ui/components/PodcastCardsTest.kt`.
- [X] T014 [P] [US2] Write failing ViewModel tests for `stop()` clearing the UI after reporting the captured duration and for end-without-next clearing in `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModelTest.kt`.

### Implementation for User Story 2

- [X] T015 [US2] Add the loaded flag to `EpisodeFavoritesRepo` and expose `favoritesLoaded` plus `toggleFavorite(episodeId, isFavorite)` in `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/EpisodeFavoritesRepo.kt` and `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodeActionsViewModel.kt`, resolving `data-model.md` `EpisodeFavoriteState.episodeId`, `EpisodeFavoriteState.isFavorite` and `EpisodeFavoriteState.loaded`.
- [X] T016 [US2] Update the episode sheet to use the live favorite set and the explicit toggle in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/sheet/EpisodeSheet.kt`.
- [X] T017 [US2] Update the episode detail / full player with an icon-only `Close` action labeled "Detener" and a `FavoriteButton` that uses the live state in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeFullPlayerSheet.kt`, resolving `data-model.md` `EpisodeFavoriteState.isFavorite` and `EpisodeFavoriteState.loaded`.
- [X] T018 [US2] Clear `EpisodePlayerUi` on `stop()` and on end without next, capturing `durationMs` for the final progress report and correcting the previous behavior that left the card visible in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt`, resolving `data-model.md` `EpisodePlayerUi.episode`, `EpisodePlayerUi.positionMs` and `EpisodePlayerUi.durationMs`.

**Checkpoint**: User Story 2 works; favorite and stop behave consistently across detail and sheet.

---

## Phase 5: User Story 3 - Duración y minutos restantes en cada episodio de la cola (Priority: P2)

**Goal**: Cada fila muestra la duración y, si está empezada, los minutos restantes en una línea secundaria; refresco silencioso al volver.

**Independent Test**: Abrir una cola con un episodio sin empezar, otro a medias y otro completado; comprobar duración/restante; reproducir y volver para ver el restante actualizado.

### Tests for User Story 3

- [X] T019 [P] [US3] Write failing Compose tests for row duration and remaining (unstarted, in-progress, completed, less than one minute, unknown duration, 200 % font) in `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/queue/QueueScreenTest.kt`.
- [X] T020 [P] [US3] Write failing ViewModel tests for `refreshSilently()` preserving visible content while updating progress and for exclusion of the playing episode in `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/queue/QueueViewModelTest.kt`.

### Implementation for User Story 3

- [X] T021 [US3] Add `refreshSilently()` to `QueueViewModel` without switching to `Loading` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/queue/QueueViewModel.kt`.
- [X] T022 [US3] Add the metadata line to `QueueCard` using `durationLabel` and `remainingLabel`, and call `refreshSilently()` on `ON_RESUME` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/queue/QueueScreen.kt`, resolving `data-model.md` `QueueEpisodeMeta.durationLabel`, `QueueEpisodeMeta.remainingLabel`, `QueueEpisodeMeta.isStarted` and `QueueEpisodeMeta.isCompleted`.

**Checkpoint**: User Story 3 works; row metadata updates after listening.

---

## Phase 6: User Story 4 - Tiempo transcurrido y restante en la tarjeta de reproducción inferior (Priority: P2)

**Goal**: La tarjeta inferior muestra transcurrido y restante, actualizados con el sondeo existente.

**Independent Test**: Reproducir, pausar, saltar, cambiar de episodio, usar un episodio de más de una hora y uno sin duración; comprobar cifras y formato.

### Tests for User Story 4

- [X] T023 [P] [US4] Write failing Compose tests for elapsed/remaining, pause, seek, episode change, unknown duration, stop hiding the card and 200 % font in `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeMiniPlayerTest.kt`.

### Implementation for User Story 4

- [X] T024 [US4] Add the elapsed/remaining row to the bottom card (mini player, `EpisodeMiniPlayer`) with `formatPlaybackTime`, `remainingMs` and an accessible description in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeMiniPlayer.kt`, resolving `data-model.md` `EpisodeTimeInfo.elapsedLabel`, `EpisodeTimeInfo.remainingLabel`, `EpisodeTimeInfo.remainingMs` and `EpisodeTimeInfo.progressFraction`.

**Checkpoint**: User Story 4 works; no invented remaining and no residual card after stop.

---

## Phase 7: User Story 5 - Pantalla de arranque propia para podcast (Priority: P2)

**Goal**: Preferencia independiente de arranque de podcast con los cinco destinos, persistida y aplicada al iniciar en modo Podcasts.

**Independent Test**: Elegir Cola/Suscripciones, reiniciar, comprobar el destino; cambiar radio y comprobar independencia; usar un servidor sin podcasts y comprobar la caída a radio.

### Tests for User Story 5

- [X] T025 [P] [US5] Write failing JVM tests for `resolveStartDestination` (radio/podcast, all five destinations, no podcasts, default) in `app/src/test/java/com/izquierdojl/tolocharadio/core/ui/navigation/StartDestinationTest.kt`.
- [X] T026 [P] [US5] Extend `InstancePrefsTest` for `podcastStartScreen` default, corrupt value and persistence in `app/src/test/java/com/izquierdojl/tolocharadio/data/local/InstancePrefsTest.kt`.
- [X] T027 [P] [US5] Extend `SettingsViewModelTest` for `podcastStartScreen` collection, change and persistence in `app/src/test/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModelTest.kt`.
- [X] T028 [P] [US5] Write failing Compose tests for the five podcast options, default "Inicio" and selection callback in `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/settings/SettingsPodcastStartTest.kt`.
- [X] T029 [P] [US5] Extend `SettingsLabelsTest` for all `PodcastStartScreen` labels in `app/src/test/java/com/izquierdojl/tolocharadio/feature/settings/SettingsLabelsTest.kt`.

### Implementation for User Story 5

- [X] T030 [US5] Create the `PodcastStartScreen` enum and `resolveStartDestination` in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/PodcastStartScreen.kt` and `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/StartDestination.kt`, resolving `data-model.md` `PodcastStartScreen.HOME`, `PodcastStartScreen.DISCOVER`, `PodcastStartScreen.SUBSCRIPTIONS`, `PodcastStartScreen.QUEUE` and `PodcastStartScreen.FAVORITES`.
- [X] T031 [US5] Add the `podcast_start_screen` key, flow (default `HOME`, corrupt → `HOME`) and setter to `InstancePrefs` in `app/src/main/java/com/izquierdojl/tolocharadio/data/local/InstancePrefs.kt`, resolving `data-model.md` `PodcastStartScreen` and its default.
- [X] T032 [US5] Add `podcastStartScreen` to `SettingsUi`, the collector and `onPodcastStartScreenChange` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsViewModel.kt`.
- [X] T033 [US5] Add the radio heading rename, the podcast start dropdown and `SettingsActions.onPodcastStartScreenChange` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt`.
- [X] T034 [US5] Add `PodcastStartScreen.label()` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsLabels.kt`.
- [X] T035 [US5] Collect `podcastStartScreen` and pass it to the nav graph in `app/src/main/java/com/izquierdojl/tolocharadio/MainActivity.kt`.
- [X] T036 [US5] Add `podcastStartScreen` to `TolochaNavGraph` and `PodcastRouteEffects`, use `resolveStartDestination` and include the preference in the `LaunchedEffect` keys in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`, resolving `data-model.md` `StartDestination.radioStart`, `StartDestination.podcastStart`, `StartDestination.defaultSection`, `StartDestination.podcastsEnabled` and `StartDestination.route`.

**Checkpoint**: User Story 5 works; radio and podcast start preferences are independent.

---

## Phase 8: User Story 6 - Gráficas de radio o podcast según el modo activo (Priority: P3)

**Goal**: Gráficas abre el conjunto del modo activo; la vista de podcast usa el mismo lenguaje visual que radio, sin mezclar datos.

**Independent Test**: Abrir Gráficas en Radio y en Podcasts, comprobar encabezados y bloques, cambiar de modo con la vista abierta, probar servidor sin podcasts, vacío, error y credenciales.

### Tests for User Story 6

- [X] T037 [P] [US6] Write failing ViewModel tests for success with matrix, empty, any-block error, auth error, retry and foreground refresh in `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsViewModelTest.kt`.
- [X] T038 [P] [US6] Write failing JVM tests for timeline, habits, rankings, categories, recent and summary mappings in `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsMappersTest.kt`.
- [X] T039 [P] [US6] Write failing JVM tests for `Routes.statsRoute(podcastMode)` and `routeAfterModeChange(currentRoute, podcastMode, podcastsEnabled)` in `app/src/test/java/com/izquierdojl/tolocharadio/core/ui/navigation/StartDestinationTest.kt`.
- [X] T040 [P] [US6] Write failing Compose tests for the podcast header, chart blocks, empty state, error/retry and edit-server action in `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsScreenTest.kt`.
- [X] T041 [P] [US6] Update `StatsEntryFlowTest` for "Tus estadísticas de radio" and the mode-selection row in `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/stats/StatsEntryFlowTest.kt`.

### Implementation for User Story 6

- [X] T042 [US6] Add `Routes.statsRoute(podcastMode)` and wire `SettingsScreen(onStats = { ... })` in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/Routes.kt` and `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`.
- [X] T043 [P] [US6] Create reusable `RankingBars`, `ShareBars` and `RecentActivityList` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/blocks/ChartPrimitives.kt`.
- [X] T044 [US6] Refactor `TopStationsChart`, `GenreCountryCharts` and `RecentList` to use `RankingBars`/`ShareBars`/`RecentActivityList`, and extract a reusable `SummaryCard` in `StatsSummaryCards` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/blocks/TopStationsChart.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/blocks/GenreCountryCharts.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/blocks/RecentList.kt` and `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/blocks/StatsSummaryCards.kt`.
- [X] T045 [P] [US6] Create podcast-to-chart mappings in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsMappers.kt`, resolving `data-model.md` `RankingItem.label`, `RankingItem.totalMs`, `ShareItem.label`, `ShareItem.totalMs`, `RecentItem.label`, `RecentItem.startedAt`, `RecentItem.durationMs`, `StatsSummaryItem.label`, `StatsSummaryItem.value` and `StatsSummaryItem.hint`.
- [X] T046 [US6] Extend `PodcastStatsUi` and `PodcastStatsViewModel` with `Empty`, `Error(message, isAuthError)`, full parallel loading, `habitsMatrix`, `onForeground`, `retry` and `StatsErrorLogger` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsViewModel.kt`, resolving `data-model.md` `PodcastStatsUi.Loading`, `PodcastStatsUi.Empty`, `PodcastStatsUi.Error.message`, `PodcastStatsUi.Error.isAuthError`, `PodcastStatsUi.Content.summary`, `PodcastStatsUi.Content.topPodcasts`, `PodcastStatsUi.Content.topEpisodes`, `PodcastStatsUi.Content.timeline`, `PodcastStatsUi.Content.habits`, `PodcastStatsUi.Content.categories`, `PodcastStatsUi.Content.recent` and `PodcastStatsUi.Content.habitsMatrix`.
- [X] T047 [US6] Rewrite `PodcastStatsScreen` with a stateless `PodcastStatsContent` and the summary, timeline, programs, episodes, habits, categories and recent blocks in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsScreen.kt`.
- [X] T048 [US6] Change the radio header to "Tus estadísticas de radio" and pass `onEditServer` to `PodcastStatsScreen` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/stats/StatsScreen.kt` and `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`.
- [X] T049 [US6] Add the pure `routeAfterModeChange(currentRoute, podcastMode, podcastsEnabled)` helper in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/Routes.kt` and use it in `PodcastRouteEffects` in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` to swap `STATS` ↔ `PODCAST_STATS` and fall back to `STATS` when podcasts are unavailable.

**Checkpoint**: User Story 6 works; radio and podcast charts never mix.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Gates, accesibilidad, privacidad y verificación final.

- [X] T050 Run the full local gate suite `./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` from the repo root and fix regressions in the `app/build.gradle.kts` context.
- [X] T051 Run `./gradlew connectedDebugAndroidTest` as the local pre-merge Compose gate and record the device result in `specs/0048-izquierdojl-20261119-podcast-ux-refinements/quickstart.md`.
- [X] T052 Execute manual scenarios 3.1–3.7 of `specs/0048-izquierdojl-20261119-podcast-ux-refinements/quickstart.md` on API 26, 33 and 37. — verificado manualmente en emulador por el usuario (2026-11-20)
- [X] T053 Pass TalkBack over the queue card, queue rows, full player, mini player, Configuración and Gráficas; fix labels in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/queue/QueueScreen.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeMiniPlayer.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeFullPlayerSheet.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt` and `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsScreen.kt`. — verificado manualmente en emulador por el usuario (2026-11-20)
- [X] T054 Review new logs and messages for PII or technical leakage in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsViewModel.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodeActionsViewModel.kt` and `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt`.
- [X] T055 Remove dead code and unused imports, including the old private `formatMillis` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeFullPlayerSheet.kt` and the old "Suena: …" text in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/queue/QueueScreen.kt`.
- [X] T056 Verify no API, DTO, Room schema, backup rule, credential or auth change in `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/api/PodcastsApi.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/dto/PodcastDtos.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/data/local/TolochaDb.kt`, `app/src/main/res/xml/backup_rules.xml` and `app/src/main/res/xml/data_extraction_rules.xml`.
- [X] T057 Re-check the implementation against constitution v3.1.0 and record the result in `specs/0048-izquierdojl-20261119-podcast-ux-refinements/plan.md`.
- [X] T058 Update the feature status and final validation notes in `specs/0048-izquierdojl-20261119-podcast-ux-refinements/spec.md`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: depends on T001–T003; blocks all user stories.
- **User Stories (Phases 3–8)**: depend on Phase 2.
  - US1 and US2 (P1) can run in parallel after foundational: US1 touches `TolochaNavGraph.kt` (T011), while US2 touches player/sheet files, so they do not conflict.
  - US3 shares `QueueScreen.kt` and `QueueScreenTest.kt` with US1; run US3 after US1 or coordinate.
  - US4 shares `EpisodeMiniPlayer.kt` with no other story; its stop-hide test depends on T018.
  - US5 touches `TolochaNavGraph.kt` with US1 and US6; coordinate the NavGraph edits.
  - US6 touches `TolochaNavGraph.kt` with US1/US5 and `StartDestinationTest.kt` with US5; run after US5 for the test file and coordinate NavGraph.
- **Polish (Phase 9)**: depends on all selected stories.

### User Story Dependencies

- **US1**: independent; needs the foundational time rules and the queue screen.
- **US2**: independent; needs the live favorites and stop clear; no dependency on US1.
- **US3**: depends on US1 for the queue screen structure; shares `QueueScreen.kt`/`QueueScreenTest.kt`. Run after US1.
- **US4**: independent; the stop-hide assertion depends on T018 from US2.
- **US5**: independent; touches Configuración, MainActivity and NavGraph. Run before US6 to avoid `StartDestinationTest.kt` conflicts.
- **US6**: depends on US5 for `StartDestinationTest.kt`; touches NavGraph, so coordinate with US1/US5.

### Within Each User Story

- Tests MUST exist and fail before implementation.
- Pure/domain logic before UI wiring.
- Story complete before moving to the next priority unless running in parallel by file ownership.
- Every story ends with an independent validation checkpoint.

### Parallel Opportunities

- T002 and T003 can run in parallel.
- T004 and T006 can run in parallel.
- US1 test T008 and US2 tests T012–T014 can run in parallel (different files).
- US3 tests T019–T020 require US1 to be complete because T019 edits `QueueScreenTest.kt`; then T019 and T020 can run in parallel.
- US5 tests T025–T029 can run in parallel.
- US6 tests T037–T041 can run in parallel after US5 finishes `StartDestinationTest.kt`.
- US6 implementation T043 and T045 can run in parallel; T044 depends on T043 and T046 depends on T045.

---

## Parallel Example: User Story 1

```bash
Task: "Write failing Compose tests for the now-playing card and queue placement in QueueScreenTest.kt"
```

## Parallel Example: User Story 2

```bash
Task: "Write failing ViewModel tests in EpisodeActionsViewModelTest.kt"
Task: "Write failing Compose tests in EpisodeFullPlayerSheetTest.kt"
Task: "Write failing ViewModel tests in EpisodePlayerViewModelTest.kt"
```

## Parallel Example: User Story 3

```bash
Task: "Write failing Compose tests in QueueScreenTest.kt"
Task: "Write failing ViewModel tests in QueueViewModelTest.kt"
```

## Parallel Example: User Story 4

```bash
Task: "Write failing Compose tests in EpisodeMiniPlayerTest.kt"
```

## Parallel Example: User Story 5

```bash
Task: "Write failing JVM tests for resolveStartDestination in StartDestinationTest.kt"
Task: "Extend InstancePrefsTest for podcastStartScreen"
Task: "Extend SettingsViewModelTest for podcastStartScreen"
Task: "Write failing Compose tests for the podcast start selector in SettingsPodcastStartTest.kt"
Task: "Extend SettingsLabelsTest for PodcastStartScreen labels"
```

## Parallel Example: User Story 6

```bash
Task: "Write failing ViewModel tests in PodcastStatsViewModelTest.kt"
Task: "Write failing mapper tests in PodcastStatsMappersTest.kt"
Task: "Write failing statsRoute tests in StartDestinationTest.kt"
Task: "Write failing Compose tests in PodcastStatsScreenTest.kt"
Task: "Update StatsEntryFlowTest for the radio header"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 (T001–T003).
2. Complete Phase 2 (T004–T007).
3. Complete US1 (T008–T011).
4. **STOP and VALIDATE**: Play an episode, open the queue, verify the now-playing block, no duplication and empty-queue message.
5. Deploy/demo the MVP.

### Incremental Delivery

1. Foundation + US1 → validate queue header (MVP).
2. US2 → validate stop icon and favorite.
3. US3 → validate duration/remaining in rows.
4. US4 → validate mini-player times.
5. US5 → validate podcast start preference.
6. US6 → validate mode-aware charts and podcast chart parity.
7. Polish → full gates, accessibility, privacy and quickstart.

### Parallel Team Strategy

1. Team completes Setup + Foundational together.
2. Developer A: US1 then US3 (queue).
3. Developer B: US2 then US4 (player/detail/mini-player).
4. Developer C: US5 (settings/startup).
5. Developer D: US6 (stats/charts) after US5's `StartDestinationTest.kt` and coordinating NavGraph with A/C.
6. Polish together.

---

## Notes

- [P] = different files, no pending dependency.
- Tests are mandatory under constitution III; do not implement before the test fails.
- Every new UI state has an accessible label in Spanish; no PII in logs.
- Cite the constrained fields from `data-model.md` in the implementing tasks as indicated (e.g., `EpisodeTimeInfo.remainingMs`, `QueueEpisodeMeta.remainingLabel`, `EpisodeFavoriteState.loaded`, `StartDestination.route`, `PodcastStatsUi.Content.habitsMatrix`).
- Commit after each logical group, referencing spec 0048.
- Stop at each checkpoint to validate the story independently.

---

## Task Summary

- **Total tasks**: 58.
- **Setup**: 3 (T001–T003).
- **Foundational**: 4 (T004–T007).
- **US1**: 4 (T008–T011).
- **US2**: 7 (T012–T018).
- **US3**: 4 (T019–T022).
- **US4**: 2 (T023–T024).
- **US5**: 12 (T025–T036).
- **US6**: 13 (T037–T049).
- **Polish**: 9 (T050–T058).
- **MVP**: Setup + Foundational + US1 (T001–T011).

---

## Estado de ejecución (2026-11-20)

- **Completadas**: T001–T051 y T054–T058.
- **Gates verdes**: `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck`, `lintDebug`.
- **Compose tests**: `connectedDebugAndroidTest` en `Medium_Phone_API_37.0` — BUILD SUCCESSFUL.
- **Corrección del bug reportado**: el X de la tarjeta inferior llama a `EpisodePlayerViewModel.stop()`, que limpia el episodio y oculta la tarjeta; el detener del detalle principal es un icono (`EpisodeFullPlayerActions`) y el controlador conserva la última posición antes de parar.
- **Verificación manual**: T052 (escenarios 3.1–3.7) y T053 (TalkBack/200 %) completadas por el usuario en emulador (2026-11-20).
