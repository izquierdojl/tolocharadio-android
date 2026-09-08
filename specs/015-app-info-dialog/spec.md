# Feature Specification: Diálogo de información de la aplicación

**Feature Branch**: `015-app-info-dialog`

**Created**: 2026-09-08

**Status**: Draft

**Input**: User description: "en configuración poner info, repo, versión, etc. que muestre en un dialogo modal datos generales de la aplicación."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver información general de la app (Priority: P1)

El usuario está en la pantalla de Configuración y pulsa un elemento "Acerca de" o "Información". Se abre un diálogo modal que muestra datos generales de la aplicación: nombre de la app, versión actual, enlace al repositorio del código fuente, y otros datos relevantes (desarrollador, licencia, etc.).

**Why this priority**: Es la funcionalidad central de la feature. Sin la capacidad de ver la información de la app, la feature no tiene valor.

**Independent Test**: se puede probar navegando a Configuración, pulsando el elemento de información, y verificando que el diálogo muestra los datos esperados.

**Acceptance Scenarios**:

1. **Given** el usuario está en la pantalla de Configuración, **When** pulsa el elemento "Acerca de", **Then** se abre un diálogo modal con la información general de la aplicación.
2. **Given** el diálogo está abierto, **When** el usuario ve el contenido, **Then** muestra al menos: nombre de la app, número de versión, y enlace al repositorio.
3. **Given** el diálogo está abierto, **When** el usuario pulsa fuera del diálogo o en el botón de cerrar, **Then** el diálogo se cierra y vuelve a Configuración.

---

### User Story 2 - Acceder al repositorio desde el diálogo (Priority: P2)

El usuario quiere visitar el repositorio del código fuente. Desde el diálogo de información, pulsa el enlace al repositorio y se abre en el navegador del dispositivo.

**Why this priority**: El enlace al repositorio es uno de los datos clave solicitados. Sin la capacidad de abrirlo, el usuario tendría que copiar la URL manualmente.

**Independent Test**: se puede probar abriendo el diálogo de información, pulsando el enlace del repositorio, y verificando que se abre el navegador.

**Acceptance Scenarios**:

1. **Given** el diálogo de información está abierto, **When** el usuario pulsa el enlace del repositorio, **Then** se abre el navegador con la URL del repositorio.

---

### Edge Cases

- ¿Qué pasa si la app no puede obtener la versión? → Se muestra la versión por defecto configurada en el build (p. ej. "1.0").
- ¿Qué pasa si el dispositivo no tiene navegador instalado? → El intent de abrir URL falla silenciosamente; el enlace permanece visible pero sin acción.
- ¿Qué pasa si el usuario rota el dispositivo con el diálogo abierto? → El diálogo permanece abierto mostrando la misma información.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La pantalla de Configuración DEBE mostrar un elemento "Acerca de" o "Información" que abra el diálogo modal.
- **FR-002**: El diálogo modal DEBE mostrar el nombre de la aplicación.
- **FR-003**: El diálogo modal DEBE mostrar el número de versión de la aplicación.
- **FR-004**: El diálogo modal DEBE mostrar un enlace al repositorio del código fuente.
- **FR-005**: El enlace al repositorio DEBE ser interactivo y abrir la URL en el navegador del dispositivo al ser pulsado.
- **FR-006**: El diálogo DEBE poder cerrarse mediante un botón de cerrar o pulsando fuera de él.
- **FR-007**: El diálogo DEBE mostrar información adicional relevante: nombre del desarrollador y tipo de licencia.

### Key Entities

- **Información de la aplicación**: Datos estáticos o derivados del build que identifican la app (nombre, versión, repositorio, desarrollador, licencia). No requiere comunicación con el backend.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Los usuarios pueden abrir el diálogo de información en menos de 3 segundos (2 pulsaciones desde Configuración).
- **SC-002**: El 100% de la información mostrada (nombre, versión, repositorio) es correcta y coincide con los datos oficiales de la aplicación.
- **SC-003**: El enlace al repositorio se abre correctamente en el navegador en el 95% de los dispositivos.
- **SC-004**: El diálogo se cierra sin errores en todas las interacciones (botón cerrar, pulsar fuera, rotación).

## Clarifications

### Session 2026-09-08

- Q: ¿Qué URL del repositorio debe mostrarse en el diálogo de información? → A: `https://github.com/izquierdojl/tolocharadio-android`

## Assumptions

- La versión de la app se obtiene del sistema de versionado configurado en el proyecto.
- El nombre de la app y el repositorio son datos estáticos conocidos (no requieren llamada al backend). La URL del repositorio es `https://github.com/izquierdojl/tolocharadio-android`.
- El diálogo sigue el estilo visual estándar utilizado en el resto de la app.
- La información mostrada es estática: no incluye datos dinámicos como estado de conexión o configuración del servidor.
