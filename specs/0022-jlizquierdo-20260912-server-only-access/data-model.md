# Phase 1 — Data Model: Acceso solo con servidores

**Feature**: `0022-jlizquierdo-20260912-server-only-access` | **Date**: 2026-09-12

Este modelo describe los datos tras retirar la autenticación de usuario. No hay entidades de sesión, credenciales ni usuario.

---

## Entidades persistidas

### Servidor (`SavedServerEntity` — Room `saved_servers`, v7)

| Campo | Tipo | Reglas |
|-------|------|--------|
| `id` | `String` (UUID) | Clave primaria; generado al añadir |
| `url` | `String` | URL base normalizada de la instancia; única por alias (índice único `url`+`alias`); validada contra `/health` y `/config` antes de guardar (FR-004) |
| `alias` | `String` | Nombre visible; no vacío tras recortar (regla existente de `AddServerUseCase`); se permiten duplicados de URL con alias distinto |
| `appName` | `String?` | Nombre de la app leído de `/config` al añadir; solo informativo |
| `isActive` | `Boolean` | Servidor en uso en la sesión actual; a lo sumo uno con `true` |
| `isDefault` | `Boolean` | Servidor que se usa al arrancar; a lo sumo uno con `true` |
| `createdAt` | `Long` | Marca temporal de creación |

**Cambio respecto a v6**: se elimina `userEmail`. La migración `MIGRATION_6_7` recrea la tabla sin esa columna conservando filas y los índices (FR-009, FR-010).

**Modelo de dominio `SavedServer`**: mismos campos que la entidad (`id, url, alias, appName, isActive, isDefault, createdAt`) **sin** `userEmail`.

### Preferencias de instancia (`InstancePrefs` — DataStore `tolocha_prefs`)

| Clave | Tipo | Reglas |
|-------|------|--------|
| `base_url` | `String` | URL efectiva de runtime para Retrofit/Cast/Player; por defecto `BuildConfig.TOLOCHA_BASE_URL`; la mantiene el onboarding y el cambio de servidor |
| `setup_done` | `Boolean` | Compatibilidad/migración; deja de ser el gate de contenido (el gate es "¿hay servidores?") |
| `theme_mode` | `ThemeMode` | Tema claro/oscuro/sistema; ahora **solo local** (sin sincronización con servidor) |
| `start_screen` | `StartScreen` | Pantalla de arranque (Favoritos, Historial, Explorar); por defecto Explorar |
| `view_mode` | `ViewModel` | Preferencia lista/tarjetas (existente) |

---

## Entidades eliminadas

| Entidad/clase | Motivo |
|---------------|--------|
| `SessionManager` / `AuthState` (`Loading/Authenticated/Unauthenticated`) | No hay sesión de usuario (FR-008) |
| `SessionRestorer` | No hay sesión que restaurar |
| `TokenStore` / `ServerCredentials(refresh,email,password)` | Prohibido almacenar credenciales (FR-009, SC-006) |
| `AuthRepo`, `AuthApi`, `UserRepo`, `UserApi`, DTOs de auth/usuario | No hay login/registro/refresh/perfil (FR-008) |
| `AuthInterceptor`, `TokenAuthenticator` | Las peticiones no llevan credenciales (FR-011) |

**Limpieza de almacenamiento**: el `EncryptedSharedPreferences` `tolocha_tokens` de versiones anteriores se elimina una sola vez al arrancar (`TokenStore` ya no existe). No debe quedar ningún token, email ni password en disco (FR-010, SC-006).

---

## Estado derivado de arranque (no persistido)

`StartupGate`:

| Estado | Condición | Resultado |
|--------|-----------|-----------|
| `NoServers` | lista de servidores vacía | Pantalla de bienvenida/onboarding bloqueante (FR-002) |
| `Ready` | al menos un servidor | Navegar a `StartScreen` configurada (FR-005) |

---

## Reglas de validación y transiciones

1. **Alta de servidor** (FR-003/FR-004): normalizar URL → validar `/health` + `/config` → persistir. Si es el primero, `isActive=true` e `isDefault=true`. Si la validación falla, no se guarda y se muestra error en español con reintento.
2. **Cambio de activo** (FR-006/FR-007): `clearActive` → `setActive(nuevo)` → actualizar `InstancePrefs.baseUrl` → `cacheManager.clearAll()` → limpiar shortcuts → rebirth del proceso. `isDefault` no cambia.
3. **Eliminación** (FR-013): borrar fila; si era `isActive`, promover otro servidor a activo (y a default si no quedaba ninguno); si no queda ninguno, el arranque pasa a `NoServers` y la app vuelve a la bienvenida.
4. **Arranque** (FR-005/FR-012): si `Ready`, usar la URL del servidor activo/default como `baseUrl`; si la instancia no responde, estado de error accionable (reintentar/editar servidor) sin cerrar la app.
5. **Instancia no compatible** (FR-016): si el servidor responde exigiendo autenticación (401/403), mostrar "instancia no actualizada"; nunca ofrecer login.
6. **Contenido compartido** (FR-015): historial, favoritos y emisoras personalizadas se leen/escriben en el servidor sin usuario; no se particionan por cuenta en el cliente.

## Datos NO modelados

- No hay `User`, `AuthSession`, `StoredCredentials` ni campos de tema por usuario.
- No hay cambios en los DTO de catálogo/historial/favoritos/personalizadas (siguen igual, sin envío de credenciales).
