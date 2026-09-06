# Data Model: Alternador de Vista Lista/Tarjetas

Feature: `008-view-mode-toggle` | Date: 2026-09-06

## Entities

### ViewMode (enum, core/domain)

Valor de usuario único y global con la presentación del contenido de emisoras.

| Campo/Valor | Tipo | Descripción |
|---|---|---|
| `LIST` | enum | Lista compacta (`StationListItem` / list items de sección). Default. |
| `GRID` | enum | Cuadrícula de tarjetas (`StationCard`, 2 columnas). |

- **Identity/uniqueness**: un solo valor activo para toda la app (FR-004). Sin identidad por usuario/sección.
- **State transitions**: `LIST ⇄ GRID` (toggle). Sin estados intermedios.
- **Persistence**: DataStore Preferences, clave `view_mode` (String = `name` del enum), gestionada por `InstancePrefs`.
  - Ausencia de clave → `LIST` (FR-005).
  - Valor ilegible/corrupto → `LIST` sin error visible (FR-010; patrón `runCatching { valueOf(name) }.getOrDefault(LIST)` ya usado para `theme_mode`/`start_screen`).
  - Escritura: sobrescribe el valor previo en cada toggle (FR-006).
- **Sin sincronización** con el servidor ni con la web (Assumptions spec).

### Preferencia de modo de vista (relación con el resto del modelo)

- No modifica entidades existentes (`Station`, `Favorite`, `HistoryEntry`, `SavedServer`): es una preferencia ortogonal a los datos.
- No toca Room (`TolochaDb`): sin migraciones.

## Data volume / scale

1 clave en Preferences Store existente; lectura vía `Flow` reactiva, escritura por toggle (frecuencia humana). Sin impacto en tamaño ni en rendimiento.

## Validation rules

- Solo valores del enum al escribir (`setViewMode(ViewMode)` tipado; nunca String crudo desde UI).
- Lectura siempre normalizada al enum con fallback LIST.
