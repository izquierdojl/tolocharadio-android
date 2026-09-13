# Bug Verification: Cast bloqueado tras perder la sesión (recuperación automática)

- **Slug**: 0032-jlizquierdo-20260913-cast-session-loss-recovery
- **Tested**: 2026-09-13
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

Verificado en dispositivo (Redmi Note 10 + Cast "Dormitorio principal") con la release `1.7.9-fix0032` (versionCode 18): tras una pérdida de sesión, al reconectar el **mismo** dispositivo la emisora se reenvía automáticamente (sin toques) y suena en el reloj; la parada manual "DETENER ENVÍO" ahora se distingue de una pérdida (`userInitiated=true`), no muestra el aviso falso y reanuda en local. El aviso ya muestra el nombre real del dispositivo. Tests y gates en verde.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Repro post-fix (reenvío automático) | Cast "Dormitorio principal" + reproducir → Wi-Fi OFF → Wi-Fi ON → reconectar al mismo dispositivo | pass | Sesión OK y `PLAYING`/`volumeType=REMOTE` con KIIS **sin pasos extra** |
| Aviso con nombre real | Dump UI tras la pérdida | pass | "Se perdió la conexión con **Dormitorio principal**…" (antes decía "Chromecast") |
| Parada manual ("DETENER ENVÍO") | Diálogo Cast → DETENER ENVÍO | pass | `userInitiated=true code=2154 reason=2`; sin aviso de pérdida; reanuda local (`volumeType=LOCAL`) |
| Guard de dispositivo | Test unitario + observación en dispositivo | pass | No reenvía a un dispositivo distinto; un toque en el selector sobre otro Cast conecta a ese (comportamiento esperado) |
| Pérdida inesperada | Wi-Fi OFF con Cast sonando | pass | `userInitiated=false code=2155 reason=9`; receptor sigue sonando; móvil en silencio (0031 sigue OK) |
| Tests nuevos/actualizados | `gradlew.bat testDebugUnitTest` | pass | `CastFallbackPolicyTest` 4/4; `PlayerViewModelTest` 26/26 |
| Suite de regresión | `gradlew.bat testDebugUnitTest detekt ktlintCheck lintDebug assembleDebug` | pass | BUILD SUCCESSFUL |
| Instalación | `adb install -r` | pass | `versionCode=18`, `versionName=1.7.9-fix0032`, datos conservados |

## Output Excerpts

Pérdida inesperada (aviso con dispositivo real):

```
CastPlayerManager: Sesión Cast terminada: userInitiated=false code=2155 reason=9
UI: "Se perdió la conexión con Dormitorio principal. Puede seguir sonando allí; reintenta para escuchar en el móvil."
```

Reconexión al mismo dispositivo → reenvío automático (sin ningún toque):

```
SESION OK  (requestAudioFocus ... CastPlayerManager)
state=PlaybackState {state=PLAYING(3), position=18007, speed=1.0, ...}
volumeType=REMOTE, ...
metadata: description=102.7 KIIS FM, Tolocha Radio, Tolocha Radio
UI: "Sonando: 102.7 KIIS FM" + "Dormitorio principal"
```

Parada manual del usuario:

```
CastPlayerManager: Sesión Cast terminada: userInitiated=true code=2154 reason=2
state=PlaybackState {state=PLAYING(3), ...} / volumeType=LOCAL
(sin aviso de "Se perdió la conexión")
```

## Residual Risks

- **`code=2251` transitorio (GMS/receptor)**: tras la pérdida, la reconexión puede fallar con 2251 hasta que se libera el estado Cast del receptor/GMS; se desbloqueó reiniciando Google Play Services en el móvil (y en otra ocasión reiniciando el reloj). La app no puede forzarlo; una vez acepta la sesión, el reenvío es automático. Si reincide con frecuencia, abrir bug aparte para investigar mitigación (p. ej., reintento del proveedor Cast o UX de "Reconectar").
- **La reconexión sigue requiriendo la acción del usuario** (elegir el dispositivo en el diálogo Cast): la app no selecciona dispositivos por sí sola.
- **Reenvío solo al mismo dispositivo** (por diseño): si el usuario conecta a otro Cast, no se reenvía automáticamente; puede reproducir manualmente en él.
- **`error`/reason del SDK**: la discriminación depende de `CastReasonCodes.CASTING_STOPPED`; verificado con 2154→reason 2 en este dispositivo/SDK.
- **Deuda adyacente** no tocada: `release()` del singleton `CastPlayerManager` en `PlayerViewModel.onCleared()` y el `runBlocking` de `resumeLocalPlayback`/`connectToStation`.

## Recommendation

Cerrar el bug 0032: la recuperación tras perder la sesión funciona sin reinicios manuales de la app (reenvío automático al mismo dispositivo, parada manual correcta y aviso con el nombre real). Mantener como follow-up el 2251/GMS del receptor si vuelve a reproducirse.
