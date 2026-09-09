# Quickstart: Notification App Focus

**Feature**: 0016-jlizquierdo-20260909-notification-app-focus
**Date**: 20260909

## Overview

This document provides validation scenarios to verify the notification tap handling feature works correctly end-to-end.

## Prerequisites

1. **Android device or emulator** with API level 26 or higher
2. **TolochaRadio app** installed and configured
3. **Notification permissions** granted to the app
4. **Test notification system** available (adb or in-app testing)

## Setup

### 1. Enable Notification Channels

```bash
# Verify notification channels are created
adb shell dumpsys notification | grep -A5 "com.tolocharadio"
```

**Expected Output**:
- `playback_channel` - Playback Notifications
- `content_channel` - Content Notifications  
- `system_channel` - System Notifications

### 2. Grant Notification Permissions

```bash
# Grant notification permission (if needed)
adb shell pm grant com.tolocharadio android.permission.POST_NOTIFICATIONS
```

### 3. Clear Existing Notifications

```bash
# Clear all notifications for the app
adb shell service call notification 1 s16 com.tolocharadio
```

## Validation Scenarios

### Scenario 1: Playback Notification Tap (App in Background)

**Steps**:
1. Start playback of a station
2. Put app in background (press Home button)
3. Verify playback notification appears
4. Tap the playback notification

**Expected Result**:
- App comes to foreground
- Player screen is displayed
- Current station is shown
- Playback continues

**Validation Command**:
```bash
# Check if app is in foreground
adb shell dumpsys activity activities | grep -A5 "com.tolocharadio"
```

### Scenario 2: Content Notification Tap (App Not Running)

**Steps**:
1. Force stop the app
2. Send a content notification (via adb or test system)
3. Tap the content notification

**Expected Result**:
- App launches from scratch
- Relevant content screen is displayed
- Back stack is properly managed

**Validation Command**:
```bash
# Force stop app
adb shell am force-stop com.tolocharadio

# Send test notification
adb shell am broadcast -a com.tolocharadio.TEST_NOTIFICATION \
  --es notification_type "CONTENT" \
  --es notification_action "OPEN_CONTENT" \
  --es content_id "test_content_123"
```

### Scenario 3: System Notification Tap (Device Locked)

**Steps**:
1. Lock the device
2. Send a system notification
3. Tap the system notification

**Expected Result**:
- Device lock screen is shown
- User must unlock device
- After unlock, app opens to appropriate screen

**Validation Command**:
```bash
# Lock device
adb shell input keyevent 26

# Send test notification
adb shell am broadcast -a com.tolocharadio.TEST_NOTIFICATION \
  --es notification_type "SYSTEM" \
  --es notification_action "OPEN_INFO" \
  --es content_id "system_info"
```

### Scenario 4: Multiple Notification Types

**Steps**:
1. Send playback notification
2. Send content notification
3. Send system notification
4. Tap each notification in sequence

**Expected Result**:
- Each notification opens to correct screen
- Back stack is properly managed
- App state is preserved correctly

**Validation Command**:
```bash
# Send multiple notifications
adb shell am broadcast -a com.tolocharadio.TEST_NOTIFICATION \
  --es notification_type "PLAYBACK" \
  --es notification_action "OPEN_PLAYER" \
  --es content_id "station_123"

adb shell am broadcast -a com.tolocharadio.TEST_NOTIFICATION \
  --es notification_type "CONTENT" \
  --es notification_action "OPEN_CONTENT" \
  --es content_id "content_456"

adb shell am broadcast -a com.tolocharadio.TEST_NOTIFICATION \
  --es notification_type "SYSTEM" \
  --es notification_action "OPEN_INFO" \
  --es content_id "system_info"
```

### Scenario 5: Notification Tap Performance

**Steps**:
1. Start timing when notification is tapped
2. Measure time until app is fully loaded
3. Verify < 2 second response time

**Expected Result**:
- App opens within 2 seconds
- No ANR (Application Not Responding)
- Smooth transition to target screen

**Validation Command**:
```bash
# Measure app launch time
adb shell am start -W com.tolocharadio/.ui.MainActivity
```

## Test Automation

### 1. ADB Test Commands

```bash
# Test notification creation
adb shell am broadcast -a com.tolocharadio.TEST_NOTIFICATION \
  --es notification_type "PLAYBACK" \
  --es notification_action "OPEN_PLAYER" \
  --es content_id "test_station"

# Test notification tap simulation
adb shell input tap <x> <y>  # Tap on notification in status bar
```

### 2. UI Automator Tests

```kotlin
@Test
fun testPlaybackNotificationTap() {
    // Start playback and go to background
    // Verify notification appears
    // Tap notification
    // Verify player screen is shown
}

@Test
fun testContentNotificationTap() {
    // Force stop app
    // Send content notification
    // Tap notification
    // Verify content screen is shown
}
```

## Debugging

### 1. Check Notification Status

```bash
# List all notifications for the app
adb shell dumpsys notification | grep -A10 "com.tolocharadio"

# Check notification channels
adb shell dumpsys notification | grep -A5 "playback_channel"
```

### 2. Check App State

```bash
# Check if app is running
adb shell ps | grep com.tolocharadio

# Check app's current activity
adb shell dumpsys activity activities | grep -A5 "com.tolocharadio"
```

### 3. View Logs

```bash
# View app logs
adb logcat -s TolochaRadio

# View notification-related logs
adb logcat | grep -i "notification"
```

## Success Criteria Validation

| Criteria | Validation Method | Expected Result |
|----------|-------------------|-----------------|
| App opens within 2 seconds | Timing measurement | < 2000ms |
| Correct screen navigation | UI verification | Matches notification type |
| Device unlock required | Lock screen test | Security respected |
| No sensitive data in notifications | Notification inspection | Only public info |
| All notification types work | Type-by-type testing | 100% success rate |

## Troubleshooting

### Issue: Notification not appearing

**Check**:
1. Notification permissions granted
2. Notification channels created
3. App not in battery optimization

### Issue: App not opening from notification

**Check**:
1. PendingIntent flags correct
2. Intent filters in AndroidManifest.xml
3. Activity exported properly

### Issue: Wrong screen opened

**Check**:
1. Notification action mapping correct
2. Deep link patterns match
3. Navigation logic correct

## Next Steps

After validating all scenarios, proceed to `/speckit.tasks` to generate implementation tasks.
