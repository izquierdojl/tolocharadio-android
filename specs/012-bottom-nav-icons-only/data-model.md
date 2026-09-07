# Data Model: Bottom Nav Icons Only + Section Titles

**Feature**: 012-bottom-nav-icons-only | **Date**: 2026-09-07

## Entities

Este feature no introduce nuevas entidades de dominio. Los cambios son puramente de UI.

### BottomDest (existente — modificación)

**Archivo**: `TolochaNavGraph.kt`

| Campo | Tipo | Cambio |
|-------|------|--------|
| `route` | `String` | Sin cambio |
| `label` | `String` | Se mantiene para `contentDescription` y tooltip, pero se elimina del `NavigationBarItem.label` |
| `icon` | `ImageVector` | Sin cambio |

**Relación**: `BottomDest` → `NavigationBarItem` (rendering)

**Cambio de estado**: El `label` pasa de ser mostrado como texto visible + `contentDescription` a ser usado únicamente como `contentDescription` + tooltip text.

### SectionHeader (nuevo — composable UI)

**Archivo**: Nuevo composable en `core/ui/components/SectionHeader.kt`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `title` | `String` | Título de la sección (obligatorio) |
| `subtitle` | `String?` | Subtítulo descriptivo (opcional, null por defecto) |
| `modifier` | `Modifier` | Modifier para personalización externa |

**Validación**: `title` no vacío. `subtitle` puede ser null o vacío (no se muestra).

**Relación**: Usado por cada pantalla de sección como primer hijo del `Column` de contenido.

**Estilo**:
- `title`: `MaterialTheme.typography.headlineSmall`
- `subtitle`: `MaterialTheme.typography.bodySmall` + `MaterialTheme.colorScheme.onSurfaceVariant`
- `padding`: `horizontal = 16.dp, vertical = 8.dp`

## State Transitions

No aplicable — este feature no modela estados de negocio.
