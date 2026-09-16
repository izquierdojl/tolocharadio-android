package com.izquierdojl.tolocharadio.cast

/**
 * Contrato del dispositivo remoto (receptor Cast) para el control de volumen.
 *
 * Abstrae la `CastSession` para que el controlador de volumen sea testeable en JVM sin
 * dependencias de framework. Todas las operaciones se invocan desde el hilo principal y
 * **no lanzan**: un fallo del receptor se manifiesta por falta de eco (research D7).
 */
interface RemoteVolumeDevice {
    /** Volumen actual del dispositivo en `[0.0, 1.0]`, o `null` si la sesión ya no está disponible. */
    fun readVolume(): Double?

    /** Silencio actual del dispositivo. */
    fun readMuted(): Boolean

    /** Aplica [volume] (se clampa a `[0.0, 1.0]`). No lanza. */
    fun writeVolume(volume: Double)

    /** Aplica el silencio [muted]. No lanza. */
    fun writeMuted(muted: Boolean)

    /** Registra [onChanged] para cambios de volumen/silencio (eco del receptor u otros mandos). */
    fun observe(onChanged: () -> Unit)

    /** Deja de observar cambios. Idempotente. */
    fun stopObserving()
}
