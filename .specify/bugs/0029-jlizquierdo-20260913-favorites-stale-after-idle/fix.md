# Bug Fix: Favoritos no disponibles tras reposo prolongado

- **Slug**: 0029-jlizquierdo-20260913-favorites-stale-after-idle
- **Fixed**: 2026-09-13
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

La sesión autenticada no se recuperaba de forma fiable tras el reposo: el `TokenAuthenticator` no encontraba el servidor activo si `active_server_id` no estaba sincronizado (migrados de 0022 / editados), limpiaba la sesión ante fallos transitorios de red y nadie recargaba Favoritos/Historial al volver a primer plano; además la caché Room no se usaba ante errores de autenticación. Se sincroniza el servidor activo al arrancar, se conserva la sesión ante fallos transitorios, se recarga en `ON_RESUME` y se degrada a caché en errores de auth.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/core/network/TokenAuthenticator.kt` | modified | Solo 401/403 definitivos de refresh/login limpian la sesión; IOException/5xx se conservan para reintentar. Constantes HTTP + helper `isDefinitiveRejection`. |
| `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/FavoritesRepo.kt` | modified | `list()` cae a caché también con `DomainError.Unauthorized` (offline=true). |
| `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/HistoryRepo.kt` | modified | Mismo fallback a caché en errores de autenticación. |
| `app/src/main/java/com/izquierdojl/tolocharadio/domain/auth/AuthenticateServerUseCase.kt` | modified | Inyecta `TokenStore` y fija `setActiveServerId(server.id)` antes de asegurar la sesión (sana migrados/editados). |
| `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/AuthRepo.kt` | modified | `ensureSession` serializado con `Mutex` (single-flight) para no rotar el refresh en paralelo. |
| `app/src/main/java/com/izquierdojl/tolocharadio/MainActivity.kt` | modified | `onResume()`: si la sesión está `Idle`, re-lanza el auto-login con credenciales guardadas. |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModel.kt` | modified | `onForeground()` + guarda `loading` para no duplicar peticiones. |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/history/HistoryViewModel.kt` | modified | Mismo `onForeground()` con guarda de carga. |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt` | modified | `LifecycleEventEffect(ON_RESUME)` → `viewModel.onForeground()`. |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/history/HistoryScreen.kt` | modified | Igual que Favoritos. |
| `app/src/test/java/com/izquierdojl/tolocharadio/core/network/TokenAuthenticatorTest.kt` | tests added | Fallos transitorios conservan la sesión; refresh 503 cae a re-login. |
| `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/FavoritesRepoTest.kt` | tests added | 401 con caché → offline; 401 sin caché → `Unauthorized`. |
| `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/HistoryRepoTest.kt` | test added | 401 con caché → offline. |
| `app/src/test/java/com/izquierdojl/tolocharadio/domain/auth/AuthenticateServerUseCaseTest.kt` | tests added (nuevo) | Fija servidor activo y asegura sesión; sin servidor no toca credenciales. |

## Diff Highlights (optional)

```kotlin
// TokenAuthenticator: solo rechazo definitivo invalida la sesión
if (loginRejected || (!loginAttempted && refreshRejected)) {
    session.clear()
}
```

```kotlin
// AuthenticateServerUseCase: sana active_server_id de migrados/editados
tokens.setActiveServerId(server.id)
return authRepo.ensureSession(server.id, server.url)
```

```kotlin
// MainActivity.onResume: recupera la sesión al volver del reposo
if (sessionManager.state.value == SessionState.Idle) {
    lifecycleScope.launch { authenticateServer() }
}
```

## Tests Added or Updated

- `TokenAuthenticatorTest::fallo de red en refresh y login no limpia la sesion (reintentable)` — un doble IOException conserva `SessionState.Ready`.
- `TokenAuthenticatorTest::refresh rechazado y login con fallo de red conserva la sesion` — el 401 del refresh no invalida si el login no llegó a verificarse.
- `TokenAuthenticatorTest::refresh 503 cae al re-login con credenciales` — un 5xx es transitorio y usa credenciales guardadas.
- `FavoritesRepoTest::list 401 con cache devuelve cache offline` — degradación elegante en auth (FR-010/IV).
- `FavoritesRepoTest::list 401 sin cache devuelve Unauthorized` — el error accionable se mantiene sin caché (FR-006).
- `HistoryRepoTest::list 401 con cache devuelve offline` — misma garantía en Historial.
- `AuthenticateServerUseCaseTest::fija el servidor activo y asegura su sesion` y `::sin servidor de arranque no toca credenciales`.

## Local Verification

- Red (test-first): `.\gradlew.bat testDebugUnitTest --tests "…TokenAuthenticatorTest" --tests "…FavoritesRepoTest" --tests "…HistoryRepoTest"` → 4 fallos esperados en los tests nuevos.
- Green + gates: `.\gradlew.bat assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug` → BUILD SUCCESSFUL (JAVA_HOME = Android Studio JBR).
- Manual checks: no ejecutados (requiere dispositivo/emulador y servidor real); pendiente en `/speckit.bug.test`.
- Rama: `0029-jlizquierdo-20260913-favorites-stale-after-idle`.

## Deviations from Assessment

- La remediación preferida ofrecía como alternativa que `TokenAuthenticator` cayese al servidor de arranque de Room; se implementó la otra opción (sincronizar `active_server_id` en `AuthenticateServerUseCase`) para no añadir dependencia `core.network → data.local`. El guard de `getActiveServerId()` se mantiene.
- Para "recargar al volver a primer plano" se eligió `ON_RESUME` en pantallas + re-auth en `MainActivity` en lugar de observar `SessionManager.state` (opción "o" del assessment).
- Se añadió `Mutex` a `AuthRepo.ensureSession` (el assessment lo listaba como riesgo "si aplica") para evitar dobles refresh con token rotado.

## Follow-ups

- **403 de access caducado**: OkHttp solo invoca el `Authenticator` en 401. Si el backend responde 403 con el access expirado, la renovación no se dispara; confirmar el contrato del backend y, si aplica, añadir un interceptor que reintente una vez tras `ensureSession`.
- **Playback tras reposo**: `PlayerDataSourceFactory` usa un `OkHttpClient` propio sin `AuthInterceptor`/`TokenAuthenticator` y captura el token al crear el data source; conviene revisarlo en una spec/bug aparte (fuera del alcance de este fix).
- Verificar en dispositivo la reproducción exacta del reporte (proceso vivo vs. matado, Wi-Fi vs. datos) en `/speckit.bug.test`.
