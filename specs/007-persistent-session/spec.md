# Feature Specification: Gestión de Sesión Persistente, Servidores y Configuración

**Feature Branch**: `007-persistent-session`

**Created**: 2026-09-06

**Status**: Done (2026-09-06; todo verificado manualmente en emulador por el usuario)

**Input**: User description: "que se guarde la sesión en la app de forma persistente, ahora si cierro e inicio, me pide usuario y clave cuando selecciono cualquier posición, como historial o favoritos. Propongo hacer apartado de gestión de servidores y credenciales y que se guarden persistentes en teléfono para no tener que introducirlos cada vez"

**Enmienda (2026-09-06)**: Rediseño conforme al modelo de usuario:
un solo usuario (perfil), muchos servidores con URL + credenciales
propias; sección **Servidores** de primer nivel (no dentro de
Perfil); **servidor activo** ≠ **servidor por defecto**; Perfil
se sustituye por **Configuración** (tema + pantalla de arranque +
logout).

## Clarifications

### Session 2026-09-06

- Q: ¿Cómo se identifica de forma único un servidor para evitar duplicados? → A: Alias definido por el usuario (el usuario nombra cada servidor, se permiten duplicados).
- Q: ¿Qué pasa con los datos en caché al cambiar de servidor? → A: Limpiar caché al cambiar y cargar datos frescos del nuevo servidor.

### Session 2026-09-06 (enmienda: modelo Servidores + Configuración)

- Q: ¿Dónde vive la gestión de servidores? → A: Sección **Servidores** propia de primer nivel en la navegación, NO dentro de Perfil/Configuración.
- Q: ¿Servidor activo vs servidor por defecto? → A: Son conceptos distintos: el **activo** es el que se usa en la sesión actual; el **por defecto** es el que se conecta al arrancar la app.
- Q: ¿Qué credenciales se guardan por servidor? → A: **Refresh token cifrado + email + password cifrados**. El refresh token se usa para uso normal; el password queda como respaldo para re-login automático si el token es revocado. Nada de esto sale en logs.
- Q: ¿Qué contiene Configuración (sustituye a Perfil)? → A: Modo claro/oscuro + pantalla de arranque seleccionable (Favoritos, Historial, Explorar) + Cerrar sesión. El nombre de usuario y el cambio de contraseña quedan fuera de esta fase.
- Q: ¿Qué pasa al arrancar la app sin sesión válida? → A: Si hay servidores guardados (con sus credenciales), se muestra la **lista de Servidores** para que el usuario seleccione a cuál conectarse (auto-login con las credenciales de ese servidor); solo si el servidor elegido no tiene credenciales se pide login. Si no hay servidores guardados, se muestra Login.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Login con persistencia automática (Priority: P1)

El usuario inicia sesión con email y contraseña, la app guarda
las credenciales de forma segura en el dispositivo, y al cerrar
y reabrir la app la sesión se restaura automáticamente sin pedir
credenciales. El usuario puede acceder a Historial, Favoritos y
todas las secciones autenticadas directamente.

**Why this priority**: Es el problema principal reportado por el
usuario — cada vez que cierra la app pierde la sesión y tiene que
volver a introducir usuario y contraseña. Sin esto la experiencia
de uso es muy frustrante.

**Independent Test**: hacer login, cerrar la app completamente,
reabrirla y acceder a Historial o Favoritos sin que pida
credenciales.

**Acceptance Scenarios**:

1. **Given** usuario autenticado, **When** cierro la app
   completamente y la reabro, **Then** la sesión se restaura
   automáticamente y puedo acceder a Historial y Favoritos sin
   introducir credenciales.
2. **Given** sesión guardada, **When** el access token ha caducado,
   **Then** la app renueva el token automáticamente con el refresh
   token guardado y la sesión continúa sin interrupción.
3. **Given** refresh token inválido o revocado, **When** intento
   restaurar la sesión, **Then** se me redirige a Login con un
   aviso de que la sesión ha expirado y se limpian las credenciales
   guardadas.
4. **Given** usuario con credenciales guardadas, **When** pulso
   "Cerrar sesión", **Then** se eliminan las credenciales del
   dispositivo y se me redirige a la pantalla de Login.

---

### User Story 2 - Gestión de servidores guardados (Priority: P2)

El usuario puede ver una lista de servidores (instancias
TolochaRadio) guardados en una sección **Servidores** propia de
primer nivel. Cada servidor guarda su URL, alias y credenciales
(email + password cifrados + refresh token) de forma independiente.
Existe un **servidor activo** (el que se usa en la sesión actual)
y un **servidor por defecto** (el que se conecta al arrancar la
app). Al cambiar de servidor la sesión se restaura automáticamente
con las credenciales guardadas de ese servidor, sin pedir nada.

**Why this priority**: Permite a usuarios con múltiples instancias
cambiar entre ellas transparentemente. Es una mejora de usabilidad
pero no bloqueante para el uso básico.

**Independent Test**: desde la sección Servidores se añade un
servidor (URL + credenciales), se ve la lista indicando cuál es el
activo y cuál el por defecto, se selecciona otro y la app cambia
de instancia con auto-login.

**Acceptance Scenarios**:

1. **Given** sin servidores guardados, **When** añado la URL de
   mi instancia y hago login, **Then** el servidor se guarda en
   la lista y queda como activo y por defecto.
2. **Given** servidores guardados, **When** abro la sección
   Servidores, **Then** veo la lista con URL, alias, email usado
   y qué servidor es el **activo** y cuál el **por defecto**.
3. **Given** múltiples servidores guardados con credenciales,
   **When** selecciono uno diferente, **Then** la app cambia a
   esa instancia, limpia la caché del anterior y hace login
   automático con las credenciales guardadas de ese servidor sin
   pedirme nada.
4. **Given** un servidor guardado, **When** lo elimino, **Then**
   se borran sus credenciales (token y password cifrados) y datos
   asociados del dispositivo.
5. **Given** dos servidores con URLs similares, **When** añado el
   segundo, **Then** se permite como entrada separada con su
   propio alias y credenciales independientes.
6. **Given** el refresh token de un servidor revocado, **When**
   intento usar ese servidor, **Then** la app re-autentica
   automáticamente con el password guardado (cifrado) sin pedirme
   nada, y renueva el token.

---

### User Story 3 - Seguridad de credenciales almacenadas (Priority: P1)

Las credenciales (tokens y datos de sesión) se almacenan de
forma cifrada en el dispositivo usando las APIs de seguridad
nativas de Android. El usuario puede ver qué datos están
guardados y eliminarlos. Las credenciales nunca se exponen
en logs ni se transmiten en texto plano.

**Why this priority**: La seguridad de las credenciales es
fundamental — guardar tokens sin cifrado sería un riesgo
grave de seguridad. Debe implementarse junto con la
persistencia.

**Independent Test**: verificar que los tokens se almacenan
en EncryptedSharedPreferences/DataStore cifrado, que no
aparecen en logs, y que al cerrar sesión se eliminan
correctamente.

**Acceptance Scenarios**:

1. **Given** login exitoso, **When** verifico el almacenamiento,
   **Then** el refresh token está en EncryptedSharedPreferences
   y el access token solo está en memoria.
2. **Given** app en segundo plano, **When** el sistema Android
   cierra la app por memoria, **Then** al reabrir la sesión se
   restaura desde el almacenamiento cifrado.
3. **Given** usuario cierra sesión, **When** verifico el
   almacenamiento, **Then** no quedan tokens ni datos de sesión
   en el dispositivo.
4. **Given** error de cifrado o almacenamiento, **When** no se
   pueden guardar/recuperar credenciales, **Then** la app pide
   login de nuevo sin crash.

---

### User Story 4 - Configuración en lugar de Perfil (Priority: P1)

El usuario dispone de una sección **Configuración** (que sustituye
a la actual Perfil) donde configura el modo claro/oscuro, la
**pantalla de arranque** de la app (Favoritos, Historial o
Explorar) y cierra la sesión. Al abrir la app con sesión
restaurada, la app se sitúa directamente en la pantalla de
arranque configurada.

**Why this priority**: Es parte central del nuevo modelo de
navegación del usuario; elimina la sección Perfil y define dónde
empieza la app cada día.

**Independent Test**: abrir Configuración, cambiar el tema (se
aplica al instante), elegir "Favoritos" como pantalla de arranque,
cerrar la app, reabrirla → la app abre en Favoritos (con sesión).

**Acceptance Scenarios**:

1. **Given** usuario en Configuración, **When** cambio el tema
   claro/oscuro, **Then** se aplica inmediatamente y queda
   persistente entre sesiones.
2. **Given** usuario en Configuración, **When** selecciono
   "Favoritos" como pantalla de arranque, **Then** la elección
   se guarda y, al reabrir la app con sesión, se abre directamente
   Favoritos.
3. **Given** usuario en Configuración, **When** pulso "Cerrar
   sesión", **Then** se limpian las credenciales del servidor
   activo y la app vuelve al flujo de login.
4. **Given** usuario autenticado, **When** navego, **Then** la
   sección Perfil ya no existe y en su lugar está Configuración.

---

### Edge Cases

- Dispositivo sin soporte para EncryptedSharedPreferences
  (API < 23, aunque minSdk=26): usar fallback seguro o pedir
  login cada vez.
- Se permiten servidores duplicados (misma URL) con alias
  diferentes; la deduplicación es responsabilidad del usuario.
- Refresh token guardado pero servidor inaccesible: mostrar
  estado offline con reintento, no pedir login inmediatamente.
- Refresh token revocado pero password guardado: re-login
  automático silencioso con el password cifrado (FR-006b); si
  también el password falla, pedir login.
- Dos instancias con el mismo email pero diferentes: cada
  servidor tiene sus propias credenciales independientes.
- Cambio de servidor activo: la caché se limpia completamente
  antes de cargar datos del nuevo servidor; el servidor por
  defecto NO cambia (se conserva para el próximo arranque).
- Usuario borra datos de la app desde ajustes de Android:
  las credenciales se eliminan y la app pide login al reabrir.
- Sin pantalla de arranque configurada: se usa Explorar por
  defecto.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La app MUST persistir la sesión de autenticación
  (refresh token cifrado) de forma que sobreviva al cierre
  completo de la aplicación y al cierre forzado por el sistema
  operativo.
- **FR-002**: Al iniciar la app con una sesión guardada, la app
  MUST restaurar la sesión automáticamente sin pedir credenciales
  al usuario, verificando validez con `POST /auth/refresh` si el
  access token ha caducado.
- **FR-003**: Las credenciales MUST almacenarse usando
  EncryptedSharedPreferences o equivalente cifrado nativo de
  Android. El access token SOLO debe estar en memoria; el refresh
  token cifrado en almacenamiento persistente.
- **FR-004**: La app MUST ofrecer una sección **Servidores** de
  primer nivel (no dentro de Configuración) donde el usuario puede
  ver, añadir, seleccionar y eliminar instancias TolochaRadio
  guardadas.
- **FR-005**: Cada servidor guardado MUST almacenar: URL de la
  instancia, alias, email del usuario, **password cifrado**,
  **refresh token cifrado** (si hay sesión), y sus dos estados:
  **activo** (en uso en la sesión actual) y **por defecto** (el
  que se conecta al arrancar la app). Solo un servidor puede estar
  activo y solo uno puede ser el por defecto a la vez.
- **FR-006**: Al cambiar entre servidores, la app MUST hacer
  login automático con las credenciales (token o password
  cifrados) guardadas de ese servidor, sin pedir nada al usuario,
  y limpiar la caché del servidor anterior.
- **FR-006b**: Si el refresh token de un servidor está revocado,
  la app MUST re-autenticar automáticamente con el password
  cifrado guardado y renovar el token, sin pedirla al usuario.
- **FR-007**: Al cerrar sesión, la app MUST eliminar las
  credenciales del servidor actual del almacenamiento persistente
  y limpiar el estado de la sesión en memoria.
- **FR-008**: Al eliminar un servidor de la lista, la app MUST
  eliminar todas sus credenciales (token y password cifrados),
  datos en caché y datos asociados del dispositivo.
- **FR-008b**: Al cambiar entre servidores, la app MUST limpiar
  la caché local (favoritos, historial, emisoras personalizadas)
  del servidor anterior antes de cargar datos del nuevo servidor.
- **FR-009**: Las credenciales (tokens y passwords) NUNCA deben
  aparecer en logs, crash reports, ni transmisiones en texto
  plano. El access token NUNCA debe persistirse en disco.
- **FR-010**: Si falla el acceso al almacenamiento cifrado o la
  recuperación de credenciales, la app MUST redirigir a Login
  sin crash, logueando el error de forma estructurada sin
  exponer datos sensibles.
- **FR-011**: La app MUST ofrecer una sección **Configuración**
  (que sustituye a Perfil) con: modo claro/oscuro con aplicación
  inmediata, **pantalla de arranque** seleccionable (Favoritos,
  Historial o Explorar) y Cerrar sesión. El nombre de usuario y
  el cambio de contraseña quedan FUERA de esta fase.
- **FR-011b**: Al abrir la app con sesión restaurada, la app MUST
  situarse directamente en la pantalla de arranque configurada
  (Favoritos, Historial o Explorar; por defecto Explorar).
- **FR-014**: Si al arrancar la app no hay token o usuario válido,
  la app MUST mostrar la lista de servidores guardados (con sus
  credenciales) para que el usuario seleccione a cuál conectarse.
  Seleccionar un servidor con credenciales MUST auto-loguearse
  (refresh o password cifrado) sin pedir nada. Si el servidor
  elegido no tiene credenciales, se pide login para ese servidor.
  Si no hay servidores guardados, se muestra Login.
- **FR-012**: Al añadir un nuevo servidor, la app MUST validar
  la URL contra `GET /health` antes de guardarlo, igual que
  hace actualmente con `baseUrl`.
- **FR-013**: Cada funcionalidad nueva (persistencia de sesión,
  gestión de servidores, almacenamiento cifrado, configuración)
  MUST tener tests unitarios y de integración correspondientes.

### Key Entities

- **SavedServer**: `{id(UUID), url, alias, userEmail, isActive,
  isDefault, createdAt}` — instancia TolochaRadio guardada. El
  alias es el identificador mostrado al usuario (duplicados
  permitidos). `isActive` = en uso en la sesión actual;
  `isDefault` = el que se conecta al arrancar la app.
- **StoredCredentials**: `{serverId, refreshTokenEncrypted,
  passwordEncrypted, userId, userEmail, lastLoginAt}` —
  credenciales cifradas (token + password) por servidor, para
  auto-login y re-login tras revocación.
- **AuthSession**: `{accessToken(en memoria), refreshToken,
  isAuthenticated, user}` — estado de sesión actual.
- **ServerList**: `{servers: SavedServer[], activeServerId,
  defaultServerId}` — estado de la lista de servidores.
- **AppSettings**: `{themeMode(light|dark|system),
  startScreen(favorites|history|explore)}` — configuración de la
  app (pantalla de arranque; por defecto Explorar).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los usuarios que cierran y reabren la
  app con sesión válida acceden directamente a sus secciones
  sin introducir credenciales.
- **SC-002**: La restauración de sesión al abrir la app tarda
  menos de 2 segundos en condiciones normales de red.
- **SC-003**: Un usuario con 3 servidores guardados puede
  cambiar entre ellos en menos de 5 segundos sin introducir
  credenciales.
- **SC-004**: Cero tokens o credenciales aparecen en logs,
  traces o almacenamiento no cifrado (verificable con
  revisión de código y herramientas de análisis estático).
- **SC-005**: La gestión de servidores permite añadir un
  nuevo servidor en menos de 30 segundos (introducir URL +
  validar + login).

## Assumptions

- La app ya tiene implementado `POST /auth/refresh` con
  refresh token rotatorio según la constitución del proyecto.
- EncryptedSharedPreferences está disponible en minSdk=26
  (API 26+) sin necesidad de librerías adicionales.
- Un usuario puede tener cuentas diferentes en diferentes
  instancias TolochaRadio (multi-cuenta por servidor).
- La instancia actual de `baseUrl` configurada en la app se
  migrará automáticamente al nuevo sistema de servidores
  guardados como primer servidor predeterminado.
- El acceso a secciones autenticadas (Historial, Favoritos,
  etc.) ya está implementado; esta spec solo garantiza que la
  sesión persista entre cierres de la app.
- No se implementará desbloqueo biométrico (huella/face) en
  esta fase; el acceso es transparente con las credenciales
  guardadas.
