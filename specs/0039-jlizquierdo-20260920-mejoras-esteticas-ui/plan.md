# Implementation Plan: Mejoras estéticas UI

**Branch**: `0039-jlizquierdo-20260920-mejoras-esteticas-ui` | **Date**: 2026-09-20 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/0039-jlizquierdo-20260920-mejoras-esteticas-ui/spec.md`

## Summary

Tres ajustes de UI sin cambios de backend ni de datos: (1) eliminar por completo el menú "Mover arriba/abajo" de favoritas y dejar el arrastre como única vía de reordenación; (2) quitar el botón de copiar enlace del mini-player (solo play/pausa + silenciar en reproducción normal) y ofrecer en la ficha de emisora una sección "Enlace" con compartir del sistema (principal) + copiar (secundaria); (3) añadir subtítulos confirmados bajo "Explorar" y "Tus favoritos" con el mismo estilo del historial. Enfoque: edición mínima de composables existentes, reutilización del helper de enlace actual, eliminación de código muerto y actualización de tests de UI.

## Technical Context

**Language/Version**: Kotlin (JVM target 17, null-safe obligatorio)

**Primary Dependencies**: Jetpack Compose + Material3 (UI), androidx.media3 (player, sin cambios), Hilt, Coil; sin dependencias nuevas (YAGNI)

**Storage**: N/A (sin cambios de esquema Room, DataStore ni credenciales)

**Testing**: JUnit + kotlinx-coroutines-test + Turbine y MockK/Mockito-Kotlin para unit; `composeTestRule` para los flujos UI críticos tocados; Detekt + ktlint + Android Lint en CI

**Target Platform**: Android `minSdk` 26, `targetSdk`/`compileSdk` 37

**Project Type**: mobile-app de un solo módulo `app` (organizado por feature: `favorites/`, `explore/`, `history/`, `player/`)

**Performance Goals**: Sin nuevos objetivos; no debe degradar el arranque en frío (< 2 s en gama media) ni introducir tirones en el arrastre de favoritas

**Constraints**: MVVM + Clean (`ui → domain → data`, ViewModel sin `Context` de Activity); Compose M3 sin XML/Views nuevos; Media3 para reproducción; HTTPS-only; sin PII ni credenciales en logs; textos de UI en español; compartir usa `ACTION_SEND` del framework + portapapeles (sin librerías nuevas)

**Scale/Scope**: 4 superficies tocadas (lista de favoritas, mini-player, ficha de emisora, encabezados de Explorar/Favoritas); ~5 ficheros de UI más ajustes de tests; 0 endpoints nuevos

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. MVVM + Clean por capas**: PASS. Cambios confinados a `ui` (composables y parámetros de `SectionHeader`); sin reglas de negocio nuevas. El reorden por arrastre sigue usando el `ViewModel`/`UseCase` existente (`moveItem`/`commitOrder`); solo se retiran los callbacks de menú.
- **II. Stack Kotlin-First, Compose M3 y Media3**: PASS. Solo Compose M3, sin XML, sin `MediaPlayer`/`VideoView`, sin pantallas de login, sin dependencias nuevas. El compartir usa `Intent.ACTION_SEND` + portapapeles del framework.
- **III. Calidad Test-First**: PASS con plan de tests (ver research R7): se actualizan/añaden tests de UI Compose de los flujos tocados y se mantienen los gates estáticos; sin lógica `domain`/`data` nueva que exigiría unit tests nuevos.
- **IV. Streaming Robusto y Manejo de Errores**: PASS. Estados del player intactos; error y carga conservan reintentar/cancelar; el enlace real solo se comparte/copia, nunca altera la URL de reproducción; aviso "Enlace no disponible" sin PII.
- **V. Simplicidad Modular (YAGNI)**: PASS. Sin multi-módulo, sin BaaS, sin abstracciones nuevas; se elimina código muerto (menú + botón del panel) en lugar de comentarlo.

Re-evaluación post-diseño (Fase 1): sin violaciones nuevas; no se añade `Complexity Tracking`.

## Project Structure

### Documentation (this feature)

```text
specs/0039-jlizquierdo-20260920-mejoras-esteticas-ui/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/java/com/izquierdojl/tolocharadio/
├── feature/favorites/FavoritesScreen.kt        # eliminar FavoriteRowMenu + callbacks de menú
├── feature/explore/ExploreScreen.kt            # subtitle en SectionHeader
├── feature/history/HistoryScreen.kt            # referencia (sin cambios, coherencia visual)
├── feature/player/PlayerUi.kt                  # retirar PanelCopyButton del MiniPlayer
├── feature/player/PanelHelpers.kt              # resolveCopyLink reutilizado por la ficha
├── feature/player/StationInfoSheet.kt          # nueva sección "Enlace" (Compartir + Copiar)
└── core/ui/components/SectionHeader.kt         # sin cambios (ya soporta subtitle)

app/src/test/java/.../feature/
├── favorites/FavoritesViewModelTest.kt         # sin cambios de lógica esperados
└── player/PlayerViewModelTest.kt               # sin cambios de lógica esperados

app/src/androidTest/java/.../feature/
└── favorites/FavoritesScreenTest.kt            # actualizar aserciones del menú
```

**Structure Decision**: Se mantiene el módulo único `app` con organización por feature espejo de la web; la ficha de emisora vive en `feature/player/` junto al panel que la abre. Sin módulos ni paquetes nuevos.

## Complexity Tracking

Sin violaciones constitucionales que justificar; sección intencionalmente vacía.
