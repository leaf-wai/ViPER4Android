package com.llsl.viper4android.ui.screens.status

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llsl.viper4android.R
import com.llsl.viper4android.ui.components.StatusBadge
import com.llsl.viper4android.ui.screens.main.DriverStatus
import com.llsl.viper4android.ui.theme.StatusColors
import com.llsl.viper4android.ui.theme.ViperTheme
import com.llsl.viper4android.utils.RootShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

private val DialogRowInsideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp)

/**
 * 驱动详细状态弹窗：集中展示版本（名称与版本号）、架构、工作模式、实时音频流、采样率及 ROOT 权限状态，
 * 由概览页驱动状态摘要卡及音效页顶栏信息按钮共用，避免概览页重复堆砌明细行。
 */
@Composable
fun DriverStatusDialog(
    driverStatus: DriverStatus,
    aidlMode: Boolean,
    globalMode: Boolean,
    onDismiss: () -> Unit,
    show: Boolean = true,
) {
    var rootGranted by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(show) {
        if (show) {
            rootGranted = withContext(Dispatchers.IO) { RootShell.isRootAvailable() }
        }
    }

    WindowDialog(
        title = stringResource(R.string.menu_driver_status),
        show = show,
        insideMargin = DpSize(0.dp, 24.dp),
        onDismissRequest = onDismiss,
    ) {
        DriverStatusDialogContent(
            driverStatus = driverStatus,
            aidlMode = aidlMode,
            globalMode = globalMode,
            rootGranted = rootGranted,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun DriverStatusDialogContent(
    driverStatus: DriverStatus,
    aidlMode: Boolean,
    globalMode: Boolean,
    rootGranted: Boolean?,
    onDismiss: () -> Unit,
) {
    val aidlLabel = if (aidlMode) {
        stringResource(R.string.overview_mode_aidl)
    } else {
        stringResource(R.string.overview_mode_legacy)
    }
    val scopeLabel = if (globalMode) {
        stringResource(R.string.overview_mode_global)
    } else {
        stringResource(R.string.overview_mode_session)
    }
    val versionText = if (driverStatus.installed) {
        val name = driverStatus.versionName.ifEmpty { "—" }
        if (driverStatus.versionCode >= 0) "$name (${driverStatus.versionCode})" else name
    } else {
        stringResource(R.string.driver_not_found)
    }

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
            if (!driverStatus.installed) {
                BasicComponent(
                    title = stringResource(R.string.driver_not_found),
                    insideMargin = DialogRowInsideMargin,
                    startAction = {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = stringResource(R.string.driver_not_found),
                            modifier = Modifier.padding(end = 6.dp),
                            tint = StatusColors.danger,
                        )
                    },
                    endActions = {
                        StatusBadge(
                            text = stringResource(R.string.overview_driver_not_installed),
                            color = StatusColors.danger,
                        )
                    },
                )
            } else {
                DriverInfoRow(
                    icon = Icons.Rounded.Tag,
                    title = stringResource(R.string.settings_driver_version),
                    value = versionText,
                )
                DriverInfoRow(
                    icon = Icons.Rounded.DeveloperBoard,
                    title = stringResource(R.string.driver_architecture),
                    value = driverStatus.architecture.ifEmpty {
                        stringResource(R.string.status_unknown)
                    },
                )
                DriverInfoRow(
                    icon = Icons.Rounded.Memory,
                    title = stringResource(R.string.overview_driver_mode),
                    value = "$aidlLabel · $scopeLabel",
                )
                BasicComponent(
                    title = stringResource(R.string.driver_streaming),
                    insideMargin = DialogRowInsideMargin,
                    startAction = {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = stringResource(R.string.driver_streaming),
                            modifier = Modifier.padding(end = 6.dp),
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    },
                    endActions = {
                        StatusBadge(
                            text = if (driverStatus.streaming) {
                                stringResource(R.string.status_active)
                            } else {
                                stringResource(R.string.overview_status_standby)
                            },
                            color = if (driverStatus.streaming) {
                                StatusColors.healthy
                            } else {
                                StatusColors.neutral
                            },
                        )
                    },
                )
                DriverInfoRow(
                    icon = Icons.Rounded.Speed,
                    title = stringResource(R.string.driver_sampling_rate),
                    value = if (driverStatus.samplingRate > 0) {
                        "${driverStatus.samplingRate} Hz"
                    } else {
                        stringResource(R.string.status_unknown)
                    },
                )
            }
            RootStatusRow(rootGranted = rootGranted)
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

@Composable
private fun RootStatusRow(
    rootGranted: Boolean?,
) {
    val badgeText = when (rootGranted) {
        true -> stringResource(R.string.driver_root_granted)
        false -> stringResource(R.string.driver_root_denied)
        null -> stringResource(R.string.driver_root_checking)
    }
    val badgeColor = when (rootGranted) {
        true -> StatusColors.healthy
        false -> StatusColors.danger
        null -> StatusColors.neutral
    }
    BasicComponent(
        title = stringResource(R.string.driver_root_permission),
        insideMargin = DialogRowInsideMargin,
        startAction = {
            Icon(
                imageVector = Icons.Rounded.AdminPanelSettings,
                contentDescription = stringResource(R.string.driver_root_permission),
                modifier = Modifier.padding(end = 6.dp),
                tint = MiuixTheme.colorScheme.onBackground,
            )
        },
        endActions = {
            StatusBadge(
                text = badgeText,
                color = badgeColor,
            )
        },
    )
}

@Composable
private fun DriverInfoRow(
    icon: ImageVector,
    title: String,
    value: String,
) {
    BasicComponent(
        title = title,
        insideMargin = DialogRowInsideMargin,
        startAction = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.padding(end = 6.dp),
                tint = MiuixTheme.colorScheme.onBackground,
            )
        },
        endActions = {
            Text(
                text = value,
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        },
    )
}

@Preview(name = "Driver Installed", showBackground = true)
@Composable
private fun DriverStatusInstalledPreview() {
    ViperTheme {
        DriverStatusDialogContent(
            driverStatus = DriverStatus(
                installed = true,
                versionCode = 2504,
                versionName = "2.5.0.4",
                architecture = "arm64-v8a",
                streaming = true,
                samplingRate = 48000,
            ),
            aidlMode = true,
            globalMode = false,
            rootGranted = true,
            onDismiss = {},
        )
    }
}

@Preview(name = "Driver Not Installed", showBackground = true)
@Composable
private fun DriverStatusNotInstalledPreview() {
    ViperTheme {
        DriverStatusDialogContent(
            driverStatus = DriverStatus(installed = false),
            aidlMode = false,
            globalMode = false,
            rootGranted = false,
            onDismiss = {},
        )
    }
}
