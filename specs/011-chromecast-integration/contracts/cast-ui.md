# UI Contract: Chromecast Integration

**Feature**: 011-chromecast-integration
**Date**: 2026-09-07

## Component 1: Cast Button (TopAppBar)

Botón de Chromecast en la barra superior, integrado como `MediaRouteButton` via `AndroidView` interop.

### Location

`TolochaNavGraph.kt` — TopAppBar `actions`, antes del `ViewModeToggle` (si aplica) y el botón de Servidores.

### Behavior

| State | Appearance | Action |
|-------|------------|--------|
| No devices on network | Gris, atenuado | Tap: no action (o toast informativo) |
| Devices available | Color activo (theme primary) | Tap: abre picker de dispositivos Cast |
| Connected to device | Color activo + indicador | Tap: muestra opciones (disconnect) |

### Integration

```kotlin
// En TopAppBar > actions
AndroidView(factory = { ctx ->
    MediaRouteButton(ctx).apply {
        CastButtonFactory.setUpMediaRouteButton(ctx, this)
    }
})
```

### Accessibility

- `contentDescription`: "Chromecast" / "Conectado a {deviceName}"
- Announce connection state changes via TalkBack

---

## Component 2: Cast Indicator (Mini-player)

Indicador visual en el mini-player cuando el audio se reproduce en Chromecast.

### Location

`PlayerUi.kt` — `PanelIdentity`, debajo del nombre de la emisora (subtitle area).

### Layout

```
┌─────────────────────────────────────────────┐
│  ┌─────────┐                                │
│  │  LOGO   │  Station Name                  │
│  │ (48dp)  │  🔴 Living Room TV             │
│  └─────────┘                                │
│           [⏸] [🔇] [📋]                    │
└─────────────────────────────────────────────┘
```

### States

| Condition | Subtitle Text | Icon |
|-----------|---------------|------|
| Local playback | Technical info (codec, bitrate) | None |
| Cast connected | "{deviceName}" | Cast icon (small, before text) |
| Cast buffering | "Conectando a {deviceName}..." | Cast icon + progress |
| Cast error | "Error en Chromecast" | Error color |

### Data Source

- Device name: from `CastPlayerState.Cast.deviceName`
- Connection state: from `CastPlayerState.Cast.connectionState`

### Behavior

- Cast indicator replaces the technical subtitle line when active
- Transitions: crossfade between local subtitle and Cast indicator
- Tap on indicator: no action (informational only)

---

## Component 3: Cast State in Full Player

El full player (bottom sheet) debe reflejar el estado de Cast.

### Location

`PlayerUi.kt` — `FullPlayerSheet`

### Behavior

When connected to Cast:
- Show device name below station name
- Show Cast icon next to device name
- Controls (play/pause/stop) work on CastPlayer
- Volume slider controls Cast volume (not local)

### Layout Addition

```
┌─────────────────────────────────────────────┐
│  ┌─────────┐                                │
│  │  LOGO   │  Station Name                  │
│  │ (48dp)  │  🔴 Living Room TV             │
│  └─────────┘                                │
│                                              │
│  [  ▶  ] [  ⏹  ] [  🔇  ]                  │
│                                              │
│  ─── Volumen Chromecast ───────────────────  │
│  🔈 ━━━━━━━━━━━━━━━━━━━━━━━━━━━ 🔊          │
└─────────────────────────────────────────────┘
```

---

## Component 4: Error Snackbar (Connection Failure)

Snackbar que se muestra cuando la conexión al Chromecast falla.

### Trigger

- `CastPlayerManager` emite `CastConnectionState.DISCONNECTED` después de un `CONNECTING` fallido

### Content

| Element | Value |
|---------|-------|
| Message | "No se pudo conectar al dispositivo" |
| Action | "Reintentar" button |
| Duration | `SnackbarDuration.Long` |

### Behavior

- Tap "Reintentar": intenta reconectar al último dispositivo seleccionado
- Auto-dismiss después de 4 segundos si no hay acción
- No interrumpe la reproducción local actual

---

## Theme Integration

| Element | Token |
|---------|-------|
| Cast icon color (active) | `MaterialTheme.colorScheme.primary` |
| Cast icon color (disabled) | `MaterialTheme.colorScheme.onSurfaceVariant` (alpha 0.5) |
| Cast indicator text | `MaterialTheme.typography.bodySmall` |
| Error text | `MaterialTheme.colorScheme.error` |
| Snackbar | Default Material3 snackbar styling |
