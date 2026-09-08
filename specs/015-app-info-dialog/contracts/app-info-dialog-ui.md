# UI Contract: App Info Dialog

**Date**: 2026-09-08
**Feature**: 015-app-info-dialog

## Componente: AppInfoDialog

### Ubicación

En `SettingsScreen.kt`, como elemento al final de la columna de Configuración, y el diálogo modal como composable separado en `core/ui/components/AppInfoDialog.kt`.

### Disparador en SettingsScreen

Después del botón de "Cerrar sesión", añadir una fila clickable:

```
┌─────────────────────────────────────┐
│  Acerca de                    ℹ️   │
└─────────────────────────────────────┘
```

### Diálogo Modal (AlertDialog)

```
┌─────────────────────────────────────┐
│         Tolocha Radio               │  ← título (appName)
│                                     │
│  Versión: 1.2.3                     │
│  Desarrollador: izquierdojl         │
│  Licencia: MIT                      │
│                                     │
│  🔗 Ver repositorio                │  ← enlace clicable
│                                     │
│            [ Cerrar ]               │  ← botón de cierre
└─────────────────────────────────────┘
```

### Comportamiento

| Acción | Resultado |
|--------|-----------|
| Tocar "Acerca de" | Abre el AlertDialog con la información |
| Tocar "Ver repositorio" | Abre `https://github.com/izquierdojl/tolocharadio-android` en el navegador |
| Tocar "Cerrar" | Cierra el diálogo |
| Tocar fuera del diálogo | Cierra el diálogo (dismissOnBackPress = true, dismissOnClickOutside = true) |
| Rotar dispositivo | Diálogo permanece abierto con los mismos datos |

### Accessibility

- `contentDescription` del disparador: "Acerca de la aplicación"
- El enlace del repositorio DEBE ser anunciado como "Enlace" por TalkBack
- El botón "Cerrar" DEBE tener `contentDescription`: "Cerrar diálogo de información"

### States de la UI

| State | Descripción |
|-------|-------------|
| `AppInfoUiState.Hidden` | Diálogo no visible, SettingsScreen muestra solo el disparador |
| `AppInfoUiState.Showing(info)` | Diálogo visible con los datos de `AppInfo` |

### Interacciones

```
User taps "Acerca de" in SettingsScreen
  └─► Call settingsViewModel.showAppInfoDialog()
       └─► State changes to Showing(info)
            └─► AppInfoDialog renders

User taps "Ver repositorio" inside dialog
  └─► Open Intent.ACTION_VIEW with repositoryUrl
       └─► Dialog remains open (user may want to read other info)

User taps "Cerrar" or outside dialog
  └─► Call settingsViewModel.dismissAppInfoDialog()
       └─► State changes to Hidden
            └─► Dialog dismisses
```

### Tema

- Usa `AlertDialog` de Material3 con `MaterialTheme.colorScheme.surface` como fondo
- Título: `MaterialTheme.typography.headlineSmall`
- Contenido: `MaterialTheme.typography.bodyMedium`
- Enlace: `MaterialTheme.colorScheme.primary` con subrayado
- Botón "Cerrar": `TextButton` estándar de Material3
