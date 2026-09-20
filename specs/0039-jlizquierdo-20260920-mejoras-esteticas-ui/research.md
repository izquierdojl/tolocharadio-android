# Research: Mejoras estéticas UI

**Feature**: `0039-jlizquierdo-20260920-mejoras-esteticas-ui` | **Date**: 2026-09-20

Sin `NEEDS CLARIFICATION` abiertos: la sesión de clarificación del 2026-09-20 resolvió los 5 puntos materiales. Este documento consolida las decisiones y las alternativas descartadas.

## R1. Mecanismo dual de compartir en la ficha

- **Decision**: La ficha ofrece compartir del sistema (`ACTION_SEND` con el enlace como texto, principal) más copiar al portapapeles con aviso "Enlace copiado" (secundaria). Enlace en blanco o ausente → aviso "Enlace no disponible" sin copiar ni compartir nada.
- **Rationale**: Respuesta C de clarificación; conserva el gesto actual (copiar) y añade el compartir real que pedía el nombre de la función, sin permisos nuevos.
- **Alternatives considered**: Solo copiar (descartado: no cubre "compartir" hacia otras apps); solo menú del sistema (descartado: pierde el gesto rápido actual que la usuaria usa).

## R2. Ubicación: sección propia "Enlace" encima de la homepage

- **Decision**: Nueva sección "Enlace" dentro de `StationInfoSheet`, encima del bloque de homepage, con los botones Compartir y Copiar; la ficha ya es una columna con scroll, así que no desplaza el botón Cerrar.
- **Rationale**: Respuesta A de clarificación; sigue el patrón de secciones existente (Audio / Estadísticas / Homepage) y queda visible sin tocar cabecera ni pie.
- **Alternatives considered**: Fila junto a Cerrar (descartado: mezcla acción de contenido con despedida del sheet); icono en cabecera (descartado: poco descubrible y sin sitio para la opción secundaria).

## R3. Alcance del panel: solo reproducción normal

- **Decision**: El límite de 2 botones (principal + silenciar) aplica a sonando/pausado. Error conserva reintentar y carga conserva cancelar; el silencio sigue oculto en error (comportamiento vigente FR-003b).
- **Rationale**: Respuesta A de clarificación; preserva el manejo de errores accionable exigido por la constitución (IV) con el cambio mínimo.
- **Alternatives considered**: Dos botones siempre, incluso en error (descartado: eliminaría el reintento del panel y ampliaría el alcance); reintentar + silenciar en error (descartado: cambia el comportamiento vigente sin pedirlo).

## R4. Subtítulos confirmados

- **Decision**: Explorar → "Descubre emisoras de todo el mundo."; Tus favoritos → "Tus emisoras guardadas, en tu orden.". Mismo componente y estilo que el historial ("Lo último que has escuchado.").
- **Rationale**: Respuesta A de clarificación; tono explicativo paralelo al existente, textos ya cerrados antes de planificar.
- **Alternatives considered**: Versiones cortas y versiones de acción (descartadas en la votación frente a las propuestas).

## R5. Eliminación completa del menú de mover

- **Decision**: Borrar `FavoriteRowMenu`, sus llamadas y sus callbacks (`onMoveUp`/`onMoveDown` en `FavoriteListActions`/`FavoriteRowActions`, campos `canMoveUp`/`canMoveDown` si quedan sin uso), más imports huérfanos (`MoreVert`, `DropdownMenu*`). El asa de arrastre (`favoriteDragHandle`, `onMove`/`onCommit`, texto de ayuda) queda intacto. Verificar en implementación si `FavoritesViewModel.moveBy` queda sin consumidores de UI y, en ese caso, eliminarlo también con sus tests asociados.
- **Rationale**: Respuesta A de clarificación + principio YAGNI (código muerto se elimina, no se comenta).
- **Alternatives considered**: Solo ocultar la UI conservando la lógica (descartado: deja acciones muertas y deuda).

## R6. Reutilización del enlace real

- **Decision**: La ficha reutiliza `resolveCopyLink` (`station.url.trim().ifEmpty { null }`); el mismo valor alimenta el `ACTION_SEND` y el portapapeles, garantizando SC-004 sin duplicar lógica.
- **Rationale**: Paridad exacta con lo que ofrecía el panel y un único punto de verdad para "enlace disponible".
- **Alternatives considered**: Duplicar la resolución en la ficha (descartado: dos fuentes de verdad para el mismo dato).

## R7. Estrategia de tests y gates

- **Decision**: Sin unit tests nuevos de `domain`/`data` (no hay lógica nueva). Actualizar/añadir tests de UI Compose: favoritas sin menú + arrastre que persiste; mini-player con solo 2 acciones en normal y reintento en error; ficha con sección "Enlace" (compartir entrega el enlace, copiar avisa, sin enlace avisa no disponible); encabezados con subtítulo. Gates obligatorios: `assembleDebug`, `testDebugUnitTest`, `detekt ktlintCheck lintDebug`.
- **Rationale**: La constitución exige Compose Test solo en flujos críticos; los tres flujos tocados lo son (favoritas, player, ficha).
- **Alternatives considered**: Cubrir solo con tests manuales (descartado: viola el principio III para flujos críticos).
