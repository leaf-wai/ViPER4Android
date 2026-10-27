package com.llsl.viper4android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.llsl.viper4android.R
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import java.util.Locale
import kotlin.math.roundToInt

@Immutable
data class SliderEdit(
    val displayValue: Double,
    val displayRange: ClosedFloatingPointRange<Double>,
    val decimals: Int,
    val onCommit: (Double) -> Unit,
    val unit: String = "",
    val defaultValue: Double? = null,
)

@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true,
    valueLabel: String? = null,
    edit: SliderEdit? = null,
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val canEdit = enabled && edit != null

    SliderPreference(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        title = label,
        valueText = valueLabel ?: value.roundToInt().toString(),
        onClick = if (canEdit) ({ showEditDialog = true }) else null,
        holdDownState = showEditDialog,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps.coerceAtLeast(0),
    )

    if (showEditDialog && edit != null) {
        NumberInputDialog(
            label = label,
            edit = edit,
            onDismiss = { showEditDialog = false },
        )
    }
}

@Composable
fun NumberInputDialog(
    label: String,
    edit: SliderEdit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    show: Boolean = true,
) {
    val min = edit.displayRange.start
    val max = edit.displayRange.endInclusive

    fun fmt(v: Double): String =
        if (edit.decimals <= 0) {
            v.roundToInt().toString()
        } else {
            String.format(Locale.US, "%.${edit.decimals}f", v)
        }

    var text by remember(edit.displayValue) { mutableStateOf(fmt(edit.displayValue)) }
    val parsed = text.trim().toDoubleOrNull()
    val isValid = parsed != null

    val rangeHint =
        if (edit.unit.isNotEmpty()) {
            stringResource(
                R.string.dialog_value_range_unit,
                fmt(min),
                fmt(max),
                edit.unit,
            )
        } else {
            stringResource(R.string.dialog_value_range, fmt(min), fmt(max))
        }

    WindowDialog(
        show = show,
        modifier = modifier,
        title = label,
        summary = rangeHint,
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                trailingIcon = if (edit.unit.isNotEmpty()) {
                    {
                        Text(
                            text = edit.unit,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body2,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    }
                } else {
                    null
                },
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Edit Dialog 按钮顺序：not_modified | cancel | confirm（三按钮 weight(1f) + spacedBy(8.dp)）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = stringResource(R.string.common_not_modified),
                    onClick = {
                        text = fmt(edit.defaultValue ?: edit.displayValue)
                    },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = stringResource(R.string.common_confirm),
                    onClick = {
                        val clamped = parsed!!.coerceIn(min, max)
                        edit.onCommit(clamped)
                        onDismiss()
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}
