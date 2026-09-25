# Bug Assessment: La barra de navegación inferior solo muestra "Explorar"

- **Slug**: 0040-jlizquierdo-20260925-bottom-nav-solo-explorar
- **Created**: 2026-09-25
- **Source**: pasted text (reporte del usuario + captura de pantalla)
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

> Revisa un problema con la barra de navegación. Con las últimas actualizaciones
> solo se ve el botón de "explorar" y han quedado escondidos los botones de
> "Favoritos", "Historial...". Mira la imagen adjunta.

La captura muestra la zona inferior de la app: fondo claro, **un único icono**
(lupa verde, "Explorar") **centrado horizontalmente** sobre toda la barra y la
píldora de gestos del sistema debajo. No se ve ninguno de los otros cuatro
destinos (Favoritos, Historial, Mis emisoras, Configuración).

## Symptom

La `NavigationBar` inferior renderiza únicamente el primer destino ("Explorar")
ocupando todo el ancho, y los demás quedan con ancho 0 (invisibles). Se espera
ver los **cinco** iconos distribuidos uniformemente. Es una regresión reciente:
la barra funcionaba antes de las últimas actualizaciones.

## Reproduction

1. Abrir la app (con servidor configurado, llega a Explorar).
2. Observar la barra de navegación inferior.
3. Resultado observado: solo el icono de Explorar, centrado y a ancho completo;
   Favoritos/Historial/Mis emisoras/Configuración no aparecen ni son pulsables.
4. Resultado esperado: los 5 iconos con el mismo ancho, cada uno navegable.

[NEEDS CLARIFICATION: ¿ocurría ya en la versión instalada antes del commit
`82b3995` (converge, 2026-09-24) o empezó con él? La evidencia de código apunta
a ese commit como origen.]

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt:275-304`
  — `NavigationBar { BOTTOM_DESTS.forEach { TooltipBox { NavigationBarItem(...) } } }`.
  El `NavigationBarItem` dejó de ser hijo directo del `Row` de `NavigationBar`:
  ahora lo envuelve un `TooltipBox`.
- Material3 1.4.0 (`androidx.compose.material3:material3`, source
  `NavigationBar.kt:196,255,617-645`) — `RowScope.NavigationBarItem` aplica
  `Modifier.weight(1f)` a su `Box` raíz y su layout (`placeIcon`) usa
  `constraints.maxWidth` como ancho cuando no es infinito.
- `gradle/libs.versions.toml:8` — `composeBom = "2026.05.00"` → material3
  `1.4.0` (resuelto en el BOM). El commit `7be5ee9` (spec 0037) subió el BOM desde
  `2025.01.00`.
- `82b3995` (`feat: converge 0016/0034/013/012 — ... tooltip ...`) — introdujo el
  `TooltipBox` alrededor de `NavigationBarItem` (spec 012, T016). Antes de este
  commit el `NavigationBarItem` era hijo directo del `Row`.
- `specs/012-bottom-nav-icons-only/tasks.md` (T004) — la implementación del
  tooltip se había aparcado por incompatibilidad de `TooltipBox` con el BOM
  `2025.01.00`; el BOM `2026.05.00` la desbloqueó y el converge la añadió.

## Root Cause Hypothesis

**Confianza: alta.** `TooltipBox` no es `RowScope` y su `content` es
`@Composable () -> Unit`; al envolver `NavigationBarItem`, la llamada sigue
resolviéndose contra el receptor `RowScope` externo de `NavigationBar`, pero el
`Modifier.weight(1f)` interno se aplica al `Box` del item, que ya **no** es hijo
directo del `Row` (el hijo directo es el `Box` interno de `TooltipBox`). El
`Row` ignora el `weight` y mide el primer `TooltipBox` con el ancho restante
completo; `placeIcon` toma `constraints.maxWidth` como ancho del item, de modo
que el primer destino llena toda la barra y los siguientes reciben `maxWidth = 0`
y quedan invisibles. El icono se ve centrado precisamente porque el primer item
ocupa todo el ancho. Nota: pasar `Modifier.weight(1f)` al `TooltipBox` **no**
arreglaría el problema, porque `BasicTooltipBox` aplica ese `modifier` a un `Box`
interno (`WrappedAnchor`), no a su nodo raíz.

## Proposed Remediation

**Preferred**: restaurar un hijo directo con `weight` envolviendo el `TooltipBox`
en un `Box(Modifier.weight(1f))` dentro del `forEach` de
`TolochaNavGraph.kt`:

```kotlin
NavigationBar {
    BOTTOM_DESTS.forEach { dest ->
        val tooltipState = rememberTooltipState()
        Box(Modifier.weight(1f)) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Above,
                ),
                tooltip = { PlainTooltip { Text(dest.label) } },
                state = tooltipState,
            ) {
                NavigationBarItem(
                    selected = currentRoute == dest.route ||
                        (dest.route == Routes.EXPLORE && currentRoute == Routes.STATION_DETAIL),
                    onClick = { /* sin cambios */ },
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                )
            }
        }
    }
}
```

Cambio mínimo (un `Box` por item + import `androidx.compose.foundation.layout.Box`),
mantiene el tooltip de accesibilidad (spec 012 US1/AC3) y no toca el `Scaffold`
externo ni el `NavHost`. Alternativa válida: dar `Modifier.fillMaxWidth()` al
`NavigationBarItem` dentro del `Box` con `weight` (redundante si el `Box` ya
ocupa el slot).

**Alternatives** (optional):
- **Revertir el tooltip (T016)**: quitar el `TooltipBox` y dejar
  `NavigationBarItem` como hijo directo (más simple, recupera el layout
  original). El tooltip era un requisito de accesibilidad *nice-to-have* de 012;
  el texto ya está en `contentDescription`, así que TalkBack sigue funcionando.
  Coste: se pierde el tooltip al mantener pulsado de spec 012 AC3.
- **Extraer la barra a un composable `internal`** (`TolochaBottomBar`) y aplicar
  los `weight`/tooltips ahí; facilita testear la producción sin Hilt (ver Tests).

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/navigation/TolochaNavGraph.kt`
- `app/src/androidTest/java/com/izquierdojl/tolocharadio/core/ui/navigation/BottomNavBarTest.kt` (nuevo) o extensión de `CommonUiTest.kt`

**Tests to add or update**:
- Test de UI Compose (androidTest) que componga la barra real (idealmente vía un
  `TolochaBottomBar` `internal` extraído, o el mismo patrón `NavigationBar` +
  `Box(weight){TooltipBox{NavigationBarItem}}` si se prefiere no extraer) y
  verifique: (a) existen y se muestran los 5 nodos por `contentDescription`
  ("Explorar", "Favoritos", "Historial", "Mis emisoras", "Configuración"); (b)
  los cinco tienen ancho comparable (≈ ancho del contenedor / 5, tolerancia), y
  en particular **ninguno con ancho 0**. Esto fija la no-regresión del `weight`.
- Reutilizar el enfoque de `SectionHeaderSpacingTest` (compone el patrón y mide
  en píxeles) para no depender de Hilt.

## Risks & Considerations

- **Anclaje del tooltip**: al envolver en `Box(weight)`, el tooltip se ancla al
  slot completo en vez de al icono; visualmente aceptable (posición `Above`).
- **Gestos**: el `TooltipBox` consume el long-press; verificar que el tap normal
  sigue navegando (ya funcionaba así antes del fix).
- **Cobertura**: no hay test de UI de la barra inferior; el fallo pasó los gates
  porque los tests de navegación son unitarios (`RoutesTest`).
- **Otras instancias**: revisar si el patrón `TooltipBox { NavigationBarItem }`
  se repite en otro sitio (no se ha encontrado; solo en `TolochaNavGraph`).

## Open Questions

- [NEEDS CLARIFICATION: ¿se confirma que la regresión entró con `82b3995` y no
  antes? En dispositivo: instalar la build previa al commit y comprobar la barra.]
- [NEEDS CLARIFICATION: ¿se quiere conservar el tooltip al mantener pulsado
  (elegir Preferred) o basta con `contentDescription` (elegir revertir)?]
