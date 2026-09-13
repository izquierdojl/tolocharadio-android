# Bug Fix: Cast pierde la sesión y arranca audio local duplicado

- **Slug**: 0031-jlizquierdo-20260913-cast-desync-local-audio
- **Fixed**: 2026-09-13
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Se corrige la duplicidad de audio al perderse una sesión Cast: ya no se recrea/libera el `CastPlayer` al reanudar la sesión (ese `release()` terminaba la sesión recién reanudada) y, ante un fin de sesión no pedido por la app, ya no se arranca el reproductor local: se muestra un aviso accionable y el usuario decide reanudar en local (reintentar). La reanudación local automática queda solo para la desconexión manual de la propia app, que además para el receptor con `endCurrentSession(true)`.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastFallbackPolicy.kt` | added | Política pura de fallback (RESUME_LOCAL solo si la app detuvo el receptor y estaba sonando) |
| `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt` | modified | `createCastPlayer()` ya no libera al reanudar; `handleSessionEnd()` aplica la política; `CastNotice.SessionLost`; logs de sesión con códigos; `disconnect()` usa `endCurrentSession(true)` |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` | modified | Colecciona `notices`: sesión perdida → `PlayerState.Error` con aviso y emisora; `retry()` reanuda en local |
| `app/src/test/java/com/izquierdojl/tolocharadio/cast/CastFallbackPolicyTest.kt` | added test | 4 casos de la política |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` | updated test | Pérdida de sesión no arranca local + aviso; retry reanuda local |

## Diff Highlights

```kotlin
// CastPlayerManager: ya no se recrea el player en onSessionResumed
private fun createCastPlayer() {
    castError = null
    if (castPlayer == null) { ... }
    requestAudioFocus()
    updateCastState()
}
```

```kotlin
// CastPlayerManager: fin de sesión resuelto por política
private fun handleSessionEnd(userInitiated: Boolean, remoteWasPlaying: Boolean, deviceName: String) {
    _connectionState.value = CastConnectionState.DISCONNECTED
    releaseCastPlayer()
    when (CastFallbackPolicy.decide(userInitiated, remoteWasPlaying)) {
        CastFallbackDecision.RESUME_LOCAL -> resumeLocalPlayback()
        CastFallbackDecision.DO_NOT_RESUME_LOCAL ->
            _notices.tryEmit(CastNotice.SessionLost(deviceName))
    }
}
```

```kotlin
// CastFallbackPolicy: solo es seguro reanudar local si la app paró el receptor
userInitiated && remoteWasPlaying -> CastFallbackDecision.RESUME_LOCAL
else -> CastFallbackDecision.DO_NOT_RESUME_LOCAL
```

```kotlin
// PlayerViewModel: aviso accionable, sin arrancar ExoPlayer
PlayerState.Error(station, "Se perdió la conexión con $deviceName. Puede seguir sonando allí; " +
    "reintenta para escuchar en el móvil.")
```

## Tests Added or Updated

- `CastFallbackPolicyTest` — desconexión manual con/sin reproducción y pérdida de conexión con/sin reproducción.
- `PlayerViewModelTest.sesion Cast perdida muestra aviso accionable sin arrancar el player local` — Error con la emisora, mensaje con el nombre del dispositivo y cero arranques locales.
- `PlayerViewModelTest.retry tras perdida de sesion arranca la reproduccion local` — el reintento explícito vuelve a Buffering por la vía local (sin Cast).

## Local Verification

- Commands run: `gradlew.bat testDebugUnitTest detekt ktlintCheck lintDebug assembleDebug` → **BUILD SUCCESSFUL** (1m 54s).
- Tests: `CastFallbackPolicyTest` 4/4 OK; `PlayerViewModelTest` 23/23 OK (0 fallos/skips).
- Manual checks: ninguna en dispositivo todavía (pendiente `/speckit.bug.test`); verificado en fuentes de `media3-cast-1.4.1` que `CastPlayer.release()` ejecuta `sessionManager.endCurrentSession(false)`, causa de la muerte de la sesión al reanudar.

## Deviations from Assessment

1. **Desconexión manual del diálogo del sistema**: el assessment proponía distinguir "usuario" de "pérdida" y mantener la reanudación automática en la manual. No hay discriminador fiable en el SDK (`error` de `onSessionEnded` es un `CastStatusCodes` sin semántica pública clara; `onRouteUnselected` compite con el propio SDK). Se optó por lo conservador: **ningún fin de sesión arranca local automáticamente salvo `disconnect()` de la app** (`endCurrentSession(true)` para el receptor). Cambia FR-008 de spec 011 (la desconexión manual ahora requiere un toque en "reintentar"); a cambio se garantiza SC-005 en todos los caminos.
2. **`ActiveStationHolder` no se sincroniza con el estado Cast**: la decisión de fallback ya no depende del holder (usa el `isPlaying` real del `CastPlayer`), así que no fue necesario; queda como follow-up de precisión de UI.
3. **Sin cambios en `PlayerUi.kt`/`PanelHelpers.kt`**: el estado `Error` ya existente muestra mensaje + botón de reintentar en mini-player y reproductor completo; el aviso accionable se resuelve sin UI nueva.

## Follow-ups

- Verificar en dispositivo (bug 0031): sesión Cast de varios minutos, pérdida inducida (Wi-Fi/receptor) → sin audio local y aviso visible; reintentar suena en local; desconexión manual y cambio de emisora sin regresiones.
- Opcional: exponer un control de desconexión que llame a `disconnect()` (hoy solo existe el diálogo nativo).
- Opcional: sincronizar `ActiveStationHolder` con el estado Cast para subtítulo/UI (assessment, punto 3).
- Opcional: valorar "Reintentar conexión" (reconectar y parar el receptor) como acción adicional.
