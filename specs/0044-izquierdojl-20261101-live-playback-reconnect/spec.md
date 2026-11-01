# Feature Specification: Reproducción continua y reconexión automática

**Feature Branch**: `0044-izquierdojl-20261101-live-playback-reconnect`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Adoptar el endpoint continuo de reproducción del backend (versión 0.29.0) para todas las emisoras, de forma que la app reciba audio progresivo también cuando el origen es HLS y no tenga que gestionar manifiestos ni caducidades. Incluye fallback para HLS no concatenable, supervisor de reconexión con espera acotada, comprobación de disponibilidad antes de reproducir, estados de interfaz claros, funcionamiento en segundo plano, diagnóstico estructurado y despliegue por fases."

## Clarifications

### Session 2026-11-01

- Q: En la fase 3, ¿debe conservarse la reproducción HLS como ruta de reserva para las emisoras que responden 415, o debe retirarse por completo el soporte HLS local? → A: Conservar HLS solo como ruta de reserva: la ruta principal es el flujo continuo y el manifiesto HLS queda únicamente como reserva para emisoras 415 o servidores sin flujo continuo; Cast no cambia.
- Q: ¿Cómo debe controlarse la activación por fases del nuevo flujo continuo? → A: Con una bandera de compilación por versión: la fase se decide al publicar, sin configuración remota ni ajuste visible; la fase 3 la activa por defecto.
- Q: Si la primera conexión falla antes de que la emisora llegue a sonar, ¿debe la app aplicar la misma política de 6 reintentos automáticos o mostrar el error antes? → A: Aplicar los 6 reintentos; mientras tanto se muestra "Conectando" con "Reintentando…" y "Reconectando…" se reserva para cortes tras haber sonado.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Escucha continua en emisoras HLS que caducan (Priority: P1)

El usuario elige una emisora cuyo origen es HLS y que, hasta ahora, dejaba de
sonar al cabo de aproximadamente un minuto porque la sesión del proveedor
caducaba. Al pulsar play, la app reproduce un flujo de audio continuo y la
emisora sigue sonando sin que el usuario tenga que recargar, volver a conectar
ni salir de la aplicación. Lo mismo ocurre con emisoras de stream directo y con
listas de reproducción: para el usuario todas se comportan igual.

**Why this priority**: Es el problema que motiva la propuesta y el que más
degrada la experiencia: una radio debe sonar de forma continua. Sin esta
historia, el resto de mejoras de reconexión no aporta valor sobre el problema
principal de las emisoras HLS.

**Independent Test**: Reproducir una emisora HLS problemática (por ejemplo,
Onda Cero) durante al menos 10 minutos y comprobar que el audio no se
interrumpe al renovarse la sesión del origen, sin interacción del usuario y sin
recargar la emisora.

**Acceptance Scenarios**:

1. **Given** una emisora HLS cuya sesión de origen caduca cada ~60 segundos,
   **When** el usuario la reproduce durante 10 minutos, **Then** el audio sigue
   sonando de forma continua, la app muestra el estado "Reproduciendo" y no se
   produce ninguna recarga manual.
2. **Given** una emisora de stream directo o una lista de reproducción,
   **When** el usuario pulsa play, **Then** la app reproduce el flujo continuo
   del backend y arranca en un tiempo comparable al actual.
3. **Given** una emisora HLS cuyo formato no puede entregarse como flujo
   continuo, **When** el usuario pulsa play, **Then** la app intenta una única
   vez la ruta de manifiesto existente y, si el formato es compatible, el audio
   suena; si no lo es, muestra un mensaje en español y no se produce ningún
   cierre inesperado.
4. **Given** un servidor anterior a la versión 0.29.0 que no ofrece el flujo
   continuo, **When** el usuario reproduce una emisora, **Then** la app usa la
   ruta existente sin mostrar un error y sin degradar la experiencia actual.

---

### User Story 2 - Reconexión automática ante cortes (Priority: P2)

El usuario está escuchando la radio y se produce un corte: se va la red unos
segundos, el origen cierra la conexión, el reproductor recibe un error o el
audio deja de avanzar. La app detecta la interrupción, muestra que está
reconectando y recupera el audio por sí sola en la misma emisora y en el punto
en directo, tanto si la app está abierta como si está en segundo plano o con la
pantalla apagada.

**Why this priority**: La continuidad no depende solo de que el origen no
caduque; también hay cortes de red y cierres de conexión. Sin reconexión
automática, el usuario sigue teniendo que intervenir a diario, que es
exactamente lo que esta propuesta quiere eliminar.

**Independent Test**: Reproducir una emisora de audio directo (por ejemplo,
Litera), cortar la red 10 segundos y restaurarla. Verificar que el audio vuelve
solo, en la misma emisora, sin interacción del usuario.

**Acceptance Scenarios**:

1. **Given** una emisora reproduciéndose, **When** la red se corta 10 segundos y
   vuelve, **Then** el audio se recupera automáticamente en menos de 20
   segundos desde que hay red, sin que el usuario pulse nada.
2. **Given** una emisora reproduciéndose, **When** el origen cierra la conexión
   o el reproductor notifica un error de red o del servidor, **Then** la app
   inicia la secuencia de reconexión con esperas de 0, 1, 2, 4, 8 y 15 segundos,
   hasta un máximo de 6 intentos.
3. **Given** una emisora reproduciéndose, **When** transcurren 15 segundos sin
   que avance la posición ni lleguen datos nuevos, **Then** la app lo trata
   como un corte y reconecta.
4. **Given** una emisora reproduciéndose, **When** el dispositivo cambia entre
   Wi-Fi y datos móviles, **Then** la app realiza una sola reconexión y no
   quedan dos conexiones ni dos audios activos.
5. **Given** una reconexión con éxito, **When** la emisora lleva 60 segundos
   reproduciéndose de forma estable, **Then** el contador de intentos se
   reinicia para un futuro corte.
6. **Given** un token de acceso a punto de caducar, **When** la app va a
   reconectar, **Then** lo renueva antes de abrir la nueva conexión; si la
   renovación falla, se detiene la reconexión y se ofrece editar el servidor
   activo.
7. **Given** una reconexión en curso, **When** se produce otro evento de corte,
   **Then** la app mantiene una sola reconexión en vuelo y no duplica
   conexiones ni audio.
8. **Given** que se agotan los 6 intentos, **Then** la app deja de reintentar
   automáticamente y muestra el estado de error con la acción "Reintentar".
9. **Given** una emisora reproduciéndose en segundo plano o con la pantalla
   apagada, **When** se produce un corte, **Then** la reconexión funciona igual
   que en primer plano.

---

### User Story 3 - Saber qué ocurre y poder reintentar (Priority: P2)

Mientras la app conecta o reconecta, el usuario ve un estado claro en lugar de
un silencio o un spinner eterno. Cuando la recuperación automática no es
posible, ve un mensaje comprensible en español y un botón "Reintentar" que
vuelve a conectar la emisora sin salir de la pantalla ni buscar la emisora otra
vez.

**Why this priority**: La transparencia evita que el usuario crea que la app se
ha colgado y elimina el trabajo manual de reconectar o reiniciar. Además, los
estados son la base sobre la que se apoyan las pruebas de recuperación.

**Independent Test**: Provocar un fallo que agote los reintentos automáticos,
comprobar que aparece el mensaje con "Reintentar" y verificar que un solo toque
recupera el audio.

**Acceptance Scenarios**:

1. **Given** una emisora que aún no ha empezado a sonar, **When** el primer
   intento de conexión falla, **Then** se muestra "Conectando" con
   "Reintentando…", la app aplica hasta 6 reintentos y nunca queda en un spinner
   indefinido: o pasa a reproducción o termina en el estado de error.
2. **Given** una emisora reproduciéndose, **When** se detecta un corte y la app
   reconecta, **Then** el mini-player y el reproductor completo muestran
   "Reconectando…", y la notificación multimedia refleja el estado de transporte
   (buffering) sin duplicar el texto detallado.
3. **Given** que se agotan los intentos, **When** el estado de error es visible,
   **Then** el usuario ve un mensaje en español que explica el problema y una
   acción "Reintentar".
4. **Given** el estado de error visible, **When** el usuario pulsa "Reintentar",
   **Then** la app muestra el progreso de conexión y, si tiene éxito, reanuda la
   misma emisora conservando el volumen.
5. **Given** el estado de error visible, **When** el usuario pulsa "Reintentar"
   varias veces seguidas, **Then** la app no abre conexiones paralelas ni
   reproduce audio duplicado.
6. **Given** una emisora nueva seleccionada durante una reconexión, **When** el
   usuario pulsa play, **Then** se cancela la reconexión anterior y solo suena
   la emisora nueva.

---

### User Story 4 - Comprobar disponibilidad antes de reproducir (Priority: P3)

Antes de arrancar el reproductor, la app comprueba si la emisora está
disponible. Si no lo está, el usuario lo sabe de inmediato y conoce el motivo,
sin esperar a un spinner que acabaría en error.

**Why this priority**: Mejora la percepción de rapidez y evita intentos inútiles,
pero no es imprescindible para que la reproducción continua y la reconexión
funcionen; por eso queda después de las historias principales.

**Independent Test**: Abrir una emisora marcada como no disponible y comprobar
que aparece el aviso con el motivo sin que el reproductor llegue a arrancar; en
una emisora disponible, comprobar que la comprobación no retrasa el play más de
2 segundos.

**Acceptance Scenarios**:

1. **Given** que el usuario abre la ficha de una emisora, **When** la
   comprobación de disponibilidad responde que no es reproducible, **Then** se
   muestra un aviso con el motivo en español y no se inicia la reproducción.
2. **Given** una emisora disponible, **When** el usuario pulsa play, **Then** la
   comprobación no retrasa el inicio más de 2 segundos y la reproducción
   continúa con normalidad.
3. **Given** que la comprobación no responde en 2 segundos, **When** el usuario
   pulsa play, **Then** la app intenta la reproducción igualmente y muestra el
   estado de conexión, sin bloquear la interfaz.
4. **Given** una emisora que se vuelve no disponible durante la escucha, **When**
   la reconexión falla, **Then** el aviso final usa el motivo más reciente
   disponible y mantiene la acción "Reintentar".

---

### Edge Cases

- **Formato HLS no concatenable (fMP4, MPEG-TS o cifrado)**: la app intenta una
  sola vez la ruta de manifiesto existente; si el reproductor admite el formato,
  suena; si no, muestra el aviso y no reintenta en bucle, porque es un error
  determinista.
- **Servidor antiguo sin flujo continuo (404/405)**: se usa la ruta existente de
  forma transparente y no se vuelve a probar el flujo continuo en la misma
  sesión de reproducción.
- **Token caducado o inválido (401/403)**: se renueva la sesión y se reintenta;
  si la renovación falla, se detiene la reconexión y se ofrece editar el
  servidor activo, sin reintentos ciegos del stream.
- **Emisora no disponible de forma permanente (404/503)**: tras los intentos
  acotados se muestra un error accionable y se deja de reintentar.
- **Sin conectividad total (modo avión)**: no se entra en un bucle de reintentos
  ni se consume batería; se muestra "Sin conexión" y se respeta el ajuste
  existente de reanudación al recuperar la red.
- **Usuario pausa o detiene durante la reconexión**: se cancela la recuperación
  y no se reanuda solo.
- **Cambio de emisora durante la reconexión**: se cancela la recuperación de la
  emisora anterior y nunca suenan las dos.
- **Temporizador de apagado vencido durante la reconexión**: la reproducción se
  detiene y no se reintenta.
- **Llamada entrante, pérdida de foco de audio, auriculares o Bluetooth**: se
  aplica el comportamiento existente de pausa/reanudación; no se confunde con un
  corte de stream.
- **Cambio de red Wi-Fi ↔ datos móviles**: se reconecta una sola vez y no se
  duplica el audio.
- **Estancamiento tras arrancar**: si pasan 15 segundos sin avanzar la posición
  ni llegar datos, se considera corte aunque el reproductor siga en estado
  activo.
- **Estancamiento antes de arrancar**: la conexión inicial nunca queda en
  spinner indefinido; se aplican hasta 6 reintentos con "Reintentando…" y, al
  agotar la política, se muestra el error con "Reintentar".
- **Cortes repetidos en poco tiempo**: la espera es creciente y el número de
  intentos está limitado; tras 60 segundos de reproducción estable se reinicia
  el contador.
- **Respuesta chunked sin longitud declarada**: la reproducción debe funcionar
  sin depender de un tamaño total de contenido.
- **Respuestas cacheadas por intermediarios**: cada conexión lleva un marcador
  de intento que evita reutilizar una respuesta anterior; el marcador no
  contiene credenciales ni datos personales.
- **Reproducción por Chromecast activa**: la recuperación del audio local no
  arranca sonido en el teléfono; el comportamiento de Cast se mantiene y no
  adopta el flujo continuo del proxy.
- **Reproducción en directo**: tras reconectar se vuelve al punto en directo, no
  se reanuda audio antiguo acumulado.
- **Historial**: una recuperación correcta no crea una entrada duplicada; el
  servidor sigue siendo la fuente de verdad.
- **Varias pulsaciones de "Reintentar"**: no se abren conexiones paralelas ni se
  produce audio duplicado.
- **App en segundo plano o pantalla apagada**: la reconexión sigue funcionando
  sin que el usuario abra la app.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Para todas las emisoras, la app MUST reproducir el flujo continuo
  ofrecido por el backend, con independencia de que el origen sea un stream
  directo, una lista de reproducción o un HLS concatenable.
- **FR-002**: La app MUST tratar la respuesta continua como audio progresivo,
  aceptar los tipos de audio ofrecidos por el backend y funcionar con respuestas
  sin longitud declarada, sin exigir un manifiesto.
- **FR-003**: Si el flujo continuo responde que el formato no es compatible, la
  app MUST intentar una única vez la ruta de manifiesto existente; si el
  reproductor admite el formato, MUST reproducirlo; si no, MUST mostrar el
  mensaje "Esta emisora usa un formato que este reproductor no admite" sin
  cerrarse y sin reintentar en bucle.
- **FR-004**: Si el flujo continuo no está disponible en el servidor (versión
  anterior), la app MUST usar la ruta existente de forma transparente, sin error
  visible y sin repetir la prueba del flujo continuo más de una vez por sesión
  de reproducción.
- **FR-005**: Cada conexión y reconexión MUST incluir un marcador de intento
  distinto para evitar respuestas cacheadas por intermediarios; el marcador MUST
  NOT contener credenciales, tokens ni datos personales.
- **FR-006**: La app MUST detectar interrupciones por error del reproductor
  (red, error del servidor o cierre del socket), por fin de la conexión del
  origen, por estancamiento de 15 segundos sin progreso ni datos y por cambio de
  red que invalide la conexión.
- **FR-007**: Ante una interrupción recuperable y con conectividad disponible,
  incluido el fallo de la primera conexión antes de que la emisora llegue a
  sonar, la app MUST reconectar con un máximo de 6 intentos y esperas de 0, 1,
  2, 4, 8 y 15 segundos; la secuencia completa MUST terminar en un máximo de 2
  minutos desde el primer fallo, aunque algún intento no responda, y el contador
  MUST reiniciarse tras 60 segundos de reproducción estable.
- **FR-008**: Antes de cada reconexión, la app MUST renovar el token de acceso
  si está a punto de caducar, reutilizando el flujo de renovación existente; si
  la renovación falla, MUST detener los reintentos y ofrecer la edición del
  servidor activo.
- **FR-009**: Cada reconexión MUST recrear el elemento de reproducción con el
  nuevo marcador de intento, mantener la misma emisora, conservar el volumen y
  volver al punto en directo.
- **FR-010**: La app MUST mantener una sola reconexión en vuelo; los eventos
  repetidos durante una reconexión MUST agruparse y MUST NOT abrir conexiones
  paralelas ni producir audio duplicado.
- **FR-011**: La app MUST representar el estado de reproducción con cuatro
  estados visibles: conectando (indicador de carga), reproduciendo (controles
  normales), reconectando ("Reconectando…") y fallido (mensaje y botón
  "Reintentar"). El estado conectando MUST mantenerse durante los reintentos de
  la primera conexión con el texto "Reintentando…", y reconectando MUST
  reservarse para cortes después de haber sonado. La app MUST NOT dejar un
  indicador de carga indefinido.
- **FR-012**: Tras agotar los intentos automáticos, la app MUST pasar al estado
  fallido y MUST dejar de reintentar en segundo plano hasta que el usuario pulse
  "Reintentar" o cambie de emisora.
- **FR-013**: La acción "Reintentar" MUST ser idempotente: pulsaciones repetidas
  MUST NOT abrir conexiones paralelas ni reproducir audio duplicado.
- **FR-014**: La app MUST NOT reconectar cuando el usuario pausó o detuvo
  manualmente, cuando cambió de emisora, cuando venció el temporizador de
  apagado, cuando hay una sesión de Cast activa o cuando la interrupción es una
  pérdida de foco de audio, una llamada o una desconexión de auriculares.
- **FR-015**: El supervisor de reconexión MUST vivir en el servicio de
  reproducción en segundo plano, de forma que funcione con la pantalla apagada,
  la app en segundo plano y la sesión multimedia activa.
- **FR-016**: Al abrir la ficha de una emisora o al pulsar play, la app MUST
  comprobar su disponibilidad; si no es reproducible, MUST avisar con el motivo
  en español y no iniciar el reproductor.
- **FR-017**: La comprobación de disponibilidad MUST NOT bloquear la interfaz
  más de 2 segundos; si no responde a tiempo, la app MUST intentar la
  reproducción y mostrar el estado de conexión.
- **FR-018**: Los estados de reproducción MUST ser coherentes entre el
  mini-player, el reproductor completo, la notificación multimedia y la pantalla
  de la emisora. La notificación MUST reflejar el estado de transporte
  (buffering/playing/paused) y ofrecer «Reintentar» en errores recuperables
  (`FAILED(RETRY)`); en `FAILED(EDIT_SERVER)` mantiene stop/mute y la app ofrece
  «Editar servidor», sin duplicar el texto detallado de las pantallas.
- **FR-019**: La app MUST registrar de forma estructurada cada intento de
  conexión y reconexión con el código HTTP, el código de error del reproductor,
  el número de intento y el motivo (red, fin de conexión, estancamiento, token,
  formato no compatible), usando un identificador de emisora anonimizado y sin
  credenciales ni datos personales.
- **FR-020**: Una recuperación correcta MUST NOT duplicar la entrada del
  historial ni registrar reproducciones que no ocurrieron; el servidor sigue
  siendo la fuente de verdad del historial y de las estadísticas.
- **FR-021**: La adopción del flujo continuo MUST poder activarse por fases: una
  primera fase, detrás de una bandera de compilación por versión, con el flujo
  continuo y la ruta de reserva; una segunda con el supervisor, los estados y la
  comprobación de disponibilidad; y una tercera en la que se activa por defecto
  y se retira el camino de manifiesto HLS como ruta principal, conservando el
  soporte HLS únicamente como reserva para emisoras que respondan 415 o para
  servidores que no ofrezcan el flujo continuo; Cast no cambia. Desactivar la
  funcionalidad mediante una compilación con la bandera desactivada MUST
  restaurar el comportamiento anterior sin pérdida de datos.
- **FR-022**: La funcionalidad MUST preservar el comportamiento existente de
  foco de audio, llamadas, auriculares y Bluetooth, temporizador de apagado,
  volumen, controles de la notificación, perfiles de buffer, reanudación al
  recuperar la red y reproducción por Chromecast.
- **FR-023**: Ante falta de conectividad, la app MUST NOT ejecutar reintentos en
  bucle; MUST mostrar un aviso de "Sin conexión" dentro del estado de
  reconexión, esperar a la recuperación de la red o a una acción del usuario y
  respetar la preferencia existente de reanudación.
- **FR-024**: Ante un error de credenciales (401/403), la app MUST intentar
  renovar la sesión y, si no es posible, ofrecer la acción de editar el servidor
  activo, sin reintentos ciegos del stream.
- **FR-025**: Todos los textos visibles nuevos MUST estar en español.

### Key Entities

- **Sesión de reproducción**: emisora activa, estado visible (conectando,
  reproduciendo, reconectando, fallido), si la emisora ya ha llegado a sonar,
  número de intento actual, motivo del último fallo, instante del último
  progreso y si la reproducción se inició con flujo continuo o con la ruta de
  manifiesto de reserva.
- **Intento de conexión**: número de intento, espera aplicada, instante de
  inicio, código HTTP recibido, resultado y marcador anticaché utilizado.
- **Disponibilidad de emisora**: identificador de la emisora, indicador de si es
  reproducible, motivo cuando no lo es e instante de la comprobación.
- **Evento de diagnóstico**: causa de la interrupción, código de error del
  reproductor, código HTTP, número de intento, resultado y referencia
  anonimizada a la emisora; sin credenciales ni datos personales.
- **Configuración de activación**: indica si la nueva ruta de reproducción está
  incluida y activa en la versión instalada, para permitir el despliegue por
  fases y la vuelta al comportamiento anterior sin migrar datos.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En al menos el 95% de las sesiones de 10 minutos con emisoras HLS
  cuyo origen caduca, el audio se mantiene continuo sin recargar la emisora ni
  intervenir; en las pruebas de referencia (Onda Cero) se alcanzan 10 minutos
  seguidos sin corte atribuible a la caducidad del origen.
- **SC-002**: En al menos 9 de cada 10 cortes de red de 10 segundos, el audio se
  recupera automáticamente en menos de 20 segundos desde que vuelve la red, sin
  interacción del usuario.
- **SC-003**: El 100% de los escenarios de fallo con conectividad disponible
  termina en audio funcionando o en un estado de error visible con "Reintentar"
  dentro de los 120 segundos siguientes al inicio de la interrupción; sin red,
  la app muestra "Sin conexión" y no mantiene un indicador de carga indefinido
  ni un bucle de reintentos.
- **SC-004**: El 100% de las emisoras con formato no compatible inicia la ruta
  de reserva o muestra el aviso en menos de 5 segundos, sin cierres inesperados
  ni bucles de reintento.
- **SC-005**: El 100% de las pruebas de continuidad en segundo plano (pantalla
  apagada 10 minutos y app en segundo plano 10 minutos) y de cambio de red
  (Wi-Fi ↔ datos móviles) recupera el audio o muestra el error accionable, sin
  audio duplicado.
- **SC-006**: En al menos el 95% de las comprobaciones de disponibilidad, la
  respuesta llega en 2 segundos o menos; cuando la emisora no es reproducible,
  no se inicia el reproductor y se muestra el motivo.
- **SC-007**: El 100% de los eventos de conexión y reconexión registrados
  contiene código HTTP o de error, número de intento y motivo, con la emisora
  anonimizada; una revisión de los registros no encuentra credenciales ni datos
  personales.
- **SC-008**: Al menos el 90% de las sesiones de escucha de 30 minutos en las
  emisoras HLS antes problemáticas transcurre sin interrupciones visibles para
  el usuario. Se considera interrupción visible cualquier silencio audible de
  más de 1 segundo, un cambio a RECONNECTING/FAILED o una intervención manual
  del usuario.
- **SC-009**: El 100% de las pruebas existentes de foco de audio, auriculares,
  temporizador de apagado, volumen, Chromecast e historial sigue pasando sin
  regresiones.
- **SC-010**: En el 100% de las pruebas de activación y desactivación de la
  fase, la app vuelve al comportamiento anterior sin pérdida de datos ni
  migración, y la activación por defecto de la fase 3 no requiere cambios del
  usuario.

## Assumptions

- El backend objetivo es la versión 0.29.0 o superior y ofrece el flujo continuo
  para directos, listas resueltas y HLS concatenable (AAC-ADTS/MP3). Las rutas
  anteriores siguen existiendo y no cambian.
- Contrato de referencia del backend: `GET /api/v1/playback/{stationId}/live`
  devuelve el flujo continuo con `audio/aac`, `audio/mpeg` o `audio/aacp` y sin
  longitud declarada; responde 415 (`HLS_UNSUPPORTED_CONTAINER`) cuando el HLS
  no es concatenable, 503 (`STREAM_UNAVAILABLE`, `PLAYLIST_*`) cuando el origen
  no está disponible y 401 cuando el token caduca o no es válido.
  `GET /api/v1/playback/{stationId}/status` devuelve `{ id, playable, reason? }`.
  `GET /api/v1/playback/{stationId}` y su ruta de subrecursos HLS se mantienen
  como camino de reserva. El backend ignora el marcador `?r=<intento>`.
- La autenticación del flujo continuo es la misma que la del resto del proxy
  (token de acceso o cookie); no se modifica el contrato de autenticación y
  ninguna credencial viaja en la URL.
- Las emisoras afectadas son streams en directo: tras una reconexión se vuelve
  al punto en directo y no se reanuda audio acumulado.
- La app ya dispone de reproducción HLS para la ruta de reserva y de un flujo de
  renovación de token; esta feature los reutiliza y no introduce un segundo
  reproductor.
- El resultado 415 no se persiste por emisora: cada reproducción vuelve a probar
  el flujo continuo una vez y, si el formato sigue sin ser compatible, usa la
  ruta de reserva. Así una actualización del backend o de la emisora no queda
  bloqueada por un estado antiguo.
- El supervisor se apoya en el servicio de reproducción en segundo plano
  existente; no se crea un servicio nuevo. El servicio controla su arranque y
  cancelación; la UI solo observa y delega acciones.
- La ficha de emisora comprueba la disponibilidad al abrirse y muestra el mismo
  estado de reproducción que el resto de la app; si `playable=false`, no inicia
  el reproductor y muestra el motivo. La ficha y el play comparten el resultado
  si tiene menos de 5 segundos; si no, se repite la consulta. Con
  `playable=false`, el botón «Reproducir» se deshabilita o se sustituye por el
  aviso.
- La notificación multimedia refleja el estado de transporte
  (buffering/playing/paused) y el botón «Reintentar» en `FAILED(RETRY)`; en
  errores de credenciales no recuperables (`FAILED(EDIT_SERVER)`) mantiene
  stop/mute y la app ofrece «Editar servidor».
- Glosario: «ruta continua» es `CONTINUOUS`/`/live`; «ruta de reserva» es
  `LEGACY`/fallback/ruta de manifiesto existente.
- La comprobación de disponibilidad consulta el estado de la emisora en el
  backend antes de reproducir; si la consulta tarda más de 2 segundos o falla,
  la reproducción se intenta igualmente para no bloquear al usuario.
- El despliegue por fases se controla con una bandera de compilación por
  versión, gestionada por el equipo. No se introduce un servicio de
  configuración remota ni un ajuste visible para el usuario; la fase 3 activa la
  funcionalidad por defecto.
- El diagnóstico se registra localmente de forma estructurada; no se añade un
  canal nuevo de telemetría ni se envían datos a un servicio externo. El
  servidor sigue registrando duración, bytes y motivo de cierre de cada stream.
- El historial y las estadísticas se siguen registrando en el servidor; la app
  no duplica esa lógica y solo mantiene su caché de lectura existente.
- Chromecast queda fuera del alcance: el receptor usa la URL pública de la
  emisora y no puede autenticarse contra el proxy. La reproducción local y la
  reconexión del teléfono se mantienen separadas de la sesión de Cast.
- La transcodificación en el servidor y el soporte de HLS fMP4, MPEG-TS o
  cifrado como flujo continuo quedan fuera de alcance; el backend responde que
  el formato no es compatible y la app aplica la ruta de reserva definida.
- Esta feature extiende la spec 0042 (continuidad de reproducción y buffer) y
  ajusta su política de reintentos a 6 intentos con esperas de 0, 1, 2, 4, 8 y
  15 segundos, un watchdog de 15 segundos, un límite total de 2 minutos y un
  reinicio del contador tras 60 segundos de reproducción estable; el resto de
  0042 (perfiles de buffer, preferencia de reanudación, estados accionables) se
  conserva.
- Se mantienen las decisiones de las specs 0019 (foco de audio y sesión
  multimedia), 0021/0024 (reproducción exclusivamente por el proxy autenticado)
  y 0037 (control de volumen contextual).
- Todos los textos visibles nuevos estarán en español.
- No se prevén dependencias nuevas: la solución se apoya en el reproductor, el
  servicio en segundo plano y la red ya presentes (principio V, YAGNI).
