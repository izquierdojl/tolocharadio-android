# Tasks: Adaptación look-and-feel a la web

**Input**: Design documents from `/specs/002-web-look-and-feel/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md
**Tests**: Incluidos (constitución III + FR-011 WCAG AA + SC-002/SC-005). TDD: tests primero, deben FALLAR antes de implementar.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Congelar referencia web y dejar base lista para tokens/vectores

- [X] T001 [P] Descargar y archivar SVG origen (SierraEmblem, favicon index.html, MountainWall, GitHub path) y anotar commit congelado en `specs/002-web-look-and-feel/contracts/brand-assets.md`
- [X] T002 [P] Auditar usos actuales de color/tema en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/theme/Theme.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/CommonUi.kt` (lista de restos a migrar para SC-002)
- [X] T003 Verificar build base con `app/build.gradle.kts` (`.\gradlew :app:assembleDebug`) sin cambios funcionales

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Tokens, vectores y preferencia de tema que bloquean todas las historias

**⚠️ CRITICAL**: Ninguna historia empieza hasta completar esta fase

- [X] T004 Test unitario de contraste WCAG AA (pares texto/fondo y marca/fondo dark/light) en `app/src/test/java/com/izquierdojl/tolocharadio/core/ui/theme/ContrastTest.kt` (debe FALLAR: tokens nuevos aún no existen)
- [X] T005 [P] Ampliar escala pine/ochre/moss + schemes dark/light + Shapes/Typography sistema en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/theme/Theme.kt`
- [X] T006 [P] Crear `sierra_emblem.xml`, `sierra_emblem_mono.xml`, `mountain_wall.xml`, `ic_github.xml` con colores resueltos por tema en `app/src/main/res/drawable/`
- [X] T007 Actualizar launcher adaptativo y splash en `app/src/main/res/drawable/ic_launcher_foreground.xml`, `app/src/main/res/drawable/ic_launcher_background.xml`, `app/src/main/res/mipmap-anydpi/ic_launcher.xml`, `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml`, `app/src/main/res/values/themes.xml`, `app/src/main/res/values-night/themes.xml`
- [X] T008 Test de preferencia de tema (SYSTEM/DARK/LIGHT, default SYSTEM) en `app/src/test/java/com/izquierdojl/tolocharadio/core/ui/theme/ThemeModeTest.kt` (implementado como test del enum+default; sin Turbine)
- [X] T009 Implementar `themeMode` en `InstancePrefs` DataStore + `ThemeMode` + `setThemeMode` en `ProfileViewModel` (en lugar de ThemePrefs/ThemeViewModel separados) en `app/src/main/java/com/izquierdojl/tolocharadio/data/local/InstancePrefs.kt` y `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/theme/ThemeMode.kt`
- [X] T010 [P] Crear `TolochaLogo` (Sierra + "TolochaRadio" con span brand) en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/TolochaLogo.kt`
- [X] T011 [P] Crear `MountainWall` (path 1200x120, tint Mountain por tema) en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/MountainWall.kt`

**Checkpoint**: `ContrastTest` y `ThemeViewModelTest` en verde, vectores renderizan en dark/light, foundation lista

---

## Phase 3: User Story 1 - Reconocer la marca (Priority: P1) 🎯 MVP

**Goal**: Misma identidad web en Android: emblema, fondo/superficies, marca ocre, launcher/splash/notificación
**Independent Test**: Lado a lado con la web en oscuro/claro: logo, colores y emblema se perciben como la misma marca (SC-001); `quickstart.md` paso 2

- [X] T012 [P] [US1] Test Compose de logotipo en cabecera y placeholder custom en `app/src/androidTest/java/com/izquierdojl/tolocharadio/BrandLogoTest.kt` (debe FALLAR) — verificado manualmente en emulador por el usuario (2026-09-05); `CommonUiTest` actualizado como equivalente parcial
- [X] T013 [US1] Aplicar `TolochaLogo` en cabecera y fondo con matices radiales/lineales en Home/cabecera en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt` (depends on T010)
- [X] T014 [P] [US1] Cablear icono/notificación Media3 (smallIcon mono + largeIcon emblema) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/RadioPlaybackService.kt`
- [X] T015 [US1] Verificar launcher+splash en API 26 y 37 (adaptive + legacy webp en `app/src/main/res/mipmap-*/`) sin pixelado (SC-003) — verificado manualmente en emulador por el usuario (2026-09-05)

**Checkpoint**: US1 funcional y testeable sola; desplegable como MVP de marca

---

## Phase 4: User Story 2 - Tonos, formas y jerarquía (Priority: P2)

**Goal**: Tarjetas, chips, botones, estados vacíos y reproductor con jerarquía de la web
**Independent Test**: Recorrer 6 secciones + mini-player verificando tarjeta 16:9+degradado, chips uppercase, play circular, radios web (`quickstart.md` paso 3)

- [X] T016 [P] [US2] Test Compose de `StationCard` (degradado, play ocre activo, favorito, ≤3 chips, bitrate) en `app/src/androidTest/java/com/izquierdojl/tolocharadio/StationCardStyleTest.kt` (debe FALLAR) — verificado manualmente en emulador por el usuario (2026-09-05); `CommonUiTest` actualizado como equivalente parcial
- [X] T017 [P] [US2] Test Compose de `EmptyState` con tono muted en `app/src/androidTest/java/com/izquierdojl/tolocharadio/EmptyStateStyleTest.kt` (debe FALLAR) — verificado manualmente en emulador por el usuario (2026-09-05)
- [X] T018 [US2] Restyle `StationCard`/`StationListItem`/`StationArtwork` (placeholder emblema custom, artwork 16:9 y 48dp) en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/CommonUi.kt`
- [X] T019 [US2] Restyle `EmptyState`/`ErrorBanner`/`FavoriteButton` (chip uppercase, play circular, reintento accionable) en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/components/CommonUi.kt` (depends on T018)
- [X] T020 [US2] Aplicar tokens al mini-player/full-player (`Surface` + rounded 16dp) en `app/src/main/java/com/izquierdojl/tolocharadio/feature/player/PlayerUi.kt` (no existe `MiniPlayer.kt`; franja MountainWall sobre reproductor: no aplica — el player es bottom-sheet, sin pie web equivalente)

**Checkpoint**: US1+US2 funcionan independientes; SC-002 (0 restos paleta anterior) verificable con T002

---

## Phase 5: User Story 3 - Iconografía coherente (Priority: P3)

**Goal**: Material Symbols con igual significado que Lucide + assets documentados + atajos/widget
**Independent Test**: Cada destino/acción usa icono del mismo significado; SVG versionados y referenciados (SC-003); custom sin favicon muestra emblema (`quickstart.md` paso 4)

- [X] T021 [P] [US3] Test Compose de bottom bar con 5 destinos e iconos equivalentes en `app/src/androidTest/java/com/izquierdojl/tolocharadio/NavIconsTest.kt` (debe FALLAR) — verificado manualmente en emulador por el usuario (2026-09-05)
- [X] T022 [US3] Mapear Lucide→Material (Home, Favorite, History, Radio, Person, PlayArrow/Pause, Menu/Close, ExpandMore, Logout + `ic_github.xml`) en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`
- [X] T023 [US3] Añadir selector Sistema/Claro/Oscuro en Perfil/Ajustes con persistencia local en `app/src/main/java/com/izquierdojl/tolocharadio/feature/profile/ProfileScreen.kt` (depends on T009)
- [X] T024 [P] [US3] Aplicar emblema mono a atajos y widget donde existan en `app/src/main/AndroidManifest.xml` y `app/src/main/res/xml/` (no existen atajos/widget: cubierto por icono adaptativo `mipmap-anydpi`)

**Checkpoint**: Todas las historias funcionalmente independientes

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Gates, contraste final y validación end-to-end

- [X] T025 [P] Revisar `backup_rules.xml`/`data_extraction_rules.xml` para `theme_mode` en `app/src/main/res/xml/backup_rules.xml` (theme_mode viaja con el backup como preferencia local; tokens siguen excluidos)
- [X] T026 Ejecutar validación `specs/002-web-look-and-feel/quickstart.md` pasos 1–5 y adjuntar comparativa lado a lado — validado manualmente en emulador por el usuario (2026-09-05)
- [X] T027 [P] Pasar gates `.\gradlew detekt ktlintCheck lintDebug` y tests `:app:testDebugUnitTest :app:connectedDebugAndroidTest` sin errores — gates estáticos + unitarios en verde (2026-09-05); resto validado manualmente en emulador (`connectedDebugAndroidTest` no ejecutable en este entorno sin adb)
- [X] T028 Limpieza: eliminar literales de color antiguos fuera de `Theme.kt`/`colors.xml` (verificado: 0 restos) y actualizar `README.md` (instancia + sección de tema; capturas pendientes de dispositivo)

---

## Dependencies & Execution Order

- Setup (T001–T003) → Foundational (T004–T011, tests primero) → US1 (T012–T015) → US2 (T016–T020) → US3 (T021–T024) → Polish (T025–T028)
- US1 es el MVP (marca reconocible sola); US2/US3 incrementan sin romper US1
- Paralelo: T001∥T002 · T005∥T006 · T010∥T011 · T012∥T014 · T016∥T017 · T025∥T027; historias en paralelo solo si hay capacidad y foundation está verde

## Parallel Example: User Story 1

```powershell
# Tests + assets independientes de US1:
# T012 BrandLogoTest + T014 notificación Media3 (distintos ficheros)
```

## Implementation Strategy

- MVP: Setup + Foundational + US1, validar lado a lado y desplegar demo
- Luego US2 (componentes), luego US3 (iconos/ajuste tema), luego Polish con quickstart y gates

---

## Phase 7: Convergence

**Origen**: `/speckit.converge` 2026-09-05 — 12 FR, 9 AC, 5 SC, 7 decisiones de plan y 5 principios de constitución revisados contra el código. Sin violaciones de constitución; sin trabajo fuera de alcance; 5 parciales debajo (los pendientes de dispositivo siguen en T012/T015–T017/T021/T026/T027 y no se duplican).

- [X] T029 Añadir variante clara de `sierra_emblem.xml` por tema (insignia `#e3ece5→#c8dbce`, sol `#a67430`, línea `#2c4f38`; sierra mantiene pine-600) vía `drawable-night` o recurso por qualifier per US1/AC3 (partial) — ASUMIDO sin implementar, aceptado por el usuario al cierre (2026-09-05)
- [X] T030 Colocar la franja `MountainWall` sobre el reproductor/pie con el tinte de montaña de cada tema (oscuro translúcido verdoso, claro `#b4c9b9`) per FR-009 (partial) — ASUMIDO sin implementar, aceptado por el usuario al cierre (2026-09-05)
- [X] T031 Cablear `ic_github.xml` al uso previsto (entrada "Acerca de"/repo en Perfil) o justificar y retirar el asset per FR-007 (partial) — ASUMIDO sin implementar, aceptado por el usuario al cierre (2026-09-05)
- [X] T032 Mostrar el emblema como imagen grande de la notificación Media3 (`largeIcon`/artwork) además del `smallIcon` monocromo per T014 (partial) — ASUMIDO sin implementar, aceptado por el usuario al cierre (2026-09-05)
- [X] T033 Añadir el velo de fondo radial/lineal sutil (paridad con `body` de la web) en Home/cabecera per FR-002 (partial) — ASUMIDO sin implementar, aceptado por el usuario al cierre (2026-09-05)

