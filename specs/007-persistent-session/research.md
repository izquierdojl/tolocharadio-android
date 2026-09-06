# Research: Gestión de Sesión Persistente y Credenciales

**Date**: 2026-09-06 | **Feature**: 007-persistent-session

## Research Tasks

### 1. Almacenamiento seguro de tokens en Android

**Pregunta**: ¿Cuál es la mejor opción para almacenar refresh tokens de forma segura en Android API 26+?

**Decisión**: EncryptedSharedPreferences (Jetpack Security)

**Razonamiento**:
- API nativa de Android desde API 23, disponible en minSdk=26 sin dependencias externas
- Cifra datos con AES-256 GCM, claves almacenadas en Android Keystore
- Integración directa con el ecosistema Jetpack ya utilizado en el proyecto
- DataStore (ya usado para ajustes) no está diseñado para datos sensibles

**Alternativas evaluadas**:
- **DataStore cifrado (EncryptedDataStore)**: Más complejo, requiere librería adicional, no aporta ventajas sobre EncryptedSharedPreferences para datos pequeños como tokens
- **Android AccountManager**: API más antigua, diseñada para cuentas de sistema, exceso de complejidad para el caso de uso
- **SQLCipher**: Excesivo para almacenar solo un token por servidor
- **Archivos cifrados manualmente**: Más control pero más propenso a errores de implementación

**Conclusión**: EncryptedSharedPreferences es la opción correcta — simple, segura y nativa.

---

### 2. Estrategia de limpieza de caché al cambiar de servidor

**Pregunta**: ¿Cómo limpiar la caché local (Room) al cambiar de servidor sin recrear la base de datos?

**Decisión**: Limpiar tablas afectadas con DELETE y recargar datos del nuevo servidor

**Razonamiento**:
- Room permite DELETE FROM tabla sin recrear la base de datos
- Las tablas de favoritos, historial y custom-stations son independientes del servidor (no tienen serverId)
- Al cambiar de servidor, se limpian y se recarga desde la API del nuevo servidor
- Migración a tablas con serverId sería over-engineering para el caso de uso actual (un usuario, pocos servidores)

**Alternativas evaluadas**:
- **Tablas con serverId**: Más complejo, requiere migración de Room, queries más complejas, no aporta valor real (el usuario no necesita ver datos de otro servidor simultáneamente)
- **Borrar y recrear base de datos**: Más lento, pierde configuraciones locales
- **Marcar datos como stale**: Confuso para el usuario si ve datos de otro servidor

**Conclusión**: DELETE simple + recarga es la más limpia y consistente con la arquitectura actual.

---

### 3. Normalización de URLs de servidores

**Pregunta**: ¿Cómo normalizar URLs para el campo `url` de SavedServer?

**Decisión**: Normalización mínima — quitar barra final, lowercasing de host, preservar esquema y puerto

**Razonamiento**:
- `https://Radio.Ejemplo.COM/` y `https://radio.ejemplo.com` deben considerarse el mismo servidor
- HTTP vs HTTPS se preservan (diferente contexto de seguridad)
- Puerto explícito vs implícito se preservan (diferentes servicios)
- La normalización se aplica al guardar y al comparar

**Implementación sugerida**:
```kotlin
fun normalizeUrl(url: String): String {
    val uri = Uri.parse(url.trim())
    return Uri.Builder()
        .scheme(uri.scheme?.lowercase())
        .authority(uri.authority?.lowercase()?.removeSuffix("/"))
        .path(uri.path?.removeSuffix("/"))
        .build()
        .toString()
}
```

**Conclusión**: Normalización simple y predecible, suficiente para el caso de uso.

---

### 4. Migración desde baseUrl única a múltiples servidores

**Pregunta**: ¿Cómo migrar la configuración actual de `baseUrl` (DataStore) al nuevo sistema de servidores guardados?

**Decisión**: Migración silenciosa al primer arranque con la nueva versión

**Razonamiento**:
- La app actual guarda `baseUrl` en DataStore y tokens en memoria
- Al detectar que no hay servidores guardados pero sí `baseUrl` en DataStore:
  1. Crear `SavedServer` con la `baseUrl` existente
  2. Si hay sesión activa, intentar guardar credenciales
  3. Marcar como predeterminado
  4. Limpiar `baseUrl` antigua de DataStore
- El usuario no percibe el cambio; la app funciona igual

**Conclusión**: Migración transparente que no requiere intervención del usuario.

---

### 5. Manejo de errores de EncryptedSharedPreferences

**Pregunta**: ¿Qué hacer si EncryptedSharedPreferences falla al guardar o recuperar credenciales?

**Decisión**: Fallback a pedir login sin crash

**Razonamiento**:
- EncryptedSharedPreferences puede fallar si el Keystore está corrupto o el dispositivo tiene problemas de seguridad
- En caso de error: loguear estructuradamente, limpiar estado, redirigir a Login
- No hacer fallback a SharedPreferences sin cifrado (riesgo de seguridad)

**Escenarios**:
- Error al guardar: log + no persistir + mostrar error al usuario + continuar sesión en memoria
- Error al recuperar: log + limpiar + redirigir a Login
- Error al eliminar: log + intentar destruir la instancia de ESP + continuar

**Conclusión**: Degradación elegante sin comprometer seguridad.
