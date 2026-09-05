# Research: look-and-feel web → Android

**Feature**: `002-web-look-and-feel` | **Date**: 2026-09-05

Fuente inspeccionada (2026-09-05, rama `main` de `izquierdojl/tolocharadio`): `apps/web/src/index.css`, `apps/web/src/components/{SierraEmblem,AppShell,StationCard,StationListItem,PlayerBar,EmptyState,ThemeToggle}.tsx`, `apps/web/index.html`. Código Android inspeccionado: `core/ui/theme/Theme.kt`, `core/ui/components/CommonUi.kt`, `core/ui/navigation/TolochaNavGraph.kt`, `res/drawable|mipmap-anydpi|values(-night)`, `AndroidManifest.xml`.

## Decisiones

### 1. Traducción de tokens CSS → M3 `ColorScheme`

- **Decision**: Mapear roles web a M3 así (dark / light):
  - `surface` → `background`+`surface` (dark `pine-950 #08100b` / light `#eef3ef`); `surface-raised` → `surfaceContainer` (dark `pine-900 #0f1a12` / light `#ffffff`); `surface-soft` → `surfaceContainerLow`.
  - `foreground` → `onBackground`+`onSurface` (dark `pine-100 #deeae2` / light `#123424`); `muted` → `onSurfaceVariant`; `line` → `outlineVariant`, `line-strong` → `outline`.
  - `brand` → `primary` para texto/botones de marca en light es problemático en M3 (contraste); usar `primary` = dark `ochre-400 #d3a568` con `onPrimary pine-950`, light `ochre-600 #a67430` con `onPrimary white` — ya validado en `Theme.kt` actual — y reservar `secondary/tertiary` para ocre en superficies oscuras. Verificación WCAG AA obligatoria en tests.
  - `mountain` → color dedicado fuera del scheme (`MountainDark rgba(32,58,40,0.45)` / `MountainLight #b4c9b9`).
  - Fondos con `radial-gradient` web → aproximar en Compose con `Brush.radialGradient` + `linearGradient` solo en Home/cabecera; no en todas las pantallas (coste de overdraw).
- **Rationale**: M3 exige roles fijos; este mapeo conserva la percepción web sin romper componentes M3 (cards, bottom bar, sheets).
- **Alternatives considered**: Copiar hex literales en cada Composable (rechazado: imposible auditar SC-002); dynamic-color (rechazado: rompería identidad Tolocha).

### 2. SVG con variables CSS → VectorDrawable por tema

- **Decision**: Descargar `SierraEmblem` (montaña+sol+antena), favicon inline de `index.html` y path `MountainWall` de `AppShell.tsx`; resolver `var(--emblem-badge-a/b, --emblem-sun, --emblem-line, --color-pine-600)` a hex por tema (dark: `#08100b/#203a28/#e2c091/#bcd2c4`; light: `#e3ece5/#c8dbce/#a67430/#3c6a4d`) y generar `sierra_emblem.xml`, `mountain_wall.xml` + variante `night`. Versión `mono` simplificada (silueta + sol, sin ondas finas) para notificación/launcher-legacy.
- **Rationale**: `VectorDrawable` no entiende `var()`; la variante night es el mecanismo nativo equivalente. La variante mono garantiza legibilidad a 24dp y cumple reglas de notification icon.
- **Alternatives considered**: Un único vector con `?attr` (rechazado: frágil en launcher/notificación fuera de Compose); WebView/SVG runtime (rechazado: YAGNI + peso).

### 3. Launcher adaptativo + splash + notificación

- **Decision**: `ic_launcher_foreground.xml` = emblema simplificado centrado sobre `ic_launcher_background.xml` = `pine-950`; actualizar `mipmap-anydpi/ic_launcher(_round).xml`; `themes.xml`/`themes-night` apuntan splash al mismo fondo; notificación Media3 usa `sierra_emblem_mono.xml` como `smallIcon` y emblema completo como `largeIcon`/artwork fallback.
- **Rationale**: Cumple clarificación C con mínimo toque (solo `res/` + `RadioPlaybackService`/MediaSession metadata).
- **Alternatives considered**: Generar PNGs por densidad a mano (rechazado: el adaptive-icon vectorial ya escala; solo se conservan webp legacy si el build los exige).

### 4. Iconos: Material Symbols con igual significado

- **Decision**: Mapa Lucide→Material (ya disponible en `material-icons-extended`): Home→`Home`, Heart→`Favorite/FavoriteBorder`, History→`History`, Radio→`Radio`, UserRound→`Person`, Play/Pause→`PlayArrow/Pause`, Menu/X→`Menu/Close`, ChevronDown→`ExpandMore`, LogOut→`Logout`, GitHub→vector propio (no existe en Material; se versiona el path del SVG web como `ic_github.xml`). Bottom bar pasa de 4 a 5 destinos si la spec de navegación lo exige (Historial + Mis emisoras hoy son stubs en `TolochaNavGraph.kt`).
- **Rationale**: Sin dependencias nuevas (constitución V); GitHub es el único path que debe copiarse.
- **Alternatives considered**: Copiar todos los paths Lucide (rechazado: duplica set, rompe coherencia M3).

### 5. Tipografía del sistema con jerarquía web

- **Decision**: Sin empaquetar Inter (clarificación B). `Typography()` M3 con: título tarjeta `titleMedium/semibold` 1 línea ellipsis; metadatos `bodySmall` color `muted`; chips `labelSmall` uppercase + `letterSpacing 0.08em`; logotipo "Tolocha**Radio**" con `titleLarge` y span ocre. Todo texto en español ya existente.
- **Rationale**: Cero peso/APK, cero licencias; la jerarquía (pesos, tracking, truncado) es lo que el ojo percibe como "web".
- **Alternatives considered**: Google Fonts downloadable (rechazado: requiere dependencia + red; re-evaluable si diseño lo exige).

### 6. Persistencia del modo de tema

- **Decision**: DataStore `theme_mode` (STRING: `SYSTEM|DARK|LIGHT`, default `SYSTEM`) + `ThemeRepository`/`GetThemeModeUseCase` finos + `ThemeViewModel` con `StateFlow`; `TolochaTheme(darkTheme)` se resuelve en el grafo (por defecto `isSystemInDarkTheme()` cuando es SYSTEM). Selector segmentado en Perfil/Ajustes. Excluir de auto-backup si contiene solo preferencia local trivial (o incluir; documentar en `backup_rules.xml`).
- **Rationale**: Patrón ya usado (`InstancePrefs`, `SessionManager`); sobrevive a rotación/background y cumple SC-005.
- **Alternatives considered**: `User.theme` del servidor (rechazado en clarificación A); `AppCompatDelegate` legacy (rechazado: Compose M3 gobierna).

### 7. Verificación WCAG AA

- **Decision**: Test unitario que calcula ratio de contraste de cada par (texto/fondo, marca/fondo, chip, botón play) en dark/light y exige ≥4.5:1 (texto) y ≥3:1 (grande/componentes). Pares de riesgo conocidos: `ochre-400 sobre pine-950` (ok esperado) y `ochre-600 sobre blanco` (revisar; ajustar a `ochre-700 #8a5f26` para texto si falla sin cambiar el acento decorativo).
- **Rationale**: Convierte FR-011 en gate de CI en lugar de revisión visual subjetiva.
- **Alternatives considered**: Solo revisión manual (rechazado: no auditable para SC-002).
