# Feature Specification: Chromecast Integration

**Feature Branch**: `011-chromecast-integration`

**Created**: 2026-09-07

**Status**: Done (2026-09-07; todo verificado en emulador)

**Input**: User description: "Integrar la aplicación con Chromecast para poder enviar el audio de la radio a un dispositivo Chromecast (TV, altavoz). El botón de Cast debe aparecer en la barra superior de la app, junto a los botones existentes."

## Clarifications

### Session 2026-09-07

- Q: ¿Qué receiver de Chromecast usar? → A: Default Media Receiver (CC1AD845) sin registro en Google. App personal con fines educativos.
- Q: ¿Qué ocurre con la reproducción local al conectar a Chromecast? → A: El audio se transfiere al Chromecast y se detiene localmente. Al desconectar, vuelve al dispositivo local.
- Q: Cuando el Chromecast se desconecta inesperadamente (pérdida de red, apagado), ¿debe la app reanudar automáticamente la reproducción local? → A: Sí, reanudar automáticamente sin diálogo. En radio, el usuario quiere seguir escuchando sin interrupciones.
- Q: Cuando el usuario selecciona una emisora diferente estando conectado a Chromecast, ¿dónde se reproduce? → A: Se reproduce automáticamente en el Chromecast. El usuario espera mantener la experiencia en la TV/altavoz.
- Q: Si la conexión con el Chromecast falla al intentar conectar, ¿qué experiencia debe tener el usuario? → A: Snackbar con mensaje de error ("No se pudo conectar al dispositivo") y botón de reintento, consistente con el patrón de errores del PlayerViewModel.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Botón de Cast visible en la barra superior (Priority: P1)

Un usuario abre la app y ve un icono de Chromecast en la barra superior, junto a los botones de Servidores y Modo de vista. El icono indica visualmente si hay dispositivos Chromecast disponibles en la red local.

**Why this priority**: Sin el botón visible y accesible, el usuario no puede iniciar la proyección. Es el punto de entrada obligatorio para toda la feature.

**Independent Test**: Abrir la app y verificar que el icono de Chromecast aparece en la TopAppBar. Verificar que el icono cambia de apariencia cuando hay un dispositivo Chromecast disponible en la red.

**Acceptance Scenarios**:

1. **Given** la app abierta en cualquier pantalla principal, **When** el usuario observa la barra superior, **Then** el icono de Chromecast es visible junto a los botones existentes.
2. **Given** no hay dispositivos Chromecast en la red, **When** el usuario ve el icono de Cast, **Then** el icono muestra un estado "no disponible" (gris o atenuado).
3. **Given** hay al menos un dispositivo Chromecast en la red, **When** el usuario ve el icono de Cast, **Then** el icono muestra un estado "disponible" (color activo).

---

### User Story 2 - Conectar a Chromecast y enviar audio (Priority: P1)

Un usuario está reproduciendo una emisora de radio en la app. Toca el botón de Chromecast, selecciona un dispositivo de la lista y el audio se transfiere al Chromecast. La TV o altavoz comienza a reproducir la emisora.

**Why this priority**: Es la funcionalidad central de la feature. Sin transferencia de audio, la integración no tiene valor.

**Independent Test**: Reproducir una emisora, tocar el botón de Cast, seleccionar un Chromecast y verificar que el audio sale del dispositivo Chromecast (no del teléfono).

**Acceptance Scenarios**:

1. **Given** una emisora en reproducción local, **When** el usuario toca el botón de Cast y selecciona un dispositivo, **Then** el audio se transfiere al Chromecast y la reproducción local se detiene.
2. **Given** audio reproduciéndose en Chromecast, **When** el usuario ve la app, **Then** el mini-player muestra el nombre de la emisora, el estado "Reproduciendo en Chromecast" y los controles de reproducción.
3. **Given** audio reproduciéndose en Chromecast, **When** el usuario toca play/pause en la app, **Then** el Chromecast responde correctamente (pausa/reanuda).
4. **Given** una emisora en estado Buffering, **When** el usuario conecta a Chromecast, **Then** la emisora comienza a reproducirse en el Chromecast después del buffering.
5. **Given** audio reproduciéndose en Chromecast, **When** el usuario selecciona una emisora diferente, **Then** la nueva emisora se reproduce automáticamente en el Chromecast sin desconexión.

---

### User Story 3 - Desconectar y volver al dispositivo local (Priority: P1)

Un usuario tiene audio reproduciéndose en un Chromecast. Toca el botón de Cast nuevamente y selecciona "Desconectar" o "Usar dispositivo local". El audio vuelve a reproducirse desde el teléfono.

**Why this priority**: La capacidad de desconectar es tan importante como la de conectar. Sin esto, el usuario quedaría "atrapado" reproduciendo en el Chromecast.

**Independent Test**: Conectar a Chromecast, reproducir audio, desconectar y verificar que el audio vuelve al dispositivo local sin interrupciones.

**Acceptance Scenarios**:

1. **Given** audio reproduciéndose en Chromecast, **When** el usuario toca el botón de Cast y selecciona "Desconectar", **Then** el audio se detiene en el Chromecast y se reanuda en el dispositivo local.
2. **Given** audio reproduciéndose en Chromecast, **When** la conexión con el Chromecast se pierde (red, apagado), **Then** la app detecta la desconexión y reanuda automáticamente la reproducción en el dispositivo local sin intervención del usuario.
3. **Given** el usuario desconecta del Chromecast, **When** vuelve al estado local, **Then** el mini-player muestra el estado normal sin indicador de Chromecast.

---

### User Story 4 - Control de volumen del Chromecast (Priority: P2)

*(SUPERSEDED por la spec 0037 FR-004 — converge 2026-09-20: la app ya no muestra control de
volumen in-app. Las teclas físicas y la barra del sistema controlan la salida activa de forma
nativa con media3 1.11.0; ver también T019/T020 marcadas como superseded en `tasks.md`.)*

Un usuario tiene audio reproduciéndose en un Chromecast. Puede ajustar el volumen del Chromecast desde la app usando los controles de volumen del teléfono o un slider en la interfaz.

**Why this priority**: El control de volumen mejora la experiencia pero no es esencial para la funcionalidad básica. El usuario puede usar el control remoto del Chromecast o del TV.

**Independent Test**: Conectar a Chromecast, reproducir audio y verificar que el volumen se puede controlar desde la app.

**Acceptance Scenarios**:

1. **Given** audio reproduciéndose en Chromecast, **When** el usuario ajusta el volumen con los botones del teléfono, **Then** el volumen del Chromecast cambia (no el del teléfono).
2. **Given** audio reproduciéndose en Chromecast, **When** el usuario ve el mini-player, **Then** el control de volumen está disponible y funcional.
   *(SUPERSEDED por 0037 FR-004: sin control de volumen in-app; la barra del sistema lo sustituye.)*

---

### User Story 5 - Estado visual claro de conexión Cast (Priority: P2)

Un usuario puede distinguir visualmente cuándo el audio se está reproduciendo en un Chromecast versus localmente. La app muestra un indicador claro del dispositivo Chromecast conectado.

**Why this priority**: La claridad visual evita confusión sobre dónde se está reproduciendo el audio, especialmente cuando el usuario alterna entre dispositivos.

**Independent Test**: Conectar a Chromecast y verificar que la UI muestra claramente el estado de conexión y el nombre del dispositivo.

**Acceptance Scenarios**:

1. **Given** audio reproduciéndose en Chromecast, **When** el usuario ve el mini-player, **Then** se muestra un indicador visual (icono de Cast) y el nombre del dispositivo Chromecast.
2. **Given** audio reproduciéndose localmente, **When** el usuario ve el mini-player, **Then** no se muestra indicador de Chromecast.

---

### Edge Cases

- ¿Qué ocurre si el usuario cierra la app mientras el audio se reproduce en el Chromecast? El audio debe detenerse en el Chromecast o continuar (según comportamiento estándar del receptor).
- ¿Qué pasa si hay múltiples Chromecasts en la red y el usuario selecciona uno diferente estando ya conectado a otro? Debe desconectar del actual y conectar al nuevo.
- ¿Cómo se comporta la app si el Chromecast se queda sin red durante la reproducción? La app detecta la pérdida de conexión y reanuda automáticamente la reproducción local.
- ¿Qué ocurre si el usuario reproduce una emisora diferente mientras está conectado a Chromecast? La nueva emisora se reproduce automáticamente en el Chromecast sin desconexión.
- ¿Cómo afecta el Chromecast al precheck de `playable`? El precheck debe ejecutarse antes de enviar al Chromecast.
- ¿Qué pasa si el usuario tiene la app en background y el Chromecast se desconecta? La notificación debe actualizarse para reflejar el estado local.
- ¿Qué ocurre si la conexión al Chromecast falla al intentar conectar? Se muestra un Snackbar con mensaje de error y botón de reintento, volviendo al estado local.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST mostrar un botón de Chromecast (MediaRouteButton) en la barra superior de la app, junto a los botones existentes de Servidores y Modo de vista.
- **FR-002**: El botón de Cast MUST mostrar el estado de disponibilidad de dispositivos Chromecast en la red local (disponible/no disponible).
- **FR-003**: Al tocar el botón de Cast, el sistema MUST mostrar una lista de dispositivos Chromecast disponibles para selección.
- **FR-004**: Al seleccionar un dispositivo Chromecast, el sistema MUST transferir la reproducción de audio actual del dispositivo local al Chromecast seleccionado.
- **FR-005**: La reproducción en Chromecast MUST respetar el precheck de `playable` antes de iniciar la transmisión.
- **FR-006**: El mini-player MUST mostrar un indicador visual cuando el audio se reproduce en un Chromecast, incluyendo el nombre del dispositivo.
- **FR-007**: Los controles de reproducción (play, pause, stop) en la app MUST funcionar igual cuando el audio se reproduce en Chromecast que cuando se reproduce localmente.
- **FR-008**: Al desconectar del Chromecast (manualmente o por pérdida de conexión), el sistema MUST reanudar automáticamente la reproducción en el dispositivo local sin intervención del usuario.
- **FR-009**: El sistema MUST gestionar el foco de audio correctamente al conectar/desconectar del Chromecast (no duplicar audio local y remoto).
- **FR-010**: La integración con Chromecast MUST usar el Default Media Receiver (CC1AD845) sin requerir registro en Google.
- **FR-011**: El sistema MUST desactivar la media session y notificaciones del SDK de Cast para evitar duplicados con la MediaSession existente de la app.
- **FR-012**: Si la conexión al Chromecast falla, el sistema MUST mostrar un Snackbar con mensaje de error claro en español y botón de reintento, volviendo al estado de reproducción local.
  *(Excepción anotada — converge 2026-09-20: el Snackbar "No se pudo conectar al dispositivo" SÍ
  se muestra (`PlayerUi.kt:83-92`) y se vuelve al estado local (`resumeLocalPlayback`), pero **no
  se ofrece botón de reintento**: la conexión se inicia exclusivamente desde el selector de rutas
  del sistema (`MediaRouteButton`, `TolochaNavGraph.kt:238`) y la app no puede reprogramar una
  selección de ruta sin reabrir el diálogo del framework (ruta sensible, bugs 0031/0036). El
  framework ya reintenta solo vía `onSessionResuming`/`RECONNECTING` y el botón de Cast del
  sistema. Reintento manual = volver a tocar el botón de Cast.)*

### Key Entities

- **CastDeviceInfo**: Dispositivo Chromecast descubierto en la red local. Atributos: deviceId, name, deviceType, isConnected.
- **CastConnectionState**: Estado de conexión con el Chromecast (DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING).
- **CastPlayerState**: Estado de reproducción (Local o Cast). Indica si el audio es local o remoto, con metadata del dispositivo.
- **PlayerState**: Estado sellado del reproductor existente (Idle, Buffering, Playing, Paused, Error). Se mantiene la misma interfaz, pero el player subyacente cambia entre local (ExoPlayer) y remoto (CastPlayer).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El botón de Chromecast es visible y accesible en el 100% de las pantallas principales de la app.
- **SC-002**: Al seleccionar un Chromecast, la transferencia de audio se completa en menos de 5 segundos (incluyendo buffering del Chromecast).
- **SC-003**: Los controles de reproducción (play/pause/stop) funcionan correctamente en el 100% de los casos cuando el audio está en Chromecast.
- **SC-004**: Al desconectar del Chromecast (manual o inesperado), el audio se reanuda automáticamente localmente en menos de 3 segundos sin intervención del usuario.
- **SC-005**: La app no muestra audio duplicado (local + Chromecast) en ningún caso.
- **SC-006**: La integración funciona con el Default Media Receiver sin configuración adicional del usuario.

## Assumptions

- La app ya utiliza Media3 ExoPlayer y MediaSessionService (verificado en código). La integración con Chromecast se construye sobre esta base existente.
- Se usará el módulo `media3-cast` de Media3 que proporciona un `CastPlayer` compatible con la interfaz `Player` de Media3.
- El Default Media Receiver (CC1AD845) es suficiente para esta implementación. No se requiere receiver personalizado.
- Los dispositivos Chromecast deben estar en la misma red local que el dispositivo Android.
- El flujo de autenticación y la API del backend no cambian. El Chromecast recibe la URL del stream directamente.
- La barra superior (TopAppBar) ya existe en `TolochaNavGraph.kt` y tiene espacio para añadir el botón de Cast.
- El mini-player existente puede adaptarse para mostrar el estado de Chromecast sin cambios estructurales mayores.
- La notificación nativa existente puede integrarse con el estado de Chromecast mediante MediaSession.
- No se requiere soporte para Chromecast en pantallas de login, registro u onboarding (solo pantallas principales autenticadas).
