# UI Contract: SectionHeader

**Feature**: 012-bottom-nav-icons-only | **Date**: 2026-09-07

## Componente: `SectionHeader`

### Signature

```kotlin
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
)
```

### Comportamiento

| Entrada | Salida visual |
|---------|---------------|
| `title = "Explorar"`, `subtitle = null` | Texto "Explorar" en `headlineSmall`, sin subtítulo |
| `title = "Tu historial"`, `subtitle = "Lo último..."` | Texto "Tu historial" en `headlineSmall` + subtítulo en `bodySmall` con `onSurfaceVariant` |
| `title = ""` | Error de validación (no renderizar) |

### Padding

- Horizontal: `16.dp`
- Vertical: `8.dp`
- Aplicado al `Column` contenedor, no al `Text` individual

### Uso en cada pantalla

| Pantalla | Título | Subtítulo |
|----------|--------|-----------|
| ExploreScreen | "Explorar" | `null` |
| FavoritesScreen | "Tus favoritos" | `null` |
| HistoryScreen | "Tu historial" | "Lo último que has escuchado." |
| CustomStationsScreen | "Mis emisoras" | "Añade emisoras que no están en el catálogo para escucharlas desde el reproductor." |
| SettingsScreen | "Configuración" | `null` |

---

## Componente: NavigationBar (modificación)

### Comportamiento actual

```kotlin
NavigationBarItem(
    selected = ...,
    onClick = ...,
    icon = { Icon(dest.icon, contentDescription = dest.label) },
    label = { Text(dest.label) },  // ← ELIMINAR
)
```

### Comportamiento nuevo

```kotlin
TooltipBox(
    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
    tooltip = { PlainTooltip { Text(dest.label) } },
    state = rememberPlainTooltipState(),
) {
    NavigationBarItem(
        selected = ...,
        onClick = ...,
        icon = { Icon(dest.icon, contentDescription = dest.label) },
        // label eliminado
    )
}
```

### Accesibilidad

- `contentDescription` del `Icon` se mantiene con el texto del label para TalkBack.
- Tooltip visible al pulsación larga para usuarios sighted.
