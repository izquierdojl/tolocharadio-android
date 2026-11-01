package com.izquierdojl.tolocharadio.feature.player

import android.util.Log
import com.izquierdojl.tolocharadio.domain.playback.InterruptionCause
import com.izquierdojl.tolocharadio.domain.playback.PlaybackFailure
import com.izquierdojl.tolocharadio.domain.playback.PlaybackRoute
import javax.inject.Inject
import javax.inject.Singleton

/** Logs estructurados de recuperación, sin PII (spec 0042/0044). */
@Singleton
class PlaybackRecoveryLogger
    @Inject
    constructor() {
        fun attempt(
            failure: PlaybackFailure,
            attempt: Int,
            stationId: String,
        ) {
            Log.i(
                TAG,
                "event=recovery_attempt cause=${failure.cause} attempt=$attempt " +
                    "httpStatus=${failure.httpStatus} playerErrorCode=${failure.playerErrorCode} " +
                    "station=${hash(stationId)}",
            )
        }

        fun attempt(
            cause: InterruptionCause,
            attempt: Int,
            stationId: String,
        ) {
            attempt(PlaybackFailure(cause), attempt, stationId)
        }

        fun fallback(
            stationId: String,
            from: PlaybackRoute,
            to: PlaybackRoute,
            failure: PlaybackFailure,
            attempt: Int,
        ) {
            Log.i(
                TAG,
                "event=live_fallback from=$from to=$to cause=${failure.cause} " +
                    "httpStatus=${failure.httpStatus} playerErrorCode=${failure.playerErrorCode} " +
                    "attempt=$attempt station=${hash(stationId)}",
            )
        }

        fun success(
            failure: PlaybackFailure,
            attempt: Int,
            stationId: String,
            durationMs: Long,
        ) {
            Log.i(
                TAG,
                "event=recovery_success cause=${failure.cause} attempt=$attempt " +
                    "httpStatus=${failure.httpStatus} playerErrorCode=${failure.playerErrorCode} " +
                    "durationMs=$durationMs station=${hash(stationId)}",
            )
        }

        fun failure(
            failure: PlaybackFailure,
            attempts: Int,
            stationId: String,
            durationMs: Long,
        ) {
            Log.w(
                TAG,
                "event=recovery_failed cause=${failure.cause} attempts=$attempts " +
                    "httpStatus=${failure.httpStatus} playerErrorCode=${failure.playerErrorCode} " +
                    "durationMs=$durationMs station=${hash(stationId)}",
            )
        }

        private fun hash(stationId: String): String = stationId.hashCode().toUInt().toString(HEX_RADIX)

        private companion object {
            const val TAG = "PlaybackRecovery"
            const val HEX_RADIX = 16
        }
    }
