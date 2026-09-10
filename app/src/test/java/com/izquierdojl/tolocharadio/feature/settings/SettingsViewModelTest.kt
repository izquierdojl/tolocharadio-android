package com.izquierdojl.tolocharadio.feature.settings

import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.network.ApiResult
import com.izquierdojl.tolocharadio.core.ui.navigation.StartScreen
import com.izquierdojl.tolocharadio.core.ui.theme.ThemeMode
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.data.repo.UserRepo
import com.izquierdojl.tolocharadio.domain.auth.LogoutUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val prefs: InstancePrefs = mockk(relaxed = true)
    private val users: UserRepo = mockk(relaxed = true)
    private val logoutUseCase: LogoutUseCase = mockk(relaxed = true)

    private fun vm(): SettingsViewModel {
        every { prefs.themeMode } returns flowOf(ThemeMode.DARK)
        every { prefs.startScreen } returns flowOf(StartScreen.FAVORITES)
        coEvery { users.patchMe(any(), any()) } returns ApiResult.Ok(mockk(relaxed = true))
        return SettingsViewModel(prefs, users, logoutUseCase)
    }

    @Test
    fun `ui refleja tema y pantalla de arranque persistidos (FR-011)`() =
        runTest {
            val v = vm()
            assertEquals(ThemeMode.DARK, v.ui.value.themeMode)
            assertEquals(StartScreen.FAVORITES, v.ui.value.startScreen)
        }

    @Test
    fun `cambio de pantalla de arranque persiste (FR-011b)`() =
        runTest {
            val v = vm()
            v.onStartScreenChange(StartScreen.HISTORY)
            coVerify { prefs.setStartScreen(StartScreen.HISTORY) }
            assertEquals(StartScreen.HISTORY, v.ui.value.startScreen)
        }

    @Test
    fun `cambio de tema persiste (FR-011)`() =
        runTest {
            val v = vm()
            v.onThemeModeChange(ThemeMode.LIGHT)
            coVerify { prefs.setThemeMode(ThemeMode.LIGHT) }
        }

    @Test
    fun `logout delega en LogoutUseCase`() =
        runTest {
            val v = vm()
            var done = false
            v.logout { done = true }
            coVerify { logoutUseCase() }
            assertEquals(true, done)
        }

    @Test
    fun `showAppInfoDialog transiciona a Showing`() =
        runTest {
            val v = vm()
            assertEquals(AppInfoUiState.Hidden, v.appInfoUiState.value)
            v.showAppInfoDialog()
            assert(v.appInfoUiState.value is AppInfoUiState.Showing)
        }

    @Test
    fun `dismissAppInfoDialog transiciona a Hidden`() =
        runTest {
            val v = vm()
            v.showAppInfoDialog()
            assert(v.appInfoUiState.value is AppInfoUiState.Showing)
            v.dismissAppInfoDialog()
            assertEquals(AppInfoUiState.Hidden, v.appInfoUiState.value)
        }

    @Test
    fun `AppInfo contiene datos correctos por defecto`() =
        runTest {
            val info = AppInfo()
            assertEquals("Tolocha Radio", info.appName)
            assertEquals("1.0", info.version)
            assertEquals("https://github.com/izquierdojl/tolocharadio-android", info.repositoryUrl)
            assertEquals("izquierdojl", info.developer)
            assertEquals("MIT", info.license)
        }
}
