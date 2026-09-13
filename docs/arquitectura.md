# Arquitectura

> [← Volver al README](../README.md)

La app sigue **MVVM + Clean por capas** en un único módulo Gradle (`app`), organizado por feature. La regla de dependencias es siempre `ui → domain → data`: la UI no conoce Retrofit ni Room, y el dominio no depende de Android.

## Capas

| Capa | Paquete | Responsabilidad |
| --- | --- | --- |
| UI | `feature/**` | Composables sin lógica de negocio; estado expuesto vía `StateFlow`/`UiState` desde el `ViewModel` |
| Dominio | `domain/**` | Casos de uso puros en Kotlin: favoritos, historial, servidores, credenciales, reproducción, temporizador, atajos |
| Datos | `data/**` | Repositorios + fuentes remotas (Retrofit contra `/api/v1`) y locales (Room, DataStore, `EncryptedSharedPreferences`) |
| Núcleo | `core/**` | Red, sesión, tema, navegación, utilidades y componentes Compose compartidos |
| DI | `di/**` | Módulos Hilt para ViewModels, repositorios, red, player y almacenamiento |

Detalles relevantes:

- Los `ViewModel` **no** referencian vistas ni `Context` de Activity.
- Los repositorios exponen `Flow` y ocultan los detalles de red/BD al dominio.
- La inyección de dependencias se resuelve con **Hilt**.
- La modularización es pragmática: se extrae un módulo Gradle nuevo solo cuando un feature tiene API estable, tests propios y ciclo de cambio independiente (YAGNI).

## Stack

- **Kotlin** como único lenguaje de producción; Java solo para interoperabilidad de librerías.
- **Jetpack Compose + Material Design 3** para toda la UI (prohibido XML/Views en pantallas nuevas).
- **Media3** (`ExoPlayer` + `MediaSessionService`) para reproducción, background y foco de audio (prohibido `MediaPlayer`/`VideoView`).
- **Retrofit + OkHttp + kotlinx.serialization** contra la API del backend `/api/v1` (OpenAPI 3.1).
- **Room** (caché de servidores, favoritos, historial y emisoras propias), **DataStore** (ajustes, tema, pantalla de arranque) y **EncryptedSharedPreferences** (credenciales).
- **Coil** para carátulas/`favicon`.
- **Corrutinas + Flow** para concurrencia; prohibidos `LiveData`, callbacks anidados y `GlobalScope`.
- **Chromecast** con `media3-cast`, `play-services-cast-framework` y `androidx.mediarouter`.

## Red y sesión

- La app habla exclusivamente con **instancias TolochaRadio propias** (`GET/POST /api/v1/**`).
- **Autenticación por servidor**: cada servidor guarda email y contraseña (esta cifrada). Al activarlo, la app hace login automático (`POST /auth/login`) y renueva con refresh rotatorio (`POST /auth/refresh`).
- Todas las peticiones que lo requieren (incluida la reproducción por proxy, los subrecursos HLS y Chromecast) llevan `Authorization: Bearer`. La credencial **nunca** viaja en la URL.
- Si una petición devuelve 401/403, se reintenta tras renovar el token; si falla, se solicita re-login con las credenciales guardadas y se ofrece editar el servidor activo.
- **No existen pantallas de login/registro**: las credenciales se gestionan en el formulario unificado de servidor.

## Reproducción

- El stream se resuelve mediante el proxy autenticado del backend (`GET /playback/{stationId}`), nunca exponiendo el token.
- `MediaSessionService` mantiene la reproducción en segundo plano, publica la notificación multimedia y gestiona el foco de audio y las pausas por pérdida de foco.
- El reproductor es flotante y persistente: la barra inferior sobrevive a la navegación entre secciones.
- **Chromecast**: la app envía el audio a un dispositivo del Cast, con recuperación automática si se pierde la sesión y fallback al audio local.
- **Temporizador de apagado**: detiene la reproducción tras el tiempo elegido.

## Almacenamiento local

| Dato | Tecnología |
| --- | --- |
| Lista de servidores | Room (`saved_servers`) |
| Caché de favoritos, historial y emisoras propias | Room (lectura offline) |
| Ajustes, tema, pantalla de arranque, `baseUrl` | DataStore Preferences |
| Credenciales (email, contraseña, refresh) | `EncryptedSharedPreferences` |

Las credenciales cifradas quedan **excluidas de la copia de seguridad** y no se registran en logs. Los logs evitan PII (tag, causa y `code`/`status`, con `stationId` anonimizado).

## Notificaciones y atajos

- Bajo el paquete `com.tolocharadio` viven la gestión de notificaciones (canales, acciones, navegación al tocar) y el estado de la app en primer/segundo plano.
- Android Shortcuts publica las **emisoras recientes** en el menú largo del icono, con resolución del intent de arranque.

## Calidad

- `domain`, `data` y `ViewModel` tienen **tests obligatorios** (regla Red-Green; JUnit + Turbine + MockK).
- La UI Compose se prueba solo en los flujos críticos.
- Los gates de `detekt`, `ktlintCheck` y `lintDebug` son obligatorios en CI. Existe un `detekt-baseline.xml` con deuda estructural preexistente.

## Documentación relacionada

| Tema | Documento |
| --- | --- |
| Uso | [docs/uso.md](uso.md) |
| Instalación | [docs/instalacion.md](instalacion.md) |
| Desarrollo y CI | [docs/desarrollo.md](desarrollo.md) |
