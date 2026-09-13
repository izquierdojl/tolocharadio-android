# Desarrollo

> [← Volver al README](../README.md)

## Comandos (Windows / PowerShell 7)

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat detekt ktlintCheck lintDebug
.\gradlew.bat connectedDebugAndroidTest   # requiere dispositivo/emulador
```

En CI (Linux) los mismos con `./gradlew`.

## Tests

- **Obligatorios** para `domain`, `data` y `ViewModel` (regla Red-Green; **JUnit + Turbine + MockK**).
- La UI Compose se prueba solo en los **flujos críticos** (`connectedDebugAndroidTest`).
- Sitúa los tests unitarios en `app/src/test/` y los instrumentados en `app/src/androidTest/`.

## Calidad y gates

`.github/workflows/android.yml` ejecuta en cada push a `main` y en cada PR:

1. `assembleDebug`
2. `testDebugUnitTest`
3. `detekt ktlintCheck lintDebug`

Los tres pasos son **gates obligatorios**: no se debe mergear con ellos en rojo. `detekt` usa `detekt.yml` y `detekt-baseline.xml` (deuda estructural preexistente documentada); el gate sigue activo para issues nuevos.

## Convenciones

- **Kotlin + Compose Material 3**; prohibido XML/Views en pantallas nuevas.
- **Media3** (`ExoPlayer` + `MediaSessionService`); prohibido `MediaPlayer`/`VideoView`.
- MVVM + Clean: `ui → domain → data`; `ViewModel` sin `Context` de Activity; Hilt.
- HTTPS-only; sin PII en logs (tag + causa + `code`/`status`, `stationId` anonimizado).
- Sin multi-módulo Gradle ni dependencias nuevas sin justificar (YAGNI).
- **Commits**: Conventional Commits en español (`feat:`, `fix:`, `chore:`, `docs:`), referenciando la spec (`feat: 0022 ...`).
- Ramas desde `main`, PR pequeña y revisable, enlazando spec/issue.
- Idioma: español en docs, mensajes de UI y specs.

## Flujo de trabajo (SpecKit)

- Feature nueva: `/speckit.specify` → `/speckit.clarify` → `/speckit.plan` → `/speckit.tasks` → `/speckit.analyze` → `/speckit.implement`.
- El preset `tolocha-naming` crea `specs/NNNN-author-YYYYMMDD-slug/` y una rama con el mismo `NNNN`.
- Los bugs viven en `.specify/bugs/NNNN-author-YYYYMMDD-slug/` (`assessment.md`, `fix.md`, `test.md`).
- Cambios de principio, stack o gates se tramitan enmendando la constitución (`.specify/memory/constitution.md`), no editándola a mano.

## Versionado y release

El versionado se deriva del **tag Git**: `vX.Y.Z` → `versionName = X.Y.Z`, y `versionCode` lo pone CI con `GITHUB_RUN_NUMBER`. En local (sin tag) cae a `versionName = 1.0`, `versionCode = 1`.

Al pushear un tag `v*.*.*`, el workflow **Release APK** compila `assembleRelease` firmado y publica `tolocharadio-vX.Y.Z.apk` en la GitHub Release del tag. Los detalles (creación del keystore, secrets y publicación) están en [docs/RELEASE.md](RELEASE.md).

## Entorno

- **Windows + PowerShell 7**; usar `gradlew.bat`.
- **JVM target 17** (CI: Temurin 17).
- **Android SDK**: `compileSdk`/`targetSdk` 37, `minSdk` 26.
- En Windows, el stub `python3` de Microsoft Store ya está parcheado en `.specify/scripts/powershell/common.ps1`.

## Documentación relacionada

| Tema | Documento |
| --- | --- |
| Uso | [docs/uso.md](uso.md) |
| Instalación | [docs/instalacion.md](instalacion.md) |
| Arquitectura | [docs/arquitectura.md](arquitectura.md) |
| Releases | [docs/RELEASE.md](RELEASE.md) |
