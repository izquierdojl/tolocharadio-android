package com.izquierdojl.tolocharadio.feature.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Combobox reutilizable para filtros de Explorar con autocompletado,
 * estados de carga/error y modo degradado (entrada libre).
 *
 * Usa una lista inline en lugar de un popup para evitar problemas
 * de coordenadas cuando se usa dentro de un [ModalBottomSheet].
 *
 * @param value valor actual del filtro.
 * @param onValueChange callback al hacer clic en un item de la lista.
 * @param label etiqueta del campo.
 * @param catalogList estado de la lista de catálogo (Loading/Loaded/Error).
 * @param modifier modificador Compose.
 */
@Composable
fun FilterComboBox(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    catalogList: CatalogList,
    modifier: Modifier = Modifier,
) {
    var textValue by remember(value) { mutableStateOf(value) }

    when (catalogList) {
        is CatalogList.Loading -> {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text(label) },
                enabled = false,
                trailingIcon = { CircularProgressIndicator(modifier = Modifier.padding(4.dp)) },
                modifier = modifier.fillMaxWidth(),
            )
        }

        is CatalogList.Error -> {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text(label) },
                supportingText = {
                    Text(
                        "No se pudo cargar la lista; escribe el valor manualmente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                singleLine = true,
                modifier = modifier.fillMaxWidth(),
            )
        }

        is CatalogList.Loaded -> {
            val items = catalogList.items
            val filtered = if (textValue.isBlank()) items
            else items.filter { it.contains(textValue, ignoreCase = true) }

            Column(modifier = modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (filtered.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .padding(top = 4.dp),
                    ) {
                        items(filtered.take(50)) { option ->
                            ListItem(
                                headlineContent = { Text(option) },
                                modifier = Modifier.clickable {
                                    textValue = option
                                    onValueChange(option)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
