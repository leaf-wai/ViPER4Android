package com.llsl.viper4android.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.llsl.viper4android.R
import com.llsl.viper4android.ui.theme.StatusColors
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LabeledDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onDeleteOption: ((Int, String) -> Unit)? = null,
    isOptionDeletable: (Int, String) -> Boolean = { _, _ -> onDeleteOption != null },
) {
    var deleteTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }
    val selectedIndex = remember(options, selectedValue) {
        options.indexOf(selectedValue).coerceAtLeast(0)
    }
    val canDeleteCurrent = enabled &&
        onDeleteOption != null &&
        selectedIndex in options.indices &&
        isOptionDeletable(selectedIndex, options[selectedIndex])

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OverlayDropdownPreference(
            items = options,
            selectedIndex = selectedIndex,
            title = label,
            modifier = Modifier.weight(1f),
            enabled = enabled && options.isNotEmpty(),
            onSelectedIndexChange = { index ->
                if (index in options.indices) {
                    onOptionSelected(index, options[index])
                }
            },
        )
        if (canDeleteCurrent) {
            IconButton(
                onClick = { deleteTarget = selectedIndex to options[selectedIndex] },
                modifier = Modifier.padding(end = 12.dp),
                minHeight = 35.dp,
                minWidth = 35.dp,
                backgroundColor = MiuixTheme.colorScheme.secondaryContainer,
            ) {
                Icon(
                    imageVector = MiuixIcons.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = StatusColors.danger,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }

    deleteTarget?.let { (index, name) ->
        ConfirmDialog(
            title = stringResource(R.string.delete_file_title),
            body = stringResource(R.string.delete_file_message, name),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                onDeleteOption?.invoke(index, name)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}
