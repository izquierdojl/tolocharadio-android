package com.izquierdojl.tolocharadio.domain.podcasts

/**
 * Ventana máxima de avance continuo que se considera escucha real. Los
 * deltas mayores son saltos de la barra de progreso y no deben sumar
 * tiempo (misma política que la web, con margen para el muestreo de 1 s).
 */
const val MAX_CONTINUOUS_LISTEN_DELTA_MS = 10_000L

/**
 * Acumula el tiempo realmente escuchado de una sesión de episodio.
 *
 * Sólo suma avances continuos de posición mientras se está reproduciendo;
 * ignora pausas, saltos hacia delante y retrocesos. Un retroceso seguido de
 * reproducción vuelve a contar como escucha (reescucha real).
 */
class ListeningTimeTracker(private val maxContinuousDeltaMs: Long = MAX_CONTINUOUS_LISTEN_DELTA_MS) {
    private var lastPositionMs: Long? = null
    private var listenedMs: Long = 0L

    /** Tiempo escuchado acumulado en la sesión actual. */
    val totalMs: Long get() = listenedMs

    /** Reinicia el acumulador al empezar una sesión nueva. */
    fun reset() {
        lastPositionMs = null
        listenedMs = 0L
    }

    /**
     * Registra una muestra de posición. Devuelve el acumulado resultante.
     *
     * @param playing `true` sólo si el reproductor estaba reproduciendo en
     *   esa muestra. Las muestras en pausa actualizan la referencia sin
     *   sumar tiempo.
     */
    fun onProgress(
        positionMs: Long,
        playing: Boolean,
    ): Long {
        val previous = lastPositionMs
        if (previous != null && playing) {
            val delta = positionMs - previous
            if (delta in 1..maxContinuousDeltaMs) listenedMs += delta
        }
        lastPositionMs = positionMs
        return listenedMs
    }

    /**
     * Sincroniza la posición final de un reporte para no perder el último
     * tramo. Se aplica el mismo límite anti-salto que en [onProgress].
     */
    fun synchronize(positionMs: Long): Long = onProgress(positionMs, playing = true)
}
