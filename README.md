# TolochaRadio Android

Cliente Android de [TolochaRadio](https://github.com/izquierdojl/tolocharadio):
radio en línea self-hosted con cuentas propias. Look-and-feel en paridad
con la app web (paleta pine/ochre/moss, emblema Sierra) con Material
Design 3 y reproductor persistente (Media3).

## Requisitos

- JDK 21 (toolchain Gradle), Android SDK (API 37), `minSdk=26`.
- Una instancia TolochaRadio, p. ej. `https://radio.mi-dominio.com`
  (la app la pide al arrancar; override local con
  `tolocha.baseUrl` en `local.properties`).
- **Instancia actualizada** (con resolución de listas en el proxy): desde la
  spec 0021 las emisoras `.m3u`/`.m3u8`/`.pls` se reproducen por el proxy
  autenticado. Contra una instancia antigua, esas emisoras fallarán con un
  error accionable.

## Compilar y probar

```sh
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew detekt ktlintCheck lintDebug
./gradlew connectedDebugAndroidTest  # requiere dispositivo/emulador
```

## Arquitectura

MVVM + Clean por capas (`ui → domain → data`), Hilt, Retrofit + OkHttp +
kotlinx.serialization contra `/api/v1` (OpenAPI 3.1), Media3
(`MediaSessionService`), Room (caché) + DataStore, Coil. Auth JWT con
refresh rotatorio cifrado; playback por proxy con `Authorization: Bearer`
(nunca en la URL).

Especificación: `specs/001-auth-explore-base/` (spec, plan, tasks).

## Tema (spec 002)

- Tokens de color en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/theme/Theme.kt`
  (fuente de verdad; `res/values/colors.xml` solo alimenta los temas XML de
  launcher/splash). Contraste WCAG AA verificado en `ContrastTest`.
- Selector Sistema/Claro/Oscuro en Perfil → se guarda local en DataStore
  `theme_mode` (default: sistema). Sin fuente externa: solo fuente del sistema.

