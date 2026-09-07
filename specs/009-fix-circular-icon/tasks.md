# Tasks: Fix Circular Icon Crop

**Input**: Design documents from `/specs/009-fix-circular-icon/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, quickstart.md

**Tests**: No se generan tareas de tests automatizados (spec/plan definen verificación visual manual como método proporcional; sin lógica domain/data/ViewModel afectada — ver plan.md Constitution Check III).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inspección base y confirmación del diagnóstico antes de tocar recursos

- [x] T001 Medir bounding-box actual del emblema en app/src/main/res/drawable/ic_launcher_foreground.xml y confirmar exceso sobre zona segura 21–87dp (research R1)
- [x] T002 [P] Verificar referencias icon/roundIcon/monochrome en app/src/main/AndroidManifest.xml y app/src/main/res/mipmap-anydpi/ic_launcher.xml y app/src/main/res/mipmap-anydpi/ic_launcher_round.xml
- [x] T003 [P] Inventariar rasters legacy en app/src/main/res/mipmap-mdpi, mipmap-hdpi, mipmap-xhdpi, mipmap-xxhdpi, mipmap-xxxhdpi (ic_launcher.webp e ic_launcher_round.webp)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Fijar la geometría objetivo que bloquea toda edición posterior

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Fijar transformación objetivo del emblema (scale ≈ 0.9375, translate ≈ 24dp, bounding-box 24–84dp con margen ≥ 2dp) según research.md R1 y data-model.md IconForeground

**Checkpoint**: Geometría objetivo fijada — la implementación de historias puede comenzar

---

## Phase 3: User Story 1 - Icono legible con máscara circular (Priority: P1) ⭐ MVP

**Goal**: Sol, sierra y antena completos y sin recortes bajo máscara circular, manteniendo paleta Tema Tolocha

**Independent Test**: Instalar debug en launcher con forma circular y comprobar en cajón de apps + inicio + Ajustes que el emblema está 100% visible sin tocar el borde (SC-001, quickstart.md paso 2)

### Implementation for User Story 1

- [x] T005 [US1] Reencuadrar grupo emblema en app/src/main/res/drawable/ic_launcher_foreground.xml (aplicar scale/translate de T004, verificar sol/antena/sierra dentro de 21–87dp)
- [x] T006 [US1] Eliminar rect de fondo #08100B duplicado en app/src/main/res/drawable/ic_launcher_foreground.xml dejando foreground transparente salvo emblema (research R2)
- [x] T007 [US1] Compilar con ./gradlew assembleDebug y corregir errores de recursos si aparecen
- [X] T008 [US1] Verificación visual circular en ≥ 2 densidades según specs/009-fix-circular-icon/quickstart.md paso 2 (SC-001) — verificado manualmente en emulador por el usuario (2026-09-07)

**Checkpoint**: User Story 1 completa — icono circular sin recortes, verificable de forma independiente (MVP entregable)

---

## Phase 4: User Story 2 - Icono consistente en resto de formas (Priority: P2)

**Goal**: Sin regresiones en squircle/gota/cuadrada + themed-icon + legacy webp + tamaño pequeño

**Independent Test**: Cambiar forma de icono del sistema por squircle/gota/cuadrada y activar iconos con tema; el emblema aparece centrado con aire uniforme y la silueta mono es reconocible (SC-002/SC-003, quickstart.md paso 3)

### Implementation for User Story 2

- [x] T009 [US2] Regenerar los 10 rasters legacy con Image Asset Studio desde los vectores corregidos en app/src/main/res/mipmap-mdpi, mipmap-hdpi, mipmap-xhdpi, mipmap-xxhdpi, mipmap-xxxhdpi (ic_launcher.webp e ic_launcher_round.webp) (research R3)
- [X] T010 [US2] Verificación visual de matriz circular/squircle/gota/cuadrada + themed-icon según specs/009-fix-circular-icon/quickstart.md paso 3 (SC-002/SC-003) — verificado manualmente en emulador por el usuario (2026-09-07)
- [X] T011 [US2] Verificación de legibilidad a 48dp y en vista de ajustes según specs/009-fix-circular-icon/quickstart.md paso 4 (FR-005) — verificado manualmente en emulador por el usuario (2026-09-07)

**Checkpoint**: User Stories 1 y 2 funcionan — sin recortes en ninguna forma ni densidad

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Gates de calidad y cierre de PR

- [x] T012 [P] Ejecutar Android Lint sobre res/ y Detekt/ktlint sin errores (./gradlew lint detekt ktlintCheck o equivalentes del repo)
- [X] T013 Ejecutar validación completa de specs/009-fix-circular-icon/quickstart.md y confirmar los 4 criterios SC-001–SC-004 — verificado manualmente en emulador por el usuario (2026-09-07)
- [X] T014 Revisar diff final acotado a app/src/main/res (1 foreground + 10 webp, sin código Kotlin ni dependencias) y enlazar spec en la PR — verificado manualmente por el usuario (2026-09-07)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Reusa el foreground de US1 (T005/T006); si se trabaja en paralelo, coordinar que T009 espere al vector final

### Within Each User Story

- Foreground primero (T005 → T006), luego build (T007), luego verificación visual (T008)
- Legacy (T009) después del vector final; verificaciones (T010, T011) al final de su fase
- No hay tests automatizados que escribir antes (verificación visual según quickstart.md)

### Parallel Opportunities

- T002 y T003 (inventario) pueden correr en paralelo tras T001
- T012 puede adelantarse en paralelo con las verificaciones visuales (distinto ámbito: lint vs. visual)
- US1 y US2 no se paralelizan plenamente porque T009 depende del vector final de T005/T006

---

## Parallel Example: Setup

```bash
# Lanzar inventario base en paralelo (distintos ficheros, sin dependencias):
Task: "Verificar referencias icon/roundIcon/monochrome en AndroidManifest.xml y mipmap-anydpi"
Task: "Inventariar rasters legacy en mipmap-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T003)
2. Complete Phase 2: Foundational (T004)
3. Complete Phase 3: User Story 1 (T005–T008)
4. **STOP and VALIDATE**: Test US1 independientemente (circular en 2 densidades, SC-001)
5. Deploy/demo if ready (el MVP ya resuelve el bug reportado)

### Incremental Delivery

1. Complete Setup + Foundational → geometría fijada
2. Add User Story 1 → Test independiente → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independiente (matriz + legacy + 48dp) → Deploy/Demo
4. Polish → Lint + quickstart completo + PR pequeña

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- T004 es el único gate de cálculo; el resto es edición de recursos + verificación visual
- Commit tras T006 (vector), tras T009 (legacy) y al cierre (T014)
- Stop en cada checkpoint para validar la historia independientemente
