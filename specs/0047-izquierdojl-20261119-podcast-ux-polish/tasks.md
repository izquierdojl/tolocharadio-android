---
description: "Task list for Mejoras de experiencia podcast"
---

# Tasks: Mejoras de experiencia podcast

**Input**: Design documents from `/specs/0047-izquierdojl-20261119-podcast-ux-polish/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: REQUIRED by constitution III (Red-Green). Write each test first, confirm it fails, then implement.

**Organization**: Tasks are grouped by user story so each one can be implemented, tested and delivered independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no pending dependencies)
- **[Story]**: User story label (`US1`…`US8`)
- Every task includes an exact file path

## Path Conventions

- **Main**: `app/src/main/java/com/izquierdojl/tolocharadio/`
- **Unit tests**: `app/src/test/java/com/izquierdojl/tolocharadio/`
- **Compose tests**: `app/src/androidTest/java/com/izquierdojl/tolocharadio/`
- **Resources**: `app/src/main/res/values/strings.xml`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar que la rama parte de una base verde.

- [X] T001 Verify baseline gates before changes: run `./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` from the repo root and record any pre-existing failure in `app/build.gradle.kts` context.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Recursos y constantes compartidos por varias historias.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T002 [P] Add new Spanish string resources for media seek, drawer/menu, voice boost, download states and queue badge in `app/src/main/res/values/strings.xml`.
- [X] T003 [P] Create UI test tag constants for top bar, drawer, queue badge, download button, cards and voice toggle in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/TolochaTestTags.kt`.
- [X] T004 [P] Create shared transport constants (`15_000 ms` back, `30_000 ms` forward) in `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodeTransport.kt` and replace the duplicated constants in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt`.
- [X] T005 [P] Create reusable test builders for episodes, progress and queue in `app/src/test/java/com/izquierdojl/tolocharadio/support/PodcastFixtures.kt`.

**Checkpoint**: Shared strings, tags, constants and fixtures ready.

---

## Phase 3: User Story 1 - Retomar desde la cola donde se dejó (Priority: P1) 🎯 MVP

**Goal**: Al seleccionar un episodio a medias desde la cola, continuar en la posición guardada; completado o inválido empieza en 0; mismo criterio en auto-avance.

**Independent Test**: Reproducir 2 minutos de un episodio, pausarlo, seleccionarlo desde la cola y comprobar que arranca cerca del minuto 2.

### Tests for User Story 1

- [X] T006 [P] [US1] Write failing tests for `resumePositionSec` covering partial, completed, null, negative and out-of-range progress in `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodeResumeTest.kt`.
- [X] T007 [P] [US1] Write failing ViewModel tests for embedded progress, remote fallback, completed episode, auto-advance, persistence after resume, boundary clamping and end-of-episode completion in `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModelTest.kt`.

### Implementation for User Story 1

- [X] T008 [US1] Implement `ResumePoint` and `resumePositionSec` in `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodeResume.kt`, resolving `data-model.md` `EpisodePlaybackProgress.completed`, `EpisodePlaybackProgress.positionSec`, `EpisodePlaybackProgress.durationSec`, `ResumePoint.episodeId`, `ResumePoint.positionMs` and `ResumePoint.source`.
- [X] T009 [US1] Add `LoadEpisodeProgressUseCase` for the remote fallback in `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePlaybackUseCases.kt`.
- [X] T010 [US1] Update `EpisodePlayerViewModel.play()` to use embedded progress or `LoadEpisodeProgressUseCase` and start at the resolved position in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt`.
- [X] T011 [US1] Update `EpisodePlayerViewModel.onEnded()` to reuse the resume resolution for the next queue item in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt`.

**Checkpoint**: User Story 1 fully functional and independently testable.

---

## Phase 4: User Story 2 - Adelantar y atrasar desde la notificación (Priority: P1)

**Goal**: La notificación y la pantalla de bloqueo ofrecen −15 s / +30 s con episodios; la radio en directo no ofrece seek.

**Independent Test**: Reproducir un episodio, bloquear la pantalla y comprobar −15 s / +30 s; reproducir radio y comprobar que no aparecen.

### Tests for User Story 2

- [X] T012 [P] [US2] Write failing tests for the radio/episode/retry/cast button layout in `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/MediaButtonPolicyTest.kt`.

### Implementation for User Story 2

- [X] T013 [US2] Implement the pure media button layout selection in `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/MediaButtonPolicy.kt`.
- [X] T014 [US2] Configure seek increments from `EpisodeTransport` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackPlayerFactory.kt`.
- [X] T015 [US2] Publish radio/episode media button preferences with `ICON_SKIP_BACK_15`/`ICON_SKIP_FORWARD_30` and the existing stop/mute commands in `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt`.
- [X] T016 [US2] Observe `EpisodePlaybackController` state (including `castConnected`/`casting`) and recovery state to swap button layouts without touching the radio seek policy in `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt`.

**Checkpoint**: User Story 2 works independently; radio notification unchanged.

---

## Phase 5: User Story 3 - Cabecera despejada y menú de hamburguesa (Priority: P2)

**Goal**: Cabecera con hamburguesa, logotipo adaptable, modo y Cast; Temporizador/Servidores/Configuración en el cajón; Configuración fuera de la barra inferior.

**Independent Test**: En 320 dp y letra al 200 %, sin solapes; abrir el cajón y acceder a las tres entradas; comprobar que Configuración no está en la barra inferior.

### Tests for User Story 3

- [X] T017 [P] [US3] Write failing Compose tests for hamburger, logo/emblem, mode switch, Cast and narrow width in `app/src/androidTest/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaTopBarTest.kt`.
- [X] T018 [P] [US3] Write failing Compose tests for drawer entries, timer state and navigation callbacks in `app/src/androidTest/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaDrawerTest.kt`.
- [X] T019 [P] [US3] Update `app/src/androidTest/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavigationBarTest.kt` to expect four radio destinations and no Configuración.

### Implementation for User Story 3

- [X] T020 [US3] Add `showText` and compact-emblem rendering to `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/TolochaLogo.kt`, resolving `data-model.md` `TopBarState.showLogoText`.
- [X] T021 [US3] Extract the top bar to `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaTopBar.kt` with hamburger, logo, mode switch and Cast, resolving `data-model.md` `TopBarState.podcastsEnabled`.
- [X] T022 [US3] Create the drawer content in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaDrawer.kt`, resolving `data-model.md` `DrawerEntry.id` (`TIMER`, `SERVERS`, `SETTINGS`) and `TopBarState.menuOpen`.
- [X] T023 [US3] Extract the sleep-timer menu content into `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/SleepTimerMenu.kt`, add the drawer row and delete the now-unused `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/SleepTimerButton.kt`.
- [X] T024 [US3] Wire `ModalNavigationDrawer`, drawer close-on-navigate and settings/servers navigation in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`.
- [X] T025 [US3] Remove `Routes.SETTINGS` from `BOTTOM_DESTS` and `PODCAST_DESTS` in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/BottomDest.kt`, resolving `data-model.md` `BottomBarDestinations`.
- [X] T026 [US3] Keep `Routes.SETTINGS` in the NavHost, update KDoc and verify back/startup navigation in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`.

**Checkpoint**: User Story 3 works; navigation parity preserved through the drawer.

---

## Phase 6: User Story 4 - Potenciador de voz para episodios (Priority: P2)

**Goal**: Interruptor de voz que mejora voces bajas, se recuerda y solo se aplica a episodios locales.

**Independent Test**: Activar en un episodio bajo, comprobar mejora inmediata, persistencia tras reinicio y ausencia de efecto en radio/Cast.

### Tests for User Story 4

- [X] T027 [P] [US4] Write failing DSP tests for compression, gain smoothing, limiter and bypass in `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/VoiceBoostDspTest.kt`.
- [X] T028 [P] [US4] Write failing policy tests for preference, episode active and Cast exclusion in `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/VoiceBoostPolicyTest.kt`.
- [X] T029 [P] [US4] Write failing Compose tests for the voice toggle label, state and accessibility in `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/player/VoiceBoostToggleTest.kt`.
- [X] T030 [P] [US4] Extend `app/src/test/java/com/izquierdojl/tolocharadio/data/local/InstancePrefsTest.kt` for the new `voiceBoost` preference.

### Implementation for User Story 4

- [X] T031 [US4] Implement the pure effectiveness policy in `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/VoiceBoostPolicy.kt`, resolving `data-model.md` `VoiceBoostPreference.enabled` and `VoiceBoostRuntime.episodeActive`, `VoiceBoostRuntime.castConnected`, `VoiceBoostRuntime.effective`.
- [X] T032 [US4] Add the persisted `voiceBoost` flow and setter (default `false`) to `app/src/main/java/com/izquierdojl/tolocharadio/data/local/InstancePrefs.kt`, resolving `data-model.md` `VoiceBoostPreference.enabled` and `VoiceBoostPreference.key`.
- [X] T033 [US4] Implement the compressor/limiter in `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/VoiceBoostDsp.kt`.
- [X] T034 [US4] Wrap the DSP in a Media3 `BaseAudioProcessor` with 16-bit PCM and bypass in `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/VoiceBoostAudioProcessor.kt`.
- [X] T035 [US4] Create the singleton `VoiceBoostController` for preference, episode-active and Cast state in `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/VoiceBoostController.kt`.
- [X] T036 [US4] Inject one processor per player through a custom renderers factory in `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlaybackPlayerFactory.kt`.
- [X] T037 [US4] Mark episode active/inactive on play, stop and error in `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePlaybackController.kt`.
- [X] T038 [US4] Mark episodes inactive when radio starts in `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt`.
- [X] T039 [US4] Expose the voice boost state and toggle from `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModel.kt`.
- [X] T040 [US4] Create the `VoiceBoostToggle` composable in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/VoiceBoostToggle.kt`.
- [X] T041 [US4] Add the toggle to the full player sheet in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/player/EpisodeFullPlayerSheet.kt`.

**Checkpoint**: User Story 4 works; radio and Cast remain unprocessed.

---

## Phase 7: User Story 5 - Contador de episodios en la cola (Priority: P3)

**Goal**: Insignia con el número de pendientes en el destino Cola, sin incluir el episodio en curso.

**Independent Test**: Añadir 3, quitar 1, vaciar y comprobar que la insignia pasa de 3 a 2 a invisible.

### Tests for User Story 5

- [X] T042 [P] [US5] Write failing tests for pending count, zero and `99+` formatting in `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/QueueBadgeTest.kt`.
- [X] T043 [P] [US5] Write failing ViewModel tests for queue changes, playing-id exclusion and refresh failure in `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/queue/QueueBadgeViewModelTest.kt`.
- [X] T044 [P] [US5] Extend `app/src/androidTest/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavigationBarTest.kt` for the badge, zero state and accessibility text.

### Implementation for User Story 5

- [X] T045 [US5] Implement `pendingCount` and `displayText` in `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/QueueBadge.kt`, resolving `data-model.md` `QueueSnapshot.items`, `QueueBadge.pendingCount`, `QueueBadge.displayText` and `QueueBadge.contentDescription`.
- [X] T046 [US5] Create the activity-scoped `QueueBadgeViewModel` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/queue/QueueBadgeViewModel.kt`, observing `data-model.md` `QueueSnapshot.revision`, `QueueSnapshot.limit` and `QueueSnapshot.items`.
- [X] T047 [US5] Render the badge around the Cola icon in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` without covering the label.
- [X] T048 [US5] Wire the badge ViewModel, playing id and `ON_RESUME`/podcast-mode refresh in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`.

**Checkpoint**: User Story 5 works and matches the queue screen count.

---

## Phase 8: User Story 6 - Descarga animada con porcentaje (Priority: P3)

**Goal**: El botón de descarga muestra el porcentaje real, se conserva al navegar y no duplica descargas.

**Independent Test**: Descargar un episodio, ver el porcentaje, completar, fallar y reintentar sin duplicados.

### Tests for User Story 6

- [X] T049 [P] [US6] Extend `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/DownloadsRepoTest.kt` with progress, unknown total, duplicate and re-monitor cases.
- [X] T050 [P] [US6] Write failing Compose tests for determined, indeterminate and completed states in `app/src/androidTest/java/com/izquierdojl/tolocharadio/core/ui/components/DownloadProgressButtonTest.kt`.

### Implementation for User Story 6

- [X] T051 [US6] Add the ephemeral `DownloadProgress` model and `StateFlow` to `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/DownloadsRepo.kt`, resolving `data-model.md` `DownloadProgress.episodeId`, `DownloadProgress.bytesDownloaded`, `DownloadProgress.totalBytes` and `DownloadProgress.fraction`.
- [X] T052 [US6] Extend `queryStatus`, `monitor` and `syncRunning` to read bytes/total, poll every second and re-monitor running downloads in `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/DownloadsRepo.kt`.
- [X] T053 [US6] Guard against duplicate downloads and clean progress on completion/failure in `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/DownloadsRepo.kt`.
- [X] T054 [US6] Create the shared `DownloadProgressButton` for the four `DownloadUiState` cases in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/DownloadProgressButton.kt`, resolving `data-model.md` `DownloadUiState`.
- [X] T055 [US6] Expose download progress from `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodeActionsViewModel.kt`.
- [X] T056 [US6] Add progress parameters and use `DownloadProgressButton` in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/EpisodeRow.kt`.
- [X] T057 [US6] Show “Descargando N %” in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/sheet/EpisodeSheet.kt`.
- [X] T058 [US6] Pass progress to the rows while preserving the `downloadsEnabled` gate in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/home/PodcastHomeScreen.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/show/PodcastShowScreen.kt` and `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/favorites/PodcastFavoritesScreen.kt`.

**Checkpoint**: User Story 6 works in rows and detail sheet.

---

## Phase 9: User Story 7 - Tarjetas de programa y episodio más legibles (Priority: P3)

**Goal**: Títulos a dos líneas garantizadas, metadatos legibles y acciones en fila propia bajo el texto.

**Independent Test**: Con títulos largos, comprobar dos líneas, acciones debajo, altura uniforme y letra al 200 %.

### Tests for User Story 7

- [X] T059 [P] [US7] Write failing Compose tests for title lines, action row, equal height and font scale in `app/src/androidTest/java/com/izquierdojl/tolocharadio/core/ui/components/PodcastCardsTest.kt`.

### Implementation for User Story 7

- [X] T060 [US7] Restructure `PodcastCard` with `minLines = 2` and a dedicated action row in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/PodcastRows.kt`, resolving `data-model.md` `CardPresentation.title`, `CardPresentation.metadata`, `CardPresentation.actions` and `CardPresentation.height`.
- [X] T061 [US7] Restructure `EpisodeCard` with `minLines = 2`, readable podcast metadata and play/favorite actions below the text in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/EpisodeCardUi.kt`.
- [X] T062 [US7] Verify and adjust grid spacing/height in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/home/PodcastHomeScreen.kt`, `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/subscriptions/SubscriptionsScreen.kt` and `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/favorites/PodcastFavoritesScreen.kt`.
- [X] T063 [US7] Preserve 48 dp touch targets and accessible descriptions in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/PodcastRows.kt` and `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/EpisodeCardUi.kt`.

**Checkpoint**: All user stories are independently functional.

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Validación final, accesibilidad y limpieza.

- [X] T064 Run the full local gate suite `./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` and fix any regression in `app/build.gradle.kts` context.
- [X] T065 Run `./gradlew connectedDebugAndroidTest` as the local pre-merge Compose gate and record device results in `specs/0047-izquierdojl-20261119-podcast-ux-polish/quickstart.md`; CI keeps the four unit/static gates.
- [X] T066 Execute manual scenarios 3.1–3.7 of `specs/0047-izquierdojl-20261119-podcast-ux-polish/quickstart.md` on API 26, 33 and 37.
- [X] T067 Pass TalkBack over top bar, drawer, badge, download button, cards and voice toggle; fix labels in `app/src/main/res/values/strings.xml`.
- [X] T068 Review new logs and messages for PII/technical leakage in `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt` and `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/VoiceBoostController.kt`.
- [X] T069 Remove dead code and unused imports from `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` and the new player files.
- [X] T070 Re-check the implementation against constitution v3.1.0 (navigation and quality gates) and record the result in `specs/0047-izquierdojl-20261119-podcast-ux-polish/plan.md`.
- [X] T071 Verify no Room schema, backup-rule, API/DTO or auth change in `app/src/main/java/com/izquierdojl/tolocharadio/data/local/TolochaDb.kt`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`, `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/api/PodcastsApi.kt` and `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/dto/PodcastDtos.kt`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: depends on T001; blocks all user stories.
- **User Stories (Phases 3–9)**: depend on Phase 2.
  - US1 and US2 (P1) can run in parallel once foundational is done.
  - US3 and US4 (P2) can run in parallel after foundational; US4 should start after US2 because both edit `PlaybackPlayerFactory.kt`.
  - US5, US6 and US7 (P3) depend on foundational; US5 and US6 integrate with files touched by US3, so run US3 first.
- **Polish (Phase 10)**: depends on all selected stories.

### User Story Dependencies

- **US1**: independent; no dependency on other stories.
- **US2**: independent functionally; shares `EpisodeTransport` with US1 (foundational) and touches `PlaybackPlayerFactory` before US4.
- **US3**: independent; touches the navigation graph and bottom bar.
- **US4**: independent; needs US2's factory changes to avoid conflicts.
- **US5**: independent functionally; reuses US3's navigation bar/graph.
- **US6**: independent; touches `EpisodeRow`, `EpisodeActionsViewModel` and podcast screens.
- **US7**: independent; shares the podcast screens with US6, so run after US6 or coordinate the edits.

### Within Each User Story

- Tests MUST exist and fail before implementation.
- Domain/pure logic before integration.
- UI wiring last.
- Story complete before moving to the next priority unless running in parallel by file ownership.

### Parallel Opportunities

- T002–T005 (foundational) can run in parallel.
- US1 tests T006–T007 can run in parallel.
- US2 test T012 and US3 tests T017–T019 can run in parallel.
- US4 tests T027–T030 can run in parallel.
- US5 tests T042–T044 can run in parallel.
- US6 tests T049–T050 can run in parallel.
- Different stories can be developed in parallel only if they do not edit the same file (see dependency notes above).

---

## Parallel Example: User Story 1

```bash
Task: "Write failing tests for resumePositionSec in EpisodeResumeTest.kt"
Task: "Write failing ViewModel tests for embedded/remote/completed/auto-advance in EpisodePlayerViewModelTest.kt"
```

## Parallel Example: User Story 4

```bash
Task: "Write failing DSP tests in VoiceBoostDspTest.kt"
Task: "Write failing policy tests in VoiceBoostPolicyTest.kt"
Task: "Write failing Compose tests in VoiceBoostToggleTest.kt"
Task: "Extend InstancePrefsTest.kt for voiceBoost"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 and Phase 2.
2. Complete US1 (T006–T011).
3. Stop and validate resume from queue independently.
4. Deliver/demo the MVP.

### Incremental Delivery

1. Foundational → US1 (MVP) → validate.
2. US2 → validate notification controls.
3. US3 → validate header/drawer/navigation.
4. US4 → validate voice boost.
5. US5 → validate queue badge.
6. US6 → validate download progress.
7. US7 → validate cards.
8. Polish → full gates, accessibility, quickstart.

### Parallel Team Strategy

1. Team completes Setup + Foundational.
2. Developer A: US1 + US2 (playback and notification).
3. Developer B: US3 (navigation) then US5 (badge).
4. Developer C: US4 (voice) after US2; then US6 + US7 (download and cards).

---

## Notes

- [P] = different files, no pending dependency.
- Tests are mandatory under constitution III; do not implement before the test fails.
- Every UI state has an accessible label; no PII in logs.
- Commit after each logical group, referencing spec 0047.
- Stop at each checkpoint to validate the story independently.

---

## Estado de ejecución (2026-11-19)

- **Completadas y verificadas**: T001–T081 (81/81), incluida la extensión de retención de descargas y las correcciones del potenciador/ANR.
- **Gates**: `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck`, `lintDebug`, `compileDebugAndroidTestKotlin` y `connectedDebugAndroidTest` en API 37 — en verde.
- **Validación manual**: audio del emulador, potenciador de voz, descargas, retención, notificación, reanudación, ficha de acciones compactas y revisión TalkBack — completada por la persona usuaria.
- **Sin cambios** en `PodcastsApi.kt`, DTOs, `TolochaDb.kt`, `backup_rules.xml` ni `data_extraction_rules.xml` (T071 verificado).
- **Extensión 2026-11-21**: T082–T098 completadas; T096 verificada manualmente en emulador por el usuario (2026-11-21).
- **Gates de la extensión**: `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck`, `lintDebug`, `compileDebugAndroidTestKotlin` y `connectedDebugAndroidTest` en API 37 — en verde.

---

## Phase 11: Extensión 2026-11-19 — Retención de descargas y correcciones

- [X] T072 Add the retention options, default and labels in `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/DownloadRetention.kt`.
- [X] T073 Persist `downloadRetentionDays` (default 30) in `app/src/main/java/com/izquierdojl/tolocharadio/data/local/InstancePrefs.kt`.
- [X] T074 Implement `cleanupExpired` with `DownloadManager.remove` + Room delete in `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/DownloadsRepo.kt`.
- [X] T075 Create `CleanupDownloadsUseCase` (keeps the playing episode) in `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/CleanupDownloadsUseCase.kt` and run it from `EpisodeActionsViewModel`.
- [X] T076 Add the Descargas > Retención selector to `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt` and `SettingsViewModel.kt`.
- [X] T077 Add retention and cleanup tests in `app/src/test/java/com/izquierdojl/tolocharadio/data/local/InstancePrefsTest.kt` and `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/DownloadsRepoTest.kt`.
- [X] T078 Validate the extension with the full gates and on the emulator.
- [X] T079 Fix the empty output buffer in `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/VoiceBoostAudioProcessor.kt` (`ByteBuffer` position before `flip`) and add `VoiceBoostAudioProcessorTest`.
- [X] T080 Remove the cleanup effect that looped in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` and move it to `EpisodeActionsViewModel`.
- [X] T081 Compact the episode sheet actions into one icon row (play, favorite, a continuación, al final, reproducido, descarga y descartar) in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/sheet/EpisodeSheet.kt`.

---

## Phase 12: Extensión 2026-11-21 — Búsqueda en Suscripciones (US8)

**Goal**: añadir a Suscripciones un buscador con ámbitos «Podcasts» (filtro local) y «Episodios» (búsqueda por título en las suscripciones), con término compartido, resultados que sustituyen a la lista, estados de carga/vacío/error, paginación y las mismas acciones de episodio.

**Independent Test**: con varias suscripciones, filtrar por nombre sin tildes/mayúsculas; buscar un episodio suscrito por título y comprobar que aparece con su programa; comprobar vacío, error y «Cargar más»; cambiar de ámbito y pulsar Atrás.

**Dependencias**: fases 1–11 completadas (suscripciones, `EpisodeRow`, player y acciones de episodio). No introduce API, DTOs, dependencias ni persistencia nuevos.

### Tests for User Story 8

- [X] T082 [P] [US8] Write failing domain tests for normalization and local filtering (case, accents, partial, blank, order preserved, special characters) in `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/SubscriptionSearchTest.kt`.
- [X] T083 [P] [US8] Write failing use-case tests for blank term (no network), delegation with offset/limit and error passthrough in `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/SubscriptionSearchUseCaseTest.kt`.
- [X] T084 [P] [US8] Write failing ViewModel tests for scope/term, search success, empty, error/retry, pagination, stale response discarded, scope change, clear, close/back and unsubscribe cleanup in `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/SubscriptionsViewModelTest.kt`.
- [X] T085 [P] [US8] Write failing Compose tests for the search bar (labels, scope selection, clear, submit, 48 dp and semantics) and result states (loading, empty, error/retry, content, load more) in `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/subscriptions/SubscriptionSearchBarTest.kt` and `SubscriptionSearchResultsTest.kt`.

### Implementation for User Story 8

- [X] T086 [US8] Implement `normalizeSearchText` and `filterSubscriptionsByTitle` in `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/SubscriptionSearch.kt`, using NFD + combining marks + `lowercase(Locale.ROOT)`.
- [X] T087 [US8] Add `SearchSubscriptionEpisodesUseCase` (trim/blank validation, delegate to `EpisodesRepo.searchEpisodes`) in `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/SearchSubscriptionEpisodesUseCase.kt`.
- [X] T088 [US8] Extend `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/SubscriptionsViewModel.kt` with `SubscriptionSearchScope`, `SubscriptionSearchUi`, `searchUi`, `setSearchTerm`, `setSearchScope`, `searchEpisodes`, `loadMoreEpisodes`, `closeEpisodeSearch`, `onEpisodeProgressChanged`, a generation guard and unsubscribe cleanup.
- [X] T089 [P] [US8] Add search strings in `app/src/main/res/values/strings.xml` and test tags in `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/TolochaTestTags.kt`.
- [X] T090 [P] [US8] Add the `SubscriptionEpisodeResult` model and the mapper from `PodcastEpisodeSearchItem` to `PodcastEpisode` + `PodcastSummary` in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/subscriptions/SubscriptionEpisodeResult.kt`.
- [X] T091 [US8] Implement the stateless `SubscriptionSearchBar` (field, segmented scope, clear/search, labels, 48 dp, 200 % font) in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/subscriptions/SubscriptionSearchBar.kt`.
- [X] T092 [US8] Implement the stateless `SubscriptionSearchResults` (loading, empty, error/retry, content, load more, `EpisodeRow`, `liveRegion`) in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/subscriptions/SubscriptionSearchResults.kt`.
- [X] T093 [US8] Add `SubscriptionSearchHost` in the same file to collect `EpisodePlayerViewModel`/`EpisodeActionsViewModel`, derive favorites/downloads, wire actions, `FavoriteFailureEffect` and `EpisodeSheet`, and update the episode state locally on `progressChanged`.
- [X] T094 [US8] Integrate the search bar/results into `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/subscriptions/SubscriptionsScreen.kt`: show only with subscriptions, filter the list after sorting, hide sort/view on results, `BackHandler`, empty message with term and no search when the list is empty.
- [X] T095 [US8] Run the full gates (`./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` and `compileDebugAndroidTestKotlin`) and fix regressions.
- [X] T096 [US8] Validate quickstart 3.9 on an emulator API 26+/37 with TalkBack and font scale 200 %. — verificado manualmente en emulador por el usuario (2026-11-21)
- [X] T097 [US8] Verify no changes to `PodcastsApi.kt`, DTOs, auth, database schema or dependencies, and record the result in the PR notes.
- [X] T098 [US8] Collapse the search controls behind a header search toggle placed to the left of subscription sorting, clear term/results on hide and animate the reveal in `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/subscriptions/SubscriptionsScreen.kt`.

**Checkpoint**: User Story 8 funcional y testeable de forma independiente; gates en verde y quickstart 3.9 verificado manualmente en emulador (T096).
