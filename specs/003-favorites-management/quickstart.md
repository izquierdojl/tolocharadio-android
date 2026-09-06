# Quickstart: validar Favoritos — lista, marcado y navegación

**Feature**: `specs/003-favorites-management/` ([spec](spec.md), [plan](plan.md), [contracts](contracts/))

## Prerrequisitos

1. Instancia TolochaRadio válida en `local.properties` (`tolocha.baseUrl=https://<tu-instancia>`) — ver spec 001 (bootstrap + `GET /health`, `GET /config`).
2. Cuenta de prueba con ≥3 favoritas y otra cuenta nueva sin favoritas.
3. Contratos: [favorites-api](contracts/favorites-api.md) (confirmar el nombre del campo de `PUT /favorites/order` en `/api/v1/openapi.json` antes de implementar).

## Validación automatizada

```powershell
# Unitarios + estáticos (gate de merge)
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:detekt
# Foco en lo nuevo:
.\gradlew.bat :app:testDebugUnitTest --tests "com.izquierdojl.tolocharadio.data.repo.FavoritesRepoTest" --tests "com.izquierdojl.tolocharadio.domain.*" --tests "com.izquierdojl.tolocharadio.feature.favorites.*" --tests "com.izquierdojl.tolocharadio.data.local.FavoritesCacheTest"
# UI crítica (requiere emulador/dispositivo):
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.izquierdojl.tolocharadio.feature.favorites.FavoritesScreenTest
```

## Recorrido manual (cuenta con favoritas)

1. Login → **Favoritos** en la bottom bar: se ven las 3+ favoritas en su orden (< 2 s), con fecha "hace X". ✅ US-1.
2. Quitar una con el corazón → `Snackbar` "Deshacer" 10 s → deshacer la restaura en su sitio. ✅ US-2/FR-005.
3. Ir a **Explorar**: las favoritas muestran el corazón relleno (lista y grid) y la ficha también. Marcar una nueva en Explorar → aparece en Favoritos. ✅ FR-003/FR-004.
4. Arrastrar una tarjeta por su asa y soltar primera → se guarda solo; cerrar y reabrir → el orden persiste. ✅ US-3.
5. Activar modo avión con caché previa → se ve la lista marcada offline + reintento; cuenta nueva sin caché → error + reintento. ✅ FR-010.
6. Reproducir una favorita → navegar por Explorar/Perfil/Historial sin corte, mini-player visible. ✅ US-4.
7. Sin sesión → pulsar Favoritos redirige a Login. ✅ FR-007.

## Criterios de aceptación exprés

- [ ] SC-001 < 2 s, SC-003 coherencia 100 %, SC-004 orden persiste, SC-005 audio continuo, SC-006 reversión < 2 s.
- [ ] Cero XML nuevo, cero dependencias nuevas, Detekt/ktlint/Lint en verde, migración Room v1→v2 con test.

