# Feature Specification: Notification App Focus

**Feature Branch**: `0016-jlizquierdo-20260909-notification-app-focus`

**Created**: 20260909

**Status**: Done (2026-09-10; todo verificado, notification tap fix via setSessionActivity)

**Input**: User description: "Al pulsar en notificación abrir app o establecer foco si está en segundo plano."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Open App from Notification (Priority: P1)

As a user, when I tap on a notification from TolochaRadio, I expect the app to open and bring me to the relevant content or screen.

**Why this priority**: This is the core functionality requested - tapping notifications should open the app. This provides immediate value by allowing users to quickly access the app from notifications.

**Independent Test**: Can be fully tested by sending a test notification and tapping it to verify the app opens correctly.

**Acceptance Scenarios**:

1. **Given** the app is not running, **When** user taps on a TolochaRadio notification, **Then** the app launches and displays the main screen or relevant content.
2. **Given** the app is in the background, **When** user taps on a TolochaRadio notification, **Then** the app comes to foreground and displays the relevant content.
3. **Given** the app is already in foreground, **When** user taps on a TolochaRadio notification, **Then** the app handles the notification appropriately (e.g., refreshes content or shows relevant screen).

---

### User Story 2 - Focus App from Background (Priority: P1)

As a user, when I receive a notification while TolochaRadio is playing in the background, tapping the notification should bring the app to the foreground with focus on the current playback or relevant content.

**Why this priority**: This is equally important as P1 because it addresses the specific scenario mentioned in the feature description - establishing focus when app is in background.

**Independent Test**: Can be tested by starting playback, putting app in background, receiving a notification, and tapping it to verify the app comes to foreground with proper focus.

**Acceptance Scenarios**:

1. **Given** the app is playing audio in background, **When** user taps on a playback-related notification, **Then** the app opens to the player screen showing current playback.
2. **Given** the app is in background with no active playback, **When** user taps on a general notification, **Then** the app opens to the main screen.

---

### User Story 3 - Handle Different Notification Types (Priority: P2)

As a user, I expect different types of notifications (playback status, new content, system messages) to open the app to the appropriate screen when tapped.

**Why this priority**: This enhances the user experience by ensuring notifications are contextually relevant and take users to the right place in the app.

**Independent Test**: Can be tested by triggering different notification types and verifying each opens the correct screen.

**Acceptance Scenarios**:

1. **Given** a playback notification is shown, **When** user taps it, **Then** the app opens to the player screen.
2. **Given** a new content notification is shown, **When** user taps it, **Then** the app opens to the relevant content screen.
3. **Given** a system message notification is shown, **When** user taps it, **Then** the app opens to an appropriate information screen.

---

### Edge Cases

- When the app is force-stopped by the user: Launch app from scratch (main screen).
- When the device is locked: Require device unlock before opening the app.
- When notification content is no longer available: Show appropriate message or navigate to main screen.
- When multiple notifications arrive in quick succession: Handle the most recent notification.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST open the app when user taps on any TolochaRadio notification.
- **FR-002**: System MUST bring the app to foreground if it's running in background when notification is tapped.
- **FR-003**: System MUST establish proper focus on relevant content when opening from notification.
- **FR-004**: System MUST handle different notification types (playback status, new content, system messages) and navigate to appropriate screens.
- **FR-005**: System MUST maintain app state when opening from background via notification.
- **FR-006**: System MUST work correctly when app is in different states (foreground, background, not running).
- **FR-007**: System MUST require device unlock before opening the app from notifications on locked devices.
- **FR-008**: System MUST only display public information in notifications (station name, playback status) - no sensitive user data.

### Key Entities

- **Notification**: Represents a message sent to the user with type, content, and action data.
- **NotificationAction**: Defines what happens when a notification is tapped (navigation target, content ID).
- **AppState**: Current state of the application (foreground, background, not running).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can open the app from a notification within 2 seconds of tapping.
- **SC-002**: App successfully focuses on relevant content in 95% of notification tap scenarios.
- **SC-003**: All notification types (playback status, new content, system messages) correctly navigate to appropriate screens.
- **SC-004**: App state is preserved when opening from background via notification in 100% of cases.
- **SC-005**: Device unlock is required before opening the app from notifications on locked devices in 100% of cases.
- **SC-006**: No sensitive user data is displayed in notifications in 100% of cases.

## Clarifications

### Session 2026-09-09

- Q: ¿Qué tipos de notificación específicos debe soportar TolochaRadio al pulsarlos? → A: Estado de reproducción, nuevo contenido y mensajes del sistema.
- Q: ¿Las notificaciones pueden mostrar información sensible como datos de usuario o historial? → A: No - solo información pública (nombre de emisora, estado de reproducción).
- Q: ¿Qué comportamiento debe tener la app cuando se pulsa una notificación en dispositivo bloqueado? → A: Requerir desbloqueo antes de abrir la app.
- Q: ¿Qué debe ocurrir cuando la app ha sido forzada a cerrar por el usuario y se pulsa una notificación? → A: Lanzar la app desde cero (pantalla principal).

## Assumptions

- Notifications are already implemented in the app using Android's notification system.
- The app uses standard Android activity navigation patterns.
- Notification channels are properly configured for different notification types (playback, content, system).
- The app has proper activity lifecycle management for handling different launch scenarios.
- Notifications only contain public information (station name, playback status) - no sensitive user data.
- Device unlock is required before opening the app from notifications on locked devices.
