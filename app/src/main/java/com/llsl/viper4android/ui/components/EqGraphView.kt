package com.llsl.viper4android.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.llsl.viper4android.R
import com.llsl.viper4android.data.model.EqPreset
import com.llsl.viper4android.effect.BuiltinPresets
import com.llsl.viper4android.ui.theme.StatusColors
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private const val DB_MIN = -12f
private const val DB_MAX = 12f
private val DB_GRID_LINES = listOf(-12f, -6f, 0f, 6f, 12f)

@Composable
fun EqCurveGraph(
    bands: List<Float>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bandCount: Int = 10,
) {
    val freqLabels = BuiltinPresets.eqGraphLabelsForCount(bandCount)
    val primary = MiuixTheme.colorScheme.primary
    val surfaceColor = MiuixTheme.colorScheme.secondaryContainer
    val onSurfaceVariant = MiuixTheme.colorScheme.onSurfaceVariantSummary
    val dividerColor = MiuixTheme.colorScheme.dividerLine
    val density = LocalDensity.current

    val graphModifier =
        if (modifier == Modifier) {
            Modifier
                .fillMaxWidth()
                .height(UiDimens.EqGraphHeight)
        } else {
            modifier.fillMaxWidth()
        }

    Box(
        modifier = graphModifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .squircleSurface(color = surfaceColor, cornerRadius = 16.dp)
            .clickable { onClick() },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val paddingLeft = UiDimens.ChipHeight.toPx()
            val paddingRight = UiDimens.Standard.toPx()
            val paddingTop = UiDimens.IconLarge.toPx()
            val paddingBottom = UiDimens.ChipHeight.toPx()

            val graphWidth = size.width - paddingLeft - paddingRight
            val graphHeight = size.height - paddingTop - paddingBottom

            val gridPaint =
                Paint().apply {
                    color = dividerColor.toArgb()
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                }

            val freqTextPaint =
                Paint().apply {
                    color = onSurfaceVariant.toArgb()
                    textSize =
                        with(density) {
                            (if (bandCount > 15) UiDimens.GraphLabelSmall else UiDimens.GraphLabel).toPx()
                        }
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = Typeface.DEFAULT
                }

            val dbLabelPaint =
                Paint().apply {
                    color = onSurfaceVariant.toArgb()
                    textSize = with(density) { UiDimens.Medium.toPx() }
                    textAlign = Paint.Align.RIGHT
                    isAntiAlias = true
                    typeface = Typeface.DEFAULT
                }

            val valuePaint =
                Paint().apply {
                    color = primary.toArgb()
                    textSize = with(density) { UiDimens.Medium.toPx() }
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = Typeface.DEFAULT_BOLD
                }

            for (db in DB_GRID_LINES) {
                val y = paddingTop + graphHeight * (1f - (db - DB_MIN) / (DB_MAX - DB_MIN))
                drawContext.canvas.nativeCanvas.drawLine(
                    paddingLeft,
                    y,
                    size.width - paddingRight,
                    y,
                    gridPaint,
                )
                val label =
                    when {
                        db > 0 -> "+${db.toInt()}"
                        db == 0f -> "0"
                        else -> "${db.toInt()}"
                    }
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    paddingLeft - UiDimens.XSmall.toPx(),
                    y + with(density) { UiDimens.Mini.toPx() },
                    dbLabelPaint,
                )
            }

            if (bands.size < bandCount) return@Canvas

            val points =
                bands.take(bandCount).mapIndexed { i, db ->
                    val x = paddingLeft + graphWidth * i / (bandCount - 1).toFloat()
                    val y =
                        paddingTop + graphHeight * (
                            1f - (
                                db.coerceIn(
                                    DB_MIN,
                                    DB_MAX,
                                ) - DB_MIN
                            ) / (DB_MAX - DB_MIN)
                        )
                    Offset(x, y)
                }

            val curvePath = buildSplinePath(points)

            val fillPath =
                Path().apply {
                    addPath(curvePath)
                    lineTo(points.last().x, paddingTop + graphHeight)
                    lineTo(points.first().x, paddingTop + graphHeight)
                    close()
                }

            drawPath(
                path = fillPath,
                brush =
                    Brush.verticalGradient(
                        colors = listOf(primary.copy(alpha = 0.35f), Color.Transparent),
                        startY = paddingTop,
                        endY = paddingTop + graphHeight,
                    ),
            )

            drawPath(
                path = curvePath,
                color = primary,
                style = Stroke(width = UiDimens.Tiny.toPx()),
            )

            val labelStep =
                when (bandCount) {
                    31 -> 5
                    25 -> 4
                    15 -> 2
                    else -> 1
                }
            val showValues = bandCount <= 15

            points.forEachIndexed { i, pt ->
                drawCircle(
                    color = primary,
                    radius = (if (bandCount > 15) UiDimens.Tiny else UiDimens.Mini).toPx(),
                    center = pt,
                )

                if (i % labelStep == 0) {
                    drawContext.canvas.nativeCanvas.drawText(
                        freqLabels.getOrElse(i) { "" },
                        pt.x,
                        paddingTop + graphHeight + with(density) { UiDimens.XLarge.toPx() },
                        freqTextPaint,
                    )
                }

                if (showValues) {
                    val valText = "%.1f".format(bands[i])
                    drawContext.canvas.nativeCanvas.drawText(
                        valText,
                        pt.x,
                        pt.y - with(density) { UiDimens.Compact.toPx() },
                        valuePaint,
                    )
                }
            }
        }
    }
}

private fun buildSplinePath(points: List<Offset>): Path {
    val path = Path()
    val n = points.size
    if (n == 0) return path
    path.moveTo(points[0].x, points[0].y)
    if (n == 1) return path
    if (n == 2) {
        path.lineTo(points[1].x, points[1].y)
        return path
    }

    val tension = 0.3f
    val damping = 0.15f

    for (i in 0 until n - 1) {
        val prev = points[max(0, i - 1)]
        val curr = points[i]
        val next = points[i + 1]
        val afterNext = points[min(n - 1, i + 2)]

        var t1 = tension
        val isLocalMax = curr.y <= prev.y && curr.y <= next.y
        val isLocalMin = curr.y >= prev.y && curr.y >= next.y
        if (isLocalMax || isLocalMin) t1 = damping

        var t2 = tension
        val isNextLocalMax = next.y <= curr.y && next.y <= afterNext.y
        val isNextLocalMin = next.y >= curr.y && next.y >= afterNext.y
        if (isNextLocalMax || isNextLocalMin) t2 = damping

        val cp1x = curr.x + (next.x - prev.x) * t1
        val cp1y = curr.y + (next.y - prev.y) * t1
        val cp2x = next.x - (afterNext.x - curr.x) * t2
        val cp2y = next.y - (afterNext.y - curr.y) * t2

        path.cubicTo(cp1x, cp1y, cp2x, cp2y, next.x, next.y)
    }
    return path
}

@Composable
fun EqEditDialog(
    bands: List<Float>,
    onBandsChange: (List<Double>) -> Unit,
    presetId: Long?,
    presets: List<EqPreset>,
    onPresetSelect: (Long) -> Unit,
    onPresetAdd: (String) -> Unit,
    onPresetDelete: (Long) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    show: Boolean = true,
    bandCount: Int = 10,
) {
    val localBands =
        remember(bandCount) {
            mutableStateListOf<Float>().apply { addAll(bands.take(bandCount)) }
        }

    LaunchedEffect(bands) {
        val incoming = bands.take(bandCount)
        if (incoming != localBands.toList()) {
            localBands.clear()
            localBands.addAll(incoming)
        }
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    var deletePresetId by remember { mutableStateOf<Long?>(null) }

    WindowDialog(
        show = show,
        modifier = modifier,
        title = stringResource(R.string.section_equalizer),
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.heightIn(max = 500.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                EqCurveGraph(
                    bands = localBands.toList(),
                    onClick = {},
                    modifier = Modifier.height(UiDimens.EqEditGraphHeight),
                    bandCount = bandCount,
                )

                Spacer(modifier = Modifier.height(8.dp))

                val presetNames = presets.map { resolvePresetName(it) }
                val selectedPresetName =
                    presets.find { it.id == presetId }?.let { resolvePresetName(it) }
                        ?: stringResource(R.string.label_custom)

                LabeledDropdown(
                    label = stringResource(R.string.label_preset),
                    selectedValue = selectedPresetName,
                    options = presetNames,
                    onOptionSelected = { index, _ -> onPresetSelect(presets[index].id) },
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        text = stringResource(R.string.action_save),
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = stringResource(R.string.action_delete),
                        onClick = { deletePresetId = presetId },
                        enabled = presetId != null,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            color = StatusColors.danger.copy(alpha = 0.15f),
                            textColor = StatusColors.danger,
                        ),
                    )
                    TextButton(
                        text = stringResource(R.string.action_reset),
                        onClick = {
                            for (i in localBands.indices) {
                                localBands[i] = 0f
                            }
                            onReset()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val bandLabels = BuiltinPresets.eqBandLabelsForCount(bandCount)
                bandLabels.forEachIndexed { index, label ->
                    if (index < localBands.size) {
                        var showBandEdit by remember { mutableStateOf(false) }

                        val applyBandChange = { newVal: Float ->
                            localBands[index] = newVal.coerceIn(DB_MIN, DB_MAX)
                            val list =
                                localBands.map { String.format(Locale.US, "%.1f", it).toDouble() }
                            onBandsChange(list)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(UiDimens.ListRowHeight),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = label,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.width(UiDimens.SwitchSlotHeight),
                            )
                            Slider(
                                value = localBands[index],
                                onValueChange = { applyBandChange(it) },
                                valueRange = DB_MIN..DB_MAX,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${"%.1f".format(localBands[index])}dB",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.primary,
                                modifier = Modifier
                                    .width(UiDimens.ValueColumnWidth)
                                    .clickable { showBandEdit = true },
                                maxLines = 1,
                            )
                        }

                        if (showBandEdit) {
                            NumberInputDialog(
                                label = label,
                                edit = SliderEdit(
                                    displayValue = localBands[index].toDouble(),
                                    displayRange = DB_MIN.toDouble()..DB_MAX.toDouble(),
                                    decimals = 1,
                                    unit = "dB",
                                    defaultValue = 0.0,
                                    onCommit = { applyBandChange(it.toFloat()) },
                                ),
                                onDismiss = { showBandEdit = false },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                text = stringResource(R.string.common_confirm),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
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
            placeholder = stringResource(R.string.preset_name_hint),
        )
    }

    deletePresetId?.let { targetId ->
        val targetName =
            presets.find { it.id == targetId }?.let { resolvePresetName(it) }
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
            onDismiss = { deletePresetId = null },
        )
    }
}
