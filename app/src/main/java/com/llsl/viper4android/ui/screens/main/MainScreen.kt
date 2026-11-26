package com.llsl.viper4android.ui.screens.main

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BlurCircular
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CandlestickChart
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LayersClear
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SettingsInputComponent
import androidx.compose.material.icons.rounded.SpatialAudio
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SpeakerPhone
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerticalAlignCenter
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.llsl.viper4android.R
import com.llsl.viper4android.audio.AudioDevice
import com.llsl.viper4android.audio.AudioSessionInfo
import com.llsl.viper4android.audio.AudioSessionMonitor
import com.llsl.viper4android.effect.BassState
import com.llsl.viper4android.effect.ClarityState
import com.llsl.viper4android.effect.ConvolverState
import com.llsl.viper4android.effect.EffectState
import com.llsl.viper4android.effect.EqState
import com.llsl.viper4android.ui.components.AdaptiveTopAppBar
import com.llsl.viper4android.ui.components.BlurredBar
import com.llsl.viper4android.ui.components.CardItem
import com.llsl.viper4android.ui.components.DriverStatusCard
import com.llsl.viper4android.ui.components.StatusBadge
import com.llsl.viper4android.ui.components.groupedCardItems
import com.llsl.viper4android.ui.components.layerBackdrop
import com.llsl.viper4android.ui.components.rememberBlurBackdrop
import com.llsl.viper4android.ui.screens.status.DriverStatusDialog
import com.llsl.viper4android.ui.theme.StatusColors
import com.llsl.viper4android.ui.theme.ViperTheme
import com.llsl.viper4android.ui.util.ImmutableList
import com.llsl.viper4android.ui.util.WideContentBox
import com.llsl.viper4android.ui.util.rememberIsWideScreen
import com.llsl.viper4android.ui.util.rememberPlatformToast
import com.llsl.viper4android.ui.util.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onOpenEqScreen: () -> Unit = {},
    onOpenPresets: () -> Unit = {},
    onOpenDevices: () -> Unit = {},
    onOpenSessions: () -> Unit = {},
    onOpenExcludedApps: () -> Unit = {},
    onOpenKernelFiles: () -> Unit = {},
    onOpenVdcFiles: () -> Unit = {},
    onOpenDebugLog: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val driverStatus by viewModel.driverStatus.collectAsStateWithLifecycle()
    val autoStart by viewModel.autoStartEnabled.collectAsStateWithLifecycle()
    val persistentNotification by viewModel.persistentNotificationEnabled.collectAsStateWithLifecycle()
    val globalMode by viewModel.globalModeEnabled.collectAsStateWithLifecycle()
    val aidlMode by viewModel.aidlModeEnabled.collectAsStateWithLifecycle()
    val debugMode by viewModel.debugModeEnabled.collectAsStateWithLifecycle()
    val excludedApps by viewModel.excludedApps.collectAsStateWithLifecycle()
    val activeSessions by viewModel.activeSessions.collectAsStateWithLifecycle()
    val kernelFileCount by viewModel.kernelFileList.collectAsStateWithLifecycle()
    val vdcFileCount by viewModel.vdcFileList.collectAsStateWithLifecycle()

    val globalModeFallbackNotice by viewModel.globalModeFallbackNotice.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.saveSettingsOnBackground()
    }

    val context = LocalContext.current
    val platform = rememberPlatformToast()
    val scope = rememberCoroutineScope()
    var checkingSessionPermission by remember { mutableStateOf(false) }

    val globalModeFallbackToast = stringResource(R.string.settings_global_mode_fallback_toast)
    val sessionModeRequiresRootToast = stringResource(R.string.settings_session_mode_requires_root)
    val notificationPermissionDeniedToast = stringResource(R.string.settings_notification_permission_denied)

    LaunchedEffect(globalModeFallbackNotice) {
        if (globalModeFallbackNotice) {
            viewModel.consumeGlobalModeFallbackNotice()
            platform.showToast(globalModeFallbackToast, long = true)
        }
    }

    val hasNotificationPermission = {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted && NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                viewModel.togglePersistentNotification(true)
            } else {
                platform.showToast(notificationPermissionDeniedToast, long = false)
            }
        }

    // 返回前台时若系统通知权限已被外部撤销，同步关闭开关状态
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (persistentNotification && !hasNotificationPermission()) {
            viewModel.togglePersistentNotification(false)
        }
    }

    val appVersionName = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    MainScreenContent(
        state = state,
        driverStatus = driverStatus,
        autoStart = autoStart,
        persistentNotification = persistentNotification,
        globalMode = globalMode,
        aidlMode = aidlMode,
        debugMode = debugMode,
        excludedApps = excludedApps,
        activeSessions = activeSessions,
        kernelFileCount = kernelFileCount.size,
        vdcFileCount = vdcFileCount.size,
        appVersionName = appVersionName,
        effectActions = viewModel,
        onPollDriverStatus = {
            while (true) {
                viewModel.queryDriverStatus()
                delay(500L)
            }
        },
        onToggleAutoStart = viewModel::toggleAutoStart,
        onTogglePersistentNotification = { enabled ->
            if (!enabled) {
                viewModel.togglePersistentNotification(false)
            } else {
                val permGranted =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                when {
                    !permGranted -> {
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS,
                        )
                    }
                    !NotificationManagerCompat.from(context).areNotificationsEnabled() -> {
                        platform.showToast(
                            notificationPermissionDeniedToast,
                            long = false,
                        )
                    }
                    else -> {
                        viewModel.togglePersistentNotification(true)
                    }
                }
            }
        },
        onToggleGlobalMode = { enabled ->
            if (enabled) {
                // 全局模式通过 session 0 生效，无需 Root 或特权权限
                viewModel.toggleGlobalMode(true)
            } else if (!checkingSessionPermission) {
                // 关闭全局模式切回会话模式时，需具备特权应用权限或 Root 权限
                checkingSessionPermission = true
                scope.launch {
                    val granted = try {
                        withContext(Dispatchers.IO) {
                            AudioSessionMonitor.canUseSessionMode(context)
                        }
                    } finally {
                        checkingSessionPermission = false
                    }
                    if (granted) {
                        viewModel.toggleGlobalMode(false)
                    } else {
                        platform.showToast(sessionModeRequiresRootToast, long = false)
                    }
                }
            }
        },
        onToggleDebugMode = { enabled ->
            if (enabled) {
                viewModel.enableDebugMode()
            } else {
                viewModel.disableDebugMode()
            }
        },
        onOpenEqScreen = onOpenEqScreen,
        onOpenPresets = onOpenPresets,
        onOpenDevices = onOpenDevices,
        onOpenSessions = onOpenSessions,
        onOpenExcludedApps = onOpenExcludedApps,
        onOpenKernelFiles = onOpenKernelFiles,
        onOpenVdcFiles = onOpenVdcFiles,
        onOpenDebugLog = onOpenDebugLog,
        onOpenAbout = onOpenAbout,
    )
}

@Composable
private fun MainScreenContent(
    state: EffectState,
    driverStatus: DriverStatus,
    autoStart: Boolean,
    persistentNotification: Boolean,
    globalMode: Boolean,
    aidlMode: Boolean,
    debugMode: Boolean,
    excludedApps: Set<String>,
    activeSessions: ImmutableList<AudioSessionInfo>,
    kernelFileCount: Int,
    vdcFileCount: Int,
    appVersionName: String,
    effectActions: EffectActions,
    onPollDriverStatus: (suspend () -> Unit)? = null,
    onToggleAutoStart: (Boolean) -> Unit = {},
    onTogglePersistentNotification: (Boolean) -> Unit = {},
    onToggleGlobalMode: (Boolean) -> Unit = {},
    onToggleDebugMode: (Boolean) -> Unit = {},
    onOpenEqScreen: () -> Unit = {},
    onOpenPresets: () -> Unit = {},
    onOpenDevices: () -> Unit = {},
    onOpenSessions: () -> Unit = {},
    onOpenExcludedApps: () -> Unit = {},
    onOpenKernelFiles: () -> Unit = {},
    onOpenVdcFiles: () -> Unit = {},
    onOpenDebugLog: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    initialPage: Int = 0,
) {
    var showDriverStatusDialog by rememberSaveable { mutableStateOf(false) }
    var pendingEffectJump by remember { mutableStateOf<EffectJumpRequest?>(null) }
    var effectJumpCounter by remember { mutableLongStateOf(0L) }

    val scope = rememberCoroutineScope()
    val expandController = rememberEffectExpandController()

    val isWideScreen = rememberIsWideScreen()
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 3 })
    val railState = rememberNavigationRailState()

    val outerBackdrop = rememberBlurBackdrop()
    val outerBlurActive = outerBackdrop != null
    val outerBarColor = if (outerBlurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    LaunchedEffect(pagerState.currentPage, showDriverStatusDialog, onPollDriverStatus) {
        if (onPollDriverStatus != null && (pagerState.currentPage == 0 || showDriverStatusDialog)) {
            onPollDriverStatus()
        }
    }

    // 与 KernelSU 对齐：非首 Tab 时返回先平滑回到概览 Tab，位于首 Tab 时交还系统预测性返回
    val pagerBackState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = pagerBackState,
        isBackEnabled = pagerState.currentPage != 0,
        onBackCompleted = {
            scope.launch { pagerState.animateScrollToPage(0) }
        }
    )

    val navItems = listOf(
        NavigationItem(
            label = stringResource(R.string.tab_overview),
            icon = MiuixIcons.Home
        ),
        NavigationItem(
            label = stringResource(R.string.tab_effects),
            icon = MiuixIcons.Tune
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
                    aidlMode = aidlMode,
                    globalMode = globalMode,
                    activeSessions = activeSessions,
                    onSetMasterEnabled = effectActions::setMasterEnabled,
                    bottomPadding = bottomPadding,
                    onJumpToEffects = { sectionKey ->
                        if (sectionKey != null) {
                            effectJumpCounter += 1L
                            pendingEffectJump = EffectJumpRequest(sectionKey, effectJumpCounter)
                        }
                        scope.launch { pagerState.animateScrollToPage(1) }
                    },
                    onShowDriverDialog = { showDriverStatusDialog = true },
                    onOpenDevices = onOpenDevices,
                    onOpenSessions = onOpenSessions
                )

                1 -> EffectsTab(
                    state = state,
                    effectActions = effectActions,
                    expandController = expandController,
                    jumpRequest = pendingEffectJump,
                    bottomPadding = bottomPadding,
                    onOpenEqScreen = onOpenEqScreen,
                    onOpenPresets = onOpenPresets,
                    onShowDriverDialog = { showDriverStatusDialog = true },
                    showDriverStatusDialog = showDriverStatusDialog
                )

                2 -> SettingsTab(
                    autoStart = autoStart,
                    persistentNotification = persistentNotification,
                    globalMode = globalMode,
                    debugMode = debugMode,
                    excludedApps = excludedApps,
                    kernelFileCount = kernelFileCount,
                    vdcFileCount = vdcFileCount,
                    appVersionName = appVersionName,
                    onToggleAutoStart = onToggleAutoStart,
                    onTogglePersistentNotification = onTogglePersistentNotification,
                    onToggleGlobalMode = onToggleGlobalMode,
                    onToggleDebugMode = onToggleDebugMode,
                    bottomPadding = bottomPadding,
                    onOpenExcludedApps = onOpenExcludedApps,
                    onOpenKernelFiles = onOpenKernelFiles,
                    onOpenVdcFiles = onOpenVdcFiles,
                    onOpenDebugLog = onOpenDebugLog,
                    onOpenAbout = onOpenAbout
                )
            }
        }
    }

    val selectedPage = pagerState.targetPage
    val navigateToPage: (Int) -> Unit = { target ->
        if (target != pagerState.targetPage) {
            scope.launch { pagerState.animateScrollToPage(target) }
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
                            selected = selectedPage == index,
                            onClick = { navigateToPage(index) },
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
                                selected = selectedPage == index,
                                onClick = { navigateToPage(index) },
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
            aidlMode = aidlMode,
            globalMode = globalMode,
            onDismiss = { showDriverStatusDialog = false }
        )
    }
}

@Composable
private fun OverviewTab(
    state: EffectState,
    driverStatus: DriverStatus,
    aidlMode: Boolean,
    globalMode: Boolean,
    activeSessions: ImmutableList<AudioSessionInfo>,
    onSetMasterEnabled: (Boolean) -> Unit,
    bottomPadding: Dp,
    onJumpToEffects: (String?) -> Unit,
    onShowDriverDialog: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenSessions: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    val isSpeaker = state.activeDeviceId == AudioDevice.ID_SPEAKER
    val enabledEffects = remember(state) { collectEnabledEffects(state) }
    val enabledSessionCount = remember(activeSessions) { activeSessions.count { it.enabled } }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.app_name),
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
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
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

                item(key = "overview_driver_hero") {
                    DriverStatusCard(
                        driverStatus = driverStatus,
                        masterEnabled = state.masterEnable,
                        aidlMode = aidlMode,
                        globalMode = globalMode,
                        onClick = onShowDriverDialog,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    )
                }

                groupedCardItems(
                    keyPrefix = "overview_master",
                    outerBottomPadding = 6.dp,
                    items = buildList {
                        add(
                            CardItem("master_switch") {
                                SwitchPreference(
                                    title = stringResource(R.string.master_enable),
                                    summary = if (state.masterEnable) {
                                        stringResource(R.string.overview_master_summary_on)
                                    } else {
                                        stringResource(R.string.overview_master_summary_off)
                                    },
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Rounded.PowerSettingsNew,
                                            contentDescription = stringResource(R.string.master_enable),
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = MiuixTheme.colorScheme.onBackground
                                        )
                                    },
                                    checked = state.masterEnable,
                                    onCheckedChange = onSetMasterEnabled
                                )
                            }
                        )
                        add(
                            CardItem("current_device") {
                                ArrowPreference(
                                    title = stringResource(R.string.overview_current_device),
                                    summary = if (isSpeaker) {
                                        stringResource(R.string.device_mode_speaker)
                                    } else {
                                        stringResource(R.string.device_mode_headphone)
                                    },
                                    startAction = {
                                        Icon(
                                            imageVector = if (isSpeaker) Icons.Rounded.Speaker else Icons.Rounded.Headphones,
                                            contentDescription = stringResource(R.string.overview_current_device),
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = MiuixTheme.colorScheme.onBackground
                                        )
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
                                    onClick = onOpenDevices
                                )
                            }
                        )
                        if (!globalMode) {
                            add(
                                CardItem("active_sessions") {
                                    val sessionSummary = when {
                                        !state.masterEnable -> stringResource(R.string.overview_sessions_master_off_summary)
                                        activeSessions.isEmpty() -> stringResource(R.string.overview_sessions_empty_summary)
                                        enabledSessionCount == activeSessions.size ->
                                            stringResource(R.string.overview_sessions_processing_summary, enabledSessionCount)
                                        else ->
                                            stringResource(
                                                R.string.overview_sessions_active_summary,
                                                enabledSessionCount,
                                                activeSessions.size
                                            )
                                    }
                                    ArrowPreference(
                                        title = stringResource(R.string.overview_session_management),
                                        summary = sessionSummary,
                                        startAction = {
                                            Icon(
                                                imageVector = Icons.Rounded.GraphicEq,
                                                contentDescription = stringResource(R.string.overview_session_management),
                                                modifier = Modifier.padding(end = 6.dp),
                                                tint = MiuixTheme.colorScheme.onBackground
                                            )
                                        },
                                        endActions = {
                                            StatusBadge(
                                                text = enabledSessionCount.toString(),
                                                color = MiuixTheme.colorScheme.primary
                                            )
                                        },
                                        onClick = onOpenSessions
                                    )
                                }
                            )
                        }
                    }
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
                        outerBottomPadding = 12.dp,
                        items = listOf(
                            CardItem("empty") {
                                ArrowPreference(
                                    title = stringResource(R.string.tab_effects),
                                    summary = stringResource(R.string.status_inactive),
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Rounded.Tune,
                                            contentDescription = stringResource(R.string.tab_effects),
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = MiuixTheme.colorScheme.onBackground
                                        )
                                    },
                                    onClick = { onJumpToEffects(null) }
                                )
                            }
                        )
                    )
                } else {
                    groupedCardItems(
                        keyPrefix = "overview_active_effects",
                        outerBottomPadding = 12.dp,
                        items = enabledEffects.map { effect ->
                            CardItem("active_${effect.sectionKey}") {
                                ArrowPreference(
                                    title = stringResource(effect.titleRes),
                                    startAction = {
                                        Icon(
                                            imageVector = effect.icon,
                                            contentDescription = stringResource(effect.titleRes),
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = MiuixTheme.colorScheme.onBackground
                                        )
                                    },
                                    endActions = {
                                        StatusBadge(
                                            text = stringResource(R.string.status_active),
                                            color = StatusColors.healthy
                                        )
                                    },
                                    onClick = { onJumpToEffects(effect.sectionKey) }
                                )
                            }
                        }
                    )
                }

                item(key = "overview_bottom_spacer") {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun EffectsTab(
    state: EffectState,
    effectActions: EffectActions,
    expandController: EffectExpandController,
    jumpRequest: EffectJumpRequest?,
    bottomPadding: Dp,
    onOpenEqScreen: () -> Unit,
    onOpenPresets: () -> Unit,
    onShowDriverDialog: () -> Unit,
    showDriverStatusDialog: Boolean,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val sectionHeaderIndices = remember { mutableMapOf<String, Int>() }
    val scrollTopOffsetPx = with(LocalDensity.current) { 12.dp.roundToPx() }

    LaunchedEffect(jumpRequest) {
        val request = jumpRequest ?: return@LaunchedEffect
        expandController.expand(request.sectionKey, scope)
        // 等待一帧确保 LazyListScope 已更新各音效卡片的最新索引
        withFrameNanos {}
        val targetIndex = sectionHeaderIndices[request.sectionKey] ?: return@LaunchedEffect
        listState.animateScrollToItem(index = targetIndex, scrollOffset = -scrollTopOffsetPx)
    }

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
                    navigationIcon = {
                        IconButton(
                            onClick = onOpenPresets
                        ) {
                            Icon(
                                imageVector = MiuixIcons.ListView,
                                contentDescription = stringResource(R.string.menu_presets),
                                tint = MiuixTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (allCollapsed) {
                                    expandController.expandAll(scope)
                                } else {
                                    expandController.collapseAll(scope)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (allCollapsed) MiuixIcons.ExpandMore else MiuixIcons.ExpandLess,
                                contentDescription = if (allCollapsed) {
                                    stringResource(R.string.common_expand_all)
                                } else {
                                    stringResource(R.string.common_collapse_all)
                                },
                                tint = MiuixTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = onShowDriverDialog,
                            holdDownState = showDriverStatusDialog
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Info,
                                contentDescription = stringResource(R.string.menu_driver_status),
                                tint = MiuixTheme.colorScheme.onSurface
                            )
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
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
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
                    outerBottomPadding = 12.dp,
                    items = listOf(
                        CardItem("master_switch") {
                            SwitchPreference(
                                title = stringResource(R.string.master_enable),
                                summary = state.activeDeviceName.ifEmpty {
                                    if (isSpeaker) stringResource(R.string.device_mode_speaker)
                                    else stringResource(R.string.device_mode_headphone)
                                },
                                startAction = {
                                    Icon(
                                        imageVector = Icons.Rounded.PowerSettingsNew,
                                        contentDescription = stringResource(R.string.master_enable),
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = MiuixTheme.colorScheme.onBackground
                                    )
                                },
                                checked = state.masterEnable,
                                onCheckedChange = effectActions::setMasterEnabled
                            )
                        }
                    )
                )

                allEffectSectionsItems(
                    state = state,
                    viewModel = effectActions,
                    controller = expandController,
                    scope = scope,
                    onOpenEqScreen = onOpenEqScreen,
                    startIndex = 2,
                    onHeaderIndices = { indices ->
                        sectionHeaderIndices.clear()
                        sectionHeaderIndices.putAll(indices)
                    }
                )

                item(key = "effects_bottom_spacer") {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    autoStart: Boolean,
    persistentNotification: Boolean,
    globalMode: Boolean,
    debugMode: Boolean,
    excludedApps: Set<String>,
    kernelFileCount: Int,
    vdcFileCount: Int,
    appVersionName: String,
    onToggleAutoStart: (Boolean) -> Unit,
    onTogglePersistentNotification: (Boolean) -> Unit,
    onToggleGlobalMode: (Boolean) -> Unit,
    onToggleDebugMode: (Boolean) -> Unit,
    bottomPadding: Dp,
    onOpenExcludedApps: () -> Unit,
    onOpenKernelFiles: () -> Unit,
    onOpenVdcFiles: () -> Unit,
    onOpenDebugLog: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

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
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
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
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Rounded.RestartAlt,
                                            contentDescription = stringResource(R.string.settings_auto_start),
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = MiuixTheme.colorScheme.onBackground
                                        )
                                    },
                                    checked = autoStart,
                                    onCheckedChange = onToggleAutoStart
                                )
                            }
                        )
                        add(
                            CardItem("persistent_notification") {
                                SwitchPreference(
                                    title = stringResource(R.string.settings_persistent_notification),
                                    summary = stringResource(R.string.settings_persistent_notification_desc),
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Rounded.Notifications,
                                            contentDescription = stringResource(R.string.settings_persistent_notification),
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = MiuixTheme.colorScheme.onBackground
                                        )
                                    },
                                    checked = persistentNotification,
                                    onCheckedChange = onTogglePersistentNotification
                                )
                            }
                        )
                        add(
                            CardItem("global_mode") {
                                SwitchPreference(
                                    title = stringResource(R.string.settings_global_mode),
                                    summary = stringResource(R.string.settings_global_mode_desc),
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Rounded.Public,
                                            contentDescription = stringResource(R.string.settings_global_mode),
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = MiuixTheme.colorScheme.onBackground
                                        )
                                    },
                                    checked = globalMode,
                                    onCheckedChange = onToggleGlobalMode
                                )
                            }
                        )
                        if (!globalMode) {
                            add(
                                CardItem("session_exclude") {
                                    ArrowPreference(
                                        title = stringResource(R.string.settings_excluded_apps),
                                        summary = stringResource(R.string.settings_excluded_apps_desc),
                                        startAction = {
                                            Icon(
                                                imageVector = Icons.Rounded.LayersClear,
                                                contentDescription = stringResource(R.string.settings_excluded_apps),
                                                modifier = Modifier.padding(end = 6.dp),
                                                tint = MiuixTheme.colorScheme.onBackground
                                            )
                                        },
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
                        add(
                            CardItem("debug_mode") {
                                SwitchPreference(
                                    title = stringResource(R.string.settings_debug_mode),
                                    summary = stringResource(R.string.settings_debug_mode_desc),
                                    startAction = {
                                        Icon(
                                            imageVector = Icons.Rounded.BugReport,
                                            contentDescription = stringResource(R.string.settings_debug_mode),
                                            modifier = Modifier.padding(end = 6.dp),
                                            tint = MiuixTheme.colorScheme.onBackground
                                        )
                                    },
                                    checked = debugMode,
                                    onCheckedChange = onToggleDebugMode
                                )
                            }
                        )
                        if (debugMode) {
                            add(
                                CardItem("debug_log") {
                                    ArrowPreference(
                                        title = stringResource(R.string.debug_log_title),
                                        summary = stringResource(R.string.debug_mode_enabled),
                                        startAction = {
                                            Icon(
                                                imageVector = Icons.Rounded.Terminal,
                                                contentDescription = stringResource(R.string.debug_log_title),
                                                modifier = Modifier.padding(end = 6.dp),
                                                tint = MiuixTheme.colorScheme.onBackground
                                            )
                                        },
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
                    keyPrefix = "settings_files",
                    items = listOf(
                        CardItem("manage_kernels") {
                            ArrowPreference(
                                title = stringResource(R.string.files_kernel_title),
                                summary = stringResource(R.string.settings_import_kernel_desc),
                                startAction = {
                                    Icon(
                                        imageVector = Icons.Rounded.GraphicEq,
                                        contentDescription = stringResource(R.string.files_kernel_title),
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = MiuixTheme.colorScheme.onBackground
                                    )
                                },
                                endActions = {
                                    if (kernelFileCount > 0) {
                                        StatusBadge(
                                            text = kernelFileCount.toString(),
                                            color = MiuixTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = onOpenKernelFiles
                            )
                        },
                        CardItem("manage_vdcs") {
                            ArrowPreference(
                                title = stringResource(R.string.files_vdc_title),
                                summary = stringResource(R.string.settings_import_vdc_desc),
                                startAction = {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = stringResource(R.string.files_vdc_title),
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = MiuixTheme.colorScheme.onBackground
                                    )
                                },
                                endActions = {
                                    if (vdcFileCount > 0) {
                                        StatusBadge(
                                            text = vdcFileCount.toString(),
                                            color = MiuixTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = onOpenVdcFiles
                            )
                        }
                    )
                )

                item(key = "settings_about_title") {
                    SmallTitle(text = stringResource(R.string.settings_about_section))
                }

                groupedCardItems(
                    keyPrefix = "settings_about",
                    outerBottomPadding = 12.dp,
                    items = listOf(
                        CardItem("about") {
                            ArrowPreference(
                                title = stringResource(R.string.about_app_title),
                                startAction = {
                                    Icon(
                                        imageVector = MiuixIcons.Info,
                                        contentDescription = stringResource(R.string.about_app_title),
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = MiuixTheme.colorScheme.onBackground
                                    )
                                },
                                endActions = {
                                    Text(
                                        text = appVersionName,
                                        fontSize = 14.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                    )
                                },
                                onClick = onOpenAbout
                            )
                        }
                    )
                )

                item(key = "settings_bottom_spacer") {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Immutable
private data class EffectJumpRequest(
    val sectionKey: String,
    val id: Long,
)

@Immutable
private data class EnabledEffectItem(
    val sectionKey: String,
    val titleRes: Int,
    val icon: ImageVector,
)

private fun collectEnabledEffects(state: EffectState): List<EnabledEffectItem> {
    return buildList {
        if (state.playbackGainControl.enable) add(EnabledEffectItem("playbackGain", R.string.section_agc, Icons.AutoMirrored.Rounded.TrendingUp))
        if (state.lufs.enable) add(EnabledEffectItem("lufsTargeting", R.string.section_lufs_targeting, Icons.Rounded.VerticalAlignCenter))
        if (state.fetCompressor.enable) add(EnabledEffectItem("fetCompressor", R.string.section_fet_compressor, Icons.Rounded.Compress))
        if (state.multibandCompressor.enable) add(EnabledEffectItem("multibandComp", R.string.section_multiband_compressor, Icons.Rounded.SettingsInputComponent))
        if (state.ddc.enable) add(EnabledEffectItem("ddc", R.string.section_ddc, Icons.Rounded.SpeakerPhone))
        if (state.spectrumExtension.enable) add(EnabledEffectItem("vse", R.string.section_spectrum_extension, Icons.Rounded.Waves))
        if (state.eq.enable) add(EnabledEffectItem("eq", R.string.section_equalizer, Icons.Rounded.Equalizer))
        if (state.dynamicEq.enable) add(EnabledEffectItem("dynamicEq", R.string.section_dynamic_eq, Icons.Rounded.Insights))
        if (state.convolver.enable) add(EnabledEffectItem("convolver", R.string.section_convolver, Icons.Rounded.BlurCircular))
        if (state.fieldSurround.enable) add(EnabledEffectItem("fieldSurround", R.string.section_field_surround, Icons.Rounded.SurroundSound))
        if (state.diffSurround.enable) add(EnabledEffectItem("diffSurround", R.string.section_diff_surround, Icons.Rounded.SpatialAudio))
        if (state.stereoImager.enable) add(EnabledEffectItem("stereoImager", R.string.section_stereo_imager, Icons.Rounded.AspectRatio))
        if (state.headphoneSurround.enable) add(EnabledEffectItem("vhe", R.string.section_headphone_surround, Icons.Rounded.Headphones))
        if (state.reverb.enable) add(EnabledEffectItem("reverb", R.string.section_reverb, Icons.Rounded.BlurOn))
        if (state.dynamicSystem.enable) add(EnabledEffectItem("dynamicSystem", R.string.section_dynamic_system, Icons.Rounded.CandlestickChart))
        if (state.tubeSimulator.enable) add(EnabledEffectItem("tubeSimulator", R.string.section_tube_simulator, Icons.Rounded.MusicNote))
        if (state.psychoacousticBass.enable) add(EnabledEffectItem("psychoBass", R.string.section_psycho_bass, Icons.Rounded.Psychology))
        if (state.bass.enable) add(EnabledEffectItem("viperBass", R.string.section_viper_bass, Icons.Rounded.GraphicEq))
        if (state.bassMono.enable) add(EnabledEffectItem("viperBassMono", R.string.section_viper_bass_mono, Icons.Rounded.GraphicEq))
        if (state.clarity.enable) add(EnabledEffectItem("viperClarity", R.string.section_viper_clarity, Icons.Rounded.Hearing))
        if (state.cure.enable) add(EnabledEffectItem("cure", R.string.section_cure, Icons.Rounded.HealthAndSafety))
        if (state.analogX.enable) add(EnabledEffectItem("analogX", R.string.section_analogx, Icons.Rounded.Memory))
        if (state.speakerCorrection.enable) add(EnabledEffectItem("speakerOpt", R.string.section_speaker_optimization, Icons.Rounded.SpeakerPhone))
    }
}

private fun previewEffectState(): EffectState =
    EffectState(
        masterEnable = true,
        eq = EqState(
            enable = true,
            bandCount = 10,
            bands = listOf(4.5, 4.0, 2.5, 1.0, 0.5, 0.5, 1.5, 2.0, 3.5, 2.5),
        ),
        convolver = ConvolverState(
            enable = true,
            kernelFile = "Dolby_Atmos.irs",
        ),
        bass = BassState(enable = true, frequency = 60, gain = 0.6f),
        clarity = ClarityState(enable = true, mode = 0, gain = 0.5f),
        activeDeviceName = "WH-1000XM5",
        activeDeviceId = "AA:BB:CC:DD:EE:01",
    )

private fun previewDriverStatus(): DriverStatus =
    DriverStatus(
        installed = true,
        versionCode = 2504,
        versionName = "2.5.0.4",
        architecture = "arm64-v8a",
        streaming = true,
        samplingRate = 48000,
    )

private fun previewActiveSessions(): ImmutableList<AudioSessionInfo> =
    listOf(
        AudioSessionInfo(
            sessionId = 142,
            packageName = "com.netease.cloudmusic",
            enabled = true,
            openedAt = 1710000000000L,
            usage = AudioAttributes.USAGE_MEDIA,
        ),
        AudioSessionInfo(
            sessionId = 208,
            packageName = "com.google.android.youtube",
            enabled = true,
            openedAt = 1710000035000L,
            usage = AudioAttributes.USAGE_MEDIA,
        ),
    ).toPersistentList()

@Preview(name = "Main - Overview", showBackground = true)
@Composable
private fun MainOverviewPreview() {
    ViperTheme {
        MainScreenContent(
            state = previewEffectState(),
            driverStatus = previewDriverStatus(),
            autoStart = true,
            persistentNotification = true,
            globalMode = false,
            aidlMode = true,
            debugMode = true,
            excludedApps = setOf("com.spotify.music"),
            activeSessions = previewActiveSessions(),
            kernelFileCount = 3,
            vdcFileCount = 2,
            appVersionName = "2.1.2",
            effectActions = PreviewEffectActions,
            initialPage = 0,
        )
    }
}

@Preview(name = "Main - Effects", showBackground = true)
@Composable
private fun MainEffectsPreview() {
    ViperTheme {
        MainScreenContent(
            state = previewEffectState(),
            driverStatus = previewDriverStatus(),
            autoStart = true,
            persistentNotification = false,
            globalMode = false,
            aidlMode = true,
            debugMode = false,
            excludedApps = emptySet(),
            activeSessions = previewActiveSessions(),
            kernelFileCount = 3,
            vdcFileCount = 2,
            appVersionName = "2.1.2",
            effectActions = PreviewEffectActions,
            initialPage = 1,
        )
    }
}

@Preview(name = "Main - Settings", showBackground = true)
@Composable
private fun MainSettingsPreview() {
    ViperTheme {
        MainScreenContent(
            state = previewEffectState(),
            driverStatus = previewDriverStatus(),
            autoStart = true,
            persistentNotification = true,
            globalMode = false,
            aidlMode = true,
            debugMode = true,
            excludedApps = setOf("com.spotify.music", "com.miHoYo.Yuanshen"),
            activeSessions = previewActiveSessions(),
            kernelFileCount = 3,
            vdcFileCount = 2,
            appVersionName = "2.1.2",
            effectActions = PreviewEffectActions,
            initialPage = 2,
        )
    }
}

@Preview(name = "Main - Wide Screen", showBackground = true, widthDp = 840, heightDp = 640)
@Composable
private fun MainWideScreenPreview() {
    ViperTheme {
        MainScreenContent(
            state = previewEffectState(),
            driverStatus = previewDriverStatus(),
            autoStart = true,
            persistentNotification = true,
            globalMode = false,
            aidlMode = true,
            debugMode = true,
            excludedApps = setOf("com.spotify.music"),
            activeSessions = previewActiveSessions(),
            kernelFileCount = 3,
            vdcFileCount = 2,
            appVersionName = "2.1.2",
            effectActions = PreviewEffectActions,
            initialPage = 0,
        )
    }
}
