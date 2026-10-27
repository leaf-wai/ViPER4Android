package com.llsl.viper4android.ui.screens.eq

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.llsl.viper4android.R
import com.llsl.viper4android.data.model.EqPreset
import com.llsl.viper4android.effect.BuiltinPresets
import com.llsl.viper4android.ui.components.AdaptiveTopAppBar
import com.llsl.viper4android.ui.components.BlurredBar
import com.llsl.viper4android.ui.components.CardItem
import com.llsl.viper4android.ui.components.ConfirmDialog
import com.llsl.viper4android.ui.components.EqCurveGraph
import com.llsl.viper4android.ui.components.InputDialog
import com.llsl.viper4android.ui.components.LabeledDropdown
import com.llsl.viper4android.ui.components.LabeledSlider
import com.llsl.viper4android.ui.components.SliderEdit
import com.llsl.viper4android.ui.components.UiDimens
import com.llsl.viper4android.ui.components.groupedCardItems
import com.llsl.viper4android.ui.components.layerBackdrop
import com.llsl.viper4android.ui.components.rememberBlurBackdrop
import com.llsl.viper4android.ui.components.resolvePresetName
import com.llsl.viper4android.ui.theme.StatusColors
import com.llsl.viper4android.ui.util.horizontalCutoutPadding
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.util.Locale

private const val EQ_DB_MIN = -12f
private const val EQ_DB_MAX = 12f

@Composable
fun EqEditScreen(
    bands: List<Float>,
    bandCount: Int,
    presetId: Long?,
    presets: List<EqPreset>,
    onBandCountChange: (Int) -> Unit,
    onBandsChange: (List<Double>) -> Unit,
    onPresetSelect: (Long) -> Unit,
    onPresetAdd: (String) -> Unit,
    onPresetDelete: (Long) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    val localBands = remember(bandCount) {
        mutableStateListOf<Float>().apply { addAll(bands.take(bandCount)) }
    }

    LaunchedEffect(bands, bandCount) {
        val incoming = bands.take(bandCount)
        if (incoming != localBands.toList()) {
            localBands.clear()
            localBands.addAll(incoming)
        }
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var deletePresetId by remember { mutableStateOf<Long?>(null) }

    val bandCounts = listOf(10, 15, 25, 31)
    val bandCountOptions = bandCounts.map { stringResource(R.string.label_eq_n_bands, it) }
    val bandCountIndex = when (bandCount) {
        15 -> 1
        25 -> 2
        31 -> 3
        else -> 0
    }

    val presetNames = presets.map { resolvePresetName(it) }
    val selectedPresetName = presets.find { it.id == presetId }?.let { resolvePresetName(it) }
        ?: stringResource(R.string.label_custom)
    val bandLabels = BuiltinPresets.eqBandLabelsForCount(bandCount)

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.section_equalizer),
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.action_close),
                                tint = MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .horizontalCutoutPadding()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding())
        ) {
            item(key = "top_spacer") {
                Spacer(modifier = Modifier.height(12.dp))
            }

            groupedCardItems(
                keyPrefix = "eq_header",
                items = listOf(
                    CardItem(key = "graph") {
                        EqCurveGraph(
                            bands = localBands.toList(),
                            onClick = {},
                            modifier = Modifier
                                .height(UiDimens.EqEditGraphHeight)
                                .padding(top = 8.dp),
                            bandCount = bandCount
                        )
                    },
                    CardItem(key = "band_count") {
                        LabeledDropdown(
                            label = stringResource(R.string.label_eq_bands),
                            selectedValue = bandCountOptions[bandCountIndex],
                            options = bandCountOptions,
                            onOptionSelected = { index, _ -> onBandCountChange(bandCounts[index]) }
                        )
                    },
                    CardItem(key = "preset") {
                        LabeledDropdown(
                            label = stringResource(R.string.label_preset),
                            selectedValue = selectedPresetName,
                            options = presetNames,
                            onOptionSelected = { index, _ -> onPresetSelect(presets[index].id) }
                        )
                    },
                    CardItem(key = "actions") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                text = stringResource(R.string.action_save),
                                onClick = { showSaveDialog = true },
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                text = stringResource(R.string.action_delete),
                                onClick = { deletePresetId = presetId },
                                enabled = presetId != null,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColors(
                                    color = StatusColors.danger.copy(alpha = 0.15f),
                                    textColor = StatusColors.danger
                                )
                            )
                            TextButton(
                                text = stringResource(R.string.action_reset),
                                onClick = {
                                    for (i in localBands.indices) {
                                        localBands[i] = 0f
                                    }
                                    onReset()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                )
            )

            item(key = "bands_title") {
                SmallTitle(text = stringResource(R.string.label_eq_bands))
            }

            groupedCardItems(
                keyPrefix = "eq_bands_$bandCount",
                items = bandLabels.mapIndexedNotNull { index, label ->
                    if (index >= localBands.size) return@mapIndexedNotNull null
                    CardItem(key = "band_$index") {
                        val bandValue = localBands[index]
                        val applyBandChange = { newVal: Float ->
                            localBands[index] = newVal.coerceIn(EQ_DB_MIN, EQ_DB_MAX)
                            val list = localBands.map {
                                String.format(Locale.US, "%.1f", it).toDouble()
                            }
                            onBandsChange(list)
                        }
                        LabeledSlider(
                            label = "$label Hz",
                            value = bandValue,
                            onValueChange = applyBandChange,
                            valueRange = EQ_DB_MIN..EQ_DB_MAX,
                            valueLabel = String.format(Locale.US, "%.1f dB", bandValue),
                            edit = SliderEdit(
                                displayValue = bandValue.toDouble(),
                                displayRange = EQ_DB_MIN.toDouble()..EQ_DB_MAX.toDouble(),
                                decimals = 1,
                                unit = "dB",
                                defaultValue = 0.0,
                                onCommit = { applyBandChange(it.toFloat()) }
                            )
                        )
                    }
                }
            )

            item(key = "bottom_spacer") {
                Spacer(
                    modifier = Modifier
                        .height(24.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }

    if (showSaveDialog) {
        InputDialog(
            title = stringResource(R.string.preset_save_title),
            initialValue = "",
            confirmLabel = stringResource(R.string.common_confirm),
            onConfirm = { name ->
                onPresetAdd(name)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false },
            placeholder = stringResource(R.string.preset_name_hint)
        )
    }

    deletePresetId?.let { targetId ->
        val targetName = presets.find { it.id == targetId }?.let { resolvePresetName(it) }
            ?: stringResource(R.string.label_custom)
        ConfirmDialog(
            title = stringResource(R.string.preset_delete_title),
            body = stringResource(R.string.preset_delete_confirm, targetName),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                onPresetDelete(targetId)
                deletePresetId = null
            },
            onDismiss = { deletePresetId = null }
        )
    }
}
