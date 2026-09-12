# Phase 0 — Research: Credenciales por servidor y auto-login

**Feature**: `0024-jlizquierdo-20260912-per-server-credentials` | **Date**: 2026-09-12

Resuelve los puntos abiertos del `Technical Context`. No quedan `NEEDS CLARIFICATION`.

---

## 1. Almacenamiento de credenciales

- **Decision**: guardar por servidor `email`, `password` y `refreshToken` en `EncryptedSharedPreferences` (`tolocha_tokens`, AES-256), indexados por `serverId`; `activeServerId` en el mismo almacén. Room (`saved_servers`, v7) **no cambia**.
- **Rationale**: FR-001/SC-005 (contraseña cifrada, nunca en claro) y FR-010 (los servidores de la 0022 se conservan sin migración de esquema). Mantener las credenciales fuera de Room evita PII sin cifrar y una migración adicional.
- **Alternatives considered**:
  - Columnas `email`/`password` en Room (spec 007 ponía `email` en Room): descartado por privacidad y por exigir migración 7→8.
  - DataStore cifrado: viable, pero `EncryptedSharedPreferences` ya se usó en la 007 y es el patrón conocido del proyecto.

## 2. Flujo de autenticación

- **Decision**: `AuthRepo.login(email,password)` y `AuthRepo.refresh(refresh)`; el token de acceso vive en memoria (`SessionManager`), el refresh cifrado en disco. `AuthInterceptor` añade `Authorization: Bearer` a todas las peticiones; `TokenAuthenticator` ante 401 hace **refresh single-flight** (mutex) y reintenta **una** vez; si el refresh falla, intenta **re-login** con email/password guardados; si también falla, error de credenciales.
- **Rationale**: FR-004/FR-005 y edge cases (token revocado, 401 repetidos sin bucle); reutiliza el patrón probado de las specs 001/007.
- **Alternatives considered**:
  - Basic auth en cada petición: descartado (el backend usa JWT + refresh).
  - Login solo al arrancar sin authenticator: descartado (tokens caducan durante el uso).

## 3. Gate de arranque

- **Decision**: `StartupGate` pasa a tres estados: `NoServers` → **ServerForm (alta)**; `NeedsCredentials` (servidor activo/por defecto sin credenciales) → **ServerForm (edición) bloqueante**; `Ready` → auto-login y contenido.
- **Rationale**: FR-010/FR-011 y la prueba real: los servidores migrados de la 0022 no tienen credenciales y sin sesión no hay reproducción ni contenido de usuario.
- **Alternatives considered**:
  - Flujo perezoso (entrar al contenido y fallar con error): descartado por el usuario (opción A de la clarificación).
  - Bloquear solo si ningún servidor tiene credenciales: descartado (el activo es el que importa).

## 4. Pantalla unificada de servidor

- **Decision**: `ServerFormScreen` + `ServerFormViewModel` con modos **Add** y **Edit**; campos URL, alias, email y contraseña. En Edit la contraseña se precarga **enmascarada** y, si no se modifica, se conserva. Se usa en la bienvenida (sin servidores), en la sección Servidores (FAB añadir, icono Editar por tarjeta) y en el arranque bloqueante (`NeedsCredentials`). Se eliminan `InstanceSetupScreen/ViewModel`.
- **Rationale**: FR-002/FR-007 y US1; unifica los dos formularios que hoy divergen (el de la 0022 no pedía credenciales).
- **Alternatives considered**:
  - Diálogo en la lista + pantalla solo para el alta: descartado (duplicación y experiencia inconsistente).
  - Mantener `InstanceSetup` para la bienvenida: descartado (el usuario pidió unificar).

## 5. Reintento y recuperación

- **Decision**: `CommonUi.ErrorBanner` admite una acción primaria configurable. En Favoritos/Historial (y demás secciones autenticadas), si el error de dominio es de credenciales, la acción muestra "Editar servidor" y navega a `ServerForm(edit, activeServerId)`; si es de red, muestra "Reintentar" y repite la carga.
- **Rationale**: FR-006 y US3; evita pantallas de login y da salida al usuario.
- **Alternatives considered**:
  - Reintentar siempre la carga: descartado por el usuario (lleva a un bucle si la contraseña es incorrecta).
  - Ir a la lista de Servidores: descartado; el usuario pidió la **ficha del servidor activo**.

## 6. Reproducción con Bearer

- **Decision**: `PlayerDataSourceFactory` vuelve a leer `SessionManager` y añade `Authorization: Bearer` en cada petición del datasource (Media3 lo aplica por request, así que cubre manifiestos, variantes y segmentos HLS); Chromecast usa la misma fuente.
- **Rationale**: FR-004 (proxy y HLS autenticados) y clarificación de la sesión.
- **Alternatives considered**: dejar el player sin auth: descartado (la reproducción falla en la instancia real).

## 7. Migración desde la 0022

- **Decision**: los servidores guardados (URL/alias/estado) se conservan tal cual; `hasCredentials` se deriva de la existencia de credenciales en el almacén cifrado. Sin cambio de esquema Room. Al actualizar, si el servidor activo/por defecto no tiene credenciales, el arranque abre el formulario bloqueante.
- **Rationale**: FR-010 y edge case de migración; cero pérdida de servidores.
- **Alternatives considered**: migración Room 7→8 con columnas de credenciales: descartada (ver §1).

## 8. Dependencia y backup

- **Decision**: reintroducir `androidx.security:security-crypto` en `libs.versions.toml` y `app/build.gradle.kts`; volver a excluir `tolocha_tokens(.xml)` en `backup_rules.xml` y `data_extraction_rules.xml`.
- **Rationale**: SC-005; el almacén cifrado no debe respaldarse.
- **Alternatives considered**: cifrado manual con Android Keystore: descartado (más código y riesgo sin aportar valor YAGNI).

## 9. Superficie mínima de auth

- **Decision**: `AuthApi` expone solo `POST /auth/login` y `POST /auth/refresh`. No se restauran registro, recuperación de contraseña, logout ni `/users/me`. `OkResult` se mantiene como DTO compartido.
- **Rationale**: YAGNI y Out of Scope de la spec; el usuario no gestiona cuentas desde la app más allá de guardar credenciales por servidor.
- **Alternatives considered**: restaurar `AuthApi` completa de la 007: descartado (código muerto).

## 10. Historial y favoritos por usuario

- **Decision**: no se requieren cambios de cliente más allá de que la sesión funcione; al autenticar, el backend devuelve los datos del usuario. Se retira de la documentación la semántica "compartido por instancia" de la 0022.
- **Rationale**: FR-009/SC-006.
- **Alternatives considered**: mantener caché compartida entre usuarios: descartado (fuga de datos entre cuentas).

## 11. Gobernanza

- **Decision**: enmienda constitucional **2.0.0 → 3.0.0 (MAJOR)** como primera tarea bloqueante: redefine II y IV (credenciales cifradas por servidor + auto-login JWT/Bearer, sin pantallas de login; 401 → refresh/re-login), restaura el cifrado/backup en la sección de seguridad y marca la 0022 como parcialmente superseded.
- **Rationale**: la 2.0.0 prohíbe credenciales; la feature no puede implementarse sin este cambio de gobernanza.
- **Alternatives considered**: tratar las credenciales como "excepción de deuda": descartado; la constitución exige que un principio se cambie en una enmienda explícita, no por excepción.
