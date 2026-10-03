# Feature Specification: Gráficas de escucha (estadísticas) en la app Android

**Feature Branch**: `0041-jlizquierdo-20261002-stats-charts`

**Created**: 2026-10-02

**Status**: Done (2026-10-03; todo verificado manualmente por el usuario, 35/35 tareas)

**Input**: User description: "Revisa el repositorio del backend https://github.com/izquierdojl/tolocharadio. Se ha añadido la capacidad de añadir gráficas y la página web ya las muestra. Vamos a ver como las podemos añadir a la aplicación android. Pienso que lo mejor sería o un apartado o un botón en el apartado de configuración, junto a acerca de. Exploremos y veamos la mejor opción."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver el resumen y la evolución de mi escucha (Priority: P1)

Como oyente con un servidor configurado, quiero abrir las gráficas de mi escucha y ver cuánto tiempo he escuchado en un periodo, cuál es mi emisora destacada, mi día de mayor escucha y cómo se reparte la escucha en el tiempo (por día, semana o mes), para conocer mis hábitos sin salir de la app.

**Why this priority**: Es el corazón de la función: sin resumen ni evolución temporal no hay gráficas que ver, y ninguna de las historias restantes justifica la función por sí sola.

**Independent Test**: Con un servidor con credenciales y historial de escucha previo, abrir la vista de gráficas con el periodo por defecto (30 días) muestra tiempo total, emisora destacada, día con más escucha y la serie temporal; cambiar a "7 días" recalcula todos los valores.

**Acceptance Scenarios**:

1. **Given** un servidor activo con historial de escucha en los últimos 30 días, **When** el usuario abre la vista de gráficas desde el botón de Configuración junto a "Acerca de", **Then** ve el tiempo total del periodo, su emisora destacada, el día con más escucha y la evolución temporal del periodo.
2. **Given** la vista de gráficas abierta con periodo "30 días", **When** el usuario selecciona "7 días", **Then** todos los indicadores se recalculan con el nuevo periodo en menos de 2 segundos.
3. **Given** un periodo sin ninguna escucha, **When** el usuario abre la vista de gráficas, **Then** ve un estado vacío que invita a reproducir una emisora, sin valores en cero engañosos ni errores.

---

### User Story 2 - Explorar qué y de dónde escucho (Priority: P2)

Como oyente, quiero ver un ranking de mis emisoras más escuchadas y el reparto de escucha por género y por país del periodo seleccionado, para descubrir patrones en mis gustos.

**Why this priority**: Aporta el valor analítico diferencial de las gráficas, pero se apoya en la infraestructura de datos y estados de la US1.

**Independent Test**: Con historial de varias emisoras de distintos géneros y países, abrir la vista de gráficas muestra el ranking de emisoras ordenado por tiempo de escucha y las distribuciones por género y país; cada bloque se lee de forma independiente.

**Acceptance Scenarios**:

1. **Given** historial con varias emisoras escuchadas en el periodo, **When** el usuario abre la vista de gráficas, **Then** ve el ranking de emisoras del periodo ordenado de mayor a menor tiempo de escucha, con nombre y tiempo de cada una.
2. **Given** historial con emisoras de varios géneros y países, **When** el usuario abre la vista de gráficas, **Then** ve el reparto de tiempo por género y por país, con los valores identificados de forma legible.
3. **Given** un periodo en el que solo se ha escuchado una emisora de un único género y país, **When** el usuario abre la vista de gráficas, **Then** los repartos muestran esa única categoría sin romper el diseño.

---

### User Story 3 - Consultar hábitos y actividad reciente (Priority: P3)

Como oyente, quiero ver en qué días y franjas horarias suelo escuchar (matriz de hábitos) y mis reproducciones más recientes, para conocer mi rutina y volver a lo último que escuché.

**Why this priority**: Completa la paridad con la página de gráficas de la web; es valioso como complemento, pero no sostiene la función por sí solo.

**Independent Test**: Con historial repartido en distintos días y horas, la vista de gráficas muestra la matriz de hábitos día×hora con intensidad proporcional al tiempo y la lista de reproducciones recientes con emisora, fecha y duración.

**Acceptance Scenarios**:

1. **Given** historial en distintos días y franjas horarias, **When** el usuario abre la vista de gráficas, **Then** ve una matriz de hábitos por día de la semana y hora, con intensidad proporcional al tiempo escuchado.
2. **Given** reproducciones registradas, **When** el usuario consulta la actividad reciente, **Then** ve las últimas escuchas con emisora, fecha y duración, ordenadas de más reciente a más antigua.

---

### Edge Cases

- ¿Qué ocurre cuando el periodo seleccionado no tiene ninguna escucha? → Estado vacío (por bloque o global) con invitación a reproducir una emisora.
- ¿Qué ocurre si el servidor no responde o falla la red al cargar las gráficas? → Estado de error con mensaje en español y acción de reintento.
- ¿Qué ocurre si la sesión del servidor caduca durante la carga? → Renovación de token y re-login automático con las credenciales guardadas; si falla, la acción de error abre la edición del servidor activo.
- ¿Qué ocurre con periodos muy amplios (todo el histórico) y mucho historial? → La evolución temporal agrupa por semana o mes para mantenerse legible; los rankings respetan los límites del servidor.
- ¿Qué ocurre si el usuario cambia de servidor activo? → La vista muestra los datos del servidor activo en el momento de la carga y permite actualizarlos.
- ¿Qué ocurre con días sin escucha dentro del periodo? → La serie temporal los representa con valor cero o los omite sin falsear la escala.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La app MUST ofrecer una vista de gráficas de escucha accesible mediante un botón "Gráficas" en la pantalla de Configuración, junto a "Acerca de", que abra la vista completa; el botón MUST ser visible y reconocible sin búsqueda adicional.
- **FR-002**: La vista MUST mostrar, para el periodo activo, los mismos bloques de datos que la página de gráficas de la web: resumen (tiempo total, emisora destacada, día con más escucha), evolución temporal, ranking de emisoras, reparto por género, reparto por país, matriz de hábitos (día×hora) y actividad reciente.
- **FR-003**: La vista MUST permitir seleccionar el periodo de análisis con al menos estas opciones: 7 días, 30 días, 90 días y todo el histórico; el periodo por defecto al abrir MUST ser 30 días y cambiarlo MUST recalcular todos los bloques.
- **FR-004**: La evolución temporal MUST agrupar los datos por día, semana o mes según la amplitud del periodo, de forma automática y sin configuración adicional del usuario.
- **FR-005**: Los datos MUST corresponder al usuario autenticado del servidor activo; al cambiar de servidor o al recargar, la vista MUST mostrar los datos del servidor activo en ese momento.
- **FR-006**: La función MUST basarse exclusivamente en las escuchas ya registradas por el servidor (reproducción vía proxy), sin capturar ni registrar localmente datos de escucha nuevos.
- **FR-007**: La vista MUST manejar estados de carga, vacío y error; los errores MUST mapearse al formato de errores del backend y mostrar mensajes accionables en español con reintento, sin filtrar datos personales ni credenciales.
- **FR-008**: Los tiempos de escucha MUST presentarse en formato legible para el usuario (minutos/horas) con etiquetas en español.
- **FR-009**: La vista MUST ser usable en pantallas móviles sin desplazamiento horizontal, manteniendo legibles etiquetas y valores de cada gráfica.
- **FR-010**: El periodo personalizado por fechas a medida y la exportación de datos QUEDAN FUERA de alcance de esta versión.

### Key Entities *(include if feature involves data)*

- **Periodo de análisis**: rango temporal de la consulta (7 días, 30 días, 90 días o todo el histórico); determina qué escuchas se agregan.
- **Indicador de escucha agregado**: resultado agregado por bloque — tiempo total por intervalo (día/semana/mes), tiempo por emisora, tiempo por género, tiempo por país y tiempo por franja día×hora.
- **Escucha registrada**: reproducción del usuario con emisora, fecha de inicio y duración; es la fuente de la que derivan todos los indicadores y la registra el servidor, no la app.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El usuario localiza y abre la vista de gráficas en 10 segundos o menos y sin ayuda, en una primera prueba con usuarios de la app.
- **SC-002**: Tras abrir la vista, los indicadores del periodo por defecto son visibles en menos de 2 segundos con conexión normal.
- **SC-003**: Para el mismo usuario, servidor y periodo, los bloques de datos de la app coinciden al 100% con los mostrados en la página de gráficas de la web.
- **SC-004**: Cambiar de periodo actualiza todos los bloques sin inconsistencias (ningún bloque conserva datos del periodo anterior).
- **SC-005**: Con más de un año de historial de escucha, la vista se navega con fluidez, sin bloqueos visibles ni pérdida de interacción.
- **SC-006**: En pruebas con datos parciales o vacíos, la vista nunca muestra pantalla en blanco ni termina en error sin salida: siempre hay estado vacío o de error accionable.

## Assumptions

- La app ya dispone de sesión automática por servidor (email y contraseña cifradas); las gráficas usan esa sesión sin pedir credenciales ni mostrar pantallas de login.
- El backend ya registra las escuchas al reproducir vía proxy y ya expone los agregados de estadísticas del usuario autenticado; no se prevén cambios en el backend.
- Paridad con la web: se replican los bloques de la página de gráficas de la web (resumen, evolución temporal, top emisoras, géneros, países, hábitos y recientes), sin inventar analítica nueva.
- El periodo por defecto es 30 días, igual que en la web.
- El rango personalizado por fechas y la exportación de datos quedan fuera de alcance de v1; podrían añadirse en una iteración futura si se echan en falta.
- Las gráficas son privadas: solo se muestran datos del usuario autenticado del servidor activo; no existen datos agregados de otros usuarios.
- Punto de entrada decidido (opción A, 2026-10-02): botón "Gráficas" en Configuración junto a "Acerca de". Se descarta un apartado de primer nivel en la navegación para no saturarla; la paridad de secciones exigida con la web se refiere al contenido (Explorar, Favoritos, Historial, Mis emisoras), no a esta vista.
