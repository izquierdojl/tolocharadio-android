# Feature Specification: Emisoras recientes en los accesos directos del icono

**Feature Branch**: `0018-jlizquierdo-20260910-app-shortcut-recent-stations`

**Created**: 2026-09-10

**Status**: Done (2026-09-10; todo verificado manualmente en emulador)

**Input**: User description: "Emisoras frecuentes en icono al pulsar. Algunas aplicaciones, cuando pulsas sostenidamente el icono de acceso directo de aplicación, muestran un menú personalizado con una selección. Me gustaría que se vieran los últimos registros del historial, por ejemplo los ultimos 4 o 5 y que al pulsar se reprodujera directamente. Planifica esto"

**Contexto de producto**: la app ya registra historial de reproducción por cuenta (spec `005-history-management`) y publica información de reproducción en el sistema (spec `0016-...-notification-app-focus`). Esta feature reutiliza ese historial como fuente para el menú que el sistema operativo muestra al mantener pulsado el icono de la app, con reproducción directa desde cada elemento.

## Clarifications

### Session 2026-09-10

- Q: Al tocar una emisora en el menú del icono con la app cerrada, ¿qué pantalla debe verse al abrirse la app mientras empieza a sonar? → A: Se abre el reproductor a pantalla completa con la emisora ya sonando.
- Q: Si en el historial aparece una emisora personalizada (creada por mí en "Mis emisoras"), ¿debe aparecer también en el menú del icono? → A: Se incluyen todas las emisoras del historial, también las personalizadas, en el mismo orden de recencia.
- Q: Si al abrir o sincronizar la app no hay conexión, ¿qué debe mostrar el menú del icono? → A: Se muestra la última lista conocida guardada en el dispositivo, sin avisos; si al pulsar no hay red, se aplica el manejo de error de reproducción existente.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver mis últimas emisoras al mantener pulsado el icono (Priority: P1)

Una persona con cuenta y con historial de escucha mantiene pulsado el icono de TolochaRadio en la pantalla de inicio y ve, junto a las acciones habituales del sistema, un menú con sus últimas emisoras escuchadas (hasta 4-5, las más recientes primero y sin repetidas). Cada elemento muestra al menos el nombre de la emisora.

**Why this priority**: es el corazón del pedido — sin esta lista visible, no existe la funcionalidad. Entrega valor por sí sola y se puede demostrar sin reproducir nada.

**Independent Test**: con una cuenta que tiene al menos 4 emisoras en el historial, mantener pulsado el icono en el lanzador y comprobar que aparecen esas emisoras ordenadas de más reciente a más antigua y sin duplicados.

**Acceptance Scenarios**:

1. **Given** una cuenta con historial de 6 emisoras distintas, **When** mantengo pulsado el icono de la app, **Then** veo hasta las 4-5 más recientes (según permita el lanzador) ordenadas de más reciente a más antigua.
2. **Given** una cuenta cuyo historial tiene la misma emisora reproducida 3 veces entre otras, **When** mantengo pulsado el icono, **Then** esa emisora aparece exactamente una vez, en la posición de su reproducción más reciente.
3. **Given** una cuenta sin historial, **When** mantengo pulsado el icono, **Then** no aparece ningún acceso directo a emisora (solo las acciones propias del sistema, si las hubiera).
4. **Given** sesión cerrada (sin cuenta), **When** mantengo pulsado el icono, **Then** no aparece ningún acceso directo a emisora.
5. **Given** un historial con una emisora personalizada entre las más recientes, **When** mantengo pulsado el icono, **Then** esa emisora personalizada aparece en el menú igual que las del catálogo, en su posición de recencia.

---

### User Story 2 - Reproducir directamente desde el acceso del icono (Priority: P1)

Una persona toca una emisora en el menú del icono y la radio empieza a sonar directamente, con el reproductor a pantalla completa, sin pasos intermedios. Funciona igual si la app estaba cerrada, en segundo plano o abierta en ese momento.

**Why this priority**: el usuario pidió explícitamente "que al pulsar se reprodujera directamente"; sin esto el menú es solo un adorno.

**Independent Test**: con la app cerrada (proceso terminado), mantener pulsado el icono, tocar una emisora y comprobar que la app arranca y suena esa emisora; repetir con la app en segundo plano y en primer plano.

**Acceptance Scenarios**:

1. **Given** la app no está en ejecución, **When** toco una emisora en el menú del icono, **Then** la app se abre en el reproductor a pantalla completa y comienza a reproducir esa emisora en menos de 5 segundos con red normal.
2. **Given** la app está en segundo plano reproduciendo otra emisora, **When** toco otra emisora en el menú del icono, **Then** la app pasa a primer plano mostrando el reproductor a pantalla completa y cambia la reproducción a la emisora elegida sin cierres ni pantallas intermedias.
3. **Given** la app está abierta en cualquier sección, **When** toco una emisora del menú del icono, **Then** la reproducción empieza o cambia a esa emisora y el reproductor a pantalla completa la muestra.
4. **Given** la sesión caducó, **When** toco una emisora del menú del icono, **Then** la app intenta renovar la sesión de forma transparente; si no lo consigue, me lleva a la pantalla de inicio de sesión con un aviso claro y no intenta reproducir nada.

---

### User Story 3 - Menú siempre al día con mi historial (Priority: P2)

Después de escuchar una emisora nueva, eliminar una entrada del historial o limpiar todo el historial, el menú del icono refleja el cambio la próxima vez que uso la app en primer plano. Nunca muestra emisoras que ya no están en mi historial ni deja huecos con datos viejos.

**Why this priority**: mantiene la confianza en el menú; un listado desactualizado lleva a reproducir algo que el usuario ya no esperaba ver. No bloquea el núcleo (US1/US2) pero es necesario para que la función sea fiable.

**Independent Test**: reproducir una emisora nueva, volver a la pantalla de inicio y comprobar que aparece la primera en el menú del icono; después limpiar el historial y comprobar que los accesos de emisora desaparecen.

**Acceptance Scenarios**:

1. **Given** el menú del icono muestra mis 4 emisoras más recientes, **When** reproduzco una emisora nueva desde la app y vuelvo a la pantalla de inicio, **Then** el menú muestra esa emisora en primera posición en menos de 5 segundos desde la reproducción.
2. **Given** una emisora presente en el menú del icono, **When** la elimino individualmente de mi historial, **Then** deja de aparecer en el menú.
3. **Given** historial con varias emisoras, **When** limpio todo el historial, **Then** el menú del icono deja de mostrar accesos de emisora.
4. **Given** cambios en mi historial hechos en otro dispositivo, **When** vuelvo a poner la app en primer plano y después mantengo pulsado el icono, **Then** el menú refleja el historial actualizado.

---

### User Story 4 - Nunca mostrar datos de otra cuenta (Priority: P2)

Al cerrar sesión o cambiar de cuenta, los accesos de emisoras de la cuenta anterior desaparecen del menú del icono de inmediato y nunca se muestran a la nueva cuenta.

**Why this priority**: es privacidad básica; el historial es dato personal y no debe filtrarse entre cuentas ni quedar visible en el lanzador tras cerrar sesión.

**Independent Test**: con emisoras visibles en el menú, cerrar sesión y comprobar que el menú ya no muestra ninguna emisora; iniciar sesión con otra cuenta sin historial y comprobar que sigue sin mostrar emisoras.

**Acceptance Scenarios**:

1. **Given** el menú del icono muestra emisoras de mi cuenta, **When** cierro sesión, **Then** ningún acceso de emisora permanece visible en el menú.
2. **Given** cerré sesión y otra persona inicia sesión con su cuenta sin historial, **When** mantiene pulsado el icono, **Then** no ve ninguna emisora de mi cuenta.
3. **Given** cambio de cuenta con historial propio, **When** mantengo pulsado el icono, **Then** solo veo las emisoras de la cuenta activa.

---

### Edge Cases

- ¿Qué pasa si hay más emisoras en el historial que huecos permite el lanzador? Se muestran solo las más recientes que quepan; el resto quedan accesibles desde la app. Nunca se muestran más de los huecos disponibles ni se desplazan las acciones propias del sistema.
- ¿Qué ocurre si la emisora del acceso directo ya no existe o no es reproducible? La app muestra un mensaje en español comprensible con opción de reintentar o elegir otra emisora; nunca se queda en pantalla en blanco ni se cierra.
- ¿Qué pasa si el lanzador o el dispositivo no soportan accesos directos, o la persona los desactivó? La app funciona con normalidad; simplemente no aparece el menú personalizado.
- ¿Qué ocurre si se pulsa el mismo acceso directo mientras esa emisora ya está sonando? No se reinicia ni se duplica la reproducción; el estado visible sigue siendo coherente.
- ¿Qué pasa si el historial cambia mientras la app está en segundo plano? Los cambios se aplican cuando la app vuelve a primer plano; hasta entonces el menú puede mostrar la última versión conocida.
- ¿Qué pasa si no hay conexión al usar la app? El menú conserva la última lista conocida guardada en el dispositivo; si se pulsa un acceso sin red, se muestra el error de reproducción con reintento, sin cierres ni pantallas en blanco.
- ¿Qué ocurre en un dispositivo bloqueado? Para abrir la app y reproducir se requiere desbloquear el dispositivo, igual que al abrir desde una notificación.
- ¿Qué pasa si una emisora tiene un nombre muy largo? La etiqueta se recorta de forma legible en el menú sin romper la identificación de la emisora.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Al mantener pulsado el icono de la app, el sistema MUST ofrecer un menú con accesos directos a las últimas emisoras únicas del historial de la cuenta activa (incluidas las emisoras personalizadas presentes en el historial), ordenadas de más reciente a más antigua.
- **FR-002**: El número de accesos de emisora MUST adaptarse a los huecos que permita el lanzador (objetivo 4-5), reservando primero los huecos de las acciones propias del sistema; nunca se deben eliminar ni desplazar acciones estáticas existentes.
- **FR-003**: El historial MUST deduplicarse por emisora para el menú: una emisora reproducida varias veces aparece una sola vez, en la posición de su reproducción más reciente.
- **FR-004**: Sin historial o sin sesión activa, el menú NO MUST mostrar ningún acceso directo a emisora.
- **FR-005**: Al pulsar un acceso directo de emisora, la app MUST abrirse o pasar a primer plano mostrando el reproductor a pantalla completa con la emisora elegida sonando, sin pantallas ni confirmaciones intermedias.
- **FR-006**: La reproducción directa MUST funcionar con la app cerrada (arranque en frío), en segundo plano y en primer plano; si ya había otra emisora sonando, MUST cambiarse a la elegida de forma controlada.
- **FR-007**: Si al pulsar un acceso directo la sesión ha caducado, el sistema MUST intentar renovarla de forma transparente una vez; si falla, MUST llevar a inicio de sesión con aviso en español, sin reproducir y sin errores técnicos.
- **FR-008**: Cada acceso directo MUST mostrar el nombre de la emisora y, cuando el lanzador lo permita, su imagen. Solo se admite información pública de la emisora; PROHIBIDO incluir datos de la cuenta o del historial personal.
- **FR-009**: El menú MUST actualizarse cuando cambie el historial (nueva reproducción, eliminación individual, limpieza, cambio de cuenta) mientras la app esté en primer plano; si el cambio ocurrió en segundo plano, MUST aplicarse al volver a primer plano.
- **FR-010**: Al cerrar sesión o cambiar de cuenta, los accesos de emisoras de la cuenta anterior MUST eliminarse de inmediato del menú y MUST NOT mostrarse a cualquier otra cuenta.
- **FR-011**: Si la emisora de un acceso directo ya no existe o no es reproducible, la app MUST mostrar un mensaje en español con acción de recuperación (reintentar o elegir otra) y MUST NOT quedarse colgada, en blanco o cerrarse.
- **FR-012**: La función MUST degradarse con elegancia cuando el lanzador no soporte accesos directos o la persona los haya desactivado: la app sigue funcionando igual y sin errores visibles.
- **FR-013**: Pulsar un acceso directo de una emisora que ya está sonando MUST NOT reiniciar, duplicar ni interrumpir la reproducción de forma incorrecta.
- **FR-014**: Las etiquetas largas MUST recortarse de forma legible en el menú, preservando el reconocimiento de la emisora.
- **FR-015**: Sin conexión, el menú MUST seguir mostrando la última lista conocida guardada en el dispositivo; al pulsar un acceso sin red, la app MUST aplicar el manejo de error de reproducción existente (mensaje en español con reintento) sin cierres ni pantallas en blanco.

### Key Entities

- **AccesoDirectoEmisora**: representación de una emisora dentro del menú del icono; incluye la emisora, la etiqueta visible (nombre), una imagen opcional, la posición según recencia y una identidad estable que permite reproducirla aunque cambie su nombre.
- **EntradaDeHistorial**: registro de una reproducción (`station` + momento de la reproducción); es la fuente de datos del menú, ya existente en la app (spec 005).
- **CuentaActiva**: identidad con sesión iniciada en el dispositivo; determina a quién pertenecen los accesos mostrados y el momento en que deben limpiarse (cierre de sesión o cambio de cuenta).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con al menos 4 emisoras en el historial, 9 de cada 10 personas ven sus emisoras recientes al mantener pulsado el icono en el primer intento, sin instrucciones.
- **SC-002**: Pulsar un acceso directo con sesión válida inicia la reproducción en menos de 5 segundos con red normal, en el 100 % de las pruebas y en los tres estados (cerrada, segundo plano, primer plano).
- **SC-003**: Tras cerrar sesión o cambiar de cuenta, el 100 % de las verificaciones muestran 0 accesos directos pertenecientes a la cuenta anterior.
- **SC-004**: Tras reproducir una emisora nueva con la app en primer plano, el menú del icono refleja el nuevo orden en 5 segundos o menos.
- **SC-005**: Con historial vacío o sin sesión, no aparece ningún acceso de emisora en el 100 % de los casos.
- **SC-006**: Ante una emisora no disponible, el 100 % de los intentos muestran un mensaje comprensible y la app permanece estable (sin cierres inesperados ni pantallas en blanco).
- **SC-007**: En al menos 3 lanzadores distintos, el número de accesos mostrados nunca excede los huecos permitidos por el lanzador y las acciones propias del sistema siguen disponibles.

## Assumptions

- La app soporta accesos directos del icono desde Android 7.1 (`minSdk 26`); el lanzador decide cuántos mostrar (habitualmente 4-5) y la app se adapta a ese máximo.
- El historial existente (spec 005) es la única fuente de datos; se usa la lista deduplicada por emisora y ordenada por recencia, sin lógica nueva de negocio sobre qué es "frecuente".
- Se requiere sesión iniciada: sin cuenta no hay historial ni accesos de emisora; el login/registro ya existe (spec 001).
- Las actualizaciones de los accesos solo pueden garantizarse con la app en primer plano (limitación del sistema operativo); los cambios hechos con la app en segundo plano se aplican al volver a primer plano.
- El menú puede construirse con la última lista de historial disponible en el dispositivo (caché de lectura); no requiere conexión para mostrarse y se refresca desde el servidor al volver a primer plano.
- La reproducción directa reutiliza el flujo existente de reproducción autenticada y su comprobación previa de disponibilidad de la emisora.
- Los nombres e imágenes de emisoras se consideran información pública (mismo criterio que la spec 0016 para notificaciones).
- En esta iteración no se crean accesos para búsquedas, sugerencias ni favoritas; la fuente es solo el historial reciente (que puede incluir emisoras personalizadas, tal como se aclaró).
