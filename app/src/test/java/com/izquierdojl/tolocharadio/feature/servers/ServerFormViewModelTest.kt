package com.izquierdojl.tolocharadio.feature.servers

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.session.TokenStore
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.local.servers.SavedServerEntity
import com.izquierdojl.tolocharadio.data.repo.servers.ServerRepository
import com.izquierdojl.tolocharadio.domain.servers.AddServerUseCase
import com.izquierdojl.tolocharadio.domain.servers.SavedServer
import com.izquierdojl.tolocharadio.domain.servers.UpdateServerUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ServerFormViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val addServer: AddServerUseCase = mockk()
    private val updateServer: UpdateServerUseCase = mockk()
    private val repository: ServerRepository = mockk(relaxed = true)
    private val tokens: TokenStore = mockk(relaxed = true)
    private val prefs: InstancePrefs = mockk(relaxed = true)

    private fun vm() = ServerFormViewModel(addServer, updateServer, repository, tokens, prefs)

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
    fun `save sin datos muestra error y no llama al caso de uso`() =
        runTest {
            val v = vm()
            v.save()
            assertNotNull(v.ui.value.error)
            coVerify(exactly = 0) { addServer(any(), any(), any(), any()) }
        }

    @Test
    fun `save alta valida guarda baseUrl y marca guardado (FR-011)`() =
        runTest {
            coEvery { addServer(any(), any(), any(), any()) } returns
                ApiResult.Ok(server("https://radio.test"))
            val v = vm()
            v.onUrlChange("radio.test")
            v.onAliasChange("Mi servidor")
            v.onEmailChange("a@b.c")
            v.onPasswordChange("secreta123")
            v.save()
            advanceUntilIdle()
            assertTrue(v.ui.value.saved)
            coVerify { prefs.setBaseUrl("https://radio.test") }
            coVerify { prefs.setSetupDone(true) }
        }

    @Test
    fun `edit precarga email y contrasena guardados (FR-007)`() =
        runTest {
            coEvery { repository.getById("1") } returns
                SavedServerEntity(id = "1", url = "https://radio.test", alias = "srv")
            every { tokens.getCredentials("1") } returns
                TokenStore.ServerCredentials("a@b.c", "secreta123", "refresh")
            val v = vm()
            v.loadForEdit("1")
            advanceUntilIdle()
            assertEquals(ServerFormMode.EDIT, v.ui.value.mode)
            assertEquals("a@b.c", v.ui.value.email)
            assertEquals("secreta123", v.ui.value.password)
        }

    @Test
    fun `edit guarda con UpdateServerUseCase`() =
        runTest {
            coEvery { repository.getById("1") } returns
                SavedServerEntity(id = "1", url = "https://radio.test", alias = "srv")
            every { tokens.getCredentials("1") } returns
                TokenStore.ServerCredentials("a@b.c", "secreta123", "refresh")
            coEvery { updateServer("1", any(), any(), any()) } returns
                ApiResult.Ok(server("https://radio.test"))
            val v = vm()
            v.loadForEdit("1")
            advanceUntilIdle()
            v.save()
            advanceUntilIdle()
            assertTrue(v.ui.value.saved)
            coVerify { updateServer("1", "srv", "a@b.c", "secreta123") }
        }
}
