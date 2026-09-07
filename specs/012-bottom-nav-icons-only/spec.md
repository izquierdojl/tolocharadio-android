# Feature Specification: Bottom Nav Icons Only + Section Titles

**Feature Branch**: `012-bottom-nav-icons-only`

**Created**: 2026-09-07

**Status**: Done (2026-09-07; T004 asumido sin implementar - incompatibilidad API TooltipBox con Compose BOM 2025.01.00)

**Input**: User description: "Los botones de abajo están bien, pero si el texto es muy largo, se ve en dos alturas. Por ejemplo, Mis Emisoras, se ve en dos líneas y queda muy feo. Quita el texto de los botones. Al quitar el texto, tenemos que tener consistencia en como mostramos la información, por lo tanto, cuando accedamos a cada sección debe aparecer el título de la sección como ya aparece en 'Tu Historial' o 'Configuración'. Revisa esas opciones. Revisa también el padding que existe entre la barra superior y ese texto, debe ser mínimo para aprovechar el espacio en todas las opciones."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Barra de navegación inferior solo con iconos (Priority: P1)

Un usuario abre la app y ve la barra de navegación inferior con 5 iconos (Explorar, Favoritos, Historial, Mis emisoras, Configuración) sin texto debajo. Los iconos son reconocibles por sí solos y la barra ocupa menos espacio vertical, dando más área útil al contenido.

**Why this priority**: Es el cambio principal reportado. El texto largo ("Mis emisoras", "Configuración") se parte en dos líneas y rompe la estética. Eliminarlo resuelve el problema raíz.

**Independent Test**: Abrir la app y verificar que la barra de navegación muestra solo iconos sin etiquetas de texto. Verificar que todos los iconos son tappable y navegan a su sección correcta.

**Acceptance Scenarios**:

1. **Given** la app está abierta en cualquier sección, **When** el usuario mira la barra de navegación inferior, **Then** solo se ven 5 iconos sin texto, alineados horizontalmente con espaciado uniforme.
2. **Given** la barra de navegación solo con iconos, **When** el usuario pulsa cada icono, **Then** navega a la sección correspondiente (Explorar, Favoritos, Historial, Mis emisoras, Configuración) y el icono seleccionado muestra estado activo.
3. **Given** la barra de navegación solo con iconos, **When** el usuario mantiene pulsado un icono, **Then** se muestra un tooltip con el nombre de la sección (accesibilidad).

---

### User Story 2 - Título de sección visible en todas las pantallas (Priority: P2)

Un usuario navega a cualquier sección (Explorar, Favoritos, Historial, Mis emisoras, Configuración) y ve el título de la sección en la parte superior del contenido, justo debajo de la barra superior (TopAppBar), de forma consistente en todas las pantallas.

**Why this priority**: Al quitar el texto de los botones, el usuario necesita saber en qué sección está. "Tu Historial" y "Configuración" ya muestran título; las demás (Explorar, Favoritos) no. La consistencia es clave.

**Independent Test**: Navegar a cada una de las 5 secciones y verificar que aparece el título de la sección en la parte superior del contenido con estilo tipográfico consistente.

**Acceptance Scenarios**:

1. **Given** el usuario navega a "Explorar", **When** la pantalla carga, **Then** aparece el título "Explorar" en la parte superior del contenido con estilo `headlineSmall`.
2. **Given** el usuario navega a "Favoritos", **When** la pantalla carga, **Then** aparece el título "Tus favoritos" en la parte superior del contenido con estilo `headlineSmall`.
3. **Given** el usuario navega a "Historial", **When** la pantalla carga, **Then** aparece el título "Tu historial" (ya existente) con estilo consistente con las demás secciones.
4. **Given** el usuario navega a "Mis emisoras", **When** la pantalla carga, **Then** aparece el título "Mis emisoras" (ya existente) con estilo consistente con las demás secciones.
5. **Given** el usuario navega a "Configuración", **When** la pantalla carga, **Then** aparece el título "Configuración" (ya existente) con estilo consistente con las demás secciones.

---

### User Story 3 - Padding mínimo entre barra superior y título de sección (Priority: P3)

Un usuario abre cualquier sección y el título de la sección aparece lo más cerca posible de la barra superior, sin espacios excesivos, aprovechando al máximo el espacio vertical disponible.

**Why this priority**: El padding excesivo desperdicia espacio en pantalla, especialmente en dispositivos pequeños. Debe ser uniforme y mínimo en todas las secciones.

**Independent Test**: Comparar visualmente el espacio entre la barra superior y el título en las 5 secciones. Debe ser consistente y mínimo (no mayor a 8dp).

**Acceptance Scenarios**:

1. **Given** el usuario está en cualquier sección, **When** mide el espacio entre el borde inferior de la TopAppBar y el texto del título, **Then** el padding es de como máximo 8dp (vertical) y 16dp (horizontal).
2. **Given** el usuario navega entre secciones, **When** compara el espaciado visual, **Then** el padding es idéntico en todas las secciones (consistencia).

---

### Edge Cases

- ¿Qué pasa con pantallas que usan un Scaffold interno (HistoryScreen, CustomStationsScreen)? El padding del scaffold interno debe alinearse con el estándar definido.
- ¿La pantalla de Configuración usa padding de 24dp en todo el Column? Debe ajustarse para que el título tenga el mismo padding que las demás secciones.
- ¿Las pantallas sin título actual (Explorar, Favoritos) tienen contenido propio (campo de búsqueda, empty state) justo al inicio? El título debe insertarse antes de ese contenido sin romper el layout.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La barra de navegación inferior MUST mostrar únicamente iconos, sin etiquetas de texto (`label`) debajo de cada icono.
- **FR-002**: Los 5 iconos de la barra de navegación MUST mantener su funcionalidad completa (navegar a su sección, estado seleccionado, accesibilidad con `contentDescription`).
- **FR-003**: Cada sección de la app (Explorar, Favoritos, Historial, Mis emisoras, Configuración) MUST mostrar un título de sección en la parte superior del contenido, debajo de la TopAppBar.
- **FR-004**: El título de sección MUST usar tipografía `headlineSmall` de Material3 para consistencia.
- **FR-005**: El título de sección MAY incluir un subtítulo descriptivo opcional (como ya hace Historial con "Lo último que has escuchado." y Mis emisoras con "Añade emisoras...").
- **FR-006**: El padding entre la TopAppBar y el título de sección MUST ser mínimo y consistente en todas las secciones: `horizontal = 16dp, vertical = 8dp` o menor.
- **FR-007**: Las secciones que ya muestran título (Historial, Mis emisoras, Configuración) MUST ajustar su padding actual al estándar definido en FR-006 si difiere.
- **FR-008**: Las secciones sin título actual (Explorar, Favoritos) MUST añadir un título siguiendo el mismo patrón visual.

### Key Entities

- **NavigationBar**: barra de navegación inferior con 5 destinos (Explorar, Favoritos, Historial, Mis emisoras, Configuración).
- **Section Header**: componente visual (título + subtítulo opcional) que aparece en la parte superior de cada sección.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: La barra de navegación inferior no muestra texto en ninguno de sus 5 elementos; verificación visual pasa al 100%.
- **SC-002**: Las 5 secciones muestran título de sección al cargar; verificación visual pasa al 100%.
- **SC-003**: El espacio entre la TopAppBar y el título de sección es visualmente idéntico (±2dp) en las 5 secciones.
- **SC-004**: El espacio vertical entre TopAppBar y título de sección no excede 8dp en ninguna sección.
- **SC-005**: La funcionalidad de navegación (tap en iconos, estado seleccionado, accesibilidad) se mantiene sin regresiones.

## Assumptions

- Los 5 iconos de la barra de navegación (`Search`, `Favorite`, `History`, `Radio`, `Settings`) son reconocibles sin texto según estándares de Material Design 3.
- El tooltip de accesibilidad al mantener pulsado se implementa usando el composable `TooltipBox` o `PlainTooltip` de M3.
- Las secciones que ya tienen título (Historial, Mis emisoras) usan `headlineSmall`; se asume que este es el estándar correcto y Configuración (`headlineMedium`) debe ajustarse.
- El subtítulo descriptivo es opcional por sección; no todas las secciones necesitan uno.
- El padding estándar será `horizontal = 16dp, vertical = 8dp` basado en el patrón ya existente en Historial y Mis emisoras.
