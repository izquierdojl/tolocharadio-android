package com.izquierdojl.tolocharadio.cast

/**
 * Contrato del dispositivo remoto (receptor Cast) para el control de silencio (spec 0037).
 *
 * El volumen deja de ser estado de la app (lo representa la barra del sistema vía player
 * nativo de media3 1.11.0): este contrato solo cubre silencio. Abstrae la `CastSession`
 * para que el controlador sea testeable en JVM sin dependencias de framework. Todas las
 * operaciones se invocan desde el hilo principal y **no lanzan**: un fallo del receptor
 * se manifiesta por falta de eco.
 */
interface RemoteVolumeDevice {
    /** Silencio actual del dispositivo. */
    fun readMuted(): Boolean

    /** Aplica el silencio [muted]. No lanza. */
    fun writeMuted(muted: Boolean)

    /** Registra [onChanged] para cambios de silencio (eco del receptor u otros mandos). */
    fun observe(onChanged: () -> Unit)

    /** Deja de observar cambios. Idempotente. */
    fun stopObserving()
}
