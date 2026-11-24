package com.izquierdojl.tolocharadio.feature.player

import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent
import com.izquierdojl.tolocharadio.domain.playback.SkipSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Detecta la pulsación larga de volumen (spec 0060, FR-004/FR-005).
 *
 * Las teclas de volumen no llegan a `MediaSession`; esta clase se invoca desde
 * `MainActivity.dispatchKeyEvent` y solo captura cuando la app está visible y
 * hay un episodio que puede saltar.
 *
 * - Corta (< [VOLUME_LONG_PRESS_MS]): ajusta el volumen del sistema una vez.
 * - Larga: ejecuta un único salto y consume repeticiones/ACTION_UP.
 *
 * Hilos: se construye y usa desde el hilo principal.
 */
class HeadsetVolumeGestureHandler(
    private val controller: HeadsetControlsController,
    private val audioManager: AudioManager,
    private val scope: CoroutineScope,
) {
    private var trackingKeyCode: Int? = null
    private var longTriggered = false
    private var timerJob: Job? = null

    /**
     * Procesa una tecla de volumen.
     *
     * @return `true` si la app consume el evento; `false` para que el sistema
     *   gestione el volumen de forma nativa.
     */
    fun handle(
        keyCode: Int,
        action: Int,
        repeatCount: Int,
        hostResumed: Boolean,
    ): Boolean {
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false
        }
        return when (action) {
            KeyEvent.ACTION_DOWN -> onDown(keyCode, repeatCount, hostResumed)
            KeyEvent.ACTION_UP -> onUp(keyCode)
            else -> false
        }
    }

    /** Cancela un gesto en curso (pausa/destrucción de la Activity). */
    fun cancel() {
        timerJob?.cancel()
        reset()
    }

    private fun onDown(
        keyCode: Int,
        repeatCount: Int,
        hostResumed: Boolean,
    ): Boolean {
        if (!hostResumed || !controller.canSkip(SkipSource.HeadsetGesture)) {
            cancel()
            return false
        }
        if (trackingKeyCode == keyCode) return true
        if (repeatCount > 0) return false
        trackingKeyCode = keyCode
        longTriggered = false
        timerJob =
            scope.launch {
                delay(VOLUME_LONG_PRESS_MS)
                onLongPress(keyCode)
            }
        return true
    }

    private fun onUp(keyCode: Int): Boolean {
        if (trackingKeyCode != keyCode) return false
        timerJob?.cancel()
        if (longTriggered) {
            reset()
            return true
        }
        adjustVolume(keyCode)
        reset()
        return true
    }

    private fun onLongPress(keyCode: Int) {
        if (trackingKeyCode != keyCode) return
        longTriggered = true
        if (!controller.canSkip(SkipSource.HeadsetGesture)) {
            logGesture(if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) "volume_long_up" else "volume_long_down", "ignore")
            return
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            controller.skipForward(SkipSource.HeadsetGesture, "volume_long_up")
        } else {
            controller.skipBack(SkipSource.HeadsetGesture, "volume_long_down")
        }
    }

    private fun adjustVolume(keyCode: Int) {
        val direction =
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                AudioManager.ADJUST_RAISE
            } else {
                AudioManager.ADJUST_LOWER
            }
        val gesture = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) "volume_short_up" else "volume_short_down"
        runCatching {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI,
            )
        }.onFailure { error ->
            Log.w(TAG, "volume_adjust_failed gesture=$gesture cause=${error.javaClass.simpleName}")
        }
        logGesture(gesture, "volume")
    }

    private fun reset() {
        trackingKeyCode = null
        longTriggered = false
        timerJob = null
    }

    private fun logGesture(
        gesture: String,
        result: String,
    ) {
        Log.d(TAG, "gesture=$gesture content=episode source=headset result=$result")
    }

    companion object {
        /** Umbral de pulsación larga. */
        const val VOLUME_LONG_PRESS_MS = 500L

        private const val TAG = "HeadsetControls"
    }
}
