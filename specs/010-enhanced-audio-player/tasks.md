# Tasks: Enhanced Audio Player UX

**Input**: Design documents from `/specs/010-enhanced-audio-player/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/station-info-ui.md

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

Base: `app/src/main/java/com/izquierdojl/tolocharadio/`

- `feature/player/` — Player UI, ViewModel, Service
- `core/ui/components/` — Shared UI components
- `di/` — Hilt modules
- `data/remote/dto/` — DTOs

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project verification and dependency check

- [x] T001 Verify build.gradle.kts has media3-session, media3-exoplayer, media3-ui dependencies at 1.4.1
- [x] T002 Verify AndroidManifest.xml has FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK, POST_NOTIFICATIONS permissions and RadioPlaybackService declaration with foregroundServiceType="mediaPlayback"

**Checkpoint**: Existing project structure verified — no new dependencies or manifest changes needed.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: ActiveStationHolder singleton — shared by US1 (notification) and US3 (persistence)

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 Create ActiveStationHolder @Singleton in feature/player/ActiveStationHolder.kt with fields: station (StationDto?), state (PlayerState enum: IDLE, BUFFERING, PLAYING, PAUSED, ERROR)
- [x] T004 Add @Inject constructor to ActiveStationHolder and provide via Hilt in di/PlayerModule.kt
- [x] T005 Update PlayerViewModel.kt: inject ActiveStationHolder, write station+state in play(), toggle(), stop(), cancelLoad(), and on playback state changes
- [x] T006 Update PlayerViewModel.kt init block: if activeStationHolder.station != null, sync state from ExoPlayer (isPlaying → Playing, playWhenReady=false → Paused, etc.) instead of defaulting to Idle

**Checkpoint**: Station persists across ViewModel recreation. US1 and US3 can now proceed.

---

## Phase 3: User Story 1 — Notificación nativa con controles multimedia (Priority: P1) 🎯 MVP

**Goal**: Notificación nativa con play/pause/stop que persiste al pausar

**Independent Test**: Reproducir emisora, minimizar app, verificar notificación con controles. Pausar → notificación persiste. Stop → notificación desaparece.

### Implementation for User Story 1

- [x] T007 [US1] Configure MediaSession media button preferences in RadioPlaybackService.kt onCreate() to include COMMAND_STOP action for notification stop button
- [x] T008 [US1] Override onTaskRemoved() in RadioPlaybackService.kt: only call stopSelf() if player state is Idle; do NOT stop if Paused (notification must persist)
- [x] T009 [US1] Verify notification shows station name and favicon via existing DefaultMediaNotificationProvider metadata mapping from MediaItem
- [x] T010 [US1] Test: play → minimize → verify notification with play/pause/stop. Pause → verify notification persists with play icon. Stop → verify notification disappears.

**Checkpoint**: US1 complete — notification with play/pause/stop, persists on pause, disappears on stop.

---

## Phase 4: User Story 3 — Panel flotante persistente al navegar (Priority: P1)

**Goal**: Mini-player reaparece con estado correcto al salir/volver a la app

**Independent Test**: Reproducir emisora, minimizar (home), volver a app → mini-player visible con estado correcto. Pausar, minimizar, volver → estado pausado.

### Implementation for User Story 3

- [x] T011 [US3] Verify PlayerViewModel init sync from ActiveStationHolder (T006) restores correct state: Playing when exoPlayer.isPlaying, Paused when playWhenReady=false, Buffering when STATE_BUFFERING
- [x] T012 [US3] Update PlayerViewModel.kt: clear ActiveStationHolder in stop() and cancelLoad() to prevent stale state on next app open
- [x] T013 [US3] Verify MiniPlayer composable in PlayerUi.kt correctly reflects restored state (no UI changes needed — existing code reads from state Flow)
- [x] T014 [US3] Test: play → home → reopen → mini-player shows Playing. Pause → home → reopen → shows Paused with play button. Stop → home → reopen → no mini-player.

**Checkpoint**: US3 complete — mini-player persists across app lifecycle. Bug fixed.

---

## Phase 5: User Story 2 — Información completa de emisora al tocar logo (Priority: P2)

**Goal**: Bottom sheet con info completa al tocar logo en mini-player o full-player

**Independent Test**: Reproducir emisora, tocar logo → sheet con nombre, país, idioma, tags, codec, bitrate, votos, clicks, homepage. Homepage link abre navegador.

### Implementation for User Story 2

- [x] T015 [P] [US2] Create StationInfoSheet.kt composable in feature/player/StationInfoSheet.kt: ModalBottomSheet with station info layout per contracts/station-info-ui.md
- [x] T016 [P] [US2] Implement StationInfoContent composable in feature/player/StationInfoSheet.kt: favicon (Coil AsyncImage 64dp), name, country, language, tags section
- [x] T017 [US2] Implement audio/stats sections in StationInfoSheet.kt: codec (uppercase), bitrate (kbps), votes (formatted), clickCount (formatted), lastCheckOk (OK/Con problemas)
- [x] T018 [US2] Implement homepage link section in StationInfoSheet.kt: clickable text with Intent.ACTION_VIEW, hidden if homepage is null
- [x] T019 [US2] Update PanelIdentity in PlayerUi.kt: change onOpen callback to show StationInfoSheet instead of FullPlayerSheet when tapping logo/identity area
- [x] T020 [US2] Update FullPlayerSheet in PlayerUi.kt: add logo tap handler to show StationInfoSheet (separate from expand/collapse behavior)
- [x] T021 [US2] Add accessibility: contentDescription on favicon ("Logo de {name}"), Role.Link on homepage with onClickLabel "Abrir sitio web"
- [x] T022 [US2] Test: play → tap logo → verify sheet shows all fields. Tap homepage → browser opens. Swipe down → sheet closes, playback continues.

**Checkpoint**: US2 complete — station info sheet with all fields, homepage link, dismissible without interrupting playback.

---

## Phase 6: User Story 4 — Experiencia visual consistente y profesional (Priority: P3)

**Goal**: Transiciones fluidas, estados claros, paleta Tema Tolocha consistente

**Independent Test**: Navegar por reproductor, verificar transiciones < 300ms sin parpadeos. Error muestra mensaje en español + retry.

### Implementation for User Story 4

- [x] T023 [P] [US4] Wrap MiniPlayer in PlayerUi.kt with AnimatedVisibility(enter = slideInVertically + fadeIn, exit = slideOutVertically + fadeOut) for smooth appear/disappear transitions
- [x] T024 [P] [US4] Wrap PanelMainAction icons in PlayerUi.kt with Crossfade for smooth state transitions (Playing ↔ Paused ↔ Buffering ↔ Error)
- [x] T025 [US4] Verify error state in PlayerUi.kt: message in Spanish (already in PlayerViewModel), MaterialTheme.colorScheme.error color, retry button with Refresh icon
- [x] T026 [US4] Verify all player surfaces use Tema Tolocha palette: Surface tonalElevation, MaterialTheme.colorScheme for text/icons, typography M3 (titleMedium, bodySmall)
- [x] T027 [US4] Verify StationInfoSheet.kt uses consistent theme: surface background, outlineVariant dividers, M3 typography
- [x] T028 [US4] Test: verify transitions are smooth (< 300ms), error states show Spanish message with retry, all colors match Tema Tolocha.

**Checkpoint**: US4 complete — visual consistency across all player states and components.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Edge cases, cleanup, validation

- [x] T029 Verify edge case: notification from previous session is cleaned up when app opens without active playback (ActiveStationHolder is null → Idle state)
- [x] T030 Verify edge case: app closed from recents (multitask) → playback stops, notification disappears (existing onTaskRemoved behavior)
- [x] T031 Verify edge case: stream error while navigating → mini-player shows persistent error state with retry button
- [x] T032 Run quickstart.md validation scenarios V1-V5 end-to-end
- [x] T033 Run Detekt + ktlint + Android Lint — fix any new warnings

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — immediate
- **Phase 2 (Foundational)**: Depends on Phase 1. BLOCKS all user stories.
- **Phase 3 (US1 - Notification)**: Depends on Phase 2
- **Phase 4 (US3 - Persistence)**: Depends on Phase 2. Can run in parallel with US1.
- **Phase 5 (US2 - Station Info)**: Depends on Phase 2. Can run in parallel with US1/US3.
- **Phase 6 (US4 - Visual)**: Depends on US1, US2, US3 being complete (polish layer)
- **Phase 7 (Polish)**: Depends on all user stories

### User Story Dependencies

- **US1 (P1)**: Depends on Phase 2 only — notification config is independent
- **US3 (P1)**: Depends on Phase 2 only — persistence fix is independent
- **US2 (P2)**: Depends on Phase 2 only — new sheet is independent
- **US4 (P3)**: Depends on US1 + US2 + US3 — polish applies to existing components

### Within Each User Story

- US1: T007 → T008 → T009 → T010 (sequential, same file)
- US3: T011 → T012 → T013 → T014 (sequential, same file)
- US2: T015 ∥ T016 (parallel, different composables) → T017 → T018 → T019 → T020 → T021 → T022
- US4: T023 ∥ T024 (parallel, different sections) → T025 → T026 → T027 → T028

---

## Parallel Example: User Story 2

```bash
# T015 and T016 can run in parallel (different composables in same file):
Task: "Create StationInfoSheet.kt ModalBottomSheet shell"
Task: "Create StationInfoContent composable with header section"

# After T015+T016, sequential for integration:
Task: "Implement audio/stats sections"
Task: "Implement homepage link"
Task: "Wire logo tap in MiniPlayer"
Task: "Wire logo tap in FullPlayerSheet"
```

---

## Implementation Strategy

### MVP First (User Story 1 + US3)

1. Complete Phase 1: Setup verification
2. Complete Phase 2: ActiveStationHolder (foundational)
3. Complete Phase 3: US1 — Notification with stop button
4. Complete Phase 4: US3 — Mini-player persistence fix
5. **STOP and VALIDATE**: Test notification + persistence independently
6. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 (Notification) → Test → Deploy (core improvement)
3. US3 (Persistence bug fix) → Test → Deploy (bug fix)
4. US2 (Station Info) → Test → Deploy (new feature)
5. US4 (Visual polish) → Test → Deploy (quality)
6. Polish → Final validation

---

## Notes

- All changes are within `app/` module — no new modules or dependencies
- `StationDto` fields already available — no backend changes needed
- `DefaultMediaNotificationProvider` already handles play/pause — only stop button and persistence need configuration
- ActiveStationHolder is the key shared piece — must complete before any user story
