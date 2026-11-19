package com.izquierdojl.tolocharadio.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.R

/**
 * Logotipo TolochaRadio (paridad con `Logo` de `AppShell.tsx` web):
 * emblema Sierra + "Tolocha" en foreground con "Radio" en marca.
 *
 * @param showText si es `false`, solo se pinta el emblema (cabeceras
 * estrechas, spec 0047).
 */
@Composable
fun TolochaLogo(
    modifier: Modifier = Modifier,
    showText: Boolean = true,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.sierra_emblem),
            contentDescription = "Emblema de TolochaRadio",
            modifier = Modifier.size(36.dp),
        )
        if (showText) {
            Spacer(Modifier.width(8.dp))
            Text(
                buildAnnotatedString {
                    append("Tolocha")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                        append("Radio")
                    }
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
        }
    }
}
