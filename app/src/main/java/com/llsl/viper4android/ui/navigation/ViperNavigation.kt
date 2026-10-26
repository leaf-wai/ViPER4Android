package com.llsl.viper4android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.llsl.viper4android.R
import com.llsl.viper4android.ui.screens.debug.DebugLogScreen
import com.llsl.viper4android.ui.screens.device.DevicesScreen
import com.llsl.viper4android.ui.screens.eq.EqEditScreen
import com.llsl.viper4android.ui.screens.main.MainScreen
import com.llsl.viper4android.ui.screens.main.MainViewModel
import com.llsl.viper4android.ui.screens.preset.PresetsScreen
import com.llsl.viper4android.ui.screens.settings.AudioFilesScreen
import com.llsl.viper4android.ui.screens.settings.ExcludedAppsScreen
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection

/** 应用路由键，基于 miuix-nav 实现与 KernelSU 一致的连续深度转场与预测性返回动画 */
private sealed interface ViperRoute : NavKey {
    val routeName: String

    data object Main : ViperRoute {
        override val routeName: String = "main"
    }

    data object Presets : ViperRoute {
        override val routeName: String = "presets"
    }

    data object Devices : ViperRoute {
        override val routeName: String = "devices"
    }

    data object EqEdit : ViperRoute {
        override val routeName: String = "eq_edit"
    }

    data object ExcludedApps : ViperRoute {
        override val routeName: String = "excluded_apps"
    }

    data object KernelFiles : ViperRoute {
        override val routeName: String = "kernel_files"
    }

    data object VdcFiles : ViperRoute {
        override val routeName: String = "vdc_files"
    }

    data object DebugLog : ViperRoute {
        override val routeName: String = "debug_log"
    }

    companion object {
        fun fromName(name: String): ViperRoute? = when (name) {
            Main.routeName -> Main
            Presets.routeName -> Presets
            Devices.routeName -> Devices
            EqEdit.routeName -> EqEdit
            ExcludedApps.routeName -> ExcludedApps
            KernelFiles.routeName -> KernelFiles
            VdcFiles.routeName -> VdcFiles
            DebugLog.routeName -> DebugLog
            else -> null
        }
    }
}

@Composable
private fun rememberViperBackStack(start: ViperRoute = ViperRoute.Main): NavBackStack {
    return rememberSaveable(
        saver = listSaver(
            save = { stack -> stack.mapNotNull { (it as? ViperRoute)?.routeName } },
            restore = { names ->
                val restored = names.mapNotNull(ViperRoute::fromName)
                (restored.ifEmpty { listOf(start) }).toMutableStateList()
            }
        )
    ) {
        listOf<NavKey>(start).toMutableStateList()
    }
}

@Composable
fun ViperNavigation(
    viewModel: MainViewModel = hiltViewModel()
) {
    val backStack = rememberViperBackStack(ViperRoute.Main)

    // 幂等入栈：防止双击重复压入同一路由导致 contentKey 冲突
    fun push(route: ViperRoute) {
        if (route !in backStack) {
            backStack.add(route)
        }
    }

    fun pop() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    val swipeDismiss = if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
        NavSwipeDirection.RightToLeft
    } else {
        NavSwipeDirection.LeftToRight
    }

    Scaffold {
        NavDisplay(
            backStack = backStack,
            effects = NavDisplayEffects(
                cornerClipRadius = rememberNavSystemCornerRadius()
            ),
            onBack = { pop() }
        ) {
            entry<ViperRoute.Main>(swipeDismiss = swipeDismiss) {
                MainScreen(
                    viewModel = viewModel,
                    onOpenEqScreen = { push(ViperRoute.EqEdit) },
                    onOpenPresets = { push(ViperRoute.Presets) },
                    onOpenDevices = { push(ViperRoute.Devices) },
                    onOpenExcludedApps = { push(ViperRoute.ExcludedApps) },
                    onOpenKernelFiles = { push(ViperRoute.KernelFiles) },
                    onOpenVdcFiles = { push(ViperRoute.VdcFiles) },
                    onOpenDebugLog = { push(ViperRoute.DebugLog) }
                )
            }
            entry<ViperRoute.Devices>(swipeDismiss = swipeDismiss) {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val deviceSettings by viewModel.deviceSettingsList.collectAsStateWithLifecycle()
                DevicesScreen(
                    devices = deviceSettings,
                    activeDeviceId = state.activeDeviceId,
                    onRename = viewModel::renameDevice,
                    onLoad = viewModel::loadDevicePreset,
                    onUpdate = viewModel::saveDevicePreset,
                    onDelete = viewModel::deleteDeviceSettings,
                    onDeleteDevices = viewModel::deleteDeviceSettingsBatch,
                    onBack = { pop() }
                )
            }
            entry<ViperRoute.Presets>(swipeDismiss = swipeDismiss) {
                val presets by viewModel.presetList.collectAsStateWithLifecycle()
                PresetsScreen(
                    presets = presets,
                    onSavePreset = viewModel::savePreset,
                    onLoadPreset = viewModel::loadPreset,
                    onDeletePreset = viewModel::deletePreset,
                    onDeletePresets = viewModel::deletePresets,
                    onRenamePreset = viewModel::renamePreset,
                    onUpdatePreset = viewModel::updatePreset,
                    onImportPresetFiles = viewModel::importPresetFiles,
                    onBack = { pop() }
                )
            }
            entry<ViperRoute.EqEdit>(swipeDismiss = swipeDismiss) {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                EqEditScreen(
                    bands = state.eq.bands.map { it.toFloat() },
                    bandCount = state.eq.bandCount,
                    presetId = state.eq.presetId,
                    presets = state.eq.presets,
                    onBandCountChange = viewModel::setEqBandCount,
                    onBandsChange = viewModel::setEqBands,
                    onPresetSelect = viewModel::setEqPreset,
                    onPresetAdd = viewModel::addEqPreset,
                    onPresetDelete = viewModel::deleteEqPreset,
                    onReset = viewModel::resetEqBands,
                    onBack = { pop() }
                )
            }
            entry<ViperRoute.ExcludedApps>(swipeDismiss = swipeDismiss) {
                val excludedApps by viewModel.excludedApps.collectAsStateWithLifecycle()
                ExcludedAppsScreen(
                    excludedApps = excludedApps,
                    onToggle = viewModel::setAppExcluded,
                    loadInstalledApps = viewModel::loadInstalledApps,
                    onBack = { pop() }
                )
            }
            entry<ViperRoute.KernelFiles>(swipeDismiss = swipeDismiss) {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val kernelFiles by viewModel.kernelFileItems.collectAsStateWithLifecycle()
                AudioFilesScreen(
                    title = stringResource(R.string.files_kernel_title),
                    importActionLabel = stringResource(R.string.settings_import_kernel),
                    emptyTitle = stringResource(R.string.files_kernel_empty),
                    emptyDescription = stringResource(R.string.files_kernel_empty_desc),
                    itemIcon = Icons.Rounded.GraphicEq,
                    files = kernelFiles,
                    activeFileName = state.convolver.kernelFile,
                    onImportFiles = viewModel::importKernels,
                    onSelectFile = viewModel::setConvolverKernel,
                    onRenameFile = viewModel::renameKernelFile,
                    onDeleteFile = viewModel::deleteKernelFile,
                    onDeleteFiles = viewModel::deleteKernelFiles,
                    onBack = { pop() }
                )
            }
            entry<ViperRoute.VdcFiles>(swipeDismiss = swipeDismiss) {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val vdcFiles by viewModel.vdcFileItems.collectAsStateWithLifecycle()
                AudioFilesScreen(
                    title = stringResource(R.string.files_vdc_title),
                    importActionLabel = stringResource(R.string.settings_import_vdc),
                    emptyTitle = stringResource(R.string.files_vdc_empty),
                    emptyDescription = stringResource(R.string.files_vdc_empty_desc),
                    itemIcon = Icons.Rounded.Tune,
                    files = vdcFiles,
                    activeFileName = state.ddc.device,
                    onImportFiles = viewModel::importVdcs,
                    onSelectFile = viewModel::setDdcDevice,
                    onRenameFile = viewModel::renameVdcFile,
                    onDeleteFile = viewModel::deleteVdcFile,
                    onDeleteFiles = viewModel::deleteVdcFiles,
                    onBack = { pop() }
                )
            }
            entry<ViperRoute.DebugLog>(swipeDismiss = swipeDismiss) {
                DebugLogScreen(
                    onDisableDebug = {
                        viewModel.disableDebugMode()
                        pop()
                    },
                    onBack = { pop() }
                )
            }
        }
    }
}
