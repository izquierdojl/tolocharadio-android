# Feature Specification: Refinamiento del reproductor y filas de podcast

**Feature Branch**: `0062-izquierdojl-20261128-podcast-player-rows`

**Created**: 2026-11-28

**Status**: Done (2026-11-28; todo verificado en emulador)

**Input**: User description: "En el panel de reproducción inferior, bajar la imagen a la fila de los botones y que ocupe el mismo alto; el texto de encima solo debe verse si el episodio no tiene imagen. Los iconos de atrás/adelante deben ser los mismos que en el detalle. En la lista de episodios, la imagen se ve pequeña y se desperdicia espacio: estilo Pocket Casts, con el play como acción principal a la derecha y el resto de acciones al mantener pulsado."

## Clarifications

### Session 2026-11-28

- Q: ¿En qué listas se aplica el nuevo estilo de fila? → A: En todas las listas de episodios que usan `EpisodeRow` (Inicio, Suscripciones, Ficha, Favoritos y búsqueda).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Mini-player con carátula integrada (Priority: P1)

La persona escucha un episodio y el panel inferior dedica una fila entera al título y al podcast, dejando la carátula pequeña y el panel alto. Quiere que la carátula ocupe el alto de los controles y que el texto desaparezca cuando hay imagen, sin perder tiempos ni progreso.

**Why this priority**: Es el cambio que más reduce la altura del panel y el que pidió explícitamente para el reproductor persistente.

**Independent Test**: Reproducir un episodio con imagen y comprobar que la carátula mide 56 dp, está en la fila de controles, no hay título visible y el panel no supera 72 dp. Repetir con un episodio sin imagen y comprobar que aparece el texto y el panel no supera 112 dp.

**Acceptance Scenarios**:

1. **Given** un episodio con imagen, **When** se muestra el panel, **Then** la carátula de 56 dp está en la fila de controles y no se muestran las líneas de título ni de podcast.
2. **Given** un episodio sin imagen, **When** se muestra el panel, **Then** se mantiene la fila de título/podcast sobre los controles y el panel no supera 112 dp.
3. **Given** el panel con o sin imagen, **When** la persona mira los controles, **Then** play/pausa mide 56 dp, los saltos y Detener 48 dp, y se conservan tiempos, progreso y el nombre accesible del episodio.
4. **Given** el panel, **When** la persona compara los saltos con el detalle del episodio, **Then** ambos usan los mismos iconos (`Undo`/`Redo`).
5. **Given** una URL de imagen rota, **When** se muestra el panel, **Then** se usa una imagen de reserva y no se rompe la altura ni la reproducción.

---

### User Story 2 - Filas de episodio con play principal y acciones al mantener pulsado (Priority: P1)

La persona revisa una lista de episodios y ve imágenes pequeñas y una fila de cinco acciones que ocupa mucho alto. Quiere una fila más limpia, con una imagen mayor y el play a la derecha, conservando descarga, cola, favorito y estado reproducido en un menú al mantener pulsado.

**Why this priority**: Es el cambio visual principal de las listas y afecta a todas las pantallas de podcast.

**Independent Test**: Abrir Favoritos con al menos un episodio; comprobar imagen de 56 dp, play de 48 dp a la derecha y ausencia de la fila de cinco acciones. Mantener pulsada la fila y comprobar que el menú ofrece todas las acciones anteriores.

**Acceptance Scenarios**:

1. **Given** una fila de episodio, **When** la persona la mira, **Then** ve una imagen de 56 dp, título a dos líneas, podcast a una línea, fecha/duración/estado y progreso si existe.
2. **Given** una fila de episodio, **When** la persona pulsa el play de la derecha, **Then** reproduce o pausa el episodio; **When** pulsa el resto de la fila, **Then** abre la ficha.
3. **Given** una fila de episodio, **When** la persona mantiene pulsada la fila, **Then** se abre un menú contextual con descargar/reintentar, añadir a continuación, añadir al final, favorito, marcar reproducido/pendiente y abrir ficha.
4. **Given** una descarga en curso, **When** la persona mira la fila, **Then** ve el progreso o el estado de descarga sin abrir el menú; al terminar, la meta indica “Descargado”.
5. **Given** una acción no disponible (por ejemplo, descargas desactivadas), **When** se abre el menú, **Then** esa acción no aparece y el resto sigue funcionando.
6. **Given** una fila en cualquier lista de episodios, **When** se aplica el nuevo estilo, **Then** el comportamiento es el mismo en Inicio, Suscripciones, Ficha, Favoritos y búsqueda.

---

### User Story 3 - Accesibilidad y consistencia (Priority: P2)

La persona usa TalkBack o mantiene pulsado para descubrir acciones. Quiere que la carátula, el play y el menú tengan nombres claros, y que el cambio no altere la reproducción ni los datos.

**Why this priority**: El nuevo patrón oculta acciones; sin accesibilidad se perdería funcionalidad para quien no ve la pantalla.

**Independent Test**: Activar TalkBack y recorrer una fila y el mini-player; comprobar que se anuncia episodio/podcast, play/pausa, saltos, Detener y “Más acciones”, y que el menú lista cada acción.

- **FR-001**: El panel de podcast MUST mostrar la carátula de 56 dp en la misma fila que los controles cuando el episodio o el podcast tienen imagen; en ese caso MUST NOT mostrar título ni nombre del podcast.
- **FR-002**: Si no hay imagen, el panel MUST mostrar el título a una línea y el podcast a una línea sobre los controles.
- **FR-003**: El panel MUST conservar play/pausa de 56 dp, saltos y Detener de 48 dp, tiempos transcurrido/restante y barra de progreso; los saltos MUST usar los mismos iconos que el detalle (`Undo`/`Redo`).
- **FR-004**: La altura del panel MUST ser como máximo 72 dp con imagen y 112 dp sin imagen, con la fuente por defecto.
- **FR-005**: La carátula del panel MUST tener un nombre accesible con episodio y podcast; tocar el panel MUST abrir el reproductor completo.
- **FR-006**: Las filas de episodio MUST usar una imagen de 56 dp, título a dos líneas, podcast a una línea, meta de fecha/duración/estado y progreso cuando exista.
- **FR-007**: El play/pausa de la fila MUST ser la única acción visible, de 48 dp y alineado a la derecha; pulsar la fila MUST abrir la ficha.
- **FR-008**: La fila MUST NOT mostrar la fila de cinco acciones; una pulsación larga MUST abrir un menú contextual con las acciones disponibles (descargar/reintentar, cola a continuación, cola al final, favorito, reproducido/pendiente y abrir ficha).
- **FR-009**: La fila MUST mostrar el progreso/estado de descarga sin abrir el menú; el menú MUST reflejar el estado de descarga y permitir reintentar si falla.
- **FR-010**: El nuevo estilo MUST aplicarse a todas las listas que usan `EpisodeRow`: Inicio, Suscripciones, Ficha, Favoritos y búsqueda. La tarjeta de cuadrícula `EpisodeCard` queda fuera de alcance.
- **FR-011**: El cambio MUST NOT alterar reproducción, datos, contrato de red, permisos ni dependencias; MUST usar los tokens y componentes existentes.
- **FR-012**: La pulsación larga MUST dar feedback háptico y el menú MUST ser navegable por TalkBack, con etiquetas claras para cada acción.

### Key Entities

No hay entidades nuevas. Se reutilizan `PodcastEpisode`, `PodcastSummary`, `DownloadUiState` y `EpisodePlayerUi`; el cambio es de presentación e interacción.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las filas de episodio muestra una imagen de 56 dp (±1 dp), un play de 48 dp y ninguna fila de cinco acciones.
- **SC-002**: El panel de podcast mide como máximo 72 dp con imagen y 112 dp sin imagen; el play mide 56 dp y los saltos/Detener 48 dp.
- **SC-003**: El 100 % de las acciones que hoy están en la fila sigue disponible en el menú de pulsación larga; no se pierde ninguna.
- **SC-004**: Con imagen no aparece texto de título/podcast en el panel; sin imagen sí aparece.
- **SC-005**: Los iconos de salto del panel coinciden con los del detalle del episodio.
- **SC-006**: La suite existente y los nuevos tests de fila, panel y menú pasan; `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck`, `lintDebug` y `connectedDebugAndroidTest` en verde.

## Assumptions

- Imagen de 56 dp tanto en el panel como en las filas; play de fila 48 dp y play del panel 56 dp.
- El menú contextual será un `ModalBottomSheet` con las acciones existentes; se cierra al elegir una.
- Se conservan tiempos, progreso y Detener en el panel; no se elimina funcionalidad.
- Si la URL de imagen falla, se muestra una imagen de reserva; no se cambia el layout a texto.
- Las descargas desactivadas ocultan la acción de descarga; el resto del menú sigue disponible.
- No se toca la cuadrícula `EpisodeCard` ni el modo radio.
- Este cambio supersede `0061 FR-007/FR-009` para el caso con imagen y el patrón de acciones siempre visibles de `0047/0048` en las filas de episodio.
