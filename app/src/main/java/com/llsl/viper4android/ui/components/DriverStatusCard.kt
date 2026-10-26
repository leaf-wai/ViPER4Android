package com.llsl.viper4android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.llsl.viper4android.R
import com.llsl.viper4android.ui.screens.main.DriverStatus
import com.llsl.viper4android.ui.theme.StatusColors
import com.llsl.viper4android.ui.theme.ViperTheme
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * KernelSU MIUIX 风格驱动安装状态卡片：
 * - 已安装：绿色卡片底色 + 右下角偏移裁剪的 110dp 大号绿色打钩圆圈图标 + Tilt 按压反馈；
 * - 未安装：标准卡片 + ErrorOutline 图标提示未找到驱动。
 */
@Composable
fun DriverStatusCard(
    driverStatus: DriverStatus,
    masterEnabled: Boolean,
    aidlMode: Boolean,
    globalMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (driverStatus.installed) {
        val statusTitle = if (masterEnabled) {
            stringResource(R.string.overview_driver_working)
        } else {
            stringResource(R.string.overview_driver_installed)
        }
        val versionDisplay = driverStatus.versionName.ifEmpty {
            if (driverStatus.versionCode >= 0) driverStatus.versionCode.toString() else "--"
        }
        val modeLabel = buildString {
            append(
                if (aidlMode) stringResource(R.string.overview_mode_aidl)
                else stringResource(R.string.overview_mode_legacy),
            )
            append(" · ")
            append(
                if (globalMode) stringResource(R.string.overview_mode_global)
                else stringResource(R.string.overview_mode_session),
            )
        }
        val streamTag = if (driverStatus.streaming) {
            stringResource(R.string.status_active)
        } else {
            stringResource(R.string.overview_status_standby)
        }

        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(
                color = StatusColors.workingCardContainer,
            ),
            onClick = onClick,
            showIndication = true,
            pressFeedbackType = PressFeedbackType.Tilt,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(27.dp, 31.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Icon(
                        modifier = Modifier.size(110.dp),
                        imageVector = Icons.Rounded.CheckCircleOutline,
                        tint = StatusColors.workingCheckIcon,
                        contentDescription = null,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 112.dp)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = statusTitle,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(1.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.overview_driver_version_format, versionDisplay),
                                modifier = Modifier.weight(1f, fill = false),
                                fontSize = 15.sp,
                                color = MiuixTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.width(8.dp))
                            StatusBadge(
                                text = streamTag,
                                color = StatusColors.workingTagContent,
                                backgroundColor = StatusColors.workingTagBackground,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = modeLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            onClick = onClick,
            showIndication = true,
            pressFeedbackType = PressFeedbackType.Tilt,
        ) {
            BasicComponent(
                title = stringResource(R.string.overview_driver_not_installed),
                summary = stringResource(R.string.driver_not_found),
                startAction = {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = stringResource(R.string.overview_driver_not_installed),
                        modifier = Modifier.padding(end = 6.dp),
                        tint = StatusColors.danger,
                    )
                },
            )
        }
    }
}

@Preview(name = "Driver Working", showBackground = true)
@Composable
private fun DriverStatusCardWorkingPreview() {
    ViperTheme {
        DriverStatusCard(
            driverStatus = DriverStatus(
                installed = true,
                versionCode = 2504,
                versionName = "2.5.0.4",
                architecture = "arm64-v8a",
                streaming = true,
                samplingRate = 48000,
            ),
            masterEnabled = true,
            aidlMode = true,
            globalMode = false,
            onClick = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Preview(name = "Driver Not Installed", showBackground = true)
@Composable
private fun DriverStatusCardNotInstalledPreview() {
    ViperTheme {
        DriverStatusCard(
            driverStatus = DriverStatus(installed = false),
            masterEnabled = false,
            aidlMode = false,
            globalMode = false,
            onClick = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}
