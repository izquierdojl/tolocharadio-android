---
description: Tolocha naming para ramas - nombre exacto con NNNN de 4 digitos via GIT_BRANCH_NAME (wraps git feature)
---

# Tolocha Naming — ramas (preset `tolocha-naming`, strategy `wrap`)

> NO editar `.opencode/commands/speckit.git.feature.md` directamente: se regenera.
> La convencion vive en `.specify/presets/tolocha-naming/`. Solo afecta a ramas NUEVAS.

## Regla

- Cuando el llamante es `/speckit.specify` o `/speckit.bug.assess` bajo este preset,
  la rama YA viene calculada como `NNNN-author-YYYYMMDD-slug` y se pasa con
  `GIT_BRANCH_NAME=<nombre exacto>`. En ese caso: usar el valor tal cual, sin
  aplicar `branch_template` / `branch_prefix`, sin recalcular numero, sin pasar
  `--number` / `--timestamp`. El `FEATURE_NUM` resultante es el `NNNN` del nombre.
- Solo si NO hay `GIT_BRANCH_NAME` (uso manual aislado): calcular el siguiente numero
  con el contador global (max de `specs/*/`, `.specify/bugs/*/`, ramas locales y
  remotas, patron `^(\d{3,})-` ignorando timestamps) + 1, formateado a 4 digitos, y
  construir `NNNN-<slug>`. `branch_template` se mantiene vacio para no interferir.
- La rama final SIEMPRE debe empezar su ultimo segmento por `NNNN-` para que
  `speckit.git.validate` la acepte y para poder relacionarla con
  `specs/NNNN-...` o `.specify/bugs/NNNN-...`.

{CORE_TEMPLATE}
