# Feature Specification: Enhanced Audio Player UX

**Feature Branch**: `010-enhanced-audio-player`

**Created**: 2026-09-07

**Status**: Done (2026-09-07; todo verificado en emulador)

**Input**: User description: "Necesitamos mejoras en el reproductor de audio para una experiencia más rica y mejorada, como notificaciones nativas con controles multimedia, si hago click en logo información más completa emisora. Otro comportamiento es que sí salgo y entro sigue reproduciendo pero se pierde panel flotante. Debemos hacerlo más profesional y consistente para los usuarios"

## Clarifications

### Session 2026-09-07

- Q: Cuando el usuario pulsa pause en la notificación nativa, ¿debe la notificación permanecer visible para permitir reanudar, o debe desaparecer como ocurre con stop? → A: La notificación permanece visible al pausar, mostrando botón de play para reanudar. Solo desaparece al pulsar stop o deslizar.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Notificación nativa con controles multimedia (Priority: P1)

Un usuario reproduce una emisora y minimiza la app (o bloquea el teléfono). La notificación nativa del sistema muestra el nombre de la emisora, el favicon y controles de reproducción (play/pause, stop). El usuario puede controlar la reproducción sin abrir la app.

**Why this priority**: Es la mejora más impactante para la experiencia de uso continuo. Sin notificación nativa, el usuario pierde control al salir de la app y la percepción de profesionalismo se degrada.

**Independent Test**: Reproducir una emisora, minimizar la app y verificar que la notificación aparece con controles funcionales. Pausar/reanudar desde la notificación y confirmar que el audio responde.

**Acceptance Scenarios**:

1. **Given** una emisora en reproducción, **When** el usuario minimiza la app o bloquea el dispositivo, **Then** aparece una notificación nativa con nombre de emisora, favicon y controles play/pause.
2. **Given** la notificación visible, **When** el usuario pulsa pause en la notificación, **Then** el audio se pausa, el icono cambia a play y la notificación permanece visible para reanudar.
3. **Given** la notificación visible, **When** el usuario pulsa play en la notificación, **Then** el audio se reanuda y el icono cambia a pause.
4. **Given** la notificación visible, **When** el usuario pulsa stop o desliza la notificación, **Then** la reproducción se detiene y la notificación desaparece.

---

### User Story 2 - Información completa de emisora al tocar logo (Priority: P2)

Un usuario reproduce una emisora y quiere saber más sobre ella. Al tocar el logo/favicon de la emisora en el reproductor (mini-player o full-player), se muestra una vista detallada con toda la información disponible: nombre completo, país, idioma, etiquetas/géneros, bitrate, codec, votos, enlace a homepage y estadísticas.

**Why this priority**: Hoy al tocar el logo no pasa nada o se abre algo mínimo. Mostrar información completa aumenta el engagement y permite al usuario descubrir más sobre la emisora que escucha.

**Independent Test**: Reproducir una emisora, tocar el logo en el reproductor y verificar que se muestra un panel/sheet con toda la información de la emisora.

**Acceptance Scenarios**:

1. **Given** una emisora en reproducción, **When** el usuario toca el logo/favicon en el mini-player o full-player, **Then** se muestra un panel con información completa: nombre, país, idioma, tags, bitrate, codec, votos, homepage.
2. **Given** el panel de información abierto, **When** el usuario toca el enlace de homepage, **Then** se abre el navegador con la página de la emisora.
3. **Given** el panel de información abierto, **When** el usuario toca fuera del panel o desliza hacia abajo, **Then** el panel se cierra y la reproducción continúa.

---

### User Story 3 - Panel flotante persistente al navegar (Priority: P1)

Un usuario está reproduciendo una emisora y navega a otra sección de la app (por ejemplo, de Explorar a Favoritos). El mini-player flotante debe permanecer visible y funcional en todas las pantallas. Si el usuario sale de la app y vuelve a entrar, el mini-player debe reaparecer con el estado correcto (emisora actual, estado de reproducción).

**Why this priority**: El bug reportado es que al salir y entrar se pierde el panel flotante. Esto rompe la continuidad de la experiencia y es inconsistente con apps profesionales como Pocket Casts o Spotify.

**Independent Test**: Reproducir una emisora, navegar entre secciones y verificar que el mini-player persiste. Salir de la app (home), volver a entrar y verificar que el mini-player reaparece con el estado correcto.

**Acceptance Scenarios**:

1. **Given** una emisora en reproducción, **When** el usuario navega de una sección a otra (Home → Explorar → Favoritos), **Then** el mini-player flotante permanece visible sobre la barra de navegación.
2. **Given** una emisora en reproducción, **When** el usuario pulsa home (minimiza) y vuelve a abrir la app, **Then** el mini-player reaparece con el nombre de la emisora, favicon y estado de reproducción correctos.
3. **Given** una emisora pausada, **When** el usuario vuelve a la app, **Then** el mini-player muestra estado pausado con botón play visible.
4. **Given** el mini-player visible, **When** el usuario toca el botón de expandir, **Then** se abre el full-player como bottom sheet.

---

### User Story 4 - Experiencia visual consistente y profesional (Priority: P3)

Un usuario nota que el reproductor tiene apariencia profesional: transiciones suaves al expandir/colapsar, estados claros (buffering, playing, paused, error), tipografía consistente con M3 y la paleta Tema Tolocha.

**Why this priority**: La consistencia visual es lo que separa una app "funcional" de una "profesional". Mejora la percepción de calidad sin cambiar funcionalidad.

**Independent Test**: Navegar por todas las pantallas del reproductor y verificar consistencia visual con el tema de la app.

**Acceptance Scenarios**:

1. **Given** el reproductor en cualquier estado, **When** el usuario interactúa con él, **Then** las transiciones y animaciones son fluidas (no cortes ni parpadeos).
2. **Given** un estado de error, **When** el usuario ve el reproductor, **Then** muestra un mensaje claro en español con botón de reintento.

---

### Edge Cases

- ¿Qué ocurre si la notificación ya existe de una sesión anterior y el usuario abre la app sin reproducir? La notificación debe limpiarse al no haber reproducción activa.
- ¿Cómo se comporta el mini-player si la emisora falla (stream caído) mientras el usuario navega? Debe mostrar estado de error persistente.
- ¿Qué pasa si el usuario tiene múltiples apps de radio con notificaciones? La notificación debe gestionar foco de audio correctamente (pausar al recibir foco de otra app).
- ¿Cómo se maneja la rotación de dispositivo con el full-player abierto? El estado debe preservarse.
- ¿Qué ocurre si el usuario cierra la app desde el multitarea (recents)? La reproducción debe detenerse limpiamente y la notificación desaparecer.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST mostrar una notificación nativa del sistema operativo cuando una emisora está en reproducción, incluyendo: nombre de la emisora, favicon y controles de reproducción (play/pause, stop).
- **FR-002**: La notificación MUST persistir mientras la reproducción esté activa o pausada (incluido background) y MUST desaparecer solo al pulsar stop, deslizar la notificación o cerrar la app desde multitarea.
- **FR-003**: Los controles de la notificación MUST ser funcionales: play/pause alterna el estado y mantiene la notificación visible, stop detiene la reproducción y elimina la notificación.
- **FR-004**: Al tocar el logo/favicon de la emisora en el mini-player o full-player, el sistema MUST mostrar un panel/modal con información completa de la emisora: nombre, país, idioma, tags/géneros, bitrate, codec, votos, clickCount y enlace a homepage.
- **FR-005**: El panel de información de emisora MUST ser dismissible (toque fuera, deslizar abajo, botón cerrar) sin interrumpir la reproducción.
- **FR-006**: El mini-player flotante MUST ser visible y funcional en TODAS las pantallas principales de la app (Home, Explorar, Favoritos, Historial, Mis Emisoras, Perfil) mientras haya una emisora en reproducción o pausada.
- **FR-007**: Al salir de la app (minimizar/home) y volver a entrar, el mini-player MUST reaparecer con el estado correcto: nombre de emisora, favicon y estado de reproducción (playing/paused/buffering/error).
- **FR-008**: El full-player (bottom sheet) MUST preservar su estado durante rotación de dispositivo y al volver de background.
- **FR-009**: Los estados del reproductor MUST ser claros y visuales: Buffering (indicador de carga), Playing (animación sutil), Paused (estático), Error (mensaje en español + botón de reintento).
- **FR-010**: El reproductor MUST respetar la paleta Tema Tolocha (verde-bosque, ocre-montaña, pine-950) y la tipografía M3 en todos sus estados.

### Key Entities

- **MediaSession**: Sesión multimedia del sistema que gestiona notificaciones nativas, controles de audio y foco de audio. Vinculada al MediaSessionService existente.
- **Station**: Emisora de radio con atributos para mostrar en el panel de información (name, country, language, tags, bitrate, codec, votes, clickCount, homepage, favicon).
- **PlayerState**: Estado sellado del reproductor (Idle, Buffering, Playing, Paused, Error) que alimenta tanto el mini-player como el full-player y la notificación.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las sesiones de reproducción muestran notificación nativa con controles funcionales al minimizar la app.
- **SC-002**: Al tocar el logo de la emisora, el panel de información muestra al menos 8 datos de la emisora (nombre, país, idioma, tags, bitrate, codec, votos, homepage).
- **SC-003**: Al salir y volver a la app, el mini-player reaparece con el estado correcto en el 100% de los casos (sin pérdida de estado).
- **SC-004**: El mini-player es visible en el 100% de las pantallas principales mientras hay reproducción activa o pausada.
- **SC-005**: Las transiciones del reproductor (expandir/colapsar, estados) se ejecutan en menos de 300ms sin parpadeos visibles.
- **SC-006**: Los mensajes de error son comprensibles en español y el botón de reintento funciona en el 100% de los casos.

## Assumptions

- La app ya utiliza Media3 ExoPlayer + MediaSessionService (verificado en constitución). La notificación nativa se implementa sobre esa base existente.
- Los datos de la emisora (Station) ya están disponibles en el modelo de datos actual con los campos requeridos (name, country, language, tags, bitrate, codec, votes, clickCount, homepage, favicon).
- El mini-player flotante ya existe (spec 004-floating-player); esta spec corrige el bug de persistencia y mejora la UX visual.
- El panel de información de emisora es un nuevo componente UI (bottom sheet o dialog) que se muestra al tocar el logo.
- La paleta Tema Tolocha y la tipografía M3 ya están definidas (spec 002-web-look-and-feel).
- Se asume compatibilidad con Android 8+ (API 26+, minSdk actual) para notificaciones de canal.
- El backend ya expone los campos de Station necesarios vía `/api/v1/stations/:id` (verificado en contrato OpenAPI).
