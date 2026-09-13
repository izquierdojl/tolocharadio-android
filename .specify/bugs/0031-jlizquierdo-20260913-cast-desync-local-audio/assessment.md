# Bug Assessment: Cast pierde la sesión tras unos minutos y arranca audio local duplicado

- **Slug**: 0031-jlizquierdo-20260913-cast-desync-local-audio
- **Created**: 2026-09-13
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

Reportado por el usuario (texto pegado):

> Revisa este bug relacionado con la reproducción remota "ChromeCast": Cuando se inicia la reproducción la traslada correctamente al dispositivo externo y durante unos pocos minutos, está sincronizado, pero pasado un tiempo, se desconecta del dispositivo remoto y lo que es peor suena en local sin dejar de sonar en remoto. Habría que ver como reforzar o hacer que esta sincronización no se pierda, y si se pierde por motivos ajenos, no sonar en local.

## Symptom

Con reproducción activa en Chromecast, tras unos minutos la app pierde la sesión remota y **arranca también la reproducción local**, mientras el receptor **sigue sonando**: dos streams simultáneos (eco/desincronizado). Esperado: o se mantiene la sesión remota, o si se pierde por causas ajenas a la app, no debe arrancarse audio local mientras el receptor pueda seguir reproduciendo (spec 011, SC-005 y FR-009: "no duplicar audio local y remoto en ningún caso").

## Reproduction

1. Conectar la app a un Chromecast (misma red) y reproducir una emisora con la sesión conectada.
2. Esperar unos minutos con reproducción activa (según el reporte; presumiblemente con pantalla apagada / app en segundo plano). [NEEDS CLARIFICATION: ¿pantalla encendida o apagada? ¿app en primer plano o background?]
3. Observado: la sesión Cast se pierde, el receptor sigue sonando y la app empieza a sonar en local a la vez.
4. Esperado: sin duplicidad; si la sesión no se puede recuperar, no arrancar local (o hacerlo solo tras confirmar que el remoto se detuvo).

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt:135-143` — `onSessionEnded` (cualquier motivo, incluida pérdida de red) llama incondicionalmente a `resumeLocalPlayback()`. El receptor Chromecast es autónomo y sigue sonando cuando el sender pierde la sesión.
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt:160-168` — `onSessionResumeFailed` hace lo mismo: libera el player y arranca local, sin parar ni confirmar el estado del receptor.
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt:178-195` + `145-158` — `onSessionResumed` vuelve a llamar a `createCastPlayer()`, que ejecuta `castPlayer?.release()` (línea 181). En Media3 1.4.1, `CastPlayer.release()` **termina la sesión** con `sessionManager.endCurrentSession(false)` (verificado en el sources jar de `media3-cast-1.4.1`), y `stopCasting=false` deja la app del receptor en ejecución. Es decir: **la propia app puede matar la sesión que acaba de reanudar** (p. ej. tras una suspensión transitoria), disparando después `onSessionEnded` → fallback local con el remoto aún sonando.
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt:234-246` — `resumeLocalPlayback()` decide con `ActiveStationHolder` (`PLAYING`/`BUFFERING`) y arranca ExoPlayer. Durante Cast el holder **nunca** sale de `BUFFERING`, así que arranca local incluso si el usuario había pausado en el remoto.
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt:321-329` — `disconnect()` sí para el remoto (`castPlayer.stop()` + `endCurrentSession` vía `release()`), pero **no tiene ningún llamador** (grep sin resultados): la desconexión manual real va por el diálogo nativo de Cast/MediaRouter.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt:107-142` — el listener local retorna temprano cuando hay Cast conectado, por lo que `_state`/holder no reflejan el estado remoto (se queda en `BUFFERING`).
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt:212-232` — `startSource()` en rama Cast no detiene el ExoPlayer local ni sincroniza el holder; `connectToStation()` (CastPlayerManager:305-319) tampoco.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt:311-320` — `toggle()` con Cast actúa sobre `activePlayer` (CastPlayer) pero `syncHolderToState()` reescribe el holder con `_state` local (buffering).
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt:135-140` — `onTaskRemoved` consulta el `ExoPlayer` local, no el player activo; con Cast el servicio puede pararse al quitar la tarea (contexto secundario, no la causa).
- Evidencia pública del comportamiento del receptor: el botón "detener emisión" del diálogo de Cast no siempre detiene la reproducción en el receptor (Stack Overflow 64137019, 45709683); `endCurrentSession(false)` desengancha sin parar la app remota.

## Root Cause Hypothesis

**Confidence: high en el mecanismo de duplicidad; medium-high en el disparador "pérdida tras unos minutos"; medium en la causa externa exacta.**

Dos defectos encadenados:

1. **Fallback local a ciegas.** Cualquier fin de sesión (`onSessionEnded`/`onSessionResumeFailed`) arranca la reproducción local sin comprobar que el receptor se ha detenido. En Cast, el receptor (Default Media Receiver) es una app independiente y sigue reproduciendo cuando el sender se desconecta o cuando la sesión se cierra con `stopCasting=false` (que es justo lo que hace `CastPlayer.release()` en Media3 1.4.1). Resultado: local + remoto simultáneos (SC-005 violado).
2. **Auto-destrucción de la sesión al reanudarla.** `createCastPlayer()` libera el `CastPlayer` anterior; en `onSessionResumed` ese `release()` ejecuta `endCurrentSession(false)` y termina la sesión recién reanudada. Cualquier suspensión transitoria (típica con pantalla apagada, ahorro de energía Wi-Fi o microcorte de red tras unos minutos) desemboca en: suspensión → reanudación → la app cierra la sesión → `onSessionEnded` → arranque local mientras el receptor sigue sonando. Encaja con el "durante unos minutos está sincronizado y luego se desconecta".

Como factor de precisión añadido, durante Cast el holder local permanece en `BUFFERING`, por lo que el fallback arranca local incluso si el remoto estaba pausado.

## Proposed Remediation

**Preferred** (mínima, sin dependencias nuevas; alineada con la petición "si se pierde por motivos ajenos, no sonar en local"):

1. **Ciclo de vida de la sesión Cast** (`CastPlayerManager.kt`):
   - En `onSessionResumed`, **reutilizar** el `CastPlayer` existente (no liberar ni recrear); crear solo si `castPlayer == null`. Eliminar `castPlayer?.release()` de `createCastPlayer()`. Media3 soporta suspensión/reanudación con el mismo player.
   - En `onSessionStartFailed`/`onSessionEnded`, liberar el player sin provocar `endCurrentSession` sobre sesiones nuevas (liberar solo si la sesión terminó de verdad).
   - Diferenciar **fin intencionado por el usuario** de **pérdida inesperada**: bandera de intención en `disconnect()` y/o callback `MediaRouter.onRouteUnselected`; registrar `error`/`reason` (código, sin PII) en `onSessionSuspended/Resumed/Ended/ResumeFailed` para diagnóstico.
2. **Política de fallback anti-duplicado** (extraíble a clase pura testable, p. ej. `cast/CastFallbackDecision.kt` o `feature/player/`):
   - Pérdida inesperada: **no** arrancar ExoPlayer. Publicar estado de aviso accionable ("Se perdió la conexión con <dispositivo>; puede seguir sonando") y ofrecer en UI **"Escuchar en el móvil"** (nuevo método explícito que sí reanuda local) y, si aplica, reintentar conexión.
   - Desconexión manual: parar seguro el remoto (`castPlayer.stop()` / `endCurrentSession(true)`) y después reanudar local (comportamiento actual de `disconnect()`, cableándolo al flujo real).
   - No reanudar local si el remoto estaba **pausado** en el momento de la pérdida.
3. **Sincronización de estado mientras hay Cast** (`PlayerViewModel.kt`): reflejar el estado real de Cast en `ActiveStationHolder` (o basar la decisión de fallback en el estado del `CastPlayer`, no en el holder), para que UI y fallback no arrastren un `BUFFERING` obsoleto.
4. **UI** (`PlayerUi.kt`, `PanelHelpers.kt`): aviso con acción al perderse la sesión de forma inesperada; opcional usar `CastConnectionState.RECONNECTING` para "Reconectando…" en el subtítulo. La notificación no debe mostrar reproducción local hasta que el usuario elija.

**Alternatives**:

- **Reconexión activa + parada del remoto**: al perder la sesión, intentar reconectar (reselección de ruta / API de sesión) y `endCurrentSession(true)` para detener el receptor; solo entonces reanudar local automáticamente. Es lo más fiel a "seguir escuchando", pero depende de la red y del SDK, añade complejidad y puede fallar (el receptor queda sonando igualmente). Alto riesgo.
- **Auto-fallback con ventana de gracia**: esperar N segundos intentando reconectar; si falla, silencio + aviso (evita duplicidad sin obligar a tocar). Combina con la preferida pero añade latencia y estados intermedios.
- **Solo documentar/avisar sin tocar el fallback**: descartada; no resuelve el audio duplicado.

**Files likely to change**:

- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/` (nueva clase de decisión pura, p. ej. `CastFallbackDecision.kt`)
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` (+ `PanelHelpers.kt` si aplica)
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/ActiveStationHolder.kt` (si se sincroniza con Cast)
- Tests: `app/src/test/java/com/izquierdojl/tolocharadio/cast/CastFallbackDecisionTest.kt` (nuevo), `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt`

**Tests to add or update**:

- Decisión pura: fin intencionado → reanudar local; pérdida inesperada → **no** reanudar; remoto pausado → no reanudar; reanudación de sesión → no recrear/liberar player.
- `PlayerViewModelTest`: sesión Cast perdida de forma inesperada → no se arranca ExoPlayer y se emite estado de aviso; acción "Escuchar en el móvil" → sí arranca local una vez; desconexión manual → para remoto y reanuda local; `retry` de Cast (0027/0028) sigue verde.
- Regresión: flujo Cast normal (conectar, reproducir, cambiar emisora, pausa/reanudar) sin cambios.

## Risks & Considerations

- **Garantía imposible sobre el receptor**: si la red se perdió y no hay forma de reconectar, no se puede parar el Chromecast; la única garantía es no sonar en local (decisión explícita del usuario en el reporte). El aviso + acción de un toque debe ser claro para que no parezca que "se ha roto".
- **Falso positivo**: si el Chromecast se apagó de verdad, el usuario tendrá que pulsar "Escuchar en el móvil"; aceptable si el aviso es visible.
- **Reutilización del player en resumen**: verificar en dispositivo que Media3 resincroniza título/estado tras suspensión (debería; validar con el fix).
- **Semántica de `error` en `onSessionEnded`**: no basar la decisión solo en el código; combinar con intención de usuario/ruta.
- **Servicio en foreground**: con Cast reproduciendo el servicio Media3 ya va en foreground (`shouldRunInForeground` con `CastPlayer` READY+playWhenReady), así que el proceso no debería morir en background; no se propone workaround de batería.
- **Deuda adyacente** (no bloqueante): `runBlocking` en `resumeLocalPlayback`/`connectToStation`; `disconnect()` sin llamadores; `onTaskRemoved` consulta el player local.
- **Constitución**: sin dependencias nuevas, tests obligatorios (Red-Green) para la lógica extraída y el ViewModel, logs sin PII (solo `code`/`status`/stationId anonimizado), gates `testDebugUnitTest detekt ktlintCheck lintDebug`.

## Open Questions

- [NEEDS CLARIFICATION: ¿Ocurre solo con pantalla apagada/app en background (suspensión por red/ahorro) o también con la app en primer plano?]
- [NEEDS CLARIFICATION: ¿Se vio el estado "Reconectando…" o un aviso antes de que empezara el audio local?]
- [NEEDS CLARIFICATION: versión instalada (¿1.7.7?) y modelo de receptor Chromecast (Chromecast / Android TV / altavoz).]
- [NEEDS CLARIFICATION: ¿Se acepta que ante pérdida inesperada la app quede en silencio hasta pulsar "Escuchar en el móvil"? Es la lectura literal de "no sonar en local".]
- [NEEDS CLARIFICATION: ¿Se quiere intentar reconectar automáticamente para detener el receptor (alternativa compleja) o basta con no sonar en local + aviso?]
