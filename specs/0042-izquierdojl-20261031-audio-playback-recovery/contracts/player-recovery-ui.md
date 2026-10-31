# Contract: UI de recuperación del reproductor

**Feature**: `0042-izquierdojl-20261031-audio-playback-recovery`
**Scope**: mini-player, reproductor completo y notificación multimedia.

## Estados visibles

| Estado | Mini-player | Reproductor completo | Notificación |
|---|---|---|---|
| `Playing` | Nombre + subtítulo técnico; botón pausa | Nombre; botón pausa/stop | Play/pause + stop + mute |
| `Buffering` | Spinner; botón cancelar | Barra de progreso; botón cancelar | Play/pause estándar |
| `Reconnecting` | Spinner; subtítulo "Reconectando (n/5)…" | Barra de progreso; texto "Reconectando (n/5)…" | Sin acción extra; estado de reproducción estándar |
| `WaitingForNetwork` | Spinner; subtítulo "Sin conexión. Se reanudará al volver la red." | Texto "Sin conexión"; sin botón de reintento mientras espera | Sin acción extra |
| `Paused` | Nombre; botón play | Botón play/stop | Play/pause estándar |
| `Error(RETRY)` | Subtítulo con mensaje; icono "Reintentar" | Mensaje + botón "Reintentar" | Botón "Reintentar" + stop + mute |
| `Error(EDIT_SERVER)` | Subtítulo con mensaje; icono "Editar servidor" | Mensaje + botón "Editar servidor" | Botón "Reintentar" no aplica; se mantiene stop/mute |

`WaitingForNetwork` se representa en el código como
`PlayerState.Reconnecting(waitingForNetwork = true)`; no es un subtipo adicional.

## Acciones

### Reintentar

- Visible en mini-player, reproductor completo y notificación cuando el estado
  es `Error` con acción `RETRY`.
- Un toque llama a `PlaybackRecoveryManager.manualRetry()`.
- Es idempotente: pulsaciones repetidas no crean conexiones paralelas; si ya hay
  un ciclo en curso, se ignora o se reutiliza el ciclo activo.
- Inicia un ciclo nuevo de hasta 5 intentos/60 s.
- No registra historial duplicado.

### Editar servidor

- Visible cuando el error es de credenciales no recuperables (401/403).
- Un toque navega a la edición del servidor activo mediante el callback
  `onEditServer` que ya usa el resto de la app.
- No reintenta el stream automáticamente al volver; el usuario puede editar y
  guardar credenciales.

### Pausa/stop durante recuperación

- Pausa manual: cancela el ciclo y deja la emisora en `Paused`.
- Stop: cancela el ciclo y limpia la emisora.
- Cambio de emisora: cancela el ciclo anterior y reproduce la nueva.
- Temporizador de apagado vencido: cancela y detiene.
- Cast conectado: cancela la recuperación local; el audio remoto no se toca.

## Notificación

- El comando `CUSTOM_COMMAND_RETRY` se añade al `MediaSession.Callback`.
- El botón de reintento solo se muestra en `Error(RETRY)`; en el resto de
  estados se mantiene el layout actual (stop + mute).
- El botón usa un icono de reproducción con `displayName` "Reintentar" (Media3
  1.11 no incluye un icono de refresh específico).
- La notificación MUST seguir mostrando la emisora y el estado tras rotación,
  background y bloqueo de pantalla.

## Textos

- "Reconectando (n/5)…"
- "Sin conexión. Se reanudará al volver la red."
- "No se pudo recuperar la conexión. Comprueba tu red e inténtalo de nuevo."
- "La emisora no está disponible en este momento."
- "La sesión del servidor ha caducado. Edita el servidor para continuar."
- "Se ha interrumpido la reproducción."
- "Reintentar"
- "Editar servidor"

Todos los textos visibles están en español y no incluyen datos técnicos crudos
ni PII.

## Accesibilidad

- El panel anuncia el estado: "Sonando", "Pausado", "Cargando",
  "Reconectando", "Sin conexión" o "Error de reproducción".
- Los botones tienen `contentDescription` ("Reintentar", "Editar servidor",
  "Pausar", "Reanudar", "Detener").
- El estado "Reconectando" no debe mover el foco de TalkBack repetidamente en
  cada intento; se anuncia el cambio de estado, no cada tick del watchdog.
- El botón de reintento cumple el área táctil mínima de 48 dp.

## Reglas de consistencia

1. El estado mostrado en mini-player, reproductor completo y notificación debe
   ser el mismo.
2. `Reconnecting` y `Error` tienen prioridad sobre el estado crudo del player
   mientras el ciclo de recuperación esté activo.
3. Al recuperar, el estado vuelve a `Playing` sin parpadeos intermedios a
   `Idle`; si el player emite `IDLE` durante el swap, la UI mantiene el estado
   de recuperación.
4. Rotación, navegación y recreación de Activity conservan el estado desde
   `ActiveStationHolder` y `RecoveryState`.
5. Una recuperación correcta no añade una entrada de historial.

## Trazabilidad

Cubre FR-001 a FR-009 y FR-014 a FR-022; US1, US2 y US4.
