# Instalación y compilación

> [← Volver al README](../README.md)

## Opción 1: instalar el APK

La forma más rápida de probar la app es descargar el APK firmado desde [Releases](https://github.com/izquierdojl/tolocharadio-android/releases) e instalarlo en el dispositivo. Al abrirlo por primera vez hay que aceptar la instalación desde orígenes desconocidos si el sistema lo solicita.

Para publicar tus propias releases (firma con keystore, secrets de GitHub y versionado por tag) consulta [docs/RELEASE.md](RELEASE.md).

## Opción 2: compilar desde el código

### Requisitos

- **JDK 17** (CI usa Temurin 17; el toolchain de Gradle resuelve la JVM).
- **Android SDK** con `compileSdk`/`targetSdk` **37**; `minSdk = 26`.
- **Gradle** mediante el wrapper incluido (`gradlew`).
- Una instancia **TolochaRadio** con login email/contraseña (JWT + refresh), por ejemplo `https://radio.mi-dominio.com`.

### Comandos

Windows (PowerShell 7):

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat detekt ktlintCheck lintDebug
.\gradlew.bat connectedDebugAndroidTest   # requiere dispositivo/emulador
```

En CI (Linux) los mismos con `./gradlew`. El APK de depuración queda en `app/build/outputs/apk/debug/`.

### URL por defecto (opcional)

Sin configurar nada, la app pide la URL en el formulario de servidor. Para prellenar una instancia durante el desarrollo, añade en `local.properties` (fichero no versionado):

```properties
tolocha.baseUrl=https://radio.mi-dominio.com
```

## Configurar un servidor

Al arrancar sin servidores, la app muestra el formulario **Añadir servidor**, que pide:

| Campo | Descripción |
| --- | --- |
| URL del servidor | Origen HTTPS de tu instancia (p. ej. `https://radio.mi-dominio.com`) |
| Alias | Nombre corto para identificar la instancia |
| Email de la cuenta | Email con el que te registraste en la instancia |
| Contraseña | Contraseña de esa cuenta |

La app normaliza la URL, valida las credenciales contra `POST /api/v1/auth/login` y guarda el servidor. **No existen pantallas de login ni registro**: si más adelante el token caduca, se renueva solo con el refresh rotatorio y, si falla, la app te lleva a editar el servidor activo.

Las credenciales se almacenan cifradas con `EncryptedSharedPreferences` y quedan excluidas de las copias de seguridad. Nunca se escriben en logs ni se incluyen en la URL.

## Solución de problemas

| Síntoma | Causa probable | Qué hacer |
| --- | --- | --- |
| «Esta instancia requiere autenticación» | Credenciales incorrectas o caducadas | Edita el servidor activo y corrige email/contraseña |
| No conecta | URL mal escrita o sin HTTPS | Revisa en **Servidores → Editar**; solo se admite HTTPS |
| No aparecen dispositivos Chromecast | Permiso de red local denegado | Concede el permiso de dispositivos cercanos en los ajustes del sistema |
| Faltan `ANDROID_*` en CI | Secrets del repositorio sin configurar | Revisa [docs/RELEASE.md](RELEASE.md) |

## Documentación relacionada

| Tema | Documento |
| --- | --- |
| Uso de la app | [docs/uso.md](uso.md) |
| Arquitectura | [docs/arquitectura.md](arquitectura.md) |
| Desarrollo y CI | [docs/desarrollo.md](desarrollo.md) |
| Releases | [docs/RELEASE.md](RELEASE.md) |
