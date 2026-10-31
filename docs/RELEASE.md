# Releases: versionado y APK automático

## Cómo funciona

- El **versionado** se deriva del último **tag Git** `vX.Y.Z` y del tipo de PR:
  - `feat` → sube **MINOR**.
  - `fix`, `perf`, `refactor` o `revert` → sube **PATCH**.
  - `release:major`, un título con `!:` o un cuerpo con `BREAKING CHANGE` → sube **MAJOR**.
  - El resto (por ejemplo `chore`, `docs`, `ci`) **no publica APK** salvo que la PR lleve la etiqueta `release` o `release:*`.
- El **APK se publica al mergear una PR a `main`**, no al subir un tag manualmente.
  El workflow **Release APK** (`.github/workflows/release.yml`) compila
  `assembleRelease` firmado con tu keystore, crea el tag `vX.Y.Z`, publica
  `tolocharadio-vX.Y.Z.apk` en la GitHub Release y muestra la versión en el
  título y el cuerpo de la release.
- La etiqueta `skip-release` cancela la publicación aunque la PR sea `feat`/`fix`.
- Varias PRs mergeadas generan una release por PR. Si quieres agrupar cambios,
  mergea una sola PR con todo el contenido publicable.

## 1. Crear el keystore (una sola vez)

```powershell
keytool -genkeypair -v -keystore release.keystore -alias tolocha `
  -keyalg RSA -keysize 2048 -validity 10000
```

Guarda el fichero y las contraseñas en un lugar seguro (no commitear).

## 2. Configurar GitHub Secrets (una sola vez)

En GitHub → Settings → Secrets and variables → Actions → New repository secret:

| Secret | Valor |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Contenido de `release.keystore` en base64 |
| `ANDROID_KEYSTORE_PASSWORD` | Contraseña del keystore |
| `ANDROID_KEY_ALIAS` | Alias (ej. `tolocha`) |
| `ANDROID_KEY_PASSWORD` | Contraseña de la clave |

Generar el base64 en PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Set-Content keystore.b64
# copiar el contenido de keystore.b64 al secret ANDROID_KEYSTORE_BASE64 y borrar el fichero
Remove-Item keystore.b64
```

## 3. Publicar una release

1. Crea una PR a `main` con los cambios publicables.
2. Usa un título Conventional Commit (`feat: ...`, `fix: ...`) o etiqueta la PR
   con `release`, `release:minor` o `release:major`.
3. Mergea la PR. El workflow calcula la versión, crea el tag `vX.Y.Z`, compila
   el APK firmado y publica la Release con la versión en el título y el cuerpo.

No subas tags `vX.Y.Z` manualmente para publicar: ese flujo ya no dispara el
release. Para evitar una release en una PR concreta, añade la etiqueta
`skip-release`.

Si el workflow falla por falta de secrets, revísalos en Settings → Actions y
relanza el workflow **Release APK** desde la ejecución de la PR.

## 4. Probar la firma en local (opcional)

En `local.properties` (no commiteado):

```properties
tolocha.keystore.path=C:\\ruta\\a\\release.keystore
tolocha.keystore.password=...
tolocha.key.alias=tolocha
tolocha.key.password=...
```

Luego `./gradlew assembleRelease`. Sin keystore, la build release usa la clave
debug (solo para pruebas, no distribuir).
