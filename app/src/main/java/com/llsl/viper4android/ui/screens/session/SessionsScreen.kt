package com.llsl.viper4android.ui.screens.session

import android.content.Context
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.llsl.viper4android.R
import com.llsl.viper4android.audio.AudioSessionInfo
import com.llsl.viper4android.ui.components.AdaptiveTopAppBar
import com.llsl.viper4android.ui.components.BlurredBar
import com.llsl.viper4android.ui.components.ConfirmDialog
import com.llsl.viper4android.ui.components.StatusBadge
import com.llsl.viper4android.ui.components.UiDimens
import com.llsl.viper4android.ui.components.layerBackdrop
import com.llsl.viper4android.ui.components.rememberBlurBackdrop
import com.llsl.viper4android.ui.theme.StatusColors
import com.llsl.viper4android.ui.theme.ViperTheme
import com.llsl.viper4android.ui.util.ImmutableList
import com.llsl.viper4android.ui.util.horizontalCutoutPadding
import com.llsl.viper4android.ui.util.rememberPlatformToast
import com.llsl.viper4android.ui.util.toPersistentList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.FloatingToolbar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.ToolbarPosition
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.SelectAll
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val SessionAppIconSize = 40.dp
private val SessionAppIconCornerRadius = 10.dp
private const val MIN_REFRESH_DISPLAY_MS = 400L
private val RefreshTimeout = 6.seconds

private val IntSetSaver = listSaver<Set<Int>, Int>(
    save = { it.toList() },
    restore = { it.toSet() },
)

private data class ResolvedAppMeta(
    val label: String?,
    val icon: ImageBitmap?,
)

/**
 * 会话模式下的音频会话管理二级页面，交互与视觉对齐 DevicesScreen / PresetsScreen / AudioFilesScreen：
 * 常规模式下展示每个活跃音频流所属应用图标、名称、包名、会话 ID 与音频类型，可通过右侧开关或单击卡片即时关闭/恢复单会话；
 * 长按卡片或点击右上角多选按钮进入选择模式并通过底部 FloatingToolbar 批量开启、批量关闭或将所属应用加入排除列表。
 */
@Composable
fun SessionsScreen(
    sessions: ImmutableList<AudioSessionInfo>,
    masterEnabled: Boolean,
    globalMode: Boolean,
    onToggleSession: (Int, Boolean) -> Unit,
    onSetSessionsEnabled: (Set<Int>, Boolean) -> Unit,
    onExcludeApps: (Set<String>) -> Unit,
    onRefresh: (onComplete: () -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val platform = rememberPlatformToast()
    LaunchedEffect(masterEnabled, globalMode) {
        if (!globalMode && masterEnabled) {
            onRefresh {}
        }
    }
    SessionsScreenContent(
        sessions = sessions,
        masterEnabled = masterEnabled,
        globalMode = globalMode,
        onToggleSession = onToggleSession,
        onSetSessionsEnabled = onSetSessionsEnabled,
        onExcludeApps = onExcludeApps,
        onRefresh = onRefresh,
        onShowToast = { message -> platform.showToast(message) },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun SessionsScreenContent(
    sessions: ImmutableList<AudioSessionInfo>,
    masterEnabled: Boolean,
    globalMode: Boolean,
    onToggleSession: (Int, Boolean) -> Unit,
    onSetSessionsEnabled: (Set<Int>, Boolean) -> Unit,
    onExcludeApps: (Set<String>) -> Unit,
    onRefresh: (onComplete: () -> Unit) -> Unit,
    onShowToast: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialSelectionMode: Boolean = false,
    initialSelectedIds: Set<Int> = emptySet(),
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    val enabledCountToastFmt = stringResource(R.string.sessions_enabled_count_toast)
    val disabledCountToastFmt = stringResource(R.string.sessions_disabled_count_toast)
    val excludedSingleToastFmt = stringResource(R.string.sessions_excluded_single_toast)
    val excludedMultiToastFmt = stringResource(R.string.sessions_excluded_multi_toast)

    var isSelectionMode by rememberSaveable { mutableStateOf(initialSelectionMode) }
    var selectedIds by rememberSaveable(stateSaver = IntSetSaver) {
        mutableStateOf(initialSelectedIds)
    }
    var showExcludeConfirm by rememberSaveable { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val triggerRefresh: () -> Unit = {
        if (!isRefreshing) {
            isRefreshing = true
            coroutineScope.launch {
                val startMs = SystemClock.elapsedRealtime()
                val done = CompletableDeferred<Unit>()
                onRefresh { done.complete(Unit) }
                // 兜底超时，防止底层异常时下拉刷新指示器永久悬停
                withTimeoutOrNull(RefreshTimeout) { done.await() }
                // 保证短暂最小展示时长，避免特权模式或空态下瞬时完成导致动画跳闪
                val remainingMs = MIN_REFRESH_DISPLAY_MS - (SystemClock.elapsedRealtime() - startMs)
                if (remainingMs > 0L) {
                    delay(remainingMs.milliseconds)
                }
                isRefreshing = false
            }
        }
    }

    val sortedSessions = remember(sessions) {
        sessions.sortedWith(
            compareByDescending<AudioSessionInfo> { it.enabled }
                .thenByDescending { it.openedAt }
                .thenBy { it.sessionId },
        )
    }

    // 会话列表变动时同步裁剪已选集合，空列表时自动退出选择模式
    LaunchedEffect(sessions) {
        if (sessions.isEmpty()) {
            isSelectionMode = false
            selectedIds = emptySet()
            showExcludeConfirm = false
        } else if (selectedIds.isNotEmpty()) {
            val existing = sessions.mapTo(HashSet(sessions.size)) { it.sessionId }
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

    val selectedSessions = remember(sortedSessions, selectedIds) {
        sortedSessions.filter { it.sessionId in selectedIds }
    }
    val allSelected = sortedSessions.isNotEmpty() && selectedIds.size == sortedSessions.size
    val enableTargets = remember(selectedSessions) {
        selectedSessions.filter { !it.enabled }.mapTo(LinkedHashSet()) { it.sessionId }
    }
    val disableTargets = remember(selectedSessions) {
        selectedSessions.filter { it.enabled }.mapTo(LinkedHashSet()) { it.sessionId }
    }
    val excludablePackages = remember(selectedSessions) {
        selectedSessions
            .map { it.packageName }
            .filterTo(LinkedHashSet()) { isRealPackageName(it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = if (isSelectionMode) {
                        stringResource(R.string.files_selected_count, selectedIds.size)
                    } else {
                        stringResource(R.string.sessions_title)
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
                                        sortedSessions.mapTo(LinkedHashSet(sortedSessions.size)) { it.sessionId }
                                    }
                                },
                                enabled = sortedSessions.isNotEmpty(),
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
                                enabled = sortedSessions.isNotEmpty(),
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.SelectAll,
                                    contentDescription = stringResource(R.string.files_multi_select),
                                    tint = if (sortedSessions.isNotEmpty()) {
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
                val canEnable = enableTargets.isNotEmpty()
                val canDisable = disableTargets.isNotEmpty()
                val canExclude = excludablePackages.isNotEmpty()
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
                                val targets = enableTargets.toSet()
                                onSetSessionsEnabled(targets, true)
                                isSelectionMode = false
                                selectedIds = emptySet()
                                onShowToast(String.format(Locale.getDefault(), enabledCountToastFmt, targets.size))
                            },
                            enabled = canEnable,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = stringResource(R.string.sessions_action_enable),
                                tint = if (canEnable) {
                                    MiuixTheme.colorScheme.onSurface
                                } else {
                                    MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                            )
                        }
                        IconButton(
                            onClick = {
                                val targets = disableTargets.toSet()
                                onSetSessionsEnabled(targets, false)
                                isSelectionMode = false
                                selectedIds = emptySet()
                                onShowToast(String.format(Locale.getDefault(), disabledCountToastFmt, targets.size))
                            },
                            enabled = canDisable,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeOff,
                                contentDescription = stringResource(R.string.sessions_action_disable),
                                tint = if (canDisable) {
                                    MiuixTheme.colorScheme.onSurface
                                } else {
                                    MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                            )
                        }
                        IconButton(
                            onClick = { showExcludeConfirm = true },
                            enabled = canExclude,
                            holdDownState = showExcludeConfirm,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Block,
                                contentDescription = stringResource(R.string.sessions_action_exclude),
                                tint = if (canExclude) {
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
        val topPadding = innerPadding.calculateTopPadding()
        val refreshTexts = listOf(
            stringResource(R.string.common_pull_to_refresh),
            stringResource(R.string.common_release_to_refresh),
            stringResource(R.string.common_refreshing),
            stringResource(R.string.common_refresh_complete),
        )
        PullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = triggerRefresh,
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
            contentPadding = PaddingValues(top = topPadding),
            topAppBarScrollBehavior = scrollBehavior,
            refreshTexts = refreshTexts,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalCutoutPadding()
                    .scrollEndHaptic()
                    .overScrollVertical(),
                contentPadding = PaddingValues(top = topPadding),
            ) {
                item(key = "sessions_top_spacer") {
                    Spacer(Modifier.height(12.dp))
                }

                if (sortedSessions.isEmpty()) {
                    item(key = "sessions_empty_card") {
                        SessionsEmptyStateCard(
                            globalMode = globalMode,
                            masterEnabled = masterEnabled,
                        )
                    }
                } else {
                    items(
                        items = sortedSessions,
                        key = { "session_${it.sessionId}" },
                    ) { session ->
                        val isSelected = session.sessionId in selectedIds
                        val openedTime = remember(session.openedAt) {
                            timeFormatter.format(Date(session.openedAt))
                        }
                        SessionRow(
                            session = session,
                            openedTime = openedTime,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onToggleEnabled = { enabled ->
                                onToggleSession(session.sessionId, enabled)
                            },
                            onSelectToggle = {
                                selectedIds = if (isSelected) {
                                    selectedIds - session.sessionId
                                } else {
                                    selectedIds + session.sessionId
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isSelectionMode = true
                                    selectedIds = setOf(session.sessionId)
                                }
                            },
                        )
                    }
                }

                item(key = "sessions_bottom_spacer") {
                    Spacer(
                        Modifier
                            .height(UiDimens.FabListPadding)
                            .navigationBarsPadding(),
                    )
                }
            }
        }
    }

    if (showExcludeConfirm && excludablePackages.isNotEmpty()) {
        val packages = excludablePackages.toSet()
        val singlePkg = packages.singleOrNull()
        val singleLabel = remember(singlePkg, context) {
            singlePkg?.let { resolveAppMeta(context, it).label ?: it }
        }
        ConfirmDialog(
            title = stringResource(
                if (singleLabel != null) R.string.sessions_exclude_single_title else R.string.sessions_exclude_multi_title,
            ),
            body = if (singleLabel != null) {
                stringResource(R.string.sessions_exclude_single_confirm, singleLabel)
            } else {
                stringResource(R.string.sessions_exclude_multi_confirm, packages.size)
            },
            confirmLabel = stringResource(R.string.sessions_action_exclude),
            destructive = true,
            onConfirm = {
                showExcludeConfirm = false
                onExcludeApps(packages)
                isSelectionMode = false
                selectedIds = emptySet()
                val toastMsg = if (singleLabel != null) {
                    String.format(Locale.getDefault(), excludedSingleToastFmt, singleLabel)
                } else {
                    String.format(Locale.getDefault(), excludedMultiToastFmt, packages.size)
                }
                onShowToast(toastMsg)
            },
            onDismiss = { showExcludeConfirm = false },
        )
    }
}

@Composable
private fun SessionRow(
    session: AudioSessionInfo,
    openedTime: String,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectToggle: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val appMeta = remember(session.packageName) {
        resolveAppMeta(context, session.packageName)
    }
    val unknownAppLabel = stringResource(R.string.sessions_unknown_app)
    val displayTitle = when {
        appMeta.label != null -> appMeta.label
        isRealPackageName(session.packageName) -> session.packageName
        session.packageName.isNotEmpty() -> stringResource(R.string.sessions_uid_format, session.packageName)
        else -> unknownAppLabel
    }
    val subtitle = when {
        appMeta.label != null && isRealPackageName(session.packageName) -> session.packageName
        else -> null
    }
    val usageLabel = audioUsageLabel(session.usage)
    val contentAlpha = if (!isSelectionMode && !session.enabled) 0.58f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = {
                        if (isSelectionMode) {
                            onSelectToggle()
                        } else {
                            onToggleEnabled(!session.enabled)
                        }
                    },
                    onLongClick = if (isSelectionMode) null else onLongClick,
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.graphicsLayer { alpha = contentAlpha },
                contentAlignment = Alignment.Center,
            ) {
                if (appMeta.icon != null) {
                    Image(
                        bitmap = appMeta.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(SessionAppIconSize)
                            .squircleClip(SessionAppIconCornerRadius),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(SessionAppIconSize)
                            .squircleBackground(
                                MiuixTheme.colorScheme.secondaryContainer,
                                SessionAppIconCornerRadius,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MiuixTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .graphicsLayer { alpha = contentAlpha },
            ) {
                Text(
                    text = displayTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusBadge(
                        text = stringResource(
                            if (session.enabled) R.string.sessions_status_processing else R.string.sessions_status_disabled,
                        ),
                        color = if (session.enabled) StatusColors.healthy else StatusColors.neutral,
                    )
                    StatusBadge(
                        text = stringResource(R.string.sessions_id_badge, session.sessionId),
                        color = MiuixTheme.colorScheme.primary,
                    )
                    if (usageLabel != null) {
                        StatusBadge(
                            text = usageLabel,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                    Text(
                        text = openedTime,
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                    )
                }
            }

            if (isSelectionMode) {
                Checkbox(
                    state = ToggleableState(isSelected),
                    onClick = null,
                )
            } else {
                Switch(
                    checked = session.enabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.semantics {
                        contentDescription = displayTitle
                    },
                )
            }
        }
    }
}

@Composable
private fun SessionsEmptyStateCard(
    globalMode: Boolean,
    masterEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val titleRes = when {
        globalMode -> R.string.sessions_empty_global_mode_title
        !masterEnabled -> R.string.sessions_empty_master_off_title
        else -> R.string.sessions_empty_title
    }
    val descRes = when {
        globalMode -> R.string.sessions_empty_global_mode_desc
        !masterEnabled -> R.string.sessions_empty_master_off_desc
        else -> R.string.sessions_empty_desc
    }
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
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(titleRes),
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(descRes),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun isRealPackageName(value: String): Boolean = value.isNotEmpty() && !value.all(Char::isDigit)

private fun resolveAppMeta(
    context: Context,
    packageName: String,
): ResolvedAppMeta {
    if (!isRealPackageName(packageName)) return ResolvedAppMeta(label = null, icon = null)
    return try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        val label = pm.getApplicationLabel(appInfo).toString()
        val drawable: Drawable = pm.getApplicationIcon(appInfo)
        ResolvedAppMeta(label = label, icon = drawable.toBitmap().asImageBitmap())
    } catch (_: Exception) {
        ResolvedAppMeta(label = null, icon = null)
    }
}

@Composable
private fun audioUsageLabel(usage: Int): String? =
    when (usage) {
        AudioAttributes.USAGE_MEDIA -> stringResource(R.string.sessions_usage_media)
        AudioAttributes.USAGE_GAME -> stringResource(R.string.sessions_usage_game)
        AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE -> stringResource(R.string.sessions_usage_navigation)
        AudioAttributes.USAGE_VOICE_COMMUNICATION -> stringResource(R.string.sessions_usage_voice)
        AudioAttributes.USAGE_ALARM -> stringResource(R.string.sessions_usage_alarm)
        AudioAttributes.USAGE_NOTIFICATION -> stringResource(R.string.sessions_usage_notification)
        AudioAttributes.USAGE_ASSISTANT -> stringResource(R.string.sessions_usage_assistant)
        else -> null
    }

@Preview(name = "Sessions - Populated", showBackground = true)
@Composable
private fun SessionsScreenPreview() {
    val now = 1710000000000L
    ViperTheme {
        SessionsScreenContent(
            sessions = listOf(
                AudioSessionInfo(
                    sessionId = 142,
                    packageName = "com.netease.cloudmusic",
                    enabled = true,
                    openedAt = now,
                    usage = AudioAttributes.USAGE_MEDIA,
                ),
                AudioSessionInfo(
                    sessionId = 208,
                    packageName = "com.google.android.youtube",
                    enabled = true,
                    openedAt = now - 35_000L,
                    usage = AudioAttributes.USAGE_MEDIA,
                ),
                AudioSessionInfo(
                    sessionId = 315,
                    packageName = "com.miHoYo.Yuanshen",
                    enabled = false,
                    openedAt = now - 120_000L,
                    usage = AudioAttributes.USAGE_GAME,
                ),
            ).toPersistentList(),
            masterEnabled = true,
            globalMode = false,
            onToggleSession = { _, _ -> },
            onSetSessionsEnabled = { _, _ -> },
            onExcludeApps = {},
            onRefresh = { it() },
            onShowToast = {},
            onBack = {},
        )
    }
}

@Preview(name = "Sessions - Selection Mode", showBackground = true)
@Composable
private fun SessionsScreenSelectionPreview() {
    val now = 1710000000000L
    ViperTheme {
        SessionsScreenContent(
            sessions = listOf(
                AudioSessionInfo(
                    sessionId = 142,
                    packageName = "com.netease.cloudmusic",
                    enabled = true,
                    openedAt = now,
                    usage = AudioAttributes.USAGE_MEDIA,
                ),
                AudioSessionInfo(
                    sessionId = 315,
                    packageName = "com.miHoYo.Yuanshen",
                    enabled = false,
                    openedAt = now - 120_000L,
                    usage = AudioAttributes.USAGE_GAME,
                ),
            ).toPersistentList(),
            masterEnabled = true,
            globalMode = false,
            onToggleSession = { _, _ -> },
            onSetSessionsEnabled = { _, _ -> },
            onExcludeApps = {},
            onRefresh = { it() },
            onShowToast = {},
            onBack = {},
            initialSelectionMode = true,
            initialSelectedIds = setOf(142),
        )
    }
}

@Preview(name = "Sessions - Empty", showBackground = true)
@Composable
private fun SessionsScreenEmptyPreview() {
    ViperTheme {
        SessionsScreenContent(
            sessions = emptyList<AudioSessionInfo>().toPersistentList(),
            masterEnabled = true,
            globalMode = false,
            onToggleSession = { _, _ -> },
            onSetSessionsEnabled = { _, _ -> },
            onExcludeApps = {},
            onRefresh = { it() },
            onShowToast = {},
            onBack = {},
        )
    }
}
