# Feature Specification: Tops de podcasts por país y género en Descubrir

**Feature Branch**: `0053-izquierdojl-20261122-podcast-top-charts`

**Created**: 2026-11-22

**Status**: Done (2026-11-22; todo verificado manualmente y con tests automatizados)

**Input**: User description: "En la API principal, se ha incorporado un endpoint top para poder buscar por países, genero, etc... Dale un vistazo a http://www.github.com/izquierdojl/tolocharadio y en la pantalla de buscar, incluimos la opción a seleccionar tops. Mira como está en la web, del repo tolocha radio. Debe ser igual, por pais y genero y los resultados"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Descubrir los podcasts más populares sin escribir nada (Priority: P1)

Como oyente, al abrir la pantalla de Descubrir podcasts con la búsqueda vacía quiero ver el ranking de los programas más populares de un país, para descubrir contenido nuevo y suscribirme sin tener que conocer de antemano ningún nombre.

**Why this priority**: Es el valor central de la funcionalidad y lo que ya ofrece la web: descubrimiento pasivo mediante un ranking listo para consumir. Sin esta historia no hay feature.

**Independent Test**: Con una sesión válida y un servidor que ofrece descubrimiento de Apple, abrir Descubrir sin texto de búsqueda y comprobar que aparece la sección «Populares» con el ranking de España ordenado, numerado y con la acción de suscripción operativa.

**Acceptance Scenarios**:

1. **Given** un usuario con sesión y un servidor que ofrece descubrimiento de Apple, **When** abre Descubrir con la caja de búsqueda vacía, **Then** ve la sección «Populares» con hasta 50 podcasts de España ordenados por posición y numerados.
2. **Given** el ranking ya cargado, **When** el usuario pulsa suscribirse en un programa al que no está suscrito, **Then** la suscripción se crea con el flujo existente y la acción pasa a reflejar el estado suscrito.
3. **Given** un programa del ranking al que el usuario ya está suscrito, **When** se muestra el ranking, **Then** la acción indica que ya está suscrito y no se crea una suscripción duplicada.
4. **Given** una petición de ranking en curso, **When** el usuario interactúa con la búsqueda, el alta por RSS o la importación OPML, **Then** esos controles siguen operativos y el ranking muestra su propio estado de carga.

---

### User Story 2 - Elegir país y género y conservar la preferencia (Priority: P2)

Como oyente quiero cambiar el país y el género del ranking para explorar el contenido popular de mi país o de una categoría concreta, y que la app recuerde mi última elección la próxima vez.

**Why this priority**: El ranking global ya aporta valor; los filtros convierten el descubrimiento en algo relevante para cada usuario y son parte explícita de la paridad con la web.

**Independent Test**: Cambiar el país a México y el género a Comedia, comprobar que el ranking se recarga con esa combinación, cerrar y reabrir la app y verificar que se mantienen ambos selectores.

**Acceptance Scenarios**:

1. **Given** un usuario que abre Descubrir por primera vez, **When** se muestra «Populares», **Then** el país seleccionado es España y el género es «Todas las categorías».
2. **Given** el selector de país, **When** el usuario elige otro país de la lista, **Then** el ranking se recarga para ese país conservando el género activo.
3. **Given** el selector de género, **When** el usuario elige una categoría, **Then** el ranking se recarga con esa categoría y el país activo.
4. **Given** una preferencia guardada de país y género, **When** el usuario vuelve a abrir la app, **Then** «Populares» usa esa combinación sin intervención.
5. **Given** una preferencia local inválida o corrupta, **When** se carga la sección, **Then** se aplican los valores por defecto (España y todas las categorías) sin error visible.
6. **Given** «Populares» visible con una combinación elegida, **When** el usuario busca un término y después borra la búsqueda, **Then** el ranking reaparece con la combinación que tenía seleccionada.

---

### User Story 3 - Degradación elegante y recuperación (Priority: P3)

Como oyente quiero que un fallo del ranking no me impida buscar podcasts ni usar mis suscripciones, y poder reintentar cuando la red o el proveedor fallen.

**Why this priority**: El ranking depende de un proveedor externo y de un endpoint nuevo; la app no puede romperse ni bloquear el resto de Descubrir cuando falle. Es un requisito de robustez, no el valor principal.

**Independent Test**: Simular el fallo recuperable del ranking por categoría y comprobar que se mantiene el ranking general con un aviso y acción de reintento; simular el fallo total y comprobar que aparece un error con reintento; simular credenciales inválidas y comprobar «Editar servidor»; simular un fallo transitorio de capacidad y comprobar que no se oculta la sección; mientras tanto, la búsqueda manual sigue funcionando.

**Acceptance Scenarios**:

1. **Given** el ranking general cargado y el usuario elige una categoría, **When** la petición por categoría falla con un error recuperable, **Then** se muestra el ranking general del país con un aviso discreto de que el filtro por categoría no está disponible, el aviso ofrece una acción «Reintentar» para la categoría y la búsqueda manual sigue operativa.
2. **Given** la sección «Populares», **When** la petición del ranking general falla, **Then** se muestra un estado de error con acción de reintento y no se presenta una lista vacía como si no hubiera resultados.
3. **Given** el usuario pulsa reintentar tras un error, **When** la petición vuelve a completarse correctamente, **Then** el ranking se muestra con la combinación seleccionada.
4. **Given** un servidor con el descubrimiento externo deshabilitado, con un proveedor distinto de Apple o sin la capacidad anunciada, **When** el usuario abre Descubrir, **Then** la sección «Populares» no se muestra y la búsqueda, el alta por RSS y el OPML funcionan con normalidad.
5. **Given** una combinación país/género sin resultados, **When** termina la carga, **Then** se muestra un mensaje explícito de que no hay ranking para esa selección, distinto del «sin resultados» de la búsqueda.
6. **Given** el usuario cambia de país o género varias veces seguidas, **When** las respuestas llegan en distinto orden, **Then** solo se muestra el ranking de la última combinación seleccionada.
7. **Given** el dispositivo sin conectividad, **When** se intenta cargar el ranking, **Then** la app muestra un error controlado con reintento y no se produce ningún cierre inesperado.
8. **Given** una sesión que no puede renovarse, **When** la carga del ranking devuelve un error de credenciales, **Then** la sección muestra el mensaje de credenciales y una acción «Editar servidor» que abre la edición del servidor activo, sin ofrecer un reintento inútil.
9. **Given** una comprobación de capacidad que falla de forma transitoria (red o 503), **When** el usuario abre Descubrir, **Then** la sección muestra un error con reintento y no se oculta como si el servidor no ofreciera el ranking.

---

### Edge Cases

- Títulos o autores muy largos: se recortan con elipsis sin ocultar ni desbordar la acción de suscripción.
- Programa sin autor, sin portada o sin categorías: la fila se muestra con los campos disponibles y sin huecos visuales extraños.
- Programa sin `feedUrl` válido: el servidor lo omite y la app no muestra elementos fantasma.
- Ranking con menos de 50 elementos o vacío: la numeración es consecutiva desde 1 y la sección comunica el caso vacío.
- Cambio de pestaña o app en segundo plano durante la carga: al volver no hay crash ni estado mezclado con una selección anterior.
- Sesión caducada durante la carga: se aplica la renovación automática existente; si no es posible, se muestra el mensaje de credenciales con una acción «Editar servidor» que abre la edición del servidor activo.
- Servidor antiguo que no publica la capacidad de descubrimiento: la sección se oculta, sin errores ni avisos técnicos.
- La comprobación de capacidad falla de forma transitoria (sin red o 503): la sección muestra un error con reintento; no se oculta como si el servidor no ofreciera el ranking.
- Respuesta 400, 403 o 503 del endpoint: se traduce a un mensaje en español comprensible y, cuando aplica, se ofrece reintento o «Editar servidor» si es un error de credenciales.
- Texto de búsqueda compuesto solo por espacios: se considera búsqueda vacía y se mantiene el ranking.
- Tamaño de letra grande, modo oscuro o lector de pantalla: selectores, posiciones, títulos y estados de suscripción siguen siendo legibles y anunciados.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La pantalla de Descubrir podcasts MUST mostrar una sección «Populares» cuando la caja de búsqueda esté vacía y el servidor activo ofrezca descubrimiento de podcasts de Apple.
- **FR-002**: En la primera apertura, la sección MUST usar España como país y «Todas las categorías» como género, y MUST solicitar hasta 50 elementos ordenados por posición.
- **FR-003**: Cada elemento del ranking MUST mostrar su posición, portada, título, autor y categorías disponibles, reutilizando la presentación y la acción de suscripción ya existentes en la búsqueda de podcasts.
- **FR-004**: La acción de suscripción MUST crear la suscripción mediante el flujo existente, MUST reflejar el estado suscrito/no suscrito y MUST evitar duplicados.
- **FR-005**: El usuario MUST poder elegir entre 13 países, en este orden: España, México, Argentina, Colombia, Chile, Perú, Estados Unidos, Reino Unido, Francia, Alemania, Italia, Portugal y Brasil.
- **FR-006**: El usuario MUST poder elegir «Todas las categorías» o una de las 18 categorías de Apple: Artes, Negocios, Comedia, Educación, Ficción, Historia, Salud y bienestar, Niños y familia, Ocio, Música, Noticias, Religión y espiritualidad, Ciencia, Sociedad y cultura, Deportes, Tecnología, Crímenes reales, TV y cine.
- **FR-007**: Cambiar el país o el género MUST solicitar un nuevo ranking para la combinación resultante, conservando el otro selector y mostrando un estado de carga propio de la sección.
- **FR-008**: La app MUST recordar localmente el país y el género elegidos entre sesiones; si el valor guardado no pertenece a las listas admitidas, MUST volver a los valores por defecto sin mostrar errores.
- **FR-009**: Al enviar una búsqueda con texto, la sección de ranking MUST dar paso a los resultados de búsqueda; al vaciar la búsqueda, el ranking MUST reaparecer con la última combinación elegida.
- **FR-010**: Si falla la petición de una categoría concreta con un error recuperable, la app MUST mantener visible el ranking general del país seleccionado, MUST informar de forma discreta de que el filtro por categoría no está disponible y MUST ofrecer una acción visible para reintentar la categoría. Los errores de credenciales y el 404 se tratan según FR-017 y la ocultación por capacidad no soportada.
- **FR-011**: Si falla la petición del ranking general, la app MUST mostrar un estado de error con acción de reintentar y MUST NOT presentar el fallo como una lista sin resultados.
- **FR-012**: La búsqueda manual, el alta por URL RSS, la importación y la exportación OPML MUST seguir operativas con independencia del estado del ranking.
- **FR-013**: Si el servidor no ofrece descubrimiento de Apple (deshabilitado, otro proveedor o capacidad ausente), la sección «Populares» MUST ocultarse y la app MUST NOT solicitar rankings. Si la comprobación de capacidad falla de forma transitoria, la app MUST mostrar un error con reintento y MUST NOT ocultar la sección como si no estuviera soportada.
- **FR-014**: Si la combinación seleccionada devuelve un ranking vacío, la app MUST mostrar un mensaje específico para esa situación.
- **FR-015**: Si el usuario cambia de selección mientras hay peticiones en curso, la app MUST descartar las respuestas obsoletas y mostrar únicamente el resultado de la última combinación elegida.
- **FR-016**: La app MUST obtener el ranking del endpoint autenticado `GET /api/v1/podcasts/top` con los parámetros `country` (código ISO de dos letras, por defecto `es`), `genre` (identificador numérico opcional) y `limit` (1–100, por defecto 50), y MUST interpretar la respuesta con el mismo formato de resultado que la búsqueda de podcasts.
- **FR-017**: La app MUST traducir los errores del endpoint (validación, sesión, podcasts deshabilitados, proveedor no soportado y proveedor no disponible) a mensajes en español acordes al tratamiento de errores existente, sin exponer detalles técnicos. Ante un error de credenciales, la acción principal MUST abrir la edición del servidor activo; ante un error de red, MUST reintentar la carga.
- **FR-018**: Los selectores y la lista MUST ser accesibles: etiquetas comprensibles, posiciones anunciadas, estados de carga y error comunicados, y operables con lector de pantalla y tamaños de letra grandes.
- **FR-019**: El ranking MUST cargarse en una sola petición de hasta 50 elementos, sin paginación ni desplazamiento infinito, en paridad con la web.
- **FR-020**: La carga del ranking MUST NOT bloquear ni retrasar la renderización de los controles de búsqueda, RSS y OPML de la pantalla.

### Key Entities *(include if feature involves data)*

- **Ranking de podcasts**: lista ordenada de programas populares para una combinación de país y género (o género general), con una posición implícita según el orden recibido.
- **Resultado de ranking**: programa descubierto con título, autor, URL de feed, portada, categorías y descripción; misma forma que un resultado de búsqueda y sin identificador de servidor hasta que se suscribe.
- **Preferencia de ranking**: país y género elegidos por el usuario, persistidos localmente y validados contra las listas admitidas.
- **Catálogo de filtros**: lista fija de países y categorías de Apple compartida con la web, con España y «Todas las categorías» como valores por defecto.
- **Capacidad del servidor**: indicación pública de si la instancia ofrece descubrimiento de podcasts y con qué proveedor; determina la visibilidad de la sección.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un usuario con sesión ve la sección «Populares» con datos o un estado de carga en menos de 2 segundos tras abrir Descubrir, medido en Wi-Fi o 4G con latencia estable y registrando el tiempo hasta el primer estado visible.
- **SC-002**: Al cambiar de país o género, el 95 % de las cargas muestra el ranking actualizado en menos de 3 segundos, medido en una serie de 20 cambios con red estable.
- **SC-003**: El 100 % de los usuarios conserva su país y género elegidos tras cerrar y reabrir la app.
- **SC-004**: Ante un fallo recuperable del filtro por categoría, el 100 % de los casos mantiene visible el ranking general, la búsqueda manual operativa y una acción para reintentar la categoría.
- **SC-005**: Suscribirse a un programa desde el ranking requiere como máximo 2 interacciones desde que la sección es visible y el estado suscrito se refleja en menos de 1 segundo tras la confirmación, medido con el ranking ya cargado.
- **SC-006**: El 100 % de los flujos existentes de búsqueda, alta por RSS y OPML siguen funcionando sin cambios de comportamiento.
- **SC-007**: Cero cierres inesperados o bloqueos de interfaz atribuibles al ranking en la matriz de pruebas (red lenta, sin red, error 400/401/403/503, lista vacía y cambios rápidos de filtro), incluyendo la apertura de «Editar servidor» ante credenciales inválidas.
- **SC-008**: En una prueba de usabilidad con 5 personas que abren Descubrir por primera vez, al menos 4 encuentran el ranking y realizan una acción con él (desplazarse, cambiar un filtro o suscribirse) sin ayuda.

## Assumptions

- La funcionalidad aplica a la pantalla **Descubrir podcasts**, no a la búsqueda de emisoras de radio: el endpoint incorporado en la API principal es `GET /api/v1/podcasts/top` y la web lo expone en su pantalla de búsqueda de podcasts.
- El backend con el endpoint `/podcasts/top` (rama `feat/podcast-top-charts`, PR #54 del repositorio web) estará desplegado antes de validar la feature; sin él, la app se comporta como servidor sin capacidad de descubrimiento y oculta la sección.
- La respuesta del endpoint reutiliza el mismo formato normalizado que `GET /api/v1/podcasts/search` (`title`, `author`, `feedUrl`, `imageUrl`, `categories`, `description`) y devuelve los elementos en orden de posición.
- El servidor cachea los rankings durante horas; la app no necesita refresco periódico ni histórico de posiciones. Cada apertura de la pantalla o cambio de filtro puede solicitar el ranking actual.
- El límite de 50 elementos es suficiente para el caso de uso y coincide con la web; no se requiere paginación, scroll infinito ni carga incremental.
- Los selectores usan listas fijas en el cliente (13 países y 18 categorías) copiadas de la web, en lugar de consultar catálogos al servidor.
- La suscripción, la sesión automática y el tratamiento de errores de credenciales ya existen y se reutilizan sin cambios de contrato.
- La interfaz se escribe en español, con el estilo visual y los componentes Material 3 existentes.
- La preferencia de país y género es local al dispositivo; no se sincroniza entre dispositivos ni requiere cambios en el perfil del servidor.
- Una selección de género fallida por un error recuperable se degrada al ranking general del país, igual que en la web; 401/403/404 se tratan con el flujo de credenciales o la ocultación de la sección. No se sirven datos caducados de una combinación distinta.
- No se añaden dependencias nuevas ni cambios de esquema de base de datos para esta feature.
