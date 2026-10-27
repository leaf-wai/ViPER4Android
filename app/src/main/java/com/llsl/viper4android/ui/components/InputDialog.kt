package com.llsl.viper4android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.llsl.viper4android.R
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun InputDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    show: Boolean = true,
    dismissLabel: String = stringResource(R.string.common_cancel),
    body: String? = null,
    placeholder: String = "",
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }

    WindowDialog(
        show = show,
        modifier = modifier,
        title = title,
        summary = body,
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                label = placeholder,
                useLabelAsPlaceholder = true,
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = dismissLabel,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = confirmLabel,
                    onClick = { if (value.isNotBlank()) onConfirm(value.trim()) },
                    enabled = value.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}
