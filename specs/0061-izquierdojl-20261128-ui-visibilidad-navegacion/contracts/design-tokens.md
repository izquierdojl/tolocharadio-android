# Design Tokens: Visibilidad UI y navegación

**Feature**: `0061-izquierdojl-20261128-ui-visibilidad-navegacion` | **Date**: 2026-11-28

Contrato de los tokens semánticos que introducen la separación visual. Los valores son la fuente única; ningún componente debe hardcodear estos colores fuera de `Theme.kt`/`TolochaColors.kt`.

## Tokens

| Token | Oscuro | Claro | Uso |
|-------|--------|-------|-----|
| `cardContainer` | `#24422E` | `#FFFFFF` | Fondo de `StationCard`, `HistoryGridCard`, `CustomStationGridCard` |
| `cardBorder` | `#3C6A4D` | `#C4D4C9` | Borde de 1 dp de tarjeta |
| `cardPlaceholder` | `#17291C` | `#D9E4DC` | Fondo del hueco de carátula sin favicon |
| `playerContainer` | `#1B3121` | `#F5F9F6` | Fondo de `NowPlayingContainer` |
| `playerBorder` | `#3C6A4D` | `#C4D4C9` | Línea superior de 1 dp del panel |
| `chromeDivider` | `#203A28` | `#D9E4DC` | Divisor bajo la cabecera y sobre la barra inferior |
| `navIndicator` | `#D3A568` | `#8A5F26` | Pill del destino seleccionado |
| `navSelectedContent` | `#08100B` | `#FFFFFF` | Icono dentro del pill seleccionado |
| `navUnselectedContent` | `#8CB29A` | `#4C6B58` | Icono/etiqueta no seleccionados |

La etiqueta seleccionada usa `onSurface` (no `navSelectedContent`) porque el pill no está detrás del texto; se refuerza con semibold.

`isDark: Boolean` acompaña a los tokens para elegir elevación de tarjeta (0 dp en oscuro, 2 dp en claro) sin consultar el color.

## Roles M3 ajustados

| Rol | Oscuro | Claro | Motivo |
|-----|--------|-------|--------|
| `onSurfaceVariant` | `Pine300` `#8CB29A` | `LightMuted` `#4C6B58` | Chips y texto secundario ≥4.5:1 |
| `outlineVariant` | sin cambios (`Pine800`) | sin cambios (`LightLine`) | Divisores no críticos |
| `surfaceVariant` | sin cambios (`Pine800`) | sin cambios (`LightLine`) | Fondo de chip |

## Contraste mínimo

| Par | Oscuro | Claro | Objetivo |
|-----|--------|-------|----------|
| Texto secundario / tarjeta | 4.73:1 | 5.91:1 | ≥4.5 |
| Texto de chip / chip | 6.53:1 | 4.53:1 | ≥4.5 |
| Borde / tarjeta | 1.77:1 | 1.54:1 | ≥1.5 |
| Tarjeta / fondo | 1.74:1 | 1.12:1 + elevación | ≥1.3 en oscuro |
| Borde / panel | 2.23:1 | 1.45:1 | separación visual |
| Indicador nav / barra | 8.59:1 | 5.61:1 | ≥3 |
| Contenido seleccionado / indicador | 8.59:1 | 5.61:1 | ≥4.5 |

El 3:1 se exige a **controles y estados esenciales** (indicador de navegación, play/pausa). Los bordes de tarjeta y panel son decorativos y se rigen por SC-002: ≥1.5:1 en tarjeta y ≥1.4:1 en panel; no se computan como controles esenciales.

## Reglas de uso

1. **Tarjeta**: `cardContainer` + `cardBorder` de 1 dp; en claro se añade `CardDefaults.cardElevation(defaultElevation = 2.dp)`. No se usa `surfaceContainer` para tarjetas.
2. **Chip**: mantiene `surfaceVariant` y usa `onSurfaceVariant`; no se crea un token de chip salvo que el rol cambie.
3. **Panel**: `playerContainer` explícito; `tonalElevation = 0.dp`; borde superior `playerBorder`; forma `RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)`.
4. **Barra inferior**: `navIndicator` + `navSelectedContent`; el no seleccionado usa `navUnselectedContent`; la etiqueta seleccionada va en semibold.
5. **Chrome**: `chromeDivider` de 1 dp bajo la cabecera y sobre la barra; no se cambia la altura de la cabecera ni de la barra.
6. **Modo claro**: no se usan colores fijos de oscuro (por ejemplo, `Pine800`) en placeholders ni fondos; se usan los tokens claros.
7. **Accesibilidad**: ningún texto normal por debajo de 4.5:1; ningún estado esencial por debajo de 3:1; el color nunca es la única señal (icono, peso o texto acompañan).

## Tests de contrato

`ContrastTest` debe cubrir, como mínimo:

- `onSurfaceVariant` / `surfaceVariant` en oscuro y claro.
- `onSurfaceVariant` / `cardContainer` en oscuro y claro.
- `cardBorder` / `cardContainer` en oscuro y claro.
- `cardContainer` / `surface` en oscuro.
- `navSelectedContent` / `navIndicator` en oscuro y claro.
- `navIndicator` / `surfaceContainer` en oscuro y claro.
