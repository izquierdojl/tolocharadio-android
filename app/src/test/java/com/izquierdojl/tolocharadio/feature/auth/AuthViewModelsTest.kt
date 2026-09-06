package com.izquierdojl.tolocharadio.feature.auth

import app.cash.turbine.test
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.data.remote.dto.AppConfigDto
import com.izquierdojl.tolocharadio.data.remote.dto.ThemeDto
import com.izquierdojl.tolocharadio.data.remote.dto.UserDto
import com.izquierdojl.tolocharadio.data.repo.AuthRepo
import com.izquierdojl.tolocharadio.data.repo.SystemRepo
import com.izquierdojl.tolocharadio.domain.ValidateAuthUseCase
import com.izquierdojl.tolocharadio.domain.auth.LoginUseCase
import com.izquierdojl.tolocharadio.domain.auth.StoreServerCredentialsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val auth: AuthRepo = mockk()
    private val loginUseCase: LoginUseCase = mockk()
    private val storeCredentials: StoreServerCredentialsUseCase = mockk(relaxed = true)
    private val system: SystemRepo = mockk()
    private val user = UserDto(1, "a@b.c", "Ana", ThemeDto.DARK, 0)

    private fun vm() = LoginViewModel(loginUseCase, storeCredentials, system, ValidateAuthUseCase())

    private suspend fun LoginViewModel.awaitForm(): LoginUiState.Form {
        var s: LoginUiState = LoginUiState.Form(loading = true)
        ui.test {
            s = awaitItem()
            if ((s as? LoginUiState.Form)?.loading == true) {
                s = awaitItem()
            }
        }
        return s as LoginUiState.Form
    }

    @Test
    fun `email invalido no llama a red`() =
        runTest {
            coEvery { system.config() } returns ApiResult.Ok(AppConfigDto("T", true))
            val v = vm()
            v.onEmailChange("mal")
            v.login {}
            assertTrue(v.awaitForm().emailError != null)
            coVerify(exactly = 0) { loginUseCase(any(), any()) }
        }

    @Test
    fun `login OK invoca callback`() =
        runTest {
            coEvery { system.config() } returns ApiResult.Ok(AppConfigDto("T", true))
            coEvery { loginUseCase("a@b.c", "secreta123") } returns ApiResult.Ok(user)
            val v = vm()
            v.onEmailChange("a@b.c")
            v.onPasswordChange("secreta123")
            var done = false
            v.login { done = true }
            assertTrue(done)
        }

    @Test
    fun `login OK guarda credenciales del servidor activo (FR-005)`() =
        runTest {
            coEvery { system.config() } returns ApiResult.Ok(AppConfigDto("T", true))
            coEvery { loginUseCase("a@b.c", "secreta123") } returns ApiResult.Ok(user)
            val v = vm()
            v.onEmailChange("a@b.c")
            v.onPasswordChange("secreta123")
            v.login {}
            coVerify { storeCredentials("a@b.c", "secreta123") }
        }

    @Test
    fun `registro oculto si la instancia lo cierra`() =
        runTest {
            coEvery { system.config() } returns ApiResult.Ok(AppConfigDto("T", false))
            assertEquals(false, vm().awaitForm().registrationEnabled)
        }
}

class RegisterViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val auth: AuthRepo = mockk()
    private val storeCredentials: StoreServerCredentialsUseCase = mockk(relaxed = true)
    private val user = UserDto(1, "a@b.c", "Ana", ThemeDto.DARK, 0)

    @Test
    fun `password corta no llama a red`() =
        runTest {
            val v = RegisterViewModel(auth, storeCredentials, ValidateAuthUseCase())
            v.onEmailChange("a@b.c")
            v.onPasswordChange("corta")
            v.register {}
            v.ui.test {
                val s = awaitItem()
                assertTrue(s.passwordError != null)
            }
            coVerify(exactly = 0) { auth.register(any(), any(), any()) }
        }

    @Test
    fun `registro OK invoca callback`() =
        runTest {
            coEvery { auth.register("a@b.c", "secreta123", null) } returns ApiResult.Ok(user)
            val v = RegisterViewModel(auth, storeCredentials, ValidateAuthUseCase())
            v.onEmailChange("a@b.c")
            v.onPasswordChange("secreta123")
            var done = false
            v.register { done = true }
            assertTrue(done)
        }

    @Test
    fun `registro OK guarda credenciales del servidor activo (FR-005)`() =
        runTest {
            coEvery { auth.register("a@b.c", "secreta123", "Ana") } returns ApiResult.Ok(user)
            val v = RegisterViewModel(auth, storeCredentials, ValidateAuthUseCase())
            v.onNameChange("Ana")
            v.onEmailChange("a@b.c")
            v.onPasswordChange("secreta123")
            v.register {}
            coVerify { storeCredentials("a@b.c", "secreta123") }
        }
}
