package com.izquierdojl.tolocharadio.feature.podcasts.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.izquierdojl.tolocharadio.R
import com.izquierdojl.tolocharadio.core.ui.components.TolochaTestTags
import com.izquierdojl.tolocharadio.feature.podcasts.SubscriptionSearchScope

/**
 * Buscador de Suscripciones (US8): campo de texto y ámbito Podcasts | Episodios.
 * En «Podcasts» filtra al escribir; en «Episodios» consulta al confirmar.
 */
@Composable
@Suppress("LongParameterList")
fun SubscriptionSearchBar(
    term: String,
    scope: SubscriptionSearchScope,
    searching: Boolean,
    onTermChange: (String) -> Unit,
    onScopeChange: (SubscriptionSearchScope) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val scopeDescription = stringResource(R.string.subscriptions_search_scope_description)
    val submit: () -> Unit = {
        if (scope == SubscriptionSearchScope.EPISODES && term.isNotBlank() && !searching) {
            focusManager.clearFocus()
            onSubmit()
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = term,
            onValueChange = onTermChange,
            label = {
                Text(
                    stringResource(
                        if (scope == SubscriptionSearchScope.PODCASTS) {
                            R.string.subscriptions_search_podcasts_label
                        } else {
                            R.string.subscriptions_search_episodes_label
                        },
                    ),
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
            trailingIcon = {
                Row(horizontalArrangement = Arrangement.Center) {
                    if (term.isNotBlank()) {
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier.testTag(TolochaTestTags.SUBSCRIPTIONS_SEARCH_CLEAR),
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.subscriptions_search_clear),
                            )
                        }
                    }
                    if (scope == SubscriptionSearchScope.EPISODES) {
                        IconButton(
                            onClick = submit,
                            enabled = term.isNotBlank() && !searching,
                            modifier = Modifier.testTag(TolochaTestTags.SUBSCRIPTIONS_SEARCH_SUBMIT),
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = stringResource(R.string.subscriptions_search_action),
                            )
                        }
                    }
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(TolochaTestTags.SUBSCRIPTIONS_SEARCH_FIELD),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.subscriptions_search_scope),
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = scopeDescription }
                    .testTag(TolochaTestTags.SUBSCRIPTIONS_SEARCH_SCOPE),
        ) {
            SubscriptionSearchScope.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = scope == option,
                    onClick = { onScopeChange(option) },
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = SubscriptionSearchScope.entries.size,
                        ),
                    modifier = Modifier.heightIn(min = 48.dp).weight(1f),
                    label = {
                        Text(
                            stringResource(
                                if (option == SubscriptionSearchScope.PODCASTS) {
                                    R.string.subscriptions_scope_podcasts
                                } else {
                                    R.string.subscriptions_scope_episodes
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}
