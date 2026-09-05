# TolochaRadio Android

Cliente Android de [TolochaRadio](https://github.com/izquierdojl/tolocharadio):
radio en línea self-hosted con cuentas propias. Estética tipo Pocket Casts
con Material Design 3 y reproductor persistente (Media3).

## Requisitos

- JDK 17, Android SDK (API 37), `minSdk=26`.
- Una instancia TolochaRadio, p. ej. `https://radio.mi-dominio.com`
  (la app la pide al arrancar; override local con
  `tolocha.baseUrl` en `local.properties`).

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
