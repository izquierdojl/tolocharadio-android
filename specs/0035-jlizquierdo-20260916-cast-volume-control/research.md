# Research: Control de volumen del dispositivo Chromecast

**Feature**: `0035-jlizquierdo-20260916-cast-volume-control` | **Date**: 2026-09-16

Hallazgos verificados contra las fuentes instaladas (`media3 1.4.1`,
`play-services-cast-framework 21.5.0`) y la documentación oficial. Cada decisión cierra un
punto técnico del plan; no quedan `NEEDS CLARIFICATION`.

---

## D1. Transporte del volumen remoto: `CastSession` (volumen de dispositivo)

**Decision**: Usar `CastSession.setVolume(double)` / `getVolume()` / `setMute(boolean)` /
`isMute()` (rango `[0.0, 1.0]`) como único transporte del volumen del receptor, más
`CastSession.addCastListener(Cast.Listener)` para observar `onVolumeChanged()`.

**Rationale**:

- `CastSession` opera el **volumen del dispositivo** (el que el usuario percibe y que los
  receptores muestran con feedback propio: slider en tablets, luces en altavoces Nest).
- `RemoteMediaClient.setStreamVolume` opera el volumen del *stream* del receptor, no tiene
  feedback visual y su funcionamiento es irregular entre dispositivos (hallazgo del
  mantenedor de media3 en la discusión del PR androidx/media#2279).
- Es API pública vigente en 21.5.0 (`getVolume`, `setVolume`, `isMute`, `setMute`,
  `addCastListener`), sin deprecación.

**Alternatives considered**:

- `RemoteMediaClient.setStreamVolume` / `setStreamMute`: descartado por falta de feedback y
  fiabilidad; se reservaría para receivers que solo soportan volumen de stream, caso no
  contemplado por la spec (YAGNI).
- `CastSession.setVolume` vía `CastPlayer` nativo de media3: no existe en 1.4.1 (ver D3).

---

## D2. Teclas físicas y barra del sistema: comandos de volumen del `Player` de la sesión

**Decision**: Pasar a `MediaSession.setPlayer(...)` un wrapper `Player`
(`CastDeviceVolumePlayer`) sobre `CastPlayer` que:

- declara `COMMAND_GET_DEVICE_VOLUME`, `COMMAND_SET_DEVICE_VOLUME(_WITH_FLAGS)` y
  `COMMAND_ADJUST_DEVICE_VOLUME(_WITH_FLAGS)` en `getAvailableCommands()`;
- devuelve `DeviceInfo.Builder(PLAYBACK_TYPE_REMOTE).setMaxVolume(100).build()`;
- implementa `getDeviceVolume`/`setDeviceVolume`/`increase/decreaseDeviceVolume`/
  `isDeviceMuted`/`setDeviceMuted` delegando en `PlaybackVolumeController`.

**Rationale** (verificado en fuente media3 1.4.1):

- `MediaSessionLegacyStub.onDeviceInfoChanged` llama a
  `PlayerWrapper.createVolumeProviderCompat()`; si el player es remoto, hace
  `sessionCompat.setPlaybackToRemote(volumeProviderCompat)`, lo que hace que **SystemUI
  muestre la barra de volumen remota y enrute las teclas físicas** al provider.
- `createVolumeProviderCompat()` decide el tipo de control por comandos disponibles:
  `VOLUME_CONTROL_ABSOLUTE` solo si el player declara `COMMAND_SET_DEVICE_VOLUME(_WITH_FLAGS)`.
  `CastPlayer` 1.4.1 declara `DEVICE_INFO` remoto pero **no** esos comandos:

  ```java
  // media3 1.4.1 PlayerWrapper.createVolumeProviderCompat()
  if (getDeviceInfo().playbackType == DeviceInfo.PLAYBACK_TYPE_LOCAL) return null;
  int volumeControlType = VolumeProviderCompat.VOLUME_CONTROL_FIXED;
  if (availableCommands.containsAny(COMMAND_ADJUST_DEVICE_VOLUME, COMMAND_ADJUST_DEVICE_VOLUME_WITH_FLAGS)) {
      volumeControlType = RELATIVE;
      if (availableCommands.containsAny(COMMAND_SET_DEVICE_VOLUME, COMMAND_SET_DEVICE_VOLUME_WITH_FLAGS)) {
          volumeControlType = ABSOLUTE;
      }
  }
  ```

- `MediaSessionLegacyStub.onDeviceVolumeChanged` propaga
  `volumeProviderCompat.setCurrentVolume(...)`, por lo que el wrapper debe **emitir**
  `Player.Listener.onDeviceVolumeChanged(volumen 0–100, muted)` para que la barra del sistema
  refleje también cambios externos y confirmaciones.
- El wrapper se implementa con delegación Kotlin (`class ...(delegate: Player) : Player by
  delegate`) y mantiene su propia lista de listeners para los eventos sintéticos de volumen,
  sin reimplementar el resto de la interfaz `Player`.

**Alternatives considered**:

- `VolumeProviderCompat` propio: la API pública de media3 1.4.1 no permite inyectarlo en la
  `MediaSession` (el provider es interno al stub); la vía del player es la oficial
  (androidx/media issue #328).
- Interceptar `KEYCODE_VOLUME_*` en Activity/servicio + UI propia: no cubre segundo plano ni
  pantalla apagada y duplica la barra del sistema; descartado.

---

## D3. Actualizar media3 de 1.4.1 a 1.11.0

**Decision**: Subir `media3` de `1.4.1` a `1.11.0`.

**Rationale**: Las versiones 1.8.0+ implementan volumen nativo en `CastPlayer`
(PR #2279: `setVolume()` → `RemoteMediaClient.setStreamVolume()`,
`fetchDeviceInfo()` con `maxVolume`). En 1.4.1 el `CastPlayer` no declaraba comandos de
volumen y el `VolumeProviderCompat` no entregaba callbacks en Android 17. La auditoría de
código confirmó 0 breaking changes en nuestro codebase (no usamos APIs removidas en
1.5.0–1.11.0). 366 tests pasan sin cambios.

**Alternatives considered**: mantener 1.4.1 y usar wrapper (descartado: el wrapper no se
seteaba correctamente en la sesión y el provider no se creaba); usar `MediaRouter.RouteInfo`
(descartado: API interna del Cast SDK, no expuesta directamente).

---

## D4. Sincronización de cambios externos y errores transitorios

**Decision**:

- Fuente de verdad de la UI: `PlaybackVolumeController.castVolume` (0.0–1.0) y
  `muted`, alimentados por `Cast.Listener.onVolumeChanged()` y por los ajustes locales.
- Confirmación: cada ajuste envía el valor al receptor; cuando llega `onVolumeChanged` con un
  valor equivalente (±1 paso = 0.01) se marca confirmado y el estado se alinea al valor real.
- FR-014: si un ajuste no se confirma, el siguiente `onVolumeChanged` (o la relectura
  `CastSession.getVolume()`) corrige el valor mostrado sin avisos.

**Rationale**: `Cast.Listener.onVolumeChanged` es el único canal de retorno del volumen de
dispositivo; evita sondeos (`polling`) y cumple SC-004 (<2 s, normalmente inmediato).

**Alternatives considered**: leer `CastSession.volume` periódicamente; descartado por
innecesario y costoso en batería.

---

## D5. Silencio único y persistente al cambiar de salida (FR-007, FR-008, FR-015)

**Decision**: `PlaybackVolumeController.muted: StateFlow<Boolean>` es la única fuente de
silencio. `toggleMute()` aplica al dispositivo activo: con Cast → `CastSession.setMute`;
sin Cast → `exoPlayer.volume = 0f/1f`. `bind(remote)` al conectar Cast aplica el silencio
vigente al receptor; `unbind()` lo aplica al reproductor local. Ajustar volumen mientras está
silenciado des-silencia y aplica el nuevo nivel (FR-008). `resetMute()` conserva la regla de
la spec 004 (play/stop siempre vuelven con sonido) aplicando el cambio al dispositivo activo.

**Rationale**: elimina las dos fuentes de silencio actuales (VM y notificación) que causan la
incoherencia con Cast; el comando MUTE de la notificación y el botón del mini-player usan el
mismo camino.

**Alternatives considered**: mantener `_isMuted` en el VM y sincronizar con un espejo en el
controlador; descartado por duplicar estado y por no cubrir el MUTE de la notificación.

---

## D6. Escala y mapeo de valores

**Decision**: El máximo del control remoto es **100 pasos**. Mapeos:

- UI (`Slider`): `Float 0f..1f` ↔ `Int 0..100` (redondeo).
- Receptor: `double 0.0..1.0` = pasos / 100.0; clamp a `[0,1]`.
- Confirmación/eco: tolerancia de 1 paso (±0.01) para redondeos del receptor.
- Teclas: `increase/decreaseDeviceVolume` avanzan 1 paso (1 %).

**Rationale**: 1 % es suficientemente fino para radio, coincide con la granularidad típica
del panel de volumen del sistema para proveedores remotos y evita saltos perceptibles.

**Alternatives considered**: 15 pasos estilo volumen local de Android; descartado por
brusco para el slider y sin ventaja funcional.

---

## D7. Receivers sin soporte de volumen (FR-009, FR-014)

**Decision**: Detección por **ventana de silencio**: tras el último ajuste solicitado, si
pasan `VOLUME_CONFIRMATION_TIMEOUT_MS` (3 000 ms) sin `onVolumeChanged` que confirme (y la
relectura `getVolume()` difiere en más de un paso), el controlador marca
`castVolumeSupported = false` una única vez: oculta el slider
del full-player, muestra un aviso no bloqueante y sigue reproduciendo. Si el receptor ignora
el ajuste, el valor mostrado se corrige al real (FR-014).

- Degradación de teclas: al marcar `castVolumeSupported = false`, el wrapper pasa a anunciar
  `DeviceInfo` con `PLAYBACK_TYPE_LOCAL` y `CastPlayerManager` reinstala el player de la
  sesión (`mediaSession.setPlayer` con una instancia nueva del wrapper); media3 ejecuta
  `setPlaybackToLocal(STREAM_MUSIC)` y las teclas vuelven a controlar el volumen del móvil
  (FR-009, edge case). `bind()` de una nueva sesión restaura `PLAYBACK_TYPE_REMOTE`.

**Rationale**: no existe API de capacidades de volumen de dispositivo en Cast SDK; el único
indicio fiable es el eco del receptor. Algunos receptores (p. ej. Chromecast dongle sin CEC)
muestran además su propio aviso físico ("ajusta con el mando"), que complementa al de la app.

**Alternatives considered**:

- Detectar por ausencia de callback tras N ajustes consecutivos sin espera: falsos positivos
  durante un arrastre continuo (los callbacks pueden coalescer).
- No detectar y dejar siempre el control visible: incumple FR-009.
- Mantener la sesión remota inerte (teclas sin efecto) al detectar no soporte: descartado por
  incumplir el edge case de la spec ("las teclas vuelven a comportarse como locales").

---

## D8. Notificación y segundo plano (clarificación Q1)

**Decision**: La notificación no añade controles de volumen; el comando MUTE existente pasa a
enrutarse por `PlaybackVolumeController` (afecta al dispositivo activo). Las teclas físicas
funcionan con la app en segundo plano porque la `MediaSession` vive en
`RadioPlaybackService` y el `VolumeProviderCompat` remoto se crea sobre el player de la
sesión.

**Rationale**: decisión de clarify; comportamiento de referencia Pocket Casts.

---

## D9. Identificación del dispositivo en la barra del sistema

**Decision**: Aceptar identificación por **icono** en el panel del sistema (comportamiento de
SystemUI para sesiones remotas) y mostrar el **nombre** del dispositivo en la etiqueta del
slider del full-player (ya existente).

**Rationale**: SystemUI no muestra el nombre del receptor en el panel de volumen; FR-001
admite "nombre o icono" y SC-002 se cumple con el icono de sesión remota.

---

## D10. Estrategia de pruebas (sin Robolectric)

**Decision**:

- `PlaybackVolumeControllerTest` (JUnit + MockK + Turbine + coroutines-test): mapeos y
  clamping, silencio local/remoto, persistencia al bind/unbind, ajuste mientras silenciado,
  confirmación/eco, detección de no soportado (con `TestScope` inyectado para la ventana de
  3 s) y `resetMute`.
- `CastDeviceVolumePlayerTest`: comandos disponibles, `DeviceInfo` remoto con `maxVolume`,
  delegación de reproducción al `CastPlayer` mockeado, mapeo de `setDeviceVolume` y emisión
  de `onDeviceVolumeChanged` a los listeners registrados.
- `PlayerViewModelTest`: actualizar los casos de silencio al nuevo enrutado por controlador y
  añadir la exposición de `castVolume`/`castVolumeSupported`.

**Rationale**: la constitución exige tests de ViewModel/dominio; la lógica Android (Cast) se
aísla tras `RemoteVolumeDevice` y el wrapper solo depende de interfaces de media3 (mockeables
con MockK). El proyecto no usa Robolectric, así que no se testean clases de framework.

---

## D11. Hallazgos de prueba en dispositivo real (Redmi M2101K7AG / Android 17 / API 37)

**Dispositivo receptor**: Chromecast "Dormitorio principal" (posiblemente Nest hub o dongle
con receptor Google default).

### D11.1. Transporte de volumen: CastSession vs RemoteMediaClient

**Hallazgo**: Ambos transportes (`CastSession.setVolume` y
`RemoteMediaClient.setStreamVolume`) son rechazados silenciosamente por el receptor en
pruebas reales. Los `PendingResult` de `setVolume` y `setStreamVolume` no generan errores
visibles, pero el volumen audible del receptor no cambia. El receptor sigue reproduciendo
al nivel original.

**Evidencia**:
- `CastSession.getVolume()` devuelve `0.28` (28%) y `MediaStatus.getStreamVolume()` devuelve
  `1.0` (100%). El eco por `RemoteMediaClient.Callback.onStatusUpdated` funciona correctamente
  (lee los valores y los propaga al controlador). El provider muestra `current=28`.
- Al invocar `CastSession.setVolume(0.5)` o `RemoteMediaClient.setStreamVolume(0.5)`, el
  valor no cambia en el receptor. El usuario confirma: "el volumen no cambia".

**Posibles causas**:
1. El receptor puede no soportar control de volumen vía Cast SDK (receptor embebido en smart TV
   o dongle sin CEC que delega el volumen al TV).
2. Puede haber una restricción de permisos o de versión del protocolo Cast entre el SDK 21.5.0
   y el firmware del receptor.
3. El receptor puede estar ignorando los comandos `SET_VOLUME` del sender.

### D11.2. Callback del VolumeProviderCompat en media3 1.4.1 + Android 17

**Hallazgo**: `MediaSessionService` detecta nuestro provider (`volumeType=REMOTE,
controlType=ABSOLUTE, max=100, current=28`) y muestra la barra de volumen remota
identificada como "TolochaRadio". Al ajustar la barra o pulsar teclas, el sistema llama
`Adjusting com.izquierdojl.tolocharadio/... por N` y `setVolumeTo`. Sin embargo, el callback
nunca llega al `PlayerWrapper.setDeviceVolume` de la app.

**Evidencia**:
- `player.getAvailableCommands` se consulta periódicamente (5 veces cada ~3 s), confirmando
  que media3 ve nuestro wrapper con los comandos de volumen declarados.
- No hay ningún log de `setDeviceVolume`, `increaseDeviceVolume` o `decreaseDeviceVolume`
  en la app cuando el sistema ajusta la barra. El valor del provider (`current=28`) permanece
  estático.
- El `PlaybackState` de la sesión se reporta como `state=NONE(0)` (idle), aunque la sesión
  ESTÁ activa (`active=true`).

**Causa raíz**: En Android 17 con media3 1.4.1, el `VolumeProviderCompat` integrado en
`MediaSessionLegacyStub` no propaga correctamente las llamadas del sistema al player wrapper.
Esto puede ser un regression en la compatibilidad del `MediaSessionCompat` bundled con
`MediaSession` de media3 en Android 17.

### D11.3. Ruteo de teclas físicas

**Hallazgo**: `MediaSessionService.dispatchVolumeKeyEvent` envía el evento a nuestro
paquete (`pkg=com.izquierdojl.tolocharadio`), pero el log muestra `session=null` en la
línea `Adjusting`, lo que significa que el sistema NO encuentra nuestra sesión para
rutear el volumen. Esto ocurre aunque la sesión ES el media button session y tiene
un provider remoto registrado.

**Evidencia**:
```
MediaSessionService: dispatchVolumeKeyEvent, pkg=com.izquierdojl.tolocharadio, ...
MediaSessionService: Adjusting suggestedStream=-2147483648 by 1. flags=4113, session=null
```

Comparar con una sesión que SÍ funciona (de otra app):
```
MediaSessionService: Adjusting com.otra.app/androidx.media3.session... by -1. flags=4113
```

**Posible causa**: En Android 17, `MediaSessionService.getVolumeSession()` requiere que la
sesión tenga un `PlaybackState.state` distinto de `NONE` para ser considerada candidata al
ruteo de volumen. Nuestra sesión reporta `state=NONE` aunque el `CastPlayer` está reproduciendo.

### D11.4. Resumen de impacto

| Funcionalidad | Estado actual | Causa |
|---|---|---|
| Teclas físicas → volumen Cast | **No funciona** | `session=null` en MediaSessionService |
| Barra del sistema → volumen Cast | **No funciona** | Callback del provider no entregado |
| Slider de la app → volumen Cast | **No funciona** | CastSession/RemoteMediaClient rechazan |
| Eco de cambios externos | **Funciona** | RemoteMediaClient.Callback.onStatusUpdated |
| Provider remoto registrado | **Funciona** | volumeType=REMOTE, controlType=ABSOLUTE |
| Detección de receptor sin volumen | **Funciona** | Ventana de 3s + castVolumeSupported |

---

## Referencias

- media3 1.4.1: `libraries/session/.../PlayerWrapper.java` (`createVolumeProviderCompat`),
  `MediaSessionLegacyStub.java` (`onDeviceInfoChanged`, `onDeviceVolumeChanged`),
  `libraries/cast/.../CastPlayer.java` (`setVolume`/`setDeviceVolume` no-op, `DEVICE_INFO`
  remoto, `PERMANENT_AVAILABLE_COMMANDS` sin comandos de volumen).
- media3 issues/PRs: androidx/media#328 (teclas con sesión remota), #2089 y #2279 (volumen de
  dispositivo en `CastPlayer`; discusión `CastSession.volume` vs `setStreamVolume`).
- Google Cast: `CastSession` (`setVolume`, `getVolume`, `setMute`, `isMute`,
  `addCastListener`), `Cast.Listener.onVolumeChanged`, `RemoteMediaClient` (`setStreamVolume`,
  `MediaStatus.isMediaCommandSupported`).
- Código actual: `cast/CastPlayerManager.kt`, `feature/player/PlayerViewModel.kt`
  (`toggleMute`, `resetPlaybackSession`), `feature/player/PlayerUi.kt` (slider no-op),
  `feature/player/RadioPlaybackService.kt` (comando MUTE).
