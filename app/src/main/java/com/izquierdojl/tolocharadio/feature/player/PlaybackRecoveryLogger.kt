package com.izquierdojl.tolocharadio.feature.player

import android.util.Log
import com.izquierdojl.tolocharadio.domain.playback.InterruptionCause
import javax.inject.Inject
import javax.inject.Singleton

/** Logs estructurados de recuperación, sin PII (spec 0042, FR-021). */
@Singleton
class PlaybackRecoveryLogger
    @Inject
    constructor() {
        fun attempt(
            cause: InterruptionCause,
            attempt: Int,
            stationId: String,
        ) {
            Log.i(TAG, "event=recovery_attempt cause=$cause attempt=$attempt station=${hash(stationId)}")
        }

        fun success(
            cause: InterruptionCause,
            attempt: Int,
            stationId: String,
            durationMs: Long,
        ) {
            Log.i(
                TAG,
                "event=recovery_success cause=$cause attempt=$attempt " +
                    "durationMs=$durationMs station=${hash(stationId)}",
            )
        }

        fun failure(
            cause: InterruptionCause,
            attempts: Int,
            stationId: String,
            durationMs: Long,
        ) {
            Log.w(
                TAG,
                "event=recovery_failed cause=$cause attempts=$attempts " +
                    "durationMs=$durationMs station=${hash(stationId)}",
            )
        }

        private fun hash(stationId: String): String = stationId.hashCode().toUInt().toString(HEX_RADIX)

        private companion object {
            const val TAG = "PlaybackRecovery"
            const val HEX_RADIX = 16
        }
    }
