# Quickstart: Validación de Sesión Persistente, Servidores y Configuración

**Date**: 2026-09-06 | **Feature**: 007-persistent-session (enmendada: Servidores de primer nivel, activo ≠ por defecto, Configuración sustituye a Perfil)

## Prerrequisitos

- Android Studio instalado
- Dispositivo Android API 26+ o emulador
- Instancia TolochaRadio accesible (local o remota)
- Cuenta de usuario en la instancia

---

## Escenarios de Validación

### 1. Login con persistencia automática

**Objetivo**: Verificar que la sesión sobrevive al cierre completo de la app.

**Pasos**:
1. Abrir la app
2. Introducir URL de instancia (si es primera vez)
3. Hacer login con email + contraseña
4. Verificar que se accede a Historial y Favoritos
5. Cerrar la app completamente (swipe away en recientes)
6. Reabrir la app
7. Intentar acceder a Historial o Favoritos

**Resultado esperado**:
- La app se abre sin pedir credenciales
- Historial y Favoritos son accesibles directamente
- La sesión se restaura en < 2 segundos

**Verificación adicional**:
- Esperar > 15 minutos (access token caduca)
- Repetir pasos 5-7
- Debería funcionar (refresh transparente)

---

### 2. Gestión de servidores (sección de primer nivel, activo ≠ por defecto)

**Objetivo**: Verificar que se pueden guardar y cambiar entre múltiples
servidores, con conceptos separados de activo y por defecto (FR-005).

**Pasos**:
1. Tocar el icono de **Servidores** en la barra superior (no está
   dentro de Configuración — FR-004)
2. Verificar que el servidor actual aparece marcado como **Activo**
   y también como **Por defecto**
3. Añadir un segundo servidor (URL + alias + email + contraseña
   cifrados)
4. Verificar que aparece en la lista con su alias y email, sin
   marcas de activo/por defecto
5. Seleccionar el segundo servidor
6. Verificar que la app renace contra la nueva instancia (la sesión
   se restaura con el refresh guardado de ese servidor, sin pedir
   credenciales — FR-006)
7. Reabrir Servidores y comprobar: el segundo es **Activo** pero el
   **Por defecto** sigue siendo el primero

**Resultado esperado**:
- La lista muestra ambos servidores con marcas independientes
- Cambiar de servidor NO modifica el por defecto
- La caché del servidor anterior se limpia (FR-008b)
- Al arrancar la app de nuevo se conecta al servidor por defecto
- El cambio toma < 5 segundos

### 2b. Re-login silencioso tras revocación (FR-006b)

**Pasos**:
1. Estar autenticado en un servidor con credenciales guardadas
   (email + password cifrados)
2. Revocar el refresh token (p. ej. cambiar la contraseña en el
   servidor y restaurarla, o esperar rotación inválida)
3. Cerrar y reabrir la app

**Resultado esperado**:
- La app re-autentica automáticamente con el password cifrado
  guardado, sin pedir credenciales
- La sesión queda restaurada y el token renovado

---

### 3. Eliminación de servidor

**Objetivo**: Verificar que al eliminar un servidor se limpian sus credenciales.

**Pasos**:
1. Tener al menos 2 servidores guardados
2. Eliminar el servidor no activo
3. Verificar que desaparece de la lista
4. Intentar añadir el mismo servidor de nuevo
5. Verificar que se pide login (credenciales eliminadas)

**Resultado esperado**:
- El servidor se elimina de la lista
- Las credenciales asociadas se eliminan
- Al re-añadir, se requiere login nuevo

---

### 4. Cierre de sesión

**Objetivo**: Verificar que el logout limpia credenciales persistentes.

**Pasos**:
1. Estar autenticado con sesión guardada
2. Ir a Configuración → Cerrar sesión
3. Verificar redirección a Login
4. Cerrar la app completamente
5. Reabrir la app

**Resultado esperado**:
- Se redirige a Login
- Al reabrir, se pide login (no se restaura sesión)
- Las credenciales eliminadas no están en almacenamiento

---

### 5. Error de servidor al restaurar sesión

**Objetivo**: Verificar comportamiento cuando el servidor no es accesible.

**Pasos**:
1. Tener sesión guardada
2. Desconectar la red o apagar el servidor
3. Cerrar y reabrir la app

**Resultado esperado**:
- La app muestra estado offline con opción de reintento
- No pide login inmediatamente
- Al reconectar, la sesión se restaura

---

### 6. Migración desde versión anterior

**Objetivo**: Verificar que la migración de baseUrl a SavedServer es transparente.

**Pasos**:
1. Instalar versión anterior (con baseUrl en DataStore)
2. Hacer login y usar la app
3. Actualizar a nueva versión
4. Abrir la app

**Resultado esperado**:
- La app funciona sin pedir login
- En Servidores aparece "Mi servidor" con la URL anterior, marcado
  como Activo y Por defecto
- La sesión se restaura automáticamente

---

### 7. Configuración: pantalla de arranque (FR-011b)

**Objetivo**: Verificar que la app abre en la pantalla configurada.

**Pasos**:
1. Ir a Configuración (pestaña inferior, sustituye a Perfil)
2. Cambiar el tema Claro/Oscuro → se aplica al instante
3. Seleccionar "Favoritos" como pantalla de arranque
4. Cerrar la app completamente
5. Reabrir la app con sesión activa

**Resultado esperado**:
- La app abre directamente en Favoritos (sin pasar por la pantalla
  inicial)
- La elección persiste entre sesiones
- Sin configuración previa, se usa Explorar por defecto

---

## Verificaciones de Seguridad

### Tokens no expuestos

1. Activar logging verbose
2. Hacer login y usar la app
3. Revisar logs (Logcat)
4. Verificar que NO aparecen tokens

### Almacenamiento cifrado

1. Conectar dispositivo con root (emulador)
2. Navegar a `/data/data/com.izquierdojl.tolocharadio/shared_prefs/`
3. Verificar que tokens, email y password están cifrados (no en
   texto plano)

### Logout completo

1. Hacer logout
2. Verificar que `EncryptedSharedPreferences` no contiene tokens
3. Verificar que Room no contiene datos de sesión

---

## Comandos de Ejecución

```bash
# Compilar
./gradlew assembleDebug

# Ejecutar tests unitarios
./gradlew testDebugUnitTest

# Ejecutar tests de integración
./gradlew connectedDebugAndroidTest

# Verificar lint
./gradlew lintDebug
```

---

## Criterios de Aceptación Rápidos

| Criterio | Verificación |
|----------|-------------|
| Sesión persiste a cierre de app | Escenario 1 |
| Refresh transparente | Escenario 1 (verificación adicional) |
| Re-login silencioso tras revocación | Escenario 2b |
| Múltiples servidores (activo ≠ por defecto) | Escenarios 2, 3 |
| Caché limpia al cambiar | Escenario 2 |
| Logout limpia todo | Escenario 4 |
| Degradación offline | Escenario 5 |
| Migración transparente | Escenario 6 |
| Pantalla de arranque configurable | Escenario 7 |
| Tokens/credenciales seguros | Verificaciones de seguridad |
