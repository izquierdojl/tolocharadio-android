# Contract: Intent de acceso directo → app

**Feature**: `0018-jlizquierdo-20260910-app-shortcut-recent-stations` | **Tipo**: contrato externo (lanzador del sistema → app)

Define cómo el menú del icono (shortcut dinámico) arranca la app y qué debe hacer esta. Cubre FR-005, FR-006, FR-007, FR-011 y FR-013.

## Emisor y receptor

- **Emisor**: el lanzador del dispositivo al pulsar un shortcut dinámico publicado por la app.
- **Receptor**: `com.izquierdojl.tolocharadio.MainActivity` (`launchMode="singleTask"`, ya existente).
- El intent es **explícito** (componente fijado): no se requieren `intent-filter` ni deep links web.

## Acción y extras

| Campo | Valor | Obligatorio |
|-------|-------|-------------|
| Action | `com.izquierdojl.tolocharadio.OPEN_STATION` | Sí |
| Extra `station_id` | `String` con el id de la emisora | Sí |
| Extra `station_name` | `String` con el nombre público de la emisora | No (solo UI/mensajes) |

Reglas:
- PROHIBIDO incluir tokens, email, ids de usuario o cualquier dato personal en el intent o en los extras.
- Si `station_id` falta o es blank, el intent se ignora silenciosamente (sin crash, sin navegación).
- Un intent con action distinta se ignora por este contrato (otros flujos, p. ej. notificaciones, no se ven afectados).

## Comportamiento por estado de la app

| Estado | Secuencia esperada |
|--------|--------------------|
| Proceso no ejecutado (arranque en frío) | `onCreate` parsea el intent → pendiente; se completa `SessionRestorer.restore()`; con sesión válida se reproduce y se abre el reproductor a pantalla completa |
| En segundo plano | `onNewIntent` parsea → pendiente; la app pasa a primer plano, reproduce y abre el reproductor a pantalla completa |
| En primer plano | `onNewIntent` parsea → pendiente; cambia la reproducción de forma controlada y abre el reproductor |
| Dispositivo bloqueado | El sistema exige desbloqueo antes de abrir la app; la reproducción no arranca hasta que la app esté en foreground |
| Sesión caducada / ausente | No se reproduce; se navega a Login con aviso en español (ver abajo) |

## Resolución de la emisora

1. `AuthState.Loading` → esperar (no consumir el pendiente).
2. `AuthState.Unauthenticated` → navegar a `Routes.LOGIN` + snackbar "Tu sesión ha caducado. Inicia sesión de nuevo." y limpiar el pendiente.
3. `AuthState.Authenticated`:
   - buscar `station_id` en el historial en memoria; si no, en la caché Room;
   - si no aparece, `GET /stations/{id}`;
   - si el precheck `GET /playback/{id}/status` (ya dentro de `play()`) o la carga fallan → mensaje accionable en español, sin abrir el reproductor ni quedarse colgado.

## Idempotencia y concurrencia

- Pulsar el shortcut de la emisora que ya está sonando NO reinicia ni duplica la reproducción (FR-013); el reproductor se muestra con el estado actual.
- Pulsaciones repetidas antes de consumir el pendiente: prevalece la última (el holder conserva un único valor).
- La reproducción siempre pasa por `PlayerViewModel.play()`, que cancela cualquier carga previa (`loadJob?.cancel()`).

## Errores visibles (en español, sin texto técnico)

| Situación | Mensaje/comportamiento |
|-----------|------------------------|
| Emisora no existe o eliminada (404) | "Emisora no disponible." + no se abre el reproductor |
| No reproducible (`playable=false` o motivo) | Motivo del servidor si es apto; si no, "Emisora no disponible." con opción de reintentar |
| Sin red al pulsar | Error de reproducción existente con reintento (FR-015) |
| Sesión caducada y refresh fallido | Redirección a Login con aviso, sin reintentos en bucle |

## Criterios de aceptación cubiertos

- FR-005 (abrir/reproducir con full player), FR-006 (tres estados de app), FR-007 (sesión), FR-011 (no disponible), FR-013 (misma emisora), FR-015 (offline).
