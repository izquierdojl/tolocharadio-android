# Research: Filtros avanzados de Explorar

**Date**: 2026-09-07
**Feature**: 013-explore-filters

## Research Tasks

### 1. Combobox/Filter Component Pattern

**Decision**: Crear un componente `FilterComboBox` reutilizable basado en `ExposedDropdownMenuBox` de Material3.

**Actualización (converge 2026-09-20)**: el `ExposedDropdownMenuBox` se **sustituyó por una
lista inline dentro del bottom sheet**. El popup de Material3 se solapaba con el propio bottom
sheet (bug `filter-dropdown-overlap`, ver `.specify/bugs/filter-dropdown-overlap/fix.md`). El
resto de la decisión se mantiene: `FilterComboBox` conserva el autocompletado por coincidencia
parcial, los estados de carga/error y la entrada manual.

**Rationale**:
- Material3 proporciona `ExposedDropdownMenuBox` y `DropdownMenu` que soportan autocompletado nativo
- El proyecto ya usa Material3 (ver `build.gradle.kts`)
- Permite filtrado por coincidencia parcial mientras el usuario escribe
- Soporta estados de carga, error y modo degradado (entrada libre)

**Alternatives considered**:
- Usar un `TextField` simple con `DropdownMenu` manual: más código, menos accesible
- Usar una librería externa (ej. `compose-multiplatform-comobox`): dependencia adicional innecesaria
- Usar `LazyColumn` como lista desplegable: no es estándar UX móvil

### 2. Catalog List Loading Strategy

**Decision**: Cargar listas de catálogo (countries, languages, tags) en el `ExploreViewModel` al inicializar, con caché en memoria y fallback a entrada manual.

**Rationale**:
- Los endpoints ya existen en `StationsApi.kt` (`countries()`, `languages()`, `tags()`)
- `StationsRepo.kt` ya tiene los métodos `countries()`, `languages()`, `tags()`
- Las listas son relativamente estáticas (cambian poco)
- Cachear en memoria evita llamadas repetidas durante la sesión
- Si falla, el campo permite entrada manual (FR-006)

**Alternatives considered**:
- Cachear en Room (base de datos): sobredimensionado para listas estáticas
- Cargar bajo demanda (al abrir cada desplegable): UX más lenta
- Cargar en paralelo con la búsqueda: complejidad innecesaria

### 3. Filter State Management

**Decision**: Mantener el `ExploreFilters` data class existente y añadir estados para las listas de catálogo en el ViewModel.

**Rationale**:
- `ExploreFilters` ya tiene `country`, `language`, `tag` como campos opcionales
- Solo necesitamos añadir estados para las listas de catálogo (`_countries`, `_languages`, `_tags`)
- El flujo de búsqueda ya soporta estos filtros (`StationQuery` y `StationsApi.search()`)

**Alternatives considered**:
- Crear un nuevo ViewModel para filtros: fragmentación innecesaria
- Usar StateFlow independiente para cada filtro: más complejo sin beneficio

### 4. UI Layout for Filters

**Decision**: Mostrar filtros en un layout responsive: campo de nombre arriba, filtros desplegables debajo en fila (wrap en pantallas pequeñas), botón "Limpiar filtros" al final.

**Rationale**:
- Sigue el patrón de la web (Explore.tsx): nombre + filtros en fila
- En móvil, los filtros pueden apilarse verticalmente si no caben
- El botón "Limpiar filtros" debe ser accesible y visible

**Alternatives considered**:
- Filtros en un drawer lateral: menos descubrible
- Filtros en un chip row (como Material Design filter chips): requiere más espacio
- Filtros colapsables: oculta funcionalidad importante

### 5. Degraded Mode Behavior

**Decision**: Si la carga de una lista de filtros falla, mostrar el campo como `TextField` libre con un texto de ayuda "No se pudo cargar la lista; escribe el valor manualmente".

**Rationale**:
- Especificado en FR-006 y aprobado en clarificación
- Permite uso offline o con API caída
- Consistente con el comportamiento de la web (Explore.tsx línea 72-85)

**Alternatives considered**:
- Mostrar error y deshabilitar el campo: bloquea al usuario
- Reintentar automáticamente: puede causar bucles
- Usar caché local de listas: complejidad adicional para listas estáticas

## Resolved Questions

| Question | Answer | Source |
|----------|--------|--------|
| ¿Qué endpoints usar para listas? | `GET /stations/countries`, `/languages`, `/tags` | StationsApi.kt |
| ¿Las listas requieren auth? | No (datos públicos de RadioBrowser) | Spec assumptions |
| ¿Cómo manejar listas grandes? | Filtrado por coincidencia parcial en combobox | Spec edge cases |
| ¿Qué mostrar durante carga? | Campos deshabilitados con spinner | Clarification Q2 |
| ¿Qué mostrar al cambiar filtros? | Mantener resultados anteriores con indicador sutil | Clarification Q3 |
