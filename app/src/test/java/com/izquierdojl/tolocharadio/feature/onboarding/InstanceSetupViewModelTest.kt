package com.izquierdojl.tolocharadio.feature.onboarding

import app.cash.turbine.test
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.domain.NormalizeBaseUrlUseCase
import com.izquierdojl.tolocharadio.domain.servers.AddServerUseCase
import com.izquierdojl.tolocharadio.domain.servers.SavedServer
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

    private val addServer: AddServerUseCase = mockk()
    private val prefs: InstancePrefs = mockk(relaxed = true)

    private fun vm() = InstanceSetupViewModel(addServer, prefs, NormalizeBaseUrlUseCase())

    private fun server(url: String) =
        SavedServer(
            id = "1",
            url = url,
            alias = "Mi servidor",
            appName = null,
            isActive = true,
            isDefault = true,
            createdAt = 0,
        )

    @Test
    fun `url invalida muestra error sin llamar a red`() =
        runTest {
            val v = vm()
            v.connect("no url con espacios y &&&", "srv", {})
            v.ui.test {
                var s = awaitItem()
                if (s is SetupUiState.Connecting) s = awaitItem()
                assertTrue(s is SetupUiState.Error)
            }
            coVerify(exactly = 0) { addServer(any(), any(), any()) }
        }

    @Test
    fun `url valida crea servidor y marca setup`() =
        runTest {
            coEvery { addServer("https://radio.test", "Mi servidor", any()) } returns
                ApiResult.Ok(server("https://radio.test"))
            val v = vm()
            var connected = false
            v.connect("radio.test", "Mi servidor", { connected = true })
            v.ui.test {
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
            coEvery { addServer(any(), any(), any()) } returns
                ApiResult.Err(DomainError.Unavailable("No se pudo conectar"))
            val v = vm()
            v.connect("https://caida.test", "srv", {})
            v.ui.test {
                var s = awaitItem()
                if (s is SetupUiState.Connecting) s = awaitItem()
                assertTrue(s is SetupUiState.Error)
            }
        }
}
