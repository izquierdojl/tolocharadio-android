# Feature Specification: Reproductor flotante inferior

**Feature Branch**: `004-floating-player`

**Created**: 2026-09-05

**Status**: Done (2026-09-06; todo verificado salvo recorrido manual en dispositivo — quickstart §2 y TalkBack en emulador, igual que specs 002/003)

**Input**: User description: "En el apartado de favoritos ya se reproduce y se oyen bien algunas impresoras, supongo que en busqueda y en historial también se reproducirán. Necesitamos mejorar el reproductor con lo siguiente: - Que aparezca un reproductor flotante o en panel inferior encima de los botones. Tendrá las funciones en la parte derecha de play/pause, mute, copiar enlace de reproducción. En la parte izquierda se verá el icono/avatar de la emisora si dispone y a su derecha el nombre y aspectos técnicos. Planifiquemos esto"

**Alcance previo**: las specs `001-auth-explore-base` (US-5) y `002-web-look-and-feel` dejaron un mini-player básico (solo nombre + play/pausa + detener) colocado junto a la barra inferior. Esta spec lo eleva a panel inferior persistente con identidad de emisora y tres acciones a la derecha, visible desde cualquier sección con reproducción (Favoritos, Búsqueda/Explorar, Historial y ficha de emisora).

**Decisiones acordadas con el usuario (2026-09-05)**:
- Copiar enlace = copia la URL original del stream de la emisora (no el proxy autenticado ni la homepage).
- El botón de silencio sustituye al botón de detener en el panel: la derecha queda en play/pausa, silencio, copiar. Detener/cerrar vive solo en el reproductor completo.
- Posición = panel fijo justo encima de la barra inferior de navegación (no tarjeta arrastrable).

## Clarifications

### Session 2026-09-05

- Q: Cuando la reproducción falla y el panel muestra error, ¿qué botones debe ofrecer la parte derecha del panel? → A: Reintentar + copiar (play/pausa se convierte en reintentar, mute se oculta, copiar sigue disponible).
- Q: ¿En qué secciones debe aparecer el panel cuando hay una emisora activa? → A: Global — en todas las pantallas con barra inferior (Favoritos, Búsqueda/Explorar, Historial, Mis emisoras, Perfil e inicio), no solo las tres citadas.
- Q: Cuando la emisora trae todos los datos, ¿qué debe mostrar exactamente la segunda línea del panel? → A: País · idioma + calidad ("{país} · {idioma} · {codec} {bitrate} kbps", omitiendo lo que falte).
- Q: Al cambiar de emisora o al cerrar y reabrir la app, ¿debe mantenerse el estado silenciado? → A: No — se resetea siempre (al cambiar de emisora y al reiniciar vuelve con sonido).
- Q: Mientras la emisora está cargando, ¿qué debe hacer el botón principal del panel si el usuario lo pulsa? → A: Cancela — detiene el intento y oculta el panel (vuelve a sin-reproducción).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver qué suena sin perder la pantalla (Priority: P1)

Una persona reproduce una emisora desde Favoritos, Búsqueda o Historial y sigue navegando por la app mientras un panel fijo justo encima de los botones le muestra qué está sonando: a la izquierda el icono de la emisora (o un avatar genérico si no tiene), y a su derecha el nombre más datos técnicos (país/idioma y calidad cuando se conocen).

**Why this priority**: es el núcleo del pedido — sin panel persistente con identidad, el usuario no sabe qué suena al cambiar de sección.

**Independent Test**: reproducir una emisora, navegar por 3 secciones y comprobar que el panel sigue visible encima de los botones con icono, nombre y datos técnicos.

**Acceptance Scenarios**:

1. **Given** una emisora sonando, **When** navego a otra sección (p. ej. de Favoritos a Historial), **Then** el audio continúa y el panel sigue visible encima de la barra inferior con el mismo contenido.
2. **Given** una emisora con imagen, **When** miro el panel, **Then** veo su imagen a la izquierda, y a la derecha su nombre en una línea (recortado con puntos suspensivos) más una segunda línea con país/idioma y calidad si se conocen.
3. **Given** una emisora sin imagen, **When** miro el panel, **Then** veo un avatar genérico con la inicial o icono de radio, nunca un hueco roto.
4. **Given** nada en reproducción, **When** miro la parte inferior, **Then** no hay panel (solo la barra de botones habitual).
5. **Given** una pantalla sin barra inferior (login, registro, configuración inicial), **When** hay audio sonando, **Then** no se exige panel sobre botones inexistentes (el audio se controla desde la notificación del sistema / reproductor completo).

---

### User Story 2 - Controlar el audio desde el panel (Priority: P1)

Una persona que escucha una emisora pausa y reanuda con un toque, silencia el sonido sin detener la emisión, y copia el enlace original del stream para pegarlo donde quiera, todo desde los tres botones de la parte derecha del panel.

**Why this priority**: son las tres funciones pedidas explícitamente; sin ellas el panel es solo decorativo.

**Independent Test**: con una emisora sonando, pausar, silenciar, copiar el enlace y pegarlo en otra app comprobando que es la URL original de la emisora.

**Acceptance Scenarios**:

1. **Given** una emisora sonando, **When** pulso play/pausa, **Then** el audio se pausa y el icono cambia a "reanudar"; al pulsar de nuevo, el audio vuelve sin cambiar de emisora.
2. **Given** una emisora sonando, **When** pulso silencio, **Then** dejo de oírla pero la emisión sigue en curso (al quitar silencio vuelve el sonido al instante, sin reconectar); el icono indica estado silenciado.
3. **Given** una emisora sonando, **When** pulso copiar enlace, **Then** la URL original de su stream queda en el portapapeles y veo una confirmación breve ("Enlace copiado").
4. **Given** una emisora cargando (buffering), **When** miro la derecha del panel, **Then** veo indicador de carga; si pulso el botón principal durante la carga, el intento se cancela y el panel se oculta.
5. **Given** error de reproducción, **When** miro el panel, **Then** veo un mensaje breve en español a la izquierda y el botón principal ofrece reintentar.

---

### User Story 3 - Escuchar igual en toda la app (Priority: P2)

Una persona reproduce desde cualquier sección (Favoritos, Búsqueda, Historial, Mis emisoras…) y obtiene el mismo panel con los mismos controles; al cambiar de sección o abrir la ficha de la emisora, el estado (sonando/pausado/silenciado) es coherente en todas partes.

**Why this priority**: el usuario supone que "en búsqueda y en historial también se reproducirán" — hay que garantizar paridad global, no solo Favoritos.

**Independent Test**: reproducir una vez desde Búsqueda y otra desde Historial, comprobando que el panel aparece idéntico y el estado se conserva al navegar.

**Acceptance Scenarios**:

1. **Given** una emisora iniciada desde Búsqueda, **When** voy a Favoritos o Historial, **Then** el panel muestra la misma emisora y el mismo estado (sonando/pausado/silenciado).
2. **Given** audio pausado en una sección, **When** cambio de sección, **Then** sigue pausado (no se reanuda solo).
3. **Given** toco la zona izquierda del panel (icono/nombre), **When** quiero más detalle, **Then** se abre el reproductor completo con la misma emisora y controles ampliados (incluido detener).

---

### Edge Cases

- ¿Qué pasa si la emisora no trae URL original (campo vacío)? El botón de copiar muestra "enlace no disponible" y no copia texto vacío ni falla.
- ¿Qué pasa si la emisora no trae datos técnicos (sin país/idioma/codec/bitrate)? La segunda línea muestra solo lo disponible ("Emisora de radio" o similar como fallback), nunca texto técnico crudo ni huecos vacíos.
- ¿Qué pasa si la imagen no carga (URL rota o sin red)? Se muestra el avatar genérico sin reintentos visibles ni parpadeo.
- ¿Qué pasa si el usuario pulsa copiar sin portapapeles disponible o el sistema lo rechaza? Se muestra aviso breve y no se finge el copiado.
- ¿Qué pasa si el usuario silencia y a la vez pausa? Ambos estados coexisten: al reanudar sigue silenciado hasta quitar silencio, y el panel refleja ambos iconos correctamente.
- ¿Qué pasa al girar el dispositivo o ir a segundo plano? El panel conserva emisora y estado; el audio no se corta por rotación.
- ¿Qué pasa con lectores de pantalla? Los tres botones tienen descripción ("Pausar/Reanudar", "Silenciar/Activar sonido", "Copiar enlace") y el panel anuncia "Sonando: {nombre}".

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST mostrar un panel fijo justo encima de la barra inferior de navegación siempre que haya una emisora activa (cargando, sonando, pausada o en error), en todas las pantallas con barra inferior (Favoritos, Búsqueda/Explorar, Historial, Mis emisoras, Perfil e inicio), y MUST ocultarlo cuando no haya nada en reproducción.
- **FR-002**: La zona izquierda del panel MUST mostrar el icono de la emisora (su imagen cuando exista y cargue; avatar genérico en caso contrario) y a su derecha el nombre en una línea con recorte, más una segunda línea con formato "{país} · {idioma} · {codec} {bitrate} kbps" omitiendo cada parte que falte (si no hay ningún dato, "Emisora de radio"); PROHIBIDO mostrar identificadores internos o texto de error técnico en estas líneas.
- **FR-003**: La zona derecha del panel MUST ofrecer exactamente tres acciones en estado normal (play/pausa, silencio y copiar enlace), con iconos que reflejan el estado actual (pausado vs sonando, silenciado vs con sonido). En estado de error rige FR-010 en lugar de este layout.
  *(SUPERSEDED por la spec 0039 FR-003 — converge 2026-09-20: en reproducción normal solo
  play/pausa + silenciar; sin botón de compartir/copiar en el panel. Compartir/copiar pasa a la
  ficha de la emisora, 0039 FR-004.)*
- **FR-003b**: En estado de error, el botón play/pausa se convierte en reintentar, el botón de silencio se oculta y el botón de copiar sigue disponible (layout de error: reintentar + copiar).
  *(SUPERSEDED parcialmente por la spec 0039 FR-003/FR-004: en error solo reintentar (+cancelar en
  carga); el botón de copiar del panel desaparece.)*
- **FR-004**: El botón play/pausa MUST pausar y reanudar la misma emisora sin cambiar de emisora ni reiniciar la lista; durante la carga MUST mostrar espera y pulsar el botón principal MUST cancelar el intento (detiene la carga y oculta el panel, volviendo a sin-reproducción).
- **FR-005**: El botón de silencio MUST cortar el sonido sin detener la emisión (al quitarlo el sonido vuelve al instante sin reconectar) y MUST conservar su estado al navegar entre secciones; es independiente de pausar. El silencio MUST resetearse al cambiar de emisora y al reiniciar la app (siempre vuelve con sonido).
- **FR-006**: El botón de copiar MUST llevar al portapapeles la URL original del stream de la emisora, MUST mostrar confirmación breve en español, y si no hay URL disponible MUST mostrar "enlace no disponible" sin copiar nada.
  *(SUPERSEDED por la spec 0039 FR-004 — converge 2026-09-20: el copiar/compartir se ofrece desde
  la ficha de la emisora, no desde el panel.)*
- **FR-007**: Tocar la zona izquierda del panel (icono/nombre) MUST abrir el reproductor completo con la misma emisora; los tres botones de la derecha no MUST abrir el completo al pulsarlos (solo ejecutan su acción).
  *(SUPERSEDED por la spec 010 FR-004 — converge 2026-09-20: el tap abre `StationInfoSheet`; el
  reproductor completo se abre desde el estado de reproducción/notificación.)*
- **FR-008**: El reproductor completo MUST conservar la acción de detener/cerrar (sale del estado de reproducción y oculta el panel); el panel inferior no ofrece detener.
- **FR-009**: El panel MUST reflejar el mismo estado en todas las pantallas con barra inferior (Favoritos, Búsqueda/Explorar, Historial, Mis emisoras, Perfil, inicio) y en la ficha de emisora: la misma emisora y los mismos estados (cargando/sonando/pausado/silenciado/error) en todas ellas.
- **FR-010**: En estado de error, el panel MUST mostrar un mensaje breve en español con opción de reintentar desde el botón principal (que sustituye a play/pausa según FR-003b; copiar sigue disponible y silencio queda oculto), y nunca una pantalla en blanco ni texto técnico crudo.
  *(SUPERSEDED parcialmente por la spec 0039 FR-003 — converge 2026-09-20: en error, copiar ya no
  está disponible; solo reintentar.)*
- **FR-011**: Fuera de alcance: ecualizador, temporizador de apagado, cola de reproducción, historial de lo copiado y reordenar el panel; esta spec no cambia cómo se descubre ni se guarda una emisora.

### Key Entities

- **Emisora en reproducción**: lo que está sonando o intentando sonar (`nombre`, `imagen` opcional, `urlOriginal` del stream, `país`, `idioma`, `codec`, `bitrate`); la imagen y los técnicos pueden faltar y el panel debe degradarse con elegancia.
- **Estado del reproductor**: situación actual visible en el panel (`cargando` / `sonando` / `pausado` / `silenciado` sí-no / `error` con mensaje breve); el silencio es un modificador independiente que se combina con sonando/pausado.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Una persona que reproduce una emisora reconoce qué suena (nombre + icono) en el panel en menos de 2 segundos con red normal.
- **SC-002**: 9 de cada 10 personas pausan/reanudan, silencian y copian el enlace al primer intento sin ayuda.
- **SC-003**: El enlace copiado desde el panel es la URL original de la emisora en el 100 % de las pruebas con emisoras que la tienen, verificable pegándolo fuera de la app.
- **SC-004**: Navegar por 3 secciones con audio en curso no interrumpe el sonido ni oculta el panel en el 100 % de las pruebas con red normal.
- **SC-005**: Con la emisión silenciada, el sonido vuelve al instante al quitar silencio (sin reconexión perceptible) en el 100 % de los casos probados.
- **SC-006**: Ante emisora sin imagen o sin datos técnicos, el panel muestra avatar genérico y línea alternativa legible en el 100 % de los casos (nunca hueco roto ni texto técnico).

## Assumptions

- La "impresora" del mensaje original se interpreta como "emisora" (errata de dictado).
- La reproducción base ya funciona (Favoritos suena; Búsqueda e Historial usan el mismo motor); esta spec solo mejora el panel, no el motor de audio.
- Copiar lleva la URL original del stream tal cual la trae la emisora; si algún día la emisora solo trae proxy, se copiará ese valor con el mismo aviso — no se construyen URLs en cliente.
- Si la emisora no trae URL, el botón informa y no copia; si no trae homepage tampoco, no se sustituye por nada.
- El silencio se implementa como corte de volumen local (la emisión sigue corriendo por debajo); no equivale a pausar ni a detener.
- Detener sigue existiendo solo en el reproductor completo y en la notificación del sistema; el usuario lo aceptó así al elegir "mute sustituye detener".
- El panel vive encima de la barra inferior (orden visual: contenido → panel → botones); en pantallas sin barra inferior no se exige panel.
- Segunda línea del panel: formato "{país} · {idioma}" más calidad "{codec} {bitrate} kbps" solo si se conocen; si no hay nada, "Emisora de radio".
