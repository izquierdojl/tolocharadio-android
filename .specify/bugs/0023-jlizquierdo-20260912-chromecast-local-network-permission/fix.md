# Bug Fix: Descubrimiento Cast bloqueado por falta de ACCESS_LOCAL_NETWORK (Android 17)

- **Slug**: 0023-jlizquierdo-20260912-chromecast-local-network-permission
- **Fixed**: 2026-09-12
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Se declara y solicita `ACCESS_LOCAL_NETWORK`, el permiso runtime que Android 17 (API 37) exige para acceder a la red local. Como el descubrimiento de Google Cast es mDNS (`_googlecast._tcp`), sin él el selector quedaba siempre vacío. La lógica de permisos por API level se extrajo a un helper puro `CastPermissions` (testeable) y el estado se refresca al volver a la app (`ON_RESUME`).

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/AndroidManifest.xml` | modified | Añadido `<uses-permission android:name="android.permission.ACCESS_LOCAL_NETWORK" />` |
| `app/src/main/java/com/izquierdojl/tolocharadio/core/util/CastPermissions.kt` | added | Selección de permisos por API level (31/33/37) + `areGranted` |
| `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` | modified | Usa `CastPermissions`; añade `LifecycleEventEffect(ON_RESUME)` y reevalúa el estado real del sistema; elimina las funciones privadas duplicadas |
| `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt` | modified | Avisa en log si faltan permisos de descubrimiento al inicializar el `CastContext` |
| `app/src/test/java/com/izquierdojl/tolocharadio/core/util/CastPermissionsTest.kt` | added test | Fija la matriz de permisos por API level (26-30, 31-32, 33-36, 37) |

## Diff Highlights

**AndroidManifest.xml**
```xml
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />
<!-- Android 17 (API 37): sin este permiso el mDNS de descubrimiento Cast queda bloqueado. -->
<uses-permission android:name="android.permission.ACCESS_LOCAL_NETWORK" />
```

**CastPermissions.kt**
```kotlin
fun required(sdkInt: Int = Build.VERSION.SDK_INT): List<String> =
    buildList {
        when {
            sdkInt >= Build.VERSION_CODES.TIRAMISU -> add(Manifest.permission.NEARBY_WIFI_DEVICES)
            sdkInt >= Build.VERSION_CODES.S -> add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (sdkInt >= Build.VERSION_CODES.CINNAMON_BUN) {
            add(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }

fun areGranted(context: Context): Boolean =
    required().all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
```

**TolochaNavGraph.kt**
```kotlin
LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    castPermissionsGranted = CastPermissions.areGranted(context)
}
LaunchedEffect(chromeVisible, castPermissionsGranted) {
    if (chromeVisible && !castPermissionsGranted) {
        val permissions = CastPermissions.required()
        if (permissions.isNotEmpty()) {
            castPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
```

**CastPlayerManager.kt**
```kotlin
private fun initCastContext() {
    if (!CastPermissions.areGranted(context)) {
        android.util.Log.w(
            "CastPlayerManager",
            "Faltan permisos de descubrimiento Cast: el selector no mostrará dispositivos",
        )
    }
    runCatching { ... }
}
```

## Tests Added or Updated

- `CastPermissionsTest`
  - `api 26 a 30 no requieren permiso runtime para descubrir Cast`
  - `api 31 y 32 usan ubicacion precisa`
  - `api 33 a 36 usan nearby wifi devices`
  - `api 37 anade acceso a red local` (regresión clave: incluye `ACCESS_LOCAL_NETWORK`)

## Local Verification

- `gradlew.bat testDebugUnitTest --tests "...CastPermissionsTest"` → **PASS**
- `gradlew.bat testDebugUnitTest assembleDebug` → **PASS** (BUILD SUCCESSFUL)
- `gradlew.bat ktlintCheck detekt lintDebug` → **PASS** (BUILD SUCCESSFUL)
- Manual checks: **pendiente**. El descubrimiento real de dispositivos no es verificable en emulador (NAT/mDNS); requiere un dispositivo físico con un receptor Cast en la misma Wi-Fi.

## Deviations from Assessment

- El helper se ubicó en `core/util/CastPermissions.kt` en lugar de `core/ui/util/CastPermissions.kt` (mencionado en el assessment): `core/util` ya existe (`UrlNormalizer`) y no es específico de UI. Cambio solo de ruta.
- Se añadió el refresco del estado en `ON_RESUME` (`LifecycleEventEffect`), no descrito explícitamente pero alineado con la remediación ("verificar/re-solicitar en `onResume`").
- No se añadió test de instrumentación ni se integró el output switcher (ruta A): se mantiene el `MediaRouteButton` actual (ruta B, la preferida en el assessment).

## Follow-ups

- **Verificación manual obligatoria en dispositivo físico** (Android 17) con un receptor Cast real: conceder "Dispositivos cercanos" y confirmar que el picker lista el dispositivo.
- **Decisión ruta A vs B**: evaluar migrar al output switcher / `MediaTransferReceiver` (recomendación de Google) para evitar el permiso amplio a medio plazo.
- Si el usuario aloja el servidor Tolocha en su LAN (`http://192.168.x.x`), `ACCESS_LOCAL_NETWORK` también es necesario para la conectividad base; conviene un aviso de racional compartido.
- Considerar `android:usesPermissionFlags="neverForLocation"` en `NEARBY_WIFI_DEVICES` para la ficha de Play.
