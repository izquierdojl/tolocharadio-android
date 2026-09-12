# TolochaRadio Android

Cliente Android de [TolochaRadio](https://github.com/izquierdojl/tolocharadio):
radio en línea self-hosted. Las credenciales se configuran **por servidor**
(email y contraseña cifrada) y la sesión es automática: no hay pantallas
de login. Look-and-feel en paridad con la app web (paleta pine/ochre/moss,
emblema Sierra) con Material Design 3 y reproductor persistente (Media3).

## Requisitos

- JDK 21 (toolchain Gradle), Android SDK (API 37), `minSdk=26`.
- Una instancia TolochaRadio con login email/contraseña (JWT + refresh),
  p. ej. `https://radio.mi-dominio.com` (la app la pide en el formulario
  de servidor; override local con `tolocha.baseUrl` en `local.properties`).
- **Instancia con autenticación**: favoritos, historial y la reproducción
  por proxy exigen sesión; la app la obtiene con las credenciales del
  servidor, sin pedirlas en cada uso.

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
(`MediaSessionService`), Room (caché) + DataStore, Coil. Credenciales por
servidor cifradas (`EncryptedSharedPreferences`) y auto-login JWT con
refresh rotatorio; playback por el proxy con `Authorization: Bearer`
(nunca en la URL).

Especificación: `specs/0024-jlizquierdo-20260912-per-server-credentials/`
(spec, plan, tasks); previas: `specs/0022-jlizquierdo-20260912-server-only-access/`,
`specs/001-auth-explore-base/`.

## Tema (spec 002)

- Tokens de color en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/theme/Theme.kt`
  (fuente de verdad; `res/values/colors.xml` solo alimenta los temas XML de
  launcher/splash). Contraste WCAG AA verificado en `ContrastTest`.
- Selector Sistema/Claro/Oscuro en Configuración → se guarda local en DataStore
  `theme_mode` (default: sistema). Sin fuente externa: solo fuente del sistema.

