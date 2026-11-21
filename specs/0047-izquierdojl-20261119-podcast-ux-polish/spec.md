# Feature Specification: Mejoras de experiencia podcast

**Feature Branch**: `0047-izquierdojl-20261119-podcast-ux-polish`

**Created**: 2026-11-19

**Status**: Done (2026-11-19; validado en emulador, incluida la revisión manual y TalkBack); extensión de búsqueda 2026-11-21 cerrada y verificada manualmente en emulador; extensión de actualización por arrastre cerrada y verificada manualmente en emulador (2026-11-21)

**Input**: User description: "Vamos a planificar una spec con las siguientes mejoras

- En el botón de la cola, poner en pequeño un budgie o similar con el total de episodios encolados.
- Animar el botón de descarga con el porcentaje descargado cuando se pulse
- Mejorar visibilidad tarjeta de podcast, aumentar un poco la altura para que se vea el titulo completo y los botones de acción debajo, ahora deja poco espacio al nombre del podcast y apenas se lee.
- En la notificación multimedia android, que se pueda adelantar y atrasar como en la interfaz
- Potenciador de voz para mejorar el sonido, similar a pocket cast
- Reorganizar el menu superior, ya que se monta un texto. El unico que se debe quedar es el cast y el intercambiador radio/podcast, el resto incluirlo en un menú de hamburguesa. En el menú hamburguesa incluir también la opción de configuración y quitarlo de la barra de abajo, asi ganamos espacio para los iconos.
- En la gestión de cola, si un episodio ya ha reproducido parte de su duración, si se selecciona, debe continuar donde termino, ahora comienza desde el principio."

## Clarifications

### Session 2026-11-19

- Q: ¿A qué tarjeta se refiere la mejora de altura/legibilidad? → A: Ambas tarjetas (programa y episodio), unificando el criterio visual.
- Q: ¿Dónde debe aplicarse el potenciador de voz? → A: Solo episodios de podcast; no se aplica a radio en directo ni a Chromecast.
- Q: ¿Qué debe ocupar el hueco izquierdo de la cabecera, donde hoy está el logotipo "TolochaRadio"? → A: Se mantiene el logotipo completo y se añade la hamburguesa; en pantallas estrechas el logotipo se reduce hasta el emblema.
- Q: ¿La insignia de la cola debe incluir el episodio que ya está sonando cuando forma parte de la cola? → A: Solo cuenta los pendientes, sin el episodio en curso.

### Session 2026-11-21

- Q: Cuando el ámbito «Episodios» muestra resultados, ¿estos sustituyen a la lista de suscripciones o se muestran junto a ella? → A: Sustituyen a la lista mientras la búsqueda está activa; al vaciar el término o cambiar de ámbito vuelve la lista.
- Q: Al cambiar entre «Podcasts» y «Episodios», ¿se conserva el término escrito y se aplica al nuevo ámbito, o se borra? → A: Se conserva un único término compartido y se aplica al nuevo ámbito; «Podcasts» filtra al instante y «Episodios» espera a confirmar.
- Q: Con resultados de episodios visibles, ¿el botón Atrás debe volver primero a la lista de suscripciones o salir directamente de la pantalla? → A: Atrás vuelve a la lista con el ámbito «Podcasts» y el término conservado; un segundo Atrás sale. Con el teclado abierto, el primer Atrás solo lo oculta.
- Q: ¿La búsqueda de episodios debe incluir también los episodios ya completados o solo los pendientes y en curso? → A: Incluye todos los episodios de las suscripciones, completados incluidos, mostrando su estado de escucha.
- Q: Cuando la persona no tiene ninguna suscripción, ¿debe ver el buscador en la pantalla de Suscripciones o solo el estado vacío con la acción de descubrir? → A: Sin suscripciones no se muestra el buscador; se mantiene el estado vacío con la acción de descubrir.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Retomar desde la cola donde se dejó el episodio (Priority: P1)

La persona usuaria escucha parte de un episodio, lo deja a medias y más tarde lo selecciona desde la cola de reproducción. La reproducción continúa en el punto donde se quedó, no desde el principio. Si el episodio ya estaba completado, empieza de nuevo.

**Why this priority**: Es una promesa básica del podcast y hoy se rompe justo en el flujo más usado (la cola): obliga a buscar a mano el punto de escucha y hace perder el hilo. Corregirlo aporta valor por sí solo, sin depender del resto de mejoras.

**Independent Test**: Reproducir un episodio de 60 minutos durante unos 10, pausarlo, comprobar que figura en la cola y seleccionarlo desde allí; la reproducción debe reanudarse cerca del minuto 10.

**Acceptance Scenarios**:

1. **Given** un episodio de la cola con progreso parcial guardado, **When** la persona lo selecciona, **Then** la reproducción comienza en la posición guardada con una desviación máxima de 5 segundos, no en el segundo cero.
2. **Given** un episodio de la cola marcado como completado, **When** la persona lo selecciona, **Then** la reproducción comienza desde el principio.
3. **Given** un episodio de la cola sin progreso guardado, **When** la persona lo selecciona, **Then** la reproducción comienza desde el principio sin mostrar errores.
4. **Given** un episodio reanudado desde la cola, **When** la persona lo pausa y vuelve a seleccionarlo más tarde, **Then** continúa en la última posición alcanzada.

---

### User Story 2 - Adelantar y atrasar desde la notificación multimedia (Priority: P1)

Con la pantalla apagada o bloqueada, la persona usuaria controla el episodio desde la notificación multimedia y la pantalla de bloqueo: además de reproducir/pausar, puede retroceder y avanzar con los mismos saltos que en la interfaz.

**Why this priority**: Buena parte de la escucha ocurre con el teléfono guardado o en el bolsillo. Obligar a desbloquear para saltar 30 segundos rompe la experiencia y es arriesgado al conducir o caminar. La paridad con los controles de la app es exigible.

**Independent Test**: Reproducir un episodio, bloquear la pantalla y usar los controles de la notificación para retroceder y avanzar; comprobar que la posición cambia en los mismos saltos que en la app y que la radio en directo no ofrece esos controles.

**Acceptance Scenarios**:

1. **Given** un episodio en reproducción con la pantalla bloqueada, **When** la persona pulsa retroceder en la notificación, **Then** la posición retrocede 15 segundos y el cambio se refleja también al abrir la app.
2. **Given** un episodio en reproducción con la pantalla bloqueada, **When** la persona pulsa avanzar en la notificación, **Then** la posición avanza 30 segundos y el cambio se refleja también al abrir la app.
3. **Given** una emisora de radio en directo, **When** la persona mira la notificación, **Then** no se ofrecen controles de retroceso ni avance porque el directo no se puede desplazar.
4. **Given** un episodio pausado, **When** la persona usa los controles de la notificación, **Then** estos siguen disponibles y actúan sobre la posición actual.
5. **Given** un episodio al principio o cerca del final, **When** la persona retrocede o avanza, **Then** el salto se limita al inicio o al final sin fallar, y llegar al final completa el episodio con el comportamiento habitual.

---

### User Story 3 - Cabecera despejada y menú de hamburguesa (Priority: P2)

La persona usuaria ve una cabecera superior limpia: el menú de hamburguesa, el identificador de la app, el selector Radio | Podcasts y el botón de emisión a otros dispositivos. El temporizador de apagado, los servidores y la configuración pasan al menú de hamburguesa. Configuración deja de ocupar un hueco en la barra inferior, que gana espacio para el resto de iconos.

**Why this priority**: Hoy el texto y los iconos se montan unos sobre otros y la cabecera resulta ilegible en pantallas estrechas. Además, liberar la barra inferior mejora el uso de todas las secciones. No bloquea la escucha, pero afecta a toda la app.

**Independent Test**: En una pantalla estrecha y con el tamaño de letra grande, comprobar que la cabecera no solapa elementos; abrir el menú de hamburguesa y acceder al temporizador, servidores y configuración; comprobar que configuración ya no está en la barra inferior.

**Acceptance Scenarios**:

1. **Given** cualquier pantalla con cabecera compartida en un ancho de móvil habitual o estrecho, **When** la persona mira la cabecera, **Then** la hamburguesa, el identificador, el selector Radio | Podcasts y el botón de Cast se ven sin solaparse ni cortarse; si el identificador no cabe entero, se muestra al menos el emblema.
2. **Given** la cabecera, **When** la persona abre el menú de hamburguesa, **Then** ve al menos Temporizador, Servidores y Configuración, cada uno con su etiqueta, y puede abrirlos.
3. **Given** el menú de hamburguesa abierto, **When** la persona elige una opción, **Then** el menú se cierra, se abre la función elegida y la reproducción en curso no se interrumpe.
4. **Given** el temporizador de apagado activo, **When** la persona abre el menú de hamburguesa, **Then** ve que está activo y el tiempo restante, y puede cancelarlo.
5. **Given** la barra inferior en modo Radio o Podcasts, **When** la persona la mira, **Then** Configuración ya no aparece en ella y los destinos restantes conservan su orden y muestran su etiqueta completa.
6. **Given** una pantalla abierta desde el menú de hamburguesa, **When** la persona vuelve atrás, **Then** regresa a la misma pantalla y conserva su estado (posición de lista y reproducción).
7. **Given** un servidor sin podcasts, **When** la persona usa la app, **Then** el selector Radio | Podcasts no se muestra y el resto de la cabecera y el menú siguen siendo coherentes.

---

### User Story 4 - Potenciador de voz para episodios (Priority: P2)

La persona usuaria activa un potenciador de voz en el reproductor de episodios para oír con más claridad y volumen las voces de los podcast con audio bajo o desigual. El ajuste se aplica al instante, se recuerda para los siguientes episodios y no altera la radio en directo ni la emisión a otros dispositivos.

**Why this priority**: Muchos podcast llegan con niveles bajos o muy variables; es una función de referencia en Pocket Casts y mejora directamente la experiencia de escucha. Depende de la reproducción de episodios ya existente.

**Independent Test**: Reproducir un episodio con voces bajas, activar el potenciador y comprobar que se oye más alto y claro sin distorsión; cerrar y reabrir la app y comprobar que sigue activo; cambiar a radio y comprobar que no se aplica.

**Acceptance Scenarios**:

1. **Given** un episodio en reproducción, **When** la persona activa el potenciador de voz, **Then** el cambio se nota de inmediato, sin pausar ni reiniciar el episodio.
2. **Given** el potenciador activo, **When** la persona desactiva, **Then** el audio vuelve a su comportamiento original de inmediato.
3. **Given** el potenciador activo, **When** la persona empieza otro episodio o reinicia la app, **Then** el potenciador sigue activo y aplicado.
4. **Given** el potenciador activo, **When** la persona reproduce una emisora de radio o emite a otro dispositivo, **Then** el efecto no se aplica a esas salidas.
5. **Given** contenido ya muy alto, **When** el potenciador está activo, **Then** no se produce distorsión audible ni saturación.
6. **Given** el reproductor de episodios, **When** la persona busca el control, **Then** encuentra un interruptor con estado claramente visible y etiqueta comprensible.

---

### User Story 5 - Contador de episodios en el botón de la cola (Priority: P3)

La persona usuaria ve en el botón de la cola una insignia pequeña con el número de episodios pendientes, de modo que sabe de un vistazo si tiene algo encolado sin abrir la cola.

**Why this priority**: Es información útil y de bajo coste, pero no imprescindible para escuchar. Complementa la gestión de cola existente.

**Independent Test**: Añadir tres episodios, ver la insignia con el número; quitar uno, vaciar la cola y comprobar que la insignia se actualiza y desaparece con la cola vacía.

**Acceptance Scenarios**:

1. **Given** una cola con episodios pendientes, **When** la persona mira la barra inferior, **Then** el botón de la cola muestra una insignia con el total de pendientes.
2. **Given** la cola abierta o no, **When** la persona añade, quita, reordena o vacía episodios, **Then** la insignia refleja el nuevo total sin necesidad de abrir la cola.
3. **Given** una cola sin pendientes, **When** la persona mira el botón de la cola, **Then** no se muestra insignia alguna (no un cero).
4. **Given** un episodio en reproducción que también figura en la cola, **When** la persona mira la insignia, **Then** el total cuenta solo los pendientes, en coherencia con lo que muestra la pantalla de cola.
5. **Given** más de cien pendientes, **When** la persona mira la insignia, **Then** se muestra un indicador compacto del tipo "99+" en lugar de un número desbordado.

---

### User Story 6 - Descarga animada con porcentaje (Priority: P3)

Cuando la persona usuaria pulsa descargar en un episodio, el botón se transforma en una animación de progreso con el porcentaje descargado hasta completarse, momento en el que pasa al estado "descargado". Si la descarga falla o se cancela, vuelve al estado inicial con un aviso.

**Why this priority**: Mejora la confianza y evita pulsaciones repetidas, pero la descarga ya funciona; es pulido de experiencia.

**Independent Test**: Descargar un episodio grande y observar cómo el botón avanza de 0 % a 100 % y termina en estado descargado; simular un fallo y comprobar que se puede reintentar.

**Acceptance Scenarios**:

1. **Given** un episodio descargable no descargado, **When** la persona pulsa descargar, **Then** el botón muestra una animación de progreso con el porcentaje descargado.
2. **Given** una descarga en curso, **When** la persona mira el botón, **Then** el porcentaje avanza de forma perceptible y comprensible, sin superar el 100 %.
3. **Given** una descarga completada, **When** la persona mira el episodio, **Then** se muestra el estado descargado y desaparece el porcentaje.
4. **Given** una descarga que falla o se cancela, **When** termina, **Then** el botón vuelve a permitir descargar y se informa del motivo en español.
5. **Given** una descarga en curso, **When** la persona cambia de pantalla y vuelve, **Then** el progreso sigue visible y coherente con la descarga real.
6. **Given** un servidor que no permite descargas, **When** la persona mira un episodio, **Then** no aparece el botón de descarga ni su animación.

---

### User Story 7 - Tarjetas de programa y episodio más legibles (Priority: P3)

Las tarjetas en cuadrícula de programas y de episodios ganan un poco de altura para que el título se lea completo y las acciones (reproducir, favorito, baja) tengan su propio espacio en lugar de disputárselo al texto. En la tarjeta de episodio, el nombre del podcast deja de quedar reducido a un resto ilegible.

**Why this priority**: Es legibilidad y estética; no bloquea ninguna función, pero afecta a la primera impresión de las listas de podcast.

**Independent Test**: Abrir Inicio, Suscripciones y Favoritos con títulos largos y comprobar que los títulos se leen hasta dos líneas, que las acciones no pisan el texto y que las tarjetas de una misma fila mantienen la misma altura.

**Acceptance Scenarios**:

1. **Given** un programa con título largo en la cuadrícula, **When** la persona lo mira, **Then** el título dispone de todo el ancho de la tarjeta y se lee hasta dos líneas, y la acción de baja queda en una fila propia debajo del texto, sin recortar el nombre.
2. **Given** un episodio con nombre de podcast y metadatos, **When** la persona lo mira, **Then** el nombre del podcast y los metadatos se leen completos en su línea, y solo se recortan si de verdad no caben.
3. **Given** una fila de tarjetas con títulos de distinta longitud, **When** se muestran, **Then** todas mantienen la misma altura y no saltan al cargar las portadas.
4. **Given** el tamaño de letra del sistema aumentado, **When** la persona mira las tarjetas, **Then** el contenido crece en altura y no se solapa con las acciones ni con el borde.
5. **Given** las tarjetas actualizadas, **When** la persona reproduce, marca favorito o cancela una suscripción, **Then** las acciones siguen funcionando y conservan su descripción accesible.

---

### User Story 8 - Buscar en la pantalla de Suscripciones (Priority: P2)

La persona usuaria, desde la pantalla de Suscripciones y con al menos una suscripción, dispone de un buscador con dos ámbitos: «Podcasts» y «Episodios». Los controles de búsqueda están ocultos por defecto y se despliegan con el botón de búsqueda de la cabecera, situado a la izquierda del orden de suscripciones. En «Podcasts» filtra al escribir entre los programas que sigue por nombre. En «Episodios» busca por título entre los episodios de sus suscripciones y ve los resultados con el programa al que pertenecen, pudiendo abrirlos y usar sus acciones habituales. La búsqueda sigue el mismo modelo que la versión web: filtrar podcasts por nombre y buscar episodios por título dentro de las suscripciones.

**Why this priority**: Con muchas suscripciones, encontrar un programa o un episodio concreto obliga a recorrer la lista entera o a acudir al buscador del directorio, que no se limita a lo que ya sigue. El servidor ya ofrece esta búsqueda a la web; llevarla a la app cierra una brecha de paridad y ahorra tiempo sin bloquear la escucha.

**Independent Test**: Con varias suscripciones, escribir parte del nombre de una en «Podcasts» y comprobar que la lista se reduce al instante; cambiar a «Episodios», buscar un término que solo aparezca en el título de un episodio suscrito y comprobar que aparece con su programa; buscar un término sin coincidencias y comprobar el mensaje; borrar el término y comprobar que vuelve la lista completa.

**Acceptance Scenarios**:

1. **Given** la pantalla Suscripciones con varias suscripciones, **When** la persona escribe parte del nombre de un programa con el ámbito «Podcasts» activo, **Then** la lista se reduce al instante a los programas cuyo nombre contiene el término, sin distinguir mayúsculas ni tildes, y se conserva el orden elegido.
2. **Given** el ámbito «Podcasts» con un término que no coincide con ninguna suscripción, **When** la persona mira la lista, **Then** ve un mensaje en español con el término buscado y puede borrarlo para recuperar la lista completa.
3. **Given** el ámbito «Episodios», **When** la persona escribe un término y confirma la búsqueda, **Then** los resultados sustituyen a la lista de suscripciones y muestran episodios de sus suscripciones —incluidos los completados— cuyo título contiene el término, con el nombre del programa, la fecha, la duración y su estado de escucha, ordenados del más reciente al más antiguo.
4. **Given** una búsqueda de episodios con más coincidencias que las mostradas, **When** la persona llega al final de los resultados, **Then** puede cargar más sin repetir ni perder los ya visibles.
5. **Given** los resultados de episodios, **When** la persona abre uno, **Then** accede a su detalle y sus acciones de reproducir, favorito, cola y descarga funcionan con el mismo comportamiento y estado que en el resto de la app.
6. **Given** una búsqueda de episodios activa, **When** la persona cambia al ámbito «Podcasts», **Then** los resultados desaparecen, el término se conserva y la lista de suscripciones se filtra al instante con ese término.
7. **Given** una búsqueda de episodios activa, **When** la persona cancela la suscripción de un podcast cuyos episodios aparecen, **Then** esos episodios desaparecen de los resultados y la lista refleja la baja.
8. **Given** una búsqueda de episodios sin coincidencias o con error de red, **When** termina la consulta, **Then** se informa en español, se conserva el término, se puede reintentar si hubo error y no se muestran resultados de búsquedas anteriores.
9. **Given** la pantalla con lector de pantalla activo, **When** la persona recorre el buscador, **Then** el campo y la alternancia de ámbito tienen etiquetas comprensibles, el ámbito activo se anuncia como seleccionado y el cambio de resultados se comunica sin perder el foco.
10. **Given** resultados de episodios visibles con el teclado cerrado, **When** la persona pulsa Atrás, **Then** vuelve a la lista de suscripciones con el ámbito «Podcasts» activo y el término conservado, y un segundo Atrás sale de la pantalla.
11. **Given** una cuenta sin suscripciones, **When** la persona abre Suscripciones, **Then** no ve el buscador y sí el estado vacío con la acción de descubrir podcasts.
12. **Given** la pantalla Suscripciones con suscripciones, **When** la persona mira la cabecera, **Then** los controles de búsqueda están ocultos y ve un botón de búsqueda a la izquierda del orden; al pulsarlo se despliegan el campo y los ámbitos, y al volver a pulsarlo se ocultan y la lista vuelve completa.

---

### User Story 9 - Actualizar Inicio y Suscripciones arrastrando hacia abajo (Priority: P3)

La persona usuaria, en las pantallas de Inicio y de Suscripciones, arrastra la vista hacia abajo cuando está al principio y la suelta: la pantalla vuelve a cargar sus datos y muestra la información más reciente, como al recargar una pestaña del navegador. Mientras se actualiza ve un indicador; al terminar, el indicador desaparece y la vista conserva su modo, su orden, su búsqueda y la reproducción en curso.

**Why this priority**: Es una convención de uso muy extendida que da control inmediato sobre datos que cambian en el servidor (nuevas suscripciones, novedades, cola) sin cerrar y abrir la pantalla. No bloquea ninguna función y se apoya en las consultas existentes.

**Independent Test**: En Inicio, con la lista al principio, arrastrar hacia abajo y soltar: aparece el indicador y, al terminar, las secciones reflejan un cambio hecho desde otro cliente. Repetir en Suscripciones y comprobar que aparecen altas y desaparecen bajas sin perder el orden ni el modo de vista; con la lista desplazada, comprobar que el gesto no actualiza hasta llegar al principio.

**Acceptance Scenarios**:

1. **Given** la pantalla Inicio con contenido y al principio, **When** la persona arrastra hacia abajo y suelta, **Then** se inicia una actualización, aparece un indicador visible y este permanece hasta que se muestran los datos actualizados, sin dejar la pantalla en blanco ni cambiar de sección.
2. **Given** la pantalla Suscripciones con contenido y al principio, **When** la persona arrastra hacia abajo y suelta, **Then** la lista se vuelve a cargar y refleja las altas y bajas, conservando el modo de vista, el orden activo y el término de búsqueda si lo hubiera.
3. **Given** una lista desplazada lejos del principio, **When** la persona arrastra hacia abajo, **Then** el gesto solo desplaza la lista y no actualiza hasta que la vista está al principio; una vez allí, seguir arrastrando hacia abajo inicia la actualización.
4. **Given** una actualización en curso, **When** la persona vuelve a arrastrar hacia abajo, **Then** no se lanza una segunda actualización y el indicador sigue visible hasta que termina la vigente.
5. **Given** una actualización que falla por falta de conexión o error del servidor, **When** termina, **Then** el contenido anterior sigue visible, se informa en español y se puede reintentar, sin pantalla en blanco ni detalles técnicos.
6. **Given** Suscripciones con una búsqueda activa, **When** la persona actualiza con el gesto, **Then** se conservan el término y el ámbito: en «Podcasts» se recarga la lista y se reaplica el filtro; en «Episodios» se repite la búsqueda vigente y se muestran resultados actualizados.
7. **Given** una reproducción o una descarga en curso, **When** la persona actualiza Inicio o Suscripciones, **Then** la reproducción no se interrumpe, la descarga, la cola y el progreso local no se reinician y, si la respuesta ya no incluye el programa o episodio en reproducción, este sigue sonando y solo desaparece de la lista.
8. **Given** el lector de pantalla activo, **When** la persona abre las acciones del contenido y elige «Actualizar», **Then** se inicia la recarga sin depender del gesto, el inicio y el fin se anuncian y el foco no se pierde.

---

### Edge Cases

- Episodio de la cola cuyo progreso no está disponible en ese momento: se empieza desde el principio sin bloquear la cola ni mostrar un error técnico.
- Progreso guardado fuera de rango (negativo, igual o mayor que la duración): se empieza desde el principio.
- Episodio completado que se vuelve a seleccionar desde la cola: empieza desde el principio y conserva el marcado manual de reproducido/pendiente que ya existe.
- Cola que cambia en otro dispositivo: la insignia se actualiza con la cola vigente y no muestra cifras obsoletas.
- Cola vacía pero con un episodio sonando: no hay insignia, porque no hay pendientes.
- Radio en directo, contenido no desplazable o emisión a otro dispositivo: la notificación no ofrece retroceso ni avance.
- Salto hacia atrás al inicio de la pista: se queda en el segundo cero; salto hacia delante más allá del final: se detiene al final y completa el episodio.
- Temporizador activo cuando se reorganiza la cabecera: debe seguir siendo visible y cancelable desde el menú de hamburguesa.
- Menú de hamburguesa abierto mientras suena audio: la reproducción continúa y el menú se cierra al elegir una opción o al tocar fuera.
- Rotación, cambio de tema o vuelta desde segundo plano con el menú abierto: el estado no se corrompe.
- Potenciador de voz activo al cambiar de episodio, de radio o a Cast: no hay cortes, ni doble aplicación, ni efecto sobre radio/Cast.
- Potenciador con audio muy alto o con auriculares: no debe provocar saturación ni subidas bruscas de volumen.
- Descarga con tamaño total desconocido: se muestra una animación indeterminada y una descripción comprensible, sin un porcentaje inventado.
- Descarga en curso al girar la pantalla, cambiar de pantalla o volver del segundo plano: el progreso no se reinicia ni se duplica la descarga.
- Descarga fallida o cancelada, o sin espacio: se vuelve al estado descargable con aviso y sin dejar ficheros corruptos.
- Tarjetas con títulos muy largos, caracteres especiales o tamaño de letra máximo: se recortan con puntos suspensivos solo tras dos líneas y nunca tapan las acciones.
- Término de búsqueda compuesto solo por espacios: se trata como vacío; no se consulta ni se muestra el mensaje de «sin resultados».
- Ocultar el buscador con un término o resultados activos: se limpian el término y los resultados y se vuelve a la lista completa, sin dejar la lista filtrada sin control visible.
- Búsqueda de episodios con un término muy largo o con caracteres especiales: se envía tal cual, sin romper la consulta; si no hay coincidencias se muestra el mensaje habitual.
- Cambio rápido de ámbito o de término con una búsqueda en curso: solo la última consulta puede mostrar resultados; las respuestas tardías se descartan.
- Rotación, cambio de tema o vuelta desde segundo plano durante la búsqueda: se conservan el término, el ámbito y los resultados visibles sin repetir la consulta de forma innecesaria.
- Cancelación de la suscripción del único podcast que coincidía con la búsqueda: sus episodios desaparecen de los resultados y la lista de suscripciones refleja la baja.
- Sin conexión al buscar episodios: se muestra un error accionable y se conserva el término; el filtrado local de podcasts sigue funcionando con la lista ya cargada.
- Sin ninguna suscripción: no se muestra el buscador; se mantiene el estado vacío con la acción de descubrir podcasts.
- Servidor sin podcasts: no hay barra de podcast, ni insignia de cola, ni controles de episodio, ni buscador de suscripciones; la cabecera y la barra de radio siguen completas.
- Arrastre con la lista desplazada: solo desplaza; la actualización solo se activa al llegar al principio y continuar el arrastre.
- Actualización solicitada durante la carga inicial o mientras hay una búsqueda o un «cargar más» en curso: no se duplican consultas y prevalece la solicitud vigente.
- Actualización sin conexión o con error del servidor: se conservan los datos ya visibles y se informa con reintento; si la pantalla aún no tenía datos, se mantiene el estado de error existente.
- Rotación, cambio de tema o vuelta desde segundo plano durante la actualización: el indicador no se duplica y, al volver, se muestra el resultado vigente sin perder el estado de la pantalla.
- Actualización mientras se reordena la lista de suscripciones: el gesto de orden tiene prioridad y no dispara una recarga accidental.
- Datos que cambian entre actualizaciones: las bajas desaparecen, las altas se incorporan y lo que ya no existe en el servidor deja de mostrarse; la reproducción en curso continúa aunque su programa o episodio haya desaparecido de la lista.
- Actualización con el reproductor o una descarga activos: no se pausa la reproducción ni se cancelan las descargas.
- Arrastre en una pantalla vacía o en error: el gesto no es obligatorio; siguen disponibles las acciones existentes de descubrir o reintentar.
- Arrastres repetidos muy rápidos: solo una actualización está vigente y las solicitudes descartadas no generan mensajes de error.
- Con lector de pantalla activo, la recarga se inicia desde la acción accesible «Actualizar» del contenido; no depende de que la persona acierte con el gesto de arrastre.
- La respuesta de la recarga ya no incluye el programa o episodio en reproducción: desaparece de la lista, pero la reproducción continúa sin cortes y conserva título y posición.

## Requirements *(mandatory)*

### Functional Requirements

**Reanudación desde la cola**

- **FR-001**: Al seleccionar un episodio con progreso parcial desde la cola, el sistema MUST continuar en la posición guardada, con una desviación máxima de 5 segundos, y MUST NOT reiniciar desde el principio.
- **FR-002**: Un episodio marcado como completado MUST empezar desde el principio al seleccionarlo desde la cola.
- **FR-003**: Si no hay progreso válido (ausente, negativo, igual o mayor que la duración), el sistema MUST empezar desde el principio sin mostrar errores.
- **FR-004**: La posición alcanzada tras reanudar MUST guardarse como cualquier otra escucha para que la siguiente selección continúe en ese punto.
- **FR-005**: El mismo criterio de reanudación MUST aplicarse cuando el episodio empieza automáticamente al terminar el anterior de la cola.
- **FR-006**: La posición usada para reanudar MUST ser la más reciente conocida por la app para ese episodio, no una copia obsoleta de la lista.

**Controles en la notificación multimedia**

- **FR-007**: Durante la reproducción local de un episodio, la notificación multimedia y la pantalla de bloqueo MUST ofrecer retroceso y avance además de reproducir/pausar.
- **FR-008**: Los saltos de la notificación MUST coincidir con los de la interfaz: 15 segundos atrás y 30 segundos adelante.
- **FR-009**: Las acciones de la notificación MUST actuar sobre el mismo estado de reproducción que muestra la app y MUST reflejarse en ella de inmediato.
- **FR-010**: En radio en directo o en contenido no desplazable, la notificación MUST NOT ofrecer retroceso ni avance.
- **FR-011**: Los saltos MUST limitarse al inicio y al final de la pista sin fallar; llegar al final con un avance MUST completar el episodio con el comportamiento habitual.
- **FR-012**: Los controles MUST tener etiquetas accesibles en español y MUST seguir disponibles con la reproducción pausada.
- **FR-013**: Durante la emisión a otro dispositivo, los controles mostrados MUST ser coherentes con la sesión remota y MUST NOT duplicar ni contradecir sus acciones.

**Cabecera, menú de hamburguesa y barra inferior**

- **FR-014**: La cabecera superior MUST mostrar el menú de hamburguesa, el identificador de la app, el selector Radio | Podcasts (si el servidor ofrece podcasts) y el acceso a la emisión a otros dispositivos, y MUST NOT incluir ninguna otra acción directa.
- **FR-015**: El temporizador de apagado, los servidores y la configuración MUST dejar de ser acciones directas de la cabecera y MUST trasladarse al menú de hamburguesa.
- **FR-016**: La cabecera MUST adaptarse a anchos de móvil pequeños y medianos y a tamaños de letra de hasta el 200 % (SC-003) sin solapar ni recortar sus elementos; si el identificador completo no cabe, MUST reducirse hasta el emblema, y la hamburguesa y las acciones MUST permanecer visibles y utilizables.
- **FR-017**: El menú de hamburguesa MUST estar disponible en todas las pantallas que muestran la cabecera compartida y MUST incluir Temporizador, Servidores y Configuración con icono y etiqueta.
- **FR-018**: Al elegir una opción del menú, este MUST cerrarse y MUST abrirse la función correspondiente sin detener ni reiniciar la reproducción en curso.
- **FR-019**: Si el temporizador está activo, el menú de hamburguesa MUST indicarlo y MUST mostrar el tiempo restante y la opción de cancelarlo.
- **FR-020**: Al volver desde una pantalla abierta desde el menú, la persona MUST regresar a la pantalla anterior conservando su estado (posición de lista y reproducción).
- **FR-021**: Configuración MUST dejar de aparecer en la barra inferior, tanto en modo Radio como en modo Podcasts, y los destinos restantes MUST conservar su orden y sus etiquetas completas.
- **FR-022**: Quitar Configuración de la barra inferior MUST NOT romper el acceso directo, la navegación atrás ni el arranque; Configuración MUST seguir accesible desde el menú de hamburguesa en todas las pantallas con cabecera compartida.
- **FR-023**: El selector Radio | Podcasts y el acceso a la emisión MUST seguir siendo accesibles y visibles cuando corresponda.

**Potenciador de voz**

- **FR-024**: El reproductor de episodios MUST ofrecer un control de potenciador de voz con estado encendido/apagado claramente visible y etiqueta comprensible.
- **FR-025**: Activar o desactivar el potenciador MUST aplicarse de inmediato, sin pausar ni reiniciar la reproducción.
- **FR-026**: La preferencia MUST recordarse entre episodios y entre reinicios de la app.
- **FR-027**: El potenciador MUST aplicarse solo a la reproducción local de episodios y MUST NOT aplicarse a radio en directo ni a la emisión a otros dispositivos.
- **FR-028**: El potenciador MUST mejorar la claridad y el nivel percibido de las voces sin introducir distorsión audible (según la muestra y el protocolo de SC-006) ni saturación en contenido ya alto.
- **FR-029**: El potenciador MUST estar desactivado por defecto la primera vez.
- **FR-030**: El control MUST estar disponible en el reproductor de episodios y MUST NOT aparecer como opción de radio.

**Insignia de la cola**

- **FR-031**: El destino Cola de la barra inferior MUST mostrar una insignia pequeña con el número de episodios pendientes.
- **FR-032**: El número MUST actualizarse automáticamente, sin abrir la cola, tras añadir, quitar, reordenar, vaciar o avanzar automáticamente de episodio.
- **FR-033**: La insignia MUST ocultarse cuando no haya pendientes y MUST mostrar un indicador compacto del tipo "99+" cuando se superen cien.
- **FR-034**: El número MUST excluir el episodio en reproducción y MUST coincidir con lo que muestra la pantalla de cola.
- **FR-035**: La insignia MUST incluir una descripción accesible con el total y MUST NOT mostrarse si la cola no está disponible, sin generar errores visibles.
- **FR-036**: La insignia MUST NOT solaparse con la etiqueta ni con el icono del destino y MUST mantener un área táctil de al menos 48 dp.

**Descarga con porcentaje**

- **FR-037**: Al pulsar descargar, el botón MUST transformarse en una animación de progreso que muestre el porcentaje descargado.
- **FR-038**: El porcentaje MUST avanzar de forma perceptible y MUST NOT superar el 100 %.
- **FR-039**: Al completarse, MUST mostrarse el estado descargado y MUST desaparecer el porcentaje.
- **FR-040**: Si la descarga falla o se cancela, el botón MUST volver al estado descargable y MUST informarse del motivo en español.
- **FR-041**: El progreso MUST conservarse al cambiar de pantalla, girar el dispositivo o volver del segundo plano, y MUST ser coherente con la descarga real.
- **FR-042**: Mientras una descarga esté en curso, MUST NOT iniciarse una segunda descarga del mismo episodio por pulsaciones repetidas.
- **FR-043**: Si el tamaño total no se conoce, MUST mostrarse una animación indeterminada con una descripción comprensible en lugar de un porcentaje inventado.
- **FR-044**: Si el servidor no permite descargas, MUST NOT mostrarse el botón de descarga ni su animación.

**Tarjetas de programa y episodio**

- **FR-045**: Las tarjetas en cuadrícula de programa y de episodio MUST ganar altura suficiente para mostrar el título hasta dos líneas y MUST ubicar sus acciones en una fila propia debajo del texto, de modo que no reduzcan el ancho del título ni de los metadatos.
- **FR-046**: En la tarjeta de episodio, el nombre del podcast y los metadatos MUST leerse completos en su línea y MUST recortarse solo cuando de verdad no quepan.
- **FR-047**: Las tarjetas de una misma fila MUST mantener la misma altura y MUST NOT cambiar de tamaño al cargar las portadas.
- **FR-048**: Con el tamaño de letra del sistema aumentado, las tarjetas MUST crecer en altura antes que solapar o recortar el contenido.
- **FR-049**: Las acciones de reproducir, marcar favorito y cancelar suscripción MUST conservar su comportamiento, su tamaño táctil mínimo y sus descripciones accesibles.
- **FR-050**: Los cambios de tarjeta MUST aplicarse de forma coherente en Inicio, Suscripciones y Favoritos, sin introducir variantes visuales divergentes.

**Principios transversales**

- **FR-051**: Todas las cadenas nuevas visibles MUST estar en español y MUST usar el estilo y los componentes visuales existentes de la app.
- **FR-052**: Ninguna de estas mejoras MUST requerir cambios en el contrato del servidor ni introducir nuevas credenciales; todo el comportamiento MUST apoyarse en las capacidades y datos ya existentes.
- **FR-053**: Los estados de error derivados de estas mejoras MUST ser accionables, MUST evitar detalles técnicos y MUST respetar la política existente de privacidad en registros.

### Key Entities *(include if feature involves data)*

- **Episodio en cola**: episodio pendiente con un progreso de escucha asociado; la posición guardada determina el punto de reanudación.
- **Progreso de escucha**: última posición conocida por episodio y marca de completado; gobierna la reanudación desde cualquier lista y desde la cola.
- **Cola de reproducción**: lista ordenada de pendientes más el episodio en curso; la insignia muestra solo el número de pendientes.
- **Controles de transporte**: conjunto de acciones disponibles para el contenido en curso (reproducir/pausar, retroceder, avanzar); depende de si el contenido es desplazable o en directo.
- **Cabecera y menú de navegación**: acciones de primer nivel (identificador, modo, emisión) y acciones secundarias agrupadas en el menú de hamburguesa (temporizador, servidores, configuración).
- **Barra inferior**: destinos de contenido por modo, sin Configuración, con una insignia opcional en el destino de cola.
- **Preferencia de potenciador de voz**: estado encendido/apagado recordado por la persona usuaria para la reproducción local de episodios.
- **Descarga de episodio**: transferencia del archivo a la ubicación controlada de Descargas, con estado (en curso, completada, fallida) y porcentaje de avance cuando se conoce el tamaño.
- **Tarjeta de contenido**: representación en cuadrícula de un programa o de un episodio, con portada, título, metadatos, acciones y altura uniforme por fila.
- **Resultado de búsqueda de episodios**: episodio de una suscripción que coincide por título, acompañado de su programa, fecha, duración y estado de escucha/favorito para ofrecer las mismas acciones que en el resto de la app.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las selecciones desde la cola de un episodio con progreso parcial reanudan a menos de 5 segundos de la posición guardada; ninguna empieza desde cero salvo que esté completado o no haya progreso válido.
- **SC-002**: La persona puede retroceder o avanzar un episodio desde la pantalla bloqueada con un solo toque, y el salto coincide con el de la app (15 s atrás / 30 s adelante) en al menos el 95 % de los usos.
- **SC-003**: Ninguna pantalla con cabecera compartida muestra solapamientos ni recortes entre la hamburguesa, el identificador, el selector de modo y el acceso a emisión en móviles pequeños y medianos, con tamaño de letra hasta el 200 %.
- **SC-004**: Todas las funciones retiradas de la cabecera (temporizador, servidores, configuración) se alcanzan en un máximo de 2 toques desde cualquier pantalla con cabecera compartida.
- **SC-005**: La barra inferior muestra todos sus destinos con etiqueta completa en un móvil de ancho habitual, y Configuración ya no ocupa ninguno de esos huecos.
- **SC-006**: El potenciador de voz surte efecto en menos de 1 segundo, se recuerda tras reiniciar la app en el 100 % de los casos y no produce distorsión audible en una muestra de al menos 5 episodios de voz y 2 de música, escuchados con auriculares y altavoz.
- **SC-007**: La insignia de cola coincide con el total de pendientes mostrado en la pantalla de cola en el 100 % de las comprobaciones, se actualiza en menos de 1 segundo y desaparece con la cola vacía.
- **SC-008**: Tras pulsar descargar, el porcentaje es visible en menos de 1 segundo, se actualiza al menos cada 2 segundos, se mantiene dentro de 0–100 % y nunca se inicia una descarga duplicada por pulsaciones repetidas.
- **SC-009**: Las tarjetas muestran el título completo hasta dos líneas con el tamaño de letra por defecto y no presentan solapamientos con las acciones con el tamaño de letra al 200 %.
- **SC-010**: Las mejoras se completan sin cambios en el contrato del servidor ni en el flujo de sesión, y sin regresiones en reproducción, cola, descargas, favoritos y navegación existentes.

## Assumptions

- "Budgie" en la petición se interpreta como **insignia (badge)**; no se introduce ningún elemento gráfico ajeno al lenguaje visual de la app.
- La insignia cuenta **solo episodios pendientes**, excluyendo el que está en reproducción aunque forme parte de la cola (confirmado en la clarificación 2026-11-19), para coincidir con la pantalla de cola; con más de 99 se muestra "99+".
- Los saltos de la notificación son los mismos que en la interfaz (**15 s atrás / 30 s adelante**), tal como pide "como en la interfaz".
- El potenciador de voz se limita a **episodios en reproducción local**; no se aplica a radio en directo ni a Chromecast (decisión de clarificación 2026-11-19).
- El potenciador se ofrece como **interruptor** (no como varios niveles) en esta primera iteración; el ajuste fino de niveles o la eliminación de silencios quedan fuera de alcance.
- "Ambas tarjetas" (decisión de clarificación 2026-11-19): se ajustan la tarjeta de programa y la tarjeta de episodio con el mismo criterio de altura y acciones en fila propia debajo del texto.
- El menú de hamburguesa incluye **Temporizador, Servidores y Configuración**; no se añaden entradas nuevas no pedidas.
- El identificador de la app se mantiene en la cabecera junto a la hamburguesa; si no cabe con el ancho disponible, se reduce hasta el emblema sin perder el acceso a las acciones (decisión de clarificación 2026-11-19).
- Configuración sigue siendo accesible también desde donde ya lo estuviera (por ejemplo, ajustes de servidor) además del menú de hamburguesa; quitarla de la barra inferior no elimina la ruta.
- La reanudación se aplica también cuando el episodio arranca solo al terminar el anterior de la cola, para que el comportamiento sea el mismo se elija a mano o no.
- La descarga sigue usando la transferencia gestionada por el sistema y el indicador de descargas disponibles ya existente; el porcentaje se obtiene de esa misma transferencia.
- No se modifican el contrato del servidor, el modelo de sesión ni las credenciales.
- Los ámbitos de búsqueda siguen el modelo de la web: «Podcasts» filtra en el dispositivo mientras se escribe y «Episodios» consulta al servidor al confirmar; los episodios se buscan por título, no por descripción, notas ni transcripción.
- El servidor ya ofrece la búsqueda de episodios por título limitada a las suscripciones de la persona y la web ya la consume; esta extensión no cambia el contrato ni añade credenciales.
- La búsqueda de episodios se muestra por páginas con una acción de «Cargar más» mientras el servidor indique que hay más; no hay historial de búsquedas, sugerencias ni búsqueda incremental en ese ámbito.
- «Podcasts» filtra sobre la lista de suscripciones ya cargada; si esa lista no se ha podido cargar, se mantienen el error y el reintento existentes.
- Los controles de búsqueda están colapsados por defecto para no ocupar espacio; el botón de búsqueda se sitúa a la izquierda del orden de suscripciones y, al ocultarlos, se limpia el término para no dejar la lista filtrada sin control visible.
- La búsqueda de episodios no filtra por estado de escucha: aparecen pendientes, en curso y completados, con su estado visible.
- La actualización por arrastre se limita a las pantallas **Inicio** y **Suscripciones** del modo Podcast; no se añade a otras listas ni al modo Radio.
- La actualización sigue el modelo de recargar una pestaña del navegador: vuelve a pedir el contenido vigente de la vista actual, sin actualización automática periódica ni notificaciones push.
- La actualización conserva el estado local (modo de vista, orden activo y manual, término y ámbito de búsqueda, reproducción, cola y descargas); solo se sustituyen los datos que provienen del servidor.
- Con una búsqueda de episodios visible, la actualización repite la consulta vigente desde la primera página; las páginas adicionales cargadas se descartan y pueden recuperarse con «Cargar más».
- El gesto no se exige en los estados vacío y de error, donde se mantienen las acciones existentes de descubrir o reintentar; tampoco durante la carga inicial.
- Además del gesto, el contenido con datos ofrece una acción accesible «Actualizar» para que la recarga sea utilizable con lector de pantalla; no añade ningún botón visible ni altera la cabecera.
- En modo lista y en modo tarjetas el gesto se comporta igual y conserva el modo elegido.
- Fuera de alcance de la búsqueda en Suscripciones: buscar dentro de descripciones o notas, la búsqueda global del directorio (que sigue en Descubrir), los filtros por fecha o estado y una ordenación de resultados distinta de la fecha de publicación.
- Fuera de alcance: eliminación de silencios, ecualizador manual, cambio de los intervalos de salto, potenciador en radio o Cast, rediseño completo de la navegación, actualización por arrastre fuera de Inicio y Suscripciones, y cambios en el modo Radio más allá de la barra inferior y la cabecera compartidas.

## Dependencies

- **Spec 0046 — Modo Podcast**: la cola, las tarjetas, el reproductor de episodios, las descargas y la cabecera nacen de esa spec; esta los ajusta.
- **Spec 0042/0044 — Motor y recuperación de reproducción**: la notificación y el estado del reproductor son la base de los controles de transporte.
- **Spec 0035/0037 — Volumen y Cast**: el potenciador de voz debe convivir con el control de volumen y no aplicarse a la sesión remota.
- **API de podcasts del servidor**: la búsqueda de episodios por título dentro de las suscripciones ya existe y la web la consume; esta extensión la incorpora a la app sin cambios de contrato.
- **API de podcasts del servidor (actualización)**: las consultas que alimentan la portada de Inicio y la lista de suscripciones ya existen; la actualización por arrastre las reutiliza sin cambios de contrato.
- **Constitución (v3.1.0)**: la guía de navegación ya reserva la barra inferior a contenido y sitúa Configuración y Servidores en el menú de hamburguesa; la enmienda está aplicada y no se requiere excepción.

## Extensión 2026-11-19 — Limpieza automática de descargas

- **FR-054**: Configuración MUST ofrecer una retención de descargas en días: 7, 14, 30, 60, 90 o «Nunca», con 30 días por defecto.
- **FR-055**: Al abrir una pantalla de Podcasts o al cambiar la retención, el sistema MUST borrar las descargas completadas cuya fecha de finalización sea anterior al umbral configurado.
- **FR-056**: La limpieza MUST NOT borrar la descarga del episodio que está en reproducción.
- **FR-057**: El borrado MUST eliminar el archivo gestionado por Descargas y el registro local, y reflejar el estado «no descargado» en la interfaz.
- **FR-058**: La opción «Nunca» MUST desactivar la limpieza automática.
- **SC-011**: Con retención de 30 días, el 100 % de las descargas completadas hace más de 30 días desaparecen en la siguiente apertura de Podcasts, mientras las recientes y el episodio en curso permanecen.

**Supuestos de la extensión**: la antigüedad se mide desde la finalización de la descarga; la limpieza se ejecuta al abrir las pantallas de Podcasts y al cambiar el ajuste, sin WorkManager (YAGNI); el episodio en curso se conserva aunque supere el umbral.

## Extensión 2026-11-21 — Búsqueda en Suscripciones

Desarrolla la User Story 8. La búsqueda de podcasts se resuelve sobre la lista ya cargada; la de episodios usa la capacidad del servidor que ya consume la web, limitada a las suscripciones de la persona y sin cambios de contrato.

### Functional Requirements

- **FR-059**: Cuando exista al menos una suscripción, la pantalla de Suscripciones MUST mantener los controles de búsqueda ocultos por defecto y MUST ofrecer un botón de búsqueda a la izquierda del orden de suscripciones para mostrarlos u ocultarlos; al ocultarlos MUST limpiarse el término y los resultados y MUST volver la lista completa. Sin suscripciones MUST mostrar solo el estado vacío con la acción de descubrir, sin buscador.
- **FR-060**: En el ámbito Podcasts, el sistema MUST filtrar la lista al escribir por nombre del programa con coincidencia parcial, sin distinguir mayúsculas ni tildes, conservando el orden activo y sin consultar al servidor.
- **FR-061**: En el ámbito Episodios, al confirmar la búsqueda el sistema MUST mostrar solo episodios de las suscripciones de la persona cuyo título contenga el término —incluidos los completados, sin excluir por estado—, con programa, fecha, duración y estado de escucha, ordenados del más reciente al más antiguo.
- **FR-062**: Los resultados de episodios MUST permitir cargar más coincidencias mientras existan, sin repetir ni perder las ya mostradas.
- **FR-063**: Los resultados de episodios MUST permitir abrir el detalle y MUST ofrecer reproducir, favorito, cola y descarga con el mismo comportamiento y estado que en el resto de la app.
- **FR-064**: Mientras haya resultados de episodios, estos MUST sustituir a la lista de suscripciones —sin mostrarse ambas a la vez—; cambiar de ámbito o vaciar el término MUST abandonar los resultados y volver a la lista, y una respuesta de una búsqueda anterior MUST NOT sustituir a la vigente.
- **FR-065**: Una búsqueda de episodios sin coincidencias MUST mostrar un mensaje en español con el término; un error MUST informar de forma accionable, conservar el término y permitir reintentar.
- **FR-066**: El término MUST ignorar espacios sobrantes; un término vacío o compuesto solo por espacios MUST NOT lanzar búsquedas ni mostrar resultados.
- **FR-067**: Tras cancelar una suscripción, sus episodios MUST desaparecer de los resultados y de la lista, sin mostrar datos obsoletos.
- **FR-068**: El buscador MUST ser accesible con lector de pantalla: campo con etiqueta en español, ámbito activo anunciado como seleccionado, resultados actualizados comunicados, áreas táctiles de al menos 48 dp y adaptación al tamaño de letra hasta el 200 % (SC-003) sin solapamientos.
- **FR-069**: La búsqueda MUST NOT requerir cambios en el contrato del servidor ni credenciales nuevas, MUST usar la búsqueda de episodios dentro de las suscripciones que el servidor ya ofrece a la web, y MUST NOT mostrarse en servidores sin podcasts.
- **FR-070**: El término MUST ser único y conservarse al cambiar de ámbito; al volver a Podcasts, la lista MUST filtrarse con ese término sin exigir volver a escribirlo.
- **FR-071**: Con resultados de episodios visibles, Atrás MUST volver a la lista de suscripciones con el ámbito «Podcasts» activo y el término conservado, sin relanzar la búsqueda; solo desde la lista MUST salir de la pantalla. Con el teclado abierto, el primer Atrás MUST limitarse a ocultarlo.

### Success Criteria

- **SC-012**: Con al menos 50 suscripciones, el filtrado por nombre en el ámbito Podcasts responde en menos de 300 ms sin esperar a la red; la primera página de resultados de episodios aparece en menos de 2 s con una conexión habitual.
- **SC-013**: El 100 % de los resultados de episodios pertenece a podcasts suscritos, el 100 % de los términos sin coincidencia muestra un mensaje claro en español y ninguna respuesta tardía sustituye a la búsqueda vigente.
- **SC-014**: La búsqueda completa se puede usar con lector de pantalla y con tamaño de letra al 200 %, sin solapamientos, con etiquetas comprensibles y sin perder el término al cambiar de ámbito, rotar o volver del segundo plano.
- **SC-015**: En el 100 % de las aperturas de Suscripciones los controles de búsqueda están ocultos; un solo toque en el botón de búsqueda los muestra y otro los oculta devolviendo la lista completa, sin dejar filtros activos.

## Extensión 2026-11-21 — Actualización por arrastre en Inicio y Suscripciones

Desarrolla la User Story 9. El gesto sigue el modelo de recargar una pestaña del navegador: vuelve a pedir el contenido vigente de la pantalla sin cambiar de contexto y conservando el estado local de la persona.

### Functional Requirements

- **FR-072**: Las pantallas de Inicio y Suscripciones MUST permitir actualizar el contenido arrastrando hacia abajo y soltando cuando la vista está al principio, tanto en modo lista como en tarjetas, con un umbral que evite activaciones accidentales.
- **FR-073**: Con la vista desplazada, el arrastre MUST desplazar la lista y MUST NOT iniciar la actualización hasta que la vista esté al principio; una vez allí, seguir arrastrando hacia abajo MUST iniciar la actualización.
- **FR-074**: Durante la actualización MUST mostrarse un indicador visible y comprensible; el contenido ya visible MUST permanecer en pantalla, sin sustituirse por una pantalla de carga completa, y el indicador MUST desaparecer al terminar.
- **FR-075**: La actualización de Inicio MUST volver a obtener los datos de sus secciones (continuar escuchando, novedades, programas y resumen de la cola) y reflejar altas, bajas y cambios; la de Suscripciones MUST volver a obtener la lista de suscripciones.
- **FR-076**: La actualización MUST conservar el modo de vista, el orden activo, el orden manual guardado, el término y el ámbito de búsqueda, y MUST NOT reiniciar la reproducción, la cola, las descargas ni el progreso local.
- **FR-077**: Mientras haya una actualización en curso, una nueva solicitud MUST NOT lanzar otra consulta; el indicador MUST mantenerse hasta que termine la vigente y las solicitudes descartadas MUST NOT mostrar errores.
- **FR-078**: Si la actualización falla, MUST conservarse el contenido previo, MUST informarse en español de forma accionable y MUST poder reintentarse; si no había contenido, MUST mantenerse el estado de error existente con su reintento.
- **FR-079**: En Suscripciones con búsqueda activa, la actualización MUST respetar el término y el ámbito: en «Podcasts» MUST recargar la lista y reaplicar el filtro; en «Episodios» MUST repetir la búsqueda vigente desde la primera página, descartar las páginas anteriores y descartar respuestas obsoletas.
- **FR-080**: Los cambios obtenidos MUST aplicarse sin cerrar la pantalla ni mover el foco; si la actualización elimina de la lista el programa o episodio en reproducción, la reproducción MUST continuar sin interrupción y MUST conservar título y posición.
- **FR-081**: Con lector de pantalla activo, el inicio y el fin de la actualización MUST anunciarse, MUST NOT perderse el foco ni la posición de lectura, y, cuando la pantalla tenga contenido, MUST existir una acción accesible «Actualizar» que permita iniciar la recarga sin depender del gesto de arrastre.
- **FR-082**: La actualización MUST NOT requerir cambios en el contrato del servidor ni credenciales nuevas, y MUST usar las consultas que ya alimentan ambas pantallas.

### Success Criteria

- **SC-016**: El 100 % de los arrastres deliberados desde el principio de Inicio o Suscripciones inicia exactamente una actualización; el indicador aparece en menos de 1 segundo y desaparece al terminar, sin duplicar consultas aunque se arrastre repetidamente.
- **SC-017**: Tras una actualización correcta, el 100 % de altas, bajas y cambios del servidor se refleja en un máximo de 3 segundos con una conexión habitual, conservando el modo de vista, el orden, la búsqueda y la reproducción en curso.
- **SC-018**: El 100 % de las actualizaciones fallidas conserva el contenido previo y muestra un mensaje claro en español con reintento, sin pantallas en blanco, pérdida de foco con lector de pantalla ni interrupción de descargas o reproducción.
