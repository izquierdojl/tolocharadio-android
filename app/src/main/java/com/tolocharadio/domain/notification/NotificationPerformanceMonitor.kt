package com.tolocharadio.domain.notification

/**
 * Monitors notification performance.
 */
object NotificationPerformanceMonitor {
    
    const val PERFORMANCE_THRESHOLD_MS = 2000L // 2 seconds
    
    /**
     * Measures the time taken to process a notification.
     * @param operation The operation name
     * @param block The operation to measure
     * @return The result of the operation
     */
    inline fun <T> measurePerformance(operation: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val durationMs = System.currentTimeMillis() - startTime
        
        NotificationLogger.logPerformance(operation, durationMs)
        
        if (durationMs > PERFORMANCE_THRESHOLD_MS) {
            NotificationLogger.logError("Performance threshold exceeded: $operation took ${durationMs}ms")
        }
        
        return result
    }
    
    /**
     * Measures the time taken to process a suspend operation.
     * @param operation The operation name
     * @param block The suspend operation to measure
     * @return The result of the operation
     */
    suspend inline fun <T> measurePerformanceSuspend(operation: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val durationMs = System.currentTimeMillis() - startTime
        
        NotificationLogger.logPerformance(operation, durationMs)
        
        if (durationMs > PERFORMANCE_THRESHOLD_MS) {
            NotificationLogger.logError("Performance threshold exceeded: $operation took ${durationMs}ms")
        }
        
        return result
    }
    
    /**
     * Checks if a performance metric is within threshold.
     * @param operation The operation name
     * @param durationMs The duration in milliseconds
     * @return true if within threshold, false otherwise
     */
    fun isWithinThreshold(operation: String, durationMs: Long): Boolean {
        val isWithin = durationMs <= PERFORMANCE_THRESHOLD_MS
        if (!isWithin) {
            NotificationLogger.logError("Performance threshold exceeded: $operation took ${durationMs}ms")
        }
        return isWithin
    }
    
    /**
     * Gets the performance threshold.
     * @return The performance threshold in milliseconds
     */
    fun getPerformanceThreshold(): Long {
        return PERFORMANCE_THRESHOLD_MS
    }
}
