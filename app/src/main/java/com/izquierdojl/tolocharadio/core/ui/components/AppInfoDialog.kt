package com.izquierdojl.tolocharadio.core.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.izquierdojl.tolocharadio.feature.settings.AppInfo
import com.izquierdojl.tolocharadio.feature.settings.toClipboardText

/** Opacidad del scrim que separa el diálogo del contenido de fondo. */
private const val SCRIM_ALPHA = 0.32f

/**
 * Diálogo modal con la información general de la aplicación.
 *
 * Muestra versión real, metadatos del build y configuración activa, permite
 * copiar todo el bloque al portapapeles y conserva el diálogo abierto tras
 * copiar (el aviso se muestra con el [snackbar]).
 *
 * @param info Datos de la aplicación a mostrar.
 * @param onDismiss Callback para cerrar el diálogo.
 * @param onCopyResult Resultado del copiado (true = éxito) para avisar al usuario.
 * @param snackbar Host del aviso de copiado, anclado a la parte inferior.
 */
@Composable
fun AppInfoDialog(
    info: AppInfo,
    onDismiss: () -> Unit,
    onCopyResult: (Boolean) -> Unit,
    snackbar: SnackbarHostState,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                        .clickable(onClick = onDismiss),
            )
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        text = info.appName,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        InfoLine("Versión", info.version)
                        InfoLine("Número de compilación", info.versionCode.toString())
                        InfoLine("Identificador", info.applicationId)
                        InfoLine("Tipo de build", info.buildType)
                        InfoLine("Tema", info.theme)
                        InfoLine("Pantalla de arranque", info.startScreen)
                        if (!info.activeServerAlias.isNullOrBlank()) {
                            InfoLine("Servidor activo", info.activeServerAlias)
                        }
                        InfoLine("Desarrollador", info.developer)
                        InfoLine("Licencia", info.license)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Ver repositorio",
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription = "Enlace al repositorio del código fuente"
                                    }
                                    .clickable { openUrl(context, info.repositoryUrl) },
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = {
                                val copied =
                                    runCatching {
                                        clipboard.setText(AnnotatedString(info.toClipboardText()))
                                    }.isSuccess
                                onCopyResult(copied)
                            },
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "Copiar información de la aplicación"
                                },
                        ) {
                            Text("Copiar información")
                        }
                        TextButton(
                            onClick = onDismiss,
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "Cerrar diálogo de información"
                                },
                        ) {
                            Text("Cerrar")
                        }
                    }
                }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
            )
        }
    }
}

@Composable
private fun InfoLine(
    label: String,
    value: String,
) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(4.dp))
}

private fun openUrl(
    context: Context,
    url: String,
) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {
        // Dispositivo sin navegador instalado — fallo silencioso
    }
}
