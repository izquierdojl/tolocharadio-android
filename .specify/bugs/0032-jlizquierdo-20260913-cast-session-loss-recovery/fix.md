# Bug Fix: Cast bloqueado tras perder la sesión (recuperación automática)

- **Slug**: 0032-jlizquierdo-20260913-cast-session-loss-recovery
- **Fixed**: 2026-09-13
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Tras perder una sesión Cast, la app ahora se recupera sola: recuerda la emisora y el dispositivo, y al reconectar el **mismo** dispositivo reenvía la emisora sin pasos manuales. Además, la parada manual ("DETENER ENVÍO") se distingue de una pérdida mediante el reason del SDK (`CASTING_STOPPED`) y reanuda en local (FR-008) sin mostrar el aviso falso, `retry()` funciona con y sin Cast, y el aviso muestra el nombre real del dispositivo.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastFallbackPolicy.kt` | modified | Tercera decisión `RESUME_LOCAL_PAUSED` para parada manual con remoto pausado |
| `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt` | modified | `userInitiated` por `CastReasonCodes.CASTING_STOPPED`; `lastDeviceName` real en avisos; `resumeLocalPlayback(play)` |
| `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` | modified | `lostStation`/`lostDeviceName`; reenvío al reconectar solo al mismo dispositivo; `retry()` robusto |
| `app/src/test/java/com/izquierdojl/tolocharadio/cast/CastFallbackPolicyTest.kt` | updated test | 4 casos con la nueva decisión |
| `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt` | updated test | 26 tests: reenvío al reconectar, no reenvío a otro dispositivo, retry con Cast idle |

## Diff Highlights

```kotlin
// CastPlayerManager: parada manual del SDK vs pérdida
val reasonCode = castContext?.getCastReasonCodeForCastStatusCode(error)
val userInitiated = userDisconnectRequested || reasonCode == CastReasonCodes.CASTING_STOPPED
```

```kotlin
// CastPlayerManager: reanuda o prepara local en pausa
when (CastFallbackPolicy.decide(userInitiated, remoteWasPlaying)) {
    RESUME_LOCAL -> resumeLocalPlayback(play = true)
    RESUME_LOCAL_PAUSED -> resumeLocalPlayback(play = false)
    DO_NOT_RESUME_LOCAL -> _notices.tryEmit(CastNotice.SessionLost(deviceName))
}
```

```kotlin
// PlayerViewModel: reenvío automático solo al mismo dispositivo perdido
if (state != CastConnectionState.CONNECTED) return@collect
val expectedDevice = lostDeviceName
val connectedDevice = (castPlayerManager.castState.value as? CastPlayerState.Cast)?.deviceName
lostStation = null; lostDeviceName = null
if (expectedDevice == null || connectedDevice == null || connectedDevice == expectedDevice) play(station)
```

## Tests Added or Updated

- `CastFallbackPolicyTest`: parada manual sonando/pausada y pérdida con/sin reproducción.
- `PlayerViewModelTest.sesion Cast perdida reenvia la emisora al reconectar` — reenvío automático al reconectar.
- `PlayerViewModelTest.sesion Cast perdida no reenvia a un dispositivo distinto` — salvaguarda de dispositivo.
- `PlayerViewModelTest.retry con Cast conectado en idle reenvia la emisora` — `retry()` no-op corregido.
- `PlayerViewModelTest.retry tras perdida de sesion arranca la reproduccion local` — sin Cast sigue local.

## Local Verification

- Commands run: `gradlew.bat testDebugUnitTest detekt ktlintCheck lintDebug assembleDebug` → **BUILD SUCCESSFUL**.
- Tests: `CastFallbackPolicyTest` 4/4; `PlayerViewModelTest` 26/26.
- Dispositivo (Redmi Note 10 + Cast "Dormitorio principal"), release `1.7.9-fix0032` (versionCode 18):
  - Pérdida por Wi-Fi (`code=2155 reason=9`) → aviso con "Dormitorio principal" y sin audio local.
  - Reconexión al **mismo** dispositivo → la emisora se reenvía sola (`PLAYING`, `volumeType=REMOTE`, panel "Sonando"; sin toques extra).
  - "DETENER ENVÍO" manual → `userInitiated=true code=2154 reason=2`, sin aviso falso y reanudación local (`volumeType=LOCAL`).
- Guard de dispositivo verificado por test unitario; en dispositivo se vio que un toque en el selector sobre otro Cast (TV LG) conectaba a ese dispositivo (el reenvío respeta el dispositivo conectado).

## Deviations from Assessment

- Se añadió, a raíz del feedback del usuario, la salvaguarda de "no reenviar a un dispositivo distinto al perdido" (el assessment no la contemplaba). El resto sigue lo propuesto.

## Follow-ups

- `code=2251` transitorio al reconectar mientras GMS tiene estado Cast atascado (se desbloqueó reiniciando Google Play Services en el móvil; también con reinicio del reloj). No controlable desde la app; considerar reinicio del proveedor Cast o esperar. Documentado en el `test.md`.
- Opcional: botón "Reconectar" explícito en el aviso, y auditoría del `release()` de `PlayerViewModel.onCleared()` sobre el singleton `CastPlayerManager` (reutilización tras liberar).
