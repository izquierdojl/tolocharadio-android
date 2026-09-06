# Research: Reproductor flotante inferior (004-floating-player)

**Fecha**: 2026-09-05 | **Spec**: [spec.md](spec.md)

Se investigó el código real (`feature/player/`, `core/ui/navigation/`, `core/ui/components/`, `di/PlayerModule.kt`) para resolver los 5 unknowns del Technical Context. No quedan `NEEDS CLARIFICATION`.

## R1. Silencio sin detener la emisión

- **Decision**: `isMuted: StateFlow<Boolean>` en el `PlayerViewModel` compartido; silenciar fija `exoPlayer.volume = 0f`, quitarlo restaura `1f`. El stream sigue corriendo por debajo (sin `stop`/`pause`), por eso el sonido vuelve al instante. `play(nuevaEmisora)` y `stop()` resetean `isMuted = false` y volumen `1f` (acuerdo de clarify: el silencio siempre se resetea).
- **Rationale**: mute por volumen local es el patrón estándar de ExoPlayer; no toca el `MediaSessionService` ni la notificación (que sigue mostrando play/pausa reales) y no requiere permisos ni persistencia.
- **Alternatives considered**: pausar+reanudar en lugar de mute (rechazado: la spec exige emisión en curso y retorno instantáneo sin reconectar); persistir mute en DataStore (rechazado: el usuario acordó reset siempre; YAGNI).

## R2. Copiar enlace al portapapeles + confirmación

- **Decision**: operación de portapapeles en la capa UI con `LocalClipboardManager` (Compose foundation, sin dependencia nueva) + `SnackbarHostState` del `Scaffold` para "Enlace copiado" / "enlace no disponible". El ViewModel solo expone datos; helper puro `resolveCopyLink(station): String?` (`station.url` en blanco → `null`) con tests unitarios. Se copia **solo** `station.url`; jamás la URL del proxy (`streamUrl`) para no filtrar nada ligado a la sesión.
- **Rationale**: el ViewModel tiene prohibido el `Context` de Activity (constitución I); el portapapeles es API de framework y vive en el composable, igual que Coil vive en UI. El helper puro mantiene la regla test-first.
- **Alternatives considered**: copiar desde el ViewModel con `ApplicationContext` (rechazado: acopla lógica de player a framework Android y complica tests); copiar la URL del proxy (rechazado: solo funciona con sesión y expone superficie de sesión en el portapapeles).

## R3. Un solo estado de reproducción en toda la app

- **Decision**: un único `PlayerViewModel` compartido a ámbito de Activity (`hiltViewModel(activity)`) usado por el panel y por todas las pantallas que inician reproducción (Favoritos, Explorar, ficha, Historial cuando exista). Hallazgo que lo exige: hoy cada destino crea su propio VM (`hiltViewModel()` por defecto) y solo comparten el `ExoPlayer` singleton; un VM recién creado arranca en `Idle` y la identidad de la emisora vive solo en el `StateFlow` del VM anterior, así que el panel "global" (FR-009) es imposible sin compartir el VM.
- **Rationale**: cambio mínimo dentro del módulo único, sin reestructurar navegación ni añadir dependencias; además conserva el estado al navegar (los `NavHost` con `saveState/restoreState` ya no fragmentan el player) y a rotación (ya la tenía).
- **Alternatives considered**: mantener un VM por pantalla e inicializarlo desde `exoPlayer` (rechazado: la emisora actual no se puede deducir del player — el `MediaItem` lleva la URL del proxy, no el `StationDto`); anidar un grafo de navegación para scoping (rechazado: no existe grafo anidado y reestructurar el nav está fuera de alcance, YAGNI).

## R4. Cancelar la carga desde el panel

- **Decision**: guardar el `Job` de carga en `play()` (`loadJob`) y exponer `cancelLoad()`: cancela el job, `exoPlayer.stop()` + `clearMediaItems()`, estado `Idle` (oculta el panel). El botón principal durante `Buffering` invoca `cancelLoad()`.
- **Rationale**: hoy `play()` lanza `viewModelScope.launch` sin referencia guardada (solo se guarda el `retryJob`); sin `loadJob` la cancelación es imposible. Reutiliza primitivas existentes, sin backoff nuevo.
- **Alternatives considered**: botón deshabilitado durante la carga (rechazado: el usuario eligió cancelable en clarify); cancelar solo el job sin `stop()` (rechazado: dejaría al player preparando el stream en segundo plano).

## R5. Avatar + línea técnica con patrones existentes

- **Decision**: reutilizar `StationArtwork(station, 48.dp)` de `core/ui/components` (Coil + fallback al emblema Sierra, mismo que tarjetas) y formatear la 2ª línea con función pura `panelSubtitle(station)` (`"{país} · {idioma} · {codec} {bitrate} kbps"`, omitiendo partes ausentes; fallback `"Emisora de radio"`), con tests unitarios. Colocación: `Column(MiniPanel)` encima del `NavigationBar` dentro del slot `bottomBar` del `Scaffold` (hoy el panel va **debajo** de la barra; hay que invertir el orden). Accesibilidad: `contentDescription` decorativo en imagen + descripciones en los 3 botones + anuncio "Sonando: {nombre}".
- **Rationale**: cero código duplicado de imagen (paridad con tarjetas, spec 002/003), formato testeable sin Compose, y el `bottomBar` acepta cualquier `Column` — el cambio de orden es de una línea.
- **Alternatives considered**: nuevo componente de imagen propio del panel (rechazado: duplica el fallback del emblema); subtítulo hardcodeado en el composable (rechazado: no unit-testeable).
