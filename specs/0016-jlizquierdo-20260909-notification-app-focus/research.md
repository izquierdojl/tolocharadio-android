# Research: Notification App Focus

**Feature**: 0016-jlizquierdo-20260909-notification-app-focus
**Date**: 20260909
**Status**: Complete

## Overview

This document consolidates research findings for implementing notification tap handling in TolochaRadio Android app.

## Research Tasks

### 1. Android Notification Handling Best Practices

**Decision**: Use Android's standard notification system with PendingIntent and TaskStackBuilder

**Rationale**: 
- Android's notification system is well-documented and provides consistent behavior
- PendingIntent ensures proper app launching from notifications
- TaskStackBuilder handles back stack navigation correctly

**Alternatives considered**:
- Custom notification handling: Rejected as it would reinvent Android's built-in functionality
- Third-party notification libraries: Rejected to avoid unnecessary dependencies

**Implementation approach**:
- Create NotificationCompat.Builder for each notification type
- Use PendingIntent with proper flags (FLAG_IMMUTABLE, FLAG_UPDATE_CURRENT)
- Implement NotificationChannel for Android 8.0+ (API 26+)

### 2. Notification Types and Channels

**Decision**: Create separate notification channels for each notification type

**Rationale**:
- Android 8.0+ requires notification channels for proper notification management
- Users can control notification settings per channel
- Provides better user experience and system integration

**Alternatives considered**:
- Single channel for all notifications: Rejected as it doesn't allow granular control
- No channels (legacy approach): Rejected as minSdk is 26

**Channels to create**:
1. `playback_channel` - For playback status notifications
2. `content_channel` - For new content notifications
3. `system_channel` - For system messages

### 3. App State Handling

**Decision**: Handle three app states: foreground, background, and not running

**Rationale**:
- Different states require different handling for optimal user experience
- Android provides different mechanisms for each state
- Proper state handling ensures consistent behavior

**Alternatives considered**:
- Only handle background state: Rejected as it doesn't cover all use cases
- Force app restart for all notifications: Rejected as it would disrupt foreground usage

**State handling approach**:
- **Foreground**: Update current UI or show in-app notification
- **Background**: Bring app to foreground with proper navigation
- **Not running**: Launch app with proper initialization

### 4. Navigation from Notifications

**Decision**: Use Android's Navigation component with deep links

**Rationale**:
- Navigation component provides consistent back stack management
- Deep links ensure proper navigation to specific screens
- Works well with Compose and existing app structure

**Alternatives considered**:
- Manual activity launching: Rejected as it doesn't handle back stack properly
- Custom navigation system: Rejected to avoid reinventing existing functionality

**Navigation targets**:
- Playback notifications → Player screen
- Content notifications → Relevant content screen
- System notifications → Information screen or main screen

### 5. Device Security Handling

**Decision**: Use Android's KeyguardManager to check lock screen state

**Rationale**:
- Android provides built-in APIs for security handling
- Respects user's device security settings
- Provides consistent behavior across different devices

**Alternatives considered**:
- Always require unlock: Rejected as it would be too restrictive
- Ignore lock screen: Rejected as it would violate security best practices

**Implementation approach**:
- Check KeyguardManager.isKeyguardLocked()
- If locked, use FLAG_SHOW_WHEN_LOCKED or require unlock based on notification type
- Respect user's security settings while providing good UX

### 6. Notification Data Structure

**Decision**: Use Parcelable data classes for notification content

**Rationale**:
- Parcelable is efficient for Android inter-process communication
- Works well with Intent extras
- Provides type safety

**Alternatives considered**:
- JSON serialization: Rejected as it's less efficient for Android
- Simple strings: Rejected as it doesn't provide structure

**Data structure**:
```kotlin
@Parcelize
data class NotificationData(
    val type: NotificationType,
    val title: String,
    val message: String,
    val contentId: String?,
    val action: NotificationAction
) : Parcelable

enum class NotificationType {
    PLAYBACK, CONTENT, SYSTEM
}

enum class NotificationAction {
    OPEN_PLAYER, OPEN_CONTENT, OPEN_INFO, OPEN_MAIN
}
```

### 7. Testing Strategy

**Decision**: Use combination of unit tests, integration tests, and UI tests

**Rationale**:
- Unit tests ensure notification logic correctness
- Integration tests verify notification handling with app states
- UI tests validate user-facing behavior

**Alternatives considered**:
- Only unit tests: Rejected as it doesn't verify end-to-end behavior
- Manual testing only: Rejected as it's not scalable or reliable

**Test categories**:
1. **Unit tests**: Notification data handling, navigation logic
2. **Integration tests**: Notification creation, PendingIntent handling
3. **UI tests**: Notification tap behavior, navigation verification

## Summary of Decisions

| Area | Decision | Rationale |
|------|----------|-----------|
| Notification system | Android's standard system | Well-documented, consistent |
| Channels | Separate channels per type | Granular user control |
| App states | Handle foreground/background/not running | Complete coverage |
| Navigation | Navigation component with deep links | Consistent back stack |
| Security | KeyguardManager for lock screen | Respects device security |
| Data structure | Parcelable data classes | Efficient, type-safe |
| Testing | Unit + integration + UI tests | Comprehensive coverage |

## Resolved NEEDS CLARIFICATION

All technical unknowns have been resolved through research:

1. ✅ Notification handling approach → Android's standard system
2. ✅ Channel configuration → Separate channels per notification type
3. ✅ App state handling → Foreground, background, not running
4. ✅ Navigation approach → Navigation component with deep links
5. ✅ Security handling → KeyguardManager for lock screen
6. ✅ Data structure → Parcelable data classes
7. ✅ Testing strategy → Unit + integration + UI tests

## Next Steps

Proceed to Phase 1: Design & Contracts
