# Feature Specification: Historial — lista, reproducción y gestión

**Feature Branch**: `005-history-management`

**Created**: 2026-09-06

**Status**: Done (2026-09-06; todo verificado en emulador por el usuario)

**Input**: User description: "Incorporar opciones de historial a través del endpoint /history proporcionado por el api de izquierdojl/tolocharadio. Navegación, visualización de historial, reproducción desde el mismo. Las mismas opciones que hay en la app web del repositorio indicado."

**Fuente de verdad del contrato**: backend `izquierdojl/tolocharadio`, endpoints autenticados `GET /history` (lista ordenada por más reciente), `DELETE /history` (limpiar todo), `DELETE /history/:stationId` (quitar una emisora). Modelo `HistoryEntry { station, playedAt }` (playedAt = timestamp Unix en milisegundos). Errores con formato `{error:{code,message,status,details?}}`.

**Alcance previo**: la spec `001-auth-explore-base` (FR-009) dejó la pantalla completa de Historial fuera de alcance — solo mencionó que el historial se registra al reproducir vía proxy. La spec `003-favorites-management` (FR-011) confirmó que Historial queda en su propia spec. Esta spec completa ese pendiente: pantalla de Historial con paridad web (`/historial`).

## Clarifications

### Session 2026-09-06

- Q: ¿Debe Historial soportar vista de tarjetas y lista con toggle, como la web y Favoritos? → A: Solo lista por ahora (como están hechos los favoritos actualmente); el toggle de vista se aplicará en una spec aparte para toda la app.
- Q: ¿Qué patrón de confirmación usar al pulsar "Limpiar" para borrar todo el historial? → A: Diálogo de confirmación antes de borrar (AlertDialog con "¿Limpiar todo el historial?" + Cancelar/Limpiar).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver mi historial de reproducción (Priority: P1)

Una persona con cuenta entra en la sección Historial y ve todas las emisoras que ha escuchado recientemente, con su imagen, nombre, país/idioma y la hora relativa de la última reproducción (p. ej. "hace 2 días"). Las emisora duplicadas se agrupan mostrando solo la escucha más reciente. Si aún no ha escuchado nada, ve un mensaje amable que le invita a explorar el catálogo.

**Why this priority**: es el núcleo del pedido — sin una pantalla de Historial no hay "opciones de historial". Entrega valor por sí sola.

**Independent Test**: iniciando sesión con una cuenta que tiene historial se puede abrir Historial y ver la lista completa; con una cuenta nueva se ve el estado vacío con enlace a Explorar.

**Acceptance Scenarios**:

1. **Given** una cuenta con 5 reproducciones registradas (2 de la misma emisora), **When** abre Historial, **Then** ve 4 emisoras únicas con imagen, nombre, país/idioma y hora relativa, ordenadas de más reciente a más antigua.
2. **Given** una cuenta sin historial, **When** abre Historial, **Then** ve un estado vacío ("Todavía no has escuchado nada") con un botón que lleva a Explorar.
3. **Given** sesión caducada al abrir Historial, **When** se intenta cargar la lista, **Then** la sesión se renueva de forma transparente y la lista aparece sin pedir login; si la renovación falla, voy a Login con aviso.
4. **Given** error de red o del servidor al cargar, **When** falla la carga, **Then** veo un mensaje en español con botón de reintento y nunca una pantalla en blanco.

---

### User Story 2 - Reproducir una emisora desde el historial (Priority: P1)

Una persona puede tocar una emisora de su historial para reproducirla inmediatamente. Al hacerlo, la emisora se reproduce y el historial se actualiza automáticamente situando esa emisora en primera posición, sin interrumpir la reproducción ni necesidad de recargar manualmente.

**Why this priority**: el "reproducción desde el mismo" pedido por el usuario — sin esto el historial es solo una lista estática sin utilidad práctica.

**Independent Test**: reproducir una emisora desde Historial y comprobar que suena, que el mini-reproductor aparece y que la lista se reordena al instante.

**Acceptance Scenarios**:

1. **Given** una emisora en tercera posición del historial, **When** pulso reproducir, **Then** suena, el mini-reproductor aparece y la emisora pasa a la primera posición de la lista al instante.
2. **Given** estoy reproduciendo desde Historial, **When** la lista se actualiza automáticamente, **Then** la reproducción continúa sin interrupción mientras la lista se reordena.
3. **Given** una emisora en el historial, **When** pulso su título o imagen, **Then** se abre su ficha con los mismos datos y acciones que en Explorar (paridad con la web).

---

### User Story 3 - Eliminar emisoras individuales del historial (Priority: P2)

Una persona puede quitar una emisora concreta de su historial con un solo toque en el botón de papelera. La emisora desaparece de la lista al instante y el cambio se refleja en el servidor.

**Why this priority**: la web ofrece eliminación individual (`DELETE /history/:stationId`); es control de privacidad básico, pero no bloquea ver/reproducir.

**Independent Test**: eliminar una emisora del historial y comprobar que desaparece de la lista y que al recargar sigue sin aparecer.

**Acceptance Scenarios**:

1. **Given** una emisora en el historial, **When** pulso el botón de eliminar, **Then** la emisora desaparece de la lista al instante y se envía `DELETE /history/:stationId` al servidor.
2. **Given** fallo de red al eliminar, **When** el servidor rechaza la operación, **Then** la emisora vuelve a aparecer en la lista y veo un mensaje breve en español.
3. **Given** una emisora que ya no existe en el servidor (404), **When** intento eliminarla, **Then** se trata como éxito (la emisora desaparece localmente).

---

### User Story 4 - Limpiar todo el historial (Priority: P2)

Una persona puede borrar todo su historial de reproducción de una sola vez con un botón "Limpiar". La lista se vacía inmediatamente y se confirma con un mensaje.

**Why this priority**: la web ofrece `DELETE /history` completo; es funcionalidad de privacidad esperada, pero no bloquea ver/reproducir.

**Independent Test**: con historial existente, pulsar Limpiar y comprobar que la lista se vacía y muestra el estado vacío.

**Acceptance Scenarios**:

1. **Given** historial con 3 emisoras, **When** pulso "Limpiar", **Then** aparece un diálogo de confirmación "¿Limpiar todo el historial?"; al confirmar, la lista se vacía al instante, se envía `DELETE /history` y veo el estado vacío con confirmación.
2. **Given** fallo de red al limpiar, **When** el servidor no responde, **Then** la lista vuelve a su estado anterior y veo un mensaje de error con reintento.

---

### User Story 5 - Navegar y escuchar desde Historial sin perder el hilo (Priority: P2)

Una persona puede llegar a Historial desde la barra inferior, reproducir una emisora y seguir navegando por otras secciones sin que la música se corte. Si no ha iniciado sesión e intenta entrar, se le pide login como en el resto de secciones privadas.

**Why this priority**: cierra la "navegación" pedido por el usuario y la paridad con la web (`/historial` exige cuenta).

**Independent Test**: con y sin sesión, recorrer barra inferior → Historial → reproducir → navegar a otra sección comprobando que el audio continúa.

**Acceptance Scenarios**:

1. **Given** sesión iniciada, **When** pulso "Historial" en la barra inferior, **Then** llego a mi lista en menos de 2 segundos con red normal.
2. **Given** sin sesión, **When** pulso "Historial", **Then** se me lleva a Login (igual que Favoritos o Perfil).
3. **Given** una emisora reproduciéndose desde Historial, **When** voy a Explorar o Favoritos, **Then** la música no se corta y el mini-reproductor sigue visible.

---

### Edge Cases

- ¿Qué pasa si el historial tiene muchas emisoras (cientos)? Se carga completo sin paginación (el contrato no pagina historial); si el servidor pagina en el futuro, se pagina igual que Explorar.
- ¿Cómo se comporta la lista si el servidor devuelve la misma emisora varias veces? Se deduplican en cliente mostrando solo la reproducción más reciente por emisora, igual que la web.
- ¿Qué ocurre si el usuario reproduce rápidamente varias emisoras desde Historial? La lista se actualiza tras cada reproducción sin parpadeos; la última reproducida siempre queda primera.
- ¿Qué pasa sin red al abrir Historial si ya se abrió antes con red? Se muestra la última lista conocida marcada como offline con botón de reintentar.
- ¿Cómo se muestra la fecha de reproducción? Relativa ("hace un momento", "hace N minutos", "hace N horas", "hace N días") con la fecha y hora absoluta completa disponible como detalle (tooltip o texto secundario).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Las personas con cuenta MUST poder abrir una sección Historial que muestre su lista de emisoras reproducidas en vista de lista (única), con imagen, nombre, país/idioma y hora relativa de la última reproducción, ordenadas de más reciente a más antigua. El toggle de vista (lista/tarjetas) se implementará en una spec futura de UX global.
- **FR-002**: El historial MUST deduplicar emisoras en cliente: si la misma emisora aparece varias veces, solo se muestra la reproducción más reciente.
- **FR-003**: Sin historial, Historial MUST mostrar un estado vacío con mensaje en español ("Todavía no has escuchado nada") y acceso directo a Explorar; nunca una pantalla en blanco o un error técnico.
- **FR-004**: El sistema MUST permitir reproducir una emisora desde el historial con un solo toque; al hacerlo, la lista MUST actualizarse automáticamente situando la emisora reproducida en primera posición sin interrumpir la reproducción.
- **FR-005**: El sistema MUST permitir eliminar una emisora individual del historial (`DELETE /history/:stationId`) con reflejo inmediato y reversión ante error.
- **FR-006**: El sistema MUST permitir limpiar todo el historial de una vez (`DELETE /history`) con diálogo de confirmación previo ("¿Limpiar todo el historial?" con Cancelar/Limpiar) y reversión ante error.
- **FR-007**: Historial MUST exigir sesión como el resto de secciones privadas: sin sesión redirige a Login; con sesión caducada intenta renovación transparente una vez antes de pedir login.
- **FR-008**: Historial MUST ser alcanzable desde la barra inferior de navegación con paridad web (`/historial`), y cada emisora MUST enlazar a su ficha y permitir reproducción sin interrumpir la navegación (reproductor persistente).
- **FR-009**: Todos los errores del servidor con formato `{error:{code,message,status}}` MUST traducirse a mensajes en español orientados a la acción; PROHIBIDO mostrar texto técnico crudo.
- **FR-010**: Sin red y con una visita previa, el historial MUST mostrar la última versión conocida marcada como offline y MUST permitir reintento manual; en primer arranque sin caché MUST mostrar error con reintento.
- **FR-011**: El historial MUST actualizarse automáticamente cuando el usuario reproduce una emisora desde la propia pantalla de Historial, sin necesidad de recarga manual.
- **FR-012**: La hora relativa MUST seguir el formato: "hace un momento" (< 60 s), "hace N minuto(s)" (< 60 min), "hace N hora(s)" (< 24 h), "hace N día(s)" (≥ 24 h), con pluralización correcta en español.

### Key Entities

- **HistoryEntry**: emisora reproducida por una persona (`station` + `playedAt` marca de tiempo de reproducción); pertenece a una sola cuenta, puede haber múltiples entradas por emisora.
- **Station**: emisora del catálogo (`id`, `name`, `homepage`, `favicon`, `country`, `language`, `tags`, `bitrate`, entre otros); es lo que se muestra dentro de cada entrada de historial.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Una persona con cuenta abre Historial y reconoce su lista completa en menos de 2 segundos con red normal.
- **SC-002**: 9 de cada 10 personas reproducen una emisora desde Historial al primer intento sin ayuda.
- **SC-003**: La deduplicación funciona correctamente: una emisora reproducida 3 veces aparece una sola vez con la marca de tiempo más reciente, verificable en el 100 % de los casos.
- **SC-004**: Reproducir desde Historial y navegar por 3 secciones no interrumpe el audio en el 100 % de las pruebas con red normal.
- **SC-005**: La eliminación individual de una emisora del historial se refleja en servidor y en la vista en menos de 2 segundos tras la acción.
- **SC-006**: Ante fallo de red al eliminar/limpiar, el estado visible se corrige solo (reversión) en menos de 2 segundos tras la respuesta de error.

## Assumptions

- La instancia del usuario implementa el contrato de historial visto en `izquierdojl/tolocharadio` (`GET /history`, `DELETE /history`, `DELETE /history/:stationId`); si su versión difiere, se declara en notas de release.
- El usuario de esta spec ya tiene cuenta y sesión (el login/registro lo cubre la spec 001); Historial exige sesión como en la web.
- El historial por usuario es de decenas a cientos de entradas: se carga completo sin paginación; si el servidor pagina en el futuro, se pagina igual que Explorar.
- La caché offline del historial es solo de lectura (última lista conocida); la fuente de verdad al abrir con red es siempre el servidor.
- El historial se registra automáticamente en servidor al reproducir vía proxy (`GET /playback/:stationId`); el cliente no necesita registrar historial manualmente.
- La deduplicación se hace en cliente igual que la web: se agrupa por `station.id` y se conserva la entrada con `playedAt` más reciente.
- El formato de hora relativa sigue la convención de la web (español, singular/plural correcto).
