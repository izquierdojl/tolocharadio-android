# Quickstart: Chromecast Integration

**Feature**: 011-chromecast-integration
**Date**: 2026-09-07

## Prerequisites

- Android device with Google Play Services installed (Cast SDK requires it)
- Chromecast device (TV, speaker, or dongle) on the same Wi-Fi network
- Tolocha Radio app installed and logged in

## Validation Scenarios

### Scenario 1: Cast Button Visibility

**Goal**: Verify the Cast button appears in the top bar

1. Open the app and log in
2. Navigate to any main screen (Explore, Favorites, History, My Stations, Settings)
3. **Expected**: Cast icon visible in the TopAppBar, next to Servers and View Mode buttons
4. **If no Chromecast on network**: Icon appears grayed/disabled
5. **If Chromecast on network**: Icon appears active/colored

### Scenario 2: Connect and Transfer Audio

**Goal**: Verify audio transfers to Chromecast

1. Select and play a radio station (mini-player appears)
2. Tap the Cast button in the TopAppBar
3. Select a Chromecast device from the picker
4. **Expected**: Audio stops on phone, starts playing on Chromecast
5. **Expected**: Mini-player shows "Reproduciendo en [device name]" with Cast icon

### Scenario 3: Control Playback on Chromecast

**Goal**: Verify play/pause/stop controls work remotely

1. With audio playing on Chromecast (from Scenario 2)
2. Tap pause in the mini-player
3. **Expected**: Audio pauses on Chromecast
4. Tap play in the mini-player
5. **Expected**: Audio resumes on Chromecast
6. Tap stop in the mini-player
7. **Expected**: Audio stops on Chromecast, mini-player disappears

### Scenario 4: Switch Station While Casting

**Goal**: Verify station switch continues on Chromecast

1. With audio playing on Chromecast
2. Navigate to Explore and select a different station
3. **Expected**: New station starts playing on Chromecast (not on phone)
4. **Expected**: Mini-player updates with new station name

### Scenario 5: Manual Disconnect

**Goal**: Verify audio returns to local device

1. With audio playing on Chromecast
2. Tap the Cast button in the TopAppBar
3. Select "Disconnect" or tap the connected device
4. **Expected**: Audio stops on Chromecast
5. **Expected**: Audio resumes on phone within 3 seconds
6. **Expected**: Mini-player shows normal state (no Cast indicator)

### Scenario 6: Unexpected Disconnect (Network Loss)

**Goal**: Verify automatic local resumption

1. With audio playing on Chromecast
2. Turn off Wi-Fi on the phone (or power off the Chromecast)
3. **Expected**: Audio stops on Chromecast
4. **Expected**: Audio resumes automatically on phone within 3 seconds
5. **Expected**: Mini-player shows normal state

### Scenario 7: Connection Failure

**Goal**: Verify error handling on failed connection

1. Start playing a station
2. Tap Cast button and select a device that's about to go offline
3. **Expected**: If connection fails, Snackbar appears with "No se pudo conectar al dispositivo"
4. **Expected**: Snackbar has retry button
5. **Expected**: Audio continues playing locally

### Scenario 8: Background Behavior

**Goal**: Verify notification reflects Cast state

1. With audio playing on Chromecast
2. Press home (minimize app)
3. **Expected**: Notification shows current station with play/pause controls
4. Disconnect Chromecast from another device or turn it off
5. **Expected**: Audio resumes on phone, notification updates

## Test Commands

```bash
# Build and install debug APK
./gradlew installDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (requires device/emulator)
./gradlew connectedDebugAndroidTest
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Cast button not visible | Ensure device has Google Play Services. Cast SDK requires it. |
| No devices found | Ensure phone and Chromecast are on the same Wi-Fi network. |
| Audio doesn't transfer | Check that the station's `playable` precheck passes (stream is online). |
| Audio duplicated | Ensure `CastOptionsProvider` has `setMediaSessionEnabled(false)`. |
| Controls don't work on Cast | Verify `MediaSession.setPlayer()` is called with the active CastPlayer. |
