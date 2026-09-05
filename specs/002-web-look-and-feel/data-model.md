# Data Model: look-and-feel web (sin entidades de negocio)

**Feature**: `002-web-look-and-feel` — esta feature no crea entidades de dominio ni cambia DTOs/contrato `/api/v1`. Modela solo **tokens de diseño**, **assets de marca** y la **preferencia de tema**.

## 1. DesignTokens (por tema)

| Campo | Tipo | Reglas |
|-------|------|--------|
| pine50…pine950 | Color hex | Escala fija web: `50 #f0f5f1, 100 #deeae2, 200 #bcd2c4, 300 #8cb29a, 400 #5f8f73, 500 #3c6a4d, 600 #2c4f38, 700 #203a28, 800 #17291c, 850 #122016, 900 #0f1a12, 950 #08100b` |
| ochre100…ochre950 | Color hex | `100 #f7ecdb, 200 #efdabb, 300 #e2c091, 400 #d3a568, 500 #c0883e, 600 #a67430, 700 #8a5f26, 800 #6b4a1d, 900 #4a3314, 950 #2b1d0c` |
| moss300/400/500 | Color hex | `#cfdaa2 / #b4c47e / #9aae63` (acentos, no texto) |
| surface / surfaceRaised / surfaceSoft | rol → Color | dark: `pine-950 / pine-900 / pine-900@50%`; light: `#eef3ef / #ffffff / #f5f9f6` |
| foreground / muted / soft / faint | rol → Color | dark: `pine-100 / pine-400 / pine-300 / pine-500`; light: `#123424 / #4c6b58 / #263f30 / #2c4f38` |
| line / lineStrong | rol → Color | dark: `pine-800 / pine-700`; light: `#d9e4dc / #c4d4c9` |
| brand / onBrand | rol → Color | dark `ochre-400 / pine-950`; light `ochre-600 (o 700 si WCAG lo exige) / white` |
| mountain | rol → Color | dark `rgba(32,58,40,0.45)`; light `#b4c9b9` |
| radii | dp | card 16, button/field 12 (M3 `medium`), chip/play/avatar/fab 50% (full) |

Validación: todo par texto/fondo y marca/fondo MUST cumplir WCAG AA (test en `research.md` §7). Transiciones: `SYSTEM→resuelto` según `isSystemInDarkTheme()`; `DARK|LIGHT` fijos.

## 2. BrandAsset

| Campo | Tipo | Reglas |
|-------|------|--------|
| id | enum | `SIERRA_EMBLEM / FAVICON / MOUNTAIN_WALL / GITHUB` |
| sourceUrl | string | URL cruda `raw.githubusercontent.com/.../main/apps/web/...` + commit congelado; documentado en `contracts/brand-assets.md` |
| variants | set | `full-dark, full-light, mono` (mono obligatoria para notificación) |
| usages | set | `header-logo, launcher, splash, notification-small, notification-large, placeholder-custom, empty-state, shortcuts, widget` |
| nightAware | bool | `true` para full (qualifier `night`), `false` para mono monocromo |

Relaciones: `ComponentStyle` 1—N `BrandAsset` (placeholder/empty-state/logo referencian `SIERRA_EMBLEM`); `Launcher` 1—1 `SIERRA_EMBLEM.mono/full-simplificada`.

## 3. ThemePreference (única persistencia nueva)

| Campo | Tipo | Reglas |
|-------|------|--------|
| mode | enum `SYSTEM/DARK/LIGHT` | Default `SYSTEM`; persistido en DataStore `theme_mode`; cambio visible <1s y sin mezcla de temas |
| source | const | `LOCAL_ONLY` (no se sincroniza con `User.theme` del servidor — clarificación A) |

Estado: no hay máquina de estados de negocio; el estado de UI del player (`Idle/Buffering/Playing/Paused/Error`, constitución IV) no cambia, solo su estilo.

## 4. ComponentStyle (resumen de equivalencia)

`StationCard` (imagen 16:9 + degradado `pine-950/80` + play circular + favorito + título + país·idioma + ≤3 chips uppercase + bitrate) · `StationListItem` (artwork 48dp + headline + supporting + trailing favorito) · `ChipTag` (`pine-800/pine-300` dark) · `PlayButton` (reposo `black/50+pine-100`, reproduciendo `ochre-500+pine-950`) · `Header/Logo` (Sierra 36dp + "Tolocha**Radio**") · `MountainWall` (1200×120, color `mountain`) · `EmptyState` (icono + título + acción, tono muted).
