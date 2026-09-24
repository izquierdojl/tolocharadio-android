# Feature Specification: Control de volumen del dispositivo Chromecast

**Feature Branch**: `0035-jlizquierdo-20260916-cast-volume-control`

**Created**: 2026-09-16

**Status**: Done (2026-09-24; control de volumen del dispositivo entregado por la spec 0037 — la herramienta in-app de esta spec queda superseded por 0037 FR-004; validado en uso real)

**Input**: User description: "Planificar que cuando el dispositivo se conecte al chromecast o dispositivo compatible, se pueda controlar el volumen del mismo. He probado como lo hace Pocketcast y muestra la barra de volumen del dispositivo ChromeCast. La app debería tener la misma funcionalidad."

## Clarifications

### Session 2026-09-16

- Q: Cuando la app está en segundo plano, ¿basta con que las teclas físicas y la barra de volumen del sistema controlen el dispositivo Cast, o la notificación multimedia debe incluir también controles de volumen propios? → A: Solo teclas físicas + barra del sistema con la app en segundo plano; la notificación multimedia no añade controles de volumen.
- Q: Si un ajuste de volumen no llega al dispositivo pero la sesión Cast sigue conectada, ¿qué experiencia debe tener el usuario? → A: Silencioso: el control vuelve solo al nivel real del dispositivo en la siguiente sincronización, sin avisos.
- Q: Si el usuario tenía el audio silenciado en el teléfono y conecta al dispositivo Cast (o al revés), ¿qué debe pasar con el estado de silencio? → A: El estado de silencio se mantiene al cambiar de dispositivo.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Las teclas de volumen del teléfono controlan el volumen del dispositivo Cast (Priority: P1)

Un usuario está escuchando la radio en su Chromecast o altavoz compatible. Pulsa las teclas
físicas de volumen del teléfono y aparece la barra de volumen del sistema identificando el
dispositivo Cast (su nombre/icono), subiendo o bajando el volumen real del altavoz o TV.
El volumen multimedia del propio teléfono no cambia.

**Why this priority**: Es la experiencia de referencia que el usuario ha pedido explícitamente
(igual que Pocket Casts) y la forma más natural de controlar el volumen sin abrir la app. Sin
esto, la feature no cumple el objetivo.

**Independent Test**: Con una sesión Cast activa reproduciendo una emisora, pulsar subir/bajar
volumen y comprobar que la barra del sistema muestra el dispositivo Cast y que el volumen
audible del receptor cambia, permaneciendo intacto el volumen del teléfono.

**Acceptance Scenarios**:

1. **Given** audio reproduciéndose en un dispositivo Cast, **When** el usuario pulsa la tecla de
   subir volumen, **Then** aparece la barra de volumen del sistema identificando el dispositivo
   Cast y su volumen remoto sube un paso, sin modificar el volumen multimedia del teléfono.
2. **Given** el volumen del dispositivo Cast al máximo o al mínimo, **When** el usuario sigue
   pulsando la misma tecla, **Then** la barra muestra el tope alcanzado y no se altera ni el
   volumen del teléfono ni la reproducción.
3. **Given** la app en segundo plano con sesión Cast activa, **When** el usuario pulsa las teclas
   de volumen, **Then** también controlan el volumen del dispositivo Cast.
4. **Given** el usuario se desconecta del dispositivo Cast, **When** pulsa las teclas de volumen,
   **Then** vuelven a controlar el volumen local del teléfono, sin pasos manuales adicionales.

---

### User Story 2 - Control deslizante de volumen coherente con el dispositivo (Priority: P1)

*(SUPERSEDED por la spec 0037 — volumen contextual único: sin control de volumen in-app con
Cast activo; la única representación del volumen es la barra del sistema.)*

Un usuario con sesión Cast abierta abre el reproductor completo y mueve el control deslizante de
volumen. El dispositivo cambia de volumen de forma continua y el valor mostrado es siempre el
real del dispositivo, también al cerrar y reabrir el panel o volver a la app desde segundo plano.

**Why this priority**: Complementa a las teclas físicas para ajustes precisos y evita la
sensación de control roto (hoy el control no refleja el estado real al reabrirse). Comparte el
núcleo con US1: el volumen remoto.

**Independent Test**: Con sesión Cast activa, abrir el reproductor completo y comprobar que el
control deslizante muestra el volumen real actual; moverlo y verificar el cambio audible;
cerrar y reabrir el panel y comprobar que mantiene el valor real.

**Acceptance Scenarios**:

1. **Given** una sesión Cast activa, **When** el usuario abre el reproductor completo, **Then** el
   control de volumen aparece mostrando el nivel real actual del dispositivo (no un valor fijo).
2. **Given** el usuario arrastra el control deslizante, **When** cambia de posición, **Then** el
   volumen del dispositivo Cast se ajusta de forma continua y fluida, sin exigir soltar el dedo.
3. **Given** el usuario cerró y reabrió el reproductor completo (o volvió desde segundo plano),
   **When** observa el control de volumen, **Then** muestra el nivel real del dispositivo.
4. **Given** el usuario ajusta el volumen, **When** termina el ajuste, **Then** la reproducción
   continúa sin interrupciones ni reinicios del stream.

---

### User Story 3 - Los cambios hechos desde el propio dispositivo se reflejan en la app (Priority: P2)

*(SUPERSEDED parcialmente por la spec 0037 — volumen contextual único: el reflejo lo hace la
barra del sistema (eco nativo ≤ 2 s); el "control deslizante de la app" de esta historia ya no
existe.)*

Un usuario baja el volumen con el mando del TV, con la app de Google Home o desde otro teléfono.
Al volver a la app, el control de volumen muestra el nivel actualizado sin tener que tocarlo.

**Why this priority**: Evita estados incoherentes entre lo que muestra la app y lo que suena.
Es importante para la confianza, pero la feature ya aporta valor solo con US1 y US2.

**Independent Test**: Con sesión Cast activa, cambiar el volumen desde un mando externo o desde
el propio dispositivo y verificar que el control deslizante de la app refleja el nuevo nivel en
pocos segundos.

**Acceptance Scenarios**:

1. **Given** una sesión Cast activa, **When** el volumen se cambia desde otro mando o desde el
   propio dispositivo, **Then** el control deslizante de la app se actualiza al nuevo nivel en
   un máximo de 2 segundos.
2. **Given** un cambio externo de volumen mientras el usuario no tiene abierto el reproductor,
   **When** el usuario lo abre, **Then** el control muestra el nivel actualizado.

---

### User Story 4 - Silenciar y restablecer el volumen del dispositivo Cast (Priority: P2)

Un usuario con sesión Cast activa pulsa el botón de silencio de la app: el dispositivo Cast se
silencia. Al volver a pulsarlo, el volumen regresa al nivel que tenía antes de silenciar.

**Why this priority**: El botón de silencio ya existe en la app; si con Cast activo no afecta al
audio, la UI resulta engañosa. Es una extensión natural del control de volumen.

**Independent Test**: Con sesión Cast activa, pulsar silencio y comprobar que el dispositivo deja
de sonar; volver a pulsar y comprobar que recupera exactamente el nivel previo.

**Acceptance Scenarios**:

1. **Given** una sesión Cast activa, **When** el usuario pulsa el botón de silencio, **Then** el
   dispositivo Cast se silencia y la app indica el estado silenciado.
2. **Given** el dispositivo Cast silenciado, **When** el usuario vuelve a pulsar el botón de
   silencio, **Then** el volumen se restablece exactamente al nivel previo.
3. **Given** el dispositivo Cast silenciado, **When** el usuario sube el volumen (tecla o control
   deslizante), **Then** el audio se restablece al nuevo nivel ajustado.
4. **Given** el audio local silenciado, **When** el usuario conecta al dispositivo Cast, **Then**
   el Cast arranca silenciado y el botón de silencio mantiene su estado.

---

### Edge Cases

- ¿Qué ocurre si el dispositivo Cast no admite control de volumen absoluto (solo pasos relativos
  o ninguno)? La app degrada con elegancia: mantiene la reproducción, oculta o deshabilita el
  control con un aviso, y las teclas de volumen vuelven a comportarse como locales.
- ¿Qué pasa si el usuario está conectado a un grupo de altavoces (multi-room)? El control afecta
  al volumen del grupo según lo exponga el receptor; no se ofrece control por altavoz individual.
- ¿Qué ocurre si un cambio externo de volumen llega mientras el usuario arrastra el control
  deslizante? Prevalece el último cambio aplicado, sin saltos bruscos ni parpadeos.
- ¿Cómo se comporta la app si la sesión Cast se pierde en mitad de un ajuste de volumen? El
  ajuste termina sin bloquear la UI y el control de volumen vuelve al comportamiento local.
- ¿Qué ocurre si el dispositivo Cast está silenciado desde sí mismo (mute del TV)? La app lo
  representa como volumen a cero/silenciado y permite restablecerlo desde ella.
- ¿Qué pasa si hay auriculares Bluetooth conectados al teléfono a la vez que la sesión Cast?
  Mientras el Cast esté conectado, las teclas controlan el dispositivo Cast.
- ¿Qué ocurre si la sesión Cast está conectada pero en pausa o detenida? Los controles de volumen
  siguen disponibles y afectan al dispositivo.
- ¿Qué pasa si el usuario pulsa las teclas de volumen muy rápido (pulsaciones repetidas)? El
  volumen remoto sigue el ritmo sin perder pulsaciones ni desincronizarse.
- ¿Qué ocurre si un ajuste de volumen no llega al dispositivo pero la sesión Cast sigue
  conectada? El control vuelve en silencio al nivel real del dispositivo en la siguiente
  sincronización, sin avisos ni bloqueo de la interacción.
- ¿Qué pasa con el silencio al cambiar de dispositivo (local ↔ Cast)? El estado de silencio se
  mantiene: si estaba silenciado, la nueva salida arranca silenciada y el botón sigue marcado.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST dirigir las teclas de volumen del teléfono al dispositivo Cast
  mientras exista una sesión activa, mostrando la barra de volumen del sistema con la identidad
  del dispositivo Cast (nombre o icono) y no la del teléfono.
- **FR-002**: El volumen multimedia del propio teléfono MUST NOT modificarse como consecuencia del
  control remoto de volumen.
- **FR-003**: El sistema MUST ofrecer un control deslizante de volumen en el reproductor completo
  cuando hay sesión Cast, mostrando siempre el nivel real actual del dispositivo.
  *(SUPERSEDED por 0037 FR-004: sin control de volumen in-app con Cast activo.)*
- **FR-004**: Los cambios del control deslizante MUST aplicarse al dispositivo Cast de forma
  continua durante el arrastre, sin exigir soltar el dedo ni pulsar confirmar.
  *(SUPERSEDED por 0037 FR-004.)*
- **FR-005**: El nivel de volumen mostrado en la app MUST reflejar los cambios originados en el
  propio dispositivo Cast o en otros mandos en un máximo de 2 segundos.
  *(SUPERSEDED parcialmente por 0037 FR-004/FR-008: la representación ya no es "en la app" sino
  la barra del sistema, con eco ≤ 2 s nativo de media3 1.11.0.)*
- **FR-006**: El nivel mostrado MUST mantenerse correcto al cerrar y reabrir el reproductor, y al
  volver la app desde segundo plano.
  *(SUPERSEDED parcialmente por 0037 FR-004: "el nivel mostrado" es ahora la barra del sistema.)*
- **FR-007**: El botón de silencio de la app MUST silenciar el dispositivo Cast cuando la sesión
  está activa, recordando el nivel previo, y MUST restablecerlo al des-silenciar.
- **FR-008**: Ajustar el volumen (tecla o control deslizante) mientras el dispositivo está
  silenciado MUST restablecer el audio al nuevo nivel ajustado.
  *(SUPERSEDED parcialmente por 0037 FR-004/FR-007: sin control deslizante; aplica a las teclas
  y la barra del sistema.)*
- **FR-009**: Si el dispositivo Cast no admite control de volumen, el sistema MUST degradar con
  elegancia (avisar y ocultar/deshabilitar el control) sin bloquear la reproducción ni
  desconectar la sesión.
  *(SUPERSEDED por 0037 FR-011: sin gestión especial ante receptores sin volumen — modelo
  Pocket Casts.)*
- **FR-010**: Al finalizar la sesión Cast (desconexión manual o pérdida de conexión), los
  controles de volumen MUST volver al comportamiento local, sin pasos manuales del usuario.
- **FR-011**: El control de volumen del Cast MUST funcionar con la app en segundo plano
  (teclas físicas y barra de volumen del sistema) mientras la sesión esté activa; la
  notificación multimedia NO añade controles de volumen propios.
- **FR-012**: Ajustar el volumen MUST NOT alterar el estado de reproducción: no pausar, no
  reiniciar ni cortar el stream.
- **FR-013**: Con Cast conectado pero sin reproducción en curso, los controles de volumen MUST
  seguir disponibles y afectar al dispositivo.
- **FR-014**: Si un ajuste de volumen no se confirma en el dispositivo pero la sesión sigue
  activa, el sistema MUST corregir el nivel mostrado al real de forma silenciosa, sin mostrar
  avisos ni bloquear la interacción.
- **FR-015**: El estado de silencio MUST mantenerse al cambiar de salida: al conectar o
  desconectar del dispositivo Cast, la nueva salida activa MUST respetar el silencio vigente y
  el botón de silencio MUST reflejarlo.

### Key Entities

- **Nivel de volumen remoto**: valor normalizado (0–100%) del dispositivo Cast conectado, con
  indicación de estado silenciado y del nivel previo al silencio.
- **Sesión Cast activa**: dispositivo destino al que se aplican los ajustes de volumen; determina
  cuándo las teclas y el control deslizante operan en remoto.
- **Estado de volumen expuesto a la UI**: representación observable del nivel remoto que alimenta
  tanto la barra del sistema como el control deslizante del reproductor.
  *(SUPERSEDED parcialmente por 0037 FR-004: la única representación es la barra del sistema; la
  app no mantiene estado de volumen expuesto (solo silencio).)*

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con sesión Cast activa, el 100% de las pulsaciones de las teclas de volumen ajustan
  el volumen del dispositivo Cast y ninguna modifica el volumen multimedia del teléfono.
- **SC-002**: La barra de volumen del sistema identifica el dispositivo Cast en el 100% de los
  ajustes realizados con sesión activa.
- **SC-003**: Mover el control deslizante del 0 al 100% produce el cambio audible correspondiente
  en el dispositivo en menos de 1 segundo.
  *(SUPERSEDED por 0037 FR-004: no existe control deslizante in-app.)*
- **SC-004**: Un cambio de volumen hecho desde otro mando se refleja en la app en menos de 2
  segundos en al menos el 95% de los intentos.
  *(SUPERSEDED parcialmente por 0037 FR-008: se refleja en la barra del sistema, no "en la app".)*
- **SC-005**: Tras cerrar y reabrir el reproductor, el nivel mostrado coincide con el nivel real
  del dispositivo en el 100% de los casos.
  *(SUPERSEDED parcialmente por 0037 FR-004: el nivel lo muestra la barra del sistema.)*
- **SC-006**: Silenciar y des-silenciar restablece exactamente el nivel previo en el 100% de los
  casos.
- **SC-007**: Ningún ajuste de volumen interrumpe la reproducción en el 100% de los casos.
- **SC-008**: La funcionalidad de control de volumen funciona en el 100% de los dispositivos Cast
  probados (Chromecast, Google TV y altavoces inteligentes compatibles).
- **SC-009**: Al desconectarse del dispositivo Cast, las teclas de volumen vuelven a controlar el
  teléfono en el primer intento, sin acciones adicionales del usuario.

## Assumptions

- La app ya cuenta con integración Cast operativa; esta feature se centra en el control de volumen
  del dispositivo conectado, no en descubrimiento, conexión ni reproducción remota.
- El reproductor completo ya muestra un control de volumen cuando hay sesión Cast, pero hoy no
  garantiza el ajuste real del dispositivo ni su sincronización; esta feature asegura ambos.
- Mientras hay sesión Cast, las teclas de volumen del teléfono se destinan al dispositivo remoto
  (comportamiento de referencia de Pocket Casts); el volumen propio del teléfono se ajusta
  cuando no hay Cast conectado.
- La barra de volumen mostrada es la nativa del sistema operativo; no se implementa una barra
  propia superpuesta.
- El botón de silencio existente se conserva y pasa a aplicarse al dispositivo activo (Cast o
  local) según corresponda.
- Se asume que los dispositivos Cast aceptan ajuste de volumen absoluto (0–100%); si solo
  aceptan pasos relativos, la app aproxima el nivel mostrado a partir de los pasos enviados.
- El ajuste de volumen es una interacción directa entre el teléfono y el dispositivo Cast en la
  red local; no cambia el contrato del backend ni requiere endpoints nuevos.
- El control de grupos multi-room se limita al volumen que el propio receptor exponga para el
  grupo; no se ofrece control individual por altavoz en esta versión.
- **Terminología**: "dispositivo Cast", "receptor" y "Chromecast" designan el mismo concepto
  (un receptor compatible con Google Cast); los documentos técnicos usan "receptor" y la UI
  muestra el nombre del dispositivo.

## On-Device Testing Findings (2026-09-16)

Se probó la implementación en un **Redmi M2101K7AG** (Android 17/API 37) con un Chromecast
**"Dormitorio principal"**. Hallazgos clave (ver `research.md` D11 para detalle completo):

1. **`CastSession.setVolume` y `RemoteMediaClient.setStreamVolume` son rechazados
   silenciosamente** por el receptor. El volumen audible no cambia a pesar de que el eco
   (`RemoteMediaClient.Callback.onStatusUpdated`) funciona correctamente y el provider
   muestra los valores correctos (`current=28`).

2. **El `VolumeProviderCompat` de media3 1.4.1 no entrega callbacks en Android 17**.
   SystemUI muestra la barra remota y llama `setVolumeTo`/`adjustVolume` sobre nuestra
   sesión, pero el `PlayerWrapper.setDeviceVolume` nunca se invoca en la app.

3. **Las teclas físicas no se rutean**: `MediaSessionService` muestra `session=null` al
   ajustar, aunque la sesión ES el media button session y tiene provider remoto. Esto puede
   estar relacionado con `state=NONE(0)` en el `PlaybackState`.

**Impacto**: Las funcionalidades FR-001 (teclas), FR-005 (barra del sistema) y FR-004
(slider de la app) no funcionan en este dispositivo/receptor. El eco de cambios externos
(FR-003) SÍ funciona. La degradación de receptor sin soporte (FR-009) SÍ funciona.

**Decisión (resuelta)**: Requería investigación adicional (upgrade de media3, uso de
`AudioManager` para interceptar teclas, o prueba con otros receptores para determinar si
el problema era específico de este dispositivo/receptor). **Cerrada.**

**Resolución (2026-09-18, spec 0037)**: RESUELTO con el upgrade a **media3 1.11.0**
(commit `7496a53`): el `CastPlayer` nativo soporta volumen de dispositivo — teclas y barra
del sistema funcionan sin código propio, validado en `specs/0037-jlizquierdo-20260918-contextual-volume/quickstart.md`
§3.2. La parte de FR-001/FR-005 queda confirmada; FR-004 (slider) ya no aplica por el
supersede de 0037 FR-004. Los hallazgos anteriores corresponden a media3 1.4.1 y quedan
como registro histórico.

**Validación (2026-09-24)**: confirmado en uso real durante varios días — el control de
volumen contextual (teclas + barra del sistema) funciona de forma aceptable. Duda de
seguimiento **cerrada**.
