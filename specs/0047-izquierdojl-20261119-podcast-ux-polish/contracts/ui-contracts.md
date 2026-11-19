# UI Contracts: Mejoras de experiencia podcast

**Feature**: `0047-izquierdojl-20261119-podcast-ux-polish` | **Date**: 2026-11-19

Contratos internos de presentación. No hay API externa nueva; estos contratos fijan qué debe poder observar y hacer la persona usuaria, y qué semántica deben exponer los tests.

---

## 1. `TolochaTopBar`

### Entradas

| Entrada | Tipo | Obligatoria | Descripción |
|---------|------|-------------|-------------|
| `mode` | `ContentMode` | Sí | Modo actual; el selector muestra el destino |
| `podcastsEnabled` | `Boolean` | Sí | Si es `false`, no se muestra el selector |
| `onSelectMode` | `(ContentMode) -> Unit` | Sí | Cambia de modo sin detener la reproducción |
| `onOpenMenu` | `() -> Unit` | Sí | Abre el cajón |
| `castAction` | `@Composable () -> Unit` | Sí | Botón de Cast existente (`MediaRouteButton`) |

### Composición

- `navigationIcon`: hamburguesa, `contentDescription = "Abrir menú"`.
- `title`: `TolochaLogo(showText = disponible)`. Si no cabe el texto, solo emblema con `contentDescription = "Emblema de TolochaRadio"`.
- `actions`: selector Radio | Podcasts (si procede) y Cast.
- No hay temporizador, servidores ni configuración como acciones directas.

### Invariantes

- En un ancho de 320 dp y letra al 200 %, ningún elemento se solapa ni se recorta.
- La hamburguesa y el Cast siempre son visibles y accionables.
- El selector nunca aparece si el servidor no ofrece podcasts.

### Semántica para tests

`Abrir menú`, `Cambiar a Podcasts`, `Cambiar a Radio`, `Emblema de TolochaRadio`.

---

## 2. `TolochaDrawer`

### Entradas

| Entrada | Tipo | Descripción |
|---------|------|-------------|
| `uiState` | `SleepTimerUiState` | Estado del temporizador (activo/inactivo) |
| `onStartTimer` | `(SleepTimerDuration) -> Unit` | Inicia el temporizador |
| `onCancelTimer` | `() -> Unit` | Cancela el temporizador |
| `onServers` | `() -> Unit` | Navega a Servidores |
| `onSettings` | `() -> Unit` | Navega a Configuración |

### Entradas visibles

| Etiqueta | Estado | Acción |
|----------|--------|--------|
| Temporizador | “Activo · N min” si corre; “Apagado” si no | Abre duraciones o cancelar |
| Servidores | — | Navega |
| Configuración | — | Navega |

### Invariantes

- El cajón se cierra antes de navegar.
- Atrás con el cajón abierto lo cierra; no navega.
- La reproducción y el mini-player no se detienen ni se reinician.
- Con el temporizador activo se ven los minutos restantes y la opción de cancelar.

### Semántica para tests

`Abrir menú`, `Temporizador`, `Servidores`, `Configuración`, `Temporizador activo`, `Cancelar temporizador`.

---

## 3. Barra inferior con insignia

### Entradas

| Entrada | Tipo | Descripción |
|---------|------|-------------|
| `dests` | `List<BottomDest>` | Destinos del modo actual |
| `currentRoute` | `String?` | Ruta activa |
| `onNavigate` | `(String) -> Unit` | Navegación |
| `queueBadge` | `QueueBadge?` | `null` o `pendingCount = 0` → sin insignia |

### Composición

- Radio: Explorar, Favoritos, Historial, Mis emisoras.
- Podcast: Inicio, Descubrir, Suscripciones, Cola, Favoritos.
- Configuración no aparece en ninguna de las dos.
- La insignia se coloca sobre el icono de Cola; no tapa la etiqueta.

### Invariantes

- `pendingCount` coincide con la lista de pendientes de la pantalla de cola.
- 0 → sin insignia; > 99 → “99+”.
- Descripción accesible: “N episodios en cola”.
- Todos los destinos conservan el tamaño táctil y reparten el ancho (regresión 0040).

### Semántica para tests

Etiquetas de destino, `Cola`, `N episodios en cola`.

---

## 4. Tarjetas de programa y episodio

### `PodcastCard`

| Zona | Contenido | Regla |
|------|-----------|-------|
| Portada | Imagen 1:1 | Recorte uniforme |
| Texto | Título (2 líneas) + autor/episodios (1 línea) | Título a todo el ancho |
| Acciones | Baja de suscripción, si aplica | Fila propia debajo del texto |

### `EpisodeCard`

| Zona | Contenido | Regla |
|------|-----------|-------|
| Portada | Imagen 1:1 | Sin botones superpuestos |
| Texto | Título (2 líneas) + podcast/duración/estado (1 línea) | Podcast legible |
| Acciones | Reproducir/pausar + favorito | Fila propia debajo del texto |

### Invariantes

- `minLines = 2` en el título; el texto solo se recorta después de dos líneas.
- Las tarjetas de una misma fila tienen la misma altura.
- La altura crece con el tamaño de letra; nunca se solapa con las acciones.
- Acciones con descripción accesible y área táctil ≥ 48 dp.
- Mismo criterio visual en Inicio, Suscripciones y Favoritos.

### Semántica para tests

Título visible, `Reproducir`/`Pausar`, `Añadir a favoritos`/`Quitar de favoritos`, `Cancelar suscripción`.

---

## 5. `DownloadProgressButton`

### Entradas

| Entrada | Tipo | Descripción |
|---------|------|-------------|
| `state` | `DownloadUiState` | `NotDownloaded`, `Downloading(fraction?)`, `Downloaded`, `Failure` |
| `onDownload` | `() -> Unit` | Inicia o reintenta |
| `contentDescription` | `String` | “Descargar”, “Descargando N %”, “Descargado” |

### Estados y presentación

| Estado | Icono | Texto/Anillo | Acción |
|--------|-------|--------------|--------|
| `NotDownloaded` | Descarga | — | Habilitada |
| `Downloading(fraction)` | — | Anillo determinado + “N %” | Deshabilitada |
| `Downloading(null)` | — | Anillo indeterminado | Deshabilitada |
| `Downloaded` | Descargado | — | Ninguna |
| `Failure` | Descarga | — | Habilitada (reintento) + aviso |

### Invariantes

- El porcentaje está entre 0 y 100.
- No se inicia una segunda descarga del mismo episodio mientras corre.
- El estado se conserva al cambiar de pantalla, girar o volver del segundo plano.
- Si el servidor no permite descargas, el botón no existe.
- La ficha (`EpisodeSheet`) usa el mismo estado y muestra “Descargando N %”.

### Semántica para tests

`Descargar`, `Descargando 45 %`, `Descargado`, `Reintentar descarga`.

---

## 6. `VoiceBoostToggle`

### Entradas

| Entrada | Tipo | Descripción |
|---------|------|-------------|
| `enabled` | `Boolean` | Preferencia actual |
| `onToggle` | `(Boolean) -> Unit` | Cambia y persiste |

### Presentación

- Control seleccionable con etiqueta “Potenciador de voz”.
- Estado visible (seleccionado/no seleccionado) y `stateDescription` (“Activado”/“Desactivado”).
- Solo aparece en el reproductor de episodios; nunca en la UI de radio.
- El cambio se aplica sin pausar ni reiniciar.

### Semántica para tests

`Potenciador de voz`, `Activado`, `Desactivado`.
