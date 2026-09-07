# Research: Fix Circular Icon Crop (009-fix-circular-icon)

**Fecha**: 2026-09-07 — **Fuente**: inspección directa de `app/src/main/res` + `AndroidManifest.xml` + `app/build.gradle.kts`. Sin NEEDS CLARIFICATION abiertos (Technical Context completo).

## R1. Geometría del recorte y factor de corrección

- **Decision**: El emblema actual excede la zona segura; reencuadrarlo con escala ≈ 0.94 y centrado (grupo `translate ≈ 24dp`, `scale ≈ 0.9375` sobre el viewBox 64), de modo que el contenido quede aprox. en 24–84dp (dentro de las bandas 21–87dp de la zona segura Ø 66dp).
- **Rationale**: Medición sobre `ic_launcher_foreground.xml`: grupo `translate 10.8 / scale 1.35` → emblema ocupa 10.8–97.2dp. Sol (centro y≈22.3, borde sup. ≈13.5), punto de antena (borde sup. ≈15.5) y base de la sierra (y≈97.2) quedan fuera de 21–87dp → recorte circular confirmado en los tres puntos reportados. Con escala 60/64 = 0.9375 y translate (108−60)/2 = 24, el contenido (y≈3.5–64 en coords. locales) cae en ≈27.3–84dp: todo dentro con margen de ~6dp al borde de recorte.
- **Alternatives considered**:
  - Escala 1.0 centrada (translate 22): contenido en ≈25.5–86dp — válido pero con solo ~1dp de margen inferior; rechazada por fragilidad ante stroke width.
  - Redibujar el emblema (simplificar antena/sol): rechazada — viola FR-002 / supuesto "sin rediseño".
  - `android:inset` en el adaptive-icon XML: API disponible pero menos portable que corregir el vector raíz (el inset no arregla los webp legacy); rechazada.

## R2. Rectángulo de fondo duplicado en el foreground

- **Decision**: Eliminar el `<path>` de fondo `#08100B` a pantalla completa del `ic_launcher_foreground.xml`; el fondo lo provee la capa `background` (`ic_launcher_background.xml`).
- **Rationale**: Best practice de adaptive-icons: el foreground debe ser transparente salvo el emblema; el rect duplicado impide previsualizar correctamente capas y puede interferir con el `monochrome` (la capa mono debe ser solo silueta). El `background` ya pinta `#08100B` a 108dp, por lo que quitarlo es visualmente neutro en launcher normal y más correcto en themed icons.
- **Alternatives considered**: Mantener el rect (cambio mínimo de una línea menos) — rechazada porque perpetúa el problema de themed-icon y dificulta validar la zona segura por capas.

## R3. Regeneración de los raster legacy (`mipmap-*/ic_launcher*.webp`)

- **Decision**: Regenerar los 10 webp (5 densidades × launcher/round) con Android Studio **Image Asset Studio** (tipo Launcher Icons Adaptive + Legacy) a partir del foreground/background corregidos; fallback aceptable: exportar el vector compuesto a PNG por densidad y convertir a webp con calidad equivalente.
- **Rationale**: Los dispositivos/launchers pre-O o que ignoran `mipmap-anydpi` usan estos webp; si no se regeneran, el bug persiste en esos terminales (edge case de la spec). Image Asset Studio aplica el escalado por densidad correcto (mdpi 48px → xxxhdpi 192px) sin cálculo manual.
- **Alternatives considered**: Editar los webp a mano / reescalar por script — rechazada (riesgo de densidades inconsistentes y artefactos); eliminar los webp y confiar solo en adaptive — rechazada (rompe fallback pre-O aunque minSdk sea 26, launchers de terceros pueden usarlos).

## R4. Variante monocromática (themed icon)

- **Decision**: Sin archivo nuevo: al compartir drawable con el foreground, hereda la corrección. Verificar que la silueta a un solo tono siga legible (sierra + sol distinguibles) en el check visual.
- **Rationale**: `mipmap-anydpi/ic_launcher*.xml` declaran `<monochrome android:drawable="@drawable/ic_launcher_foreground" />`; tras R1+R2 el mono queda automáticamente dentro de zona segura y sin fondo parásito.
- **Alternatives considered**: Crear `ic_launcher_monochrome.xml` simplificado — rechazada por YAGNI (solo si el check visual muestra pérdida de legibilidad a un tono).

## R5. Contratos / data-model / tests automatizados

- **Decision**: Omitir `contracts/` (sin interfaz externa: ni API, ni CLI, ni eventos); `data-model.md` describe recursos (no entidades de dominio/BD); sin tests unitarios ni de UI — gates: `assembleDebug` + Lint + verificación visual de `quickstart.md`.
- **Rationale**: Principio III exige tests para lógica domain/data/ViewModel; aquí no hay ninguna. Un screenshot-test del icono sería frágil (depende del launcher del device) y su coste supera el beneficio para un fix de recurso; la matriz visual manual (SC-001–SC-003) es la verificación proporcional según YAGNI (principio V).
- **Alternatives considered**: Añadir test de composición que aserte bounding-box del vector dentro de 21–87dp parseando el XML — considerado como mejora opcional de bajo coste, pero se deja fuera del plan base (puede proponerse en tasks como estiramiento, no como gate).
