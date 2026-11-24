package com.izquierdojl.tolocharadio.feature.player

import android.media.AudioManager
import android.view.KeyEvent
import com.izquierdojl.tolocharadio.domain.playback.SkipSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pulsación corta/larga de volumen y cancelación (spec 0060, US2). */
class HeadsetVolumeGestureHandlerTest {
    private val controller = mockk<HeadsetControlsController>(relaxed = true)
    private val audioManager = mockk<AudioManager>(relaxed = true)

    @Test
    fun `la pulsacion corta ajusta el volumen sin saltar`() =
        runTest {
            every { controller.canSkip(SkipSource.HeadsetGesture) } returns true
            val handler = HeadsetVolumeGestureHandler(controller, audioManager, this)

            assertTrue(handler.handle(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, 0, true))
            assertTrue(handler.handle(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_UP, 0, true))

            verify {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI,
                )
            }
            verify(exactly = 0) {
                controller.skipForward(SkipSource.HeadsetGesture, "volume_long_up")
            }
        }

    @Test
    fun `la pulsacion larga salta una sola vez`() =
        runTest {
            every { controller.canSkip(SkipSource.HeadsetGesture) } returns true
            val handler = HeadsetVolumeGestureHandler(controller, audioManager, this)

            assertTrue(handler.handle(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, 0, true))
            advanceTimeBy(HeadsetVolumeGestureHandler.VOLUME_LONG_PRESS_MS)
            runCurrent()
            assertTrue(handler.handle(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_UP, 0, true))

            verify(exactly = 1) {
                controller.skipForward(SkipSource.HeadsetGesture, "volume_long_up")
            }
            verify(exactly = 0) {
                audioManager.adjustStreamVolume(any(), any(), any())
            }
        }

    @Test
    fun `las repeticiones durante la pulsacion larga no duplican el salto`() =
        runTest {
            every { controller.canSkip(SkipSource.HeadsetGesture) } returns true
            val handler = HeadsetVolumeGestureHandler(controller, audioManager, this)

            handler.handle(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, 0, true)
            handler.handle(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, 1, true)
            advanceTimeBy(HeadsetVolumeGestureHandler.VOLUME_LONG_PRESS_MS)
            runCurrent()
            handler.handle(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_UP, 0, true)

            verify(exactly = 1) {
                controller.skipBack(SkipSource.HeadsetGesture, "volume_long_down")
            }
        }

    @Test
    fun `en segundo plano no captura el volumen`() =
        runTest {
            every { controller.canSkip(SkipSource.HeadsetGesture) } returns true
            val handler = HeadsetVolumeGestureHandler(controller, audioManager, this)

            assertFalse(handler.handle(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, 0, false))
            verify(exactly = 0) { audioManager.adjustStreamVolume(any(), any(), any()) }
        }

    @Test
    fun `con gestos desactivados no captura el volumen`() =
        runTest {
            every { controller.canSkip(SkipSource.HeadsetGesture) } returns false
            val handler = HeadsetVolumeGestureHandler(controller, audioManager, this)

            assertFalse(handler.handle(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, 0, true))
            verify(exactly = 0) { audioManager.adjustStreamVolume(any(), any(), any()) }
        }

    @Test
    fun `cancelar descarta el salto pendiente`() =
        runTest {
            every { controller.canSkip(SkipSource.HeadsetGesture) } returns true
            val handler = HeadsetVolumeGestureHandler(controller, audioManager, this)

            handler.handle(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, 0, true)
            handler.cancel()
            advanceTimeBy(HeadsetVolumeGestureHandler.VOLUME_LONG_PRESS_MS)
            runCurrent()

            verify(exactly = 0) {
                controller.skipForward(SkipSource.HeadsetGesture, "volume_long_up")
            }
            verify(exactly = 0) { audioManager.adjustStreamVolume(any(), any(), any()) }
        }
}
