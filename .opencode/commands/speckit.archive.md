---
description: Cierra una especificación verificada manualmente — marca tasks de revisión como pasadas por el usuario, hace commit + push (+ PR si procede) e informa del cierre.
---

## User Input

```text
$ARGUMENTS
```

Si `$ARGUMENTS` trae un identificador de feature (p. ej. `002-web-look-and-feel`), úsalo. Si viene vacío, resuelve la feature actual con `.specify/scripts/powershell/check-prerequisites.ps1 -Json` y su `FEATURE_DIR`.

## Goal

Automatizar el cierre de una especificación que el usuario ya ha verificado manualmente (en emulador/dispositivo): dejar `tasks.md` y `spec.md` en estado cerrado, consolidar el trabajo en git (commit + push + PR cuando proceda) e informar de que está cerrado y si queda algo pendiente.

Este comando NO implementa código ni valida gates — eso es trabajo de `/speckit.implement` y `/speckit.converge`. Solo ejecuta el ritual de cierre. Si al revisar encuentras trabajo sin hacer que el usuario no haya aceptado explícitamente, NO lo marques como hecho: pregúntale.

## Steps

### 1. Resolver contexto

- FEATURE_DIR = `specs/<feature>/`; lee `tasks.md` y `spec.md` (solo `Status`).
- Ejecuta `git status --short` y `git branch --show-current` en la raíz del repo.

### 2. Marcar tasks de revisión como pasadas

- Lista las tareas abiertas (`- [ ]`) de `tasks.md`.
- Para cada una abierta, usa la herramienta `question` para que el usuario decida: **pasada** (la verificó manualmente) o **asumida** (no hecha, se acepta así y queda constancia).
- Marca `- [X]` con sufijo de fecha y motivo:
  - pasada: `— verificado manualmente en emulador por el usuario (YYYY-MM-DD)`
  - asumida: `— ASUMIDO sin implementar, aceptado por el usuario al cierre (YYYY-MM-DD)`
- NUNCA marques como pasada una tarea sin confirmación explícita del usuario en este paso.

### 3. Cerrar `spec.md`

- Si todas las tareas quedan `[X]`, cambia `**Status**: Draft` → `**Status**: Done (YYYY-MM-DD; <nota breve: p. ej. "Tx–Ty asumidos sin implementar" o "todo verificado">)`.
- Si ya estaba en `Done`, no lo toques.

### 4. Commit + push (+ PR si procede)

- Revierte ruido del IDE sin preguntar: `git checkout -- .idea/compiler.xml .idea/misc.xml` (solo si están modificados).
- Stagea todo menos IDE u otros directorios relacionados.: `git add . ":!.idea"` (PowerShell: `git add . ':!.idea'`).
- Muestra el `git status` stageado de forma compacta para que quede constancia.
- Commit: `git commit -m "feat: <feature> (<descripción corta>, spec cerrada)"`.
- `git push`. Si la rama actual NO es la principal (`main`/`master`):
  - `git push -u origin <rama>` y crea PR con `gh pr create --fill --base main` (si `gh` no existe, indícalo y termina con push hecho).
  - Informa la URL de la PR.
- Si ya estás en la rama principal, basta con el push (sin PR).

### 5. Informe de cierre

Responde con:

- ✅ Qué se marcó (nº de tasks pasadas vs asumidas).
- ✅ Commit (hash corto) y destino del push (+ URL de PR si se creó).
- ⚠️ Lo que quede pendiente de verdad (tareas ASUMIDO + recordatorio de `connectedDebugAndroidTest`/dispositivo si aplica).
- Nada más que hacer salvo que el usuario quiera retomar lo asumido.
