# Feature Specification: Auth + Explorar + base app contra instancia TolochaRadio

**Feature Branch**: `001-auth-explore-base`

**Created**: 2026-09-04

**Status**: Draft

> **Nota de gobernanza (actualizada 2026-09-20)**: el **modelo de UI** de autenticación de usuario definido en esta spec (pantallas de login/registro/Perfil, `GET`/`PATCH /users/me`, registro/recuperación de contraseña y logout desde la app) queda **retirado**: la constitución 2.0.0 y la spec `0022-jlizquierdo-20260912-server-only-access` lo eliminaron. **Sin embargo, la constitución 3.0.0 y la spec `0024-jlizquierdo-20260912-per-server-credentials` reintrodujeron las credenciales por servidor** (email/contraseña cifrada) con **login automático JWT/`Bearer`** — incluida la reproducción por proxy (ver también 0021) —; lo que sigue retirado es únicamente el modelo de UI anterior, no el uso de credenciales. El resto de la spec se conserva como histórico.

**Input**: User description: "Comencemos con esta especificación para la feature de auth + explorar contra tu instancia y el desarrollo inicial de la aplicación."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Arranque contra mi instancia (Priority: P1)

El usuario abre la app por primera vez, introduce la URL de su
instancia TolochaRadio self-hosted (p. ej. `https://radio.mi-dominio.com`),
la app verifica `GET /health` y `GET /config` y llega a una Home
pública funcional. Si cambia de instancia más tarde, puede editar la
URL en ajustes y la sesión se cierra.

**Why this priority**: sin esto nada más funciona: todo el contrato
(`/api/v1`) cuelga de una `baseUrl` válida y configurable. Es el
bootstrap inicial de la app.

**Independent Test**: instalando solo esto se puede abrir la app,
configurar una URL, ver el estado de conexión y la Home pública.
No requiere login.

**Acceptance Scenarios**:

1. **Given** primer arranque sin `baseUrl`, **When** introduzco
   `https://radio.valida.com` y pulso conectar, **Then** la app
   llama a `/health` + `/config`, guarda la URL y muestra la Home.
2. **Given** URL inválida o sin red, **When** intento conectar,
   **Then** veo error accionable en español con reintento y no se
   guarda la URL.
3. **Given** instancia con `registrationEnabled=false`, **When**
   llego al login, **Then** no se ofrece enlace a registro.

---

### User Story 2 - Login y sesión persistente (Priority: P1)

Usuario con cuenta en su instancia inicia sesión con email +
contraseña, la sesión sobrevive a reinicios, el access token se
renueva solo ante un 401 y puede cerrar sesión. La parte de
recuperación de contraseña queda para la historia 5.

**Why this priority**: Explorar a fondo, favoritas, historial y
playback exigen auth (el proxy de streaming es autenticado). Es el
desbloqueador del resto.

**Independent Test**: se puede probar solo con las pantallas
Login/Sesión: login OK, matar y reabrir la app sigue autenticado,
401 provoca refresh transparente, logout limpia todo.

**Acceptance Scenarios**:

1. **Given** credenciales válidas, **When** hago login, **Then**
   llego a Explorar autenticado y el refresh queda guardado cifrado.
2. **Given** sesión con access caducado (15 min), **When** hago
   cualquier llamada, **Then** se renueva vía `POST /auth/refresh`
   y la llamada original se reintenta una vez sin pedirme login.
3. **Given** refresh inválido/revocado, **When** falla el refresh,
   **Then** se cierra la sesión y se me lleva a Login con aviso.
4. **Given** autenticado, **When** pulso salir, **Then** se llama a
   `POST /auth/logout`, se borran tokens y vuelvo a Home pública.

---

### User Story 3 - Registro cuando la instancia lo permite (Priority: P2)

Si `GET /config` dice `registrationEnabled=true`, el usuario puede
crear cuenta (nombre/email/password 8–72) y queda logueado. Si está
deshabilitado, no hay camino de registro en la app.

**Why this priority**: necesario para instancias abiertas, pero
muchas serán unipersonales con registro cerrado; por eso P2.

**Independent Test**: contra instancia con registro abierto se crea
cuenta y se entra; contra instancia cerrada la opción no existe.

**Acceptance Scenarios**:

1. **Given** registro habilitado, **When** registro con datos
   válidos, **Then** la cuenta se crea (`201`), quedo autenticado
   y veo mi perfil (`GET /users/me`).
2. **Given** email ya existente, **When** intento registrarme,
   **Then** veo error de conflicto (409) en español.
3. **Given** registro deshabilitado, **When** busco cómo registrarme,
   **Then** no hay botón/enlace de registro en Login ni en Home.

---

### User Story 4 - Explorar el catálogo como en la web (Priority: P1)

Usuario autenticado entra en **Explorar**, busca por nombre, filtra
por país/idioma/etiqueta (listas de `stations/countries|languages|
tags`), pagina resultados (24 por defecto, `hasMore`), alterna vista
lista/grid estilo Pocket Casts y abre la ficha de una emisora
(`GET /stations/:id`) con opción de reproducir y favoritar.

**Why this priority**: es el corazón del producto junto al player;
paridad con `/explorar` de la web.

**Independent Test**: con sesión iniciada se puede buscar, filtrar,
paginar y abrir fichas sin necesidad de reproducir (player mockeado
o ausente).

**Acceptance Scenarios**:

1. **Given** autenticado en Explorar, **When** busco "jazz" con país
   "España", **Then** veo tarjetas con favicon, nombre, país/idioma/
   tags y puedo cargar más con `offset` mientras `hasMore=true`.
2. **Given** catálogo con duplicados, **When** activo "únicas",
   **Then** la llamada incluye `unique=true`.
3. **Given** RadioBrowser caído (503), **When** exploro,
   **Then** veo aviso con reintento y, si hay caché local, resultados
   en caché marcados como offline.
4. **Given** una emisora en la lista, **When** pulso favoritar,
   **Then** se crea el favorito (`POST /favorites`) y el icono
   cambia a estado guardado (optimista con rollback ante error).

---

### User Story 5 - Mini-player persistente con proxy autenticado (Priority: P1)

Desde cualquier sección, al pulsar play en una emisora la app
comprueba `GET /playback/:stationId/status`; si `playable=true`
reproduce el proxy `GET /playback/:stationId` con Bearer en
Media3, con mini-player persistente sobre la bottom bar (play/pausa,
emisora actual, quitar) y estados `Idle/Buffering/Playing/Paused/
Error`. Escuchar registra historial en el servidor.

**Why this priority**: sin playback no hay radio; el usuario lo pidió
explícitamente para esta spec.

**Independent Test**: con una emisora `playable` se reproduce, se
navega entre secciones sin corte, se pausa y se quita; con una no
disponible se muestra el motivo sin arrancar el player.

**Acceptance Scenarios**:

1. **Given** emisora con `playable=true`, **When** pulso play,
   **Then** el mini-player pasa a Buffering→Playing y el audio
   suena en segundo plano con notificación multimedia.
2. **Given** emisora con `playable=false`, **When** pulso play,
   **Then** veo "no disponible: {reason}" y el player no arranca.
3. **Given** reproduciendo, **When** navego a otra sección o roto
   la pantalla, **Then** el audio continúa y el mini-player sigue
   visible con la emisora actual.
4. **Given** error de stream en curso, **When** falla la red,
   **Then** el player muestra Error con reintento (backoff) y nunca
   expone el token en URL ni logs.

---

### User Story 6 - Cuenta y recuperación (Priority: P3)

Usuario autenticado ve su perfil (`GET /users/me` con tema
light/dark), puede cambiar nombre/tema y contraseña, y un usuario
deslogueado puede pedir reset (`forgot` → token de un solo uso →
`reset`). Como `forgot` devuelve `resetToken=null` si el email no
existe, la UI no distingue casos (anti-enumeración).

**Why this priority**: completa la paridad con `/perfil` web pero no
bloquea escuchar radio; por eso P3.

**Independent Test**: perfil editable y flujo forgot/reset funcionan
sin tocar Explorar ni el player.

**Acceptance Scenarios**:

1. **Given** autenticado en Perfil, **When** cambio mi nombre/tema,
   **Then** se guarda vía `PATCH /users/me` y el tema se aplica.
2. **Given** autenticado, **When** cambio contraseña,
   **Then** se llama a `PATCH /users/me/password` y se me pide
   re-login (revocación de refresh previos).
3. **Given** deslogueado, **When** pido reset para cualquier email,
   **Then** veo siempre el mismo mensaje neutro ("si existe, te
   hemos enviado instrucciones").

---

### Edge Cases

- `baseUrl` con `/` final, sin esquema o con subpath: normalizar y
  validar antes de guardar; rechazar `http://` salvo instancia local
  explícita con aviso de seguridad.
- Access caduca a mitad de una paginación de Explorar: refresh
  transparente y reintento único; si falla, login.
- `limit` fuera de 1–100: el cliente clamp a 24/100 y nunca envía
  valores ilegales.
- Emisora `isCustom=true` sin favicon: placeholder con inicial,
  nunca crash por null.
- `Station.url` directa nunca se reproduce ni se muestra; solo el
  proxy autenticado.
- Sin red durante playback: Error con reintento + backoff, respeta
  `ConnectivityManager`, no loop infinito.
- Llamada/música externa/Bluetooth: pausa/duck según Media3.
- Cambio de `baseUrl` con sesión activa: cierra sesión y limpia
  tokens + caché de la instancia anterior.
- `forgot-password` con email inexistente: mismo mensaje neutro,
  sin revelar cuentas.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La app MUST arrancar con bootstrap inicial: migración
  del esqueleto Views a Compose M3 + Navigation, Hilt, Retrofit +
  OkHttp + kotlinx.serialization, Media3, Coil, Room + DataStore
  cifrado, corrutinas/Flow y gates Detekt/ktlint/Lint/CI en verde.
- **FR-002**: La app MUST pedir y guardar una `baseUrl` editable
  (primer arranque + ajustes), validarla contra `GET /health` y
  `GET /config`, y ante cambio MUST cerrar sesión y limpiar datos
  de la instancia anterior.
- **FR-003**: La app MUST implementar login `POST /auth/login`
  (email + password), persistir access en memoria y refresh cifrado,
  renovar ante 401 con `POST /auth/refresh` (reintento único) y
  logout con `POST /auth/logout` + borrado local.
  *(Matiz 2026-09-20 — constitución 3.0.0 + spec 0024: se conserva la mecánica de tokens
  (access en memoria, refresh cifrado, renovación única ante 401, `Bearer` en proxy), pero el
  **login ya no es una pantalla**: las credenciales por servidor se guardan y el login
  `POST /auth/login` es automático. El logout desde la app queda retirado; se gestiona
  eliminando/editando el servidor.)*
- **FR-004**: Si `registrationEnabled=true`, la app MUST ofrecer
  registro `POST /auth/register` (name/email/password 8–72) con
  errores 409/422 por campo; si es false MUST ocultar el registro.
  *(Retirado 2026-09-20 — spec 0024: sin pantallas de registro en la app.)*
- **FR-005**: La app MUST exigir auth para Explorar, Favoritas,
  Historial, Mis emisoras y Perfil (redirección a Login, paridad
  con `RequireAuth` web); Home y Login/Registro son públicos.
  *(Matiz 2026-09-20 — spec 0024: sin redirección a Login ni pantallas públicas de
  Login/Registro. Las secciones privadas quedan tras el arranque bloqueante
  `StartupGate.NeedsCredentials`; sin credenciales válidas no se llega a la app.)*
- **FR-006**: Explorar MUST consumir `GET /stations` con filtros
  `name/country/language/tag`, `limit` (default 24, max 100),
  `offset`, `unique` y paginación con `hasMore`; filtros con
  `GET /stations/countries|languages|tags`; detalle con
  `GET /stations/:id`.
- **FR-007**: El reproductor MUST usar `MediaSessionService` +
  ExoPlayer, precheck `GET /playback/:stationId/status`
  (`{id,playable,reason}`), stream por `GET /playback/:stationId`
  con `Authorization: Bearer` inyectado en `DataSource.Factory`,
  y estados sellados `Idle/Buffering/Playing/Paused/Error` con
  reintento backoff. PROHIBIDO token en URL y reproducir
  `Station.url` directa.
- **FR-008**: La app MUST ofrecer mini-player persistente (sobre la
  bottom bar) + full-player bottom sheet con play/pausa/quitar y
  notificación multimedia, que sobrevive a navegación y rotación.
- **FR-009**: Desde Explorar la app MUST permitir añadir/quitar
  favorito (`POST /favorites {stationId}`, `DELETE
  /favorites/:stationId`) con actualización optimista y rollback.
  Pantalla completa de Favoritos (lista + `PUT /favorites/order`),
  Historial, Mis emisoras (`/custom-stations`) y Sugerencias
  (`/suggestions`) quedan FUERA de esta spec (siguiente spec).
- **FR-010**: Perfil mínimo MUST mostrar `GET /users/me` y permitir
  `PATCH /users/me` (name, theme light/dark con aplicación
  inmediata) en esta spec; cambio de password y forgot/reset
  (historia 6) SHOULD incluirse si no crece el alcance, si no se
  difieren con test pendiente.
  *(Matiz 2026-09-20 — spec 0022/0024: la pantalla de Perfil, `GET`/`PATCH /users/me` y el
  cambio/recuperación de contraseña quedan **retirados**; la gestión de la instancia/credenciales
  se hace desde el flujo de servidor de la spec 0024 ("Editar servidor").)*
- **FR-011**: Todos los errores backend `{error:{code,message,
  status,details?}}` MUST mapearse a dominio tipado y mensajes en
  español (401→sesión, 404→no encontrado, 409→conflicto,
  422→validación por campo con `details`, 503→reintento/caché);
  PROHIBIDO volcar mensaje técnico crudo al usuario o loguear PII/
  tokens.
- **FR-012**: La UI MUST seguir la dirección Pocket Casts en M3:
  `NavigationBar` inferior (Home/Explorar/Perfil en esta spec;
  Favoritos/Historial/Mis emisoras como destinos visibles pero con
  estado "próximamente" si no hay sesión o fuera de alcance),
  `StationCard/StationListItem` con Coil, toggle lista/grid,
  `EmptyState`, `FavoriteButton`, tema oscuro Tolocha por defecto
  (verde-bosque/ocre-montaña).
- **FR-013**: La app MUST usar `applicationId`
  `com.izquierdojl.tolocharadio`, `minSdk=26`,
  HTTPS-only y `versionCode` incremental.
- **FR-014**: Cada UseCase/Repositorio/ViewModel nuevo MUST tener
  test unitario (JUnit + coroutines-test + Turbine) y los DTOs
  MUST tener tests de serialización contra el OpenAPI; flujos
  críticos (login, explorar+paginar, play/stop, favoritar, error
  con reintento) MUST tener Compose Test.

### Key Entities

- **AppInstance**: `baseUrl` normalizada + `appName` +
  `registrationEnabled` (de `/config`); una activa por dispositivo.
- **User**: `{id, email, name, theme(light|dark), createdAt}`.
- **AuthSession**: access JWT en memoria + refresh rotatorio
  cifrado; estado `Loading/Authenticated/Unauthenticated`.
- **Station**: `{id(UUID), name, homepage?, favicon?, country?,
  countryCode?, language?, tags[], codec?, bitrate?, isSsl?,
  lastCheckOk?, votes?, clickCount?, isCustom}`; `url` solo proxy.
- **StationPage**: `{items: Station[], offset, limit, hasMore}`.
- **Favorite**: `{station, addedAt}` (en esta spec solo toggle).
- **PlaybackStatus**: `{id, playable, reason?}`; **PlayerState**:
  `Idle/Buffering/Playing/Paused/Error(mensaje, reintentable)`.
- **ApiError**: `{code, message, status, details[{field,message}]}`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un usuario nuevo conecta su instancia, crea cuenta o
  hace login, busca una emisora por nombre+país y la reproduce en
  < 3 minutos en un dispositivo gama media con red normal.
- **SC-002**: El 100% de llamadas con access caducado se recuperan
  con un solo refresh sin intervención, y el 100% con refresh
  inválido terminan en Login sin crash.
- **SC-003**: Explorar pagina 3 páginas seguidas (24+24+24) sin
  duplicados ni saltos cuando `hasMore=true`, y respeta `limit≤100`.
- **SC-004**: Reproducir sobrevive a navegar entre 3 secciones y a
  rotar 2 veces sin corte; ante stream no disponible se muestra el
  motivo sin arrancar el player (verificado manual + test).
- **SC-005**: Cero tokens en URLs, logs o cachés en claro (revisión
  de código + test de `DataSource.Factory`); cero pantallas nuevas
  en XML.

## Assumptions

- La instancia del usuario implementa el OpenAPI 3.1 visto en
  `izquierdojl/tolocharadio` (`/api/v1/openapi.json`); si su
  versión difiere, se declara en release notes.
- `baseUrl` configurable al arrancar (respuesta elegida); sin
  instancia válida la app no funciona más allá del onboarding.
- Alcance de esta spec: bootstrap + auth + explorar + mini-player
  con favorito rápido; Favoritos completo, Historial, Mis emisoras
  y Sugerencias van en la siguiente spec.
- Tema oscuro Tolocha por defecto; light disponible (perfil).
- Usuarios con conectividad intermitente; el servidor ya cachea
  RadioBrowser y registra historial al hacer proxy.
- `minSdk=26`, HTTPS-only, sin DRM ni caché offline de audio.
- Paquete final pendiente de confirmación (FR-013).

