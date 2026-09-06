# Research: Alternador de Vista Lista/Tarjetas

Feature: `008-view-mode-toggle` | Date: 2026-09-06

## D1. ¿Dónde vive la preferencia de modo de vista?

- **Decision**: Nueva preferencia en DataStore Preferences gestionada por `InstancePrefs` (`data/local/InstancePrefs.kt`), clave `view_mode` (String, nombre del enum). Sin Room ni backend.
- **Rationale**: La constitución (II) asigna ajustes y tema a DataStore; `InstancePrefs` ya centraliza theme/startScreen con el mismo patrón (`Flow` + `runCatching { valueOf() }.getOrDefault(DEFAULT)`), que resuelve directamente FR-005 (default LIST), FR-006 (sobrescritura) y FR-010 (valor corrupto → lista sin error). La spec prohíbe sincronizar con el servidor (Assumptions), y Room solo está para cachés de lectura de emisoras.
- **Alternatives considered**:
  - Room: sobreingeniería para un valor escalar (Principio V).
  - `SharedPreferences` clásico: prohibido por convención del proyecto (DataStore ya inyectado).
  - Guardar en servidor (`PATCH /users/me`): explícitamente fuera de alcance (Assumptions spec).

## D2. ¿Cómo se comparte el modo entre 4 pantallas y la TopAppBar global?

- **Decision**: Un único `ViewModeViewModel` (`@HiltViewModel`, obtenido a ámbito de Activity como `PlayerViewModel` en `TolochaNavGraph`), que expone `StateFlow<ViewMode>` y `toggle()`. Las 4 secciones lo colectan para renderizar; la `TopAppBar` compartida muestra el botón solo en rutas con lista.
- **Rationale**: FR-004 exige modo global simultáneo; una sola StateFlow de Activity elimina desincronización y colecciones duplicadas. Patrón ya establecido con `PlayerViewModel` (spec 004, R3). FR-003 se cumple porque el toggle solo reescribe el modo; los datos paginados/filtros viven en los ViewModels de sección y no se tocan.
- **Alternatives considered**:
  - Cada ViewModel de sección inyecta `InstancePrefs` y mantiene su propio estado: 4 colecciones + riesgo de desincronización al navegar; contradice FR-004.
  - `SettingsViewModel` global existente: mezcla responsabilidades de la pantalla Ajustes con chrome global; VM nuevo es más simple y con un solo consumidor claro.
  - Recolectar el Flow directo de `InstancePrefs` en cada Composable: salta la capa UI→domain→data y pierde testeabilidad de VM (Principio I/III).

## D3. ¿Dónde se ubica el control? (barra superior vs. ubicaciones locales)

- **Decision**: Action (`IconButton`) en la `TopAppBar` compartida del `Scaffold` de `TolochaNavGraph`, visible únicamente cuando la ruta actual es EXPLORE, FAVORITES, HISTORY o CUSTOM_STATIONS (FR-001, FR-009). Nuevo componente `ViewModeToggle` en `core/ui/components/`.
- **Rationale**: Existe una única TopAppBar global con `actions` (logo + servidores); añadir el toggle ahí garantiza misma posición en las 4 secciones con un solo composable. La spec (Assumptions) pide paridad con la web (control en la barra superior).
- **Alternatives considered**:
  - Repetir el toggle en cada pantalla: 4 puntos de mantenimiento, posiciones inconsistentes.
  - Mantener el toggle actual de Explorar (trailingIcon del buscador): no está en la barra superior y solo cubre Explorar; se elimina (código muerto, Principio V).

## D4. ¿Cómo renderiza el modo tarjetas en Favoritos/Historial/Mis emisoras?

- **Decision**: Reutilizar `StationCard` (`core/ui/components/CommonUi.kt`) en `LazyVerticalGrid` (2 columnas, paridad con Explorar). Las acciones específicas de sección (quitar de favoritos, quitar del historial, borrar emisora propia) se mapean al `onToggleFavorite`/acción equivalente disponible en la card; en Favoritos, el reordenar manual (drag) solo aplica en modo lista.
- **Rationale**: FR-008 exige misma información esencial y mismas acciones; `StationCard` ya las soporta (usada en Explorar). Reordenar en cuadrícula no tiene equivalente en la web y el orden personalizado es semántica de lista (`PUT /favorites/order` es una permutación); en grid se conserva el orden actual sin drag.
- **Alternatives considered**:
  - Cards específicas por sección duplicando `StationCard`: viola DRY y paridad visual.
  - Soportar drag en grid: sin paridad web, complejidad alta, cero valor demostrado (YAGNI).

## D5. ¿Persiste el modo en la misma escritura que el render o optimista?

- **Decision**: Optimista + persistente: `toggle()` emite el nuevo valor inmediatamente en la StateFlow y lanza escritura a DataStore en `viewModelScope`; el render de UI nunca espera al disco.
- **Rationale**: DataStore es lo bastante rápido, pero FR-003 pide re-presentación inmediata (<1s SC-001); separar emisión de persistencia evita jank. Si la escritura falla, el Flow de `store.data` reconverge al último valor persistido en el próximo arranque (FR-010 cae a LIST solo si el valor es ilegible).
- **Alternatives considered**:
  - Bloquear el toggle hasta confirmación de escritura: latencia perceptible innecesaria.
  - Debounce de escritura: innecesario (interacción humana, 1 escritura por pulsación).

## D6. ¿Qué iconos y semántica de accesibilidad?

- **Decision**: `Icons.Filled.GridView` cuando el modo actual es LIST (destino: tarjetas) y `Icons.Filled.ViewList` cuando es GRID (destino: lista); `contentDescription` dinámico "Cambiar a vista de tarjetas"/"Cambiar a vista de lista".
- **Rationale**: FR-002 (aclarado en spec) fija icono = modo destino; mismos iconos ya usados en ExploreScreen hoy (consistencia interna).
- **Alternatives considered**: icono del modo activo — descartado por decisión del usuario (Clarification Session 2026-09-06).
