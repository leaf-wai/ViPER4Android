package com.llsl.viper4android.ui.screens.settings

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.llsl.viper4android.R
import com.llsl.viper4android.ui.components.AdaptiveTopAppBar
import com.llsl.viper4android.ui.components.BlurredBar
import com.llsl.viper4android.ui.components.CardItem
import com.llsl.viper4android.ui.components.DialogCard
import com.llsl.viper4android.ui.components.DialogEmptyState
import com.llsl.viper4android.ui.components.DialogList
import com.llsl.viper4android.ui.components.RowDivider
import com.llsl.viper4android.ui.components.ToggleRow
import com.llsl.viper4android.ui.components.UiDimens
import com.llsl.viper4android.ui.components.groupedCardItems
import com.llsl.viper4android.ui.components.layerBackdrop
import com.llsl.viper4android.ui.components.rememberBlurBackdrop
import com.llsl.viper4android.ui.screens.main.InstalledAppInfo
import com.llsl.viper4android.ui.theme.ViperTheme
import com.llsl.viper4android.ui.util.horizontalCutoutPadding
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

private val ExcludedAppIconSize = 40.dp
private val ExcludedAppIconCornerRadius = 10.dp

/**
 * 排除应用二级页面，严格遵循 AGENTS.md 二级页规范与 KernelSU SuperUser 应用行排版：
 * 应用图标置左（40dp squircle）、应用名与包名居中、选择框置右（CheckboxLocation.End），间距均衡清晰。
 */
@Composable
fun ExcludedAppsScreen(
    excludedApps: Set<String>,
    onToggle: (String, Boolean) -> Unit,
    loadInstalledApps: ((List<InstalledAppInfo>) -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>?>(null) }

    LaunchedEffect(Unit) {
        loadInstalledApps { installedApps = it }
    }

    ExcludedAppsScreenContent(
        installedApps = installedApps,
        excludedApps = excludedApps,
        onToggle = onToggle,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun ExcludedAppsScreenContent(
    installedApps: List<InstalledAppInfo>?,
    excludedApps: Set<String>,
    onToggle: (String, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }

    val filtered = remember(installedApps, excludedApps, showSystemApps, query) {
        installedApps?.let { apps ->
            filterAndSortExcludedApps(
                installedApps = apps,
                excludedApps = excludedApps,
                showSystemApps = showSystemApps,
                query = query,
            )
        }
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
                    title = stringResource(R.string.excluded_apps_title),
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    navigationIcon = {
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
                    },
                )
            }
        },
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
            item(key = "top_spacer") {
                Spacer(Modifier.height(12.dp))
            }
            item(key = "search_field") {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = stringResource(R.string.excluded_apps_search),
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                )
            }
            groupedCardItems(
                keyPrefix = "excluded_filter",
                outerBottomPadding = 12.dp,
                items = listOf(
                    CardItem("show_system") {
                        SwitchPreference(
                            title = stringResource(R.string.excluded_apps_show_system),
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.Android,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 6.dp),
                                    tint = MiuixTheme.colorScheme.onBackground,
                                )
                            },
                            checked = showSystemApps,
                            onCheckedChange = { showSystemApps = it },
                        )
                    },
                ),
            )
            if (filtered == null) {
                item(key = "loading") {
                    DialogEmptyState(
                        text = stringResource(R.string.excluded_apps_loading),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            } else {
                groupedCardItems(
                    keyPrefix = "excluded_apps",
                    outerBottomPadding = 12.dp,
                    items = filtered.map { app ->
                        CardItem(app.packageName) {
                            AppCheckboxRow(
                                app = app,
                                checked = app.packageName in excludedApps,
                                onToggle = { checked -> onToggle(app.packageName, checked) },
                            )
                        }
                    },
                )
            }
            item(key = "bottom_spacer") {
                Spacer(Modifier.height(24.dp).navigationBarsPadding())
            }
        }
    }
}

@Composable
fun ExcludedAppsDialog(
    excludedApps: Set<String>,
    onToggle: (String, Boolean) -> Unit,
    loadInstalledApps: ((List<InstalledAppInfo>) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    show: Boolean = true,
) {
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>?>(null) }
    var query by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loadInstalledApps { installedApps = it }
    }

    WindowDialog(
        title = stringResource(R.string.excluded_apps_title),
        show = show,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.excluded_apps_search),
                useLabelAsPlaceholder = true,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = UiDimens.Medium),
            )
            val apps = installedApps
            Column(
                modifier = Modifier.weight(1f, fill = false),
            ) {
                if (apps == null) {
                    DialogEmptyState(text = stringResource(R.string.excluded_apps_loading))
                } else {
                    val filtered = remember(apps, excludedApps, showSystemApps, query) {
                        filterAndSortExcludedApps(
                            installedApps = apps,
                            excludedApps = excludedApps,
                            showSystemApps = showSystemApps,
                            query = query,
                        )
                    }
                    DialogCard {
                        ToggleRow(
                            label = stringResource(R.string.excluded_apps_show_system),
                            checked = showSystemApps,
                            onCheckedChange = { showSystemApps = it },
                        )
                        RowDivider()
                        DialogList {
                            itemsIndexed(filtered, key = { _, app -> app.packageName }) { index, app ->
                                AppCheckboxRow(
                                    app = app,
                                    checked = app.packageName in excludedApps,
                                    onToggle = { checked -> onToggle(app.packageName, checked) },
                                )
                                if (index != filtered.lastIndex) {
                                    RowDivider()
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                text = stringResource(R.string.action_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * 过滤并排序排除应用列表：已勾选的应用（含已勾选的系统应用与已卸载残留包名）始终置顶，
 * 同组内按应用名称（不区分大小写）及包名稳定排序。
 */
internal fun filterAndSortExcludedApps(
    installedApps: List<InstalledAppInfo>,
    excludedApps: Set<String>,
    showSystemApps: Boolean,
    query: String,
): List<InstalledAppInfo> {
    val installedPackages = installedApps.mapTo(HashSet(installedApps.size)) { it.packageName }
    val missing = excludedApps
        .filter { pkg -> pkg !in installedPackages }
        .map { InstalledAppInfo(packageName = it, label = it, isSystemApp = false) }
    return (installedApps + missing)
        .filter { app ->
            (showSystemApps || !app.isSystemApp || app.packageName in excludedApps) &&
                (
                    query.isBlank() ||
                        app.label.contains(query, ignoreCase = true) ||
                        app.packageName.contains(query, ignoreCase = true)
                    )
        }
        .sortedWith(
            compareByDescending<InstalledAppInfo> { it.packageName in excludedApps }
                .thenBy { it.label.lowercase() }
                .thenBy { it.packageName },
        )
}

@Composable
private fun AppCheckboxRow(
    app: InstalledAppInfo,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val iconBitmap = remember(app.packageName) {
        try {
            val drawable: Drawable = context.packageManager.getApplicationIcon(app.packageName)
            drawable.toBitmap().asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    // 参考 KernelSU SuperUser 应用项：图标置左（40dp + StartActionSlot 5dp + BasicComponent 8dp = 13dp），选择框置右
    CheckboxPreference(
        title = app.label,
        summary = app.packageName,
        checked = checked,
        onCheckedChange = onToggle,
        checkboxLocation = CheckboxLocation.End,
        startAction = {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(ExcludedAppIconSize)
                        .squircleClip(ExcludedAppIconCornerRadius),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(ExcludedAppIconSize)
                        .squircleBackground(
                            MiuixTheme.colorScheme.secondaryContainer,
                            ExcludedAppIconCornerRadius,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Android,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MiuixTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        },
    )
}

@Preview(name = "Excluded Apps", showBackground = true)
@Composable
private fun ExcludedAppsScreenPreview() {
    ViperTheme {
        ExcludedAppsScreenContent(
            installedApps = listOf(
                InstalledAppInfo("com.spotify.music", "Spotify", isSystemApp = false),
                InstalledAppInfo("com.netease.cloudmusic", "NetEase Cloud Music", isSystemApp = false),
                InstalledAppInfo("com.google.android.youtube", "YouTube", isSystemApp = false),
            ),
            excludedApps = setOf("com.spotify.music"),
            onToggle = { _, _ -> },
            onBack = {},
        )
    }
}
