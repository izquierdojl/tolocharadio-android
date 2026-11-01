# Contract: UI de reproducción continua

**Feature**: `0044-izquierdojl-20261101-live-playback-reconnect`
**Scope**: mini-player, reproductor completo, notificación multimedia y ficha de
emisora.

## Estados visibles

| Estado | Condición | Mini-player | Reproductor completo | Notificación |
|---|---|---|---|---|
| `CONNECTING` | Primera conexión o reintento antes del primer audio | Spinner; subtítulo «Cargando…» o «Reintentando…» | Indicador de progreso; texto equivalente | Estado de buffering estándar |
| `CONNECTING` sin red | Sin conectividad antes del primer audio | Spinner; subtítulo «Sin conexión. Se reanudará al volver la red.» | Texto equivalente | Estado de buffering estándar |
| `PLAYING` | Audio en reproducción | Nombre + pausa/stop | Controles normales | Play/pause + stop + mute |
| `RECONNECTING` | Corte después de haber sonado | Spinner; subtítulo «Reconectando (n/6)…» | Indicador de progreso; texto equivalente | Buffering estándar; sin texto duplicado |
| `RECONNECTING` sin red | Corte tras haber sonado y sin red | Spinner; subtítulo «Sin conexión. Se reanudará al volver la red.» | Texto equivalente | Buffering estándar; sin texto duplicado |
| `FAILED(RETRY)` | Se agotaron los intentos | Mensaje + icono «Reintentar» | Mensaje + botón «Reintentar» | Botón «Reintentar» + stop + mute |
| `FAILED(EDIT_SERVER)` | 401/403 no recuperable | Mensaje + icono «Editar servidor» | Mensaje + botón «Editar servidor» | Stop + mute; sin reintento ciego |
| `PAUSED` | Pausa manual o reanudación desactivada | Nombre + play | Play/stop | Play/pause estándar |

`n` es el número acumulado de intento (1..6). Antes del primer audio no se usa
la palabra «Reconectando»: se muestra «Reintentando…» dentro de `CONNECTING`.

## Textos

- «Cargando…»
- «Reintentando…»
- «Reconectando (n/6)…»
- «Sin conexión. Se reanudará al volver la red.»
- «Esta emisora usa un formato que este reproductor no admite.»
- «La emisora no está disponible en este momento.»
- «No se pudo recuperar la conexión. Comprueba tu red e inténtalo de nuevo.»
- «La sesión del servidor ha caducado. Edita el servidor para continuar.»
- «Se ha interrumpido la reproducción.»
- «Reintentar»
- «Editar servidor»

Todos los textos visibles están en español y no incluyen códigos técnicos crudos
ni PII.

## Acciones

### Reintentar

- Visible en mini-player, reproductor completo y notificación cuando el estado
  es `FAILED(RETRY)`.
- Un toque llama a `PlaybackRecoveryManager.manualRetry()`.
- Es idempotente: pulsaciones repetidas no abren conexiones paralelas.
- Reinicia el presupuesto de intentos y conserva la ruta actual; si ya se hizo
  el fallback, reintenta la ruta `LEGACY` para no repetir un 415 determinista.
- No duplica historial.

### Editar servidor

- Visible cuando el error es 401/403 no recuperable.
- Navega a la edición del servidor activo mediante `onEditServer`.
- No reintenta el stream automáticamente al volver.

### Pausa/stop durante la reconexión

- Pausa manual: cancela el ciclo y deja la emisora en `PAUSED`.
- Stop: cancela el ciclo y limpia la emisora.
- Cambio de emisora: cancela el ciclo anterior y reproduce la nueva.
- Temporizador de apagado vencido: cancela y detiene.
- Cast conectado: cancela la recuperación local; el audio remoto no se toca.

## Ficha de emisora

- Al abrirse, la ficha invoca `PlayerViewModel.checkAvailability(station)`, que
  comparte el presupuesto de 2 s con el play.
- Si el resultado tiene menos de 5 s, el play lo reutiliza y no repite
  `/status`.
- `playable=false`: muestra el motivo en español, no arranca el reproductor y
  deshabilita o sustituye el botón «Reproducir».
- Timeout o error de transporte: permite reproducir y muestra `CONNECTING`.
- La ficha muestra el mismo `PlayerState` que el reproductor
  (`CONNECTING`/`PLAYING`/`RECONNECTING`/`FAILED`) y la acción «Reintentar»
  cuando corresponda.

## Notificación

- Se conserva la notificación multimedia existente (Media3).
- El comando `CUSTOM_COMMAND_RETRY` y el botón «Reintentar» solo se muestran en
  `FAILED(RETRY)`.
- El resto de estados mantiene stop + mute y el estado de reproducción estándar.
- El texto detallado («Reintentando…», «Reconectando…») vive en el mini-player y
  en el reproductor completo; la notificación refleja el estado de transporte
  (buffering/playing/paused) y no duplica cada tick del watchdog.
- La ficha de emisora usa el mismo `PlayerState` y la misma acción de reintento
  que el reproductor completo.
- La notificación MUST seguir mostrando la emisora y el estado tras rotación,
  background y bloqueo de pantalla.

## Accesibilidad

- El panel anuncia el estado: «Cargando», «Reintentando», «Reconectando»,
  «Sin conexión», «Sonando», «Pausado» o «Error de reproducción».
- Los botones tienen `contentDescription` («Reintentar», «Editar servidor»,
  «Pausar», «Reanudar», «Detener»).
- El estado `RECONNECTING` no mueve el foco de TalkBack en cada tick del
  watchdog; se anuncia el cambio de estado, no cada segundo.
- El botón de reintento cumple el área táctil mínima de 48 dp.
- Los nuevos subtítulos se integran con las `semantics` existentes de Compose.

## Reglas de consistencia

1. El estado de transporte mostrado en mini-player, reproductor completo y
   notificación debe ser el mismo; la ficha de emisora usa el mismo
   `PlayerState`.
2. `CONNECTING` y `RECONNECTING` tienen prioridad sobre el estado crudo del
   player mientras el ciclo esté activo.
3. Al recuperar, el estado vuelve a `PLAYING` sin parpadeos intermedios a
   `Idle`; si el player emite `IDLE` durante el cambio de ruta, se mantiene el
   estado de conexión.
4. Rotación, navegación y recreación de Activity conservan el estado desde
   `ActiveStationHolder` y `RecoveryState`.
5. Una recuperación correcta no añade una entrada de historial.
6. Un 415 no deja un spinner indefinido: o suena la ruta de reserva o se muestra
   el mensaje con acción.

## Fuera de alcance

- No se añade un ajuste visible para activar/desactivar la ruta continua.
- No se muestran códigos HTTP, `PlaybackException` ni detalles técnicos.
- No se modifica el comportamiento de Chromecast.

## Trazabilidad

Cubre FR-003, FR-011..FR-018, FR-021 y FR-025; US1..US4; SC-003..SC-006.
