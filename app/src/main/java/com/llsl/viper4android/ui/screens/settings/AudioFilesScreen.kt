package com.llsl.viper4android.ui.screens.settings

import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.ui.platform.LocalContext
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
import com.llsl.viper4android.ui.components.AdaptiveTopAppBar
import com.llsl.viper4android.ui.components.BlurredBar
import com.llsl.viper4android.ui.components.ConfirmDialog
import com.llsl.viper4android.ui.components.InputDialog
import com.llsl.viper4android.ui.components.UiDimens
import com.llsl.viper4android.ui.components.layerBackdrop
import com.llsl.viper4android.ui.components.rememberBlurBackdrop
import com.llsl.viper4android.ui.screens.main.ManagedAudioFile
import com.llsl.viper4android.ui.theme.ViperTheme
import com.llsl.viper4android.ui.util.horizontalCutoutPadding
import com.llsl.viper4android.ui.util.rememberPlatformToast
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
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
import top.yukonga.miuix.kmp.icon.extended.Rename
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val StringSetSaver = listSaver<Set<String>, String>(
    save = { it.toList() },
    restore = { it.toSet() },
)

/**
 * 已导入音频资源（内核 / VDC）管理二级页面，对齐 PresetsScreen 与 MIUIX 规范：
 * 单击直接选用、长按或点击右上角多选按钮进入选择模式并通过底部 FloatingToolbar 执行重命名 / 删除。
 */
@Composable
fun AudioFilesScreen(
    title: String,
    importActionLabel: String,
    emptyTitle: String,
    emptyDescription: String,
    itemIcon: ImageVector,
    files: List<ManagedAudioFile>,
    activeFileName: String,
    onImportFiles: (uris: List<Uri>, notificationTitle: String, successStr: String, onDone: (Boolean) -> Unit) -> Unit,
    onSelectFile: (String) -> Unit,
    onRenameFile: (String, String) -> Boolean,
    onDeleteFile: (String) -> Boolean,
    onDeleteFiles: (Set<String>) -> Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val platform = rememberPlatformToast()
    val importSuccessStr = stringResource(R.string.import_success)
    val importFailedStr = stringResource(R.string.import_failed)

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        onImportFiles(
            uris,
            importActionLabel,
            importSuccessStr,
        ) { ok ->
            platform.showToast(if (ok) importSuccessStr else importFailedStr)
        }
    }

    AudioFilesScreenContent(
        title = title,
        importActionLabel = importActionLabel,
        emptyTitle = emptyTitle,
        emptyDescription = emptyDescription,
        itemIcon = itemIcon,
        files = files,
        activeFileName = activeFileName,
        onImportClick = { importLauncher.launch(arrayOf("*/*")) },
        onSelectFile = onSelectFile,
        onRenameFile = onRenameFile,
        onDeleteFile = onDeleteFile,
        onDeleteFiles = onDeleteFiles,
        onShowToast = { message -> platform.showToast(message) },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun AudioFilesScreenContent(
    title: String,
    importActionLabel: String,
    emptyTitle: String,
    emptyDescription: String,
    itemIcon: ImageVector,
    files: List<ManagedAudioFile>,
    activeFileName: String,
    onImportClick: () -> Unit,
    onSelectFile: (String) -> Unit,
    onRenameFile: (String, String) -> Boolean,
    onDeleteFile: (String) -> Boolean,
    onDeleteFiles: (Set<String>) -> Int,
    onShowToast: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialSelectionMode: Boolean = false,
    initialSelectedNames: Set<String> = emptySet(),
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    val renamedToastFmt = stringResource(R.string.files_renamed_toast)
    val renameFailedStr = stringResource(R.string.files_rename_failed)
    val deletedToastFmt = stringResource(R.string.files_deleted_toast)
    val deletedCountToastFmt = stringResource(R.string.files_deleted_count_toast)

    var isSelectionMode by rememberSaveable { mutableStateOf(initialSelectionMode) }
    var selectedNames by rememberSaveable(stateSaver = StringSetSaver) {
        mutableStateOf(initialSelectedNames)
    }

    var showBatchDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var renameTargetName by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteTargetName by rememberSaveable { mutableStateOf<String?>(null) }

    // 文件列表变动时同步裁剪已选集合，空列表时自动退出多选模式
    LaunchedEffect(files) {
        if (files.isEmpty()) {
            isSelectionMode = false
            selectedNames = emptySet()
        } else if (selectedNames.isNotEmpty()) {
            val existing = files.mapTo(HashSet(files.size)) { it.name }
            val pruned = selectedNames.intersect(existing)
            if (pruned.size != selectedNames.size) {
                selectedNames = pruned
            }
        }
    }

    // 多选模式下按返回键优先退出多选模式
    val selectionBackState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = selectionBackState,
        isBackEnabled = isSelectionMode,
        onBackCompleted = {
            isSelectionMode = false
            selectedNames = emptySet()
        },
    )

    val allSelected = files.isNotEmpty() && selectedNames.size == files.size
    val renameTarget = renameTargetName?.let { name -> files.find { it.name == name } }
    val deleteTarget = deleteTargetName?.let { name -> files.find { it.name == name } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = if (isSelectionMode) {
                        stringResource(R.string.files_selected_count, selectedNames.size)
                    } else {
                        title
                    },
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    navigationIcon = {
                        if (isSelectionMode) {
                            IconButton(
                                onClick = {
                                    isSelectionMode = false
                                    selectedNames = emptySet()
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
                                    selectedNames = if (allSelected) {
                                        emptySet()
                                    } else {
                                        files.mapTo(LinkedHashSet(files.size)) { it.name }
                                    }
                                },
                                enabled = files.isNotEmpty(),
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
                                    selectedNames = emptySet()
                                },
                                enabled = files.isNotEmpty(),
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.SelectAll,
                                    contentDescription = stringResource(R.string.files_multi_select),
                                    tint = if (files.isNotEmpty()) {
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
                    onClick = onImportClick,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = importActionLabel,
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
                val canRename = selectedNames.size == 1
                val canDelete = selectedNames.isNotEmpty()
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
                                selectedNames.singleOrNull()?.let { renameTargetName = it }
                            },
                            enabled = canRename,
                            holdDownState = renameTarget != null,
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Rename,
                                contentDescription = stringResource(R.string.action_rename),
                                tint = if (canRename) {
                                    MiuixTheme.colorScheme.onSurface
                                } else {
                                    MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                            )
                        }
                        IconButton(
                            onClick = {
                                if (selectedNames.size == 1) {
                                    deleteTargetName = selectedNames.first()
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
            item(key = "files_top_spacer") {
                Spacer(Modifier.height(12.dp))
            }

            if (files.isEmpty()) {
                item(key = "files_empty_card") {
                    AudioFilesEmptyStateCard(
                        icon = itemIcon,
                        title = emptyTitle,
                        description = emptyDescription,
                        importLabel = importActionLabel,
                        onImport = onImportClick,
                    )
                }
            } else {
                items(
                    items = files,
                    key = { "file_${it.name}" },
                ) { file ->
                    val sizeText = remember(file.sizeBytes, context) {
                        Formatter.formatShortFileSize(context, file.sizeBytes)
                    }
                    val timeText = remember(file.lastModified) {
                        dateFormatter.format(Date(file.lastModified))
                    }
                    val isActive = file.name == activeFileName
                    val isSelected = file.name in selectedNames

                    AudioFileRow(
                        fileName = file.name,
                        summary = "$sizeText · $timeText",
                        itemIcon = itemIcon,
                        isActive = isActive,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                selectedNames = if (isSelected) {
                                    selectedNames - file.name
                                } else {
                                    selectedNames + file.name
                                }
                            } else {
                                onSelectFile(file.name)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isSelectionMode = true
                                selectedNames = setOf(file.name)
                            }
                        },
                    )
                }
            }

            item(key = "files_bottom_spacer") {
                Spacer(
                    Modifier
                        .height(UiDimens.FabListPadding)
                        .navigationBarsPadding(),
                )
            }
        }
    }

    renameTarget?.let { target ->
        InputDialog(
            title = stringResource(R.string.files_rename_title),
            initialValue = target.name,
            placeholder = stringResource(R.string.files_rename_hint),
            confirmLabel = stringResource(R.string.action_rename),
            onConfirm = { newName ->
                val trimmed = newName.trim()
                val ok = onRenameFile(target.name, trimmed)
                renameTargetName = null
                if (ok) {
                    isSelectionMode = false
                    selectedNames = emptySet()
                }
                onShowToast(
                    if (ok) {
                        String.format(Locale.getDefault(), renamedToastFmt, trimmed)
                    } else {
                        renameFailedStr
                    },
                )
            },
            onDismiss = { renameTargetName = null },
        )
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = stringResource(R.string.delete_file_title),
            body = stringResource(R.string.delete_file_message, target.name),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                val ok = onDeleteFile(target.name)
                deleteTargetName = null
                if (ok) {
                    isSelectionMode = false
                    selectedNames = emptySet()
                    onShowToast(String.format(Locale.getDefault(), deletedToastFmt, target.name))
                }
            },
            onDismiss = { deleteTargetName = null },
        )
    }

    if (showBatchDeleteConfirm && selectedNames.isNotEmpty()) {
        val count = selectedNames.size
        ConfirmDialog(
            title = stringResource(R.string.files_delete_selected_title),
            body = stringResource(R.string.files_delete_selected_confirm, count),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                val targets = selectedNames
                showBatchDeleteConfirm = false
                val deleted = onDeleteFiles(targets)
                isSelectionMode = false
                selectedNames = emptySet()
                onShowToast(String.format(Locale.getDefault(), deletedCountToastFmt, deleted))
            },
            onDismiss = { showBatchDeleteConfirm = false },
        )
    }
}

@Composable
private fun AudioFileRow(
    fileName: String,
    summary: String,
    itemIcon: ImageVector,
    isActive: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    // 非选择模式下当前生效项高亮为主题蓝；选择模式下卡片背景保持默认白色
    val isHighlighted = !isSelectionMode && isActive
    val cardContainerColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MiuixTheme.colorScheme.primary
        } else {
            MiuixTheme.colorScheme.surfaceContainer
        },
        label = "AudioFileCardContainerColor",
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
            title = fileName,
            titleColor = titleColor,
            summary = summary,
            summaryColor = summaryColor,
            interactionSource = interactionSource,
            startAction = {
                Icon(
                    imageVector = itemIcon,
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
private fun AudioFilesEmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    importLabel: String,
    onImport: () -> Unit,
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
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            TextButton(
                text = importLabel,
                onClick = onImport,
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

@Preview(name = "Kernel Files Populated", showBackground = true)
@Composable
private fun KernelFilesPopulatedPreview() {
    ViperTheme {
        val now = 1735689600000L
        AudioFilesScreenContent(
            title = "Kernel Files",
            importActionLabel = "Import Kernel",
            emptyTitle = "No kernel files imported",
            emptyDescription = "Import .wav or .irs impulse response files for Convolver",
            itemIcon = Icons.Rounded.GraphicEq,
            files = listOf(
                ManagedAudioFile(name = "Dolby_Atmos.irs", sizeBytes = 142_336L, lastModified = now),
                ManagedAudioFile(name = "Studio_Hall.wav", sizeBytes = 524_288L, lastModified = now + 3_600_000L),
            ),
            activeFileName = "Dolby_Atmos.irs",
            onImportClick = {},
            onSelectFile = {},
            onRenameFile = { _, _ -> true },
            onDeleteFile = { true },
            onDeleteFiles = { it.size },
            onShowToast = {},
            onBack = {},
        )
    }
}

@Preview(name = "Kernel Files Multi-Select", showBackground = true)
@Composable
private fun KernelFilesSelectionPreview() {
    ViperTheme {
        val now = 1735689600000L
        AudioFilesScreenContent(
            title = "Kernel Files",
            importActionLabel = "Import Kernel",
            emptyTitle = "No kernel files imported",
            emptyDescription = "Import .wav or .irs impulse response files for Convolver",
            itemIcon = Icons.Rounded.GraphicEq,
            files = listOf(
                ManagedAudioFile(name = "Dolby_Atmos.irs", sizeBytes = 142_336L, lastModified = now),
                ManagedAudioFile(name = "Studio_Hall.wav", sizeBytes = 524_288L, lastModified = now + 3_600_000L),
                ManagedAudioFile(name = "Cinema_Live.irs", sizeBytes = 318_464L, lastModified = now + 7_200_000L),
            ),
            activeFileName = "Dolby_Atmos.irs",
            onImportClick = {},
            onSelectFile = {},
            onRenameFile = { _, _ -> true },
            onDeleteFile = { true },
            onDeleteFiles = { it.size },
            onShowToast = {},
            onBack = {},
            initialSelectionMode = true,
            initialSelectedNames = setOf("Dolby_Atmos.irs"),
        )
    }
}

@Preview(name = "VDC Files Empty", showBackground = true)
@Composable
private fun VdcFilesEmptyPreview() {
    ViperTheme {
        AudioFilesScreenContent(
            title = "VDC Files",
            importActionLabel = "Import VDC",
            emptyTitle = "No VDC files imported",
            emptyDescription = "Import .vdc calibration files for ViPER DDC",
            itemIcon = Icons.Rounded.Tune,
            files = emptyList(),
            activeFileName = "",
            onImportClick = {},
            onSelectFile = {},
            onRenameFile = { _, _ -> true },
            onDeleteFile = { true },
            onDeleteFiles = { 0 },
            onShowToast = {},
            onBack = {},
        )
    }
}
