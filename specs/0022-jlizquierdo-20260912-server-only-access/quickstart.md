# Quickstart — Validación: Acceso solo con servidores y retirada de la autenticación

**Feature**: `0022-jlizquierdo-20260912-server-only-access` | **Date**: 2026-09-12

Guía de validación end-to-end. Referencias de contrato: [contracts/server-only-access.md](./contracts/server-only-access.md) y [data-model.md](./data-model.md).

## Prerrequisitos

- Instancia TolochaRadio **actualizada** (sin auth de usuario) accesible por HTTPS.
- Build de debug: `./gradlew assembleDebug` (o `installDebug` en emulador/dispositivo).
- Para la actualización: dispositivo con una build anterior (con sesión y servidores guardados).

## Escenario 0 — Gates automáticos

```bash
./gradlew assembleDebug testDebugUnitTest detekt ktlintCheck lintDebug
```

Esperado: todo en verde. Los tests de auth/sesión eliminados; los de servidores/onboarding/settings/shortcuts reescritos sin credenciales.

## Escenario 1 — Primer arranque sin servidores (FR-002, SC-001, SC-002)

1. Instala limpia (o borra datos de la app).
2. Abre la app.

Esperado:
- Se muestra la **pantalla de bienvenida** con el mensaje de configurar un servidor.
- No hay campo de usuario ni contraseña, ni pantalla de login/registro.
- No se puede acceder a Explorar/Favoritos/Historial.

## Escenario 2 — Configurar el primer servidor (FR-003, FR-004, SC-003)

1. En la bienvenida, añade URL + alias de la instancia y confirma.
2. Cronometra hasta ver el contenido.

Esperado:
- Validación correcta y acceso al contenido en **< 30 s**.
- El servidor aparece en la sección Servidores como activo y por defecto, **sin datos de usuario**.
- Con URL inválida o http: error en español, no se guarda, se puede reintentar.

## Escenario 3 — Uso sin credenciales (FR-001, FR-005, SC-002, SC-005)

1. Reproduce una emisora.
2. Abre Historial.
3. Abre Favoritos, añade y quita una favorita.
4. Abre Mis emisoras y añade una personalizada.

Esperado: ninguna petición de usuario/contraseña; todo funciona igual que antes; sin `Authorization` en el tráfico (verificable con proxy/logger).

## Escenario 4 — Historial y favoritos compartidos por instancia (FR-015, SC-003)

1. En el dispositivo A, reproduce una emisora y márcala favorita.
2. En el dispositivo B (misma instancia), abre Historial y Favoritos.

Esperado: ambos ven los mismos datos sin iniciar sesión (el servidor es la unidad de datos).

## Escenario 5 — Varios servidores y cambio (FR-006, FR-007)

1. Añade un segundo servidor.
2. Selecciónalo como activo.

Esperado: la app cambia al nuevo servidor, limpia la caché del anterior y carga su contenido, sin credenciales.

## Escenario 6 — Eliminar hasta el último servidor (FR-013)

1. Elimina todos los servidores uno a uno.

Esperado: al borrar el último, la app vuelve a la pantalla de bienvenida.

## Escenario 7 — Instancia no disponible / no actualizada (FR-012, FR-016)

1. Detén el servidor (o apunta a una instancia antigua que exija login).
2. Abre la app o navega.

Esperado:
- Instancia caída: mensaje accionable con reintento; sin cierre ni pantalla en blanco.
- Instancia antigua (responde exigiendo auth): mensaje "instancia no actualizada"; **no** aparece pantalla de login.

## Escenario 8 — Actualización desde versión anterior (FR-010, SC-006)

1. Con una build anterior, guarda un servidor con credenciales y deja sesión iniciada.
2. Instala encima esta build.
3. Abre la app.

Esperado:
- Sin crash; el servidor guardado se conserva **sin credenciales** y sigue disponible.
- No queda ningún token/email/password en el dispositivo (revisar `tolocha_tokens` eliminado).
- No se pide login en ningún momento.

## Escenario 9 — Ajustes y shortcuts sin sesión (FR-008)

1. Cambia el tema y la pantalla de arranque.
2. Cierra y reabre la app.

Esperado: se conservan localmente; **no** existe "Cerrar sesión"; los shortcuts de las emisoras recientes aparecen sin login.
