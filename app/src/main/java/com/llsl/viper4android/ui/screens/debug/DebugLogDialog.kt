package com.llsl.viper4android.ui.screens.debug

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llsl.viper4android.R
import com.llsl.viper4android.ui.components.AdaptiveTopAppBar
import com.llsl.viper4android.ui.components.BlurredBar
import com.llsl.viper4android.ui.components.CardSegment
import com.llsl.viper4android.ui.components.ConfirmDialog
import com.llsl.viper4android.ui.components.DialogCard
import com.llsl.viper4android.ui.components.DialogEmptyState
import com.llsl.viper4android.ui.components.IconActionItem
import com.llsl.viper4android.ui.components.UiDimens
import com.llsl.viper4android.ui.components.layerBackdrop
import com.llsl.viper4android.ui.components.rememberBlurBackdrop
import com.llsl.viper4android.ui.theme.StatusColors
import com.llsl.viper4android.ui.util.PlatformToast
import com.llsl.viper4android.ui.util.horizontalCutoutPadding
import com.llsl.viper4android.ui.util.rememberPlatformToast
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SourceFilter { ALL, APP, DRIVER }

private enum class LevelFilter { ALL, INFO, DEBUG, WARN, ERROR }

/**
 * 调试日志二级页面，严格遵循 AGENTS.md 二级页规范：
 * Scaffold + BlurredBar + AdaptiveTopAppBar + LazyColumn（无 bottomPadding 参数，末尾 Spacer 自适应）。
 */
@Composable
fun DebugLogScreen(
    onDisableDebug: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val platform = rememberPlatformToast()
    val state = remember { DebugLogState() }
    val listState = rememberLazyListState()
    var sourceFilter by remember { mutableStateOf(SourceFilter.ALL) }
    var levelFilter by remember { mutableStateOf(LevelFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showExportConfirm by remember { mutableStateOf(false) }

    DisposableEffect(state) {
        state.start(includeFileHistory = true)
        onDispose { state.shutdown() }
    }

    val filteredEntries by remember(state) {
        derivedStateOf {
            val query = searchQuery
            state.visibleEntries.filter { entry ->
                matchesSource(entry, sourceFilter) &&
                    matchesLevel(entry, levelFilter) &&
                    (query.isEmpty() || entry.raw.contains(query, ignoreCase = true))
            }
        }
    }

    if (showExportConfirm) {
        val ts = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) }
        ConfirmDialog(
            title = stringResource(R.string.debug_export_title),
            body = stringResource(R.string.debug_export_confirm, "$ts.app.log", "$ts.driver.log"),
            confirmLabel = stringResource(R.string.debug_export_action),
            onConfirm = {
                exportLogs(context, platform, state, ts)
                showExportConfirm = false
            },
            onDismiss = { showExportConfirm = false },
        )
    }

    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.debug_log_title),
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.padding(start = 16.dp),
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.action_close),
                            )
                        }
                    },
                    actions = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(end = 16.dp),
                        ) {
                            IconButton(
                                onClick = { state.clear() },
                                minHeight = 35.dp,
                                minWidth = 35.dp,
                                backgroundColor = MiuixTheme.colorScheme.secondaryContainer,
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Delete,
                                    contentDescription = stringResource(R.string.action_clear),
                                )
                            }
                            IconButton(
                                onClick = { showExportConfirm = true },
                                minHeight = 35.dp,
                                minWidth = 35.dp,
                                backgroundColor = MiuixTheme.colorScheme.secondaryContainer,
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Download,
                                    contentDescription = stringResource(R.string.debug_export_action),
                                )
                            }
                            IconButton(
                                onClick = {
                                    onDisableDebug()
                                    onBack()
                                },
                                minHeight = 35.dp,
                                minWidth = 35.dp,
                                backgroundColor = MiuixTheme.colorScheme.secondaryContainer,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoNotDisturb,
                                    contentDescription = stringResource(R.string.debug_disable_debug),
                                    tint = StatusColors.danger,
                                )
                            }
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .horizontalCutoutPadding()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
        ) {
            item(key = "top_spacer") {
                Spacer(Modifier.height(12.dp))
            }
            item(key = "search_field") {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = stringResource(R.string.debug_search_hint),
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                )
            }
            item(key = "source_filter") {
                SourceFilterRow(
                    selected = sourceFilter,
                    onSelect = { sourceFilter = it },
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 8.dp),
                )
            }
            item(key = "level_filter") {
                LevelFilterRow(
                    selected = levelFilter,
                    onSelect = { levelFilter = it },
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 8.dp),
                )
            }
            item(key = "count_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                ) {
                    StreamErrorBanner(state.appStreamError, state.driverStreamError)
                    Text(
                        text = "${filteredEntries.size} / ${state.totalCount}",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            if (filteredEntries.isEmpty()) {
                item(key = "empty") {
                    DialogEmptyState(
                        text = "0 / ${state.totalCount}",
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            } else {
                val lastIdx = filteredEntries.lastIndex
                itemsIndexed(filteredEntries) { index, entry ->
                    CardSegment(
                        isFirst = index == 0,
                        isLast = index == lastIdx,
                        outerBottomPadding = if (index == lastIdx) 12.dp else 0.dp,
                        insidePadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = if (index == 0 || index == lastIdx) 6.dp else 2.dp,
                        ),
                    ) {
                        Text(
                            text = entry.raw,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = colorForEntry(entry),
                        )
                    }
                }
            }
            item(key = "bottom_spacer") {
                Spacer(Modifier.height(24.dp).navigationBarsPadding())
            }
        }
    }
}

@Composable
fun DebugLogDialog(
    onDisableDebug: () -> Unit,
    onDismiss: () -> Unit,
    show: Boolean = true,
) {
    val context = LocalContext.current
    val platform = rememberPlatformToast()
    val state = remember { DebugLogState() }
    val listState = rememberLazyListState()
    var sourceFilter by remember { mutableStateOf(SourceFilter.ALL) }
    var levelFilter by remember { mutableStateOf(LevelFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showExportConfirm by remember { mutableStateOf(false) }

    DisposableEffect(state) {
        state.start(includeFileHistory = true)
        onDispose { state.shutdown() }
    }

    val filteredEntries by remember(state) {
        derivedStateOf {
            val query = searchQuery
            state.visibleEntries.filter { entry ->
                matchesSource(entry, sourceFilter) &&
                    matchesLevel(entry, levelFilter) &&
                    (query.isEmpty() || entry.raw.contains(query, ignoreCase = true))
            }
        }
    }

    LaunchedEffect(filteredEntries.size) {
        if (filteredEntries.isNotEmpty()) {
            listState.animateScrollToItem(filteredEntries.size - 1)
        }
    }

    if (showExportConfirm) {
        val ts = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) }
        ConfirmDialog(
            title = stringResource(R.string.debug_export_title),
            body = stringResource(R.string.debug_export_confirm, "$ts.app.log", "$ts.driver.log"),
            confirmLabel = stringResource(R.string.debug_export_action),
            onConfirm = {
                exportLogs(context, platform, state, ts)
                showExportConfirm = false
            },
            onDismiss = { showExportConfirm = false },
        )
    }

    WindowDialog(
        title = stringResource(R.string.debug_log_title),
        show = show,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = stringResource(R.string.debug_search_hint),
                useLabelAsPlaceholder = true,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = UiDimens.Medium),
            )
            SourceFilterRow(
                selected = sourceFilter,
                onSelect = { sourceFilter = it },
                modifier = Modifier.padding(bottom = UiDimens.XSmall),
            )
            LevelFilterRow(
                selected = levelFilter,
                onSelect = { levelFilter = it },
                modifier = Modifier.padding(bottom = UiDimens.XSmall),
            )

            StreamErrorBanner(state.appStreamError, state.driverStreamError)

            Text(
                text = "${filteredEntries.size} / ${state.totalCount}",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(bottom = UiDimens.XSmall),
            )

            DialogCard(
                modifier = Modifier.weight(1f, fill = false),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .padding(UiDimens.Large),
                ) {
                    items(filteredEntries) { entry ->
                        Text(
                            text = entry.raw,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = colorForEntry(entry),
                            modifier = Modifier.padding(vertical = UiDimens.Hairline),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                IconActionItem(
                    icon = MiuixIcons.Delete,
                    label = stringResource(R.string.action_clear),
                    onClick = { state.clear() },
                    modifier = Modifier.weight(1f),
                )
                IconActionItem(
                    icon = MiuixIcons.Download,
                    label = stringResource(R.string.debug_export_action),
                    onClick = { showExportConfirm = true },
                    modifier = Modifier.weight(1f),
                )
                IconActionItem(
                    icon = Icons.Default.DoNotDisturb,
                    label = stringResource(R.string.debug_disable_debug),
                    onClick = onDisableDebug,
                    tint = StatusColors.danger,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SourceFilterRow(
    selected: SourceFilter,
    onSelect: (SourceFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = SourceFilter.entries
    val tabs = entries.map { sourceLabel(it) }
    TabRowWithContour(
        tabs = tabs,
        selectedTabIndex = entries.indexOf(selected),
        onTabSelected = { index -> onSelect(entries[index]) },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun LevelFilterRow(
    selected: LevelFilter,
    onSelect: (LevelFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = LevelFilter.entries
    val tabs = entries.map { levelLabel(it) }
    TabRowWithContour(
        tabs = tabs,
        selectedTabIndex = entries.indexOf(selected),
        onTabSelected = { index -> onSelect(entries[index]) },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun StreamErrorBanner(
    appError: String?,
    driverError: String?,
) {
    if (appError == null && driverError == null) return
    Column(modifier = Modifier.padding(bottom = UiDimens.XSmall)) {
        appError?.let {
            Text(
                text = "App log: $it",
                style = MiuixTheme.textStyles.footnote1,
                color = StatusColors.danger,
            )
        }
        driverError?.let {
            Text(
                text = "Driver log: $it",
                style = MiuixTheme.textStyles.footnote1,
                color = StatusColors.danger,
            )
        }
    }
}

@Composable
private fun sourceLabel(option: SourceFilter): String =
    when (option) {
        SourceFilter.ALL -> stringResource(R.string.debug_filter_all)
        SourceFilter.APP -> stringResource(R.string.debug_filter_app)
        SourceFilter.DRIVER -> stringResource(R.string.debug_filter_driver)
    }

@Composable
private fun levelLabel(option: LevelFilter): String =
    when (option) {
        LevelFilter.ALL -> stringResource(R.string.debug_filter_all)
        LevelFilter.INFO -> stringResource(R.string.debug_filter_info)
        LevelFilter.DEBUG -> stringResource(R.string.debug_filter_debug)
        LevelFilter.WARN -> stringResource(R.string.debug_filter_warn)
        LevelFilter.ERROR -> stringResource(R.string.debug_filter_error)
    }

private fun matchesSource(
    entry: LogEntry,
    filter: SourceFilter,
): Boolean =
    when (filter) {
        SourceFilter.ALL -> true
        SourceFilter.APP -> entry.source == LogSource.APP
        SourceFilter.DRIVER -> entry.source == LogSource.DRIVER
    }

private fun matchesLevel(
    entry: LogEntry,
    filter: LevelFilter,
): Boolean =
    when (filter) {
        LevelFilter.ALL -> true
        LevelFilter.INFO -> entry.level == LogLevel.INFO
        LevelFilter.DEBUG -> entry.level == LogLevel.DEBUG
        LevelFilter.WARN -> entry.level == LogLevel.WARN
        LevelFilter.ERROR -> entry.level == LogLevel.ERROR
    }

@Composable
private fun colorForEntry(entry: LogEntry): Color =
    when (entry.level) {
        LogLevel.ERROR -> StatusColors.logError
        LogLevel.WARN -> StatusColors.logWarn
        LogLevel.INFO -> StatusColors.logInfo
        LogLevel.DEBUG -> StatusColors.logDebug
        LogLevel.UNKNOWN -> StatusColors.neutral
    }

private fun exportLogs(
    context: Context,
    platform: PlatformToast,
    state: DebugLogState,
    timestamp: String,
) {
    val dir = File(context.getExternalFilesDir(null), "logs")
    dir.mkdirs()
    try {
        File(dir, "$timestamp.app.log").writeText(
            state.visibleEntries.filter { it.source == LogSource.APP }.joinToString("\n") { it.raw },
        )
        File(dir, "$timestamp.driver.log").writeText(
            state.visibleEntries.filter { it.source == LogSource.DRIVER }.joinToString("\n") { it.raw },
        )
        platform.showToast(context.getString(R.string.debug_export_done, dir.absolutePath), long = true)
    } catch (_: Exception) {
        platform.showToast(context.getString(R.string.debug_export_failed), long = false)
    }
}
