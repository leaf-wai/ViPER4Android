package com.llsl.viper4android.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.llsl.viper4android.R
import com.llsl.viper4android.ui.screens.main.DriverStatus
import com.llsl.viper4android.ui.theme.StatusColors
import com.llsl.viper4android.ui.util.rememberPlatformToast
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun SettingsDialog(
    autoStartEnabled: Boolean,
    globalModeEnabled: Boolean,
    aidlModeActive: Boolean,
    debugModeEnabled: Boolean,
    driverStatus: DriverStatus,
    appVersionName: String,
    onAutoStartChanged: (Boolean) -> Unit,
    onGlobalModeChanged: (Boolean) -> Unit,
    onOpenExcludedApps: () -> Unit,
    onImportPreset: () -> Unit,
    onImportKernel: () -> Unit,
    onDebugUnlocked: () -> Unit,
    onImportVdc: () -> Unit,
    onCheckUpdate: () -> Unit,
    onDismiss: () -> Unit,
    show: Boolean = true,
) {
    val platform = rememberPlatformToast()
    val tapCount = remember { mutableIntStateOf(0) }
    val debugModeEnabledStr = stringResource(R.string.debug_mode_enabled)
    val debugAlreadyEnabledStr = stringResource(R.string.debug_already_enabled)
    val rowMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    val titleMargin = PaddingValues(horizontal = 24.dp, vertical = 8.dp)

    WindowDialog(
        title = stringResource(R.string.menu_settings),
        show = show,
        onDismissRequest = onDismiss,
        insideMargin = DpSize(0.dp, 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                SmallTitle(
                    text = stringResource(R.string.settings_general_section),
                    insideMargin = titleMargin,
                )
                SwitchPreference(
                    title = stringResource(R.string.settings_auto_start),
                    summary = stringResource(R.string.settings_auto_start_desc),
                    checked = autoStartEnabled,
                    onCheckedChange = onAutoStartChanged,
                    insideMargin = rowMargin,
                )
                SwitchPreference(
                    title = stringResource(R.string.settings_global_mode),
                    summary = stringResource(R.string.settings_global_mode_desc),
                    checked = globalModeEnabled,
                    onCheckedChange = onGlobalModeChanged,
                    insideMargin = rowMargin,
                )
                if (!globalModeEnabled) {
                    ArrowPreference(
                        title = stringResource(R.string.settings_excluded_apps),
                        summary = stringResource(R.string.settings_excluded_apps_desc),
                        onClick = onOpenExcludedApps,
                        insideMargin = rowMargin,
                    )
                }

                SmallTitle(
                    text = stringResource(R.string.settings_files_section),
                    insideMargin = titleMargin,
                )
                ArrowPreference(
                    title = stringResource(R.string.settings_import_preset),
                    summary = stringResource(R.string.settings_import_preset_desc),
                    onClick = onImportPreset,
                    insideMargin = rowMargin,
                    startAction = {
                        Icon(
                            imageVector = MiuixIcons.Download,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(20.dp),
                        )
                    },
                )
                ArrowPreference(
                    title = stringResource(R.string.settings_import_kernel),
                    summary = stringResource(R.string.settings_import_kernel_desc),
                    onClick = onImportKernel,
                    insideMargin = rowMargin,
                    startAction = {
                        Icon(
                            imageVector = MiuixIcons.Download,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(20.dp),
                        )
                    },
                )
                ArrowPreference(
                    title = stringResource(R.string.settings_import_vdc),
                    summary = stringResource(R.string.settings_import_vdc_desc),
                    onClick = onImportVdc,
                    insideMargin = rowMargin,
                    startAction = {
                        Icon(
                            imageVector = MiuixIcons.Download,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(20.dp),
                        )
                    },
                )

                SmallTitle(
                    text = stringResource(R.string.settings_about_section),
                    insideMargin = titleMargin,
                )
                ArrowPreference(
                    title = stringResource(R.string.settings_driver_version),
                    endActions = {
                        Text(
                            text = if (driverStatus.installed) driverStatus.versionName else "—",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                    onClick = {
                        if (debugModeEnabled) {
                            platform.showToast(debugAlreadyEnabledStr, long = false)
                        } else {
                            tapCount.intValue++
                            if (tapCount.intValue >= 7) {
                                tapCount.intValue = 0
                                onDebugUnlocked()
                                platform.showToast(debugModeEnabledStr, long = false)
                            }
                        }
                    },
                    insideMargin = rowMargin,
                )
                BasicComponent(
                    title = stringResource(R.string.settings_driver_arch),
                    endActions = {
                        Text(
                            text = if (driverStatus.installed) driverStatus.architecture else "—",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    },
                    insideMargin = rowMargin,
                )
                if (aidlModeActive) {
                    val activeColor = StatusColors.healthy
                    BasicComponent(
                        title = stringResource(R.string.settings_aidl_mode),
                        endActions = {
                            Canvas(modifier = Modifier.size(10.dp)) {
                                drawCircle(activeColor)
                            }
                        },
                        insideMargin = rowMargin,
                    )
                }
                ArrowPreference(
                    title = stringResource(R.string.settings_app_version),
                    endActions = {
                        Text(
                            text = appVersionName,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                    onClick = onCheckUpdate,
                    insideMargin = rowMargin,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                text = stringResource(R.string.action_close),
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
        }
    }
}
