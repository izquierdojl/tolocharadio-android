<!---
## Sync Impact Report

- Version change: 1.1.0 → 2.0.0
- Razón del bump (MAJOR/MINOR/PATCH): MAJOR — se elimina el
  modelo de autenticación de usuario (JWT + refresh + header
  Bearer) de la app y se redefinen los Principios II y IV. Cambio
  incompatible: cualquier PR que asuma cuentas de usuario viola
  esta constitución.
- Principios modificados:
  - II. Stack Kotlin-First, Compose M3 y Media3 → retirados
    JWT/refresh/`Authenticator`/Bearer; el servidor (instancia)
    pasa a ser la unidad de acceso, sin credenciales de usuario.
  - IV. Streaming Robusto y Manejo de Errores → el 401/403 deja
    de mapearse a "sesión expirada → login" y pasa a "instancia
    no compatible/actualizar servidor".
  - I. MVVM + Clean por capas → lista de features sin `auth/` ni
    `profile/`; se añaden `servers/`, `onboarding/` y `settings/`.
  - III. Calidad Test-First → flujos críticos de UI sin
    login/registro; se añaden bienvenida/configuración de servidor.
  - V. Simplicidad (YAGNI) → el backend ya no aporta "cuentas" al
    cliente.
- Secciones modificadas:
  - Technology Stack, Constraints & Security → retirados los
    endpoints `/auth/*` y `/users/me` del contrato de la app, las
    etiquetas `(auth)` de favoritos/historial/custom-stations/
    sugerencias/playback y los requisitos de Bearer,
    almacenamiento cifrado de credenciales y revocación por
    password; self-hosted pasa a lista de servidores.
  - Development Workflow & Quality Gates → paridad de navegación
    sin Login/Registro/Perfil (acceso por servidor configurado);
    destinos de contenido + Configuración/Servidores; ADRs sin
    auth/refresh.
- Secciones añadidas: ninguna (estructura de 5 principios intacta).
- Secciones eliminadas: ninguna.
- Impacto de migración: la app deja de consumir `/auth/*` y
  `/users/me`; los servidores guardados conservan URL/alias y
  pierden credenciales (spec 0022); se asume una instancia
  actualizada que no exige autenticación (las antiguas no se
  soportan). Las referencias de gobernanza de las specs 001 y 007
  quedan marcadas como superseded por la spec 0022.
- TODOs diferidos: ninguno.
--->

# TolochaRadio Constitution

## Core Principles

### I. Arquitectura MVVM + Clean por Capas

Toda funcionalidad MUST seguir MVVM con separación en 3 capas
dentro del módulo (UI → domain → data).

- `UI`: Composables sin lógica de negocio, estado expuesto vía
  `StateFlow`/`UiState` desde `ViewModel`. ViewModel MUST NOT
  referenciar vistas ni `Context` de Activity.
- `domain`: casos de uso (`UseCase`) puros en Kotlin, sin
  dependencias Android. Reglas de negocio (p. ej. favoritos,
  reintentos, selección de emisora, orden personalizado de
  favoritos, gestión de servidores) MUST vivir aquí.
- `data`: repositorios + fuentes (`RemoteDataSource` con Retrofit
  contra `/api/v1`, `LocalDataSource` con Room/DataStore).
  Repositorios MUST exponer `Flow` y ocultar detalles de red/BD
  al dominio.
- Inyección de dependencias con Hilt MUST usarse para ViewModels,
  repositorios y clientes de red/player.
- Modularización pragmática: se empieza con un solo módulo `app`
  organizado por feature espejo de la web (`home/`, `explore/`,
  `favorites/`, `history/`, `customStations/`, `servers/`,
  `onboarding/`, `settings/`, `player/`); se extrae un módulo
  Gradle nuevo solo cuando un feature tiene API estable, tests
  propios y ciclo de cambio independiente.

Rationale: un streaming necesita testabilidad del player y evolución
sin acoplar UI a red/BD. Clean-lite evita la sobreingeniería de
multi-módulo prematuro y mantiene el principio modular del proyecto.

### II. Stack Kotlin-First, Compose M3 y Media3

Kotlin es el único lenguaje de producción. Java solo se admite en
interoperabilidad de librerías.

- UI MUST construirse con Jetpack Compose + Material Design 3
  (tema, color dinámico, tipografía, dark mode). Queda prohibido
  añadir pantallas nuevas con XML/Views salvo interoperabilidad
  justificada en la PR.
- Reproducción MUST implementarse con `androidx.media3` (ExoPlayer
  + `MediaSessionService`) para background playback, notificación
  multimedia y gestión de foco de audio. Está prohibido usar
  `MediaPlayer` crudo o `VideoView` para el stream principal.
- Red: Retrofit + OkHttp + `kotlinx.serialization` contra el
  backend propio `GET/POST /api/v1/**` (spec OpenAPI 3.1 en
  `GET /api/v1/openapi.json`, Swagger en `GET /api/v1/docs`).
  La app opera **sin autenticación de usuario**: el servidor
  (instancia) es la unidad de configuración y acceso. PROHIBIDO
  enviar credenciales, tokens o `Authorization` en cabeceras o
  query. Playback MUST ir por el proxy del servidor
  `GET /playback/{stationId}` vía `DataSource.Factory` de Media3.
  La app NO consume `/auth/*` ni `/users/me`.
- Persistencia local: Room para la lista de servidores
  (`saved_servers`, sin credenciales), snapshot de favoritas/
  historial/custom-stations (caché offline de lectura), DataStore
  para ajustes, tema, pantalla de arranque y `baseUrl` del
  servidor activo. PROHIBIDO persistir tokens o contraseñas.
  Imágenes: Coil para `favicon`.
- Concurrencia MUST usar corrutinas + `Flow`; `LiveData`, callbacks
  anidados y `GlobalScope` están prohibidos en código nuevo.
- `minSdk = 26`, `targetSdk` = última estable. Todo código nuevo
  MUST ser null-safe y respetar `compileSdk` declarado en Gradle.

Rationale: Compose M3 acelera UI consistente; Media3 es el estándar
soportado para radio en segundo plano; al no haber cuentas, el
servidor autohospedado define el acceso y el cliente no custodia
credenciales.

### III. Calidad Test-First (NON-NEGOTIABLE)

Tests unitarios son obligatorios. Ninguna PR que añada o cambie
lógica de `domain`, `data` o `ViewModel` puede mergearse sin tests.

- Cobertura mínima exigible: ViewModels, UseCases y Repositorios
  MUST tener tests JUnit + corrutinas (`kotlinx-coroutines-test` +
  Turbine). Se recomienda MockK o Mockito-Kotlin. Los contratos
  DTO del backend (Station, Favorite, HistoryEntry, paginación)
  MUST tener tests de serialización contra ejemplos del OpenAPI.
- Regla Red-Green: el test que reproduce el bug o la regla nueva
  MUST existir y fallar antes del fix/implementación.
- Tests de UI Compose (`composeTestRule`) REQUIRED solo para
  flujos críticos: bienvenida/configuración de servidor, explorar
  con filtros, play/stop, favoritas, historial, custom-station,
  cambio de servidor, estado de error con reintento y mini-player
  persistente.
- Calidad estática REQUIRED en CI: Android Lint + ktlint + Detekt
  sin errores. Warnings nuevos MUST justificarse en la PR.
- Flaky tests MUST ponerse en cuarentena con issue enlazado y
  fix en ≤ 2 sprints; no se permite `@Ignore` sin issue.

Rationale: el streaming falla en red intermitente y ciclos de vida;
sin tests del estado del player y repositorios, cada fix rompe
regresiones silenciosas.

### IV. Streaming Robusto y Manejo de Errores

La app MUST degradarse con elegancia ante fallos de red, stream
caído o interrupciones del sistema. Nunca pantalla negra ni crash
silencioso.

- Todo estado del player MUST modelarse como `UiState` sellado
  (p. ej. `Idle/Buffering/Playing/Paused/Error`) con mensaje
  accionable y botón de reintento.
- Formato de error del backend `{error:{code,message,status,
  details?[{field,message}]}}` MUST mapearse a errores de dominio
  tipados (401/403 instancia no compatible o que exige
  autenticación → mensaje accionable "actualiza el servidor", sin
  ofrecer login; 404 emisora no encontrada, 409 conflicto, 422
  validación con `details` por campo, 503 RadioBrowser caído con
  caché si hay). Mensajes al usuario en español, sin filtrar PII
  ni volcar `message` crudo del servidor si es técnico.
- Antes de reproducir una emisora dudosa SHOULD consultarse
  `GET /playback/{stationId}/status` (`{id,playable,reason}`) para
  mostrar "no disponible" sin arrancar el player en vano.
  Escuchar vía proxy MUST registrar historial en servidor; el
  historial local es solo caché de lectura.
- Reintentos MUST usar backoff exponencial con límite y respetar
  `ConnectivityManager`; la app MUST reanudar o pausar según
  preferencia del usuario al recuperar red.
- Foco de audio, llamadas entrantes y Bluetooth MUST pausar/bajar
  volumen según política de Media3; la sesión multimedia MUST
  sobrevivir a rotación y background vía servicio.
- Errores MUST loguearse de forma estructurada (tag + causa +
  `code`/`status` + stationId anonimizado) sin filtrar PII.
  `try/catch` genéricos que tragan excepciones están prohibidos.
- APIs públicas de `domain`/`data` MUST documentarse con KDoc
  (contrato, errores posibles, hilos/dispatcher).

Rationale: la propuesta de valor de una radio es continuidad y
feedback claro. Respetar el contrato de errores y el precheck de
playability evita reproductores colgados y pantallas de error
confusas.

### V. Simplicidad Modular (YAGNI)

Se elige la solución más simple que cumpla el caso de uso actual.
Ninguna abstracción sin dos consumidores reales.

- Prohibido introducir multi-módulo Gradle, BaaS, caché offline
  de audio o DRM hasta que una spec aprobada lo exija. El
  servidor ya resuelve catálogo (RadioBrowser + caché) y proxy;
  el cliente no duplica esa lógica.
- Cada clase/función pública MUST tener un propósito claro y
  KDoc si forma parte de `domain`/`data`. Código muerto MUST
  eliminarse, no comentarse.
- Dependencias nuevas MUST justificarse en la PR (tamaño,
  mantenimiento, alternativa con librerías ya aprobadas).

Rationale: el equipo es pequeño y el backend ya existe y es
self-hosted; el cliente Android debe ser una capa fina de
presentación + player, no un segundo backend.

## Technology Stack, Constraints & Security

Stack canónico (verificado contra `izquierdojl/tolocharadio`):

- Arquitectura: **MVVM + Clean por capas** (descartado MVP legacy
  y MVI puro para v1 por boilerplate innecesario).
- Lenguaje/UI: **Kotlin + Compose + Material3**, dark mode por
  defecto con paleta Tema Tolocha (verde-bosque/ocre-montaña,
  silueta de sierra como emblema). AppCompat/XML solo como legado
  a migrar.
- Player: **Media3 ExoPlayer + MediaSessionService** con
  `DataSource.Factory` hacia el proxy del servidor (sin
  credenciales), precheck de `playback/:id/status`.
- Red/BD: **Retrofit + OkHttp + kotlinx.serialization, Coil,
  Room + DataStore, Hilt, Coroutines + Flow**.
- Contrato backend (`/api/v1`, fuente de verdad: `openapi.json`):
  - Sistema: `GET /health`, `GET /config` (`{appName}`) para
    validar una instancia antes de guardarla como servidor.
  - Auth de usuario: la app NO consume `/auth/*` ni `/users/me`;
    el acceso es por servidor (instancia) sin credenciales de
    usuario. Las instancias que exijan autenticación no se
    soportan.
  - Catálogo público: `GET /stations?name&country&language&tag&
    limit&offset&unique` (limit 1–100, default 24, `hasMore`),
    `GET /stations/:id`, `GET /stations/countries|languages|
    tags` para filtros.
  - Modelo `Station`: `{id,name,url,homepage,favicon,country,
    countryCode,language,tags[],codec,bitrate,isSsl,lastCheckOk,
    votes,clickCount,isCustom}`. `url` solo se usa vía proxy.
  - Favoritos: `GET/POST /favorites`, `DELETE
    /favorites/:stationId`, `PUT /favorites/order` (permutación
    exacta de ids para orden personalizado); compartidos por
    instancia, sin cuenta de usuario.
  - Historial: `GET /history`, `DELETE /history`,
    `DELETE /history/:stationId`; compartido por instancia, sin
    cuenta de usuario.
  - Emisoras personalizadas: `GET/POST /custom-stations`
    (`{name(1–256),url HTTP(S)}`), `DELETE /custom-stations/:id`.
  - Sugerencias de género: `GET/POST /suggestions`
    (`{genre}`), `DELETE /suggestions/:id`.
  - Reproducción: `GET /playback/:stationId` (stream),
    `GET /playback/:stationId/status` (`{id,playable,reason}`).
  - Errores siempre `{error:{code,message,status,details?}}`.
- Self-hosted: lista de **servidores** (URL + alias) como unidad
  de configuración; `baseUrl` configurable (BuildConfig por
  defecto, editable al añadir un servidor); la app MUST funcionar
  contra cualquier instancia compatible con el OpenAPI 3.1 que no
  exija autenticación de usuario.
- Seguridad: HTTPS-only (`cleartextTrafficPermitted=false`);
  PROHIBIDO almacenar o transmitir credenciales, tokens o
  contraseñas; validación de URL de servidor y de custom-station
  (solo http/https). Sin DRM.
- Rendimiento: arranque en frío < 2s en gama media, paginación
  con `hasMore` (no cargar 100 de golpe salvo caso justificado),
  StrictMode en debug, liberar player al detener la reproducción.
- Calidad: **JUnit + Turbine + MockK, Compose Test, Detekt +
  ktlint + Android Lint, GitHub Actions CI** (obligatorio).

## Development Workflow & Quality Gates

- Flujo: ramas `feature/*` desde `main`, PR pequeña y revisable.
  Cada PR MUST enlazar spec/issue y verificar esta constitución.
- Gates de merge (todos REQUIRED): compilación Gradle OK, tests
  unitarios OK, Detekt/ktlint/Android Lint sin errores, y al menos
  1 aprobación. CI (GitHub Actions) MUST ejecutar estos 4 gates.
- Paridad de secciones con la app web (fuente: `apps/web/src/
  App.tsx` + `pages/`): la navegación Android MUST ofrecer las
  mismas secciones de contenido (`Explorar`, `Favoritos`,
  `Historial`, `Mis emisoras`) más `Configuración` y
  `Servidores`. Sin autenticación de usuario no existen
  `Login`/`Registro`/`Perfil`; el acceso al contenido requiere
  tener al menos un servidor configurado y, sin ninguno, se
  muestra la bienvenida/onboarding. Nombres de ruta y orden del
  menú SHOULD espejar la web en las secciones de contenido
  (`/explorar, /favoritos, /historial, /mis-emisoras`).
- Dirección UX estilo Pocket Casts con M3: `NavigationBar`
  inferior con los destinos de contenido (Explorar, Favoritos,
  Historial, Mis emisoras, Configuración/Servidores),
  `StationCard`/`StationListItem` con favicon (Coil) +
  país/idioma/tags, toggle vista lista/grid, `EmptyState`
  dedicados, `FavoriteButton` omnipresente, y reproductor flotante
  persistente (mini-player sobre la bottom bar + full-player en
  bottom sheet) que no se interrumpe al navegar — equivalente al
  `PlayerBar`+Zustand de la web llevado a `MediaSessionService`.
- Versionado: SemVer para releases (`versionName`), `versionCode`
  incremental. Migraciones Room probadas. Versionado del
  contrato: si el OpenAPI sube minor breaking, la app MUST
  declararlo en release notes.
- Observabilidad mínima: crash reporting y eventos de player
  (play/error/buffer) REQUIRED antes de release pública; sin
  telemetría invasiva (principio web: sin seguimiento).
- Documentación: README con cómo compilar/probar y cómo apuntar
  a una instancia (`baseUrl`/servidores), KDoc en APIs públicas de
  `domain`/`data`, y ADRs breves (player, servidores/arranque, BD).

## Governance

La constitución es vinculante y prevalece sobre prácticas
informales. Toda PR y revisión MUST verificar el cumplimiento de
los 5 principios y de los Quality Gates.

- Enmiendas: cualquier cambio requiere PR con justificación,
  impacto de migración y actualización de versión según SemVer
  de gobernanza (MAJOR = eliminación/redefinición incompatible,
  MINOR = principio/sección nueva o guía ampliada,
  PATCH = aclaraciones o redacción).
- Cumplimiento: el revisor puede bloquear el merge por violación
  constitucional; las excepciones MUST registrarse como deuda
  con issue y fecha de revisión (máx. 2 sprints).
- Guía runtime: esta constitución es la fuente de verdad; las
  plantillas `spec/plan/tasks` la leen en cada comando. El
  contrato OpenAPI del backend (`/api/v1/openapi.json`) es la
  fuente de verdad para DTOs y códigos de error.

**Version**: 2.0.0 | **Ratified**: 2026-09-04 | **Last Amended**: 2026-09-12
