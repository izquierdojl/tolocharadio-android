# Feature Specification: Reordenación intuitiva de favoritos

**Feature Branch**: `0034-jlizquierdo-20260913-favorites-reorder-animation`

**Created**: 2026-09-13

**Status**: Draft

**Input**: User description: "Vamos a mejorar un aspecto del programa en la opción de favoritos. Ahora mismo se pueden mover con un selector, pero es muy poco intuitivo, no tiene animación y solo se ve el movimiento cuando se suelta al final."

## Clarifications

### Session 2026-09-13

- Q: Si un arrastre se cancela o se interrumpe, ¿qué debe pasar con el orden que veías? → A: Conservar el orden mostrado y tratarlo como un soltar normal (se guarda).
- Q: ¿Cómo debe empezar el arrastre de una favorita? → A: Arrastre inmediato al tocar y mover el asa, sin pulsación larga ni espera previa.
- Q: ¿Cómo se debe ofrecer la alternativa de reordenar sin arrastre? → A: Menú de desbordamiento "..." por fila con "Mover arriba" / "Mover abajo".
  *(SUPERSEDED por la spec 0039: eliminación completa del menú; el arrastre es la única vía.)*
- Q: ¿Qué debe pasar si el usuario intenta reordenar sin conexión (caché offline)? → A: Deshabilitar el reorden sin conexión (sin asa ni acciones), caché en solo lectura.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reordenar favoritas viendo el movimiento en vivo (Priority: P1)

Como oyente con varias emisoras favoritas, quiero arrastrar una favorita hacia otra
posición y ver **mientras arrastro** cómo el resto de filas se reacomodan con
animación, para saber con certeza dónde va a quedar antes de soltar.

**Why this priority**: Es el núcleo de la mejora. Hoy el orden solo se percibe al
soltar, sin animación, lo que convierte el reorden en una acción a ciegas y poco
intuitiva. Sin este comportamiento el resto de mejoras pierde valor.

**Independent Test**: Con al menos 2 favoritas en vista de lista, iniciar el arrastre de
una fila y comprobar que la fila activa se resalta (elevada) y que las demás se
desplazan de forma continua y animada al cambiar de posición, antes de soltar.

**Acceptance Scenarios**:

1. **Given** una lista con al menos 2 favoritas, **When** se arrastra el asa de una
   fila hacia arriba o hacia abajo, **Then** la fila activa se muestra elevada/resaltada
   y las filas cruzadas se desplazan con una animación continua.
2. **Given** un arrastre en curso, **When** la fila activa cruza la posición de otra
   fila, **Then** el hueco de destino se actualiza de inmediato (sin esperar al soltar).
3. **Given** un arrastre en curso, **When** se suelta el dedo, **Then** la fila se
   asienta en la posición mostrada durante el arrastre, sin saltos.

---

### User Story 2 - Iniciar el arrastre de forma natural y cómoda (Priority: P2)

Como usuario, quiero poder empezar a arrastrar la favorita con un gesto directo sobre
el asa (sin una espera poco clara) y que la lista se desplace sola al acercarme a los
bordes, para reordenar listas largas sin soltar ni repetir el gesto.

**Why this priority**: Mejora la descubribilidad y el alcance del reorden, pero solo
aporta si ya existe feedback visual en vivo (US1).

**Independent Test**: Iniciar el gesto sobre el asa de una favorita y comprobar que el
arrastre comienza de forma inmediata y predecible; en una lista que desborde la
pantalla, acercar la fila al borde superior/inferior y verificar que la lista se
desplaza automáticamente de forma progresiva.

**Acceptance Scenarios**:

1. **Given** la lista de favoritas, **When** se apoya el dedo sobre el asa y se mueve
   verticalmente, **Then** el arrastre comienza sin ambigüedad y con respuesta
   inmediata.
2. **Given** un arrastre en curso sobre una lista larga, **When** la fila activa se
   acerca al borde superior o inferior, **Then** la lista se auto-desplaza y la
   velocidad aumenta cuanto más cerca está del borde.
3. **Given** un gesto que no llega a ser un arrastre, **When** el usuario toca una fila
   fuera del asa, **Then** se conserva el comportamiento normal (abrir ficha,
   reproducir, quitar favorita) sin iniciar reorden.

---

### User Story 3 - Reordenar sin gestos de arrastre (Priority: P3)

*(SUPERSEDED por la spec 0039 FR-001 — eliminación completa del menú de mover: esta historia
ya no aplica; ver también la clarificación Q3 de esta spec.)*

Como usuario que no puede o no quiere usar gestos de arrastre (incluida la navegación
asistida por lector de pantalla), quiero una forma alternativa de mover una favorita
una posición arriba o abajo, para reordenar con acciones explícitas.

**Why this priority**: Garantiza que la mejora sea usable por todos, pero es
complementaria al gesto principal.

**Independent Test**: Sobre una fila de favorita, abrir el menú de desbordamiento y
ejecutar "Mover arriba" y "Mover abajo" y verificar que la fila cambia una posición y
que el orden se guarda igual que con el arrastre.

**Acceptance Scenarios**:

1. **Given** una favorita que no está en la primera posición, **When** se elige "Mover
   arriba", **Then** la favorita intercambia posición con la inmediatamente anterior.
2. **Given** una favorita que no está en la última posición, **When** se elige "Mover
   abajo", **Then** la favorita intercambia posición con la inmediatamente siguiente.
3. **Given** una favorita en el primer o último lugar, **When** se abre el menú, **Then**
   la opción correspondiente aparece deshabilitada y no mueve la favorita fuera de la
   lista.

---

### Edge Cases

- **Lista de una sola favorita**: no hay nada que reordenar; el asa y las acciones del
  menú de movimiento deben mostrarse inactivas o no mostrarse, y no debe guardarse orden.
  *(Parte del menú SUPERSEDED por la 0039: solo aplica ocultar el asa.)*
- **Soltar fuera de la lista o cancelar el gesto**: el orden queda en la última posición
  mostrada y se trata como un soltar normal (se guarda automáticamente), sin dejar la
  lista en estado inconsistente.
- **Fallo al guardar el orden**: se restaura el último orden confirmado por el servidor
  y se informa al usuario; si se puede, se ofrece reintentar.
- **Orden cambiado en otro dispositivo**: si al guardar el servidor rechaza la
  permutación, prevalece el orden del servidor y se avisa al usuario.
- **Sin conexión**: la lista muestra la caché offline en modo solo lectura; el asa y las
  acciones de movimiento aparecen deshabilitadas y no se permite reordenar.
  *(Parte del menú SUPERSEDED por la 0039: solo aplica ocultar el asa.)*
- **Arrastre hasta un extremo con auto-scroll**: la posición debe seguir actualizándose
  mientras la lista se desplaza, sin perder la fila activa.
- **Interrupción (rotación, salida a segundo plano, llamada) durante el arrastre**: al
  volver, la lista muestra el último orden confirmado y no queda un gesto "pegado".
- **Vista de cuadrícula**: el reorden por arrastre no aplica (ya ocurre así); debe
  mantenerse sin asa y sin romper la disposición en tarjetas.
- **Preferencia del sistema "reducir movimiento" activada**: las animaciones de
  reacomodo deben reducirse o desactivarse, manteniéndose el feedback de posición.
- **Reproducción en curso**: reordenar no debe detener ni reiniciar la reproducción.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir iniciar el reorden de una favorita desde un
  control dedicado (asa de arrastre) visible en cada fila en la vista de lista, siempre
  que haya conexión (con caché offline la lista es de solo lectura).
- **FR-002**: Durante el arrastre, la fila activa MUST diferenciarse visualmente del
  resto (p. ej. elevación, escala o sombra) para indicar que está siendo movida.
- **FR-003**: Durante el arrastre, las filas afectadas MUST desplazarse mediante una
  animación continua hasta sus nuevas posiciones, en lugar de cambiar de golpe.
- **FR-004**: La posición de destino MUST actualizarse en cuanto la fila activa cruza
  la posición de otra fila, sin esperar a que el usuario suelte.
- **FR-005**: Al soltar, la favorita MUST asentarse en la posición que se mostraba
  durante el arrastre, sin saltos adicionales.
- **FR-006**: El arrastre MUST iniciarse de forma inmediata al tocar y mover el asa,
  sin pulsación larga ni espera previa.
- **FR-007**: Cuando la fila activa se acerque al borde superior o inferior de la lista,
  el sistema MUST desplazar la lista automáticamente, con velocidad creciente según la
  cercanía al borde.
- **FR-008**: El sistema SHOULD emitir respuesta háptica al agarrar la fila, al cambiar
  de posición y al soltar.
- **FR-009**: El orden resultante MUST guardarse automáticamente al soltar, sin pasos
  adicionales del usuario, y el sistema MUST indicar que el guardado está en curso.
- **FR-010**: Si el guardado del orden falla, el sistema MUST restaurar el último orden
  confirmado, avisar al usuario y ofrecer una acción de reintento cuando el error sea
  recuperable (p. ej. fallo de red); en errores no recuperables se informa sin reintento.
- **FR-011**: Si el servidor rechaza la permutación por haber cambiado en otro
  dispositivo, el sistema MUST mostrar el orden del servidor y notificarlo.
- **FR-012**: El sistema MUST ofrecer, en cada fila de la vista de lista, un menú de
  desbordamiento ("...") con las acciones "Mover arriba" y "Mover abajo", usables con
  lector de pantalla y equivalentes en resultado al arrastre.
  *(SUPERSEDED por la spec 0039 FR-001: eliminación completa del menú de mover.)*
- **FR-013**: Las acciones del menú de movimiento MUST aparecer deshabilitadas cuando la
  favorita ya está en el extremo correspondiente, de modo que no se pueda mover fuera de la
  lista.
  *(SUPERSEDED por la spec 0039 FR-001.)*
- **FR-014**: El sistema MUST respetar la preferencia del sistema de "reducir
  movimiento", reduciendo o eliminando las animaciones sin perder la indicación de
  posición.
- **FR-015**: El reorden MUST limitarse a la vista de lista; en la vista de cuadrícula
  la lista se mantiene sin control de arrastre.
- **FR-016**: Reordenar MUST NOT alterar la reproducción en curso ni el estado del
  reproductor.
- **FR-017**: Las interacciones existentes de la fila (abrir ficha, reproducir, quitar
  favorita con deshacer) MUST seguir funcionando y no confundirse con el arrastre.
- **FR-018**: Sin conexión, el sistema MUST deshabilitar el reorden (asa y acciones de
  movimiento) y mantener la caché offline como solo lectura, sin permitir cambios de
  orden.

### Key Entities *(include if feature involves data)*

- **Favorita**: emisora guardada por el usuario; su posición dentro de la lista define
  el orden personalizado. Atributos relevantes: identificador de emisora y posición.
- **Orden de favoritas**: secuencia ordenada de identificadores de emisora que
  representa la preferencia del usuario; es la unidad que se confirma y se guarda.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Al menos el 90 % de los usuarios consigue reordenar una favorita al primer
  intento en una prueba de usabilidad de 5 personas.
- **SC-002**: Durante el arrastre, la reacomodación visual se percibe como inmediata
  (por debajo de 100 ms tras cruzar una posición) en al menos el 95 % de los cruces.
- **SC-003**: El 100 % de los reordenamientos completados se guardan sin que el usuario
  tenga que pulsar ningún botón adicional, salvo cuando falla la red.
- **SC-004**: El tiempo medio para mover una favorita de la última a la primera posición
  se reduce al menos un 40 % respecto al comportamiento actual.
- **SC-005**: El 100 % de los flujos de reorden se pueden completar usando solo lector de
  pantalla y acciones alternativas, sin gesto de arrastre.
  *(SUPERSEDED por la spec 0039 FR-001: ya no existen acciones alternativas de menú; el
  arrastre con asa es la única vía.)*
- **SC-006**: Con la preferencia "reducir movimiento" activada, no se observan
  animaciones de reacomodo, pero la posición de destino sigue siendo identificable.

## Assumptions

- La funcionalidad de reorden de favoritas ya existe (spec 003); esta feature la mejora,
  no la reemplaza ni cambia el modelo de datos ni el contrato del servidor.
- El backend ya ofrece la operación de guardar el orden (permutación de identificadores);
  no se requieren cambios de servidor.
- El reorden se ofrece únicamente en la vista de lista; la vista de cuadrícula queda como
  está.
- El gesto de arrastre del asa es la interacción principal; la alternativa accesible es
  complementaria.
- La respuesta háptica depende de que el dispositivo la soporte; su ausencia no impide el
  reorden.
- El guardado del orden es automático al soltar (autoguardado), con indicación de
  progreso y recuperación ante error.
- El reorden requiere conexión; no se introduce una cola de cambios offline (la caché
  offline es de solo lectura).
- Los textos de la interfaz están en español.
- No se introducen dependencias nuevas ni cambios de arquitectura; la mejora se apoya en
  las capacidades de animación y accesibilidad ya disponibles en el stack aprobado.
