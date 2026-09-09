# Data Model: Notification App Focus

**Feature**: 0016-jlizquierdo-20260909-notification-app-focus
**Date**: 20260909
**Status**: Complete

## Overview

This document defines the data model for notification handling in TolochaRadio Android app.

## Entities

### 1. NotificationData

Represents the data structure for a notification.

**Fields**:
- `type: NotificationType` - Type of notification (PLAYBACK, CONTENT, SYSTEM)
- `title: String` - Notification title (public information only)
- `message: String` - Notification message (public information only)
- `contentId: String?` - Optional ID of the content to navigate to
- `action: NotificationAction` - Action to perform when notification is tapped
- `timestamp: Long` - When the notification was created

**Validation rules**:
- `title` must not be empty
- `message` must not be empty
- `contentId` is optional but must be valid if provided
- `timestamp` must be positive

**Relationships**:
- Used by NotificationManager to create notifications
- Used by NotificationHandler to process notification taps

### 2. NotificationType

Enum representing the type of notification.

**Values**:
- `PLAYBACK` - Playback status notifications (currently playing, playback controls)
- `CONTENT` - New content notifications (station updates, recommendations)
- `SYSTEM` - System messages (maintenance, updates, alerts)

**State transitions**:
- No transitions - this is an immutable enum

### 3. NotificationAction

Enum representing the action to perform when a notification is tapped.

**Values**:
- `OPEN_PLAYER` - Navigate to the player screen
- `OPEN_CONTENT` - Navigate to specific content screen
- `OPEN_INFO` - Navigate to information screen
- `OPEN_MAIN` - Navigate to main screen

**State transitions**:
- No transitions - this is an immutable enum

### 4. AppState

Represents the current state of the application.

**Values**:
- `FOREGROUND` - App is in foreground and visible
- `BACKGROUND` - App is in background (may be playing audio)
- `NOT_RUNNING` - App is not running (process killed or force-stopped)

**State transitions**:
- `NOT_RUNNING` → `FOREGROUND` (app launched)
- `FOREGROUND` → `BACKGROUND` (app minimized)
- `BACKGROUND` → `FOREGROUND` (app brought to foreground)
- `BACKGROUND` → `NOT_RUNNING` (app killed by system)
- `FOREGROUND` → `NOT_RUNNING` (app force-stopped)

### 5. NotificationChannel

Represents an Android notification channel.

**Fields**:
- `id: String` - Unique channel identifier
- `name: String` - User-visible channel name
- `description: String` - User-visible channel description
- `importance: Int` - Notification importance level

**Validation rules**:
- `id` must be unique and follow Android naming conventions
- `name` must not be empty
- `description` should be descriptive
- `importance` must be one of Android's predefined levels

**Channels to create**:
1. `playback_channel` - "Playback Notifications" - "Notifications about current playback"
2. `content_channel` - "Content Notifications" - "Notifications about new content"
3. `system_channel` - "System Notifications" - "System messages and alerts"

## Relationships

```
NotificationData (1) → (1) NotificationType
NotificationData (1) → (1) NotificationAction
NotificationData (1) → (0..1) Content (via contentId)
AppState (1) → (0..*) NotificationData (notifications can exist in any state)
NotificationChannel (1) → (0..*) NotificationData (notifications belong to channels)
```

## Data Flow

1. **Notification Creation**:
   - App creates `NotificationData` with appropriate type and action
   - `NotificationManager` creates Android `Notification` using `NotificationCompat.Builder`
   - Notification is posted to appropriate `NotificationChannel`

2. **Notification Tap Handling**:
   - User taps notification
   - Android launches app with `PendingIntent`
   - `NotificationHandler` receives intent with `NotificationData`
   - `NotificationHandler` checks `AppState`
   - Based on state and action, navigates to appropriate screen

3. **State Management**:
   - `AppState` is tracked by application lifecycle
   - `NotificationHandler` uses `AppState` to determine behavior
   - Different handling for foreground, background, and not running states

## Storage

No persistent storage required for this feature. Notification data is transient and stored only in memory during notification lifecycle.

## Security Considerations

1. **Public Information Only**: NotificationData contains only public information (station name, playback status)
2. **No Sensitive Data**: No user data, authentication tokens, or personal information in notifications
3. **Device Lock Respect**: Use KeyguardManager to check lock screen state before opening app
4. **PendingIntent Security**: Use FLAG_IMMUTABLE for security on Android 12+

## Validation Summary

| Entity | Validation Rules |
|--------|-----------------|
| NotificationData | title not empty, message not empty, valid contentId if provided |
| NotificationChannel | unique id, non-empty name, valid importance |
| NotificationType | Must be valid enum value |
| NotificationAction | Must be valid enum value |
| AppState | Must be valid enum value |

## Implementation Notes

1. Use Kotlin data classes with `@Parcelize` annotation for efficient serialization
2. Use Android's `NotificationCompat.Builder` for backward compatibility
3. Use `TaskStackBuilder` for proper back stack management
4. Use `KeyguardManager` for lock screen detection
5. Use `PendingIntent.FLAG_IMMUTABLE` for Android 12+ security
