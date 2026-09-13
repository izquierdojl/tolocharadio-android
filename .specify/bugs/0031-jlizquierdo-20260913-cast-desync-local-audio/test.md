# Bug Verification: Cast pierde la sesión y arranca audio local duplicado

- **Slug**: 0031-jlizquierdo-20260913-cast-desync-local-audio
- **Tested**: 2026-09-13
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

Verificado en dispositivo físico (Redmi Note 10, Android 17) con la build release `1.7.8-fix0031` (versionCode 16, firmada con release.keystore, instalada sobre `1.7.7-fix0030` conservando datos) contra el Cast "Dormitorio principal". Al cortar el Wi-Fi del teléfono con reproducción Cast activa: la sesión se perdió de forma no intencionada, **el receptor siguió sonando y el móvil se quedó en silencio** (sin audio local), mostrando el aviso accionable. El reintento explícito sí reanuda en local. Tests y gates en verde; sin regresiones observadas en el flujo normal de Cast.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproducción post-fix | Cast "Dormitorio principal" + reproducir; `adb shell svc wifi disable` 60 s | pass | Sesión terminada `userInitiated=false code=2155`; receptor siguió sonando; **móvil en silencio** (confirmado por el usuario); aviso visible |
| Aviso accionable | Dump UI tras la pérdida | pass | "Se perdió la conexión con Chromecast. Puede seguir sonando allí; reintenta para escuchar en el móvil." |
| Reintento → local | Tap en "Reintentar" (mini-player) | pass | `dumpsys media_session`: `state=PLAYING`, `volumeType=LOCAL`, metadata de la emisora; aviso despejado |
| Flujo Cast normal | Conectar + reproducir en Cast | pass | `state=PLAYING`, `volumeType=REMOTE`, mini-player con "Dormitorio" |
| Tests nuevos/actualizados | `gradlew.bat testDebugUnitTest` | pass | `CastFallbackPolicyTest` 4/4; `PlayerViewModelTest` 23/23; 0 fallos |
| Suite de regresión | `gradlew.bat testDebugUnitTest detekt ktlintCheck lintDebug assembleDebug` | pass | BUILD SUCCESSFUL (1m 54s) en la fase de fix |
| Lint / type-check | `gradlew.bat detekt ktlintCheck lintDebug` | pass | Sin issues nuevos |
| Instalación | `adb install -r app-release.apk` | pass | `versionCode=16`, `versionName=1.7.8-fix0031`, datos y sesión de servidor conservados |

## Output Excerpts

Pérdida de sesión (corte de Wi-Fi del teléfono a las 15:28:53):

```
15:28:55.689 MediaRouterCallback: onRouteUnselected with reason = 3, routeId = ...
15:28:55.692 CastPlayerManager: Sesión Cast terminada: userInitiated=false code=2155
15:28:55.696 MediaFocusControl: abandonAudioFocus() ... com.izquierdojl.tolocharadio.cast.CastPlayerManager
15:28:55.712 MediaSessionService: onSessionPlaybackStateChanged: ... playbackState={state=NONE(0), ...}
```

Estado de la sesión de la app justo después de la pérdida (sin reproducción local):

```
state=PlaybackState {state=NONE(0), position=0, speed=0.0, ...}
volumeType=LOCAL, controlType=FIXED, max=0, current=0
metadata: size=1, description=null, null, null
```

Aviso mostrado en la UI:

```
Se perdió la conexión con Chromecast. Puede seguir sonando allí; reintenta para escuchar en el móvil.
```

Reintento explícito (tap en "Reintentar"):

```
state=PlaybackState {state=PLAYING(3), position=2680, ...}
volumeType=LOCAL, controlType=FIXED, ...
metadata: size=9, description=SER - Radio Zaragoza, Tolocha Radio, Tolocha Radio
```

Confirmación auditiva del usuario: "Reloj sigue sonando y móvil en silencio".

## Residual Risks

- **Camino `onSessionResumed` no ejercitado en dispositivo**: el corte de Wi-Fi terminó la sesión (`code=2155`) en ~2 s en lugar de suspenderla/reanudarla, así que la reutilización del `CastPlayer` al reanudar (parte "reforzar sincronización") queda cubierta por revisión de código y tests de la política, no por reproducción real. No era el síntoma reportado.
- **Nombre del dispositivo genérico en el aviso**: `onSessionEnded` recibe la sesión ya desconectada (`session.castDevice == null`), así que el mensaje dice "Chromecast" en vez de "Dormitorio principal". Mejora de UX pendiente (recordar el último nombre conocido).
- **Desconexión manual desde el diálogo del sistema**: en la verificación 0031 mostraba el aviso y no reanudaba local (desviación intencionada); **resuelto en el bug 0032** (`userInitiated` por `CASTING_STOPPED`, reanuda local y sin aviso falso; verificado en dispositivo).
- **Arranque de sesión con el reloj Xiaomi**: falló con `code=2251` hasta reiniciar (desenchufar/enchufar) el reloj; era una sesión residual en el receptor, no relacionada con el fix. Puede repetirse si se fuerza el cierre de la app mientras emite Cast.
- **Descubrimiento Cast en arranque en frío**: el botón Cast no registra selector de MediaRouter de forma fiable en arranques en frío (comportamiento preexistente, ajeno al fix; Google Home sí ve los dispositivos). Recomendable abrir bug aparte si se confirma.
- **Otras rutas no Cast**: `detekt ktlintCheck lintDebug` + suite completa en verde, sin regresiones detectadas.

## Recommendation

Cerrar el bug 0031: el síntoma original (audio local duplicado al perderse la sesión Cast) **no se reproduce** con `1.7.8-fix0031`; el receptor conserva la reproducción y el móvil no arranca audio local, mostrando un aviso accionable cuyo reintento funciona. Abrir como follow-up menor el nombre del dispositivo en el aviso y, si se desea, investigar aparte el descubrimiento Cast en arranque en frío y el error 2251 al arrancar sesión con el reloj tras forzar cierres.
