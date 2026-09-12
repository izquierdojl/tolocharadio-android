package com.izquierdojl.tolocharadio.feature.servers

import android.content.Context
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity
import com.izquierdojl.tolocharadio.domain.servers.AddServerUseCase
import com.izquierdojl.tolocharadio.domain.servers.DeleteServerUseCase
import com.izquierdojl.tolocharadio.domain.servers.GetServersUseCase
import com.izquierdojl.tolocharadio.domain.servers.SavedServer
import com.izquierdojl.tolocharadio.domain.servers.SwitchServerUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ServerListViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val getServers: GetServersUseCase = mockk(relaxed = true)
    private val addServer: AddServerUseCase = mockk(relaxed = true)
    private val switchServer: SwitchServerUseCase = mockk(relaxed = true)
    private val deleteServer: DeleteServerUseCase = mockk(relaxed = true)
    private val instancePrefs: InstancePrefs = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    private fun vm(): ServerListViewModel {
        every { getServers() } returns flowOf(emptyList())
        return ServerListViewModel(getServers, addServer, switchServer, deleteServer, instancePrefs, context)
    }

    private fun server(id: String) =
        SavedServer(
            id = id,
            url = "https://srv-$id.example.com",
            alias = id,
            appName = null,
            isActive = true,
            isDefault = true,
            createdAt = 0,
        )

    @Test
    fun `addServer sin credenciales muestra exito (FR-009)`() =
        runTest {
            coEvery { addServer(any(), any(), any()) } returns
                ApiResult.Ok(server("a"))
            val v = vm()
            v.addServer("https://srv.example.com", "srv")
            advanceUntilIdle()
            assertNotNull(v.uiState.value.successMessage)
        }

    @Test
    fun `borrado del ultimo servidor pide la bienvenida (FR-013)`() =
        runTest {
            every { getServers() } returns flowOf(emptyList())
            val v = vm()
            var welcome = false
            v.deleteServer("a") { welcome = true }
            advanceUntilIdle()
            assertTrue(welcome)
        }

    @Test
    fun `switch fallido no re-apunta la red`() =
        runTest {
            coEvery { switchServer("a") } returns
                ApiResult.Err(DomainError.NotFound("server_not_found"))
            val v = vm()
            v.switchServer("a")
            advanceUntilIdle()
            assertNotNull(v.uiState.value.error)
            coVerify(exactly = 0) { instancePrefs.setBaseUrl(any()) }
        }

    @Test
    fun `servidores sin entity de credenciales`() {
        // Guarda de compilación: SavedServer no tiene userEmail (FR-009).
        val entity = SavedServerEntity(id = "x", url = "https://x", alias = "x")
        assertTrue(entity.javaClass.declaredFields.none { it.name == "userEmail" })
    }
}
