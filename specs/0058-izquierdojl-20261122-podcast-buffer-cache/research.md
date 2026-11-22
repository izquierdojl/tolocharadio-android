# Research: Buffer profundo y caché en disco para episodios

**Feature**: `0058-izquierdojl-20261122-podcast-buffer-cache`
**Date**: 2026-11-22
**Spec**: [spec.md](./spec.md)

## R1 — Buffer por tipo de contenido

**Decision**: mantiene un único `ExoPlayer` activo, pero con `LoadControl` distinto
según el contenido. `PlaybackEngine` gana `ensureKind(ContentKind)` que
intercambia el player (reutilizando `swap`) cuando cambia `LIVE` ↔ `VOD`. El
servicio escucha `engine.playerFlow` para actualizar `RecoveryAwarePlayer` y
`MediaSession.setPlayer`, y libera el player anterior después del cambio.

**Rationale**: `DefaultLoadControl` se fija al construir el player y no se puede
cambiar en caliente. La radio en directo no puede permitirse 4 minutos de
latencia; el VOD sí. El intercambio ya existe y está probado en el cambio de
perfil de buffer (spec 0042).

**Alternatives considered**:
- **Perfil global de 4 min**: rompe la radio (hasta 4 min por detrás del
  directo). Descartado.
- **Dos `ExoPlayer` vivos (LIVE y VOD)**: evita el intercambio, pero duplica
  renderers, hilos y memoria, y toca volumen/Cast/sesión en todos los caminos.
  Descartado por coste y riesgo.
- **Reflection sobre `DefaultLoadControl`**: no soportado. Descartado.

## R2 — Valores del buffer de episodios

**Decision**: `minBufferMs = 60_000`, `maxBufferMs = 240_000`,
`bufferForPlaybackMs = 2_000`, `bufferForPlaybackAfterRebufferMs = 2_500`.

**Rationale**: Pocket Casts usa 2 min / 4 min solo en streaming y deja el
arranque en el valor por defecto de Media3. El margen de 4 min cubre una zona
sin cobertura típica sin retrasar el play.

**Alternatives considered**:
- 10 min: a 192 kbps son ~14 MB, por encima del objetivo de audio de Media3
  (12,8 MB) y con riesgo de OOM. Descartado.
- 2 min: margen insuficiente para el caso reportado. Descartado.

## R3 — Memoria del buffer

**Decision**: no se toca `setTargetBufferBytes`. El objetivo por defecto de
Media3 para audio es `200 × 64 KB = 12,8 MB`; 4 min a 192 kbps ≈ 5,6 MB.

**Rationale**: cabe con margen y evita asignar memoria de más. El back buffer se
deja en 0 (no se necesita para VOD; solo para radio).

## R4 — Caché en disco

**Decision**: `SimpleCache` + `CacheDataSource` de Media3 en
`context.cacheDir/episode-cache`, con un `CacheEvictor` propio
(`QuotaCacheEvictor`) cuyo límite se lee de `EpisodeCachePolicy`. Escritura
activa para episodios en streaming; lectura con
`CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR`.

**Rationale**: el buffer en RAM se pierde al recrear el player o al morir el
proceso. La caché en disco permite volver atrás sin red y sobrevive al
reinicio. `FLAG_IGNORE_CACHE_ON_ERROR` evita que una entrada corrupta bloquee la
reproducción.

**Alternatives considered**:
- `DownloadManager` para todo: descarga completa siempre, sin control fino de
  cuota ni de "solo lo escuchado". Ya existe para descargas explícitas.
- Caché en `filesDir`: sobrevive a la limpieza del sistema, pero duplica la
  política de las descargas y puede crecer sin control si falla la app.

## R5 — Clave de caché

**Decision**: `MediaItem.setCustomCacheKey("episode-$id")` y `DataSpec` con la
misma clave. La URL firmada nunca es la identidad.

**Rationale**: las URLs de reproducción caducan y se reemiten; si la clave fuera
la URL, cada renovación invalidaría el audio ya descargado.

## R6 — Precarga

**Decision**: `EpisodePrefetchManager` con `CacheWriter` de Media3, corriendo en
el `@ApplicationScope` mientras el `MediaSessionService` está vivo. Sin
WorkManager.

**Rationale**: la precarga solo tiene sentido mientras se reproduce un episodio;
el servicio ya mantiene el proceso. Añadir WorkManager sería una dependencia
nueva sin necesidad (YAGNI).

**Reglas**:
- Red no medida: precargar el episodio completo hasta el límite de 500 MB.
- Red medida: precargar como máximo 5 min por delante del punto de reproducción
  y hasta 50 MB.
- Pausa, parada, cambio de episodio o pérdida de red: cancelar.
- Cambio de red: recalcular el objetivo sin cortar el audio.

## R7 — Detección de red medida

**Decision**: ampliar `ConnectivityMonitor` con `isMetered: StateFlow<Boolean>`.
Se considera medida si falta `NET_CAPABILITY_NOT_METERED`, si hay roaming
(`NET_CAPABILITY_NOT_ROAMING` en API 28+, `TelephonyManager.isNetworkRoaming` en
26–27) o si el Ahorro de datos del sistema está activo
(`ConnectivityManager.restrictBackgroundStatus == RESTRICT_BACKGROUND_STATUS_ENABLED`).

**Rationale**: la política 500/50 MB depende del tipo de red y debe reaccionar a
cambios en caliente. `AndroidConnectivityMonitor` ya recibe
`onCapabilitiesChanged`.

## R8 — Límites dinámicos

**Decision**: `QuotaCacheEvictor` implementa `CacheEvictor` con un
`limitProvider: () -> Long`; al cambiar `isMetered`, el límite baja a 50 MB y el
evictor recorta por LRU. `SimpleCache` se crea una sola vez con el máximo
(500 MB) para no recrearlo.

**Rationale**: el evictor de Media3 (`LeastRecentlyUsedCacheEvictor`) fija el
límite en el constructor. Uno propio permite adaptarlo sin cerrar la caché.

## R9 — Ajustes

**Decision**: sección «Caché de episodios» en Ajustes con espacio ocupado,
explicación de los límites 500/50 MB y acción «Liberar espacio».

**Rationale**: FR-012 y SC-005 exigían visibilidad y control. Se reutiliza el
patrón de la sección de buffer (plegable).

## R10 — Exclusiones

**Decision**: no se cachean episodios descargados (`content://`), ni la
reproducción por Cast.

**Rationale**: el archivo local ya es offline; el receptor gestiona su propio
buffer y no puede leer la caché del móvil.
