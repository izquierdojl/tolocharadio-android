# Tasks: Notification App Focus

**Input**: Design documents from `/specs/0016-jlizquierdo-20260909-notification-app-focus/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Constitution requires tests for all domain/data/ViewModel logic. UI tests for critical flows.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Android app**: `app/src/main/java/com/tolocharadio/`
- **Tests**: `app/src/test/java/com/tolocharadio/` (unit), `app/src/androidTest/java/com/tolocharadio/` (instrumentation)
- **Resources**: `app/src/main/res/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [x] T001 [P] Create notification package structure in app/src/main/java/com/tolocharadio/ui/notification/
- [x] T002 [P] Create notification domain package in app/src/main/java/com/tolocharadio/domain/notification/
- [x] T003 [P] Create notification test packages in app/src/test/java/com/tolocharadio/domain/notification/ and app/src/test/java/com/tolocharadio/ui/notification/

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Create NotificationType enum in app/src/main/java/com/tolocharadio/domain/notification/NotificationType.kt
- [x] T005 Create NotificationAction enum in app/src/main/java/com/tolocharadio/domain/notification/NotificationAction.kt
- [x] T006 Create AppState enum in app/src/main/java/com/tolocharadio/domain/notification/AppState.kt
- [x] T007 Create NotificationData data class in app/src/main/java/com/tolocharadio/domain/notification/NotificationData.kt
- [x] T008 Create NotificationChannelConfig data class in app/src/main/java/com/tolocharadio/domain/notification/NotificationChannelConfig.kt
- [x] T009 Create NotificationChannels object in app/src/main/java/com/tolocharadio/domain/notification/NotificationChannels.kt with channel definitions
- [x] T010 Create NotificationHandler interface in app/src/main/java/com/tolocharadio/domain/notification/NotificationHandler.kt
- [x] T011 Create AppStateTracker interface in app/src/main/java/com/tolocharadio/domain/notification/AppStateTracker.kt
- [x] T012 Create NotificationValidator in app/src/main/java/com/tolocharadio/domain/notification/NotificationValidator.kt

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Open App from Notification (Priority: P1) 🎯 MVP

**Goal**: As a user, when I tap on a notification from TolochaRadio, I expect the app to open and bring me to the relevant content or screen.

**Independent Test**: Can be fully tested by sending a test notification and tapping it to verify the app opens correctly.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T013 [P] [US1] Unit test for NotificationData validation in app/src/test/java/com/tolocharadio/domain/notification/NotificationDataTest.kt
- [x] T014 [P] [US1] Unit test for NotificationValidator in app/src/test/java/com/tolocharadio/domain/notification/NotificationValidatorTest.kt
- [x] T015 [P] [US1] Unit test for AppStateTracker in app/src/test/java/com/tolocharadio/domain/notification/AppStateTrackerTest.kt

### Implementation for User Story 1

- [x] T016 [US1] Create NotificationMapper in app/src/main/java/com/tolocharadio/domain/notification/NotificationMapper.kt (depends on T004, T005, T007)
- [x] T017 [US1] Create HandleNotificationTapUseCase in app/src/main/java/com/tolocharadio/domain/notification/HandleNotificationTapUseCase.kt (depends on T010, T011, T012)
- [x] T018 [US1] Create NotificationViewModel in app/src/main/java/com/tolocharadio/ui/notification/NotificationViewModel.kt (depends on T017)
- [x] T019 [US1] Create NotificationNavigation in app/src/main/java/com/tolocharadio/ui/notification/NotificationNavigation.kt
- [x] T020 [US1] Update MainActivity to handle notification intents in app/src/main/java/com/tolocharadio/ui/MainActivity.kt (depends on T019)
- [x] T021 [US1] Add intent filter for NOTIFICATION_TAP action in app/src/main/AndroidManifest.xml

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Focus App from Background (Priority: P1)

**Goal**: As a user, when I receive a notification while TolochaRadio is playing in the background, tapping the notification should bring the app to the foreground with focus on the current playback or relevant content.

**Independent Test**: Can be tested by starting playback, putting app in background, receiving a notification, and tapping it to verify the app comes to foreground with proper focus.

### Tests for User Story 2

- [x] T022 [P] [US2] Unit test for background state handling in app/src/test/java/com/tolocharadio/domain/notification/BackgroundStateTest.kt
- [x] T023 [P] [US2] Integration test for playback notification tap in app/src/androidTest/java/com/tolocharadio/notification/PlaybackNotificationTest.kt

### Implementation for User Story 2

- [x] T024 [US2] Create BackgroundStateHandler in app/src/main/java/com/tolocharadio/domain/notification/BackgroundStateHandler.kt (depends on T011)
- [x] T025 [US2] Create PlaybackNotificationBuilder in app/src/main/java/com/tolocharadio/ui/notification/PlaybackNotificationBuilder.kt
- [x] T026 [US2] Update NotificationNavigation to handle playback focus in app/src/main/java/com/tolocharadio/ui/notification/NotificationNavigation.kt (depends on T024)
- [x] T027 [US2] Add playback notification channel creation in app/src/main/java/com/tolocharadio/ui/notification/NotificationChannelManager.kt

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Handle Different Notification Types (Priority: P2)

**Goal**: As a user, I expect different types of notifications (playback status, new content, system messages) to open the app to the appropriate screen when tapped.

**Independent Test**: Can be tested by triggering different notification types and verifying each opens the correct screen.

### Tests for User Story 3

- [x] T028 [P] [US3] Unit test for notification type routing in app/src/test/java/com/tolocharadio/domain/notification/NotificationTypeRoutingTest.kt
- [x] T029 [P] [US3] Integration test for content notification tap in app/src/androidTest/java/com/tolocharadio/notification/ContentNotificationTest.kt
- [x] T030 [P] [US3] Integration test for system notification tap in app/src/androidTest/java/com/tolocharadio/notification/SystemNotificationTest.kt

### Implementation for User Story 3

- [x] T031 [US3] Create ContentNotificationBuilder in app/src/main/java/com/tolocharadio/ui/notification/ContentNotificationBuilder.kt
- [x] T032 [US3] Create SystemNotificationBuilder in app/src/main/java/com/tolocharadio/ui/notification/SystemNotificationBuilder.kt
- [x] T033 [US3] Create NotificationTypeRouter in app/src/main/java/com/tolocharadio/domain/notification/NotificationTypeRouter.kt (depends on T004, T005)
- [x] T034 [US3] Update NotificationNavigation to handle all notification types in app/src/main/java/com/tolocharadio/ui/notification/NotificationNavigation.kt (depends on T033)
- [x] T035 [US3] Add content and system notification channels in app/src/main/java/com/tolocharadio/ui/notification/NotificationChannelManager.kt

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Security & Edge Cases

**Purpose**: Implement security requirements and handle edge cases

- [x] T036 [P] Create LockScreenHandler in app/src/main/java/com/tolocharadio/domain/notification/LockScreenHandler.kt
- [x] T037 [P] Create ForceStopHandler in app/src/main/java/com/tolocharadio/domain/notification/ForceStopHandler.kt
- [x] T038 [P] Create UnavailableContentHandler in app/src/main/java/com/tolocharadio/domain/notification/UnavailableContentHandler.kt
- [x] T039 Update NotificationHandler to use security handlers in app/src/main/java/com/tolocharadio/domain/notification/NotificationHandler.kt (depends on T036, T037, T038)
- [x] T040 Add KeyguardManager integration for lock screen detection in app/src/main/java/com/tolocharadio/ui/notification/NotificationNavigation.kt (depends on T036)

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [x] T041 [P] Add structured logging for notification events in app/src/main/java/com/tolocharadio/domain/notification/NotificationLogger.kt
- [x] T042 [P] Create notification error handling in app/src/main/java/com/tolocharadio/domain/notification/NotificationErrorHandler.kt
- [x] T043 [P] Add notification performance monitoring in app/src/main/java/com/tolocharadio/domain/notification/NotificationPerformanceMonitor.kt
- [x] T044 Update quickstart.md with actual validation results
- [x] T045 Run Android Lint and fix any warnings
- [x] T046 Run ktlint and Detekt and fix any errors
- [x] T047 Update documentation with notification handling details

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2)
- **Security & Edge Cases (Phase 6)**: Depends on User Stories completion
- **Polish (Phase 7)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently testable
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1/US2 but should be independently testable

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Unit test for NotificationData validation in app/src/test/java/com/tolocharadio/domain/notification/NotificationDataTest.kt"
Task: "Unit test for NotificationValidator in app/src/test/java/com/tolocharadio/domain/notification/NotificationValidatorTest.kt"
Task: "Unit test for AppStateTracker in app/src/test/java/com/tolocharadio/domain/notification/AppStateTrackerTest.kt"

# Launch all models for User Story 1 together:
Task: "Create NotificationMapper in app/src/main/java/com/tolocharadio/domain/notification/NotificationMapper.kt"
Task: "Create HandleNotificationTapUseCase in app/src/main/java/com/tolocharadio/domain/notification/HandleNotificationTapUseCase.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
