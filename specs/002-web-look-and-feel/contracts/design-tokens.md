# Contratos de diseño

## design-tokens.md

**Fuente**: `apps/web/src/index.css` (`@theme` + `:root[data-theme]`) — commit congelado documentado en `brand-assets.md`.

### Escalas (fijas, ambos temas)

- pine: `50 #f0f5f1 · 100 #deeae2 · 200 #bcd2c4 · 300 #8cb29a · 400 #5f8f73 · 500 #3c6a4d · 600 #2c4f38 · 700 #203a28 · 800 #17291c · 850 #122016 · 900 #0f1a12 · 950 #08100b`
- ochre: `100 #f7ecdb · 200 #efdabb · 300 #e2c091 · 400 #d3a568 · 500 #c0883e · 600 #a67430 · 700 #8a5f26 · 800 #6b4a1d · 900 #4a3314 · 950 #2b1d0c`
- moss: `300 #cfdaa2 · 400 #b4c47e · 500 #9aae63`

### Roles por tema

| Rol web | M3 destino | Dark | Light |
|---------|-----------|------|-------|
| surface | background+surface | `pine-950` | `#eef3ef` |
| surface-raised | surfaceContainer | `pine-900` | `#ffffff` |
| surface-soft | surfaceContainerLow | `pine-900 @50%` | `#f5f9f6` |
| foreground | onBackground+onSurface | `pine-100` | `#123424` |
| muted | onSurfaceVariant | `pine-400` | `#4c6b58` |
| soft/faint | onSurfaceVariant @niveles | `pine-300/pine-500` | `#263f30/#2c4f38` |
| line / line-strong | outlineVariant / outline | `pine-800/pine-700` | `#d9e4dc/#c4d4c9` |
| brand / onBrand | primary / onPrimary | `ochre-400/pine-950` | `ochre-600/white` (700 si WCAG) |
| mountain (custom) | `Mountain` | `rgba(32,58,40,.45)` | `#b4c9b9` |
| emblem-badge-a/b, sun, line | resueltos en vector | `#08100b/#203a28/#e2c091/#bcd2c4` | `#e3ece5/#c8dbce/#a67430/#3c6a4d` |

### Formas y tipo (contrato)

- Radios: card 16dp · button/field 12dp · chip/play/avatar 100%.
- Tipo sistema: tarjeta `titleMedium semibold 1 línea` · meta `bodySmall muted` · chip `labelSmall UPPER 10sp ls .08em` · logo `titleLarge` con span brand.
- Play: reposo `black50+pine100`, activo `ochre500+pine950`, 40dp, icono `PlayArrow`.
- Degradado tarjeta: `pine-950 80% → transparent` inferior; fondo pantalla: radial pino 16% + radial ocre 22% + lineal surface→raised (solo Home/cabecera).
