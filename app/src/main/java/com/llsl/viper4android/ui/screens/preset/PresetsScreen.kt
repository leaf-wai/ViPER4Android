package com.llsl.viper4android.ui.screens.preset

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.llsl.viper4android.data.model.Preset
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
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.FloatingToolbar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.ToolbarPosition
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Rename
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LongSetSaver = listSaver<Set<Long>, Long>(
    save = { it.toList() },
    restore = { it.toSet() },
)

/** 过滤预设名中的路径与非法文件名字符，确保 SAF 导出的默认文件名合法 */
internal fun sanitizePresetFileName(name: String): String {
    val cleaned = name.trim().replace(Regex("""[\\/:*?"<>|]"""), "_")
    val base = cleaned.ifBlank { "preset" }
    return if (base.endsWith(".json", ignoreCase = true)) base else "$base.json"
}

/**
 * 预设管理二级页面，由音效 Tab 左上角入口进入，交互与视觉对齐 DeviceScreen 与 MIUIX 规范：
 * 常规模式下单击卡片加载预设、右下角 FloatingActionButton 保存当前预设、顶栏提供导入与多选入口；
 * 长按卡片或点击多选按钮进入选择模式并弹出底部 FloatingToolbar 执行加载 / 更新 / 重命名 / 导出 / 删除。
 */
@Composable
fun PresetsScreen(
    presets: List<Preset>,
    onSavePreset: (String) -> Unit,
    onLoadPreset: (Long) -> Unit,
    onDeletePreset: (Long) -> Unit,
    onDeletePresets: (Set<Long>) -> Unit,
    onRenamePreset: (Long, String) -> Unit,
    onUpdatePreset: (Long) -> Unit,
    onImportPresetFiles: (uris: List<Uri>, notificationTitle: String, successStr: String, onDone: (Boolean) -> Unit) -> Unit,
    onExportPreset: (id: Long, targetUri: Uri, onDone: (Boolean) -> Unit) -> Unit,
    onExportPresets: (ids: Set<Long>, treeUri: Uri, onDone: (Int) -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val platform = rememberPlatformToast()
    val importPresetTitle = stringResource(R.string.settings_import_preset)
    val importSuccessStr = stringResource(R.string.import_success)
    val importFailedStr = stringResource(R.string.import_failed)
    val exportedToastFmt = stringResource(R.string.preset_exported_toast)
    val exportedCountToastFmt = stringResource(R.string.preset_exported_count_toast)
    val exportFailedStr = stringResource(R.string.preset_export_failed_toast)

    var pendingExportId by rememberSaveable { mutableLongStateOf(-1L) }
    var pendingExportName by rememberSaveable { mutableStateOf("") }
    var pendingBatchExportIds by rememberSaveable(stateSaver = LongSetSaver) {
        mutableStateOf(emptySet())
    }
    var exportSuccessTick by rememberSaveable { mutableIntStateOf(0) }

    val importPresetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        onImportPresetFiles(
            uris,
            importPresetTitle,
            importSuccessStr,
        ) { ok ->
            platform.showToast(if (ok) importSuccessStr else importFailedStr)
        }
    }

    val exportSingleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val targetId = pendingExportId
        val targetName = pendingExportName
        pendingExportId = -1L
        pendingExportName = ""
        if (uri == null || targetId < 0L) return@rememberLauncherForActivityResult
        onExportPreset(targetId, uri) { ok ->
            if (ok) {
                exportSuccessTick++
                platform.showToast(String.format(Locale.getDefault(), exportedToastFmt, targetName))
            } else {
                platform.showToast(exportFailedStr)
            }
        }
    }

    val exportBatchLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        val targets = pendingBatchExportIds
        pendingBatchExportIds = emptySet()
        if (treeUri == null || targets.isEmpty()) return@rememberLauncherForActivityResult
        onExportPresets(targets, treeUri) { exportedCount ->
            if (exportedCount > 0) {
                exportSuccessTick++
                platform.showToast(String.format(Locale.getDefault(), exportedCountToastFmt, exportedCount))
            } else {
                platform.showToast(exportFailedStr)
            }
        }
    }

    PresetsScreenContent(
        presets = presets,
        onSavePreset = onSavePreset,
        onLoadPreset = onLoadPreset,
        onDeletePreset = onDeletePreset,
        onDeletePresets = onDeletePresets,
        onRenamePreset = onRenamePreset,
        onUpdatePreset = onUpdatePreset,
        onImportClick = { importPresetLauncher.launch(arrayOf("*/*")) },
        onExportSelected = { targets ->
            if (targets.size == 1) {
                val target = targets.first()
                pendingExportId = target.id
                pendingExportName = target.name
                exportSingleLauncher.launch(sanitizePresetFileName(target.name))
            } else if (targets.size > 1) {
                pendingBatchExportIds = targets.mapTo(LinkedHashSet(targets.size)) { it.id }
                exportBatchLauncher.launch(null)
            }
        },
        exportSuccessTick = exportSuccessTick,
        onShowToast = { message -> platform.showToast(message) },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun PresetsScreenContent(
    presets: List<Preset>,
    onSavePreset: (String) -> Unit,
    onLoadPreset: (Long) -> Unit,
    onDeletePreset: (Long) -> Unit,
    onDeletePresets: (Set<Long>) -> Unit,
    onRenamePreset: (Long, String) -> Unit,
    onUpdatePreset: (Long) -> Unit,
    onImportClick: () -> Unit,
    onExportSelected: (List<Preset>) -> Unit,
    onShowToast: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    exportSuccessTick: Int = 0,
    initialSelectionMode: Boolean = false,
    initialSelectedIds: Set<Long> = emptySet(),
) {
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    val savedToastFmt = stringResource(R.string.preset_saved_toast)
    val loadedToastFmt = stringResource(R.string.preset_loaded_toast)
    val updatedToastFmt = stringResource(R.string.preset_updated_toast)
    val renamedToastFmt = stringResource(R.string.preset_renamed_toast)
    val deletedToastFmt = stringResource(R.string.preset_deleted_toast)
    val deletedCountToastFmt = stringResource(R.string.preset_deleted_count_toast)
    val updatedLabel = stringResource(R.string.preset_label_updated)

    var isSelectionMode by rememberSaveable { mutableStateOf(initialSelectionMode) }
    var selectedIds by rememberSaveable(stateSaver = LongSetSaver) {
        mutableStateOf(initialSelectedIds)
    }

    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var loadTargetId by rememberSaveable { mutableLongStateOf(-1L) }
    var updateTargetId by rememberSaveable { mutableLongStateOf(-1L) }
    var renameTargetId by rememberSaveable { mutableLongStateOf(-1L) }
    var deleteTargetId by rememberSaveable { mutableLongStateOf(-1L) }
    var showBatchDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    // 预设列表变动时同步裁剪已选集合，空列表时自动退出选择模式
    LaunchedEffect(presets) {
        if (presets.isEmpty()) {
            isSelectionMode = false
            selectedIds = emptySet()
        } else if (selectedIds.isNotEmpty()) {
            val existing = presets.mapTo(HashSet(presets.size)) { it.id }
            val pruned = selectedIds.intersect(existing)
            if (pruned.size != selectedIds.size) {
                selectedIds = pruned
            }
        }
    }

    // 导出成功后自动退出选择模式
    LaunchedEffect(exportSuccessTick) {
        if (exportSuccessTick > 0) {
            isSelectionMode = false
            selectedIds = emptySet()
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

    val allSelected = presets.isNotEmpty() && selectedIds.size == presets.size
    val loadTarget = presets.find { it.id == loadTargetId }
    val updateTarget = presets.find { it.id == updateTargetId }
    val renameTarget = presets.find { it.id == renameTargetId }
    val deleteTarget = presets.find { it.id == deleteTargetId }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = if (isSelectionMode) {
                        stringResource(R.string.files_selected_count, selectedIds.size)
                    } else {
                        stringResource(R.string.menu_presets)
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
                                        presets.mapTo(LinkedHashSet(presets.size)) { it.id }
                                    }
                                },
                                enabled = presets.isNotEmpty(),
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
                            IconButton(onClick = onImportClick) {
                                Icon(
                                    imageVector = MiuixIcons.Download,
                                    contentDescription = stringResource(R.string.settings_import_preset),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                )
                            }
                            IconButton(
                                onClick = {
                                    isSelectionMode = true
                                    selectedIds = emptySet()
                                },
                                enabled = presets.isNotEmpty(),
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.SelectAll,
                                    contentDescription = stringResource(R.string.files_multi_select),
                                    tint = if (presets.isNotEmpty()) {
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
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
            ) {
                FloatingActionButton(
                    modifier = Modifier
                        .border(0.05.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape),
                    shadowElevation = 0.dp,
                    onClick = { showSaveDialog = true },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.preset_save_current),
                        modifier = Modifier.size(32.dp),
                        tint = MiuixTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
        floatingToolbar = {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
            ) {
                val hasSingleSelection = selectedIds.size == 1
                val hasSelection = selectedIds.isNotEmpty()
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
                                selectedIds.singleOrNull()?.let { renameTargetId = it }
                            },
                            enabled = hasSingleSelection,
                            holdDownState = renameTarget != null,
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
                                val targets = presets.filter { it.id in selectedIds }
                                if (targets.isNotEmpty()) {
                                    onExportSelected(targets)
                                }
                            },
                            enabled = hasSelection,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Share,
                                contentDescription = stringResource(R.string.action_export),
                                tint = if (hasSelection) {
                                    MiuixTheme.colorScheme.onSurface
                                } else {
                                    MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                            )
                        }
                        IconButton(
                            onClick = {
                                if (selectedIds.size == 1) {
                                    deleteTargetId = selectedIds.first()
                                } else {
                                    showBatchDeleteConfirm = true
                                }
                            },
                            enabled = hasSelection,
                            holdDownState = deleteTarget != null || showBatchDeleteConfirm,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = if (hasSelection) {
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
            item(key = "presets_top_spacer") {
                Spacer(Modifier.height(12.dp))
            }

            if (presets.isEmpty()) {
                item(key = "preset_empty_card") {
                    PresetEmptyStateCard(
                        onSaveCurrent = { showSaveDialog = true },
                        onImportPreset = onImportClick,
                    )
                }
            } else {
                items(
                    items = presets,
                    key = { "preset_${it.id}" },
                ) { preset ->
                    val isSelected = preset.id in selectedIds
                    val updatedText = remember(preset.updatedAt, updatedLabel) {
                        "$updatedLabel · ${dateFormatter.format(Date(preset.updatedAt))}"
                    }

                    PresetRow(
                        presetName = preset.name,
                        summary = updatedText,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                selectedIds = if (isSelected) {
                                    selectedIds - preset.id
                                } else {
                                    selectedIds + preset.id
                                }
                            } else {
                                loadTargetId = preset.id
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isSelectionMode = true
                                selectedIds = setOf(preset.id)
                            }
                        },
                    )
                }
            }

            item(key = "presets_bottom_spacer") {
                Spacer(
                    Modifier
                        .height(UiDimens.FabListPadding)
                        .navigationBarsPadding(),
                )
            }
        }
    }

    if (showSaveDialog) {
        InputDialog(
            title = stringResource(R.string.preset_save_title),
            initialValue = "",
            placeholder = stringResource(R.string.preset_name_hint),
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = { name ->
                val trimmed = name.trim()
                onSavePreset(trimmed)
                showSaveDialog = false
                onShowToast(String.format(Locale.getDefault(), savedToastFmt, trimmed))
            },
            onDismiss = { showSaveDialog = false },
        )
    }

    loadTarget?.let { target ->
        ConfirmDialog(
            title = stringResource(R.string.preset_load_title),
            body = stringResource(R.string.preset_load_confirm, target.name),
            confirmLabel = stringResource(R.string.action_load),
            onConfirm = {
                onLoadPreset(target.id)
                loadTargetId = -1L
                isSelectionMode = false
                selectedIds = emptySet()
                onShowToast(String.format(Locale.getDefault(), loadedToastFmt, target.name))
            },
            onDismiss = { loadTargetId = -1L },
        )
    }

    updateTarget?.let { target ->
        ConfirmDialog(
            title = stringResource(R.string.preset_update_title),
            body = stringResource(R.string.preset_update_confirm, target.name),
            confirmLabel = stringResource(R.string.action_update),
            onConfirm = {
                onUpdatePreset(target.id)
                updateTargetId = -1L
                isSelectionMode = false
                selectedIds = emptySet()
                onShowToast(String.format(Locale.getDefault(), updatedToastFmt, target.name))
            },
            onDismiss = { updateTargetId = -1L },
        )
    }

    renameTarget?.let { target ->
        InputDialog(
            title = stringResource(R.string.preset_rename_title),
            initialValue = target.name,
            placeholder = stringResource(R.string.preset_name_hint),
            confirmLabel = stringResource(R.string.action_rename),
            onConfirm = { newName ->
                val trimmed = newName.trim()
                onRenamePreset(target.id, trimmed)
                renameTargetId = -1L
                isSelectionMode = false
                selectedIds = emptySet()
                onShowToast(String.format(Locale.getDefault(), renamedToastFmt, trimmed))
            },
            onDismiss = { renameTargetId = -1L },
        )
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = stringResource(R.string.preset_delete_title),
            body = stringResource(R.string.preset_delete_confirm, target.name),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                onDeletePreset(target.id)
                deleteTargetId = -1L
                isSelectionMode = false
                selectedIds = emptySet()
                onShowToast(String.format(Locale.getDefault(), deletedToastFmt, target.name))
            },
            onDismiss = { deleteTargetId = -1L },
        )
    }

    if (showBatchDeleteConfirm && selectedIds.isNotEmpty()) {
        val targets = selectedIds.toSet()
        ConfirmDialog(
            title = stringResource(R.string.preset_delete_selected_title),
            body = stringResource(R.string.preset_delete_selected_confirm, targets.size),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                showBatchDeleteConfirm = false
                onDeletePresets(targets)
                isSelectionMode = false
                selectedIds = emptySet()
                onShowToast(String.format(Locale.getDefault(), deletedCountToastFmt, targets.size))
            },
            onDismiss = { showBatchDeleteConfirm = false },
        )
    }
}

@Composable
private fun PresetRow(
    presetName: String,
    summary: String,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        BasicComponent(
            modifier = Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = if (isSelectionMode) null else onLongClick,
            ),
            title = presetName,
            summary = summary,
            interactionSource = interactionSource,
            startAction = {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                    tint = MiuixTheme.colorScheme.onBackground,
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
private fun PresetEmptyStateCard(
    onSaveCurrent: () -> Unit,
    onImportPreset: () -> Unit,
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
                    imageVector = Icons.Rounded.Bookmarks,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.preset_empty),
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.preset_empty_desc),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = stringResource(R.string.settings_import_preset),
                    onClick = onImportPreset,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = stringResource(R.string.preset_save_current),
                    onClick = onSaveCurrent,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

@Preview(name = "Presets Populated", showBackground = true)
@Composable
private fun PresetsScreenPopulatedPreview() {
    ViperTheme {
        val now = 1735689600000L
        PresetsScreenContent(
            presets = listOf(
                Preset(id = 1L, name = "Studio Reference", settingsJson = "{}", createdAt = now, updatedAt = now),
                Preset(id = 2L, name = "Deep Bass Boost", settingsJson = "{}", createdAt = now, updatedAt = now + 3_600_000L),
                Preset(id = 3L, name = "Vocal Clarity", settingsJson = "{}", createdAt = now, updatedAt = now + 7_200_000L),
            ),
            onSavePreset = {},
            onLoadPreset = {},
            onDeletePreset = {},
            onDeletePresets = {},
            onRenamePreset = { _, _ -> },
            onUpdatePreset = {},
            onImportClick = {},
            onExportSelected = {},
            onShowToast = {},
            onBack = {},
        )
    }
}

@Preview(name = "Presets Multi-Select", showBackground = true)
@Composable
private fun PresetsScreenSelectionPreview() {
    ViperTheme {
        val now = 1735689600000L
        PresetsScreenContent(
            presets = listOf(
                Preset(id = 1L, name = "Studio Reference", settingsJson = "{}", createdAt = now, updatedAt = now),
                Preset(id = 2L, name = "Deep Bass Boost", settingsJson = "{}", createdAt = now, updatedAt = now + 3_600_000L),
                Preset(id = 3L, name = "Vocal Clarity", settingsJson = "{}", createdAt = now, updatedAt = now + 7_200_000L),
            ),
            onSavePreset = {},
            onLoadPreset = {},
            onDeletePreset = {},
            onDeletePresets = {},
            onRenamePreset = { _, _ -> },
            onUpdatePreset = {},
            onImportClick = {},
            onExportSelected = {},
            onShowToast = {},
            onBack = {},
            initialSelectionMode = true,
            initialSelectedIds = setOf(1L),
        )
    }
}

@Preview(name = "Presets Empty", showBackground = true)
@Composable
private fun PresetsScreenEmptyPreview() {
    ViperTheme {
        PresetsScreenContent(
            presets = emptyList(),
            onSavePreset = {},
            onLoadPreset = {},
            onDeletePreset = {},
            onDeletePresets = {},
            onRenamePreset = { _, _ -> },
            onUpdatePreset = {},
            onImportClick = {},
            onExportSelected = {},
            onShowToast = {},
            onBack = {},
        )
    }
}
