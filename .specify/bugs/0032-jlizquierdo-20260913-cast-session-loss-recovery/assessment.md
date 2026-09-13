# Bug Assessment: Cast bloqueado tras perder la sesión (no permite reenviar sin reiniciar)

- **Slug**: 0032-jlizquierdo-20260913-cast-session-loss-recovery
- **Created**: 2026-09-13
- **Source**: pasted text (feedback del usuario durante la verificación del bug 0031)
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

> pero si sucede eso, se queda bloqueada, ahora por ejemplo habiendo detenido la reproducción en el reloj no admite que le vuelva a enviar, se queda bloqueado y no es aceptable tener que reiniciarlo todo manualmente

## Symptom

Tras perder la sesión Cast (bug 0031), la app no ofrece una recuperación funcional: el usuario percibe que "no admite reenviar" al dispositivo y acaba reiniciando a mano (app/reloj). Se espera que, al reconectar el dispositivo (o en cuanto la sesión vuelva a establecerse), la reproducción se reenvíe sola; y que una parada manual del usuario no se confunda con una pérdida de conexión.

## Reproduction

Matriz ejecutada en dispositivo (Redmi Note 10 + Cast "Dormitorio principal", build 1.7.8-fix0031) con logs `adb`:

1. Wi-Fi OFF con Cast sonando → pérdida `code=2155`, aviso "Se perdió la conexión...", sin audio local (bug 0031 OK).
2. Reconectar inmediatamente con el botón Cast → **falla `onSessionStartFailed code=2251`**; el aviso persiste y parece bloqueado.
3. "Reintentar" sin sesión → reproduce **en local** (no reenvía al reloj).
4. Reconectar ~2,5 min después → conecta (sesión huérfana del receptor ya resuelta); el panel muestra la emisora en pausa y "Reanudar" **sí** reanuda en el reloj, pero la app no lo hace sola.
5. "DETENER ENVÍO" manual desde el diálogo → `onSessionEnded code=2154`, `userInitiated=false` → **aviso falso** de "Se perdió la conexión..." y no reanuda local.

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt:167-177` — `showCastSessionLost()` deja `_state = Error`; nada escucha la reconexión para reenviar la emisora.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt:336-339` — `retry()` usa solo `effectiveState()`: con un Cast conectado y vacío (`Cast(Idle)`) devuelve `null` y **no hace nada**.
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt:153-169` — `onSessionEnded` marca `userInitiated` solo con la bandera de `disconnect()`; la parada manual del diálogo llega como no intencionada (`false`).
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt:186-197` — igual en `onSessionResumeFailed`.
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt:160-168` — el nombre del dispositivo cae a "Chromecast" (`session.castDevice == null` tras la desconexión).
- Evidencia de discriminación por código en dispositivo: parada manual → `error=2154`; pérdida de red → `error=2155`. El SDK expone `CastContext.getCastReasonCodeForCastStatusCode(int)` y `CastReasonCodes.CASTING_STOPPED` (verificado por javap en play-services-cast-framework 21.5.0).

## Root Cause Hypothesis

**Confidence: high (verificado en dispositivo).** Tres carencias encadenadas:

1. **No hay reenvío automático al reconectar**: el VM no observa `connectionState`; aunque la sesión vuelva, la emisora queda en pausa/idle y el usuario tiene que descubrir el panel y pulsar play.
2. **La parada manual no se distingue de una pérdida**: `onSessionEnded` ignora el `error`/reason, así que "DETENER ENVÍO" muestra el aviso de pérdida y no reanuda local (contradice FR-008 de spec 011).
3. **`retry()` puede no-op**: al usar solo `effectiveState()`, con `Cast(Idle)` no encuentra la emisora del error (que está en `_state`).

El error `2251` del arranque de sesión es transitorio y del receptor (la sesión huérfana sigue sonando en el reloj ~2-3 min); la app no puede evitarlo, pero con el reenvío automático sí puede recuperarse sola en cuanto el receptor acepte la nueva sesión.

## Proposed Remediation

**Preferred**:

1. **Reenvío automático al reconectar** (`PlayerViewModel`): recordar la emisora al mostrar el aviso (`lostStation`), observar `castPlayerManager.connectionState` y, al pasar a `CONNECTED`, lanzar `play(station)` (que con Cast conectado usa `connectToStation`). Limpiar `lostStation` en `play()`/`stop()`.
2. **Distinguir parada manual de pérdida** (`CastPlayerManager`): calcular `userInitiated` también por reason del SDK (`castContext.getCastReasonCodeForCastStatusCode(error) == CastReasonCodes.CASTING_STOPPED`) en `onSessionEnded` y `onSessionResumeFailed`. Con parada del usuario y reproducción activa, la política reanuda local (FR-008); sin aviso de "pérdida".
3. **`retry()` robusto** (`PlayerViewModel`): usar `effectiveState()` y caer a `_state.value` si no hay error efectivo, para que funcione con Cast conectado en idle (reenvía al dispositivo) y sin Cast (local).
4. **Nombre real del dispositivo en el aviso** (`CastPlayerManager`): recordar el último `friendlyName` conocido y usarlo como fallback (hoy dice "Chromecast").

**Alternatives**:

- Auto-reconexión programática al dispositivo tras la pérdida (reselección de ruta/`startSession`): más "mágico", depende del SDK y del receptor; se descarta por ahora (el reenvío al reconectar ya elimina el bloqueo).
- No distinguir la parada manual y solo cambiar el texto del aviso: parche menor; no restaura FR-008.

**Files likely to change**:

- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt`
- `app/src/test/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModelTest.kt`
- `app/src/test/java/com/izquierdojl/tolocharadio/cast/CastFallbackPolicyTest.kt` (si se extrae la clasificación de reason)

**Tests to add or update**:

- `PlayerViewModelTest`: aviso de sesión perdida + `connectionState` → `CONNECTED` ⇒ se reenvía la emisora (precheck + `connectToStation`/rama Cast); `retry()` con `Cast(Idle)` reenvía (no no-op); `retry()` sin Cast sigue siendo local; `lostStation` se limpia al reproducir otra emisora o al detener.
- `CastFallbackPolicyTest`: parada manual (reason CASTING_STOPPED) ≡ `userInitiated`; pérdida de red ⇒ no reanuda local.

## Risks & Considerations

- **Duplicado en parada manual**: si el receptor no se detuviera al "DETENER ENVÍO", reanudar local podría solapar. Mitigación: la parada manual la ejecuta el SDK (reason `CASTING_STOPPED`); verificar en dispositivo.
- **Reenvío automático**: reconectar Cast tras una pérdida reanuda la emisora sin preguntar; es el comportamiento esperado por el usuario ("volver a enviar"), pero conviene que no ocurra si el usuario ya empezó otra reproducción local (por eso `lostStation` se limpia en `play()`/`stop()`).
- **`2251` transitorio del receptor**: la app no puede evitarlo; con el fix, cuando la sesión vuelva a aceptarse, la reproducción se reenvía sola. Documentar en el aviso/UX si se confirma reincidencia.
- **Sin cambios de dependencias ni de contrato HTTP**; logs sin PII (solo códigos/reason) y tests de `ViewModel`/política en rojo-verde.

## Open Questions

- [NEEDS CLARIFICATION: ¿La reproducción local automática en "DETENER ENVÍO" es deseable (FR-008) o se prefiere silencio + aviso? Se implementará FR-008 salvo indicación contraria.]
- [NEEDS CLARIFICATION: ¿Se acepta el auto-reenvío al reconectar aunque el usuario no haya pulsado nada? Es la lectura de "no admite que le vuelva a enviar".]
