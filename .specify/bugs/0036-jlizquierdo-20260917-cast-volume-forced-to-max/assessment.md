# Bug Assessment: Chromecast volume forced to 100% after connection

- **Slug**: 0036-jlizquierdo-20260917-cast-volume-forced-to-max
- **Created**: 2026-09-17
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

> Con la comunicación a Chromecast, existe un problema. Se conecta y responde correctamente, pero siempre después de conectarse sube el volumen del dispositivo Chromecast al 100%, lo que lo hace bastante molesto. O que no lo suba, o si no queda más remedio que modificarlo que lo deje como estaba.

## Symptom

Al conectar a un dispositivo Chromecast, el volumen del receptor se fuerza al 100% independientemente de su nivel real. El usuario esperaría que se mantuviera el volumen previo del receptor o, como mínimo, que no se modificara al conectar.

## Reproduction

1. Iniciar la app y conectar a un dispositivo Chromecast (por ejemplo, "Dormitorio principal").
2. Ajustar el volumen del Chromecast a un nivel bajo (por ejemplo, 30%) desde el mando del TV o Google Home.
3. Desconectar y volver a conectar la app al mismo Chromecast.
4. Observar que el volumen del receptor salta al 100% inmediatamente después de la conexión.

[NEEDS CLARIFICATION: Confirmar si el salto al 100% ocurre en el receptor físicamente o solo en la UI/barra del sistema de Android.]

## Suspected Code Paths

- `CastPlayerManager.kt:257-278` — `createCastPlayer()`: llama `startVolumeEvents()` ANTES de `volume.bind()`, lo que provoca que el collector emita el valor por defecto de `_castVolume` (1.0 = 100%) al CastPlayer antes de leer el volumen real del receptor.
- `PlaybackVolumeController.kt:44` — `_castVolume = MutableStateFlow(1f)`: el valor por defecto es 1.0 (100%). Cuando `startVolumeEvents()` se ejecuta antes de `bind()`, el collector recibe inmediatamente 1.0 y lo emite al CastPlayer.
- `PlaybackVolumeController.kt:287-298` — `startVolumeEvents()`: combina `castVolume` y `muted` y ejecuta `emitDeviceVolume()` en cada emisión. La primera emisión ocurre con el valor por defecto (1.0) antes de que `bind()` lea el volumen real.
- `CastPlayerManager.kt:295-298` — `emitDeviceVolume()`: lee `volume.deviceVolumePercent()` (que retorna 100 cuando `_castVolume` es 1.0) y lo aplica al CastPlayer con `player.setDeviceVolume(100, 0)`.

## Root Cause Hypothesis

**Confidence: high**

El bug es una **condición de carrera en la inicialización del volumen**. En `createCastPlayer()` (`CastPlayerManager.kt:257-278`), el flujo es:

1. `startVolumeEvents()` se ejecuta primero (línea 269)
2. `volume.bind(...)` se ejecuta después (línea 272)

Cuando `startVolumeEvents()` lanza su coroutine, el `StateFlow` `_castVolume` ya tiene el valor por defecto `1f` (100%). El collector se ejecuta inmediatamente y llama a `emitDeviceVolume()`, que push 100% al CastPlayer. Solo después, `bind()` lee el volumen real del receptor (por ejemplo, 30%) y actualiza `_castVolume`, lo que dispara otra emisión correcta.

Existe una ventana de tiempo donde el CastPlayer muestra/recibe 100% antes de que se lea el volumen real. Si en ese intervalo el MediaSession o el sistema Android consulta o aplica el volumen del CastPlayer, el receptor puede recibir el comando de volumen 100% antes de que `bind()`有机会 corregirlo.

Adicionalmente, si `readVolume()` retorna `null` (sesión no disponible o receptor aún no listo), `_castVolume` se queda en 1.0 y el receptor nunca se corrige.

## Proposed Remediation

**Preferred**: Reordenar `createCastPlayer()` para que `volume.bind()` se ejecute ANTES de `startVolumeEvents()`. De esta forma:

1. `bind()` lee el volumen real del receptor y actualiza `_castVolume`
2. `startVolumeEvents()` recibe el valor correcto desde la primera emisión
3. Nunca se push 100% al CastPlayer

```kotlin
private fun createCastPlayer(session: CastSession) {
    castError = null
    if (castPlayer == null) {
        castContext?.let { ctx ->
            castPlayer = CastPlayer(ctx).apply {
                addListener(castPlayerListener)
            }
        }
        castPlayer?.let { player ->
            mediaSession?.setPlayer(player)
        }
    }
    if (!volume.isRemoteActive) {
        volume.bind(CastSessionVolumeDevice(session))  // bind FIRST
    }
    startVolumeEvents()  // then start observing
    requestAudioFocus()
    lastDeviceName = currentDeviceName()
    updateCastState()
}
```

**Alternatives**:
- Inicializar `_castVolume` con `Float.NaN` y tratar NaN como "sin valor conocido" en `emitDeviceVolume()` (no emitir). Más seguro pero requiere cambios en múltiples lugares.
- Usar `drop(1)` o `conflate()` en el collector para ignorar la primera emisión. Más frágil y menos claro.

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/cast/CastPlayerManager.kt`

**Tests to add or update**:
- Test en `CastPlayerManagerTest` (si existe) que verifique que al crear la sesión Cast, el volumen del CastPlayer no se fuerza a 100% antes de leer el volumen real del receptor.
- Test en `PlaybackVolumeControllerTest` que verifique que `bind()` actualiza `_castVolume` con el valor leído del receptor y que `emitDeviceVolume()` no se ejecuta con el valor por defecto.

## Risks & Considerations

- Reordenar `bind()` antes de `startVolumeEvents()` podría tener efectos secundarios si `startVolumeEvents()` asume que `castPlayer` ya existe (actualmente `castPlayer` se crea antes de ambos, así que no debería haber problema).
- Si `readVolume()` retorna `null` durante `bind()`, `_castVolume` se queda en 1.0 y el bug persiste para ese caso edge. Se debería considerar inicializar `_castVolume` con un valor sentinela o verificar el caso null.
- La spec 0035 (On-Device Testing Findings) reporta que `CastSession.setVolume` es rechazado silenciosamente por el receptor en Android 17. Si el receptor ignora el comando de volumen 100%, el bug podría no ser audible en todos los dispositivos, pero sí visible en la UI (barra del sistema muestra 100%).

## Open Questions

- [NEEDS CLARIFICATION: ¿El salto al 100% es solo en la UI (barra de volumen del sistema) o el receptor físicamente sube el volumen?]
- [NEEDS CLARIFICATION: ¿Ocurre en todos los dispositivos Chromecast o solo en algunos modelos/receptores?]
