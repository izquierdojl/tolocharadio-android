# Feature Specification: Controles de auriculares para pausar y saltar

**Feature Branch**: `0060-izquierdojl-20261124-headset-media-controls`

**Created**: 2026-11-24

**Status**: Done (2026-11-24; T011 y T036 asumidas sin verificar en dispositivo real; validación pendiente)

**Input**: User description: "Vamos a planificar la posibilidad de que la aplicación utilce los controles de los auriculares cuando estén conectados, ya sean de cable o bluetooth. Por ejemplo, en una pulsación corta sube o baja volumen ( esto ya lo hace ) pero en una pulsación larga debería ir hacia adelante o hacia atrás los segundos establecidos en la aplicación, la aplicación central pausar, etc... Es algo como lo que ya dispone PocketCats."

## Clarifications

### Session 2026-11-24

- Q: ¿Dónde debe funcionar la pulsación larga de volumen para saltar? → A: Solo con la app visible (primer plano); en segundo plano los botones de volumen mantienen su comportamiento nativo y no se añaden permisos.
- Q: ¿Los segundos de salto deben poder configurarse? → A: Sí, configurables en Configuración con opciones de 5 a 60 s y valores por defecto de 15 s atrás y 30 s adelante.
- Q: ¿Qué deben hacer los botones anterior/siguiente del auricular? → A: Saltar los segundos configurados, sin cambiar de episodio, al estilo Pocket Casts.
- Q: ¿Cómo debe cubrir la app los auriculares Bluetooth cuyos botones de volumen no llegan a la aplicación? → A: Documentar que la pulsación larga de volumen solo está garantizada en auriculares con cable o modelos que entregan las teclas a la app; en Bluetooth se salta con anterior/siguiente cuando el auricular los tiene.
- Q: ¿Deben los gestos de salto funcionar cuando el podcast está pausado? → A: Sí, cambian la posición y el episodio sigue pausado; no se reanuda automáticamente.
- Q: ¿Los segundos configurados deben aplicarse a todos los controles de salto o solo a los gestos del auricular? → A: A todos: gestos del auricular, reproductor, notificación y pantalla de bloqueo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Pausar y reanudar desde el auricular (Priority: P1)

Una persona escucha la radio o un podcast con auriculares con cable o Bluetooth.
Con el teléfono en el bolsillo, pulsa una vez el botón central del auricular: el
audio se pausa; lo vuelve a pulsar y la reproducción continúa. No necesita
desbloquear el teléfono ni abrir la aplicación.

**Why this priority**: Es el control más básico y el que aporta valor por sí
solo. Sin una pulsación central fiable, los gestos de salto no tienen una base
sobre la que construirse.

**Independent Test**: Conectar un auricular con cable y uno Bluetooth,
reproducir una emisora y un episodio, y pulsar el botón central con la app
abierta, en segundo plano y con la pantalla bloqueada. Verificar que la pausa y
la reanudación se reflejan en la app, la notificación y la pantalla de bloqueo.

**Acceptance Scenarios**:

1. **Given** audio en reproducción (radio o podcast) y un auricular conectado,
   **When** se pulsa una vez el botón central, **Then** la reproducción se pausa
   en menos de 500 ms y el estado se actualiza en la app, la notificación y la
   pantalla de bloqueo.
2. **Given** un podcast pausado, **When** se pulsa una vez el botón central,
   **Then** la reproducción se reanuda exactamente en la misma posición.
3. **Given** la radio en directo pausada, **When** se pulsa una vez el botón
   central, **Then** la reproducción se reanuda sin cambiar de emisora y el
   estado se refleja en la app y la notificación.
4. **Given** la pantalla bloqueada o la app en segundo plano, **When** se pulsa
   una vez el botón central, **Then** la acción funciona sin desbloquear el
   teléfono ni abrir la app.
5. **Given** que no hay contenido activo, **When** se pulsa el botón central,
   **Then** no se inicia ninguna reproducción automáticamente.

---

### User Story 2 - Saltar adelante y atrás desde el auricular (Priority: P2)

Una persona escucha un podcast y quiere evitar un anuncio o repasar una frase.
Sin sacar el teléfono, mantiene pulsado el volumen arriba para avanzar los
segundos configurados, o el volumen abajo para retroceder (con auriculares con
cable o modelos que entregan las teclas de volumen a la app). Si su auricular
tiene botones de anterior y siguiente, también puede usarlos para saltar en
lugar de cambiar de episodio; en los auriculares Bluetooth cuyo volumen controla
el sistema, esta es la vía principal para saltar. También puede saltar con el
episodio pausado para preparar el punto de escucha sin reanudarlo. La radio en
directo no se ve afectada porque no se puede retroceder.

**Why this priority**: Es la petición principal y el comportamiento que más se
parece a Pocket Casts, pero depende de que la pulsación central (US1) ya
funcione.

**Independent Test**: Con un auricular con cable, reproducir un episodio con la
app visible y mantener pulsado el volumen arriba y abajo, comprobando que la
posición cambia exactamente los segundos configurados y que una pulsación corta
sigue cambiando el volumen del sistema. Con un auricular Bluetooth con botones
laterales, probar anterior y siguiente. Verificar que la radio en directo no
sufre saltos ni reinicios.

**Acceptance Scenarios**:

1. **Given** un episodio en reproducción y la app visible, **When** se mantiene
   pulsado el volumen arriba al menos 0,5 s, **Then** la posición avanza los
   segundos configurados (30 s por defecto), sin pausar ni reiniciar el audio.
2. **Given** un episodio en reproducción y la app visible, **When** se mantiene
   pulsado el volumen abajo al menos 0,5 s, **Then** la posición retrocede los
   segundos configurados (15 s por defecto), sin bajar de 0.
3. **Given** un episodio en reproducción, **When** se pulsa brevemente el
   volumen arriba o abajo, **Then** el volumen del sistema cambia como hasta
   ahora y no se produce ningún salto.
4. **Given** un auricular con botones de anterior y siguiente, **When** se pulsa
   anterior o siguiente, **Then** la posición retrocede o avanza los segundos
   configurados en lugar de cambiar de episodio.
5. **Given** la radio en directo reproduciéndose, **When** se mantiene pulsado
   el volumen o se pulsa anterior/siguiente, **Then** la reproducción continúa
   sin saltos, sin reinicios y sin cambiar de emisora.
6. **Given** un salto hacia adelante cerca del final del episodio, **When** se
   ejecuta el gesto, **Then** la posición se limita al final del episodio y este
   termina de forma normal, sin error.
7. **Given** un episodio en carga, en error o sin episodio activo, **When**
   se ejecuta un gesto de salto, **Then** no se produce ninguna acción ni se
   interrumpe el audio.
8. **Given** la app en segundo plano o la pantalla apagada, **When** se intenta
   un salto con los botones de volumen, **Then** el gesto no está disponible y
   los botones siguen controlando el volumen del sistema.
9. **Given** un episodio pausado, **When** se ejecuta un gesto de salto,
   **Then** la posición cambia los segundos configurados y el episodio sigue
   pausado, sin reanudarse automáticamente.

---

### User Story 3 - Elegir los segundos de salto y activar o desactivar los gestos (Priority: P3)

Una persona quiere decidir cuántos segundos retrocede y avanza con cada gesto,
y poder desactivar los gestos de salto si prefiere que los botones de su
auricular solo cambien el volumen. Los cambios se aplican al siguiente salto,
venga del auricular o de los controles en pantalla, y se conservan al cerrar y
volver a abrir la app.

**Why this priority**: Mejora la experiencia y da control, pero los saltos ya
aportan valor con los valores por defecto.

**Independent Test**: Abrir Configuración, cambiar los segundos de retroceso y
avance, ejecutar un salto desde el auricular y desde los controles en pantalla,
y comprobar que usan los nuevos valores sin reiniciar la reproducción; cerrar
la app, reabrirla y verificar que se mantienen.

**Acceptance Scenarios**:

1. **Given** una instalación nueva, **When** se abre Configuración, **Then**
   existe una sección de controles de auriculares con 15 s de retroceso y 30 s
   de avance por defecto.
2. **Given** la sección de controles de auriculares, **When** se eligen otros
   segundos, **Then** el siguiente salto de cualquier control usa los nuevos
   valores sin reiniciar la reproducción ni reconectar el auricular.
3. **Given** unos segundos personalizados, **When** se cierra y se vuelve a abrir
   la app o se reconecta el auricular, **Then** los valores se conservan.
4. **Given** los gestos de salto desactivados, **When** se mantiene pulsado el
   volumen o se pulsa anterior/siguiente, **Then** no se produce ningún salto,
   el volumen recupera su comportamiento nativo, anterior/siguiente no realizan
   ninguna acción y la pulsación central sigue pausando y reanudando.
5. **Given** unos segundos personalizados, **When** se muestran los controles de
   salto en el reproductor y en la notificación, **Then** las etiquetas reflejan
   los segundos configurados.
6. **Given** los gestos de salto desactivados, **When** se usan los botones de
   salto del reproductor o de la notificación, **Then** el episodio salta los
   segundos configurados; el interruptor solo afecta a los gestos del
   auricular.
7. **Given** unos segundos personalizados, **When** se usan los botones de salto
   del reproductor, la notificación o la pantalla de bloqueo, **Then** la
   posición cambia esos segundos.

---

### Edge Cases

- Auricular con un único botón central y sin botones de volumen, anterior o
  siguiente: la pausa y la reanudación funcionan; los saltos no están
  disponibles y no se muestra ningún error.
- Auricular o adaptador que no informa de la pulsación larga: el gesto no se
  detecta, el volumen sigue funcionando y no se produce ningún salto accidental.
- Auricular Bluetooth cuyo volumen se gestiona de forma absoluta por el sistema:
  la app no recibe la pulsación larga de volumen; el volumen sigue funcionando y
  el salto se hace con anterior/siguiente o con los controles en pantalla.
- Pulsación larga que genera eventos repetidos: se ejecuta un único salto por
  gesto, no una cadena de saltos.
- Pulsaciones rápidas y encadenadas: cada gesto completo produce como máximo un
  salto y no se acumulan acciones fuera de control.
- Auricular Bluetooth que se desconecta o reconecta a mitad de un gesto: no se
  cierra la app, no se repite la acción y los controles vuelven a funcionar al
  reconectar.
- Auricular Bluetooth multipunto conectado a dos dispositivos: los controles
  actúan solo cuando la reproducción activa pertenece a esta app.
- Llamada entrante o pérdida de foco de audio: los controles respetan la
  llamada y no reanudan la reproducción por encima de ella.
- Reproducción en Cast: si el receptor admite el salto, se aplica al audio
  remoto; si no lo admite, no se produce ningún efecto local ni suena audio
  duplicado.
- Volumen en el máximo o en el mínimo: el salto se ejecuta igualmente y no se
  altera el volumen.
- Episodio pausado: el salto cambia la posición y no reanuda la reproducción.
- Cambio de segundos durante la reproducción: el siguiente salto de cualquier
  control usa el valor nuevo, sin reiniciar ni reconectar.
- Gestos del auricular desactivados: los botones de salto en pantalla y en la
  notificación siguen funcionando con los segundos configurados.
- Episodio más corto que los segundos configurados: la posición se mantiene
  dentro del episodio.
- Saltar hacia atrás al principio o hacia adelante al final: la posición se
  limita al inicio o al final sin errores.
- App recién abierta sin reproducción activa: los gestos no arrancan audio.
- Pantalla apagada con botones de volumen: el gesto largo no se captura; la
  pulsación central y los botones anterior/siguiente sí siguen funcionando.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST reconocer, en cuanto se conectan y sin
  configuración previa, los controles estándar de reproducción de auriculares
  con cable y Bluetooth (central, anterior y siguiente) y los botones de
  volumen cuando el dispositivo los entregue a la app.
- **FR-002**: Una pulsación corta del botón central MUST pausar o reanudar el
  contenido activo, tanto radio como podcast, con la app en primer plano, en
  segundo plano o con la pantalla bloqueada.
- **FR-003**: El cambio de estado producido por un control del auricular MUST
  reflejarse en la app, la notificación y la pantalla de bloqueo en menos de
  500 ms.
- **FR-004**: Una pulsación larga del volumen arriba o abajo (al menos 0,5 s)
  MUST avanzar o retroceder los segundos configurados cuando hay un episodio
  activo, en reproducción o en pausa, y la app está visible en primer plano.
  Esta captura solo es posible en auriculares cuyos botones de volumen llegan a
  la app como controles de tecla (habitualmente, auriculares con cable); en los
  auriculares Bluetooth que usan el volumen absoluto del sistema, el volumen
  MUST seguir controlándose por el sistema y los saltos MUST hacerse con
  anterior/siguiente o con los controles en pantalla. Con la app en segundo
  plano o la pantalla apagada, la pulsación larga MUST NOT capturarse y los
  botones de volumen MUST mantener su comportamiento nativo.
- **FR-005**: Una pulsación corta del volumen MUST mantener el comportamiento
  actual del volumen del sistema, sin producir saltos.
- **FR-006**: Los botones anterior y siguiente de un auricular MUST retroceder
  y avanzar los segundos configurados cuando hay un episodio activo, en
  reproducción o en pausa, sin cambiar de episodio y sin reanudar la
  reproducción si estaba pausada. Este comportamiento no es configurable en
  esta versión.
- **FR-007**: Los valores por defecto de salto MUST ser 15 s hacia atrás y 30 s
  hacia adelante.
- **FR-008**: El usuario MUST poder elegir los segundos de retroceso y avance en
  Configuración entre las opciones 5, 10, 15, 20, 30, 45 y 60 s. Los nuevos
  valores MUST aplicarse al siguiente salto de cualquier control (auricular,
  reproductor, notificación o pantalla de bloqueo) y MUST conservarse al
  reiniciar la app o reconectar el auricular.
- **FR-009**: Los saltos MUST aplicarse solo a episodios. En radio en directo,
  un gesto de salto MUST ignorarse sin pausar, reiniciar, cortar ni cambiar la
  emisora.
- **FR-010**: La posición tras un salto MUST quedar limitada al rango válido del
  episodio (de 0 al final); un salto hacia adelante cerca del final MUST
  terminar el episodio de forma normal.
- **FR-011**: Cada gesto físico MUST producir como máximo una acción; los
  eventos repetidos de una misma pulsación larga MUST ignorarse y las
  pulsaciones rápidas MUST procesarse sin acumular saltos descontrolados.
- **FR-012**: El usuario MUST poder desactivar los gestos de salto (pulsación
  larga de volumen y uso de anterior/siguiente para saltar) sin desactivar la
  pulsación central, los botones de salto en pantalla ni los controles de la
  notificación. Con los gestos desactivados, el volumen recupera su
  comportamiento nativo y anterior/siguiente no realizan ninguna acción; los
  botones explícitos de salto siguen funcionando.
- **FR-013**: Cuando no hay contenido activo, los controles del auricular MUST
  NOT iniciar ninguna reproducción automáticamente.
- **FR-014**: Cuando la reproducción activa sea Cast, los controles MUST actuar
  sobre el audio remoto si el receptor lo admite; si no lo admite, MUST NOT
  producir ningún efecto local ni audio duplicado.
- **FR-015**: La funcionalidad MUST NOT requerir permisos nuevos ni pasos de
  configuración más allá de conectar el auricular; MUST NOT depender de un
  servicio de accesibilidad.
- **FR-016**: Una desconexión o reconexión del auricular MUST NOT provocar
  cierres inesperados, acciones duplicadas ni dejar los controles bloqueados.
- **FR-017**: Los controles de salto MUST usar los segundos configurados. Los
  textos y etiquetas visibles o accesibles en el reproductor y la notificación
  MUST mostrar esos segundos.
- **FR-018**: Los eventos de control MUST registrarse de forma estructurada y
  sin datos personales, incluyendo el tipo de gesto y el resultado.
- **FR-019**: Configuración MUST incluir una sección de controles de auriculares
  en español, con los ajustes de segundos, la activación de gestos y una
  indicación clara de que la pulsación larga de volumen solo funciona con la
  app visible y en auriculares que entregan las teclas de volumen a la app
  (habitualmente, con cable), y de que el interruptor solo afecta a los gestos
  del auricular.
- **FR-020**: Los controles MUST respetar el foco de audio y las llamadas: no
  MUST reanudar ni saltar por encima de una llamada u otra app con foco.

### Key Entities *(include if feature involves data)*

- **Contenido activo**: audio que se está reproduciendo o está pausado en la
  app; distingue radio en directo (no permite saltos) de episodio (permite
  saltos y conserva posición).
- **Preferencia de controles de auriculares**: segundos de retroceso y avance,
  y si los gestos de salto están activados. Es global para la app, no por
  emisora ni por podcast.
- **Gesto de auricular**: combinación de botón (central, volumen, anterior,
  siguiente), tipo de pulsación (corta o larga) y acción resultante (pausar,
  reanudar, saltar, ignorar).
- **Salida de reproducción activa**: local o Cast; determina dónde se aplica el
  control y si el salto es posible.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En pruebas manuales con al menos un auricular con cable y dos
  Bluetooth, el 100 % de las pulsaciones centrales pausan o reanudan
  correctamente con la app en primer plano, en segundo plano y con la pantalla
  bloqueada.
- **SC-002**: Al menos el 95 % de los gestos de salto se reconocen en el primer
  intento en los dispositivos soportados, y la posición cambia los segundos
  configurados con una tolerancia de ±1 s por el avance natural del audio.
- **SC-003**: La respuesta visible a cualquier control del auricular (cambio de
  estado o de posición en la app y la notificación) ocurre en 500 ms o menos.
- **SC-004**: En una matriz de 50 gestos encadenados no se produce ningún
  cambio de emisora o de episodio no solicitado.
- **SC-005**: El 100 % de los cambios de segundos o de activación de gestos se
  conservan tras cerrar la app, reabrirla y reconectar el auricular.
- **SC-006**: La funcionalidad funciona sin permisos nuevos y sin pasos de
  configuración adicionales tras conectar el auricular.
- **SC-007**: Una sesión de 30 minutos con al menos 100 gestos no produce
  cierres inesperados, bloqueos de la app ni cortes de audio.
- **SC-008**: La radio en directo no sufre ninguna interrupción, reinicio ni
  cambio de emisora por gestos de salto en el 100 % de las pruebas.
- **SC-009**: En una prueba de usabilidad con 5 personas, todas encuentran y
  cambian los segundos de salto en menos de 60 s.
- **SC-010**: Tras cambiar los segundos, el 100 % de las etiquetas de salto
  visibles o accesibles del reproductor y la notificación muestran el nuevo
  valor.
- **SC-011**: La matriz de pruebas incluye al menos un auricular con cable para
  validar la pulsación larga de volumen y dos Bluetooth para validar los
  botones anterior/siguiente, sin exigir el gesto de volumen a los Bluetooth
  que no entregan esas teclas a la app.
- **SC-012**: El 100 % de los saltos ejecutados con el episodio pausado cambian
  la posición los segundos configurados sin reanudar la reproducción.
- **SC-013**: El 100 % de los controles de salto (auricular, reproductor,
  notificación y pantalla de bloqueo) aplican los segundos configurados tras un
  cambio.
- **SC-014**: Con los gestos del auricular desactivados, el 100 % de los
  botones de salto en pantalla y en la notificación siguen funcionando con los
  segundos configurados.

## Assumptions

- Los auriculares y adaptadores emiten las teclas multimedia estándar de
  Android; los protocolos propietarios que no las emiten quedan fuera de
  alcance.
- La pulsación larga de volumen solo se captura con la app visible en primer
  plano; con la pantalla apagada o la app en segundo plano, los botones de
  volumen siguen controlando el volumen del sistema. No se usa ningún servicio
  de accesibilidad.
- El gesto de volumen no requiere detectar si hay un auricular conectado: se
  aplica a cualquier tecla de volumen que llegue a la app con la pantalla
  encendida y la app visible.
- En la mayoría de auriculares Bluetooth el volumen se gestiona de forma
  absoluta en el sistema y no llega a la app como eventos de tecla; la
  pulsación larga de volumen solo está garantizada en auriculares con cable o
  en modelos que entregan esas teclas a la app. En Bluetooth, el salto se hace
  con anterior/siguiente o con los controles en pantalla.
- La radio en directo no se puede retroceder ni avanzar; los gestos de salto se
  ignoran sin afectar a la reproducción.
- Al reanudar una radio en directo no se garantiza volver al borde live; el
  player conserva el comportamiento actual y no cambia de emisora.
- Los segundos de salto son configurables y se aplican a todos los podcasts;
  los valores por defecto son 15 s atrás y 30 s adelante, con opciones de 5 a
  60 s.
- Los gestos de salto funcionan también con el episodio pausado y no reanudan
  la reproducción.
- Los segundos configurados se aplican a todos los controles de salto, no solo
  a los gestos del auricular.
- Se considera app visible cualquier pantalla de la app en primer plano, no solo
  el reproductor.
- Los gestos de salto están activados por defecto; el usuario puede
  desactivarlos desde Configuración. El interruptor solo afecta a los gestos del
  auricular; los botones de salto en pantalla y en la notificación siguen
  activos.
- No se añade un aviso adicional tras cada salto: el cambio se refleja en la
  posición y en los controles de reproducción.
- Los botones anterior y siguiente saltan siempre los segundos configurados, al
  estilo Pocket Casts; no cambian de episodio ni se remapean en esta versión.
- Las preferencias son globales para la app, no por servidor, emisora o podcast.
- La detección de pulsaciones largas depende del firmware del auricular; si el
  dispositivo no las envía, la app no puede inventarlas.
- El control de reproducción remota en Cast depende de las capacidades del
  receptor; si no admite salto, se degrada sin efectos locales.
- No se incluyen en esta versión la doble o triple pulsación del botón central,
  la pulsación larga del botón central, el remapeo libre de botones ni el
  desplazamiento de la radio en directo.

## Fuera de alcance (non-goals)

- Capturar botones de volumen en segundo plano mediante un servicio de
  accesibilidad.
- Soportar protocolos propietarios de auriculares que no exponen controles
  multimedia estándar.
- Implementar DVR, retroceso o avance en radio en directo.
- Añadir atajos de doble o triple pulsación en el botón central.
- Permitir configuraciones distintas por emisora, podcast o servidor.
- Controlar la reproducción desde relojes, coche u otros dispositivos más allá
  de los controles multimedia estándar que ya llegan a la sesión de la app.
