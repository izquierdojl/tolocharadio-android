# Implementation Plan: Modo Podcast — paridad con web

**Branch**: `0046-izquierdojl-20261118-modo-podcast-completo` | **Date**: 2026-11-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0046-izquierdojl-20261118-modo-podcast-completo/spec.md`

## Summary

Llevar a Android la gestión completa de podcast del servicio (paridad `docs/uso.md`+`docs/api.md`): selector Radio|Podcasts con sección inicial configurable, barra inferior propia (Inicio, Descubrir, Suscripciones, Cola, Favoritos; clarify 2026-11-18), suscripciones/búsqueda/OPML, ficha con filtros, reproducción VOD con progreso y reanudación, cola sincronizada por revisión, favoritos independientes, descargas internas en Descargas con reproducción local (estilo Pocket Casts) y estadísticas privadas. Enfoque (ver `research.md`): extender `/config` con capacidades, una `PodcastsApi` espejo del backend, URLs firmadas con DataSource plano, progreso `{sessionId,sequence,positionSec}` + keepalive, descargas con DownloadManager + tabla Room de mapeo, y navegación por `ContentMode` reutilizando shell/player/stats existentes.

## Technical Context

**Language/Version**: Kotlin (JVM target 17; CI Temurin 17), Jetpack Compose + Material 3

**Primary Dependencies**: Media3 1.11.0 (ExoPlayer + HLS + Session + OkHttp DataSource), Retrofit 2.11.0 + OkHttp 4.12.0 + kotlinx.serialization 1.7.3, Room 2.7.2 + DataStore Preferences, Hilt 2.60.1 (+ navigation-compose), Coil 2.6.0, DownloadManager/SAF/MediaStore (plataforma, sin dependencia nueva)

**Storage**: Room (nueva `episode_downloads` + cachés de lectura existentes), DataStore (`content_mode`, sección inicial Radio|Podcasts, `startScreen` vigente), EncryptedSharedPreferences (credenciales existentes, sin cambios), Descargas del sistema vía DownloadManager

**Testing**: JUnit + kotlinx-coroutines-test + Turbine 1.1.0 + MockK 1.13.12; serialización de DTOs contra ejemplos del contrato; Compose Test solo flujos críticos (modo, play/pausa episodio, cola básica, ficha); Detekt + ktlint + Android Lint en CI

**Target Platform**: Android `minSdk 26`, `targetSdk 37` (compileSdk 37)

**Project Type**: mobile-app (módulo único `app`, organización por feature espejo web)

**Performance Goals**: Portada completa < 3 s y búsquedas con respuesta < 2 s con red normal (SC-005/SC-006); reanudación con desviación ≤ 10 s en 9/10 casos (SC-003); paginación `hasMore`, jamás carga completa del catálogo

**Constraints**: HTTPS-only; sin pantallas de login/registro (sesión por servidor con Bearer + refresh silencioso); credenciales nunca en URL/logs (las URLs firmadas HMAC de stream/descarga son del servidor, con contextos distintos); cola ≤ `podcastQueueMax` (200); conflicto de revisión → recargar, no autofusionar; todo error visible en español y accionable

**Scale/Scope**: 7 destinos podcast + hoja de ficha + controles VOD en player; suscripciones típicas ≤ 50 programas; cola ≤ 200 episodios; sin biblioteca offline avanzada (fuera de alcance)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. MVVM + Clean por capas**: PASS — UI (Composables + `StateFlow`/`UiState`, ViewModels sin `Context` de Activity) → `domain/podcasts` (casos de uso puros: resolver fuente local/firmada, colas con `mergeQueueOrder`, descarte, OPML) → `data` (repos con `Flow`, `RemoteDataSource` Retrofit + `LocalDataSource` Room/DataStore). Hilt para ViewModels/repos/clientes. Sin módulo nuevo (feature espejo en `app`, como manda la constitución).
- **II. Stack Kotlin-First, Compose M3 y Media3**: PASS — UI nueva solo Compose M3; reproducción en `ExoPlayer + MediaSessionService` (ruta VOD separada de radio, `DataSource` plano para URLs firmadas + `Bearer` solo en API JSON); red Retrofit/OkHttp/serialización contra `/api/v1`; Coil para portadas; corrutinas + `Flow`; null-safe.
- **III. Calidad Test-First**: PASS — tests obligatorios (Red-Green) para repos/casos de uso/ViewModels de podcast, serialización de DTOs contra el contrato, funciones puras (cola, OPML, estados de progreso); Compose UI solo en críticos (modo, play/pausa, cola, ficha); gates `assembleDebug`, `testDebugUnitTest`, `detekt ktlintCheck lintDebug`.
- **IV. Streaming robusto**: PASS — `UiState` sellado por pantalla, errores de dominio tipados (401/403 renueva + re-login silencioso → editar servidor; 409 recarga cola; 404 idempotente en bajas; 422 por campo; 503 caché si hay), mensajes ES sin PII, reintento manual en VOD (sin backoff infinito de directo), `try/catch` genéricos prohibidos, KDoc en `domain/data`, logs estructurados.
- **V. Simplicidad (YAGNI)**: PASS — sin multi-módulo, sin BaaS, sin dependencias nuevas (DownloadManager/SAF son plataforma); la única persistencia nueva es la tabla `episode_downloads`, justificada en research D5 como puente mínimo episodio→fichero.

*Re-check post-Phase 1*: PASS — el diseño (`data-model.md`, `contracts/`) mantiene módulo único, reutiliza shell/player/bloques de stats y `safeCall`/`DomainError`; no introduce dependencias ni pantallas de acceso; la tabla Room y las preferencias DataStore son la persistencia mínima acordada.

## Project Structure

### Documentation (this feature)

```text
specs/0046-izquierdojl-20261118-modo-podcast-completo/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── podcasts-api.md
│   └── ui-navigation.md
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── core/ui/navigation/        # Routes + TolochaNavGraph (destinos podcast, shell por modo)
├── core/ui/components/        # EpisodeRow/EpisodeSheet/hoja ficha (reutiliza StationCard/EmptyState)
├── data/remote/api/           # PodcastsApi (+ SystemApi con features)
├── data/remote/dto/           # PodcastDtos (programa, episodio, progreso, cola, home, stats, tokens)
├── data/repo/                 # PodcastSubscriptionsRepo, EpisodesRepo, QueueRepo, EpisodeFavoritesRepo,
│                              #   ProgressRepo, DownloadsRepo (+ SystemRepo capabilities)
├── data/local/                # episode_downloads (Room) + InstancePrefs (content_mode, sección inicial)
├── domain/podcasts/           # ResolveEpisodePlayback, QueueOrder (merge/apply/next), DismissEpisode,
│                              #   ParseOpmlFeeds, ToggleEpisodeFavorite, ReportProgress use cases
├── feature/podcasts/          # home, discover, subscriptions, show, queue, favorites, stats, sheet
├── feature/player/            # controles VOD (velocidad, ±15/30, búsqueda, origen local/online)
├── feature/settings/          # sección inicial Radio|Podcasts
└── di/                        # providers PodcastsApi/repos (NetworkModule/StorageModule existentes)

app/src/test/java/.../        # espejo: dto serialización, repos, domain puros, viewmodels (Turbine+MockK)
```

**Structure Decision**: Módulo único `app` (opción por defecto de la constitución) con paquetes `feature/podcasts/*` espejo de la web y `domain/podcasts` + `data` por capas; sin multi-módulo hasta que un feature tenga API estable y ciclo independiente (no es el caso).

## Complexity Tracking

> Fill ONLY if Constitution Check has violations that must be justified

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — (ninguna) | — | — |
