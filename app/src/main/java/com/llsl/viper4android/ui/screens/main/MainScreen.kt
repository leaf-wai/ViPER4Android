package com.llsl.viper4android.ui.screens.main

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.llsl.viper4android.R
import com.llsl.viper4android.audio.AudioDevice
import com.llsl.viper4android.data.model.DeviceSettings
import com.llsl.viper4android.data.model.Preset
import com.llsl.viper4android.effect.EffectState
import com.llsl.viper4android.ui.components.AdaptiveTopAppBar
import com.llsl.viper4android.ui.components.BlurredBar
import com.llsl.viper4android.ui.components.CardItem
import com.llsl.viper4android.ui.components.ConfirmDialog
import com.llsl.viper4android.ui.components.InputDialog
import com.llsl.viper4android.ui.components.groupedCardItems
import com.llsl.viper4android.ui.components.layerBackdrop
import com.llsl.viper4android.ui.components.rememberBlurBackdrop
import com.llsl.viper4android.ui.screens.device.DeviceDialog
import com.llsl.viper4android.ui.screens.preset.PresetDialog
import com.llsl.viper4android.ui.screens.settings.UpdateDialog
import com.llsl.viper4android.ui.screens.status.DriverStatusDialog
import com.llsl.viper4android.ui.theme.StatusColors
import com.llsl.viper4android.ui.util.PlatformToast
import com.llsl.viper4android.ui.util.WideContentBox
import com.llsl.viper4android.ui.util.rememberIsWideScreen
import com.llsl.viper4android.ui.util.rememberPlatformToast
import com.llsl.viper4android.utils.RootShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Sidebar
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.icon.extended.UploadCloud
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onOpenEqScreen: () -> Unit = {},
    onOpenExcludedApps: () -> Unit = {},
    onOpenDebugLog: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val presets by viewModel.presetList.collectAsStateWithLifecycle()
    val deviceSettings by viewModel.deviceSettingsList.collectAsStateWithLifecycle()
    val driverStatus by viewModel.driverStatus.collectAsStateWithLifecycle()
    val autoStart by viewModel.autoStartEnabled.collectAsStateWithLifecycle()
    val globalMode by viewModel.globalModeEnabled.collectAsStateWithLifecycle()
    val aidlMode by viewModel.aidlModeEnabled.collectAsStateWithLifecycle()
    val debugMode by viewModel.debugModeEnabled.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val excludedApps by viewModel.excludedApps.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.saveSettingsOnBackground()
    }

    var showDriverStatusDialog by rememberSaveable { mutableStateOf(false) }
    var showPresetDialog by rememberSaveable { mutableStateOf(false) }
    var showDeviceDialog by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val platform = rememberPlatformToast()
    val scope = rememberCoroutineScope()
    val expandController = rememberEffectExpandController()

    val isWideScreen = rememberIsWideScreen()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val railState = rememberNavigationRailState()

    val outerBackdrop = rememberBlurBackdrop()
    val outerBlurActive = outerBackdrop != null
    val outerBarColor = if (outerBlurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    val importPresetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        viewModel.importPresetFiles(
            uris = uris,
            notificationTitle = context.getString(R.string.settings_import_preset),
            successStr = context.getString(R.string.import_success),
        ) { ok ->
            val msg = context.getString(if (ok) R.string.import_success else R.string.import_failed)
            platform.showToast(msg)
        }
    }

    val importKernelLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        viewModel.importKernels(
            uris = uris,
            notificationTitle = context.getString(R.string.settings_import_kernel),
            successStr = context.getString(R.string.import_success),
        ) { ok ->
            val msg = context.getString(if (ok) R.string.import_success else R.string.import_failed)
            platform.showToast(msg)
        }
    }

    val importVdcLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        viewModel.importVdcs(
            uris = uris,
            notificationTitle = context.getString(R.string.settings_import_vdc),
            successStr = context.getString(R.string.import_success),
        ) { ok ->
            val msg = context.getString(if (ok) R.string.import_success else R.string.import_failed)
            platform.showToast(msg)
        }
    }

    LaunchedEffect(pagerState.currentPage, showDriverStatusDialog) {
        if (pagerState.currentPage == 0 || showDriverStatusDialog) {
            while (true) {
                viewModel.queryDriverStatus()
                delay(500L)
            }
        }
    }

    val navItems = listOf(
        NavigationItem(
            label = stringResource(R.string.tab_overview),
            icon = if (isWideScreen) MiuixIcons.Home else MiuixIcons.Basic.Sidebar
        ),
        NavigationItem(
            label = stringResource(R.string.tab_effects),
            icon = MiuixIcons.Tune
        ),
        NavigationItem(
            label = stringResource(R.string.tab_preset_device),
            icon = MiuixIcons.UploadCloud
        ),
        NavigationItem(
            label = stringResource(R.string.tab_settings),
            icon = MiuixIcons.Settings
        )
    )

    val pagerContent: @Composable (Dp) -> Unit = { bottomPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .then(if (outerBackdrop != null) Modifier.layerBackdrop(outerBackdrop) else Modifier),
            beyondViewportPageCount = 1
        ) { page ->
            when (page) {
                0 -> OverviewTab(
                    state = state,
                    driverStatus = driverStatus,
                    viewModel = viewModel,
                    bottomPadding = bottomPadding,
                    onJumpToEffects = {
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                    onShowDriverDialog = { showDriverStatusDialog = true },
                    showDriverDialog = showDriverStatusDialog,
                    onShowDeviceDialog = { showDeviceDialog = true },
                    showDeviceDialog = showDeviceDialog
                )

                1 -> EffectsTab(
                    state = state,
                    viewModel = viewModel,
                    expandController = expandController,
                    bottomPadding = bottomPadding,
                    onOpenEqScreen = onOpenEqScreen,
                    onShowDriverDialog = { showDriverStatusDialog = true }
                )

                2 -> PresetsAndDevicesTab(
                    presets = presets,
                    deviceSettings = deviceSettings,
                    currentDeviceId = state.activeDeviceId,
                    currentDeviceName = state.activeDeviceName,
                    viewModel = viewModel,
                    platform = platform,
                    bottomPadding = bottomPadding,
                    showPresetDialog = showPresetDialog,
                    onShowPresetDialog = { showPresetDialog = true },
                    showDeviceDialog = showDeviceDialog,
                    onShowDeviceDialog = { showDeviceDialog = true },
                    onImportPreset = { importPresetLauncher.launch(arrayOf("*/*")) }
                )

                3 -> SettingsTab(
                    driverStatus = driverStatus,
                    autoStart = autoStart,
                    globalMode = globalMode,
                    aidlMode = aidlMode,
                    debugMode = debugMode,
                    excludedApps = excludedApps,
                    updateState = updateState,
                    viewModel = viewModel,
                    platform = platform,
                    bottomPadding = bottomPadding,
                    onOpenExcludedApps = onOpenExcludedApps,
                    onOpenDebugLog = onOpenDebugLog,
                    onImportPreset = { importPresetLauncher.launch(arrayOf("*/*")) },
                    onImportKernel = { importKernelLauncher.launch(arrayOf("*/*")) },
                    onImportVdc = { importVdcLauncher.launch(arrayOf("*/*")) }
                )
            }
        }
    }

    if (isWideScreen) {
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    state = railState,
                    defaultWindowInsetsPadding = true,
                    color = MiuixTheme.colorScheme.surface
                ) {
                    navItems.forEachIndexed { index, item ->
                        NavigationRailItem(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            icon = item.icon,
                            label = item.label
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .consumeWindowInsets(WindowInsets.systemBars.only(WindowInsetsSides.Start))
                        .windowInsetsPadding(
                            WindowInsets.systemBars
                                .union(WindowInsets.displayCutout)
                                .only(WindowInsetsSides.End)
                        )
                ) {
                    pagerContent(innerPadding.calculateBottomPadding())
                }
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                BlurredBar(backdrop = outerBackdrop, blurActive = outerBlurActive) {
                    NavigationBar(
                        color = outerBarColor
                    ) {
                        navItems.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                icon = item.icon,
                                label = item.label
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            pagerContent(innerPadding.calculateBottomPadding())
        }
    }

    if (showDriverStatusDialog) {
        DriverStatusDialog(
            driverStatus = driverStatus,
            onDismiss = { showDriverStatusDialog = false }
        )
    }

    if (showPresetDialog) {
        PresetDialog(
            presets = presets,
            onSave = { name -> viewModel.savePreset(name) },
            onLoad = { id ->
                viewModel.loadPreset(id)
                showPresetDialog = false
            },
            onDelete = { id -> viewModel.deletePreset(id) },
            onRename = { id, newName -> viewModel.renamePreset(id, newName) },
            onUpdate = { id -> viewModel.updatePreset(id) },
            onClearAll = {
                viewModel.clearAllPresets(
                    notificationTitle = context.getString(R.string.preset_clear_all_progress),
                    successStr = context.getString(R.string.preset_cleared),
                ) { deleted ->
                    platform.showToast("${context.getString(R.string.preset_cleared)}: $deleted")
                }
            },
            onDismiss = { showPresetDialog = false }
        )
    }

    if (showDeviceDialog) {
        DeviceDialog(
            devices = deviceSettings,
            activeDeviceId = state.activeDeviceId,
            onRename = { deviceId, newName -> viewModel.renameDevice(deviceId, newName) },
            onLoad = { deviceId -> viewModel.loadDevicePreset(deviceId) },
            onUpdate = { deviceId -> viewModel.saveDevicePreset(deviceId) },
            onDelete = { deviceId -> viewModel.deleteDeviceSettings(deviceId) },
            onDismiss = { showDeviceDialog = false }
        )
    }

    val releaseInfo = updateState.release
    if (releaseInfo != null) {
        val noAssetStr = stringResource(R.string.update_no_apk_asset)
        val appVersionName = remember(context) {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
            } catch (_: Exception) {
                ""
            }
        }
        UpdateDialog(
            release = releaseInfo,
            currentVersion = appVersionName,
            upToDate = updateState.upToDate,
            downloading = updateState.downloading,
            downloadProgress = updateState.downloadProgress,
            onDownloadInstall = {
                viewModel.downloadAndInstall(
                    release = releaseInfo,
                    onNoAsset = { platform.showToast(noAssetStr) }
                )
            },
            onViewOnGithub = {
                val intent = Intent(Intent.ACTION_VIEW, releaseInfo.htmlUrl.toUri())
                context.startActivity(intent)
                viewModel.dismissUpdate()
            },
            onDismiss = { viewModel.dismissUpdate() }
        )
    }

    val updateError = updateState.error
    if (updateError != null) {
        val errorMsg = stringResource(R.string.update_check_failed, updateError)
        LaunchedEffect(updateError) {
            platform.showToast(errorMsg)
            viewModel.dismissUpdate()
        }
    }
}

@Composable
private fun OverviewTab(
    state: EffectState,
    driverStatus: DriverStatus,
    viewModel: MainViewModel,
    bottomPadding: Dp,
    onJumpToEffects: () -> Unit,
    onShowDriverDialog: () -> Unit,
    showDriverDialog: Boolean,
    onShowDeviceDialog: () -> Unit,
    showDeviceDialog: Boolean,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    val isSpeaker = state.activeDeviceId == AudioDevice.ID_SPEAKER
    val enabledEffects = remember(state) { collectEnabledEffectNames(state) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.app_name),
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    actions = {
                        IconButton(
                            onClick = onShowDriverDialog,
                            minHeight = 35.dp,
                            minWidth = 35.dp,
                            backgroundColor = MiuixTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Info,
                                contentDescription = stringResource(R.string.menu_driver_status),
                                tint = MiuixTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        WideContentBox { sidePadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = bottomPadding,
                    start = sidePadding,
                    end = sidePadding
                )
            ) {
                item(key = "overview_top_spacer") {
                    Spacer(Modifier.height(12.dp))
                }

                groupedCardItems(
                    keyPrefix = "overview_master",
                    items = listOf(
                        CardItem("master_switch") {
                            SwitchPreference(
                                title = stringResource(R.string.master_enable),
                                summary = if (state.masterEnable) {
                                    stringResource(R.string.overview_master_summary_on)
                                } else {
                                    stringResource(R.string.overview_master_summary_off)
                                },
                                checked = state.masterEnable,
                                onCheckedChange = viewModel::setMasterEnabled
                            )
                        },
                        CardItem("current_device") {
                            ArrowPreference(
                                title = stringResource(R.string.overview_current_device),
                                summary = if (isSpeaker) {
                                    stringResource(R.string.device_mode_speaker)
                                } else {
                                    stringResource(R.string.device_mode_headphone)
                                },
                                endActions = {
                                    StatusBadge(
                                        text = state.activeDeviceName.ifEmpty {
                                            if (isSpeaker) stringResource(R.string.device_type_speaker)
                                            else stringResource(R.string.device_mode_headphone)
                                        },
                                        color = MiuixTheme.colorScheme.primary
                                    )
                                },
                                holdDownState = showDeviceDialog,
                                onClick = onShowDeviceDialog
                            )
                        }
                    )
                )

                item(key = "overview_driver_title") {
                    SmallTitle(text = stringResource(R.string.menu_driver_status))
                }

                groupedCardItems(
                    keyPrefix = "overview_driver",
                    items = listOf(
                        CardItem("driver_status_state") {
                            val isActive = driverStatus.installed && driverStatus.streaming
                            BasicComponent(
                                title = stringResource(R.string.driver_streaming),
                                summary = if (driverStatus.installed) {
                                    driverStatus.versionName
                                } else {
                                    stringResource(R.string.driver_not_found)
                                },
                                endActions = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(StatusColors.runState(isActive))
                                        )
                                        StatusBadge(
                                            text = if (driverStatus.streaming) {
                                                stringResource(R.string.status_active)
                                            } else {
                                                stringResource(R.string.status_inactive)
                                            },
                                            color = StatusColors.runState(isActive)
                                        )
                                    }
                                },
                                holdDownState = showDriverDialog,
                                onClick = onShowDriverDialog
                            )
                        },
                        CardItem("driver_sampling_rate") {
                            BasicComponent(
                                title = stringResource(R.string.driver_sampling_rate),
                                endActions = {
                                    Text(
                                        text = if (driverStatus.samplingRate > 0) "${driverStatus.samplingRate} Hz" else "--",
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                    )
                                }
                            )
                        },
                        CardItem("driver_architecture") {
                            BasicComponent(
                                title = stringResource(R.string.driver_architecture),
                                endActions = {
                                    StatusBadge(
                                        text = driverStatus.architecture.ifEmpty {
                                            stringResource(R.string.status_unknown)
                                        },
                                        color = StatusColors.neutral
                                    )
                                }
                            )
                        }
                    )
                )

                item(key = "overview_active_effects_title") {
                    SmallTitle(
                        text = stringResource(
                            R.string.effects_enabled_count,
                            enabledEffects.size
                        )
                    )
                }

                if (enabledEffects.isEmpty()) {
                    groupedCardItems(
                        keyPrefix = "overview_no_effects",
                        items = listOf(
                            CardItem("empty") {
                                ArrowPreference(
                                    title = stringResource(R.string.tab_effects),
                                    summary = stringResource(R.string.status_inactive),
                                    onClick = onJumpToEffects
                                )
                            }
                        )
                    )
                } else {
                    groupedCardItems(
                        keyPrefix = "overview_active_effects",
                        items = enabledEffects.mapIndexed { index, effectResId ->
                            CardItem("active_$index") {
                                ArrowPreference(
                                    title = stringResource(effectResId),
                                    endActions = {
                                        StatusBadge(
                                            text = stringResource(R.string.status_active),
                                            color = StatusColors.healthy
                                        )
                                    },
                                    onClick = onJumpToEffects
                                )
                            }
                        }
                    )
                }

                item(key = "overview_bottom_spacer") {
                    Spacer(Modifier.height(24.dp).navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun EffectsTab(
    state: EffectState,
    viewModel: MainViewModel,
    expandController: EffectExpandController,
    bottomPadding: Dp,
    onOpenEqScreen: () -> Unit,
    onShowDriverDialog: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val isSpeaker = state.activeDeviceId == AudioDevice.ID_SPEAKER
    val allCollapsed = expandController.isAllCollapsed

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.tab_effects),
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (allCollapsed) {
                                        expandController.expandAll(scope)
                                    } else {
                                        expandController.collapseAll(scope)
                                    }
                                },
                                minHeight = 35.dp,
                                minWidth = 35.dp,
                                backgroundColor = MiuixTheme.colorScheme.secondaryContainer
                            ) {
                                Icon(
                                    imageVector = if (allCollapsed) MiuixIcons.ExpandMore else MiuixIcons.ExpandLess,
                                    contentDescription = if (allCollapsed) {
                                        stringResource(R.string.common_expand_all)
                                    } else {
                                        stringResource(R.string.common_collapse_all)
                                    },
                                    tint = MiuixTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            IconButton(
                                onClick = onShowDriverDialog,
                                minHeight = 35.dp,
                                minWidth = 35.dp,
                                backgroundColor = MiuixTheme.colorScheme.secondaryContainer
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Info,
                                    contentDescription = stringResource(R.string.menu_driver_status),
                                    tint = MiuixTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        WideContentBox { sidePadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = bottomPadding,
                    start = sidePadding,
                    end = sidePadding
                )
            ) {
                item(key = "effects_top_spacer") {
                    Spacer(Modifier.height(12.dp))
                }

                groupedCardItems(
                    keyPrefix = "effects_master_bar",
                    items = listOf(
                        CardItem("master_switch") {
                            SwitchPreference(
                                title = stringResource(R.string.master_enable),
                                summary = state.activeDeviceName.ifEmpty {
                                    if (isSpeaker) stringResource(R.string.device_mode_speaker)
                                    else stringResource(R.string.device_mode_headphone)
                                },
                                checked = state.masterEnable,
                                onCheckedChange = viewModel::setMasterEnabled
                            )
                        }
                    )
                )

                allEffectSectionsItems(
                    state = state,
                    viewModel = viewModel,
                    controller = expandController,
                    scope = scope,
                    onOpenEqScreen = onOpenEqScreen
                )

                item(key = "effects_bottom_spacer") {
                    Spacer(Modifier.height(24.dp).navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun PresetsAndDevicesTab(
    presets: List<Preset>,
    deviceSettings: List<DeviceSettings>,
    currentDeviceId: String,
    currentDeviceName: String,
    viewModel: MainViewModel,
    platform: PlatformToast,
    bottomPadding: Dp,
    showPresetDialog: Boolean,
    onShowPresetDialog: () -> Unit,
    showDeviceDialog: Boolean,
    onShowDeviceDialog: () -> Unit,
    onImportPreset: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val context = LocalContext.current

    var showSaveDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.tab_preset_device),
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    actions = {
                        IconButton(
                            onClick = { showSaveDialog = true },
                            minHeight = 35.dp,
                            minWidth = 35.dp,
                            backgroundColor = MiuixTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Add,
                                contentDescription = stringResource(R.string.preset_save_current),
                                tint = MiuixTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        WideContentBox { sidePadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = bottomPadding,
                    start = sidePadding,
                    end = sidePadding
                )
            ) {
                item(key = "presets_section_title") {
                    SmallTitle(text = stringResource(R.string.preset_actions_title))
                }

                groupedCardItems(
                    keyPrefix = "preset_actions",
                    items = listOf(
                        CardItem("save_current") {
                            ArrowPreference(
                                title = stringResource(R.string.preset_save_current),
                                holdDownState = showSaveDialog,
                                onClick = { showSaveDialog = true }
                            )
                        },
                        CardItem("manage_presets") {
                            ArrowPreference(
                                title = stringResource(R.string.menu_presets),
                                endActions = {
                                    StatusBadge(
                                        text = presets.size.toString(),
                                        color = MiuixTheme.colorScheme.primary
                                    )
                                },
                                holdDownState = showPresetDialog,
                                onClick = onShowPresetDialog
                            )
                        },
                        CardItem("import_preset") {
                            ArrowPreference(
                                title = stringResource(R.string.settings_import_preset),
                                onClick = onImportPreset
                            )
                        }
                    )
                )

                if (presets.isNotEmpty()) {
                    item(key = "saved_presets_title") {
                        SmallTitle(text = stringResource(R.string.preset_section_title))
                    }

                    groupedCardItems(
                        keyPrefix = "saved_presets_list",
                        items = presets.map { preset ->
                            CardItem("preset_${preset.id}") {
                                ArrowPreference(
                                    title = preset.name,
                                    summary = stringResource(R.string.preset_tap_for_details),
                                    onClick = {
                                        viewModel.loadPreset(preset.id)
                                        platform.showToast(
                                            context.getString(R.string.preset_loaded_toast, preset.name)
                                        )
                                    }
                                )
                            }
                        }
                    )
                }

                item(key = "devices_section_title") {
                    SmallTitle(text = stringResource(R.string.device_section_title))
                }

                groupedCardItems(
                    keyPrefix = "device_actions",
                    items = buildList {
                        add(
                            CardItem("manage_devices") {
                                ArrowPreference(
                                    title = stringResource(R.string.menu_devices),
                                    summary = currentDeviceName,
                                    endActions = {
                                        StatusBadge(
                                            text = deviceSettings.size.toString(),
                                            color = StatusColors.neutral
                                        )
                                    },
                                    holdDownState = showDeviceDialog,
                                    onClick = onShowDeviceDialog
                                )
                            }
                        )
                        deviceSettings.forEach { device ->
                            val isCurrent = device.deviceId == currentDeviceId
                            val displayName = if (device.deviceId == AudioDevice.ID_SPEAKER && device.deviceName.isEmpty()) {
                                null
                            } else {
                                device.deviceName.ifEmpty { device.deviceId }
                            }
                            add(
                                CardItem("dev_${device.deviceId}") {
                                    BasicComponent(
                                        title = displayName ?: stringResource(R.string.device_type_speaker),
                                        summary = device.deviceId,
                                        endActions = {
                                            if (isCurrent) {
                                                StatusBadge(
                                                    text = stringResource(R.string.status_active),
                                                    color = StatusColors.healthy
                                                )
                                            }
                                        },
                                        onClick = onShowDeviceDialog
                                    )
                                }
                            )
                        }
                    }
                )

                item(key = "presets_bottom_spacer") {
                    Spacer(Modifier.height(24.dp).navigationBarsPadding())
                }
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
                viewModel.savePreset(name)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false }
        )
    }
}

@Composable
private fun SettingsTab(
    driverStatus: DriverStatus,
    autoStart: Boolean,
    globalMode: Boolean,
    aidlMode: Boolean,
    debugMode: Boolean,
    excludedApps: Set<String>,
    updateState: UpdateState,
    viewModel: MainViewModel,
    platform: PlatformToast,
    bottomPadding: Dp,
    onOpenExcludedApps: () -> Unit,
    onOpenDebugLog: () -> Unit,
    onImportPreset: () -> Unit,
    onImportKernel: () -> Unit,
    onImportVdc: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var showDebugDisableConfirm by rememberSaveable { mutableStateOf(false) }

    val isCheckingUpdate = updateState.checking
    val appVersionName = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.menu_settings),
                    scrollBehavior = scrollBehavior,
                    color = barColor
                )
            }
        }
    ) { innerPadding ->
        WideContentBox { sidePadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = bottomPadding,
                    start = sidePadding,
                    end = sidePadding
                )
            ) {
                item(key = "settings_general_title") {
                    SmallTitle(text = stringResource(R.string.settings_general_section))
                }

                groupedCardItems(
                    keyPrefix = "settings_general",
                    items = buildList {
                        add(
                            CardItem("auto_start") {
                                SwitchPreference(
                                    title = stringResource(R.string.settings_auto_start),
                                    summary = stringResource(R.string.settings_auto_start_desc),
                                    checked = autoStart,
                                    onCheckedChange = viewModel::toggleAutoStart
                                )
                            }
                        )
                        add(
                            CardItem("global_mode") {
                                SwitchPreference(
                                    title = stringResource(R.string.settings_global_mode),
                                    summary = stringResource(R.string.settings_global_mode_desc),
                                    checked = globalMode,
                                    onCheckedChange = { enabled ->
                                        if (enabled) {
                                            scope.launch {
                                                val granted = withContext(Dispatchers.IO) {
                                                    RootShell.isRootAvailable()
                                                }
                                                if (granted) {
                                                    viewModel.toggleGlobalMode(true)
                                                }
                                            }
                                        } else {
                                            viewModel.toggleGlobalMode(false)
                                        }
                                    }
                                )
                            }
                        )
                        if (!globalMode) {
                            add(
                                CardItem("session_exclude") {
                                    ArrowPreference(
                                        title = stringResource(R.string.settings_excluded_apps),
                                        summary = stringResource(R.string.settings_excluded_apps_desc),
                                        endActions = {
                                            if (excludedApps.isNotEmpty()) {
                                                StatusBadge(
                                                    text = excludedApps.size.toString(),
                                                    color = MiuixTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = onOpenExcludedApps
                                    )
                                }
                            )
                        }
                        if (debugMode) {
                            add(
                                CardItem("debug_log") {
                                    ArrowPreference(
                                        title = stringResource(R.string.debug_log_title),
                                        summary = stringResource(R.string.debug_mode_enabled),
                                        onClick = onOpenDebugLog
                                    )
                                }
                            )
                        }
                    }
                )

                item(key = "settings_import_title") {
                    SmallTitle(text = stringResource(R.string.settings_files_section))
                }

                groupedCardItems(
                    keyPrefix = "settings_import",
                    items = listOf(
                        CardItem("import_preset") {
                            ArrowPreference(
                                title = stringResource(R.string.settings_import_preset),
                                summary = stringResource(R.string.settings_import_preset_desc),
                                onClick = onImportPreset
                            )
                        },
                        CardItem("import_kernel") {
                            ArrowPreference(
                                title = stringResource(R.string.settings_import_kernel),
                                summary = stringResource(R.string.settings_import_kernel_desc),
                                onClick = onImportKernel
                            )
                        },
                        CardItem("import_vdc") {
                            ArrowPreference(
                                title = stringResource(R.string.settings_import_vdc),
                                summary = stringResource(R.string.settings_import_vdc_desc),
                                onClick = onImportVdc
                            )
                        }
                    )
                )

                item(key = "settings_about_title") {
                    SmallTitle(text = stringResource(R.string.settings_about_section))
                }

                groupedCardItems(
                    keyPrefix = "settings_about",
                    items = listOf(
                        CardItem("driver_version") {
                            BasicComponent(
                                title = stringResource(R.string.settings_driver_version),
                                endActions = {
                                    Text(
                                        text = if (driverStatus.installed) driverStatus.versionName else "—",
                                        fontSize = 14.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                    )
                                },
                                onClick = {
                                    if (debugMode) {
                                        showDebugDisableConfirm = true
                                    } else {
                                        val now = System.currentTimeMillis()
                                        if (now - lastTapTime > 2000L) tapCount = 0
                                        lastTapTime = now
                                        tapCount++
                                        if (tapCount >= 7) {
                                            tapCount = 0
                                            viewModel.enableDebugMode()
                                            platform.showToast(
                                                context.getString(R.string.debug_mode_enabled)
                                            )
                                        }
                                    }
                                }
                            )
                        },
                        CardItem("driver_arch") {
                            BasicComponent(
                                title = stringResource(R.string.settings_driver_arch),
                                endActions = {
                                    Text(
                                        text = if (driverStatus.installed) {
                                            driverStatus.architecture
                                        } else {
                                            stringResource(R.string.status_unknown)
                                        },
                                        fontSize = 14.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                    )
                                }
                            )
                        },
                        CardItem("aidl_mode") {
                            BasicComponent(
                                title = stringResource(R.string.settings_aidl_mode),
                                endActions = {
                                    Text(
                                        text = if (aidlMode) {
                                            stringResource(R.string.status_active)
                                        } else {
                                            stringResource(R.string.status_inactive)
                                        },
                                        fontSize = 14.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                    )
                                }
                            )
                        },
                        CardItem("app_version") {
                            ArrowPreference(
                                title = stringResource(R.string.settings_app_version),
                                endActions = {
                                    Text(
                                        text = appVersionName,
                                        fontSize = 14.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                    )
                                },
                                onClick = {
                                    if (!isCheckingUpdate) {
                                        viewModel.checkForUpdate()
                                    }
                                }
                            )
                        }
                    )
                )

                item(key = "settings_bottom_spacer") {
                    Spacer(Modifier.height(24.dp).navigationBarsPadding())
                }
            }
        }
    }

    if (showDebugDisableConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.debug_disable_debug),
            body = stringResource(R.string.debug_disable_debug),
            confirmLabel = stringResource(R.string.common_confirm),
            destructive = true,
            onConfirm = {
                viewModel.disableDebugMode()
                showDebugDisableConfirm = false
            },
            onDismiss = { showDebugDisableConfirm = false }
        )
    }
}

@Composable
private fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun collectEnabledEffectNames(state: EffectState): List<Int> {
    return buildList {
        if (state.playbackGainControl.enable) add(R.string.section_agc)
        if (state.lufs.enable) add(R.string.section_lufs_targeting)
        if (state.fetCompressor.enable) add(R.string.section_fet_compressor)
        if (state.multibandCompressor.enable) add(R.string.section_multiband_compressor)
        if (state.ddc.enable) add(R.string.section_ddc)
        if (state.spectrumExtension.enable) add(R.string.section_spectrum_extension)
        if (state.eq.enable) add(R.string.section_equalizer)
        if (state.dynamicEq.enable) add(R.string.section_dynamic_eq)
        if (state.convolver.enable) add(R.string.section_convolver)
        if (state.fieldSurround.enable) add(R.string.section_field_surround)
        if (state.diffSurround.enable) add(R.string.section_diff_surround)
        if (state.stereoImager.enable) add(R.string.section_stereo_imager)
        if (state.headphoneSurround.enable) add(R.string.section_headphone_surround)
        if (state.reverb.enable) add(R.string.section_reverb)
        if (state.dynamicSystem.enable) add(R.string.section_dynamic_system)
        if (state.tubeSimulator.enable) add(R.string.section_tube_simulator)
        if (state.psychoacousticBass.enable) add(R.string.section_psycho_bass)
        if (state.bass.enable) add(R.string.section_viper_bass)
        if (state.bassMono.enable) add(R.string.section_viper_bass_mono)
        if (state.clarity.enable) add(R.string.section_viper_clarity)
        if (state.cure.enable) add(R.string.section_cure)
        if (state.analogX.enable) add(R.string.section_analogx)
        if (state.speakerCorrection.enable) add(R.string.section_speaker_optimization)
    }
}
