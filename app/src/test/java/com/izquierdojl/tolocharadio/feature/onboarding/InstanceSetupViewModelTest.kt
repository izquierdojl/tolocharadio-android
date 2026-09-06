package com.izquierdojl.tolocharadio.feature.onboarding

import app.cash.turbine.test
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.session.SessionManager
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.local.TolochaDb
import com.izquierdojl.tolocharadio.domain.NormalizeBaseUrlUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InstanceSetupViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val validator: InstanceValidator = mockk()
    private val prefs: InstancePrefs = mockk(relaxed = true)
    private val db: TolochaDb = mockk(relaxed = true)
    private val session = SessionManager(mockk<TokenStore>(relaxed = true))

    private fun vm() = InstanceSetupViewModel(validator, prefs, db, session, NormalizeBaseUrlUseCase())

    @Test
    fun `url invalida muestra error sin llamar a red`() =
        runTest {
            val v = vm()
            v.connect("no url con espacios y &&&", {})
            v.ui.test {
                var s = awaitItem()
                if (s is SetupUiState.Connecting) s = awaitItem()
                assertTrue(s is SetupUiState.Error)
            }
            coVerify(exactly = 0) { validator.validate(any()) }
        }

    @Test
    fun `url valida guarda base y marca setup`() =
        runTest {
            coEvery { validator.validate("https://radio.test") } returns true
            val v = vm()
            var connected = false
            v.connect("radio.test", { connected = true })
            v.ui.test {
                // Connecting sin cambios posteriores en éxito.
                var s = awaitItem()
                if (s is SetupUiState.Idle) s = awaitItem()
                assertTrue(s is SetupUiState.Connecting)
            }
            assertTrue(connected)
            coVerify { prefs.setBaseUrl("https://radio.test") }
            coVerify { prefs.setSetupDone(true) }
        }

    @Test
    fun `instancia caida muestra error accionable`() =
        runTest {
            coEvery { validator.validate(any()) } returns false
            val v = vm()
            v.connect("https://caida.test", {})
            v.ui.test {
                var s = awaitItem()
                if (s is SetupUiState.Connecting) s = awaitItem()
                assertTrue(s is SetupUiState.Error)
            }
        }
}
