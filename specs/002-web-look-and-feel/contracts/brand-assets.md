# Brand assets (origen → uso)

**Origen** (`izquierdojl/tolocharadio`, rama `main`, congelar commit al implementar):

| id | Origen web | Destino Android |
|----|-----------|-----------------|
| SIERRA_EMBLEM | `apps/web/src/components/SierraEmblem.tsx` (viewBox 64, rx14, sol+montaña+antena) | `res/drawable/sierra_emblem.xml` (+ variante `values-night` si se separa), `sierra_emblem_mono.xml`, `TolochaLogo` en cabecera, placeholder custom, empty-state, `largeIcon` notificación |
| FAVICON | `apps/web/index.html` (`<link rel="icon" data:image/svg+xml,…>`, gradiente `#08100b→#203a28`, sol `#e2c091`) | referencia de color para `ic_launcher_background.xml`; no se empaqueta como tal (el launcher es adaptativo) |
| MOUNTAIN_WALL | `apps/web/src/components/AppShell.tsx` → `MountainWall` (path `M0 120 L200 30 …`, viewBox 1200×120) | `res/drawable/mountain_wall.xml` usado en `MountainWall.kt` sobre reproductor/pie, tint `Mountain` |
| GITHUB | path SVG inline en `AppShell.tsx` (`M12 0C5.37…`) | `res/drawable/ic_github.xml` (uso: menú/perfil "Acerca de") |
| LUCIDE→MATERIAL | `lucide-react` (Home, Heart, History, Radio, UserRound, Play, Menu, X, ChevronDown, LogOut) | Material `material-icons-extended`: `Home, Favorite(+Border), History, Radio, Person, PlayArrow/Pause, Menu/Close, ExpandMore, Logout` |

**Reglas**: cada drawable lleva cabecera con URL origen + commit; `var(--*)` siempre resueltos a hex por tema; `mono` sin detalles <2dp; launcher = emblema simplificado sobre `pine-950`; notificación `smallIcon` = mono monocromo.
