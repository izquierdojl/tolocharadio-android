# Contrato — Acceso solo con servidores (sin autenticación de usuario)

**Feature**: `0022-jlizquierdo-20260912-server-only-access` | **Date**: 2026-09-12

Define el comportamiento observable de la app tras retirar la autenticación: arranque, configuración de servidor, uso de la API y manejo de errores. Es un contrato de aplicación (UI + red), no un contrato de backend nuevo.

---

## 1. Arranque

| Condición | Resultado obligatorio |
|-----------|------------------------|
| No hay ningún servidor guardado | Pantalla de bienvenida/onboarding dedicada; mensaje "Configura un servidor para acceder" + acción para añadir; el contenido NO es accesible |
| Hay ≥ 1 servidor y responde | Navegar a `StartScreen` (Favoritos/Historial/Explorar; por defecto Explorar) |
| Hay ≥ 1 servidor y no responde | Estado de error accionable (reintentar / editar servidor); sin cierre ni pantalla en blanco |
| El servidor exige autenticación (401/403) | Mensaje "instancia no actualizada/no compatible"; NUNCA pantalla de login |

Invariantes:

- No existe ninguna pantalla, ruta ni acción de login, registro, recuperación de contraseña ni cierre de sesión (FR-008, SC-004).
- Ninguna petición lleva `Authorization` ni credenciales en query (FR-011, SC-006).

## 2. Configuración de servidor

**Añadir**:

1. Entrada: URL y alias.
2. Normalizar URL; rechazar si no es válida o no es cifrada, con mensaje en español.
3. Validar contra `GET /api/v1/health` y `GET /api/v1/config` (sin credenciales).
4. Persistir `SavedServerEntity` (URL, alias, `appName` de `/config`); si es el primero → `isActive=true`, `isDefault=true`; fijar `InstancePrefs.baseUrl`.
5. En caso de fallo: no persistir y mostrar motivo + reintento (FR-004).

**Cambiar activo**: `clearActive` → `setActive(nuevo)` → `InstancePrefs.baseUrl` → limpiar caché → limpiar shortcuts → rebirth. Sin credenciales ni login (FR-006/FR-007).

**Eliminar**: borrar la fila y sus datos asociados; promover nuevo activo/default si procede; si era el último → estado de bienvenida (FR-013).

**Invariantes**: `SavedServerEntity` nunca contiene email, password ni token; el diálogo de alta no pide credenciales; la tarjeta de servidor no muestra usuario (FR-009).

## 3. Uso de la API (todas sin credenciales)

| Endpoint | Uso | Auth |
|----------|-----|------|
| `GET /api/v1/health`, `GET /api/v1/config` | Validación de servidor | Ninguna |
| `GET /api/v1/stations*` | Explorar/buscar | Ninguna |
| `GET /api/v1/favorites`, `POST /api/v1/favorites`, `DELETE /api/v1/favorites/{id}`, `PUT /api/v1/favorites/order` | Favoritos | Ninguna (compartidos por instancia) |
| `GET /api/v1/history`, `DELETE /api/v1/history`, `DELETE /api/v1/history/{id}` | Historial | Ninguna (compartido por instancia) |
| `GET/POST /api/v1/custom-stations`, `DELETE /api/v1/custom-stations/{id}` | Emisoras personalizadas | Ninguna |
| `GET /api/v1/playback/{id}` (proxy) y subrecursos `/hls` | Reproducción | Ninguna |
| `GET /api/v1/playback/{id}/status` | Precheck de disponibilidad | Ninguna |

Se eliminan del cliente: `POST /auth/*`, `GET/PATCH /users/me`, `PATCH /users/me/password`.

## 4. Reproductores

- URI de reproducción: siempre el proxy del servidor (`/api/v1/playback/{id}`), HLS o progresivo según extensión (comportamiento de la spec 0021).
- El datasource de Media3 (`PlayerDataSourceFactory`) no añade cabeceras de autorización.
- Chromecast y reanudación usan la misma fuente sin credenciales (FR-014).

## 5. Errores

| Situación | Mensaje/comportamiento |
|-----------|------------------------|
| Instancia inalcanzable | Mensaje en español + reintento; sin cierre |
| 401/403 de la instancia | "Instancia no actualizada: requiere una versión sin autenticación"; sin login |
| Lista/stream no disponible (precheck) | Mensajes tipados ya existentes (spec 0021) |
| Validación de servidor fallida | Motivo + reintento; no se guarda |

Prohibido: mostrar códigos técnicos crudos, ofrecer re-login, o dejar pantalla en blanco (FR-012).

## 6. Ajustes y shortcuts

- Ajustes: tema (solo local), pantalla de arranque, modo de vista. Sin "Cerrar sesión" (FR-008).
- Shortcuts: se publican en cuanto hay servidor configurado, sin depender de sesión; al cambiar/eliminar servidor se limpian (FR-005).
