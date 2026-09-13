package com.izquierdojl.tolocharadio.feature.settings

import app.cash.turbine.test
import com.izquierdojl.tolocharadio.MainDispatcherRule
import com.izquierdojl.tolocharadio.core.ui.navigation.StartScreen
import com.izquierdojl.tolocharadio.core.ui.theme.ThemeMode
import com.izquierdojl.tolocharadio.core.util.AppBuildInfo
import com.izquierdojl.tolocharadio.data.local.InstancePrefs
import com.izquierdojl.tolocharadio.domain.servers.GetServersUseCase
import com.izquierdojl.tolocharadio.domain.servers.SavedServer
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
    private val getServers: GetServersUseCase = mockk()

    private val buildInfo =
        AppBuildInfo(
            appName = "Tolocha Radio",
            versionName = "2.3.1",
            versionCode = 42,
            applicationId = "com.izquierdojl.tolocharadio",
            buildType = "Publicación",
            repositoryUrl = "https://github.com/izquierdojl/tolocharadio-android",
            developer = "izquierdojl",
            license = "MIT",
        )

    private fun savedServer(
        id: String,
        alias: String,
        isActive: Boolean,
    ) = SavedServer(
        id = id,
        url = "https://$id.test",
        alias = alias,
        appName = null,
        isActive = isActive,
        isDefault = isActive,
        createdAt = 0L,
    )

    private fun vm(
        info: AppBuildInfo = buildInfo,
        servers: List<SavedServer> = emptyList(),
    ): SettingsViewModel {
        every { prefs.themeMode } returns flowOf(ThemeMode.DARK)
        every { prefs.startScreen } returns flowOf(StartScreen.FAVORITES)
        every { getServers() } returns flowOf(servers)
        return SettingsViewModel(prefs, getServers, info)
    }

    @Test
    fun `ui refleja tema y pantalla de arranque persistidos (FR-005)`() =
        runTest {
            val v = vm()
            assertEquals(ThemeMode.DARK, v.ui.value.themeMode)
            assertEquals(StartScreen.FAVORITES, v.ui.value.startScreen)
        }

    @Test
    fun `cambio de pantalla de arranque persiste`() =
        runTest {
            val v = vm()
            v.onStartScreenChange(StartScreen.HISTORY)
            coVerify { prefs.setStartScreen(StartScreen.HISTORY) }
            assertEquals(StartScreen.HISTORY, v.ui.value.startScreen)
        }

    @Test
    fun `cambio de tema persiste localmente`() =
        runTest {
            val v = vm()
            v.onThemeModeChange(ThemeMode.LIGHT)
            coVerify { prefs.setThemeMode(ThemeMode.LIGHT) }
        }

    @Test
    fun `showAppInfoDialog usa la version del build`() =
        runTest {
            val v = vm()
            assertEquals(AppInfoUiState.Hidden, v.appInfoUiState.value)
            v.showAppInfoDialog()
            val info = (v.appInfoUiState.value as AppInfoUiState.Showing).info
            assertEquals("2.3.1", info.version)
        }

    @Test
    fun `showAppInfoDialog incluye metadatos y configuracion activa`() =
        runTest {
            val servers =
                listOf(
                    savedServer(id = "a", alias = "Otro", isActive = false),
                    savedServer(id = "b", alias = "Mi servidor", isActive = true),
                )
            val v = vm(servers = servers)
            v.showAppInfoDialog()
            val info = (v.appInfoUiState.value as AppInfoUiState.Showing).info
            assertEquals(42L, info.versionCode)
            assertEquals("com.izquierdojl.tolocharadio", info.applicationId)
            assertEquals("Publicación", info.buildType)
            assertEquals("Oscuro", info.theme)
            assertEquals("Favoritos", info.startScreen)
            assertEquals("Mi servidor", info.activeServerAlias)
        }

    @Test
    fun `buildType refleja depuracion y publicacion`() =
        runTest {
            val debug = vm(info = buildInfo.copy(buildType = "Depuración"))
            debug.showAppInfoDialog()
            assertEquals(
                "Depuración",
                (debug.appInfoUiState.value as AppInfoUiState.Showing).info.buildType,
            )

            val release = vm(info = buildInfo.copy(buildType = "Publicación"))
            release.showAppInfoDialog()
            assertEquals(
                "Publicación",
                (release.appInfoUiState.value as AppInfoUiState.Showing).info.buildType,
            )
        }

    @Test
    fun `sin servidor activo el alias es null`() =
        runTest {
            val v = vm()
            v.showAppInfoDialog()
            val info = (v.appInfoUiState.value as AppInfoUiState.Showing).info
            assertEquals(null, info.activeServerAlias)
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
    fun `onCopyResult true emite exito y no cierra el dialogo`() =
        runTest {
            val v = vm()
            v.showAppInfoDialog()
            v.messages.test {
                v.onCopyResult(true)
                assertEquals("Información copiada", awaitItem())
            }
            assert(v.appInfoUiState.value is AppInfoUiState.Showing)
        }

    @Test
    fun `onCopyResult false emite error sin cerrar el dialogo`() =
        runTest {
            val v = vm()
            v.showAppInfoDialog()
            v.messages.test {
                v.onCopyResult(false)
                assertEquals("No se pudo copiar la información", awaitItem())
            }
            assert(v.appInfoUiState.value is AppInfoUiState.Showing)
        }
}
