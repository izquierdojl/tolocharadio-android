# Research: Volumen contextual único estilo Pocket Casts

**Feature**: 0037-jlizquierdo-20260918-contextual-volume | **Fecha**: 2026-09-18

Investigación realizada durante el análisis (`/speckit.analyze`) de la conversación y del
bug 0036, incluyendo lectura del código actual de `main` (v1.11.0, post-revert), historial
git y del repositorio de referencia `Automattic/pocket-casts-android` (clone superficial
local, rama `main`).

## D1. El stack real es media3 1.11.0, no 1.4.1

**Verificación**: `gradle/libs.versions.toml` → `media3 = "1.11.0"`. Historial: el commit
`7496a53` (feat 0035) subió media3 de 1.4.1 a 1.11.0 "para soporte nativo de volumen en
CastPlayer". Los artefactos de la 0035 (plan/research/tasks) nunca se actualizaron y siguen
describiendo 1.4.1.

**Impacto técnico**: en la fuente de media3 1.4.1 (tag del repo androidx/media),
`CastPlayer.setDeviceVolume()` es un no-op vacío y `getAvailableCommands()` no incluye
comandos de volumen. En 1.11.0, `CastPlayer` soporta volumen de dispositivo de forma
nativa: `setDeviceVolume` escribe en `CastSession.setVolume`, `getDeviceVolume` lee el
nivel real, emite `onDeviceVolumeChanged` con el eco del receptor, y expone un
`DeviceInfo` remoto. Consecuencia: **el `MediaSession` de media3 crea automáticamente el
`VolumeProviderCompat` remoto** cuando la sesión recibe el `CastPlayer` crudo → las teclas
físicas y la barra del sistema controlan el volumen del receptor identificado por su
nombre, sin wrapper ni provider propios.

**Decisión**: todo el andamiaje de 0035 concebido para 1.4.1 (wrapper
`CastDeviceVolumePlayer`, pipeline `startVolumeEvents`/`emitDeviceVolume`, hallazgos D11)
queda obsoleto. Esta feature lo retira.

## D2. Modelo Pocket Casts (referencia verificada en fuente)

Clone de `Automattic/pocket-casts-android`; hallazgos:

1. **Sin control de volumen in-app**: el único "volume" de su UI es "Volume Boost" (efecto
   de reproducción local). No existe slider de volumen ni local ni Cast.
2. **Cero código de volumen Cast**: en toda la app no hay `CastSession.setVolume`,
   `setStreamVolume` ni `VolumeProvider`/`VolumeProviderCompat`; `setVolume` solo se usa
   para ducking/fade del player local. Su propio `CastPlayer` (wrapper de
   `RemoteMediaClient`, no el de media3) tiene `setVolume()` vacío.
3. **`CastOptionsProvider` por defecto**: sin personalización de volumen; el framework de
   Cast gestiona teclas + barra del sistema automáticamente (documentado por Google: "the
   physical buttons on the sender device can be used to change the volume of the Cast
   session on the Web Receiver by default" desde Jelly Bean+).
4. **Sesión media3 al castear**: instalan un `CastStatePlayer` (player solo-estado que
   delega play/pause/seek a su `PlaybackManager`) envuelto en
   `PocketCastsForwardingPlayer`; **sin overrides de volumen**.

**Decisión (Q1, 2026-09-18)**: adoptar este modelo — una sola representación de volumen
(la del sistema), contextual según la salida.

## D3. Root cause del bug 0036 (confirmado, vivo en main tras el revert)

Trazado del código actual (`v1.11.0`):

1. `CastPlayerManager.createCastPlayer()`: `volume.bind(CastSessionVolumeDevice(session))`
   → `PlaybackVolumeController.bind()` lee `CastSession.getVolume()`, que devuelve **1.0
   por defecto antes del primer eco del receptor** → `_castVolume = 1.0`.
2. `startVolumeEvents()` (llamada al crear el player) colecta
   `combine(castVolume, muted)` → dispara `emitDeviceVolume()` con la emisión inicial.
3. `emitDeviceVolume()` → `castPlayer.setDeviceVolume(100, 0)` → en media3 1.11.0 esto
   **escribe físicamente** `CastSession.setVolume(1.0)` → **el receptor salta al 100%**.

El assessment de 0036 (recuperado de git, `527dec0`) identificó correctamente la cadena;
su fix v2 falló por otra vía (D6). **Eliminar el pipeline de escritura mata el bug de
raíz**: nadie escribe volumen al conectar; el `CastPlayer` nativo lee el eco real.

## D4. `CastDeviceVolumePlayer` es código muerto

Nunca se instancia: `createCastPlayer()` pasa el `CastPlayer` crudo a
`mediaSession.setPlayer(...)` (CastPlayerManager.kt:267). El KDoc (líneas 253-254) afirma
"La sesión recibe el wrapper", lo cual es falso. Las tareas T007/T008/T010 de la 0035
describen un wiring que no existe en el código fusionado. El wrapper y sus tests se
eliminan.

## D5. `bind()` des-silencia receptores silenciados externamente

`PlaybackVolumeController.bind()` ejecuta `device.writeMuted(_muted.value)` al conectar:
si la app no está silenciada y el receptor sí (mando del TV), la conexión lo
**des-silencia**, contradiciendo el edge case de la propia 0035. Corrección (0037 FR-005):
al conectar solo se escribe el silencio si la app estaba silenciada (carry-over, FR-006);
si no, se lee y representa el estado del receptor.

## D6. Regresión del fix v2 de 0036 (sin audio) — no reintentar esa vía

El fix v2 (revertido en `4935315`) envolvía el player de sesión con
`CastDeviceVolumePlayer` y hacía `emitDeviceVolume` solo-notificación; el usuario reportó
"no suena el volumen tras conectar". Root cause **sin diagnosticar**. Mitigación
estructural en 0037: nunca se re-envuelve el player de sesión — el `CastPlayer` crudo
permanece como player de la sesión (el modelo nativo de 1.11.0), por lo que la vía que
rompió el audio no se vuelve a tomar. Si en validación apareciera cualquier síntoma de
audio, parar y diagnosticar antes de tocar el wiring de la sesión.

## D7. Los bloqueadores D11 de la 0035 están resueltos por el upgrade

Los hallazgos en dispositivo del 2026-09-16 (volumen rechazado silenciosamente, callbacks
del provider no entregados en Android 17, teclas no ruteadas) se obtuvieron contra el
andamiaje de 1.4.1. Con 1.11.0 y el player crudo en sesión, el usuario confirma que el
control funciona ("aunque funciona"): teclas + barra del sistema controlan el receptor.
La corrección al confirmar por eco de la 0035 (FR-014) también es nativa
(`onDeviceVolumeChanged`).

## D8. Alcance de la simplificación (Q1 = no hacer nada especial)

Con FR-011 resuelto a "modelo Pocket Casts", la maquinaria de detección de receptor sin
volumen de la 0035 (`VOLUME_CONFIRMATION_TIMEOUT_MS`, `pendingVolume`,
`castVolumeSupported`, avisos, `reinstallSessionPlayerAsLocal`) se retira por completo.

## Fuentes

- Código actual: `app/src/main/java/com/izquierdojl/tolocharadio/` (main @ 37ec246).
- Historial: `7496a53` (upgrade media3), `527dec0` (assessment 0036), `4935315`/`4ba844e`
  (reverts), `37ec246` (cherry-pick lint).
- androidx/media tag 1.4.1: `libraries/cast/src/main/java/androidx/media3/cast/CastPlayer.java`.
- Automattic/pocket-casts-android (main): `CastPlayer.kt`, `PocketCastsForwardingPlayer.kt`,
  `MediaSessionManager.kt`, `CastStatePlayer.kt`, `CastOptionsProvider.kt` + grep global
  de volumen (clone local en `%TEMP%\opencode\pca`).
