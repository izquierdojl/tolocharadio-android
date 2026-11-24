# Contrato interno: controles de auriculares

**Feature**: `0060-izquierdojl-20261124-headset-media-controls`
**Date**: 2026-11-24
**Tipo**: contrato de comportamiento interno (no hay API pública nueva)

Este contrato fija cómo se traducen las teclas y gestos del auricular en
acciones de reproducción. Cualquier implementación o test debe respetar estas
tablas.

## 1. Teclas multimedia físicas

| Tecla | Acción `ACTION_DOWN` (`repeatCount == 0`) | Repeticiones / `ACTION_UP` | Quién lo gestiona |
|-------|-------------------------------------------|----------------------------|-------------------|
| `KEYCODE_MEDIA_PLAY_PAUSE` | Play/pausa nativo | Se delega en Media3 | Media3 |
| `KEYCODE_HEADSETHOOK` | Play/pausa nativo | Se delega en Media3 | Media3 |
| `KEYCODE_MEDIA_PLAY` | Play nativo | Se delega en Media3 | Media3 |
| `KEYCODE_MEDIA_PAUSE` | Pausa nativa | Se delega en Media3 | Media3 |
| `KEYCODE_MEDIA_NEXT` | `SkipForward` si episodio activo y gestos activados; si no, no-op | Se consumen sin acción | `HeadsetControlsController` |
| `KEYCODE_MEDIA_PREVIOUS` | `SkipBack` si episodio activo y gestos activados; si no, no-op | Se consumen sin acción | `HeadsetControlsController` |
| Otras | Se delega en Media3 | Se delega en Media3 | Media3 |

**Reglas**:
- Un `ACTION_DOWN` con `repeatCount > 0` nunca produce un segundo salto.
- Un `ACTION_UP` de anterior/siguiente se consume para evitar el
  comportamiento por defecto de cambiar de elemento.
- Si no hay contenido activo, no se arranca reproducción (FR-013).
- En radio en directo, anterior/siguiente no cambian de emisora ni reinician el
  stream (FR-009).

## 2. Pulsación de volumen

La captura solo existe en `MainActivity.dispatchKeyEvent`, es decir, con la app
visible.

| Situación | `ACTION_DOWN` | `ACTION_UP` antes de 500 ms | 500 ms sin soltar | Repeticiones / up posterior |
|-----------|---------------|-----------------------------|-------------------|-----------------------------|
| Episodio activo, gestos activos, modo de audio normal, foco normal | Consume y arranca temporizador | Ajusta volumen del sistema una vez y consume | Ejecuta un único salto (`VOLUME_UP` → adelante, `VOLUME_DOWN` → atrás) | Consume sin acción |
| Radio, idle, gestos desactivados, app en segundo plano, llamada/ringtone o foco perdido | No consume | Volumen nativo | Volumen nativo | Volumen nativo |

**Umbral**: 500 ms (`VOLUME_LONG_PRESS_MS`).

**Alcance**: esta captura es `SkipSource.HeadsetGesture`; desactivar los gestos
del auricular no afecta a los botones de salto en pantalla ni a la notificación
(FR-012, SC-014).

**Volumen corto**:
- `AudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, ADJUST_RAISE|ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)`.
- No se cambia el volumen durante una pulsación larga.
- En dispositivos con volumen fijo, el ajuste puede ser un no-op; el gesto
  largo sigue funcionando si la tecla llega a la app.

**Limitaciones**:
- En la mayoría de Bluetooth, el volumen absoluto lo gestiona el sistema y no
  llega como `KeyEvent`; la app no puede detectar la pulsación larga. Se usa
  anterior/siguiente.
- Con la pantalla apagada o la app en segundo plano no hay captura; el volumen
  es nativo.

## 3. Comandos de la sesión (notificación, bloqueo, controles del sistema)

| Comando | Origen | Acción |
|---------|--------|--------|
| `CUSTOM_COMMAND_SEEK_BACK` | Botón de retroceso de la notificación | `SkipBack` con segundos configurados |
| `CUSTOM_COMMAND_SEEK_FORWARD` | Botón de avance de la notificación | `SkipForward` con segundos configurados |
| `COMMAND_SEEK_BACK` | Lock screen / controlador del sistema | `SkipBack` con segundos configurados; se suprime el seek por defecto |
| `COMMAND_SEEK_FORWARD` | Lock screen / controlador del sistema | `SkipForward` con segundos configurados; se suprime el seek por defecto |
| `COMMAND_SEEK_TO_NEXT` / `COMMAND_SEEK_TO_NEXT_MEDIA_ITEM` | Lock screen / controlador del sistema | `SkipForward`; no cambia de episodio |
| `COMMAND_SEEK_TO_PREVIOUS` / `COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM` | Lock screen / controlador del sistema | `SkipBack`; no cambia de episodio |
| `COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM` | Barra de progreso / seek absoluto | Se permite el comportamiento normal |
| `COMMAND_PLAY_PAUSE` | Notificación / lock screen | Comportamiento normal de Media3 |

**Supresión**: los comandos de salto interceptados devuelven
`SessionError.INFO_CANCELLED` tras ejecutar la acción propia, para que el player
no ejecute además su seek por defecto.

**Fuente**: estos comandos son `SkipSource.ExplicitControl`; no dependen de
`headset_skip_enabled`. El interruptor solo afecta a los gestos del auricular
(sección 1 y 2).

**Foco y llamadas**: todos los comandos de salto pasan por
`HeadsetControlsController.canSkip(source)`, que exige modo de audio normal y
ausencia de supresión por foco. Si no se cumple, se suprimen sin acción (FR-020).

**Radio**: los comandos de salto se suprimen sin efecto; la reproducción
continúa.

## 4. Botones de la notificación

| Layout | Botones de salto | Nombre visible |
|--------|------------------|----------------|
| `RADIO` | No | — |
| `RADIO_RETRY` | No | — |
| `EPISODE` | Sí | «Retroceder N segundos» / «Avanzar N segundos» |

- Los botones usan `CommandButton.SLOT_BACK`/`SLOT_FORWARD`.
- Al cambiar los segundos, la notificación se reconstruye con el nuevo nombre.
- Los botones siguen visibles y activos aunque `headset_skip_enabled = false`
  (SC-014); el interruptor solo desactiva los gestos del auricular.
- Si el valor no es 15/30, se usa el icono genérico de saltar si está disponible;
  si no, se mantiene el icono 15/30 y se prioriza el nombre accesible.

## 5. Esquema de preferencias

| Clave DataStore | Tipo | Default | Validación |
|-----------------|------|---------|------------|
| `headset_skip_back_sec` | `Int` | `15` | En `{5,10,15,20,30,45,60}`; si no, `15` |
| `headset_skip_forward_sec` | `Int` | `30` | En `{5,10,15,20,30,45,60}`; si no, `30` |
| `headset_skip_enabled` | `Boolean` | `true` | `true`/`false`; solo afecta a `SkipSource.HeadsetGesture` |

**Contrato de cambio**:
- Un cambio se persiste y se emite por `HeadsetControlSettingsCache.settings`.
- El siguiente salto, venga del auricular, la notificación, el reproductor o el
  bloqueo, usa el valor nuevo.
- No se requiere reiniciar la app, la reproducción ni reconectar el auricular.

## 6. Contrato de logs

- Tag: `HeadsetControls`.
- Campos: tipo de gesto (`media_next`, `media_previous`, `volume_long_up`,
  `volume_long_down`, `volume_short_up`, `volume_short_down`,
  `notification_skip`), contenido (`radio`, `episode`, `idle`), resultado
  (`skip`, `ignore`, `volume`) y milisegundos del salto.
- Prohibido: nombre de emisora, título de episodio, URL, credenciales, tokens o
  cualquier PII.
- Un fallo de `seekTo` se registra con causa y `positionMs`, sin datos del
  contenido.

## 7. Casos que no forman parte del contrato

- Doble/triple pulsación del botón central como atajo.
- Pulsación larga del botón central.
- Remapeo libre de botones.
- Captura de volumen en segundo plano o mediante accesibilidad.
- DVR, retroceso o avance de radio en directo.
- Configuraciones por emisora, podcast o servidor.
