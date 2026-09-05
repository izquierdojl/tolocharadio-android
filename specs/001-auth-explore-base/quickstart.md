# Quickstart (001-auth-explore-base)

## Apuntar a tu instancia

1. Al primer arranque introduce la `baseUrl`, p. ej.
   `https://radio.mi-dominio.com` (se valida con `/health` + `/config`).
2. Para desarrollo, default en `local.properties`:
   `tolocha.baseUrl=https://radio.mi-dominio.com` (nunca commitear URLs
   con credenciales; no hay secretos en esta API).

## Comandos

- `./gradlew assembleDebug` — compilar.
- `./gradlew testDebugUnitTest` — unitarios (ViewModels, UseCases,
  repos, DTOs contra OpenAPI).
- `./gradlew connectedDebugAndroidTest` — Compose Test (login,
  explorar+paginar, play/stop, favoritar, reintento).
- `./gradlew detekt ktlintCheck lintDebug` — calidad estática.

## Flujo manual de verificación

1. Onboarding → Home pública.
2. Registro (si habilitado) o Login → Explorar autenticado.
3. Buscar "jazz", filtrar país, paginar, abrir ficha.
4. Play → mini-player Buffering→Playing; navegar/rotar sin corte;
   pausa/quitar; probar emisora no disponible (`reason` visible).
5. Favoritar desde la tarjeta (optimista); Perfil muestra nombre/tema.
6. Matar la app → sigue autenticado; forzar 401 → refresh invisible.
