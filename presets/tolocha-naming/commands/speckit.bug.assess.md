---
description: Tolocha naming para bugs nuevos - NNNN-author-fecha-slug (wraps bug assess)
---

# Tolocha Naming — bugs (preset `tolocha-naming`, strategy `wrap`)

> NO editar `.opencode/commands/speckit.bug.assess.md` directamente: se regenera.
> La convencion vive en `.specify/presets/tolocha-naming/`. Solo aplica a bugs NUEVOS.
> Los ya existentes (`chromecast-no-devices-found`, `filter-dropdown-overlap`,
> `miniplayer-npe-station-not-found`) NO se renombran.

## Formato obligatorio para NUEVOS bugs

`.specify/bugs/NNNN-author-YYYYMMDD-slug/` (y `BUG_SLUG=NNNN-author-YYYYMMDD-slug`).

Ejemplo: `.specify/bugs/0017-jlizquierdo-20260909-chromecast-no-devices/`

- **NNNN**: el MISMO contador global secuencial de 4 digitos que las features
  (max de `specs/*/`, `.specify/bugs/*/`, ramas locales y remotas con patron
  `^(\d{3,})-` ignorando timestamps, + 1, formateado a 4 digitos).
  Compartir contador evita que un bug y una feature colisionen en el nombre de rama.
- **author / fecha / slug**: igual que en `speckit.specify` (git user.name
  sanitizado, `YYYYMMDD` local, 2–4 palabras kebab-case).
- Resolucion de slug:
  - Si el usuario pasa un slug completo que ya empieza por `NNNN-`, usarlo verbatim.
  - Si pasa un slug corto (`login-timeout`), anteponer `NNNN-author-fecha-`.
  - En modo interactivo, sugerir el candidato ya con formato completo.
  - En modo automatico, generar con formato completo y garantizar unicidad: si el
    directorio existe, incrementar NNNN (no usar sufijos `-2` del core).
- No sobrescribir un `BUG_DIR/assessment.md` existente.

## Rama para el fix (trazabilidad)

- El flujo bug (`assess` -> `fix` -> `test`) no crea rama automaticamente.
  Antes de `/speckit.bug.fix`, crear/cambiar a la rama `NNNN-author-YYYYMMDD-slug`
  con el MISMO NNNN del bug (via `speckit.git.feature` con `GIT_BRANCH_NAME` exacto).
  Asi la rama incluye el NNNN y se relaciona con `.specify/bugs/NNNN-...`.

## Precedencia

- Esta seccion ANULA la resolucion de slug simple del core (slug corto sin numero).
  El resto del core (ingesta del reporte, severidad, remediation, assessment.md)
  se sigue tal cual.

{CORE_TEMPLATE}
