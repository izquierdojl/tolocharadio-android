# Feature Specification: Filtros avanzados de Explorar

**Feature Branch**: `013-explore-filters`

**Created**: 2026-09-07

**Status**: Done (2026-09-07; todo verificado manualmente por el usuario)

**Input**: User description: "La opción de explorar actualmente sólo permite buscar por nombre. Explora la app web del repo https://github.com/izquierdojl/tolocharadio para que se pueda buscar también por país, idioma, género, etc.... Tendría que tener la misma funcionalidad que desde la web."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Filtrar emisoras por país, idioma y género (Priority: P1)

El usuario entra en la sección **Explorar** y, además del campo de búsqueda por nombre que ya existe, encuentra controles desplegables para filtrar por **país**, **idioma** y **género/etiqueta**. Cada desplegable muestra una lista de valores disponibles obtenida de la API (`/stations/countries`, `/stations/languages`, `/stations/tags`). El usuario puede combinar varios filtros simultáneamente y los resultados se actualizan al pulsar "Buscar".

**Why this priority**: Es la funcionalidad central de la feature y la que aporta mayor valor al usuario: descubrir emisoras por criterios geográficos, lingüísticos o temáticos sin tener que adivinar nombres. La web ya lo ofrece y el usuario espera paridad.

**Independent Test**: se puede probar aisladamente abriendo Explorar, seleccionando un país, un idioma y/o un género, y verificando que los resultados se filtran correctamente. No requiere login.

**Acceptance Scenarios**:

1. **Given** el usuario está en Explorar, **When** abre el desplegable de país, **Then** ve una lista de países disponibles ordenada alfabéticamente.
2. **Given** ha seleccionado "Spain" como país, **When** pulsa "Buscar", **Then** los resultados muestran únicamente emisoras de España.
3. **Given** ha seleccionado un país, un idioma y un género, **When** pulsa "Buscar", **Then** los resultados cumplen los tres criterios simultáneamente.
4. **Given** ha aplicado filtros, **When** pulsa "Limpiar filtros", **Then** todos los campos se vacían y se muestran resultados sin filtrar.

---

### User Story 2 - Autocompletado con listas de sugerencias (Priority: P1)

Los desplegables de país, idioma y género funcionan como controles de autocompletado (combobox): al escribir, el usuario ve coincidencias parciales de la lista de valores disponibles. Si la lista no se puede cargar (error de red o API), el campo permite entrada manual libre con un aviso informativo.

**Why this priority**: Sin autocompletado, el usuario tendría que escribir valores exactos (sensibles a mayúsculas, tildes, etc.), lo que genera frustración y errores. Es esencial para la usabilidad.

**Independent Test**: se puede probar escribiendo parcialmente un valor en cada campo y verificando que aparecen sugerencias. También desconectando la red para verificar el modo degradado.

**Acceptance Scenarios**:

1. **Given** las listas de países están cargadas, **When** el usuario escribe "Esp", **Then** ve "Spain" (o el nombre normalizado) como sugerencia seleccionable.
2. **Given** la API de países devuelve error, **When** el usuario abre el campo de país, **Then** ve un aviso de que no se pudo cargar la lista y puede escribir el valor manualmente.
3. **Given** el usuario selecciona un valor del desplegable, **When** el valor se aplica, **Then** el campo muestra el valor seleccionado y el filtro se incluye en la búsqueda.
4. **Given** las listas de filtros están cargando, **When** el usuario abre la pantalla de Explorar, **Then** los campos de filtro aparecen deshabilitados con un indicador de carga y se habilitan automáticamente cuando las listas están listas.

---

### Edge Cases

- ¿Qué pasa si el usuario aplica un filtro con un valor que no existe en la lista (p. ej. país inventado)? → La API devolverá 0 resultados; se muestra el estado vacío estándar.
- ¿Qué pasa si la red se cae mientras se cargan las listas de filtros? → Los campos entran en modo degradado (entrada libre con aviso).
- ¿Qué pasa si el usuario combina nombre + país + idioma + género y no hay resultados? → Se muestra "Sin resultados" con opción de limpiar filtros.
- ¿Qué pasa si las listas de países/idiomas/tags son muy grandes (>500 elementos)? → El combobox filtra por coincidencia parcial para no mostrar la lista completa.
- ¿Qué pasa mientras se cargan nuevos resultados tras cambiar filtros? → Los resultados anteriores permanecen visibles con un indicador de carga sutil; se reemplazan al llegar los nuevos.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE mostrar un campo desplegable de autocompletado para filtrar por **país**, con valores obtenidos del catálogo del servidor.
- **FR-002**: El sistema DEBE mostrar un campo desplegable de autocompletado para filtrar por **idioma**, con valores obtenidos del catálogo del servidor.
- **FR-003**: El sistema DEBE mostrar un campo desplegable de autocompletado para filtrar por **género/etiqueta**, con valores obtenidos del catálogo del servidor.

*(FR-001–FR-003, converge 2026-09-20: el desplegable NO usa `ExposedDropdownMenuBox`; el
popup se solapaba con el bottom sheet (bug `filter-dropdown-overlap`), así que se sustituyó
por una **lista inline dentro del bottom sheet**. Se conservan el autocompletado (coincidencia
parcial) y la entrada manual del componente `FilterComboBox`.)*
- **FR-004**: El sistema DEBE permitir combinar búsqueda por nombre con filtros de país, idioma y género simultáneamente.
- **FR-005**: El sistema DEBE mostrar un botón "Limpiar filtros" que vacíe todos los campos de filtro y recargue resultados sin filtros.
- **FR-006**: Si la carga de una lista de filtros falla, el sistema DEBE permitir entrada manual del valor con un aviso informativo.

### Key Entities

- **Filtro de exploración**: Criterio de búsqueda (país, idioma, género, nombre). Cada filtro es opcional y combinable.
- **Lista de catálogo**: Conjunto de valores disponibles para un filtro (países, idiomas, tags). Obtenida de la API y cacheada localmente.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Los usuarios pueden filtrar emisoras por país, idioma y género en la app Android, igual que en la web.
- **SC-002**: Los desplegables de filtros cargan sus listas en menos de 2 segundos en conexiones normales.
- **SC-003**: El 90% de los usuarios que buscan emisoras por criterios distintos al nombre encuentran resultados relevantes en su primer intento.

## Clarifications

### Session 2026-09-07

- Q: ¿Qué funcionalidades de la app web NO deben incluirse en esta feature? → A: Excluir sugerencias de géneros (User Story 3). Mantener solo filtros + paginación.
- Q: ¿Cómo debe comportarse la UI mientras se cargan las listas de filtros? → A: Mostrar los campos deshabilitados con un spinner/indicador de carga; se habilitan cuando la lista está lista.
- Q: ¿Qué debe mostrar la pantalla mientras se cargan nuevos resultados tras cambiar filtros? → A: Mantener resultados anteriores visibles con indicador de carga sutil; se reemplazan al llegar los nuevos.
- Q: ¿Implementar paginación anterior/siguiente? → A: No. Mantener el botón "Cargar más" actual. Solo añadir los filtros de búsqueda.

## Assumptions

- La API del servidor proporciona listas de países, idiomas y etiquetas para los filtros (verificado en el código fuente del Android app).
- Los endpoints de catálogo no requieren autenticación (son datos públicos de RadioBrowser).
- La lista de tags/géneros puede contener cientos de elementos; el combobox debe filtrar por coincidencia parcial.
- El modo de presentación de resultados (lista/grid) ya está implementado y no cambia con esta feature.
