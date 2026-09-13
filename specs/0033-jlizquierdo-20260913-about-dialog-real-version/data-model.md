# Data Model: Diálogo "Acerca de" con versión real y detalles ampliados

**Date**: 2026-09-13
**Feature**: 0033-jlizquierdo-20260913-about-dialog-real-version

## Entidades

### AppBuildInfo (value object — metadatos del build)

Datos estáticos de compilación, provistos por Hilt desde `BuildConfig`. Inmutable.

```
AppBuildInfo
├── appName: String            # Nombre visible ("Tolocha Radio")
├── versionName: String        # BuildConfig.VERSION_NAME (versión real)
├── versionCode: Long          # BuildConfig.VERSION_CODE (nº de compilación)
├── applicationId: String      # BuildConfig.APPLICATION_ID (identificador de la app)
├── buildType: String          # "Depuración" | "Publicación" (derivado de BuildConfig.DEBUG)
├── repositoryUrl: String      # URL del repositorio
├── developer: String          # Desarrollador
└── license: String            # Licencia
```

**Validaciones**:
- `versionName` no vacío; si el build no lo define, el proveedor cae al valor por defecto del proyecto.
- `versionCode` no negativo.
- `applicationId` no vacío.
- `buildType` ∈ {"Depuración", "Publicación"}.
- `repositoryUrl` es una URL válida.

### AppInfo (data class — datos del diálogo)

Datos ya resueltos que el diálogo muestra y copia. Combina `AppBuildInfo` + configuración activa.

```
AppInfo
├── appName: String
├── version: String               # = AppBuildInfo.versionName (versión real del build)
├── versionCode: Long             # = AppBuildInfo.versionCode
├── applicationId: String         # = AppBuildInfo.applicationId
├── buildType: String             # = AppBuildInfo.buildType
├── developer: String
├── license: String
├── repositoryUrl: String
├── theme: String                 # Etiqueta del tema activo ("Sistema"|"Claro"|"Oscuro")
├── startScreen: String           # Etiqueta de la pantalla de arranque ("Explorar"|"Favoritos"|"Historial")
└── activeServerAlias: String?    # Alias del servidor activo; null si no hay servidor
```

**Validaciones**:
- `version` NO DEBE estar codificada de forma fija; DEBE proceder de `AppBuildInfo`.
- `theme` y `startScreen` DEBEN ser etiquetas legibles (no el nombre del enum).
- `activeServerAlias` puede ser `null`; la UI lo omite o muestra "Sin servidor".
- Ningún campo puede contener credenciales, tokens ni datos personales (FR-009).

**Formato copiable** (`toClipboardText()` — función pura):

```
Tolocha Radio
Versión: 2.3.1
Número de compilación: 42
Identificador: com.izquierdojl.tolocharadio
Tipo de build: Publicación
Tema: Oscuro
Pantalla de arranque: Favoritos
Servidor activo: Mi servidor
Desarrollador: izquierdojl
Licencia: MIT
Repositorio: https://github.com/izquierdojl/tolocharadio-android
```

`activeServerAlias == null` omite la línea "Servidor activo". El formato es estable y testeable.

### AppInfoUiState (sellado)

Estado de la UI del diálogo (se mantiene de la spec 015).

```
AppInfoUiState
├── Hidden                         # Diálogo no visible
└── Showing(info: AppInfo)         # Diálogo visible con datos ya compuestos
```

**Transiciones de estado**:

```
Hidden  ──[showAppInfoDialog]──►  Showing(info resuelto)
Showing ──[dismissAppInfoDialog]──► Hidden
Showing ──[onCopyResult(true)]──► Showing + messages("Información copiada")
Showing ──[onCopyResult(false)]──► Showing + messages("No se pudo copiar la información")
```

## Relaciones

```
SettingsViewModel
├── injects: InstancePrefs        (themeMode, startScreen)
├── injects: GetServersUseCase    (alias del servidor activo)
├── injects: AppBuildInfo         (metadatos del build vía Hilt)
├── provides: SettingsUi          (themeMode, startScreen, activeServerAlias)
├── provides: AppInfoUiState      (estado del diálogo)
├── provides: messages: SharedFlow<String>  (resultado del copiado)
├── triggers: showAppInfoDialog() (compone AppInfo y pasa a Showing)
├── triggers: dismissAppInfoDialog()
└── triggers: onCopyResult(success)

SettingsScreen (Compose)
├── observes: SettingsUi / AppInfoUiState / messages
├── holds: SnackbarHostState (dentro de la ventana del diálogo)
└── calls: AppInfoDialog(info, onDismiss, onCopy, snackbar)

AppInfoDialog (Compose)
├── renders: campos de AppInfo
├── copies: AppInfo.toClipboardText() al portapapeles + onCopyResult(success)
└── calls: onDismiss()
```

## Persistencia

**Ninguna nueva**. `AppInfo` se compone en memoria a partir de `BuildConfig` (estático), `InstancePrefs` (DataStore existente) y `GetServersUseCase` (Room existente). El diálogo no escribe nada.

## Consideraciones de threading

- `SettingsViewModel` emite `AppInfoUiState` y `messages` en `Dispatchers.Main` (scope del ViewModel).
- `GetServersUseCase()` es un `Flow` de Room; se recoge en `viewModelScope`.
- El copiado al portapapeles y el snackbar ocurren en la UI; no requieren dispatcher propio.
