# Phase 0 — Research: Acceso solo con servidores y retirada de la autenticación

**Feature**: `0022-jlizquierdo-20260912-server-only-access` | **Date**: 2026-09-12

Este documento resuelve los puntos abiertos del `Technical Context` y fija las decisiones técnicas del plan. No quedan `NEEDS CLARIFICATION`.

---

## 1. Modelo de acceso sin credenciales

- **Decision**: eliminar por completo la capa de autenticación: `AuthInterceptor`, `TokenAuthenticator`, `SessionManager`, `SessionRestorer`, `TokenStore`, `AuthRepo`, `AuthApi`, `UserApi`, `UserRepo` y las pantallas de auth. `RetrofitFactory` construye el `OkHttpClient` sin interceptor ni authenticator; las peticiones a `/api/v1/**` viajan sin `Authorization`.
- **Rationale**: FR-001, FR-008, FR-011 y la clarificación "acceso totalmente sin credenciales"; el backend asumido no exige credenciales (FR-016).
- **Alternatives considered**:
  - Token de instancia guardado una vez (opción B del clarify): descartado por el usuario.
  - Login único con auto-login silencioso (opción C): descartado por el usuario; mantiene código y almacenamiento de credenciales prohibidos por FR-009/SC-006.

## 2. Arranque y pantalla de bienvenida

- **Decision**: el gate de arranque deja de ser `hasInstance` + `AuthState` y pasa a ser "¿hay servidores configurados?". Sin ninguno se muestra una **pantalla de bienvenida dedicada** (se reutiliza y adapta `InstanceSetupScreen`) que explica que hay que configurar un servidor y ofrece la acción; con al menos uno se navega a la pantalla de arranque configurada (`StartScreen`). El estado de bienvenida es bloqueante: no se accede al contenido hasta configurar un servidor.
- **Rationale**: FR-002 + clarificación Q5 (opción B, pantalla dedicada); evita reutilizar el login como punto de entrada.
- **Alternatives considered**:
  - Estado vacío dentro de la sección Servidores (opción A): descartado por el usuario.
  - Mantener `hasInstance` (DataStore `setup_done`) como gate: rechazado porque no refleja la lista real de servidores y permitía quedar "sin servidores" con `setup_done=true`.
- **Detalle**: `InstanceSetupViewModel.connect` pasa a crear un `SavedServerEntity` (activo + por defecto) además de fijar `InstancePrefs.baseUrl`/`setupDone`, cerrando el gap actual (hoy no crea fila de servidor).

## 3. Fuente de verdad del servidor y cambio de servidor

- **Decision**: mantener dos piezas con responsabilidades claras: `SavedServerEntity` (lista persistida, `isActive`/`isDefault`, sin credenciales) y `InstancePrefs.baseUrl` (URL efectiva de runtime para Retrofit/Cast/Player). El cambio de servidor actualiza `isActive`, limpia caché, escribe `InstancePrefs.baseUrl` y reinicia el proceso con `ProcessPhoenix` (patrón ya existente).
- **Rationale**: YAGNI; el `Retrofit` singleton se construye una vez leyendo `InstancePrefs.baseUrl`, por lo que el rebirth es la vía más simple y ya probada para repuntar la red. Elimina la lógica de credenciales de `ServerRepository.switchTo`/`delete`.
- **Alternatives considered**:
  - Base URL dinámica por petición (interceptor que lee un holder): más flexible pero añade concurrencia y tests; innecesario si el rebirth sigue disponible.
  - Usar solo Room (`isDefault` para arrancar): requeriría leer Room de forma bloqueante al crear el grafo Hilt; se conserva DataStore como caché de arranque.

## 4. Onboarding/creación del primer servidor

- **Decision**: `InstanceSetupViewModel.connect` normaliza la URL (`NormalizeBaseUrlUseCase`), valida con `InstanceValidator` (`/health` + `/config`), persiste `baseUrl`/`setupDone` y crea el servidor activo/por defecto vía `ServerRepository`. Se retira `switchInstance` (código muerto) y la dependencia de `SessionManager`.
- **Rationale**: FR-002/FR-003/FR-004; el modelo deja de tener servidores "implícitos" en DataStore sin fila en la BD.
- **Alternatives considered**: dejar que `MigrationHelper` cree la fila más tarde; rechazado por fragilidad y porque el helper tiene un bug de `collect` que nunca completa (ver §9).

## 5. Modelo de datos: quitar `userEmail` (Room 6→7)

- **Decision**: eliminar `userEmail` de `SavedServerEntity` y `SavedServer`, añadir `MIGRATION_6_7` que recrea `saved_servers` sin la columna (crear tabla nueva → copiar → borrar → renombrar → recrear índices), y actualizar `TolochaDb` a v7.
- **Rationale**: FR-009 (un servidor solo tiene URL, alias y estados) y SC-006 (cero credenciales). `ALTER TABLE ... DROP COLUMN` solo existe desde SQLite 3.35 (Android 12+), incompatible con `minSdk 26`; la recreación de tabla es el patrón estándar de Room.
- **Alternatives considered**:
  - Destructive migration: rechazada por perder los servidores guardados (FR-010).
  - Mantener la columna sin usarla: viola FR-009 y deja datos de usuario en disco.

## 6. Migración de datos de autenticación existentes

- **Decision**: al actualizar, los servidores guardados se conservan sin credenciales; `TokenStore` deja de existir, con lo que `EncryptedSharedPreferences` (`tolocha_tokens`) queda huérfano. Añadir una limpieza explícita de ese almacén en el arranque (una sola vez) y retirar sus exclusiones de backup.
- **Rationale**: FR-010 (sin crash) y FR-009/SC-006 (no debe quedar ninguna credencial en el dispositivo). Sin limpieza, los tokens/email/password cifrados permanecerían en disco.
- **Alternatives considered**: ignorar el archivo huérfano; rechazado por SC-006. Borrado total de la app; rechazado por FR-010.

## 7. Reproductor sin Bearer

- **Decision**: renombrar `AuthDataSourceFactory` a `PlayerDataSourceFactory` y construir un datasource sin cabeceras de autorización; `StationMediaItemFactory` mantiene la URI del proxy (`/api/v1/playback/{id}`) y la elección HLS/progresivo por extensión (spec 0021). `PlaybackApi.streamUrl` no cambia.
- **Rationale**: FR-005/FR-011/FR-014 y la clarificación de acceso anónimo; el playback de la 0021 debe seguir igual salvo la credencial.
- **Alternatives considered**: mantener `authDataSource` con token vacío; rechazado por claridad y para no dejar superficie de auth.

## 8. Shortcuts, ajustes y navegación sin sesión

- **Decision**:
  - `ShortcutSyncCoordinator` publica el historial en cuanto hay servidor configurado, sin `SessionManager`.
  - `ResolveShortcutLaunchUseCase`/`ShortcutLaunchResolution` eliminan `AuthState`, `GoLogin` y `Wait`; el único destino posible es contenido (o la bienvenida si no hay servidor).
  - `SettingsViewModel`/`SettingsScreen` retiran `LogoutUseCase` y el botón "Cerrar sesión"; el tema deja de sincronizarse con el servidor (`UserRepo.patchMe`) y se guarda solo local (DataStore).
  - `TolochaNavGraph` retira `AUTH_REQUIRED`, destinos y ramas de login/registro, y recibe `hasServers` como entrada de arranque.
- **Rationale**: FR-001/FR-005/FR-008 y eliminar dependencias de sesión en consumidores.
- **Alternatives considered**: mantener el tema en servidor; descartado porque el endpoint era autenticado y ya no hay usuario.

## 9. Bugs y deuda encontrados en la exploración (a corregir dentro de esta spec)

- `MigrationHelper.migrateIfNeeded()` usa `instancePrefs.baseUrl.collect { }` (nunca completa) antes de `sessionRestorer.restore()`: debe ser `first()`. Al retirar la sesión, el helper se simplifica y deja de bloquear el arranque.
- `InstanceSetupViewModel.switchInstance` no tiene llamadas: eliminar.
- `feature/auth/ForgotScreen.kt` no tenía ruta: eliminar.
- `ServerRepository.getDefault()`/`ServerDao.getDefault()` sin consumidor de producción: conservar solo si el gate de arranque los usa; en caso contrario, eliminar.
- `Routes.SERVERS` estaba en `AUTH_REQUIRED` pero se abría sin sesión (inconsistencia): desaparece al retirar el set.

## 10. Contrato del backend asumido

- **Decision**: asumir que `GET /api/v1/**` (stations, favorites, history, custom-stations, playback y status) responde sin `Authorization`, con el mismo formato de errores `{error:{code,message,status,details?}}`. No se conserva compatibilidad con instancias que devuelvan 401 por falta de auth (FR-016): ese caso se muestra como "instancia no actualizada".
- **Rationale**: FR-011/FR-016 y clarificación Q1/Q3.
- **Alternatives considered**: fallback a login; descartado explícitamente.

## 11. Manejo de errores de acceso

- **Decision**: retirar `DomainError.Unauthorized` como "sesión expirada" y mapear 401/403 a un mensaje accionable de "instancia no compatible/actualiza el servidor" (sin re-login). Los fallos de red/instancia inalcanzable mantienen los mensajes actuales con reintento.
- **Rationale**: FR-012 y Principio IV (estados sellados, mensajes en español); sin sesión no hay renovación posible.
- **Alternatives considered**: tratar 401 como error genérico; rechazado por UX (el usuario debe saber que el servidor no es compatible).

## 12. Dependencias y limpieza

- **Decision**: eliminar `androidx.security:security-crypto` de `app/build.gradle.kts` y `gradle/libs.versions.toml` (único consumidor: `TokenStore`); retirar exclusiones de tokens en `backup_rules.xml` y `data_extraction_rules.xml`; eliminar `UserApi`/`UserRepo` y los DTOs de auth/`user` no usados (conservar `AppConfigDto` y `PlaybackStatusDto`).
- **Rationale**: Principio V (código y dependencias muertas se eliminan) y SC-004/SC-006.
- **Alternatives considered**: dejar la dependencia "por si acaso"; rechazado por YAGNI.
