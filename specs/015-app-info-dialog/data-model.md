# Data Model: Diálogo de información de la aplicación

**Date**: 2026-09-08
**Feature**: 015-app-info-dialog

## Entidades

### AppInfo (data class)

Información general de la aplicación para mostrar en el diálogo. Inmutable y estática.

```
AppInfo
├── appName: String              # Nombre de la aplicación
├── version: String              # Número de versión (versionName del build)
├── repositoryUrl: String        # URL del repositorio (https://github.com/izquierdojl/tolocharadio-android)
├── developer: String            # Nombre del desarrollador
└── license: String              # Tipo de licencia (ej: "MIT", "GPL-3.0")
```

**Validaciones**:
- `appName` NO DEBE estar vacío
- `version` NO DEBE estar vacío; fallback a "1.0" si no disponible
- `repositoryUrl` DEBE ser una URL válida
- `developer` NO DEBE estar vacío
- `license` NO DEBE estar vacío

### AppInfoUiState (sellado)

Estado de la UI del diálogo de información.

```
AppInfoUiState
├── Hidden                         # Diálogo no visible
└── Showing(info: AppInfo)         # Diálogo visible con datos
```

**Transiciones de estado**:

```
Hidden ──[showDialog]──► Showing
Showing ──[dismiss]──► Hidden
Showing ──[openUrl]──► Showing (abre navegador, diálogo permanece)
```

## Relaciones

```
SettingsViewModel
├── provides: AppInfoUiState      (estado del diálogo)
├── triggers: showDialog()        (muestra el diálogo)
└── triggers: dismiss()           (cierra el diálogo)

AppInfoDialog (Composable)
├── observes: AppInfoUiState      (del ViewModel)
├── calls: openUrl(url)           (Intent.ACTION_VIEW)
└── calls: onDismiss()            (cierra el diálogo)
```

## Persistencia

**Ninguna**. Los datos de `AppInfo` son estáticos y se derivan del build/constantes. No se almacenan en base de datos ni DataStore.

## Consideraciones de threading

- `AppInfoUiState` se emite en `Dispatchers.Main` (StateFlow del ViewModel)
- La apertura de URL via Intent no requiere threading especial (lo maneja el sistema)
