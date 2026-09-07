# Data Model: Enhanced Audio Player UX

**Feature**: 010-enhanced-audio-player
**Date**: 2026-09-07

## Entities

### PlayerState (existing — sealed interface)

Estado sellado del reproductor. Ya existe en `PlayerViewModel.kt`.

| Variant | Fields | Description |
|---------|--------|-------------|
| `Idle` | — | Sin reproducción activa. Mini-player oculto. |
| `Buffering` | `station: StationDto` | Cargando stream. Indicador de progreso visible. |
| `Playing` | `station: StationDto` | Reproducción activa. Animación sutil. |
| `Paused` | `station: StationDto` | Reproducción pausada. Notificación persiste. |
| `Error` | `station: StationDto?, message: String` | Error de stream. Mensaje en español + retry. |

**Transiciones**:
```
Idle → Buffering (play)
Buffering → Playing (stream ready)
Buffering → Error (stream fail / precheck fail)
Playing → Paused (toggle)
Paused → Playing (toggle)
Playing → Error (stream interrupted)
Error → Buffering (retry manual)
Any → Idle (stop)
```

**Cambio requerido**: Sin cambios a la estructura. Solo se modifica la lógica de inicialización del ViewModel para sincronizar con el ExoPlayer compartido al recrearse.

---

### StationDto (existing — data class)

DTO de emisora del backend. Ya existe en `StationDtos.kt`. Se usa directamente en toda la app (sin dominio separado).

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | `String` | No | Identificador único de la emisora |
| `name` | `String` | No | Nombre de la emisora |
| `url` | `String` | No | URL del stream (solo vía proxy) |
| `homepage` | `String?` | Sí | Sitio web de la emisora |
| `favicon` | `String?` | Sí | URL del logo/favicon |
| `country` | `String?` | Sí | País de la emisora |
| `countryCode` | `String?` | Sí | Código ISO del país |
| `language` | `String?` | Sí | Idioma principal |
| `tags` | `List<String>` | No | Géneros/etiquetas |
| `codec` | `String?` | Sí | Códec de audio (MP3, AAC, etc.) |
| `bitrate` | `Int?` | Sí | Bitrate en kbps |
| `votes` | `Int?` | Sí | Votos de usuarios |
| `clickCount` | `Int?` | Sí | Reproducciones totales |
| `isSsl` | `Boolean?` | Sí | Usa HTTPS |
| `lastCheckOk` | `Boolean?` | Sí | Último check exitoso |
| `isCustom` | `Boolean` | No | Emisora personalizada del usuario |

**Cambio requerido**: Ninguno. Todos los campos para el Station Info Sheet ya existen.

---

### ActiveStationHolder (new — singleton)

Holder temporal para preservar la emisora activa entre recreaciones de Activity/ViewModel.

| Field | Type | Description |
|-------|------|-------------|
| `station` | `StationDto?` | Emisora actualmente en reproducción/pausa |
| `playerState` | `String` | Estado textual: "playing", "paused", "buffering", "error" |

**Lifecycle**: Se escribe en `PlayerViewModel.play()` y se limpia en `stop()`. Se lee en `init` para sincronizar el estado.

**Alternativa**: Persistir en DataStore como JSON. Rechazado por YAGNI — el holder es suficiente para el scope de una sesión de app.

---

### MediaNotification.Actions (conceptual — not a class)

Acciones de notificación configuradas en `RadioPlaybackService`.

| Action | Media3 Command | Behavior |
|--------|---------------|----------|
| Play | `COMMAND_PLAY_PAUSE` | Reanuda reproducción |
| Pause | `COMMAND_PLAY_PAUSE` | Pausa reproducción, notificación persiste |
| Stop | `COMMAND_STOP` | Detiene reproducción, elimina notificación |

**Implementación**: Configurar `MediaSession.setMediaButtonPreferences()` para incluir stop en la notificación.
