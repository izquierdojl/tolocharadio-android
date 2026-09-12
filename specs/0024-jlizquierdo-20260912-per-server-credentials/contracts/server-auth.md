# Contrato — Auth por servidor y pantalla unificada

**Feature**: `0024-jlizquierdo-20260912-per-server-credentials` | **Date**: 2026-09-12

Contrato de aplicación (UI + red + seguridad). El backend no cambia.

---

## 1. Endpoints usados por la app

| Endpoint | Cuerpo | Respuesta | Uso |
|----------|--------|-----------|-----|
| `POST /api/v1/auth/login` | `{email, password}` | `{user, accessToken, refreshToken}` | Login al añadir/editar servidor, al cambiar de servidor y como re-login si el refresh falla |
| `POST /api/v1/auth/refresh` | `{refreshToken?}` | `{user, accessToken, refreshToken}` | Renovación transparente ante 401 (single-flight) |

No se usan `/auth/register`, `/auth/forgot-password`, `/auth/reset-password`, `/auth/logout` ni `/users/me`.

**Cabecera obligatoria**: `Authorization: Bearer <accessToken>` en todas las peticiones autenticadas, incluidas reproducción (`/playback/{id}`), manifiestos, variantes y segmentos HLS. La credencial NUNCA viaja en la URL ni en query.

**Errores**: formato `{error:{code,message,status,details?}}`. 401/403 con credenciales inválidas → error de credenciales accionable (sin texto técnico). El token no se registra en logs.

## 2. Pantalla unificada de servidor

| Aspecto | Contrato |
|---------|----------|
| Campos | URL, alias, email, contraseña (obligatorios URL/email/contraseña; alias no vacío) |
| Modos | Add (bienvenida/sin servidores, FAB de Servidores) y Edit (icono Editar por tarjeta; arranque con `NeedsCredentials`) |
| Edit | Email precargado; contraseña precargada **enmascarada**; no modificarla la conserva |
| Validación | Cliente bloquea incompletos con aviso en español; servidor valida `/health`+`/config` y `POST /auth/login` antes de persistir |
| Error | URL inalcanzable o login fallido → mensaje accionable, no se persiste |

## 3. Arranque y sesión

| Condición | Resultado obligatorio |
|-----------|------------------------|
| Sin servidores | `ServerForm` (alta) bloqueante |
| Servidor activo/por defecto sin credenciales | `ServerForm` (edición) bloqueante hasta completarlas |
| Servidor activo con credenciales | Login automático (refresh si hay token, si no login) y acceso al contenido |
| 401 en cualquier petición | Refresh single-flight + reintento único; si falla, re-login con credenciales guardadas |
| Refresh y re-login fallan | Error de credenciales accionable (nunca pantalla de login) |

Invariantes:
- No existe ninguna pantalla de login/registro.
- Cambiar de servidor limpia caché y sesión del anterior y hace login con las credenciales del nuevo.
- El token de acceso no se persiste; el refresh y la contraseña sí (cifrados) y se excluyen de backup.

## 4. Errores y recuperación

| Situación | Acción primaria |
|-----------|-----------------|
| Error de credenciales (401/403/refresh fallido) | "Editar servidor" → `ServerForm(edit, servidor activo)` |
| Error de red o instancia inalcanzable | "Reintentar" → repite la carga |
| Error de validación del backend (422) | Mensaje por campo en español |

## 5. Reproducción

- `PlayerDataSourceFactory` inyecta `Authorization: Bearer` en cada petición del proxy y de los subrecursos HLS; Chromecast reutiliza la misma fuente autenticada.
- Sin sesión válida, el arranque ya ha bloqueado en el formulario, por lo que no se intenta reproducir sin token.

## 6. Migración desde la 0022

- Los servidores existentes se conservan (URL/alias/estado) y quedan `hasCredentials=false`.
- Si el activo/por defecto no tiene credenciales, el primer arranque abre el formulario bloqueante; al completarlo, login y contenido.
- No hay cambio de esquema Room ni pérdida de datos.
