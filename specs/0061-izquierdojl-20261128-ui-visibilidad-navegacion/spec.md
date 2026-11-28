# Feature Specification: Visibilidad UI y navegación

**Feature Branch**: `0061-izquierdojl-20261128-ui-visibilidad-navegacion`

**Created**: 2026-11-28

**Status**: Done (2026-11-28; T036 asumida: Cast/reducción de animaciones no verificados)

**Input**: User description: "Mejorar la visibilidad y la navegación de la app, sobre todo en modo oscuro: los elementos no se distinguen bien; separar el panel de reproducción del resto y variar ligeramente su color; reducir el texto del panel (carátula, título pequeño, play/pausa centrado y más grande, saltos a los lados, barra de progreso y tiempos); mejorar gráficamente las tarjetas para que se distingan; hacer la navegación más agradable."

## Clarifications

### Session 2026-11-28

- Q: ¿Dónde debe aparecer el indicador de la emisora en reproducción? → A: En todas las superficies de emisora, incluida Mis emisoras.
- Q: Cuando una emisora ya está sonando, ¿qué debe hacer el botón de reproducir de su tarjeta? → A: Alternar: pausa si está sonando y reanuda si está pausada.
- Q: Cuando la radio está pausada, ¿qué debe mostrar el panel en la línea de estado? → A: "Pausado" al pausar y "En directo" al sonar.
- Q: ¿Dónde debe estar el botón Detener del podcast? → A: En el panel compacto, como acción secundaria.
- Q: ¿Qué proporción debe tener la carátula de las tarjetas? → A: Mantener 16:9, con paridad respecto a la web.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Distinguir las tarjetas y la emisora en reproducción (Priority: P1)

Quien explora el catálogo en modo oscuro ve tarjetas que se funden con el fondo y entre sí. Al añadir una emisora a la reproducción, tampoco puede saber cuál está sonando sin abrir el reproductor. Quiere reconocer de un vistazo la tarjeta, sus datos y el estado de reproducción.

**Why this priority**: Es la causa directa de la queja de visibilidad y afecta a Explorar, Favoritos e Historial, que son las pantallas de uso principal.

**Independent Test**: Abrir Explorar en modo oscuro con varias tarjetas y una emisora sonando. Comprobar que dos tarjetas contiguas se distinguen, que los chips y el texto secundario se leen y que la emisora activa muestra un indicador visible sin abrir el reproductor. Repetir en modo claro.

**Acceptance Scenarios**:

1. **Given** la app en modo oscuro con una cuadrícula de emisoras, **When** la persona mira dos tarjetas contiguas, **Then** cada una tiene una superficie y un borde perceptibles y no se confunden con el fondo.
2. **Given** una emisora en reproducción, **When** la persona recorre Explorar, Favoritos, Historial o Mis emisoras, **Then** la tarjeta o fila de esa emisora muestra un indicador de "en reproducción" que no depende solo del color y cuyo nombre accesible lo anuncia.
3. **Given** una emisora pausada, **When** la persona la busca en una lista o cuadrícula, **Then** distingue el estado "pausada" del estado "sonando".
4. **Given** el modo claro, **When** la persona repite el recorrido, **Then** la jerarquía visual es la misma y el hueco de carátula no aparece como un bloque oscuro.
5. **Given** una emisora con favicon roto o ausente, **When** se muestra su tarjeta, **Then** se ve el emblema de reserva y no un hueco vacío.
6. **Given** una tarjeta de emisora en Explorar o Favoritos, **When** la persona pulsa el botón de reproducir, **Then** empieza la reproducción de esa emisora y no se abre la ficha; si ya es la emisora activa, **Then** el botón alterna entre pausar y reanudar; **When** pulsa el resto de la tarjeta, **Then** se abre su ficha.

---

### User Story 2 - Controlar la reproducción desde un panel compacto (Priority: P1)

La persona escucha una emisora o un episodio y el panel inferior ocupa demasiado y acumula texto. Quiere un panel que se distinga del contenido, que deje claro que es el reproductor y que tenga los controles principales grandes y centrados, sin perder la información de progreso cuando escucha un podcast.

**Why this priority**: El reproductor es el elemento persistente de la app. Su altura y su ruido visual condicionan toda la navegación y son la segunda parte explícita de la petición.

**Independent Test**: Reproducir una emisora y comprobar el panel de radio; después reproducir un episodio y comprobar el panel de podcast. Medir altura, número de acciones y tamaño de los controles, y verificar que error, carga y Cast siguen funcionando.

**Acceptance Scenarios**:

1. **Given** una emisora en reproducción normal, **When** la persona mira el panel, **Then** ve carátula, nombre, estado "En directo" cuando suena o "Pausado" cuando está en pausa, play/pausa y silenciar; no ve la línea técnica de país, idioma, códec o bitrate; hay exactamente dos acciones.
2. **Given** un episodio en reproducción, **When** la persona mira el panel, **Then** ve como máximo dos líneas de texto (título del episodio a una línea y contexto del podcast), un play/pausa centrado y más grande, los saltos a ambos lados, la barra de progreso, los tiempos transcurrido y restante y Detener como acción secundaria.
3. **Given** cualquiera de los dos paneles, **When** la persona lo compara con el contenido y con la barra inferior, **Then** el panel se distingue por su superficie, su borde superior y su forma, y no parece una tarjeta más.
4. **Given** un estado de error, carga o reconexión, **When** la persona mira el panel, **Then** siguen apareciendo el mensaje y las acciones de reintentar, editar servidor o cancelar que correspondan.
5. **Given** un título largo de emisora o episodio, **When** no cabe en el panel, **Then** se recorta con elipsis en una línea y los tiempos y controles siguen visibles.
6. **Given** un episodio sin duración conocida, **When** se muestra el panel, **Then** solo aparece el tiempo transcurrido y no se dibuja una barra de progreso.
7. **Given** una reproducción activa por Cast, **When** la persona mira el panel, **Then** se sigue viendo el nombre del dispositivo y los controles siguen reflejando el estado real.
8. **Given** cualquier botón del panel, **When** la persona lo pulsa, **Then** el área táctil tiene al menos 48 dp, el play/pausa es visualmente mayor que el resto y cada control conserva un nombre accesible que describe su acción.
9. **Given** el panel en reproducción normal, **When** se mide con la fuente por defecto, **Then** no supera 64 dp en radio ni 112 dp en podcast.

---

### User Story 3 - Saber dónde estoy y a dónde voy (Priority: P2)

La barra inferior muestra solo iconos y el estado seleccionado casi no se distingue. La persona quiere leer el nombre de cada sección, reconocer en cuál está y que el cambio entre secciones se sienta fluido.

**Why this priority**: Afecta a la orientación en toda la app y a la facilidad para descubrir secciones como Cola o Suscripciones; es menos crítico que la reproducción, pero se nota en cada navegación.

**Independent Test**: Recorrer las cuatro secciones de radio y las cinco de podcast, comprobar que todas muestran su etiqueta, que la seleccionada se distingue sin ambigüedad y que el cambio de sección tiene una transición suave. Repetir con fuente al 200 %.

**Acceptance Scenarios**:

1. **Given** el modo radio, **When** la persona mira la barra inferior, **Then** ve las etiquetas completas de Explorar, Favoritos, Historial y Mis emisoras.
2. **Given** el modo podcast, **When** la persona mira la barra inferior, **Then** ve las etiquetas completas de Inicio, Descubrir, Suscripciones, Cola y Favoritos.
3. **Given** una sección seleccionada, **When** la persona mira la barra, **Then** la reconoce por el indicador de selección y por el peso o contraste de la etiqueta, no solo por el color.
4. **Given** una fuente al 200 % en una pantalla de 320 dp, **When** la persona mira la barra, **Then** las etiquetas no se solapan ni se pisan y cada destino conserva un nombre accesible completo.
5. **Given** la persona cambia de sección, **When** la transición termina, **Then** el cambio se percibe fluido y, si el sistema tiene activada la reducción de animaciones, el cambio es inmediato.
6. **Given** la barra inferior, **When** la persona compara modos, **Then** los destinos y su orden no han cambiado.
7. **Given** la cabecera superior y la barra inferior, **When** la persona mira el límite con el contenido, **Then** ve un borde o divisor que las separa sin robar altura útil.

---

### User Story 4 - Modo claro con la misma jerarquía (Priority: P3)

La persona que usa el tema claro quiere la misma separación entre tarjetas, panel y navegación que en oscuro, sin bloques oscuros heredados del diseño anterior.

**Why this priority**: La queja principal es el modo oscuro, pero la app ofrece tres modos de tema y la incoherencia entre ellos se percibe como un defecto.

**Independent Test**: Cambiar a modo claro y repetir los recorridos de las historias 1 y 2; comprobar contraste, bordes, placeholder de carátula y panel.

**Acceptance Scenarios**:

1. **Given** el modo claro, **When** la persona mira tarjetas, chips, panel y barra inferior, **Then** la jerarquía y la separación son equivalentes a las del modo oscuro.
2. **Given** una tarjeta sin favicon en modo claro, **When** se muestra, **Then** el hueco de carátula usa un color claro coherente y no un bloque verde oscuro.
3. **Given** el cambio de tema con la app abierta, **When** la persona vuelve a la pantalla, **Then** no quedan superficies mezcladas ni textos ilegibles.

---

### Edge Cases

- **Sin reproducción activa**: el panel no se muestra y ninguna tarjeta ni fila muestra indicador de reproducción.
- **Carga, error o reconexión**: el panel mantiene el mensaje de estado y la acción correspondiente; el indicador de la tarjeta no debe prometer "sonando" mientras está cargando o en error.
- **Cast conectado**: el panel sigue mostrando el dispositivo y el estado remoto; el indicador de la tarjeta refleja el estado efectivo.
- **Títulos largos**: se recortan con elipsis; los tiempos, el play/pausa y los saltos permanecen visibles.
- **Episodio sin duración**: solo tiempo transcurrido, sin barra de progreso ni tiempo restante.
- **Fuente al 200 % y 5 destinos**: las etiquetas pueden elidirse, pero no solaparse; el nombre accesible sigue completo.
- **Favicon roto**: se muestra el emblema de reserva, nunca un hueco vacío.
- **Modo sin conexión con caché**: las tarjetas y el estado de reproducción siguen siendo legibles.
- **Emisora personalizada**: el indicador de reproducción también aparece en Mis emisoras, con el mismo criterio que en el resto de superficies.
- **Radio pausada**: el panel muestra "Pausado" y el botón ofrece reanudar; no sigue anunciando "En directo".
- **Reducción de animaciones del sistema**: las transiciones se desactivan o se acortan sin romper la navegación.
- **Tema oscuro/claro**: ninguna superficie usa un color fijo pensado solo para un tema.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Las tarjetas de emisora MUST tener una separación perceptible respecto al fondo y entre sí en ambos temas, mediante superficie, borde y espaciado, sin cambiar la identidad cromática verde/ocre.
- **FR-002**: Todos los pares texto/fondo usados en cabecera, tarjetas, chips, botones y panel de reproducción MUST cumplir WCAG AA en ambos temas: 4.5:1 para texto normal, 3:1 para texto grande y para controles o estados esenciales (por ejemplo, selección de navegación y play/pausa). Los bordes decorativos de tarjeta y panel se rigen por SC-002 y no cuentan como controles esenciales.
- **FR-003**: La emisora en reproducción MUST poder identificarse en cuadrícula y en lista, en todas las superficies de emisora (Explorar, Favoritos, Historial y Mis emisoras), sin abrir el reproductor, con un indicador que no dependa solo del color y con un nombre accesible que anuncie el estado.
- **FR-004**: El botón de reproducir de una tarjeta o fila MUST iniciar la reproducción de esa emisora y MUST NOT abrir su ficha; si esa emisora ya es la activa, el botón MUST alternar entre pausar y reanudar y reflejar el icono correspondiente. Pulsar el resto de la tarjeta o fila MUST abrir la ficha. Si una lista no ofrece botón de reproducir, la fila mantiene su comportamiento actual.
- **FR-005**: El panel de reproducción MUST diferenciarse del contenido y de la barra inferior mediante superficie, borde superior y forma; MUST NOT parecer otra tarjeta de contenido.
- **FR-006**: Durante la reproducción normal de radio, el panel MUST mostrar únicamente carátula, nombre, estado "En directo" cuando suena o "Pausado" cuando está en pausa, play/pausa y silenciar. Los datos técnicos de país, idioma, códec y bitrate MUST trasladarse a la ficha de la emisora. Los estados de error, carga y reconexión MUST conservar sus mensajes y acciones.
- **FR-007**: Durante la reproducción de un episodio, el panel MUST mostrar como máximo dos líneas de texto, un play/pausa centrado y visualmente mayor, los saltos a ambos lados, la barra de progreso y los tiempos transcurrido y restante. Detener MUST seguir disponible como acción secundaria.
- **FR-008**: Todos los controles del panel MUST tener un área táctil mínima de 48 dp; el play/pausa MUST ser visualmente mayor que los controles secundarios.
- **FR-009**: La altura del panel en reproducción normal MUST ser como máximo 64 dp para radio y 112 dp para podcast, con la fuente por defecto.
- **FR-010**: La barra inferior MUST mostrar la etiqueta completa de cada destino en modo radio y en modo podcast.
- **FR-011**: El destino seleccionado MUST distinguirse por un indicador visible y por una diferencia de peso o contraste en la etiqueta, no solo por color.
- **FR-012**: Las etiquetas de la barra inferior MUST seguir siendo legibles sin solaparse con fuente al 200 % en una pantalla de 320 dp; si se eliden, el nombre accesible MUST conservarse completo.
- **FR-013**: El cambio entre secciones MUST tener una transición suave de 180-220 ms y MUST respetar la preferencia del sistema de reducir animaciones.
- **FR-014**: La cabecera superior y la barra inferior MUST separarse del contenido con un borde o divisor visible, sin añadir altura innecesaria.
- **FR-015**: El modo claro MUST reproducir la misma jerarquía de superficies, bordes y contraste que el modo oscuro, sin fondos fijos pensados solo para oscuro.
- **FR-016**: Una tarjeta con favicon ausente o roto MUST mostrar el emblema de reserva, nunca un hueco vacío o un bloque de color incoherente.
- **FR-017**: La mejora MUST NOT cambiar la reproducción, los datos, el contrato con el servidor, los permisos, los destinos de la barra inferior ni su orden.
- **FR-018**: La mejora MUST conservar los anuncios de accesibilidad existentes (emisora, episodio, estado y controles) y no duplicar etiquetas visibles y accesibles.

### Key Entities

- **Emisora**: nombre, país, idioma, etiquetas, bitrate y estado de reproducción. El estado activo determina el indicador de la tarjeta o fila; los datos técnicos pasan a la ficha.
- **Episodio**: título, podcast de origen, duración y posición. Determina las dos líneas del panel de podcast, el progreso y los tiempos.
- **Estado de reproducción**: inactivo, sonando, pausado, cargando, reconectando o error. Gobierna el contenido y las acciones del panel, y el indicador de la emisora.
- **Preferencia de tema**: sistema, claro u oscuro. Determina la paleta efectiva, sin cambiar la estructura de las pantallas.

No se introducen entidades ni datos nuevos.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de los pares texto/fondo usados en cabecera, tarjetas, chips, botones y panel cumple AA en ambos temas; el 100 % de los controles y estados esenciales (selección de navegación, play/pausa) alcanza 3:1; los bordes decorativos se miden en SC-002.
- **SC-002**: En modo oscuro, la superficie de una tarjeta se separa del fondo al menos 1.3:1; en ambos temas, el borde se separa al menos 1.5:1 respecto a la tarjeta y, en modo claro, la tarjeta añade una elevación visible.
- **SC-003**: El panel mide como máximo 64 dp en radio y 112 dp en podcast; el play/pausa tiene al menos 56 dp y el resto de controles al menos 48 dp de área táctil.
- **SC-004**: En reproducción normal de radio hay exactamente 2 acciones; en podcast hay como máximo 4 (play/pausa, dos saltos y detener).
- **SC-005**: El 100 % de los destinos de la barra inferior muestra su etiqueta completa con fuente por defecto en 360 dp; con fuente al 200 % en 320 dp no hay solapamiento.
- **SC-006**: En el 100 % de las comprobaciones manuales, una persona identifica la emisora en reproducción en cuadrícula y en lista, en cualquiera de las cuatro superficies de emisora, sin abrir el reproductor.
- **SC-007**: En modo claro no aparece ningún fondo oscuro fijo en tarjetas o placeholders, y la jerarquía visual coincide con la de modo oscuro.
- **SC-008**: La suite existente de reproducción, navegación y accesibilidad sigue pasando sin regresiones.
- **SC-009**: En el 100 % de las tarjetas de la emisora activa, el botón muestra el icono de pausa y alterna entre pausar y reanudar sin abrir la ficha.
- **SC-010**: El panel de radio muestra "En directo" solo cuando está sonando y "Pausado" cuando está en pausa, en el 100 % de los estados comprobados.

## Assumptions

- El modo oscuro sigue siendo el principal y el tema por defecto; la paleta mantiene sus tonos verde y ocre y solo se ajustan niveles de luminosidad. No se introduce color dinámico.
- La radio es en directo: no hay saltos, barra de progreso ni tiempos de emisión. El progreso y los tiempos son exclusivos del podcast.
- La línea técnica del panel de radio se traslada a la ficha de la emisora; esta decisión supersede el requisito de segunda línea técnica de la spec `004`.
- Detener se mantiene en el panel de podcast como acción secundaria para no perder funcionalidad existente; su tamaño y posición se subordinan al play/pausa.
- La carátula de las tarjetas mantiene la proporción 16:9 (confirmado en la clarificación 2026-11-28); un cambio a 4:3 queda fuera de alcance.
- La barra inferior conserva los mismos destinos y orden; las etiquetas ya eran un requisito de la spec `0047` y aquí se hacen efectivas. Esta decisión supersede el aspecto de "solo iconos" de la spec `012` en favor de `0047 FR-021`.
- El estado "en reproducción" se deriva del estado de reproducción ya existente; no se añade persistencia ni se consulta al servidor.
- El reproductor sigue siendo persistente al navegar y no se interrumpe por los cambios visuales.
- No se añaden permisos, dependencias, pantallas ni flujos nuevos; el alcance es visual y de interacción.
- Los valores concretos de color se definirán en la fase de diseño técnico; la spec fija los objetivos medibles, no los valores exactos.
