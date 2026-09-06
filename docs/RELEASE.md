# Releases: versionado y APK automático

## Cómo funciona

- El **versionado** se deriva del **tag Git**: `vX.Y.Z` → `versionName = X.Y.Z`.
  El `versionCode` lo pone CI con `GITHUB_RUN_NUMBER` (creciente por cada build).
  En local (sin tag) cae a `versionName = 1.0`, `versionCode = 1`.
- El workflow **Release APK** (`.github/workflows/release.yml`) se dispara al pushear
  un tag `v*.*.*`, compila `assembleRelease` firmado con tu keystore y publica el
  fichero `tolocharadio-vX.Y.Z.apk` en la **GitHub Release** del tag.

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

```powershell
git tag v1.0.0
git push origin v1.0.0
```

Eso crea automáticamente la Release en GitHub con el APK firmado adjunto
(`tolocharadio-v1.0.0.apk`) y notas generadas desde los commits.

Si el workflow falla por falta de secrets, revísalos en Settings → Actions
y vuelve a lanzar el tag (borra y recrea el tag, o haz re-run del workflow).

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
