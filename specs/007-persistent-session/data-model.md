# Data Model: Gestión de Sesión Persistente y Credenciales

**Date**: 2026-09-06 | **Feature**: 007-persistent-session

## Entities

### SavedServer

Instancia TolochaRadio guardada por el usuario.

| Campo | Tipo | Descripción | Validación |
|-------|------|-------------|------------|
| id | UUID | Identificador único | Auto-generado |
| url | String | URL de la instancia (normalizada) | Requerida, formato URL válido, normalizada |
| alias | String | Nombre asignado por el usuario | Requerido, 1-100 caracteres |
| appName | String? | Nombre de la app (de `GET /config`) | Opcional, obtenido al validar |
| isDefault | Boolean | Si es el servidor predeterminado | Solo uno puede ser true |
| createdAt | Long | Timestamp de creación | Auto-generado |

**Relaciones**:
- Un SavedServer tiene un StoredCredentials opcional (1:1 por serverId)

**Estado**:
- `validating` → URL validándose contra `/health`
- `ready` → Listo para usar
- `error` → Validación fallida

---

### StoredCredentials

Credenciales cifradas asociadas a un servidor.

| Campo | Tipo | Descripción | Validación |
|-------|------|-------------|------------|
| serverId | UUID | FK a SavedServer | Requerido, único |
| refreshTokenEncrypted | String | Refresh token cifrado (AES-256 GCM) | Requerido si hay sesión |
| userId | String | ID del usuario en esa instancia | Requerido si hay sesión |
| userEmail | String | Email del usuario | Requerido si hay sesión |
| lastLoginAt | Long | Timestamp del último login | Auto-generado |

**Almacenamiento**: EncryptedSharedPreferences (no Room)

**Relaciones**:
- Pertenece a un SavedServer (1:1)

---

### AuthSession (ampliación)

Estado de sesión actual en memoria.

| Campo | Tipo | Descripción | Persistencia |
|-------|------|-------------|--------------|
| accessToken | String? | JWT de acceso | Solo en memoria |
| refreshToken | String? | Refresh token | Cifrado en EncryptedSharedPreferences |
| isAuthenticated | Boolean | Si hay sesión activa | Calculado |
| user | User? | Datos del usuario | Cache en memoria |
| activeServerId | UUID? | Servidor activo | Room/DataStore |

---

### User (existente)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | String | ID del usuario |
| email | String | Email |
| name | String | Nombre |
| theme | String | light/dark |

---

## State Transitions

### AuthSession

```
Unauthenticated
    ↓ (login exitoso)
Authenticated ←→ (refresh token válido)
    ↓ (refresh fallido / logout)
Unauthenticated
```

### SavedServer

```
validating → ready (validación OK)
validating → error (validación fallida)
ready → (eliminado)
```

---

## Relationships

```
SavedServer (1) ──→ (0..1) StoredCredentials
SavedServer (1) ──→ (0..1) AuthSession (si es el activo)
```

---

## Storage Strategy

| Entidad | Almacenamiento | Justificación |
|---------|---------------|---------------|
| SavedServer | Room | Consultas, ordenamiento, relaciones |
| StoredCredentials | EncryptedSharedPreferences | Datos sensibles, cifrado nativo |
| AuthSession.accessToken | Memoria (ViewModel/Singleton) | Nunca persistir en disco |
| activeServerId | DataStore | Preferencia de usuario |

---

## Validation Rules

### SavedServer
- `url`: Debe pasar validación de formato URL + `GET /health` exitoso
- `alias`: 1-100 caracteres, no vacío
- `isDefault`: Solo un servidor puede ser default; al añadir nuevo con isDefault=true, el anterior pierde el flag

### StoredCredentials
- `serverId`: Debe existir en SavedServer
- `refreshTokenEncrypted`: No puede estar vacío si se guarda sesión
- `userEmail`: Formato email válido

---

## Migration from Current Architecture

### Current
- `baseUrl` en DataStore (String)
- Tokens en memoria (se pierden al cerrar app)

### Target
- `SavedServer` en Room (migrado desde baseUrl)
- Refresh token en EncryptedSharedPreferences
- Access token en memoria (igual que ahora)

### Migration Steps
1. Al abrir app, detectar si hay `baseUrl` en DataStore pero no SavedServers en Room
2. Crear SavedServer con la baseUrl existente, alias = "Mi servidor"
3. Si hay sesión activa, intentar guardar refresh token en EncryptedSharedPreferences
4. Marcar como isDefault = true
5. Limpiar baseUrl antigua de DataStore
