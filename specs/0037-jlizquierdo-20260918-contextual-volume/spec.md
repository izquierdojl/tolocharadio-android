# Feature Specification: Volumen contextual único estilo Pocket Casts

**Feature Branch**: `0037-jlizquierdo-20260918-contextual-volume`

**Created**: 2026-09-18

**Status**: Ready

**Input**: User description: "Quiero que el control de volumen funcione como en Pocket Cast: si suena la aplicación es el volumen del teléfono y si está conectado por cast, tiene que ser sólo el volumen del cast. Ahora muestra los dos y cuando se conecta al cast sube de golpe al 100% y aunque funciona es muy incómodo y se siguen viendo las dos franjas de volumen." Referencia de comportamiento: app Pocket Casts (Android).

## Clarifications

### Session 2026-09-18

- Q: Si el receptor Cast no admite control de volumen, ¿degradar con aviso como exigía la
  spec 0035, o no hacer nada especial como Pocket Casts? → A: No hacer nada especial
  (modelo Pocket Casts): la reproducción continúa, la sesión no se desconecta y no se
  muestran avisos (FR-011).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Al conectar al Cast, el volumen del receptor no cambia (Priority: P1)

Un usuario tiene su Chromecast o altavoz al 30% (ajustado con el mando del TV o Google Home).
Conecta la app al dispositivo para escuchar la radio: la reproducción arranca y el volumen
audible del receptor es exactamente el que tenía (30%). Ni al conectar, ni al reconectar tras
una pérdida de red, ni al reabrir la app el volumen del receptor salta al 100% ni a ningún
otro valor impuesto por la app.

**Why this priority**: Es el fallo más molesto reportado (bug 0036, abierto en la versión
actual): el volumen se dispara al máximo en cada conexión. Sin esto, el resto de la
experiencia sigue siendo incómoda.

**Independent Test**: Ajustar el receptor al 30%, conectar la app, desconectar y volver a
conectar: el nivel audible del receptor permanece en 30% en todo momento.

**Acceptance Scenarios**:

1. **Given** un receptor con volumen al 30% y la app reproduciendo en local, **When** el
   usuario conecta al dispositivo Cast, **Then** la reproducción arranca en el receptor con
   su volumen al 30%, sin ningún salto.
2. **Given** una sesión Cast activa, **When** la sesión se pierde y se recupera
   automáticamente, **Then** el volumen del receptor tampoco cambia por la reconexión.
3. **Given** un receptor al nivel que sea (incluido silenciado desde el mando del TV),
   **When** la app conecta, **Then** el nivel y el silencio del receptor se respetan tal
   cual estaban.

---

### User Story 2 - Una sola franja de volumen, contextual según la salida (Priority: P1)

Con la app sonando en el teléfono, las teclas de volumen y la barra del sistema controlan el
volumen del teléfono (como siempre). Al conectar a un dispositivo Cast, las teclas y la barra
del sistema pasan a controlar exclusivamente el volumen del dispositivo, identificándolo por
su nombre, y la app deja de mostrar su propio control de volumen: existe una única
representación del volumen, la del sistema, que cambia de destino (teléfono ↔ dispositivo)
según la salida activa, sin pasos manuales.

**Why this priority**: Es la petición central del usuario y el modelo de referencia Pocket
Casts: hoy se ven dos franjas (la del sistema y el deslizador de la app), lo que resulta
redundante e incómodo.

**Independent Test**: Con sesión Cast activa, pulsar las teclas de volumen: la barra del
sistema identifica el dispositivo y solo cambia el volumen del receptor; abrir el reproductor
completo y comprobar que ya no se muestra ningún deslizador de volumen.

**Acceptance Scenarios**:

1. **Given** reproducción local (sin Cast), **When** el usuario pulsa las teclas de volumen,
   **Then** cambian solo el volumen del teléfono y la barra del sistema es la del teléfono.
2. **Given** una sesión Cast activa, **When** el usuario pulsa las teclas de volumen,
   **Then** cambian solo el volumen del dispositivo Cast y la barra del sistema lo identifica
   con el nombre del dispositivo.
3. **Given** una sesión Cast activa, **When** el usuario abre el reproductor completo de la
   app, **Then** no se muestra ningún control de volumen propio: el único control visible es
   el del sistema.
4. **Given** una sesión Cast activa, **When** el usuario se desconecta, **Then** las teclas
   vuelven a controlar el teléfono en el primer intento, sin pasos manuales.

---

### User Story 3 - Silencio coherente y respetuoso con el receptor (Priority: P2)

El botón de silencio de la app sigue silenciando la salida activa (teléfono o dispositivo
Cast), recordando el nivel previo; el silencio iniciado por el usuario sobrevive al cambio
de salida. Además, conectar la app a un receptor que estaba silenciado desde sí mismo
(mando del TV) no lo des-silencia: la app lo representa tal cual y el usuario decide si
quitar el silencio.

**Why this priority**: Conserva la funcionalidad de silencio ya valorada (0035) y corrige el
caso en que la propia conexión alteraba el silencio del receptor.

**Independent Test**: Con la app silenciada en local, conectar al Cast: el receptor arranca
silenciado y el botón sigue marcado. Con el receptor silenciado desde el mando del TV,
conectar: sigue silenciado y la app lo refleja.

**Acceptance Scenarios**:

1. **Given** la app silenciada reproduciendo en local, **When** el usuario conecta al
   dispositivo Cast, **Then** el receptor arranca silenciado y el botón de silencio mantiene
   su estado.
2. **Given** un receptor silenciado desde el mando del TV y la app sin silencio activo,
   **When** la app conecta, **Then** el receptor permanece silenciado y la app lo refleja.
3. **Given** una sesión Cast activa, **When** el usuario pulsa el botón de silencio,
   **Then** el receptor se silencia y, al volver a pulsarlo, recupera exactamente el nivel
   previo.
4. **Given** silencio activo con sesión Cast activa, **When** el usuario sube el volumen
   (teclas o barra del sistema), **Then** el receptor se des-silencia y el audio se
   restablece al nuevo nivel ajustado (comportamiento nativo del receptor).

---

### User Story 4 - El volumen que se ve es siempre el real del dispositivo (Priority: P3)

Si el usuario baja el volumen con el mando del TV, con Google Home o desde otro teléfono, la
barra del sistema refleja el nuevo nivel en un máximo de 2 segundos, también con la app en
segundo plano; al reconectar o volver a la app no aparecen valores caducados.

**Why this priority**: Da confianza en el control; hoy ya funciona mayoritariamente gracias
al comportamiento estándar de las sesiones de medios, por lo que es verificación y
conservación más que construcción.

**Independent Test**: Con sesión Cast activa, cambiar el volumen desde el mando del TV y
comprobar que la barra del sistema lo refleja en ≤ 2 s; repetir con la app en segundo plano
pulsando las teclas.

**Acceptance Scenarios**:

1. **Given** una sesión Cast activa, **When** el volumen se cambia desde el mando del TV o
   Google Home, **Then** la barra del sistema refleja el nuevo nivel en un máximo de 2
   segundos.
2. **Given** la app en segundo plano con sesión Cast activa, **When** el usuario pulsa las
   teclas de volumen, **Then** controlan el volumen del dispositivo Cast.

---

### Edge Cases

- ¿Qué ocurre si el dispositivo Cast no admite control de volumen? La app no hace nada
  especial: la reproducción continúa, la sesión no se desconecta y no se muestran avisos
  (FR-011).
- ¿El silencio de un receptor silenciado externamente se arrastra al teléfono al
  desconectar? No: al desconectar, la reproducción local retoma su volumen y el botón de
  silencio queda desactivado; solo el silencio iniciado por el usuario sobrevive al cambio
  de salida (FR-006).
- ¿Qué pasa si la sesión Cast se pierde en mitad de un ajuste de volumen? El ajuste termina
  sin bloquear la UI y las teclas vuelven al teléfono sin pasos manuales.
- ¿Qué ocurre si un cambio externo de volumen llega mientras el usuario ajusta con las
  teclas? Prevalece el último cambio aplicado, sin saltos bruscos.
- ¿Qué pasa con auriculares Bluetooth conectados a la vez que la sesión Cast? Mientras el
  Cast esté conectado, las teclas controlan el dispositivo Cast.
- ¿Qué ocurre si la sesión Cast está conectada pero en pausa? Los controles de volumen
  siguen disponibles y afectan al dispositivo.
- ¿Qué pasa si el usuario pulsa las teclas de volumen muy rápido? El volumen del receptor
  sigue el ritmo sin perder pulsaciones ni desincronizarse.
- ¿Qué pasa en grupos multi-room? El control afecta al volumen del grupo según lo exponga el
  receptor; no hay control por altavoz individual.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Al establecer o restablecer una conexión con un dispositivo Cast, el sistema
  MUST NOT modificar el volumen del receptor: el nivel audible previo se conserva intacto
  (ni 100% ni ningún otro valor impuesto por la conexión).
- **FR-002**: Con sesión Cast activa, las teclas de volumen y la barra del sistema MUST
  controlar exclusivamente el volumen del dispositivo Cast, identificándolo por su nombre;
  el volumen multimedia del teléfono MUST NOT cambiar.
- **FR-003**: Sin sesión Cast, las teclas de volumen y la barra del sistema MUST controlar
  exclusivamente el volumen del teléfono; el cambio de destino (teléfono ↔ dispositivo) MUST
  ser automático al conectar y desconectar, sin pasos manuales.
- **FR-004**: Con sesión Cast activa, la app MUST NOT mostrar ningún control de volumen
  propio: la barra del sistema es la única representación del volumen. *(Sustituye a
  FR-003/FR-004 de la spec 0035, que exigían el deslizador en el reproductor.)*
- **FR-005**: Al conectar, si el receptor estaba silenciado desde sí mismo, el sistema MUST
  respetar ese silencio (mantenerlo y representarlo) y MUST NOT des-silenciarlo
  automáticamente.
- **FR-006**: El botón de silencio de la app (mini-player y notificación) MUST silenciar y
  restablecer la salida activa (teléfono o dispositivo Cast), recordando el nivel previo.
  El silencio iniciado por el usuario MUST mantenerse al cambiar de salida; el silencio
  adoptado de un receptor silenciado externamente (eco) MUST NOT arrastrarse al teléfono
  al desconectar: se descarta y el botón vuelve a su estado.
- **FR-007**: Con sesión Cast activa, ajustar el volumen (teclas o barra del sistema)
  estando silenciado MUST restablecer el audio al nuevo nivel del receptor (comportamiento
  nativo del receptor). En salida local, el silencio se retira con el botón de silencio o
  al reproducir/detener (regla de la spec 004); las teclas del teléfono no des-silencian
  al reproductor de la app.
- **FR-008**: El nivel mostrado con sesión Cast activa MUST reflejar los cambios de volumen
  originados en el propio dispositivo o en otros mandos en un máximo de 2 segundos.
- **FR-009**: Ajustar el volumen MUST NOT interrumpir, pausar ni reiniciar la reproducción.
- **FR-010**: El control de volumen con sesión Cast activa MUST funcionar también con la app
  en segundo plano (teclas físicas y barra del sistema).
- **FR-011**: Si el dispositivo Cast no admite control de volumen, el sistema MUST mantener
  la reproducción y la sesión sin desconectar y sin mostrar avisos: no se realiza ninguna
  gestión especial (modelo Pocket Casts; el ajuste simplemente no tiene efecto en el
  receptor). *(Sustituye a FR-009 de la spec 0035, que exigía aviso y degradación.)*

### Key Entities

- **Volumen de la salida activa**: nivel audible de la salida en curso (teléfono o
  dispositivo Cast), representado al usuario únicamente por la barra del sistema.
- **Salida activa**: teléfono (reproducción local) o dispositivo Cast conectado; determina
  qué volumen controlan las teclas y qué muestra la barra del sistema.
- **Silencio de la app**: estado único de silencio que sobrevive al cambio de salida y se
  aplica a la salida activa por acción explícita del usuario.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Al conectar a un receptor con volumen al 30%, el nivel audible permanece en
  30% en el 100% de las conexiones (cero saltos al 100%).
- **SC-002**: Con sesión Cast activa, el 100% de las pulsaciones de teclas cambia solo el
  volumen del receptor, y la barra del sistema lo identifica en el 100% de los ajustes.
- **SC-003**: Sin sesión Cast, el 100% de las pulsaciones cambia solo el volumen del
  teléfono; al desconectar, las teclas vuelven al teléfono en el primer intento.
- **SC-004**: Con sesión Cast activa existe exactamente un control de volumen visible (el
  del sistema): cero controles de volumen dentro de la app.
- **SC-005**: Un receptor silenciado externamente permanece silenciado tras la conexión en
  el 100% de los casos.
- **SC-006**: Un cambio de volumen externo se refleja en la barra del sistema en un máximo
  de 2 segundos en al menos el 95% de los intentos.
- **SC-007**: Ningún ajuste de volumen interrumpe la reproducción en el 100% de los casos.

## Assumptions

- El modelo de referencia es Pocket Casts (Android): sin control de volumen dentro de la
  app; el volumen se gestiona con las teclas y la barra del sistema, cambiando de destino
  según la salida activa.
- Esta spec supersede parcialmente la 0035 ("Control de volumen del dispositivo
  Chromecast"): sustituye su deslizador en el reproductor (FR-003/FR-004 de la 0035), su
  degradación con aviso ante receptores sin volumen (FR-009 de la 0035) y corrige el
  comportamiento al conectar; el resto (teclas, silencio, eco de cambios externos) se
  conserva.
- La integración Cast (descubrimiento, conexión, reproducción remota, reconexión) ya
  funciona; esta spec solo cambia el comportamiento de volumen y silencio y su
  representación.
- El volumen es estado de sesión: no se persiste ni sincroniza entre salidas (cada salida
  recuerda su propio nivel, como el propio receptor).
- "Franja de volumen" designa la barra que muestra el sistema al pulsar las teclas de
  volumen.
- Terminología: "dispositivo Cast", "receptor" y "Chromecast" designan el mismo concepto; la
  UI muestra el nombre del dispositivo.
- El botón de silencio existente (mini-player y notificación) se conserva; no se añaden
  controles nuevos a la notificación.
