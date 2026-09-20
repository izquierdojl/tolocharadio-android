# Feature Specification: Mejoras estéticas UI

**Feature Branch**: `0039-jlizquierdo-20260920-mejoras-esteticas-ui`

**Created**: 2026-09-20

**Status**: Draft

**Input**: User description: "Hola, vamos a hacer unas mejoras estéticas para organizar mejor el espacio: en la pantalla de favoritos, el menú de la derecha que permite seleccionar mover arriba y mover abajo es redundante ya que el arrastrar funciona muy bien (ocultarlo o eliminarlo directamente); en el panel de reproductor, trasladar el botón para compartir el enlace de la emisora real a la pantalla donde se ve la información de la emisora al hacer click, dejando sólo el botón de play/pause y el de silenciar; debajo del texto historial aparece un texto descriptivo 'Lo último que has escuchado', proponer dos textos sencillos similares y explicativos debajo de Explorar y Tus favoritos."

## Clarifications

### Session 2026-09-20

- Q: ¿Cómo debe ofrecer la ficha de información de la emisora el enlace real para compartirlo? → A: Ambas: compartir del sistema como principal y "Copiar" como secundaria.
- Q: ¿Dónde debe aparecer la opción de compartir dentro de la ficha de información de la emisora? → A: Sección propia "Enlace" encima de la homepage, con botones Compartir y Copiar.
- Q: ¿El límite de "solo play/pausa y silenciar" en el panel debe aplicarse también a los estados de error y carga? → A: Solo a reproducción normal (sonando/pausado); error y carga conservan reintentar y cancelar.
- Q: ¿Confirmas los subtítulos propuestos para Explorar y Tus favoritos? → A: Sí, los propuestos: Explorar "Descubre emisoras de todo el mundo." y Favoritas "Tus emisoras guardadas, en tu orden.".
- Q: ¿El menú de mover en favoritas debe eliminarse por completo del código o solo ocultarse la interfaz? → A: Eliminación completa del menú y de sus acciones de mover arriba/abajo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reordenar favoritas solo por arrastre (Priority: P1)

La persona usuaria abre la pantalla de favoritas y reordena su lista arrastrando cada emisora por su asa. Ya no encuentra ningún menú lateral con opciones de mover arriba o mover abajo; el arrastre es la única vía de reordenación.

**Why this priority**: Elimina un control redundante que ocupa espacio en cada fila y simplifica la interfaz; es el cambio de mayor impacto visual de la petición.

**Independent Test**: Se puede probar abriendo la lista de favoritas con dos o más emisoras y verificando que no existe menú de mover y que el arrastre sigue reordenando y guardando el orden.

**Acceptance Scenarios**:

1. **Given** la usuaria está en favoritas con varias emisoras, **When** mira cada fila, **Then** no ve ningún botón de más opciones ni las opciones de mover arriba o mover abajo.
2. **Given** la usuaria está en favoritas con varias emisoras, **When** arrastra una emisora a otra posición y suelta, **Then** el orden cambia y se conserva al salir y volver a la pantalla.
3. **Given** la usuaria está en favoritas con una sola emisora o sin conexión de solo lectura, **When** mira la lista, **Then** tampoco aparece el menú de mover.

---

### User Story 2 - Compartir enlace desde la ficha de emisora (Priority: P1)

La persona usuaria está escuchando una emisora y ve en el panel de reproducción únicamente los botones de play/pausa y silenciar. Cuando quiere compartir el enlace real de la emisora, abre la ficha de información de la emisora (al pulsar sobre el panel) y allí encuentra la opción de compartir mediante el menú del sistema y una opción secundaria de copiar el enlace.

**Why this priority**: Libera espacio en el panel de reproducción manteniendo una funcionalidad que la usuaria quiere conservar, solo reubicada.

**Independent Test**: Se puede probar reproduciendo una emisora, verificando los botones del panel y abriendo la ficha de información para compartir el enlace.

**Acceptance Scenarios**:

1. **Given** se está reproduciendo una emisora, **When** la usuaria mira el panel de reproducción, **Then** solo ve el botón principal (play/pausa) y el de silenciar, sin botón de compartir enlace.
2. **Given** se está reproduciendo una emisora, **When** la usuaria abre la ficha de información de la emisora y activa la opción de compartir, **Then** se abre el menú de compartir del sistema con el mismo enlace real de la emisora que antes ofrecía el panel; con la opción secundaria de copiar, el enlace queda en el portapapeles y se muestra el aviso (se puede pegar fuera de la app).
3. **Given** la emisora no tiene enlace disponible, **When** la usuaria intenta compartirlo desde la ficha, **Then** recibe un aviso claro de que el enlace no está disponible.

---

### User Story 3 - Encabezados explicativos en Explorar y Favoritas (Priority: P2)

La persona usuaria entra en Explorar y en Tus favoritos y, debajo de cada título, lee una frase corta que explica qué encontrará allí, con el mismo tono que el subtítulo existente del historial ("Lo último que has escuchado.").

**Why this priority**: Da coherencia visual a las tres secciones principales y orienta a quien abre la app por primera vez; es un cambio puramente informativo.

**Independent Test**: Se puede probar navegando a Explorar, Favoritas e Historial y comparando los encabezados.

**Acceptance Scenarios**:

1. **Given** la usuaria abre Explorar, **When** mira el encabezado, **Then** ve el título "Explorar" y debajo el subtítulo "Descubre emisoras de todo el mundo.".
2. **Given** la usuaria abre favoritas, **When** mira el encabezado, **Then** ve el título "Tus favoritos" y debajo el subtítulo "Tus emisoras guardadas, en tu orden.".
3. **Given** la usuaria abre el historial, **When** mira el encabezado, **Then** sigue viendo "Tu historial" con "Lo último que has escuchado.", con el mismo estilo visual que los dos anteriores.

### Edge Cases

- ¿Qué ocurre cuando la lista de favoritas está vacía o hay un error de carga? No se muestra ningún control de reordenación; solo el estado vacío o el mensaje de error habitual.
- ¿Cómo se comporta la opción de compartir si la emisora no tiene URL o es inválida? La ficha muestra el aviso de enlace no disponible y no se copia ni se comparte nada.
- ¿Qué ocurre si el enlace de la emisora es muy largo en la ficha? Se muestra recortado de forma legible pero al compartir se usa el enlace completo.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La lista de favoritas debe eliminar por completo (no solo ocultar) el menú de más opciones y las acciones de mover arriba o mover abajo en todas las filas.
- **FR-002**: La lista de favoritas debe seguir permitiendo reordenar por arrastre con el mismo comportamiento actual (mover en vivo y guardar el orden al soltar).
- **FR-003**: Durante la reproducción normal (sonando o en pausa) el panel debe mostrar únicamente el botón principal (play/pausa) y el botón de silenciar/activar sonido, sin botón de compartir o copiar enlace; los estados de error y carga conservan sus acciones especiales (reintentar y cancelar).
- **FR-004**: La ficha de información de la emisora (la que se abre al pulsar sobre el panel) debe incluir una sección propia "Enlace" encima de la homepage con una opción principal de compartir mediante el menú del sistema y una opción secundaria de copiar el enlace.
- **FR-005**: Ambas opciones de la ficha deben usar el mismo enlace real que antes ofrecía el panel; al copiar se muestra el aviso de confirmación y al compartir se entrega el enlace completo al sistema.
- **FR-006**: La opción de compartir de la ficha debe informar de forma clara cuando el enlace no está disponible, sin bloquear la ficha ni la reproducción.
- **FR-007**: La pantalla Explorar debe mostrar bajo el título el subtítulo "Descubre emisoras de todo el mundo.".
- **FR-008**: La pantalla de favoritas debe mostrar bajo el título "Tus favoritos" el subtítulo "Tus emisoras guardadas, en tu orden.".
- **FR-009**: Los tres encabezados (Explorar, Tus favoritos, Tu historial) deben mantener el mismo estilo visual de título y subtítulo entre sí.

### Key Entities

No hay entidades de datos nuevas: la mejora reutiliza la emisora (nombre y enlace real) y los textos de encabezado de cada sección.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las filas de favoritas inspeccionadas no muestran el menú de mover arriba o mover abajo.
- **SC-002**: Una usuaria puede reordenar su lista de favoritas por arrastre y el orden persiste al volver a la pantalla en el 100 % de los intentos.
- **SC-003**: El panel de reproducción muestra como máximo 2 botones de acción (principal + silenciar) durante la reproducción normal.
- **SC-004**: El 100 % de las veces que se comparte el enlace desde la ficha, el texto obtenido coincide con el enlace real de la emisora mostrada.
- **SC-005**: Las 3 secciones (Explorar, Favoritas, Historial) muestran título más subtítulo descriptivo con apariencia coherente.

## Assumptions

- Se elimina el menú de mover (no solo se oculta), junto con sus acciones asociadas, porque el arrastre ya cubre el caso y la usuaria lo permite explícitamente.
- La función de compartir en la ficha ofrece el enlace real mediante el menú de compartir del sistema (principal) más la opción de copiar al portapapeles con aviso (secundaria); no incluye edición del enlace.
- Los subtítulos quedan confirmados (clarificación 2026-09-20): "Descubre emisoras de todo el mundo." para Explorar y "Tus emisoras guardadas, en tu orden." para Tus favoritos; cualquier cambio posterior de redacción es un ajuste menor de revisión.
- Fuera de estos tres puntos no cambia ningún otro comportamiento: ni el orden personalizado, ni los estados de carga o error, ni la reproducción en curso.
