package com.izquierdojl.tolocharadio.data.local

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Limpia la caché local de datos del servidor (favoritos, historial,
 * emisoras personalizadas) al cambiar de instancia.
 *
 * La caché es de solo lectura (el servidor es la verdad), por lo que
 * limpiarla no causa pérdida de datos — se recargará del nuevo servidor.
 */
@Singleton
class CacheManager
    @Inject
    constructor(
        private val db: TolochaDb,
    ) {
        /** Elimina todos los datos en caché de todas las tablas del servidor. */
        suspend fun clearAll() {
            db.favoritesCache().clear()
            db.historyCache().clear()
            db.customStationsCache().clear()
            db.stationsCache().clear()
        }
    }
