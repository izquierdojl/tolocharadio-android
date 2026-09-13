# Bug Assessment: Favoritos no disponibles tras reposo prolongado

- **Slug**: 0029-jlizquierdo-20260913-favorites-stale-after-idle
- **Created**: 2026-09-13
- **Source**: pasted text
- **Verdict**: likely valid, needs reproduction
- **Severity**: high

## Report (verbatim or summarized)

Reportado por el usuario (texto pegado, resumido):

> Cuando el móvil lleva en reposo unos minutos u horas y vuelvo a la aplicación, las favoritas no aparecen disponibles. Supongo que la aplicación se queda en caché pero se pierde la conexión. Investiga por qué sucede y si se puede revisar. Podría hacerse también que, si no las puede cargar, intente refrescarlas con las credenciales guardadas (que sí las tiene, porque el resto de opciones funcionan).

## Symptom

Al volver a la app tras un reposo prolongado (proceso en segundo plano/doze), la pestaña **Favoritos** no muestra la lista (estado de error o vacío) aunque las credenciales guardadas son correctas y el resto de la app (catálogo público, reproducción) funciona. Se espera que la app restaure la sesión automáticamente y recargue las favoritas, o al menos muestre la última copia en caché.

## Reproduction

1. Configurar un servidor con credenciales válidas y abrir Favoritos al menos una vez (la caché Room se llena en `FavoritesRepo.list()`).
2. Dejar el móvil en reposo varios minutos/horas (pantalla apagada; el SO puede matar el proceso o dejar el access token en memoria caducado).
3. Volver a la app (icono o recientes) y abrir **Favoritos**.
4. Observado: la lista no se carga; otros apartados (Explorar/playback) sí funcionan. [NEEDS CLARIFICATION: ¿el estado observado es `Error` con "Reintentar/Editar servidor", `Empty` ("Aún no tienes favoritas") o `Loading` infinito?]
5. Pulsar "Reintentar" (si aparece) y comprobar si los favoritos cargan. [NEEDS CLARIFICATION: ¿se probó?]

## Suspected Code Paths

- `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/FavoritesRepo.kt:45-57` — `list()` solo usa la caché Room cuando el error es `DomainError.Unavailable`; un fallo de credenciales (`Unauthorized`) devuelve error y descarta la caché.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModel.kt:78-113` — la carga ocurre una sola vez en `init` y en `retry()` manual; no hay recarga al volver a primer plano ni al restaurarse la sesión.
- `app/src/main/java/com/izquierdojl/tolocharadio/core/network/TokenAuthenticator.kt:37-75` — renovación ante 401: si `TokenStore.getActiveServerId()` es `null` retorna sin intentar refresh/re-login (L45); cualquier fallo de refresh/login (incluido `IOException` transitorio al despertar la red) hace `session.clear()` (L72) y devuelve 401 al llamante.
- `app/src/main/java/com/izquierdojl/tolocharadio/core/session/SessionManager.kt:25-53` — el access token es solo en memoria; si el proceso muere, se pierde, y no hay re-autenticación al reanudar (solo al crear la Activity).
- `app/src/main/java/com/izquierdojl/tolocharadio/MainActivity.kt:98-101` — `LaunchedEffect(gate) { authenticateServer() }`: auto-login solo al componer, no en `ON_RESUME`; el resultado del auto-login se ignora (sin reintento).
- `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/servers/ServerRepository.kt:100,136,159` — `tokens.setActiveServerId(...)` solo se llama al añadir el primer servidor, al cambiar de servidor y al borrar. `update()` (L108-122, usado por el formulario de edición y por la migración 0022→0024) **no** lo establece.
- `app/src/main/java/com/izquierdojl/tolocharadio/data/local/servers/MigrationHelper.kt:32-42` — crea el `SavedServer` migrado sin `active_server_id`, y `AuthenticateServerUseCase` (L20-25) tampoco lo sincroniza.
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesScreen.kt:171-176` — en error de credenciales se muestra "Editar servidor"; el usuario puede percibirlo como "no aparecen disponibles".
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/explore/ExploreViewModel.kt:92` — el catálogo es público y `favorites.list()` se dispara en paralelo; explica que "el resto funcione" aunque falle la sesión.

## Root Cause Hypothesis

**Confidence: medium-high** (defectos confirmados en código; falta reproducir en dispositivo para fijar el disparador exacto).

Tras el reposo, el access token en memoria ([SessionManager]) caduca o se pierde con el proceso, y las peticiones autenticadas (favoritos/historial) dependen de dos mecanismos frágiles: (a) el auto-login de arranque, que solo corre al crear la Activity y cuyo fallo se ignora; y (b) el `TokenAuthenticator`, que ante un 401 necesita `active_server_id` y, si no está sincronizado (caso de servidores migrados desde la 0022 o editados vía `update()`, que es el camino del formulario bloqueante de la 0024), **no intenta refresh ni re-login** y devuelve 401. Además, si el refresh/login falla por causa transitoria (red aún no despierta tras doze), el authenticator limpia la sesión y el llamante recibe `Unauthorized`; como `FavoritesRepo.list()` solo cae a caché con `Unavailable`, la pantalla muestra error de credenciales en lugar de las favoritas cacheadas o de reintentar cuando la red/sesión vuelven. No hay recarga de Favoritos al reanudar ni observación de `SessionManager.state`, así que el estado roto persiste hasta reiniciar o editar el servidor.

Defectos concretos confirmados (cada uno reproducible con el código actual):

1. `TokenAuthenticator` no resuelve credenciales si `active_server_id` es null, aunque existan para el servidor de arranque (`TokenAuthenticator.kt:45`; `ServerRepository.update()` no lo setea; `MigrationHelper` no lo setea).
2. `TokenAuthenticator` trata cualquier fallo (incluido `IOException`/5xx transitorio) como credenciales inválidas y llama `session.clear()` (`TokenAuthenticator.kt:72`), violando la renovación "sin pedir nada" de la constitución IV.
3. `FavoritesRepo.list()` no cae a caché en errores de autenticación (`FavoritesRepo.kt:53-55`), con lo que la degradación elegante de la constitución IV no se cumple.
4. `FavoritesViewModel` no reintenta al volver a primer plano ni cuando la sesión pasa a `Ready`; `SessionManager.state` no lo observa nadie en producción.

## Proposed Remediation

**Preferred**: 
1. **Sincronizar y garantizar la identidad del servidor activo** en el arranque: que `AuthenticateServerUseCase`/`MainActivity` (o `ServerRepository.getStartupServer()`) llamen a `tokens.setActiveServerId(server.id)`, y que `ServerRepository.update()` lo haga también para el servidor editado. Alternativa/complemento: que `TokenAuthenticator` caiga al servidor de arranque de Room cuando `active_server_id` sea null.
2. **Hacer tolerante el authenticator**: distinguir fallo transitorio (`IOException`, 5xx) de credenciales inválidas (401/403 del refresh y del login); no limpiar la sesión en el primer caso y permitir que el siguiente request reintente.
3. **Reintentar con credenciales en primer plano**: al reanudar la Activity, si `SessionManager.state != Ready`, ejecutar `authenticateServer()`; en Favoritos/Historial, recargar en `ON_RESUME` (o al observar `state == Ready`) para cubrir "si no las puede cargar, que intente refrescarlas".
4. **Fallback a caché en errores de autenticación**: en `FavoritesRepo.list()` (y `HistoryRepo`), si la renovación no es posible, devolver la caché con `offline = true` en lugar de error duro; reservar "Editar servidor" para cuando no haya caché. Mantener el banner "Mostrando caché sin conexión".

**Alternatives**:
- Reintento central en repos: antes de cada llamada autenticada, si `SessionManager.state != Ready`, llamar a `AuthRepo.ensureSession(serverId, baseUrl)` y reintentar una vez. Más explícito pero duplica la lógica del authenticator y añade dependencia data→AuthRepo en cada repo.
- Solo caché + reintento manual: mitiga el síntoma pero no cumple FR-005 de la 0024 (renovación transparente) ni la constitución IV.

**Files likely to change**:
- `app/src/main/java/com/izquierdojl/tolocharadio/core/network/TokenAuthenticator.kt`
- `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/AuthRepo.kt` (single-flight de refresh, si aplica)
- `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/FavoritesRepo.kt` (y `HistoryRepo.kt` por simetría)
- `app/src/main/java/com/izquierdojl/tolocharadio/feature/favorites/FavoritesViewModel.kt` (y `HistoryViewModel.kt`)
- `app/src/main/java/com/izquierdojl/tolocharadio/data/repo/servers/ServerRepository.kt` / `domain/auth/AuthenticateServerUseCase.kt` / `MainActivity.kt` (sincronizar `active_server_id` y auto-login en reanudación)

**Tests to add or update**:
- `TokenAuthenticatorTest`: refresh con `IOException` → **no** limpia la sesión y no consume el refresh guardado; `active_server_id` null → resuelve el servidor de arranque y renueva.
- `FavoritesRepoTest`: 401 con caché disponible → `Ok(FavoritesResult(offline = true))`; 401 sin caché → `Unauthorized`.
- `AuthRepoTest`/`ServerRepositoryTest`: `update()` (y arranque) sincronizan `active_server_id`; `ensureSession` no re-loguea si el refresh sigue válido.
- `FavoritesViewModelTest`: `refresh()` tras reanudar recupera `Content` cuando la sesión vuelve a `Ready`; el error de credenciales sin caché sigue ofreciendo "Editar servidor" (FR-006).

## Risks & Considerations

- No introducir tormentas de refresh concurrentes: `AuthRepo.ensureSession` no tiene single-flight; el authenticator usa `Mutex`, pero un refresh simultáneo con auto-login puede rotar el refresh token dos veces. Revisar durante el fix.
- El fallback a caché en error de credenciales podría enmascarar credenciales realmente inválidas: mostrar siempre el banner offline y mantener accesible "Editar servidor".
- No loguear credenciales/tokens (constitución IV y FR-001 de la 0024); solo `code`/`status` y stationId anonimizado.
- Si el backend devuelve 403 (no 401) para el access caducado, el `Authenticator` de OkHttp no se invoca (solo actúa en 401); la constitución IV pide cubrir 401/403. Verificar con el backend.
- Cambio de comportamiento visible (FR-006): con caché disponible ya no se verá el banner de "Editar servidor"; decidir UX (banner offline + snackbar con acción).
- Mantener HTTPS-only y sin nuevas dependencias (YAGNI).

## Open Questions

- [NEEDS CLARIFICATION: ¿qué estado exacto muestra Favoritos tras el reposo: error con botón, lista vacía o carga infinita? ¿Aparece "Editar servidor" o "Reintentar"?]
- [NEEDS CLARIFICATION: ¿el servidor se configuró/editó después de la 0024 (alta) o se migró de la 0022 (edición bloqueante)? Determina si `active_server_id` está a null en el dispositivo.]
- [NEEDS CLARIFICATION: ¿el fallo se reproduce con el proceso vivo (recientes) o solo tras matar la app? ¿Con qué conectividad (Wi-Fi/datos) al reanudar?]
- [NEEDS CLARIFICATION: ¿el backend devuelve 401 o 403 con el access token caducado, y cuál es el TTL de access/refresh?]
