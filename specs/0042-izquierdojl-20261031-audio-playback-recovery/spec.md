# Feature Specification: Continuidad de reproducción y buffer ajustable

**Feature Branch**: `0042-izquierdojl-20261031-audio-playback-recovery`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "En esta aplicación, uno de los problemas que existe es que a veces se pierde el sonido de la emisora que está sonando, y para volver a recuperarlo, hay que volver a reconectar o incluso volver a salir o entrar de la aplicación. Explora si es posible mejorarlo aumentando buffer, o cambiando alguna configuración, incluso añadiéndola en la propia configuración de la aplicación para que el propio usuario pueda ajustársela"

## Clarifications

### Session 2026-10-31

- Q: ¿Cuándo se aplica un cambio de perfil de buffer mientras suena una emisora? → A: Inmediatamente, reconectando una vez la misma emisora con un aviso de corte breve.
- Q: ¿Qué debe hacer la app cuando vuelve la red tras un corte? → A: Reanudar automáticamente si el ajuste "Reanudar al recuperar conexión" está activo; el ajuste viene activado por defecto y es configurable.
- Q: ¿Cuántos reintentos automáticos debe hacer la app antes de mostrar el error? → A: Como máximo 5 intentos con espera creciente y un tope de 60 segundos en total.
- Q: ¿Cuánto arranque adicional es aceptable con "Máxima estabilidad"? → A: Como máximo 5 segundos adicionales frente a "Equilibrado" en las mismas condiciones de red.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Recuperación automática tras una pérdida de audio (Priority: P1)

El usuario está escuchando una emisora. Por un corte breve de red, una caída
del servidor o un fallo puntual del stream, el sonido se detiene aunque la app
sigue mostrando la emisora como activa. En lugar de quedarse en silencio, la
app detecta la interrupción, muestra un aviso de reconexión y recupera el audio
por sí sola, sin que el usuario tenga que pulsar nada ni salir de la app.

**Why this priority**: Es el problema reportado y el que más rompe la
experiencia: una radio debe sonar de forma continua y, cuando no puede, debe
avisar y ofrecer una salida clara.

**Independent Test**: Reproducir una emisora, cortar la red 10 segundos y
restaurarla. Verificar que el audio vuelve solo, en la misma emisora, sin
interacción del usuario y sin abrir/cerrar la app.

**Acceptance Scenarios**:

1. **Given** una emisora reproduciéndose, **When** el audio deja de avanzar
   durante más de 5 segundos sin que el usuario haya pausado, **Then** la app
   muestra el estado "Reconectando" en el reproductor y en la notificación.
2. **Given** una interrupción transitoria de red de hasta 15 segundos,
   **When** la conectividad vuelve, **Then** el audio se reanuda en la misma
   emisora en menos de 10 segundos desde la recuperación de la red, sin
   interacción del usuario.
3. **Given** una recuperación automática correcta, **Then** la reproducción
   continúa en el punto en directo de la emisora y no se crea una entrada
   duplicada en el historial.
4. **Given** que el usuario pausó o detuvo la reproducción manualmente,
   **When** transcurre un periodo de silencio, **Then** la app no intenta
   reconectar ni reanuda el audio por su cuenta.

---

### User Story 2 - Reintento manual sin salir de la app (Priority: P2)

Cuando la recuperación automática no basta, el usuario ve un mensaje claro y
un botón para reintentar la conexión desde el propio reproductor. Un solo toque
vuelve a conectar la emisora, sin tener que buscar la emisora de nuevo ni
cerrar y abrir la aplicación.

**Why this priority**: Elimina el trabajo manual que hoy hace el usuario
(reconectar o reiniciar la app) y da control cuando el fallo persiste.

**Independent Test**: Provocar un fallo de stream que agote los reintentos
automáticos y comprobar que aparece el error con "Reintentar"; pulsarlo una vez
y verificar que el audio vuelve.

**Acceptance Scenarios**:

1. **Given** una reproducción detenida por un fallo, **When** el usuario abre
   el mini-player o el reproductor completo, **Then** ve un mensaje en español
   que explica el problema y una acción "Reintentar".
2. **Given** el estado de error visible, **When** el usuario pulsa
   "Reintentar", **Then** la app muestra el progreso de conexión y, si tiene
   éxito, reanuda la misma emisora conservando el volumen.
3. **Given** el estado de error visible, **When** el usuario pulsa
   "Reintentar" varias veces seguidas, **Then** la app no abre conexiones
   paralelas ni reproduce audio duplicado.
4. **Given** el usuario cambia de emisora mientras se reintenta la anterior,
   **Then** el reintento anterior se cancela y solo suena la emisora nueva.

---

### User Story 3 - Ajustes de reproducción: buffer y reanudación (Priority: P2)

El usuario quiere adaptar el comportamiento del audio a su red y a su forma de
escuchar. En Configuración puede elegir entre un perfil de bajo consumo, uno
equilibrado y uno de máxima estabilidad, y decidir si el audio debe reanudarse
solo cuando vuelve la conexión. La app explica en español qué gana y qué pierde
con cada opción, recuerda las elecciones y las aplica sin reinstalar ni
reiniciar la aplicación.

**Why this priority**: Responde a la petición explícita de que el propio
usuario pueda ajustar el buffer y permite compensar redes lentas o inestables
sin afectar a todos los usuarios por igual; el control de reanudación evita
sorpresas cuando el usuario prefiere silencio tras un corte.

**Independent Test**: Entrar en Configuración, cambiar el perfil de buffer y el
ajuste de reanudación, cerrar y reabrir la app y verificar que ambas
selecciones se conservan y que la reproducción usa el comportamiento elegido.

**Acceptance Scenarios**:

1. **Given** la pantalla de Configuración, **When** el usuario busca ajustes de
   reproducción, **Then** encuentra una opción "Buffer de audio" a no más de
   tres toques desde Configuración.
2. **Given** una instalación nueva, **Then** el perfil seleccionado por defecto
   es "Equilibrado".
3. **Given** el selector de buffer, **When** el usuario elige
   "Máxima estabilidad", **Then** se muestra una descripción que indica mayor
   tolerancia a cortes a cambio de hasta 5 segundos más de arranque y mayor
   consumo de datos.
4. **Given** el usuario elige "Ahorro de datos", **Then** se muestra una
   descripción que indica arranque más rápido y menor consumo a cambio de mayor
   sensibilidad a los cortes de red.
5. **Given** un perfil seleccionado, **When** el usuario cierra y reabre la
   app, **Then** el perfil sigue seleccionado.
6. **Given** una emisora reproduciéndose, **When** el usuario cambia el perfil,
   **Then** el cambio se aplica inmediatamente reconectando una sola vez la
   misma emisora, con un aviso visible del breve corte, manteniendo la emisora
   activa y volviendo al punto en directo.
7. **Given** la pantalla de Configuración, **When** el usuario busca el ajuste
   de reanudación, **Then** encuentra "Reanudar al recuperar conexión",
   activado por defecto, con una descripción en español de su efecto.
8. **Given** el ajuste "Reanudar al recuperar conexión" desactivado, **When**
   la red vuelve tras un corte, **Then** la emisora permanece en pausa hasta
   que el usuario pulse play.

---

### User Story 4 - Continuidad al volver a la app (Priority: P3)

El usuario escucha la radio, bloquea el teléfono o cambia a otra aplicación y,
al volver, espera encontrar la misma emisora sonando o, si se perdió la
conexión mientras estaba fuera, que la app se recupere o le ofrezca reintentar
sin tener que salir y entrar de nuevo.

**Why this priority**: El usuario menciona salir y entrar de la app como
workaround; la app debe eliminar esa necesidad y mantener el contexto de
escucha.

**Independent Test**: Reproducir una emisora, enviar la app a segundo plano o
bloquear el dispositivo durante varios minutos, volver y comprobar que el
estado es coherente y que el audio se recupera o se ofrece "Reintentar".

**Acceptance Scenarios**:

1. **Given** una emisora reproduciéndose en segundo plano, **When** el usuario
   vuelve a la app, **Then** el mini-player y el reproductor completo muestran
   la misma emisora y el estado real de reproducción.
2. **Given** que la conexión se perdió mientras la app estaba en segundo plano,
   **When** el usuario vuelve, **Then** la app ha intentado recuperarse o
   muestra el error con "Reintentar", sin exigir cerrar y abrir la aplicación.
3. **Given** una recuperación en curso, **When** el usuario rota la pantalla o
   navega a otra sección, **Then** el estado "Reconectando" se mantiene y la
   recuperación no se interrumpe.

---

### Edge Cases

- **Sin conectividad total (modo avión)**: la app no debe entrar en un bucle de
  reintentos ni consumir batería; muestra "Sin conexión" y reanuda cuando la
  red vuelva, si la reproducción estaba activa antes del corte y el ajuste de
  reanudación está activo.
- **Usuario pausa durante la reconexión**: se cancela la recuperación y no se
  reanuda solo.
- **Cambio de emisora durante la reconexión**: se cancela la recuperación de la
  emisora anterior y nunca suenan las dos.
- **Temporizador de apagado activo**: si vence durante la reconexión, la
  reproducción se detiene y no se reintenta.
- **Llamada entrante o pérdida de foco de audio**: se aplica el comportamiento
  existente de pausa/reanudación; no se confunde con un corte de stream.
- **Desconexión de auriculares o Bluetooth**: se pausa según el comportamiento
  existente; no se reconecta automáticamente.
- **Credenciales caducadas (401/403)**: la app intenta renovar la sesión; si no
  es posible, muestra el error con la acción de editar el servidor, sin
  reintentos ciegos.
- **Emisora retirada o servidor caído de forma permanente (404/503)**: tras los
  intentos acotados, se muestra un error accionable y se deja de reintentar.
- **Red muy lenta con "Máxima estabilidad"**: el arranque puede tardar más; la
  app muestra "Cargando" y no un silencio sin explicación. El umbral de 5 s
  aplica una vez que la emisora ha empezado a sonar; durante la carga inicial se
  toleran hasta 10 s antes de considerar un fallo.
- **Cortes repetidos en poco tiempo**: los reintentos tienen espera creciente y
  un límite; la app no entra en un bucle.
- **Reproducción por Chromecast activa**: la recuperación del audio local no
  debe arrancar sonido en el teléfono; el comportamiento de Cast se mantiene.
- **App en segundo plano o pantalla apagada**: la recuperación debe funcionar
  sin que el usuario abra la app.
- **Reproducción en directo**: tras reconectar se vuelve al punto en directo,
  no se reanuda audio antiguo acumulado.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST detectar que una emisora está en estado de
  reproducción pero no entrega audio de forma sostenida durante más de 5
  segundos, sin confundirlo con una pausa, un cambio de emisora o una pérdida
  de foco de audio.
- **FR-002**: El sistema MUST clasificar la interrupción al menos en: falta de
  conectividad, stream detenido, error del reproductor y error de credenciales,
  y MUST aplicar una respuesta distinta a cada una.
- **FR-003**: Ante una interrupción recuperable, el sistema MUST intentar
  reconectar automáticamente con espera creciente entre intentos, hasta un
  máximo de 5 intentos y sin superar 60 segundos en total.
- **FR-004**: Cuando una reconexión automática tiene éxito, el sistema MUST
  reanudar la misma emisora en el punto en directo, conservando el volumen y
  sin requerir interacción del usuario.
- **FR-005**: Durante la reconexión, el sistema MUST mostrar un estado visible
  y coherente en el mini-player, el reproductor completo y la notificación
  multimedia.
- **FR-006**: Si se agotan los intentos automáticos, el sistema MUST pasar a un
  estado de error con un mensaje en español y una acción "Reintentar"; MUST NOT
  permanecer en silencio mostrando que sigue reproduciendo.
- **FR-007**: El usuario MUST poder reintentar la conexión con un solo toque
  desde el mini-player, el reproductor completo y la notificación multimedia.
- **FR-008**: Los reintentos manuales MUST ser idempotentes: pulsaciones
  repetidas no deben abrir conexiones paralelas ni producir audio duplicado.
- **FR-009**: El sistema MUST NOT intentar reconectar cuando el usuario pausó o
  detuvo manualmente, cuando cambió de emisora, cuando venció el temporizador de
  apagado o cuando la reproducción está inactiva.
- **FR-010**: El sistema MUST ofrecer en Configuración un ajuste "Buffer de
  audio" con tres perfiles: "Ahorro de datos", "Equilibrado" y "Máxima
  estabilidad", con "Equilibrado" como valor por defecto en una instalación
  nueva.
- **FR-011**: Cada perfil MUST mostrar una descripción en español de su efecto
  sobre el arranque, el consumo de datos y la tolerancia a cortes.
- **FR-012**: La selección del perfil MUST persistir entre sesiones y MUST
  aplicarse sin reinstalar ni reiniciar la aplicación, como máximo en la
  siguiente conexión.
- **FR-013**: Si el usuario cambia el perfil mientras una emisora suena, el
  sistema MUST aplicar el cambio inmediatamente reconectando una sola vez la
  misma emisora, con un aviso visible del breve corte, sin perder la emisora
  activa y volviendo al punto en directo.
- **FR-014**: Al recuperarse la conectividad, si el ajuste "Reanudar al
  recuperar conexión" está activo y la reproducción estaba activa antes del
  corte y el usuario no la había pausado, el sistema MUST reanudar
  automáticamente; si el ajuste está desactivado o el usuario pausó, MUST
  permanecer en pausa.
- **FR-015**: Sin conectividad, el sistema MUST NOT ejecutar reintentos en
  bucle; MUST esperar a la recuperación de la red o a una acción del usuario.
- **FR-016**: Ante un error de credenciales (401/403), el sistema MUST intentar
  renovar la sesión y, si no es posible, ofrecer la acción de editar el servidor
  activo, sin reintentos ciegos del stream.
- **FR-017**: Ante una emisora no disponible de forma permanente (404/503), el
  sistema MUST detener los reintentos tras el límite y mostrar un error
  accionable.
- **FR-018**: La recuperación MUST funcionar con la app en primer plano, en
  segundo plano y con la pantalla apagada.
- **FR-019**: Una recuperación correcta MUST NOT duplicar la entrada del
  historial ni registrar reproducciones que no ocurrieron.
- **FR-020**: La recuperación y los cambios de perfil MUST NOT alterar el
  comportamiento existente de foco de audio, pausa por auriculares
  desconectados, temporizador de apagado, controles de la notificación ni
  reproducción por Chromecast.
- **FR-021**: El sistema MUST registrar de forma estructurada cada intento de
  recuperación (causa, número de intento, resultado y duración) sin incluir
  datos personales ni credenciales y con el identificador de emisora
  anonimizado.
- **FR-022**: Al volver a la app tras un periodo en segundo plano, el sistema
  MUST reflejar el estado real de reproducción y MUST ofrecer recuperación o
  reintento si el audio se perdió mientras estaba fuera.
- **FR-023**: El sistema MUST ofrecer en Configuración el ajuste "Reanudar al
  recuperar conexión", activado por defecto, con una descripción en español de
  su efecto, y MUST persistir su valor entre sesiones.
- **FR-024**: El perfil "Máxima estabilidad" MUST limitar el tiempo de arranque
  a un máximo de 5 segundos adicionales frente a "Equilibrado" en las mismas
  condiciones de red.

### Key Entities

- **Sesión de reproducción**: emisora activa, estado visible (inactiva,
  cargando, reproduciendo, pausada, reconectando, error), motivo del último
  fallo y número de intentos de recuperación.
- **Perfil de buffer**: identificador, nombre visible, descripción del
  compromiso entre arranque, consumo de datos y estabilidad, y marca de perfil
  por defecto.
- **Preferencia de reanudación**: indicador de si el audio debe reanudarse solo
  al recuperar la conexión, activado por defecto.
- **Evento de recuperación**: causa, número de intento, instante de inicio y
  fin, resultado y referencia anonimizada a la emisora.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En al menos el 90% de 20 interrupciones transitorias simuladas de
  entre 5 y 15 segundos, el audio se recupera automáticamente en menos de 10
  segundos desde que vuelve la red, sin interacción del usuario.
- **SC-002**: En el 100% de los escenarios de fallo probados, la app termina en
  audio funcionando o en un error visible con "Reintentar" dentro de 60
  segundos; nunca queda en silencio indefinido mostrando que reproduce.
- **SC-003**: El 100% de los escenarios de la matriz de pruebas (corte de red,
  modo avión, error 5xx, stream detenido, segundo plano prolongado y rotación)
  se resuelve sin cerrar ni reiniciar la aplicación.
- **SC-004**: Los ajustes de reproducción (buffer y reanudación) son accesibles
  en tres toques o menos desde Configuración y conservan su valor tras
  reiniciar la app en el 100% de las comprobaciones.
- **SC-005**: En una prueba controlada de red débil, el perfil "Máxima
  estabilidad" produce al menos un 50% menos de interrupciones audibles que el
  perfil "Equilibrado", sin añadir más de 5 segundos al tiempo de arranque.
- **SC-006**: El 100% de las recuperaciones correctas conserva la emisora y no
  genera entradas duplicadas en el historial.
- **SC-007**: Tras 30 minutos sin conectividad y con una emisora activa antes
  del corte, la app no realiza reintentos automáticos mientras no hay red ni
  muestra un consumo de batería perceptible atribuible a reintentos.
- **SC-008**: Al menos 8 de cada 10 usuarios de una prueba guiada indican que
  ya no necesitan reconectar manualmente ni reiniciar la app ante un corte.
- **SC-009**: La batería de pruebas existente de foco de audio, auriculares,
  temporizador de apagado y Chromecast sigue pasando al 100%, sin regresiones.
- **SC-010**: En el 100% de las instalaciones nuevas, "Reanudar al recuperar
  conexión" aparece activado; tras desactivarlo y reiniciar la app, sigue
  desactivado.

## Assumptions

- El alcance cubre la reproducción local en el teléfono. La recuperación de una
  sesión de Chromecast queda fuera de esta versión y mantiene su comportamiento
  actual.
- El perfil de buffer se aplica únicamente al reproductor local; Chromecast usa
  el buffer de su receptor y no se ve afectado por este ajuste.
- Se usan tres perfiles predefinidos, no valores numéricos de buffer
  introducidos por el usuario, para evitar configuraciones inválidas y mantener
  la interfaz comprensible. Los valores concretos de cada perfil se deciden en
  la fase de planificación.
- La recuperación de cortes está activada siempre; el usuario controla si el
  audio se reanuda al volver la red mediante el ajuste "Reanudar al recuperar
  conexión", activado por defecto. No se expone un interruptor para desactivar
  los reintentos automáticos.
- Las emisoras son streams en directo: tras una reconexión se vuelve al punto
  en directo y no se reanuda audio acumulado.
- El comportamiento ante llamadas, pérdida de foco de audio y desconexión de
  auriculares se rige por la spec 0019 y no se modifica.
- El mini-player persistente y la notificación multimedia existentes (spec 010)
  se reutilizan; no se introduce un segundo reproductor.
- La reproducción sigue pasando exclusivamente por el proxy autenticado del
  servidor (spec 0021); esta feature no cambia el contrato del backend.
- El perfil de buffer se guarda localmente en el dispositivo y no se sincroniza
  entre servidores ni dispositivos.
- La app puede observar el estado de conectividad del sistema sin solicitar
  permisos nuevos.
- Todos los textos visibles nuevos estarán en español.
- No se prevén dependencias nuevas; la solución se apoya en el reproductor y
  los ajustes ya presentes (principio V, YAGNI).
