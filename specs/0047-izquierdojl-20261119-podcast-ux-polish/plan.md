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
