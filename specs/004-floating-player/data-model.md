# Data Model: Reproductor flotante inferior (004-floating-player)

**Spec**: [spec.md](spec.md) | **Research**: [research.md](research.md)

Sin cambios de esquema: no hay BD, red ni DTOs nuevos. `StationDto` (`id, name, url, homepage, favicon, country, language, tags, codec, bitrate, …`) se reutiliza tal cual; el modelo de esta feature es **estado de UI derivado** + 2 funciones puras.

## Entidades

| Entidad | Origen | Campos | Reglas |
|---------|--------|--------|--------|
| `PlayerState` (existente, se amplía) | `feature/player/PlayerViewModel.kt` | `Idle / Buffering(station) / Playing(station) / Paused(station) / Error(station?, message)` + nuevo `isMuted: StateFlow<Boolean>` paralelo | El silencio es modificador independiente: se combina con `Playing`/`Buffering`/`Paused`; en `Error` se oculta (FR-003b); se resetea a `false` en `play()` y `stop()` |
| `PanelContent` (derivado, no persistido) | Derivado de `PlayerState` + `isMuted` | `station, titleLine (= name), subtitleLine (= panelSubtitle(station)), artworkKey (= favicon), actions` | Visible si estado ≠ `Idle`; oculto en `Idle`. Sin red ni disco: vive en memoria del VM compartido |
| `CopyPayload` | `resolveCopyLink(station)` | `String?` | `station.url` en blanco → `null` (UI muestra "enlace no disponible", no copia nada). Jamás URL del proxy |

## Funciones puras (unit-testeables)

- `panelSubtitle(station): String` → `"{country} · {language} · {codec} {bitrate} kbps"`, omitiendo cada parte ausente/vacía; si no hay ninguna → `"Emisora de radio"`. Bitrate solo si `> 0`; codec tal cual (p. ej. `MP3 128 kbps`).
- `resolveCopyLink(station): String?` → `station.url.trim().ifEmpty { null }`.

## Transiciones de estado (panel)

```text
Idle ──play()──▶ Buffering ──listo──▶ Playing ⇄ Paused (toggle)
  ▲                │  │                    │  │
  │                │  │ cancelLoad()       │  │ mute on/off (solo volumen)
  │                │  └────────────────────┘  ▼
  │                ▼                     (isMuted=true/false, sin cambio de estado)
  │              Error ──retry()──▶ Buffering (reintentar+copiar visibles, mute oculto)
  │                │
  └──── stop() ◀───┴─── (solo desde full-player / cancelLoad en Buffering)
```

- Navegar entre secciones: sin transición (mismo VM compartido, R3).
- `play(otraEmisora)`: `isMuted=false`, volumen `1f`, `Buffering(nueva)`.
- Rotación/background: sin transición (VM de Activity + `MediaSessionService`).
- Sin imagen o técnicas ausentes: misma transición; solo cambia el render (avatar genérico / fallback de texto).

## Validación

- `subtitle` nunca vacío, nunca identificador interno, nunca texto de error técnico (FR-002).
- `CopyPayload==null` → no se escribe al portapapeles (FR-006).
- `isMuted` jamás `true` en `Idle` (se resetea al salir de reproducción).
