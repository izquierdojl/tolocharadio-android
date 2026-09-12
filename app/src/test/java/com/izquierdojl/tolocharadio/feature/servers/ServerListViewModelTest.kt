package com.izquierdojl.tolocharadio.feature.servers

import android.content.Context
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.network.DomainError
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity
import com.izquierdojl.tolocharadio.domain.servers.DeleteServerUseCase
import com.izquierdojl.tolocharadio.domain.servers.GetServersUseCase
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
    private val switchServer: SwitchServerUseCase = mockk(relaxed = true)
    private val deleteServer: DeleteServerUseCase = mockk(relaxed = true)
    private val instancePrefs: InstancePrefs = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    private fun vm(): ServerListViewModel {
        every { getServers() } returns flowOf(emptyList())
        return ServerListViewModel(getServers, switchServer, deleteServer, instancePrefs, context)
    }

    @Test
    fun `borrado del ultimo servidor pide el formulario (FR-010)`() =
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
                ApiResult.Err(DomainError.Unauthorized("bad_credentials"))
            val v = vm()
            v.switchServer("a")
            advanceUntilIdle()
            assertNotNull(v.uiState.value.error)
            coVerify(exactly = 0) { instancePrefs.setBaseUrl(any()) }
        }

    @Test
    fun `servidores sin entity de credenciales`() {
        // El servidor ya no guarda credenciales en Room: viven cifradas en TokenStore.
        val entity = SavedServerEntity(id = "x", url = "https://x", alias = "x")
        assertTrue(entity.javaClass.declaredFields.none { it.name == "password" || it.name == "email" })
    }
}
