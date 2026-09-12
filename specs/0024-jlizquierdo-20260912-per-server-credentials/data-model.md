# Phase 1 — Data Model: Credenciales por servidor

**Feature**: `0024-jlizquierdo-20260912-per-server-credentials` | **Date**: 2026-09-12

---

## Entidades persistidas

### Servidor (`SavedServerEntity` — Room `saved_servers`, v7, SIN CAMBIOS)

| Campo | Tipo | Reglas |
|-------|------|--------|
| `id` | `String` (UUID) | Clave primaria |
| `url` | `String` | URL base normalizada; validada contra `/health` y `/config` |
| `alias` | `String` | Nombre visible; no vacío |
| `appName` | `String?` | Nombre de la app leído de `/config` |
| `isActive` | `Boolean` | Servidor en uso en la sesión actual; a lo sumo uno |
| `isDefault` | `Boolean` | Servidor que se usa al arrancar; a lo sumo uno |
| `createdAt` | `Long` | Marca temporal |

**No hay migración Room**: las credenciales no viven en esta tabla.

### Credenciales (`EncryptedSharedPreferences` `tolocha_tokens`)

| Clave | Tipo | Reglas |
|-------|------|--------|
| `srv:<serverId>:email` | `String` | Email de la cuenta (FR-001) |
| `srv:<serverId>:password` | `String` (cifrada) | Contraseña; nunca en logs ni en URL |
| `srv:<serverId>:refresh` | `String` (cifrada) | Refresh token rotatorio de la sesión de ese servidor |
| `active_server_id` | `String?` | Servidor activo |

Reglas:
- Toda escritura/lectura pasa por `TokenStore` (KDoc con contrato).
- El **token de acceso** nunca se persiste: vive solo en memoria.
- Al borrar un servidor se borran sus tres claves y, si era el activo, se actualiza `active_server_id`.
- El almacén se excluye de backup (`backup_rules.xml`, `data_extraction_rules.xml`).

## Entidades en memoria

### Sesión (`SessionManager`)

| Campo | Tipo | Reglas |
|-------|------|--------|
| `accessToken` | `String?` | Solo memoria; `accessTokenNow()` para el interceptor y el player |
| `state` | `StateFlow<SessionState>` | `Ready`/`Authenticating`/`MissingCredentials` (sin datos de usuario) |

Al cambiar de servidor o al fallar el re-login, `clear()` deja la sesión vacía.

### Modelo de dominio `SavedServer`

Mismos campos que la entidad + `hasCredentials: Boolean` (derivado de `TokenStore`).

## Reglas de validación

1. **Alta** (FR-001/FR-003/FR-004): URL, email y contraseña obligatorios (alias no vacío) → validar `/health` + `/config` → `POST /auth/login` con las credenciales → si OK, persistir servidor + credenciales y activar sesión; si falla, no persistir y mostrar error.
2. **Edición** (FR-007): el formulario precarga email y contraseña enmascarada; si la contraseña no se modifica, se conserva la guardada; al guardar se revalida (login) y se renueva la sesión.
3. **Cambio de activo** (FR-008): limpiar caché y sesión del anterior → cargar credenciales del nuevo → login; el servidor por defecto no cambia.
4. **Borrado** (FR-010 de la 0022 se mantiene): borrar servidor + credenciales; promover activo/default; si no queda ninguno → `NoServers`.

## Estados de arranque (`StartupGate`)

| Estado | Condición | Resultado |
|--------|-----------|-----------|
| `NoServers` | lista vacía | `ServerForm` (alta) bloqueante |
| `NeedsCredentials` | servidor activo/por defecto sin credenciales | `ServerForm` (edición) bloqueante |
| `Ready` | servidor activo/por defecto con credenciales | auto-login y contenido |

## Transiciones de sesión

- `Ready(access)` → 401 → refresh OK → `Ready(access nuevo)`.
- refresh falla → re-login con email/password guardados → `Ready` o `MissingCredentials`.
- `MissingCredentials` → `ServerForm` (edición del activo) → guardar → `Ready`.
- cambio de servidor → `clear()` → credenciales del nuevo → login → `Ready`.

## Datos NO modelados

- No hay `User`/perfil en el cliente (no se usa `/users/me`).
- No hay registro, recuperación de contraseña ni logout.
