# Feature Specification: Credenciales por servidor, auto-login y pantalla unificada de servidor

**Feature Branch**: `0024-jlizquierdo-20260912-per-server-credentials`

**Created**: 2026-09-12

**Status**: Done (2026-09-12; todo verificado manualmente en dispositivo real por el usuario)

**Input**: User description: "Tras probar la 0022 en entorno real: (1) la pantalla inicial muestra alta de servidor pero sin usuario y contraseña; hay que unificar en una pantalla general de añadir servidor en todos los casos; (2) en cada servidor deben guardarse usuario y contraseña; (3) en favoritos e historial aparece 'Esta instancia requiere autenticación...', lo cual es correcto, pero el botón Reintentar debe llevar a la ficha del servidor activo para corregir las credenciales."

## Contexto

La spec 0022 asumió que la instancia no exige autenticación de usuario y eliminó toda la capa de sesión. La prueba en entorno real demuestra que **la instancia sí exige login (JWT + refresh)**: el catálogo es público, pero favoritos, historial (y demás recursos de usuario) requieren sesión. Esta spec **revierte parcialmente la 0022**: vuelve a haber credenciales y sesión autenticada, pero sin pantallas de login/registro: las credenciales se configuran **por servidor** y la app hace login automático.

## Clarifications

### Session 2026-09-12

- Q: ¿Cómo autentica la instancia? → A: Login email/contraseña → JWT (Bearer) con refresh rotatorio.
- Q: ¿Cómo se introducen y gestionan las credenciales? → A: Formulario de servidor unificado (alta y edición) con URL, alias, email y contraseña; auto-login silencioso; sin pantallas de login.
- Q: ¿Adónde debe llevar Reintentar cuando el error es de credenciales? → A: A la ficha/edición del **servidor activo** para corregir email/contraseña.
- Q: ¿Historial y favoritos vuelven a ser por usuario? → A: Sí, por usuario autenticado (se retira "compartido por instancia" de la 0022).
- Q: ¿Registro del trabajo? → A: Nueva spec (esta, 0024) y enmienda constitucional (la 2.0.0 prohíbe credenciales).

- Q: ¿El "usuario" del servidor es el email de la cuenta? → A: Sí; el campo es email (mismo contrato que las specs 001/007).
- Q: ¿La reproducción por el proxy exige sesión? → A: Sí; el proxy y los subrecursos HLS exigen `Bearer`, y el player/Cast vuelven a inyectar el token.
- Q: ¿Qué hace la app al arrancar si el servidor activo/por defecto no tiene credenciales (p. ej. migrado de la 0022)? → A: Abre la pantalla unificada de servidor de forma bloqueante hasta completar las credenciales.
- Q: ¿Cómo se edita un servidor desde la lista? → A: Con un icono "Editar" en cada tarjeta; tocar la tarjeta sigue cambiando el servidor activo.
- Q: ¿Cómo se comporta el campo contraseña al editar? → A: Precargada enmascarada; si no se toca se conserva, y si se cambia se revalida y se vuelve a iniciar sesión.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Alta y edición unificada de servidor con credenciales (Priority: P1)

Una persona añade un servidor desde cualquier punto de la app (primera vez sin servidores, o desde la sección Servidores) usando **el mismo formulario**: URL, alias, email y contraseña. También puede editar un servidor existente para corregir sus credenciales.

**Why this priority**: Es la corrección directa de los dos primeros problemas reportados y la base para que el resto funcione: sin credenciales no hay sesión y favoritos/historial fallan.

**Independent Test**: Con datos borrados, abrir la app y comprobar que la bienvenida pide URL+alias+email+contraseña; añadir el servidor y acceder; desde Servidores, añadir otro y editar las credenciales del primero, viendo el mismo formulario.

**Acceptance Scenarios**:

1. **Given** una instalación sin servidores, **When** se abre la app, **Then** se muestra la bienvenida con un formulario que pide URL, alias, email y contraseña.
2. **Given** la sección Servidores, **When** se pulsa añadir, **Then** se abre el mismo formulario (URL, alias, email y contraseña).
3. **Given** la sección Servidores con un servidor guardado, **When** pulso su icono Editar, **Then** se abre el mismo formulario precargado (contraseña enmascarada, nunca en claro) y al guardar se validan las credenciales.
4. **Given** un formulario incompleto (sin email o contraseña), **When** se intenta guardar, **Then** se bloquea con un aviso en español y no se llama al servidor.
5. **Given** una URL o credenciales inválidas, **When** se guarda, **Then** no se persiste el servidor (o no se actualizan las credenciales) y se muestra un error accionable.

---

### User Story 2 - Sesión automática por servidor, sin pantallas de login (Priority: P1)

Con un servidor configurado, la app inicia sesión sola con las credenciales guardadas de ese servidor y firma las peticiones (favoritos, historial, personalizadas, playback). Si el token caduca, lo renueva de forma transparente. El usuario nunca ve una pantalla de login/registro.

**Why this priority**: Resuelve el error real de favoritos/historial y mantiene la experiencia "solo servidores" decidida en la 0022.

**Independent Test**: Configurar un servidor con credenciales válidas, abrir Favoritos e Historial y comprobar que cargan sin pedir nada; forzar caducidad del token y verificar que se renueva sin intervención.

**Acceptance Scenarios**:

1. **Given** un servidor con credenciales válidas, **When** se abre Favoritos o Historial, **Then** el contenido carga sin pedir credenciales.
2. **Given** un token caducado, **When** se hace una petición autenticada, **Then** la app renueva el token (refresh) y reintenta una vez de forma transparente.
3. **Given** un cambio de servidor activo, **When** se selecciona otro servidor, **Then** se limpia la caché y la sesión del anterior y se inicia sesión con las credenciales del nuevo.
4. **Given** una instalación sin sesión y sin servidores, **When** se abre la app, **Then** nunca aparece una pantalla de login/registro; solo la bienvenida de servidor.

---

### User Story 3 - Corregir credenciales desde el error (Priority: P2)

Una persona con credenciales incorrectas (por ejemplo, contraseña cambiada en la instancia) ve un error de autenticación en Favoritos o Historial; al pulsar Reintentar, la app abre la ficha/edición del **servidor activo** para corregir email/contraseña.

**Why this priority**: Cierra el ciclo de recuperación sin pantallas de login y evita que el usuario quede bloqueado sin saber qué hacer.

**Independent Test**: Guardar un servidor con contraseña incorrecta, abrir Favoritos y pulsar Reintentar; comprobar que se abre la edición del servidor activo y que al corregir la contraseña los datos cargan.

**Acceptance Scenarios**:

1. **Given** credenciales incorrectas, **When** Favoritos o Historial muestran el error de autenticación, **Then** el botón Reintentar abre la edición del servidor activo.
2. **Given** la edición abierta desde ese error, **When** se corrigen las credenciales y se guarda, **Then** la app revalida, inicia sesión y los datos vuelven a cargar.
3. **Given** un error de red (no de credenciales), **When** se pulsa Reintentar, **Then** se reintenta la carga sin abrir la edición del servidor.

---

### Edge Cases

- Servidores migrados desde versiones anteriores sin credenciales: se conservan y su ficha pide email/contraseña; si el activo/por defecto no las tiene, el arranque abre la pantalla unificada (bloqueante) hasta completarlas.
- Credenciales válidas pero token revocado en el servidor: la app re-loguea con las credenciales guardadas sin pedir nada.
- Contraseña cambiada en la instancia: el error de autenticación lleva a editar el servidor activo (US3).
- Varios servidores con usuarios distintos: cada servidor guarda y usa sus propias credenciales, sin mezclarlas.
- Cambio de servidor con peticiones en vuelo: se cancelan/descartan las del anterior y se limpia la caché.
- Sin conexión al arrancar: se mantiene el último estado/caché y no se pide login.
- Errores 401 repetidos: no se entra en bucle de login; se muestra el error accionable.
- El usuario borra los datos de la app: se pierden las credenciales y vuelve a la bienvenida.
- Manifiestos, variantes y segmentos HLS: cada petición de reproducción lleva `Bearer`; el token MUST NOT viajar en la URL.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Un servidor guardado MUST incluir URL, alias, email y contraseña; la contraseña MUST almacenarse cifrada en el dispositivo y MUST NOT aparecer en logs ni en la URL.
- **FR-002**: MUST existir un único formulario de alta/edición de servidor (URL, alias, email, contraseña) usado en todos los casos: bienvenida sin servidores y sección Servidores (alta y edición).
- **FR-003**: El formulario MUST validar que URL, email y contraseña están completos antes de llamar al servidor, y MUST mostrar avisos en español sin texto técnico.
- **FR-004**: La app MUST autenticarse automáticamente contra el servidor con las credenciales guardadas (login → token) y MUST usar `Authorization: Bearer` en todas las peticiones que lo requieran, incluida la reproducción (proxy y subrecursos HLS) y Chromecast, sin pantallas de login/registro.
- **FR-005**: Ante un 401, la app MUST renovar el token una sola vez (refresh) y reintentar la petición; si no es posible, MUST mostrar un error de credenciales accionable.
- **FR-006**: Cuando el error mostrado sea de autenticación/credenciales, el botón Reintentar MUST abrir la ficha/edición del servidor activo; cuando el error sea de red, MUST reintentar la carga (FR-005 de la 0022 se ajusta a esto).
- **FR-007**: Al guardar la edición de un servidor, la app MUST revalidar las credenciales e iniciar sesión sin requerir reinicio manual ni pantallas adicionales; el campo contraseña MUST precargarse enmascarado y, si no se modifica, MUST conservarse la contraseña guardada.
- **FR-008**: Al cambiar de servidor activo, la app MUST limpiar la caché y la sesión del anterior e iniciar sesión con las credenciales del nuevo (FR-007 de la 0022 se mantiene).
- **FR-009**: Historial y favoritos MUST ser datos del usuario autenticado de ese servidor (se retira la semántica "compartidos por instancia" de la 0022).
- **FR-010**: Los servidores existentes sin credenciales MUST conservarse en la migración; si el servidor activo/por defecto no tiene credenciales, el arranque MUST abrir la pantalla unificada de servidor de forma bloqueante hasta completarlas.
- **FR-011**: Al arrancar sin ningún servidor, la app MUST seguir mostrando la bienvenida, ahora con el formulario completo (URL, alias, email y contraseña).
- **FR-012**: La app MUST mantener la política de solo conexiones cifradas (HTTPS) y MUST NOT enviar credenciales en la URL.
- **FR-013**: Todos los errores del backend `{error:{code,message,status,details?}}` MUST mapearse a mensajes en español orientados a la acción.
- **FR-014**: El resto de funciones (explorar catálogo público, reproducir, ajustes, shortcuts) MUST seguir funcionando sin regresiones.

### Key Entities *(include if feature involves data)*

- **Servidor**: instancia configurada; atributos: URL, alias, email, contraseña (cifrada), activo y por defecto.
- **Sesión del servidor**: tokens de acceso/renovación obtenidos con las credenciales del servidor activo; el de acceso en memoria y el de renovación cifrado.
- **Contenido autenticado**: favoritos, historial y emisoras personalizadas del usuario de la instancia.
- **(Se retira) "Compartido por instancia"**: la 0022 definía favoritos/historial compartidos sin usuario; esta spec lo revierte a datos por usuario.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las altas de servidor (bienvenida o Servidores) usan el mismo formulario con URL, alias, email y contraseña.
- **SC-002**: 0 pantallas de login/registro y 0 solicitudes de email/contraseña fuera del formulario de servidor.
- **SC-003**: Con credenciales válidas, el 100% de las secciones autenticadas (Favoritos, Historial, Mis emisoras) cargan sin intervención del usuario.
- **SC-004**: Con credenciales incorrectas, el botón Reintentar abre la edición del servidor activo en el 100% de los casos.
- **SC-005**: 0 credenciales en logs o URLs; el 100% de las contraseñas se almacenan cifradas en el dispositivo.
- **SC-006**: Historial y favoritos reflejan los datos del usuario autenticado, no un agregado de la instancia.
- **SC-007**: 0 regresiones en explorar catálogo público, reproducción, ajustes y shortcuts.

## Assumptions

- La instancia usa login con email/contraseña y JWT con refresh rotatorio (contrato de las specs 001/007); el catálogo de emisoras sigue siendo público.
- Se acepta guardar la contraseña cifrada en el dispositivo (revierte la prohibición de la constitución 2.0.0; requiere enmienda constitucional explícita antes de implementar).
- El usuario es quien configura las credenciales al añadir/editar el servidor; no hay registro desde la app.
- Se reutiliza el patrón de la spec 007 (credenciales por servidor + auto-login) pero sin pantallas de login ni la sección Perfil.
- Los servidores guardados por la 0022 no tienen credenciales y se completarán desde la edición.

## Out of Scope

- Registro de cuentas, recuperación de contraseña y perfil de usuario desde la app.
- Biometría o desbloqueo de credenciales.
- Cambios en el backend (se documenta el contrato de auth asumido).
- Rediseño del reproductor o de la navegación.
