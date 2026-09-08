# Bug Assessment: Chromecast button does not discover devices on network

- **Slug**: chromecast-no-devices-found
- **Created**: 2026-09-08
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

El botón enviar a Chromecast no encuentra dispositivos pese a haber disponibles en mi red. Tengo un despertador inteligente Xiaomi que funciona con otras apps y este no lo detecta. Revisar si es un problema de código, permisos, etc.

The user reports that the Cast button in the app does not discover any Cast-compatible devices on their network, even though they have a Xiaomi smart clock that works with other Cast-enabled apps.

## Symptom

The Cast button (MediaRouteButton) appears in the TopAppBar but tapping it shows no available devices. The user's Xiaomi smart clock is discoverable by other Cast apps but not by Tolocha Radio. Expected behavior: the device picker should list all Cast-compatible devices on the same network.

## Reproduction

1. Open the Tolocha Radio app on a device connected to the same WiFi network as a Cast-compatible device (e.g., Xiaomi smart clock).
2. Tap the Cast button in the TopAppBar.
3. Observe that no devices appear in the picker.
4. Open another Cast-enabled app (e.g., YouTube, Spotify) and verify the Xiaomi clock is discovered.

## Suspected Code Paths

- `app/src/main/AndroidManifest.xml:1-48` — Missing permissions required for Cast device discovery. The manifest declares only `INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, and `POST_NOTIFICATIONS`. Critically absent: `ACCESS_FINE_LOCATION`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, and `NEARBY_WIFI_DEVICES`.
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastOptionsProvider.kt:10-21` — CastOptions configuration uses `DEFAULT_MEDIA_RECEIVER_APPLICATION_ID`, which is correct for basic usage. No issues here.
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt:83-92` — `initCastContext()` uses `runCatching` which silently swallows initialization errors, masking potential permission-related failures.

## Root Cause Hypothesis

**Confidence: high**

The Android Cast SDK requires specific permissions to discover devices via mDNS (multicast DNS) on the local network. On Android 12+ (API 31+), `ACCESS_FINE_LOCATION` is mandatory for Cast device discovery because mDNS scanning is classified as a nearby device detection mechanism. On Android 13+ (API 33+), `NEARBY_WIFI_DEVICES` replaces the location permission for WiFi-based discovery.

The app's `AndroidManifest.xml` is missing all of these permissions:
- `ACCESS_FINE_LOCATION` — Required on Android 12-12L for mDNS/Bonjour device scanning
- `ACCESS_WIFI_STATE` — Required to query WiFi network state for Cast discovery
- `CHANGE_WIFI_MULTICAST_STATE` — Required to join multicast groups for mDNS
- `NEARBY_WIFI_DEVICES` — Required on Android 13+ as a replacement for location permission for nearby device discovery

Without these permissions, the Cast SDK cannot scan the network for devices, so the MediaRouteButton's device picker remains empty. The user's Xiaomi smart clock works with other apps because those apps have already been granted the necessary permissions.

Additionally, `CastPlayerManager.initCastContext()` wraps initialization in `runCatching` (line 84), which silently catches and discards any exceptions thrown during CastContext initialization. This means permission-related errors are swallowed without any logging or user feedback, making the issue harder to diagnose from logs alone.

## Proposed Remediation

**Preferred**: Add the missing permissions to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />
```

Then implement a runtime permission check flow before Cast discovery begins. On Android 12+, the app must request `ACCESS_FINE_LOCATION` at runtime before the Cast SDK can discover devices. On Android 13+, request `NEARBY_WIFI_DEVICES` instead. The permission request should be triggered when the user taps the Cast button for the first time (or when no devices are found after a timeout).

**Alternatives**:
- Use `ACCESS_COARSE_LOCATION` instead of `ACCESS_FINE_LOCATION` — This is sufficient for mDNS discovery on Android 12-12L, but `NEARBY_WIFI_DEVICES` is still needed on Android 13+. Trade-off: slightly less privacy-invasive, but still requires a runtime permission dialog.
- Add a `LocationRequest` with `ACCESS_FINE_LOCATION` only for Android 12-12L and rely solely on `NEARBY_WIFI_DEVICES` for Android 13+ — More granular but adds complexity with version-conditional logic.

**Files likely to change**:
- `app/src/main/AndroidManifest.xml` — Add missing permissions
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt` — Add runtime permission check before Cast initialization; improve error handling in `initCastContext()` (remove silent `runCatching` or add logging)
- `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` — Add permission request flow tied to Cast button tap (or show rationale dialog)

**Tests to add or update**:
- Unit test: verify `CastPlayerManager` emits appropriate state when permissions are not granted
- Integration test: verify MediaRouteButton triggers permission request on first tap when permissions are missing
- Manual test: verify device discovery works on Android 12, 13, and 14 with the added permissions

## Risks & Considerations

- **User experience**: Requesting `ACCESS_FINE_LOCATION` may confuse users ("Why does a radio app need location?"). Consider adding a rationale dialog explaining that location permission is required by Android to discover nearby Cast devices, not for actual location tracking.
- **Xiaomi/MIUI specifics**: Xiaomi devices have aggressive battery optimization and permission management. Even with manifest permissions declared, users may need to manually grant "Nearby devices" permission in MIUI settings. Consider adding a troubleshooting hint if no devices are found after permission grant.
- **Privacy**: The `ACCESS_FINE_LOCATION` permission triggers a Play Store "sensitive permissions" review. Document the necessity clearly for the store listing.
- **No regression**: The current Cast integration code (session management, player switching, disconnect handling) is well-implemented. The fix is purely at the permission layer and should not affect existing Cast behavior once devices are discoverable.

## Open Questions

- [NEEDS CLARIFICATION: Does the app need to support Android 11 and below? If minSdk is 26 (Android 8.0), the permission strategy differs for API 26-30 vs 31+ vs 33+.]
- [NEEDS CLARIFICATION: Should the app show a dedicated "Cast requires nearby devices permission" rationale dialog, or rely on the system permission dialog alone?]
