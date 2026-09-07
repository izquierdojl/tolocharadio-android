# Data Model: Fix Circular Icon Crop (009-fix-circular-icon)

> Alcance: este feature no toca dominio/BD/red. Las "entidades" son recursos `res/` (capas del adaptive-icon). Se documentan aquí para fijar vocabulario y reglas de validación del fix.

## Entidades (recursos)

### 1. AdaptiveIcon (compuesto)

- **Qué es**: icono principal declarado en `mipmap-anydpi/ic_launcher.xml` e `ic_launcher_round.xml`; elManifest lo referencia vía `android:icon` / `android:roundIcon`.
- **Atributos**: `background` → `drawable/ic_launcher_background`; `foreground` → `drawable/ic_launcher_foreground`; `monochrome` → mismo foreground.
- **Relaciones**: 1 AdaptiveIcon compone 1 Background + 1 Foreground (+ vista mono derivada).
- **Reglas de validación**:
  - Ambas capas en lienzo 108dp, viewport 108×108.
  - El contenido significativo del foreground MUST inscribirse en la zona segura Ø 66dp (bandas 21–87dp).
  - `ic_launcher.xml` e `ic_launcher_round.xml` MUST referenciar los mismos drawables (sin divergencias).

### 2. IconBackground (capa)

- **Qué es**: `drawable/ic_launcher_background.xml` — fondo plano pine-950.
- **Atributos**: `fillColor = #08100B`, rect 0,0–108,108 a pantalla completa.
- **Reglas**: color inmutable en este fix; debe cubrir todo el lienzo (sin transparencias).

### 3. IconForeground — Emblema Sierra (capa)

- **Qué es**: `drawable/ic_launcher_foreground.xml` — sol + sierra + antena sobre fondo transparente (tras R2).
- **Atributos**:
  - `sol`: círculo ocre `#E2C091` (local: centro ≈ (16, 8.5), r ≈ 6.5).
  - `sierra`: polígono verde-bosque `#2C4F38` (local: base y = 64, picos hasta y ≈ 26).
  - `antena`: trazo ocre `#E2C091` ancho 3 + punto r ≈ 3 (local: x = 40, y 6.5–27).
  - Transformación objetivo: `scale ≈ 0.9375`, `translate ≈ (24, 24)` (valores finales los fija la implementación dentro de la tolerancia de la regla siguiente).
- **Reglas de validación**:
  - Bounding-box del emblema (incluido `strokeWidth` y radio del punto) MUST quedar dentro de 21–87dp en ambos ejes, con margen ≥ 2dp al borde.
  - Paleta MUST preservarse (`#E2C091`, `#2C4F38`); fondo del foreground MUST ser transparente.
  - Legible a 48dp: sol visible, sierra distinguible (FR-005).

### 4. LegacyRaster (derivado)

- **Qué es**: `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher{,_round}.webp` — fallback pre-O.
- **Reglas**: MUST regenerarse desde el vector corregido (misma composición, padding y paleta); 10 ficheros consistentes entre densidades y entre variantes launcher/round.

## Sin transiciones de estado / sin identidad de dominio

No hay ciclos de vida, unicidad ni volúmenes: artefactos estáticos versionados en git. Trazabilidad spec→recurso: FR-001→IconForeground.bounds, FR-002→paleta, FR-003→AdaptiveIcon.refs + mono, FR-004→LegacyRaster, FR-005→legibilidad 48dp.
