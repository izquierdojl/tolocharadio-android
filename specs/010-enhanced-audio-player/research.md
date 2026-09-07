# Research: Enhanced Audio Player UX

**Feature**: 010-enhanced-audio-player
**Date**: 2026-09-07

## R1: Customización de notificación Media3

### Contexto

El proyecto usa `DefaultMediaNotificationProvider` de Media3 1.4.1 en `RadioPlaybackService.kt`. Este provider genera automáticamente notificaciones con controles play/pause basados en el estado del player. Sin embargo, no incluye botón de stop por defecto y el comportamiento de pausa (mantener notificación visible) depende de la configuración del provider.

### Investigación

Media3 `DefaultMediaNotificationProvider` soporta:
- `setSmallIcon()` — ya usado con `R.drawable.sierra_emblem_mono`
- Control de acciones vía `MediaNotification.ActionFactory` personalizado
- El comportamiento de persistencia en pausa está controlado por `MediaSessionService.onTaskRemoved()` — por defecto, si el player está pausado, el servicio NO se destruye (la notificación persiste)

Para añadir botón de stop: Media3 usa `COMMAND_STOP` en la sesión. El `DefaultMediaNotificationProvider` no muestra stop por defecto; se requiere un `MediaNotification.Provider` personalizado o configurar las acciones de la notificación vía `MediaSession.setMediaButtonPreferences()`.

### Decisión

Usar `DefaultMediaNotificationProvider` con configuración de acciones personalizadas vía `MediaSession` para incluir stop. No requiere un provider completamente personalizado.

### Alternativas consideradas

1. **Provider personalizado completo** — Demasiado complejo para el caso de uso; YAGNI.
2. **NotificationManager nativo sin Media3** — Rompe la integración con MediaSession (controles externos, Bluetooth, Android Auto). Prohibido por constitución.
3. **Mantener solo play/pause** — No cumple FR-001/FR-003 (stop debe estar disponible).

---

## R2: Persistencia del mini-player al salir/volver a la app

### Contexto

Bug reportado: al minimizar la app (home) y volver a entrar, el mini-player desaparece aunque la reproducción continúa en background. El `PlayerViewModel` es un `@HiltViewModel` con scope de navegación; al recrear la Activity, el ViewModel se re-crea en `Idle`.

### Investigación

El problema es que `PlayerViewModel` guarda el estado en un `MutableStateFlow<PlayerState>` que se inicializa en `Idle`. Cuando la Activity se destruye y recrea (minimizar + volver), el ViewModel se re-crea desde cero. El `ExoPlayer` es `@Singleton` (inyectado por Hilt), por lo que sigue reproduciendo, pero el ViewModel no lee su estado actual.

La solución es que el ViewModel, en su `init`, sincronice el estado inicial del `ExoPlayer` compartido. Si `exoPlayer.isPlaying` o `exoPlayer.playWhenReady`, reconstruir el `PlayerState` adecuado. Esto requiere acceso al `StationDto` de la emisora actual — que se puede guardar en DataStore o en el propio MediaItem del ExoPlayer (vía `MediaItem.requestMetadata` o `extras`).

### Decisión

Persistir la emisora activa en DataStore (o en un `@Singleton` holder) y sincronizar el estado del ViewModel en `init` leyendo el estado real del ExoPlayer.

### Alternativas consideradas

1. **ViewModel con scope de Activity** — No resuelve el problema; el ViewModel sigue destruyéndose.
2. **Guardar StationDto en MediaItem.extras** — Funciona pero requiere deserialización manual y puede fallar con datos grandes.
3. **Singleton PlayerStateHolder** — Funciona pero añade complejidad sin beneficio sobre DataStore ya existente.

---

## R3: Station Info Sheet — datos disponibles

### Contexto

FR-004 requiere mostrar: nombre, país, idioma, tags, bitrate, codec, votos, clickCount, homepage. El `StationDto` ya tiene todos estos campos excepto `clickCount` que también está presente.

### Investigación

Campos disponibles en `StationDto`:
- `name: String` ✅
- `country: String?` ✅
- `language: String?` ✅
- `tags: List<String>` ✅
- `bitrate: Int?` ✅
- `codec: String?` ✅
- `votes: Int?` ✅
- `clickCount: Int?` ✅
- `homepage: String?` ✅
- `favicon: String?` ✅

No se requiere llamada adicional al backend — todos los datos ya están en el `StationDto` que el `PlayerViewModel` tiene en su estado.

### Decisión

Usar el `StationDto` existente del `PlayerState` directamente. Sin llamadas adicionales.

### Alternativas consideradas

1. **GET /stations/:id adicional** — Innecesario; todos los campos ya están en el DTO. Añadiría latencia y complejidad.

---

## R4: Comportamiento de notificación al pausar

### Contexto

Clarificación del spec: al pulsar pause en la notificación, esta debe permanecer visible con botón de play para reanudar. Solo desaparece al pulsar stop o deslizar.

### Investigación

Media3 `DefaultMediaNotificationProvider`:
- En pausa: la notificación permanece visible por defecto si el servicio sigue activo
- `onTaskRemoved()` en `MediaSessionService`: por defecto llama `player.stop()` solo si no está reproduciendo ni pausado con `playWhenReady`
- Para que la notificación persista en pausa: NO hacer `player.stop()` en `onTaskRemoved()` cuando el estado es pausado

La clave está en `MediaSessionService.onTaskRemoved()`. Por defecto Media3 lo maneja correctamente (no destruye si hay sesión activa), pero necesitamos verificar que no se destruya al deslizar la app de recientes.

### Decisión

Configurar `onTaskRemoved()` para NO destruir el servicio si el player está en estado pausado. Solo destruir si el usuario pulsó stop (estado Idle).

### Alternativas consideradas

1. **ForegroundService con tipo `mediaPlayback`** — Ya está declarado en el manifest. Solo requiere verificación.
2. **startForeground() manual** — No necesario; Media3 lo gestiona automáticamente.

---

## R5: Animaciones y transiciones del reproductor

### Contexto

SC-005: transiciones < 300ms sin parpadeos. US-4: experiencia visual profesional.

### Investigación

Compose `ModalBottomSheet` de M3 usa `animateContentSize()` por defecto con duración ~300ms. Para el mini-player, `AnimatedVisibility` con `fadeIn/fadeOut` + `slideInVertically/slideOutVertically` ofrece transiciones fluidas. `Crossfade` para cambios de estado (playing ↔ paused) evita parpadeos.

### Decisión

Usar `AnimatedVisibility` para el mini-player y `ModalBottomSheet` (ya existente) para el full-player. `Crossfade` para cambios de icono de estado.

### Alternativas consideradas

1. **Animaciones personalizadas con `animateXAsState`** — Más control pero más complejo. YAGNI para v1.
2. **Sin animaciones** — No cumple SC-005 ni US-4.
