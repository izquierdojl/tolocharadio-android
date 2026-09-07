# Feature Specification: Fix Circular Icon Crop

**Feature Branch**: `009-fix-circular-icon`

**Created**: 2026-09-07

**Status**: Done (2026-09-07; todo verificado en emulador)

**Input**: User description: "Haz un pequeño Fix. El icono principal de la aplicación, se ve cortado si el icono de Android es circular. Ajústalo para que el sol o la antena se vean correctamente."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Icono legible con máscara circular (Priority: P1)

Un usuario con un launcher que aplica máscara circular (o "roundIcon") ve el icono de TolochaRadio en el cajón de apps o en la pantalla de inicio. El sol, la sierra y la antena se ven completos, sin recortes, y el icono sigue siendo reconocible.

**Why this priority**: Es el bug reportado directamente. El icono es la identidad de la app; si se ve cortado transmite dejadez y rompe la marca Tema Tolocha.

**Independent Test**: Instalar la app en un dispositivo/emulador con iconos circulares (p. ej. Pixel Launcher con forma circular) y comprobar visualmente que sol y antena no se recortan. Entrega valor por sí sola sin ningún otro cambio.

**Acceptance Scenarios**:

1. **Given** un launcher con máscara circular, **When** el usuario abre el cajón de apps, **Then** el emblema completo (sol, sierra y antena con su punto) es visible dentro del círculo sin recortes.
2. **Given** un launcher con máscara circular, **When** el usuario mira el icono en pantalla de inicio y en ajustes del sistema, **Then** no hay bordes del emblema tocando o saliendo del círculo.

---

### User Story 2 - Icono consistente en resto de formas (Priority: P2)

Un usuario con máscara cuadrada redondeada (squircle), gota o cuadrada ve el mismo icono sin regresión: sigue centrado, con márgenes equilibrados y colores pine-950 / verde-bosque / ocre.

**Why this priority**: El fix no debe romper las formas que hoy se ven bien. Garantiza que el ajuste de zona segura funciona en todas las máscaras adaptativas.

**Independent Test**: Cambiar la forma de icono del sistema (ajustes de fondo/estilo o launchers distintos) y comprobar que el icono se ve centrado y sin deformaciones en cada forma.

**Acceptance Scenarios**:

1. **Given** un launcher con máscara squircle, **When** el usuario ve el icono, **Then** el emblema aparece centrado con aire visual uniforme y sin recortes.
2. **Given** la variante monocromática (themed icon), **When** el usuario activa iconos con tema, **Then** la silueta sigue siendo reconocible y sin recortes.

### Edge Cases

- ¿Qué ocurre en launchers antiguos que usan el PNG legacy (`mipmap-*/ic_launcher.webp`) en lugar del adaptive-icon? El fix debe cubrir también esos recursos o regenerarlos desde el vector corregido.
- ¿Cómo se comporta el icono en densidades mdpi a xxxhdpi? El recorte debe desaparecer en todas.
- ¿Qué ocurre con el icono monocromático (`android:drawable monochrome`) que reutiliza el mismo foreground? Debe heredar la corrección automáticamente.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El emblema del icono (sol + sierra + antena) MUST quedar inscrito íntegramente dentro de la zona segura circular del adaptive-icon (diámetro central de 66dp sobre lienzo de 108dp), sin que ningún trazo toque el borde de recorte.
- **FR-002**: El icono MUST mantener fondo pine-950 (`#08100B`) y colores de emblema existentes (ocre `#E2C091`, verde-bosque `#2C4F38`) salvo ajustes mínimos de escala/posición necesarios para el encaje.
- **FR-003**: El mismo foreground corregido MUST aplicarse a `ic_launcher.xml` e `ic_launcher_round.xml` (ambos en `mipmap-anydpi-v26`) y a la variante monocromática si comparte drawable.
- **FR-004**: Los recursos legacy rasterizados (`mipmap-*/ic_launcher*.webp`) MUST regenerarse o ajustarse para que reflejen el vector corregido, de modo que dispositivos sin adaptive-icon tampoco muestren recorte.
- **FR-005**: El icono MUST seguir siendo reconocible a tamaño pequeño (48dp y vista de notificación/ajustes): silueta de sierra legible y sol visible.

### Key Entities

- **Launcher icon (adaptive)**: icono principal de la app compuesto por fondo (color pine-950) y primer plano (emblema sol + sierra + antena); vive en `mipmap-anydpi` + drawables `ic_launcher_foreground/background`.
- **Emblema Sierra**: marca Tema Tolocha (sol ocre, sierra verde-bosque, antena ocre con punto); es el elemento que hoy se recorta en máscara circular.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Con máscara circular del sistema, el 100% del emblema (sol, sierra y antena con punto) es visible sin recortes en inspección visual en al menos 2 densidades distintas.
- **SC-002**: El icono corregido se verifica sin recortes en al menos 3 formas de máscara (circular, squircle y cuadrada) en revisión visual.
- **SC-003**: Cero regresiones reportadas en el resto de formas de icono tras el cambio (verificación visual pasa al 100% en la matriz circular/squircle/gota/cuadrada).
- **SC-004**: El tiempo de verificación visual completa del icono (instalar y revisar en launcher circular + resto de formas) lleva menos de 10 minutos.

## Assumptions

- El problema está en el `ic_launcher_foreground.xml` actual: emblema escalado x1.35 que excede la zona segura circular; basta reducir escala y/o recentrar dentro de los 66dp centrales.
- No se rediseña la marca: solo ajuste de escala/posición/márgenes, sin cambiar formas ni paleta.
- El dispositivo objetivo usa adaptive icons (API 26+, minSdk 26 según constitución), por lo que el fix se centra en el vector adaptive más regeneración de PNG legacy.
- La validación es visual/manual sobre launcher real o emulador; no requiere test automatizado salvo captura de referencia si el equipo lo considera útil.
