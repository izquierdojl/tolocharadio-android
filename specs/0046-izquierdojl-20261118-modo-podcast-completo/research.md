# Research: Modo Podcast — paridad con web

**Feature**: `specs/0046-izquierdojl-20261118-modo-podcast-completo/spec.md`
**Date**: 2026-11-18
**Sources**: `docs/uso.md`, `docs/api.md` y código cliente del servicio (`apps/web/src/lib/api.ts`, `types.ts`, `podcastQueue.ts`, `podcastDownloads.ts`); código Android existente (`data/remote`, `data/repo`, `domain/playback`, `feature/player`, `di/*`, `core/network`).

Todos los NEEDS CLARIFICATION del contexto técnico quedan resueltos abajo. Sin bloqueos para Phase 1.

## D1 — Detección de capacidades vía `GET /config`

- **Decision**: Extender `AppConfigDto` con `features` (`podcasts`, `podcastDiscovery`, `podcastOpml`, `podcastQueue`, `podcastFavorites`, `podcastStats`, `podcastDownloads`) y `limits` (`podcastQueueMax`, `podcastSubscriptionsMax`, `podcastEpisodesPageSize`, `podcastOpmlMaxFeeds`, …), todos opcionales con valores que conservan el comportamiento actual (podcasts desactivado → solo Radio) cuando el servidor no los envía.
- **Rationale**: El backend ya publica `PublicConfig.features/limits` (verificado en `types.ts`); una sola llamada pública existente gobierna la visibilidad de todo el modo sin nuevos permisos ni pantallas.
- **Alternatives considered**: Comprobación por versión de API (frágil ante despliegues propios); endpoint separado de capacidades (duplica lo que `/config` ya ofrece).

## D2 — Superficie API: una interfaz `PodcastsApi` + DTOs espejo del backend

- **Decision**: Una interfaz Retrofit `PodcastsApi` con los endpoints verificados en `api.ts` (search, resolve, subscriptions CRUD + bulk + OPML texto, home, queue CRUD + order + count, favorites, episodes paginados + búsqueda global `episodes/search`, playback/download firmados, progress PUT + keepalive POST, dismiss, stats summary/top-podcasts/top-episodes/timeline/habits/categories/recent) y DTOs `kotlinx.serialization` espejo de `PodcastSummary`, `PodcastEpisode(+progress)`, páginas `{items,pagination{offset,limit,hasMore}}`, `PodcastQueue{revision,items}`, `PodcastHome`, `BulkSubscriptionResult`, `IssuedToken{url,expiresAt}`. Autenticación con el interceptor Bearer existente; OPML como texto plano (`ResponseBody`/String converter). Repositorios por agregado que reutilizan `safeCall`/`ApiResult`/`DomainError` (el `409` de cola ya mapea a `Conflict`).
- **Rationale**: Sigue el patrón probado de `StationsApi/FavoritesApi/StatsApi` + `*Repo` (verdad en servidor, caché de lectura, flujos compartidos); un solo fichero de interfaz agrupado por comentarios evita la dispersión.
- **Alternatives considered**: Una API por agregado (4 interfaces) — rechazado por YAGNI; la agrupación por comentarios ya funciona en `StatsApi`.

## D3 — Reproducción de episodios con URL firmada (no proxy Bearer)

- **Decision**: `POST /podcasts/episodes/:id/playback` → `{url, expiresAt}` (TTL 6 h) y reproducción en Media3 con `DataSource` OkHttp **plano** (sin `Authorization`; el HMAC va en la URL por diseño del servidor y no son credenciales de usuario). Reemisión anticipada cuando el vencimiento se acerca durante episodios largos. Ruta VOD separada de las rutas de radio `CONTINUOUS/LEGACY` (sin bucles de reconexión live; duración finita → el progreso % es real).
- **Rationale**: Es el contrato del backend (contextos HMAC distintos para stream y descarga); reutilizar el proxy Bearer de radio rompería el contrato y mezclaría credenciales donde el servidor exige firma.
- **Alternatives considered**: Proxy Bearer como radio — rechazado (contrato incompatible); descargar siempre antes de reproducir — rechazado (streaming inmediato + ahorro solo cuando hay descarga, según spec).

## D4 — Progreso: `{sessionId, sequence, positionSec}` + keepalive

- **Decision**: Al iniciar una escucha se genera `sessionId` (UUID v4) y `sequence` monótona; `PUT progress` periódica (~30 s, alineado con la semántica de "empezados ≥ 30 s") más en pausa/búsqueda/cierre; `POST progress` como keepalive al pasar a segundo plano/cerrar (equivalente al `sendBeacon` web). Fin natural → completado + auto-avance (`nextQueueEpisode`); descarte vía `POST dismiss` (no marca completado).
- **Rationale**: Idempotencia y orden del servidor (`sequence`) toleran reintentos y red intermitente; el keepalive evita perder la última posición al matar la app.
- **Alternatives considered**: Reporte cada segundo — rechazado (batería y carga del servidor); solo al pausar — rechazado (pierde posición ante cierres bruscos).

## D5 — Descargas internas vía DownloadManager + tabla Room de mapeo

- **Decision**: `POST …/download` → URL firmada → transferencia con el **DownloadManager del sistema** (sin cabecera de auth; la firma basta) hacia Descargas, conservando el nombre `tolocharadio_*` del servidor. Tabla Room `episode_downloads(episodeId, downloadManagerId, status, localUri, updatedAt)` para localizar el archivo y preferirlo en reproducción cuando está completo (SC-010). Archivo perdido/incompleto/sin espacio → aviso + reintentar o reproducir online (FR-025).
- **Rationale**: DownloadManager es válido en todo `minSdk 26`, da notificación/progreso/reintentos del sistema y deja el archivo visible en Descargas (petición explícita del usuario); la tabla de mapeo es el puente mínimo para la reproducción local.
- **Alternatives considered**: Descarga manual OkHttp + MediaStore — rechazado (ramas de scoped-storage, notificaciones propias, reinventar reintentos); `getExternalFilesDir` — rechazado (oculto al usuario); solo DownloadManager sin tabla — rechazado (imposible resolver episodio→fichero de forma fiable).

## D6 — Cola con revisión: `baseRevision` + recarga ante 409

- **Decision**: Guardar `revision` del último `GET`; `PUT order {baseRevision, episodeIds}` con la lista completa (incluido el hueco del episodio oculto en curso, lógica `mergeQueueOrder` portada a dominio puro y testada). `409` → `DomainError.Conflict` → recargar cola, avisar y pedir reintentar (sin autofusión: evita pérdida de cambios del otro dispositivo, igual que la web).
- **Rationale**: El servidor ya serializa con revisión; el cliente solo necesita disciplina de lectura-modificación-escritura y el mapeo de error existente.
- **Alternatives considered**: Autofusión local ante 409 — rechazado (riesgo de reordenamientos perdidos entre dispositivos).

## D7 — OPML con selector del sistema + parseo cliente

- **Decision**: Importar con SAF (`GetContent` text/xml/.opml) → parsear `xmlUrl` en cliente (puerto de la regex web `parseOpmlFeedUrls`, función pura + tests) → `POST bulk {feedUrls}` → informar importados/omitidos/fallidos. Exportar: `GET opml` (texto) → fichero temporal + hoja de compartir del sistema (incluye guardar en Descargas).
- **Rationale**: Cumple la clarificación "Sistema Android"; el parseo cliente evita subir ficheros al servidor (el bulk espera URLs) y es unit-testeable sin Android.
- **Alternatives considered**: Subir el fichero tal cual — rechazado (contrato bulk es `{feedUrls}`); pegado manual — rechazado en clarify.

## D8 — Navegación por modo: `ContentMode` en DataStore + barras separadas

- **Decision**: Nueva preferencia `content_mode` (RADIO/PODCAST) + preferencia de sección inicial Radio|Podcasts (clarify Q1/Q2 de spec: barra podcast = Inicio, Descubrir, Suscripciones, Cola, Favoritos; estadísticas desde Inicio). Arranque: puerta de sesión existente → modo → pantalla. `ViewModeToggle` y mini-player/full-player se reutilizan; la ficha de episodio es hoja inferior modal; el reproductor muestra controles de episodio (velocidad 1/1.25/1.5/2, ±15/30 s, búsqueda) solo en VOD. Todo el modo se condiciona a `features.podcasts` cacheado de `/config`.
- **Rationale**: Mínima invasión del grafo actual (el shell ya conmuta destinos); DataStore ya guarda preferencias análogas (`startScreen`, `viewMode`).
- **Alternatives considered**: Duplicar NavHost por modo — rechazado (duplica mantenimiento); `StartScreen` ampliado sin modo — rechazado (no expresa "modo" global que la spec exige).

## D9 — Estadísticas: adaptar bloques existentes

- **Decision**: Nuevas llamadas espejo (`summary`, `top-podcasts`, `top-episodes`, `timeline`, `habits`, `categories`, `recent`) mapeadas al patrón `StatsBundle/LoadStatsUseCase`; reutilizar bloques de gráficas (timeline, hábitos, recientes, tarjetas) con adaptadores de datos de podcast.
- **Rationale**: La pantalla de stats de radio ya resolvió gráfica + estados + tests; solo cambia la fuente y las etiquetas (programas/episodios en vez de emisoras/géneros).
- **Alternatives considered**: Pantalla de stats propia desde cero — rechazado (duplicación).

## D10 — Comportamiento VOD del reproductor (sin reconexión live)

- **Decision**: Los episodios usan perfil VOD: ante corte, mensaje + reintento manual (sin backoff infinito de directo); velocidades y saltos según spec; al terminar: completar + auto-avance con la cola vigente; emisión a otros dispositivos y temporizador se reutilizan tal cual (FR-028).
- **Rationale**: Un fichero finito no necesita reconexión de directo; el auto-avance y el completado son reglas de dominio testables sin el player.
- **Alternatives considered**: Reutilizar la política live tal cual — rechazado (reintentos infinitos sobre un fichero).
