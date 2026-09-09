# Tolocha Naming (preset local)

Convencion del proyecto: features y bugs NUEVOS se guardan como
`NNNN-author-fecha-slug` y la rama incluye el mismo `NNNN`.

- Features: `specs/NNNN-author-YYYYMMDD-slug/` (ej. `specs/0016-jlizquierdo-20260909-chromecast-retry/`)
- Bugs: `.specify/bugs/NNNN-author-YYYYMMDD-slug/`
- Ramas: `NNNN-author-YYYYMMDD-slug` (mismo basename, trazabilidad 1:1)
- `NNNN`: contador global secuencial de 4 digitos (max de `specs/*/`,
  `.specify/bugs/*/`, ramas locales/remotas, +1). Los `001`–`015` existentes
  cuentan como 1–15 y NO se renombran.
- `author`: `git config user.name` sanitizado. `fecha`: `YYYYMMDD` local.

## Instalar / reinstalar

```bash
specify preset add --dev ./presets/tolocha-naming
```

Ver que gana la resolucion:

```bash
specify preset resolve speckit.specify
specify preset resolve speckit.git.feature
specify preset resolve speckit.bug.assess
specify preset list
```

## Por que sobrevive a actualizaciones

- Los comandos usan `strategy: wrap` con `{CORE_TEMPLATE}`: en cada install/upgrade
  Spec Kit recompone `presets -> core`, asi las mejoras del core siguen entrando
  y solo la seccion de nombres queda fijada por este preset.
- No editar `.opencode/commands/speckit.*.md` a mano (se regeneran).
- `git-config.yml` (`branch_template: ""`) es config de proyecto y no la pisa el core.

## Cuando NO usarlo

- Para specs/bugs ya creados: se dejan como estan.
- Si algun dia se quiere otro esquema, `specify preset disable tolocha-naming`
  o `specify preset remove tolocha-naming` y vuelve el comportamiento stock.
