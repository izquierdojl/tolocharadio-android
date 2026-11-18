# Tasks: Modo Podcast — paridad con web

**Input**: Design documents from `/specs/0046-izquierdojl-20261118-modo-podcast-completo/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md
**Tests**: SÍ incluidos (Red-Green obligatorios por constitución III para `domain`/`data`/ViewModels + serialización de DTOs; Compose solo flujos críticos). Escribir el test PRIMERO y verlo FALLAR antes de implementar.

**Organization**: Una fase por historia de usuario (US1–US7 de spec.md), en orden de prioridad P1 → P2 → P3.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: paralelizable (ficheros distintos, sin dependencias)
- **[Story]**: historia de spec.md que implementa
- Citas de campos con restricción → `data-model.md Entidad.campo` en la tarea que los implementa

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Línea base de gates y esqueleto de paquetes podcast

- [X] T001 Verificar línea base de gates en `app/build.gradle.kts` (`assembleDebug`, `testDebugUnitTest`, `detekt ktlintCheck lintDebug` en verde antes de empezar)
- [X] T002 [P] Crear esqueleto de paquetes `feature/podcasts/{home,discover,subscriptions,show,queue,favorites,stats,sheet}`, `domain/podcasts`, `data/local/podcasts` en `app/src/main/java/com/izquierdojl/tolocharadio/`
- [X] T003 [P] Crear espejo de paquetes de tests podcast en `app/src/test/java/com/izquierdojl/tolocharadio/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Capacidades, API/DTOs, funciones puras de dominio, modo/navegación, VOD base y descargas — bloquea TODAS las historias

**⚠️ CRITICAL**: Ninguna historia puede empezar hasta completar esta fase

- [X] T004 Extender `AppConfigDto` en `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/dto/AuthDtos.kt` con `features` y `limits` opcionales (constraints: `data-model.md PodcastCapabilities.podcasts`, `PodcastCapabilities.limits`)
- [X] T005 [P] Test Red-Green de serialización de capacidades con/sin `features` en `app/src/test/java/com/izquierdojl/tolocharadio/data/remote/dto/PodcastCapabilitiesDtoTest.kt`
- [X] T006 Exponer capacidades cacheadas en `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/SystemRepo.kt` (puerta de todo el modo, research D1)
- [X] T007 [P] Crear `PodcastsApi` en `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/api/PodcastsApi.kt` (endpoints de `contracts/podcasts-api.md`: search, resolve, subscriptions, bulk, opml, home, queue, favorites, episodes, playback, download, progress, dismiss, stats)
- [X] T008 [P] Crear DTOs en `app/src/main/java/com/izquierdojl/tolocharadio/data/remote/dto/PodcastDtos.kt` (constraints: `data-model.md PodcastSummary.*`, `PodcastEpisode.*`, `EpisodeProgress.*`, `PodcastQueue.revision`, `IssuedToken.expiresAt`, `BulkSubscriptionResult.*`)
- [X] T009 [P] Test Red-Green de serialización de DTOs contra ejemplos del contrato en `app/src/test/java/com/izquierdojl/tolocharadio/data/remote/dto/PodcastDtosTest.kt`
- [X] T010 [P] Registrar `PodcastsApi` en `app/src/main/java/com/izquierdojl/tolocharadio/di/NetworkModule.kt`
- [X] T011 [P] Funciones puras de cola en `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/QueueOrder.kt` (merge/apply/next, puerto de `podcastQueue.ts`; constraints: `data-model.md PodcastQueue.revision`, `PodcastQueue.items`)
- [X] T012 [P] Test Red-Green de `QueueOrder` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/QueueOrderTest.kt`
- [X] T013 [P] Parseo OPML puro en `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/ParseOpmlFeeds.kt` (puerto de `parseOpmlFeedUrls`)
- [X] T014 [P] Test Red-Green de `ParseOpmlFeeds` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/ParseOpmlFeedsTest.kt`
- [X] T015 [P] Estado derivado de episodio en `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodeState.kt` (SinEmpezar/EnProgreso/Completado; constraints: `data-model.md PodcastEpisode.progress`)
- [X] T016 [P] Test Red-Green de `EpisodeState` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodeStateTest.kt`
- [X] T017 Añadir `ContentMode` (RADIO/PODCAST) y sección inicial a `app/src/main/java/com/izquierdojl/tolocharadio/data/local/InstancePrefs.kt` (research D8)
- [X] T018 Test Red-Green de prefs de modo en `app/src/test/java/com/izquierdojl/tolocharadio/data/local/ContentModePrefsTest.kt`
- [X] T019 Añadir destinos podcast a `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/Routes.kt` (`contracts/ui-navigation.md`)
- [X] T020 Conmutación de barra inferior por modo en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` (puerta `features.podcasts`; ruta podcast con podcasts deshabilitados o destino fuera de modo → redirige a la pantalla Radio vigente sin errores; reproducción intacta al conmutar)
- [X] T021 Entidad Room de descargas en `app/src/main/java/com/izquierdojl/tolocharadio/data/local/podcasts/EpisodeDownloadEntity.kt` + DAO (constraints: `data-model.md DownloadRecord.*`)
- [X] T022 Registrar DAO en `app/src/main/java/com/izquierdojl/tolocharadio/data/local/TolochaDb.kt` con migración probada
- [X] T023 [P] Test Red-Green del DAO de descargas en `app/src/test/java/com/izquierdojl/tolocharadio/data/local/EpisodeDownloadDaoTest.kt`
- [X] T024 Ruta VOD separada de radio en `app/src/main/java/com/izquierdojl/tolocharadio/domain/playback/PlaybackRoute.kt` (episodios finitos, sin backoff live; research D10)

**Checkpoint**: Fundación lista — las historias pueden implementarse en paralelo

---

## Phase 3: User Story 1 - Cambiar entre Radio y Podcasts y elegir sección inicial (Priority: P1) 🎯 MVP

**Goal**: Selector Radio|Podcasts que no corta la reproducción + sección inicial configurable

**Independent Test**: Con podcasts habilitados, cambiar de modo y volver con reproducción en curso intacta; reiniciar con inicial=Podcasts abre en Podcasts; sin podcasts no hay rastro del modo (quickstart §1)

### Tests for User Story 1 ⚠️

> NOTE: Escribir PRIMERO y ver FALLAR antes de implementar

- [X] T025 [P] [US1] Test Red-Green del ViewModel de modo en `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/ModeViewModelTest.kt` (conmutación, arranque por defecto, puerta por capacidades)

### Implementation for User Story 1

- [X] T026 [US1] ViewModel de modo + selector en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/ModeViewModel.kt` (lee `ContentMode`, capacidades y sección inicial)
- [X] T027 [US1] Segmentado Radio|Podcasts en la cabecera de `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` (visible solo con `features.podcasts`)
- [X] T028 [US1] Preferencia de sección inicial Radio|Podcasts en `app/src/main/java/com/izquierdojl/tolocharadio/feature/settings/SettingsScreen.kt` + `SettingsViewModel.kt` + `SettingsLabels.kt` (puerta de arranque)
- [X] T029 [US1] Test Compose de navegación crítica modo Radio↔Podcasts en `app/src/androidTest/java/com/izquierdojl/tolocharadio/feature/podcasts/ModeNavigationTest.kt` (solo flujo crítico)

**Checkpoint**: US1 funciona y es testeable sin ningún otro contenido podcast

---

## Phase 4: User Story 2 - Descubrir programas y suscribirse (Priority: P1)

**Goal**: Búsqueda en directorio, alta RSS manual, suscripción/baja, import/export OPML

**Independent Test**: Buscar→suscribirse→ver en Suscripciones→darse de baja; RSS válida e inválida; OPML sin duplicados + exportar (quickstart §2)

### Tests for User Story 2 ⚠️

- [X] T030 [P] [US2] Test Red-Green de `PodcastSubscriptionsRepo` en `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/PodcastSubscriptionsRepoTest.kt` (search/resolve/subscribe/unsubscribe/bulk, deduplicación por `feedUrl`)
- [X] T031 [P] [US2] Test Red-Green de `DiscoverViewModel` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/DiscoverViewModelTest.kt` (estados carga/vacío/error ES)

### Implementation for User Story 2

- [X] T032 [US2] Implementar `PodcastSubscriptionsRepo` en `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/PodcastSubscriptionsRepo.kt` (constraints: `data-model.md PodcastSummary.feedUrl`, `PodcastSummary.isSubscribed`, `BulkSubscriptionResult.*`)
- [X] T033 [US2] Casos de uso suscribir/darse de baja/importar en `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/SubscriptionUseCases.kt` (usa `ParseOpmlFeeds` de T013)
- [X] T034 [US2] Pantalla Descubrir en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/discover/DiscoverScreen.kt` (búsqueda, alta RSS, importar/exportar OPML con SAF + compartir; constraints: `data-model.md Búsqueda de podcast`)
- [X] T035 [US2] Pantalla Suscripciones en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/subscriptions/SubscriptionsScreen.kt` + `SubscriptionsViewModel.kt` (lista, baja, vacío con acceso a Descubrir)

**Checkpoint**: US1 + US2 funcionan de forma independiente

---

## Phase 5: User Story 3 - Escuchar un episodio y retomar donde lo dejó (Priority: P1)

**Goal**: Reproducción VOD con velocidad/saltos/búsqueda, progreso + reanudación, completado y auto-avance

**Independent Test**: Reproducir→velocidad/saltos→pausar→salir→continuar ≤10 s; terminar con cola avanza solo; descartar de continuar (quickstart §3)

### Tests for User Story 3 ⚠️

- [X] T036 [P] [US3] Test Red-Green de `ProgressRepo` en `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/ProgressRepoTest.kt` (PUT periódica, keepalive POST, idempotencia por `sequence`; constraints: `data-model.md EpisodeProgress.*`)
- [X] T037 [P] [US3] Test Red-Green de `ResolveEpisodePlaybackUseCase` en `app/src/test/java/com/izquierdojl/tolocharadio/domain/podcasts/ResolveEpisodePlaybackUseCaseTest.kt` (local completo vs URL firmada + reemisión; constraints: `data-model.md IssuedToken.expiresAt`, `DownloadRecord.status`)
- [X] T038 [P] [US3] Test Red-Green de `EpisodePlayerViewModel` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/EpisodePlayerViewModelTest.kt` (play/pausa, completado, auto-avance, descarte)

### Implementation for User Story 3

- [X] T039 [US3] Implementar `ProgressRepo` en `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/ProgressRepo.kt` (`sessionId`+`sequence`, PUT + keepalive POST, completado, dismiss)
- [X] T040 [US3] Casos de uso en `app/src/main/java/com/izquierdojl/tolocharadio/domain/podcasts/EpisodePlaybackUseCases.kt` (resolver fuente, siguiente en cola, descartar; constraints: `data-model.md PodcastEpisode.progress`)
- [X] T041 [US3] Controles VOD en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/EpisodePlayerControls.kt` (velocidad 1/1.25/1.5/2, ±15/30 s, búsqueda, insignia local/online; solo episodio; temporizador de apagado detiene el episodio igual que la radio)
- [X] T042 [US3] Cablear emisión firmada en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerViewModel.kt` (DataSource plano, reemisión anticipada, error ES + reintento manual; emisión a otros dispositivos disponible para episodios con la misma política que radio)

**Checkpoint**: Núcleo P1 (modo + descubrir + escuchar) completo — MVP sugerido

---

## Phase 6: User Story 4 - Gestionar la cola de reproducción (Priority: P2)

**Goal**: Añadir (al final/a continuación), reordenar, quitar, vaciar sin detener, con revisión 409

**Independent Test**: Añadir/mover/reordenar/quitar/vaciar con episodio en curso intacto; 409 → aviso + recarga (quickstart §4)

### Tests for User Story 4 ⚠️

- [X] T043 [P] [US4] Test Red-Green de `QueueRepo` en `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/QueueRepoTest.kt` (add/move/reorder/remove/clear+keep, límite `limit`, 409→recarga; constraints: `data-model.md PodcastQueue.revision`, `PodcastQueue.items`, `PodcastQueue.limit`)

### Implementation for User Story 4

- [X] T044 [US4] Implementar `QueueRepo` en `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/QueueRepo.kt` (`baseRevision`, lista completa exacta, episodio oculto con `QueueOrder` de T011)
- [X] T045 [US4] Pantalla Cola en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/queue/QueueScreen.kt` + `QueueViewModel.kt` (pendientes, arrastre, vaciar con `keepEpisodeId`, insignia `queue/count`)

**Checkpoint**: US1–US4 independientes y combinables

---

## Phase 7: User Story 5 - Favoritos de episodios y ficha de detalle (Priority: P2)

**Goal**: Corazón independiente de radio + hoja inferior modal sin perder contexto

**Independent Test**: Marcar→ver en Favoritos→abrir ficha→desmarcar; radio intacta; ficha no detiene (quickstart §5)

### Tests for User Story 5 ⚠️

- [X] T046 [P] [US5] Test Red-Green de `EpisodeFavoritesRepo` en `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/EpisodeFavoritesRepoTest.kt` (add/remove idempotente 404, independencia de emisoras)

### Implementation for User Story 5

- [X] T047 [US5] Implementar `EpisodeFavoritesRepo` en `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/EpisodeFavoritesRepo.kt`
- [X] T048 [US5] Hoja de ficha en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/sheet/EpisodeSheet.kt` + `EpisodeSheetViewModel.kt` (detalle, acciones, devuelve el foco; constraints: `data-model.md PodcastEpisode.*`)
- [X] T049 [US5] Pantalla Favoritos en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/favorites/PodcastFavoritesScreen.kt` + ViewModel (corazón en filas/cola/ficha)

**Checkpoint**: US1–US5 funcionales

---

## Phase 8: User Story 7 - Descargar episodios (Priority: P2)

**Goal**: Descarga interna a Descargas + reproducción local preferente (decisión clarify, estilo Pocket Casts)

**Independent Test**: Descargar con progreso→reproducir en local (modo avión); sin `podcastDownloads` no hay opción; archivo perdido/sin espacio → aviso (quickstart §7)

### Tests for User Story 8 ⚠️

*(ID de historia US7 de spec.md; se numera Phase 8 por orden de prioridad P2 antes que P3)*

- [X] T050 [P] [US7] Test Red-Green de `DownloadsRepo` en `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/DownloadsRepoTest.kt` (mapeo `DownloadManagerId`, preferencia local, archivo perdido/incompleto; constraints: `data-model.md DownloadRecord.*`)

### Implementation for User Story 7

- [X] T051 [US7] Implementar `DownloadsRepo` en `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/DownloadsRepo.kt` (firmada `download` → DownloadManager → `episode_downloads`, nombre `tolocharadio_*`)
- [X] T052 [US7] Estados de descarga en filas/ficha/player en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/sheet/EpisodeSheet.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/EpisodeRow.kt` (descargar→%→descargado/local)

**Checkpoint**: US7 completa sin biblioteca offline avanzada (fuera de alcance)

---

## Phase 9: User Story 6 - Portada, ficha de programa y estadísticas (Priority: P3)

**Goal**: Inicio (continuar/novedades/programas/cola), ficha filtrable/ordenable y estadísticas privadas

**Independent Test**: Portada <3 s con tres bloques; filtros y órdenes; stats coherentes solo cuenta activa (quickstart §6, §8)

### Tests for User Story 6 ⚠️

- [X] T053 [P] [US6] Test Red-Green de `PodcastStatsRepo`/`LoadPodcastStatsUseCase` en `app/src/test/java/com/izquierdojl/tolocharadio/data/repo/PodcastStatsRepoTest.kt` (mapeo a `StatsBundle`; constraints: `data-model.md PodcastStats.*`)
- [X] T054 [P] [US6] Test Red-Green de `PodcastHomeViewModel` y `PodcastShowViewModel` en `app/src/test/java/com/izquierdojl/tolocharadio/feature/podcasts/HomeShowViewModelTest.kt` (filtros `sort/filter`, paginación `hasMore`)

### Implementation for User Story 6

- [X] T055 [US6] Repos de portada/ficha en `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/EpisodesRepo.kt` (home, ficha, episodios paginados, búsqueda global; constraints: `data-model.md PodcastHome.*`, `EpisodePage.*`)
- [X] T056 [US6] Pantalla Inicio en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/home/PodcastHomeScreen.kt` + ViewModel (continuar con descarte, novedades, programas, resumen cola)
- [X] T057 [US6] Ficha de programa en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/show/PodcastShowScreen.kt` + ViewModel (filtros, órdenes, paginación)
- [X] T058 [US6] Stats de podcast en `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/PodcastStatsRepo.kt` + `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/stats/PodcastStatsScreen.kt` (reutiliza bloques de gráficas con adaptadores)

**Checkpoint**: Todas las historias independientes y funcionales

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Coherencia transversal, rendimiento y validación final

- [X] T059 [P] Mensajes ES accionables y estados vacío/carga/error coherentes en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/` (revisión transversal FR-027)
- [X] T060 [P] KDoc en APIs públicas de `domain/podcasts` y `data/repo`/`data/remote` podcast (constitución IV)
- [X] T061 Verificar que ningún log incluye PII/credenciales ni URLs firmadas en `app/src/main/java/com/izquierdojl/tolocharadio/feature/podcasts/` y player VOD
- [X] T062 Ejecutar validación `quickstart.md` completa en `specs/0046-izquierdojl-20261118-modo-podcast-completo/quickstart.md` (9 escenarios, SC-001…SC-010)
- [X] T063 Gates finales en `app/build.gradle.kts` (`assembleDebug`, `testDebugUnitTest`, `detekt ktlintCheck lintDebug` en verde)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias — inicio inmediato
- **Foundational (Phase 2)**: Depende de Setup — BLOQUEA todas las historias
- **User Stories (Phase 3–9)**: Dependen de Foundational; entre sí van en prioridad P1 → P2 → P3 (o en paralelo con equipo suficiente)
- **Polish (Phase 10)**: Depende de las historias que se entreguen

### User Story Dependencies

- **US1 (P1, Phase 3)**: Tras Foundational; sin dependencias de otras historias
- **US2 (P1, Phase 4)**: Tras Foundational; integrable con US1, testeable sola
- **US3 (P1, Phase 5)**: Tras Foundational; usa `DownloadRecord` de Phase 2 para preferencia local (sin US7 implementada, siempre online)
- **US4 (P2, Phase 6)**: Tras Foundational; integrable con US3, testeable sola
- **US5 (P2, Phase 7)**: Tras Foundational; integrable con US3/US4, testeable sola
- **US7 (P2, Phase 8)**: Tras Foundational; completa la preferencia local de US3
- **US6 (P3, Phase 9)**: Tras Foundational; consume US2/US3/US4/US7 pero con stubs testeable sola

### Within Each User Story

- Tests Red-Green PRIMERO y en rojo antes de implementar
- DTOs/entidades antes que repos; repos antes que casos de uso; casos de uso antes que ViewModels/pantallas
- Historia completa antes de pasar a la siguiente prioridad

### Parallel Opportunities

- T002 ∥ T003; T005 ∥ T007 ∥ T008 ∥ T010 ∥ T011 ∥ T013 ∥ T015 ∥ T023 (ficheros distintos)
- Tras Foundational, US1–US7 pueden avanzar en paralelo (distintos ficheros por historia)
- Tests de cada historia (T030∥T031, T036∥T037∥T038, T053∥T054) en paralelo
- T059 ∥ T060 en Polish

---

## Parallel Example: User Story 3

```bash
# Tests de US3 juntos (ficheros distintos):
Task: "Test Red-Green de ProgressRepo en app/src/test/.../ProgressRepoTest.kt"
Task: "Test Red-Green de ResolveEpisodePlaybackUseCase en app/src/test/.../ResolveEpisodePlaybackUseCaseTest.kt"
Task: "Test Red-Green de EpisodePlayerViewModel en app/src/test/.../EpisodePlayerViewModelTest.kt"

# Implementación tras verlos fallar (ficheros distintos donde se indica [P]):
Task: "Implementar ProgressRepo en app/src/main/.../data/repo/ProgressRepo.kt"
```

---

## Implementation Strategy

### MVP First (US1 + US2 + US3)

1. Completar Phase 1 (Setup) + Phase 2 (Foundational, bloqueante)
2. Completar US1 (modo) → validar independiente (quickstart §1)
3. Completar US2 (descubrir/suscribir) → validar (quickstart §2)
4. Completar US3 (escuchar/retomar) → validar (quickstart §3) → **MVP: primera escucha de podcast extremo a extremo**
5. STOP y validar; demo si procede

### Incremental Delivery

1. Setup + Foundational → base lista
2. + US1/US2/US3 → MVP audible
3. + US4/US5/US7 (P2) → oyente diario completo (cola, favoritos, offline local)
4. + US6 (P3) → descubrimiento y conocimiento (portada, ficha, stats)
5. + Polish → gates verdes y quickstart completo

### Parallel Team Strategy

1. Todo el equipo: Setup + Foundational
2. Después: A→US2, B→US3, C→US1 (P1 en paralelo); luego A→US4, B→US5/US7, C→US6
3. Cada historia se integra sin romper las anteriores (contratos fijos de Phase 2)

---

## Notes

- [P] = ficheros distintos, sin dependencias; [USn] traza a spec.md (US7 vive en Phase 8 por prioridad P2-sobre-P3)
- Cada tarea cita su fichero exacto; las restricciones de campos citan `data-model.md Entidad.campo`
- Tests en rojo antes de implementar (constitución III); Compose solo críticos (T029 + fichas implicadas)
- Sin commits salvo petición explícita del usuario (gobernanza del repo)
