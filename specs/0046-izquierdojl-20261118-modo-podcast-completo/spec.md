# Feature Specification: Modo Podcast — paridad con web

**Feature Branch**: `0046-izquierdojl-20261118-modo-podcast-completo`

**Created**: 2026-11-18

**Status**: Done (2026-11-19; flujo verificado en emulador por el usuario; incluye ajustes de UX posteriores)

**Input**: User description: "Esta aplicación ya esta muy desarrollada y estable, pero se ha desarrollado en el servicio web http://www.github.com/izquierdojl/tolocharadio gestión completa de Podcast y ya está funcional. Habría que planear pasarla a la aplicación móvil con las siguientes características similares a la web. - Selector rápido de modo radio o podcast, con opción que pueda predeterminar el usuario en la configuración - Al seleccionar podcast, en la barra inferior se ven en este orden "Suscripciones", "Favoritos...." como en radio, pero adaptado a las opciones de podcast. - Hay que extender absolutamente toda la funcionad de la web en cuanto a suscripciones, favoritos, colas, descargas, búsquedas, etc..."

## Clarifications

### Session 2026-11-18

- Q: ¿Qué 5 destinos debe mostrar la barra inferior en modo Podcasts y en qué orden? → A: Inicio, Descubrir, Suscripciones, Cola, Favoritos de episodios (Estadísticas accesible desde Inicio).
- Q: Cuando el servidor permite descargas, ¿cómo debe entregar la app el archivo del episodio? → A: Descarga interna guardada en Descargas de Android (ubicación controlada); si el archivo está descargado, reproducir directamente el archivo local en lugar del modo online para ahorrar datos (estilo Pocket Casts).
- Q: ¿Cuándo debe salir un episodio de 'continuar escuchando' y qué acciones manuales debe ofrecer? → A: Sale automáticamente al terminar; además la persona puede descartarlo manualmente de "continuar escuchando" (paridad web).
- Q: ¿Cómo debe abrirse la ficha detalle de un episodio sin perder el punto de la lista ni detener la reproducción? → A: Hoja inferior modal sobre la lista; al cerrarse devuelve el foco al elemento que la abrió.
- Q: ¿Cómo debe importar y exportar la app los ficheros OPML de suscripciones? → A: Importar con el selector de ficheros del sistema y exportar con compartir/guardar del sistema.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cambiar entre Radio y Podcasts y elegir sección inicial (Priority: P1)

La persona usuaria ve un selector rápido **Radio | Podcasts** en la cabecera y cambia de modo sin perder la reproducción en curso. En Configuración puede predeterminar con qué modo se abre la aplicación.

**Why this priority**: Es la puerta de entrada a todo lo demás. Sin selector y sin valor predeterminado no hay paridad con la web y cada arranque obliga a navegar de más.

**Independent Test**: Con un servidor que ofrece podcasts, cambiar de Radio a Podcasts y volver, reiniciar la aplicación y comprobar que se abre en el modo predeterminado configurado. Entrega valor aunque no exista nada más de podcast.

**Acceptance Scenarios**:

1. **Given** un servidor con podcasts habilitados, **When** la persona toca el selector "Podcasts", **Then** la barra inferior muestra las secciones de podcast y el reproductor en curso (si lo hay) sigue sonando sin interrupción.
2. **Given** la persona está en modo Podcasts, **When** abre Configuración y elige "Radio" como sección inicial y reinicia la aplicación, **Then** la aplicación se abre en modo Radio.
3. **Given** un servidor sin podcasts habilitados, **When** la persona usa la aplicación, **Then** no se ofrece el modo Podcasts y la aplicación sigue funcionando solo en Radio sin errores.

---

### User Story 2 - Descubrir programas y suscribirse (Priority: P1)

La persona usuaria busca programas por nombre en el directorio, da de alta manual una dirección RSS y puede importar/exportar su lista de suscripciones en formato OPML. Desde el resultado se suscribe o se da de baja.

**Why this priority**: Sin suscripciones no hay biblioteca. Es el flujo de adquisición equivalente a "Explorar" en radio y el que la web ofrece en Descubrir/Suscripciones.

**Independent Test**: Buscar un programa por nombre, suscribirse, verlo en Suscripciones, darse de baja y comprobar que desaparece. Importar un fichero OPML y comprobar que aparecen las suscripciones.

**Acceptance Scenarios**:

1. **Given** la persona está en modo Podcasts, **When** busca por texto en Descubrir, **Then** ve resultados con portada, título y descripción y puede suscribirse desde cada resultado.
2. **Given** la persona tiene una dirección RSS válida, **When** la da de alta manualmente, **Then** el programa se resuelve, se añade a sus suscripciones y su ficha queda disponible.
3. **Given** la persona tiene un fichero OPML, **When** lo importa con el selector de ficheros del sistema, **Then** las suscripciones contenidas se añaden sin duplicar las ya existentes y se informa del resultado.
4. **Given** la persona tiene suscripciones, **When** solicita exportar, **Then** obtiene un fichero OPML con todas sus suscripciones mediante la opción de compartir/guardar del sistema.

---

### User Story 3 - Escuchar un episodio y retomar donde lo dejó (Priority: P1)

La persona usuaria reproduce un episodio desde Inicio, ficha del programa o favoritos, con controles de pausa, salto adelante/atrás, velocidad de reproducción y búsqueda en la pista. Al pausar y volver más tarde, la reproducción continúa donde la dejó. Al terminar un episodio se marca como completado y suena el siguiente de la cola si lo hay.

**Why this priority**: Es el valor central de un podcast. La reanudación y los controles de velocidad/salto son la diferencia frente a la radio en directo.

**Independent Test**: Reproducir un episodio, avanzar, pausar, salir, volver a entrar y pulsar continuar; la posición se conserva. Terminar un episodio con cola no vacía hace sonar el siguiente.

**Acceptance Scenarios**:

1. **Given** un episodio sin empezar, **When** la persona pulsa reproducir, **Then** la reproducción comienza y la fila muestra estado en reproducción con pausa disponible.
2. **Given** un episodio en reproducción, **When** la persona cambia la velocidad o salta ±15/30 segundos, **Then** el cambio se aplica de inmediato y se mantiene durante ese episodio.
3. **Given** un episodio pausado a mitad, **When** la persona vuelve más tarde y pulsa continuar, **Then** la reproducción se reanuda en la misma posición con un margen de error de pocos segundos.
4. **Given** un episodio en reproducción hasta el final, **When** termina, **Then** se marca como completado, desaparece de "continuar escuchando" y, si hay cola, comienza el siguiente episodio.
5. **Given** un episodio a medias en "continuar escuchando", **When** la persona lo descarta manualmente, **Then** desaparece de "continuar escuchando" sin marcarse como completado y sigue disponible en su programa y favoritos.

---

### User Story 4 - Gestionar la cola de reproducción (Priority: P2)

La persona usuaria añade episodios a la cola (a continuación o al final), reordena, quita episodios sueltos y vacía la cola sin detener lo que suena. Si la cola cambió en otro dispositivo, la vista se actualiza y se evita perder cambios.

**Why this priority**: La cola es el flujo diario del oyente de podcast (equivalente al historial/orden personalizado de radio) y la web la sincroniza entre dispositivos con control de revisiones.

**Independent Test**: Añadir tres episodios, mover uno a "a continuación", reordenar por arrastre, quitar uno y vaciar la cola manteniendo el episodio en curso sonando.

**Acceptance Scenarios**:

1. **Given** un episodio fuera de la cola, **When** la persona elige "A continuación", **Then** el episodio queda el siguiente tras el actual sin duplicarse.
2. **Given** un episodio ya en la cola, **When** la persona elige "A continuación" o "Al final", **Then** el episodio se mueve a esa posición en vez de duplicarse.
3. **Given** una cola con varios episodios, **When** la persona reordena o quita uno, **Then** el orden resultante se conserva y el episodio en reproducción no se ve afectado.
4. **Given** una cola con pendientes, **When** la persona pulsa "Vaciar cola", **Then** se eliminan los pendientes, el episodio en curso sigue sonando y la vista queda vacía con estado vacío explicativo.
5. **Given** la cola cambió en otro dispositivo, **When** la persona intenta guardar un reorden obsoleto, **Then** la vista muestra un aviso y recarga la cola actual sin corromperla.

---

### User Story 5 - Favoritos de episodios y ficha de detalle (Priority: P2)

La persona usuaria marca episodios como favoritos (independientes de las emisoras favoritas de radio), los consulta en su lista y abre la ficha de cualquier episodio en una hoja inferior modal (portada, programa, fecha, duración, progreso y descripción completa) sin detener la reproducción ni salir de la página.

**Why this priority**: Los favoritos de episodios son memoria personal y la ficha en ventana/modal es el patrón web que evita perder el contexto de escucha.

**Independent Test**: Marcar un episodio como favorito desde una lista, verlo en Favoritos de podcast, abrir su ficha desde el título y desmarcarlo; la radio favorita no se ve afectada.

**Acceptance Scenarios**:

1. **Given** un episodio no favorito, **When** la persona toca el corazón, **Then** queda marcado y aparece en la lista de episodios favoritos.
2. **Given** cualquier lista con episodios, **When** la persona toca el título o la tarjeta, **Then** se abre la ficha en una hoja inferior modal con detalle completo sin detener la reproducción y al cerrarse se devuelve el foco al mismo punto de la lista.
3. **Given** la ficha de un episodio en reproducción, **When** la persona pausa, encola, marca favorito o descarga desde la ficha, **Then** la acción se refleja en la lista subyacente.

---

### User Story 6 - Portada, ficha de programa y estadísticas (Priority: P3)

La persona usuaria abre Inicio de podcasts y ve "continuar escuchando", novedades de sus suscripciones, sus programas y resumen de la cola. Desde la ficha de un programa filtra episodios (sin empezar, en progreso, completados) y los ordena (recientes, duración, título). En Estadísticas ve su consumo privado (tiempo, episodios, tasa de finalización, evolución, hábitos y categorías).

**Why this priority**: Es descubrimiento y autoconocimiento; completa la paridad con la web pero no bloquea escuchar, suscribirse ni encolar.

**Independent Test**: Con varias suscripciones y escuchas, abrir Inicio y comprobar los tres bloques; abrir un programa con muchos episodios, filtrar por "en progreso" y ordenar por duración; abrir Estadísticas y ver resumen coherente.

**Acceptance Scenarios**:

1. **Given** suscripciones con episodios nuevos y uno a medias, **When** la persona abre Inicio, **Then** ve continuar escuchando, novedades y sus programas con accesos a Descubrir y Cola.
2. **Given** la ficha de un programa con muchos episodios, **When** la persona filtra por estado u ordena, **Then** la lista paginada refleja el filtro y el orden elegidos.
3. **Given** historial de escucha de podcasts, **When** la persona abre Estadísticas de podcasts, **Then** ve tiempo consumido, episodios empezados/completados, tasa de finalización y desgloses, visibles solo para su cuenta.

---

### User Story 7 - Descargar episodios cuando el servidor lo permite (Priority: P2)

La persona usuaria descarga el archivo original de un episodio a una ubicación controlada en Descargas de Android. La descarga es interna (gestionada por la propia aplicación). Si el episodio está descargado, la reproducción usa directamente el archivo local en lugar del modo online para ahorrar datos. Si el archivo ya no está en el origen o el archivo local se ha perdido, recibe un aviso claro.

**Why this priority**: Pedido explícito ("descargas") y ahorro de datos mediante reproducción local del archivo descargado (estilo Pocket Casts).

**Independent Test**: Con descargas habilitadas, descargar un episodio y comprobar el fichero recibido; con descargas deshabilitadas, comprobar que no aparece la opción.

**Acceptance Scenarios**:

1. **Given** descargas disponibles en el servidor, **When** la persona toca descargar en un episodio, **Then** la aplicación guarda internamente el archivo original en Descargas de Android con el nombre propuesto y se informa del progreso o del resultado.
2. **Given** un episodio con archivo descargado, **When** la persona lo reproduce, **Then** la reproducción usa el archivo local en lugar del modo online.
3. **Given** descargas no disponibles en el servidor, **When** la persona mira un episodio, **Then** no se muestra la opción de descargar y no hay errores.
4. **Given** un episodio cuyo archivo ya no existe en el origen o cuyo archivo local se ha perdido o está incompleto, **When** la persona intenta descargarlo o reproducirlo en local, **Then** se muestra un aviso de error accionable (reintentar descarga o reproducir online).

---

### Edge Cases

- Servidor con podcasts deshabilitados: se oculta todo el modo Podcast (selector, rutas y opciones de descarga); Radio sigue íntegro.
- Sin suscripciones: Inicio y Suscripciones muestran estado vacío con acción directa a Descubrir.
- Cola vacía o con un solo episodio en curso: el episodio en reproducción se oculta de pendientes y se controla desde el reproductor; vaciar no lo detiene.
- Cola llena (hasta 200 episodios): al intentar añadir uno más se informa del límite sin romper la cola actual.
- Conflicto de revisión de cola (cambió en otro dispositivo): se recarga la versión actual y se pide reintentar la acción.
- Episodio no disponible o stream interrumpido: se muestra motivo y botón Reintentar; no hay pantalla negra ni cierre.
- Feed RSS inválido o inaccesible al dar de alta manual: se muestra el motivo sin crear suscripción fantasma.
- OPML malformado o vacío: se informa sin importar nada a medias.
- Búsqueda sin resultados o sin texto: estado vacío con sugerencia, sin errores.
- Ficha de programa con cientos de episodios: paginación con "más" explícito, sin cargar todo de golpe.
- Título de episodio muy largo o con caracteres especiales al descargar: el nombre propuesto sigue siendo válido y único en Descargas de Android.
- Archivo local perdido, incompleto o sin espacio: se avisa y se ofrece reintentar la descarga o reproducir online; la lista nunca se bloquea.
- Sin conexión o con red intermitente: mensajes en español, reintento manual y reanudación según preferencia existente de red.
- Episodio ya completado o descartado manualmente: no reaparece en "continuar escuchando" (el descartado conserva su progreso y puede volver a escucharse desde su programa o favoritos).
- Favoritos de podcast y favoritos de radio son independientes: marcar uno no afecta al otro.
- Sesión caducada o servidor sin credenciales: se aplica el flujo existente de sesión (renovar sin pedir nada; si falla, editar servidor), sin pantallas nuevas de acceso.

## Requirements *(mandatory)*

### Functional Requirements

**Modo y navegación**

- **FR-001**: El sistema MUST mostrar un selector rápido Radio | Podcasts cuando el servidor activo ofrece podcasts, y MUST ocultar todo rastro de Podcasts cuando no los ofrece.
- **FR-002**: El sistema MUST mantener la reproducción en curso al cambiar de modo (radio ↔ podcast) sin detenerla ni reiniciarla.
- **FR-003**: El sistema MUST ofrecer en Configuración la elección de sección inicial entre Radio y Podcasts y MUST abrirse en la elegida al arrancar con sesión válida.
- **FR-004**: En modo Podcasts, el sistema MUST mostrar una barra inferior con las secciones en este orden: Inicio, Descubrir, Suscripciones, Cola y Favoritos de episodios (decisión de clarificación 2026-11-18), manteniendo el acceso a Configuración y Servidores con el mismo patrón que en Radio.
- **FR-005**: El sistema MUST resolver la pantalla inicial de Podcasts en la portada (continuar, novedades, programas y resumen de cola) y MUST redirigir rutas no disponibles (p. ej. podcasts deshabilitados) a Radio sin errores.

**Descubrir y suscripciones**

- **FR-006**: El sistema MUST permitir buscar programas por texto en el directorio configurado y MUST mostrar portada, título y descripción por resultado.
- **FR-007**: El sistema MUST permitir dar de alta manual un programa a partir de una dirección RSS válida y MUST informar del motivo si el feed es inválido o inaccesible.
- **FR-008**: El sistema MUST permitir suscribirse y darse de baja desde resultados, ficha y lista de suscripciones, y MUST reflejar el cambio en todas las vistas.
- **FR-009**: El sistema MUST listar las suscripciones propias con portada y nombre, con paginación o carga progresiva si son muchas, y MUST ofrecer estado vacío con acceso a Descubrir.
- **FR-010**: El sistema MUST permitir importar suscripciones desde un fichero OPML elegido con el selector de ficheros del sistema, sin duplicar las ya existentes e informar del número añadido, y MUST permitir exportar todas las suscripciones a OPML mediante la opción de compartir/guardar del sistema.

**Ficha de programa y episodios**

- **FR-011**: El sistema MUST mostrar la ficha de un programa con portada, descripción y lista de episodios paginada.
- **FR-012**: El sistema MUST permitir filtrar episodios por estado (sin empezar, en progreso, completados) y ordenarlos (recientes, duración, título).
- **FR-013**: El sistema MUST mostrar por episodio título, programa, fecha, duración, progreso y estado de favorito, y MUST abrir su ficha detalle en una hoja inferior modal sin detener la reproducción, devolviendo el foco al cerrarse.

**Reproducción y progreso**

- **FR-014**: El sistema MUST reproducir/pausar episodios desde listas, ficha y reproductor persistente, con controles de salto adelante/atrás (±15/30 s), velocidad (al menos 1x–2x) y búsqueda en la pista.
- **FR-015**: El sistema MUST guardar el progreso de escucha y MUST reanudar en la misma posición al volver (continuar escuchando), con precisión de pocos segundos.
- **FR-016**: El sistema MUST marcar un episodio como completado al terminarlo, retirarlo de pendientes/continuar y, si hay cola, comenzar automáticamente el siguiente; además MUST permitir descartar manualmente un episodio de "continuar escuchando" sin marcarlo como completado.
- **FR-017**: El sistema MUST mostrar la ficha del episodio en curso desde el reproductor y MUST permitir desde ella reproducir/pausar, reintentar, encolar, marcar favorito y descargar (si disponible).
- **FR-018**: Ante corte o error de reproducción, el sistema MUST mostrar motivo en español con acción Reintentar y MUST reintentar con esperas crecientes cuando el corte es transitorio.

**Cola**

- **FR-019**: El sistema MUST permitir añadir un episodio a la cola al final o a continuación, moviéndolo si ya estaba en vez de duplicarlo, hasta el límite del servidor (200 episodios).
- **FR-020**: El sistema MUST ocultar el episodio en reproducción de la lista de pendientes, controlarlo desde el reproductor y devolverlo a su posición al pausarlo, detenerlo o cambiar de episodio.
- **FR-021**: El sistema MUST permitir reordenar la cola, quitar episodios sueltos y vaciar los pendientes sin detener el episodio en curso, conservando opcionalmente el episodio indicado.
- **FR-022**: Ante cambio concurrente de la cola en otro dispositivo, el sistema MUST detectar el conflicto de revisión, recargar la versión actual e informar para reintentar, sin perder la reproducción en curso.

**Favoritos de episodios**

- **FR-023**: El sistema MUST permitir marcar/desmarcar episodios como favoritos desde filas, ficha y cola, MUST listarlos aparte y MUST mantenerlos independientes de las emisoras favoritas de radio.

**Descargas**

- **FR-024**: Cuando el servidor publica descargas disponibles, el sistema MUST descargar internamente el archivo original a una ubicación controlada en Descargas de Android con el nombre propuesto por el servidor, MUST indicar progreso/resultado y estado descargado por episodio, y MUST reproducir el archivo local cuando exista en lugar del modo online.
- **FR-025**: Si el archivo ya no está disponible en el origen, el archivo local se ha perdido o está incompleto, o no hay espacio suficiente, el sistema MUST mostrar un aviso de error sin crear ficheros corruptos ni bloquear la lista, ofreciendo reintentar la descarga o reproducir online.

**Estadísticas de podcast**

- **FR-026**: El sistema MUST mostrar estadísticas privadas de podcast (tiempo consumido, programas distintos, episodios empezados/completados, tasa de finalización, media por episodio, evolución temporal, hábitos y reparto por categoría, sesiones recientes) solo para la cuenta activa.

**Errores, sesión y preferencias existentes**

- **FR-027**: Todos los mensajes de error visibles MUST estar en español, ser accionables y MUST reutilizar el flujo existente de sesión (renovar automáticamente; si fallan las credenciales, llevar a editar el servidor activo) sin pantallas nuevas de acceso.
- **FR-028**: El sistema MUST respetar las preferencias existentes aplicables (pantalla de arranque cuando no hay predeterminado de modo, reanudación/pausa al recuperar red, temporizador de apagado y emisión a otros dispositivos) también en modo Podcasts.

### Key Entities *(include feature involves data)*

- **Modo de contenido**: Radio o Podcasts; determina qué secciones muestra la barra inferior sin detener la reproducción.
- **Programa (podcast)**: programa con portada, título, descripción, categorías y dirección del feed; unidad de suscripción.
- **Episodio**: entrega con programa, título, fecha, duración, posición/temporada, marca explícita, progreso y estado (sin empezar / en progreso / completado), favorito y disponibilidad.
- **Suscripción**: relación cuenta↔programa; base de novedades, portada y exportación OPML.
- **Cola de reproducción**: lista ordenada de hasta 200 episodios con número de revisión para sincronizar entre dispositivos; distingue episodio en curso de pendientes.
- **Favorito de episodio**: marca personal sobre episodios, independiente de emisoras favoritas.
- **Progreso de escucha**: posición guardada por episodio y sesiones que alimentan "continuar escuchando" y estadísticas.
- **Descarga**: archivo original del episodio guardado internamente por la aplicación en Descargas de Android (ubicación controlada); si existe y está completo, la reproducción usa el archivo local en lugar del modo online.
- **Búsqueda de podcast**: consulta por texto al directorio más alta manual por RSS e importación OPML.
- **Estadística de podcast**: agregados privados (tiempo, conteos, tasa, evolución, hábitos, categorías, sesiones recientes).
- **Sección inicial**: preferencia Radio o Podcasts que determina la pantalla de arranque.
- **Capacidad del servidor**: indica si ofrece podcasts y si permite descargas; gobierna visibilidad de todo el modo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: La persona usuaria cambia de Radio a Podcasts (o viceversa) en un máximo de 2 toques sin detener una reproducción en curso.
- **SC-002**: Una persona nueva es capaz de buscar un programa, suscribirse y reproducir su primer episodio en menos de 3 minutos con conexión normal.
- **SC-003**: 9 de cada 10 reanudaciones de un episodio a medias continúan en la misma posición con desviación máxima de 10 segundos.
- **SC-004**: 9 de cada 10 operaciones de cola (añadir a continuación/al final, mover, quitar, vaciar) se reflejan correctamente al primer intento sin duplicados ni pérdida del episodio en curso.
- **SC-005**: La portada de podcasts (continuar, novedades, programas, resumen de cola) se presenta completa en menos de 3 segundos con conexión normal y suscripciones típicas (hasta 50 programas).
- **SC-006**: 9 de cada 10 búsquedas con texto muestran resultados o estado vacío explicativo en menos de 2 segundos con conexión normal.
- **SC-007**: Al terminar un episodio con cola no vacía, el siguiente comienza sin acción manual en al menos 9 de cada 10 casos.
- **SC-008**: Al menos el 90% de las personas que prueban el modo completan una escucha (reproducir + pausa o finalización) sin pedir ayuda, medido en pruebas de usabilidad con 10 participantes.
- **SC-009**: Con podcasts deshabilitados en el servidor, el 100% de las sesiones de Radio funcionan sin mostrar opciones de podcast ni errores.
- **SC-010**: 9 de cada 10 reproducciones de episodios con archivo descargado completo se sirven del archivo local sin usar datos de streaming.

## Assumptions

- Se busca paridad funcional con la web descrita en `docs/uso.md` y `docs/api.md` del servicio (portada, descubrir, suscripciones, ficha, cola con revisión, favoritos de episodios, progreso y estadísticas privadas), con la diferencia explícita de que las descargas son internas con reproducción local (ver supuestos de descargas).
- La barra inferior de Podcasts contendrá Inicio, Descubrir, Suscripciones, Cola y Favoritos de episodios; las estadísticas de podcast vivirán accesibles desde Inicio y no ocuparán hueco fijo inferior por espacio, igual que en Radio las estadísticas no están en la barra.
- Las descargas son internas: la aplicación guarda el archivo original en una ubicación controlada en Descargas de Android con el nombre propuesto por el servidor y reproduce el archivo local cuando existe y está completo; la gestión avanzada de biblioteca sin conexión (descargas automáticas, caducidad, limpieza programada) sería una spec aparte.
- La autenticación, la gestión de servidores y las preferencias existentes (pantalla de arranque, reanudación con red, temporizador, emisión) se reutilizan tal cual; no hay pantallas nuevas de acceso.
- El contenido de podcast exige al menos un servidor con sesión válida y podcasts habilitados; sin servidores se aplica el flujo existente de bienvenida/formulario.
- El límite de cola (200), el conflicto por revisión obsoleta, los filtros de ficha (sin empezar/en progreso/completados), los órdenes (recientes/duración/título) y la velocidad (1x–2x) y saltos (±15/30 s) replican el comportamiento web salvo que la planificación demuestre lo contrario.
- Los favoritos de episodios y las emisoras favoritas son colecciones independientes; el historial de radio no mezcla escuchas de podcast (las estadísticas de podcast son su propio desglose).
- Rendimiento objetivo con conexión normal y paginación progresiva; no se carga nunca el catálogo completo de golpe.

## Dependencies

- Servidor compatible con el contrato de podcast del servicio (suscripciones, episodios, cola con revisión, favoritos de episodios, progreso, descargas y estadísticas) y con la bandera pública que indica si ofrece podcasts y descargas.
- Sesión por servidor existente con renovación automática; todo el contenido de podcast requiere cuenta activa.
- Acceso a red para directorio, feeds, audio y descargas; la descarga la gestiona internamente la aplicación y la reproducción prioriza el archivo local cuando existe y está completo.

## Out of Scope

- Gestión avanzada de biblioteca sin conexión: descargas automáticas por suscripción, caducidad y limpieza programada (más allá de descargar, conservar y reproducir el archivo local).
- Subida o edición de feeds RSS, gestión de programas propios y edición de metadatos.
- Notificaciones de nuevos episodios y descargas automáticas por suscripción.
- Pantallas nuevas de acceso/registro/recuperación (la sesión sigue configurándose por servidor).
- Cambios en radio (explorar, favoritas, historial, mis emisoras) más allá de añadir el selector de modo y la sección inicial.
