# Feature Specification: Refinamientos de experiencia podcast

**Feature Branch**: `0048-izquierdojl-20261119-podcast-ux-refinements`

**Created**: 2026-11-19

**Status**: Done (2026-11-20; todo verificado en emulador; gates y Compose tests en API 37 verdes)

**Input**: User description: "Vamos a poner mas mejoras de podcast relacionadas con la ultima especificación incorporada, son las siguientes:
En la cola, se debería ver , en la parte de arrriba el episodio que se está reproduciendo y el tiempo, y debajo el resto.
En la cola, en cada episodio de debería ver la duración, y si ha comenzado, los minutos restantes en una pequeña fila
En la pantalla de detalle del episodio, el botón detener debería ser un dibujo como los demás, y hay que añadir un botón de favoritos.
En la tarjeta de reproducción inferior, de debe ver el tiempo del capítulo y el tiemporestante.
En configuración, al igual que hay una opción para decidir cual es la pantalla inicial en radio, también debería estar para podcast
En gráficas, deben mostrarse las de radio o podcast en función de como está seleccionado. "

## Clarifications

### Session 2026-11-19

- Q: ¿Qué debe mostrar la pantalla de Gráficas cuando el modo activo es Podcasts? → A: Paridad visual con radio: los mismos tipos de gráfica (resumen, evolución, hábitos, categorías y rankings) con datos de podcast.
- Q: ¿Qué cifras debe mostrar la tarjeta de reproducción inferior? → A: Tiempo transcurrido y tiempo restante; la duración total queda en el reproductor completo.
- Q: ¿Qué tiempo debe mostrar el bloque superior de la cola? → A: Tiempo transcurrido y duración total con barra de avance; el restante se reserva para las filas y la tarjeta inferior.
- Q: ¿Qué destinos debe ofrecer la pantalla de arranque de podcast? → A: Los cinco: Inicio, Descubrir, Suscripciones, Cola y Favoritos.
- Q: ¿Cómo debe decidirse qué conjunto de gráficas se muestra? → A: Siguiendo el selector global Radio | Podcasts de la cabecera; sin selector propio ni pestañas dentro de Gráficas.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - La cola muestra arriba el episodio en curso y su tiempo (Priority: P1)

La persona usuaria está escuchando un episodio y abre la cola. En la parte superior ve claramente el episodio que suena, con su portada, título, programa, tiempo transcurrido, duración total y avance. Debajo aparecen los episodios pendientes. Ya no tiene que leer una línea de texto suelta ni preguntarse cuál de los episodios de la lista está sonando.

**Why this priority**: Es la mejora que más cambia la utilidad de la cola. Hoy el episodio en curso se oculta de la lista y solo queda una línea "Suena: …" sin tiempo ni contexto. Sin esta cabecera, la cola no responde a la pregunta básica "¿qué está sonando y por dónde va?" y el resto de mejoras de la pantalla pierden sentido.

**Independent Test**: Reproducir un episodio con una cola de al menos tres pendientes, abrir la cola y comprobar que el episodio en curso aparece en un bloque superior con portada, título, programa, tiempo transcurrido y duración total; comprobar que los pendientes aparecen debajo y que el episodio en curso no se repite en la lista.

**Acceptance Scenarios**:

1. **Given** un episodio de podcast reproduciéndose y una cola con pendientes, **When** la persona abre la cola, **Then** ve en la parte superior un bloque con la portada, el título, el programa, el tiempo transcurrido, la duración total y el avance de ese episodio, y debajo los episodios pendientes; al desplazar la lista, el bloque permanece visible.
2. **Given** un episodio pausado que sigue siendo el episodio en curso, **When** la persona abre la cola, **Then** el bloque superior sigue visible y muestra la última posición alcanzada.
3. **Given** un episodio en curso que también forma parte de la cola, **When** la persona abre la cola, **Then** aparece una sola vez, en el bloque superior, y no se duplica entre los pendientes.
4. **Given** un episodio en curso que no forma parte de la cola, **When** la persona abre la cola, **Then** el bloque superior lo muestra igualmente y los pendientes no cambian.
5. **Given** el bloque superior visible, **When** la reproducción avanza, la persona pausa o salta de posición, **Then** el tiempo y el avance del bloque se actualizan en un máximo de 2 segundos y coinciden con el reproductor.
6. **Given** que no hay ningún episodio en curso, **When** la persona abre la cola, **Then** no aparece bloque superior y los pendientes ocupan el inicio de la pantalla.
7. **Given** que hay un episodio en curso pero ningún pendiente, **When** la persona abre la cola, **Then** ve el bloque superior y un mensaje claro de que no hay más episodios en cola.
8. **Given** el bloque superior, **When** la persona lo toca, **Then** se abre el reproductor completo del episodio sin detener, reiniciar ni alterar la reproducción.

---

### User Story 2 - Detalle del episodio con detener gráfico y favorito (Priority: P1)

En el reproductor completo del episodio, la acción de detener deja de ser un texto y pasa a ser un icono integrado con el resto de controles. Además, la persona puede marcar o desmarcar el episodio como favorito desde ese mismo detalle, con un estado visible y sin interrumpir la escucha.

**Why this priority**: El texto "Detener" rompe la coherencia visual del reproductor y ocupa espacio que necesita el resto de acciones. La ausencia de favorito en el detalle obliga a salir a la ficha o a una lista para una acción básica. Ambas correcciones son pequeñas, muy visibles y completan la paridad con el resto de la app.

**Independent Test**: Abrir el reproductor completo de un episodio, comprobar que detener es un icono con etiqueta accesible y que existe un control de favorito; marcarlo, comprobar que el estado cambia sin pausar y que se refleja al abrir la ficha y la lista de favoritos; reabrir el detalle y comprobar que el estado persiste.

**Acceptance Scenarios**:

1. **Given** el reproductor completo de un episodio, **When** la persona mira la acción de detener, **Then** esta es un icono sin texto visible, con el mismo tamaño y peso visual que los demás controles del reproductor, y con la etiqueta accesible "Detener".
2. **Given** el reproductor completo, **When** la persona pulsa detener, **Then** la reproducción se detiene y el detalle se cierra, igual que antes.
3. **Given** el reproductor completo de un episodio no favorito, **When** la persona pulsa el control de favorito, **Then** el episodio queda marcado como favorito, el estado visual cambia de inmediato y la reproducción no se pausa, no se reinicia y el detalle no se cierra.
4. **Given** el reproductor completo de un episodio favorito, **When** la persona vuelve a pulsar el control de favorito, **Then** el episodio deja de ser favorito y el cambio se refleja en el resto de vistas.
5. **Given** un episodio marcado como favorito desde el detalle, **When** la persona abre la ficha del episodio o la lista de favoritos, **Then** el estado aparece actualizado sin necesidad de recargar manualmente.
6. **Given** el detalle abierto de un episodio cuyo favorito cambió en otra vista, **When** el detalle se vuelve a mostrar, **Then** el control refleja el estado más reciente conocido.
7. **Given** un fallo al guardar el favorito, **When** termina la operación, **Then** el control conserva el estado anterior, se informa en español con una acción de reintento y no se muestran detalles técnicos.

---

### User Story 3 - Duración y minutos restantes en cada episodio de la cola (Priority: P2)

En cada fila de la cola, la persona usuaria ve la duración del episodio y, si ya lo ha empezado, una pequeña línea con los minutos que le quedan. Así puede decidir qué escuchar a continuación sin abrir cada ficha ni calcular mentalmente el tiempo restante.

**Why this priority**: La cola es una herramienta de planificación. Sin duración ni restante, elegir el siguiente episodio es a ciegas. Es una mejora de información, no bloquea la reproducción, y depende de datos que ya existen.

**Independent Test**: Abrir una cola con un episodio sin empezar, otro a medias y otro completado; comprobar que el primero muestra solo su duración, el segundo muestra duración y minutos restantes en una línea secundaria, y el tercero no muestra restante.

**Acceptance Scenarios**:

1. **Given** un episodio pendiente con duración conocida, **When** la persona mira su fila en la cola, **Then** ve la duración total.
2. **Given** un episodio pendiente empezado pero no completado, **When** la persona mira su fila, **Then** ve la duración y, en una línea secundaria, los minutos restantes calculados desde la posición guardada.
3. **Given** un episodio completado, **When** la persona mira su fila, **Then** ve su duración y no se muestra una línea de minutos restantes.
4. **Given** un episodio sin empezar, **When** la persona mira su fila, **Then** ve su duración y no se muestra una línea de minutos restantes.
5. **Given** un episodio al que le queda menos de un minuto, **When** la persona mira su fila, **Then** la línea secundaria indica "Queda menos de 1 min" en lugar de un cero o un valor negativo.
6. **Given** un episodio con duración desconocida, **When** la persona mira su fila, **Then** no se muestra una duración ni un restante inventados.
7. **Given** un episodio cuyo progreso cambia mientras la cola está abierta, **When** la persona vuelve a la cola o la actualiza, **Then** la duración y el restante reflejan el progreso más reciente.
8. **Given** el tamaño de letra del sistema aumentado, **When** la persona mira las filas, **Then** la línea de duración y restante se lee completa, no se solapa con el título ni con las acciones y la fila conserva un tamaño táctil utilizable.

---

### User Story 4 - Tiempo transcurrido y restante en la tarjeta de reproducción inferior (Priority: P2)

Con cualquier pantalla abierta, la tarjeta de reproducción inferior muestra el tiempo transcurrido del episodio y el tiempo que queda. La persona sabe cuánto lleva y cuánto falta sin abrir el reproductor completo.

**Why this priority**: La tarjeta inferior es la referencia permanente de reproducción. Hoy solo tiene una barra de avance sin cifras, por lo que no permite decidir si da tiempo a terminar el episodio. La mejora es visible en toda la app y de bajo coste.

**Independent Test**: Reproducir un episodio de 45 minutos, comprobar que la tarjeta muestra el tiempo transcurrido y el restante; avanzar 30 segundos y comprobar que ambos cambian; pausar y comprobar que se mantienen.

**Acceptance Scenarios**:

1. **Given** un episodio en reproducción, **When** la persona mira la tarjeta inferior, **Then** ve el tiempo transcurrido y el tiempo restante del episodio.
2. **Given** el episodio avanzando, **When** pasa el tiempo, **Then** ambas cifras se actualizan al menos una vez por segundo y coinciden con el reproductor.
3. **Given** la reproducción en pausa, **When** la persona mira la tarjeta, **Then** las cifras se mantienen en la última posición y no siguen avanzando.
4. **Given** la persona salta hacia atrás o hacia delante, **When** el salto termina, **Then** ambas cifras reflejan la nueva posición de inmediato.
5. **Given** el episodio termina y comienza el siguiente de la cola, **When** la tarjeta se actualiza, **Then** las cifras se reinician para el nuevo episodio y no arrastran las del anterior.
6. **Given** una duración desconocida o cero, **When** la persona mira la tarjeta, **Then** se muestra solo el tiempo transcurrido y no se inventa un restante.
7. **Given** un episodio de más de una hora, **When** la persona mira las cifras, **Then** el formato incluye las horas de forma legible y el restante nunca es negativo.
8. **Given** el tamaño de letra del sistema aumentado, **When** la persona mira la tarjeta, **Then** las cifras se leen sin solaparse con el título, la portada ni los controles, y la tarjeta sigue abriendo el reproductor completo.

---

### User Story 5 - Pantalla de arranque propia para podcast (Priority: P2)

En Configuración, además de elegir en qué pantalla de radio arranca la app, la persona puede elegir en qué pantalla de podcast arranca cuando la sección inicial es Podcasts: Inicio, Descubrir, Suscripciones, Cola o Favoritos.

**Why this priority**: Quien escucha podcast a diario suele querer arrancar en Suscripciones o en Cola, no siempre en Inicio. La preferencia de radio ya existe y la app ya recuerda la sección inicial Radio | Podcasts; falta la mitad simétrica de esa configuración. No bloquea la escucha, pero elimina un toque diario.

**Independent Test**: En Configuración, elegir "Cola" como pantalla de arranque de podcast, cerrar y reabrir la app con la sección inicial Podcasts y comprobar que abre directamente la cola; repetir con "Suscripciones" y comprobar que la preferencia de radio no cambia.

**Acceptance Scenarios**:

1. **Given** Configuración, **When** la persona busca la pantalla de arranque, **Then** encuentra una opción para radio y otra para podcast, claramente diferenciadas.
2. **Given** la opción de pantalla de arranque de podcast, **When** la persona la despliega, **Then** puede elegir Inicio, Descubrir, Suscripciones, Cola y Favoritos.
3. **Given** que la persona nunca ha cambiado esta preferencia, **When** arranca la app en modo Podcasts, **Then** la pantalla de arranque de podcast es Inicio.
4. **Given** la preferencia de podcast en Suscripciones y la sección inicial Podcasts, **When** la app arranca con un servidor que ofrece podcasts, **Then** abre Suscripciones.
5. **Given** la preferencia de podcast elegida, **When** la app se reinicia, cambia el tema, gira la pantalla o se cambia de servidor, **Then** la preferencia se conserva.
6. **Given** un servidor que no ofrece podcasts, **When** la app arranca aunque la sección inicial guardada sea Podcasts, **Then** abre la pantalla de arranque de radio sin mostrar errores y sin ofrecer el modo Podcasts.
7. **Given** la preferencia de podcast en Cola y una cola vacía, **When** la app arranca en modo Podcasts, **Then** abre la pantalla de Cola con su estado vacío habitual, no otra pantalla.
8. **Given** que la persona cambia la pantalla de arranque de podcast, **When** vuelve a la app sin reiniciarla, **Then** el cambio no la lleva de golpe a otra pantalla; se aplica en el siguiente arranque.
9. **Given** la preferencia de radio, **When** la persona cambia la de podcast o viceversa, **Then** la otra preferencia conserva su valor.

---

### User Story 6 - Gráficas de radio o podcast según el modo activo (Priority: P3)

Al abrir "Gráficas" desde Configuración, la persona ve las gráficas de radio si el selector está en Radio y las de podcast si está en Podcasts. La pantalla indica claramente de qué contenido son los datos y no mezcla ambas fuentes. La vista de podcast usa el mismo lenguaje visual de gráficas que la de radio, no una lista de texto.

**Why this priority**: Hoy la entrada de Gráficas siempre muestra el conjunto de radio, mientras que las estadísticas de podcast existen pero no se alcanzan desde ahí. Corregir la selección evita datos engañosos. Es la mejora de menor frecuencia de uso, por eso queda en último lugar.

**Independent Test**: Con historial de radio y de podcast, abrir Gráficas en modo Radio y comprobar que aparecen las de radio; cambiar el selector a Podcasts, abrir Gráficas y comprobar que aparecen las de podcast, con un título que lo indique, con gráficas equivalentes a las de radio y sin datos de radio.

**Acceptance Scenarios**:

1. **Given** el modo activo Radio, **When** la persona abre Gráficas desde Configuración, **Then** se muestran las gráficas de radio.
2. **Given** el modo activo Podcasts, **When** la persona abre Gráficas, **Then** se muestran las estadísticas de podcast.
3. **Given** cualquiera de las dos vistas, **When** la persona mira el encabezado, **Then** identifica sin ambigüedad si son datos de radio o de podcast.
4. **Given** una vista de Gráficas abierta, **When** la persona cambia el modo activo, **Then** no queda visible contenido del modo anterior ni se mezclan ambos conjuntos.
5. **Given** un servidor sin podcasts, **When** la persona abre Gráficas, **Then** se muestran las de radio y no se ofrece una vista de podcast vacía o rota.
6. **Given** el modo Podcasts sin historial de escucha, **When** la persona abre Gráficas, **Then** ve un estado vacío explicativo propio de podcast, no gráficas de radio ni cifras inventadas.
7. **Given** un fallo al cargar las estadísticas del modo activo, **When** la pantalla termina de cargar, **Then** se muestra un error en español con reintento y no se sustituye por los datos del otro modo.
8. **Given** el modo Radio, **When** la persona usa el filtro de periodo, **Then** este sigue funcionando sobre las gráficas de radio.
9. **Given** la vista de Gráficas en modo Podcasts con datos, **When** la persona la mira, **Then** ve resumen, evolución temporal, hábitos, categorías y rankings de podcast con el mismo lenguaje visual de gráficas que radio, no una lista de texto.

---

### Edge Cases

- Episodio en curso que no pertenece a la cola: se muestra igualmente en el bloque superior, sin alterar los pendientes.
- Episodio en curso que sí pertenece a la cola: no se duplica en la lista de pendientes y la cuenta de pendientes sigue siendo coherente con lo que se ve.
- Cola sin pendientes y con episodio en curso: bloque superior más mensaje de cola sin más episodios; no se muestra el estado vacío como si no hubiera reproducción.
- Cola sin pendientes y sin episodio en curso: estado vacío habitual, sin bloque superior.
- Radio en directo sonando en modo Podcasts: no se muestra como episodio en curso de la cola.
- Episodio con duración desconocida, cero o negativa: se evita cualquier duración o restante inventado; el tiempo transcurrido se muestra si es válido.
- Posición guardada negativa o mayor que la duración: el restante se limita a cero y nunca se muestra un valor negativo; si el episodio figura como completado, se trata como completado.
- Restante inferior a un minuto: se indica "Queda menos de 1 min" y no "0 min".
- Episodios de varias horas: el formato conserva horas, minutos y segundos de forma legible.
- Cambio de episodio, pausa, reanudación, salto o cambio de velocidad mientras se muestra la cola o la tarjeta inferior: los tiempos se actualizan sin desfases ni saltos hacia atrás.
- Detalle del episodio abierto cuando el favorito se cambia desde otra vista: al volver, el control muestra el estado actualizado.
- Favorito fallido por red o sesión: se conserva el estado anterior, se informa en español y se puede reintentar; la reproducción continúa.
- Detener cuando la reproducción ya terminó o el episodio ya no está disponible: el detalle se cierra sin errores visibles.
- Detener con la tarjeta inferior visible: el episodio se limpia y la tarjeta desaparece; no queda un botón de reproducir sin efecto.
- Preferencia de arranque de podcast guardada y servidor cambiado a uno sin podcasts: se usa el arranque de radio sin bloqueo ni error.
- Preferencia de arranque de podcast guardada y sección inicial Radio: no se aplica hasta que la sección inicial sea Podcasts.
- Cambio de modo activo con Gráficas abierta: no se muestran datos del modo anterior ni una mezcla de ambos.
- Estadísticas del modo activo sin datos, con error de red o con error de credenciales: estados diferenciados, mensajes en español y reintento.
- Estadísticas de podcast con historial escaso: las gráficas se dibujan con los datos disponibles o se muestra el estado vacío; no se inventan series ni cifras.
- Servidor antiguo sin estadísticas de podcast: error accionable, sin caer silenciosamente en las gráficas de radio.
- Tamaño de letra del sistema al 200 %, pantallas estrechas y rotación: tiempos, metadatos, controles y encabezados se leen sin solaparse.

## Requirements *(mandatory)*

### Functional Requirements

**Cola — episodio en curso**

- **FR-001**: Cuando haya un episodio de podcast en curso, en reproducción o pausado, la pantalla de Cola MUST mostrar un bloque superior con portada, título, programa cuando se conozca, tiempo transcurrido, duración total cuando se conozca y avance de reproducción.
- **FR-002**: El bloque superior MUST ser el primer elemento de contenido de la pantalla, por debajo de la cabecera, y MUST permanecer fijo sobre la lista de pendientes, sin desplazarse con ella, de modo que la persona siempre vea qué está escuchando.
- **FR-003**: El tiempo transcurrido y el avance del bloque superior MUST mantenerse sincronizados con el reproductor con una desviación máxima de 2 segundos mientras la reproducción avanza, y MUST reflejar de inmediato pausas, reanudaciones y saltos de posición.
- **FR-004**: El bloque superior MUST mostrarse aunque el episodio en curso no forme parte de la cola; si forma parte de ella, MUST NOT duplicarse entre los pendientes.
- **FR-005**: Cuando no haya episodio en curso, la pantalla de Cola MUST NOT mostrar el bloque superior y los pendientes MUST ocupar el inicio de la pantalla.
- **FR-006**: Cuando haya episodio en curso y ningún pendiente, la pantalla MUST mostrar el bloque superior y un mensaje explícito de que no hay más episodios en cola.
- **FR-007**: El bloque superior MUST NOT ofrecer acciones de reordenar ni quitar de la cola, porque representa la reproducción en curso y no un pendiente.
- **FR-008**: Al tocar el bloque superior, MUST abrirse el reproductor completo del episodio sin detener, reiniciar ni alterar la reproducción.
- **FR-009**: El bloque superior MUST mostrar únicamente contenido de podcast; una emisora de radio en directo MUST NOT representarse como episodio en curso de la cola.
- **FR-010**: Si la duración del episodio en curso no se conoce, el bloque MUST mostrar el tiempo transcurrido y MUST NOT inventar una duración total ni un porcentaje de avance.

**Cola — duración y tiempo restante por episodio**

- **FR-011**: Cada episodio pendiente de la cola MUST mostrar su duración total cuando esta se conozca.
- **FR-012**: Cuando un episodio pendiente tenga progreso mayor que cero, menor que su duración y no esté completado, su fila MUST mostrar en una línea secundaria los minutos restantes.
- **FR-013**: El tiempo restante MUST calcularse como duración menos posición guardada, redondearse hacia arriba al minuto entero y MUST NOT ser negativo.
- **FR-014**: Si el restante es inferior a un minuto pero mayor que cero, la fila MUST indicar "Queda menos de 1 min" en lugar de "0 min".
- **FR-015**: Un episodio sin empezar o completado MUST mostrar solo su duración, sin línea de minutos restantes.
- **FR-016**: Si la duración es desconocida, la fila MUST NOT mostrar duración ni minutos restantes inventados.
- **FR-017**: La duración y el restante MUST actualizarse con el progreso más reciente al volver a la cola, al actualizarla o tras un cambio de progreso, sin exigir reiniciar la app.
- **FR-018**: La línea de duración y restante MUST leerse completa con el tamaño de letra del sistema hasta el 200 %, sin solaparse con el título, la portada ni las acciones.
- **FR-019**: La duración y el restante MUST tener una descripción accesible comprensible en español que no dependa solo de abreviaturas o símbolos.

**Detalle del episodio — detener y favorito**

- **FR-020**: La acción de detener del reproductor completo MUST presentarse como un icono sin texto visible, con el mismo tamaño y peso visual que los demás controles del reproductor.
- **FR-021**: La acción de detener MUST conservar el comportamiento actual: al pulsarla, la reproducción se detiene y el detalle se cierra.
- **FR-022**: El icono de detener MUST tener la etiqueta accesible "Detener" y un área táctil conforme al estándar de la app.
- **FR-023**: El reproductor completo MUST ofrecer un control de favorito con icono de corazón lleno cuando el episodio está marcado y de contorno cuando no, y con una etiqueta accesible que indique la acción y el estado.
- **FR-024**: Activar o desactivar el favorito desde el detalle MUST NOT pausar, reiniciar ni cerrar la reproducción.
- **FR-025**: El cambio de favorito MUST guardarse para la cuenta activa y MUST reflejarse en la ficha, las tarjetas, las listas y la pantalla de favoritos sin recarga manual y en un máximo de 2 segundos desde que la operación se confirma; si la red es lenta, el estado se actualiza al confirmarse sin mostrar un valor contradictorio.
- **FR-026**: Al abrir el detalle, el control de favorito MUST mostrar el estado más reciente conocido del episodio.
- **FR-027**: Si el cambio de favorito falla, el sistema MUST conservar el estado anterior, MUST informar en español con una acción de reintento y MUST NOT mostrar detalles técnicos ni datos personales.
- **FR-028**: El control de favorito MUST tener una etiqueta accesible que describa tanto la acción como el estado actual.

**Tarjeta de reproducción inferior**

- **FR-029**: La tarjeta de reproducción inferior MUST mostrar el tiempo transcurrido y el tiempo restante del episodio en curso, sin sustituir el tiempo transcurrido por la duración total.
- **FR-030**: Ambas cifras MUST actualizarse al menos una vez por segundo mientras la reproducción avanza y MUST quedar fijas al pausar.
- **FR-031**: Las cifras MUST reflejar de inmediato los saltos hacia atrás y hacia delante, los cambios de posición y el cambio de episodio.
- **FR-032**: El tiempo restante MUST calcularse como duración menos posición, limitarse entre cero y la duración y MUST NOT mostrarse nunca como un valor negativo.
- **FR-033**: El formato MUST ser compacto y coherente: minutos y segundos por debajo de una hora, y horas, minutos y segundos a partir de una hora.
- **FR-034**: Si la duración es desconocida o cero, la tarjeta MUST mostrar solo el tiempo transcurrido y MUST NOT inventar un restante.
- **FR-035**: Al detener la reproducción o al no haber episodio, la tarjeta MUST desaparecer, sin dejar cifras residuales.
- **FR-036**: Las cifras MUST tener una descripción accesible en español que incluya transcurrido, total cuando se conozca y restante.
- **FR-037**: Las cifras MUST leerse sin solaparse con la portada, el título ni los controles con el tamaño de letra del sistema hasta el 200 %, y la tarjeta MUST seguir abriendo el reproductor completo al tocarla.

**Configuración — pantalla de arranque de podcast**

- **FR-038**: Configuración MUST ofrecer una preferencia de pantalla de arranque de podcast con las opciones Inicio, Descubrir, Suscripciones, Cola y Favoritos.
- **FR-039**: La preferencia de pantalla de arranque de podcast MUST tener Inicio como valor por defecto para quien nunca la haya cambiado.
- **FR-040**: La preferencia MUST guardarse localmente y conservarse entre reinicios, cambios de tema, rotaciones, actualizaciones y cambios de servidor.
- **FR-041**: Cuando la sección inicial sea Podcasts y el servidor ofrezca podcasts, el arranque de la app MUST abrir la pantalla de podcast elegida.
- **FR-042**: Si el servidor no ofrece podcasts, el arranque MUST usar la pantalla de arranque de radio sin errores visibles, aunque la sección inicial guardada sea Podcasts.
- **FR-043**: La pantalla de arranque de radio MUST mantenerse con sus opciones y su comportamiento actual, y MUST ser independiente de la de podcast.
- **FR-044**: Cambiar una de las dos preferencias de arranque MUST NOT alterar el valor de la otra.
- **FR-045**: Un cambio en la preferencia de arranque de podcast MUST aplicarse en el siguiente arranque y MUST NOT provocar una navegación abrupta en la sesión actual.
- **FR-046**: Si la pantalla elegida no tiene contenido, el arranque MUST abrirla igualmente con su estado vacío habitual, sin sustituirla por otra.
- **FR-047**: Las dos preferencias de arranque MUST estar etiquetadas de forma que se distinga claramente radio de podcast, y MUST ser accesibles con lector de pantalla.
- **FR-048**: La preferencia de podcast MUST seguir visible aunque el servidor activo no ofrezca podcasts; en ese caso simplemente no tendrá efecto en el arranque.

**Gráficas según el modo activo**

- **FR-049**: La acción "Gráficas" de Configuración MUST abrir el conjunto de estadísticas correspondiente al modo activo: radio en modo Radio y podcast en modo Podcasts; MUST NOT añadir un selector propio de Radio | Podcasts dentro de la pantalla ni mostrar ambos conjuntos a la vez.
- **FR-050**: La pantalla de Gráficas MUST identificar en su encabezado si los datos son de radio o de podcast.
- **FR-051**: Una vista de Gráficas MUST NOT mezclar ni presentar como propios datos del otro modo.
- **FR-052**: Si el modo activo cambia, la siguiente apertura de Gráficas MUST mostrar el conjunto del nuevo modo; si la pantalla está abierta durante el cambio, MUST NOT dejar visible contenido obsoleto del modo anterior.
- **FR-053**: Si el servidor no ofrece podcasts, Gráficas MUST mostrar el conjunto de radio y MUST NOT ofrecer una vista de podcast vacía o defectuosa.
- **FR-054**: La vista de podcast MUST presentar sus datos con el mismo lenguaje de gráficas que la de radio: resumen, evolución temporal, hábitos, categorías, programas y episodios más escuchados y sesiones recientes, y MUST NOT limitarse a listas de texto ni mostrar gráficas de radio como sustituto.
- **FR-055**: La vista de podcast MUST ofrecer estados de carga, vacío explicativo y error con reintento, con mensajes en español y sin cifras inventadas.
- **FR-056**: La vista de radio MUST conservar su filtro de periodo y su comportamiento actual, y el filtro MUST afectar solo a los datos de radio.
- **FR-057**: Si falla la carga del conjunto del modo activo, el sistema MUST mostrar un error accionable y MUST NOT sustituirlo silenciosamente por el conjunto del otro modo.
- **FR-058**: Ambas vistas MUST mantener la privacidad existente: solo la cuenta activa puede ver sus datos y no se MUST registrar información personal en los registros.

**Principios transversales**

- **FR-059**: Todas las cadenas nuevas visibles MUST estar en español y MUST usar el estilo, los componentes y el lenguaje visual existentes de la app.
- **FR-060**: Ninguna de estas mejoras MUST requerir cambios en el contrato del servidor, nuevas credenciales ni nuevos permisos; MUST apoyarse en los datos y capacidades ya existentes.
- **FR-061**: Los estados de error derivados de estas mejoras MUST ser accionables, MUST evitar detalles técnicos y MUST respetar la política existente de privacidad en registros.
- **FR-062**: Ninguna de estas mejoras MUST interrumpir, reiniciar ni degradar la reproducción en curso, la cola, los favoritos, el cambio de modo o la navegación existente.

### Key Entities *(include if feature involves data)*

- **Episodio en curso**: episodio de podcast que el reproductor mantiene activo, en reproducción o pausado, con posición, duración conocida o desconocida, avance y programa asociado.
- **Entrada de cola**: episodio pendiente con su duración y su progreso de escucha; el progreso determina si se muestran minutos restantes.
- **Progreso de escucha**: última posición conocida de un episodio y marca de completado; alimenta el restante de la cola y la reanudación existente.
- **Favorito de episodio**: marca persistente por cuenta que el detalle puede alternar y que el resto de vistas debe reflejar.
- **Preferencia de arranque de podcast**: destino elegido entre Inicio, Descubrir, Suscripciones, Cola y Favoritos, aplicado al iniciar en modo Podcasts.
- **Preferencia de arranque de radio**: destino de radio existente, independiente de la de podcast.
- **Modo de contenido activo**: selección Radio o Podcasts que determina, entre otras cosas, qué conjunto de estadísticas se abre.
- **Conjunto de estadísticas**: datos privados de escucha de radio o de podcast, con su propio encabezado, estados y filtros.
- **Tiempo restante**: diferencia entre duración y posición, limitada a valores no negativos, redondeada a minutos en la cola y expresada con precisión en la tarjeta inferior.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En el 100 % de las aperturas de la cola con un episodio en curso, este aparece en el bloque superior con portada, título, programa y tiempo, y no se duplica entre los pendientes.
- **SC-002**: El tiempo y el avance del bloque superior coinciden con el reproductor con una desviación máxima de 2 segundos en el 100 % de las comprobaciones, incluidas pausa, reanudación y saltos.
- **SC-003**: El 100 % de las filas de la cola con duración conocida muestran la duración; el 100 % de las filas con progreso parcial muestran los minutos restantes correctos y ninguna muestra un restante negativo o inventado.
- **SC-004**: El 100 % de las filas con restante inferior a un minuto muestran "Queda menos de 1 min" y el 100 % de las filas sin progreso o completadas no muestran restante.
- **SC-005**: El acceso de detener del detalle es un icono con etiqueta "Detener" en el 100 % de las aperturas, sin texto visible, y el 100 % de los favoritos marcados desde el detalle se reflejan en la ficha, las tarjetas y la lista de favoritos en 2 segundos o menos desde que la operación se confirma.
- **SC-006**: La tarjeta de reproducción inferior muestra transcurrido y restante correctos en el 100 % de las comprobaciones, se actualiza al menos una vez por segundo mientras reproduce y nunca muestra un restante negativo.
- **SC-007**: La preferencia de pantalla de arranque de podcast se conserva en el 100 % de los reinicios, rotaciones y cambios de servidor, y abre el destino elegido en el 100 % de los arranques con podcasts disponibles.
- **SC-008**: Gráficas abre el conjunto del modo activo en el 100 % de las comprobaciones y ninguna vista mezcla datos de radio y podcast.
- **SC-009**: Todos los textos y descripciones accesibles nuevos están en español, y las pantallas afectadas se leen sin solapamientos con el tamaño de letra al 200 % en un móvil de ancho habitual y en uno estrecho.
- **SC-010**: Las seis mejoras se completan sin cambios en el contrato del servidor, sin nuevas credenciales y sin regresiones en reproducción, cola, favoritos, cambio de modo, configuración ni estadísticas existentes.
- **SC-011**: La vista de Gráficas en modo Podcasts presenta resumen, evolución, hábitos, categorías y rankings con gráficas equivalentes a las de radio en el 100 % de las comprobaciones, sin limitarse a texto ni mostrar datos de radio.

## Assumptions

- La expresión "el tiempo" del bloque superior de la cola se interpreta como tiempo transcurrido y duración total, acompañados de un indicador de avance; el restante se reserva para las filas de la cola y la tarjeta inferior, tal como pide la persona usuaria (confirmado en la clarificación 2026-11-19).
- La "pantalla de detalle del episodio" es el reproductor completo del episodio, porque es donde hoy existe la acción de texto "Detener"; la ficha del episodio ya ofrece favorito y no tiene acción de detener.
- El control de detener usa el mismo lenguaje visual que los demás controles del reproductor y conserva la etiqueta accesible "Detener" para no perder claridad.
- Los destinos de arranque de podcast son las cinco secciones de la barra inferior de podcast: Inicio, Descubrir, Suscripciones, Cola y Favoritos; Inicio es el valor por defecto (confirmado en la clarificación 2026-11-19).
- La selección "según cómo está seleccionado" de Gráficas se refiere al modo de contenido activo Radio | Podcasts, no a un selector interno nuevo dentro de la pantalla de estadísticas (confirmado en la clarificación 2026-11-19). Se muestra un conjunto cada vez.
- La preferencia de arranque de podcast se guarda en el dispositivo, como la de radio, y no se sincroniza entre servidores ni dispositivos.
- La preferencia de arranque de podcast se aplica en el siguiente arranque; no se fuerza una navegación inmediata al cambiarla.
- Las estadísticas de podcast ya las ofrece el servidor actual y no requieren métricas nuevas; esta mejora las hace alcanzables desde Gráficas, muestra el conjunto correcto y las presenta con el mismo lenguaje de gráficas que radio (clarificación 2026-11-19).
- El redondeo del restante en la cola es hacia arriba al minuto entero para no anunciar menos tiempo del que queda; por debajo de un minuto se usa un texto específico.
- El tiempo de la tarjeta inferior se muestra como tiempo transcurrido y restante (confirmado en la clarificación 2026-11-19); la duración total se consulta en el reproductor completo. El formato es compacto de reloj y el restante lleva signo menos o indicación equivalente, con una descripción accesible completa.
- El cierre de la tarjeta al detener requiere limpiar el episodio en curso; el código previo lo mantenía visible y no reanudable, por lo que se corrige en esta feature.
- El bloque superior de la cola no añade acciones de gestión: reproducir, pausar, saltar y detener siguen en el reproductor; tocarlo abre el reproductor completo.
- Los datos de duración, progreso, favorito y estadísticas ya existen en el contrato actual; no se añaden campos, servicios, permisos ni credenciales.
- Fuera de alcance: nuevas métricas de estadísticas, comparación simultánea radio/podcast, reordenar o quitar el episodio en curso desde el bloque superior, cambios en descargas, cambios en el temporizador de apagado y rediseño general de la cola o del reproductor.

## Dependencies

- **Spec 0046 — Modo Podcast**: la cola, la ficha, el reproductor completo, los favoritos de episodios, las estadísticas privadas y la preferencia de sección inicial nacen de esa spec; esta los refina.
- **Spec 0047 — Mejoras de experiencia podcast**: la tarjeta inferior, la insignia de cola, el potenciador de voz y la retención de descargas son la base sobre la que se añaden los tiempos y el favorito del detalle.
- **Spec 0041 — Gráficas de escucha**: el conjunto de estadísticas de radio, su filtro de periodo y sus estados son la referencia de la vista de radio.
- **Spec 0042 / 0044 — Motor y recuperación de reproducción**: la posición, la duración y el estado del reproductor alimentan los tiempos de la cola y de la tarjeta inferior.
- **Constitución (v3.1.0)**: se respetan los principios de arquitectura, privacidad, español en la UI y los gates de calidad; no se requiere enmienda.
