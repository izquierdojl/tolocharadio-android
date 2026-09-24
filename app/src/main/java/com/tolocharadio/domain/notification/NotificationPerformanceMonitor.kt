package com.tolocharadio.domain.notification

/**
 * Monitors notification performance (spec 0016, T043).
 */
object NotificationPerformanceMonitor {
    const val PERFORMANCE_THRESHOLD_MS = 2000L // 2 seconds

    /**
     * Measures the time taken to process a suspend notification operation.
     * @param operation The operation name
     * @param block The suspend operation to measure
     * @return The result of the operation
     */
    suspend inline fun <T> measurePerformanceSuspend(
        operation: String,
        block: () -> T,
    ): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val durationMs = System.currentTimeMillis() - startTime

        NotificationLogger.logPerformance(operation, durationMs)

        if (durationMs > PERFORMANCE_THRESHOLD_MS) {
            NotificationLogger.logError("Performance threshold exceeded: $operation took ${durationMs}ms")
        }

        return result
    }
}
