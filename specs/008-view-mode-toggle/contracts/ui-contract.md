# UI Contract: Alternador de Vista Lista/Tarjetas

Feature: `008-view-mode-toggle` | Date: 2026-09-06

Contrato de la interfaz interna de la app (no hay cambios de API externa: ni backend, ni formatos de intercambio). Define el comportamiento observable que deben cumplir los componentes y los tests.

## 1. ViewModeViewModel (ámbito Activity)

```kotlin
enum class ViewMode { LIST, GRID }

@HiltViewModel
class ViewModeViewModel : ViewModel() {
    val mode: StateFlow<ViewMode>          // inicial: valor persistido (default LIST)
    fun toggle()                            // emite el modo alternativo y persiste (optimista)
}
```

Garantías:
- `mode` converge al valor persistido; todas las pantallas que lo colectan observan el mismo valor simultáneamente (FR-004).
- `toggle()` NO dispara red ni recarga de datos de sección (FR-003).
- Valor corrupto en disco → `LIST` sin error (FR-010).

## 2. ViewModeToggle (componente, core/ui/components)

```kotlin
@Composable
fun ViewModeToggle(mode: ViewMode, onToggle: () -> Unit)
```

| Estado actual | Icono mostrado (modo destino) | contentDescription |
|---|---|---|
| `LIST` | `Icons.Filled.GridView` | "Cambiar a vista de tarjetas" |
| `GRID` | `Icons.Filled.ViewList` | "Cambiar a vista de lista" |

- `IconButton` M3, área táctil estándar (48dp), accesible por teclado/switch access (FR-002).

## 3. Visibilidad del alternador (TopAppBar compartida)

| Ruta actual | ¿Se muestra el toggle? |
|---|---|
| EXPLORE | Sí |
| FAVORITES | Sí |
| HISTORY | Sí |
| CUSTOM_STATIONS | Sí |
| HOME, SERVERS, PROFILE, LOGIN, REGISTER, SETUP, STATION_DETAIL, full-player | No (FR-009) |

## 4. Render por sección

| Sección | Modo LIST | Modo GRID |
|---|---|---|
| Explorar | `LazyColumn` + `StationListItem` (estado actual) | `LazyVerticalGrid` (2 col) + `StationCard` (ya existe) |
| Favoritos | list item actual (con reorden drag) | `LazyVerticalGrid` + `StationCard` (favorita = acción quitar) |
| Historial | list item actual (con quitar) | `LazyVerticalGrid` + `HistoryGridCard` (propia: play + quitar en la card, hora relativa incluida) |
| Mis emisoras | list item actual (con borrar) | `LazyVerticalGrid` + `CustomStationGridCard` (propia: play directo + borrar, URL como metadato) |

Nota de diseño (actualizado en convergence): en Historial y Mis emisoras la card de cuadrícula es propia (no `StationCard`) porque el corazón de `StationCard` significa "favoritos" y las acciones de estas secciones son quitar-del-historial / borrar emisora; se conservan las mismas descripciones de accesibilidad que la lista.

Invariante transversal (FR-007): al alternar, el estado de sección (búsqueda, filtros, items paginados, estado de error con reintento, estado vacío) permanece intacto; solo cambia el contenedor de presentación. En Favoritos, el reorden manual solo existe en modo LIST; en GRID se muestra el orden actual.

## 5. Códigos de ruta

- Rutas existentes en `Routes` (EXPLORE, FAVORITES, HISTORY, CUSTOM_STATIONS) — sin rutas nuevas.
