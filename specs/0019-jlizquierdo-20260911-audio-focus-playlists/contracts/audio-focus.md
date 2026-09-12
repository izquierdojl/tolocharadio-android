# Contract: Foco de audio y reproducción exclusiva

**Feature**: `0019-jlizquierdo-20260911-audio-focus-playlists` | **Requisitos**: FR-001..FR-005 | **SC**: SC-002, SC-003, SC-004, SC-007

Define el comportamiento observable del player local ante el foco de audio del sistema y su convivencia con Cast.

## Configuración del player (única, en `di/PlayerModule.kt`)

| Parámetro | Valor | Efecto |
|-----------|-------|--------|
| `AudioAttributes.usage` | `C.USAGE_MEDIA` | Solicita `AUDIOFOCUS_GAIN` al reproducir; `LOSS`/`GAIN` con semántica de medios |
| `AudioAttributes.contentType` | `C.AUDIO_CONTENT_TYPE_SPEECH` | Activa `willPauseWhenDucked()` de Media3: en `CAN_DUCK` **pausa**, no baja el volumen (decisión de clarify: sin duck) |
| `setAudioAttributes(attrs, handleAudioFocus = true)` | `true` | Media3 gestiona solicitud, pérdida y recuperación del foco |
| `setHandleAudioBecomingNoisy(true)` | `true` | Pausa al desconectar auriculares/Bluetooth |

## Matriz evento → comportamiento

| Evento del sistema | Condición previa | Comportamiento exigido | Resultado visible |
|--------------------|------------------|------------------------|-------------------|
| TolochaRadio pasa a reproducir | otra app suena | Solicitar `AUDIOFOCUS_GAIN`; la otra app se detiene o pausa si respeta el foco | Solo suena la radio (FR-002) |
| Otra app solicita foco (`AUDIOFOCUS_LOSS`) | radio reproduciendo | Pausa definitiva (`playWhenReady = false`), sin auto-reanudar | Estado `Paused`; el usuario decide (FR-001/FR-004) |
| Interrupción transitoria (`AUDIOFOCUS_LOSS_TRANSIENT`) | radio reproduciendo | Pausa por supresión; al recibir `AUDIOFOCUS_GAIN` reanuda automáticamente | Vuelve a sonar sola (FR-003) |
| Interrupción transitoria | usuario pausó manualmente durante la interrupción | No reanudar al recuperar el foco | Sigue en `Paused` (FR-003) |
| `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK` | radio reproduciendo | Pausar (no duck) y reanudar en `GAIN` | Silencio durante el aviso, vuelve después (FR-003) |
| Auriculares/Bluetooth desconectados | radio reproduciendo | Pausa (`AUDIO_BECOMING_NOISY`) | Estado `Paused` |
| Inicio/parada desde notificación, Bluetooth o background | cualquiera | Mismo comportamiento: el foco lo gestiona el player, no la UI | Idéntico a primer plano (FR-005) |
| Cast conectado | sesión remota activa | `CastPlayerManager` mantiene su foco manual; el `ExoPlayer` local no reproduce y no solicita foco | Sin doble petición ni mezcla local |

## Reglas de convivencia con Cast

1. Al conectar Cast: `createCastPlayer()` solicita foco manual (comportamiento existente, `CastPlayerManager.kt:172-193`).
2. Al desconectar/reanudar local: `releaseCastPlayer()` abandona su foco y restaura el `ExoPlayer` en la `MediaSession`; el player local vuelve a gestionar foco al reproducir.
3. Una sesión de emisión en dispositivo remoto no se ve afectada por el foco local (spec, supuestos).

## Casos borde cubiertos

- Pérdida de foco durante `Buffering`: la supresión deja el player listo para reanudar en `GAIN`.
- Interrupciones encadenadas: Media3 procesa cada cambio; `playWhenReady` del usuario manda.
- App externa que ignora el foco: TolochaRadio se pausa igualmente al recibir la notificación y nunca mezcla por su parte (SC-002).

## Mapeo de aceptación

| Escenario | Verificación |
|-----------|--------------|
| US1-1 (otra app empieza) | Manual: reproducir con VLC/Pocket Casts y comprobar pausa en ≤ 2 s (SC-002) |
| US1-2 (radio empieza) | Manual: con otra app sonando, pulsar play; la otra se detiene (SC-003) |
| US1-3 (interrupción + pausa manual) | Manual: simular llamada, pausar durante ella, volver; no reanuda (FR-003, SC-004) |
| US1-4 (background/Bluetooth) | Manual: repetir en background y con botón de auriculares (FR-005) |
| Unitaria | `PlayerAudioConfigTest`: `usage = USAGE_MEDIA`, `contentType = SPEECH` |

## Fuera de alcance

- Forzar el silencio de apps que no respetan el foco (no es posible desde la API pública).
- Foco en el dispositivo remoto de Cast.
