# Implementation Plan: Mejoras de experiencia podcast

**Branch**: `0047-izquierdojl-20261119-podcast-ux-polish` | **Date**: 2026-11-19 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/0047-izquierdojl-20261119-podcast-ux-polish/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command; its definition describes the execution workflow.

## Summary

Siete mejoras de UX y reproducción sobre el modo podcast ya existente (spec 0046), sin cambios en el contrato del servidor:

1. **Reanudación desde la cola**: resolver la posición de escucha más reciente antes de arrancar (progreso embebido en el episodio o `GET /podcasts/episodes/{id}/progress` como respaldo), con la misma regla en el avance automático.
2. **Controles en la notificación**: preferencias de botones de la `MediaSession` con los iconos `ICON_SKIP_BACK_15` / `ICON_SKIP_FORWARD_30`, mapeados a `COMMAND_SEEK_BACK` / `COMMAND_SEEK_FORWARD` y a incrementos de 15 s / 30 s; solo con episodio activo, nunca en radio en directo.
3. **Cabecera y navegación**: `TopAppBar` con hamburguesa + logotipo (que se reduce a emblema si no cabe), `ModalNavigationDrawer` con Temporizador/Servidores/Configuración y salida de Configuración de las dos barras inferiores.
4. **Potenciador de voz**: compresor + limitador propio como `AudioProcessor` de Media3 inyectado en el `DefaultAudioSink` del player compartido, con preferencia persistida y activo solo en episodios locales.
5. **Insignia de cola**: contador de pendientes en el destino Cola, alimentado por el estado de `QueueRepo` y actualizado tras cada mutación.
6. **Descarga animada**: progreso real desde `DownloadManager` (bytes descargados / total) expuesto por `DownloadsRepo` y pintado por un botón compartido en filas y ficha.
7. **Tarjetas legibles**: `PodcastCard` y `EpisodeCard` con título a dos líneas garantizadas, metadatos a una línea y acciones en fila propia bajo el texto.

El enfoque reutiliza la arquitectura existente (MVVM + Clean, Hilt, Media3, Compose M3), no añade dependencias ni módulos Gradle y concentra las reglas nuevas en `domain` con UI delgada.

**Extensión 2026-11-21**: se añade la búsqueda en Suscripciones (US8, FR-059–FR-071, SC-012–SC-014), planificada en la sección final. El resto del plan original sigue vigente sin cambios.

## Technical Context

**Language/Version**: Kotlin 2.3.10, JVM target 17 (`app/build.gradle.kts`)

**Primary Dependencies**: Jetpack Compose + Material 3 (BOM del proyecto), Media3 1.11.0 (`exoplayer`, `session`, `ui`, `cast`, `datasource-okhttp`), Hilt 2.60.1, Room 2.7.2, DataStore 1.1.1, Retrofit + OkHttp + kotlinx.serialization, Coil, Hilt Navigation Compose. Sin dependencias nuevas.

**Storage**: DataStore Preferences para la preferencia `voiceBoost` (booleano, por dispositivo). Room **sin cambios de esquema**: el progreso de descarga es efímero y no se persiste. El estado de descarga sigue en `episode_downloads` y en `DownloadManager`.

**Testing**: JUnit 4 + `kotlinx-coroutines-test` + Turbine + MockK para `domain`, `data` y ViewModel; Compose Test (`createComposeRule`) para cabecera, cajón, barra inferior con insignia, tarjetas y botón de descarga. Gates: `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck`, `lintDebug`.

**Target Platform**: Android; `minSdk 26`, `compileSdk`/`targetSdk` 37. Móvil, tema oscuro por defecto, UI en español.

**Project Type**: Aplicación Android de un solo módulo `app`, organizada por feature (MVVM + Clean por capas).

**Performance Goals**:
- El potenciador de voz surte efecto en el siguiente búfer de audio (< 100 ms percibidos; criterio < 1 s).
- La insignia refleja mutaciones locales de cola en < 1 s (publicación inmediata de `QueueRepo`).
- El porcentaje de descarga se refresca cada 1 s mientras hay transferencia activa y deja de sondearse al terminar.
- Sin trabajo adicional en arranque en frío: la cola solo se carga al entrar en modo Podcasts o al abrir Inicio/Cola.

**Constraints**:
- Sin cambios de API, DTO, autenticación ni credenciales (FR-052).
- Sin nuevas librerías ni módulos Gradle (YAGNI, constitución V).
- Sin XML/Views nuevas; todo Compose M3.
- Sin PII en logs; errores accionables en español.
- La reproducción de radio en directo no debe ofrecer retroceso/avance en la notificación ni recibir el potenciador de voz.
- El estado del reproductor y la sesión multimedia deben sobrevivir a rotación, segundo plano y cambio de episodio.

**Scale/Scope**: 1 módulo, ~7 áreas de UI/player, 53 requisitos funcionales, 7 historias. Cola de hasta 200 episodios; 4 destinos en la barra de radio y 5 en la de podcast tras retirar Configuración. Sin cambios de backend.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio / gate | Cumplimiento | Evidencia en el plan |
|------------------|--------------|----------------------|
| **I. MVVM + Clean** | PASS | Reglas nuevas en `domain` (posición de reanudación, conteo de pendientes, política del potenciador, fracción de descarga); ViewModels sin `Context` de Activity; Hilt para VMs y repos. El procesador de audio vive en `feature/player` como infraestructura del reproductor. |
| **II. Kotlin + Compose M3 + Media3** | PASS | UI Compose M3; `MediaSession`/`MediaButtonPreferences` y `AudioProcessor` de Media3 1.11; sin `MediaPlayer`, sin XML nuevo, sin nuevas dependencias. |
| **III. Test-First** | PASS | Tests unitarios para reanudación, descarga, insignia y DSP; tests Compose para cabecera/cajón, barra, tarjetas y descarga. Red-Green antes de cada cambio. |
| **IV. Streaming robusto** | PASS | Saltos limitados al rango válido; radio sin seek; errores en español; sin PII; estado del player tipado; Cast con controles coherentes. |
| **V. Simplicidad (YAGNI)** | PASS | Procesador mínimo (sin trim silence ni ecualizador); sin módulos ni librerías nuevas; se elimina el botón de temporizador de la cabecera si queda sin uso. |
| **Gates de calidad** | PASS | CI: `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck`, `lintDebug`. Compose tests (`connectedDebugAndroidTest`): gate local pre-merge con emulador; no se ejecutan en CI. |
| **Gobernanza (v3.1.0)** | PASS | Enmienda aplicada: barra inferior solo de contenido y Configuración/Servidores en el menú de hamburguesa. No se requiere excepción. |

## Project Structure

### Documentation (this feature)

```text
specs/0047-izquierdojl-20261119-podcast-ux-polish/
├── plan.md              # Este archivo
├── research.md          # Fase 0
├── data-model.md        # Fase 1
├── quickstart.md        # Fase 1
├── contracts/           # Fase 1 (contratos de UI y de sesión multimedia)
│   ├── ui-contracts.md
│   └── playback-notification.md
├── checklists/
│   └── requirements.md
└── tasks.md             # Fase 2 (/speckit.tasks, no se crea aquí)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── core/ui/components/
│   ├── TolochaLogo.kt              # + showText / forma reducida
│   ├── EpisodeCardUi.kt            # tarjeta de episodio con acciones bajo el texto
│   ├── PodcastRows.kt              # PodcastCard con acción en fila propia
│   ├── EpisodeRow.kt               # parámetro de progreso de descarga
│   └── DownloadProgressButton.kt   # NUEVO: botón circular con % / indeterminado
├── core/ui/navigation/
│   ├── TolochaNavGraph.kt          # orquestación, drawer state, badge y wiring
│   ├── TolochaTopBar.kt            # NUEVO: hamburguesa + logo + modo + Cast
│   ├── TolochaDrawer.kt            # NUEVO: Temporizador / Servidores / Configuración
│   └── BottomDest.kt               # sin Configuración en ambas barras
├── data/local/InstancePrefs.kt     # + voiceBoost (DataStore)
├── data/repo/DownloadsRepo.kt      # + progreso efímero, guardas de duplicado, re-monitor
├── domain/podcasts/
│   ├── EpisodeResume.kt            # NUEVO: regla pura de posición de reanudación
│   ├── QueueBadge.kt               # NUEVO: conteo de pendientes y formato 99+
│   ├── VoiceBoostPolicy.kt         # NUEVO: preferencia ∧ episodio local ∧ sin Cast
│   └── EpisodePlaybackUseCases.kt  # + carga de progreso fresco si falta
├── feature/player/
│   ├── PlaybackPlayerFactory.kt    # renderers factory + incrementos 15/30
│   ├── VoiceBoostDsp.kt            # NUEVO: compresor + limitador puro (testeable en JVM)
│   ├── VoiceBoostAudioProcessor.kt # NUEVO: envoltorio AudioProcessor (Media3)
│   ├── VoiceBoostController.kt     # NUEVO: estado persistido + política de sesión
│   ├── RadioPlaybackService.kt     # media button preferences radio ↔ episodio
│   └── SleepTimerMenu.kt           # NUEVO/reutilizado: opciones del temporizador
├── feature/podcasts/
│   ├── EpisodePlayerViewModel.kt   # reanudación + auto-avance + toggle de voz
│   ├── EpisodeActionsViewModel.kt  # expone progreso de descarga
│   ├── queue/QueueBadgeViewModel.kt# NUEVO: pendientes para la insignia
│   ├── queue/QueueScreen.kt        # selección con reanudación
│   ├── player/EpisodeFullPlayerSheet.kt # control del potenciador
│   └── sheet/EpisodeSheet.kt       # botón de descarga con progreso
└── di/                             # provisión del player si hiciera falta

app/src/test/java/com/izquierdojl/tolocharadio/
├── domain/podcasts/EpisodeResumeTest.kt
├── domain/podcasts/QueueBadgeTest.kt
├── domain/podcasts/VoiceBoostPolicyTest.kt
├── feature/player/VoiceBoostDspTest.kt
├── feature/podcasts/EpisodePlayerViewModelTest.kt
├── feature/podcasts/queue/QueueBadgeViewModelTest.kt
└── data/repo/DownloadsRepoTest.kt  # ampliado

app/src/androidTest/java/com/izquierdojl/tolocharadio/
├── core/ui/navigation/TolochaTopBarTest.kt
├── core/ui/navigation/TolochaDrawerTest.kt
├── core/ui/navigation/TolochaNavigationBarTest.kt # actualizado a 4 destinos + insignia
├── core/ui/components/PodcastCardsTest.kt
├── core/ui/components/DownloadProgressButtonTest.kt
└── feature/podcasts/player/VoiceBoostToggleTest.kt
```

**Structure Decision**: se mantiene el módulo único `app` y la separación por capas existente. La cabecera y el cajón salen de `TolochaNavGraph` a archivos propios para no engordar el composable de navegación y cumplir Detekt. Los cálculos nuevos (reanudación, pendientes, política de voz, DSP) son funciones puras en `domain`/`feature/player`, testeables sin Android salvo el envoltorio `AudioProcessor` y la UI.

## Phase 0: Research

Resumen de decisiones (detalle en [research.md](./research.md)):

- **R1 Reanudación**: usar el progreso embebido si existe; si falta, `GET /progress` antes de arrancar; función pura `resumePositionSec` (completado, negativo o ≥ duración → 0). Aplicar también en auto-avance. Alternativa descartada: cambiar el backend para incluir progreso en `/queue`.
- **R2 Notificación**: `MediaSession.setMediaButtonPreferences` con `ICON_SKIP_BACK_15` / `ICON_SKIP_FORWARD_30` y `SLOT_BACK` / `SLOT_FORWARD`, comandos `COMMAND_SEEK_BACK` / `COMMAND_SEEK_FORWARD`, incrementos `setSeekBackIncrementMs(15_000)` / `setSeekForwardIncrementMs(30_000)`. Radio conserva stop/mute/retry en `SLOT_OVERFLOW`. Alternativa descartada: notificación propia completa.
- **R3 Potenciador de voz**: `AudioProcessor` propio (compresor RMS + makeup + limitador) dentro de `DefaultAudioSink` vía `DefaultRenderersFactory.buildAudioSink`; una instancia por player; estado compartido en `VoiceBoostController`; preferencia en DataStore. Alternativa descartada: `LoudnessEnhancer` (sube ruido y no comprime).
- **R4 Insignia**: pendientes = episodios de `QueueRepo.queue` menos el que está sonando; `QueueBadgeViewModel` a ámbito de actividad; refresco al entrar en modo Podcasts y al volver a primer plano; mutaciones locales publican al instante. Alternativa descartada: usar solo `queue/count`, que incluye el episodio en curso.
- **R5 Descarga**: ampliar `queryStatus` con `COLUMN_BYTES_DOWNLOADED_SO_FAR` y `COLUMN_TOTAL_SIZE_BYTES`; `MutableStateFlow` efímero; sondeo de 1 s mientras haya descargas; guarda contra duplicados; reanudar el monitor tras reinicio. Alternativa descartada: columnas nuevas en Room (migración innecesaria).
- **R6 Tarjetas**: `Text(minLines = 2, maxLines = 2)` + metadatos 1 línea + fila de acciones fija; altura estable sin intrínsecos costosos en la cuadrícula. Alternativa descartada: `IntrinsicSize` por fila.
- **R7 Cabecera**: `navigationIcon` con hamburguesa, título con `BoxWithConstraints` que oculta el texto si no cabe, `ModalNavigationDrawer` a nivel de grafo. Alternativa descartada: `DropdownMenu` anclado (peor accesibilidad y menos espacio para etiquetas).
- **R8 Temporizador en el cajón**: reutilizar `SleepTimerDuration` y `SleepTimerViewModel`; fila con estado activo y menú de duraciones; se retira el botón-icono de la cabecera.
- **R9 Pruebas**: unitarias JVM puras para reglas y DSP; Compose para flujos críticos; actualizar los tests de navegación que fijan 5 destinos.

## Phase 1: Design & Contracts

Artefactos generados:

- [data-model.md](./data-model.md): entidades de reanudación, insignia, descarga, preferencia de voz, cabecera/cajón y tarjetas; reglas de validación y transiciones.
- [contracts/ui-contracts.md](./contracts/ui-contracts.md): contrato de la barra superior, cajón, barra inferior con insignia, tarjetas, botón de descarga y control de voz (semántica y estados).
- [contracts/playback-notification.md](./contracts/playback-notification.md): comandos, slots, iconos, incrementos y matriz radio/episodio/Cast.
- [quickstart.md](./quickstart.md): comandos de build/test y escenarios manuales de validación.

**Post-Design Constitution Check**: se mantiene el resultado anterior (PASS). La guía de navegación se actualizó en la constitución v3.1.0 y el cambio de ubicación de Configuración ya está cubierto; no quedan excepciones. El diseño no añade dependencias, módulos ni permisos.

## Complexity Tracking

Sin violaciones que justificar. La guía de navegación se actualizó en la constitución v3.1.0 (barra inferior solo de contenido; Configuración y Servidores en el menú de hamburguesa), por lo que el cambio de ubicación ya no requiere excepción.

## Extensión 2026-11-21 — Búsqueda en Suscripciones

**Rama de trabajo**: `main` (la rama `0047-izquierdojl-20261119-podcast-ux-polish` ya está fusionada; esta extensión se planifica sobre la base actual).

**Spec**: [spec.md](./spec.md) — User Story 8, FR-059–FR-071, SC-012–SC-014.

### Resumen

Añadir a la pantalla de Suscripciones un buscador con dos ámbitos, sin cambios en el contrato del servidor:

1. **«Podcasts»**: filtro local por título sobre la lista de suscripciones ya cargada, con coincidencia parcial y sin distinguir mayúsculas ni tildes, conservando el orden activo.
2. **«Episodios»**: búsqueda por título dentro de las suscripciones mediante la capacidad que el servidor ya ofrece a la web (`GET /podcasts/episodes/search`), con paginación, estados de carga/vacío/error y acciones de episodio.
3. **Buscador colapsado**: los controles están ocultos por defecto y se despliegan con un botón de la cabecera situado a la izquierda del orden de suscripciones; al ocultarlos se limpian término y resultados.
4. **Un único término compartido** entre ámbitos; los resultados sustituyen a la lista mientras están activos; Atrás vuelve a «Podcasts» con el término conservado; sin suscripciones no se muestra el buscador.
5. **Mismos estados y acciones** que el resto de la app para los episodios encontrados: abrir ficha, reproducir, favorito, cola y descarga, reutilizando `EpisodeRow`, `EpisodePlayerViewModel`, `EpisodeActionsViewModel` y `EpisodeSheet`.

No se modifican `PodcastsApi.kt`, los DTOs, la autenticación, el esquema de datos ni las dependencias.

### Technical Context (delta)

**Language/Version**: Kotlin 2.3.10, JVM target 17 (sin cambios).

**Primary Dependencies**: las existentes. Se reutilizan `EpisodesRepo.searchEpisodes`, `PodcastEpisodeSearchPage`, `EpisodeRow`, `EpisodePlayerViewModel`, `EpisodeActionsViewModel`, `EpisodeSheet` y los componentes Material 3 ya disponibles (`OutlinedTextField`, `SingleChoiceSegmentedButtonRow`, `SegmentedButton`). Sin dependencias nuevas.

**Storage**: ninguna nueva. El estado de búsqueda (término, ámbito, resultados y paginación) vive en el `ViewModel`; no se persiste historial ni término entre sesiones.

**Testing**: JUnit 4 + `kotlinx-coroutines-test` + Turbine + MockK para dominio y ViewModel; Compose Test para el buscador y los estados de resultados. Gates: `assembleDebug`, `testDebugUnitTest`, `detekt`, `ktlintCheck`, `lintDebug`; Compose tests en local con emulador.

**Target Platform**: Android; `minSdk 26`, `compileSdk`/`targetSdk` 37. Móvil, UI en español, tema oscuro por defecto.

**Project Type**: aplicación Android de un módulo `app`, MVVM + Clean por capas.

**Performance Goals**:
- El filtro local de «Podcasts» responde en < 300 ms con al menos 50 suscripciones, sin red.
- La primera página de episodios aparece en < 2 s con una conexión habitual.
- La paginación carga 20 resultados por página y solo al pulsar «Cargar más».
- En «Episodios» no se consulta al servidor al escribir; solo al confirmar la búsqueda.

**Constraints**:
- Sin cambios de API, DTO, autenticación ni credenciales (FR-069).
- Sin nuevas librerías ni módulos Gradle (YAGNI, constitución V).
- Sin XML/Views nuevas; todo Compose M3.
- Sin PII en logs; errores accionables en español.
- El buscador no aparece sin suscripciones ni en servidores sin podcasts.
- La búsqueda no altera la reproducción en curso; las acciones de episodio reutilizan el player y las acciones existentes.

**Scale/Scope**: 1 pantalla, 1 historia (US8), 13 requisitos funcionales (FR-059–FR-071), 3 criterios de éxito (SC-012–SC-014). Página de 20 resultados; sin historial, sugerencias ni búsqueda incremental en episodios.

### Constitution Check (delta)

| Principio / gate | Cumplimiento | Evidencia en el plan |
|------------------|--------------|----------------------|
| **I. MVVM + Clean** | PASS | `filterSubscriptionsByTitle` y la validación del término viven en `domain`; `SearchSubscriptionEpisodesUseCase` envuelve el repositorio; el ViewModel expone estado tipado y la UI solo compone. |
| **II. Kotlin + Compose M3 + Media3** | PASS | `OutlinedTextField`, `SingleChoiceSegmentedButtonRow`, `LazyColumn` y `EpisodeRow`; sin XML ni cambios en Media3. |
| **III. Test-First** | PASS | Tests de dominio (normalización/filtro y caso de uso), ViewModel (búsqueda, paginación, errores, carreras y baja) y Compose (campo, ámbitos, estados) antes de implementar. |
| **IV. Streaming robusto** | PASS | La búsqueda no toca el player; las acciones reutilizan los mismos ViewModels y estados; errores en español; sin PII. |
| **V. Simplicidad (YAGNI)** | PASS | Se reutilizan API, DTOs, repositorio y componentes existentes; sin historial, sugerencias, nuevas dependencias ni módulos. |
| **Gates de calidad** | PASS | Los mismos gates obligatorios; Compose tests en local con emulador. |
| **Gobernanza (v3.1.0)** | PASS | No se altera la navegación ni la ubicación de Configuración/Servidores; no se requiere excepción. |

### Project Structure (delta)

```text
specs/0047-izquierdojl-20261119-podcast-ux-polish/
├── plan.md                         # esta sección
├── research.md                     # + R10–R13
├── data-model.md                   # + sección 7
├── quickstart.md                   # + 3.9
└── contracts/
    └── subscriptions-search.md     # NUEVO: contrato de UI y API consumida

app/src/main/java/com/izquierdojl/tolocharadio/
├── core/ui/components/TolochaTestTags.kt        # + etiquetas de búsqueda
├── domain/podcasts/
│   ├── SubscriptionSearch.kt                    # NUEVO: normalización y filtro
│   └── SearchSubscriptionEpisodesUseCase.kt     # NUEVO: búsqueda de episodios
├── feature/podcasts/
│   ├── SubscriptionsViewModel.kt                # + estado y acciones de búsqueda
│   └── subscriptions/
│       ├── SubscriptionsScreen.kt               # integra buscador, resultados y Atrás
│       ├── SubscriptionSearchBar.kt             # NUEVO: campo + ámbitos
│       ├── SubscriptionSearchResults.kt         # NUEVO: resultados y estados
│       └── SubscriptionEpisodeResult.kt         # NUEVO: DTO → EpisodeRow
└── res/values/strings.xml                       # + textos de búsqueda

app/src/test/java/com/izquierdojl/tolocharadio/
├── domain/podcasts/SubscriptionSearchTest.kt
├── domain/podcasts/SubscriptionSearchUseCaseTest.kt
└── feature/podcasts/SubscriptionsViewModelTest.kt

app/src/androidTest/java/com/izquierdojl/tolocharadio/
└── feature/podcasts/subscriptions/
    ├── SubscriptionSearchBarTest.kt
    └── SubscriptionSearchResultsTest.kt
```

**Structure Decision**: se mantiene el módulo único y la separación por capas. La lógica de normalización y filtro es pura y testeable en JVM; la búsqueda remota se encapsula en un caso de uso; la pantalla se divide en componentes pequeños para no engordar `SubscriptionsScreen` ni disparar la complejidad de Detekt. Las acciones de episodio se delegan en los ViewModels de actividad ya usados por Inicio y Favoritos.

### Phase 0: Research (delta)

Resumen de decisiones (detalle en [research.md](./research.md), R10–R13):

- **R10 Filtro local**: normalización NFD + eliminación de marcas + minúsculas, coincidencia parcial y orden preservado; función pura en `domain`. Alternativa descartada: `contains(ignoreCase = true)` (no ignora tildes).
- **R11 Búsqueda de episodios**: reutilizar `EpisodesRepo.searchEpisodes` mediante `SearchSubscriptionEpisodesUseCase`; página de 20, `offset = tamaño actual`, deduplicación por id y `hasMore` del servidor. Guarda de generación para descartar respuestas tardías porque `safeCall` captura excepciones. Alternativa descartada: endpoint nuevo o filtro en cliente de todos los episodios.
- **R12 UI y navegación**: buscador colapsado por defecto bajo la cabecera, desplegable con un botón de búsqueda a la izquierda del orden; resultados sustituyen a la lista; se ocultan orden/vista mientras hay resultados; `BackHandler` vuelve a «Podcasts» conservando el término. Alternativas descartadas: resultados bajo la lista o pantalla separada.
- **R13 Pruebas**: dominio (filtro y caso de uso), ViewModel (éxito, vacío, error, reintento, paginación, carreras, baja) y Compose de componentes stateless; validación manual en quickstart.

### Phase 1: Design & Contracts (delta)

Artefactos actualizados:

- [data-model.md](./data-model.md): sección 7 con `SubscriptionSearchScope`, `SubscriptionEpisodeResult` y `SubscriptionSearchUi`, sus transiciones e invariantes.
- [contracts/subscriptions-search.md](./contracts/subscriptions-search.md): contrato del buscador, de los resultados, de la API consumida (sin cambios) y semántica de accesibilidad.
- [quickstart.md](./quickstart.md): sección 3.9 con escenarios manuales de búsqueda.

**Post-Design Constitution Check**: se mantiene en PASS. No hay nuevas dependencias, permisos, módulos ni cambios de contrato; la accesibilidad y el 200 % se cubren en el contrato de UI y en quickstart.

### Complexity Tracking (delta)

Sin violaciones que justificar. La pantalla se apoya en componentes existentes y en dos ViewModels de actividad ya compartidos; no se introduce deuda de navegación ni de persistencia.
