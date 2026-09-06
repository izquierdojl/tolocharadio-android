# Feature Specification: Emisoras personalizadas — Mis emisoras

**Feature Branch**: `006-custom-stations`

**Created**: 2026-09-06

**Status**: Done (2026-09-06; todo verificado en emulador por el usuario)

**Input**: User description: "Feature: Incorporar opciones de emisoras personalizadasl endpoint proporcionado por el api de https://github.com/izquierdojl/tolocharadio. Hay que realizar navegación, visualización de historial, navegación, adición de las mismas. Las mismas opciones que hay en la app web del repositorio indicado."

**Fuente de verdad del contrato**: backend `izquierdojl/tolocharadio`, endpoints autenticados `GET /custom-stations` (lista `{items: Station[]}`), `POST /custom-stations` (`{name (1–256), url HTTP(S)}` → `{station}`), `DELETE /custom-stations/:id` (`{ok:true}`). Modelo `Station` con `isCustom=true`, sin `favicon` propio (la web muestra el emblema de TolochaRadio). Reproducción por proxy autenticado `GET /playback/:stationId` igual que el catálogo. Errores con formato `{error:{code,message,status,details?}}`. Paridad web: página `CustomStations.tsx` en ruta `/mis-emisoras` (exige sesión), con formulario Nombre + URL del stream + botón Añadir, lista con toggle vista tarjetas/lista, borrado individual por emisora, estados de carga/vacío/error con reintento y avisos de éxito/error.

**Alcance previo**: las specs `003-favorites-management` y `005-history-management` dejaron `Mis emisoras` como sección pendiente de la paridad de navegación exigida por la constitución (`/, /explorar, /favoritos, /historial, /mis-emisoras, /perfil`). Esta spec completa ese pendiente: sección Mis emisoras con paridad web.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver mis emisoras personalizadas (Priority: P1)

Una persona con cuenta entra en la sección Mis emisoras y ve todas las emisoras que ha añadido manualmente, con su nombre y la URL del stream, cada una reproducible y eliminable. Si aún no ha añadido ninguna, ve un mensaje amable que le explica cómo añadir la primera desde el formulario de la misma pantalla.

**Why this priority**: es el núcleo del pedido — sin una pantalla de Mis emisoras no hay "visualización" ni "navegación". Entrega valor por sí sola.

**Independent Test**: iniciando sesión con una cuenta que tiene 3 emisoras personalizadas se abre Mis emisoras y se ven las 3; con una cuenta nueva se ve el estado vacío con instrucciones para añadir.

**Acceptance Scenarios**:

1. **Given** una cuenta con 3 emisoras personalizadas, **When** abre Mis emisoras, **Then** ve las 3 con nombre y emblema de TolochaRadio (sin imagen propia), en el mismo orden que la web.
2. **Given** una cuenta sin emisoras personalizadas, **When** abre Mis emisoras, **Then** ve un estado vacío ("Aún no tienes emisoras personalizadas") que explica que debe usar el formulario superior con nombre y URL del stream.
3. **Given** sesión caducada al abrir Mis emisoras, **When** se intenta cargar la lista, **Then** la sesión se renueva de forma transparente y la lista aparece sin pedir login; si la renovación falla, voy a Login con aviso.
4. **Given** error de red o del servidor al cargar, **When** falla la carga, **Then** veo un mensaje en español con botón de reintento y nunca una pantalla en blanco.

---

### User Story 2 - Añadir una emisora personalizada (Priority: P1)

Una persona escribe el nombre de una emisora que no está en el catálogo y la URL de su stream en el formulario superior y pulsa Añadir. La emisora aparece en su lista al instante con confirmación, y el formulario se limpia para añadir la siguiente.

**Why this priority**: la "adición de las mismas" pedida por el usuario — sin esto la sección es solo lectura y no hay paridad con la web.

**Independent Test**: rellenar nombre + URL válida, pulsar Añadir y comprobar que la emisora aparece en la lista y que al recargar sigue ahí.

**Acceptance Scenarios**:

1. **Given** la pantalla Mis emisoras con el formulario vacío, **When** escribo nombre "Radio Sierra" y URL "https://stream.ejemplo.org/live.mp3" y pulso Añadir, **Then** la emisora aparece en la lista al instante, el formulario se vacía y veo confirmación ("Emisora personalizada añadida").
2. **Given** el formulario con nombre vacío, **When** pulso Añadir, **Then** veo un aviso en español ("Escribe un nombre para la emisora") y no se envía nada al servidor.
3. **Given** el formulario con URL "no-es-una-url" o "ftp://emisora/live", **When** pulso Añadir, **Then** veo un aviso en español ("La URL del stream no es válida" / "La URL debe empezar por http:// o https://") y no se envía nada al servidor.
4. **Given** fallo del servidor al añadir (p. ej. URL rechazada con 422 y detalles por campo), **When** el servidor rechaza la operación, **Then** la lista no cambia y veo un mensaje breve en español con el motivo.
5. **Given** que pulso Añadir dos veces seguidas rápido, **When** la primera petición sigue en curso, **Then** el botón queda deshabilitado hasta recibir respuesta (sin duplicados).

---

### User Story 3 - Escuchar una emisora personalizada (Priority: P1)

Una persona toca una de sus emisoras personalizadas y la escucha inmediatamente en el reproductor, igual que cualquier emisora del catálogo. Puede seguir navegando por otras secciones sin que la música se corte.

**Why this priority**: una emisora que no suena no sirve; la web las reproduce "desde el reproductor" y el proxy las registra en el historial como una emisora normal.

**Independent Test**: reproducir una emisora personalizada desde Mis emisoras y comprobar que suena, que el mini-reproductor aparece y que sigue sonando al ir a Explorar.

**Acceptance Scenarios**:

1. **Given** una emisora personalizada en mi lista, **When** la pulso para reproducir, **Then** suena a través del proxy autenticado y el mini-reproductor aparece con su nombre.
2. **Given** una emisora personalizada reproduciéndose, **When** voy a Explorar o Historial, **Then** la música no se corta y el mini-reproductor sigue visible.
3. **Given** una emisora personalizada cuyo stream está caído, **When** intento reproducirla, **Then** veo un mensaje accionable en español con reintento, sin pantalla negra ni bloqueo (igual que el catálogo).

---

### User Story 4 - Eliminar una emisora personalizada (Priority: P2)

Una persona puede quitar una de sus emisoras personalizadas con el botón de papelera. La emisora desaparece de la lista al instante con confirmación.

**Why this priority**: la web ofrece borrado individual (`DELETE /custom-stations/:id`); es control básico sobre datos propios, pero no bloquea ver/añadir/escuchar.

**Independent Test**: eliminar una emisora personalizada y comprobar que desaparece de la lista y que al recargar sigue sin aparecer.

**Acceptance Scenarios**:

1. **Given** una emisora personalizada en mi lista, **When** pulso el botón de eliminar, **Then** la emisora desaparece de la lista al instante y veo confirmación ("Emisora eliminada").
2. **Given** fallo de red al eliminar, **When** el servidor rechaza la operación, **Then** la emisora vuelve a aparecer en la lista y veo un mensaje breve en español.
3. **Given** una emisora personalizada que era favorita, **When** la elimino, **Then** deja de aparecer también en Favoritos (invalidación cruzada como en la web).

---

### User Story 5 - Llegar a Mis emisoras navegando como en la web (Priority: P2)

Una persona llega a Mis emisoras desde la barra inferior de navegación, igual que a Favoritos o Historial. Si no ha iniciado sesión e intenta entrar, se le pide login como en el resto de secciones privadas.

**Why this priority**: cierra la "navegación" pedida por el usuario y la paridad con la web (`/mis-emisoras` exige cuenta).

**Independent Test**: con y sin sesión, recorrer barra inferior → Mis emisoras → reproducir → navegar a otra sección comprobando destino y continuidad del audio.

**Acceptance Scenarios**:

1. **Given** sesión iniciada, **When** pulso "Mis emisoras" en la barra inferior, **Then** llego a mi lista en menos de 2 segundos con red normal.
2. **Given** sin sesión, **When** intento abrir Mis emisoras, **Then** se me lleva a Login (igual que Favoritos, Historial o Perfil).
3. **Given** una emisora personalizada reproduciéndose, **When** navego entre las 5 secciones, **Then** la música no se corta en ningún cambio.

---

### Edge Cases

- ¿Qué pasa si el nombre supera 256 caracteres o la URL no es http/https? El cliente lo bloquea antes de enviar (mismo mensaje que la web); si el servidor responde 422 con `details` por campo, se muestra el detalle del campo correspondiente en español.
- ¿Qué ocurre si el usuario añade dos emisoras con la misma URL? Se permiten (el contrato no declara unicidad); se muestran como dos entradas distintas.
- ¿Qué pasa si se elimina la emisora que está sonando? La reproducción continúa hasta que el usuario la detenga o cambie de emisora; la lista ya no la muestra.
- ¿Cómo se ven las personalizadas en Favoritos e Historial? Como una emisora normal (nombre + emblema TolochaRadio); al eliminarla desaparece de Mis emisoras y de Favoritos.
- ¿Qué pasa sin red al abrir Mis emisoras si ya se abrió antes con red? Se muestra la última lista conocida marcada como offline con botón de reintentar.
- ¿Qué pasa si la URL del stream cambia o muere? La entrada sigue en la lista; al reproducir se muestra error accionable con reintento, igual que una emisora caída del catálogo.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Las personas con cuenta MUST poder abrir una sección Mis emisoras que muestre su lista de emisoras personalizadas con nombre y emblema de TolochaRadio (sin favicon propio), con las mismas acciones de reproducir y eliminar que en la web.
- **FR-002**: Sin emisoras personalizadas, Mis emisoras MUST mostrar un estado vacío en español que explique cómo añadir la primera (nombre + URL del stream) desde el formulario de la misma pantalla; nunca una pantalla en blanco.
- **FR-003**: El sistema MUST ofrecer en la parte superior de Mis emisoras un formulario con campo Nombre, campo URL del stream y botón Añadir, siempre visible aunque la lista esté vacía o falle la carga.
- **FR-004**: El formulario MUST validar en cliente antes de enviar: nombre obligatorio no vacío (tras recortar espacios) y URL válida con protocolo `http:` o `https:`; ante dato inválido MUST mostrar aviso en español junto al formulario sin llamar al servidor.
- **FR-005**: Al añadir con datos válidos, el sistema MUST enviar `POST /custom-stations {name, url}` (valores recortados); ante éxito MUST añadir la emisora a la lista al instante, vaciar el formulario y mostrar confirmación en español.
- **FR-006**: El sistema MUST permitir reproducir una emisora personalizada con un solo toque vía proxy autenticado, con mini-reproductor persistente y continuidad al navegar entre secciones; ante stream no disponible MUST mostrar error accionable con reintento.
- **FR-007**: El sistema MUST permitir eliminar una emisora personalizada (`DELETE /custom-stations/:id`) con reflejo inmediato en la lista, confirmación en español y reversión ante error; tras eliminar MUST refrescar Favoritos porque una personalizada puede ser favorita.
- **FR-008**: Mis emisoras MUST exigir sesión como el resto de secciones privadas: sin sesión redirige a Login; con sesión caducada intenta renovación transparente una vez antes de pedir login.
- **FR-009**: Mis emisoras MUST ser alcanzable desde la barra inferior de navegación con paridad web (`/mis-emisoras`), y cada emisora MUST permitir reproducción sin interrumpir la navegación (reproductor persistente).
- **FR-010**: Todos los errores del servidor con formato `{error:{code,message,status,details?}}` MUST traducirse a mensajes en español orientados a la acción, mostrando el detalle por campo cuando el servidor devuelva 422; PROHIBIDO mostrar texto técnico crudo.
- **FR-011**: Sin red y con una visita previa, Mis emisoras MUST mostrar la última lista conocida marcada como offline y MUST permitir reintento manual; en primer arranque sin caché MUST mostrar error con reintento.
- **FR-012**: Durante el envío del formulario y durante un borrado, los botones correspondientes MUST quedar deshabilitados hasta recibir respuesta para evitar duplicados y dobles borrados.

### Key Entities

- **CustomStation**: emisora creada por una persona (`id`, `name` 1–256, `url` del stream http/https, `isCustom=true`); pertenece a una sola cuenta, sin favicon propio (se muestra emblema TolochaRadio), reproducible por proxy y marcable como favorita.
- **Station**: emisora tal como la devuelve el contrato (`id`, `name`, `url`, `homepage`, `favicon`, `country`, `language`, `tags`, entre otros); una personalizada es un `Station` con `isCustom=true` y aparece igual en Historial y Favoritos.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Una persona con cuenta abre Mis emisoras y reconoce su lista completa en menos de 2 segundos con red normal.
- **SC-002**: 9 de cada 10 personas añaden una emisora personalizada (nombre + URL válida) al primer intento sin ayuda.
- **SC-003**: El 100 % de los intentos de añadir con nombre vacío o URL no http(s) se bloquean en cliente con aviso en español, sin llamada al servidor.
- **SC-004**: Reproducir una emisora personalizada y navegar por 3 secciones no interrumpe el audio en el 100 % de las pruebas con red normal.
- **SC-005**: La eliminación de una emisora personalizada se refleja en la lista en menos de 2 segundos tras la acción, y ante fallo de red la lista se corrige sola (reversión) en menos de 2 segundos tras el error.
- **SC-006**: Una persona sin sesión que intenta abrir Mis emisoras llega a Login en el 100 % de los casos, igual que con Favoritos o Historial.

## Assumptions

- La instancia del usuario implementa el contrato de emisoras personalizadas visto en `izquierdojl/tolocharadio` (`GET/POST /custom-stations`, `DELETE /custom-stations/:id`); si su versión difiere, se declara en notas de release.
- El usuario de esta spec ya tiene cuenta y sesión (el login/registro lo cubre la spec 001); Mis emisoras exige sesión como en la web (`RequireAuth` en `/mis-emisoras`).
- La lista de personalizadas por usuario es pequeña (decenas como máximo): se carga completa sin paginación; el contrato devuelve `{items}` sin `hasMore`.
- La caché offline de personalizadas es solo de lectura (última lista conocida); la fuente de verdad al abrir con red es siempre el servidor.
- Escuchar una personalizada vía proxy registra historial en servidor igual que el catálogo; la personalizada puede aparecer en Historial y puede marcarse como favorita (por eso el borrado invalida Favoritos, como hace la web).
- La web no ofrece edición de personalizadas (solo añadir/eliminar): queda fuera de alcance; tampoco hay limpieza completa de personalizadas (a diferencia del historial).
- El toggle de vista lista/tarjetas sigue la decisión global de UX (spec 005: solo lista por ahora; el toggle se aplicará en spec aparte para toda la app); esta spec exige al menos vista de lista con reproducir + eliminar por elemento.
