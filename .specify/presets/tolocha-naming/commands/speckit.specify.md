---
description: Tolocha naming para specs nuevas - NNNN-author-fecha-slug con rama del mismo NNNN (wraps core specify)
---

# Tolocha Naming — override prioritario (preset `tolocha-naming`, strategy `wrap`)

> NO editar `.opencode/commands/speckit.specify.md` directamente: ese archivo se
> regenera en cada `specify integration upgrade` / `specify preset ...`.
> Esta convencion vive en `.specify/presets/tolocha-naming/` (instalado desde
> `presets/tolocha-naming/` con `specify preset add --dev`) y sobrevive a las
> actualizaciones de Spec Kit porque los presets no forman parte del core.
> Solo aplica a specs NUEVAS. Las ya creadas (`001-...` a `015-...`) NO se renombran.

## Formato obligatorio para NUEVAS features

`specs/NNNN-author-YYYYMMDD-slug/`

Ejemplo: `specs/0016-jlizquierdo-20260909-chromecast-retry/`

- **NNNN**: 4 digitos, contador global secuencial compartido con bugs y ramas.
  Calculo: listar `specs/*/`, `.specify/bugs/*/`, `git branch -a` (locales +
  remotas); en cada nombre extraer el prefijo `^(\d{3,})-` ignorando timestamps
  `^\d{8}-\d{6}-`; tomar el maximo y sumar 1; formatear a 4 digitos
  (`16` -> `0016`, `123` -> `0123`, `1234` -> `1234`).
  Los legados `001`–`015` cuentan como 1–15, asi que la proxima nueva es `0016`.
  No reutilizar numeros aunque se haya borrado un directorio.
- **author**: `git config user.name` sanitizado (minusculas, `[^a-z0-9]` -> `-`,
  colapsar guiones multiples, recortar guiones extremos). Fallback en orden:
  parte local de `user.email`, luego `$env:USER` / `$env:USERNAME`, luego `unknown`.
  Ejemplo: `jlizquierdo`.
- **fecha**: `YYYYMMDD` local (`Get-Date -Format 'yyyyMMdd'` en PowerShell,
  `date +%Y%m%d` en bash). Ejemplo: `20260909`.
- **slug**: 2–4 palabras kebab-case extraidas de la descripcion.

## Rama (DEBE incluir el mismo NNNN)

- La rama DEBE llamarse exactamente igual que el basename del spec:
  `NNNN-author-YYYYMMDD-slug` (ej. `0016-jlizquierdo-20260909-chromecast-retry`).
  Asi rama <-> spec es 1:1 y `speckit.git.validate` la acepta
  (su regex `\d{3,}-` admite 4 digitos).
- Procedimiento:
  1. Calcular `NNNN` / `author` / `fecha` / `slug` ANTES del hook.
  2. Invocar el hook `speckit.git.feature` con `GIT_BRANCH_NAME=<basename exacto>`.
     Con esa variable el script usa el nombre tal cual (bypass de `branch_template`)
     y devuelve el mismo `FEATURE_NUM`. No dejar que el hook auto-genere con 3 digitos.
  3. Fijar `SPECIFY_FEATURE_DIRECTORY=specs/<basename>` de forma explicita.
     NO usar la auto-generacion `NNN-<short-name>` del core.
  4. Seguir con el resto del core (spec.md, checklists, hooks `after_specify`).
- `branch_template` en `.specify/extensions/git/git-config.yml` se deja vacio a
  proposito: el preset usa nombres exactos y el template solo seria fallback.

## Precedencia

- Esta seccion ANULA cualquier instruccion conflictiva de numeracion del core
  (`NNN`, `timestamp`) que aparece debajo. El resto del core (calidad del spec,
  requirements checklist) se sigue tal cual.

{CORE_TEMPLATE}
