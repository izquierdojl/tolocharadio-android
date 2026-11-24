# Research: Controles de auriculares para pausar y saltar

**Feature**: `0060-izquierdojl-20261124-headset-media-controls`
**Date**: 2026-11-24
**Spec**: [spec.md](./spec.md)

## R1 — Intercepción de teclas multimedia

**Decision**: usar `MediaSession.Callback.onMediaButtonEvent(session, controller,
intent)` para las teclas físicas `KEYCODE_MEDIA_NEXT` y
`KEYCODE_MEDIA_PREVIOUS`. El callback extrae `keyCode`/`action`/`repeatCount`
del `KeyEvent` y delega en `HeadsetControlsController` con primitivas, para que
los tests JVM no necesiten Robolectric. Solo se actúa en `ACTION_DOWN` con
`repeatCount == 0`; las repeticiones y el `ACTION_UP` se consumen sin acción.
Para el resto de teclas se devuelve `false` y Media3 mantiene su comportamiento
(play/pausa, `KEYCODE_HEADSETHOOK`, etc.).

**Rationale**: Media3 documenta `onMediaButtonEvent` como el punto de
intercepción para casos avanzados; además, desde 1.9.0 también entrega
`ACTION_UP`, lo que permite distinguir una pulsación nueva de una repetición.
Es la única vía que expone `repeatCount` para cumplir «un salto por gesto»
(FR-011) sin mezclar dos pulsaciones rápidas intencionadas.

**Alternatives considered**:
- **`onPlayerCommandRequest`**: no ve el `KeyEvent` ni las repeticiones; además
  está deprecado. Se reserva para comandos de lock screen/controles del sistema.
- **Envolver el `Player` (`ForwardingSimpleBasePlayer.handleSeek`)**: permitiría
  interceptar `seekToNext`/`seekBack`, pero obliga a envolver tanto
  `RecoveryAwarePlayer` como `CastPlayer` y complica la inyección Hilt
  (posible ciclo con `CastPlayerManager`). Se descarta para esta spec.
- **`MediaButtonReceiver`**: pensado para reanudar reproducción cuando el
  servicio no está vivo; podría arrancar audio y choca con FR-013 (sin
  autoplay). No se añade.
- **Servicio de accesibilidad**: rechazado por permisos y por el non-goal.

## R2 — Pulsación larga de volumen

**Decision**: capturar `VOLUME_UP`/`VOLUME_DOWN` en
`MainActivity.dispatchKeyEvent` y delegar en `HeadsetVolumeGestureHandler`
(singleton, hilo principal) con `keyCode`/`action`/`repeatCount` como
primitivas, para test JVM sin Robolectric. El handler consume el `ACTION_DOWN`
inicial, arranca un temporizador de 500 ms y:
- si llega `ACTION_UP` antes del umbral, ajusta el volumen del sistema una vez
  con `AudioManager.adjustStreamVolume(STREAM_MUSIC, ADJUST_RAISE/LOWER,
  FLAG_SHOW_UI)`;
- si se cumple el umbral, ejecuta un único salto y consume las repeticiones y el
  `ACTION_UP`.

Solo captura si la Activity está en primer plano y
`HeadsetControlsController.canSkip()` es verdadero. `canSkip()` exige episodio
activo, gestos activados, modo de audio `AudioManager.MODE_NORMAL` (sin llamada,
ringtone ni comunicación) y player sin supresión por pérdida de foco. No se
detecta el tipo de salida: el gesto aplica a cualquier tecla de volumen que
llegue a la Activity, haya o no auricular conectado; la restricción real es el
primer plano.

**Rationale**: las teclas de volumen no llegan a `MediaSession`; el sistema las
gestiona antes. Para distinguir corta/larga sin cambiar el volumen durante la
pulsación larga hay que consumirlas y emular la corta. El umbral de 0,5 s es el
de la spec y el mismo que usa la interacción típica de pulsación larga.

**Alternatives considered**:
- **Dejar que el sistema gestione el volumen y detectar la larga por eventos
  repetidos**: el volumen subiría/bajaría durante la pulsación larga, violando
  FR-005 y el caso límite de volumen sin alterar.
- **`onKeyLongPress`**: pensado para teclas de vistas y poco fiable con
  volumen; el temporizador es explícito y testeable.
- **Servicio de accesibilidad**: rechazado por permisos y non-goal; además no
  resuelve el volumen AVRCP de Bluetooth.

**Tradeoff**: la pulsación corta cambia el volumen al soltar, no al pulsar. Se
acepta para que la pulsación larga no altere el volumen; el panel del sistema se
muestra con `FLAG_SHOW_UI`.

**Nota de plataforma**: la referencia de `AudioManager.adjustStreamVolume`
desaconseja su uso a apps que no sustituyen la gestión global de audio, pero no
exige `MODIFY_AUDIO_SETTINGS` para este método (sí para otras APIs de mixer). Se
usa solo para emular una pulsación corta y se valida en la matriz de
dispositivos; no se añade ningún permiso.

## R3 — Modelo y persistencia de los segundos

**Decision**: añadir a `InstancePrefs` tres claves de DataStore:
`headset_skip_back_sec` (default 15), `headset_skip_forward_sec` (default 30) y
`headset_skip_enabled` (default `true`). Exponer un `Flow<HeadsetControlSettings>`
y setters. Crear `HeadsetControlSettings` en `domain/playback` con las opciones
`5, 10, 15, 20, 30, 45, 60` y saneado: un valor ausente o fuera de rango cae al
default correspondiente. `HeadsetControlSettingsCache` (`@Singleton`,
`@ApplicationScope`) convierte el flujo en `StateFlow` con `SharingStarted.Eagerly`
para que servicio, controlador y handler lean el valor actual sin bloquear.

**Rationale**: DataStore ya es el almacén de ajustes de la app y no hace falta
Room. Un `StateFlow` compartido evita tres colecciones independientes y permite
que el cambio se aplique al siguiente salto sin reiniciar. El saneado protege
frente a datos corruptos o versiones antiguas.

**Alternatives considered**:
- **Room**: sobredimensionado para tres claves; la app ya usa DataStore para
  ajustes.
- **Preferencias por podcast/emisora**: fuera de alcance y sin caso de uso
  aprobado (YAGNI).
- **Constantes fijas 15/30**: rechazado por la aclaración de la spec.

## R4 — Aplicar los segundos a todos los controles

**Decision**:
- **Reproductor local**: al crear un `ExoPlayer`, el servicio aplica
  `setSeekBackIncrementMs`/`setSeekForwardIncrementMs` (API añadida en Media3
  1.9.0) con los valores actuales; vuelve a aplicarlos cuando cambia la
  preferencia o se intercambia el player.
- **Notificación**: los botones de retroceso/avance de episodio dejan de usar
  `Player.COMMAND_SEEK_BACK/FORWARD` y pasan a comandos de sesión propios
  (`CUSTOM_COMMAND_SEEK_BACK/FORWARD`). Así el controlador aplica los segundos
  configurados también con Cast, donde no se pueden fijar incrementos.
- **Lock screen y controles del sistema**: `onPlayerCommandRequest` intercepta
  los comandos de seek y anterior/siguiente, ejecuta el salto configurado y
  devuelve `SessionError.INFO_CANCELLED` para evitar la ejecución por defecto.
- **UI de episodios**: `EpisodePlayerViewModel.skipBack/Forward` comprueban
  `canSkip(ExplicitControl)`, usan los valores del `StateFlow` y limitan a
  `[0, durationMs]`.

**Rationale**: una sola fuente de verdad evita que el auricular salte 30 s y el
botón en pantalla 15 s. Los comandos propios de la notificación son la única
forma uniforme de cubrir local y Cast; los incrementos de `ExoPlayer` se
mantienen para los controles que usan comandos de player (lock screen local,
Android Auto, etc.).

**Alternatives considered**:
- **Solo incrementos de `ExoPlayer`**: no funcionan con `CastPlayer` y no
  permiten etiquetas dinámicas en la notificación.
- **Solo comandos propios**: dejaría los controles del sistema con los
  incrementos por defecto de Media3 (5/15 s). Se combinan ambas vías.
- **Envolver el player**: descartado en R1 por complejidad de Cast/Hilt.

## R5 — Contenido, pausa y Cast

**Decision**: `HeadsetControlPolicy` recibe `PlaybackContent`:
`Idle`, `LiveRadio`, `EpisodePlaying` o `EpisodePaused`, y `SkipSource`
(`HeadsetGesture` o `ExplicitControl`). Solo los dos estados de episodio
permiten salto; ambos se tratan igual (el salto no cambia `playWhenReady`).
`Idle` y `LiveRadio` devuelven `Ignore` para saltos. `gesturesEnabled = false`
solo bloquea `HeadsetGesture`; los controles explícitos (reproductor,
notificación, bloqueo) siguen saltando. El controlador usa
`EpisodePlaybackController.state` para distinguir `Active(playing = true/false)`
de `Loading/Reconnecting/WaitingForNetwork/Error`; en estos últimos no hay
salto. La posición base se lee de `EpisodePlaybackController.positionMs()`
(posición real del player) y el límite superior de `state.durationMs`. Para
Cast, `EpisodePlaybackController.seekTo` opera sobre `activePlayer()`, por lo
que el mismo camino sirve para local y remoto; si el receptor no admite seek, no
se produce efecto local ni audio duplicado.

**Rationale**: la spec decidió que el salto funciona también en pausa; el
estado `Active` contiene `playing` y `durationMs`, y `positionMs()` da la
posición real del player para calcular el objetivo. `canSkip()` centraliza el
modo de audio y la supresión por foco para que los saltos del auricular, la
notificación y el bloqueo respeten llamadas y foco (FR-020). Evitar saltos en
error/reconexión cumple el caso límite de «sin acción ni interrupción».

**Alternatives considered**:
- **Permitir salto solo en `playing`**: rechazado por la aclaración Q4.
- **Usar `player.seekTo` directamente en el controlador**: no actualiza el
  estado del `EpisodePlaybackController` ni la posición persistida; se usa su
  API `seekTo`.
- **Tratar Cast como no soportado**: la notificación y la sesión ya funcionan
  con Cast; se mantiene el mejor esfuerzo de FR-014.

## R6 — Ajustes y UX

**Decision**: añadir una sección «Controles de auriculares» en Configuración,
después de «Reproducción», con:
- dos selectores de segundos (5–60) con etiquetas «5 s»…«60 s»;
- un `Switch` «Usar gestos de salto en el auricular»;
- texto de ayuda: solo con la app abierta; en Bluetooth usar anterior/siguiente;
  la radio en directo no se puede saltar; el interruptor solo afecta a los
  gestos del auricular, no a los botones en pantalla ni a la notificación.

Se reutiliza el patrón de `PlaybackSettingsSection` (título `titleSmall`,
descripciones `bodySmall`, `contentDescription` y `testTag`). Las etiquetas de
la notificación usan recursos con formato (`Retroceder %1$d segundos`).

**Rationale**: la sección de Reproducción ya agrupa preferencias de audio y
sigue el lenguaje visual de la app. Los textos explican las limitaciones para
evitar expectativas falsas (especialmente volumen Bluetooth y radio).

**Alternatives considered**:
- **Pantalla de Ajustes propia**: no hace falta para tres controles; YAGNI.
- **Opciones de 1 en 1 s**: demasiadas filas y difícil de usar con una mano; se
  usan las opciones de Pocket Casts (5–60).
- **Mostrar solo en modo Podcasts**: la sección es global y explica que aplica a
  episodios; ocultarla complicaría el descubrimiento.

## R7 — Estrategia de pruebas

**Decision**:
- Tests unitarios puros para `HeadsetControlSettings` y
  `HeadsetControlPolicy` (matriz contenido × comando × fuente × ajustes).
- Tests de `InstancePrefs` y `HeadsetControlSettingsCache` con
  `runTest`/Turbine: defaults, persistencia, valores inválidos y emisión.
- Tests de `HeadsetControlsController` con MockK: dedupe de repeticiones,
  pausa, radio, gestos desactivados (solo gestos; notificación/UI siguen
  saltando), límites y comandos de sesión.
- Tests de `HeadsetVolumeGestureHandler` con `runTest` y tiempo virtual:
  corta/larga, fondo, llamada, foco, cancelación y límites.
- Tests de `EpisodePlayerViewModel` y `SettingsViewModel` con Turbine/MockK.
- Test Compose de la sección de Ajustes (valores, cambio, switch y
  accesibilidad).
- Validación manual con cable y Bluetooth en `quickstart.md`; no se automatizan
  las teclas físicas.

**Rationale**: la lógica de decisión y el temporizador son la parte con más
riesgo; se prueban sin Android. Los puntos de entrada Android se cubren con
tests de controlador/handler y una matriz manual realista.

**Alternatives considered**:
- **Solo tests instrumentados**: lentos y frágiles para teclas de auricular;
  no sustituyen la matriz manual.
- **Robolectric**: añadiría una dependencia no aprobada; los tests puros y
  MockK cubren la lógica.

## R8 — Limitaciones de plataforma y API

**Decision**: documentar en la spec, Ajustes y quickstart:
- Las teclas de volumen solo se capturan con la app en primer plano.
- En la mayoría de Bluetooth, el volumen es absoluto del sistema y no llega
  como `KeyEvent`; la app no puede detectar la pulsación larga. Se usa
  anterior/siguiente.
- La doble/triple pulsación del central queda fuera de alcance; Media3 puede
  interpretar un doble `KEYCODE_HEADSETHOOK` como «seek to next», pero no se
  anuncia ni se prueba como gesto de la feature.
- `onPlayerCommandRequest` está deprecado en Media3 1.11; se aísla en un único
  callback y se documenta la alternativa (wrapper de player) por si se elimina.

**Rationale**: la feature debe ser honesta sobre lo que el sistema permite; la
spec ya recoge estas limitaciones y SC-011 separa la matriz cable/Bluetooth.

**Alternatives considered**:
- **Prometer volumen largo en todos los Bluetooth**: no es viable con las APIs
  estándar; generaría bugs y expectativas falsas.
- **Accesibilidad para capturar volumen en segundo plano**: rechazado por
  permisos, complejidad y porque no resuelve AVRCP.
