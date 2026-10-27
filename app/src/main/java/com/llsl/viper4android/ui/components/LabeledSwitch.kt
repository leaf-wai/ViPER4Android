package com.llsl.viper4android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun LabeledSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    SwitchPreference(
        checked = checked,
        onCheckedChange = onCheckedChange,
        title = label,
        modifier = modifier,
        summary = subtitle,
        enabled = enabled,
    )
}
