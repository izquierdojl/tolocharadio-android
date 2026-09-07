# UI Contract: Station Info Sheet

**Feature**: 010-enhanced-audio-player
**Date**: 2026-09-07

## Component: StationInfoSheet

Bottom sheet modal que muestra información completa de la emisora al tocar el logo/favicon en el mini-player o full-player.

### Trigger

- **Mini-player**: tap en `StationArtwork` o zona de identidad (`PanelIdentity`)
- **Full-player**: tap en el logo/favicon mostrado en el `FullPlayerSheet`

### Data Source

`StationDto` del `PlayerState` actual (Buffering, Playing, Paused o Error). Sin llamadas adicionales al backend.

### Layout

```
┌─────────────────────────────────────┐
│  ┌─────────┐                        │
│  │  FAVICON │  Station Name          │
│  │ (64dp)  │  Country · Language     │
│  └─────────┘  Tags                   │
│                                      │
│  ─────────────────────────────────── │
│                                      │
│  🎵 Audio         📊 Estadísticas    │
│  Codec: MP3        Votos: 1,234      │
│  Bitrate: 128k     Clicks: 5,678     │
│  SSL: Sí           Último check: OK  │
│                                      │
│  ─────────────────────────────────── │
│                                      │
│  🌐 Homepage                         │
│  [Enlace clicable]                   │
│                                      │
│  ─────────────────────────────────── │
│                                      │
│  [  Cerrar  ]                        │
└─────────────────────────────────────┘
```

### Fields Displayed

| Field | Source | Format | Fallback |
|-------|--------|--------|----------|
| Favicon | `station.favicon` | Coil AsyncImage, 64dp rounded | Placeholder con emblema Sierra |
| Name | `station.name` | titleMedium, max 2 lines | "Emisora" |
| Country | `station.country` | bodyMedium | "—" |
| Language | `station.language` | bodyMedium | "—" |
| Tags | `station.tags` | Chips o texto plano | "Sin etiquetas" |
| Codec | `station.codec` | bodyMedium, uppercase | "—" |
| Bitrate | `station.bitrate` | "${bitrate} kbps" | "—" |
| Votes | `station.votes` | Formatted number (1,234) | "—" |
| Clicks | `station.clickCount` | Formatted number | "—" |
| Last Check | `station.lastCheckOk` | "OK" / "Con problemas" | "—" |
| Homepage | `station.homepage` | Clickable link → Intent.ACTION_VIEW | Sección oculta |

### Behavior

- **Dismiss**: tap fuera, swipe down, botón "Cerrar"
- **Reproducción**: NO se interrumpe al abrir/cerrar el sheet
- **Homepage link**: abre navegador externo vía `Intent.ACTION_VIEW`
- **Scroll**: contenido scrollable si excede la altura del sheet

### Accessibility

- `contentDescription` en favicon: "Logo de {stationName}"
- Homepage link: `Role.Link` con `onClickLabel = "Abrir sitio web"`
- Sheet dismissible con botón de accesibilidad

### Theme

- Colores: paleta Tema Tolocha (verde-bosque, ocre, pine-950)
- Tipografía: Material3 (titleMedium, bodyMedium, labelSmall)
- Fondo: `MaterialTheme.colorScheme.surface`
- Separadores: `MaterialTheme.colorScheme.outlineVariant`

### State Transitions

```
Logo tap → Sheet appears (enter animation ~300ms)
Sheet open → Dismiss (exit animation ~300ms)
Sheet open → Homepage tap → External browser
```
