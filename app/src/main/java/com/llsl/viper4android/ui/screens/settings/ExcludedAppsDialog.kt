package com.llsl.viper4android.ui.screens.settings

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.llsl.viper4android.ui.util.horizontalCutoutPadding
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 排除应用二级页面，严格遵循 AGENTS.md 二级页规范：
 * Scaffold + BlurredBar + AdaptiveTopAppBar + LazyColumn（无 bottomPadding 参数，靠末尾 Spacer 自适应）。
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
    var query by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loadInstalledApps { installedApps = it }
    }

    val apps = installedApps
    val filtered = remember(apps, excludedApps, showSystemApps, query) {
        if (apps == null) {
            null
        } else {
            val missing = excludedApps
                .filter { pkg -> apps.none { it.packageName == pkg } }
                .map { InstalledAppInfo(packageName = it, label = it, isSystemApp = false) }
            val all = (apps + missing).sortedBy { it.label.lowercase() }
            all.filter { app ->
                (showSystemApps || !app.isSystemApp) &&
                    (
                        query.isBlank() ||
                            app.label.contains(query, ignoreCase = true) ||
                            app.packageName.contains(query, ignoreCase = true)
                        )
            }
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
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
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
                    val missing = excludedApps
                        .filter { pkg -> apps.none { it.packageName == pkg } }
                        .map { InstalledAppInfo(packageName = it, label = it, isSystemApp = false) }
                    val all = (apps + missing).sortedBy { it.label.lowercase() }
                    val filtered = all.filter { app ->
                        (showSystemApps || !app.isSystemApp) &&
                            (
                                query.isBlank() ||
                                    app.label.contains(query, ignoreCase = true) ||
                                    app.packageName.contains(query, ignoreCase = true)
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

@Composable
private fun AppCheckboxRow(
    app: InstalledAppInfo,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val icon: Drawable? = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName)
        } catch (_: Exception) {
            null
        }
    }

    CheckboxPreference(
        title = app.label,
        summary = app.packageName,
        checked = checked,
        onCheckedChange = onToggle,
        startAction = if (icon != null) {
            {
                Image(
                    bitmap = icon.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(UiDimens.AppIcon)
                        .squircleClip(8.dp),
                )
            }
        } else {
            null
        },
    )
}
