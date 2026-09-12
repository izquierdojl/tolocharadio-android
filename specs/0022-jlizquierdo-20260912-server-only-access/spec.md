# Feature Specification: Acceso solo con servidores y retirada de la autenticación de usuario

**Feature Branch**: `0022-jlizquierdo-20260912-server-only-access`

**Created**: 2026-09-12

**Status**: Done (2026-09-12; todo verificado manualmente en emulador por el usuario)

> **Nota de gobernanza (2026-09-12)**: la spec `0024-jlizquierdo-20260912-per-server-credentials` **parcialmente supersede** esta spec: se reintroducen credenciales por servidor (email y contraseña cifrada) y sesión automática JWT/`Bearer`, sin pantallas de login. Se mantiene el modelo "solo servidores" y la pantalla unificada, pero se invalidan FR-001/FR-004/FR-005/FR-008/FR-009/FR-011/FR-016 en lo relativo a "sin credenciales". El resto se conserva como histórico.

**Input**: User description: "Actualmente, cuando inicia la aplicación sin datos, solicita la URL del servicio, y luego pide usuario y contraseña cuando se quiere reproducir una emisora, o al acceder al historial. Es necesario simplificarlo para trabajar sólo con servidores: cuando accedamos por primera vez, si no hay servidores iniciados, el sistema debe indicar al usuario que debe configurar uno para poder acceder, y tenemos que desechar el sistema anterior de autentificación. Una vez configurado el servidor, ahora ya funciona bien y debe seguir igual."

## Clarifications

### Session 2026-09-12

- Q: ¿Debe la app enviar alguna credencial al servidor? → A: No: acceso totalmente sin credenciales; la app solo guarda la URL del servidor y no envía tokens ni usuario.
- Q: ¿Dónde viven el historial y los favoritos tras eliminar las cuentas? → A: En el servidor, compartidos por todo el que use esa instancia.
- Q: ¿Qué pasa si la instancia todavía exige usuario y contraseña? → A: No se soporta: se asume una instancia actualizada sin autenticación; no se conserva modo de compatibilidad.
- Q: ¿Qué se hace al actualizar con los servidores y credenciales ya guardados? → A: Se conservan los servidores ya configurados sin credenciales y se borran sesión, tokens y contraseñas.
- Q: ¿Cómo se guía al usuario sin servidores al primer arranque? → A: Con una pantalla de bienvenida/onboarding dedicada que explica y lleva a añadir el primer servidor.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Primer arranque te guía a configurar un servidor (Priority: P1)

Una persona abre la aplicación por primera vez (o tras haber eliminado todos los servidores) y no tiene ningún servidor configurado. En lugar de pedirle una URL y después usuario y contraseña, la aplicación le muestra una pantalla de bienvenida dedicada que le indica que debe configurar un servidor para poder acceder, con una acción directa para configurarlo. Hasta que no exista al menos un servidor, no se puede acceder al contenido.

**Why this priority**: Es el cambio central solicitado: sustituir el arranque actual (URL + credenciales) por un arranque basado solo en servidores. Sin esto, el resto de la simplificación no tiene punto de entrada.

**Independent Test**: Instalar la app sin datos, abrirla y comprobar que no pide usuario ni contraseña, que muestra el mensaje "configura un servidor para acceder" y que al configurar uno se entra al contenido.

**Acceptance Scenarios**:

1. **Given** una instalación sin servidores configurados, **When** se abre la aplicación, **Then** se muestra una pantalla de bienvenida dedicada que indica que hay que configurar un servidor para acceder, con una acción para configurarlo y sin pedir usuario ni contraseña.
2. **Given** el estado "sin servidores", **When** la persona configura un servidor válido, **Then** la aplicación guarda el servidor y da acceso al contenido sin solicitar credenciales.
3. **Given** el estado "sin servidores", **When** la persona intenta acceder a cualquier sección de contenido, **Then** no se accede y se mantiene la indicación de configurar un servidor.
4. **Given** una URL de servidor inválida o inalcanzable, **When** se intenta configurar, **Then** no se guarda y se muestra un mensaje en español con el motivo y opción de reintentar.

---

### User Story 2 - Uso diario sin credenciales (Priority: P1)

Una persona con al menos un servidor configurado explora emisoras, reproduce una emisora, consulta su historial y usa sus favoritos sin que la aplicación le pida usuario ni contraseña en ningún momento. El comportamiento es idéntico al actual una vez pasado el arranque.

**Why this priority**: Elimina la fricción reportada (peticiones de usuario y contraseña al reproducir o al entrar al historial) y preserva el valor ya existente de la aplicación.

**Independent Test**: Con un servidor configurado, reproducir una emisora y abrir Historial y Favoritos comprobando que no aparece ninguna pantalla de login ni se solicitan credenciales.

**Acceptance Scenarios**:

1. **Given** un servidor configurado, **When** la persona reproduce una emisora, **Then** la reproducción comienza sin pedir usuario ni contraseña.
2. **Given** un servidor configurado, **When** la persona abre Historial o Favoritos, **Then** el contenido se muestra sin pedir usuario ni contraseña.
3. **Given** un servidor configurado, **When** la persona usa Explorar, Mis emisoras o Ajustes, **Then** todas funcionan igual que antes de este cambio, sin credenciales.

---

### User Story 3 - Gestión de varios servidores sin credenciales (Priority: P2)

Una persona puede ver, añadir, seleccionar y eliminar servidores (instancias) desde la sección Servidores. Cada servidor se identifica por su URL y un alias, sin datos de usuario. Existe un servidor activo (el que se usa en la sesión) y un servidor por defecto (el que se usa al arrancar); cambiar de servidor no pide credenciales.

**Why this priority**: Conserva la capacidad ya existente de trabajar con varias instancias y la simplifica al quitar la parte de credenciales, condición necesaria para el modelo "solo servidores".

**Independent Test**: Añadir dos servidores, marcar uno como activo y otro como por defecto, cambiar entre ellos y comprobar que el contenido cambia sin introducir credenciales.

**Acceptance Scenarios**:

1. **Given** sin servidores guardados, **When** se añade uno, **Then** queda guardado y pasa a ser el activo y el por defecto.
2. **Given** varios servidores guardados, **When** se abre la sección Servidores, **Then** se ve la lista con URL, alias, activo y por defecto, sin ningún dato de usuario.
3. **Given** varios servidores guardados, **When** se selecciona otro como activo, **Then** la aplicación carga el contenido de ese servidor sin pedir credenciales y limpiando la caché del anterior.
4. **Given** un servidor guardado, **When** se elimina, **Then** desaparece de la lista y se borran sus datos asociados; si era el último, la aplicación vuelve al estado "sin servidores".

---

### Edge Cases

- Actualización desde una versión anterior que guardaba sesión y credenciales: los datos de usuario (tokens/email/password) se descartan y la aplicación arranca con el nuevo modelo, sin crash.
- Actualización desde una versión anterior con un servidor guardado con credenciales: el servidor se conserva sin sus credenciales y queda disponible.
- Servidor configurado pero inalcanzable al arrancar: estado de error accionable con reintento y acceso a la gestión de servidores, sin bloquear ni cerrar la aplicación.
- Instancia antigua que exija usuario y contraseña: no se soporta; se muestra un error accionable indicando que hace falta actualizar el servidor, sin ofrecer pantalla de login.
- Eliminar el último servidor: la aplicación vuelve al estado "sin servidores: configura uno".
- Cambio de servidor activo: se limpia la caché local del servidor anterior antes de cargar la del nuevo.
- El servidor deja de estar disponible durante el uso: mensajes accionables con reintento, sin pantallas en blanco ni cierres.
- Configuración de una URL no cifrada (http): se rechaza, manteniendo solo conexiones seguras.
- Añadir dos servidores con la misma URL: se permiten como entradas separadas con alias distintos.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La aplicación MUST funcionar sin autenticación de usuario y MUST NOT solicitar usuario ni contraseña en ningún flujo (reproducción, historial, favoritos, emisoras personalizadas, ajustes).
- **FR-002**: Al arrancar sin ningún servidor configurado, la aplicación MUST mostrar una pantalla de bienvenida/onboarding dedicada que indique que se debe configurar un servidor para poder acceder y MUST ofrecer la acción de configurarlo; MUST NOT permitir el acceso al contenido hasta que exista al menos un servidor.
- **FR-003**: La aplicación MUST tratar el servidor (instancia) como única unidad de configuración y acceso; cada servidor se define por su URL y un alias, y MUST NOT almacenar credenciales de usuario.
- **FR-004**: Al configurar un servidor, la aplicación MUST validar la URL contra el servicio antes de guardarlo y MUST rechazar URLs inválidas o inalcanzables con un mensaje en español y opción de reintentar.
- **FR-005**: Con al menos un servidor configurado, la aplicación MUST ofrecer las mismas funciones y contenidos que antes del cambio (explorar, reproducir, favoritos, historial, emisoras personalizadas, ajustes y reproducción en segundo plano) sin pedir credenciales.
- **FR-006**: La aplicación MUST permitir guardar varios servidores, con un servidor activo y uno por defecto, y cambiar entre ellos sin introducir credenciales.
- **FR-007**: Al cambiar de servidor activo, la aplicación MUST limpiar la caché local del servidor anterior y MUST cargar los datos del nuevo servidor.
- **FR-008**: La aplicación MUST eliminar el sistema de autenticación de usuario: pantallas de inicio de sesión, registro y recuperación de contraseña; gestión de sesión y de renovación de sesión; y cualquier estado de sesión asociado a un usuario.
- **FR-009**: Un servidor guardado MUST contener únicamente URL, alias y sus estados (activo/por defecto); MUST NOT contener email, contraseña, token de sesión ni cualquier otra credencial de usuario.
- **FR-010**: Al actualizar desde la versión anterior, los servidores ya configurados MUST conservarse sin sus credenciales, y los datos de autenticación (sesión, tokens, email y contraseña) MUST eliminarse de forma segura, sin provocar errores ni cierres.
- **FR-011**: Las peticiones de la aplicación al servidor MUST NOT incluir ninguna credencial (ni de usuario ni de instancia) y MUST mantener la política de solo conexiones cifradas (HTTPS).
- **FR-012**: Si el servidor configurado no responde al arrancar o durante el uso, la aplicación MUST mostrar un estado de error accionable (reintentar o editar servidor) sin bloquearse ni cerrarse.
- **FR-013**: Al eliminar el último servidor, la aplicación MUST devolver al usuario al estado "sin servidores: configura uno" descrito en FR-002.
- **FR-014**: El comportamiento del reproductor y del contenido una vez configurado el servidor MUST mantenerse igual que en la versión anterior a este cambio, sin regresiones.
- **FR-015**: El historial y los favoritos MUST almacenarse en el servidor y ser compartidos por cualquiera que use la instancia; MUST NOT depender de una cuenta de usuario ni gestionarse solo en el dispositivo.
- **FR-016**: La aplicación MUST asumir instancias del servicio actualizadas que no exigen autenticación y MUST NOT conservar un modo de compatibilidad con instancias que exijan credenciales. El requisito de instancia actualizada MUST quedar documentado en las notas de la versión.

### Key Entities *(include if feature involves data)*

- **Servidor**: instancia configurada por el usuario; atributos: URL, alias, activo (en uso en la sesión actual) y por defecto (se usa al arrancar). Ya no tiene credenciales de usuario.
- **Servidor activo / por defecto**: dos estados de un servidor; solo un servidor puede ser activo y solo uno puede ser el por defecto a la vez.
- **Contenido servido**: catálogo, favoritos, historial y emisoras personalizadas que el servidor entrega a la aplicación sin autenticación de usuario; favoritos e historial son compartidos por cualquiera que use la instancia.
- **(Retirado) Sesión y credenciales de usuario**: dejan de existir en el modelo; no se guardan ni se envían.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los arranques sin ningún servidor configurado muestran la indicación de configurar un servidor y no permiten acceder al contenido hasta que exista uno.
- **SC-002**: 0 solicitudes de usuario o contraseña en cualquier flujo de la aplicación (incluidos reproducción, historial y favoritos).
- **SC-003**: Una persona puede configurar el primer servidor y acceder al contenido en menos de 30 segundos.
- **SC-004**: 0 pantallas y 0 referencias del sistema de autenticación de usuario permanecen en la aplicación tras el cambio.
- **SC-005**: El 100% de las funciones disponibles antes del cambio (explorar, reproducir, favoritos, historial, emisoras personalizadas, ajustes) siguen funcionando tras configurar un servidor, sin regresiones.
- **SC-006**: 0 credenciales (de usuario o de instancia) almacenadas en el dispositivo ni enviadas al servidor.

## Assumptions

- El servicio (backend) permite el uso de la aplicación sin ninguna credencial: el servidor/instancia es la unidad de acceso y no exige credenciales de usuario ni de instancia.
- El historial y los favoritos dejan de estar asociados a una cuenta de usuario; pasan a estar asociados al servidor y son compartidos por cualquiera que use esa instancia, manteniendo la misma experiencia visible.
- Se reutiliza la sección **Servidores** existente para gestionar los servidores, eliminando de ella la parte de credenciales (email, contraseña, token); cuando no hay ningún servidor se añade una pantalla de bienvenida/onboarding dedicada que lleva a configurar el primero.
- La validación de un servidor se sigue haciendo contra el endpoint de salud del servicio antes de guardarlo.
- Se mantiene la política de HTTPS-only y la reproducción a través del servidor.
- El modelo deja de tener cuentas de usuario: no hay registro, inicio de sesión, recuperación de contraseña ni cierre de sesión.
- Los datos de sesión y credenciales de versiones anteriores se descartan durante la actualización, conservándose los servidores ya configurados sin sus credenciales.
