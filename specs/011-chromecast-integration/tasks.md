# Tasks: Chromecast Integration

**Input**: Design documents from `/specs/011-chromecast-integration/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/cast-ui.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

All source code under `app/src/main/java/com/izquierdojl/tolocharadio/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add Cast SDK dependencies and configure the app for Chromecast

- [x] T001 Add `media3-cast:1.4.1` and `play-services-cast-framework` dependencies in `gradle/libs.versions.toml` and `app/build.gradle.kts`
- [x] T002 Create `cast/CastOptionsProvider.kt` implementing `OptionsProvider` with Default Media Receiver (CC1AD845) and media session disabled
- [x] T003 Add Cast SDK metadata to `app/src/main/AndroidManifest.xml` (OPTIONS_PROVIDER_CLASS_NAME)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core Cast infrastructure that MUST be complete before ANY user story can be implemented

- [x] T004 Create `cast/CastConnectionState.kt` enum (DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING) per data-model.md
- [x] T005 Create `cast/CastDeviceInfo.kt` data class and `cast/CastDeviceType.kt` enum per data-model.md
- [x] T006 Create `cast/CastPlayerState.kt` sealed interface (Local, Cast) per data-model.md
- [x] T007 Create `cast/CastPlayerManager.kt` singleton with ExoPlayer/CastPlayer switching logic, state transfer, and MediaSession.setPlayer() integration
- [x] T008 Update `di/PlayerModule.kt` to provide `CastPlayerManager` and wire it with existing `ExoPlayer` and `ActiveStationHolder`

**Checkpoint**: Foundation ready — CastPlayerManager can switch between local and Cast players. User story implementation can now begin.

---

## Phase 3: User Story 1 — Botón de Cast visible en la barra superior (Priority: P1) MVP

**Goal**: El usuario ve un botón de Chromecast en la TopAppBar que indica disponibilidad de dispositivos

**Independent Test**: Abrir la app y verificar que el icono de Chromecast aparece en la TopAppBar. Verificar que cambia de apariencia según disponibilidad de dispositivos.

### Implementation for User Story 1

- [x] T009 [US1] Add `MediaRouteButton` via `AndroidView` interop in TopAppBar actions in `core/ui/navigation/TolochaNavGraph.kt` and configure `CastButtonFactory.setUpMediaRouteButton()` for automatic device discovery (before ViewModeToggle and Servers button)

**Checkpoint**: Botón Cast visible en todas las pantallas principales. El SDK Cast gestiona automáticamente el discovery y el picker de dispositivos.

---

## Phase 4: User Story 2 — Conectar a Chromecast y enviar audio (Priority: P1)

**Goal**: El usuario puede transferir la reproducción de audio al Chromecast seleccionado

**Independent Test**: Reproducir una emisora, tocar el botón de Cast, seleccionar un Chromecast y verificar que el audio sale del dispositivo Chromecast.

### Implementation for User Story 2

- [x] T010 [US2] Implement `CastPlayerManager.connect()` method: create CastPlayer from Cast session, transfer state from ExoPlayer, prepare and start playback on Cast
- [x] T011 [US2] Implement `CastSessionListener.kt` to handle Cast session events (connected, disconnected, suspended) and update CastConnectionState
- [x] T012 [US2] Update `feature/player/PlayerViewModel.kt` to use `CastPlayerManager` for playback instead of direct `ExoPlayer` — delegate `play()`, `toggle()`, `stop()` through active player
- [x] T013 [US2] Implement station switch on Cast: when user plays a different station while Cast is connected, auto-play on CastPlayer (FR-004, clarification: auto-play on Chromecast)
- [x] T014 [US2] Respect `playable` precheck before transferring to Cast in `CastPlayerManager.connect()` (FR-005)
- [x] T015 [US2] Implement audio focus management in `CastPlayerManager.kt`: request/release audio focus when connecting/disconnecting Cast, handle focus loss gracefully (FR-009) — **Verified**: AudioManager.requestAudioFocus/abandonAudioFocusRequest implemented

**Checkpoint**: Audio transfers to Chromecast. Play/pause/stop controls work on Cast. Station switch continues on Cast.

---

## Phase 5: User Story 3 — Desconectar y volver al dispositivo local (Priority: P1)

**Goal**: El usuario puede desconectar del Chromecast y el audio vuelve automáticamente al dispositivo local

**Independent Test**: Conectar a Chromecast, reproducir audio, desconectar y verificar que el audio vuelve al dispositivo local.

### Implementation for User Story 3

- [x] T016 [US3] Implement `CastPlayerManager.disconnect()` method: transfer state from CastPlayer to ExoPlayer, prepare and resume local playback, release CastPlayer
- [x] T017 [US3] Implement automatic local resumption on unexpected disconnect (network loss, device off) in `CastSessionListener.kt` — no user intervention required (FR-008, clarification: automatic)
- [x] T018 [US3] Implement connection failure handling: Snackbar with "No se pudo conectar al dispositivo" and retry button action (reconnect attempt) in `feature/player/PlayerUi.kt` (FR-012)

**Checkpoint**: Manual and automatic disconnect work. Audio resumes locally within 3 seconds. Error Snackbar appears on connection failure.

---

## Phase 6: User Story 4 — Control de volumen del Chromecast (Priority: P2)

**Goal**: El usuario puede controlar el volumen del Chromecast desde la app

**Independent Test**: Conectar a Chromecast, reproducir audio y verificar que el volumen se controla desde la app.

### Implementation for User Story 4

- [x] T019 [US4] Add volume slider to `FullPlayerSheet` in `feature/player/PlayerUi.kt` that controls CastPlayer volume when Cast is connected
  *(SUPERSEDED por la spec 0037 FR-004 — converge 2026-09-20: se retiró el slider; las teclas y la barra del sistema controlan la salida activa de forma nativa con media3 1.11.0.)*
- [x] T020 [US4] Wire phone hardware volume buttons to CastPlayer volume when Cast is connected in `CastPlayerManager.kt`
  *(SUPERSEDED por la spec 0037 FR-004 — converge 2026-09-20: media3 1.11.0 gestiona el ruteo de teclas nativamente; no se requiere código propio.)*

**Checkpoint**: Volume slider visible in full player during Cast. Hardware volume buttons control Cast volume.

---

## Phase 7: User Story 5 — Estado visual claro de conexión Cast (Priority: P2)

**Goal**: El usuario distingue visualmente cuándo el audio se reproduce en Chromecast vs localmente

**Independent Test**: Conectar a Chromecast y verificar que la UI muestra claramente el estado de conexión y nombre del dispositivo.

### Implementation for User Story 5

- [x] T021 [US5] Add Cast indicator (icon + device name) to mini-player subtitle in `feature/player/PlayerUi.kt` `PanelIdentity` — replaces technical info when Cast is active (contract: cast-ui.md Component 2)
- [x] T022 [US5] Add Cast indicator to `FullPlayerSheet` in `feature/player/PlayerUi.kt` — show device name below station name with Cast icon (depends on T021) (contract: cast-ui.md Component 3)
- [x] T023 [US5] Update `PlayerState` sealed interface or add `CastPlayerState` observation in `PlayerViewModel.kt` to expose Cast connection metadata to UI

**Checkpoint**: Mini-player and full player show Cast indicator with device name when connected. Normal state shows no Cast indicator.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final integration, edge cases, and validation

- [x] T024 Ensure `RadioPlaybackService.kt` MediaSession dynamically switches player via `MediaSession.setPlayer()` when Cast connects/disconnects
- [x] T025 Verify notification reflects Cast state correctly — station name, play/pause controls work on CastPlayer
- [x] T026 Handle edge case: multiple Chromecasts on network — switching between devices disconnects current and connects to new
- [x] T027 Handle edge case: app closure while Cast is playing — standard Cast SDK behavior (audio continues or stops per receiver)
- [x] T028 Run quickstart.md validation scenarios to verify all acceptance criteria

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Phase 2 — Cast button visibility
- **US2 (Phase 4)**: Depends on Phase 2 — Core Cast playback
- **US3 (Phase 5)**: Depends on Phase 2 — Disconnect handling
- **US4 (Phase 6)**: Depends on Phase 4 (Cast connected) — Volume control
- **US5 (Phase 7)**: Depends on Phase 4 (Cast connected) — Visual indicators
- **Polish (Phase 8)**: Depends on all user stories complete

### User Story Dependencies

- **US1**: Can start after Phase 2 — No dependencies on other stories
- **US2**: Can start after Phase 2 — No dependencies on US1 (button already works via SDK)
- **US3**: Can start after Phase 2 — No dependencies on US1/US2
- **US4**: Depends on US2 (Cast connected state) for volume control target
- **US5**: Depends on US2 (Cast connected state) for visual indicators

### Within Each User Story

- Models/enums before services
- Services before ViewModel/UI
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- Phase 1 tasks (T001, T002, T003) can run in parallel after T001 (dependencies)
- Phase 2 data model tasks (T004, T005, T006) can run in parallel
- US1 (T009) — single task, no parallel needed
- US2 implementation tasks sequential (T010 → T011 → T012 → T013 → T014 → T015)
- US3 tasks (T016, T017, T018) can run in parallel
- US5 tasks (T021 → T022 sequential, T023 parallel with both)

---

## Parallel Example: Phase 2 (Foundational)

```bash
# Launch data model tasks in parallel:
Task: "Create CastConnectionState enum in cast/CastConnectionState.kt"
Task: "Create CastDeviceInfo data class in cast/CastDeviceInfo.kt"
Task: "Create CastPlayerState sealed interface in cast/CastPlayerState.kt"
# Then sequential:
Task: "Create CastPlayerManager singleton in cast/CastPlayerManager.kt"
Task: "Update PlayerModule to provide CastPlayerManager"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (dependencies, CastOptionsProvider, manifest)
2. Complete Phase 2: Foundational (CastPlayerManager, data model)
3. Complete Phase 3: User Story 1 (Cast button visible)
4. **STOP and VALIDATE**: Verify Cast button appears and device picker works
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US1 → Cast button visible → Test independently
3. Add US2 → Audio transfers to Cast → Test independently
4. Add US3 → Disconnect works → Test independently (MVP complete!)
5. Add US4 → Volume control → Test independently
6. Add US5 → Visual indicators → Test independently
7. Polish → Edge cases validated

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: US1 (Cast button) + US5 (visual indicators)
   - Developer B: US2 (connect + transfer) + US3 (disconnect)
   - Developer C: US4 (volume control)
3. Stories complete and integrate independently

---

## Phase 9: Convergence

**Origen**: `/speckit.converge` 2026-09-20 — 12 FR, 5 historias y 5 principios revisados contra el código. `MediaRouteButton`, Default Receiver, desactivación de media session/notificaciones del SDK, indicadores de dispositivo y reanudación local conformes; dos huecos.

- [X] T029 Anotar el supersede de la 0037 en `specs\011-chromecast-integration\spec.md`: marcar US4 (historia completa, AC2 "el control de volumen está disponible en el mini-player") y las tareas T019/T020 en `tasks.md` como SUPERSEDED por la spec 0037 FR-004 (la app ya no muestra control de volumen in-app; las teclas y la barra del sistema controlan la salida activa de forma nativa con media3 1.11.0) per US4/AC2 (contradicts) — MEDIUM
- [X] T030 Completar FR-012 en `app/src/main/java/com/izquierdojl/tolocharadio/feature\player\PlayerUi.kt` (`LaunchedEffect` de `castConnectionState`, ~línea 88): al mostrar "No se pudo conectar al dispositivo" añadir acción de reintento que vuelva a intentar la conexión al dispositivo (o, si se decide no ofrecerla, anotar la excepción en `specs\011-chromecast-integration\spec.md` FR-012) per FR-012 (partial) — MEDIUM
