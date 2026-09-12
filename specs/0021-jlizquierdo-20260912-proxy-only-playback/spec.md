# Feature Specification: Reproducción unificada por el proxy autenticado

**Feature Branch**: `0021-jlizquierdo-20260912-proxy-only-playback`

**Created**: 2026-09-12

**Status**: Draft

**Input**: User description: "El backend ya resuelve las listas m3u/m3u8/pls en el proxy de playback. Hay que revertir la excepción que dejamos en la spec 0019 y volver a reproducir todas las emisoras por el proxy autenticado, restaurando el historial server-side y el preestado de disponibilidad, y eliminando el código de resolución de listas que ya no hace falta en el cliente."

## Clarifications

### Session 2026-09-12

- Q: ¿El preestado de disponibilidad vuelve a ser bloqueante para emisoras de lista, como lo es para streams directos? → A: Sí; el preestado es bloqueante para todo tipo de emisora. Si el servicio la marca como no disponible, la app no arranca el reproductor y muestra el motivo con opción de reintentar.
- Q: ¿Debe la app seguir funcionando contra instancias del servicio aún no actualizadas, o se asume una instancia actualizada que ya resuelve listas? → A: Se asume una instancia actualizada; no se conserva modo de compatibilidad con servidores antiguos. Las notas de la versión deben indicar que hace falta una instancia actualizada para las emisoras de lista.
- Q: Ahora que la URI siempre apunta al proxy, ¿cómo decide la app si reproduce con el motor HLS o con el progresivo? → A: Se mantiene la detección por extensión de la URL de la emisora solo para elegir el motor (`.m3u8` → HLS); la reproducción siempre apunta al proxy.
- Q: Si una lista `.m3u`/`.pls` resuelve a un HLS, ¿se mitiga ahora, se bloquea la spec o se documenta con seguimiento? → A: Se documenta el límite en esta spec y se abre un issue en el backend para que las listas de texto que apunten a HLS se sirvan también reescritas. No bloquea el cierre del issue #4.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Escuchar cualquier emisora con la misma ruta (Priority: P1)

Un usuario reproduce indistintamente emisoras de stream directo y emisoras cuyo enlace es una lista (`.m3u`, `.m3u8` o `.pls`). En todos los casos la reproducción viaja por el servicio autenticado, de modo que su sesión, su historial y el control de disponibilidad se comportan igual para todas las emisoras, sin depender del formato del enlace.

**Why this priority**: Es la corrección de la excepción registrada como deuda (issue #4). Restituye el registro de historial para las emisoras de lista y elimina una vía de reproducción paralela y no auditada.

**Independent Test**: Reproducir una emisora directa y una de cada formato de lista y comprobar, en la app y en el tráfico hacia el servicio, que todas pasan por la misma ruta autenticada y que se registran en el historial.

**Acceptance Scenarios**:

1. **Given** una emisora de stream directo, **When** el usuario la reproduce, **Then** la reproducción se sirve por el proxy autenticado y queda registrada en su historial.
2. **Given** una emisora de lista (`.m3u`, `.m3u8` o `.pls`), **When** el usuario la reproduce, **Then** la reproducción se sirve por el mismo proxy autenticado y queda registrada en su historial.
3. **Given** una emisora personalizada creada por el usuario con enlace de lista, **When** se reproduce, **Then** se comporta igual que una emisora del catálogo por la misma ruta.
4. **Given** una emisora cuya lista no se puede resolver, **When** el usuario intenta reproducirla, **Then** ve un error accionable con reintento y la app no se bloquea ni se cierra.

---

### User Story 2 - Emisión HLS continua a través del servicio (Priority: P2)

El usuario escucha una emisora HLS (`.m3u8`) y la reproducción es continua: manifiesto, variantes y segmentos se sirven a través del servicio autenticado, sin que el usuario note ninguna diferencia ni tenga que intervenir.

**Why this priority**: HLS es el formato de lista más habitual; es el caso técnicamente más delicado porque implica múltiples subrecursos encadenados que deben seguir autenticados.

**Independent Test**: Reproducir varias emisoras HLS durante varios minutos y comprobar que la emisión no se corta y que los subrecursos también pasan por el servicio autenticado.

**Acceptance Scenarios**:

1. **Given** una emisora HLS con manifiesto de variantes y segmentos, **When** el usuario la reproduce, **Then** la emisión suena de forma continua durante toda la prueba.
2. **Given** la reproducción HLS en curso, **When** el reproductor solicita manifiestos, variantes o segmentos, **Then** todas esas peticiones viajan autenticadas por el servicio.
3. **Given** una interrupción breve de red durante un HLS, **When** la conexión se recupera, **Then** la emisión se reanuda sin reiniciar manualmente.

---

### User Story 3 - Errores de lista claros y sin cierres (Priority: P3)

El usuario intenta reproducir una emisora de lista que no está disponible (vacía, solo con conexiones no seguras, formato no reconocido o inalcanzable) y recibe un mensaje comprensible que distingue el motivo, con opción de reintentar.

**Why this priority**: Mantiene la calidad de experiencia ya exigida en la app (nunca pantalla negra ni crash) ahora que los errores los decide el servicio.

**Independent Test**: Provocar de forma controlada cada tipo de fallo de lista y verificar mensaje accionable, reintento y ausencia de cierres.

**Acceptance Scenarios**:

1. **Given** una lista vacía, **When** el usuario la reproduce, **Then** ve un mensaje específico y puede reintentar.
2. **Given** una lista con solo entradas no cifradas, **When** el usuario la reproduce, **Then** ve un mensaje que explica el bloqueo por seguridad.
3. **Given** una lista con formato no reconocido o inaccesible, **When** el usuario la reproduce, **Then** ve un mensaje de error de resolución distinto del de red.

---

### Edge Cases

- Autenticación de subrecursos HLS: manifiestos, variantes y segmentos deben llevar la misma credencial que el stream principal, sin exponerla en la URL.
- Lista que apunta a otra lista: se trata como un candidato más gestionado por el servicio, sin recursión desde el cliente.
- Emisora de lista de texto (`.m3u`/`.pls`) cuya primera entrada es un HLS (`.m3u8`): el servicio actual la sirve sin reescribir y la app la trataría como progresiva, por lo que puede fallar. Se documenta como límite conocido y se aborda con un issue de seguimiento en el backend (ver FR-014 y Out of Scope).
- Cambio de red o caída del servicio durante la reproducción: error accionable con reintento, sin cierre.
- Chromecast y reanudación tras desconexión: deben usar la misma fuente autenticada que la reproducción local.
- Instancia del servicio no actualizada que todavía no resuelve listas: no se soporta; una emisora de lista puede fallar y se muestra un error accionable, sin intentar un modo de reproducción alternativo.
- Emisora de lista añadida a favoritos/historial: debe comportarse como cualquier otra emisora.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST reproducir todas las emisoras (de catálogo y personalizadas, tanto de stream directo como de lista) a través del proxy autenticado del servicio, y MUST NOT reproducirlas directamente contra la URL del proveedor.
- **FR-002**: La app MUST NOT descargar, interpretar ni resolver listas de reproducción en el dispositivo.
- **FR-003**: El sistema MUST reproducir las emisoras HLS de forma continua a través del servicio, siguiendo los manifiestos y subrecursos que este entregue, sin intervención del usuario. El motor de reproducción (HLS o progresivo) se determina por la extensión del enlace de la emisora (`.m3u8` → HLS); la URI de reproducción siempre apunta al proxy.
- **FR-004**: La autenticación MUST aplicarse a todas las peticiones al servicio, incluidos manifiestos, variantes y segmentos HLS; la credencial MUST NOT viajar en la URL ni enviarse a terceros.
- **FR-005**: El sistema MUST consultar el estado de disponibilidad antes de reproducir cualquier emisora y, si el servicio la marca como no disponible, MUST NOT arrancar el reproductor; MUST mostrar el motivo con opción de reintentar. Este preestado de disponibilidad (precheck) es bloqueante también para emisoras de lista, igual que para streams directos.
- **FR-006**: Los fallos de resolución de lista reportados por el servicio MUST mostrarse como errores accionables con reintento, sin bloquear ni cerrar la app, y MUST distinguir el motivo (vacía, no segura, no reconocible, inalcanzable) del de una caída de red.
- **FR-007**: La reproducción de emisoras de lista por el proxy MUST registrar historial en el servidor, igual que un stream directo; esto salda la deuda del issue #4.
- **FR-008**: MUST eliminarse el código de resolución de listas en el cliente y las abstracciones o dependencias que queden sin uso (complementa el comportamiento de FR-002), sin afectar a la reproducción de streams directos.
- **FR-009**: El cambio MUST NOT degradar la reproducción de streams directos ni de ninguna emisora ya soportada.
- **FR-010**: La reproducción en Chromecast y la reanudación tras una desconexión MUST usar la misma fuente autenticada que la reproducción local.
- **FR-011**: El sistema MUST mantener la política de solo conexiones cifradas; la app MUST NOT introducir URLs de proveedores no cifradas.
- **FR-012**: El cambio MUST declarar resuelta la excepción registrada en el issue #4 y actualizar las referencias de gobernanza asociadas (Complexity Tracking de la spec 0019).
- **FR-013**: La app MUST asumir una instancia del servicio actualizada que ya resuelve listas y MUST NOT conservar un modo de compatibilidad con instancias que no las resuelvan. El requisito de instancia actualizada MUST quedar documentado en las notas de la versión.
- **FR-014**: MUST abrirse un issue de seguimiento en el proyecto del servicio para que las listas de texto (`.m3u`/`.pls`) que resuelvan a un HLS se sirvan también como HLS reescrito; esta spec documenta el límite y MUST NOT intentar mitigarlo en el cliente. *(Abierto: `izquierdojl/tolocharadio#8`.)*

### Key Entities *(include if feature involves data)*

- **Fuente de reproducción**: origen de audio de una emisora. Tras este cambio todas las fuentes se sirven por el servicio autenticado; algunas son emisiones HLS con subrecursos encadenados.
- **Emisión HLS**: secuencia de manifiesto, variantes y segmentos servida por el servicio; requiere credencial en cada petición.
- **Entrada de historial**: registro en el servidor de que el usuario reprodujo una emisora; ahora también para emisoras de lista.
- **Emisora**: como ya se modela, con un enlace directo o de lista; aplica a catálogo y personalizadas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las emisoras de prueba (directas, `.m3u`, `.m3u8` y `.pls`) se reproducen a través del proxy autenticado, verificable en el tráfico hacia el servicio.
- **SC-002**: 0 peticiones de reproducción directas a hosts de proveedores desde la app.
- **SC-003**: El 100% de las reproducciones de emisoras de lista quedan registradas en el historial del usuario.
- **SC-004**: Al menos 3 emisoras HLS se reproducen de forma continua durante 5 minutos con 0 reinicios de reproducción no solicitados por el usuario.
- **SC-005**: 0 regresiones en la batería de pruebas existente de reproducción de streams directos.
- **SC-006**: El 100% de las listas no resolubles probadas muestran un error comprensible y accionable, sin cierres inesperados.
- **SC-007**: 0 referencias al código de resolución de listas en cliente en el proyecto tras el cambio.

## Assumptions

- El servicio ya resuelve listas y HLS en el proxy autenticado y registra el historial al consumirlo (cambio de backend desplegado).
- El servicio firma los subrecursos HLS de forma no falsificable y no reenvía la credencial del cliente a hosts de terceros.
- Se mantiene la capacidad de reproducción HLS en el dispositivo (necesaria para seguir los manifiestos servidos por el proxy).
- El estado de disponibilidad ya está disponible en el servicio para emisoras de lista.
- Se asume una instancia del servicio actualizada que ya resuelve listas; no se contemplan servidores antiguos (ver FR-013).
- No cambian el modelo de datos local ni la experiencia de usuario del reproductor.

## Out of Scope

- Cambios en el servicio backend (ya realizados en un proyecto aparte), salvo el issue de seguimiento para listas de texto que apunten a HLS.
- Soporte de formatos de lista adicionales (`.asx`, `.xspf`, etc.).
- Rediseño de la pantalla del reproductor o de la navegación.
