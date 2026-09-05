# Feature Specification: Favoritos — lista, marcado y navegación

**Feature Branch**: `003-favorites-management`

**Created**: 2026-09-05

**Status**: Done (2026-09-05; todo verificado salvo Compose en dispositivo — entorno API 37 incompatible con compose-ui-test, igual que spec 002 — y recorrido manual contra instancia real)

**Input**: User description: "Feature: Incorporar opciones de favoritos a través del endpoint /favorites proporcionado por el api de. Enlace web: https://github.com/izquierdojl/tolocharadio — Navegación, marcado de existentes y navegación."

**Fuente de verdad del contrato**: backend `izquierdojl/tolocharadio`, endpoints autenticados `GET /favorites` (lista), `POST /favorites` (añadir por `stationId`), `DELETE /favorites/:stationId` (quitar), `PUT /favorites/order` (orden personalizado como permutación exacta de ids). Modelo `Favorite { station, addedAt }`. Errores con formato `{error:{code,message,status,details?}}`.

**Alcance previo**: la spec `001-auth-explore-base` (FR-009) dejó la pantalla completa de Favoritos fuera de alcance — solo toggle rápido desde Explorar con actualización optimista. Esta spec completa ese pendiente: pantalla de Favoritos, marcado de existentes en todas las listas y navegación con paridad web (`/favoritos`).

## Clarifications

### Session 2026-09-05

- Q: ¿Cómo debe reordenar el usuario su lista de favoritas? → A: Drag & drop (arrastrar y soltar con asa visible, guardado automático al soltar).
- Q: ¿Cuánto tiempo debe ofrecerse deshacer tras quitar una favorita? → A: 10 segundos (barra con Deshacer visible 10 segundos).
- Q: ¿Qué debe mostrar Favoritos sin conexión si ya se abrió antes con red? → A: Mostrar caché (última lista conocida marcada offline + reintento).
- Q: ¿Cómo debe mostrarse la fecha de guardado en cada favorita? → A: Relativa (texto relativo tipo "hace 2 días" junto a cada favorita).
- Q: ¿Qué debe pasar si el orden guardado ya cambió en el servidor al reordenar? → A: Gana servidor (descartar cambio local y mostrar orden del servidor con aviso).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver mis emisoras favoritas (Priority: P1)

Una persona con cuenta entra en la sección Favoritos y ve todas las emisoras que ha guardado, con su imagen, nombre, país/idioma y etiquetas, en el orden que ella misma definió. Si aún no tiene ninguna, ve un mensaje amable que le invita a explorar el catálogo.

**Why this priority**: es el núcleo del pedido — sin una pantalla de Favoritos no hay "opciones de favoritos". Entrega valor por sí sola.

**Independent Test**: iniciando sesión con una cuenta que tiene favoritas se puede abrir Favoritos y ver la lista completa; con una cuenta nueva se ve el estado vacío con enlace a Explorar.

**Acceptance Scenarios**:

1. **Given** una cuenta con 3 favoritas guardadas, **When** abre Favoritos, **Then** ve las 3 con imagen, nombre, país/idioma y etiquetas, en su orden guardado.
2. **Given** una cuenta sin favoritas, **When** abre Favoritos, **Then** ve un estado vacío ("Aún no tienes favoritas") con un botón que lleva a Explorar.
3. **Given** sesión caducada al abrir Favoritos, **When** se intenta cargar la lista, **Then** la sesión se renueva de forma transparente y la lista aparece sin pedirme login; si la renovación falla, voy a Login con aviso.
4. **Given** error de red o del servidor al cargar, **When** falla la carga, **Then** veo un mensaje en español con botón de reintento y nunca una pantalla en blanco.

---

### User Story 2 - Marcar y desmarcar favoritas desde cualquier sitio (Priority: P1)

Una persona navegando por Explorar, por la ficha de una emisora o por su propia lista de Favoritos puede guardar o quitar una favorita con un solo toque en el botón de corazón, y el cambio se refleja al instante en todas las pantallas. Las emisoras que ya son favoritas aparecen marcadas allá donde se muestren.

**Why this priority**: el "marcado de existentes" pedido por el usuario — sin esto el usuario no sabe qué ya guardó y duplica o pierde favoritas.

**Independent Test**: marcar una emisora en Explorar y comprobar que aparece marcada en su ficha y en Favoritos; quitarla en Favoritos y comprobar que se desmarca en Explorar.

**Acceptance Scenarios**:

1. **Given** una emisora no guardada en Explorar, **When** pulso el corazón, **Then** el corazón se rellena al instante y la emisora aparece en Favoritos.
2. **Given** una emisora ya guardada que aparece en Explorar y en su ficha, **When** miro ambas, **Then** en las dos el corazón aparece relleno (marcado de existentes coherente).
3. **Given** una favorita en la lista de Favoritos, **When** pulso su corazón, **Then** desaparece de la lista al instante con opción de deshacer durante 10 segundos.
4. **Given** fallo de red al guardar/quitar, **When** el servidor rechaza la operación, **Then** el corazón vuelve a su estado anterior y veo un mensaje breve en español.
5. **Given** una emisora que ya no existe en el servidor (404), **When** intento quitarla o abrirla, **Then** veo "esta emisora ya no está disponible" y deja de aparecer como favorita.

---

### User Story 3 - Ordenar mis favoritas a mi manera (Priority: P2)

Una persona con varias favoritas puede cambiar su orden arrastrando y soltando cada tarjeta mediante un asa visible (por ejemplo, poner primero las que más escucha); al soltar, el nuevo orden se guarda automáticamente y se conserva al cerrar y reabrir la app y en todos sus dispositivos que usen la misma cuenta.

**Why this priority**: el contrato del servidor prevé orden personalizado; es valor diferencial frente a una lista alfabética fija, pero no bloquea guardar/ver.

**Independent Test**: cambiar el orden de 3 favoritas, cerrar y reabrir la app, y comprobar que el orden se mantiene.

**Acceptance Scenarios**:

1. **Given** 3 favoritas en orden A-B-C, **When** muevo B a la primera posición arrastrando su asa y suelto, **Then** la lista queda B-A-C, se guarda automáticamente y tras reiniciar sigue B-A-C.
2. **Given** estoy reordenando, **When** pierdo la conexión a mitad del guardado, **Then** veo mensaje de error con reintento y la lista vuelve al último orden confirmado por el servidor.
3. **Given** el orden cambió en el servidor mientras reordenaba, **When** se confirma el guardado, **Then** se descarta mi cambio local, se muestra el orden del servidor y veo un aviso.

---

### User Story 4 - Navegar y escuchar desde Favoritos sin perder el hilo (Priority: P2)

Una persona puede llegar a Favoritos desde la barra inferior, abrir la ficha de una favorita, reproducirla y seguir navegando por otras secciones sin que la música se corte. Si no ha iniciado sesión e intenta entrar, se le pide login como en el resto de secciones privadas.

**Why this priority**: cierra el "navegación" pedido dos veces por el usuario y la paridad con la web (`/favoritos` exige cuenta).

**Independent Test**: con y sin sesión, recorrer barra inferior → Favoritos → ficha → reproducir → navegar a otra sección comprobando que el audio continúa.

**Acceptance Scenarios**:

1. **Given** sesión iniciada, **When** pulso "Favoritos" en la barra inferior, **Then** llego a mi lista en menos de 2 segundos con red normal.
2. **Given** sin sesión, **When** pulso "Favoritos", **Then** se me lleva a Login (igual que Historial o Perfil).
3. **Given** una favorita en la lista, **When** pulso reproducir, **Then** suena y puedo ir a Explorar o Perfil sin que se corte; el mini-reproductor sigue visible.
4. **Given** una favorita en la lista, **When** pulso su título o imagen, **Then** se abre su ficha con los mismos datos y acciones que en Explorar.

---

### Edge Cases

- ¿Qué pasa si una favorita guardada corresponde a una emisora eliminada o sin datos (nombre/imagen vacíos)? Se muestra con placeholder y título alternativo, con opción de quitarla; nunca rompe la lista.
- ¿Cómo se comporta la lista si el servidor devuelve duplicados o un orden que no coincide con lo guardado? Se muestran sin duplicar y se respeta el orden del servidor como fuente de verdad.
- ¿Qué ocurre si el usuario quita la misma favorita en dos pantallas a la vez o con doble toque rápido? Solo se envía una operación efectiva; el estado final es "no favorita" sin errores visibles.
- ¿Qué pasa sin red al abrir Favoritos si ya se abrió antes con red? Se muestra la última lista conocida marcada como offline con botón de reintentar.
- ¿Cómo se evita que reordenar con muchos elementos bloquee la pantalla? El guardado del orden muestra progreso y permite seguir navegando; el error se notifica sin perder el orden local hasta confirmar.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Las personas con cuenta MUST poder abrir una sección Favoritos que muestre su lista completa guardada en el servidor, con imagen, nombre, país/idioma, etiquetas y fecha relativa de guardado (p. ej. "hace 2 días"), en el orden personalizado del servidor.
- **FR-002**: Sin favoritas, Favoritos MUST mostrar un estado vacío con mensaje en español y acceso directo a Explorar; nunca una pantalla en blanco o un error técnico.
- **FR-003**: Toda emisora que sea favorita MUST aparecer con el corazón relleno allá donde se muestre (Explorar en vista lista y grid, ficha de emisora, lista de Favoritos); toda no-favorita MUST aparecer con el corazón vacío.
- **FR-004**: El sistema MUST permitir guardar una emisora (`POST /favorites` con su identificador) y quitarla (`DELETE /favorites/:stationId`) desde Explorar, desde la ficha y desde la lista de Favoritos, con reflejo inmediato (optimista) y reversión ante error.
- **FR-005**: Al quitar desde la lista de Favoritos, el sistema MUST ofrecer deshacer durante 10 segundos (vuelve a guardar la emisora sin perder su posición cuando sea posible).
- **FR-006**: Las personas con cuenta MUST poder reordenar su lista mediante arrastrar y soltar con asa visible y guardado automático al soltar, y el sistema MUST guardar ese orden en el servidor (`PUT /favorites/order` con la lista completa de identificadores en el nuevo orden); el orden guardado MUST conservarse entre sesiones.
- **FR-007**: Favoritos MUST exigir sesión como el resto de secciones privadas: sin sesión redirige a Login; con sesión caducada intenta renovación transparente una vez antes de pedir login.
- **FR-008**: Favoritos MUST ser alcanzable desde la barra inferior de navegación con paridad web (`/favoritos`), y cada favorita MUST enlazar a su ficha y permitir reproducción sin interrumpir la navegación (reproductor persistente).
- **FR-009**: Todos los errores del servidor con formato `{error:{code,message,status}}` MUST traducirse a mensajes en español orientados a la acción (sesión expirada, no encontrado 404, conflicto 409, validación 422, servicio caído 503 con reintento); PROHIBIDO mostrar texto técnico crudo.
- **FR-010**: Sin red y con una visita previa, la lista de Favoritos MUST mostrar la última versión conocida marcada como offline y MUST permitir reintento manual; en primer arranque sin caché MUST mostrar error con reintento.
- **FR-011**: Fuera de alcance: Historial, Mis emisoras y Sugerencias siguen en sus propias specs; esta spec no cambia el catálogo público ni el registro de cuentas.

### Key Entities

- **Favorite**: emisora guardada por una persona (`station` + `addedAt` fecha de guardado); pertenece a una sola cuenta, una por emisora.
- **Station**: emisora del catálogo (`id`, `name`, `homepage`, `favicon`, `country`, `language`, `tags`, `bitrate`, entre otros); es lo que se muestra dentro de cada favorita.
- **FavoriteOrder**: orden personalizado de la lista (secuencia de identificadores de emisora); la fuente de verdad es el servidor.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Una persona con cuenta abre Favoritos y reconoce su lista completa en menos de 2 segundos con red normal.
- **SC-002**: 9 de cada 10 personas marcan o desmarcan una favorita al primer intento sin ayuda, desde Explorar, ficha o Favoritos.
- **SC-003**: El marcado de existentes es coherente: una favorita aparece como guardada en el 100 % de las pantallas donde se muestra, verificable recorriendo Explorar → ficha → Favoritos.
- **SC-004**: El orden personalizado sobrevive a cerrar y reabrir la app en el 100 % de los casos probados con 3 o más favoritas.
- **SC-005**: Reproducir desde Favoritos y navegar por 3 secciones no interrumpe el audio en el 100 % de las pruebas con red normal.
- **SC-006**: Ante fallo de red al guardar/quitar, el estado visible se corrige solo (reversión) en menos de 2 segundos tras la respuesta de error.

## Assumptions

- La instancia del usuario implementa el contrato de favoritos visto en `izquierdojl/tolocharadio` (`GET/POST /favorites`, `DELETE /favorites/:stationId`, `PUT /favorites/order` como permutación exacta); si su versión difiere, se declara en notas de release.
- El usuario de esta spec ya tiene cuenta y sesión (el login/registro lo cubre la spec 001); Favoritos exige sesión como en la web.
- La lista de favoritas por usuario es de decenas, no de miles: se carga completa sin paginación; si el servidor pagina en el futuro, se pagina igual que Explorar.
- El orden personalizado se guarda como lista completa de ids (el servidor lo exige así); envíos parciales se completan en cliente antes de guardar.
- La caché offline de favoritas es solo de lectura (última lista conocida); la fuente de verdad al abrir con red es siempre el servidor.
- Deshacer tras quitar dispone de 10 segundos (barra con Deshacer); pasado ese tiempo hay que volver a guardar manualmente.
