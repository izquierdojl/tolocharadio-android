# Feature Specification: Adaptación look-and-feel a la web

**Feature Branch**: `002-web-look-and-feel`

**Created**: 2026-09-05

**Status**: Done (2026-09-05; T029–T033 asumidos sin implementar, resto verificado)

**Input**: User description: "Adaptar el aspecto y el look and feel de la aplicación para que se asemeje a los colores, tonos, formas e iconos de la web. Enlace web: https://github.com/izquierdojl/tolocharadio/tree/main/apps/web. Descarga también los iconos, ficheros svg para incorporarlos en la aplicación como icono, como logotipo, etc."

**Fuente de verdad visual**: `apps/web/src/index.css` (tokens `@theme` pine/ochre/moss + variables `dark`/`light`), `apps/web/src/components/SierraEmblem.tsx`, `apps/web/index.html` (favicon SVG inline), `apps/web/src/components/AppShell.tsx` (Logo, MountainWall, Header), `apps/web/src/components/StationCard.tsx` + `StationListItem.tsx` + `PlayerBar.tsx` + iconos Lucide.

## Clarifications

### Session 2026-09-05

- Q: ¿Cómo debe elegir el usuario entre tema claro y oscuro en la app Android? → A: Sistema por defecto + selector manual de 3 estados (Sistema/Claro/Oscuro) en Ajustes/Perfil, guardado local
- Q: ¿Qué superficies debe cubrir el nuevo emblema además del logotipo? → A: Dentro de la app + launcher adaptativo + splash + notificación + atajos y widget si existen
- Q: ¿Debe empaquetarse la fuente Inter en la app o basta con la fuente del sistema? → A: Solo fuente del sistema mapeando pesos y tamaños
- Q: ¿Con qué set de iconos deben implementarse navegación y acciones? → A: Material Symbols con el mismo significado que Lucide
- Q: ¿Qué nivel de contraste debe exigirse a texto/fondo y marca/fondo? → A: WCAG AA (4.5:1 texto, 3:1 grande y componentes)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reconocer la marca Tolocha en Android (Priority: P1)

Un oyente que conoce la web abre la app Android y reconoce inmediatamente la misma identidad: mismos verdes bosque, acento ocre, emblema de la sierra con sol y antena, y logotipo "TolochaRadio".

**Why this priority**: Es el núcleo del pedido — paridad visual y de marca. Sin esto el resto (tonos, formas) no tiene anclaje.

**Independent Test**: Instalar la app y comparar lado a lado con la web en modo oscuro y claro: logotipo, colores de fondo/cabecera y emblema se perciben como la misma marca. Entrega valor por sí sola aunque no se retoquen el resto de componentes.

**Acceptance Scenarios**:

1. **Given** la app instalada, **When** el usuario abre cualquier pantalla, **Then** ve el emblema de la sierra (montaña + sol ocre + antena con ondas) como logotipo en la cabecera y como icono de la app.
2. **Given** modo oscuro activo, **When** el usuario mira fondo, superficies y textos, **Then** percibe fondo verde-pino muy oscuro, superficies elevadas ligeramente más claras y acento ocre en marca/acciones, equivalentes a la web.
3. **Given** modo claro activo, **When** el usuario cambia el tema, **Then** ve fondo claro verdoso, superficies blancas y acento ocre oscuro, equivalentes a la web.

---

### User Story 2 - Leer y navegar con los mismos tonos, formas y jerarquía (Priority: P2)

Un usuario navega por Home, Explorar, Favoritos, Historial, Mis emisoras y Perfil y percibe las mismas tarjetas, chips, botones redondeados, estados vacíos y barra de reproductor que en la web.

**Why this priority**: La consistencia de componentes es lo que hace que "se sienta como la web" más allá del logo.

**Independent Test**: Recorrer las 6 secciones y el mini-reproductor verificando tarjetas con imagen 16:9 + degradado, chips de etiquetas en mayúsculas, botones de play circulares y radios de esquina equivalentes a la web.

**Acceptance Scenarios**:

1. **Given** una lista de emisoras, **When** se muestra en modo tarjeta, **Then** cada tarjeta muestra imagen/favicon con degradado inferior, botón circular de play abajo-izquierda, favorito arriba-derecha, título, país/idioma y hasta 3 chips de etiqueta más bitrate.
2. **Given** la cabecera y pie visual, **When** el usuario hace scroll, **Then** reconoce la franja de silueta de montañas y el mismo estilo de cabecera con navegación y menú de usuario.
3. **Given** una lista vacía o un error, **When** no hay emisoras, **Then** se muestra un estado vacío con el mismo tono e iconografía que la web en lugar de una pantalla en blanco.

---

### User Story 3 - Iconografía coherente y assets SVG incorporados (Priority: P3)

Un usuario identifica cada sección y acción por los mismos iconos que en la web (explorar, favoritos, historial, mis emisoras, perfil, play, menú) y el equipo dispone de los SVG originales versionados en el repo Android.

**Why this priority**: Cierra el pedido explícito de "descargar los SVG e incorporarlos como icono/logotipo".

**Independent Test**: Verificar que los iconos de navegación y acciones coinciden en forma/significado con los de la web y que los ficheros SVG originales están guardados en el repo y referenciados como recursos de la app.

**Acceptance Scenarios**:

1. **Given** la barra de navegación, **When** el usuario cambia de sección, **Then** cada destino usa un icono del mismo significado que en la web (casa/explorar, corazón/favoritos, reloj/historial, radio/mis emisoras, persona/perfil).
2. **Given** los assets de la web (emblema, favicon, silueta de montañas), **When** se inspecciona el repo Android, **Then** existen copias locales de los SVG con su origen documentado y se usan como logotipo, icono de launcher/notificación y placeholder de emisora personalizada.
3. **Given** una emisora personalizada sin favicon, **When** se muestra su tarjeta, **Then** se ve el emblema de la sierra como placeholder, igual que en la web.

---

### Edge Cases

- ¿Qué ocurre cuando un favicon remoto falla o está vacío? Se muestra placeholder (emblema o icono de radio) sobre fondo degradado verde, sin hueco roto.
- ¿Cómo se comporta el tema si el sistema está en oscuro pero el ajuste local pide claro (o viceversa)? Gana el ajuste local (Sistema/Claro/Oscuro); por defecto sigue al sistema, con oscuro como punto de partida coherente con la web y la constitución.
- ¿Qué pasa con contraste/accessibilidad en ocre sobre pino oscuro y viceversa? Los pares texto/fondo deben mantener legibilidad en ambos temas.
- ¿Qué ocurre si un SVG de la web usa variables CSS (`var(--emblem-*)`)? La versión Android debe llevar los colores resueltos para cada tema, no la variable CSS literal.
- ¿Cómo se evita que el icono launcher pierda detalle a tamaños pequeños? Se usa versión simplificada/legible del emblema para launcher y notificación.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La app MUST aplicar la paleta de la web como referencia única: escala pine (12 tonos desde `#08100b` hasta `#f0f5f1`), escala ochre (10 tonos desde `#2b1d0c` hasta `#f7ecdb`) y acentos moss, con roles superficie, superficie-elevada, texto principal, texto atenuado, líneas y marca definidos por separado para tema oscuro y claro según `index.css`.
- **FR-002**: La app MUST ofrecer tema oscuro por defecto y tema claro alternativo con los mismos roles que la web (oscuro: fondo pine-950, elevada pine-900, texto pine-100, marca ochre-400 `#d3a568`; claro: fondo `#eef3ef`, elevada `#ffffff`, texto `#123424`, marca ochre-600 `#a67430`), incluyendo el fondo con matices radiales/lineales equivalentes cuando sea viable en nativo.
- **FR-003**: La app MUST usar el emblema Sierra (rectángulo redondeado radio 14/64, degradado insignia, sol ocre, sierra verde, antena con ondas) como logotipo en cabecera, placeholder de emisoras personalizadas, estado vacío relevante y favicon/icono de referencia.
- **FR-004**: La app MUST incorporar al repo Android copias locales de los SVG de la web (emblema, favicon de `index.html`, silueta `MountainWall`) con documento de origen/licencia, y MUST usarlos como recursos de imagen (logotipo, icono launcher adaptativo, splash, icono de notificación monocromo, placeholder, más atajos y widget donde existan).
- **FR-005**: La app MUST reproducir las formas de la web: tarjetas con esquinas grandes (equivalente a `rounded-2xl`), botones y campos con esquina media (`rounded-lg`), chips y botón de play circulares (`rounded-full`), insignia de avatar circular y bordes sutiles sobre superficies elevadas.
- **FR-006**: Los usuarios MUST poder reconocer las tarjetas de emisora con la misma jerarquía que la web: imagen 16:9 con degradado inferior, play circular inferior-izquierdo (ocre cuando reproduce), favorito superior-derecho, título, línea país · idioma, hasta 3 chips en mayúsculas y bitrate alineado a la derecha.
- **FR-007**: La app MUST usar Material Symbols con el mismo significado que la web (Lucide: Home/Explorar, Heart/Favoritos, History/Historial, Radio/Mis emisoras, UserRound/Perfil, Play/Pausa, Menu/X, ChevronDown, LogOut, GitHub) con estilo de trazo coherente en toda la app.
- **FR-008**: La app MUST usar la fuente del sistema mapeando la jerarquía de la web (título tarjeta semibold truncado, metadatos pequeños atenuados, chips en mayúsculas pequeñas con tracking amplio); queda excluido empaquetar Inter.
- **FR-009**: La app MUST mantener la franja decorativa de silueta de montañas sobre el reproductor/pie con el color de montaña de cada tema (oscuro translúcido verdoso, claro `#b4c9b9`).
- **FR-010**: La app MUST ofrecer selector de tema de 3 estados (Sistema / Claro / Oscuro) en Ajustes/Perfil, guardado local en el dispositivo; por defecto sigue al sistema (que en la práctica es oscuro según constitución y web). La preferencia MUST aplicarse sin pantallazos de tema mezclado al navegar o al reanudar desde segundo plano.
- **FR-011**: La app MUST cumplir WCAG AA en ambos temas para todas las combinaciones texto/fondo y marca/fondo usadas en cabecera, tarjetas, chips, botones y reproductor (4.5:1 texto normal, 3:1 texto grande y componentes).
- **FR-012**: Alcance excluido: no se rediseñan flujos ni navegación, no se añaden secciones nuevas, no se cambia el contrato `/api/v1`; solo aspecto, tokens, formas e iconos.

### Key Entities

- **Design tokens**: Paleta pine/ochre/moss + roles por tema (surface, surface-raised, foreground, muted, line, brand, mountain) y radios de esquina; representan la traducción de `index.css` a tema Android.
- **Brand assets**: Emblema Sierra, favicon, silueta MountainWall, icono GitHub; cada uno con fichero SVG origen, versión por tema si aplica y uso (launcher, cabecera, placeholder, notificación).
- **Component styles**: Estilos de tarjeta, chip, botón play/favorito, cabecera, estado vacío y reproductor; representan la equivalencia visual con `StationCard`, `AppShell`, `PlayerBar` y `EmptyState` de la web.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 9 de cada 10 usuarios que conocen la web identifican la app Android como "la misma marca" en una comparativa lado a lado (oscuro y claro).
- **SC-002**: El 100 % de las pantallas principales (Home, Explorar, Favoritos, Historial, Mis emisoras, Perfil, Login/Registro, reproductor) aplican los nuevos tokens sin restos de la paleta anterior.
- **SC-003**: Los SVG de la web están versionados en el repo y visibles en la app: logotipo en cabecera, icono launcher y placeholder de emisora personalizada usan el emblema sin pixelado en densidades comunes.
- **SC-004**: Usuarios completan el recorrido explorar → reproducir → marcar favorita sin confusión visual (tasa de éxito en primer intento ≥ 90 % en prueba con 10 participantes).
- **SC-005**: El cambio de tema oscuro/claro se refleja en toda la app en menos de 1 segundo y sin pantallas con mezcla de temas.

## Assumptions

- Se asume que la web en `main` (`index.css`, `SierraEmblem.tsx`, `index.html`, `AppShell.tsx`) es la referencia válida; si la web cambia durante la implementación, se congela la versión citada en esta spec.
- Se asume que los SVG de la web son propiedad del proyecto TolochaRadio y reutilizables en Android sin restricción externa; se documentará origen en el repo.
- Se asume que se usa la fuente del sistema (decisión de clarificación 2026-09-05); Inter queda descartado para evitar coste de empaquetado, manteniendo jerarquía y pesos.
- Se asume que el launcher adaptativo usará una versión simplificada del emblema para legibilidad en tamaños pequeños y fondo pine-950.
- Se asume que la equivalencia de iconos Lucide se resuelve con la librería de iconos del sistema de diseño Android ya aprobada, sin añadir dependencias nuevas salvo justificación en PR (constitución V).
- Dependencia: el tema actual `Theme.kt` (solo 4 colores) se amplía a la escala completa; no se tocan `domain`/`data` salvo exponer la preferencia de tema si aún no existe.
