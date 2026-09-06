# Feature Specification: Alternador de Vista Lista/Tarjetas en la Barra Superior

**Feature Branch**: `008-view-mode-toggle`

**Created**: 2026-09-06

**Status**: Done (2026-09-06; todo verificado)

**Input**: User description: "Nueva Feature: En la barra superior, poner una función que permite alternar en todos los areas entre la vista de tarjetas o la vista de lista como en la web. Por defecto al acceder se verá en modo de lista."

## Clarifications

### Session 2026-09-06

- Q: ¿El icono del alternador representa el modo al que se cambiaría o el modo activo en pantalla? → A: El modo destino: se muestra el icono de "cuadrícula" cuando se está en lista y el de "lista" cuando se está en tarjetas.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Alternar entre vista de lista y vista de tarjetas (Priority: P1)

Como usuario de la app, quiero un control en la barra superior de cada sección que muestre emisoras, para alternar entre ver el contenido como lista compacta o como tarjetas (cuadrícula), igual que hago en la web de TolochaRadio. Al entrar por primera vez en la app, todas las secciones se muestran en modo lista.

**Why this priority**: Es el núcleo de la feature: sin el alternador no hay valor. Un usuario que prefiere explorar con tarjetas visuales o con listas densas gana control inmediato sobre cómo consume el catálogo.

**Independent Test**: Se puede probar completamente abriendo cualquier sección con emisoras, pulsando el control de la barra superior y verificando que el contenido cambia entre lista y tarjetas sin recargar datos ni perder la posición.

**Acceptance Scenarios**:

1. **Given** el usuario accede por primera vez a la app (sin preferencia guardada), **When** abre cualquier sección con emisoras (Explorar, Favoritos, Historial, Mis emisoras), **Then** el contenido se muestra en modo lista.
2. **Given** el usuario está en una sección mostrada en modo lista, **When** pulsa el control de la barra superior, **Then** la sección pasa a modo tarjetas y el control pasa a mostrar el icono de lista (modo destino).
3. **Given** el usuario está en modo tarjetas, **When** vuelve a pulsar el control, **Then** la sección vuelve a modo lista.
4. **Given** el usuario cambia el modo en una sección, **When** navega a otra sección con emisoras, **Then** esa sección también se muestra en el modo seleccionado (la preferencia es global a toda la app).

---

### User Story 2 - Persistencia del modo elegido entre sesiones (Priority: P2)

Como usuario, quiero que la app recuerde el modo de vista que elegí, para no tener que activar tarjetas de nuevo cada vez que abro la aplicación.

**Why this priority**: Añade continuidad al hábito del usuario; es la diferencia entre una función anecdótica y una preferencia real. Depende del alternador (P1) pero es independiente del resto.

**Independent Test**: Se puede probar cambiando a modo tarjetas, cerrando y reabriendo la app, y verificando que todas las secciones arrancan en modo tarjetas.

**Acceptance Scenarios**:

1. **Given** el usuario seleccionó modo tarjetas, **When** cierra y vuelve a abrir la app, **Then** todas las secciones se muestran en modo tarjetas.
2. **Given** el usuario nunca cambió el modo, **When** abre la app en cualquier sesión, **Then** se muestra siempre modo lista (comportamiento por defecto).
3. **Given** el usuario vuelve a modo lista, **When** cierra y reabre la app, **Then** la preferencia vuelve a ser lista (el modo se sobrescribe, no se acumulan preferencias).

---

### User Story 3 - Transición consistente y sin pérdida de contexto (Priority: P3)

Como usuario, quiero que al alternar el modo la experiencia sea fluida, para no perder mi lugar en el catálogo ni esperar recargas.

**Why this priority**: Es un refinamiento de calidad: la feature ya es usable sin él, pero garantiza una experiencia a la par con la web.

**Independent Test**: Se puede probar alternando el modo con una lista paginada avanzada y verificando que no se reinicia la carga ni el scroll de forma disruptiva.

**Acceptance Scenarios**:

1. **Given** el usuario ha avanzado en la carga de más resultados (paginación), **When** alterna el modo de vista, **Then** la app mantiene los datos ya cargados y muestra el contenido en el nuevo formato sin pantalla de carga completa.
2. **Given** la sección está en estado vacío (sin emisoras), **When** el usuario pulsa el alternador, **Then** no ocurre ningún error y el estado vacío se mantiene.
3. **Given** hay una búsqueda o filtros activos en Explorar, **When** alterna el modo de vista, **Then** la búsqueda y los filtros se conservan y los resultados se re-presentan en el nuevo modo.

---

### Edge Cases

- ¿Qué ocurre cuando el usuario pulsa el alternador mientras se están cargando emisoras? El modo cambia y el contenido en carga se presenta ya en el nuevo formato; no se cancela ni duplica la carga.
- ¿Qué ocurre en secciones sin contenido de lista (p. ej. Perfil, Login)? El alternador no se muestra en secciones donde no aplica; no aparece deshabilitado sin sentido.
- ¿Qué ocurre si la preferencia guardada está corrupta o es inválida? La app debe caer al modo por defecto (lista) sin error visible.
- ¿Qué ocurre con estados de error de red en una sección? El alternador sigue funcionando y el estado de error con su reintento se mantiene en el formato de vista correspondiente.
- ¿Qué ocurre durante una rotación de pantalla o cambio de configuración? El modo de vista seleccionado se mantiene.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La barra superior de cada sección que muestra emisoras (Explorar, Favoritos, Historial, Mis emisoras) MUST incluir un control visible que permita alternar entre vista de lista y vista de tarjetas.
- **FR-002**: El control MUST mostrar el icono del modo destino (icono de cuadrícula cuando la vista actual es lista, icono de lista cuando la vista actual es tarjetas), ser reconocible como botón de alternancia y ser accesible mediante teclado/controles de accesibilidad con descripción hablada ("cambiar a vista de tarjetas"/"cambiar a vista de lista").
- **FR-003**: Al pulsar el control, la sección actual MUST re-presentar su contenido en el modo alternativo inmediatamente, sin recargar los datos del servidor.
- **FR-004**: El modo de vista MUST ser una preferencia global de la app: todas las secciones con listas de emisoras respetan el mismo modo simultáneamente.
- **FR-005**: El modo por defecto al acceder sin preferencia previa MUST ser vista de lista.
- **FR-006**: La preferencia de modo MUST persistir entre sesiones y sobrescribirse cada vez que el usuario alterna el modo.
- **FR-007**: El cambio de modo MUST conservar el estado de la sección: búsqueda, filtros, resultados ya paginados y estados de error/vacío.
- **FR-008**: El modo de vista de tarjetas MUST presentar cada emisora con la misma información esencial que el modo lista (nombre, favicon, país/idioma, indicador de favorita), organizada en cuadrícula, y MUST permitir las mismas acciones que el modo lista (reproducir, abrir detalle, marcar/desmarcar favorita).
- **FR-009**: El alternador MUST omitirse en pantallas sin contenido de lista (Login, Registro, Perfil, reproductor a pantalla completa).
- **FR-010**: Si la preferencia persistida resulta inválida o ilegible, el sistema MUST usar el modo por defecto (lista) sin mostrar error.

### Key Entities *(include if feature involves data)*

- **Preferencia de modo de vista**: valor de usuario único y global con dos estados posibles (lista, tarjetas); se guarda localmente en el dispositivo, sin sincronización con el servidor en esta versión; su ausencia equivale a "lista".

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El usuario puede cambiar de vista con una sola interacción desde cualquier sección con emisoras, y el contenido se re-presenta en menos de 1 segundo en un dispositivo de gama media.
- **SC-002**: El 100% de las secciones con listas de emisoras responden al mismo modo de vista global (verificable recorriendo las 4 secciones tras un solo cambio).
- **SC-003**: Al primer acceso (instalación limpia), 100% de las secciones arrancan en modo lista.
- **SC-004**: Tras elegir un modo, la preferencia se mantiene en el 100% de los reinicios de app probados.
- **SC-005**: Al alternar el modo no se pierden búsqueda, filtros ni resultados paginados en ninguna sección (verificado en los flujos principales).

## Assumptions

- La preferencia es global (un solo modo para toda la app), igual que en la web; no se ofrece modo por sección en esta versión.
- La preferencia se guarda solo localmente en el dispositivo; no hay sincronización con la cuenta del servidor (la web y la app no comparten esta preferencia).
- "Todos los áreas" se interpreta como todas las secciones que muestran listas de emisoras (Explorar, Favoritos, Historial, Mis emisoras); pantallas sin listas no muestran el alternador.
- El comportamiento por defecto "lista" aplica solo cuando no existe preferencia guardada; después, manda la última elección del usuario.
- El diseño visual del alternador seguirá la dirección UX existente (Material 3, estilo Pocket Casts, íconos representando lista/cuadrícula), en paridad con la web.
