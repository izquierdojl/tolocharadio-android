# Playback & Notification Contract: Mejoras de experiencia podcast

**Feature**: `0047-izquierdojl-20261119-podcast-ux-polish` | **Date**: 2026-11-19

Contrato de la sesión multimedia y de los controles de transporte. Aplica a la notificación multimedia, la pantalla de bloqueo, los controles de Bluetooth y la UI interna.

---

## 1. Configuración del reproductor

| Ajuste | Valor | Ámbito |
|--------|-------|--------|
| Incremento de retroceso | `15_000 ms` | Todos los players locales (radio y episodios) |
| Incremento de avance | `30_000 ms` | Todos los players locales (radio y episodios) |
| Comandos de seek | `COMMAND_SEEK_BACK`, `COMMAND_SEEK_FORWARD` | Disponibles según el contenido |
| Foco de audio | Sin cambios (`PlayerAudioConfig`) | — |
| Procesador de voz | Solo episodios locales | Ver contrato de voz |

Los incrementos se definen una sola vez y se comparten con `EPISODE_SKIP_BACK_SEC` / `EPISODE_SKIP_FORWARD_SEC` para que UI y notificación no diverjan.

---

## 2. Matriz de botones de la sesión

`RadioPlaybackService` publica las preferencias de botones según el contenido activo:

| Contenido | Slot BACK | Slot CENTRAL | Slot FORWARD | Overflow |
|-----------|-----------|--------------|--------------|----------|
| Radio en directo | — | Play/Pausa (sistema) | — | Stop, Mute |
| Radio con fallo recuperable | — | Play/Pausa (sistema) | — | Retry, Stop, Mute |
| Episodio local | Retroceder 15 s | Play/Pausa (sistema) | Avanzar 30 s | Stop, Mute |
| Episodio en Cast | Retroceder 15 s | Play/Pausa (sistema) | Avanzar 30 s | Stop, Mute |
| Sin reproducción | — | Play (sistema) | — | Stop, Mute |

### Botones

| Botón | Icono | Comando | Etiqueta accesible |
|-------|-------|---------|--------------------|
| Retroceder 15 s | `CommandButton.ICON_SKIP_BACK_15` | `Player.COMMAND_SEEK_BACK` | “Retroceder 15 segundos” |
| Avanzar 30 s | `CommandButton.ICON_SKIP_FORWARD_30` | `Player.COMMAND_SEEK_FORWARD` | “Avanzar 30 segundos” |
| Stop | `ICON_STOP` | Comando de sesión existente | “Detener” |
| Mute | `ICON_VOLUME_OFF` | Comando de sesión existente | “Silenciar” |
| Retry | `ICON_PLAY` | Comando de sesión existente | “Reintentar” |

- Los botones de seek usan los slots `SLOT_BACK` / `SLOT_FORWARD`; el proveedor por defecto los coloca en la vista compacta y sustituye a anterior/siguiente.
- Stop, mute y retry usan `SLOT_OVERFLOW` para no desplazar play/pausa.
- Las preferencias se recalculan al cambiar el estado del episodio (Idle ↔ Active) y el estado de recuperación.

---

## 3. Reglas de comportamiento

### Episodio

- Retroceder: `position - 15 s`, acotado a `0`.
- Avanzar: `position + 30 s`, acotado a la duración.
- Avanzar hasta el final dispara el flujo normal de completado y auto-avance.
- Disponible con el episodio en reproducción o pausado.
- La UI y la notificación comparten el estado: un salto en la notificación se refleja al abrir la app y viceversa.
- Con el episodio descargado se aplica igual (archivo local).

### Radio en directo

- No se publican botones de retroceso/avance.
- Aunque el player bufferee audio, no se ofrece un seek sin sentido.
- Stop/mute/retry conservan el comportamiento actual.

### Cast

- La sesión remota mantiene sus propios controles.
- El teléfono no debe mostrar acciones que contradigan al receptor.
- El potenciador de voz nunca se aplica a la salida de Cast.

### Contenido no desplazable

- Si `COMMAND_SEEK_BACK` / `COMMAND_SEEK_FORWARD` no están disponibles, no se publican los botones.
- Nunca se muestra un botón que no haga nada.

---

## 4. Accesibilidad

- Cada botón tiene etiqueta en español y se anuncia con TalkBack.
- Los botones mantienen el tamaño táctil del sistema.
- El estado (pausado/reproduciendo) se anuncia sin depender solo del icono.
- La notificación mantiene `VISIBILITY_PUBLIC` y el tap abre la app en la pantalla actual.

---

## 5. Matriz de validación

| Escenario | API | Resultado esperado |
|-----------|-----|--------------------|
| Episodio sonando, pantalla bloqueada | 26, 33, 37 | Se ven retroceder/play-pausar/avanzar; los saltos son 15/30 s |
| Episodio pausado | 26, 33, 37 | Los botones siguen disponibles |
| Episodio al segundo 5, retroceder | 26, 33, 37 | Queda en 0 sin error |
| Episodio a 20 s del final, avanzar | 26, 33, 37 | Llega al final y completa/auto-avanza |
| Radio en directo | 26, 33, 37 | No hay retroceder/avanzar; stop/mute siguen |
| Fallo recuperable de radio | 26, 33, 37 | Aparece Retry; no hay seek |
| Episodio en Cast | 33, 37 | Los controles remotos mandan; no hay contradicción |
| Bluetooth (play/pausa) | 26, 33, 37 | Sin regresión |
| Notificación sin reproducción | 26, 33, 37 | Play disponible; sin seek |

---

## 6. Fuera de alcance

- Notificación completamente personalizada.
- Controles de velocidad o temporizador en la notificación.
- Android Auto, Wear OS y widgets.
- Cambios en el canal de notificación o en el icono pequeño.
