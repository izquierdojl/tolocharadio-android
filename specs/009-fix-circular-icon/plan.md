# Implementation Plan: Fix Circular Icon Crop

**Branch**: `009-fix-circular-icon` | **Date**: 2026-09-07 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/009-fix-circular-icon/spec.md`

## Summary

El foreground adaptativo (`drawable/ic_launcher_foreground.xml`) dibuja el emblema Sierra con escala x1.35, ocupando 10.8–97.2dp sobre el lienzo de 108dp y excediendo la zona segura circular (Ø 66dp, bandas 21–87dp). El plan es reencuadrar el emblema dentro de la zona segura (escala ~0.94, centrado), eliminar el rectángulo de fondo duplicado del foreground, y regenerar los raster legacy `mipmap-*/ic_launcher*.webp` desde el vector corregido. Sin cambios de paleta, sin código Kotlin, sin dependencias nuevas.

## Technical Context

**Language/Version**: Kotlin (código existente) + recursos vectoriales Android (AVD `vector drawable` 108dp); AGP según repo, `compileSdk` declarado en Gradle

**Primary Dependencies**: Ninguna nueva; solo recursos `res/` (ninguna librería)

**Storage**: N/A (cambio de recursos estáticos, sin Room/DataStore)

**Testing**: Verificación visual manual en launcher con máscara circular + matriz squircle/gota/cuadrada (SC-001–SC-003); `./gradlew assembleDebug` + Android Lint sobre `res/` como gate; sin tests unitarios/UI (sin lógica domain/data/ViewModel afectada)

**Target Platform**: Android `minSdk = 26`, `targetSdk = 37`; adaptive-icons (API 26+) + fallback legacy pre-O vía webp

**Project Type**: mobile-app (single-module `app`), fix de recursos

**Performance Goals**: N/A (recurso estático; sin impacto en arranque < 2s)

**Constraints**: Zona segura adaptive-icon Ø 66dp centrada (bandas 21–87dp en lienzo 108dp); mantener paleta pine-950 `#08100B` / ocre `#E2C091` / verde-bosque `#2C4F38`; `roundIcon` (`mipmap-anydpi/ic_launcher_round.xml`) y `monochrome` comparten el mismo foreground

**Scale/Scope**: 1 vector foreground + 1 vector background (revisión) + 10 webp legacy (5 densidades × launcher/round) + 2 xml adaptive (sin cambio, heredan fix); fix pequeño, 1 PR

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **I. MVVM + Clean por capas** — PASS. Cambio exclusivo en `res/` (drawables + mipmaps). Sin UI lógica, ViewModel, UseCase ni repositorio tocados. Sin Hilt ni Flow implicados.
- [x] **II. Stack Kotlin-First / minSdk 26 / null-safe** — PASS. Sin código nuevo. Se respeta `minSdk 26` (adaptive-icon es el mecanismo canónico API 26+); legacy webp cubre fallback. Sin dependencias nuevas que justificar.
- [x] **III. Calidad Test-First** — PASS con justificación. El principio exige tests para lógica `domain`/`data`/`ViewModel`; este fix no añade ni cambia ninguna (solo vectores). Gate aplicable: `assembleDebug` + Android Lint/Detekt/ktlint sin errores. Verificación visual manual documentada en `quickstart.md` (SC-001–SC-004). Sin Red-Green aplicable (no hay test unitario que reproduzca un recorte visual de recurso).
- [x] **IV. Streaming robusto** — PASS (no afectado: sin player, red ni errores de backend).
- [x] **V. Simplicidad Modular (YAGNI)** — PASS. Solución mínima: reescala/recentra + regenera legacy. Sin módulos, BaaS, caché ni DRM. Sin abstracciones nuevas.

**Post-design re-check (Phase 1)**: PASS — el diseño mantiene el cambio acotado a `res/`, sin código ni dependencias; veredicto sin cambios.

## Project Structure

### Documentation (this feature)

```text
specs/009-fix-circular-icon/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output — OMITIDO (recurso interno, sin interfaz externa; ver nota)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

> Nota contracts/: este fix no expone ni consume ninguna interfaz (sin API, CLI, eventos ni UI programática). Se omite `contracts/` con justificación en `research.md` (decisión R5).

### Source Code (repository root)

```text
app/src/main/
├── AndroidManifest.xml                      # ya declara android:icon + android:roundIcon (sin cambios)
└── res/
    ├── drawable/
    │   ├── ic_launcher_foreground.xml       # MODIFICAR: reencuadrar emblema en zona segura + quitar fondo duplicado
    │   └── ic_launcher_background.xml       # REVISAR (sin cambio esperado; fondo pine-950 a 108dp)
    ├── mipmap-anydpi/
    │   ├── ic_launcher.xml                  # sin cambio (referencia a los drawables)
    │   └── ic_launcher_round.xml            # sin cambio (idem)
    └── mipmap-mdpi|hdpi|xhdpi|xxhdpi|xxxhdpi/
        ├── ic_launcher.webp                 # REGENERAR desde vector corregido
        └── ic_launcher_round.webp           # REGENERAR desde vector corregido
```

**Structure Decision**: Single project existente (módulo único `app`). El fix vive íntegramente en `app/src/main/res`; `AndroidManifest.xml` ya referencia `icon`/`roundIcon` y no requiere cambios. Sin nuevos directorios de código.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — (ninguna) | — | — |
