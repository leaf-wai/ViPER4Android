package com.llsl.viper4android.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BlurCircular
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material.icons.filled.Waves
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.llsl.viper4android.R
import com.llsl.viper4android.effect.EffectState
import com.llsl.viper4android.effect.Effects
import com.llsl.viper4android.effect.compressorAdaptAmountToSeconds
import com.llsl.viper4android.effect.compressorDbToRaw
import com.llsl.viper4android.effect.compressorMsToSeconds
import com.llsl.viper4android.effect.compressorRatioToRaw
import com.llsl.viper4android.effect.compressorRawToDb
import com.llsl.viper4android.effect.compressorRawToRatio
import com.llsl.viper4android.effect.compressorSecondsToMs
import com.llsl.viper4android.ui.components.CardItem
import com.llsl.viper4android.ui.components.CardSegment
import com.llsl.viper4android.ui.components.ConfirmDialog
import com.llsl.viper4android.ui.components.EqCurveGraph
import com.llsl.viper4android.ui.components.EqEditDialog
import com.llsl.viper4android.ui.components.InputDialog
import com.llsl.viper4android.ui.components.LabeledDropdown
import com.llsl.viper4android.ui.components.LabeledSlider
import com.llsl.viper4android.ui.components.LabeledSwitch
import com.llsl.viper4android.ui.components.RichText
import com.llsl.viper4android.ui.components.SliderEdit
import com.llsl.viper4android.ui.components.UiDimens
import com.llsl.viper4android.ui.components.resolvePresetName
import com.llsl.viper4android.ui.theme.StatusColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private val EffectHorizontalPadding = UiDimens.Standard
private val EffectHeaderIconSize = UiDimens.IconMedium
private val EffectHeaderIconSpacing = UiDimens.Large
private val EffectHeaderHorizontalPadding = EffectHorizontalPadding
private val EffectHeaderVerticalPadding = UiDimens.Large
private val EffectSwitchPlaceholderHeight = UiDimens.SwitchSlotHeight

private fun scaleToDb(scale: Number): Double = 20.0 * log10(scale.toDouble())
private fun dbToScale(db: Double): Float = 10.0.pow(db / 20.0).toFloat()

/**
 * 屏幕级音效卡片展开控制器，遵循 AGENTS.md：
 * - 展开状态上提到屏幕级 SnapshotStateList
 * - 收起时先跑连续高度收缩动画，结束才从 expandedGroups 摘除，期间 retainedGroups 保住行 item 继续产出
 * - 动画期间关掉 animateItem 的 placement 动画避免拖影不同步
 */
class EffectExpandController(
    val expandedGroups: SnapshotStateList<String>
) {
    val retainedGroups = mutableStateListOf<String>()
    val animatingGroups = mutableStateListOf<String>()
    val progressMap = mutableMapOf<String, MutableFloatState>()
    private val expandJobs = mutableMapOf<String, Job>()

    val multibandSelectedTab = mutableIntStateOf(0)
    val dynamicEqSelectedTab = mutableIntStateOf(0)

    val isAnimating: Boolean
        get() = animatingGroups.isNotEmpty()

    val isAllCollapsed: Boolean
        get() = expandedGroups.isEmpty()

    fun isTargetExpanded(key: String): Boolean =
        key in expandedGroups && key !in retainedGroups

    fun shouldEmitRows(key: String): Boolean =
        key in expandedGroups || key in retainedGroups

    fun progressStateOf(key: String): State<Float>? = progressMap[key]

    fun toggle(key: String, scope: CoroutineScope) {
        expandJobs[key]?.cancel()
        val currentlyTargetExpanded = isTargetExpanded(key)
        val progressState = progressMap.getOrPut(key) {
            mutableFloatStateOf(if (currentlyTargetExpanded) 1f else 0f)
        }
        if (key !in animatingGroups) {
            animatingGroups.add(key)
        }
        if (!currentlyTargetExpanded) {
            retainedGroups.remove(key)
            if (key !in expandedGroups) {
                expandedGroups.add(key)
            }
            expandJobs[key] = scope.launch {
                animate(
                    initialValue = progressState.floatValue,
                    targetValue = 1f,
                    animationSpec = tween(300)
                ) { value, _ ->
                    progressState.floatValue = value
                }
                animatingGroups.remove(key)
                expandJobs.remove(key)
            }
        } else {
            if (key !in retainedGroups) {
                retainedGroups.add(key)
            }
            expandJobs[key] = scope.launch {
                animate(
                    initialValue = progressState.floatValue,
                    targetValue = 0f,
                    animationSpec = tween(300)
                ) { value, _ ->
                    progressState.floatValue = value
                }
                expandedGroups.remove(key)
                retainedGroups.remove(key)
                animatingGroups.remove(key)
                expandJobs.remove(key)
            }
        }
    }

    fun collapseAll(scope: CoroutineScope) {
        val toCollapse = expandedGroups.filter { isTargetExpanded(it) }
        toCollapse.forEach { key -> toggle(key, scope) }
    }

    fun expandAll(scope: CoroutineScope) {
        ALL_EXPANDABLE_EFFECT_KEYS.forEach { key ->
            if (!isTargetExpanded(key)) {
                toggle(key, scope)
            }
        }
    }

    companion object {
        val ALL_EXPANDABLE_EFFECT_KEYS = listOf(
            "masterLimiter",
            "playbackGain",
            "lufsTargeting",
            "fetCompressor",
            "multibandComp",
            "ddc",
            "vse",
            "eq",
            "dynamicEq",
            "convolver",
            "fieldSurround",
            "diffSurround",
            "stereoImager",
            "vhe",
            "reverb",
            "dynamicSystem",
            "tubeSimulator",
            "psychoBass",
            "viperBass",
            "viperBassMono",
            "viperClarity",
            "cure",
            "analogX"
        )
    }
}

@Composable
fun rememberEffectExpandController(): EffectExpandController {
    val expandedGroups = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) {
        mutableStateListOf("masterLimiter")
    }
    return remember(expandedGroups) { EffectExpandController(expandedGroups) }
}

@Composable
private fun EffectCollapseRow(
    progressState: State<Float>?,
    rowIndex: Int,
    rowCount: Int,
    isLast: Boolean,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(
            constraints.copy(minWidth = constraints.maxWidth)
        )
        val bottomPadPx = if (isLast) 8.dp.roundToPx() else 0
        val fullHeight = placeable.height + bottomPadPx
        val progress = progressState?.value ?: 1f
        val fraction = if (rowCount <= 0) 1f else (rowCount * progress - rowIndex).coerceIn(0f, 1f)
        val currentHeight = (fullHeight * fraction).roundToInt()
        layout(constraints.maxWidth, currentHeight) {
            placeable.placeRelative(0, 0)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.effectSectionItems(
    sectionKey: String,
    titleRes: Int,
    descriptionRes: Int,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    controller: EffectExpandController,
    scope: CoroutineScope,
    icon: ImageVector? = null,
    hasEnableSwitch: Boolean = true,
    toggleOnly: Boolean = false,
    rows: List<CardItem> = emptyList()
) {
    val isTargetExpanded = !toggleOnly && controller.isTargetExpanded(sectionKey)
    val shouldEmitRows = !toggleOnly && controller.shouldEmitRows(sectionKey) && rows.isNotEmpty()
    val progressState = controller.progressStateOf(sectionKey)

    item(key = "$sectionKey:header") {
        var showHelpDialog by rememberSaveable { mutableStateOf(false) }
        val haptic = LocalHapticFeedback.current
        val title = stringResource(titleRes)
        val bottomRadius by animateDpAsState(
            targetValue = if (isTargetExpanded && rows.isNotEmpty()) 0.dp else 16.dp,
            animationSpec = tween(300),
            label = "effectHeaderBottomRadius"
        )
        val placementSpec = if (controller.isAnimating) {
            null
        } else {
            spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntOffset.VisibilityThreshold
            )
        }

        CardSegment(
            isFirst = true,
            isLast = !shouldEmitRows,
            bottomCornerRadius = bottomRadius,
            outerBottomPadding = if (shouldEmitRows) 0.dp else 12.dp,
            modifier = Modifier.animateItem(placementSpec = placementSpec)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (!toggleOnly && rows.isNotEmpty()) {
                                controller.toggle(sectionKey, scope)
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showHelpDialog = true
                        }
                    )
                    .padding(
                        horizontal = EffectHeaderHorizontalPadding,
                        vertical = EffectHeaderVerticalPadding
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(EffectHeaderIconSize),
                        tint = if (enabled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Spacer(modifier = Modifier.width(EffectHeaderIconSpacing))
                }
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.headline2,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (hasEnableSwitch) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = onEnabledChange
                    )
                } else {
                    Spacer(modifier = Modifier.height(EffectSwitchPlaceholderHeight))
                }
            }
        }

        if (showHelpDialog) {
            EffectHelpDialog(
                title = title,
                descriptionRes = descriptionRes,
                onDismiss = { showHelpDialog = false }
            )
        }
    }

    if (shouldEmitRows) {
        val rowCount = rows.size
        val lastIndex = rows.lastIndex
        rows.forEachIndexed { index, cardItem ->
            val isLastRow = index == lastIndex
            item(key = "$sectionKey:row:${cardItem.key}") {
                val placementSpec = if (controller.isAnimating) {
                    null
                } else {
                    spring(
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold
                    )
                }
                CardSegment(
                    isFirst = false,
                    isLast = isLastRow,
                    outerBottomPadding = if (isLastRow) 12.dp else 0.dp,
                    modifier = Modifier.animateItem(placementSpec = placementSpec)
                ) {
                    EffectCollapseRow(
                        progressState = progressState,
                        rowIndex = index,
                        rowCount = rowCount,
                        isLast = isLastRow
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = EffectHorizontalPadding),
                            content = cardItem.content
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EffectHelpDialog(
    title: String,
    descriptionRes: Int,
    onDismiss: () -> Unit
) {
    val showState = remember { mutableStateOf(true) }
    WindowDialog(
        title = title,
        show = showState.value,
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.heightIn(max = 500.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                RichText(text = stringResource(descriptionRes))
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                text = stringResource(R.string.common_confirm),
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColorsPrimary(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EffectSection(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    descriptionRes: Int,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    hasEnableSwitch: Boolean = true,
    toggleOnly: Boolean = false,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { if (!toggleOnly) expanded = !expanded },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showHelpDialog = true
                        }
                    )
                    .padding(
                        horizontal = EffectHeaderHorizontalPadding,
                        vertical = EffectHeaderVerticalPadding
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(EffectHeaderIconSize),
                        tint = if (enabled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Spacer(modifier = Modifier.width(EffectHeaderIconSpacing))
                }
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.headline2,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (hasEnableSwitch) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = onEnabledChange
                    )
                } else {
                    Spacer(modifier = Modifier.height(EffectSwitchPlaceholderHeight))
                }
            }

            if (!toggleOnly) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = EffectHorizontalPadding,
                                end = EffectHorizontalPadding,
                                bottom = EffectHorizontalPadding
                            )
                    ) {
                        content()
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        EffectHelpDialog(
            title = title,
            descriptionRes = descriptionRes,
            onDismiss = { showHelpDialog = false }
        )
    }
}

private fun masterLimiterCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val outputVolume = state.out.volume
    val channelPan = state.out.channelPan
    val limiter = state.out.limiter
    val gainDb = if (outputVolume > 0) scaleToDb(outputVolume) else -99.9
    val limDb = if (limiter > 0) scaleToDb(limiter) else -99.9
    val left = ((1.0f - channelPan) * 50.0f).roundToInt()
    val right = ((1.0f + channelPan) * 50.0f).roundToInt()

    return listOf(
        CardItem("volume") {
            LabeledSlider(
                label = stringResource(R.string.label_output_volume),
                value = outputVolume,
                onValueChange = { viewModel.applyPref(Effects.masterLimiter.outputVolume, it) },
                valueRange = 0.01f..2.0f,
                valueLabel = String.format(Locale.US, "%.1f dB", gainDb),
                edit = SliderEdit(
                    displayValue = gainDb,
                    displayRange = scaleToDb(0.01)..scaleToDb(2.0),
                    decimals = 1,
                    unit = "dB",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.masterLimiter.outputVolume,
                            dbToScale(it).coerceIn(0.01f, 2.0f)
                        )
                    }
                )
            )
        },
        CardItem("pan") {
            LabeledSlider(
                label = stringResource(R.string.label_output_pan),
                value = channelPan,
                onValueChange = { viewModel.applyPref(Effects.masterLimiter.channelPan, it) },
                valueRange = -1.0f..1.0f,
                valueLabel = "$left:$right",
                edit = SliderEdit(
                    displayValue = channelPan.toDouble(),
                    displayRange = -1.0..1.0,
                    decimals = 2,
                    onCommit = {
                        viewModel.applyPref(
                            Effects.masterLimiter.channelPan,
                            it.toFloat().coerceIn(-1.0f, 1.0f)
                        )
                    }
                )
            )
        },
        CardItem("limiter") {
            LabeledSlider(
                label = stringResource(R.string.label_output_limiter),
                value = limiter,
                onValueChange = { viewModel.applyPref(Effects.masterLimiter.threshold, it) },
                valueRange = 0.3f..1.0f,
                valueLabel = String.format(Locale.US, "%.1f dB", limDb),
                edit = SliderEdit(
                    displayValue = limDb,
                    displayRange = scaleToDb(0.3)..scaleToDb(1.0),
                    decimals = 1,
                    unit = "dB",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.masterLimiter.threshold,
                            dbToScale(it).coerceIn(0.3f, 1.0f)
                        )
                    }
                )
            )
        }
    )
}

private fun playbackGainCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val vals = state.playbackGainControl
    val strength = vals.strength
    val maxGain = vals.maxGain
    val threshold = vals.outputThreshold
    val threshDb = if (threshold > 0) scaleToDb(threshold) else -99.9

    return listOf(
        CardItem("strength") {
            LabeledSlider(
                label = stringResource(R.string.label_strength),
                value = strength,
                onValueChange = { viewModel.applyPref(Effects.playbackGainControl.strength, it) },
                valueRange = 0.5f..3.0f,
                valueLabel = String.format(Locale.US, "%.1fx", strength),
                edit = SliderEdit(
                    displayValue = strength.toDouble(),
                    displayRange = 0.5..3.0,
                    decimals = 1,
                    unit = "x",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.playbackGainControl.strength,
                            it.toFloat().coerceIn(0.5f, 3.0f)
                        )
                    }
                )
            )
        },
        CardItem("max_gain") {
            LabeledSlider(
                label = stringResource(R.string.label_max_gain),
                value = maxGain,
                onValueChange = { viewModel.applyPref(Effects.playbackGainControl.maxGain, it) },
                valueRange = 1.0f..10.0f,
                valueLabel = String.format(Locale.US, "%.1fx", maxGain),
                edit = SliderEdit(
                    displayValue = maxGain.toDouble(),
                    displayRange = 1.0..10.0,
                    decimals = 1,
                    unit = "x",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.playbackGainControl.maxGain,
                            it.toFloat().coerceIn(1.0f, 10.0f)
                        )
                    }
                )
            )
        },
        CardItem("threshold") {
            LabeledSlider(
                label = stringResource(R.string.label_agc_output_threshold),
                value = threshold,
                onValueChange = { viewModel.applyPref(Effects.playbackGainControl.outputThreshold, it) },
                valueRange = 0.3f..1.0f,
                valueLabel = String.format(Locale.US, "%.1f dB", threshDb),
                edit = SliderEdit(
                    displayValue = threshDb,
                    displayRange = scaleToDb(0.3)..scaleToDb(1.0),
                    decimals = 1,
                    unit = "dB",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.playbackGainControl.outputThreshold,
                            dbToScale(it).coerceIn(0.3f, 1.0f)
                        )
                    }
                )
            )
        }
    )
}

private fun lufsTargetingCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val vals = state.lufs
    val target = vals.target
    val maxGain = vals.maxGain
    val speed = vals.speed

    return listOf(
        CardItem("target") {
            LabeledSlider(
                label = stringResource(R.string.label_lufs_target_lufs),
                value = target,
                onValueChange = { viewModel.applyPref(Effects.lufs.target, it) },
                valueRange = -24f..-8f,
                valueLabel = String.format(Locale.US, "%.1f LUFS", target),
                edit = SliderEdit(
                    displayValue = target.toDouble(),
                    displayRange = -24.0..-8.0,
                    decimals = 1,
                    unit = "LUFS",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.lufs.target,
                            it.toFloat().coerceIn(-24.0f, -8.0f)
                        )
                    }
                )
            )
        },
        CardItem("max_gain") {
            LabeledSlider(
                label = stringResource(R.string.label_max_gain),
                value = maxGain,
                onValueChange = { viewModel.applyPref(Effects.lufs.maxGain, it) },
                valueRange = 0f..12f,
                valueLabel = String.format(Locale.US, "%.1f dB", maxGain),
                edit = SliderEdit(
                    displayValue = maxGain.toDouble(),
                    displayRange = 0.0..12.0,
                    decimals = 1,
                    unit = "dB",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.lufs.maxGain,
                            it.toFloat().coerceIn(0.0f, 12.0f)
                        )
                    }
                )
            )
        },
        CardItem("speed") {
            val speedNames = listOf(
                stringResource(R.string.label_lufs_speed_slow),
                stringResource(R.string.label_lufs_speed_medium),
                stringResource(R.string.label_lufs_speed_fast)
            )
            LabeledSlider(
                label = stringResource(R.string.label_lufs_speed),
                value = speed.toFloat(),
                onValueChange = { viewModel.applyPref(Effects.lufs.speed, it.roundToInt()) },
                valueRange = 0f..2f,
                steps = 1,
                valueLabel = speedNames.getOrElse(speed) { speedNames[1] }
            )
        }
    )
}

private fun fetCompressorCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val vals = state.fetCompressor
    val thresholdDb = compressorRawToDb(vals.threshold)
    val ratioDisplay = compressorRawToRatio(vals.ratio)
    val kneeDb = compressorRawToDb(vals.knee)
    val gainDb = compressorRawToDb(vals.gain)
    val attackMs = compressorSecondsToMs(vals.attack)
    val maxAttackMs = compressorSecondsToMs(vals.maxAttack)
    val releaseMs = compressorSecondsToMs(vals.release)
    val maxReleaseMs = compressorSecondsToMs(vals.maxRelease)
    val crestMs = compressorSecondsToMs(vals.crest)

    return listOf(
        CardItem("threshold") {
            LabeledSlider(
                label = stringResource(R.string.label_threshold),
                value = thresholdDb,
                onValueChange = { viewModel.applyPref(Effects.fetCompressor.threshold, compressorDbToRaw(it)) },
                valueRange = -48f..0f,
                valueLabel = "${thresholdDb.roundToInt()} dB",
                edit = SliderEdit(
                    displayValue = thresholdDb.toDouble(),
                    displayRange = -48.0..0.0,
                    decimals = 0,
                    unit = "dB",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.fetCompressor.threshold,
                            compressorDbToRaw(it.toFloat().coerceIn(-48.0f, 0.0f))
                        )
                    }
                )
            )
        },
        CardItem("ratio") {
            LabeledSlider(
                label = stringResource(R.string.label_fet_ratio),
                value = ratioDisplay,
                onValueChange = { viewModel.applyPref(Effects.fetCompressor.ratio, compressorRatioToRaw(it)) },
                valueRange = 0f..2f,
                valueLabel = String.format(Locale.US, "%.1f", ratioDisplay),
                edit = SliderEdit(
                    displayValue = ratioDisplay.toDouble(),
                    displayRange = 0.0..2.0,
                    decimals = 1,
                    onCommit = {
                        viewModel.applyPref(
                            Effects.fetCompressor.ratio,
                            compressorRatioToRaw(it.toFloat().coerceIn(0.0f, 2.0f))
                        )
                    }
                )
            )
        },
        CardItem("knee_auto") {
            LabeledSwitch(
                label = stringResource(R.string.label_fet_auto_knee),
                checked = vals.kneeAuto,
                onCheckedChange = { viewModel.applyPref(Effects.fetCompressor.kneeAuto, it) }
            )
        },
        CardItem("knee") {
            LabeledSlider(
                label = stringResource(R.string.label_fet_knee),
                value = kneeDb,
                onValueChange = { viewModel.applyPref(Effects.fetCompressor.knee, compressorDbToRaw(it)) },
                valueRange = 0f..12f,
                enabled = !vals.kneeAuto,
                valueLabel = "${kneeDb.roundToInt()} dB",
                edit = SliderEdit(
                    displayValue = kneeDb.toDouble(),
                    displayRange = 0.0..12.0,
                    decimals = 0,
                    unit = "dB",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.fetCompressor.knee,
                            compressorDbToRaw(it.toFloat().coerceIn(0.0f, 12.0f))
                        )
                    }
                )
            )
        },
        CardItem("knee_multi") {
            LabeledSlider(
                label = stringResource(R.string.label_fet_knee_multi),
                value = vals.kneeMulti,
                onValueChange = { viewModel.applyPref(Effects.fetCompressor.kneeMulti, it) },
                valueRange = 0f..4f,
                valueLabel = String.format(Locale.US, "%.1fx", vals.kneeMulti),
                edit = SliderEdit(
                    displayValue = vals.kneeMulti.toDouble(),
                    displayRange = 0.0..4.0,
                    decimals = 1,
                    unit = "x",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.fetCompressor.kneeMulti,
                            it.toFloat().coerceIn(0.0f, 4.0f)
                        )
                    }
                )
            )
        },
        CardItem("gain_auto") {
            LabeledSwitch(
                label = stringResource(R.string.label_fet_auto_gain),
                checked = vals.gainAuto,
                onCheckedChange = { viewModel.applyPref(Effects.fetCompressor.gainAuto, it) }
            )
        },
        CardItem("gain") {
            LabeledSlider(
                label = stringResource(R.string.label_gain),
                value = gainDb,
                onValueChange = { viewModel.applyPref(Effects.fetCompressor.gain, compressorDbToRaw(it)) },
                valueRange = 0f..24f,
                enabled = !vals.gainAuto,
                valueLabel = "${gainDb.roundToInt()} dB",
                edit = SliderEdit(
                    displayValue = gainDb.toDouble(),
                    displayRange = 0.0..24.0,
                    decimals = 0,
                    unit = "dB",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.fetCompressor.gain,
                            compressorDbToRaw(it.toFloat().coerceIn(0.0f, 24.0f))
                        )
                    }
                )
            )
        },
        CardItem("attack_auto") {
            LabeledSwitch(
                label = stringResource(R.string.label_fet_auto_attack),
                checked = vals.attackAuto,
                onCheckedChange = { viewModel.applyPref(Effects.fetCompressor.attackAuto, it) }
            )
        },
        CardItem("attack") {
            LabeledSlider(
                label = stringResource(R.string.label_attack),
                value = attackMs.coerceIn(1f, 100f),
                onValueChange = { viewModel.applyPref(Effects.fetCompressor.attack, compressorMsToSeconds(it)) },
                valueRange = 1f..100f,
                enabled = !vals.attackAuto,
                valueLabel = "${attackMs.roundToInt()} ms",
                edit = SliderEdit(
                    displayValue = attackMs.toDouble(),
                    displayRange = 1.0..100.0,
                    decimals = 0,
                    unit = "ms",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.fetCompressor.attack,
                            compressorMsToSeconds(it.toFloat().coerceIn(1.0f, 100.0f))
                        )
                    }
                )
            )
        },
        CardItem("max_attack") {
            LabeledSlider(
                label = stringResource(R.string.label_fet_max_attack),
                value = maxAttackMs.coerceIn(1f, 100f),
                onValueChange = { viewModel.applyPref(Effects.fetCompressor.maxAttack, compressorMsToSeconds(it)) },
                valueRange = 1f..100f,
                valueLabel = "${maxAttackMs.roundToInt()} ms",
                edit = SliderEdit(
                    displayValue = maxAttackMs.toDouble(),
                    displayRange = 1.0..100.0,
                    decimals = 0,
                    unit = "ms",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.fetCompressor.maxAttack,
                            compressorMsToSeconds(it.toFloat().coerceIn(1.0f, 100.0f))
                        )
                    }
                )
            )
        },
        CardItem("release_auto") {
            LabeledSwitch(
                label = stringResource(R.string.label_fet_auto_release),
                checked = vals.releaseAuto,
                onCheckedChange = { viewModel.applyPref(Effects.fetCompressor.releaseAuto, it) }
            )
        },
        CardItem("release") {
            LabeledSlider(
                label = stringResource(R.string.label_release),
                value = releaseMs.coerceIn(5f, 500f),
                onValueChange = { viewModel.applyPref(Effects.fetCompressor.release, compressorMsToSeconds(it)) },
                valueRange = 5f..500f,
                enabled = !vals.releaseAuto,
                valueLabel = "${releaseMs.roundToInt()} ms",
                edit = SliderEdit(
                    displayValue = releaseMs.toDouble(),
                    displayRange = 5.0..500.0,
                    decimals = 0,
                    unit = "ms",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.fetCompressor.release,
                            compressorMsToSeconds(it.toFloat().coerceIn(5.0f, 500.0f))
                        )
                    }
                )
            )
        },
        CardItem("max_release") {
            LabeledSlider(
                label = stringResource(R.string.label_fet_max_release),
                value = maxReleaseMs.coerceIn(5f, 500f),
                onValueChange = { viewModel.applyPref(Effects.fetCompressor.maxRelease, compressorMsToSeconds(it)) },
                valueRange = 5f..500f,
                valueLabel = "${maxReleaseMs.roundToInt()} ms",
                edit = SliderEdit(
                    displayValue = maxReleaseMs.toDouble(),
                    displayRange = 5.0..500.0,
                    decimals = 0,
                    unit = "ms",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.fetCompressor.maxRelease,
                            compressorMsToSeconds(it.toFloat().coerceIn(5.0f, 500.0f))
                        )
                    }
                )
            )
        },
        CardItem("crest") {
            LabeledSlider(
                label = stringResource(R.string.label_fet_crest),
                value = crestMs.coerceIn(5f, 300f),
                onValueChange = { viewModel.applyPref(Effects.fetCompressor.crest, compressorMsToSeconds(it)) },
                valueRange = 5f..300f,
                valueLabel = "${crestMs.roundToInt()} ms",
                edit = SliderEdit(
                    displayValue = crestMs.toDouble(),
                    displayRange = 5.0..300.0,
                    decimals = 0,
                    unit = "ms",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.fetCompressor.crest,
                            compressorMsToSeconds(it.toFloat().coerceIn(5.0f, 300.0f))
                        )
                    }
                )
            )
        },
        CardItem("adapt") {
            LabeledSlider(
                label = stringResource(R.string.label_fet_adapt),
                value = vals.adapt,
                onValueChange = { viewModel.applyPref(Effects.fetCompressor.adapt, it) },
                valueRange = 1f..16f,
                valueLabel = String.format(Locale.US, "%.1fs", vals.adapt),
                edit = SliderEdit(
                    displayValue = vals.adapt.toDouble(),
                    displayRange = 1.0..16.0,
                    decimals = 1,
                    unit = "s",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.fetCompressor.adapt,
                            it.toFloat().coerceIn(1.0f, 16.0f)
                        )
                    }
                )
            )
        },
        CardItem("no_clip") {
            LabeledSwitch(
                label = stringResource(R.string.label_fet_no_clip),
                checked = vals.noClip,
                onCheckedChange = { viewModel.applyPref(Effects.fetCompressor.noClip, it) }
            )
        }
    )
}

private fun multibandCompressorCardItems(
    state: EffectState,
    viewModel: MainViewModel,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
): List<CardItem> {
    val mbVals = state.multibandCompressor
    val crossoverDefaults = listOf(120, 500, 4000, 8000)
    val crossovers = mbVals.crossovers
    val tabNames = listOf("Sub", "Low", "Mid", "Pres", "Air")
    val b = selectedTab.coerceIn(0, 4)

    val threshold = mbVals.thresholds.getOrElse(b) { compressorDbToRaw(-18.0f) }
    val ratio = mbVals.ratios.getOrElse(b) { compressorRatioToRaw(0.5f) }
    val gain = mbVals.gains.getOrElse(b) { 0.0f }
    val knee = mbVals.knees.getOrElse(b) { 0.0f }
    val kneeMulti = mbVals.kneeMultis.getOrElse(b) { 0.0f }
    val attack = mbVals.attacks.getOrElse(b) { compressorMsToSeconds(1.0f) }
    val maxAttack = mbVals.maxAttacks.getOrElse(b) { compressorMsToSeconds(44.0f) }
    val release = mbVals.releases.getOrElse(b) { compressorMsToSeconds(100.0f) }
    val maxRelease = mbVals.maxReleases.getOrElse(b) { compressorMsToSeconds(200.0f) }
    val crest = mbVals.crests.getOrElse(b) { compressorMsToSeconds(100.0f) }
    val adapt = mbVals.adapts.getOrElse(b) { compressorAdaptAmountToSeconds(0.5f) }
    val bandEnabled = mbVals.bandEnables.getOrElse(b) { true }
    val kneeAuto = mbVals.kneeAutos.getOrElse(b) { true }
    val gainAuto = mbVals.gainAutos.getOrElse(b) { true }
    val attackAuto = mbVals.attackAutos.getOrElse(b) { true }
    val releaseAuto = mbVals.releaseAutos.getOrElse(b) { true }
    val noClip = mbVals.noClips.getOrElse(b) { true }

    val thresholdDb = compressorRawToDb(threshold)
    val ratioDisplay = compressorRawToRatio(ratio)
    val kneeDb = compressorRawToDb(knee)
    val gainDb = compressorRawToDb(gain)
    val attackMs = compressorSecondsToMs(attack)
    val maxAttackMs = compressorSecondsToMs(maxAttack)
    val releaseMs = compressorSecondsToMs(release)
    val maxReleaseMs = compressorSecondsToMs(maxRelease)
    val crestMs = compressorSecondsToMs(crest)

    return buildList {
        add(
            CardItem("tabs") {
                val lowFreq = if (b == 0) 20 else crossovers.getOrElse(b - 1) { crossoverDefaults.getOrElse(b - 1) { 20 } }
                val highFreq = if (b < 4) crossovers.getOrElse(b) { crossoverDefaults.getOrElse(b) { 20000 } } else 20000
                TabRowWithContour(
                    tabs = tabNames,
                    selectedTabIndex = b,
                    onTabSelected = onSelectTab,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = "$lowFreq - ${if (b < 4) "$highFreq" else "20000+"} Hz",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )
            }
        )
        add(
            CardItem("band_enable") {
                LabeledSwitch(
                    label = stringResource(R.string.label_band_enabled),
                    checked = bandEnabled,
                    onCheckedChange = { viewModel.applyBandPref(Effects.multibandCompressor.bandEnables, b, it) }
                )
            }
        )
        if (b < 4) {
            add(
                CardItem("crossover") {
                    val crossoverHardMin = intArrayOf(30, 80, 300, 2000)
                    val crossoverHardMax = intArrayOf(300, 2000, 8000, 16000)
                    val neighborMin = if (b > 0) crossovers.getOrElse(b - 1) { crossoverDefaults.getOrElse(b - 1) { 20 } } + 50 else 30
                    val neighborMax = if (b < 3) crossovers.getOrElse(b + 1) { crossoverDefaults.getOrElse(b + 1) { 20000 } } - 50 else 16000
                    val minCrossover = maxOf(crossoverHardMin[b], neighborMin)
                    val maxCrossover = minOf(crossoverHardMax[b], neighborMax)
                    val curVal = crossovers.getOrElse(b) { crossoverDefaults[b] }
                    LabeledSlider(
                        label = stringResource(R.string.label_upper_crossover),
                        value = curVal.toFloat().coerceIn(minCrossover.toFloat(), maxCrossover.toFloat()),
                        onValueChange = { viewModel.applyBandPref(Effects.multibandCompressor.crossovers, b, it.roundToInt()) },
                        valueRange = minCrossover.toFloat()..maxCrossover.toFloat(),
                        steps = ((maxCrossover - minCrossover) / 5).coerceAtLeast(1) - 1,
                        valueLabel = "$curVal Hz",
                        edit = SliderEdit(
                            displayValue = curVal.toDouble(),
                            displayRange = minCrossover.toDouble()..maxCrossover.toDouble(),
                            decimals = 0,
                            unit = "Hz",
                            onCommit = {
                                viewModel.applyBandPref(
                                    Effects.multibandCompressor.crossovers,
                                    b,
                                    it.roundToInt().coerceIn(minCrossover, maxCrossover)
                                )
                            }
                        )
                    )
                }
            )
        }
        add(
            CardItem("threshold") {
                LabeledSlider(
                    label = stringResource(R.string.label_threshold),
                    value = thresholdDb,
                    onValueChange = { viewModel.applyBandPref(Effects.multibandCompressor.thresholds, b, compressorDbToRaw(it)) },
                    valueRange = -48f..0f,
                    valueLabel = "${thresholdDb.roundToInt()} dB",
                    edit = SliderEdit(
                        displayValue = thresholdDb.toDouble(),
                        displayRange = -48.0..0.0,
                        decimals = 0,
                        unit = "dB",
                        onCommit = {
                            viewModel.applyBandPref(
                                Effects.multibandCompressor.thresholds,
                                b,
                                compressorDbToRaw(it.toFloat().coerceIn(-48.0f, 0.0f))
                            )
                        }
                    )
                )
            }
        )
        add(
            CardItem("ratio") {
                LabeledSlider(
                    label = stringResource(R.string.label_fet_ratio),
                    value = ratioDisplay,
                    onValueChange = { viewModel.applyBandPref(Effects.multibandCompressor.ratios, b, compressorRatioToRaw(it)) },
                    valueRange = 0f..2f,
                    valueLabel = String.format(Locale.US, "%.1f", ratioDisplay),
                    edit = SliderEdit(
                        displayValue = ratioDisplay.toDouble(),
                        displayRange = 0.0..2.0,
                        decimals = 1,
                        onCommit = {
                            viewModel.applyBandPref(
                                Effects.multibandCompressor.ratios,
                                b,
                                compressorRatioToRaw(it.toFloat().coerceIn(0.0f, 2.0f))
                            )
                        }
                    )
                )
            }
        )
        add(
            CardItem("knee_auto") {
                LabeledSwitch(
                    label = stringResource(R.string.label_fet_auto_knee),
                    checked = kneeAuto,
                    onCheckedChange = { viewModel.applyBandPref(Effects.multibandCompressor.kneeAutos, b, it) }
                )
            }
        )
        add(
            CardItem("knee") {
                LabeledSlider(
                    label = stringResource(R.string.label_fet_knee),
                    value = kneeDb,
                    onValueChange = { viewModel.applyBandPref(Effects.multibandCompressor.knees, b, compressorDbToRaw(it)) },
                    valueRange = 0f..12f,
                    enabled = !kneeAuto,
                    valueLabel = "${kneeDb.roundToInt()} dB",
                    edit = SliderEdit(
                        displayValue = kneeDb.toDouble(),
                        displayRange = 0.0..12.0,
                        decimals = 0,
                        unit = "dB",
                        onCommit = {
                            viewModel.applyBandPref(
                                Effects.multibandCompressor.knees,
                                b,
                                compressorDbToRaw(it.toFloat().coerceIn(0.0f, 12.0f))
                            )
                        }
                    )
                )
            }
        )
        add(
            CardItem("knee_multi") {
                LabeledSlider(
                    label = stringResource(R.string.label_fet_knee_multi),
                    value = kneeMulti,
                    onValueChange = { viewModel.applyBandPref(Effects.multibandCompressor.kneeMultis, b, it) },
                    valueRange = 0f..4f,
                    valueLabel = String.format(Locale.US, "%.1fx", kneeMulti),
                    edit = SliderEdit(
                        displayValue = kneeMulti.toDouble(),
                        displayRange = 0.0..4.0,
                        decimals = 1,
                        unit = "x",
                        onCommit = {
                            viewModel.applyBandPref(
                                Effects.multibandCompressor.kneeMultis,
                                b,
                                it.toFloat().coerceIn(0.0f, 4.0f)
                            )
                        }
                    )
                )
            }
        )
        add(
            CardItem("gain_auto") {
                LabeledSwitch(
                    label = stringResource(R.string.label_fet_auto_gain),
                    checked = gainAuto,
                    onCheckedChange = { viewModel.applyBandPref(Effects.multibandCompressor.gainAutos, b, it) }
                )
            }
        )
        add(
            CardItem("gain") {
                LabeledSlider(
                    label = stringResource(R.string.label_gain),
                    value = gainDb,
                    onValueChange = { viewModel.applyBandPref(Effects.multibandCompressor.gains, b, compressorDbToRaw(it)) },
                    valueRange = 0f..24f,
                    enabled = !gainAuto,
                    valueLabel = "${gainDb.roundToInt()} dB",
                    edit = SliderEdit(
                        displayValue = gainDb.toDouble(),
                        displayRange = 0.0..24.0,
                        decimals = 0,
                        unit = "dB",
                        onCommit = {
                            viewModel.applyBandPref(
                                Effects.multibandCompressor.gains,
                                b,
                                compressorDbToRaw(it.toFloat().coerceIn(0.0f, 24.0f))
                            )
                        }
                    )
                )
            }
        )
        add(
            CardItem("attack_auto") {
                LabeledSwitch(
                    label = stringResource(R.string.label_fet_auto_attack),
                    checked = attackAuto,
                    onCheckedChange = { viewModel.applyBandPref(Effects.multibandCompressor.attackAutos, b, it) }
                )
            }
        )
        add(
            CardItem("attack") {
                LabeledSlider(
                    label = stringResource(R.string.label_attack),
                    value = attackMs.coerceIn(1f, 100f),
                    onValueChange = { viewModel.applyBandPref(Effects.multibandCompressor.attacks, b, compressorMsToSeconds(it)) },
                    valueRange = 1f..100f,
                    enabled = !attackAuto,
                    valueLabel = "${attackMs.roundToInt()} ms",
                    edit = SliderEdit(
                        displayValue = attackMs.toDouble(),
                        displayRange = 1.0..100.0,
                        decimals = 0,
                        unit = "ms",
                        onCommit = {
                            viewModel.applyBandPref(
                                Effects.multibandCompressor.attacks,
                                b,
                                compressorMsToSeconds(it.toFloat().coerceIn(1.0f, 100.0f))
                            )
                        }
                    )
                )
            }
        )
        add(
            CardItem("max_attack") {
                LabeledSlider(
                    label = stringResource(R.string.label_fet_max_attack),
                    value = maxAttackMs.coerceIn(1f, 100f),
                    onValueChange = { viewModel.applyBandPref(Effects.multibandCompressor.maxAttacks, b, compressorMsToSeconds(it)) },
                    valueRange = 1f..100f,
                    valueLabel = "${maxAttackMs.roundToInt()} ms",
                    edit = SliderEdit(
                        displayValue = maxAttackMs.toDouble(),
                        displayRange = 1.0..100.0,
                        decimals = 0,
                        unit = "ms",
                        onCommit = {
                            viewModel.applyBandPref(
                                Effects.multibandCompressor.maxAttacks,
                                b,
                                compressorMsToSeconds(it.toFloat().coerceIn(1.0f, 100.0f))
                            )
                        }
                    )
                )
            }
        )
        add(
            CardItem("release_auto") {
                LabeledSwitch(
                    label = stringResource(R.string.label_fet_auto_release),
                    checked = releaseAuto,
                    onCheckedChange = { viewModel.applyBandPref(Effects.multibandCompressor.releaseAutos, b, it) }
                )
            }
        )
        add(
            CardItem("release") {
                LabeledSlider(
                    label = stringResource(R.string.label_release),
                    value = releaseMs.coerceIn(5f, 500f),
                    onValueChange = { viewModel.applyBandPref(Effects.multibandCompressor.releases, b, compressorMsToSeconds(it)) },
                    valueRange = 5f..500f,
                    enabled = !releaseAuto,
                    valueLabel = "${releaseMs.roundToInt()} ms",
                    edit = SliderEdit(
                        displayValue = releaseMs.toDouble(),
                        displayRange = 5.0..500.0,
                        decimals = 0,
                        unit = "ms",
                        onCommit = {
                            viewModel.applyBandPref(
                                Effects.multibandCompressor.releases,
                                b,
                                compressorMsToSeconds(it.toFloat().coerceIn(5.0f, 500.0f))
                            )
                        }
                    )
                )
            }
        )
        add(
            CardItem("max_release") {
                LabeledSlider(
                    label = stringResource(R.string.label_fet_max_release),
                    value = maxReleaseMs.coerceIn(5f, 500f),
                    onValueChange = { viewModel.applyBandPref(Effects.multibandCompressor.maxReleases, b, compressorMsToSeconds(it)) },
                    valueRange = 5f..500f,
                    valueLabel = "${maxReleaseMs.roundToInt()} ms",
                    edit = SliderEdit(
                        displayValue = maxReleaseMs.toDouble(),
                        displayRange = 5.0..500.0,
                        decimals = 0,
                        unit = "ms",
                        onCommit = {
                            viewModel.applyBandPref(
                                Effects.multibandCompressor.maxReleases,
                                b,
                                compressorMsToSeconds(it.toFloat().coerceIn(5.0f, 500.0f))
                            )
                        }
                    )
                )
            }
        )
        add(
            CardItem("crest") {
                LabeledSlider(
                    label = stringResource(R.string.label_fet_crest),
                    value = crestMs.coerceIn(5f, 300f),
                    onValueChange = { viewModel.applyBandPref(Effects.multibandCompressor.crests, b, compressorMsToSeconds(it)) },
                    valueRange = 5f..300f,
                    valueLabel = "${crestMs.roundToInt()} ms",
                    edit = SliderEdit(
                        displayValue = crestMs.toDouble(),
                        displayRange = 5.0..300.0,
                        decimals = 0,
                        unit = "ms",
                        onCommit = {
                            viewModel.applyBandPref(
                                Effects.multibandCompressor.crests,
                                b,
                                compressorMsToSeconds(it.toFloat().coerceIn(5.0f, 300.0f))
                            )
                        }
                    )
                )
            }
        )
        add(
            CardItem("adapt") {
                LabeledSlider(
                    label = stringResource(R.string.label_fet_adapt),
                    value = adapt,
                    onValueChange = { viewModel.applyBandPref(Effects.multibandCompressor.adapts, b, it) },
                    valueRange = 1f..16f,
                    valueLabel = String.format(Locale.US, "%.1fs", adapt),
                    edit = SliderEdit(
                        displayValue = adapt.toDouble(),
                        displayRange = 1.0..16.0,
                        decimals = 1,
                        unit = "s",
                        onCommit = {
                            viewModel.applyBandPref(
                                Effects.multibandCompressor.adapts,
                                b,
                                it.toFloat().coerceIn(1.0f, 16.0f)
                            )
                        }
                    )
                )
            }
        )
        add(
            CardItem("no_clip") {
                LabeledSwitch(
                    label = stringResource(R.string.label_fet_no_clip),
                    checked = noClip,
                    onCheckedChange = { viewModel.applyBandPref(Effects.multibandCompressor.noClips, b, it) }
                )
            }
        )
    }
}

private fun ddcCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val device = state.ddc.device
    return listOf(
        CardItem("device") {
            val vdcFiles by viewModel.vdcFileList.collectAsStateWithLifecycle()
            val vdcNoneLabel = stringResource(R.string.label_none)
            val cdvOptions = listOf(vdcNoneLabel) + vdcFiles
            LabeledDropdown(
                label = stringResource(R.string.label_ddc_device),
                selectedValue = device.ifEmpty { vdcNoneLabel },
                options = cdvOptions,
                onOptionSelected = { _, value ->
                    viewModel.setDdcDevice(if (value == vdcNoneLabel) "" else value)
                },
                onDeleteOption = { _, name -> viewModel.deleteVdcFile(name) },
                isOptionDeletable = { _, name -> name != vdcNoneLabel }
            )
        }
    )
}

private fun spectrumExtensionCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val vals = state.spectrumExtension
    val strength = vals.strength
    val exciter = vals.exciter
    return listOf(
        CardItem("strength") {
            LabeledSlider(
                label = stringResource(R.string.label_strength),
                value = strength.toFloat(),
                onValueChange = { viewModel.applyPref(Effects.spectrumExtension.strength, it.roundToInt()) },
                valueRange = 2200f..8200f,
                steps = 1199,
                valueLabel = "$strength Hz",
                edit = SliderEdit(
                    displayValue = strength.toDouble(),
                    displayRange = 2200.0..8200.0,
                    decimals = 0,
                    unit = "Hz",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.spectrumExtension.strength,
                            it.roundToInt().coerceIn(2200, 8200)
                        )
                    }
                )
            )
        },
        CardItem("exciter") {
            LabeledSlider(
                label = stringResource(R.string.label_vse_exciter),
                value = exciter,
                onValueChange = { viewModel.applyPref(Effects.spectrumExtension.exciter, it) },
                valueRange = 0f..6f,
                valueLabel = String.format(Locale.US, "%.1fx", exciter),
                edit = SliderEdit(
                    displayValue = exciter.toDouble(),
                    displayRange = 0.0..6.0,
                    decimals = 1,
                    unit = "x",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.spectrumExtension.exciter,
                            it.toFloat().coerceIn(0.0f, 6.0f)
                        )
                    }
                )
            )
        }
    )
}

private fun equalizerCardItems(
    state: EffectState,
    viewModel: MainViewModel,
    onOpenEqScreen: (() -> Unit)? = null
): List<CardItem> {
    val eqVals = state.eq
    val bandCount = eqVals.bandCount
    val presetId = eqVals.presetId
    val eqPresets = eqVals.presets
    val bands = eqVals.bands.map { it.toFloat() }
    val bandCounts = listOf(10, 15, 25, 31)
    val bandCountIndex = when (bandCount) {
        15 -> 1
        25 -> 2
        31 -> 3
        else -> 0
    }

    return buildList {
        add(
            CardItem("band_count") {
                val bandCountOptions = bandCounts.map { stringResource(R.string.label_eq_n_bands, it) }
                LabeledDropdown(
                    label = stringResource(R.string.label_eq_bands),
                    selectedValue = bandCountOptions[bandCountIndex],
                    options = bandCountOptions,
                    onOptionSelected = { index, _ -> viewModel.setEqBandCount(bandCounts[index]) }
                )
            }
        )
        if (bands.size >= bandCount) {
            add(
                CardItem("graph") {
                    var showEqDialog by remember { mutableStateOf(false) }
                    EqCurveGraph(
                        bands = bands,
                        onClick = {
                            if (onOpenEqScreen != null) {
                                onOpenEqScreen()
                            } else {
                                showEqDialog = true
                            }
                        },
                        bandCount = bandCount
                    )
                    if (showEqDialog) {
                        EqEditDialog(
                            bands = bands,
                            onBandsChange = viewModel::setEqBands,
                            presetId = presetId,
                            presets = eqPresets,
                            onPresetSelect = viewModel::setEqPreset,
                            onPresetAdd = viewModel::addEqPreset,
                            onPresetDelete = viewModel::deleteEqPreset,
                            onReset = viewModel::resetEqBands,
                            onDismiss = { showEqDialog = false },
                            bandCount = bandCount
                        )
                    }
                }
            )
        }
    }
}

private fun dynamicEqCardItems(
    state: EffectState,
    viewModel: MainViewModel,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
): List<CardItem> {
    val dynVals = state.dynamicEq
    val bandCount = dynVals.bandCount
    val freqs = dynVals.freqs.take(bandCount)
    val qs = dynVals.qs.take(bandCount)
    val gains = dynVals.gains.take(bandCount)
    val thresholds = dynVals.thresholds.take(bandCount)
    val attacks = dynVals.attacks.take(bandCount)
    val releases = dynVals.releases.take(bandCount)
    val filterTypes = dynVals.filterTypes.take(bandCount)

    val safeTab = if (bandCount > 0) selectedTab.coerceIn(0, bandCount - 1) else 0
    fun formatFreq(hz: Int): String = if (hz >= 1000) "${hz / 1000}kHz" else "${hz}Hz"

    return buildList {
        add(
            CardItem("tabs") {
                var deleteBandIndex by remember { mutableIntStateOf(-1) }
                if (deleteBandIndex >= 0) {
                    ConfirmDialog(
                        title = stringResource(R.string.dialog_delete_band),
                        body = stringResource(
                            R.string.dialog_delete_band_confirm,
                            formatFreq(freqs.getOrElse(deleteBandIndex) { 1000 })
                        ),
                        confirmLabel = stringResource(R.string.action_delete),
                        destructive = true,
                        onConfirm = {
                            val i = deleteBandIndex
                            deleteBandIndex = -1
                            viewModel.removeDynamicEqBand(i)
                            if (safeTab >= bandCount - 1) onSelectTab(maxOf(0, bandCount - 2))
                        },
                        onDismiss = { deleteBandIndex = -1 }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (bandCount > 0) {
                        TabRowWithContour(
                            tabs = (0 until bandCount).map { formatFreq(freqs.getOrElse(it) { 1000 }) },
                            selectedTabIndex = safeTab,
                            onTabSelected = onSelectTab,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    val lastFreq = if (bandCount > 0) freqs.getOrElse(bandCount - 1) { 0 } else 0
                    if (bandCount < 10 && lastFreq < 20000) {
                        IconButton(
                            onClick = {
                                viewModel.addDynamicEqBand()
                                onSelectTab(bandCount)
                            },
                            minHeight = 35.dp,
                            minWidth = 35.dp,
                            backgroundColor = MiuixTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MiuixTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    if (bandCount > 1) {
                        IconButton(
                            onClick = { deleteBandIndex = safeTab },
                            minHeight = 35.dp,
                            minWidth = 35.dp,
                            backgroundColor = MiuixTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                modifier = Modifier.size(18.dp),
                                tint = StatusColors.danger
                            )
                        }
                    }
                }
            }
        )

        if (bandCount > 0) {
            val freq = freqs.getOrElse(safeTab) { 1000 }
            val q = qs.getOrElse(safeTab) { 1.5f }
            val gain = gains.getOrElse(safeTab) { 0.0f }
            val threshold = thresholds.getOrElse(safeTab) { -30.0f }
            val attack = attacks.getOrElse(safeTab) { 10.0f }
            val release = releases.getOrElse(safeTab) { 100.0f }
            val filterType = filterTypes.getOrElse(safeTab) { 0 }.coerceIn(0, 2)
            val minFreq = if (safeTab > 0) (freqs.getOrElse(safeTab - 1) { 20 } + 5).toFloat() else 20f
            val maxFreq = if (safeTab < bandCount - 1) (freqs.getOrElse(safeTab + 1) { 20000 } - 5).toFloat() else 20000f

            add(
                CardItem("freq") {
                    LabeledSlider(
                        label = stringResource(R.string.label_frequency),
                        value = freq.toFloat().coerceIn(minFreq, maxFreq),
                        onValueChange = {
                            viewModel.applyBandPref(Effects.dynamicEq.freqs, safeTab, it.roundToInt(), bandCount)
                        },
                        valueRange = minFreq..maxFreq,
                        steps = ((maxFreq - minFreq).toInt() / 5).coerceAtLeast(1) - 1,
                        valueLabel = "$freq Hz",
                        edit = SliderEdit(
                            displayValue = freq.toDouble(),
                            displayRange = minFreq.toDouble()..maxFreq.toDouble(),
                            decimals = 0,
                            unit = "Hz",
                            onCommit = {
                                viewModel.applyBandPref(
                                    Effects.dynamicEq.freqs,
                                    safeTab,
                                    it.roundToInt().coerceIn(minFreq.toInt(), maxFreq.toInt()),
                                    bandCount
                                )
                            }
                        )
                    )
                }
            )
            add(
                CardItem("q") {
                    LabeledSlider(
                        label = stringResource(R.string.label_dynamic_eq_q_factor),
                        value = q,
                        onValueChange = { viewModel.applyBandPref(Effects.dynamicEq.qs, safeTab, it, bandCount) },
                        valueRange = 0.3f..10f,
                        valueLabel = String.format(Locale.US, "%.2f", q),
                        edit = SliderEdit(
                            displayValue = q.toDouble(),
                            displayRange = 0.3..10.0,
                            decimals = 2,
                            onCommit = {
                                viewModel.applyBandPref(
                                    Effects.dynamicEq.qs,
                                    safeTab,
                                    it.toFloat().coerceIn(0.3f, 10.0f),
                                    bandCount
                                )
                            }
                        )
                    )
                }
            )
            add(
                CardItem("gain") {
                    LabeledSlider(
                        label = stringResource(R.string.label_dynamic_eq_target_gain),
                        value = gain,
                        onValueChange = { viewModel.applyBandPref(Effects.dynamicEq.gains, safeTab, it, bandCount) },
                        valueRange = -12f..12f,
                        valueLabel = String.format(Locale.US, "%.1f dB", gain),
                        edit = SliderEdit(
                            displayValue = gain.toDouble(),
                            displayRange = -12.0..12.0,
                            decimals = 1,
                            unit = "dB",
                            onCommit = {
                                viewModel.applyBandPref(
                                    Effects.dynamicEq.gains,
                                    safeTab,
                                    it.toFloat().coerceIn(-12.0f, 12.0f),
                                    bandCount
                                )
                            }
                        )
                    )
                }
            )
            add(
                CardItem("threshold") {
                    LabeledSlider(
                        label = stringResource(R.string.label_threshold),
                        value = threshold,
                        onValueChange = { viewModel.applyBandPref(Effects.dynamicEq.thresholds, safeTab, it, bandCount) },
                        valueRange = -80f..0f,
                        valueLabel = String.format(Locale.US, "%.1f dB", threshold),
                        edit = SliderEdit(
                            displayValue = threshold.toDouble(),
                            displayRange = -80.0..0.0,
                            decimals = 1,
                            unit = "dB",
                            onCommit = {
                                viewModel.applyBandPref(
                                    Effects.dynamicEq.thresholds,
                                    safeTab,
                                    it.toFloat().coerceIn(-80.0f, 0.0f),
                                    bandCount
                                )
                            }
                        )
                    )
                }
            )
            add(
                CardItem("attack") {
                    LabeledSlider(
                        label = stringResource(R.string.label_attack),
                        value = attack,
                        onValueChange = { viewModel.applyBandPref(Effects.dynamicEq.attacks, safeTab, it, bandCount) },
                        valueRange = 1f..100f,
                        valueLabel = "${attack.roundToInt()} ms",
                        edit = SliderEdit(
                            displayValue = attack.toDouble(),
                            displayRange = 1.0..100.0,
                            decimals = 0,
                            unit = "ms",
                            onCommit = {
                                viewModel.applyBandPref(
                                    Effects.dynamicEq.attacks,
                                    safeTab,
                                    it.toFloat().coerceIn(1.0f, 100.0f),
                                    bandCount
                                )
                            }
                        )
                    )
                }
            )
            add(
                CardItem("release") {
                    LabeledSlider(
                        label = stringResource(R.string.label_release),
                        value = release,
                        onValueChange = { viewModel.applyBandPref(Effects.dynamicEq.releases, safeTab, it, bandCount) },
                        valueRange = 10f..500f,
                        valueLabel = "${release.roundToInt()} ms",
                        edit = SliderEdit(
                            displayValue = release.toDouble(),
                            displayRange = 10.0..500.0,
                            decimals = 0,
                            unit = "ms",
                            onCommit = {
                                viewModel.applyBandPref(
                                    Effects.dynamicEq.releases,
                                    safeTab,
                                    it.toFloat().coerceIn(10.0f, 500.0f),
                                    bandCount
                                )
                            }
                        )
                    )
                }
            )
            add(
                CardItem("filter_type") {
                    val filterTypeNames = listOf(
                        stringResource(R.string.filter_peak),
                        stringResource(R.string.filter_low_shelf),
                        stringResource(R.string.filter_high_shelf)
                    )
                    LabeledDropdown(
                        label = stringResource(R.string.label_dynamic_eq_filter_type),
                        selectedValue = filterTypeNames[filterType],
                        options = filterTypeNames,
                        onOptionSelected = { index, _ ->
                            viewModel.applyBandPref(Effects.dynamicEq.filterTypes, safeTab, index, bandCount)
                        }
                    )
                }
            )
        }
    }
}

private fun convolverCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val vals = state.convolver
    val kernel = vals.kernelFile
    val crossChannel = vals.crossChannel
    return listOf(
        CardItem("kernel") {
            val kernelFiles by viewModel.kernelFileList.collectAsStateWithLifecycle()
            val kernelNoneLabel = stringResource(R.string.label_none)
            val kernelOptions = listOf(kernelNoneLabel) + kernelFiles
            LabeledDropdown(
                label = stringResource(R.string.label_convolver_kernel),
                selectedValue = kernel.ifEmpty { kernelNoneLabel },
                options = kernelOptions,
                onOptionSelected = { _, value ->
                    viewModel.setConvolverKernel(if (value == kernelNoneLabel) "" else value)
                },
                onDeleteOption = { _, name -> viewModel.deleteKernelFile(name) },
                isOptionDeletable = { _, name -> name != kernelNoneLabel }
            )
        },
        CardItem("cross_channel") {
            LabeledSlider(
                label = stringResource(R.string.label_convolver_cross_channel),
                value = crossChannel,
                onValueChange = { viewModel.applyPref(Effects.convolver.crossChannel, it) },
                valueRange = 0f..1f,
                valueLabel = "${(crossChannel * 100.0f).roundToInt()}%",
                edit = SliderEdit(
                    displayValue = (crossChannel * 100.0f).toDouble(),
                    displayRange = 0.0..100.0,
                    decimals = 0,
                    unit = "%",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.convolver.crossChannel,
                            (it / 100.0).toFloat().coerceIn(0.0f, 1.0f)
                        )
                    }
                )
            )
        }
    )
}

private fun fieldSurroundCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val vals = state.fieldSurround
    val widening = vals.widening
    val midImage = vals.midImage
    val depth = vals.depth
    return listOf(
        CardItem("widening") {
            LabeledSlider(
                label = stringResource(R.string.label_field_surround_widening),
                value = widening,
                onValueChange = { viewModel.applyPref(Effects.fieldSurround.widening, it) },
                valueRange = 0f..8f,
                steps = 7,
                valueLabel = widening.roundToInt().toString(),
                edit = SliderEdit(
                    displayValue = widening.toDouble(),
                    displayRange = 0.0..8.0,
                    decimals = 0,
                    onCommit = {
                        viewModel.applyPref(
                            Effects.fieldSurround.widening,
                            it.toFloat().coerceIn(0.0f, 8.0f)
                        )
                    }
                )
            )
        },
        CardItem("mid_image") {
            LabeledSlider(
                label = stringResource(R.string.label_field_surround_mid_image),
                value = midImage,
                onValueChange = { viewModel.applyPref(Effects.fieldSurround.midImage, it) },
                valueRange = 1f..2f,
                valueLabel = String.format(Locale.US, "%.1f", midImage),
                edit = SliderEdit(
                    displayValue = midImage.toDouble(),
                    displayRange = 1.0..2.0,
                    decimals = 1,
                    onCommit = {
                        viewModel.applyPref(
                            Effects.fieldSurround.midImage,
                            it.toFloat().coerceIn(1.0f, 2.0f)
                        )
                    }
                )
            )
        },
        CardItem("depth") {
            LabeledSlider(
                label = stringResource(R.string.label_depth),
                value = depth.toFloat(),
                onValueChange = { viewModel.applyPref(Effects.fieldSurround.depth, it.roundToInt()) },
                valueRange = 200f..950f,
                valueLabel = "${((depth - 200) / 750.0 * 100.0).roundToInt()}%",
                edit = SliderEdit(
                    displayValue = ((depth - 200) / 750.0 * 100.0),
                    displayRange = 0.0..100.0,
                    decimals = 0,
                    unit = "%",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.fieldSurround.depth,
                            (it / 100.0 * 750.0 + 200.0).roundToInt().coerceIn(200, 950)
                        )
                    }
                )
            )
        }
    )
}

private fun diffSurroundCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val vals = state.diffSurround
    val delay = vals.delay
    val reverse = vals.reverse
    val wetDryMix = vals.wetDryMix
    val lpCutoff = vals.lpCutoff
    return listOf(
        CardItem("delay") {
            LabeledSlider(
                label = stringResource(R.string.label_delay),
                value = delay,
                onValueChange = { viewModel.applyPref(Effects.diffSurround.delay, it) },
                valueRange = 1f..20f,
                steps = 18,
                valueLabel = "${delay.roundToInt()} ms",
                edit = SliderEdit(
                    displayValue = delay.toDouble(),
                    displayRange = 1.0..20.0,
                    decimals = 0,
                    unit = "ms",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.diffSurround.delay,
                            it.toFloat().coerceIn(1.0f, 20.0f)
                        )
                    }
                )
            )
        },
        CardItem("reverse") {
            LabeledSwitch(
                label = stringResource(R.string.label_diff_surround_reverse),
                checked = reverse,
                onCheckedChange = { viewModel.applyPref(Effects.diffSurround.reverse, it) }
            )
        },
        CardItem("wet_dry") {
            LabeledSlider(
                label = stringResource(R.string.label_diff_surround_wet_dry_mix),
                value = wetDryMix,
                onValueChange = { viewModel.applyPref(Effects.diffSurround.wetDryMix, it) },
                valueRange = 0f..1f,
                valueLabel = "${(wetDryMix * 100.0f).roundToInt()}%",
                edit = SliderEdit(
                    displayValue = (wetDryMix * 100.0f).toDouble(),
                    displayRange = 0.0..100.0,
                    decimals = 0,
                    unit = "%",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.diffSurround.wetDryMix,
                            (it / 100.0).toFloat().coerceIn(0.0f, 1.0f)
                        )
                    }
                )
            )
        },
        CardItem("lp_cutoff") {
            LabeledSlider(
                label = stringResource(R.string.label_diff_surround_lp_cutoff),
                value = lpCutoff.toFloat(),
                onValueChange = { viewModel.applyPref(Effects.diffSurround.lpCutoff, it.roundToInt()) },
                valueRange = 0f..20000f,
                steps = 3999,
                valueLabel = if (lpCutoff == 0) stringResource(R.string.label_off) else "$lpCutoff Hz",
                edit = SliderEdit(
                    displayValue = lpCutoff.toDouble(),
                    displayRange = 0.0..20000.0,
                    decimals = 0,
                    unit = "Hz",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.diffSurround.lpCutoff,
                            it.roundToInt().coerceIn(0, 20000)
                        )
                    }
                )
            )
        }
    )
}


private fun stereoImagerCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val vals = state.stereoImager
    val lowWidth = vals.lowWidth
    val midWidth = vals.midWidth
    val highWidth = vals.highWidth
    val lowCrossover = vals.lowCrossover
    val highCrossover = vals.highCrossover

    return listOf(
        CardItem("low_width") {
            LabeledSlider(
                label = stringResource(R.string.label_stereo_imager_low_width),
                value = lowWidth,
                onValueChange = { viewModel.applyPref(Effects.stereoImager.lowWidth, it) },
                valueRange = 0f..2f,
                valueLabel = "${(lowWidth * 100.0f).roundToInt()}%",
                edit = SliderEdit(
                    displayValue = (lowWidth * 100.0f).toDouble(),
                    displayRange = 0.0..200.0,
                    decimals = 0,
                    unit = "%",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.stereoImager.lowWidth,
                            (it / 100.0).toFloat().coerceIn(0.0f, 2.0f)
                        )
                    }
                )
            )
        },
        CardItem("mid_width") {
            LabeledSlider(
                label = stringResource(R.string.label_stereo_imager_mid_width),
                value = midWidth,
                onValueChange = { viewModel.applyPref(Effects.stereoImager.midWidth, it) },
                valueRange = 0f..2f,
                valueLabel = "${(midWidth * 100.0f).roundToInt()}%",
                edit = SliderEdit(
                    displayValue = (midWidth * 100.0f).toDouble(),
                    displayRange = 0.0..200.0,
                    decimals = 0,
                    unit = "%",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.stereoImager.midWidth,
                            (it / 100.0).toFloat().coerceIn(0.0f, 2.0f)
                        )
                    }
                )
            )
        },
        CardItem("high_width") {
            LabeledSlider(
                label = stringResource(R.string.label_stereo_imager_high_width),
                value = highWidth,
                onValueChange = { viewModel.applyPref(Effects.stereoImager.highWidth, it) },
                valueRange = 0f..2f,
                valueLabel = "${(highWidth * 100.0f).roundToInt()}%",
                edit = SliderEdit(
                    displayValue = (highWidth * 100.0f).toDouble(),
                    displayRange = 0.0..200.0,
                    decimals = 0,
                    unit = "%",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.stereoImager.highWidth,
                            (it / 100.0).toFloat().coerceIn(0.0f, 2.0f)
                        )
                    }
                )
            )
        },
        CardItem("low_crossover") {
            LabeledSlider(
                label = stringResource(R.string.label_stereo_imager_low_crossover),
                value = lowCrossover.toFloat(),
                onValueChange = { viewModel.applyPref(Effects.stereoImager.lowCrossover, it.roundToInt()) },
                valueRange = 80f..400f,
                steps = 63,
                valueLabel = "$lowCrossover Hz",
                edit = SliderEdit(
                    displayValue = lowCrossover.toDouble(),
                    displayRange = 80.0..400.0,
                    decimals = 0,
                    unit = "Hz",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.stereoImager.lowCrossover,
                            it.roundToInt().coerceIn(80, 400)
                        )
                    }
                )
            )
        },
        CardItem("high_crossover") {
            LabeledSlider(
                label = stringResource(R.string.label_stereo_imager_high_crossover),
                value = highCrossover.toFloat(),
                onValueChange = { viewModel.applyPref(Effects.stereoImager.highCrossover, it.roundToInt()) },
                valueRange = 2000f..8000f,
                steps = 1199,
                valueLabel = "$highCrossover Hz",
                edit = SliderEdit(
                    displayValue = highCrossover.toDouble(),
                    displayRange = 2000.0..8000.0,
                    decimals = 0,
                    unit = "Hz",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.stereoImager.highCrossover,
                            it.roundToInt().coerceIn(2000, 8000)
                        )
                    }
                )
            )
        }
    )
}

private fun headphoneSurroundCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val quality = state.headphoneSurround.quality
    return listOf(
        CardItem("quality") {
            LabeledSlider(
                label = stringResource(R.string.label_vhe_quality),
                value = quality.toFloat(),
                onValueChange = { viewModel.applyPref(Effects.headphoneSurround.quality, it.roundToInt()) },
                valueRange = 0f..4f,
                steps = 3,
                valueLabel = "$quality",
                edit = SliderEdit(
                    displayValue = quality.toDouble(),
                    displayRange = 0.0..4.0,
                    decimals = 0,
                    onCommit = {
                        viewModel.applyPref(
                            Effects.headphoneSurround.quality,
                            it.roundToInt().coerceIn(0, 4)
                        )
                    }
                )
            )
        }
    )
}

private fun reverberationCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val vals = state.reverb
    val roomSize = vals.roomSize
    val width = vals.width
    val damp = vals.damp
    val wet = vals.wet
    val dry = vals.dry

    return listOf(
        CardItem("room_size") {
            LabeledSlider(
                label = stringResource(R.string.label_reverb_room_size),
                value = roomSize,
                onValueChange = { viewModel.applyPref(Effects.reverb.roomSize, it) },
                valueRange = 0f..1f,
                valueLabel = String.format(Locale.US, "%.1f", roomSize * 10.0f),
                edit = SliderEdit(
                    displayValue = (roomSize * 10.0f).toDouble(),
                    displayRange = 0.0..10.0,
                    decimals = 1,
                    onCommit = {
                        viewModel.applyPref(
                            Effects.reverb.roomSize,
                            (it / 10.0).toFloat().coerceIn(0.0f, 1.0f)
                        )
                    }
                )
            )
        },
        CardItem("width") {
            LabeledSlider(
                label = stringResource(R.string.label_width),
                value = width,
                onValueChange = { viewModel.applyPref(Effects.reverb.width, it) },
                valueRange = 0f..1f,
                valueLabel = String.format(Locale.US, "%.1f", width * 10.0f),
                edit = SliderEdit(
                    displayValue = (width * 10.0f).toDouble(),
                    displayRange = 0.0..10.0,
                    decimals = 1,
                    onCommit = {
                        viewModel.applyPref(
                            Effects.reverb.width,
                            (it / 10.0).toFloat().coerceIn(0.0f, 1.0f)
                        )
                    }
                )
            )
        },
        CardItem("damp") {
            LabeledSlider(
                label = stringResource(R.string.label_reverb_dampening),
                value = damp,
                onValueChange = { viewModel.applyPref(Effects.reverb.damp, it) },
                valueRange = 0f..1f,
                valueLabel = String.format(Locale.US, "%.1f", damp * 10.0f),
                edit = SliderEdit(
                    displayValue = (damp * 10.0f).toDouble(),
                    displayRange = 0.0..10.0,
                    decimals = 1,
                    onCommit = {
                        viewModel.applyPref(
                            Effects.reverb.damp,
                            (it / 10.0).toFloat().coerceIn(0.0f, 1.0f)
                        )
                    }
                )
            )
        },
        CardItem("wet") {
            LabeledSlider(
                label = stringResource(R.string.label_reverb_wet),
                value = wet,
                onValueChange = { viewModel.applyPref(Effects.reverb.wet, it) },
                valueRange = 0f..1f,
                valueLabel = "${(wet * 100.0f).roundToInt()}%",
                edit = SliderEdit(
                    displayValue = (wet * 100.0f).toDouble(),
                    displayRange = 0.0..100.0,
                    decimals = 0,
                    unit = "%",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.reverb.wet,
                            (it / 100.0).toFloat().coerceIn(0.0f, 1.0f)
                        )
                    }
                )
            )
        },
        CardItem("dry") {
            LabeledSlider(
                label = stringResource(R.string.label_reverb_dry),
                value = dry,
                onValueChange = { viewModel.applyPref(Effects.reverb.dry, it) },
                valueRange = 0f..1f,
                valueLabel = "${(dry * 100.0f).roundToInt()}%",
                edit = SliderEdit(
                    displayValue = (dry * 100.0f).toDouble(),
                    displayRange = 0.0..100.0,
                    decimals = 0,
                    unit = "%",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.reverb.dry,
                            (it / 100.0).toFloat().coerceIn(0.0f, 1.0f)
                        )
                    }
                )
            )
        }
    )
}

private fun dynamicSystemCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val vals = state.dynamicSystem
    val dsPresetId = vals.presetId
    val dsPresets = vals.presets
    val strength = vals.strength
    val xLow = vals.xLow
    val xHigh = vals.xHigh
    val yLow = vals.yLow
    val yHigh = vals.yHigh
    val sideGainLow = vals.sideGainLow
    val sideGainHigh = vals.sideGainHigh

    val selectedPreset = dsPresets.find { it.id == dsPresetId }
    val isDeletable = selectedPreset != null && selectedPreset.nameKey == null

    return listOf(
        CardItem("preset") {
            val customLabel = stringResource(R.string.label_custom)
            val presetDisplayNames = dsPresets.map { resolvePresetName(it) }
            val currentPresetName = selectedPreset?.let { resolvePresetName(it) } ?: customLabel
            LabeledDropdown(
                label = stringResource(R.string.label_preset),
                selectedValue = currentPresetName,
                options = presetDisplayNames,
                onOptionSelected = { index, _ -> viewModel.setDynamicSystemPreset(dsPresets[index].id) },
                onDeleteOption = { index, _ -> viewModel.deleteDynamicSystemPreset(dsPresets[index].id) },
                isOptionDeletable = { index, _ -> dsPresets[index].nameKey == null }
            )
        },
        CardItem("preset_actions") {
            var showSaveDialog by remember { mutableStateOf(false) }
            var showDeleteDialog by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    text = stringResource(R.string.action_save),
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
                if (isDeletable) {
                    TextButton(
                        text = stringResource(R.string.action_delete),
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                TextButton(
                    text = stringResource(R.string.action_reset),
                    onClick = { viewModel.resetDynamicSystemCoefficients() },
                    modifier = Modifier.weight(1f)
                )
            }

            if (showSaveDialog) {
                InputDialog(
                    title = stringResource(R.string.preset_save_title),
                    initialValue = "",
                    placeholder = stringResource(R.string.preset_name_hint),
                    confirmLabel = stringResource(R.string.action_save),
                    onConfirm = { name ->
                        showSaveDialog = false
                        viewModel.addDynamicSystemPreset(name)
                    },
                    onDismiss = { showSaveDialog = false }
                )
            }

            if (showDeleteDialog && selectedPreset != null) {
                ConfirmDialog(
                    title = stringResource(R.string.preset_delete_title),
                    body = stringResource(R.string.preset_delete_confirm, resolvePresetName(selectedPreset)),
                    confirmLabel = stringResource(R.string.action_delete),
                    destructive = true,
                    onConfirm = {
                        showDeleteDialog = false
                        viewModel.deleteDynamicSystemPreset(selectedPreset.id)
                    },
                    onDismiss = { showDeleteDialog = false }
                )
            }
        },
        CardItem("strength") {
            LabeledSlider(
                label = stringResource(R.string.label_dynamic_system_strength),
                value = strength,
                onValueChange = { viewModel.applyPref(Effects.dynamicSystem.strength, it) },
                valueRange = 0f..1f,
                valueLabel = "${(strength * 100.0f).roundToInt()}%",
                edit = SliderEdit(
                    displayValue = (strength * 100.0f).toDouble(),
                    displayRange = 0.0..100.0,
                    decimals = 0,
                    unit = "%",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.dynamicSystem.strength,
                            (it / 100.0).toFloat().coerceIn(0.0f, 1.0f)
                        )
                    }
                )
            )
        },
        CardItem("x_low") {
            LabeledSlider(
                label = stringResource(R.string.label_dynamic_system_x_low_freq),
                value = xLow.toFloat(),
                onValueChange = { viewModel.setDynamicSystemXLow(it.roundToInt()) },
                valueRange = 0f..2400f,
                steps = 479,
                valueLabel = "$xLow Hz",
                edit = SliderEdit(
                    displayValue = xLow.toDouble(),
                    displayRange = 0.0..2400.0,
                    decimals = 0,
                    unit = "Hz",
                    onCommit = {
                        viewModel.setDynamicSystemXLow(
                            it.roundToInt().coerceIn(0, 2400)
                        )
                    }
                )
            )
        },
        CardItem("x_high") {
            LabeledSlider(
                label = stringResource(R.string.label_dynamic_system_x_high_freq),
                value = xHigh.toFloat(),
                onValueChange = { viewModel.setDynamicSystemXHigh(it.roundToInt()) },
                valueRange = 0f..12000f,
                steps = 2399,
                valueLabel = "$xHigh Hz",
                edit = SliderEdit(
                    displayValue = xHigh.toDouble(),
                    displayRange = 0.0..12000.0,
                    decimals = 0,
                    unit = "Hz",
                    onCommit = {
                        viewModel.setDynamicSystemXHigh(
                            it.roundToInt().coerceIn(0, 12000)
                        )
                    }
                )
            )
        },
        CardItem("y_low") {
            LabeledSlider(
                label = stringResource(R.string.label_dynamic_system_y_low_freq),
                value = yLow.toFloat(),
                onValueChange = { viewModel.setDynamicSystemYLow(it.roundToInt()) },
                valueRange = 0f..200f,
                steps = 199,
                valueLabel = "$yLow Hz",
                edit = SliderEdit(
                    displayValue = yLow.toDouble(),
                    displayRange = 0.0..200.0,
                    decimals = 0,
                    unit = "Hz",
                    onCommit = {
                        viewModel.setDynamicSystemYLow(
                            it.roundToInt().coerceIn(0, 200)
                        )
                    }
                )
            )
        },
        CardItem("y_high") {
            LabeledSlider(
                label = stringResource(R.string.label_dynamic_system_y_high_freq),
                value = yHigh.toFloat(),
                onValueChange = { viewModel.setDynamicSystemYHigh(it.roundToInt()) },
                valueRange = 0f..300f,
                steps = 59,
                valueLabel = "$yHigh Hz",
                edit = SliderEdit(
                    displayValue = yHigh.toDouble(),
                    displayRange = 0.0..300.0,
                    decimals = 0,
                    unit = "Hz",
                    onCommit = {
                        viewModel.setDynamicSystemYHigh(
                            it.roundToInt().coerceIn(0, 300)
                        )
                    }
                )
            )
        },
        CardItem("side_gain_low") {
            LabeledSlider(
                label = stringResource(R.string.label_dynamic_system_side_gain_low),
                value = sideGainLow,
                onValueChange = { viewModel.setDynamicSystemSideGainLow(it) },
                valueRange = 0f..1f,
                valueLabel = "${(sideGainLow * 100.0f).roundToInt()}%",
                edit = SliderEdit(
                    displayValue = (sideGainLow * 100.0f).toDouble(),
                    displayRange = 0.0..100.0,
                    decimals = 0,
                    unit = "%",
                    onCommit = {
                        viewModel.setDynamicSystemSideGainLow(
                            (it / 100.0).toFloat().coerceIn(0.0f, 1.0f)
                        )
                    }
                )
            )
        },
        CardItem("side_gain_high") {
            LabeledSlider(
                label = stringResource(R.string.label_dynamic_system_side_gain_high),
                value = sideGainHigh,
                onValueChange = { viewModel.setDynamicSystemSideGainHigh(it) },
                valueRange = 0f..1f,
                valueLabel = "${(sideGainHigh * 100.0f).roundToInt()}%",
                edit = SliderEdit(
                    displayValue = (sideGainHigh * 100.0f).toDouble(),
                    displayRange = 0.0..100.0,
                    decimals = 0,
                    unit = "%",
                    onCommit = {
                        viewModel.setDynamicSystemSideGainHigh(
                            (it / 100.0).toFloat().coerceIn(0.0f, 1.0f)
                        )
                    }
                )
            )
        }
    )
}

private fun tubeSimulatorCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val vals = state.tubeSimulator
    val model = vals.model
    val drive = vals.drive
    val mix = vals.mix
    val tubeModels = listOf("12AX7", "12AU7", "6N1P")

    return listOf(
        CardItem("model") {
            LabeledDropdown(
                label = stringResource(R.string.label_tube_model),
                selectedValue = tubeModels.getOrElse(model) { "12AX7" },
                options = tubeModels,
                onOptionSelected = { index, _ -> viewModel.applyPref(Effects.tubeSimulator.model, index) }
            )
        },
        CardItem("drive") {
            LabeledSlider(
                label = stringResource(R.string.label_tube_drive),
                value = drive,
                onValueChange = { viewModel.applyPref(Effects.tubeSimulator.drive, it) },
                valueRange = 0f..1f,
                valueLabel = "${(drive * 100.0f).roundToInt()}%",
                edit = SliderEdit(
                    displayValue = (drive * 100.0f).toDouble(),
                    displayRange = 0.0..100.0,
                    decimals = 0,
                    unit = "%",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.tubeSimulator.drive,
                            (it / 100.0).toFloat().coerceIn(0.0f, 1.0f)
                        )
                    }
                )
            )
        },
        CardItem("mix") {
            LabeledSlider(
                label = stringResource(R.string.label_tube_mix),
                value = mix,
                onValueChange = { viewModel.applyPref(Effects.tubeSimulator.mix, it) },
                valueRange = 0f..1f,
                valueLabel = "${(mix * 100.0f).roundToInt()}%",
                edit = SliderEdit(
                    displayValue = (mix * 100.0f).toDouble(),
                    displayRange = 0.0..100.0,
                    decimals = 0,
                    unit = "%",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.tubeSimulator.mix,
                            (it / 100.0).toFloat().coerceIn(0.0f, 1.0f)
                        )
                    }
                )
            )
        }
    )
}

private fun psychoacousticBassCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val vals = state.psychoacousticBass
    val cutoff = vals.cutoff
    val intensity = vals.intensity
    val harmonicOrder = vals.harmonicOrder
    val originalLevel = vals.originalLevel

    return listOf(
        CardItem("cutoff") {
            LabeledSlider(
                label = stringResource(R.string.label_psycho_bass_cutoff),
                value = cutoff.toFloat(),
                onValueChange = { viewModel.applyPref(Effects.psychoacousticBass.cutoff, it.roundToInt()) },
                valueRange = 60f..150f,
                steps = 17,
                valueLabel = "$cutoff Hz",
                edit = SliderEdit(
                    displayValue = cutoff.toDouble(),
                    displayRange = 60.0..150.0,
                    decimals = 0,
                    unit = "Hz",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.psychoacousticBass.cutoff,
                            it.roundToInt().coerceIn(60, 150)
                        )
                    }
                )
            )
        },
        CardItem("intensity") {
            LabeledSlider(
                label = stringResource(R.string.label_psycho_bass_intensity),
                value = intensity,
                onValueChange = { viewModel.applyPref(Effects.psychoacousticBass.intensity, it) },
                valueRange = 0f..1f,
                valueLabel = "${(intensity * 100.0f).roundToInt()}%",
                edit = SliderEdit(
                    displayValue = (intensity * 100.0f).toDouble(),
                    displayRange = 0.0..100.0,
                    decimals = 0,
                    unit = "%",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.psychoacousticBass.intensity,
                            (it / 100.0).toFloat().coerceIn(0.0f, 1.0f)
                        )
                    }
                )
            )
        },
        CardItem("harmonic_order") {
            val harmonicLabels = listOf(
                stringResource(R.string.harmonic_2nd),
                stringResource(R.string.harmonic_3rd),
                stringResource(R.string.harmonic_4th),
                stringResource(R.string.harmonic_5th)
            )
            LabeledSlider(
                label = stringResource(R.string.label_psycho_bass_harmonic_order),
                value = harmonicOrder.toFloat(),
                onValueChange = { viewModel.applyPref(Effects.psychoacousticBass.harmonicOrder, it.roundToInt()) },
                valueRange = 2f..5f,
                steps = 2,
                valueLabel = harmonicLabels.getOrElse(harmonicOrder - 2) { harmonicLabels.last() },
                edit = SliderEdit(
                    displayValue = harmonicOrder.toDouble(),
                    displayRange = 2.0..5.0,
                    decimals = 0,
                    onCommit = {
                        viewModel.applyPref(
                            Effects.psychoacousticBass.harmonicOrder,
                            it.roundToInt().coerceIn(2, 5)
                        )
                    }
                )
            )
        },
        CardItem("original_level") {
            LabeledSlider(
                label = stringResource(R.string.label_psycho_bass_ori_bass_level),
                value = originalLevel,
                onValueChange = { viewModel.applyPref(Effects.psychoacousticBass.originalLevel, it) },
                valueRange = 0f..1f,
                valueLabel = "${(originalLevel * 100.0f).roundToInt()}%",
                edit = SliderEdit(
                    displayValue = (originalLevel * 100.0f).toDouble(),
                    displayRange = 0.0..100.0,
                    decimals = 0,
                    unit = "%",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.psychoacousticBass.originalLevel,
                            (it / 100.0).toFloat().coerceIn(0.0f, 1.0f)
                        )
                    }
                )
            )
        }
    )
}

private fun viperBassCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val vals = state.bass
    val mode = vals.mode
    val freq = vals.frequency
    val gain = vals.gain
    val antiPop = vals.antiPop

    return buildList {
        add(
            CardItem("mode") {
                val bassModes = listOf(
                    stringResource(R.string.bass_mode_natural),
                    stringResource(R.string.bass_mode_pure),
                    stringResource(R.string.bass_mode_subwoofer)
                )
                LabeledDropdown(
                    label = stringResource(R.string.label_mode),
                    selectedValue = bassModes.getOrElse(mode) { bassModes[0] },
                    options = bassModes,
                    onOptionSelected = { index, _ -> viewModel.applyPref(Effects.bass.mode, index) }
                )
            }
        )
        if (mode != 2) {
            add(
                CardItem("freq") {
                    LabeledSlider(
                        label = stringResource(R.string.label_frequency),
                        value = freq.toFloat(),
                        onValueChange = { viewModel.applyPref(Effects.bass.frequency, it.roundToInt()) },
                        valueRange = 15f..150f,
                        steps = 134,
                        valueLabel = "$freq Hz",
                        edit = SliderEdit(
                            displayValue = freq.toDouble(),
                            displayRange = 15.0..150.0,
                            decimals = 0,
                            unit = "Hz",
                            onCommit = {
                                viewModel.applyPref(
                                    Effects.bass.frequency,
                                    it.roundToInt().coerceIn(15, 150)
                                )
                            }
                        )
                    )
                }
            )
        }
        add(
            CardItem("gain") {
                LabeledSlider(
                    label = stringResource(R.string.label_gain),
                    value = gain,
                    onValueChange = { viewModel.applyPref(Effects.bass.gain, it) },
                    valueRange = 0.5f..10.0f,
                    valueLabel = String.format(Locale.US, "%.1fx", gain),
                    edit = SliderEdit(
                        displayValue = gain.toDouble(),
                        displayRange = 0.5..10.0,
                        decimals = 1,
                        unit = "x",
                        onCommit = {
                            viewModel.applyPref(
                                Effects.bass.gain,
                                it.toFloat().coerceIn(0.5f, 10.0f)
                            )
                        }
                    )
                )
            }
        )
        add(
            CardItem("anti_pop") {
                LabeledSwitch(
                    label = stringResource(R.string.label_bass_anti_pop),
                    checked = antiPop,
                    onCheckedChange = { viewModel.applyPref(Effects.bass.antiPop, it) }
                )
            }
        )
    }
}

private fun viperBassMonoCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val vals = state.bassMono
    val mode = vals.mode
    val freq = vals.frequency
    val gain = vals.gain
    val antiPop = vals.antiPop

    return buildList {
        add(
            CardItem("mode") {
                val bassModes = listOf(
                    stringResource(R.string.bass_mode_natural),
                    stringResource(R.string.bass_mode_pure),
                    stringResource(R.string.bass_mode_subwoofer)
                )
                LabeledDropdown(
                    label = stringResource(R.string.label_mode),
                    selectedValue = bassModes.getOrElse(mode) { bassModes[0] },
                    options = bassModes,
                    onOptionSelected = { index, _ -> viewModel.applyPref(Effects.bassMono.mode, index) }
                )
            }
        )
        if (mode != 2) {
            add(
                CardItem("freq") {
                    LabeledSlider(
                        label = stringResource(R.string.label_frequency),
                        value = freq.toFloat(),
                        onValueChange = { viewModel.applyPref(Effects.bassMono.frequency, it.roundToInt()) },
                        valueRange = 15f..150f,
                        steps = 134,
                        valueLabel = "$freq Hz",
                        edit = SliderEdit(
                            displayValue = freq.toDouble(),
                            displayRange = 15.0..150.0,
                            decimals = 0,
                            unit = "Hz",
                            onCommit = {
                                viewModel.applyPref(
                                    Effects.bassMono.frequency,
                                    it.roundToInt().coerceIn(15, 150)
                                )
                            }
                        )
                    )
                }
            )
        }
        add(
            CardItem("gain") {
                LabeledSlider(
                    label = stringResource(R.string.label_gain),
                    value = gain,
                    onValueChange = { viewModel.applyPref(Effects.bassMono.gain, it) },
                    valueRange = 0.5f..10.0f,
                    valueLabel = String.format(Locale.US, "%.1fx", gain),
                    edit = SliderEdit(
                        displayValue = gain.toDouble(),
                        displayRange = 0.5..10.0,
                        decimals = 1,
                        unit = "x",
                        onCommit = {
                            viewModel.applyPref(
                                Effects.bassMono.gain,
                                it.toFloat().coerceIn(0.5f, 10.0f)
                            )
                        }
                    )
                )
            }
        )
        add(
            CardItem("anti_pop") {
                LabeledSwitch(
                    label = stringResource(R.string.label_bass_anti_pop),
                    checked = antiPop,
                    onCheckedChange = { viewModel.applyPref(Effects.bassMono.antiPop, it) }
                )
            }
        )
    }
}

private fun viperClarityCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val vals = state.clarity
    val mode = vals.mode
    val gain = vals.gain

    return listOf(
        CardItem("mode") {
            val clarityModes = listOf(
                stringResource(R.string.clarity_mode_natural),
                stringResource(R.string.clarity_mode_ozone),
                stringResource(R.string.clarity_mode_xhifi)
            )
            LabeledDropdown(
                label = stringResource(R.string.label_mode),
                selectedValue = clarityModes.getOrElse(mode) { clarityModes[0] },
                options = clarityModes,
                onOptionSelected = { index, _ -> viewModel.applyPref(Effects.clarity.mode, index) }
            )
        },
        CardItem("gain") {
            LabeledSlider(
                label = stringResource(R.string.label_gain),
                value = gain,
                onValueChange = { viewModel.applyPref(Effects.clarity.gain, it) },
                valueRange = 0f..4.5f,
                valueLabel = String.format(Locale.US, "%.1fx", gain),
                edit = SliderEdit(
                    displayValue = gain.toDouble(),
                    displayRange = 0.0..4.5,
                    decimals = 1,
                    unit = "x",
                    onCommit = {
                        viewModel.applyPref(
                            Effects.clarity.gain,
                            it.toFloat().coerceIn(0.0f, 4.5f)
                        )
                    }
                )
            )
        }
    )
}

private fun auditoryProtectionCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val crossfeed = state.cure.crossfeedPreset
    return listOf(
        CardItem("crossfeed") {
            val levels = listOf(
                stringResource(R.string.label_mild),
                stringResource(R.string.label_medium),
                stringResource(R.string.label_strong)
            )
            LabeledDropdown(
                label = stringResource(R.string.label_cure_strength),
                selectedValue = levels.getOrElse(crossfeed) { levels[0] },
                options = levels,
                onOptionSelected = { index, _ -> viewModel.applyPref(Effects.cure.crossfeedPreset, index) }
            )
        }
    )
}

private fun analogXCardItems(
    state: EffectState,
    viewModel: MainViewModel
): List<CardItem> {
    val mode = state.analogX.mode
    return listOf(
        CardItem("mode") {
            val levels = listOf(
                stringResource(R.string.label_mild),
                stringResource(R.string.label_medium),
                stringResource(R.string.label_strong)
            )
            LabeledDropdown(
                label = stringResource(R.string.label_mode),
                selectedValue = levels.getOrElse(mode) { levels[0] },
                options = levels,
                onOptionSelected = { index, _ -> viewModel.applyPref(Effects.analogX.mode, index) }
            )
        }
    )
}

/**
 * 将所有 22 个音效模块拆分为独立 lazy item 挂载到 LazyListScope，
 * 遵循 AGENTS.md 的分角拼卡（CardSegment）与连续高度收缩规范。
 */
fun LazyListScope.allEffectSectionsItems(
    state: EffectState,
    viewModel: MainViewModel,
    controller: EffectExpandController,
    scope: CoroutineScope,
    onOpenEqScreen: (() -> Unit)? = null
) {
    effectSectionItems(
        sectionKey = "masterLimiter",
        titleRes = R.string.section_master_limiter,
        descriptionRes = R.string.effect_desc_master_limiter,
        icon = Icons.Default.Tune,
        enabled = true,
        onEnabledChange = {},
        hasEnableSwitch = false,
        controller = controller,
        scope = scope,
        rows = masterLimiterCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "playbackGain",
        titleRes = R.string.section_agc,
        descriptionRes = R.string.effect_desc_playback_gain_control,
        icon = Icons.AutoMirrored.Filled.TrendingUp,
        enabled = state.playbackGainControl.enable,
        onEnabledChange = viewModel::setPlaybackGainControlEnabled,
        controller = controller,
        scope = scope,
        rows = playbackGainCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "lufsTargeting",
        titleRes = R.string.section_lufs_targeting,
        descriptionRes = R.string.effect_desc_lufs_targeting,
        icon = Icons.Default.VerticalAlignCenter,
        enabled = state.lufs.enable,
        onEnabledChange = viewModel::setLufsEnabled,
        controller = controller,
        scope = scope,
        rows = lufsTargetingCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "fetCompressor",
        titleRes = R.string.section_fet_compressor,
        descriptionRes = R.string.effect_desc_fet_compressor,
        icon = Icons.Default.Compress,
        enabled = state.fetCompressor.enable,
        onEnabledChange = viewModel::setFetCompressorEnabled,
        controller = controller,
        scope = scope,
        rows = fetCompressorCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "multibandComp",
        titleRes = R.string.section_multiband_compressor,
        descriptionRes = R.string.effect_desc_multiband_compressor,
        icon = Icons.Default.SettingsInputComponent,
        enabled = state.multibandCompressor.enable,
        onEnabledChange = viewModel::setMultibandCompressorEnabled,
        controller = controller,
        scope = scope,
        rows = multibandCompressorCardItems(
            state = state,
            viewModel = viewModel,
            selectedTab = controller.multibandSelectedTab.intValue,
            onSelectTab = { controller.multibandSelectedTab.intValue = it }
        )
    )
    effectSectionItems(
        sectionKey = "ddc",
        titleRes = R.string.section_ddc,
        descriptionRes = R.string.effect_desc_ddc,
        icon = Icons.Default.SpeakerPhone,
        enabled = state.ddc.enable,
        onEnabledChange = viewModel::setDdcEnabled,
        controller = controller,
        scope = scope,
        rows = ddcCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "vse",
        titleRes = R.string.section_spectrum_extension,
        descriptionRes = R.string.effect_desc_spectrum_extension,
        icon = Icons.Default.Waves,
        enabled = state.spectrumExtension.enable,
        onEnabledChange = viewModel::setSpectrumExtensionEnabled,
        controller = controller,
        scope = scope,
        rows = spectrumExtensionCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "eq",
        titleRes = R.string.section_equalizer,
        descriptionRes = R.string.effect_desc_equalizer,
        icon = Icons.Default.Equalizer,
        enabled = state.eq.enable,
        onEnabledChange = viewModel::setEqEnabled,
        controller = controller,
        scope = scope,
        rows = equalizerCardItems(state, viewModel, onOpenEqScreen)
    )
    effectSectionItems(
        sectionKey = "dynamicEq",
        titleRes = R.string.section_dynamic_eq,
        descriptionRes = R.string.effect_desc_dynamic_eq,
        icon = Icons.Default.Insights,
        enabled = state.dynamicEq.enable,
        onEnabledChange = viewModel::setDynamicEqEnabled,
        controller = controller,
        scope = scope,
        rows = dynamicEqCardItems(
            state = state,
            viewModel = viewModel,
            selectedTab = controller.dynamicEqSelectedTab.intValue,
            onSelectTab = { controller.dynamicEqSelectedTab.intValue = it }
        )
    )
    effectSectionItems(
        sectionKey = "convolver",
        titleRes = R.string.section_convolver,
        descriptionRes = R.string.effect_desc_convolver,
        icon = Icons.Default.BlurCircular,
        enabled = state.convolver.enable,
        onEnabledChange = viewModel::setConvolverEnabled,
        controller = controller,
        scope = scope,
        rows = convolverCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "fieldSurround",
        titleRes = R.string.section_field_surround,
        descriptionRes = R.string.effect_desc_field_surround,
        icon = Icons.Default.SurroundSound,
        enabled = state.fieldSurround.enable,
        onEnabledChange = viewModel::setFieldSurroundEnabled,
        controller = controller,
        scope = scope,
        rows = fieldSurroundCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "diffSurround",
        titleRes = R.string.section_diff_surround,
        descriptionRes = R.string.effect_desc_diff_surround,
        icon = Icons.Default.SpatialAudio,
        enabled = state.diffSurround.enable,
        onEnabledChange = viewModel::setDiffSurroundEnabled,
        controller = controller,
        scope = scope,
        rows = diffSurroundCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "stereoImager",
        titleRes = R.string.section_stereo_imager,
        descriptionRes = R.string.effect_desc_stereo_imager,
        icon = Icons.Default.AspectRatio,
        enabled = state.stereoImager.enable,
        onEnabledChange = viewModel::setStereoImagerEnabled,
        controller = controller,
        scope = scope,
        rows = stereoImagerCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "vhe",
        titleRes = R.string.section_headphone_surround,
        descriptionRes = R.string.effect_desc_headphone_surround,
        icon = Icons.Default.Headphones,
        enabled = state.headphoneSurround.enable,
        onEnabledChange = viewModel::setHeadphoneSurroundEnabled,
        controller = controller,
        scope = scope,
        rows = headphoneSurroundCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "reverb",
        titleRes = R.string.section_reverb,
        descriptionRes = R.string.effect_desc_reverb,
        icon = Icons.Default.BlurOn,
        enabled = state.reverb.enable,
        onEnabledChange = viewModel::setReverbEnabled,
        controller = controller,
        scope = scope,
        rows = reverberationCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "dynamicSystem",
        titleRes = R.string.section_dynamic_system,
        descriptionRes = R.string.effect_desc_dynamic_system,
        icon = Icons.Default.CandlestickChart,
        enabled = state.dynamicSystem.enable,
        onEnabledChange = viewModel::setDynamicSystemEnabled,
        controller = controller,
        scope = scope,
        rows = dynamicSystemCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "tubeSimulator",
        titleRes = R.string.section_tube_simulator,
        descriptionRes = R.string.effect_desc_tube_simulator,
        icon = Icons.Default.MusicNote,
        enabled = state.tubeSimulator.enable,
        onEnabledChange = viewModel::setTubeSimulatorEnabled,
        controller = controller,
        scope = scope,
        rows = tubeSimulatorCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "psychoBass",
        titleRes = R.string.section_psycho_bass,
        descriptionRes = R.string.effect_desc_psychoacoustic_bass,
        icon = Icons.Default.Psychology,
        enabled = state.psychoacousticBass.enable,
        onEnabledChange = viewModel::setPsychoacousticBassEnabled,
        controller = controller,
        scope = scope,
        rows = psychoacousticBassCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "viperBass",
        titleRes = R.string.section_viper_bass,
        descriptionRes = R.string.effect_desc_viper_bass,
        icon = Icons.Default.GraphicEq,
        enabled = state.bass.enable,
        onEnabledChange = viewModel::setBassEnabled,
        controller = controller,
        scope = scope,
        rows = viperBassCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "viperBassMono",
        titleRes = R.string.section_viper_bass_mono,
        descriptionRes = R.string.effect_desc_viper_bass_mono,
        icon = Icons.Default.GraphicEq,
        enabled = state.bassMono.enable,
        onEnabledChange = viewModel::setBassMonoEnabled,
        controller = controller,
        scope = scope,
        rows = viperBassMonoCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "viperClarity",
        titleRes = R.string.section_viper_clarity,
        descriptionRes = R.string.effect_desc_viper_clarity,
        icon = Icons.Default.Hearing,
        enabled = state.clarity.enable,
        onEnabledChange = viewModel::setClarityEnabled,
        controller = controller,
        scope = scope,
        rows = viperClarityCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "cure",
        titleRes = R.string.section_cure,
        descriptionRes = R.string.effect_desc_cure,
        icon = Icons.Default.HealthAndSafety,
        enabled = state.cure.enable,
        onEnabledChange = viewModel::setCureEnabled,
        controller = controller,
        scope = scope,
        rows = auditoryProtectionCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "analogX",
        titleRes = R.string.section_analogx,
        descriptionRes = R.string.effect_desc_analogx,
        icon = Icons.Default.Memory,
        enabled = state.analogX.enable,
        onEnabledChange = viewModel::setAnalogXEnabled,
        controller = controller,
        scope = scope,
        rows = analogXCardItems(state, viewModel)
    )
    effectSectionItems(
        sectionKey = "speakerOpt",
        titleRes = R.string.section_speaker_optimization,
        descriptionRes = R.string.effect_desc_speaker_correction,
        icon = Icons.Default.SpeakerPhone,
        enabled = state.speakerCorrection.enable,
        onEnabledChange = viewModel::setSpeakerCorrectionEnabled,
        controller = controller,
        scope = scope,
        rows = emptyList(),
        toggleOnly = true
    )
}
