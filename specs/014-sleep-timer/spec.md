# Feature Specification: Temporizador de apagado (Sleep Timer)

**Feature Branch**: `014-sleep-timer`

**Created**: 2026-09-08

**Status**: Done (2026-09-08; todo verificado manualmente por el usuario) — Enmendada 2026-09-10: tiempo restante solo en minutos (actualización 1/min, FR-009)

**Input**: User description: "añadir temporizador de apagado tipico de radio. Se establecerá un tiempo límite y a su llegada se detendrá la reproducción. Será un nuevo botón típico en la barra superior"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Activar temporizador con duración predefinida (Priority: P1)

El usuario está escuchando una emisora y pulsa el botón de temporizador en la barra superior. Se despliega un menú con opciones de duración predefinidas (15, 30, 45, 60, 90 minutos). Al seleccionar una opción, el temporizador se activa y el botón muestra visualmente que está activo (p. ej. icono con indicador o badge con tiempo restante). Cuando el tiempo se agota, la reproducción se detiene automáticamente.

**Why this priority**: Es la funcionalidad central de la feature. Sin la capacidad de seleccionar un tiempo y que la reproducción se detenga, la feature no tiene valor.

**Independent Test**: se puede probar reproduciendo una emisora, activando el temporizador de 15 segundos (en modo debug) o 15 minutos, y verificando que la reproducción se detiene al expirar el tiempo.

**Acceptance Scenarios**:

1. **Given** el usuario está reproduciendo una emisora, **When** pulsa el botón de temporizador en la barra superior, **Then** se muestra un menú con opciones de duración (15, 30, 45, 60, 90 minutos).
2. **Given** el usuario selecciona "30 minutos", **When** el temporizador se activa, **Then** el botón muestra un indicador visual de que el temporizador está activo.
3. **Given** el temporizador está activo con 30 minutos, **When** pasan los 30 minutos, **Then** la reproducción se detiene automáticamente.
4. **Given** el temporizador está activo, **When** el usuario pulsa el botón de temporizador nuevamente, **Then** se muestra la opción de cancelar el temporizador activo.

---

### User Story 2 - Ver tiempo restante del temporizador (Priority: P2)

El usuario quiere saber cuánto tiempo queda antes de que se detenga la reproducción. Al mantener pulsado o al abrir el menú del temporizador, se muestra el tiempo restante en minutos (sin segundos). Para minimizar el consumo de CPU y batería, el indicador se actualiza como máximo una vez por minuto.

**Why this priority**: Proporciona feedback al usuario sobre el estado del temporizador, mejorando la experiencia de uso y evitando sorpresas cuando la reproducción se detenga.

**Independent Test**: se puede probar activando un temporizador, esperando al menos un minuto, y verificando que el tiempo restante se muestra correctamente en minutos.

**Acceptance Scenarios**:

1. **Given** el temporizador está activo, **When** el usuario pulsa el botón de temporizador, **Then** se muestra el tiempo restante en minutos en el menú desplegado.
2. **Given** quedan 5 minutos, **When** el usuario ve el tiempo restante, **Then** se muestra "5 min" de forma legible, sin segundos.

---

### User Story 3 - Cancelar temporizador activo (Priority: P2)

El usuario quiere cancelar un temporizador que ya está activo (p. ej. porque quiere seguir escuchando). Pulsa el botón de temporizador y selecciona "Desactivar" o "Cancelar temporizador". El temporizador se detiene y la reproducción continúa sin interrupciones.

**Why this priority**: Sin esta funcionalidad, el usuario no podría deshacer la activación del temporizador y tendría que esperar a que expire o reiniciar la app.

**Independent Test**: se puede probar activando un temporizador, cancelándolo inmediatamente, y verificando que la reproducción continúa y el botón vuelve a su estado normal.

**Acceptance Scenarios**:

1. **Given** el temporizador está activo, **When** el usuario pulsa el botón y selecciona "Cancelar temporizador", **Then** el temporizador se desactiva y el botón vuelve a su estado normal.
2. **Given** el temporizador está activo, **When** el usuario cancela, **Then** la reproducción continúa sin interrupciones.

---

### User Story 4 - El temporizador persiste al navegar (Priority: P3)

El usuario activa el temporizador y luego navega a otras secciones de la app (Explorar, Favoritos, etc.). El temporizador sigue activo y la reproducción continúa en segundo plano. Cuando el tiempo expira, la reproducción se detenga independientemente de la pantalla en la que esté el usuario.

**Why this priority**: El temporizador debe funcionar igual que en una radio física: una vez programado, funciona independientemente de la interacción del usuario con el dispositivo.

**Independent Test**: se puede probar activando el temporizador, navegando a otra sección, y verificando que el indicador del temporizador sigue visible y que la reproducción se detiene al expirar.

**Acceptance Scenarios**:

1. **Given** el temporizador está activo, **When** el usuario navega a la sección Explorar, **Then** el botón de temporizador en la barra superior sigue mostrando el indicador activo.
2. **Given** el temporizador está activo y el usuario está en otra sección, **When** el tiempo expira, **Then** la reproducción se detiene.

---

### Edge Cases

- ¿Qué pasa si la reproducción ya está pausada cuando el temporizador expira? → El temporizador se desactiva sin acción adicional; al reanudar la reproducción, el temporizador ya no estará activo.
- ¿Qué pasa si el usuario cambia de emisora mientras el temporizador está activo? → El temporizador sigue activo y se aplica a la nueva reproducción.
- ¿Qué pasa si el usuario cierra la app completamente (no solo la pone en segundo plano)? → El temporizador se cancela; al reabrir la app, el temporizador no estará activo.
- ¿Qué pasa si el usuario activa un segundo temporizador estando uno ya activo? → El nuevo temporizador reemplaza al anterior (comportamiento estándar en radios).
- ¿Qué pasa si el temporizador expira mientras la app está en segundo plano? → La reproducción se detiene mediante el servicio de Media3 de forma silenciosa; la notificación multimedia se descarta sin sonido ni vibración.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE mostrar un botón de temporizador en la barra superior de la pantalla principal.
- **FR-002**: Al pulsar el botón, el sistema DEBE mostrar un menú con opciones de duración predefinidas: 15, 30, 45, 60 y 90 minutos.
- **FR-003**: Al seleccionar una duración, el sistema DEBE iniciar un temporizador que detenga la reproducción al expirar el tiempo seleccionado.
- **FR-004**: El botón DEBE mostrar un indicador visual cuando el temporizador está activo (p. ej. cambio de icono, badge, o animación).
- **FR-005**: El sistema DEBE permitir al usuario cancelar un temporizador activo desde el mismo menú.
- **FR-006**: Al pulsar el botón con un temporizador activo, el sistema DEBE mostrar el tiempo restante en minutos (sin segundos) y la opción de cancelar.
- **FR-007**: El temporizador DEBE seguir funcionando mientras la reproducción continúe en segundo plano (servicio de Media3).
- **FR-008**: Si el usuario activa un nuevo temporizador estando uno activo, el sistema DEBE reemplazar el anterior por el nuevo.
- **FR-009**: El indicador del temporizador activo DEBE actualizarse como máximo una vez por minuto (sin descontar segundos), para minimizar el consumo de CPU y batería.

### Key Entities

- **Temporizador de apagado**: Configuración temporal activa que contiene la duración seleccionada y el instante de expiración. Es transitoria (no se persiste entre sesiones de app).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Los usuarios pueden activar un temporizador de apagado en menos de 3 segundos (2 pulsaciones).
- **SC-002**: La reproducción se detiene dentro de los 5 segundos siguientes a la expiración del temporizador.
- **SC-003**: El 95% de los usuarios que activan el temporizador entienden el indicador visual sin necesidad de ayuda adicional.
- **SC-004**: El temporizador funciona correctamente en segundo plano sin que la reproducción se interrumpa antes de tiempo.

## Clarifications

### Session 2026-09-08

- Q: Cuando el temporizador expira y la reproducción se detiene, ¿debe mostrarse alguna notificación o sonido, o simplemente se detiene en silencio? → A: Parada silenciosa (sin notificación, sin sonido). Es el comportamiento estándar en temporizadores de radio y evita despertar al usuario.

### Session 2026-09-10

- Q: ¿El tiempo restante debe mostrarse con precisión de segundos (MM:SS)? → A: No. Se muestra solo en minutos (p. ej. "23 min") y el contador se actualiza una vez por minuto. Motivo: menos consumo de CPU y de batería, y es suficientemente práctico para un temporizador de 15–90 minutos.

## Assumptions

- Las opciones de duración predefinidas (15, 30, 45, 60, 90 minutos) son estándar en apps de radio y cubren la mayoría de casos de uso (conciliar el sueño, escuchar un programa limitado).
- El temporizador es una funcionalidad local del cliente; no requiere comunicación con el backend.
- El temporizador no se persiste entre sesiones: si el usuario cierra la app, el temporizador se cancela.
- La barra superior ya existe en la app y tiene espacio para un botón adicional.
- El comportamiento de "reemplazar temporizador anterior" es el estándar en radios físicas y apps de radio.
