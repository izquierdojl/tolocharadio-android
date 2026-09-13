# Contrato: `SettingsViewModel` — información de la aplicación

**Feature**: 0033-jlizquierdo-20260913-about-dialog-real-version
**Tipo**: Contrato de ViewModel (MVVM). Define el comportamiento observable que los tests deben cubrir.

## Dependencias inyectadas

| Dependencia | Rol |
|-------------|-----|
| `InstancePrefs` | Flujos `themeMode` y `startScreen` |
| `GetServersUseCase` | Lista de servidores; se deriva el activo |
| `AppBuildInfo` | Metadatos del build (Hilt) |

## Estado expuesto

| Nombre | Tipo | Descripción |
|--------|------|-------------|
| `ui` | `StateFlow<SettingsUi>` | Incluye `themeMode`, `startScreen` y `activeServerAlias: String?` |
| `appInfoUiState` | `StateFlow<AppInfoUiState>` | `Hidden` o `Showing(info)` |
| `messages` | `SharedFlow<String>` | Avisos one-shot para snackbar (copiado correcto/fallido) |

## Operaciones

### `showAppInfoDialog()`

- Compone `AppInfo` con:
  - `AppBuildInfo` (nombre, versión real, nº compilación, identificador, tipo de build, repo, desarrollador, licencia),
  - etiquetas del tema y pantalla de arranque actuales,
  - `activeServerAlias` actual (o `null`).
- Transiciona `appInfoUiState` a `Showing(info)`.
- **Contrato**: el `version` de `AppInfo` DEBE ser `AppBuildInfo.versionName` (nunca `"1.0"` fijo).

### `dismissAppInfoDialog()`

- Transiciona `appInfoUiState` a `Hidden`.

### `onCopyResult(success: Boolean)`

- Emite en `messages`:
  - `success == true` → `"Información copiada"`.
  - `success == false` → `"No se pudo copiar la información"`.
- No cambia `appInfoUiState` (el diálogo permanece abierto).

## Invariantes

1. `AppInfo` nunca contiene credenciales, tokens ni PII (FR-009/SC-006).
2. `version` siempre procede de `AppBuildInfo.versionName` (FR-001).
3. `activeServerAlias` refleja el servidor con `isActive = true`; `null` si no hay ninguno.
4. `onCopyResult` nunca cierra el diálogo (SC-007).
5. La configuración mostrada (`theme`, `startScreen`) corresponde a los valores vigentes.

## Casos de test (Red-Green)

| Test | Antes (Red) | Después (Green) |
|------|-------------|-----------------|
| `showAppInfoDialog usa la versión del build` | `AppInfo()` fijo `"1.0"` | `version == buildInfo.versionName` (p. ej. `"2.3.1"`) |
| `showAppInfoDialog incluye metadatos del build` | campos ausentes | `versionCode`, `applicationId`, `buildType` presentes |
| `showAppInfoDialog incluye configuración activa` | ausente | `theme`/`startScreen`/`activeServerAlias` correctos |
| `sin servidor activo alias es null y se omite` | ausente | `activeServerAlias == null`; `toClipboardText()` sin línea "Servidor activo" |
| `onCopyResult(true) emite mensaje de éxito` | ausente | Turbine recibe `"Información copiada"` |
| `onCopyResult(false) emite mensaje de error` | ausente | Turbine recibe `"No se pudo copiar la información"` |
| `onCopyResult no cierra el diálogo` | ausente | `appInfoUiState is Showing` tras la llamada |
