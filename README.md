# TolochaRadio Android

Cliente Android de [TolochaRadio](https://github.com/izquierdojl/tolocharadio):
radio en línea self-hosted. No usa cuentas de usuario: la app solo
configura servidores (instancias) y accede a su contenido. Look-and-feel
en paridad con la app web (paleta pine/ochre/moss, emblema Sierra) con
Material Design 3 y reproductor persistente (Media3).

## Requisitos

- JDK 21 (toolchain Gradle), Android SDK (API 37), `minSdk=26`.
- Una instancia TolochaRadio, p. ej. `https://radio.mi-dominio.com`
  (la app la pide en la pantalla de bienvenida si no hay servidores;
  override local con `tolocha.baseUrl` en `local.properties`).
- **Instancia actualizada sin autenticación de usuario**: desde la
  spec 0022 la app opera solo con servidores, sin usuario ni contraseña.
  Las emisoras `.m3u`/`.m3u8`/`.pls` se reproducen por el proxy del
  servidor (spec 0021). Contra una instancia que exija login, la app
  mostrará un error accionable.

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
(`MediaSessionService`), Room (caché) + DataStore, Coil. Sin
autenticación de usuario: la app no almacena ni envía credenciales;
playback por el proxy del servidor.

Especificación: `specs/0022-jlizquierdo-20260912-server-only-access/`
(spec, plan, tasks); specs previas: `specs/001-auth-explore-base/`.

## Tema (spec 002)

- Tokens de color en `app/src/main/java/com/izquierdojl/tolocharadio/core/ui/theme/Theme.kt`
  (fuente de verdad; `res/values/colors.xml` solo alimenta los temas XML de
  launcher/splash). Contraste WCAG AA verificado en `ContrastTest`.
- Selector Sistema/Claro/Oscuro en Configuración → se guarda local en DataStore
  `theme_mode` (default: sistema). Sin fuente externa: solo fuente del sistema.

