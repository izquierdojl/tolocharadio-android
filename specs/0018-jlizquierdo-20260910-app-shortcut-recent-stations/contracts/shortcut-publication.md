# Contract: Publicación de accesos directos dinámicos

**Feature**: `0018-jlizquierdo-20260910-app-shortcut-recent-stations` | **Tipo**: contrato de plataforma (app → lanzador)

Define qué se publica en el menú del icono y cuándo. Cubre FR-001, FR-002, FR-003, FR-004, FR-008, FR-009, FR-010, FR-012 y FR-014.

## Forma de cada acceso directo

| Atributo | Regla |
|----------|-------|
| Identificador | `hist-<stationId>` — determinista y estable; republicar actualiza el mismo elemento |
| Etiqueta corta | Nombre de la emisora recortado (máx. ~25 caracteres) preservando el inicio, con elipsis si aplica |
| Etiqueta larga | Nombre completo recortado (máx. ~60 caracteres) |
| Icono | Favicon de la emisora si se descarga dentro del timeout (~1,5 s); en caso contrario, icono de la app |
| Intent | Ver `shortcut-intent.md` |
| Rank | 0..N-1, donde 0 es la emisora más reciente |
| Persistencia | Dinámico (`setDynamicShortcuts`); no requiere `res/xml/shortcuts.xml` |

## Límite de slots

- `N = min(ShortcutManagerCompat.getMaxShortcutCountPerActivity(context), 5)`.
- Si `N == 0` (lanzador/dispositivo sin soporte o shortcuts desactivados): no se publica nada y no se produce ningún error visible (FR-012).
- Nunca se publican más elementos que slots disponibles; se reservan los slots de acciones propias del sistema y no se alteran (FR-002).
- Con más historial que slots, se publican las N emisoras más recientes (FR-001).

## Contenido y privacidad

- Solo información pública de la emisora: `stationId`, `name`, `favicon`. PROHIBIDO publicar datos de la cuenta, historial con timestamps, tokens o cualquier PII (FR-008).
- Una sola entrada por emisora, en la posición de su reproducción más reciente (FR-003).
- Se incluyen emisoras personalizadas si están en el historial (clarificación 2026-09-10).
- Lista efectiva vacía (sin historial, sin sesión, historial limpiado): no se publica ningún acceso de emisora (FR-004).

## Disparadores de publicación

| Disparador | Acción |
|------------|--------|
| Cambio en `HistoryRepo.items` (nueva reproducción, borrado individual, limpieza) estando en primer plano | Recalcular y `publish()` inmediatamente |
| Login / restauración de sesión (`Authenticated`) | `historyRepo.list()` y `publish()` |
| App pasa a primer plano con sesión | `historyRepo.list()` (red; ante `Unavailable` el repo sirve caché) y `publish()` |
| Logout (`Unauthenticated`) | `clear()` (elimina todos los dinámicos) |
| Cambio de instancia/servidor | `clearNow()` inmediato, antes de recargar datos |
| Cambios ocurridos con la app en segundo plano | Se aplican al volver a primer plano (límite de plataforma, documentado en la spec) |

## Garantías temporales

- Tras una reproducción con la app en primer plano, el menú refleja el nuevo orden en ≤ 5 s (SC-004).
- Publicar no bloquea el hilo principal ni la reproducción; los iconos se resuelven en IO con timeout.
- Las operaciones de publicación/limpieza están serializadas (`Mutex`); no hay publicaciones concurrentes.

## Fallos tolerables

- Error de red al refrescar historial: se publica la última lista conocida (memoria o caché Room) sin avisos (FR-015).
- Fallo de descarga de un favicon: ese acceso usa el icono de la app; el resto se publica igual.
- Excepción de plataforma al publicar (p. ej. `IllegalArgumentException` por límites): se registra de forma estructurada y la app sigue funcionando; nunca se propaga a UI.

## Criterios de aceptación cubiertos

- FR-001, FR-002, FR-003, FR-004, FR-008, FR-009, FR-010, FR-012, FR-014, FR-015; SC-001, SC-003, SC-004, SC-005, SC-007.
