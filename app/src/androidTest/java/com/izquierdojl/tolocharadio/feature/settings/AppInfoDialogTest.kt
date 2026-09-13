package com.izquierdojl.tolocharadio.feature.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.izquierdojl.tolocharadio.core.ui.components.AppInfoDialog
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test

class AppInfoDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private val info =
        AppInfo(
            appName = "Tolocha Radio",
            version = "2.3.1",
            versionCode = 42,
            applicationId = "com.izquierdojl.tolocharadio",
            buildType = "Publicación",
            developer = "izquierdojl",
            license = "MIT",
            repositoryUrl = "https://github.com/izquierdojl/tolocharadio-android",
            theme = "Oscuro",
            startScreen = "Favoritos",
            activeServerAlias = "Mi servidor",
        )

    @Test
    fun copiar_muestra_snackbar_y_mantiene_el_dialogo() {
        compose.setContent {
            val snackbar = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            AppInfoDialog(
                info = info,
                onDismiss = {},
                onCopyResult = { ok ->
                    scope.launch {
                        snackbar.showSnackbar(if (ok) "Información copiada" else "No se pudo copiar")
                    }
                },
                snackbar = snackbar,
            )
        }

        compose.onNodeWithText("Versión: 2.3.1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Copiar información de la aplicación").performClick()
        compose.onNodeWithText("Información copiada").assertIsDisplayed()
        compose.onNodeWithText("Versión: 2.3.1").assertIsDisplayed()
    }

    @Test
    fun muestra_el_enlace_al_repositorio() {
        compose.setContent {
            val snackbar = remember { SnackbarHostState() }
            AppInfoDialog(info = info, onDismiss = {}, onCopyResult = {}, snackbar = snackbar)
        }
        compose.onNodeWithContentDescription("Enlace al repositorio del código fuente").assertExists()
        compose.onNodeWithText("Ver repositorio").assertIsDisplayed()
    }
}
