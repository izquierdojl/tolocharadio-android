# Implementation Plan: Adaptación look-and-feel a la web

**Branch**: `002-web-look-and-feel` | **Date**: 2026-09-05 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-web-look-and-feel/spec.md` (con clarificaciones 2026-09-05: tema 3 estados local, emblema en launcher+splash+notificación+atajos/widget, fuente sistema, Material Symbols, WCAG AA).

## Summary

Paridad visual Android ↔ web (`apps/web` en `main`): ampliar `Theme.kt` de 4 colores a la escala completa pine/ochre/moss con roles oscuro/claro de `index.css`; versionar los SVG (SierraEmblem, favicon, MountainWall) como drawables y aplicarlos a cabecera, launcher adaptativo, splash, notificación Media3, placeholder y estados vacíos; restyle de `StationCard`/`StationListItem`/`EmptyState`/`FavoriteButton`/cabecera a formas y jerarquía web con Material Symbols y fuente del sistema; selector de tema Sistema/Claro/Oscuro en Ajustes/Perfil con persistencia local.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JVM 17, AGP 9.3.2

**Primary Dependencies**: Jetpack Compose (BOM 2025.01.00) + Material3, material-icons-core + extended, Coil 2.6.0 (favicon), Media3 1.4.1 (notificación), Hilt, DataStore 1.1.1 (preferencia de tema), Room (sin cambios de esquema)

**Storage**: DataStore (clave `theme_mode`: SYSTEM/DARK/LIGHT) + drawables versionados (`res/drawable/`, `res/mipmap-anydpi/`); sin cambios en Room ni en contrato `/api/v1`

**Testing**: JUnit4 + kotlinx-coroutines-test + Turbine (ViewModel de tema), Compose UI tests (`composeTestRule`) para tarjeta/empty-state/mini-player, screenshot/manual lado a lado web, verificación de contraste (calculadora WCAG en test unitario de tokens)

**Target Platform**: Android minSdk 26, targetSdk/compileSdk 37

**Project Type**: mobile-app (módulo único `app`, arquitectura MVVM + Clean por capas)

**Performance Goals**: Cambio de tema aplicado en <1s sin recomposición con mezcla de temas (SC-005); arranque en frío <2s (constitución, sin regresión por drawables)

**Constraints**: HTTPS-only y Bearer en memoria sin cambios;-YAGNI: sin dependencias nuevas salvo justificación (se reutilizan Material Symbols ya incluidos); iconos launcher deben seguir reglas adaptive-icon + monocromo notificación; SVG con `var(--*)` deben llevar colores resueltos por tema

**Scale/Scope**: 8 pantallas (Home, Explorar, Favoritos, Historial, Mis emisoras, Perfil, Login/Registro, reproductor) + cabecera, bottom bar (4 destinos actuales), tarjetas, splash, launcher, notificación

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] I. MVVM + Clean: solo se toca capa UI (`core/ui/theme`, `core/ui/components`, pantallas existentes) + preferencia de tema (DataStore vía repo/UseCase si ya existe patrón; si no, nuevo `ThemeRepository`/`GetThemeModeUseCase` fino, sin lógica en Composables). ViewModel no referencia vistas.
- [x] II. Kotlin-first + Compose M3 + Media3: todo UI en Compose M3; Media3 solo para icono/colores de notificación, sin `MediaPlayer` crudo; Coil para favicons; DataStore para tema.
- [x] III. Test-first: tests de serialización no aplican (sin DTO nuevo); REQUIRED: tests ViewModel tema + Turbine, Compose tests de tarjeta/empty-state/play-favorito, Lint/ktlint/Detekt sin errores.
- [x] IV. Streaming robusto: sin cambios de player; placeholder y `ErrorBanner` mantienen mensaje accionable + reintento; sin PII en logs.
- [x] V. YAGNI: sin multi-módulo, sin BaaS, sin librerías de iconos/fuentes nuevas (Material Symbols y fuente sistema ya disponibles); Inter explícitamente descartado en clarificación.
- [x] Paridad web + Pocket Casts: bottom bar, StationCard/ListItem, EmptyState, FavoriteButton, mini-player persistente se mantienen; solo cambia su estilo a tokens/formas web.
- [x] Seguridad/self-hosted: sin cambios de red/auth; `baseUrl` editable intacto.

Post-diseño (Phase 1): sin violaciones nuevas. No se requiere Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/002-web-look-and-feel/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── design-tokens.md
│   └── brand-assets.md
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/
├── java/com/izquierdojl/tolocharadio/
│   ├── core/ui/theme/
│   │   ├── Theme.kt          # escala pine/ochre/moss + schemes dark/light + Shapes/Typography sistema
│   │   └── ThemeViewModel.kt # expone ThemeMode (si no existe); decide darkTheme para TolochaTheme
│   ├── core/ui/components/
│   │   ├── CommonUi.kt       # StationCard/ListItem/EmptyState/FavoriteButton/StationArtwork restyle
│   │   ├── TolochaLogo.kt    # (nuevo) logotipo Sierra + "TolochaRadio" (nuevo, fino)
│   │   └── MountainWall.kt   # (nuevo) franja silueta sobre reproductor/pie
│   ├── core/ui/navigation/
│   │   └── TolochaNavGraph.kt # iconos Material equivalents + cabecera con logo
│   ├── feature/
│   │   ├── profile/          # selector Sistema/Claro/Oscuro
│   │   ├── player/           # mini-player + full-player + icono notificación
│   │   └── home|explore|favorites|history|customStations|auth/ # solo estilo, sin lógica
│   └── data/local/
│       └── ThemePrefs.kt     # (nuevo si no existe) DataStore theme_mode
└── res/
    ├── drawable/
    │   ├── sierra_emblem.xml         # SVG web con colores resueltos (versión dark/light vía night qualifier si hace falta)
    │   ├── sierra_emblem_mono.xml    # versión simplificada monocromo (notificación/atajos)
    │   ├── mountain_wall.xml         # silueta MountainWall
    │   └── ic_launcher_foreground.xml # (reemplazo) emblema simplificado sobre fondo pine-950
    ├── mipmap-anydpi/                # adaptive-icon xml (ya existe, se actualiza)
    ├── values/colors.xml             # escala completa (referencia, fuente de verdad en Theme.kt)
    ├── values/themes.xml + values-night/themes.xml  # splash/estilo sistema
    └── xml/                          # backup rules (añadir exclusión de theme_mode si procede)
```

**Structure Decision**: Módulo único `app` (constitución V). Toda la feature cabe en `core/ui/theme|components|navigation` + drawables + preferencia DataStore; sin módulos nuevos ni cambios en `domain/data` salvo `ThemePrefs`/repo fino.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |

