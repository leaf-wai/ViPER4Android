package com.llsl.viper4android.ui.screens.device

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.llsl.viper4android.R
import com.llsl.viper4android.data.model.DeviceSettings
import com.llsl.viper4android.ui.components.AdaptiveTopAppBar
import com.llsl.viper4android.ui.components.BlurredBar
import com.llsl.viper4android.ui.components.ConfirmDialog
import com.llsl.viper4android.ui.components.InputDialog
import com.llsl.viper4android.ui.components.UiDimens
import com.llsl.viper4android.ui.components.layerBackdrop
import com.llsl.viper4android.ui.components.rememberBlurBackdrop
import com.llsl.viper4android.ui.theme.ViperTheme
import com.llsl.viper4android.ui.util.horizontalCutoutPadding
import com.llsl.viper4android.ui.util.rememberPlatformToast
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.FloatingToolbar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.ToolbarPosition
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Rename
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BUILTIN_DEVICE_IDS = setOf("speaker", "wired_headphone")

private val StringSetSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() },
)

/**
 * 音频设备管理二级页面，交互与视觉对齐 AudioFilesScreen 与 MIUIX 规范：
 * 常规模式下当前活跃设备卡片高亮为主题蓝、单击加载设备预设；
 * 长按卡片或点击右上角多选按钮进入选择模式并通过底部 FloatingToolbar 执行加载 / 更新 / 重命名 / 删除。
 */
@Composable
fun DevicesScreen(
    devices: List<DeviceSettings>,
    activeDeviceId: String,
    onRename: (String, String) -> Unit,
    onLoad: (String) -> Unit,
    onUpdate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteDevices: (Set<String>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val platform = rememberPlatformToast()
    DevicesScreenContent(
        devices = devices,
        activeDeviceId = activeDeviceId,
        onRename = onRename,
        onLoad = onLoad,
        onUpdate = onUpdate,
        onDelete = onDelete,
        onDeleteDevices = onDeleteDevices,
        onShowToast = { message -> platform.showToast(message) },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun DevicesScreenContent(
    devices: List<DeviceSettings>,
    activeDeviceId: String,
    onRename: (String, String) -> Unit,
    onLoad: (String) -> Unit,
    onUpdate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteDevices: (Set<String>) -> Unit,
    onShowToast: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialSelectionMode: Boolean = false,
    initialSelectedIds: Set<String> = emptySet(),
) {
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    val activeStatusLabel = stringResource(R.string.status_active)
    val loadedToastFmt = stringResource(R.string.device_preset_loaded_toast)
    val updatedToastFmt = stringResource(R.string.device_preset_updated_toast)
    val renamedToastFmt = stringResource(R.string.device_renamed_toast)
    val deletedToastFmt = stringResource(R.string.device_deleted_toast)
    val deletedCountToastFmt = stringResource(R.string.device_deleted_count_toast)

    var isSelectionMode by rememberSaveable { mutableStateOf(initialSelectionMode) }
    var selectedIds by rememberSaveable(stateSaver = StringSetSaver) {
        mutableStateOf(initialSelectedIds)
    }

    var renamingDeviceId by rememberSaveable { mutableStateOf<String?>(null) }
    var loadTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var updateTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var showBatchDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    val sortedDevices = remember(devices, activeDeviceId) {
        devices.sortedWith(
            compareByDescending<DeviceSettings> { it.deviceId == activeDeviceId }
                .thenByDescending { it.lastConnected },
        )
    }

    // 设备列表变动时同步裁剪已选集合，空列表时自动退出选择模式
    LaunchedEffect(devices) {
        if (devices.isEmpty()) {
            isSelectionMode = false
            selectedIds = emptySet()
        } else if (selectedIds.isNotEmpty()) {
            val existing = devices.mapTo(HashSet(devices.size)) { it.deviceId }
            val pruned = selectedIds.intersect(existing)
            if (pruned.size != selectedIds.size) {
                selectedIds = pruned
            }
        }
    }

    // 选择模式下按返回键优先退出选择模式
    val selectionBackState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = selectionBackState,
        isBackEnabled = isSelectionMode,
        onBackCompleted = {
            isSelectionMode = false
            selectedIds = emptySet()
        },
    )

    val allSelected = devices.isNotEmpty() && selectedIds.size == devices.size
    val deletableSelectedIds = remember(selectedIds, activeDeviceId) {
        selectedIds.filterTo(LinkedHashSet()) { id ->
            id != activeDeviceId && id !in BUILTIN_DEVICE_IDS
        }
    }

    val renamingDevice = renamingDeviceId?.let { id -> devices.find { it.deviceId == id } }
    val loadTarget = loadTargetId?.let { id -> devices.find { it.deviceId == id } }
    val updateTarget = updateTargetId?.let { id -> devices.find { it.deviceId == id } }
    val deleteTarget = deleteTargetId?.let { id -> devices.find { it.deviceId == id } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = if (isSelectionMode) {
                        stringResource(R.string.files_selected_count, selectedIds.size)
                    } else {
                        stringResource(R.string.device_dialog_title)
                    },
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    navigationIcon = {
                        if (isSelectionMode) {
                            IconButton(
                                onClick = {
                                    isSelectionMode = false
                                    selectedIds = emptySet()
                                },
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Close,
                                    contentDescription = stringResource(R.string.common_cancel),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                )
                            }
                        } else {
                            IconButton(onClick = onBack) {
                                val layoutDirection = LocalLayoutDirection.current
                                Icon(
                                    modifier = Modifier.graphicsLayer {
                                        if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                                    },
                                    imageVector = MiuixIcons.Back,
                                    contentDescription = stringResource(R.string.action_close),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    },
                    actions = {
                        if (isSelectionMode) {
                            IconButton(
                                onClick = {
                                    selectedIds = if (allSelected) {
                                        emptySet()
                                    } else {
                                        sortedDevices.mapTo(LinkedHashSet(sortedDevices.size)) { it.deviceId }
                                    }
                                },
                                enabled = devices.isNotEmpty(),
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.SelectAll,
                                    contentDescription = stringResource(
                                        if (allSelected) R.string.files_deselect_all else R.string.files_select_all,
                                    ),
                                    tint = if (allSelected) {
                                        MiuixTheme.colorScheme.primary
                                    } else {
                                        MiuixTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    isSelectionMode = true
                                    selectedIds = emptySet()
                                },
                                enabled = devices.isNotEmpty(),
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.SelectAll,
                                    contentDescription = stringResource(R.string.files_multi_select),
                                    tint = if (devices.isNotEmpty()) {
                                        MiuixTheme.colorScheme.onSurface
                                    } else {
                                        MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    },
                                )
                            }
                        }
                    },
                )
            }
        },
        floatingToolbar = {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
            ) {
                val hasSingleSelection = selectedIds.size == 1
                val canDelete = deletableSelectedIds.isNotEmpty()
                FloatingToolbar(
                    showDivider = true,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                selectedIds.singleOrNull()?.let { loadTargetId = it }
                            },
                            enabled = hasSingleSelection,
                            holdDownState = loadTarget != null,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Download,
                                contentDescription = stringResource(R.string.action_load),
                                tint = if (hasSingleSelection) {
                                    MiuixTheme.colorScheme.onSurface
                                } else {
                                    MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                            )
                        }
                        IconButton(
                            onClick = {
                                selectedIds.singleOrNull()?.let { updateTargetId = it }
                            },
                            enabled = hasSingleSelection,
                            holdDownState = updateTarget != null,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Refresh,
                                contentDescription = stringResource(R.string.action_update),
                                tint = if (hasSingleSelection) {
                                    MiuixTheme.colorScheme.onSurface
                                } else {
                                    MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                            )
                        }
                        IconButton(
                            onClick = {
                                selectedIds.singleOrNull()?.let { renamingDeviceId = it }
                            },
                            enabled = hasSingleSelection,
                            holdDownState = renamingDevice != null,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Rename,
                                contentDescription = stringResource(R.string.action_rename),
                                tint = if (hasSingleSelection) {
                                    MiuixTheme.colorScheme.onSurface
                                } else {
                                    MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                            )
                        }
                        IconButton(
                            onClick = {
                                if (deletableSelectedIds.size == 1) {
                                    deleteTargetId = deletableSelectedIds.first()
                                } else {
                                    showBatchDeleteConfirm = true
                                }
                            },
                            enabled = canDelete,
                            holdDownState = deleteTarget != null || showBatchDeleteConfirm,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = if (canDelete) {
                                    MiuixTheme.colorScheme.onSurface
                                } else {
                                    MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                            )
                        }
                    }
                }
            }
        },
        floatingToolbarPosition = ToolbarPosition.BottomCenter,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .horizontalCutoutPadding()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
        ) {
            item(key = "devices_top_spacer") {
                Spacer(Modifier.height(12.dp))
            }

            if (sortedDevices.isEmpty()) {
                item(key = "devices_empty_card") {
                    DevicesEmptyStateCard()
                }
            } else {
                items(
                    items = sortedDevices,
                    key = { "device_${it.deviceId}" },
                ) { device ->
                    val isActive = device.deviceId == activeDeviceId
                    val isSelected = device.deviceId in selectedIds
                    val typeLabel = deviceTypeName(device)
                    val summary = if (isActive) {
                        "$typeLabel · $activeStatusLabel"
                    } else {
                        val timeText = remember(device.lastConnected) {
                            dateFormatter.format(Date(device.lastConnected))
                        }
                        "$typeLabel · $timeText"
                    }

                    DeviceRow(
                        deviceName = device.deviceName,
                        summary = summary,
                        icon = deviceIcon(device),
                        isActive = isActive,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                selectedIds = if (isSelected) {
                                    selectedIds - device.deviceId
                                } else {
                                    selectedIds + device.deviceId
                                }
                            } else {
                                loadTargetId = device.deviceId
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isSelectionMode = true
                                selectedIds = setOf(device.deviceId)
                            }
                        },
                    )
                }
            }

            item(key = "devices_bottom_spacer") {
                Spacer(
                    Modifier
                        .height(UiDimens.FabListPadding)
                        .navigationBarsPadding(),
                )
            }
        }
    }

    renamingDevice?.let { target ->
        InputDialog(
            title = stringResource(R.string.device_rename_title),
            initialValue = target.deviceName,
            placeholder = stringResource(R.string.device_rename_hint),
            confirmLabel = stringResource(R.string.action_rename),
            onConfirm = { newName ->
                val trimmed = newName.trim()
                onRename(target.deviceId, trimmed)
                renamingDeviceId = null
                isSelectionMode = false
                selectedIds = emptySet()
                onShowToast(String.format(Locale.getDefault(), renamedToastFmt, trimmed))
            },
            onDismiss = { renamingDeviceId = null },
        )
    }

    loadTarget?.let { target ->
        ConfirmDialog(
            title = stringResource(R.string.device_load_title),
            body = stringResource(R.string.device_load_confirm, target.deviceName),
            confirmLabel = stringResource(R.string.action_load),
            onConfirm = {
                onLoad(target.deviceId)
                loadTargetId = null
                isSelectionMode = false
                selectedIds = emptySet()
                onShowToast(String.format(Locale.getDefault(), loadedToastFmt, target.deviceName))
            },
            onDismiss = { loadTargetId = null },
        )
    }

    updateTarget?.let { target ->
        ConfirmDialog(
            title = stringResource(R.string.device_update_title),
            body = stringResource(R.string.device_update_confirm, target.deviceName),
            confirmLabel = stringResource(R.string.action_update),
            onConfirm = {
                onUpdate(target.deviceId)
                updateTargetId = null
                isSelectionMode = false
                selectedIds = emptySet()
                onShowToast(String.format(Locale.getDefault(), updatedToastFmt, target.deviceName))
            },
            onDismiss = { updateTargetId = null },
        )
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = stringResource(R.string.device_delete_title),
            body = stringResource(R.string.device_delete_confirm, target.deviceName),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                onDelete(target.deviceId)
                deleteTargetId = null
                isSelectionMode = false
                selectedIds = emptySet()
                onShowToast(String.format(Locale.getDefault(), deletedToastFmt, target.deviceName))
            },
            onDismiss = { deleteTargetId = null },
        )
    }

    if (showBatchDeleteConfirm && deletableSelectedIds.isNotEmpty()) {
        val targets = deletableSelectedIds.toSet()
        ConfirmDialog(
            title = stringResource(R.string.device_delete_selected_title),
            body = stringResource(R.string.device_delete_selected_confirm, targets.size),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                showBatchDeleteConfirm = false
                onDeleteDevices(targets)
                isSelectionMode = false
                selectedIds = emptySet()
                onShowToast(String.format(Locale.getDefault(), deletedCountToastFmt, targets.size))
            },
            onDismiss = { showBatchDeleteConfirm = false },
        )
    }
}

@Composable
private fun DeviceRow(
    deviceName: String,
    summary: String,
    icon: ImageVector,
    isActive: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    // 非选择模式下当前活跃设备高亮为主题蓝；选择模式下卡片背景保持默认白色
    val isHighlighted = !isSelectionMode && isActive
    val cardContainerColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MiuixTheme.colorScheme.primary
        } else {
            MiuixTheme.colorScheme.surfaceContainer
        },
        label = "DeviceCardContainerColor",
    )
    val cardContentColor = if (isHighlighted) {
        MiuixTheme.colorScheme.onPrimary
    } else {
        MiuixTheme.colorScheme.onSurfaceContainer
    }
    val titleColor = if (isHighlighted) {
        BasicComponentDefaults.titleColor(color = MiuixTheme.colorScheme.onPrimary)
    } else {
        BasicComponentDefaults.titleColor()
    }
    val summaryColor = if (isHighlighted) {
        BasicComponentDefaults.summaryColor(
            color = MiuixTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
        )
    } else {
        BasicComponentDefaults.summaryColor()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        colors = CardDefaults.defaultColors(
            color = cardContainerColor,
            contentColor = cardContentColor,
        ),
    ) {
        BasicComponent(
            modifier = Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = if (isSelectionMode) null else onLongClick,
            ),
            title = deviceName,
            titleColor = titleColor,
            summary = summary,
            summaryColor = summaryColor,
            interactionSource = interactionSource,
            startAction = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = if (isHighlighted) {
                        MiuixTheme.colorScheme.onPrimary
                    } else {
                        MiuixTheme.colorScheme.onBackground
                    },
                )
            },
            endActions = if (isSelectionMode) {
                {
                    Checkbox(
                        state = ToggleableState(isSelected),
                        onClick = null,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            } else {
                null
            },
        )
    }
}

@Composable
private fun DevicesEmptyStateCard(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .squircleBackground(MiuixTheme.colorScheme.secondaryContainer, 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Headphones,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.device_no_devices),
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.device_empty_desc),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun deviceIcon(device: DeviceSettings): ImageVector =
    if (device.isHeadphone) Icons.Rounded.Headphones else Icons.Rounded.Speaker

@Composable
private fun deviceTypeName(device: DeviceSettings): String =
    when {
        device.deviceId == "speaker" -> stringResource(R.string.device_type_speaker)
        device.deviceId == "wired_headphone" -> stringResource(R.string.device_type_wired)
        device.isHeadphone -> stringResource(R.string.device_type_bluetooth)
        else -> stringResource(R.string.device_type_speaker)
    }

@Preview(name = "Devices Populated", showBackground = true)
@Composable
private fun DevicesPopulatedPreview() {
    ViperTheme {
        val now = 1735689600000L
        DevicesScreenContent(
            devices = listOf(
                DeviceSettings(
                    deviceId = "AA:BB:CC:DD:EE:01",
                    deviceName = "WH-1000XM5",
                    isHeadphone = true,
                    settingsJson = "{}",
                    lastConnected = now + 7_200_000L,
                ),
                DeviceSettings(
                    deviceId = "speaker",
                    deviceName = "Speaker",
                    isHeadphone = false,
                    settingsJson = "{}",
                    lastConnected = now + 3_600_000L,
                ),
                DeviceSettings(
                    deviceId = "wired_headphone",
                    deviceName = "Wired Headphone",
                    isHeadphone = true,
                    settingsJson = "{}",
                    lastConnected = now,
                ),
            ),
            activeDeviceId = "AA:BB:CC:DD:EE:01",
            onRename = { _, _ -> },
            onLoad = {},
            onUpdate = {},
            onDelete = {},
            onDeleteDevices = {},
            onShowToast = {},
            onBack = {},
        )
    }
}

@Preview(name = "Devices Multi-Select", showBackground = true)
@Composable
private fun DevicesSelectionPreview() {
    ViperTheme {
        val now = 1735689600000L
        DevicesScreenContent(
            devices = listOf(
                DeviceSettings(
                    deviceId = "AA:BB:CC:DD:EE:01",
                    deviceName = "WH-1000XM5",
                    isHeadphone = true,
                    settingsJson = "{}",
                    lastConnected = now + 7_200_000L,
                ),
                DeviceSettings(
                    deviceId = "AA:BB:CC:DD:EE:02",
                    deviceName = "AirPods Pro",
                    isHeadphone = true,
                    settingsJson = "{}",
                    lastConnected = now + 3_600_000L,
                ),
                DeviceSettings(
                    deviceId = "speaker",
                    deviceName = "Speaker",
                    isHeadphone = false,
                    settingsJson = "{}",
                    lastConnected = now,
                ),
            ),
            activeDeviceId = "AA:BB:CC:DD:EE:01",
            onRename = { _, _ -> },
            onLoad = {},
            onUpdate = {},
            onDelete = {},
            onDeleteDevices = {},
            onShowToast = {},
            onBack = {},
            initialSelectionMode = true,
            initialSelectedIds = setOf("AA:BB:CC:DD:EE:02"),
        )
    }
}

@Preview(name = "Devices Empty", showBackground = true)
@Composable
private fun DevicesEmptyPreview() {
    ViperTheme {
        DevicesScreenContent(
            devices = emptyList(),
            activeDeviceId = "",
            onRename = { _, _ -> },
            onLoad = {},
            onUpdate = {},
            onDelete = {},
            onDeleteDevices = {},
            onShowToast = {},
            onBack = {},
        )
    }
}
