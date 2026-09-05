# Contracts (propio): UI de Favoritos

Pantalla `feature/favorites/FavoritesScreen.kt` + `FavoritesViewModel.kt`, destino `Routes.FAVORITES` (barra inferior "Favoritos", paridad web `/favoritos`).

## Estados (`FavoritesUiState`)

`Loading | Empty(CTA→EXPLORE) | Content(items, offline, savingOrder, pendingUndo) | Error(message, offlineItems?)`. Cada tarjeta expone: imagen (Coil + placeholder emblema), título, línea `país · idioma`, ≤3 chips + bitrate, fecha relativa, `FavoriteButton` (relleno = favorita), asa de arrastre, acciones play y abrir ficha (`Routes.stationDetail(id)`).

## Acciones del ViewModel

| Acción | Efecto observable |
|--------|-------------------|
| `refresh()` | recarga `GET`; conserva lista visible durante el refresco |
| `toggleFavorite(stationId)` | cambio optimista global (vía `favoriteIds` compartido) + reversión ante error |
| `removeWithUndo(stationId)` | quita optimista + `Snackbar` "Deshacer" 10 s; expira → confirma |
| `undoRemove()` | re-`POST` + reinserción en índice previo |
| `moveItem(from, to)` + `commitOrder()` | reorden local + `PUT` al soltar; `savingOrder=true` no bloqueante; error → reversión + reintento |
| `retry()` | reintento manual desde `Error` |

## Navegación

- Sin sesión → `LOGIN` (guard `AUTH_REQUIRED` ya existente en `TolochaNavGraph`). Con sesión caducada → refresh ×1, si falla → `LOGIN` con aviso.
- Salidas: tarjeta → ficha; play → mini-player persistente (sin cambios en player); `Empty` → `EXPLORE`.

## Accesibilidad/localización (mínimo exigible)

Textos en español; `contentDescription` en corazón ("Añadir a favoritas"/"Quitar de favoritas"), asa ("Reordenar"), play; contraste WCAG AA según spec 002 (tokens ya existentes).
