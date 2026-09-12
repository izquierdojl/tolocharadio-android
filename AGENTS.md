# AGENTS.md

Guía operativa para asistentes de IA en este repo. Describe **cómo trabajar aquí**;
la gobernanza del producto vive en la constitución.

## Fuente de verdad

- **Constitución (vinculante)**: `.specify/memory/constitution.md` — principios,
  stack, quality gates. Si algo la contradice, prevalece la constitución.
- **Proceso SpecKit**: `.specify/` y comandos `/speckit.*` en `.opencode/commands/`.
- **Funcional**: `specs/NNNN-.../spec.md` (+ `plan.md`, `tasks.md`, `research.md`).
- **Bugs**: `.specify/bugs/NNNN-.../` (`assessment.md`, `fix.md`, `test.md`).
- **Compilar / arquitectura / release**: `README.md`, `docs/RELEASE.md`.

## Comandos

Windows (PowerShell 7):

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat detekt ktlintCheck lintDebug
.\gradlew.bat connectedDebugAndroidTest   # requiere dispositivo/emulador
```

En CI (Linux) los mismos con `./gradlew`. `assembleDebug`, `testDebugUnitTest` y
`detekt ktlintCheck lintDebug` son gates **obligatorios** (`.github/workflows/android.yml`).
No mergear con gates en rojo.

## Flujo de trabajo (SpecKit)

- Feature nueva: `/speckit.specify` → `/speckit.clarify` → `/speckit.plan` →
  `/speckit.tasks` → `/speckit.analyze` → `/speckit.implement`.
- El preset `tolocha-naming` crea `specs/NNNN-author-YYYYMMDD-slug/` y una rama con
  el mismo `NNNN`. Bug: `.specify/bugs/NNNN-author-YYYYMMDD-slug/`.
- Antes de implementar, verificar que el plan no contradice la constitución.
- Cambios de principio/stack/gates → enmendar la constitución con
  `/speckit.constitution` (bump SemVer + Sync Impact Report), no editarla a mano.

## Convenciones

- **Commits**: Conventional Commits en español (`feat:`, `fix:`, `chore:`, `docs:`),
  referenciando la spec (`feat: 0022 ...`).
- Ramas desde `main`, PR pequeña y revisable, enlazando spec/issue.
- **NUNCA commitear, pushear ni abrir PR sin petición explícita del usuario.**
- Idioma: español en docs, mensajes de UI y specs.

## Reglas técnicas no negociables (resumen; manda la constitución)

- MVVM + Clean (`ui → domain → data`); ViewModel sin `Context` de Activity; Hilt.
- Kotlin + Compose Material 3; prohibido XML/Views en pantallas nuevas.
- Media3 (`ExoPlayer` + `MediaSessionService`); prohibido `MediaPlayer`/`VideoView`.
- **Sin autenticación de usuario**: PROHIBIDO almacenar o enviar credenciales,
  tokens o `Authorization`. Playback vía proxy `GET /playback/{stationId}`.
- Tests obligatorios para `domain`/`data`/`ViewModel` (regla Red-Green; JUnit +
  Turbine + MockK). Compose UI solo flujos críticos.
- HTTPS-only; sin PII en logs (tag + causa + `code`/`status`, stationId anonimizado).
- Sin multi-módulo Gradle ni dependencias nuevas sin justificar (YAGNI).

## Entorno

- Windows + PowerShell 7; usar `gradlew.bat`. JVM target 17 (CI: Temurin 17).
- Android SDK: `compileSdk`/`targetSdk` 37, `minSdk` 26.
- spec-kit resuelve plantillas con PowerShell; en Windows el stub `python3` de la
  Microsoft Store ya está parcheado en `.specify/scripts/powershell/common.ps1`.
