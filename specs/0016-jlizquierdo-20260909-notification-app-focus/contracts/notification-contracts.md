# Notification Contracts

**Feature**: 0016-jlizquierdo-20260909-notification-app-focus
**Date**: 20260909

## Overview

This document defines the contracts for notification handling in TolochaRadio Android app.

## Intent Contracts

### 1. Notification Tap Intent

**Action**: `com.tolocharadio.NOTIFICATION_TAP`

**Extras**:
- `notification_type` (String): Type of notification (PLAYBACK, CONTENT, SYSTEM)
- `notification_action` (String): Action to perform (OPEN_PLAYER, OPEN_CONTENT, OPEN_INFO, OPEN_MAIN)
- `content_id` (String, optional): ID of content to navigate to
- `notification_title` (String): Notification title
- `notification_message` (String): Notification message

**Flags**:
- `FLAG_ACTIVITY_NEW_TASK`: Required when launching from background
- `FLAG_ACTIVITY_CLEAR_TOP`: Clear top of back stack
- `FLAG_ACTIVITY_SINGLE_TOP`: Don't create new instance if already on top

**Example**:
```kotlin
Intent(context, MainActivity::class.java).apply {
    action = "com.tolocharadio.NOTIFICATION_TAP"
    putExtra("notification_type", "PLAYBACK")
    putExtra("notification_action", "OPEN_PLAYER")
    putExtra("content_id", "station_123")
    putExtra("notification_title", "Now Playing")
    putExtra("notification_message", "Radio Tolocha")
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
            Intent.FLAG_ACTIVITY_CLEAR_TOP or 
            Intent.FLAG_ACTIVITY_SINGLE_TOP
}
```

### 2. Playback Notification Intent

**Action**: `com.tolocharadio.PLAYBACK_NOTIFICATION`

**Extras**:
- `station_id` (String): ID of currently playing station
- `station_name` (String): Name of currently playing station
- `is_playing` (Boolean): Whether playback is active

**Purpose**: Specialized intent for playback notifications with playback-specific data.

### 3. Content Notification Intent

**Action**: `com.tolocharadio.CONTENT_NOTIFICATION`

**Extras**:
- `content_type` (String): Type of content (STATION_UPDATE, RECOMMENDATION, etc.)
- `content_id` (String): ID of the content
- `content_title` (String): Title of the content

**Purpose**: Specialized intent for content notifications with content-specific data.

## PendingIntent Contracts

### 1. Notification PendingIntent

**Request Code**: Unique per notification type
**Flags**: `FLAG_IMMUTABLE` (required for Android 12+)
**Update Policy**: `FLAG_UPDATE_CURRENT` to reuse existing PendingIntent

**Implementation**:
```kotlin
val pendingIntent = PendingIntent.getActivity(
    context,
    requestCode,
    intent,
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
)
```

### 2. Action PendingIntent

**Purpose**: For notification actions (e.g., pause, skip)
**Request Code**: Unique per action
**Flags**: `FLAG_IMMUTABLE`

## Notification Channel Contracts

### 1. Playback Channel

**ID**: `playback_channel`
**Name**: "Playback Notifications"
**Description**: "Notifications about current playback"
**Importance**: `IMPORTANCE_LOW` (no sound, minimal interruption)

### 2. Content Channel

**ID**: `content_channel`
**Name**: "Content Notifications"
**Description**: "Notifications about new content"
**Importance**: `IMPORTANCE_DEFAULT` (standard notification behavior)

### 3. System Channel

**ID**: `system_channel`
**Name**: "System Notifications"
**Description**: "System messages and alerts"
**Importance**: `IMPORTANCE_HIGH` (prominent notification)

## Navigation Contracts

### 1. Deep Link Patterns

**Player Screen**: `tolocharadio://player/{stationId}`
**Content Screen**: `tolocharadio://content/{contentType}/{contentId}`
**Info Screen**: `tolocharadio://info/{infoType}`
**Main Screen**: `tolocharadio://main`

### 2. Back Stack Management

**TaskStackBuilder**: Used to create proper back stack for notification navigation.

**Example**:
```kotlin
val stackBuilder = TaskStackBuilder.create(context).apply {
    addParentStack(MainActivity::class.java)
    addNextIntent(notificationIntent)
}
val pendingIntent = stackBuilder.getPendingIntent(
    requestCode,
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
)
```

## Security Contracts

### 1. Lock Screen Handling

**Check**: `KeyguardManager.isKeyguardLocked()`
**Behavior**: If locked, require unlock before opening app (except for playback notifications when audio is playing)

### 2. PendingIntent Security

**Flag**: `FLAG_IMMUTABLE` (required for Android 12+)
**Scope**: Only grant necessary permissions to PendingIntent

### 3. Data Privacy

**Rule**: Only public information in notification data
**Allowed**: Station name, playback status, content titles
**Prohibited**: User data, authentication tokens, personal information

## Error Handling Contracts

### 1. Invalid Notification Data

**Behavior**: Log error, don't show notification
**Fallback**: None (fail silently)

### 2. Navigation Failure

**Behavior**: Navigate to main screen
**Fallback**: Show error message if main screen also fails

### 3. Missing Content

**Behavior**: Show appropriate message, navigate to main screen
**Fallback**: None (content no longer available)

## Testing Contracts

### 1. Unit Test Contracts

**Notification Data**: Test creation and validation
**Navigation Logic**: Test navigation based on notification type and action
**State Handling**: Test behavior for different app states

### 2. Integration Test Contracts

**Notification Creation**: Test notification creation with proper channels
**PendingIntent**: Test PendingIntent creation and flags
**Deep Links**: Test deep link pattern matching

### 3. UI Test Contracts

**Notification Tap**: Test notification tap behavior
**Navigation**: Test navigation to correct screens
**Back Stack**: Test back stack management

## Version History

- **v1.0** (20260909): Initial contract definitions
