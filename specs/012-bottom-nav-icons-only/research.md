# Research: Bottom Nav Icons Only + Section Titles

**Feature**: 012-bottom-nav-icons-only | **Date**: 2026-09-07

## R1: Tooltip en NavigationBarItem sin label

**Decision**: Usar `TooltipBox` + `PlainTooltip` de Material3 para envolver cada `NavigationBarItem`.

**Rationale**: M3 `NavigationBarItem` no soporta tooltips nativamente cuando se omite el parámetro `label`. La alternativa es envolver el `icon` lambda con un `TooltipBox` que muestre el texto al pulsación larga. Esto cumple con las guías de accesibilidad de M3 para icon-only buttons.

**Alternatives considered**:
- `TooltipBox` con `RichTooltip`: descartado, no necesitamos enlaces ni contenido rico.
- `Modifier.tooltip()`: no existe en M3 estable.
- `contentDescription` solo: necesario pero insuficiente — TalkBack lo lee pero usuarios sighted no ven el nombre.

**Implementation notes**:
- `TooltipBox` requiere un `TooltipState` y un `rememberRichTooltipState()` o `rememberPlainTooltipState()`.
- Se envuelve cada `NavigationBarItem` con `TooltipBox(tooltip = { PlainTooltip { Text(label) } }, state = tooltipState)`.
- El `icon` lambda del `NavigationBarItem` se mantiene igual; el tooltip se muestra sobre el item completo.

## R2: Consistencia de títulos de sección

**Decision**: Crear un composable reutilizable `SectionHeader` para evitar duplicación de código.

**Rationale**: Historial y Mis emisoras ya tienen títulos con el mismo patrón (`headlineSmall` + subtítulo `bodySmall` con `onSurfaceVariant`). Configuración usa `headlineMedium` + 24dp padding, lo cual es inconsistente. Un composable compartido garantiza consistencia y facilita cambios futuros.

**Alternatives considered**:
- Duplicar el patrón en cada pantalla: funciona pero viola DRY y dificulta cambios de estilo.
- Usar solo `Text` inline: no garantiza consistencia de padding ni subtítulo.

**Implementation notes**:
- `SectionHeader(title: String, subtitle: String? = null)` — Column con `padding(horizontal = 16.dp, vertical = 8.dp)`.
- Título: `headlineSmall`, subtítulo (opcional): `bodySmall` + `onSurfaceVariant`.
- Se inserta al inicio de cada pantalla, justo después del `Column(Modifier.fillMaxSize())` o del `Scaffold` padding.

## R3: Padding de Configuración

**Decision**: Reducir el padding del `Column` en `SettingsScreen` de 24dp a 0dp y aplicar padding estándar solo al título.

**Rationale**: El padding global de 24dp en todo el `Column` de Configuración crea un espacio excesivo entre la TopAppBar y el título. El estándar será `16dp horizontal, 8dp vertical` para el título, igual que las demás secciones.

**Alternatives considered**:
- Mantener 24dp global: inconsistente con el resto de secciones.
- Reducir a 16dp global: podría afectar al contenido interno de Configuración que se beneficia de más padding.

**Implementation notes**:
- Eliminar `padding(24.dp)` del `Column` raíz en `SettingsScreen`.
- Añadir `SectionHeader("Configuración")` al inicio.
- El contenido interno de Configuración mantiene su propio padding (ya lo tiene por secciones/cards).
