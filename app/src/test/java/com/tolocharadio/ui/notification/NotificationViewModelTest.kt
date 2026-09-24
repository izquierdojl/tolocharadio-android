package com.tolocharadio.ui.notification

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.tolocharadio.di.InMemoryAppStateTracker
import com.tolocharadio.domain.notification.AppState
import com.tolocharadio.domain.notification.HandleNotificationTapUseCase
import com.tolocharadio.domain.notification.NotificationAction
import com.tolocharadio.domain.notification.NotificationData
import com.tolocharadio.domain.notification.NotificationMapper
import com.tolocharadio.domain.notification.NotificationType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Regresión de la ruta de producción del tap de notificación (spec 0016, T048):
 * el VM procesa el dato, resuelve la acción y expone el destino de navegación.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val useCase: HandleNotificationTapUseCase = mockk()
    private val tracker = InMemoryAppStateTracker()

    private fun viewModel() = NotificationViewModel(useCase, NotificationMapper(), tracker)

    private fun data(
        type: NotificationType = NotificationType.CONTENT,
        action: NotificationAction = NotificationAction.OPEN_CONTENT,
        contentId: String? = "s1",
    ) = NotificationData(
        type = type,
        title = "Título",
        message = "Mensaje",
        contentId = contentId,
        action = action,
    )

    @Test
    fun `tap con datos expone la accion de navegacion con contentId (FR-003)`() =
        runTest {
            coEvery { useCase(any()) } returns NotificationAction.OPEN_CONTENT
            val v = viewModel()

            v.handleNotificationTap(data())
            advanceUntilIdle()

            val state = v.notificationState.value
            assertTrue(state is NotificationState.Navigate)
            state as NotificationState.Navigate
            assertEquals(NotificationAction.OPEN_CONTENT, state.action)
            assertEquals("s1", state.contentId)
            assertEquals("tolocharadio://content/s1", state.deepLink)
        }

    @Test
    fun `tap de reproduccion sin extras abre el reproductor (US2-AC1)`() =
        runTest {
            val v = viewModel()

            v.handlePlaybackNotificationTap()

            val state = v.notificationState.value
            assertTrue(state is NotificationState.Navigate)
            state as NotificationState.Navigate
            assertEquals(NotificationAction.OPEN_PLAYER, state.action)
            assertEquals(null, state.contentId)
        }

    @Test
    fun `resetState vuelve a Idle`() =
        runTest {
            val v = viewModel()
            v.handlePlaybackNotificationTap()
            v.resetState()
            assertEquals(NotificationState.Idle, v.notificationState.value)
        }

    @Test
    fun `updateAppState refleja el cambio en el tracker (FR-006)`() =
        runTest {
            val v = viewModel()
            v.updateAppState(AppState.FOREGROUND)
            assertEquals(AppState.FOREGROUND, tracker.currentState.value)
            v.updateAppState(AppState.BACKGROUND)
            assertEquals(AppState.BACKGROUND, tracker.currentState.value)
        }
}
