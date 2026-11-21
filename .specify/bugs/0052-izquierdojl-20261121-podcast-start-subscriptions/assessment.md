# Bug Assessment: La pantalla de arranque de podcast no se aplica y su selector no sigue el estilo de Radio

- **Slug**: 0052-izquierdojl-20261121-podcast-start-subscriptions
- **Created**: 2026-11-21
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: medium

## Report (verbatim or summarized)

> En la pantalla de configuración, se especifica en los podcast cual es la sesión de inicio al acceder. Selecciono suscripciones y no funciona. La apariencia tampoco es la correcta, es un control desplegable en lugar del selector visual que hay para el apartado de radio. Revisa estos dos temas y corrigelos

No se aportó URL; el reporte es texto pegado.

## Symptom

Con **Pantalla de arranque en podcast = Suscripciones**, la app no abre
Suscripciones al iniciar en Podcasts (o al entrar en Podcasts): cae en la
pantalla de arranque de Radio o en Inicio de Podcasts. Además, el control de esa
preferencia es un desplegable, mientras que la pantalla de arranque de Radio usa
un selector segmentado visible; la UI queda inconsistente.

## Reproduction

1. Usar un servidor que ofrece podcasts.
2. Abrir **Configuración** y elegir **Sección inicial → Podcasts**.
3. En **Pantalla de arranque en podcast**, elegir **Suscripciones**.
4. Cerrar por completo la app y volver a abrirla.
5. **Observado**: no abre Suscripciones; según el modo guardado, abre la
   pantalla de arranque de Radio o Inicio de Podcasts.
6. Alternativa sin reiniciar: estando en Radio, pulsar **Podcasts**; se abre
   Inicio de Podcasts, no Suscripciones.
7. **Observado (apariencia)**: el selector de podcast es un desplegable, no el
   selector segmentado que usa Radio.

No se pudo reproducir en emulador en esta sesión; el fallo se deduce del flujo
de arranque y de la implementación actual.

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt:191-202`
  — sección "Pantalla de arranque en podcast"; usa `PodcastStartSettings`.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt:261-303`
  — `PodcastStartSettings` implementa la preferencia con `DropdownMenu` +
  `DropdownMenuItem`; el selector de Radio (`SettingsScreen.kt:161-189`) usa
  `SingleChoiceSegmentedButtonRow` + `SegmentedButton`.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/ModeViewModel.kt:18-22,40-47`
  — `ModeUi.defaultSection` se lee y se publica, pero no se usa para fijar
  `mode`; `mode` solo se inicializa desde `contentMode`. `loading` tampoco se
  consume en la navegación.
- `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt:522-555`
  — `PodcastRouteEffects`: el primer efecto resuelve el destino solo si
  `currentRoute == Routes.HOME`, pero no espera a que las capacidades de podcast
  estén resueltas ni incluye `currentRoute` en sus claves.
- `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt:546-554`
  — el guard de modo redirige cualquier ruta de podcast a `Routes.EXPLORE`
  cuando `podcastMode == false`, incluida la ruta de arranque recién resuelta.
- `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/Routes.kt:35-46`
  — `routeAfterModeChange` devuelve `PODCAST_HOME` al pasar de Radio a
  Podcasts, sin consultar la pantalla de arranque configurada.
- `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/StartDestination.kt:23-35`
  — `resolveStartDestination` es correcto y está testeado; el defecto está en el
  estado de modo/capacidades que lo rodea, no en la resolución pura.

## Root Cause Hypothesis

Hay dos defectos funcionales que se refuerzan y uno de apariencia.

**1. El modo de arranque no sale de la sección inicial.** `ModeViewModel`
inicializa `mode` desde `contentMode` (último modo usado, por defecto `RADIO`),
mientras que `defaultSection` se guarda en `ModeUi.defaultSection` y nunca se
aplica. Si el usuario nunca cambió de modo en la sesión, o cerró la app en
Radio, el arranque resuelve la ruta de podcast con `defaultSection == PODCAST`,
navega a `podcasts/subscriptions`, pero el guard de `PodcastRouteEffects`
comprueba `podcastMode` (que sigue en `false` porque `mode == RADIO`) y redirige
de inmediato a `EXPLORE`. Confianza: alta (se ve directamente en el código y
contradice FR-003 de la spec 0046 y su `data-model.md`).

**2. El cambio de modo ignora la pantalla de arranque.** `routeAfterModeChange`
hardcodea `PODCAST_HOME` al pasar de Radio a Podcasts, así que al pulsar
Podcasts se abre Inicio aunque la preferencia sea Suscripciones. Confianza: alta
(línea directa en `Routes.kt:45`).

**3. La resolución de arranque puede ejecutarse antes de conocer las
capacidades.** `podcastsEnabled` empieza en `false` y `modeUi.loading` no se
consulta. El efecto puede resolver el fallback de Radio y consumir la guarda
`currentRoute == HOME`; como `currentRoute` no es clave del `LaunchedEffect`, la
resolución correcta ya no se reintenta. Confianza: media (depende del orden de
emisión de DataStore y de la respuesta de `/config`).

**4. Apariencia.** `PodcastStartSettings` se implementó con `DropdownMenu`
(además, el contrato de UI de 0048 lo describe así) mientras que Radio usa
botones segmentados. Confianza: alta.

## Proposed Remediation

**Preferred**:

1. **Hacer que la sección inicial gobierne el arranque**: en `ModeViewModel`,
   tras resolver las capacidades (`loading = false`), fijar `mode` a
   `defaultSection` cuando `podcastsEnabled`, y a `RADIO` en caso contrario.
   Mantener `contentMode` para los cambios en caliente de la sesión, pero que no
   pise la decisión de arranque. Esto elimina el rebote de la ruta de podcast.
2. **No resolver el destino de arranque hasta que las capacidades estén
   resueltas**: esperar a `!modeUi.loading` antes de navegar; y ejecutar la
   resolución una sola vez (flag de "arranque aplicado") en lugar de depender de
   `currentRoute == HOME` sin tenerlo como clave. Evitar así que un fallback
   prematuro a Radio consuma la única oportunidad.
3. **Usar la preferencia al acceder a Podcasts**: cambiar
   `Routes.routeAfterModeChange` para recibir la ruta de arranque de podcast
   (o resolverla en `PodcastRouteEffects`) y navegar a
   `podcastStartScreen.route()` en lugar de `PODCAST_HOME` al pasar de Radio a
   Podcasts. Así "al acceder" funciona tanto en frío como al pulsar el selector.
4. **Igualar el selector visual**: sustituir el `DropdownMenu` de
   `PodcastStartSettings` por botones segmentados como los de Radio. Como hay
   cinco opciones y "Suscripciones" es larga, usar dos
   `SingleChoiceSegmentedButtonRow` apiladas (3 + 2: Inicio, Descubrir,
   Suscripciones / Cola, Favoritos), cada una con
   `SegmentedButtonDefaults.itemShape(index, count)` y estado `selected`
   correcto. Alternativa: `FlowRow` de `FilterChip`, pero los segmentados son
   los que ya usa Radio.
5. **Actualizar el contrato de UI**: `specs/0048-.../contracts/ui-contracts.md`
   indica "Desplegable" para esta preferencia; cambiarlo a selector segmentado
   para que la spec no contradiga la implementación.

**Alternatives** (optional):

- Mantener el desplegable y solo corregir la navegación. Es menos trabajo, pero
  no responde a la queja de apariencia ni a la consistencia con Radio.
- Un único `SingleChoiceSegmentedButtonRow` con las cinco opciones y texto
  reducido. No lo recomiendo: "Suscripciones" no cabe con tamaño de fuente
  normal y menos al 200 %.

**Files likely to change**:

- `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/ModeViewModel.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/Routes.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/TolochaTestTags.kt` (si se añaden tags por opción)
- `specs/0048-izquierdojl-20261119-podcast-ux-refinements/contracts/ui-contracts.md`
- Tests: `ModeViewModelTest.kt`, `StartDestinationTest.kt`,
  `SettingsPodcastStartTest.kt`, `SettingsLabelsTest.kt` (si cambian etiquetas).

**Tests to add or update**:

- `ModeViewModelTest`: con `defaultSection = PODCAST`, `contentMode = RADIO` y
  capacidades habilitadas, el modo de arranque es `PODCAST`; sin capacidades,
  `RADIO`. Verificar que un cambio de `defaultSection` en caliente no navega ni
  cambia el modo por sí solo.
- `StartDestinationTest` / nueva función pura de cambio de modo: al pasar de
  Radio a Podcasts se devuelve `Routes.PODCAST_SUBSCRIPTIONS` si esa es la
  preferencia, y `Routes.PODCAST_HOME` como fallback.
- `SettingsPodcastStartTest`: ya no debe abrir un menú; debe comprobar que se
  muestran las cinco opciones como botones segmentados y que pulsar
  **Suscripciones** invoca `onPodcastStartScreenChange(SUBSCRIPTIONS)`. Cubrir
  también el estado seleccionado y el scroll hasta la sección.
- Test de arranque (unitario de la lógica extraída o instrumentado): con
  capacidades aún cargando no se navega; una vez resueltas y en `HOME`, se
  navega una sola vez al destino configurado; no se rebota a `EXPLORE`.

## Risks & Considerations

- Cambiar la semántica de arranque puede ignorar el último modo usado
  (`contentMode`). Es lo que pide la spec 0046, pero conviene confirmarlo y, si
  `contentMode` deja de leerse al arrancar, documentarlo o retirarlo.
- Añadir `currentRoute` como clave del efecto de arranque puede reabrir el
  destino al volver a `HOME` desde una notificación; usar un flag de una sola
  ejecución.
- Dos filas de segmentados deben conservar semántica de grupo único
  (`selectableGroup`/`Role.RadioButton`) para accesibilidad.
- El contrato de 0048 dice "Desplegable": hay que actualizarlo o la próxima
  verificación de la spec fallará por diseño.
- No hay migración de datos: la clave `podcast_start_screen` y sus valores no
  cambian.
- Sin impacto en reproducción, red ni credenciales.

## Open Questions

- [NEEDS CLARIFICATION: ¿el fallo se observa tras reiniciar la app, al pulsar
  Podcasts desde Radio, o en ambos casos? El arreglo cubre ambos, pero ayuda a
  priorizar la verificación.]
- [NEEDS CLARIFICATION: ¿se acepta el selector segmentado en dos filas (3 + 2)
  para las cinco opciones, o se prefiere otra disposición visual?]
