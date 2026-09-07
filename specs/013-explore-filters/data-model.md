# Data Model: Filtros avanzados de Explorar

**Date**: 2026-09-07
**Feature**: 013-explore-filters

## Entities

### ExploreFilters (existing, no changes)

Representa los filtros de búsqueda aplicados en la pantalla de Explorar.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | String | No | Texto de búsqueda por nombre de emisora |
| country | String? | No | País seleccionado del catálogo |
| language | String? | No | Idioma seleccionado del catálogo |
| tag | String? | No | Género/etiqueta seleccionado del catálogo |
| unique | Boolean | No | Filtrar emisoras únicas (default: false) |

**Validation**: Todos los filtros son opcionales. Al menos uno debe tener valor para una búsqueda significativa, pero se permite búsqueda sin filtros (resultados globales).

### CatalogList (new state)

Representa el estado de carga de una lista de catálogo (países, idiomas, tags).

| State | Description |
|-------|-------------|
| Loading | Lista cargando desde el servidor |
| Loaded(items: List<String>) | Lista cargada exitosamente |
| Error(message: String) | Error al cargar; modo degradado activo |

**Relationships**: Cada lista de catálogo es independiente y se carga por separado.

### StationQuery (existing, no changes)

Representa la consulta de búsqueda enviada al servidor.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | String? | No | Filtro por nombre |
| country | String? | No | Filtro por país |
| language | String? | No | Filtro por idioma |
| tag | String? | No | Filtro por género/etiqueta |
| limit | Int | Sí | Límite de resultados por página (1-100, default 24) |
| offset | Int | Sí | Desplazamiento para paginación (default 0) |
| unique | Boolean | No | Solo emisoras únicas (default false) |

### StringListDto (existing, no changes)

Respuesta de los endpoints de catálogo.

| Field | Type | Description |
|-------|------|-------------|
| items | List<String> | Lista de valores disponibles |

## State Transitions

### Filter List Loading

```
[Initial] → Loading → Loaded(items)
                   → Error(message)
```

### Filter Application

```
[No filters] → User selects filter(s) → [Filters pending]
             → User presses "Buscar" → [Filters applied] → Results loaded
             → User presses "Limpiar" → [No filters] → Results reloaded
```

## Validation Rules

1. **Country**: Debe ser un valor de la lista de países del servidor, o cualquier string en modo degradado
2. **Language**: Debe ser un valor de la lista de idiomas del servidor, o cualquier string en modo degradado
3. **Tag**: Debe ser un valor de la lista de tags del servidor, o cualquier string en modo degradado
4. **Combination**: Los filtros se combinan con AND (todos deben cumplirse simultáneamente)

## Data Flow

```
User opens Explore
    ↓
ExploreViewModel.init()
    ↓
Load countries, languages, tags (parallel)
    ↓
Each list: API call → CatalogList.Loaded or CatalogList.Error
    ↓
User selects filters + presses "Buscar"
    ↓
ExploreViewModel.setFilters(newFilters)
    ↓
StationsRepo.search(StationQuery)
    ↓
API: GET /stations?name=...&country=...&language=...&tag=...
    ↓
Results displayed (or cache fallback if offline)
```
