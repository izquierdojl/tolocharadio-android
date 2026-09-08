# Bug Fix: Add missing Cast discovery permissions

- **Slug**: chromecast-no-devices-found
- **Fixed**: 2026-09-08
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Added the four Android permissions required by the Cast SDK for mDNS device discovery (`ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, `ACCESS_FINE_LOCATION`, `NEARBY_WIFI_DEVICES`) and implemented a runtime permission request flow that fires automatically when the TopAppBar becomes visible. Also added error logging to `CastPlayerManager.initCastContext()` to surface initialization failures instead of silently swallowing them.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/AndroidManifest.xml` | modified | Added `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, `ACCESS_FINE_LOCATION`, `NEARBY_WIFI_DEVICES` permissions |
| `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` | modified | Added `requiredCastPermissions()` and `areCastPermissionsGranted()` helpers; added `rememberLauncherForActivityResult` + `LaunchedEffect` to request missing permissions on first TopAppBar display |
| `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt` | modified | Added `Log.e` to `onFailure` in `initCastContext()` |

## Diff Highlights

**AndroidManifest.xml** — four new permissions added before foreground service permissions:
```xml
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />
```

**TolochaNavGraph.kt** — permission request on TopAppBar visible:
```kotlin
val castPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions(),
) { results ->
    castPermissionsGranted = results.values.all { it }
}
LaunchedEffect(chromeVisible) {
    if (chromeVisible && !castPermissionsGranted) {
        val perms = requiredCastPermissions()
        if (perms.isNotEmpty()) {
            castPermissionLauncher.launch(perms.toTypedArray())
        }
    }
}
```

**TolochaNavGraph.kt** — API-level-aware permission selection:
```kotlin
private fun requiredCastPermissions(): List<String> =
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            listOf(Manifest.permission.NEARBY_WIFI_DEVICES)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        else -> emptyList()
    }
```

**CastPlayerManager.kt** — error logging added:
```kotlin
}.onFailure { e ->
    android.util.Log.e("CastPlayerManager", "Failed to initialize CastContext", e)
}
```

## Tests Added or Updated

No automated tests added — the fix is at the Android permission layer which requires instrumentation tests on a real device. Manual verification is the primary test path (see below).

## Local Verification

- Commands run: `gradlew :app:compileDebugKotlin` → failed (JAVA_HOME not set in this environment)
- Manual checks: none (requires Android device with Cast-compatible hardware)

## Deviations from Assessment

None. The fix follows the preferred remediation exactly: manifest permissions added, runtime permission flow implemented, and error logging improved in `initCastContext()`.

## Follow-ups

- **Xiaomi/MIUI users**: Even with permissions declared, MIUI may require users to manually enable "Nearby devices" in app settings. Consider adding a troubleshooting FAQ or in-app hint if no Cast devices are found after granting permissions.
- **Play Store review**: `ACCESS_FINE_LOCATION` triggers a sensitive-permissions review. Prepare a privacy policy declaration explaining the permission is used solely for Cast device discovery, not location tracking.
- **Consider removing `ACCESS_FINE_LOCATION` for API 33+**: On Android 13+, `NEARBY_WIFI_DEVICES` is sufficient. The current manifest declares both, but the runtime flow only requests the API-appropriate one. The manifest entry for `ACCESS_FINE_LOCATION` could be gated with `android:maxSdkVersion="32"` if desired.
