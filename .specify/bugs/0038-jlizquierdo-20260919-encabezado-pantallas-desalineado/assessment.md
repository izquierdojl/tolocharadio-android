# Bug Assessment: Encabezado desalineado en Mis emisoras / Favoritos / Historial

- **Slug**: 0038-jlizquierdo-20260919-encabezado-pantallas-desalineado
- **Created**: 2026-09-19
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: low

## Report (verbatim or summarized)

> Mira estas dos capturas de pantalla. En una se ve la pantalla de configuración con las proporciones correctas y perfectamente alineadas. En otra se ve la pantalla de mis emisoras con un espacio entre el texto "Mis Emisoras" y la barra superior, y no queda proporcionado. Este problema se extiende a las pantallas de Favoritos e Historial tambien. Tenemos que intentar resolverlo para que se vea de forma uniforme en todas las ventanas.

Adjuntos: `c:\tmp\configuracion.png` (referencia correcta), `c:\tmp\mis_emisoras.png` (con hueco extra entre TopAppBar y título).

## Symptom

En "Mis emisoras" (y según el reporte también en "Favoritos" e "Historial") hay un hueco vertical extra entre la TopAppBar global y el `SectionHeader` (título), mientras que en "Configuración" (y "Explorar") el título queda pegado a la barra con solo el padding del header (8dp vertical). El fondo del hueco deja ver el color de superficie del contenido, rompiendo la uniformidad visual exigida por la spec 012 (SC-003/SC-004: espaciado idéntico ±2dp, ≤8dp vertical).

## Reproduction

1. Abrir la app con un servidor configurado.
2. Navegar a "Configuración" (bottom bar) → observar distancia TopAppBar → "Configuración": mínima (~8dp).
3. Navegar a "Mis emisoras" → observar distancia TopAppBar → "Mis emisoras": visiblemente mayor, con banda de fondo claro.
4. Repetir en "Favoritos" ("Tus favoritos") e "Historial" ("Tu historial") → mismo hueco extra que en paso 3.
5. (Opcional) Comparar con "Explorar" → se comporta como "Configuración" (sin hueco).

[NEEDS CLARIFICATION: modelo de dispositivo / versión Android y modo claro/oscuro de las capturas; no debería importar porque es layout, pero conviene confirmarlo en la verificación manual.]

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/feature/customstations/CustomStationsScreen.kt:98-99` — `Scaffold(snackbarHost = ...) { paddingValues -> Column(Modifier.fillMaxSize().padding(paddingValues)) { SectionHeader(...) } }`. Único nivel extra frente a pantallas correctas.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt:189-190` — mismo patrón `Scaffold` interno + `padding(paddingValues)`.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/history/HistoryScreen.kt:139-140` — mismo patrón `Scaffold` interno + `padding(paddingValues)` (aquí además el `SectionHeader` va dentro de un `Row` con el botón "Limpiar", líneas 208-217, pero la causa raíz es la misma).
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt:46` — referencia correcta: `Column(Modifier.fillMaxSize()) { SectionHeader(...) }`, sin `Scaffold` interno.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/ExploreScreen.kt:80-81` — referencia correcta: `Column(Modifier.fillMaxSize()) { SectionHeader(...) }`, sin `Scaffold` interno.
- `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt:184-262` — `Scaffold` externo con `topBar`/`bottomBar` que ya aplica `Modifier.padding(padding)` al `NavHost` (línea 262). El contenido de cada destino ya llega posicionado bajo la TopAppBar.
- `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/SectionHeader.kt:28` — padding canónico `horizontal = 16.dp, vertical = 8.dp`; idéntico en las 5 pantallas, luego no es la causa diferencial.

## Root Cause Hypothesis

Confianza: **high**.

El `Scaffold` externo de `TolochaNavGraph` ya consume los insets del sistema + `topBar`/`bottomBar` y los pasa como `padding` al `NavHost`. Las tres pantallas afectadas añaden un **segundo `Scaffold` interno** (solo para alojar el `SnackbarHost` local: deshacer en Favoritos, mensajes en Historial/Mis emisoras). Aunque ese `Scaffold` interno no declara `topBar`, Material3 aplica por defecto `contentWindowInsets` (barras de sistema), de modo que `paddingValues` vuelve a incluir el inset superior y `Column(...padding(paddingValues))` desplaza el `SectionHeader` hacia abajo una segunda vez (doble inset). Configuración y Explorar no usan `Scaffold` interno y por eso quedan bien. Esto coincide además con la historia de la spec `012-bottom-nav-icons-only` (tasks T013 pedía explícitamente eliminar padding extra del `Scaffold` interno en Historial y Mis emisoras; Favoritos tiene el mismo patrón y quedó fuera de aquella limpieza).

## Proposed Remediation

**Preferred**: mantener el `SnackbarHost` local pero anular el doble inset del `Scaffold` interno en las 3 pantallas: pasar `contentWindowInsets = WindowInsets(0, 0, 0, 0)` (o equivalente `PaddingValues(0.dp)` según la API disponible) al `Scaffold` de `CustomStationsScreen`, `FavoritesScreen` e `HistoryScreen`, de forma que `paddingValues` solo refleje `snackbarHost`/slots reales y el `SectionHeader` quede a 8dp de la TopAppBar como en Configuración/Explorar. Cambio mínimo (3 líneas), sin tocar el `Scaffold` externo ni el `NavHost`, y preserva los snackbars locales (deshacer de Favoritos, errores de red).

**Alternatives** (optional):
- Eliminar el `Scaffold` interno y envolver el contenido en `Box`/`Column` con un `SnackbarHost` superpuesto manualmente. Más código y riesgo de tapar contenido; no aporta nada frente a anular insets.
- Reutilizar el `SnackbarHost` del `Scaffold` externo (`TolochaNavGraph`) y eliminar los internos. Unifica pero obliga a hoistear mensajes/undo entre ViewModels y el grafo (cambio de API, más riesgo de regresión).
- Dejar el inset y compensar con `offset` negativo o `consumeWindowInsets`. Frágil y dependiente del dispositivo; desaconsejado.

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/customstations/CustomStationsScreen.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/history/HistoryScreen.kt`

**Tests to add or update**:
- Nuevo test de UI Compose (androidTest): componer cada pantalla / `...ScreenContent` con el `Scaffold` externo simulado y asertar que la coordenada Y del `SectionHeader` ("Mis emisoras", "Tus favoritos", "Tu historial") coincide (±2dp) con la de "Configuración"/"Explorar". Extender `FavoritesScreenTest`, `ViewModeToggleTest` o `CommonUiTest` existentes.
- Verificación manual en emulador (spec 012 quickstart §32): comparar las 5 secciones en claro/oscuro; el espacio TopAppBar→título debe ser visualmente idéntico y ≤8dp.
- Gates obligatorios: `assembleDebug`, `testDebugUnitTest`, `detekt ktlintCheck lintDebug` (los tests Compose nuevos corren con `connectedDebugAndroidTest`).

## Risks & Considerations

- Si se anulan todos los insets del `Scaffold` interno, el `SnackbarHost` inferior podría quedar bajo la `NavigationBar`/mini-player en dispositivos con navegación por gestos; verificar que el snackbar sigue visible (el `Scaffold` externo ya gestiona el inset inferior, pero hay que comprobarlo visualmente).
- `ServerListScreen` usa también un `Scaffold` interno; revisar si aplica el mismo patrón, pero NO cambiarlo en este fix (fuera del alcance reportado) para mantener el PR pequeño.
- Sin riesgo de datos, auth, red ni migraciones: cambio puramente de layout.

## Open Questions

- [NEEDS CLARIFICATION: ¿el hueco se reproduce también en modo cuadrícula (toggle de vista) o solo en lista? El reporte muestra lista; el fix propuesto cubre ambos porque el `Scaffold` envuelve los dos modos, pero conviene verificarlo.]
- [NEEDS CLARIFICATION: ¿hay algún dispositivo/API donde el hueco no aparezca (p. ej. sin status bar translúcida)? Confirmar en emulador API 26 y API 37.]
