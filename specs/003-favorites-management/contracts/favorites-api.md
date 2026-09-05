# Contracts (consumidos): API de Favoritos del servidor

**Fuente de verdad**: backend `izquierdojl/tolocharadio`, `GET /api/v1/openapi.json` (base `/api/v1`). Auth: `Authorization: Bearer <access>` (infra `AuthInterceptor` + refresh en `TokenAuthenticator`; sin cambios). Esta app es consumidora — no define estos endpoints.

## `GET /favorites` — lista completa en orden personalizado

- Auth: sí. Respuesta `200`: `{items: Favorite[]}` donde `Favorite = {station: Station, addedAt: long}`.
- Uso: hidrata `FavoritesRepo.observe()` + `favoriteIds` compartido + `favorites_cache` (con `sortIndex` = posición en `items`).
- Errores: `401` → refresh transparente ×1, si falla → logout + Login; `503` → fallback a caché si hay (marca offline) o error + reintento.

## `POST /favorites` — guardar

- Body: `{stationId: string}` (no vacío, VR-03). Respuesta `200/201`: `{favorite: Favorite}`.
- Uso: toggle alta (optimista), deshacer tras quitar (re-`POST` + reinserción en índice previo).
- Errores: `404` → "esta emisora ya no está disponible" (depurar de la lista); `409` → tratar como éxito (ya era favorita, reconciliar); `422` → mensaje por campo; red → reversión + mensaje.

## `DELETE /favorites/:stationId` — quitar

- Respuesta `200`: éxito aunque no existiera (idempotente). Uso: toggle baja + quitar desde lista (optimista + `Snackbar` 10 s).
- Errores: red → reversión + mensaje; `404` → reconciliar como no-favorita.

## `PUT /favorites/order` — orden personalizado

- Body: lista completa de ids en el nuevo orden: `{stationIds: ["uuid-1", "uuid-2", ...]}` — **confirmado contra `apps/api/src/routes/favorites.ts` (T001, 2026-09-05): `reorderSchema = z.object({ stationIds: z.array(...).min(1) })`, respuesta `{ok: true}`**. El servidor exige permutación exacta.
- Uso: autoguardado al soltar tras drag & drop; validación previa en dominio (VR-02).
- Errores: `422`/divergencia → gana servidor (recargar `GET`, descartar local, avisar) + reintento manual.

## Formato de errores (todos)

`{error: {code, message, status, details?[{field, message}]}}` → mapeo existente `ApiError → userMessage()` en español; prohibido mostrar `message` crudo o PII.
